package org.neo4j.community.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.index.RelationshipAutoIndexer;
import org.neo4j.test.ImpermanentGraphDatabase;

/**
 * @author mh
 * @since 20.04.12
 */
public class IndexTest {

    private Index index;
    private ImpermanentGraphDatabase gdb;
    private RelationshipAutoIndexer relationshipAutoIndexer;
    private AutoIndexer<Node> nodeAutoIndexer;

    @Before
    public void setUp() throws Exception {
        gdb = new ImpermanentGraphDatabase();
        index = new Index(gdb);
        relationshipAutoIndexer = gdb.index().getRelationshipAutoIndexer();
        nodeAutoIndexer = gdb.index().getNodeAutoIndexer();
    }

    @Test
    public void testEnableAutoIndexes() {
        assertEquals(true, nodeAutoIndexer.isEnabled());
        assertEquals(true,gdb.index().existsForNodes("node_auto_index"));
        assertEquals(true, relationshipAutoIndexer.isEnabled());
        assertEquals(true,gdb.index().existsForRelationships("relationship_auto_index"));
    }
    @Test
    public void testRegisterNullProperty() throws Exception {
        index.registerProperty(null);
        assertEquals(true,index.getAutoIndexedProperties().isEmpty());
        assertEquals(true,nodeAutoIndexer.getAutoIndexedProperties().isEmpty());
        assertEquals(true,relationshipAutoIndexer.getAutoIndexedProperties().isEmpty());

    }
    @Test
    public void testRegisterEmptyProperty() throws Exception {
        index.registerProperty(Collections.<String>emptySet());
        assertEquals(true, index.getAutoIndexedProperties().isEmpty());
        assertEquals(true,nodeAutoIndexer.getAutoIndexedProperties().isEmpty());
        assertEquals(true,relationshipAutoIndexer.getAutoIndexedProperties().isEmpty());
    }
    @Test
    public void testRegisterTwoProperties() throws Exception {
        index.registerProperty(Arrays.asList("foo", "bar"));
        assertThat(index.getAutoIndexedProperties(),hasItems("foo","bar"));
        assertThat(nodeAutoIndexer.getAutoIndexedProperties(),hasItems("foo","bar"));
        assertThat(relationshipAutoIndexer.getAutoIndexedProperties(),hasItems("foo","bar"));
    }
    @Test
    public void testRegisterOneProperty() throws Exception {
        index.registerProperty(Arrays.asList("foo"));
        assertThat(index.getAutoIndexedProperties(), hasItems("foo"));
        assertThat(nodeAutoIndexer.getAutoIndexedProperties(),hasItems("foo"));
        assertThat(relationshipAutoIndexer.getAutoIndexedProperties(),hasItems("foo"));
    }
}
