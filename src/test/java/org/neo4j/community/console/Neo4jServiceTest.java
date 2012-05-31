package org.neo4j.community.console;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.entity.RestNode;

import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 30.05.12
 */
@Ignore
public class Neo4jServiceTest {

    private Node referenceNode;
    private Neo4jService service;
    private ConsoleService consoleService;

    @Test
    public void testInitFromUrl() throws Exception {
        consoleService.initFromUrl(service, new URL("http://localhost:7474/db/data/cypher"), "start n=node(0) return n");
        assertEquals("root", referenceNode.getProperty("name"));
    }
    @Test
    public void testInitFromUrl2() throws Exception {
        consoleService.initFromUrl2(service,new URL("http://localhost:7474/db/data/cypher"), "start n=node(0) return n");
        assertEquals("root", referenceNode.getProperty("name"));
    }

    @Before
    public void setUp() {
        consoleService = new ConsoleService();
        service = new Neo4jService();
        final RestAPI api = new RestAPI("http://localhost:7474/db/data");
        final Node restRefNode = api.getReferenceNode();
        restRefNode.setProperty("name","root");

        final GraphDatabaseService gdb = service.getGraphDatabase();
        gdb.beginTx();
        referenceNode = gdb.getReferenceNode();
        referenceNode.removeProperty("name");
    }
}
