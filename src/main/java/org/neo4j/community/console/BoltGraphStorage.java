package org.neo4j.community.console;

import org.neo4j.driver.v1.*;

import java.util.Map;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 30.05.12
 */
public class BoltGraphStorage implements GraphStorage {

    private Driver driver;

    public BoltGraphStorage(String uri, String login, String password) {
        AuthToken authToken = password == null ? AuthTokens.none() : AuthTokens.basic(login, password);
        driver = org.neo4j.driver.v1.GraphDatabase.driver(uri, authToken, Config.defaultConfig());
    }


    @Override
    public GraphInfo find(String id) {
        Map map = query("MATCH (n:Graph {id:{id}}) set n.count = coalesce(n.count,0)+1, n.time={time} return n", map("id", id, "time", System.currentTimeMillis()));
        return map == null ? null : new GraphInfo(map);
    }

    private Map query(String query, Map<String, Object> map) {
        try (Session session = driver.session()) {
            StatementResult rs = session.run(query, map);
            if (rs.hasNext()) return rs.single().get("n").asMap();
            return null;
        }
    }

    @Override
    public void update(GraphInfo info) {
        query("MATCH (n:Graph {id:{id}}) set n.query={query}, n.init={init}, n.message={message}", info.toMap());
    }

    @Override
    public GraphInfo create(GraphInfo info) {
        if (isEmpty(info)) info = info.withId(Util.randomId());
        Map node = query("MERGE (n:Graph {id:{id}}) ON CREATE SET n = {data} RETURN n", map("id", info.getId(), "data", info.toMap()));
        return new GraphInfo(node);
    }

    private boolean isEmpty(GraphInfo info) {
        return info.getId() == null || info.getId().trim().isEmpty();
    }

    @Override
    public void delete(String id) {
        query("MATCH (n:Graph {id:{id}}) DELETE n", map("id", id));
    }
}
