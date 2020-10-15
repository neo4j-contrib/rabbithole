package org.neo4j.community.console;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
* @author mh
* @since 08.04.12
*/
class Neo4jService {

    private static final Logger LOG = Logger.getLogger(Neo4jService.class.getName());
    public static final String VERSION_REGEXP = "(\\d+\\.\\d+(?:\\.experimental|-cost|-rule)?)";

    private Driver driver;
    private String db;

    private CypherQueryExecutor cypherQueryExecutor;
    private CypherExportService cypherExportService;
    private String version;
    private boolean initialized;
    private String id;

    public Neo4jService(Driver driver, String db, boolean ownsDatabase) {
        if (driver == null) throw new IllegalArgumentException("Driver must not be null");
        if (db == null) throw new IllegalArgumentException("Database must not be null");
        this.driver = driver;
        this.db = db;
        this.ownsDatabase = ownsDatabase;
        cypherQueryExecutor = new CypherQueryExecutor(driver,db);
        cypherExportService = new CypherExportService(driver,db);
    }

    public Map cypherQueryViz(String query) {
        final boolean invalidQuery = query == null || query.trim().isEmpty() || cypherQueryExecutor.isMutatingQuery(query);
        return invalidQuery ? cypherQueryViz((CypherQueryExecutor.CypherResult) null) : cypherQueryViz(cypherQuery(query, null));
    }

    public Map cypherQueryViz(CypherQueryExecutor.CypherResult result) {
        final SubGraph subGraph = SubGraph.from(driver, db).markSelection(result);
        Map<String, Object> viz = MapUtil.map("nodes", subGraph.getNodesForViz().values(), "links", subGraph.getRelationshipsForViz().values());
        return viz;
    }

    public String exportToCypher() {
        return cypherExportService.export();
    }

    public Collection<Map<String,Object>> cypherQueryResults(String query) {
        CypherQueryExecutor.CypherResult records = cypherQuery(query, null);
        Collection<Map<String,Object>> result=new ArrayList<>(records.getRowCount());
        for (Record row : records) {
            result.add(row.asMap());
        }
        return result;
    }

    public CypherQueryExecutor.CypherResult initCypherQuery(String query, Map<String, Object> queryParams) {
        return cypherQueryExecutor.cypherQuery(query,null,queryParams);
    }
    public CypherQueryExecutor.CypherResult cypherQuery(String query, Map<String, Object> queryParams) {
        return cypherQueryExecutor.cypherQuery(query,version,queryParams);
    }

    public String prettify(String query) {
        return cypherQueryExecutor.prettify(query);
    }

    public void stop() {
        if (driver !=null) {
            LOG.warning("Shutting down service "+this+" owns db "+ownsDatabase);
            driver.close();
            cypherQueryExecutor=null;
            cypherExportService =null;
            driver =null;
            System.gc();
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (version==null || version.trim().isEmpty()) this.version=null;
        else {
            this.version = checkVersion(version);
        }
    }

    public static String checkVersion(String version) {
        version = version.replaceAll("^"+VERSION_REGEXP+".*","$1");
        if (!version.matches(VERSION_REGEXP)) throw new IllegalArgumentException("Incorrect version string "+version);
        return version;
    }

    public boolean isMutatingQuery(String query) {
        return cypherQueryExecutor.isMutatingQuery(query);
    }
    public boolean isCypherQuery(String query) {
        return cypherQueryExecutor.isCypherQuery(query);
    }

    public Driver getGraphDatabase() {
        return driver;
    }

    public void importGraph(SubGraph graph) {
        graph.importTo(driver, db);
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

    public Map exportToJson(Map<String, Object> graph) {
        Map<String,Map<String,Object>> result=new HashMap<>(graph.size());
        for (Map.Entry<String, Object> entry : graph.entrySet()) {
            Map<String, Object> data = null;
            if (entry.getValue() instanceof Map) {
                //noinspection unchecked
                data = (Map<String, Object>) entry.getValue();
            }
            if (entry.getValue() instanceof Entity) {
                final Entity value = (Entity) entry.getValue();
                if (value instanceof Node) {
                    data=SubGraph.toMap((Node)value);
                }
                if (value instanceof Relationship) data=SubGraph.toMap((Relationship)value);
            }
            if (data!=null) result.put(entry.getKey(),data);
        }
        return result;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean hasId(String id) {
        return id == null || id.equals(this.id);
    }

    public void clear() {
        cypherQueryExecutor.cypherQuery("MATCH (n) detach delete n",null);
    }

    public String getDb() {
        return db;
    }
}
