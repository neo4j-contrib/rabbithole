package org.neo4j.community.console;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.test.ImpermanentGraphDatabase;

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
        assertTrue(cypherQueryExecutor.isMutatingQuery("start n = node(1) create m={ name: 'Andres'}"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("start n = node(1) create n-[:KNOWS]->n"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("start n = node(1) delete n"));
        assertTrue(cypherQueryExecutor.isMutatingQuery("start n = node(1) set n.name = 'Andres'"));
    }

    @Test
    public void testExtractProperties() throws Exception {
        assertTrue(cypherQueryExecutor.extractProperties("").isEmpty());
        assertTrue(cypherQueryExecutor.extractProperties("start n = node(1) return n").isEmpty());
        assertThat(cypherQueryExecutor.extractProperties("start n = node(1) create m={ name: 'Andres'}"), hasItem("name"));
        assertThat(cypherQueryExecutor.extractProperties("start n = node(1) create n-[:KNOWS {name:'Friends', since : 2000}]->n"), hasItems("name", "since"));
        assertThat(cypherQueryExecutor.extractProperties("start n = node(1) create m={ name: 'Andres'} set n.age = 19"), hasItems("name", "age"));
    }

    @Test
    public void testCypherQuery() throws Exception {
        final CypherQueryExecutor.CypherResult result = cypherQueryExecutor.cypherQuery("start n = node(0) return n");
        assertEquals(asList("n"), result.getColumns());
        assertTrue(result.getText().contains("Node[0]"));
        for (Map<String, Object> row : result) {
            assertEquals(1,row.size());
            assertEquals(true,row.containsKey("n"));
            assertEquals(gdb.getReferenceNode(),row.get("n"));
        }
    }

}
