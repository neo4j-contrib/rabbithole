package org.neo4j.community.console;

import org.neo4j.driver.*;
import org.neo4j.driver.summary.Plan;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author mh
 * @since 21.04.12
 */
public class CypherQueryExecutor {
    public static final Pattern CANNOT_PROFILE_PATTERN = Pattern.compile("\\b(PERIODIC)\\b", Pattern.CASE_INSENSITIVE|Pattern.MULTILINE|Pattern.DOTALL);
	private final Driver driver;
	private final String db;
    public static final int CYPHER_LENGTH = "CYPHER".length();

    public CypherQueryExecutor(Driver gdb, String db) {
	    this.driver = gdb;
        this.db = db;
    }

    public boolean isMutatingQuery(String query) {
        return query.matches("(?is).*\\b(create|drop|add|remove|merge|delete|call|set)\\b.*");
    }
    public boolean isPeriodicCommit(String query) {
        return query.matches("(?is).*\\b(using\\s+periodic\\s+commit)\\b.*");
    }
    public boolean isIndexQuery(String query) {
        return query.matches("(?is).*\\bcreate (index|constraint)\\b.*");
    }
    public boolean isCypherQuery(String query) {
        return query.matches("(?is).*\\b(drop|start|add|remove|merge|match|call|return|where|skip|limit|create|delete|set)\\b.*");
    }

    public static class CypherResult implements Iterable<Record> {
        private final List<String> columns;
        private final String query;
        private final String text;
        private final List<Record> rows;
        private final List<Map<String, Object>> json;
        private SummaryCounters queryStatistics;
        private final Plan plan;
        private final long time;

        public CypherResult(List<String> columns, List<Record> rows, SummaryCounters queryStatistics, long time, Plan plan, String query) {
            this.query = query;
            this.columns = new ArrayList<>(columns);
            this.queryStatistics = queryStatistics;
            this.time = time;
            this.rows = rows;
            this.plan = plan;
            this.text = generateText();
            this.json = createJson();
        }

        public List<String> getColumns() {
            return columns;
        }

        public String getQuery() {
            return query;
        }

        public int getRowCount() {
            return rows.size();
        }
        
        public Map getQueryStatisticsMap() {
            final Map<String, Object> stats = MapUtil.map(
                    "rows", getRowCount(),
                    "time", getTime()
            );
            if (queryStatistics!=null && queryStatistics.containsUpdates()) {
                stats.put("containsUpdates", queryStatistics.containsUpdates());
                stats.put("nodesDeleted", queryStatistics.nodesDeleted());
                stats.put("relationshipsDeleted", queryStatistics.relationshipsDeleted());
                stats.put("nodesCreated", queryStatistics.nodesCreated());
                stats.put("relationshipsCreated", queryStatistics.relationshipsCreated());
                stats.put("propertiesSet", queryStatistics.propertiesSet());
                stats.put("text", queryStatistics.toString());
            }
            return stats;
        }
        public SummaryCounters getQueryStatistics() {
            return queryStatistics;
        }
        public String getText() {
            return text;
        }

        private String generateText() {
            return new ResultPrinter().generateText(this, time);
        }

        public List<Record> getRows() {
            return rows;
        }

        public String getPlan() {
            return plan!=null ? plan.toString() : "No Query Plan";
        }

        @Override
        public String toString() {
            return getText();
        }

        @Override
        public Iterator<Record> iterator() {
            return rows.iterator();
        }

        public List<Map<String,Object>> getJson() {
            return json;
        }

        private List<Map<String, Object>> createJson() {
            final List<Map<String, Object>> rows = new ArrayList<>();
            for (Record row : this) {
                final LinkedHashMap<String, Object> newRow = new LinkedHashMap<>();
                for (String column : columns) {
                    final Value value = row.get(column);
                    newRow.put(column, toJsonCompatible(value));
                }
                rows.add(newRow);
            }
            return rows;
        }

        private Object toJsonCompatible(Object value) {
            if (value instanceof Node) {
                final Node node = (Node) value;
                final Map<String, Object> result = node.asMap();
                result.put("_id",node.id());

                final List<String> labelNames = SubGraph.getLabelNames(node);
                if (!labelNames.isEmpty()) result.put("_labels", labelNames);
                return result;
            }
            if (value instanceof Relationship) {
                final Relationship relationship = (Relationship) value;
                final Map<String, Object> result = relationship.asMap();
                result.put("_id",relationship.id());
                result.put("_start",relationship.startNodeId());
                result.put("_end",relationship.endNodeId());
                result.put("_type",relationship.type());
                return result;
            }
            if (value instanceof Map) {
                @SuppressWarnings("unchecked") Map<String,Object> map = (Map<String,Object>) value;
                final Map<String,Object> result = new LinkedHashMap<>(map.size());
                for (Map.Entry<String,Object> entry : map.entrySet()) {
                    result.put(entry.getKey(), toJsonCompatible(entry.getValue()));
                }
                return result;
            }
            if (value instanceof Iterable) {
                final List<Object> result = new ArrayList<>();
                for (Object inner : (Iterable)value) {
                    result.add(toJsonCompatible(inner));
                }
                return result;
            }
            return value;
        }

        public long getTime() {
            return time;
        }
    }

    public String prettify(String query) {
        return query;
    }
    public CypherResult cypherQuery(String query, String version, Map<String, Object> params) {
        if (version==null || version.isEmpty() || startsWithCypher(query)) return cypherQuery(query,params);
        return cypherQuery("CYPHER "+version+" "+query, params);
    }
    public CypherResult cypherQuery(String query, String version) {
        return cypherQuery(query,version,null);
    }

    private boolean startsWithCypher(String query) {
        String q = query.trim();
        return q.length() > CYPHER_LENGTH && q.substring(0, CYPHER_LENGTH).equalsIgnoreCase("cypher");
    }

    private CypherResult cypherQuery(String query, Map<String, Object> params) {
        boolean canProfile = canProfileQuery(query);
        try {
            return doExecuteQuery(query, params, canProfile);
        } catch (AssertionError e) {
            return doExecuteQuery(query, params, false);
        }
    }

    private CypherResult doExecuteQuery(String query, Map<String, Object> params, boolean canProfile) {
        params = params == null ? Collections.emptyMap() : params;
        long time=System.currentTimeMillis();
        try (Session session = driver.session(SessionConfig.builder().withDatabase(db).build())){
            Result result = canProfile ? session.run("PROFILE "+query,params) : session.run(query,params);
            final List<Record> data = result.list();
            time = System.currentTimeMillis() - time;
            List<String> keys = result.keys();
            ResultSummary summary = result.consume();
            CypherResult cypherResult = new CypherResult(keys, data, summary.counters(), time, canProfile ? summary.plan() : null, prettify(query));
            return cypherResult;
        } finally {
            awaitIndexOnline(query);
        }
    }

    private void awaitIndexOnline(String query) {
        if (!isIndexQuery(query)) return;
        try (Session s = driver.session(SessionConfig.builder().withDatabase(db).build())) {
            s.run("call db.awaitIndexes($timeout)", Collections.singletonMap("timeout",5));
        }
    }

    boolean canProfileQuery(String query) {
        return !CANNOT_PROFILE_PATTERN.matcher(query).find();
    }
}
