package org.neo4j.community.console;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.cypher.javacompat.QueryStatistics;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.impl.util.StringLogger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mh
 * @since 21.04.12
 */
public class CypherQueryExecutor {
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("((\\w+)\\s*:|\\w+\\.(\\w+)\\s*=)",Pattern.MULTILINE|Pattern.DOTALL);
    private static final Pattern INDEX_PATTERN = Pattern.compile("(node|relationship)\\s*:\\s*(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}+|`[^`]+`|)\\s*\\(",Pattern.MULTILINE);
    private ExecutionEngine executionEngine;
    private final Index index;

    public CypherQueryExecutor(GraphDatabaseService gdb, Index index) {
        this.index = index;
        executionEngine = new ExecutionEngine(gdb, StringLogger.SYSTEM);
    }

    public boolean isMutatingQuery(String query) {
        return query.matches("(?is).*\\b(create|relate|delete|set)\\b.*");
    }
    public boolean isCypherQuery(String query) {
        return query.matches("(?is).*\\b(start|match|return|where|skip|limit|create|relate|delete|set)\\b.*");
    }

    public static class CypherResult implements Iterable<Map<String, Object>> {
        private final List<String> columns;
        private String text;
        private final Collection<Map<String, Object>> rows;
        private QueryStatistics queryStatistics;
        private final long time;

        public CypherResult(List<String> columns, Collection<Map<String, Object>> rows, QueryStatistics queryStatistics, long time) {
            this.columns = columns;
            this.queryStatistics = queryStatistics;
            this.time = time;
            this.rows = rows;
        }

        public List<String> getColumns() {
            return columns;
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
            if (text==null) {
                text = generateText();
            }
            return text;
        }

        private String generateText() {
            return new ResultPrinter().generateText(columns,rows,time,queryStatistics);
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
            final List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> row : this) {
                final LinkedHashMap<String, Object> newRow = new LinkedHashMap<String, Object>();
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
                final Map<String, Object> result = SubGraph.toMap(node);
                result.put("_id",node.getId());
                return result;
            }
            if (value instanceof Relationship) {
                final Relationship relationship = (Relationship) value;
                final Map<String, Object> result = SubGraph.toMap(relationship);
                result.put("_id",relationship.getId());
                result.put("_start",relationship.getStartNode().getId());
                result.put("_end",relationship.getEndNode().getId());
                result.put("_type",relationship.getType().name());
                return result;
            }
            if (value instanceof Iterable) {
                final List<Object> result = new ArrayList<Object>();
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

    public CypherResult cypherQuery(String query, String version) {
        query = replaceIndex(query);
        if (version==null || version.isEmpty()) return cypherQuery(query);
        return cypherQuery("CYPHER "+version+" "+query);
    }

    private CypherResult cypherQuery(String query) {
        if (isMutatingQuery(query)) {
            registerProperties(query);
        }
        query = removeSemicolon( query );
        long time=System.currentTimeMillis();
        final ExecutionResult result = executionEngine.execute(query);
        final Collection<Map<String, Object>> data = IteratorUtil.asCollection(result);
        time=System.currentTimeMillis()-time;
        return new CypherResult(result.columns(), data, result.getQueryStatistics(),time);
    }

    private String removeSemicolon( String query )
    {
        if (query.trim().endsWith( ";" )) return query.substring( 0,query.lastIndexOf( ";" ) );
        return query;
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
        final Set<String> properties = new HashSet<String>();
        while (matcher.find()) {
            if (matcher.group(2)!=null) properties.add(matcher.group(2));
            if (matcher.group(3)!=null) properties.add(matcher.group(3));
        }
        return properties;
    }
}
