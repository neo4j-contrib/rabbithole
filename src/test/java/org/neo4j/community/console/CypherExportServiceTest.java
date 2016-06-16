package org.neo4j.community.console;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.junit.Assert.*;

/**
 * @author mh
 * @since 17.06.16
 */
public class CypherExportServiceTest {

    private GraphDatabaseService db;
    private Transaction tx;
    private CypherExportService exporter;

    @Before
    public void setUp() throws Exception {
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        tx = db.beginTx();
        exporter = new CypherExportService(db);
    }

    @After
    public void tearDown() throws Exception {
        if (tx != null) {
            tx.failure();
            tx.close();
        }
        if (db!=null) db.shutdown();
    }

    @Test
    public void exportNodeWithProperty() throws Exception {
        db.createNode().setProperty("foo",42);
        assertEquals("create \n(_0  {`foo`:42})",exporter.export());
    }
    @Test
    public void exportNodeWithLabel() throws Exception {
        db.createNode(DynamicLabel.label("Node")).setProperty("foo",42);
        assertEquals("create \n(_0:`Node`  {`foo`:42})",exporter.export());
    }
    @Test
    public void exportNodeWithRelationship() throws Exception {
        Node n = db.createNode();
        n.createRelationshipTo(n,DynamicRelationshipType.withName("X"));
        assertEquals("create \n(_0 ),\n(_0)-[:`X`]->(_0)",exporter.export());
    }

}
