package org.neo4j.community.console;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.NeoServer;
import org.neo4j.server.WrappingNeoServer;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.test.ImpermanentGraphDatabase;

/**
 * @author mh
 * @since 30.05.12
 */
public class ImportRemoteGraphTest {

    private Node referenceNode;
    private Neo4jService service;
    private ConsoleService consoleService;
    private static ImpermanentGraphDatabase serverGraphDatabase;
    private static NeoServer webServer;
    private static final int PORT = 7475;
    private static final String CYPHER_URL = "http://localhost:"+PORT+"/db/data/cypher";


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

    public static NeoServer startWebServer(
			ImpermanentGraphDatabase gdb, int port) {
    	Configurator configurator = new ServerConfigurator(gdb);
    	configurator.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY,	port);
		NeoServer server = new WrappingNeoServer(gdb, configurator );
		server.start();
		return server;
	}
	@AfterClass
    public static void stop() throws Exception {
        webServer.stop();
        serverGraphDatabase.shutdown();
    }

    @Before
    public void setUp() {
        serverGraphDatabase.cleanContent(true);
        final Transaction tx = serverGraphDatabase.beginTx();
        final Node remoteRefNode = serverGraphDatabase.getReferenceNode();
        remoteRefNode.setProperty("name", "root");
        tx.success();tx.finish();

        consoleService = new ConsoleService();
        service = new Neo4jService();

        final GraphDatabaseService localGraphDatabase = service.getGraphDatabase();
        localGraphDatabase.beginTx();
        referenceNode = localGraphDatabase.getReferenceNode();
        referenceNode.removeProperty("name");
    }
}
