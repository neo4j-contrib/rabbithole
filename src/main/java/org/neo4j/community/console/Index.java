package org.neo4j.community.console;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.index.RelationshipAutoIndexer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Index {

    private final Set<String> autoIndexedProperties = new HashSet<String>();
    private final AutoIndexer<Node> nodeAutoIndexer;
    private final RelationshipAutoIndexer relationshipAutoIndexer;

    public Index(GraphDatabaseService gdb) {
        nodeAutoIndexer = gdb.index().getNodeAutoIndexer();
        relationshipAutoIndexer = gdb.index().getRelationshipAutoIndexer();
        enableAutoIndex(nodeAutoIndexer);
        enableAutoIndex(relationshipAutoIndexer);
        autoIndexedProperties.addAll(nodeAutoIndexer.getAutoIndexedProperties());
        autoIndexedProperties.addAll(relationshipAutoIndexer.getAutoIndexedProperties());
    }

    private void enableAutoIndex(AutoIndexer<? extends PropertyContainer> autoIndexer) {
        autoIndexer.setEnabled(true);
        autoIndexer.getAutoIndex();
    }

    public void registerProperty(Map<String, Object> data) {
        if (data == null) return;
        for (String prop : data.keySet()) {
            if (autoIndexedProperties.contains(prop)) continue;
            System.out.println("Auto-Indexing "+prop);
            autoIndexedProperties.add(prop);
            nodeAutoIndexer.startAutoIndexingProperty(prop);
            relationshipAutoIndexer.startAutoIndexingProperty(prop);
            autoIndexedProperties.add(prop);
        }
    }

    public Set<String> getAutoIndexedProperties() {
        return autoIndexedProperties;
    }
}
