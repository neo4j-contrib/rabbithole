package org.neo4j.community.console;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

public class ResultPrinterTest {

    private final ResultPrinter resultPrinter = new ResultPrinter();

    @Test
    public void testEmptyResult() throws Exception {
        List<Map<String,Object>> rows = Collections.emptyList();
        String text = resultPrinter.generateText(asList("foo"), rows, 0, null);
        System.out.println("text = " + text);
    }

    @Test
    public void testSingleRow() throws Exception {
        List<Map<String,Object>> rows = asList(singletonMap("foo", (Object)"barbar"));
        String text = resultPrinter.generateText(asList("foo"), rows, 0, null);
        System.out.println("text = " + text);
    }
}
