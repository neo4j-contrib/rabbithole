package org.neo4j.community.console;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.cypher.SyntaxException;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.test.ImpermanentGraphDatabase;

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

    private ImpermanentGraphDatabase gdb;
    private CypherQueryExecutor cypherQueryExecutor;

    @Before
    public void setUp() throws Exception {
        gdb = new ImpermanentGraphDatabase();
        cypherQueryExecutor = new CypherQueryExecutor(gdb, new Index(gdb));
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
    public void testAdhereToCypherVersion16() throws Exception {
        cypherQueryExecutor.cypherQuery("start n=node(1) match n-[:A|B]-() return n","1.6");
    }
    @Test(expected = SyntaxException.class)
    public void testAdhereToCypherVersion17() throws Exception {
        cypherQueryExecutor.cypherQuery("create (n {})","1.7");
    }
    @Test
    public void testAdhereToCypherVersion18() throws Exception {
        cypherQueryExecutor.cypherQuery("create (n {})","1.8");
    }
    @Test
    public void testAdhereToNoCypherVersion() throws Exception {
        cypherQueryExecutor.cypherQuery("create (n {})",null);
    }

    @Test
    public void testCypherQuery() throws Exception {
        final CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("start n = node(0) return n",null);
        assertEquals(asList("n"), result.getColumns());
        assertTrue(result.getText().contains("Node[0]"));
        for (Map<String, Object> row : result) {
            assertEquals(1,row.size());
            assertEquals(true,row.containsKey("n"));
            assertEquals(gdb.getReferenceNode(),row.get("n"));
        }
    }

    @Test
    public void testToJson() throws Exception {
        gdb.beginTx();
        final Node n1 = gdb.createNode();
        n1.setProperty("name","n1");
        n1.setProperty("age",10);
        final Relationship rel = gdb.getReferenceNode().createRelationshipTo(n1, DynamicRelationshipType.withName("REL"));
        rel.setProperty("name","rel1");
        final CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("start n=node(0) match p=n-[r]->m return p,n,r,m", null);
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
}
