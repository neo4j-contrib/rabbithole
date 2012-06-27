package org.neo4j.community.console;

import org.slf4j.Logger;
import org.neo4j.geoff.except.SubgraphError;
import org.neo4j.geoff.except.SyntaxError;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.ImpermanentGraphDatabase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.neo4j.helpers.collection.MapUtil.map;
import static org.neo4j.helpers.collection.MapUtil.stringMap;

/**
* @author mh
* @since 08.04.12
*/
class Neo4jService {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Neo4jService.class);

    private GraphDatabaseService gdb;

    private Index index;
    private CypherQueryExecutor cypherQueryExecutor;
    private GeoffImportService geoffService;
    private GeoffExportService geoffExportService;
    private CypherExportService cypherExportService;
    private String version;
    private boolean initialized;

    Neo4jService() {
        this(new ImpermanentGraphDatabase(stringMap("execution_guard_enabled","true")),true);
    }

    Neo4jService(GraphDatabaseService gdb) {
        this(gdb,false);
    }

    private Neo4jService(GraphDatabaseService gdb, boolean ownsDatabase) {
        if (gdb == null) throw new IllegalArgumentException("Graph Database must not be null");
        this.gdb = gdb;
        this.ownsDatabase = ownsDatabase;
        index = new Index(this.gdb);
        cypherQueryExecutor = new CypherQueryExecutor(gdb,index);
        geoffService = new GeoffImportService(gdb, index);
        geoffExportService = new GeoffExportService(gdb);
        cypherExportService = new CypherExportService(gdb);
    }

    public Map cypherQueryViz(String query) {
        final boolean invalidQuery = query == null || query.trim().isEmpty() || cypherQueryExecutor.isMutatingQuery(query);
        return invalidQuery ? cypherQueryViz((CypherQueryExecutor.CypherResult) null) : cypherQueryViz(cypherQuery(query));
    }
    public Map cypherQueryViz(CypherQueryExecutor.CypherResult result) {
        final SubGraph subGraph = SubGraph.from(gdb).markSelection(result);
        return map("nodes", subGraph.getNodes().values(), "links", subGraph.getRelationships().values());
    }

    public String exportToGeoff() {
        return geoffExportService.export();
    }
    
    public String exportToCypher() {
        return cypherExportService.export();
    }

    public Map mergeGeoff(String geoff) {
        try {
            return geoffService.mergeGeoff(geoff);
        } catch (SubgraphError subgraphError) {
            throw new RuntimeException("Error merging:\n"+geoff,subgraphError);
        } catch (SyntaxError syntaxError) {
            throw new RuntimeException("Syntax error merging:\n"+geoff,syntaxError);
        }
    }

    public Collection<Map<String,Object>> cypherQueryResults(String query) {
        Collection<Map<String,Object>> result=new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : cypherQuery(query)) {
            result.add(row);
        }
        return result;
    }

    public CypherQueryExecutor.CypherResult initCypherQuery(String query) {
        return cypherQueryExecutor.cypherQuery(query,null);
    }
    public CypherQueryExecutor.CypherResult cypherQuery(String query) {
        return cypherQueryExecutor.cypherQuery(query,version);
    }

    public void stop() {
        if (gdb!=null) {
            LOG.warn("Shutting down service "+this);
            if (ownsDatabase) gdb.shutdown();
            index = null;
            cypherQueryExecutor=null;
            geoffExportService =null;
            cypherExportService =null;
            geoffService =null;
            gdb=null;
        }
    }

    public void deleteReferenceNode() {
        if (rootNodeRemovalNotAllowed()) return;
        final Node root = gdb.getReferenceNode();
        if (root!=null) {
            final Transaction tx = gdb.beginTx();
            try {
                root.delete();
                tx.success();
            } finally {
                tx.finish();
            }
        }
    }

    private boolean rootNodeRemovalNotAllowed() {
        return !ownsDatabase || isInitialized();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (version==null || version.trim().isEmpty()) this.version=null;
        else {
            version = version.replaceAll("^(\\d+\\.\\d+).*","$1");
            if (!version.matches("\\d+\\.\\d+")) throw new IllegalArgumentException("Incorrect version string "+version);
            this.version = version;
        }
    }

    public boolean hasReferenceNode() {
        try {
            return gdb.getReferenceNode() != null;
        } catch (NotFoundException nfe) {
            return false;
        }
    }

    public boolean isMutatingQuery(String query) {
        return cypherQueryExecutor.isMutatingQuery(query);
    }
    public boolean isCypherQuery(String query) {
        return cypherQueryExecutor.isCypherQuery(query);
    }

    public GraphDatabaseService getGraphDatabase() {
        return gdb;
    }

    public void importGraph(SubGraph graph) {
        final Transaction tx = gdb.beginTx();
        try {
            graph.importTo(gdb, hasReferenceNode());
            tx.success();
        } finally {
            tx.finish();
        }
    }

    public URL toUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private final boolean ownsDatabase;
    public boolean doesOwnDatabase() {
        return ownsDatabase;
    }

    public Neo4jService initializeFrom(SubGraph graph) {
        importGraph(graph);
        setInitialized();
        return this;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized() {
        this.initialized = true;
    }
}
