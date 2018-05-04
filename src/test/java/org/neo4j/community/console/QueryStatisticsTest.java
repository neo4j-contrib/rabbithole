package org.neo4j.community.console;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author mh
 * @since 06.12.12
 */
public class QueryStatisticsTest {
    private GraphDatabaseService gdb;
    private CypherQueryExecutor cypherQueryExecutor;
    private Long nodeId;
    private Map<String,Object> params;

    @Before
    public void setUp() throws Exception {
        gdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        cypherQueryExecutor = new CypherQueryExecutor(gdb);
        nodeId = gdb.execute("CREATE (n) RETURN id(n) as id").<Long>columnAs("id").next();
        params = Collections.singletonMap("id", nodeId);
    }

    @After
    public void tearDown() throws Exception {
        gdb.shutdown();
    }

    @Test
    public void testNoUpdate() throws Exception {
        final CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("match (n) where id(n) = $id return n", null, params);
        final int rowCount = result.getRowCount();
        assertEquals(1, rowCount);
        final long time = result.getTime();
        assertTrue("time "+time, time < 2000);
        assertTrue("time "+time, time > 0);
        final Map stats = result.getQueryStatistics();
        assertEquals(2, stats.size());
        assertEquals(rowCount, stats.get("rows"));
        assertEquals(time, stats.get("time"));
    }

    @Test
    public void testCreate() throws Exception {
        final CypherQueryExecutor.CypherResult result =
                cypherQueryExecutor.cypherQuery("match (n) where id(n) = $id create (n)-[:FOO]->(m)", null, params);
        final int rowCount = result.getRowCount();
        assertEquals(0, rowCount);
        final long time = result.getTime();
        assertTrue("time", time < 2000);
        assertTrue("time", time > 0);
        final Map stats = result.getQueryStatistics();
        assertEquals(9, stats.size());
        assertEquals(rowCount, stats.get("rows"));
        assertEquals(time, stats.get("time"));
        assertEquals(1, stats.get("nodesCreated"));
        assertEquals(1, stats.get("relationshipsCreated"));
        assertEquals(0, stats.get("relationshipsDeleted"));
        assertEquals(0, stats.get("nodesDeleted"));
        assertEquals(0, stats.get("propertiesSet"));
    }
    @Test
    public void testSet() throws Exception {
        final CypherQueryExecutor.CypherResult result =
                cypherQueryExecutor.cypherQuery("match (n) where id(n) = $id set n.foo='bar'", null, params);
        final int rowCount = result.getRowCount();
        assertEquals(0, rowCount);
        final long time = result.getTime();
        assertTrue("time", time < 2000);
        assertTrue("time", time > 0);
        final Map stats = result.getQueryStatistics();
        assertEquals(9, stats.size());
        assertEquals(rowCount, stats.get("rows"));
        assertEquals(time, stats.get("time"));
        assertEquals(0, stats.get("nodesCreated"));
        assertEquals(0, stats.get("relationshipsCreated"));
        assertEquals(0, stats.get("relationshipsDeleted"));
        assertEquals(0, stats.get("nodesDeleted"));
        assertEquals(1, stats.get("propertiesSet"));
    }
    @Test
    public void testDelete() throws Exception {
        final CypherQueryExecutor.CypherResult result =
                cypherQueryExecutor.cypherQuery("match (n) where id(n) = $id create (n)-[r:FOO]->(m) delete r,m", null, params);
        final int rowCount = result.getRowCount();
        assertEquals(0, rowCount);
        final long time = result.getTime();
        assertTrue("time", time < 2000);
        assertTrue("time", time > 0);
        final Map stats = result.getQueryStatistics();
        assertEquals(9, stats.size());
        assertEquals(rowCount, stats.get("rows"));
        assertEquals(time, stats.get("time"));
        assertEquals(1, stats.get("nodesCreated"));
        assertEquals(1, stats.get("relationshipsCreated"));
        assertEquals(1, stats.get("relationshipsDeleted"));
        assertEquals(1, stats.get("nodesDeleted"));
        assertEquals(0, stats.get("propertiesSet"));
    }
}
