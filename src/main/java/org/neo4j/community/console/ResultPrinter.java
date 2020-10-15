package org.neo4j.community.console;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class ResultPrinter {

    public String generateText(CypherQueryExecutor.CypherResult result, long time)  {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        outputResults(result,time,out);
        return sw.toString();
    }

    public void outputResults(CypherQueryExecutor.CypherResult result, long time, PrintWriter out)  {
        Collection<Map<String, Object>> rows = result.getRows().stream().map(Record::asMap).collect(Collectors.toList());

        List<String> columns = result.getColumns();
        Map<String, Integer> columnSizes = calculateColumnSizes(columns, rows);
        int totalWidth = totalWith(columnSizes.values());

        final boolean hasData = !columns.isEmpty();
        if (hasData) {
            String _________ = "+" + repeat('-', totalWidth) + "+";

            out.println(_________);
            out.println(row(columns, columnSizes));
            out.println(_________);
            for (Map<String, Object> row : rows) {
                out.println(row(columns, columnSizes, row));
            }
            out.println(_________);
        }
        out.println(rowsTime(time, rows.size()));
        final String stats = info(result.getQueryStatistics(), hasData);
        if (!stats.isEmpty()) {
            out.println(stats);
        }
    }

    private String rowsTime(long time, int rowCount) {
        return rowCount + (rowCount == 1 ? " row" : " rows") + "\n" + time + " ms";
    }

    private String row(List<String> columns, Map<String, Integer> columnSizes) {
        StringBuilder sb = new StringBuilder("| ");
        final int size = columns.size();
        for (int i = 0; i < size; i++) {
            String column = columns.get(i);
            Integer width = columnSizes.get(column);
            String padding = repeat(' ', width - column.length() - 1);
            sb.append(column).append(padding).append("|");
            if (i < size - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private String row(List<String> columns, Map<String, Integer> columnSizes, Map<String, Object> row) {
        StringBuilder sb = new StringBuilder("| ");
        final int size = columns.size();
        for (int i = 0; i < size; i++) {
            String column = columns.get(i);
            Integer width = columnSizes.get(column);
            String text = text(row.get(column));
            String padding = repeat(' ', width - text.length() - 1);
            sb.append(text).append(padding).append("|");
            if (i < size - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private String repeat(char s, int width) {
        if (width < 0) {
            return "";
        }
        char[] chars = new char[width];
        Arrays.fill(chars, s);
        return String.valueOf(chars);
    }

    private int totalWith(Collection<Integer> values) {
        int sum = values.size() - 1; // borders
        for (Integer value : values) {
            sum += value;
        }
        return sum;
    }

    private Map<String, Integer> calculateColumnSizes(List<String> columns, Collection<Map<String, Object>> rows) {
        Map<String, Integer> sizes = new HashMap<>();
        for (String column : columns) {
            sizes.put(column, column.length() + 2);
        }
        for (Map<String, Object> row : rows) {
            for (String column : columns) {
                sizes.put(column, Math.max(sizes.get(column), text(row.get(column)).length() + 2));
            }
        }
        return sizes;
    }

    private String text(Object value) {
        if (value == null) {
            return "<null>";
        }
        if (value instanceof Value) {
            Value v = (Value) value;
            if (v.isNull()) {
                return "<null>";
            }
            if (value instanceof Node) {
                return value.toString() + props((Entity) value);
            }
            if (value instanceof Relationship) {
                Relationship rel = (Relationship) value;
                return ":" + rel.type() + "[" + rel.id() + "] " + props(rel);
            }
            value = v.asObject();
        }
        // TODO
        if (value instanceof Iterable) {
            return formatIterator(((Iterable) value).iterator());
        }
        if (value.getClass().isArray()) {
            return formatArray(value);
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        return value.toString();
    }

    private String formatArray(Object array) {
        final StringBuilder sb = new StringBuilder("[");
        final int size = Array.getLength(array);
        for (int i = 0; i < size; i++) {
            sb.append(text(Array.get(array, i)));
            if (i < size - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatIterator(Iterator it) {
        final StringBuilder sb = new StringBuilder("[");
        while (it.hasNext()) {
            sb.append(text(it.next()));
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String props(Entity pc) {
        final StringBuilder sb = new StringBuilder("{");
        final Iterator<String> keys = pc.keys().iterator();
        while (keys.hasNext()) {
            String prop = keys.next();
            sb.append(prop).append(":");
            final Value value = pc.get(prop);
            sb.append(text(value));
            if (keys.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String info(SummaryCounters queryStatistics, boolean hasData) {
        boolean hasStatistics = queryStatistics != null && queryStatistics.containsUpdates();
        if (hasData) {
            if (hasStatistics) {
                return toString(queryStatistics);
            }
            return "";
        } else {
            if (hasStatistics) {
                return
                        "+-------------------+\n" +
                        "| No data returned. |\n" +
                        "+-------------------+\n" +
                                toString(queryStatistics);
            } else {
                return
                    "+--------------------------------------------+\n" +
                    "| No data returned, and nothing was changed. |\n" +
                    "+--------------------------------------------+\n";
            }

        }
    }

    private String toString(SummaryCounters queryStatistics) {
        if (!queryStatistics.containsUpdates()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        addIfNonZero(sb, "Nodes created: ",
                queryStatistics.nodesCreated());
        addIfNonZero(sb, "Relationships created: ",
                queryStatistics.relationshipsCreated());
        addIfNonZero(sb, "Properties set: ",
                queryStatistics.propertiesSet());
        addIfNonZero(sb, "Nodes deleted: ",
                queryStatistics.nodesDeleted());
        addIfNonZero(sb, "Relationships deleted: ",
                queryStatistics.relationshipsDeleted());

        return sb.toString();
    }

    private void addIfNonZero(StringBuilder builder, String message, int count) {
        if (count > 0) {
            builder.append(message).append(count).append("\n");
        }
    }
}
