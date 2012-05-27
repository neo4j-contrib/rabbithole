package org.neo4j.community.console;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphalgo.impl.util.PathImpl;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.test.ImpermanentGraphDatabase;

import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 27.05.12
 */
public class GraphTest {

    private ImpermanentGraphDatabase gdb;

    @Before
    public void setUp() throws Exception {
        gdb = new ImpermanentGraphDatabase();
        gdb.beginTx();
    }

    @Test
    public void testToMap() throws Exception {
        final Node node = gdb.getReferenceNode();
        final Map<String, Object> data = Graph.toMap(node);
        assertEquals(0,data.size());
    }

    @Test
    public void testFromSimpleCypherResult() throws Exception {
        final CypherQueryExecutor.CypherResult result = result("node",gdb.getReferenceNode());
        final Graph graph = Graph.from(result);
        assertRefNodeGraph(graph);
    }
    @Test
    public void testFromRelCypherResult() throws Exception {
        final Relationship rel = gdb.getReferenceNode().createRelationshipTo(gdb.getReferenceNode(), DynamicRelationshipType.withName("REL"));
        final CypherQueryExecutor.CypherResult result = result("rel", rel);
        final Graph graph = Graph.from(result);
        assertEquals(1,graph.getNodes().size());
        final Map<Long, Map<String, Object>> rels = graph.getRelationships();
        assertEquals(1, rels.size());
        assertEquals(true, rels.containsKey(rel.getId()));
    }
    @Test
    public void testFromPathCypherResult() throws Exception {
        final Relationship rel = gdb.getReferenceNode().createRelationshipTo(gdb.getReferenceNode(), DynamicRelationshipType.withName("REL"));
        final Path path = new PathImpl.Builder(gdb.getReferenceNode()).push(rel).build();
        final CypherQueryExecutor.CypherResult result = result("path", path);
        final Graph graph = Graph.from(result);
        assertEquals(1,graph.getNodes().size());
        final Map<Long, Map<String, Object>> rels = graph.getRelationships();
        assertEquals(1, rels.size());
        assertEquals(true, rels.containsKey(rel.getId()));
    }

    private CypherQueryExecutor.CypherResult result(String column, Object value) {
        final scala.collection.immutable.Map row = new scala.collection.immutable.HashMap().updated(column, value);
        return new CypherQueryExecutor.CypherResult(asList(column), "", (Iterable) asList(row));
    }

    @Test
    public void testFromEmptyGraph() throws Exception {
        final Graph graph = Graph.from(gdb);
        assertRefNodeGraph(graph);
    }

    private void assertRefNodeGraph(Graph graph) {
        final Map<Long,Map<String,Object>> nodes = graph.getNodes();
        assertEquals(1,nodes.size());
        final Map<String, Object> node = nodes.get(0L);
        assertEquals(1, node.size());
        assertEquals(0L, node.get("id"));
    }

    @Test
    public void testFromSimpleGraph() throws Exception {
        final Node n1 = gdb.createNode();
        n1.setProperty("name","Node1");
        final Node n0 = gdb.getReferenceNode();
        final Relationship relationship = n0.createRelationshipTo(n1, DynamicRelationshipType.withName("REL"));
        relationship.setProperty("related", true);
        final Graph graph = Graph.from(gdb);
        final Map<Long,Map<String,Object>> nodes = graph.getNodes();
        assertEquals(2, nodes.size());
        final Map<String, Object> node = nodes.get(n1.getId());
        assertEquals(2, node.size());
        assertEquals(n1.getId(), node.get("id"));
        assertEquals(n1.getProperty("name"), node.get("name"));
        final Map<Long,Map<String,Object>> rels = graph.getRelationships();
        assertEquals(1,rels.size());
        final Map<String, Object> rel = rels.get(relationship.getId());
        assertEquals(5, rel.size());
        assertEquals(relationship.getId(), rel.get("id"));
        assertEquals(relationship.getProperty("related"), rel.get("related"));
        assertEquals(relationship.getType().name(), rel.get("type"));
        assertEquals(n0.getId(), rel.get("source"));
        assertEquals(n1.getId(), rel.get("target"));
    }
}
