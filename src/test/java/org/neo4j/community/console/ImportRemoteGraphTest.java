package org.neo4j.community.console;

import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.server.web.WebServer;
import org.neo4j.test.ImpermanentGraphDatabase;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.neo4j.community.console.TestWebServer.startWebServer;

/**
 * @author mh
 * @since 30.05.12
 */
public class ImportRemoteGraphTest {

    private static final String CYPHER_URL = "http://localhost:7474/db/data/cypher";
    private Node referenceNode;
    private Neo4jService service;
    private ConsoleService consoleService;
    private static ImpermanentGraphDatabase serverGraphDatabase;
    private static WebServer webServer;
    private static final int PORT = 7475;


    @Test
    public void testInitFromUrl() throws Exception {
        consoleService.initFromUrl(service, new URL(CYPHER_URL), "start n=node(0) return n");
        assertEquals("root", referenceNode.getProperty("name"));
    }
    @Test
    public void testInitFromUrl2() throws Exception {
        consoleService.initFromUrl2(service,new URL(CYPHER_URL), "start n=node(0) return n");
        assertEquals("root", referenceNode.getProperty("name"));
    }


    @BeforeClass
    public static void startup() {
        serverGraphDatabase = new ImpermanentGraphDatabase();
        webServer = startWebServer(serverGraphDatabase, PORT);
    }

    @AfterClass
    public static void stop() throws Exception {
        webServer.stop();
        serverGraphDatabase.shutdown();
    }

    @Before
    public void setUp() {
        serverGraphDatabase.cleanContent(true);
        serverGraphDatabase.beginTx();
        final Node remoteRefNode = serverGraphDatabase.getReferenceNode();
        remoteRefNode.setProperty("name", "root");

        consoleService = new ConsoleService();
        service = new Neo4jService();

        final GraphDatabaseService localGraphDatabase = service.getGraphDatabase();
        localGraphDatabase.beginTx();
        referenceNode = localGraphDatabase.getReferenceNode();
        referenceNode.removeProperty("name");
    }
}
