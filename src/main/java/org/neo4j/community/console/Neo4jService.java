package org.neo4j.community.console;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.geoff.except.SubgraphError;
import org.neo4j.geoff.except.SyntaxError;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.*;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
* @author mh
* @since 08.04.12
*/
class Neo4jService {
    private GraphDatabaseService gdb = new ImpermanentGraphDatabase();
    private ExecutionEngine executionEngine = new ExecutionEngine(gdb);
    private GeoffService geoffService = new GeoffService(gdb);

    public String cypherQuery(String query) {
        ExecutionResult result = executionEngine.execute(query);
        return result.toString();
    }

    public Map cypherQueryViz(String query) {
        Map<Long, Map<String, Object>> nodes = nodeMap();
        Map<Long, Map<String, Object>> relationships = relationshipMap(nodes);

        if (query != null && !query.trim().isEmpty()) {
            markCypherResults(query, nodes, relationships);
        }
        return map("nodes", nodes.values(), "links", relationships.values());
    }

    private void markCypherResults(String query, Map<Long, Map<String, Object>> nodes, Map<Long, Map<String, Object>> rels) {
        ExecutionResult result = executionEngine.execute(query);
        for (Map<String, Object> row : result) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                markEntry(nodes, rels, entry);
            }
        }
    }

    private void markEntry(Map<Long, Map<String, Object>> nodes, Map<Long, Map<String, Object>> rels, Map.Entry<String, Object> entry) {
        String column = entry.getKey();
        Object value = entry.getValue();
        if (value instanceof Iterable) {
            for (Object inner : (Iterable)value) {
                markNodeOrRel(nodes,rels,column,inner);
            }
        } else {
            markNodeOrRel(nodes, rels, column, value);
        }
    }

    private void markNodeOrRel(Map<Long, Map<String, Object>> nodes, Map<Long, Map<String, Object>> rels, String column, Object value) {
        if (value instanceof Node) {
            final long id = ((Node) value).getId();
            nodes.get(id).put("selected", column);
        }
        if (value instanceof Relationship) {
            final long id = ((Relationship) value).getId();
            rels.get(id).put("selected", column);
        }
    }

    private Map<Long, Map<String, Object>> relationshipMap(Map<Long, Map<String, Object>> nodes) {
        List<Long> nodeIndex = new ArrayList<Long>(nodes.keySet());

        Map<Long, Map<String, Object>> relationships = new TreeMap<Long, Map<String, Object>>();
        for (Relationship rel : GlobalGraphOperations.at(gdb).getAllRelationships()) {
            Map<String, Object> data = geoffService.toMap(rel);
            data.put("id", rel.getId());
            data.put("source", nodeIndex.indexOf(rel.getStartNode().getId()));
            data.put("target", nodeIndex.indexOf(rel.getEndNode().getId()));
            data.put("type", rel.getType().name());
            data.put(rel.getType().name(), "type");
            relationships.put(rel.getId(), data);
        }
        return relationships;
    }

    private Map<Long, Map<String, Object>> nodeMap() {
        Map<Long, Map<String, Object>> nodes = new TreeMap<Long, Map<String, Object>>();
        for (Node n : GlobalGraphOperations.at(gdb).getAllNodes()) {
            Map<String, Object> data = geoffService.toMap(n);
            data.put("id", n.getId());
            nodes.put(n.getId(), data);
        }
        return nodes;
    }

    public String toGeoff() {
        return geoffService.toGeoff();
    }

    public Map mergeGeoff(String geoff) {
        try {
            return geoffService.mergeGeoff(geoff);
        } catch (SubgraphError subgraphError) {
            throw new RuntimeException("Error merging:\n"+geoff,subgraphError);
        } catch (SyntaxError syntaxError) {
            throw new RuntimeException("Syntax error merging:\n"+geoff,syntaxError);
        }
    }

    public Collection<Map<String,Object>> cypherQueryResults(String query) {
        Collection<Map<String,Object>> result=new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : executionEngine.execute(query)) {
            result.add(row);
        }
        return result;
    }

    public void stop() {
        if (gdb!=null) {
            System.err.println("Shutting down service "+this);
            gdb.shutdown();
            executionEngine=null;
            geoffService=null;
            gdb=null;
        }
    }
}
