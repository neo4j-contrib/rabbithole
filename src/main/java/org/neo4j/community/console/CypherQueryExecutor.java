package org.neo4j.community.console;

import org.neo4j.cypher.PipeExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.IteratorWrapper;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mh
 * @since 21.04.12
 */
public class CypherQueryExecutor {
    private static Method createTimedResults = getMethod(PipeExecutionResult.class, "createTimedResults");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("((\\w+)\\s*:|\\w+\\.(\\w+)\\s*=)",Pattern.MULTILINE|Pattern.DOTALL);
    private org.neo4j.cypher.ExecutionEngine executionEngine;
    private final Index index;

    public CypherQueryExecutor(GraphDatabaseService gdb, Index index) {
        this.index = index;
        executionEngine = new org.neo4j.cypher.ExecutionEngine(gdb);
    }

    public boolean isMutatingQuery(String query) {
        return query.matches("(?is).*\\b(create|relate|delete|set)\\b.*");
    }
    public boolean isCypherQuery(String query) {
        return query.matches("(?is).*\\b(start|match|return|where|skip|limit|create|relate|delete|set)\\b.*");
    }

    public static class CypherResult implements Iterable<Map<String, Object>> {
        private final List<String> columns;
        private final String text;
        private final Collection<Map<String, Object>> rows;

        public CypherResult(scala.collection.immutable.List<String> columns, String text, scala.collection.immutable.List<scala.collection.immutable.Map<String, Object>> rows) {
            this(JavaConversions.seqAsJavaList(columns),text,JavaConversions.asJavaIterable(rows));
        }

        public CypherResult(java.util.List<String> columns, String text, Iterable<scala.collection.immutable.Map<String, Object>> rows) {
            this.columns = columns;
            this.text = text;
            this.rows = IteratorUtil.addToCollection(iterate(rows), new ArrayList<Map<String, Object>>());
        }

        public List<String> getColumns() {
            return columns;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return getText();
        }

        @Override
        public Iterator<Map<String, Object>> iterator() {
            return rows.iterator();
        }

        public Iterator<Map<String, Object>> iterate(Iterable<scala.collection.immutable.Map<String, Object>> rows) {
            return new IteratorWrapper<Map<String, Object>, scala.collection.immutable.Map<String, Object>>(rows.iterator()) {
                @Override
                protected Map<String, Object> underlyingObjectToObject(scala.collection.immutable.Map<String, Object> row) {
                    return JavaConversions.mapAsJavaMap(row);
                }
            };
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
    }

    public CypherResult cypherQuery(String query, String version) {
        if (version==null || version.isEmpty()) return cypherQuery(query);
        return cypherQuery("CYPHER "+version+" "+query);
    }

    private CypherResult cypherQuery(String query) {
        if (isMutatingQuery(query)) {
            registerProperties(query);
        }
        query = removeSemicolon( query );
        org.neo4j.cypher.PipeExecutionResult result = (org.neo4j.cypher.PipeExecutionResult) executionEngine.execute(query);
        Tuple2<scala.collection.immutable.List<scala.collection.immutable.Map<String, Object>>, String> timedResults = createTimedResults(result);
        return new CypherResult(result.columns(), result.dumpToString(), timedResults._1());
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

    @SuppressWarnings("unchecked")
    private Tuple2<scala.collection.immutable.List<scala.collection.immutable.Map<String, Object>>, String> createTimedResults(PipeExecutionResult result) {
        try {
            return (Tuple2<scala.collection.immutable.List<scala.collection.immutable.Map<String, Object>>, String>) createTimedResults.invoke(result);
        } catch (Exception e) {
            Throwable root = e.getCause();
            while (root.getCause() != null) {
                root = root.getCause();
            }
            if(root != null) {
                throw new RuntimeException(root); 
            } else {
                throw new RuntimeException("Unable to extract cypher results", e);
            }
        }
    }

    private static Method getMethod(Class<?> type, String methodName) {
        try {
            final Method method = type.getMethod(methodName);
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
