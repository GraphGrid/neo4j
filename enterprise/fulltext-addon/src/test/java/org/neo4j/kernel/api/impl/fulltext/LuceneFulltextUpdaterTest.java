/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.api.impl.fulltext;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.junit.Test;

import java.util.Arrays;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static java.util.Collections.singletonList;
import static org.neo4j.kernel.api.impl.fulltext.FulltextProvider.FulltextIndexType.NODES;
import static org.neo4j.kernel.api.impl.fulltext.FulltextProvider.FulltextIndexType.RELATIONSHIPS;

public class LuceneFulltextUpdaterTest extends LuceneFulltextTestSupport
{
    public static final String ANALYZER = StandardAnalyzer.class.getCanonicalName();
    @Test
    public void shouldFindNodeWithString() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            provider.init();

            long firstID;
            long secondID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = createNodeIndexableByPropertyValue( "Hello. Hello again." );
                secondID = createNodeIndexableByPropertyValue(
                        "A zebroid (also zedonk, zorse, zebra mule, zonkey, and zebmule) is the offspring of any " +
                        "cross between a zebra and any other equine: essentially, a zebra hybrid." );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsIds( reader, "hello", firstID );
                assertExactQueryFindsIds( reader, "zebra", secondID );
                assertExactQueryFindsIds( reader, "zedonk", secondID );
                assertExactQueryFindsIds( reader, "cross", secondID );
            }
        }
    }

    @Test
    public void shouldFindNodeWithNumber() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            provider.init();

            long firstID;
            long secondID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = createNodeIndexableByPropertyValue( 1 );
                secondID = createNodeIndexableByPropertyValue( 234 );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsIds( reader, "1", firstID );
                assertExactQueryFindsIds( reader, "234", secondID );
            }
        }
    }

    @Test
    public void shouldFindNodeWithBoolean() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            provider.init();

            long firstID;
            long secondID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = createNodeIndexableByPropertyValue( true );
                secondID = createNodeIndexableByPropertyValue( false );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsIds( reader, "true", firstID );
                assertExactQueryFindsIds( reader, "false", secondID );
            }
        }
    }

    @Test
    public void shouldFindNodeWithArrays() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            provider.init();

            long firstID;
            long secondID;
            long thirdID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = createNodeIndexableByPropertyValue( new String[]{"hello", "I", "live", "here"} );
                secondID = createNodeIndexableByPropertyValue( new int[]{1, 27, 48} );
                thirdID = createNodeIndexableByPropertyValue( new int[]{1, 2, 48} );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsIds( reader, "live", firstID );
                assertExactQueryFindsIds( reader, "27", secondID );
                assertExactQueryFindsIds( reader, new String[]{"1", "2"}, secondID, thirdID );
            }
        }
    }

    @Test
    public void shouldRepresentPropertyChanges() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            provider.init();

            long firstID;
            long secondID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = createNodeIndexableByPropertyValue( "Hello. Hello again." );
                secondID = createNodeIndexableByPropertyValue(
                        "A zebroid (also zedonk, zorse, zebra mule, zonkey, and zebmule) is the offspring of any " +
                        "cross between a zebra and any other equine: essentially, a zebra hybrid." );

                tx.success();
            }
            try ( Transaction tx = db.beginTx() )
            {
                setNodeProp( firstID, "Hahahaha! potato!" );
                setNodeProp( secondID, "This one is a potato farmer." );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsNothing( reader, "hello" );
                assertExactQueryFindsNothing( reader, "zebra" );
                assertExactQueryFindsNothing( reader, "zedonk" );
                assertExactQueryFindsNothing( reader, "cross" );
                assertExactQueryFindsIds( reader, "hahahaha", firstID );
                assertExactQueryFindsIds( reader, "farmer", secondID );
                assertExactQueryFindsIds( reader, "potato", firstID, secondID );
            }
        }
    }

    @Test
    public void shouldNotFindRemovedNodes() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            provider.init();

            long firstID;
            long secondID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = createNodeIndexableByPropertyValue( "Hello. Hello again." );
                secondID = createNodeIndexableByPropertyValue(
                        "A zebroid (also zedonk, zorse, zebra mule, zonkey, and zebmule) is the offspring of any " +
                        "cross between a zebra and any other equine: essentially, a zebra hybrid." );

                tx.success();
            }

            try ( Transaction tx = db.beginTx() )
            {
                db.getNodeById( firstID ).delete();
                db.getNodeById( secondID ).delete();

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsNothing( reader, "hello" );
                assertExactQueryFindsNothing( reader, "zebra" );
                assertExactQueryFindsNothing( reader, "zedonk" );
                assertExactQueryFindsNothing( reader, "cross" );
            }
        }
    }

    @Test
    public void shouldNotFindRemovedProperties() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, Arrays.asList( "prop", "prop2" ) );
            provider.init();

            long firstID;
            long secondID;
            long thirdID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = createNodeIndexableByPropertyValue( "Hello. Hello again." );
                secondID = createNodeIndexableByPropertyValue(
                        "A zebroid (also zedonk, zorse, zebra mule, zonkey, and zebmule) is the offspring of any " +
                        "cross between a zebra and any other equine: essentially, a zebra hybrid." );
                thirdID = createNodeIndexableByPropertyValue( "Hello. Hello again." );

                setNodeProp( firstID, "zebra" );
                setNodeProp( secondID, "Hello. Hello again." );

                tx.success();
            }

            try ( Transaction tx = db.beginTx() )
            {
                Node node = db.getNodeById( firstID );
                Node node2 = db.getNodeById( secondID );
                Node node3 = db.getNodeById( thirdID );

                node.setProperty( "prop", "tomtar" );
                node.setProperty( "prop2", "tomtar" );

                node2.setProperty( "prop", "tomtar" );
                node2.setProperty( "prop2", "Hello" );

                node3.removeProperty( "prop" );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsIds( reader, "hello", secondID );
                assertExactQueryFindsNothing( reader, "zebra" );
                assertExactQueryFindsNothing( reader, "zedonk" );
                assertExactQueryFindsNothing( reader, "cross" );
            }
        }
    }

    @Test
    public void shouldOnlyIndexIndexedProperties() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            provider.init();

            long firstID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = createNodeIndexableByPropertyValue( "Hello. Hello again." );
                setNodeProp( firstID, "prop2", "zebra" );

                Node node2 = db.createNode();
                node2.setProperty( "prop2", "zebra" );
                node2.setProperty( "prop3", "hello" );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsIds( reader, "hello", firstID );
                assertExactQueryFindsNothing( reader, "zebra" );
            }
        }
    }

    @Test
    public void shouldSearchAcrossMultipleProperties() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, Arrays.asList( "prop", "prop2" ) );
            provider.init();

            long firstID;
            long secondID;
            long thirdID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = createNodeIndexableByPropertyValue( "Tomtar tomtar oftsat i tomteutstyrsel." );
                secondID = createNodeIndexableByPropertyValue( "Olof och Hans" );
                setNodeProp( secondID, "prop2", "och karl" );

                Node node3 = db.createNode();
                thirdID = node3.getId();
                node3.setProperty( "prop2", "Tomtar som inte tomtar ser upp till tomtar som tomtar." );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsIds( reader, new String[]{"tomtar", "karl"}, firstID, secondID, thirdID );
            }
        }
    }

    @Test
    public void shouldOrderResultsBasedOnRelevance() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, Arrays.asList( "first", "last" ) );
            provider.init();

            long firstID;
            long secondID;
            long thirdID;
            long fourthID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = db.createNode().getId();
                secondID = db.createNode().getId();
                thirdID = db.createNode().getId();
                fourthID = db.createNode().getId();
                setNodeProp( firstID, "first", "Full" );
                setNodeProp( firstID, "last", "Hanks" );
                setNodeProp( secondID, "first", "Tom" );
                setNodeProp( secondID, "last", "Hunk" );
                setNodeProp( thirdID, "first", "Tom" );
                setNodeProp( thirdID, "last", "Hanks" );
                setNodeProp( fourthID, "first", "Tom Hanks" );
                setNodeProp( fourthID, "last", "Tom Hanks" );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsIds( reader, new String[]{"Tom", "Hanks"}, firstID, secondID, thirdID, fourthID );
            }
        }
    }

    @Test
    public void shouldDifferentiateNodesAndRelationships() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            fulltextFactory.createFulltextIndex( "relationships", RELATIONSHIPS, singletonList( "prop" ) );
            provider.init();

            long firstNodeID;
            long secondNodeID;
            long firstRelID;
            long secondRelID;
            try ( Transaction tx = db.beginTx() )
            {
                firstNodeID = createNodeIndexableByPropertyValue( "Hello. Hello again." );
                secondNodeID = createNodeIndexableByPropertyValue(
                        "A zebroid (also zedonk, zorse, zebra mule, zonkey, and zebmule) is the offspring of any " +
                        "cross between a zebra and any other equine: essentially, a zebra hybrid." );
                firstRelID = createRelationshipIndexableByPropertyValue(
                        firstNodeID, secondNodeID, "Hello. Hello again." );
                secondRelID = createRelationshipIndexableByPropertyValue(
                        secondNodeID, firstNodeID, "And now, something completely different" );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsIds( reader, "hello", firstNodeID );
                assertExactQueryFindsIds( reader, "zebra", secondNodeID );
                assertExactQueryFindsNothing( reader, "different" );
            }
            try ( ReadOnlyFulltext reader = provider.getReader( "relationships", RELATIONSHIPS ) )
            {
                assertExactQueryFindsIds( reader, "hello", firstRelID );
                assertExactQueryFindsNothing( reader, "zebra" );
                assertExactQueryFindsIds( reader, "different", secondRelID );
            }
        }
    }

    @Test
    public void fuzzyQueryShouldBeFuzzy() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            provider.init();

            long firstID;
            long secondID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = createNodeIndexableByPropertyValue( "Hello. Hello again." );
                secondID = createNodeIndexableByPropertyValue(
                        "A zebroid (also zedonk, zorse, zebra mule, zonkey, and zebmule) is the offspring of any " +
                        "cross between a zebra and any other equine: essentially, a zebra hybrid." );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertFuzzyQueryFindsIds( reader, "hella", firstID );
                assertFuzzyQueryFindsIds( reader, "zebre", secondID );
                assertFuzzyQueryFindsIds( reader, "zedink", secondID );
                assertFuzzyQueryFindsIds( reader, "cruss", secondID );
                assertExactQueryFindsNothing( reader, "hella" );
                assertExactQueryFindsNothing( reader, "zebre" );
                assertExactQueryFindsNothing( reader, "zedink" );
                assertExactQueryFindsNothing( reader, "cruss" );
            }
        }
    }

    @Test
    public void fuzzyQueryShouldReturnExactMatchesFirst() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            provider.init();

            long firstID;
            long secondID;
            long thirdID;
            long fourthID;
            try ( Transaction tx = db.beginTx() )
            {
                firstID = createNodeIndexableByPropertyValue( "zibre" );
                secondID = createNodeIndexableByPropertyValue( "zebrae" );
                thirdID = createNodeIndexableByPropertyValue( "zebra" );
                fourthID = createNodeIndexableByPropertyValue( "zibra" );

                tx.success();
            }

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertFuzzyQueryFindsIds( reader, "zebra", firstID, secondID, thirdID, fourthID );
            }
        }
    }

    @Test
    public void shouldNotReturnNonMatches() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            fulltextFactory.createFulltextIndex( "relationships", RELATIONSHIPS, singletonList( "prop" ) );
            provider.init();

            try ( Transaction tx = db.beginTx() )
            {
                long firstNode = createNodeIndexableByPropertyValue( "Hello. Hello again." );
                long secondNode = createNodeWithProperty( "prop2",
                        "A zebroid (also zedonk, zorse, zebra mule, zonkey, and zebmule) is the offspring of any " +
                        "cross between a zebra and any other equine: essentially, a zebra hybrid." );
                createRelationshipIndexableByPropertyValue( firstNode, secondNode, "Hello. Hello again." );
                createRelationshipWithProperty( secondNode, firstNode, "prop2",
                        "A zebroid (also zedonk, zorse, zebra mule, zonkey, and zebmule) is the offspring of any " +
                        "cross between a zebra and any other equine: essentially, a zebra hybrid." );

                tx.success();
            }
            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsNothing( reader, "zebra" );
            }
            try ( ReadOnlyFulltext reader = provider.getReader( "relationships", RELATIONSHIPS ) )
            {
                assertExactQueryFindsNothing( reader, "zebra" );
            }
        }
    }

    @Test
    public void shouldPopulateIndexWithExistingNodesAndRelationships() throws Exception
    {
        long firstNodeID;
        long secondNodeID;
        long firstRelID;
        long secondRelID;
        try ( Transaction tx = db.beginTx() )
        {
            // skip a few rel ids, so the ones we work with are different from the node ids, just in case.
            Node node = db.createNode();
            node.createRelationshipTo( node, RELTYPE );
            node.createRelationshipTo( node, RELTYPE );
            node.createRelationshipTo( node, RELTYPE );

            firstNodeID = createNodeIndexableByPropertyValue( "Hello. Hello again." );
            secondNodeID = createNodeIndexableByPropertyValue( "This string is slightly shorter than the zebra one" );
            firstRelID = createRelationshipIndexableByPropertyValue( firstNodeID, secondNodeID, "Goodbye" );
            secondRelID = createRelationshipIndexableByPropertyValue( secondNodeID, firstNodeID,
                    "And now, something completely different" );

            tx.success();
        }

        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            fulltextFactory.createFulltextIndex( "relationships", RELATIONSHIPS, singletonList( "prop" ) );
            provider.init();
            provider.awaitPopulation();

            try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
            {
                assertExactQueryFindsIds( reader, "hello", firstNodeID );
                assertExactQueryFindsIds( reader, "string", secondNodeID );
                assertExactQueryFindsNothing( reader, "goodbye" );
                assertExactQueryFindsNothing( reader, "different" );
            }
            try ( ReadOnlyFulltext reader = provider.getReader( "relationships", RELATIONSHIPS ) )
            {
                assertExactQueryFindsNothing( reader, "hello" );
                assertExactQueryFindsNothing( reader, "string" );
                assertExactQueryFindsIds( reader, "goodbye", firstRelID );
                assertExactQueryFindsIds( reader, "different", secondRelID );
            }
        }
    }

    @Test
    public void shouldReturnMatchesThatContainLuceneSyntaxCharacters() throws Exception
    {
        try ( FulltextProvider provider = createProvider() )
        {
            fulltextFactory.createFulltextIndex( "nodes", NODES, singletonList( "prop" ) );
            provider.init();
            String[] luceneSyntaxElements =
                    {"+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "\\"};

            long nodeId;
            try ( Transaction tx = db.beginTx() )
            {
                nodeId = db.createNodeId();
                tx.success();
            }

            for ( String elm : luceneSyntaxElements )
            {
                setNodeProp( nodeId, "Hello" + elm + " How are you " + elm + "today?" );

                try ( ReadOnlyFulltext reader = provider.getReader( "nodes", NODES ) )
                {
                    assertExactQueryFindsIds( reader, "Hello" + elm, nodeId );
                    assertExactQueryFindsIds( reader, elm + "today", nodeId );
                }
            }
        }
    }
}
