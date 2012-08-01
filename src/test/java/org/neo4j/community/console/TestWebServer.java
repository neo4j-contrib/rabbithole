package org.neo4j.community.console;

import static java.util.Arrays.asList;
import static org.neo4j.server.configuration.Configurator.DEFAULT_DATA_API_PATH;
import static org.neo4j.server.configuration.Configurator.REST_API_PACKAGE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Ignore;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.NeoServer;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.database.Database;
import org.neo4j.server.plugins.Injectable;
import org.neo4j.server.plugins.PluginManager;
import org.neo4j.server.web.Jetty6WebServer;
import org.neo4j.server.web.WebServer;
import org.neo4j.test.ImpermanentGraphDatabase;

/**
* @author mh
* @since 01.06.12
*/
@Ignore
public class TestWebServer extends Jetty6WebServer {
    public TestWebServer(GraphDatabaseService gdb, int port) {
        setPort(port);
        setNeoServer(createNeoServer(gdb, baseUri(port)));
        addJAXRSPackages(asList(REST_API_PACKAGE), DEFAULT_DATA_API_PATH);
        init();
    }

    protected URI baseUri(int port) {
        final String url = "http://localhost:" + port;
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error creating URI "+url);
        }
    }

    protected NeoServer createNeoServer(final GraphDatabaseService gdb, final URI baseUri) {
        return new NeoServer() {
            @Override
            public void init() {

            }

            @Override
            public void start() {

            }

            @Override
            public Configuration getConfiguration() {
                return new BaseConfiguration();
            }

            @Override
            public void stop() {

            }

            @Override
            public Database getDatabase() {
                return new Database((AbstractGraphDatabase) gdb);
            }

            @Override
            public Configurator getConfigurator() {
                return null;
            }

            @Override
            public PluginManager getExtensionManager() {
                return null;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Collection<Injectable<?>> getInjectables(List<String> packageNames) {
                return Arrays.<Injectable<?>>asList(new Injectable<WebServer>() {
                    public WebServer getValue() {
                        return TestWebServer.this;
                    }

                    public Class<WebServer> getType() {
                        return WebServer.class;
                    }
                });
            }

            @Override
            public URI baseUri() {
                return baseUri;
            }
        };
    }

    public static WebServer startWebServer(ImpermanentGraphDatabase gdb, int port) {
        final TestWebServer webServer = new TestWebServer(gdb, port);
        webServer.start();
        return webServer;
    }
}
