package org.neo4j.community.console;

import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.Iterators;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.logging.FormattedLogProvider;
import scala.NotImplementedError;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mh
 * @since 21.04.12
 */
public class CypherQueryExecutor {
    public static final Pattern CANNOT_PROFILE_PATTERN = Pattern.compile("\\b(PERIODIC)\\b", Pattern.CASE_INSENSITIVE|Pattern.MULTILINE|Pattern.DOTALL);
    private final ThreadToStatementContextBridge threadToStatementContextBridge;
	private final GraphDatabaseService gdb;
    public static final int CYPHER_LENGTH = "CYPHER".length();

    public CypherQueryExecutor(GraphDatabaseService gdb) {
	    this.gdb = gdb;
        DependencyResolver dependencyResolver = ((GraphDatabaseAPI) gdb).getDependencyResolver();

        threadToStatementContextBridge = dependencyResolver.resolveDependency(ThreadToStatementContextBridge.class);
        FormattedLogProvider logProvider = FormattedLogProvider.toOutputStream(System.out);
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

    public static class CypherResult implements Iterable<Map<String, Object>> {
        private final List<String> columns;
        private final String query;
        private final String text;
        private final Collection<Map<String, Object>> rows;
        private final List<Map<String, Object>> json;
        private QueryStatistics queryStatistics;
        private final ExecutionPlanDescription plan;
        private final long time;

        public CypherResult(List<String> columns, Collection<Map<String, Object>> rows, QueryStatistics queryStatistics, long time, ExecutionPlanDescription plan, String query) {
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
        
        public Map getQueryStatistics() {
            final Map<String, Object> stats = MapUtil.map(
                    "rows", getRowCount(),
                    "time", getTime()
            );
            if (queryStatistics!=null && queryStatistics.containsUpdates()) {
                stats.put("containsUpdates", queryStatistics.containsUpdates());
                stats.put("nodesDeleted", queryStatistics.getNodesDeleted());
                stats.put("relationshipsDeleted", queryStatistics.getRelationshipsDeleted());
                stats.put("nodesCreated", queryStatistics.getNodesCreated());
                stats.put("relationshipsCreated", queryStatistics.getRelationshipsCreated());
                stats.put("propertiesSet", queryStatistics.getPropertiesSet());
                stats.put("text", queryStatistics.toString());
            }
            return stats;
        }
        
        public String getText() {
            return text;
        }

        private String generateText() {
            return new ResultPrinter().generateText(columns, rows, time, queryStatistics);
        }

        public Collection<Map<String, Object>> getRows() {
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
        public Iterator<Map<String, Object>> iterator() {
            return rows.iterator();
        }

        public List<Map<String,Object>> getJson() {
            return json;
        }

        private List<Map<String, Object>> createJson() {
            final List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, Object> row : this) {
                final LinkedHashMap<String, Object> newRow = new LinkedHashMap<>();
                for (String column : columns) {
                    final Object value = row.get(column);
                    newRow.put(column, toJsonCompatible(value));
                }
                rows.add(newRow);
            }
            return rows;
        }

        private Object toJsonCompatible(Object value) {
            if (value instanceof Node) {
                final Node node = (Node) value;
                final Map<String, Object> result = SubGraph.toMap((PropertyContainer)node);
                result.put("_id",node.getId());

                final List<String> labelNames = SubGraph.getLabelNames(node);
                if (!labelNames.isEmpty()) result.put("_labels", labelNames);
                return result;
            }
            if (value instanceof Relationship) {
                final Relationship relationship = (Relationship) value;
                final Map<String, Object> result = SubGraph.toMap((PropertyContainer) relationship);
                result.put("_id",relationship.getId());
                result.put("_start",relationship.getStartNode().getId());
                result.put("_end",relationship.getEndNode().getId());
                result.put("_type",relationship.getType().name());
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
        return query; // TODO PRETTIFY executionEngine.prettify(query).replaceAll("\n","\n ");
    }
    public CypherResult cypherQuery(String query, String version, Map<String, Object> params) {
        // query = replaceIndex(query);
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
        } catch (NotImplementedError |AssertionError e) {
            return doExecuteQuery(query, params, false);
        }
    }

    private CypherResult doExecuteQuery(String query, Map<String, Object> params, boolean canProfile) {
        params = params == null ? Collections.<String,Object>emptyMap() : params;
        long time=System.currentTimeMillis();
        Transaction tx = gdb.beginTx();
        KernelTransaction resumeTx;
        try {
            resumeTx = suspendTx(query);
            Result result = canProfile ? gdb.execute("PROFILE "+query,params) : gdb.execute(query,params);
            final Collection<Map<String, Object>> data = Iterators.asCollection(result);
            time = System.currentTimeMillis() - time;
            resumeTransaction(resumeTx);
            CypherResult cypherResult = new CypherResult(result.columns(), data, result.getQueryStatistics(), time, canProfile ? result.getExecutionPlanDescription() : null, prettify(query));
            result.close();
            tx.success();
            return cypherResult;
        } finally {
            tx.close();
            awaitIndexOnline(query);
        }
    }

    private void awaitIndexOnline(String query) {
        if (!isIndexQuery(query)) return;
        try (Transaction tx = gdb.beginTx()) {
            gdb.schema().awaitIndexesOnline(5, TimeUnit.SECONDS);
            tx.success();
        }
    }

    private KernelTransaction suspendTx(String query) {
        if (!isPeriodicCommit(query)) return null;
        try {
            KernelTransaction tx = threadToStatementContextBridge.getTopLevelTransactionBoundToThisThread(true);
            threadToStatementContextBridge.unbindTransactionFromCurrentThread();
            return tx;
        } catch (Exception e) {
            throw new RuntimeException("Error suspending Transaction",e);
        }
    }
    private void resumeTransaction(KernelTransaction tx) {
        if (tx == null) return;
        try {
            threadToStatementContextBridge.bindTransactionToCurrentThread(tx);
        } catch (Exception e) {
            throw new RuntimeException("Error resuming Transaction "+tx,e);
        }
    }

    boolean canProfileQuery(String query) {
//        return false;
        Matcher matcher = CANNOT_PROFILE_PATTERN.matcher(query);
        return !matcher.find();
    }
}
