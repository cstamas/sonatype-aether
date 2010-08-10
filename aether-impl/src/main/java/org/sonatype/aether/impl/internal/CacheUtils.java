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

import java.util.Iterator;
import java.util.List;

import org.sonatype.aether.ArtifactRepository;
import org.sonatype.aether.LocalRepository;
import org.sonatype.aether.RemoteRepository;
import org.sonatype.aether.RepositoryPolicy;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.WorkspaceReader;
import org.sonatype.aether.WorkspaceRepository;

/**
 * @author Benjamin Bentmann
 */
public class CacheUtils
{

    public static <T> boolean eq( T s1, T s2 )
    {
        return s1 != null ? s1.equals( s2 ) : s2 == null;
    }

    public static int hash( Object obj )
    {
        return obj != null ? obj.hashCode() : 0;
    }

    public static int repositoriesHashCode( List<RemoteRepository> repositories )
    {
        int result = 17;
        for ( RemoteRepository repository : repositories )
        {
            result = 31 * result + repositoryHashCode( repository );
        }
        return result;
    }

    private static int repositoryHashCode( RemoteRepository repository )
    {
        int result = 17;
        result = 31 * result + hash( repository.getUrl() );
        return result;
    }

    private static boolean repositoryEquals( RemoteRepository r1, RemoteRepository r2 )
    {
        if ( r1 == r2 )
        {
            return true;
        }

        return eq( r1.getId(), r2.getId() ) && eq( r1.getUrl(), r2.getUrl() )
            && policyEquals( r1.getPolicy( false ), r2.getPolicy( false ) )
            && policyEquals( r1.getPolicy( true ), r2.getPolicy( true ) );
    }

    private static boolean policyEquals( RepositoryPolicy p1, RepositoryPolicy p2 )
    {
        if ( p1 == p2 )
        {
            return true;
        }
        // update policy doesn't affect contents
        return p1.isEnabled() == p2.isEnabled() && eq( p1.getChecksumPolicy(), p2.getChecksumPolicy() );
    }

    public static boolean repositoriesEquals( List<RemoteRepository> r1, List<RemoteRepository> r2 )
    {
        if ( r1.size() != r2.size() )
        {
            return false;
        }

        for ( Iterator<RemoteRepository> it1 = r1.iterator(), it2 = r2.iterator(); it1.hasNext(); )
        {
            if ( !repositoryEquals( it1.next(), it2.next() ) )
            {
                return false;
            }
        }

        return true;
    }

    public static WorkspaceRepository getWorkspace( RepositorySystemSession session )
    {
        WorkspaceReader reader = session.getWorkspaceReader();
        return ( reader != null ) ? reader.getRepository() : null;
    }

    public static ArtifactRepository getRepository( RepositorySystemSession session,
                                                    List<RemoteRepository> repositories, Class<?> repoClass,
                                                    String repoId )
    {
        if ( repoClass != null )
        {
            if ( WorkspaceRepository.class.isAssignableFrom( repoClass ) )
            {
                return session.getWorkspaceReader().getRepository();
            }
            else if ( LocalRepository.class.isAssignableFrom( repoClass ) )
            {
                return session.getLocalRepository();
            }
            else
            {
                for ( RemoteRepository repository : repositories )
                {
                    if ( repoId.equals( repository.getId() ) )
                    {
                        return repository;
                    }
                }
            }
        }
        return null;
    }

}
