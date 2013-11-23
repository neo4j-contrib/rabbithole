package org.neo4j.community.console;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 27.05.12
 */
public class ExportTest {

    private GraphDatabaseService gdb;
    private Node node;
    private Transaction tx;

    @Before
    public void setUp() throws Exception {
        gdb =  new TestGraphDatabaseFactory().newImpermanentDatabase();
        tx = gdb.beginTx();
        node = gdb.createNode();
    }

    @After
    public void tearDown() throws Exception {
        tx.success();tx.close();
        gdb.shutdown();
    }

    @Test
    public void testSimpleToYuml() throws Exception {
        final String res = new YumlExport().toYuml(SubGraph.from(gdb));
        assertEquals("["+ node.getId()+"],", res);
    }
    @Test
    public void testPropsToYuml() throws Exception {
        node.setProperty("name", "root");
        final String res = new YumlExport().toYuml(SubGraph.from(gdb));
        System.out.println("res = " + res);
        assertEquals("["+node.getId()+"|name root;],", res);
    }
    @Test
    public void testNamedGraphToYuml() throws Exception {
        node.setProperty("name", "root");
        final Node n1 = gdb.createNode();
        n1.setProperty("name", "Peter");
        node.createRelationshipTo(n1, DynamicRelationshipType.withName("PERSON"));
        final Node n2 = gdb.createNode();
        n2.setProperty("name", "Andreas");
        node.createRelationshipTo(n2, DynamicRelationshipType.withName("PERSON"));
        final Node n3 = gdb.createNode();
        n3.setProperty("name","Michael");
        node.createRelationshipTo(n3, DynamicRelationshipType.withName("PERSON"));
        n1.createRelationshipTo(n2, DynamicRelationshipType.withName("FRIEND"));
        n3.createRelationshipTo(n1, DynamicRelationshipType.withName("FRIEND"));
        final String res = new YumlExport().toYuml(SubGraph.from(gdb), "name");
        System.out.println("res = " + res);
        assertEquals(true, res.contains("[Peter]FRIEND->[Andreas],"));
        assertEquals(true,res.contains("[root],[Peter],[Andreas],[Michael],"));
    }
    @Test
    public void testIdPropsToYuml() throws Exception {
        node.setProperty("name", "root");
        final String res = new YumlExport().toYuml(SubGraph.from(gdb), "name");
        System.out.println("res = " + res);
        assertEquals("[root],", res);
    }
    @Test
    public void testGraphToYuml() throws Exception {
        final Node n1 = gdb.createNode();
        node.createRelationshipTo(n1, DynamicRelationshipType.withName("REL"));
        final String res = new YumlExport().toYuml(SubGraph.from(gdb));
        assertEquals("["+node.getId()+"],["+n1.getId()+"],["+node.getId()+"]REL->["+n1.getId()+"],", res);
    }
}
