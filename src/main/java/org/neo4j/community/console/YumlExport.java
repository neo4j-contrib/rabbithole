package org.neo4j.community.console;

import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * @author mh
 * @since 27.05.12
 */
public class YumlExport {
    public String toYuml(SubGraph graph,String...idProps) {
        StringBuilder sb=new StringBuilder();
        final List<String> idPropsList = asList(idProps);
        final Map<Long, Node> nodes = graph.getNodes();
        for (Node node : nodes.values()) {
            sb.append("[").append(graph.uniqueId(node.id(), idPropsList));
            final List<String> props = nodeProps(node, idPropsList);
            if (!props.isEmpty()) {
                sb.append("|");
                for (String prop : props) {
                    sb.append(prop).append(";");
                }
            }
            sb.append("]").append(",");
        }
        for (Relationship rel : graph.getRelationships().values()) {
            sb.append("[").append(graph.uniqueId(rel.startNodeId(), idPropsList)).append("]");
            sb.append(rel.type()).append("->");
            sb.append("[").append(graph.uniqueId(rel.endNodeId(), idPropsList)).append("]");
            sb.append(",");
        }
        return sb.toString();
    }

    private List<String> nodeProps(Node node, List<String> idPropsList) {
        final List<String> result = new ArrayList<>();
        for (String prop : node.keys()) {
            if (prop.equals("id") || idPropsList.contains(prop)) continue;
            result.add(prop+" "+node.get(prop).asString());
        }
    return result;
    }}
