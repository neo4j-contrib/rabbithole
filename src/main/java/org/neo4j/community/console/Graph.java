package org.neo4j.community.console;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author mh
 * @since 27.05.12
 */
public class Graph {
    Map<Long,Map<String,Object>> nodes=new TreeMap<Long, Map<String, Object>>();
    Map<Long,Map<String,Object>> relationships=new TreeMap<Long, Map<String, Object>>();

    Object uniqueId(Long nodeId, List<String> uniqueProperties) {
        final Map<String, Object> data = nodes.get(nodeId);
        for (String property : uniqueProperties) {
            if (data.containsKey(property)) return data.get(property);
        }
        return nodeId;
    }


    public Map<String, Object> add(Node node) {
        final long id = node.getId();
        if (nodes.containsKey(id)) return nodes.get(id);
        final Map<String, Object> data = toMap(node);
        nodes.put(id, data);
        return data;
    }

    private Map<String, Object> toMap(Node node) {
        final Map<String, Object> data = toMap((PropertyContainer) node);
        data.put("id", node.getId());
        return data;
    }

    public Map<String, Object> add(Relationship rel) {
        final long id = rel.getId();
        if (relationships.containsKey(id)) return relationships.get(id);
        final Map<String, Object> data = toMap(rel);
        relationships.put(id, data);
        return data;
    }

    private Map<String, Object> toMap(Relationship rel) {
        final Map<String, Object> data = toMap((PropertyContainer) rel);
        data.put("id", rel.getId());
        data.put("source", rel.getStartNode().getId());
        data.put("target", rel.getEndNode().getId());
        data.put("type", rel.getType().name());
        return data;
    }

    public static Map<String, Object> toMap(PropertyContainer pc) {
        Map<String, Object> result = new TreeMap<String, Object>();
        for (String prop : pc.getPropertyKeys()) {
            result.put(prop, pc.getProperty(prop));
        }
        return result;
    }

    public static Graph from(GraphDatabaseService gdb) {
        final GlobalGraphOperations operations = GlobalGraphOperations.at(gdb);
        final Graph graph = new Graph();
        for (Node node : operations.getAllNodes()) {
            graph.add(node);
        }
        for (Relationship relationship : operations.getAllRelationships()) {
            graph.add(relationship);
        }
        return graph;
    }

    public static Graph from(CypherQueryExecutor.CypherResult result) {
        final Graph graph = new Graph();
        final List<String> columns = result.getColumns();
        for (Map<String, Object> row : result) {
            for (String column : columns) {
                final Object value = row.get(column);
                addToGraph(graph, value);
            }
        }
        return graph;
    }

    private static void addToGraph(Graph graph, Object value) {
        if (value instanceof Node) {
            graph.add((Node)value);
        }
        if (value instanceof Relationship) {
            final Relationship rel = (Relationship) value;
            graph.add(rel.getStartNode());
            graph.add(rel.getEndNode());
            graph.add(rel);
        }
        if (value instanceof Iterable) {
            for (Object inner : (Iterable) value) {
                addToGraph(graph,inner);
            }
        }
    }

    public Map<Long, Map<String, Object>> getNodes() {
        return nodes;
    }

    public Map<Long, Map<String, Object>> getRelationships() {
        return relationships;
    }
}
