package org.sonatype.aether.util.filter;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.test.util.NodeBuilder;

public class AndDependencyFilterTest
    extends AbstractDependencyFilterTest
{
    @Test
    public void acceptTest()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.artifactId( "test" );
        List<DependencyNode> parents = new LinkedList<DependencyNode>();
        

        // Empty AND
        assertTrue( new AndDependencyFilter().accept( builder.build() , parents) );

        // Basic Boolean Input
        assertTrue( new AndDependencyFilter( getAcceptFilter() ).accept( builder.build() , parents) );
        assertFalse( new AndDependencyFilter( getDenyFilter() ).accept( builder.build() , parents) );

        assertFalse( new AndDependencyFilter( getDenyFilter(), getDenyFilter() ).accept( builder.build() , parents) );
        assertFalse( new AndDependencyFilter( getDenyFilter(), getAcceptFilter() ).accept( builder.build() , parents) );
        assertFalse( new AndDependencyFilter( getAcceptFilter(), getDenyFilter() ).accept( builder.build() , parents) );
        assertTrue( new AndDependencyFilter( getAcceptFilter(), getAcceptFilter() ).accept( builder.build() , parents) );

        assertFalse( new AndDependencyFilter( getDenyFilter(), getDenyFilter(), getDenyFilter() ).accept( builder.build() , parents) );
        assertFalse( new AndDependencyFilter( getAcceptFilter(), getDenyFilter(), getDenyFilter() ).accept( builder.build() , parents) );
        assertFalse( new AndDependencyFilter( getAcceptFilter(), getAcceptFilter(), getDenyFilter() ).accept( builder.build() , parents) );
        assertTrue( new AndDependencyFilter( getAcceptFilter(), getAcceptFilter(), getAcceptFilter() ).accept( builder.build() , parents) );

        // User another constructor
        Collection<DependencyFilter> filters = new LinkedList<DependencyFilter>();
        filters.add( getDenyFilter() );
        filters.add( getAcceptFilter() );
        assertFalse( new AndDependencyFilter( filters ).accept( builder.build() , parents) );

        filters = new LinkedList<DependencyFilter>();
        filters.add( getDenyFilter() );
        filters.add( getDenyFilter() );
        assertFalse( new AndDependencyFilter( filters ).accept( builder.build() , parents) );

        filters = new LinkedList<DependencyFilter>();
        filters.add( getAcceptFilter() );
        filters.add( getAcceptFilter() );
        assertTrue( new AndDependencyFilter( filters ).accept( builder.build() , parents) );

        // newInstance
        assertTrue( AndDependencyFilter.newInstance( getAcceptFilter(), getAcceptFilter() ).accept( builder.build() , parents) );
        assertFalse( AndDependencyFilter.newInstance( getAcceptFilter(), getDenyFilter() ).accept( builder.build() , parents) );

        assertFalse( AndDependencyFilter.newInstance( getDenyFilter(), null ).accept( builder.build() , parents) );
        assertTrue( AndDependencyFilter.newInstance( getAcceptFilter(), null ).accept( builder.build() , parents) );
        assertNull( AndDependencyFilter.newInstance( null, null ) );
    }

}
