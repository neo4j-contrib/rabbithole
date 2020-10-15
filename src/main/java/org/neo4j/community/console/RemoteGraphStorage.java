package org.neo4j.community.console;

import org.neo4j.driver.*;

import java.util.Map;

/**
 * @author mh
 * @since 30.05.12
 */
public class RemoteGraphStorage implements GraphStorage {

    private final Driver driver;
    private final String db;

    public RemoteGraphStorage(Driver driver, String db) {
        this.driver = driver;
        this.db = db;
    }

    @Override
    public GraphInfo find(String id) {
        Map map = query("MATCH (n:Graph {id:$id}) set n.count = coalesce(n.count,0)+1, n.time={time} return properties(n) as n", MapUtil.map("id", id, "time", System.currentTimeMillis()));
        return map == null ? null : new GraphInfo(map);
    }

    private Map query(String query, Map<String, Object> map) {
        try (Session session = driver.session(SessionConfig.builder().withDatabase(db).build())) {
            Result rs = session.run(query, map);
            if (rs.hasNext()) return rs.next().get("n").asMap();
            return null;
        }
    }

    @Override
    public void update(GraphInfo info) {
        query("MATCH (n:Graph {id:$id) set n.query=$query, n.init=$init, n.message=$message", info.toMap());
    }

    @Override
    public GraphInfo create(GraphInfo info) {
        if (isEmpty(info)) info = info.withId(Util.randomId());
        Map node = query("MERGE (n:Graph {id:$id}) ON CREATE SET n += $data RETURN properties(n) as n", MapUtil.map("id", info.getId(), "data", info.toMap()));
        return new GraphInfo(node);
    }

    private boolean isEmpty(GraphInfo info) {
        return info.getId() == null || info.getId().trim().isEmpty();
    }

    @Override
    public void delete(String id) {
        query("MATCH (n:Graph {id:$id}) DELETE n", MapUtil.map("id", id));
    }
}
