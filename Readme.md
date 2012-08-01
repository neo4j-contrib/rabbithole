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

You can run the Console also against an existing Graph Database and then even choose to expose it or copy its content into a sandboxed in-memory graphdb
for rendering and experimentation. This feature is quite new so there might be some quirks.

    java -jar neo4j-console-jar-with-dependencies.jar port \[graphdb-path or URL\] \[expose\]

* port is the webserver port, if not provided the environment variable `PORT` is used (or 8080 as default)
* graphdb-path is on the local file-system, or a Cypher endpoint of a running Neo4j-Server (e.g. http://localhost:7474/db/data/cypher)
* if "expose" is provided the original graphdb is directly exposed, so modifcations will change it, otherwise it will 
  be sandboxed in the in-memory instance used by the console

Please note that the console is not suited for large graphs, but smaller and perhaps medium sized graphs can be handled and visualized with ease.
