package org.neo4j.community.console;

import com.google.gson.Gson;
import org.neo4j.geoff.Geoff;
import org.neo4j.geoff.Subgraph;
import org.neo4j.geoff.except.SubgraphError;
import org.neo4j.geoff.except.SyntaxError;
import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
* @author mh
* @since 08.04.12
*/
class GeoffService {
    private final GraphDatabaseService gdb;
    private final Index index;

    GeoffService(GraphDatabaseService gdb, Index index) {
        this.gdb = gdb;
        this.index = index;
    }

    private Map<String, Node> geoffNodeParams() {
        Map<String, Node> result = new HashMap<String, Node>();
        for (Node node : GlobalGraphOperations.at(gdb).getAllNodes()) {
            result.put("(" + node.getId() + ")", node);
        }
        return result;
    }

    public Map<String,PropertyContainer> mergeGeoff(final String geoff) throws SubgraphError, SyntaxError {
        final Subgraph subgraph = new Subgraph(geoff.replaceAll("\\s*;\\s*", "\n"));
        registerProperties(subgraph);
        return Geoff.mergeIntoNeo4j(subgraph, gdb, geoffNodeParams());
    }

    private void registerProperties(Subgraph subgraph) {
        for (Subgraph.Rule rule : subgraph) {
            if (rule==null) continue;
            index.registerProperty(rule.getData());
        }
    }
}
