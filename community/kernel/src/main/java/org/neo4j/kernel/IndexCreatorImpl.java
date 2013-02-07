/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel;

import static java.util.Arrays.asList;
import static org.neo4j.helpers.collection.IteratorUtil.addToCollection;
import static org.neo4j.helpers.collection.IteratorUtil.single;

import java.util.ArrayList;
import java.util.Collection;

import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.schema.IndexCreator;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.kernel.api.ConstraintViolationKernelException;
import org.neo4j.kernel.api.StatementContext;
import org.neo4j.kernel.impl.core.KeyHolder;
import org.neo4j.kernel.impl.core.PropertyIndex;

public class IndexCreatorImpl implements IndexCreator
{
    private final StatementContext context;
    private final Collection<String> propertyKeys;
    private final Label label;
    private final KeyHolder<PropertyIndex> propertyKeyManager;

    IndexCreatorImpl( StatementContext context, KeyHolder<PropertyIndex> propertyKeyManager, Label label )
    {
        this.context = context;
        this.propertyKeyManager = propertyKeyManager;
        this.label = label;
        this.propertyKeys = new ArrayList<String>();
    }
    
    private IndexCreatorImpl( StatementContext context, KeyHolder<PropertyIndex> propertyKeyManager,
            Label label, Collection<String> propertyKeys )
    {
        this.context = context;
        this.propertyKeyManager = propertyKeyManager;
        this.label = label;
        this.propertyKeys = propertyKeys;
    }
    
    @Override
    public IndexCreator on( String propertyKey )
    {
        if ( !propertyKeys.isEmpty() )
            throw new UnsupportedOperationException( "Only a single property key is supported at the moment" );
        return new IndexCreatorImpl( context, propertyKeyManager, label,
                addToCollection( asList( propertyKey ), new ArrayList<String>( propertyKeys ) ) );
    }

    @Override
    public IndexDefinition create() throws ConstraintViolationException
    {
        if ( propertyKeys.isEmpty() )
            throw new ConstraintViolationException( "An index needs at least one property key to index" );
        
        try
        {
            String singlePropertyKey = single( propertyKeys );
            context.addIndexRule( context.getOrCreateLabelId( label.name() ),
                    propertyKeyManager.getOrCreateId( singlePropertyKey ) );
            return new IndexDefinitionImpl( label, singlePropertyKey );
        }
        catch ( ConstraintViolationKernelException e )
        {
            throw new ConstraintViolationException( "", e );
        }
    }
}