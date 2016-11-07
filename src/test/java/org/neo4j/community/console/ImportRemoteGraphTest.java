package org.neo4j.community.console;

import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.helpers.ListenSocketAddress;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.server.NeoServer;
import org.neo4j.server.helpers.CommunityServerBuilder;
import org.neo4j.test.ImpermanentGraphDatabase;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 30.05.12
 */
public class ImportRemoteGraphTest {

    public static final String VALUE = "root";
    public static final String NAME = "name";
    private Neo4jService service;
    private ConsoleService consoleService;
    private static ImpermanentGraphDatabase serverGraphDatabase;
    private static NeoServer webServer;
    private static final int PORT = 7475;
    private static final String SERVER_ROOT_URI = "http://localhost:" + PORT;
    private static final String CYPHER_URL = SERVER_ROOT_URI + "/db/data/cypher";
    private Node remoteNode;


    @Test
    @Ignore
    public void testInitFromUrl() throws Exception {
        consoleService.initFromUrl(service, new URL(CYPHER_URL), "match (n) where id(n) = (" + remoteNode.getId() + ") return n");
        checkImportedNode(NAME, VALUE);
    }

    private void checkImportedNode(String prop, String value) {
        GraphDatabaseService gdb = service.getGraphDatabase();
        try (Transaction tx = gdb.beginTx()) {
            Node imported = Iterables.single(gdb.getAllNodes());
            assertEquals(value, imported.getProperty(prop));
            tx.success();
        }
    }

    @Test
    public void testInitFromUrl2() throws Exception {
        consoleService.initFromUrl2(service,new URL(CYPHER_URL), "match (n) where id(n) = ("+ remoteNode.getId()+") return n");
        checkImportedNode(NAME, VALUE);
    }


    @BeforeClass
    public static void startup() throws IOException {
        webServer = CommunityServerBuilder.server().onAddress(new ListenSocketAddress("localhost",PORT)).build();
        webServer.start();
        serverGraphDatabase = (ImpermanentGraphDatabase) webServer.getDatabase().getGraph();

        tryConnect();
    }

    private static void tryConnect() {
        int retryCount = 3;
        for (int i = 0; i < retryCount; i++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(SERVER_ROOT_URI).openConnection();
                conn.connect();
                int status = conn.getResponseCode();
                assertEquals(200, status);
                System.err.println("Successful HTTP connection to "+SERVER_ROOT_URI);
                return;
            } catch (Exception e) {
                System.err.println("Error retrieving ROOT URI " + e.getMessage());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) { }
            }
        }
    }

	@AfterClass
    public static void stop() throws Exception {
        webServer.stop();
        serverGraphDatabase.shutdown();
    }

    @Before
    public void setUp() throws Throwable {
        serverGraphDatabase.cleanContent();
        final Transaction tx = serverGraphDatabase.beginTx();
        remoteNode = serverGraphDatabase.createNode();
        remoteNode.setProperty(NAME, VALUE);
        tx.success();tx.close();

        consoleService = new ConsoleService();
        service = new Neo4jService();
    }
}
