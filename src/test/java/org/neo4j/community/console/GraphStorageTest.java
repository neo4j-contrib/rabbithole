package org.neo4j.community.console;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.server.web.WebServer;
import org.neo4j.test.ImpermanentGraphDatabase;

import static org.junit.Assert.*;
import static org.neo4j.community.console.TestWebServer.startWebServer;

/**
 * @author mh
 * @since 30.05.12
 */
public class GraphStorageTest {

    private static final int PORT = 7475;
    private GraphStorage storage;
    private static ImpermanentGraphDatabase gdb;
    private Index<Node> index;
    private static WebServer webServer;

    @BeforeClass
    public static void startup() {
        gdb = new ImpermanentGraphDatabase();
        webServer = startWebServer(gdb, PORT);
    }
    @Before
    public void setUp() throws Exception {
        gdb.cleanContent(true);
        storage = new GraphStorage("http://localhost:"+PORT+"/db/data");
        index = gdb.index().forNodes("graphs");
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
        final Node node = index.get("id", info.getId()).getSingle();
        assertNotNull(node);
        assertEquals("query2", node.getProperty("query"));
        assertEquals(info.getId(), node.getProperty("id"));
        assertEquals(info.getInit(), node.getProperty("init"));
        assertEquals(info.getMessage(),node.getProperty("message"));
        delete(node);
        assertNull(storage.find(info.getId()));
    }
    @Test
    public void testCreateWithNullId() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(null, "init", "query", "message"));
        assertNotNull(info.getId());
        final Node node = index.get("id", info.getId()).getSingle();
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
    }

    @Test
    public void testCreateWithEmptyId() throws Exception {
        final String id = " ";
        final GraphInfo info = storage.create(new GraphInfo(id, "init", "query", "message"));
        assertNotNull(info.getId());
        assertNotSame(id,info.getId());
        assertFalse(info.getId().trim().isEmpty());
        final Node node = index.get("id", info.getId()).getSingle();
        assertNull(index.get("id", id).getSingle());
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
    }

    @Test
    public void testCreateWithIdWithSpace() throws Exception {
        final String id = "";
        final GraphInfo info = storage.create(new GraphInfo(id, "init", "query", "message"));
        assertNotNull(info.getId());
        assertNotSame(id,info.getId());
        assertFalse(info.getId().trim().isEmpty());
        final Node node = index.get("id", info.getId()).getSingle();
        assertNull(index.get("id", id).getSingle());
        assertNotNull(node);
        assertEquals(info.getId(), node.getProperty("id"));
    }

    private void delete(Node node) {
        final Transaction tx = gdb.beginTx();
        node.delete();
        tx.success();tx.finish();
    }

    @Test
    public void testCreate() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo("id", "init", "query", "message"));
        assertNotNull(info);
        final Node node = index.get("id", info.getId()).getSingle();
        assertNotNull(node);
        assertEquals("query", node.getProperty("query"));
        delete(node);
    }
    @Test
    public void testDelete() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo("id", "init", "query", "message"));
        storage.delete(info.getId());
        final Node node = index.get("id", info.getId()).getSingle();
        assertNull(node);
    }
}
