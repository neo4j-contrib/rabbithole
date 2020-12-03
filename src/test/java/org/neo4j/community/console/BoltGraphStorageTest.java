package org.neo4j.community.console;

import org.junit.*;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.testcontainers.containers.Neo4jContainer;

import static org.junit.Assert.*;

/**
 * @author mh
 * @since 30.05.12
 */
public class BoltGraphStorageTest {

    private static final Neo4jContainer neo4j = new Neo4jContainer("latest").withEnterpriseEdition().withAdminPassword("test");

    private GraphStorage storage;
    private Driver driver;

    @BeforeClass
    public static void before() {
        neo4j.start();
    }

    @AfterClass
    public static void after() throws Exception {
        neo4j.stop();
    }

    @Before
    public void setUp() throws Exception {
        driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.basic("neo4j","test"));
        driver.session().run("MATCH (n) DETACH DELETE n").consume();
        storage = new BoltGraphStorage(driver,"neo4j");
    }

    @After
    public void tearDown() {
        driver.close();
    }

    private Node find(String id) {
        return driver.session().run("OPTIONAL MATCH (n:Graph {id:$id} RETURN n", Values.parameters("id",id)).single().get("n").asNode();
    }

    @Test
    public void testUpdate() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(Util.randomId(), "init", "query", "message"));
        final GraphInfo info2 = info.newQuery("query2");
        storage.update(info2);
            final Node node = find(info.getId());
            assertNotNull(node);
            assertEquals("query2", node.get("query").asString());
            assertEquals(info.getId(), node.get("id").asString());
            assertEquals(info.getInit(), node.get("init").asString());
            assertEquals(info.getMessage(),node.get("message").asString());
            delete(node);

            assertNull(storage.find(info.getId()));
    }
    
    @Test
    public void testCreateWithVersion() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(Util.randomId(), "init", "query", "message","version"));
        final Node node = find(info.getId());
        assertNotNull(node);
        assertEquals(info.getId(), node.get("id").asString());
        assertEquals(info.getVersion(),node.get("version").asString());
    }

    @Test
    public void testCreateWithNoRoot() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(Util.randomId(), "init", "query", "message","version",true));
        final Node node = find(info.getId());
        assertNotNull(node);
        assertEquals(info.getId(), node.get("id").asString());
        assertEquals(info.getVersion(),node.get("version").asString());
        assertEquals(!info.hasRoot(), node.get("no_root").asBoolean());
    }

    @Test
    public void testCreateWithNullId() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo(null, "init", "query", "message"));
        assertNotNull(info.getId());
        final Node node = find(info.getId());
        assertNotNull(node);
        assertEquals(info.getId(), node.get("id").asString());
    }

    @Test
    public void testCreateWithEmptyId() throws Exception {
        final String id = " ";
        final GraphInfo info = storage.create(new GraphInfo(id, "init", "query", "message"));
        assertNotNull(info.getId());
        assertNotSame(id,info.getId());
        assertFalse(info.getId().trim().isEmpty());
        final Node node = find(info.getId());
        assertNull(find(id));
        assertNotNull(node);
        assertEquals(info.getId(), node.get("id").asString());
    }

    @Test
    public void testCreateWithIdWithSpace() throws Exception {
        final String id = "";
        final GraphInfo info = storage.create(new GraphInfo(id, "init", "query", "message"));
        assertNotNull(info.getId());
        assertNotSame(id,info.getId());
        assertFalse(info.getId().trim().isEmpty());
        final Node node = find(info.getId());

        assertNull(find(id));
        assertNotNull(node);
        assertEquals(info.getId(), node.get("id").asString());
    }

    private void delete(Node node) {
        driver.session().run("MATCH (n) WHERE id(n) = $id DELETE n", Values.parameters("id",node.id())).consume();
    }

    @Test
    public void testCreate() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo("id", "init", "query", "message"));
            assertNotNull(info);
            final Node node = find(info.getId());
            assertNotNull(node);
            assertEquals("query", node.get("query").asString());
            delete(node);
    }
    @Test
    public void testDelete() throws Exception {
        final GraphInfo info = storage.create(new GraphInfo("id", "init", "query", "message"));
        storage.delete(info.getId());
            final Node node = find(info.getId());
            assertNull(node);
    }
}
