package org.neo4j.community.console;

import org.neo4j.cypher.PipeExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.collection.IteratorWrapper;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 21.04.12
 */
public class CypherQueryExecutor {
    private static Method createTimedResults = getMethod(PipeExecutionResult.class, "createTimedResults");
    private org.neo4j.cypher.ExecutionEngine executionEngine;

    public CypherQueryExecutor(GraphDatabaseService gdb) {
        executionEngine = new org.neo4j.cypher.ExecutionEngine(gdb);
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
        org.neo4j.cypher.PipeExecutionResult result = (org.neo4j.cypher.PipeExecutionResult) executionEngine.execute(query);
        Tuple2<scala.collection.immutable.List<scala.collection.immutable.Map<String, Object>>, String> timedResults = createTimedResults(result);
        return new CypherResult(result.columns(), result.dumpToString(), timedResults._1());
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
