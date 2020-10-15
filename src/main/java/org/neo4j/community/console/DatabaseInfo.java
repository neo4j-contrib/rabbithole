package org.neo4j.community.console;

/**
* @author mh
* @since 05.06.12
*/
public class DatabaseInfo {

    private final boolean sandbox;
    private final String database;

    public DatabaseInfo(String database, boolean sandbox) {
        this.database = database;
        this.sandbox = sandbox;
    }

    public static DatabaseInfo expose(String database) {
        return new DatabaseInfo(database,false);
    }

    public static DatabaseInfo sandbox() {
        return new DatabaseInfo(null,true);
    }

    public String getDatabase() {
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
