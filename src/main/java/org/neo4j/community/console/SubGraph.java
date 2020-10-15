package org.neo4j.community.console;

import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author mh
 * @since 27.05.12
 */
public class SubGraph {
    SortedMap<Long, Node> nodes = new TreeMap<>();
    SortedMap<Long, Relationship> relationships = new TreeMap<>();

    Object uniqueId(Long nodeId, List<String> uniqueProperties) {
        final Node data = nodes.get(nodeId);
        for (String property : uniqueProperties) {
            if (data.containsKey(property)) return data.get(property);
        }
        return nodeId;
    }

    public Node add(Node node) {
        final long id = node.id();
        if (nodes.containsKey(id)) return nodes.get(id);
        addNode(id, node);
        return node;
    }

    void addNode(long id, Node data) {
        nodes.put(id, data);
    }

    public static Map<String, Object> toMap(Node node) {
        final Map<String, Object> data = node.asMap();
        data.put("id", node.id());
        final List<String> labelNames = getLabelNames(node);
        if (!labelNames.isEmpty()) data.put("labels", labelNames);
        return data;
    }

    public static List<String> getLabelNames(Node node) {
        List<String> labelNames = new ArrayList<>();
        for (String label : node.labels()) {
            labelNames.add(label);
        }
        return labelNames;
    }

    public Relationship add(Relationship rel) {
        if (!hasNode(rel.startNodeId()) || !hasNode(rel.endNodeId())) return null;
        final long id = rel.id();
        if (relationships.containsKey(id)) {
            return relationships.get(id);
        }
        addRel(id, rel);
        return rel;
    }

    public static Map<String, Object> toMap(Relationship rel) {
        final Map<String, Object> data = new HashMap<>(rel.asMap());
        data.put("id", rel.id());
        data.put("start", rel.startNodeId());
        data.put("end", rel.endNodeId());
        data.put("type", rel.type());
        return data;
    }

    private int nodeIndex(long nodeId) {
        return nodes.headMap(nodeId).size();
    }

    public static SubGraph from(Driver driver, String db) {
        final SubGraph graph = new SubGraph();
        try (Session session = driver.session(SessionConfig.builder().withDatabase(db).build())) {
            for (Result result = session.run("MATCH (n) RETURN n"); result.hasNext(); ) {
                Node node = result.next().get("n").asNode();
                graph.add(node);
            }
            for (Result result = session.run("MATCH ()-[r]->() RETURN r"); result.hasNext(); ) {
                Relationship relationship = result.next().get("r").asRelationship();
                graph.add(relationship);
            }
        }
        return graph;
    }

    public static SubGraph from(CypherQueryExecutor.CypherResult result) {
        final SubGraph graph = new SubGraph();
        final List<String> columns = result.getColumns();
        for (Record row : result) {
            for (String column : columns) {
                final Value value = row.get(column);
                addToGraph(graph, value);
            }
        }
        return graph;
    }

    private static void addToGraph(SubGraph graph, Object value) {
        if (value instanceof Node) {
            graph.add((Node) value);
        }
        if (value instanceof Relationship) {
            final Relationship rel = (Relationship) value;
            if (graph.hasNode(rel.startNodeId()) && graph.hasNode(rel.endNodeId()))
                graph.add(rel);
        }
        if (value instanceof Iterable) {
            for (Object inner : (Iterable) value) {
                addToGraph(graph, inner);
            }
        }
    }

    private boolean hasNode(long id) {
        return nodes.containsKey(id);
    }

    public SortedMap<Long, Node> getNodes() {
        return nodes;
    }

    public SortedMap<Long, Relationship> getRelationships() {
        return relationships;
    }

    public Map<String, List<Relationship>> getRelationshipsByType() {
        return relationships.values().stream().collect(Collectors.groupingBy(Relationship::type));
    }

    public Map<Long, Map<String,Object>> getRelationshipsForViz() { // WithIndexedEnds
        SortedMap<Long, Map<String,Object>> result = new TreeMap<>();

        for (Relationship rel : relationships.values()) {
            Map<String,Object> map = toMap(rel);
            map.put("source", nodeIndex(rel.startNodeId()));
            map.put("target", nodeIndex(rel.endNodeId()));
            map.put("selected",selectedRels.get(rel.id()));
            result.put(rel.id(), map);
        }
        return result;
    }
    public Map<Long, Map<String,Object>> getNodesForViz() { // WithIndexedEnds
        SortedMap<Long, Map<String,Object>> result = new TreeMap<>();

        for (Node node : nodes.values()) {
            Map<String,Object> map = new HashMap<>(node.asMap());
            map.put("id", node.id());
            map.put("labels", node.labels());
            map.put("selected",selectedNodes.get(node.id()));
            result.put(node.id(), map);
        }
        return result;
    }

