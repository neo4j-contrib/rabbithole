package org.neo4j.community.console;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MapUtil {
        public static Map<String, Object> map(Object...keyValue) {
            if (keyValue==null || keyValue.length == 0) return new HashMap<>();
            if (keyValue.length % 2 != 0) throw new RuntimeException("Map needs even keys and values but has "+keyValue.length+": "+ Arrays.toString(keyValue));
            HashMap<String, Object> result = new HashMap<>(keyValue.length);
            for (int i=0;i<keyValue.length;i+=2) {
                result.put(String.valueOf(keyValue[i]), keyValue[i+1]);
            }
            return result;
    }
}
