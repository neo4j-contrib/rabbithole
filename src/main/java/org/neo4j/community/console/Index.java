package org.neo4j.community.console;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.index.RelationshipAutoIndexer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Index {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Index.class);

    private final Set<String> autoIndexedProperties = new HashSet<>();
    private final AutoIndexer<Node> nodeAutoIndexer;
    private final RelationshipAutoIndexer relationshipAutoIndexer;

    public Index(GraphDatabaseService gdb) {
        Transaction tx = gdb.beginTx();
        // force initialize the indexes
        gdb.index().forNodes("node_auto_index");
        gdb.index().forNodes("relationship_auto_index");
        nodeAutoIndexer = gdb.index().getNodeAutoIndexer();
        relationshipAutoIndexer = gdb.index().getRelationshipAutoIndexer();
        enableAutoIndex(nodeAutoIndexer);
        enableAutoIndex(relationshipAutoIndexer);
        autoIndexedProperties.addAll(nodeAutoIndexer.getAutoIndexedProperties());
        autoIndexedProperties.addAll(relationshipAutoIndexer.getAutoIndexedProperties());
        tx.success();tx.close();
    }

    private void enableAutoIndex(AutoIndexer<? extends PropertyContainer> autoIndexer) {
        autoIndexer.setEnabled(true);
        //autoIndexer.getAutoIndex();
    }

    public void registerProperty(final Collection<String> properties) {
        if (properties == null) return;
        for (String prop : properties) {
            if (autoIndexedProperties.contains(prop)) continue;
            LOG.warn("Auto-Indexing " + prop);
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