    public SubGraph markSelection(CypherQueryExecutor.CypherResult result) {
        if (result == null) return this;
        for (Record row : result) {
            for (String entry : row.keys()) {
                markEntry(entry, row.get(entry));
            }
        }
        return this;
    }

    private void markEntry(String column, Object value) {
        if (value instanceof Iterable) {
            for (Object inner : (Iterable) value) {
                markNodeOrRel(column, inner);
            }
        } else {
            markNodeOrRel(column, value);
        }
    }

    private Map<Long, String> selectedNodes = new HashMap<>();
    private Map<Long, String> selectedRels = new HashMap<>();

    private void markNodeOrRel(String column, Object value) {
        if (value instanceof Node) {
            final long id = ((Node) value).id();
            if (!nodes.containsKey(id)) return;
            selectedNodes.put(id, column);
        }
        if (value instanceof Relationship) {
            final long id = ((Relationship) value).id();
            if (!relationships.containsKey(id)) return;
            selectedRels.put(id, column);
        }
    }

    @SuppressWarnings("unchecked")
    public static SubGraph from(CypherQueryExecutor.CypherResult restCypherResult, boolean keepDanglingRels) {
        final SubGraph result = new SubGraph();
        for (Record record : restCypherResult) {
            for (Value value : record.values()) {
                addToGraph(result, value);
            }
        }
        if (!keepDanglingRels) result.removeDanglingRels();
        return result;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private void removeDanglingRels() {
        for (Iterator<Relationship> it = relationships.values().iterator(); it.hasNext(); ) {
            Relationship rel = it.next();
            if (nodes.containsKey(rel.startNodeId()) && nodes.containsKey(rel.endNodeId())) continue;
            it.remove();
        }
    }

    void addRel(Long id, Relationship data) {
        relationships.put(id, data);
    }

    void importTo(Driver gdb, String db) {
        try (Session session = gdb.session(SessionConfig.builder().withDatabase(db).build())) {
            session.writeTransaction(tx -> {
                Map<String, Long> nodeMapping = importNodes(tx);
                importRels(tx, nodeMapping);
                tx.commit();
                tx.close();
                return null;
            });
        }
    }

    String ADD_RELS_STATEMENT = "UNWIND $rows AS row MATCH (start),(end) WHERE id(start) = $map[row.start] and id(end) = $map[row.end] " +
            "MERGE (start)-[rel:%s {_id:row.id]]->(end) SET rel = row" +
            "REMOVE rel.start, rel.end, rel.id, rel.type, rel.labels ";

    private void importRels(Transaction tx, Map<String, Long> nodeMapping) {
        getRelationshipsByType().forEach((type, rels) -> {
            tx.run(String.format(ADD_RELS_STATEMENT, type), MapUtil.map("rows", rels, "map", nodeMapping));
        });
    }

    public static final String ADD_NODES_STATEMENT = "UNWIND $rows as row MERGE (n %s {_id:row.id}) SET n=row RETURN row.id as rowId, id(n) as nodeId";

    private Map<String, Long> importNodes(Transaction tx) {
        Map<String, Long> nodeMapping = new HashMap<>();
        Map<Collection<String>, List<Node>> nodesByLabel = getNodes().values().stream().collect(Collectors.groupingBy(SubGraph::getLabelNames));
        nodesByLabel.forEach((labels, nodes) -> {
            Result result = tx.run(String.format(ADD_NODES_STATEMENT, labelString(labels)), Collections.singletonMap("rows", nodes));
            result.forEachRemaining(r -> nodeMapping.put(String.valueOf(r.get("rowId").asLong()), r.get("nodeId").asLong()));
        });
        return nodeMapping;
    }

    public static String labelString(Collection<String> labels) {
        if (labels == null || labels.isEmpty()) return "";
        return ":`" + String.join("`:`", labels) + "`";
    }

    public Iterable<Relationship> getOutgoing(Node node) {
        long nodeId = node.id();
        return relationships.values().stream().filter(r -> r.startNodeId() == nodeId).collect(Collectors.toList());
    }

    public Node getNode(long id) {
        return nodes.get(id);
    }

    @Override
    public String toString() {
        return "Nodes "+nodes.size()+" rels "+relationships.size();
    }
}
