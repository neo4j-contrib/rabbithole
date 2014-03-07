package org.neo4j.community.console;

import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.cypher.SyntaxException;
import org.neo4j.graphdb.*;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

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
        cypherQueryExecutor = new CypherQueryExecutor(gdb, new Index(gdb));
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
    }

    @Test
    public void testExtractProperties() throws Exception {
        assertTrue(cypherQueryExecutor.extractProperties("").isEmpty());
        assertTrue(cypherQueryExecutor.extractProperties("start n = node(1) return n").isEmpty());
        assertThat(cypherQueryExecutor.extractProperties("start n = node(1) create (m { name: 'Andres'})"), hasItem("name"));
        assertThat(cypherQueryExecutor.extractProperties("start n = node(1) create n-[:KNOWS {name:'Friends', since : 2000}]->n"), hasItems("name", "since"));
        assertThat(cypherQueryExecutor.extractProperties("start n = node(1) create (m { name: 'Andres'}) set n.age = 19"), hasItems("name", "age"));
    }

    @Test
    public void testIgnoreTrailingSemicolon() throws Exception
    {
        cypherQueryExecutor.cypherQuery( "create (n {});\n " ,null);
    }

    @Test(expected = SyntaxException.class)
	@Ignore
    public void testAdhereToCypherVersion16() throws Exception {
        cypherQueryExecutor.cypherQuery("start n=node(1) match n-[:A|B]-() return n","1.6");
    }

    @Test(expected = SyntaxException.class)
    public void testAdhereToCypherVersion17() throws Exception {
        cypherQueryExecutor.cypherQuery("create (n {})","1.7");
    }

    @Test @Ignore("only 1.9 and 2.0")
    public void testAdhereToCypherVersion18() throws Exception {
        cypherQueryExecutor.cypherQuery("create (n {})","1.8");
    }
    @Test
    public void testAdhereToCypherVersion19() throws Exception {
        cypherQueryExecutor.cypherQuery("create (n {})","1.9");
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
        CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("cypher 2.0 start n=node(*) return count(*) as cnt", "cypher 2.0");
        assertEquals(asList("cnt"),result.getColumns());
        assertEquals(1, result.getRowCount());
    }

    @Test
    public void testPrettifyQuery() throws Exception {
        final String pretty = cypherQueryExecutor.prettify("start n=node(1) match n--> () return n");
        final String lineSeparator =(String)  System.getProperties().get("line.separator");
        assertEquals("START n=node(1)" +lineSeparator+
                " MATCH n-->()" +lineSeparator+
                " RETURN n",pretty);
    }

    @Test
    public void testCypherQuery() throws Exception {
        final CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("start n = node("+rootNode.getId()+") return n",null);
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
        final CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("start n=node("+rootNode.getId()+") match p=n-[r]->m return p,n,r,m", null);
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
    public void testReplaceIndex() throws Exception {
        String queryAuto="start n=node:node_auto_index(name='foo') return n;";
        assertEquals(queryAuto,cypherQueryExecutor.replaceIndex(queryAuto));
        String queryId="start n=node(3,4,5) return n;";
        assertEquals(queryId,cypherQueryExecutor.replaceIndex(queryId));
        String queryEmpty="start n=node:(name='foo') return n;";
        assertEquals(queryAuto,cypherQueryExecutor.replaceIndex(queryEmpty));
        String queryPeople="start n=node:people(name='foo') return n;";
        assertEquals(queryAuto,cypherQueryExecutor.replaceIndex(queryPeople));
        String queryPeople2="start n=node:`pe op-le`(name='foo') return n;";
        assertEquals(queryAuto,cypherQueryExecutor.replaceIndex(queryPeople2));
        String queryPeopleAndEmails="start n=node:people(name='foo'),m=node:emails(subject='foo') return n,m;";
        assertEquals("start n=node:node_auto_index(name='foo'),m=node:node_auto_index(subject='foo') return n,m;",cypherQueryExecutor.replaceIndex(queryPeopleAndEmails));
        String queryPeopleAndEmailsWith="start n=node:people(name='foo') with n start m=node:emails(subject='foo') return n,m;";
        assertEquals("start n=node:node_auto_index(name='foo') with n start m=node:node_auto_index(subject='foo') return n,m;",cypherQueryExecutor.replaceIndex(queryPeopleAndEmailsWith));

        String queryRelAuto="start r=relationship:relationship_auto_index(name='foo') return r;";
        String queryRels="start r=relationship:`lo v e s`(name='foo') return r;";
        assertEquals(queryRelAuto,cypherQueryExecutor.replaceIndex(queryRels));

    }

    @Test
    public void testDontProfileUnionCheck() throws Exception {
        assertFalse(cypherQueryExecutor.canProfileQuery("start n=node(*) return n UNION start n=node(*) return n"));
        assertFalse(cypherQueryExecutor.canProfileQuery("start n=node(*) return n \nUNION\n start n=node(*) return n"));
        assertFalse(cypherQueryExecutor.canProfileQuery("start n=node(*) return n \nunion\n start n=node(*) return n"));
        assertTrue(cypherQueryExecutor.canProfileQuery("start n=node(*) return n"));
    }

    @Test
    public void testDontProfileUnion() throws Exception {
        CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("start n=node(*) return n UNION start n=node(*) return n", null);
        assertEquals(1,result.getRowCount());
    }
    @Test
    public void testHandlePeriodicCommit() throws Exception {
        String query = "USING PERIODIC COMMIT\n" +
                "LOAD CSV WITH HEADERS FROM 'http://docs.neo4j.org/chunked/2.1-SNAPSHOT/csv/import/roles.csv' AS csvLine\n" +
                "CREATE (p:Person { id: csvLine.personId})\n" +
                "RETURN p";
        CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery(query, null);
        assertEquals(7,result.getRowCount());
    }
}
