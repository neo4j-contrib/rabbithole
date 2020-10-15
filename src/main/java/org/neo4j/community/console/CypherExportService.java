package org.neo4j.community.console;

import com.google.gson.Gson;
import org.neo4j.driver.Driver;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author mh
 * @since 22.04.12
 */
class CypherExportService {
    private final Driver driver;
    private final String db;

    CypherExportService(Driver driver, String db) {
        this.driver = driver;
        this.db = db;
    }

    public String export() {
        StringBuilder sb = new StringBuilder("create \n");
        SubGraph graph = SubGraph.from(driver, db);
        int count = appendNodes(sb, graph);
        count = appendRelationships(sb, graph, count);
        if (count > 0) return sb.toString();
        return "";
    }

    private int appendRelationships(StringBuilder sb, SubGraph graph, int count) {
        for (Node node : graph.getNodes().values()) {
            for (Relationship rel : graph.getOutgoing(node)) {
                if (count > 0) { sb.append(",\n"); }
                count++;
                appendRelationship(sb, rel, graph.getNode(rel.startNodeId()), graph.getNode(rel.endNodeId()));
            }
        }
        return count;
    }

    private void appendRelationship(StringBuilder sb, Relationship rel, Node start, Node end) {
        sb.append("(");
        formatNode(sb, start);
        sb.append(")-[:`").append(rel.type()).append("`");
        formatProperties(sb, rel);
        sb.append("]->(");
        formatNode(sb, end);
        sb.append(")");
    }

    private int appendNodes(StringBuilder sb, SubGraph graph) {
        int count = 0;
        for (Node node : graph.getNodes().values()) {
            if (count > 0) { sb.append(",\n"); }
            count++;
            appendNode(sb, node);
        }
        return count;
    }

    private void appendNode(StringBuilder sb, Node node) {
        sb.append("(");
        formatNode(sb, node);
        formatLabels(sb, node);
        sb.append(" ");
        formatProperties(sb, node);
        sb.append(")");
    }

    private void formatLabels(StringBuilder sb, Node node) {
        for (String label : node.labels()) {
            sb.append(":`").append(label).append("`");
        }
    }

    private void formatNode(StringBuilder sb, Node n) {
        sb.append("_").append(n.id());
    }

    private void formatProperties(StringBuilder sb, org.neo4j.driver.types.Entity pc) {
        final Map<String, Object> properties = toMap(pc);
        if (properties.isEmpty()) return;
        sb.append(" ");
        final String jsonString = new Gson().toJson(properties);
        sb.append(removeNameQuotes(jsonString));
    }

    private String removeNameQuotes(String json) {
        return json.replaceAll("\"([^\"]+)\":","`$1`:");
    }

    Map<String, Object> toMap(org.neo4j.driver.types.Entity pc) {
        Map<String, Object> result = new TreeMap<>();
        for (String prop : pc.keys()) {
            result.put(prop, pc.get(prop).asObject());
        }
        return result;
    }

}
