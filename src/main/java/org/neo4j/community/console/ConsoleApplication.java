package org.neo4j.community.console;

import static spark.Spark.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.geoff.Geoff;
import org.neo4j.geoff.Subgraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.test.ImpermanentGraphDatabase;

import spark.Request;
import spark.Response;
import spark.Route;
import spark.servlet.SparkApplication;

import com.google.gson.Gson;

public class ConsoleApplication implements SparkApplication
{

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
                    return "400";
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
                    res = Geoff.mergeIntoNeo4j( new Subgraph( request.body().replaceAll("\\s*;\\s*","\n") ),
                            gdb, null );
                    return new Gson().toJson( res );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                    halt( 400, e.getMessage() );
                    return "bad bad";
                }
            }

        } );

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
