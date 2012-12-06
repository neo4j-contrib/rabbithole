package org.neo4j.community.console;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.test.ImpermanentGraphDatabase;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author mh
 * @since 06.12.12
 */
public class QueryStatisticsTest {
    private ImpermanentGraphDatabase gdb;
    private CypherQueryExecutor cypherQueryExecutor;

    @Before
    public void setUp() throws Exception {
        gdb = new ImpermanentGraphDatabase();
        cypherQueryExecutor = new CypherQueryExecutor(gdb, new Index(gdb));
    }

    @Test
    public void testNoUpdate() throws Exception {
        final CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("start n=node(0) return n", null);
        final int rowCount = result.getRowCount();
        assertEquals(1, rowCount);
        final long time = result.getTime();
        assertTrue("time", time < 10);
        assertTrue("time", time > 0);
        final Map stats = result.getQueryStatistics();
        assertEquals(2, stats.size());
        assertEquals(rowCount, stats.get("rows"));
        assertEquals(time, stats.get("time"));
    }

    @Test
    public void testCreate() throws Exception {
        final CypherQueryExecutor.CypherResult result =
                cypherQueryExecutor.cypherQuery("start n=node(0) create n-[:FOO]->m", null);
        final int rowCount = result.getRowCount();
        assertEquals(0, rowCount);
        final long time = result.getTime();
        assertTrue("time", time < 10);
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
                cypherQueryExecutor.cypherQuery("start n=node(0) set n.foo='bar'", null);
        final int rowCount = result.getRowCount();
        assertEquals(0, rowCount);
        final long time = result.getTime();
        assertTrue("time", time < 10);
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
                cypherQueryExecutor.cypherQuery("start n=node(0) create n-[r:FOO]->m delete r,m", null);
        final int rowCount = result.getRowCount();
        assertEquals(0, rowCount);
        final long time = result.getTime();
        assertTrue("time", time < 10);
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
