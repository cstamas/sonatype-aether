package org.sonatype.aether.impl;

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

import java.util.Collection;
import java.util.List;

import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;

/**
 * @author Benjamin Bentmann
 */
public interface MetadataResolver
{

    /**
     * Resolves the paths for a collection of metadata. Metadata will be downloaded if necessary.
     */
    List<MetadataResult> resolveMetadata( RepositorySystemSession session, Collection<? extends MetadataRequest> requests );

}
