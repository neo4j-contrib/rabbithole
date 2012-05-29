package org.neo4j.community.console;

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
        final Map<Long, Map<String, Object>> nodes = graph.getNodes();
        for (Map.Entry<Long, Map<String, Object>> node : nodes.entrySet()) {
            sb.append("[").append(graph.uniqueId(node.getKey(), idPropsList));
            final List<String> props = nodeProps(node, idPropsList);
            if (!props.isEmpty()) {
                sb.append("|");
                for (String prop : props) {
                    sb.append(prop).append(";");
                }
            }
            sb.append("]").append(",");
        }
        for (Map<String, Object> rel : graph.getRelationships().values()) {
            final Long start = (Long) rel.get("start");
            final Long end = (Long) rel.get("end");
            sb.append("[").append(graph.uniqueId(start, idPropsList)).append("]");
            sb.append(rel.get("type")).append("->");
            sb.append("[").append(graph.uniqueId(end, idPropsList)).append("]");
            sb.append(",");
        }
        return sb.toString();
    }

    private List<String> nodeProps(Map.Entry<Long, Map<String, Object>> node, List<String> idPropsList) {
        final List<String> result = new ArrayList<String>();
        if (node.getValue().size()>1) {
            for (Map.Entry<String, Object> props : node.getValue().entrySet()) {
                if (props.getKey().equals("id") || idPropsList.contains(props.getKey())) continue;
                result.add(props.getKey()+" "+props.getValue());
            }
        }
        return result;
    }}
