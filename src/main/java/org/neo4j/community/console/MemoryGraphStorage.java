package org.neo4j.community.console;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mh
 * @since 11.12.13
 */
public class MemoryGraphStorage implements GraphStorage {

    private final Map<String, GraphInfo> data=new HashMap<>();

    @Override
    public GraphInfo find(String id) {
        return data.get(id);
    }

    @Override
    public void update(GraphInfo info) {
        String id = info.getId();
        GraphInfo found = data.get(id);
        if (found==null) create(info);
        else {
            data.put(id, info);
        }
    }

    @Override
    public GraphInfo create(GraphInfo info) {
        data.put(info.getId(),info);
        return info;
    }

    @Override
    public void delete(String id) {
        data.remove(id);
    }
}
