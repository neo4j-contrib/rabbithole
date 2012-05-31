package org.neo4j.community.console;

import org.junit.Test;
import org.neo4j.helpers.collection.MapUtil;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 30.05.12
 */
public class GraphInfoTest {
    @Test
    public void testFromNoRoot() throws Exception {
        final GraphInfo info = GraphInfo.from(MapUtil.map("init", "init", "query", "query", "message", "message", "version", "version", "no_root", "true"));
        assertEquals("query", info.getQuery());
        assertEquals("message", info.getMessage());
        assertEquals("init", info.getInit());
        assertEquals("version", info.getVersion());
        assertEquals(false, info.hasRoot());
    }
    @Test
    public void testFromNullValues() throws Exception {
        final GraphInfo info = GraphInfo.from(MapUtil.map());
        assertEquals(null, info.getQuery());
        assertEquals(null, info.getMessage());
        assertEquals(null, info.getInit());
        assertEquals(null, info.getVersion());
        assertEquals(true, GraphInfo.from(MapUtil.map("no_root", "")).hasRoot());
    }
}
