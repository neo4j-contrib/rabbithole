package org.neo4j.community.console;

import com.google.gson.Gson;
import org.neo4j.graphdb.*;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author mh
 * @since 22.04.12
 */
class CypherExportService {
    private final GraphDatabaseService gdb;

    CypherExportService(GraphDatabaseService gdb) {
        this.gdb = gdb;
    }

    public String export() {
        StringBuilder sb = new StringBuilder();
        init(sb);
        int count = appendNodes(sb);
        count = appendRelationships(sb, count);
        if (count > 0) return sb.toString();
        return "";
    }

    private void init(StringBuilder sb) {
        sb.append("create \n");
    }

    private int appendRelationships(StringBuilder sb, int count) {
        for (Node node : gdb.getAllNodes()) {
            for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
                if (count > 0) { sb.append(",\n"); }
                count++;
                appendRelationship(sb, rel);
            }
        }
        return count;
    }

    private void appendRelationship(StringBuilder sb, Relationship rel) {
        formatNode(sb, rel.getStartNode());
        sb.append("-[:").append(rel.getType().name());
        formatProperties(sb, rel);
        sb.append("]->");
        formatNode(sb, rel.getEndNode());
    }

    private int appendNodes(StringBuilder sb) {
        int count = 0;
        for (Node node : gdb.getAllNodes()) {
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
        for (Label label : node.getLabels()) {
            sb.append(":`").append(label.name()).append("`");
        }
    }

    private void formatNode(StringBuilder sb, Node n) {
        sb.append("_").append(n.getId());
    }

    private void formatProperties(StringBuilder sb, PropertyContainer pc) {
        final Map<String, Object> properties = toMap(pc);
        if (properties.isEmpty()) return;
        sb.append(" ");
        final String jsonString = new Gson().toJson(properties);
        sb.append(removeNameQuotes(jsonString));
    }

    private String removeNameQuotes(String json) {
        return json.replaceAll("\"([^\"]+)\":","`$1`:");
    }

    Map<String, Object> toMap(PropertyContainer pc) {
        Map<String, Object> result = new TreeMap<>();
        for (String prop : pc.getPropertyKeys()) {
            result.put(prop, pc.getProperty(prop));
        }
        return result;
    }

}
