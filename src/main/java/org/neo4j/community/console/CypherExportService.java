package org.neo4j.community.console;

import com.google.gson.Gson;
import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;

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
        final Node refNode = getReferenceNode();
        if (refNode !=null && refNode.hasRelationship()) {
            sb.append("start _0 = node(0) with _0 \n");
        }
        sb.append("create \n");
    }

    private Node getReferenceNode() {
        try {
            return gdb.getReferenceNode();
        } catch(NotFoundException nfe) {
            return null;
        }
    }

    private int appendRelationships(StringBuilder sb, int count) {
        for (Node node : GlobalGraphOperations.at(gdb).getAllNodes()) {
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
        for (Node node : GlobalGraphOperations.at(gdb).getAllNodes()) {
            if (isReferenceNode(node)) continue;
            if (count > 0) { sb.append(",\n"); }
            count++;
            appendNode(sb, node);
        }
        return count;
    }

    private void appendNode(StringBuilder sb, Node node) {
        sb.append("(");
        formatNode(sb, node);
        sb.append(" ");
        formatProperties(sb, node);
        sb.append(")");
    }

    private boolean isReferenceNode(Node node) {
        return node.getId() == 0;
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
        return json.replaceAll("\"([^\"]+)\":","$1:");
    }

    Map<String, Object> toMap(PropertyContainer pc) {
        Map<String, Object> result = new TreeMap<String, Object>();
        for (String prop : pc.getPropertyKeys()) {
            result.put(prop, pc.getProperty(prop));
        }
        return result;
    }

}
