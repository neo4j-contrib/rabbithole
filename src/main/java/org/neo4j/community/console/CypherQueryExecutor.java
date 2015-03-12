package org.neo4j.community.console;

import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.cypher.javacompat.PlanDescription;
import org.neo4j.cypher.javacompat.QueryStatistics;
import org.neo4j.cypher.javacompat.internal.ServerExecutionEngine;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.TopLevelTransaction;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.impl.util.StringLogger;
import org.omg.CORBA.SystemException;
import scala.NotImplementedError;

//import javax.transaction.*;
import javax.transaction.InvalidTransactionException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mh
 * @since 21.04.12
 */
public class CypherQueryExecutor {
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("((\\w+)\\s*:|\\w+\\.(\\w+)\\s*=)",Pattern.MULTILINE|Pattern.DOTALL);
    private static final Pattern INDEX_PATTERN = Pattern.compile("(node|relationship)\\s*:\\s*(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}+|`[^`]+`|)\\s*\\(",Pattern.MULTILINE);
    public static final Pattern CANNOT_PROFILE_PATTERN = Pattern.compile("\\b(UNION|OPTIONAL|LOAD)\\b|(\\bMERGE\\b.+){2,}", Pattern.CASE_INSENSITIVE|Pattern.MULTILINE|Pattern.DOTALL);
    private final ThreadToStatementContextBridge threadToStatementContextBridge;
    private ServerExecutionEngine executionEngine;
    private final Index index;
	private final GraphDatabaseService gdb;
    public static final int CYPHER_LENGTH = "CYPHER".length();

    public CypherQueryExecutor(GraphDatabaseService gdb, Index index) {
	    this.gdb = gdb;
        threadToStatementContextBridge = ((GraphDatabaseAPI) gdb).getDependencyResolver().resolveDependency(ThreadToStatementContextBridge.class);
        this.index = index;
        executionEngine = new ServerExecutionEngine(gdb, StringLogger.SYSTEM);
    }

    public boolean isMutatingQuery(String query) {
        return query.matches("(?is).*\\b(create|relate|merge|delete|set)\\b.*");
    }
    public boolean isIndexQuery(String query) {
        return query.matches("(?is).*\\bcreate (index|constraint)\\b.*");
    }
    public boolean isCypherQuery(String query) {
        return query.matches("(?is).*\\b(drop|start|merge|match|return|where|skip|limit|create|relate|delete|set)\\b.*");
    }

    public static class CypherResult implements Iterable<Map<String, Object>> {
        private final List<String> columns;
        private final String query;
        private final String text;
        private final Collection<Map<String, Object>> rows;
        private final List<Map<String, Object>> json;
        private QueryStatistics queryStatistics;
        private final PlanDescription plan;
        private final long time;

        public CypherResult(List<String> columns, Collection<Map<String, Object>> rows, QueryStatistics queryStatistics, long time, PlanDescription plan, String query) {
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
                stats.put("nodesDeleted", queryStatistics.getDeletedNodes());
                stats.put("relationshipsDeleted", queryStatistics.getDeletedRelationships());
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
        return executionEngine.prettify(query).replaceAll("\n","\n ");
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
        if (isMutatingQuery(query)) {
            registerProperties(query);
        }
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
        TopLevelTransaction resumeTx;
        try {
            resumeTx = suspendTx(query);
            ExecutionResult result = canProfile ? executionEngine.profile(query,params) : executionEngine.execute(query,params);
            final Collection<Map<String, Object>> data = IteratorUtil.asCollection((Iterator<Map<String, Object>>)result);
            time = System.currentTimeMillis() - time;
            resumeTransaction(resumeTx);
            CypherResult cypherResult = new CypherResult(result.columns(), data, result.getQueryStatistics(), time, canProfile ? result.executionPlanDescription() : null, prettify(query));
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

    private TopLevelTransaction suspendTx(String query) {
        if (!executionEngine.isPeriodicCommit(query)) return null;
        try {
            TopLevelTransaction tx = threadToStatementContextBridge.getTopLevelTransactionBoundToThisThread(true);
            threadToStatementContextBridge.unbindTransactionFromCurrentThread();
            return tx;
        } catch (Exception e) {
            throw new RuntimeException("Error suspending Transaction",e);
        }
    }
    private void resumeTransaction(TopLevelTransaction tx) {
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

    private void registerProperties(String query) {
        Set<String> properties = extractProperties(query);
        index.registerProperty(properties);
    }
    
    String replaceIndex(String query) {
        Matcher matcher = INDEX_PATTERN.matcher(query);
        if (!matcher.find()) return query;
        StringBuffer sb=new StringBuffer();
        do  {
            matcher.appendReplacement(sb,"$1:$1_auto_index(");
        } while (matcher.find());
        matcher.appendTail(sb);
        return sb.toString();
    }

    // TODO should get metadata from the cypher query
    // does not take care of quoted, non-identifier properties
    Set<String> extractProperties(String query) {
        final Matcher matcher = PROPERTY_PATTERN.matcher(query);
        final Set<String> properties = new HashSet<>();
        while (matcher.find()) {
            if (matcher.group(2)!=null) properties.add(matcher.group(2));
            if (matcher.group(3)!=null) properties.add(matcher.group(3));
        }
        return properties;
    }
}
