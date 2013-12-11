package org.neo4j.community.console;

/**
 * @author mh
 * @since 11.12.13
 */
public interface GraphStorage {
    GraphInfo find(String id);

    void update(GraphInfo info);

    GraphInfo create(GraphInfo info);

    void delete(String id);
}
