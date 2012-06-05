package org.neo4j.community.console;

import org.neo4j.graphdb.GraphDatabaseService;

/**
* @author mh
* @since 05.06.12
*/
public class DatabaseInfo {

    private boolean sandbox;
    private final GraphDatabaseService database;

    public DatabaseInfo(GraphDatabaseService database, boolean sandbox) {
        this.database = database;
        this.sandbox = sandbox;
    }

    public static DatabaseInfo expose(GraphDatabaseService database) {
        return new DatabaseInfo(database,false);
    }

    public static DatabaseInfo sandbox(GraphDatabaseService database) {
        return new DatabaseInfo(database,true);
    }

    public GraphDatabaseService getDatabase() {
        return database;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public boolean shouldCreateNew() {
        return sandbox || database == null;
    }

    public boolean shouldImport() {
        return database!=null && sandbox;
    }

    @Override
    public String toString() {
        return String.format("own-db %s import-db %s source db: %s",shouldCreateNew(), shouldImport(), database);
    }
}
