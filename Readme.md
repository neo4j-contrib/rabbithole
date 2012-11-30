Interactive Neo4j Console - Graph REPL
   
Heroku App: http://rabbithole.heroku.com or http://console.neo4j.org

to embed in iframes, see [usage.html](http://rabbithole.herokuapp.com/usage.html)

    <iframe src="http://rabbithole.heroku.com?init=URI%20ENCODED%20GEOFF&query=URI%20ENCODED%20CYPHER" width="500" height="400" id="window"/>
        
Features:
* set up graph with Geoff or Cypher
* execute Cypher (query and mutation)
* visualize graph and returned results with d3
* export graph
* share, tweet link to current graph and query
 
Endpoints:

* post /console/cypher
* post /console/geoff
* delete /console
* post /console/init
* get /console/share

Running locally:

    mvn clean install exec:java  

potential arguments for local execution:

    java org.neo4j.community.console.Console port /path/to/db [expose]

(expose will write and read-through otherwise it will pull the content in a in-memory db)

it can also import the data from a remote server

	http://console.neo4j.org?init=http://server:port/db/data/cypher
