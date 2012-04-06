package org.neo4j.community.console;

import static spark.Spark.*;

import java.util.*;

import javax.servlet.http.HttpSession;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.geoff.Geoff;
import org.neo4j.geoff.Subgraph;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.MapUtil;
import static org.neo4j.helpers.collection.MapUtil.map;
import org.neo4j.test.ImpermanentGraphDatabase;

import org.neo4j.tooling.GlobalGraphOperations;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.servlet.SparkApplication;

import com.google.gson.Gson;

public class ConsoleApplication implements SparkApplication
{

    public static final String QUOTE = "\"";

    @Override
    public void init()
    {
        post( new Route( "/console/cypher" )
        {
            public Object handle( Request request, Response response )
            {
                try
                {
                    return cypherQuery( request,
                            request.body() );
                }
                catch ( Exception e )
                {
                    halt( 400, e.getMessage() );
                    return e.getMessage();
                }
            }
        } );
        get( new Route( "/console/cypher" )
        {
            public Object handle( Request request, Response response )
            {
                try
                {
                    String query = request.queryParams( "query" );
                    return new Gson().toJson( cypherQuery( request, query ) );
                }
                catch ( Exception e )
                {
                    halt( 400, e.getMessage() );
                    return e.getMessage();
                }
            }

        } );
        get( new Route( "/console/visualization" )
        {
            public Object handle( Request request, Response response )
            {
                try
                {
                    String query = request.queryParams( "query" );
                    return new Gson().toJson( cypherQueryViz( request, query ) );
                }
                catch ( Exception e )
                {
                    halt( 400, e.getMessage() );
                    return e.getMessage();
                }
            }

        } );
        get( new Route( "/console/share" )
        {
            public Object handle( Request request, Response response )
            {
                try
                {
                    GraphDatabaseService gdb = getGDB( request );
                    return toGeoff(gdb);
                }
                catch ( Exception e )
                {
                    halt( 400, e.getMessage() );
                    return e.getMessage();
                }
            }

        } );
        
        delete(new Route("/console") {

            @Override
            public Object handle( Request req, Response resp )
            {
                req.raw().getSession().invalidate();
                return "ok";
            }
            
        });

        post( new Route( "/console/geoff" )
        {
            public Object handle( Request request, Response response )
            {

                try
                {
                    GraphDatabaseService gdb = getGDB( request );
                    java.util.Map<String, PropertyContainer> res;
                    res = Geoff.mergeIntoNeo4j(new Subgraph(request.body().replaceAll("\\s*;\\s*", "\n")),
                            gdb, Collections.<String,PropertyContainer>singletonMap("0", gdb.getReferenceNode()));
                    return new Gson().toJson( res );
                }
                catch ( Exception e )
                {
                    halt( 400, e.getMessage() );
                    return e.getMessage();
                }
            }

        } );

    }

    private String toGeoff(GraphDatabaseService gdb) {
        StringBuilder sb=new StringBuilder();
        final Node refNode = gdb.getReferenceNode();
        for (Node node : GlobalGraphOperations.at(gdb).getAllNodes()) {
            if (node.equals(refNode)) continue;
            formatNode(sb, node);
            formatProperties(sb, node);
            sb.append("\n");
        }
        for (Node node : GlobalGraphOperations.at(gdb).getAllNodes()) {
            for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
                formatNode(sb, rel.getStartNode());
                sb.append("-[:").append(rel.getType().name()).append("]->");
                formatNode(sb,rel.getEndNode());
                formatProperties(sb, rel);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void formatNode(StringBuilder sb, Node n) {
        final long id = n.getId();
        if (id == 0) {
            sb.append("{").append(id).append("}");
        }
        else {
            sb.append("(").append(id).append(")");
        }
    }

	private Map toMap(PropertyContainer pc) {
		Map result=new HashMap();
		for (String prop : pc.getPropertyKeys()) {
			result.put(prop,pc.getProperty(prop));
		}
		return result;
	}
	
    private Map cypherQueryViz( Request request, String query )
    {
        GraphDatabaseService gdb = getGDB( request );
		Map<Long,Map> nodes=new TreeMap<Long,Map>();
		for (Node n : GlobalGraphOperations.at(gdb).getAllNodes()) {
			nodes.put(n.getId(), toMap(n));
		}
		List nodeIndex = new ArrayList(nodes.keySet());

		Map<Long,Map> rels=new TreeMap<Long,Map>();
		for (Relationship rel : GlobalGraphOperations.at(gdb).getAllRelationships()) {
			Map data=toMap(rel);
			data.put("source",nodeIndex.indexOf(rel.getStartNode().getId()));
			data.put("target",nodeIndex.indexOf(rel.getEndNode().getId()));
			data.put("type", rel.getType().name());
			data.put(rel.getType().name(),"type");
			rels.put(rel.getId(), data);
		}
		if (query!=null && !query.trim().isEmpty()) {
		int count=0;
        ExecutionResult result = getEE( request ).execute( query );
		for ( Map<String, Object> row : result )
        {
            for (Map.Entry<String,Object> entry : row.entrySet()) {
				String column=entry.getKey();
				Object value=entry.getValue();
				if (value instanceof Node) {
					Map map=nodes.get(((Node)value).getId());
					// map.put(column,count);
					map.put("selected",column);
				}
				if (value instanceof Relationship) {
					Map map=rels.get(((Relationship)value).getId());
					// map.put(column,count);
					map.put("selected",column);
				}
			}
			count++;
        }
		}
        return map("nodes",nodes.values(), "links", rels.values());
    }

    private void formatProperties(StringBuilder sb, PropertyContainer pc) {
        sb.append(" {");
//		sb.append(new Gson().toJson( toMap(pc) ));
        final Iterable<String> propertyKeys = pc.getPropertyKeys();
        for (Iterator<String> it = propertyKeys.iterator(); it.hasNext(); ) {
            String prop = it.next();
            sb.append(QUOTE).append(prop).append("\" : ");
            final Object value = pc.getProperty(prop);
            if (value instanceof String) sb.append(QUOTE).append(value).append(QUOTE);
            else sb.append(value);
            if (it.hasNext()) sb.append(", ");
        }
        sb.append("}");
    }

    private ExecutionEngine getEE( Request request )
    {
        HttpSession session = request.raw().getSession( true );
        ExecutionEngine ee = (ExecutionEngine) session.getAttribute( "ee" );
        if ( ee == null )
        {
            ee = new ExecutionEngine( getGDB( request ) );
            session.setAttribute( "ee", ee );
        }
        return ee;
    }

    private GraphDatabaseService getGDB( Request request )
    {
        HttpSession session = request.raw().getSession( true );
        GraphDatabaseService gdb = (GraphDatabaseService) session.getAttribute( "gdb" );
        if ( gdb == null )
        {
            gdb = new ImpermanentGraphDatabase();
            session.setAttribute( "gdb", gdb );
        }
        return gdb;
    }

    private String cypherQuery( Request request, String query )
    {
        ExecutionResult result = getEE( request ).execute( query );
        List rows = new ArrayList();
        for ( Map<String, Object> row : result )
        {
            rows.add( row );
        }
        return result.toString();
    }

}
