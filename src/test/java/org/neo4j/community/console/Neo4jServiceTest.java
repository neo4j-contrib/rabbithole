package org.neo4j.community.console;

import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.test.ImpermanentGraphDatabase;
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

    @After
    public void tearDown() throws Exception {
        neo4jService.stop();
        gdb.shutdown();
    }
}
