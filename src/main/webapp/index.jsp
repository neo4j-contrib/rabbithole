<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Neo4j Console</title>
    <script src="http://code.jquery.com/jquery-1.6.4.min.js"></script>
	<script type="text/javascript" src="http://mbostock.github.com/d3/d3.js"></script>
	<script type="text/javascript" src="/javascripts/visualization.js"></script>
    <script type="text/javascript">
		function append(element, text) {
			element.html(element.html() + "\n" + text);
			element.prop("scrollTop", element.prop("scrollHeight") - element.height() );
		}
        function post(uri, data, done) {
            console.log("Post data: "+data);
            append($("#output"),"> "+data);
            $.ajax(uri, {
                    type:"POST",
                    data:data,
                    dataType:"text",
                    success:function (data) {
                        append($("#output"),data);
						if (done) { done(); }
                    },
                    error: function(data,error) {
                        append($("#output"),"Error: \n"+data);
                    }
                });
		}
		function getParameters() {
			var pairs = window.location.search.substr(1).split('&');
			if (!pairs) return {};
			var result = {};
       		for (var i = 0; i < pairs.length; ++i) {
				var pair=pairs[i].split('=');
				console.log(pair);
	           	if (pair.length != 2) continue;
				result[pair[0]] = decodeURIComponent(pair[1].replace(/\+/g, " "));
			}
			return result;
       	}
		function share() {
			$.ajax("/console/share", { type:"GET", success: function(data) {
				$('#share').val("http://console.neo4j.org?init="+encodeURIComponent(data));
			}});
		}
		function isCypher(query) {
			return query && query.indexOf("start") != -1;
		}
		function viz() {
			if ($('#graph').is(":hidden")) return;
            var query = $("#form input").val();
			if (!isCypher(query)) query="";
			var graph=$("#graph");
			graph.empty();
			render("graph",graph.width(),graph.height(),"/console/visualization?query="+encodeURIComponent(query));
			graph.show();
		}
		function reset(done) {
			$.ajax("/console", { type:"DELETE", success: done });
			return false;
		}
		function toggleGraph() {
			$('#graph').toggle();
			if ($('#graph').is(":visible")) {
				viz();
			}
		}
        $(document).ready(function () {
			console.log(getParameters());
			reset(function() {
				var params=getParameters();
				post("/console/geoff", params.init || "(Neo) {\"name\": \"Neo\" }; (Morpheus) {\"name\":\"Morpheus\"}; (Trinity) {\"name\":\"Trinity\"}; (Cypher) {\"name\":\"Cypher\"}; (Smith) {\"name\" : \"Agent Smith\"}; (Architect) {\"name\":\"The Architect\"};(0)-[:ROOT]->(Neo);(Neo)-[:KNOWS]->(Morpheus);(Neo)-[:LOVES]->(Trinity);(Morpheus)-[:KNOWS]->(Trinity);(Morpheus)-[:KNOWS]->(Cypher);(Cypher)-[:KNOWS]->(Smith);(Smith)-[:CODED_BY]->(Architect)",
					function() {
						var query=params.query || "start n=node(*) match n-[r]->m return n,type(r),m";
						post("/console/cypher", query);
						$("#form input").val(query);
						viz();
					}
				);
			});
            $("#form").submit(function () {
                var query = $("#form input").val();
				var url=isCypher(query) ? "/console/cypher" : "/console/geoff";
				post(url,query, function() { viz(); });
                return false;
            });
			$("#form input").focus();
        })
    </script>
   <style type="text/css">
	 body,html,div,form,input {
		margin:0px;
	  }
     .console {
		width:100%;color:EEE;
		background-color: black;
		font-family: monospace;
		height:10%;
		margin:0px;
		border:none;
	  }
 	  a:link, a:visited {
	     color:white;
	  }
	  #output {
		overflow-y:auto;
		height:90%;
		white-space: pre-wrap;       /* css-3 */
	 	white-space: -moz-pre-wrap;  /* Mozilla, since 1999 */
	 	white-space: -pre-wrap;      /* Opera 4-6 */
	 	white-space: -o-pre-wrap;    /* Opera 7 */
	 	word-wrap: break-word;       /* Internet Explorer 5.5+ */
	  }
	  #graph {
/*		display:none; */
		width:50%;
		height:90%;
		position:absolute;
		background:none;
		z-index:10;
		top:0px;
		right:0px;
/*
		filter:alpha(opacity=20);
		-moz-opacity:0.2;
		opacity:0.2;
*/		
	  }
	
	  #info {
		font-family: monospace;
		background-color: 202020;
		color:EEE;
		border: 2px solid darkgrey;
		position:absolute;
		top:25%;
		left:25%;
		display:none;
		width:50%;
		height:50%;
		z-index:50;
		padding: 20px;
	  }
      img {
		z-index:100;
		position:absolute;
		top:5px;
		width:16px;
		height:16px;
	  }
	  img.info {
		right:26px;
	  }
	  img.graph {
		right:5px;
	  }
	   /* d3 styles */
	  .link { stroke: #ccc; }
	  .path-text {
	  		font: 6px sans-serif;
	  		pointer-events: none;
	  		text-align:center;
		}

	   .shadow {
	     stroke: #000;
	     stroke-width: 1.5px;
	     stroke-opacity: .8;
	   }
	   .marker {
	      stroke: #ccc;
		  fill:#ccc;
	   }
	   text {
		 fill: #fff;
	     font: 8px sans-serif;
	     pointer-events: none;
	   }	
	  .selected {
	    stroke: red;
	    stroke-width: 1.5px;
	  }
   </style>
</head>
<body>
<div id="main">
	<img title="Info" onclick="$('#info').toggle();" class="info" src="/img/info.png"/>
	<img title="Graph" onclick="toggleGraph();" class="graph" src="/img/graph.png"/>
    <pre id="output" class="console">
    </pre>
	<form id="form" action="#">
	    <input class="console" type="text" name="text" size="180" value="start n=node(*) return n"/>
	</form>
	<div id="graph">
	</div>
	<div id="info">
		This is an interactive console (REPL) for graphs with integrated visualization.
		It is hosted on Heroku at <a href="http://console.neo4j.org">http://console.neo4j.org</a> the source
		code is available on <a href="http://github.com/neo4j-contrib/rabbithole">GitHub</a>.<br/><br/>
		To add to the Graph you can issue
		<a target="_blank" href="http://geoff.nigelsmall.net/hello-subgraph">Geoff</a> statements, like
		(Neo) {"name":"Neo"} to create nodes and (Neo)-[:LOVES]->(Trinity) to create relationships. Connect or update existing nodes by referring to (nodeId)<br/><br/>
		For querying the graph, <a href="http://docs.neo4j.org/chunked/milestone/cypher-query-lang.html" target="_blank">Cypher</a> is your friend. E.g. start user=node(1) match user-[:KNOWS]->friend where friend.age > 20 return user,friend order by friend.age limit 10 <br/><br/>
		You can <a href="#" onclick="reset()">reset</a> or share the database.<br/>
		<input type="url" id="share" style="width:80%" onclick="this.select();"><button onclick="share()">Share</button>
	</div>
</div>
</body>
</html>