package org.neo4j.community.console;

import java.sql.*;
import java.util.Map;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 30.05.12
 */
public class RestGraphStorage implements GraphStorage {

    private Connection conn;

    public RestGraphStorage(String uri, String login, String password) {
        String jdbc = "jdbc:neo4j:" + uri;
        try {
            conn = DriverManager.getConnection(jdbc, login, password);
        } catch (SQLException e) {
            throw new RuntimeException("Error connecting to "+jdbc,e);
        }
    }


    @Override
    public GraphInfo find(String id) {
        Map map = query("MATCH (n:Graph {id:{id}}) set n.count = coalesce(n.count,0)+1, n.time={time} return n", map("id", id, "time", System.currentTimeMillis()));
        return map == null ? null : new GraphInfo(map);
    }

    private Map query(String query, Map<String, Object> map) {
        try {
            int idx = 1;
            Object[] params = new Object[map.size() + 1];
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                query = query.replace("{" + entry.getKey() + "}", "{" + idx + "}");
                params[idx] = entry.getValue();
                idx++;
            }
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                for (idx = 1; idx < params.length; idx++) {
                    ps.setObject(idx, params[idx]);
                }
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return (Map) rs.getObject("n");
                return null;
            }
        } catch(SQLException e) {
            throw new RuntimeException("Error executing "+query+"\nwith "+map,e);
        }
    }

    @Override
    public void update(GraphInfo info) {
        query("MATCH (n:Graph {id:{id}}) set n.query={query}, n.init={init}, n.message={message}", info.toMap());
    }

    @Override
    public GraphInfo create(GraphInfo info) {
        if (isEmpty(info)) info = info.withId(Util.randomId());
        Map node = query("MERGE (n:Graph {id:{id}}) ON CREATE SET n += {data} RETURN n",map("id",info.getId(),"data",info.toMap()));
        return new GraphInfo(node);
    }

    private boolean isEmpty(GraphInfo info) {
        return info.getId()==null || info.getId().trim().isEmpty();
    }

    @Override
    public void delete(String id) {
        query("MATCH (n:Graph {id:{id}}) DELETE n", map("id", id));
    }
}
