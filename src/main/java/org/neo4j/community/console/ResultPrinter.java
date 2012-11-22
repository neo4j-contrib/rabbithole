package org.neo4j.community.console;

import org.neo4j.cypher.javacompat.QueryStatistics;

import java.util.*;

public class ResultPrinter {
    public String generateText(List<String> columns, Collection<Map<String, Object>> rows, long time, QueryStatistics queryStatistics) {
        Map<String,Integer> columnSizes = calculateColumnSizes(columns,rows);
        int totalWidth = totalWith(columnSizes.values());
        String line = "+" + repeat('-', totalWidth)+"+\n";
        return "\n"+ line +
                row(columns, columnSizes) +
                line +
                rows(columns,columnSizes,rows) +
                line +
                rowsTime(time, rows.size()) +
                toString(queryStatistics, !columns.isEmpty());
    }

    private String rows(List<String> columns, Map<String, Integer> columnSizes, Collection<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : rows) {
            sb.append(row(columns,columnSizes,row));
        }
        return sb.toString();
    }

    private String rowsTime(long time, int rowCount) {
        return rowCount + (rowCount == 1 ? " row" : " rows") + " " + time + " ms"+"\n";
    }

    private String row(List<String> columns, Map<String, Integer> columnSizes) {
        StringBuilder sb = new StringBuilder("| ");
        for (String column : columns) {
            Integer width = columnSizes.get(column);
            String text = text(column);
            String padding = repeat(' ', width - text.length()-1);
            sb.append(text).append(padding).append("| ");
        }
        return sb.toString()+"\n";
    }

    private String row(List<String> columns, Map<String, Integer> columnSizes,Map<String,Object> row) {
        StringBuilder sb = new StringBuilder("| ");
        for (String column : columns) {
            Integer width = columnSizes.get(column);
            String text = text(row.get(column));
            String padding = repeat(' ', width - text.length()-1);
            sb.append(text).append(padding).append("| ");
        }
        return sb.toString()+"\n";
    }

    private String repeat(char s, int width) {
        char[] chars = new char[width];
        Arrays.fill(chars,s);
        return String.valueOf(chars);
    }

    private int totalWith(Collection<Integer> values) {
        int sum=0;
        for (Integer value : values) {
            sum += value;
        }
        return sum;
    }

    private Map<String, Integer> calculateColumnSizes(List<String> columns, Collection<Map<String, Object>> rows) {
        Map<String,Integer> sizes=new HashMap<String, Integer>();
        for (String column : columns) {
            sizes.put(column,column.length()+2);
        }
        for (Map<String, Object> row : rows) {
            for (String column : columns) {
                sizes.put(column, Math.max(sizes.get(column), text(row.get(column)).length() + 2));
            }
        }
        return sizes;
    }

    private String text(Object value) {
        return value!=null ? value.toString() : "<null>";
    }

    private String toString(QueryStatistics queryStatistics, boolean hasData) {
        boolean hasStatistics = queryStatistics != null && queryStatistics.containsUpdates();
       if (hasData) {
           if (hasStatistics) return queryStatistics.toString();
           return "";
      } else {
        if (hasStatistics) {
          return
          "+-------------------+"+
          "| No data returned. |"+
          "+-------------------+"+
          queryStatistics.toString();
        } else {
          return
          "+--------------------------------------------+"+
          "| No data returned, and nothing was changed. |"+
          "+--------------------------------------------+";
        }

    }
    }
}
