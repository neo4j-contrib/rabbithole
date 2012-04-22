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
class GeoffExportService {
    private final GraphDatabaseService gdb;

    GeoffExportService(GraphDatabaseService gdb) {
        this.gdb = gdb;
    }

    public String export() {
        StringBuilder sb = new StringBuilder();
        appendNodes(sb);
        appendRelationships(sb);
        return sb.toString();
    }

    private void appendRelationships(StringBuilder sb) {
        for (Node node : GlobalGraphOperations.at(gdb).getAllNodes()) {
            for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
                appendRelationship(sb, rel);
                sb.append("\n");
            }
        }
    }

    private void appendRelationship(StringBuilder sb, Relationship rel) {
        formatNode(sb, rel.getStartNode());
        sb.append("-[:").append(rel.getType().name()).append("]->");
        formatNode(sb, rel.getEndNode());
        formatProperties(sb, rel);
    }

    private void appendNodes(StringBuilder sb) {
        for (Node node : GlobalGraphOperations.at(gdb).getAllNodes()) {
            if (isReferenceNode(node)) continue;
            formatNode(sb, node);
            formatProperties(sb, node);
            sb.append("\n");
        }
    }

    private boolean isReferenceNode(Node node) {
        return node.getId() == 0;
    }

    private void formatNode(StringBuilder sb, Node n) {
        sb.append("(").append(n.getId()).append(")");
    }

    private void formatProperties(StringBuilder sb, PropertyContainer pc) {
        final Map<String, Object> properties = toMap(pc);
        if (properties.isEmpty()) return;
        sb.append(" ");
        sb.append(new Gson().toJson(properties));
    }

    Map<String, Object> toMap(PropertyContainer pc) {
        Map<String, Object> result = new TreeMap<String, Object>();
        for (String prop : pc.getPropertyKeys()) {
            result.put(prop, pc.getProperty(prop));
        }
        return result;
    }

}
