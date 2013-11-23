package org.neo4j.community.console;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.rest.graphdb.ExecutingRestRequest;
import org.neo4j.rest.graphdb.RequestResult;
import org.neo4j.server.NeoServer;
import org.neo4j.server.WrappingNeoServer;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

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
    public void testInitFromUrl() throws Exception {
        consoleService.initFromUrl(service, new URL(CYPHER_URL), "start n=node(" + remoteNode.getId() + ") return n");
        checkImportedNode(NAME, VALUE);
    }

    private void checkImportedNode(String prop, String value) {
        GraphDatabaseService gdb = service.getGraphDatabase();
        try (Transaction tx = gdb.beginTx()) {
            Node imported = IteratorUtil.single(GlobalGraphOperations.at(gdb).getAllNodes());
            assertEquals(value, imported.getProperty(prop));
            tx.success();
        }
    }

    @Test
    public void testInitFromUrl2() throws Exception {
        consoleService.initFromUrl2(service,new URL(CYPHER_URL), "start n=node("+ remoteNode.getId()+") return n");
        checkImportedNode(NAME, VALUE);
    }


    @BeforeClass
    public static void startup() {
        serverGraphDatabase = (ImpermanentGraphDatabase) new TestGraphDatabaseFactory().newImpermanentDatabase();
        webServer = startWebServer(serverGraphDatabase, PORT);
    }

    public static NeoServer startWebServer(
			ImpermanentGraphDatabase gdb, int port) {
    	Configurator configurator = new ServerConfigurator(gdb);
    	configurator.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY,	port);
		NeoServer server = new WrappingNeoServer(gdb, configurator );
		server.start();
        tryConnect();
		return server;
	}

    private static void tryConnect() {
        int retryCount = 3;
        for (int i = 0; i < retryCount; i++) {
            try {
                RequestResult result = new ExecutingRestRequest(SERVER_ROOT_URI).get("");
                assertEquals(200, result.getStatus());
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
