package org.neo4j.community.console;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import spark.Request;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Map;
import java.util.Scanner;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
* @author mh
* @since 30.05.12
*/
public class ConsoleService {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ConsoleService.class);

    static final String DEFAULT_GRAPH_CYPHER =
            "create " +
                    "(Neo:Crew {name:'Neo'}), " +
                    "(Morpheus:Crew {name: 'Morpheus'}), " +
                    "(Trinity:Crew {name: 'Trinity'}), " +
                    "(Cypher:Crew:Matrix {name: 'Cypher'}), " +
                    "(Smith:Matrix {name: 'Agent Smith'}), " +
                    "(Architect:Matrix {name:'The Architect'}),\n" +
                    "(Neo)-[:KNOWS]->(Morpheus), " +
                    "(Neo)-[:LOVES]->(Trinity), " +
                    "(Morpheus)-[:KNOWS]->(Trinity),\n" +
                    "(Morpheus)-[:KNOWS]->(Cypher), " +
                    "(Cypher)-[:KNOWS]->(Smith), " +
                    "(Smith)-[:CODED_BY]->(Architect)";

    static final String DEFAULT_QUERY = "match (n:Crew)-[r:KNOWS*]-(m) where n.name='Neo' return n as Neo,r,m";

    private GraphStorage storage;

    public ConsoleService() {
        createGraphStorage();
    }

    private void createGraphStorage() {
        try {
            System.setProperty("org.neo4j.rest.read_timeout", "5");
            System.setProperty("org.neo4j.rest.connect_timeout", "10");
            String restUrlVar = System.getenv("NEO4J_REST_URL_VAR");
            if (restUrlVar == null) restUrlVar = "NEO4J_URL";
            String restUrl = System.getenv(restUrlVar);

            String login = null, password = null;
            if (restUrl != null) {
                login = System.getenv("NEO4J_LOGIN");
                password = System.getenv("NEO4J_PASSWORD");
                if (login == null) {
                    try {
                        URL url = new URL(restUrl);
                        String userInfo = url.getUserInfo();
                        if (userInfo != null) {
                            login = userInfo.split(":")[0];
                            password = userInfo.split(":")[1];
                        }
                    } catch (MalformedURLException e) {
                        throw new RuntimeException("Invalid Neo4j-Server-URL " + restUrl);
                    }
                }

                if (restUrl.contains("/db/data")) restUrl = restUrl.replace("/db/data", "");
                if (restUrl.startsWith("bolt")) storage = new BoltGraphStorage(restUrl, login, password);
                log("Graph Storage " + restUrl + " login " + login + " " + password + " " + storage);
            }
        } catch (Exception e) {
            LOG.error("Error creating graph storage", e);
        }
        if (storage == null) {
            storage = new MemoryGraphStorage();
        }
        log("Graph Storage " + storage);
    }

    private void log(String msg) {
        LOG.warn(msg);
    }

    // split init and query on ";\n"
    public Map<String, Object> execute(Neo4jService service, String init, String query, String version, Map<String, Object> requestParams, Map<String, Object> queryParams) {
        if (version != null) service.setVersion(version);
        boolean initial = init != null || mustInitialize(requestParams);
        if (!mustInitialize(requestParams)) {
            if (dontInitialize(service) || init == null || init.equalsIgnoreCase("none")) {
	          init = null; initial = false;
	        }
        }
        if (query == null || query.equalsIgnoreCase("none")) query = null;
        final Map<String, Object> data = map("init", init, "query", query, "version", service.getVersion());
        long start = System.currentTimeMillis(), time = start;
        try {
            time = trace("service", time);
            if (initial) service.clear();
            if (initial) {
                final URL url = service.toUrl(init);
                if (url != null) {
                    initFromUrl(service, url, "match (n) optional match (n)-[r]->() return n,r");
                } else if (service.isMutatingQuery(init)) {
                    for (String q : splitQuery(init)) {
                        service.initCypherQuery(q, queryParams);
                    }
                }
            }
            if (initial) {
                service.setInitialized();
            }
            time = trace("graph", time);
            CypherQueryExecutor.CypherResult result = null;
            if (query != null) {
                for (String q : splitQuery(query)) {
                    result = service.cypherQuery(q, queryParams);
                }
                data.put("result", result.getText());
                data.put("json", result.getJson());
                data.put("plan", result.getPlan().toString());
                data.put("columns", result.getColumns());
                data.put("stats", result.getQueryStatistics());
                String pretty = service.prettify(query);
                if (pretty != null) data.put("query", pretty);
            }
            time = trace("cypher", time);
            if (!noViz(requestParams)) {
                data.put("visualization", service.cypherQueryViz(result));
            }
            trace("viz", time);
        } catch (Exception e) {
            e.printStackTrace();
            data.put("error", e.toString());
        }
        time = trace("all", start);
        data.put("time", time - start);
        return data;
    }

    private boolean noViz(Map<String, Object> params) {
        return params != null && "none".equals(params.get("viz"));
    }
    private boolean mustInitialize(Map<String, Object> params) {
        return params != null && "true".equals(params.get("initialize"));
    }

    private String[] splitQuery(String allQueries) {
        return allQueries.split(";\n");
    }

    private boolean dontInitialize(Neo4jService service) {
        return !service.doesOwnDatabase() || service.isInitialized();
    }

    protected long trace(String msg, long time) {
        long now = System.currentTimeMillis();
        log("## " + msg + " took: " + (now - time) + " ms.");
        return now;
    }


    public String shortenUrl(String uri) {
        try {
            final InputStream stream = (InputStream) new URL("http://tinyurl.com/api-create.php?url=" + URLEncoder.encode(uri, "UTF-8")).getContent();
            final String shortUrl = new Scanner(stream).useDelimiter("\\z").next();
            stream.close();
            return shortUrl;
        } catch (IOException ioe) {
            return null;
        }
    }

    public Map<String, Object> execute(Neo4jService service, GraphInfo info, Map<String, Object> params, Map<String, Object> queryParams) {
        final Map<String, Object> result = this.execute(service, info.getInit(), info.getQuery(), info.getVersion(), params, queryParams);
        result.put("message", info.getMessage());
        return result;
    }

    protected String param(Map input, String param, String defaultValue) {
        if (input == null) return defaultValue;
        String data = (String) input.get(param);
        if (data == null || data.isEmpty()) {
            data = defaultValue;
        } else {
            log(param + ": " + data);
        }
        return data;
    }

    public Map<String, Object> init(Neo4jService service, Map<String, Object> input) {
        input.put("init", param(input, "init", DEFAULT_GRAPH_CYPHER));
        input.put("query", param(input, "query", DEFAULT_QUERY));
        return execute(service, GraphInfo.from(input), input, (Map) input.get("queryParams"));
    }

    protected String baseUri(HttpServletRequest request, String query, final String path) {
        final String requestURI = request.getRequestURL().toString();
        try {
            final URI uri = new URI(requestURI);
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), path, query, null).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error parsing URI from " + requestURI + " query " + query);
        }
    }

    protected String encodeParam(String param, String value) {
        if (value == null || value.trim().isEmpty()) return "";
        try {
            return param + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return param + "=" + URLEncoder.encode(value);
        }
    }

    public String createInitialUri(Request request, GraphInfo info) {
        return baseUri(request.raw(),
                encodeParam("init", info.getInit()) +
                        encodeParam("query", info.getQuery()) +
                        encodeParam("version", info.getVersion()) +
                        encodeParam("message", info.getMessage()) +
                        encodeParam("no_root", info.hasRoot() ? null : "true"),
                null);
    }

    Map<String, Object> init(Neo4jService service, String id, Map<String, Object> params) {
        final GraphInfo info = storage.find(id);
        Map<String, Object> result;
        if (info != null) {
            Map<String, Object> queryParams = params != null ? (Map<String, Object>) params.get("queryParams") : null;
            result = execute(service, info.getInit(), info.getQuery(), info.getVersion(), params, queryParams);
            service.setId(id);
            result.put("message", info.getMessage());
        } else {
            result = init(service, params);
            result.put("error", "Graph not found for id " + id + " rendering default");
        }
        return result;
    }

    public Object share(Request request, Map input) {
        final GraphInfo info = GraphInfo.from(input);
        try {
            if (storage != null) return storage.create(info).getId();
        } catch (Exception e) {
            log("Error storing shared data " + info);
            e.printStackTrace();
        }
        final String uri = createInitialUri(request, info);
        return shortenUrl(uri);
    }

    public Map<String, Object> save(String id, String init) {
        if (storage==null) {
            return map("error","no storage configured");
        }
        GraphInfo existingState = storage.find(id);
        GraphInfo info = new GraphInfo(id, init, "none", "none").noRoot();
        Map<String, Object> result = info.toMap();
        if (existingState==null) {
            GraphInfo newInfo = storage.create(info);
            if (newInfo == null) result.put("error","error during create");
            else result = newInfo.toMap();
            result.put("action","create");
        } else {
            storage.update(info);
            result.put("action", "update");
        }
        return result;
    }

    public boolean delete(String id) {
        try {
            if (storage != null) {
                storage.delete(id);
                return true;
            }
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
        return false;
    }

    public void initFromUrl(Neo4jService service, URL url, final String query) {
        if (!service.doesOwnDatabase()) return;
        final String urlString = url.toString().replaceAll("/cypher/?$", "");
//        final RestAPI restApi = new RestAPIFacade(urlString);
//        final QueryResult<Map<String,Object>> cypherResult = new RestCypherQueryEngine(restApi).query(query, null);
//        final SubGraph graph = new SubGraph();
//        for (Map<String, Object> row : cypherResult) {
//            for (Object value : row.values()) {
//                addResultValue(graph, value);
//            }
//        }
//        service.importGraph(graph);
    }

    private void addResultValue(SubGraph subGraph, Object value) {
        if (value instanceof Node) {
            subGraph.add((Node) value);
        }
        if (value instanceof Relationship) {
            subGraph.add((Relationship) value);
        }
        if (value instanceof Iterable) {
            for (Object inner : (Iterable) value) {
                addResultValue(subGraph, inner);
            }
        }
    }

    private <T> T post(URL url, Map<String, Object> data, Class<T> resultType) {
        try {
            final HttpClient client = clientFor(url);
            final HttpPost post = new HttpPost(url.toString());
            post.setHeader("Accept", "application/json;stream=true");
            final Gson gson = new Gson();
            final String postData = gson.toJson(data);
            post.setEntity(new StringEntity(postData, "application/json", "UTF-8"));
            HttpResponse response = client.execute(post);
            final int status = (int) response.getStatusLine().getStatusCode();
            if (status != 200)
                throw new RuntimeException("Return Status Code " + status + " " + response.getStatusLine());
            return gson.fromJson(new InputStreamReader(response.getEntity().getContent()), resultType);
        } catch (Exception e) {
            throw new RuntimeException("Error executing request to " + url + " with " + data + ":" + e.getMessage());
        }
    }

    private HttpClient clientFor(URL url) {
        HttpClientBuilder builder = HttpClients.custom();
        final String userInfo = url.getUserInfo();
        if (userInfo != null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            final String[] usernamePassword = userInfo.split(":");
            credsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()),
                    new UsernamePasswordCredentials(usernamePassword[0], usernamePassword[1]));
            builder = builder.setDefaultCredentialsProvider(credsProvider);
        }
        return builder.build();
    }
}
