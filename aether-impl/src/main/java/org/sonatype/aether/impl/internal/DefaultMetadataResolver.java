package org.sonatype.aether.impl.internal;

/*
 * Copyright (c) 2010 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0, 
 * and you may not use this file except in compliance with the Apache License Version 2.0. 
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the Apache License Version 2.0 is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.ConfigurationProperties;
import org.sonatype.aether.RepositoryListener;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.impl.MetadataResolver;
import org.sonatype.aether.impl.RemoteRepositoryManager;
import org.sonatype.aether.impl.UpdateCheck;
import org.sonatype.aether.impl.UpdateCheckManager;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.transfer.MetadataNotFoundException;
import org.sonatype.aether.transfer.MetadataTransferException;
import org.sonatype.aether.transfer.NoRepositoryConnectorException;
import org.sonatype.aether.util.listener.DefaultRepositoryEvent;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.spi.connector.MetadataDownload;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.spi.locator.Service;
import org.sonatype.aether.spi.locator.ServiceLocator;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.spi.log.NullLogger;

/**
 * @author Benjamin Bentmann
 */
@Component( role = MetadataResolver.class )
public class DefaultMetadataResolver
    implements MetadataResolver, Service
{

    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement
    private UpdateCheckManager updateCheckManager;

    @Requirement
    private RemoteRepositoryManager remoteRepositoryManager;

    public DefaultMetadataResolver()
    {
        // enables default constructor
    }

    public DefaultMetadataResolver( Logger logger, UpdateCheckManager updateCheckManager,
                                    RemoteRepositoryManager remoteRepositoryManager )
    {
        setLogger( logger );
        setUpdateCheckManager( updateCheckManager );
        setRemoteRepositoryManager( remoteRepositoryManager );
    }

    public void initService( ServiceLocator locator )
    {
        setLogger( locator.getService( Logger.class ) );
        setUpdateCheckManager( locator.getService( UpdateCheckManager.class ) );
        setRemoteRepositoryManager( locator.getService( RemoteRepositoryManager.class ) );
    }

    public DefaultMetadataResolver setLogger( Logger logger )
    {
        this.logger = ( logger != null ) ? logger : NullLogger.INSTANCE;
        return this;
    }

    public DefaultMetadataResolver setUpdateCheckManager( UpdateCheckManager updateCheckManager )
    {
        if ( updateCheckManager == null )
        {
            throw new IllegalArgumentException( "update check manager has not been specified" );
        }
        this.updateCheckManager = updateCheckManager;
        return this;
    }

    public DefaultMetadataResolver setRemoteRepositoryManager( RemoteRepositoryManager remoteRepositoryManager )
    {
        if ( remoteRepositoryManager == null )
        {
            throw new IllegalArgumentException( "remote repository manager has not been specified" );
        }
        this.remoteRepositoryManager = remoteRepositoryManager;
        return this;
    }

    public List<MetadataResult> resolveMetadata( RepositorySystemSession session,
                                                 Collection<? extends MetadataRequest> requests )
    {
        List<MetadataResult> results = new ArrayList<MetadataResult>( requests.size() );

        List<ResolveTask> tasks = new ArrayList<ResolveTask>( requests.size() );
        CountDownLatch latch = new CountDownLatch( requests.size() );

        Map<File, Long> localLastUpdates = new HashMap<File, Long>();

        for ( MetadataRequest request : requests )
        {
            MetadataResult result = new MetadataResult( request );
            results.add( result );

            Metadata metadata = request.getMetadata();
            RemoteRepository repository = request.getRepository();

            if ( repository == null )
            {
                metadataResolving( session, metadata, session.getLocalRepositoryManager().getRepository() );

                File localFile = getFile( session, metadata, null, null );
                if ( localFile.isFile() )
                {
                    metadata = metadata.setFile( localFile );
                    result.setMetadata( metadata );
                }
                else
                {
                    result.setException( new MetadataNotFoundException( metadata, null ) );
                }

                metadataResolved( session, metadata, session.getLocalRepositoryManager().getRepository(),
                                  result.getException() );
                continue;
            }

            List<RemoteRepository> repositories = getEnabledSourceRepositories( repository, metadata.getNature() );

            if ( repositories.isEmpty() )
            {
                continue;
            }

            metadataResolving( session, metadata, repository );

            File metadataFile = getFile( session, metadata, repository, request.getRequestContext() );

            if ( session.isOffline() )
            {
                if ( metadataFile.isFile() )
                {
                    metadata = metadata.setFile( metadataFile );
                    result.setMetadata( metadata );
                    metadataResolved( session, metadata, repository, null );
                }
                else
                {
                    String msg =
                        "The repository system is offline but the metadata " + metadata + " from " + repository
                            + " is not available in the local repository.";
                    result.setException( new MetadataNotFoundException( metadata, repository, msg ) );
                    metadataResolved( session, metadata, repository, result.getException() );
                }
                continue;
            }

            Long localLastUpdate = null;
            if ( request.isFavorLocalRepository() )
            {
                File localFile = getFile( session, metadata, null, null );
                localLastUpdate = localLastUpdates.get( localFile );
                if ( localLastUpdate == null )
                {
                    localLastUpdate = Long.valueOf( localFile.lastModified() );
                    localLastUpdates.put( localFile, localLastUpdate );
                }
            }

            List<UpdateCheck<Metadata, MetadataTransferException>> checks =
                new ArrayList<UpdateCheck<Metadata, MetadataTransferException>>();
            Exception exception = null;
            for ( RemoteRepository repo : repositories )
            {
                UpdateCheck<Metadata, MetadataTransferException> check =
                    new UpdateCheck<Metadata, MetadataTransferException>();
                check.setLocalLastUpdated( ( localLastUpdate != null ) ? localLastUpdate.longValue() : 0 );
                check.setItem( metadata );
                check.setFile( metadataFile );
                check.setRepository( repository );
                check.setAuthoritativeRepository( repo );
                check.setPolicy( getPolicy( session, repo, metadata.getNature() ).getUpdatePolicy() );
                updateCheckManager.checkMetadata( session, check );

                if ( check.isRequired() )
                {
                    checks.add( check );
                }
                else if ( exception == null )
                {
                    exception = check.getException();
                }
            }

            if ( !checks.isEmpty() )
            {
                RepositoryPolicy policy = getPolicy( session, repository, metadata.getNature() );

                ResolveTask task =
                    new ResolveTask( session, result, metadataFile, checks, policy.getChecksumPolicy(), latch );
                tasks.add( task );
            }
            else
            {
                result.setException( exception );
                if ( metadataFile.isFile() )
                {
                    metadata = metadata.setFile( metadataFile );
                    result.setMetadata( metadata );
                }
                metadataResolved( session, metadata, repository, result.getException() );
            }
        }

        if ( !tasks.isEmpty() )
        {
            int threads = ConfigurationProperties.get( session, "aether.metadataResolver.threads", 4 );
            Executor executor = getExecutor( Math.min( tasks.size(), threads ) );
            try
            {
                while ( latch.getCount() > tasks.size() )
                {
                    latch.countDown();
                }
                for ( ResolveTask task : tasks )
                {
                    executor.execute( task );
                }
                latch.await();
                for ( ResolveTask task : tasks )
                {
                    task.result.setException( task.exception );
                }
            }
            catch ( InterruptedException e )
            {
                for ( ResolveTask task : tasks )
                {
                    MetadataResult result = task.result;
                    result.setException( new MetadataTransferException( result.getRequest().getMetadata(),
                                                                        result.getRequest().getRepository(), e ) );
                }
            }
            finally
            {
                shutdown( executor );
            }
            for ( ResolveTask task : tasks )
            {
                Metadata metadata = task.request.getMetadata();
                File metadataFile = task.metadataFile;
                if ( metadataFile.isFile() )
                {
                    metadata = metadata.setFile( metadataFile );
                    task.result.setMetadata( metadata );
                }
                if ( task.result.getException() == null )
                {
                    task.result.setUpdated( true );
                }
                metadataResolved( session, metadata, task.request.getRepository(), task.result.getException() );
            }
        }

        return results;
    }

    private List<RemoteRepository> getEnabledSourceRepositories( RemoteRepository repository, Metadata.Nature nature )
    {
        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();

        if ( repository.isRepositoryManager() )
        {
            for ( RemoteRepository repo : repository.getMirroredRepositories() )
            {
                if ( isEnabled( repo, nature ) )
                {
                    repositories.add( repo );
                }
            }
        }
        else if ( isEnabled( repository, nature ) )
        {
            repositories.add( repository );
        }

        return repositories;
    }

    private boolean isEnabled( RemoteRepository repository, Metadata.Nature nature )
    {
        if ( !Metadata.Nature.SNAPSHOT.equals( nature ) && repository.getPolicy( false ).isEnabled() )
        {
            return true;
        }
        if ( !Metadata.Nature.RELEASE.equals( nature ) && repository.getPolicy( true ).isEnabled() )
        {
            return true;
        }
        return false;
    }

    private RepositoryPolicy getPolicy( RepositorySystemSession session, RemoteRepository repository,
                                        Metadata.Nature nature )
    {
        boolean releases = !Metadata.Nature.SNAPSHOT.equals( nature );
        boolean snapshots = !Metadata.Nature.RELEASE.equals( nature );
        return remoteRepositoryManager.getPolicy( session, repository, releases, snapshots );
    }

    private File getFile( RepositorySystemSession session, Metadata metadata, RemoteRepository repository,
                          String context )
    {
        LocalRepositoryManager lrm = session.getLocalRepositoryManager();
        String path;
        if ( repository != null )
        {
            path = lrm.getPathForRemoteMetadata( metadata, repository, context );
        }
        else
        {
            path = lrm.getPathForLocalMetadata( metadata );
        }
        return new File( lrm.getRepository().getBasedir(), path );
    }

    private void metadataResolving( RepositorySystemSession session, Metadata metadata, ArtifactRepository repository )
    {
        RepositoryListener listener = session.getRepositoryListener();
        if ( listener != null )
        {
            DefaultRepositoryEvent event = new DefaultRepositoryEvent( session, metadata );
            event.setRepository( repository );
            listener.metadataResolving( event );
        }
    }

    private void metadataResolved( RepositorySystemSession session, Metadata metadata, ArtifactRepository repository,
                                   Exception exception )
    {
        RepositoryListener listener = session.getRepositoryListener();
        if ( listener != null )
        {
            DefaultRepositoryEvent event = new DefaultRepositoryEvent( session, metadata );
            event.setRepository( repository );
            event.setException( exception );
            event.setFile( metadata.getFile() );
            listener.metadataResolved( event );
        }
    }

    private Executor getExecutor( int threads )
    {
        if ( threads <= 1 )
        {
            return new Executor()
            {
                public void execute( Runnable command )
                {
                    command.run();
                }
            };
        }
        else
        {
            return new ThreadPoolExecutor( threads, threads, 3, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>() );
        }
    }

    private void shutdown( Executor executor )
    {
        if ( executor instanceof ExecutorService )
        {
            ( (ExecutorService) executor ).shutdown();
        }
    }

    class ResolveTask
        implements Runnable
    {

        final RepositorySystemSession session;

        final MetadataResult result;

        final MetadataRequest request;

        final File metadataFile;

        final String policy;

        final List<UpdateCheck<Metadata, MetadataTransferException>> checks;

        final CountDownLatch latch;

        volatile MetadataTransferException exception;

        public ResolveTask( RepositorySystemSession session, MetadataResult result, File metadataFile,
                            List<UpdateCheck<Metadata, MetadataTransferException>> checks, String policy,
                            CountDownLatch latch )
        {
            this.session = session;
            this.result = result;
            this.request = result.getRequest();
            this.metadataFile = metadataFile;
            this.policy = policy;
            this.checks = checks;
            this.latch = latch;
        }

        public void run()
        {
            try
            {
                List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
                for ( UpdateCheck<Metadata, MetadataTransferException> check : checks )
                {
                    repositories.add( check.getAuthoritativeRepository() );
                }

                MetadataDownload download = new MetadataDownload();
                download.setMetadata( request.getMetadata() );
                download.setRequestContext( request.getRequestContext() );
                download.setFile( metadataFile );
                download.setChecksumPolicy( policy );
                download.setRepositories( repositories );

                RepositoryConnector connector =
                    remoteRepositoryManager.getRepositoryConnector( session, request.getRepository() );
                try
                {
                    connector.get( null, Arrays.asList( download ) );
                }
                finally
                {
                    connector.close();
                }

                exception = download.getException();

                if ( request.isDeleteLocalCopyIfMissing() && exception instanceof MetadataNotFoundException )
                {
                    download.getFile().delete();
                }
            }
            catch ( NoRepositoryConnectorException e )
            {
                exception = new MetadataTransferException( request.getMetadata(), request.getRepository(), e );
            }
            finally
            {
                latch.countDown();
            }

            for ( UpdateCheck<Metadata, MetadataTransferException> check : checks )
            {
                updateCheckManager.touchMetadata( session, check.setException( exception ) );
            }
        }

    }

}
