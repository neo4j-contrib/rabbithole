package org.neo4j.community.console;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.internal.InProcessServerBuilder;

import static org.junit.Assert.*;

/**
 * @author mh
 * @since 30.05.12
 */
public class BoltGraphStorageTest {

    public static ServerControls neo4j = new InProcessServerBuilder().newServer();

    private GraphStorage storage;
    private static GraphDatabaseService gdb;

    @BeforeClass
    public static void before() {
        gdb = neo4j.graph();
    }

    @AfterClass
    public static void after() throws Exception {
        neo4j.close();
        gdb.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        gdb.execute("MATCH (n) DETACH DELETE n");
        storage = new BoltGraphStorage(neo4j.boltURI().toString(),null,null);
    }

    @Test
    public void testUpdate() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(Util.randomId(), "init", "query", "message"));
        final GraphInfo info2 = info.newQuery("query2");
        storage.update(info2);
        try (Transaction tx = gdb.beginTx()) {
            final Node node = gdb.findNode(Label.label("Graph"),"id", info.getId());
            assertNotNull(node);
            assertEquals("query2", node.getProperty("query"));
            assertEquals(info.getId(), node.getProperty("id"));
            assertEquals(info.getInit(), node.getProperty("init"));
            assertEquals(info.getMessage(),node.getProperty("message"));
            delete(node);
            tx.success();
        }

        try (Transaction tx2 = gdb.beginTx()) {
            assertNull(storage.find(info.getId()));
            tx2.success();
        }
    }
    @Test
    public void testCreateWithVersion() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(Util.randomId(), "init", "query", "message","version"));
        Transaction tx = gdb.beginTx();
        final Node node = gdb.findNode(Label.label("Graph"),"id", info.getId());
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
        assertEquals(info.getVersion(),node.getProperty("version"));
        tx.success();tx.close();
    }

    @Test
    public void testCreateWithNoRoot() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(Util.randomId(), "init", "query", "message","version",true));
        Transaction tx = gdb.beginTx();
        final Node node = gdb.findNode(Label.label("Graph"),"id", info.getId());
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
        assertEquals(info.getVersion(),node.getProperty("version"));
        assertEquals(!info.hasRoot(),(Boolean)node.getProperty("no_root"));
        tx.success();tx.close();
    }

    @Test
    public void testCreateWithNullId() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(null, "init", "query", "message"));
        Transaction tx = gdb.beginTx();
        assertNotNull(info.getId());
        final Node node = gdb.findNode(Label.label("Graph"),"id", info.getId());
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
        tx.success();tx.close();
    }

    @Test
    public void testCreateWithEmptyId() throws Exception {
        final String id = " ";
        final GraphInfo info = storage.create(new GraphInfo(id, "init", "query", "message"));
        Transaction tx = gdb.beginTx();
        assertNotNull(info.getId());
        assertNotSame(id,info.getId());
        assertFalse(info.getId().trim().isEmpty());
        final Node node = gdb.findNode(Label.label("Graph"),"id", info.getId());
        assertNull(gdb.findNode(Label.label("Graph"),"id", id));
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
        tx.success();tx.close();
    }

    @Test
    public void testCreateWithIdWithSpace() throws Exception {
        final String id = "";
        final GraphInfo info = storage.create(new GraphInfo(id, "init", "query", "message"));
        Transaction tx = gdb.beginTx();
        assertNotNull(info.getId());
        assertNotSame(id,info.getId());
        assertFalse(info.getId().trim().isEmpty());
        final Node node = gdb.findNode(Label.label("Graph"),"id", info.getId());

        assertNull(gdb.findNode(Label.label("Graph"),"id", id));
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
        tx.success();tx.close();
    }

    private void delete(Node node) {
        final Transaction tx = gdb.beginTx();
        node.delete();
        tx.success();tx.close();
    }

    @Test
    public void testCreate() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo("id", "init", "query", "message"));
        try (Transaction tx2 = gdb.beginTx()) {
            assertNotNull(info);
            final Node node = gdb.findNode(Label.label("Graph"),"id", info.getId());
            assertNotNull(node);
            assertEquals("query", node.getProperty("query"));
            tx2.success();
            delete(node);
        }
    }
    @Test
    public void testDelete() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo("id", "init", "query", "message"));
        storage.delete(info.getId());
        try (Transaction tx = gdb.beginTx()) {
            final Node node = gdb.findNode(Label.label("Graph"),"id", info.getId());
            assertNull(node);
            tx.success();
        }
    }
}
