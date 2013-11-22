package org.neo4j.community.console;

import org.neo4j.graphdb.*;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.*;

import static java.util.Arrays.asList;
import static org.neo4j.graphdb.DynamicLabel.label;

/**
 * @author mh
 * @since 27.05.12
 */
public class SubGraph {
    SortedMap<Long,Map<String,Object>> nodes=new TreeMap<Long, Map<String, Object>>();
    SortedMap<Long,Map<String,Object>> relationships=new TreeMap<Long, Map<String, Object>>();

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
        addNode(id, data);
        return data;
    }

    void addNode(long id, Map<String, Object> data) {
        nodes.put(id, data);
    }

    public static Map<String, Object> toMap(Node node) {
        final Map<String, Object> data = toMap((PropertyContainer) node);
        data.put("id", node.getId());
        final List<String> labelNames = getLabelNames(node);
        if (!labelNames.isEmpty()) data.put("labels",labelNames);
        return data;
    }

    public static List<String> getLabelNames(Node node) {
        List<String> labelNames = new ArrayList<String>();
        if (!(node instanceof RestNode)) {
            for (Label label : node.getLabels()) {
                labelNames.add(label.name());
            }
        }
        return labelNames;
    }

    public Map<String, Object> add(Relationship rel) {
        final long id = rel.getId();
        if (relationships.containsKey(id)) {
            return relationships.get(id);
        }
        final Map<String, Object> data = toMap(rel);
        addResolvedNodeIndexes(data,rel);
        addRel(id, data);

        add(rel.getStartNode());
        add(rel.getEndNode());
        return data;
    }

    private Map<String, Object> addResolvedNodeIndexes(Map<String, Object> data, Relationship rel) {
        data.put("source", nodeIndex(rel.getStartNode().getId()));
        data.put("target", nodeIndex(rel.getEndNode().getId()));
        return data;
    }

    public static Map<String, Object> toMap(Relationship rel) {
        final Map<String, Object> data = toMap((PropertyContainer) rel);
        data.put("id", rel.getId());
        data.put("start", rel.getStartNode().getId());
        data.put("end", rel.getEndNode().getId());
        data.put("type", rel.getType().name());
        return data;
    }

    private Map<String, Object> relWithIndexEnds(Map<String, Object> rel) {
        if (rel.containsKey("type") && rel.containsKey("start") && rel.containsKey("end")) {
            final Map<String, Object> result = new TreeMap<String, Object>(rel);
            result.put("source", nodeIndex((Long)rel.get("start")));
            result.put("target", nodeIndex((Long)rel.get("end")));
            return result;
        }
        return rel;
    } 
    
    

    private int nodeIndex(long nodeId) {
        return nodes.headMap(nodeId).size();
    }

    public static Map<String, Object> toMap(PropertyContainer pc) {
        Map<String, Object> result = new TreeMap<String, Object>();
        for (String prop : pc.getPropertyKeys()) {
            result.put(prop, pc.getProperty(prop));
        }
        return result;
    }

    public static SubGraph from(GraphDatabaseService gdb) {
        final GlobalGraphOperations operations = GlobalGraphOperations.at(gdb);
        final SubGraph graph = new SubGraph();
        for (Node node : operations.getAllNodes()) {
            graph.add(node);
        }
        for (Relationship relationship : operations.getAllRelationships()) {
            graph.add(relationship);
        }
        return graph;
    }

    public static SubGraph from(GraphDatabaseService db, CypherQueryExecutor.CypherResult result) {
        try (Transaction tx = db.beginTx()) {
            final SubGraph graph = new SubGraph();
            final List<String> columns = result.getColumns();
            for (Map<String, Object> row : result) {
                for (String column : columns) {
                    final Object value = row.get(column);
                    addToGraph(graph, value);
                }
            }
            tx.success();
            return graph;
        }
    }

    private static void addToGraph(SubGraph graph, Object value) {
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

    public Map<Long, Map<String, Object>> getRelationshipsWithIndexedEnds() {
        SortedMap<Long,Map<String,Object>> result=new TreeMap<Long, Map<String, Object>>();

        for (Map.Entry<Long, Map<String, Object>> entry : relationships.entrySet()) {
            result.put(entry.getKey(),relWithIndexEnds(entry.getValue()));
        }
        return result;
    }

    public SubGraph markSelection(CypherQueryExecutor.CypherResult result) {
        if (result==null) return this;
        for (Map<String, Object> row : result) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                markEntry(entry);
            }
        }
        return this;
    }

    private void markEntry(Map.Entry<String, Object> entry) {
        String column = entry.getKey();
        Object value = entry.getValue();
        if (value instanceof Iterable) {
	        for (Object inner : (Iterable)value) {
                markNodeOrRel(column,inner);
            }
        } else {
            markNodeOrRel(column, value);
        }
    }

    private void markNodeOrRel(String column, Object value) {
        if (value instanceof Node) {
            final long id = ((Node) value).getId();
            if (!nodes.containsKey(id)) return;
            nodes.get(id).put("selected", column);
        }
        if (value instanceof Relationship) {
            final long id = ((Relationship) value).getId();
            if (!relationships.containsKey(id)) return;
            relationships.get(id).put("selected", column);
        }
    }

    @SuppressWarnings("unchecked")
    public static SubGraph from(Map restCypherResult, boolean keepDanglingRels) {
        final SubGraph result = new SubGraph();
        final List<String> cols = (List<String>) restCypherResult.get("columns");
        final List<Map<String,Object>> rows = (List<Map<String,Object>>) restCypherResult.get("data");
        for (Map<String,Object> row : rows) {
            for (String col : cols) {
                final Object value = row.get(col);
                result.addJsonElementToGraph(value,keepDanglingRels);
            }
        }
        if (!keepDanglingRels) result.removeDanglingRels();
        return result;
    }
    @SuppressWarnings("unchecked")
    public static SubGraph fromRaw(Map restCypherResult, boolean keepDanglingRels) {
        final SubGraph result = new SubGraph();
        final List<List<Object>> rows = (List<List<Object>>) restCypherResult.get("data");
        for (List<Object> row : rows) {
            for (int i = row.size() - 1; i >= 0; i--) {
                final Object value = row.get(i);
                result.addJsonElementToGraph(value,keepDanglingRels);
            }
        }
        if (!keepDanglingRels) result.removeDanglingRels();
        return result;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private void removeDanglingRels() {
        for (Iterator<Map<String, Object>> it = relationships.values().iterator(); it.hasNext(); ) {
            Map<String, Object> rel = it.next();
            if (nodes.containsKey(rel.get("start")) && nodes.containsKey(rel.get("end"))) continue;
            it.remove();
        }
    }

    private void addJsonElementToGraph(Object value, boolean keepDanglingRels) {
        if (value instanceof Iterable) {
            for (Object inner : (Iterable) value) {
                addJsonElementToGraph(inner, keepDanglingRels);
            }
        }

        if (value instanceof Map) {
            Map element = (Map) value;
            Long id = toId((String) element.get("self"));
            if (id == null) return;
            Map<String,Object> data = toDataMap(element, id);
            if (element.containsKey("type")) {

                final Long start = toId((String) element.get("start"));
                data.put("start", start);

                final Long end = toId((String) element.get("end"));
                data.put("end", end);

                data.put("type",element.get("type"));
                addRel(id, data);
                if (keepDanglingRels) {
                    addPlaceholderNode(start);
                    addPlaceholderNode(end);
                }
            } else {
                addNode(id, data);
            }
        }
    }

    void addRel(Long id, Map<String, Object> data) {
        relationships.put(id,data);
    }

    private void addPlaceholderNode(Long id) {
        if (nodes.containsKey(id)) return;
        addNode(id, Collections.<String, Object>singletonMap("id", id));
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> toDataMap(Map element, Long id) {
        final Map<String,Object> data = element.containsKey("data") ? (Map<String, Object>) element.get("data") : new HashMap<String, Object>();
        data.put("id",id);
        return data;
    }

    private Long toId(String selfUri) {
        if (selfUri==null) return null;
        try{
            final int idx = selfUri.lastIndexOf('/');
            if (idx!=-1) return Long.valueOf(selfUri.substring(idx+1));
        } catch (NumberFormatException nfe) {
            // ignore
        }
        return null;
    }

    void importTo(GraphDatabaseService gdb, final boolean hasReferenceNode) {
        Map<Long, Long> nodeMapping = importNodes(gdb, hasReferenceNode);
        importRels(gdb, nodeMapping);
    }

    private void importRels(GraphDatabaseService gdb, Map<Long, Long> nodeMapping) {
        final HashSet<String> relSkipProps = new HashSet<String>(asList("id", "start", "end", "type","labels"));
        for (Map<String, Object> relData : getRelationships().values()) {
            Long start = (Long) relData.get("start");
            final Node startNode = gdb.getNodeById(nodeMapping.get(start));
            Long end = (Long) relData.get("end");
            final Node endNode = gdb.getNodeById(nodeMapping.get(end));
            String type = (String) relData.get("type");
            final Relationship rel = startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName(type));
            setProperties(rel, relData, relSkipProps);
        }
    }

    private Map<Long, Long> importNodes(GraphDatabaseService gdb, boolean hasReferenceNode) {
        Map<Long,Long> nodeMapping=new HashMap<Long, Long>();
        if (hasReferenceNode) nodeMapping.put(0L,0L);
        final List<String> nodeSkipProps = Arrays.asList("id", "labels");
        for (Map.Entry<Long, Map<String, Object>> nodeData : getNodes().entrySet()) {
            final Long nodeDataId = nodeData.getKey();
            if (!nodeMapping.containsKey(nodeDataId)) {
                nodeMapping.put(nodeDataId, gdb.createNode().getId());
            }
            final Node node = gdb.getNodeById(nodeMapping.get(nodeDataId));
            final Map<String, Object> data = nodeData.getValue();
            setProperties(node, data, nodeSkipProps);
            setLabels(node, (Collection<String>) data.get("labels"));
        }
        return nodeMapping;
    }

    private void setLabels(Node node, final Collection<String> labels) {
        if (labels==null || labels.isEmpty()) return;
        for (String label : labels) {
            node.addLabel(label(label));
        }
    }

    private void setProperties(PropertyContainer pc, Map<String, Object> props, final Collection<String> skipProps) {
        for (Map.Entry<String, Object> prop : props.entrySet()) {
            if (skipProps.contains(prop.getKey())) continue;
            pc.setProperty(prop.getKey(), prop.getValue());
        }
    }
}
