package org.sonatype.aether.spi.io;

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
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A utility component to perform file-based operations.
 * 
 * @author Benjamin Hanzelmann
 * @author Benjamin Bentmann
 */
public interface FileProcessor
{

    /**
     * Creates the directory named by the given abstract pathname, including any necessary but nonexistent parent
     * directories. Note that if this operation fails it may have succeeded in creating some of the necessary parent
     * directories.
     * 
     * @param directory The directory to create, may be {@code null}.
     * @return {@code true} if and only if the directory was created, along with all necessary parent directories;
     *         {@code false} otherwise
     */
    boolean mkdirs( File directory );

    /**
     * Writes the given data to a file. UTF-8 is assumed as encoding for the data.
     * 
     * @param file The file to write to, must not be {@code null}. This file will be overwritten.
     * @param data The data to write, may be {@code null}.
     * @throws IOException If an I/O error occurs.
     */
    void write( File file, String data )
        throws IOException;

    /**
     * Copies the specified source file to the given target file. Creates the necessary directories for the target file.
     * In case of an error, the created directories will be left on the file system.
     * 
     * @param source The file to copy from, must not be {@code null}.
     * @param target The file to copy to, must not be {@code null}.
     * @param listener The listener to notify about the copy progress, may be {@code null}.
     * @return The number of copied bytes.
     * @throws IOException If an I/O error occurs.
     */
    long copy( File source, File target, ProgressListener listener )
        throws IOException;

    /**
     * A listener object that is notified for every progress made while copying files.
     * 
     * @author Benjamin Hanzelmann
     * @see FileUtils#copy(File, File, ProgressListener)
     */
    public interface ProgressListener
    {

        void progressed( ByteBuffer buffer )
            throws IOException;

    }

}
