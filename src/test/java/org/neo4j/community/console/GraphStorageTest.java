package org.neo4j.community.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import org.junit.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.server.NeoServer;
import org.neo4j.test.ImpermanentGraphDatabase;

/**
 * @author mh
 * @since 30.05.12
 */
public class GraphStorageTest {

    private static final int PORT = 7475;
    private GraphStorage storage;
    private static ImpermanentGraphDatabase gdb;
    private Index<Node> index;
    private static NeoServer webServer;

    @BeforeClass
    public static void startup() {
        gdb = new ImpermanentGraphDatabase();
        webServer = ImportRemoteGraphTest.startWebServer(gdb, PORT);
    }
    @Before
    public void setUp() throws Exception {
        gdb.cleanContent(true);
        storage = new GraphStorage("http://localhost:"+PORT+"/db/data");
        Transaction tx = gdb.beginTx();
        index = gdb.index().forNodes("graphs");
        tx.success(); tx.finish();
    }

    @AfterClass
    public static void stop() throws Exception {
        webServer.stop();
        gdb.shutdown();
    }

    @Test
    public void testUpdate() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(Util.randomId(), "init", "query", "message"));
        final GraphInfo info2 = info.newQuery("query2");
        storage.update(info2);
        try (Transaction tx = gdb.beginTx()) {
            final Node node = index.get("id", info.getId()).getSingle();
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
        final Node node = index.get("id", info.getId()).getSingle();
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
        assertEquals(info.getVersion(),node.getProperty("version"));
        tx.success();tx.finish();
    }

    @Test
    public void testCreateWithNoRoot() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(Util.randomId(), "init", "query", "message","version",true));
        Transaction tx = gdb.beginTx();
        final Node node = index.get("id", info.getId()).getSingle();
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
        assertEquals(info.getVersion(),node.getProperty("version"));
        assertEquals(!info.hasRoot(),(Boolean)node.getProperty("no_root"));
        tx.success();tx.finish();
    }

    @Test
    public void testCreateWithNullId() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(null, "init", "query", "message"));
        Transaction tx = gdb.beginTx();
        assertNotNull(info.getId());
        final Node node = index.get("id", info.getId()).getSingle();
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
        tx.success();tx.finish();
    }

    @Test
    public void testCreateWithEmptyId() throws Exception {
        final String id = " ";
        final GraphInfo info = storage.create(new GraphInfo(id, "init", "query", "message"));
        Transaction tx = gdb.beginTx();
        assertNotNull(info.getId());
        assertNotSame(id,info.getId());
        assertFalse(info.getId().trim().isEmpty());
        final Node node = index.get("id", info.getId()).getSingle();
        assertNull(index.get("id", id).getSingle());
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
        tx.success();tx.finish();
    }

    @Test
    public void testCreateWithIdWithSpace() throws Exception {
        final String id = "";
        final GraphInfo info = storage.create(new GraphInfo(id, "init", "query", "message"));
        Transaction tx = gdb.beginTx();
        assertNotNull(info.getId());
        assertNotSame(id,info.getId());
        assertFalse(info.getId().trim().isEmpty());
        final Node node = index.get("id", info.getId()).getSingle();
        assertNull(index.get("id", id).getSingle());
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
        tx.success();tx.finish();
    }

    private void delete(Node node) {
        final Transaction tx = gdb.beginTx();
        node.delete();
        tx.success();tx.finish();
    }

    @Test
    public void testCreate() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo("id", "init", "query", "message"));
        try (Transaction tx2 = gdb.beginTx()) {
            assertNotNull(info);
            final Node node = index.get("id", info.getId()).getSingle();
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
            final Node node = index.get("id", info.getId()).getSingle();
            assertNull(node);
            tx.success();
        }
    }
}
