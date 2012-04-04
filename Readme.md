Interactive Neo4j Console

Heroku App: http://rabbithole.heroku.com

to embed in iframes, see [usage.jsp](http://rabbithole.herokuapp.com/usage.jsp)

    <iframe src="http://rabbithole.heroku.com?URI%20ENCODED%20GEOFF" width="500" height="400" id="window"/>
    
    
Endpoints:

* post /console/cypher
* post /console/geoff
* delete /console

Running locally:

    mvn clean install exec:java 
