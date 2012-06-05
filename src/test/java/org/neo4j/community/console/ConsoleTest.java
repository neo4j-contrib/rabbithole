package org.neo4j.community.console;

import org.junit.Ignore;
import org.neo4j.test.ImpermanentGraphDatabase;

/**
 * @author mh
 * @since 05.06.12
 */
@Ignore
public class ConsoleTest {
    public static void main(String[] args) throws Exception {
        final Console console = new Console(DatabaseInfo.expose(new ImpermanentGraphDatabase()));
        console.start(9000);
        console.join();
    }
}
