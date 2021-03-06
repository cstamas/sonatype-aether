package org.sonatype.aether.util.artifact;

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

/**
 * The keys for common properties of artifacts.
 * 
 * @author Benjamin Bentmann
 * @see org.sonatype.aether.artifact.Artifact#getProperties()
 */
public final class ArtifactProperties
{

    /**
     * A high-level characterization of the artifact, e.g. "maven-plugin" or "test-jar".
     */
    public static final String TYPE = "type";

    /**
     * The programming language this artifact is relevant for, e.g. "java" or "none".
     */
    public static final String LANGUAGE = "language";

    /**
     * The (expected) path to the artifact on the local filesystem. An artifact which has this property set is assumed
     * to be not present in any regular repository and likewise has no artifact descriptor. Artifact resolution will
     * verify the path and resolve the artifact if the path actually denotes an existing file. If the path isn't valid,
     * resolution will fail and no attempts to search local/remote repositories are made.
     */
    public static final String LOCAL_PATH = "localPath";

    /**
     * A boolean flag indicating whether the artifact presents some kind of bundle that physically includes its
     * dependencies, e.g. a fat WAR.
     */
    public static final String INCLUDES_DEPENDENCIES = "includesDependencies";

    /**
     * A boolean flag indicating whether the artifact is meant to be used for the compile/runtime/test build path of a
     * consumer project.
     */
    public static final String CONSTITUTES_BUILD_PATH = "constitutesBuildPath";

    private ArtifactProperties()
    {
        // hide constructor
    }

}
