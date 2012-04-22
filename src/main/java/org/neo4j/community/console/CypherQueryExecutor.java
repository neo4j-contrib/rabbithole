package org.neo4j.community.console;

import org.neo4j.cypher.PipeExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
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

    public static class CypherResult implements Iterable<Map<String, Object>> {
        private final List<String> columns;
        private final String text;
        private final Iterable<scala.collection.immutable.Map<String, Object>> rows;

        public CypherResult(scala.collection.immutable.List<String> columns, String text, scala.collection.immutable.List<scala.collection.immutable.Map<String, Object>> rows) {
            this.columns = JavaConversions.seqAsJavaList(columns);
            this.text = text;
            this.rows = JavaConversions.asJavaIterable(rows);
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
            return new IteratorWrapper<Map<String, Object>, scala.collection.immutable.Map<String, Object>>(this.rows.iterator()) {
                @Override
                protected Map<String, Object> underlyingObjectToObject(scala.collection.immutable.Map<String, Object> row) {
                    return JavaConversions.mapAsJavaMap(row);
                }
            };
        }
    }

    public CypherResult cypherQuery(String query) {
        if (isMutatingQuery(query)) {
            registerProperties(query);
        }
        org.neo4j.cypher.PipeExecutionResult result = (org.neo4j.cypher.PipeExecutionResult) executionEngine.execute(query);
        Tuple2<scala.collection.immutable.List<scala.collection.immutable.Map<String, Object>>, String> timedResults = createTimedResults(result);
        return new CypherResult(result.columns(), result.dumpToString(), timedResults._1());
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
            throw new RuntimeException("Error extracting cypher results", e);
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
