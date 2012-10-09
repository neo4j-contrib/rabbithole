package org.neo4j.community.console;

import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.neo4j.rest.graphdb.RestAPIFacade;
import org.slf4j.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import spark.Request;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Scanner;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
* @author mh
* @since 30.05.12
*/
public class ConsoleService {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ConsoleService.class);

    private static final String DEFAULT_GRAPH_GEOFF = "(Neo) {\"name\": \"Neo\"} (Morpheus) {\"name\": \"Morpheus\"} " +
            "(Trinity) {\"name\": \"Trinity\"} (Cypher) {\"name\": \"Cypher\"} (Smith) {\"name\": \"Agent Smith\"} " +
            "(Architect) {\"name\":\"The Architect\"} (0)-[:ROOT]->(Neo) (Neo)-[:KNOWS]->(Morpheus) " +
            "(Neo)-[:LOVES]->(Trinity) (Morpheus)-[:KNOWS]->(Trinity) (Morpheus)-[:KNOWS]->(Cypher) " +
            "(Cypher)-[:KNOWS]->(Smith) (Smith)-[:CODED_BY]->(Architect)";
    static final String DEFAULT_GRAPH_CYPHER = 
        "start root=node(0)\n" +
        "create " +
        "(Neo {name:'Neo'}), " +
        "(Morpheus {name: 'Morpheus'}), " +
        "(Trinity {name: 'Trinity'}), " + 
        "(Cypher {name: 'Cypher'}), " + 
        "(Smith {name: 'Agent Smith'}), " +
        "(Architect {name:'The Architect'}),\n" +
        "root-[:ROOT]->Neo, " + 
        "Neo-[:KNOWS]->Morpheus, " +
        "Neo-[:LOVES]->Trinity, " + 
        "Morpheus-[:KNOWS]->Trinity,\n" +
        "Morpheus-[:KNOWS]->Cypher, " +
        "Cypher-[:KNOWS]->Smith, " + 
        "Smith-[:CODED_BY]->Architect";
            
    static final String DEFAULT_QUERY = "start n=node(*) match n-[r?]->m return n,type(r),m";

    private GraphStorage storage;

    public ConsoleService() {
        createGraphStorage();
    }

    private void createGraphStorage() {
        final String restUrl = System.getenv("NEO4J_REST_URL");
        final String login = System.getenv("NEO4J_LOGIN");
        final String password = System.getenv("NEO4J_PASSWORD");
        if (restUrl!=null) {
            final RestAPI api = new RestAPIFacade(restUrl, login, password);
            storage = new GraphStorage(api);
        }
        log("Graph Storage " + restUrl + " login " + login + " " + password + " " + storage);
    }

    private void log(String msg) {
        LOG.warn(msg);
    }

    public Map<String, Object> execute(Neo4jService service, String init, String query, String version) {
        if (version!=null) service.setVersion(version);
        if (dontInitialize(service)) init=null;
        final Map<String, Object> data = map("init", init, "query", query,"version",service.getVersion());
        long start = System.currentTimeMillis(), time = start;
        try {
            time = trace("service", time);
            if (init!=null) {
                final URL url = service.toUrl(init);
                if (url!=null) {
                    initFromUrl(service, url, "start n=node(*) match n-[r?]->() return n,r");
                    data.put("graph",service.exportToGeoff());
                } else if (service.isMutatingQuery(init)) {
                    service.initCypherQuery(init);
                    data.put("graph",service.exportToGeoff());
                } else {
                    data.put("graph", service.mergeGeoff(init));
                }
                service.setInitialized();
            }
            time = trace("graph", time);
            CypherQueryExecutor.CypherResult result = null;
            if (query!=null) {
                result = service.cypherQuery(query);
                data.put("result", result.getText());
                data.put("json", result.getJson());
            }
            time = trace("cypher", time);
            data.put("visualization", service.cypherQueryViz(result));
            trace("viz", time);
        } catch (Exception e) {
            e.printStackTrace();
            data.put("error", e.toString());
        }
        time = trace("all", start);
        data.put("time", time);
        return data;
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

    public Map<String, Object> execute(Neo4jService service, GraphInfo info) {
        if (!info.hasRoot()) {
            service.deleteReferenceNode();
        }
        final Map<String, Object> result = this.execute(service, info.getInit(), info.getQuery(), info.getVersion());
        result.put("message",info.getMessage());
        return result;
    }

    protected String param(Map input, String param, String defaultValue) {
        if (input==null) return defaultValue;
        String data = (String) input.get(param);
        if (data == null || data.isEmpty()) {
            data = defaultValue;
        } else {
            log(param+": "+data);
        }
        return data;
    }

    public Map<String, Object> init(Neo4jService service, Map<String,Object> input) {
        input.put("init",param(input,"init",DEFAULT_GRAPH_CYPHER));
        input.put("query",param(input,"query",DEFAULT_QUERY));
        return execute(service, GraphInfo.from(input));
    }

    protected String baseUri(HttpServletRequest request, String query, final String path) {
        final String requestURI = request.getRequestURL().toString();
        try {
            final URI uri = new URI(requestURI);
            return new URI(uri.getScheme(),null,uri.getHost(),uri.getPort(), path,query,null).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error parsing URI from "+requestURI+" query "+query);
        }
    }

    protected String encodeParam(String param, String value) {
        if (value==null || value.trim().isEmpty()) return "";
        try {
            return param + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return param + "=" + URLEncoder.encode(value);
        }
    }
    public String createInitialUri(Request request, GraphInfo info) {
        return baseUri( request.raw(),
                                encodeParam("init",  info.getInit()) +
                                encodeParam("query", info.getQuery()) +
                                encodeParam("version", info.getVersion()) +
                                encodeParam("message", info.getMessage()) +
                                encodeParam("no_root", info.hasRoot() ? null : "true"),
                                null);
    }

    Map<String, Object> init(Neo4jService service, String id) {
        final GraphInfo info = storage.find(id);
        Map<String, Object> result;
        if (info!=null) {
            result = execute(service, info.getInit(), info.getQuery(), info.getVersion());
            result.put("message",info.getMessage());
        } else {
            result = init(service, map());
            result.put("error","Graph not found for id " + id+ " rendering default");
        }
        return result;
    }

    public Object share(Request request, Map input) {
        final GraphInfo info = GraphInfo.from(input);
        try {
            if (storage!=null) return storage.create(info).getId();
        } catch(Exception e) {
            log("Error storing shared data "+info);
            e.printStackTrace();
        }
        final String uri = createInitialUri(request, info);
        return shortenUrl(uri);
    }

    public void initFromUrl(Neo4jService service, URL url, final String query) {
        if (!service.doesOwnDatabase()) return;
        final String urlString = url.toString().replaceAll("/cypher/?$", "");
        final RestAPI restApi = new RestAPIFacade(urlString);
        final QueryResult<Map<String,Object>> cypherResult = new RestCypherQueryEngine(restApi).query(query, null);
        final SubGraph graph = new SubGraph();
        for (Map<String, Object> row : cypherResult) {
            for (Object value : row.values()) {
                addResultValue(graph, value);
            }
        }
        service.importGraph(graph);
    }

    private void addResultValue(SubGraph subGraph, Object value) {
        if (value instanceof Node) {
            subGraph.add((Node)value);
        }
        if (value instanceof Relationship) {
            subGraph.add((Relationship)value);
        }
        if (value instanceof Iterable) {
            for (Object inner : (Iterable)value) {
                addResultValue(subGraph,inner);
            }
        }
    }

    private <T> T post(URL url, Map<String, Object> data, Class<T> resultType) {
        try {
            final HttpClient client = clientFor(url);
            final PostMethod post = new PostMethod(url.toString());
            post.setDoAuthentication(true);
            post.setRequestHeader("Accept", "application/json;stream=true");
            final Gson gson = new Gson();
            final String postData = gson.toJson(data);
            post.setRequestEntity(new StringRequestEntity(postData, "application/json", "UTF-8"));
            final int status = client.executeMethod(post);
            if (status != 200) throw new RuntimeException("Return Status Code "+post.getStatusCode()+" "+post.getStatusLine());
            return gson.fromJson(post.getResponseBodyAsString(), resultType);
        } catch (Exception e) {
            throw new RuntimeException("Error executing request to "+url+" with "+data+":" + e.getMessage());
        }
    }

    private HttpClient clientFor(URL url) {
        final HttpClient client = new HttpClient();
        final String userInfo = url.getUserInfo();
        if (userInfo != null) {
            final String[] usernamePassword = userInfo.split(":");
            client.getState().setCredentials(new AuthScope(url.getHost(), url.getPort()), new UsernamePasswordCredentials(usernamePassword[0], usernamePassword[1]));
        }
        return client;
    }
    public void initFromUrl2(Neo4jService service, URL url, final String query) {
        final Map cypherResult = post(url, map("query", query), Map.class);
        SubGraph graph=SubGraph.fromRaw(cypherResult, false);
        service.importGraph(graph);

    }
}
