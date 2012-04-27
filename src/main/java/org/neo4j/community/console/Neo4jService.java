package org.neo4j.community.console;

import org.neo4j.geoff.except.SubgraphError;
import org.neo4j.geoff.except.SyntaxError;
import org.neo4j.graphdb.*;
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

    private Index index = new Index(gdb);
    private CypherQueryExecutor cypherQueryExecutor = new CypherQueryExecutor(gdb,index);
    private GeoffImportService geoffService = new GeoffImportService(gdb, index);
    private GeoffExportService geoffExportService = new GeoffExportService(gdb);
    private CypherExportService cypherExportService = new CypherExportService(gdb);
    private String version;

    public Map cypherQueryViz(String query) {
        final boolean invalidQuery = query == null || query.trim().isEmpty() || cypherQueryExecutor.isMutatingQuery(query);
        return invalidQuery ? cypherQueryViz((CypherQueryExecutor.CypherResult) null) : cypherQueryViz(cypherQuery(query));
    }
    public Map cypherQueryViz(CypherQueryExecutor.CypherResult result) {
        Map<Long, Map<String, Object>> nodes = nodeMap();
        Map<Long, Map<String, Object>> relationships = relationshipMap(nodes);
        markCypherResults(result, nodes, relationships);
        return map("nodes", nodes.values(), "links", relationships.values());
    }

    private void markCypherResults(CypherQueryExecutor.CypherResult result, Map<Long, Map<String, Object>> nodes, Map<Long, Map<String, Object>> rels) {
        if (result==null) return;
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
            if (!nodes.containsKey(id)) return;
            nodes.get(id).put("selected", column);
        }
        if (value instanceof Relationship) {
            final long id = ((Relationship) value).getId();
            if (!rels.containsKey(id)) return;
            rels.get(id).put("selected", column);
        }
    }

    private Map<Long, Map<String, Object>> relationshipMap(Map<Long, Map<String, Object>> nodes) {
        List<Long> nodeIndex = new ArrayList<Long>(nodes.keySet());

        Map<Long, Map<String, Object>> relationships = new TreeMap<Long, Map<String, Object>>();
        for (Relationship rel : GlobalGraphOperations.at(gdb).getAllRelationships()) {
            Map<String, Object> data = geoffExportService.toMap(rel);
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
            Map<String, Object> data = geoffExportService.toMap(n);
            data.put("id", n.getId());
            nodes.put(n.getId(), data);
        }
        return nodes;
    }

    public String exportToGeoff() {
        return geoffExportService.export();
    }
    
    public String exportToCypher() {
        return cypherExportService.export();
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
        for (Map<String, Object> row : cypherQuery(query)) {
            result.add(row);
        }
        return result;
    }

    public CypherQueryExecutor.CypherResult initCypherQuery(String query) {
        return cypherQueryExecutor.cypherQuery(query,null);
    }
    public CypherQueryExecutor.CypherResult cypherQuery(String query) {
        return cypherQueryExecutor.cypherQuery(query,version);
    }

    public void stop() {
        if (gdb!=null) {
            System.err.println("Shutting down service "+this);
            gdb.shutdown();
            index = null;
            cypherQueryExecutor=null;
            geoffExportService =null;
            cypherExportService =null;
            geoffService =null;
            gdb=null;
        }
    }

    public void deleteReferenceNode() {
        final Node root = gdb.getReferenceNode();
        if (root!=null) {
            final Transaction tx = gdb.beginTx();
            try {
                root.delete();
                tx.success();
            } finally {
                tx.finish();
            }
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (version==null || version.trim().isEmpty()) this.version=null;
        else {
            if (!version.matches("\\d+\\.\\d+")) throw new IllegalArgumentException("Incorrect version string "+version);
            this.version = version;
        }
    }

    public boolean hasReferenceNode() {
        try {
            return gdb.getReferenceNode() != null;
        } catch (NotFoundException nfe) {
            return false;
        }
    }

    public boolean isMutatingQuery(String query) {
        return cypherQueryExecutor.isMutatingQuery(query);
    }
}
