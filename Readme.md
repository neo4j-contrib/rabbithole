# Interactive Neo4j Console - Graph REPL  

     
Heroku App: https://rabbithole.heroku.com or https://console.neo4j.org

to embed in iframes, see [usage.html](https://rabbithole.herokuapp.com/usage.html)

    <iframe src="https://rabbithole.heroku.com?init=URI%20ENCODED%20CYPHER%20SETUP&query=URI%20ENCODED%20CYPHER%20QUERY" width="500" height="400" id="window"/>
        
### Features:

* set up graph with [Cypher](http://neo4j.org/tracks/cypher) or [Geoff](http://nigelsmall.com/geoff)
* execute Cypher queries
* visualize graph and returned results with the [D3](http://d3js.org/) javascript library
* export graph in a variety of formats
* share, tweet link to current graph and query

### Run it as your own heroku app:

* `git clone git://github.com/neo4j-contrib/rabbithole.git` 
* `cd rabbithole`
* have the [heroku toolbelt](https://toolbelt.heroku.com/) installed
* `heroku apps:create "appname"`
* if you want to store shortlinks of your graphs add the [Neo4j addon](http://addons.heroku.com/neo4j) 
  `heroku addons:add neo4j`
* Push the application to Heroku `git push heroku master`
* open your personal console in the browser [http://appname.heroku.com](http://appname.heroku.com)
 
### Running locally on your machine:

* `git clone git://github.com/neo4j-contrib/rabbithole.git` 
* `cd rabbithole`
* `mvn clean install exec:java`
* open application in browser [http://localhost:8080](http://localhost:8080)


### building a war file

* use `mvn install war:war` to build a `war` file. This can be deployed to any web container (tomcat, jetty, ...)

### running locally via jetty

* use `mvn install jetty:run`


### Endpoints:

* post /console/cypher
* post /console/geoff
* delete /console
* post /console/init
* get /console/share


### Potential arguments for local execution:

    java org.neo4j.community.console.Console port /path/to/db [expose]

("expose" will write and read-through to the graph-db otherwise it will copy the graph content into an in-memory db)

The console can also import the data from a remote server

    https://console.neo4j.org?init=http://server:port/db/data/cypher


