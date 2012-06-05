package org.neo4j.community.console;

import org.neo4j.graphdb.GraphDatabaseService;

/**
* @author mh
* @since 05.06.12
*/
public interface DatabaseFactory {
    GraphDatabaseService create();
}
