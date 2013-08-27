package org.neo4j.community.console;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.test.ImpermanentGraphDatabase;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 27.05.12
 */
public class ExportTest {

    private ImpermanentGraphDatabase gdb;

    @Before
    public void setUp() throws Exception {
        gdb = new ImpermanentGraphDatabase();
        gdb.beginTx();
    }

    @After
    public void tearDown() throws Exception {
        gdb.shutdown();
    }

    @Test
    public void testSimpleToYuml() throws Exception {
        final String res = new YumlExport().toYuml(SubGraph.from(gdb));
        assertEquals("[0],", res);
    }
    @Test
    public void testPropsToYuml() throws Exception {
        gdb.beginTx();
        gdb.getReferenceNode().setProperty("name","root");
        final String res = new YumlExport().toYuml(SubGraph.from(gdb));
        System.out.println("res = " + res);
        assertEquals("[0|name root;],", res);
    }
    @Test
    public void testNamedGraphToYuml() throws Exception {
        gdb.beginTx();
        gdb.getReferenceNode().setProperty("name","root");
        final Node n1 = gdb.createNode();
        n1.setProperty("name", "Peter");
        gdb.getReferenceNode().createRelationshipTo(n1,DynamicRelationshipType.withName("PERSON"));
        final Node n2 = gdb.createNode();
        n2.setProperty("name", "Andreas");
        gdb.getReferenceNode().createRelationshipTo(n2,DynamicRelationshipType.withName("PERSON"));
        final Node n3 = gdb.createNode();
        n3.setProperty("name","Michael");
        gdb.getReferenceNode().createRelationshipTo(n3, DynamicRelationshipType.withName("PERSON"));
        n1.createRelationshipTo(n2, DynamicRelationshipType.withName("FRIEND"));
        n3.createRelationshipTo(n1, DynamicRelationshipType.withName("FRIEND"));
        final String res = new YumlExport().toYuml(SubGraph.from(gdb), "name");
        System.out.println("res = " + res);
        assertEquals(true, res.contains("[Peter]FRIEND->[Andreas],"));
        assertEquals(true,res.contains("[root],[Peter],[Andreas],[Michael],"));
    }
    @Test
    public void testIdPropsToYuml() throws Exception {
        gdb.beginTx();
        gdb.getReferenceNode().setProperty("name","root");
        final String res = new YumlExport().toYuml(SubGraph.from(gdb), "name");
        System.out.println("res = " + res);
        assertEquals("[root],", res);
    }
    @Test
    public void testGraphToYuml() throws Exception {
        gdb.beginTx();
        final Node n1 = gdb.createNode();
        gdb.getReferenceNode().createRelationshipTo(n1, DynamicRelationshipType.withName("REL"));
        final String res = new YumlExport().toYuml(SubGraph.from(gdb));
        assertEquals("[0],[1],[0]REL->[1],", res);
    }
}
