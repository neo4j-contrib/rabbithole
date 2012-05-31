package org.neo4j.community.console;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author mh
 * @since 30.05.12
 */
public class GraphStorageTest {

    private GraphStorage storage;
    private RestGraphDatabase gdb;
    private RestIndex<Node> index;
    private RestCypherQueryEngine cypher;

    @Before
    public void setUp() throws Exception {
        storage = new GraphStorage("http://localhost:7474/db/data");
        gdb = new RestGraphDatabase("http://localhost:7474/db/data");
        cypher = new RestCypherQueryEngine(gdb.getRestAPI());
        cypher.query("start n=node(*) match n-[r?]-() where id(n) <> 0 delete n,r",null);
        index = gdb.index().forNodes("graphs");
    }

    @Test
    public void testUpdate() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(Util.randomId(), "init", "query", "message"));
        final GraphInfo info2 = info.newQuery("query2");
        storage.update(info2);
        final Node node = index.get("id", info.getId()).getSingle();
        assertNotNull(node);
        assertEquals("query2", node.getProperty("query"));
        assertEquals(info.getId(), node.getProperty("id"));
        assertEquals(info.getInit(), node.getProperty("init"));
        assertEquals(info.getMessage(),node.getProperty("message"));
        node.delete();
        assertNull(storage.find(info.getId()));
    }

    @Test
    public void testCreate() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo("id", "init", "query", "message"));
        assertNotNull(info);
        final Node node = index.get("id", info.getId()).getSingle();
        assertNotNull(node);
        assertEquals("query", node.getProperty("query"));
        node.delete();
    }
    @Test
    public void testDelete() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo("id", "init", "query", "message"));
        storage.delete(info.getId());
        final Node node = index.get("id", info.getId()).getSingle();
        assertNull(node);
    }
}
