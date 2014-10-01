package org.neo4j.community.console;

import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Neo4jServiceTest {

    private Neo4jService neo4jService;
    private GraphDatabaseService gdb;

    @Before
    public void setUp() throws Throwable {
        gdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        neo4jService = new Neo4jService(gdb);
    }

    @Test
    public void testMergeGeoff() throws Exception {
        final Map map = neo4jService.mergeGeoff("(0) {\"foo\":\"bar\"}");
        assertEquals(MapUtil.map("foo", "bar"),map.get("(0)"));
        assertEquals("{\"(0)\":{\"foo\":\"bar\"}}",new Gson().toJson(map));
    }
    @Test
    public void testQueryCypher() throws Exception {
        try (Transaction tx = gdb.beginTx()) {
            final CypherQueryExecutor.CypherResult newNode = neo4jService.initCypherQuery("CREATE (n:Person {name:{name}}) RETURN id(n) as id", MapUtil.map("name", "Anders"));
            long id = ((Number)newNode.getRows().iterator().next().get("id")).longValue();
            final CypherQueryExecutor.CypherResult map = neo4jService.cypherQuery("MATCH (n) WHERE id(n) = {id} RETURN n.name", MapUtil.map("id", id));
            assertEquals(1, map.getRowCount());
        }
    }

    @After
    public void tearDown() throws Exception {
        neo4jService.stop();
        gdb.shutdown();
    }
}
