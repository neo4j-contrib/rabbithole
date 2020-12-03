package org.neo4j.community.console;

import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * @author mh
 * @since 22.04.12
 */
public class CypherQueryExecutorTest {

    private GraphDatabaseService gdb;
    private CypherQueryExecutor cypherQueryExecutor;
    private Transaction tx;
    private Node rootNode;

    @Before
    public void setUp() throws Exception {
        gdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        tx = gdb.beginTx();
        rootNode = gdb.createNode();
        cypherQueryExecutor = new CypherQueryExecutor(gdb);
    }

    @After
    public void tearDown() throws Exception {
        tx.failure();tx.close();
        gdb.shutdown();
    }

    @Test
    public void testIsMutatingQuery() throws Exception {
        assertFalse(cypherQueryExecutor.isMutatingQuery(""));
        assertFalse(cypherQueryExecutor.isMutatingQuery("start n = node(1) return n"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("start n = node(1) create (m { name: 'Andres'})"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("start n = node(1) create n-[:KNOWS]->n"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("start n = node(1) delete n"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("start n = node(1) set n.name = 'Andres'"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("MERGE (n:Person {name:'Andres'})"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("MATCH (n:Person {name:'Andres'}) DELETE n"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("MATCH (n:Person {name:'Andres'}) DETACH DELETE n"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("MATCH (n:Person {name:'Andres'}) SET n:Foo"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("MATCH (n:Person {name:'Andres'}) REMOVE n:Foo"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("MATCH (n:Person {name:'Andres'}) REMOVE n.foo"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("CREATE (n:Person {name:'Andres'})"));
    }

    @Test
    public void testIgnoreTrailingSemicolon() throws Exception
    {
        cypherQueryExecutor.cypherQuery( "create (n {});\n " ,null);
    }

    @Test(expected = QueryExecutionException.class)
    public void testAdhereToCypherVersion16() throws Exception {
        cypherQueryExecutor.cypherQuery("match (n) where id(n) = (1) match n-[:A|B]-() return n","1.6");
    }

    @Test(expected = QueryExecutionException.class)
    public void testAdhereToCypherVersion17() throws Exception {
        cypherQueryExecutor.cypherQuery("create (n {})","1.7");
    }

    @Test(expected = QueryExecutionException.class)
    public void testAdhereToCypherVersion18() throws Exception {
        cypherQueryExecutor.cypherQuery("create (n {})","1.8");
    }

    @Test(expected = QueryExecutionException.class)
    public void testAdhereToCypherVersion20() throws Exception {
        cypherQueryExecutor.cypherQuery("cypher 2.0 create (n:Label {name:'Foo'})","2.0");
    }

    @Test(expected = QueryExecutionException.class)
    public void testAdhereToCypherVersion21() throws Exception {
        cypherQueryExecutor.cypherQuery("cypher 2.1 create (n:Label {name:'Foo'})","2.1");
    }


    @Test
    public void testAdhereToCypherVersion23() throws Exception {
        cypherQueryExecutor.cypherQuery("cypher 2.3 match (n:Label) where n.name starts with 'Foo' return n","2.3");
        cypherQueryExecutor.cypherQuery("cypher 2.3 match (n:Label) return n['name']","2.3");
        cypherQueryExecutor.cypherQuery("cypher 2.3 match (n:Label) detach delete n","2.3");
    }

    @Test
    public void testAdhereToCypherVersion30() throws Exception {
        cypherQueryExecutor.cypherQuery("cypher 2.3 match (n:Label) where n.name starts with 'Foo' return n","3.0");
    }
    @Test
    public void testAdhereToCypherVersion23_1() throws Exception {
        cypherQueryExecutor.cypherQuery("cypher 2.3 planner=cost match (n:Label {name:'Foo'}) return n","2.3 planner cost");
        cypherQueryExecutor.cypherQuery("cypher 2.3 planner=rule create (n:Label {name:'Foo'})","2.3 planner rule");
    }
    @Test
    public void testAdhereToNoCypherVersion() throws Exception {
        cypherQueryExecutor.cypherQuery("create (n:Foo {})",null);
    }

    @Test
    public void testWorksWithMerge() throws Exception {
        final CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("merge (n {name:'foobar'}) return n.name", null);
        assertEquals(1,result.getRowCount());
        final Object value = result.getRows().iterator().next().get("n.name");
        assertEquals("foobar", value);
    }

    @Test
    public void testWorksWithCypherPrefix() throws Exception {
        CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("cypher 2.3 match (n) return count(*) as cnt", "cypher 2.0");
        assertEquals(asList("cnt"),result.getColumns());
        assertEquals(1, result.getRowCount());
    }

    @Test
    @Ignore
    public void testPrettifyQuery() throws Exception {
        final String pretty = cypherQueryExecutor.prettify("match (n) where id(n) = (1) match n--> () return n");
        final String lineSeparator =(String)  System.getProperties().get("line.separator");
        assertEquals("MATCH (n)"+lineSeparator+
                " WHERE id(n)=(1)" +lineSeparator+
                " MATCH n-->()" +lineSeparator+
                " RETURN n",pretty);
    }

    @Test
    public void testCypherQuery() throws Exception {
        final CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("match (n) where id(n) = ("+rootNode.getId()+") return n",null);
        assertEquals(asList("n"), result.getColumns());
        assertTrue(result.getText(),result.getText().contains("Node[0]"));
        for (Map<String, Object> row : result) {
            assertEquals(1,row.size());
            assertEquals(true,row.containsKey("n"));
            assertEquals(rootNode,row.get("n"));
        }
    }

    @Test
    public void testToJson() throws Exception {
        gdb.beginTx();
        final Node n1 = gdb.createNode();
        n1.setProperty("name","n1");
        n1.setProperty("age",10);
        final Relationship rel = rootNode.createRelationshipTo(n1, DynamicRelationshipType.withName("REL"));
        rel.setProperty("name","rel1");
        final CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("match (n) where id(n) = ("+rootNode.getId()+") match p=(n)-[r]->(m) return p,n,r,m", null);
        System.out.println(result);
        final List<Map<String,Object>> json = result.getJson();
        System.out.println(new Gson().toJson(json));
        assertEquals(1, json.size());
        final Map<String, Object> row = json.get(0);
        assertEquals(4, row.size());
        final Map node1 = (Map) row.get("n");
        assertEquals(1, node1.size());
        assertEquals(0L, node1.get("_id"));
        final Map node2 = (Map) row.get("m");
        assertEquals(3, node2.size());
        assertEquals(1L, node2.get("_id"));
        assertEquals("n1", node2.get("name"));
        assertEquals(10, node2.get("age"));

        final Map rel1 = (Map) row.get("r");
        assertEquals(5, rel1.size());
        assertEquals(0L, rel1.get("_id"));
        assertEquals("rel1", rel1.get("name"));
        assertEquals("REL", rel1.get("_type"));
        assertEquals(0L, rel1.get("_start"));
        assertEquals(1L, rel1.get("_end"));

        final List path = (List) row.get("p");
        assertEquals(3, path.size());
        final Map pathNode1 = (Map) path.get(0);
        assertEquals(0L, pathNode1.get("_id"));
        final Map pathRel1 = (Map) path.get(1);
        assertEquals("rel1", pathRel1.get("name"));
        final Map pathNode2 = (Map) path.get(2);
        assertEquals(10, pathNode2.get("age"));
    }

    @Test
    public void testUseParameters() throws Exception {
        CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("MATCH (n) WHERE id(n) = {id} RETURN count(*)",null, map("id",rootNode.getId()));
        assertEquals(1,result.getRowCount());
    }

    @Test
    public void testDontProfileUnionCheck() throws Exception {
        assertTrue(cypherQueryExecutor.canProfileQuery("match (n) where id(n) = (*) return n UNION match (n) where id(n) = (*) return n"));
        assertTrue(cypherQueryExecutor.canProfileQuery("match (n) where id(n) = (*) return n \nUNION\n match (n) where id(n) = (*) return n"));
        assertTrue(cypherQueryExecutor.canProfileQuery("match (n) where id(n) = (*) return n \nunion\n match (n) where id(n) = (*) return n"));
        assertTrue(cypherQueryExecutor.canProfileQuery("match (n) where id(n) = (*) return n"));
    }

    @Test
    public void testDontProfileUnion() throws Exception {
        CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("match (n) return n UNION match (n) return n", null);
        assertEquals(1,result.getRowCount());
    }
    @Test
    public void testHandlePeriodicCommit() throws Exception {
        String query = "USING PERIODIC COMMIT\n" +
                "LOAD CSV WITH HEADERS FROM 'http://docs.neo4j.org/chunked/stable/csv/import/roles.csv' AS csvLine\n" +
                "CREATE (p:Person { id: csvLine.personId})\n" +
                "RETURN p";
        CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery(query, null);
        assertEquals(6,result.getRowCount());
    }

}
