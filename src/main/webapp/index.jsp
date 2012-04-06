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
				window.open("http://console.neo4j.org?init="+encodeURIComponent(data),"Share Neo4j Database");
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
				post("/console/geoff", params.init || "(A) {\"name\":\"Neo\"}; (B) {\"name\" : \"Trinity\"}; (A)-[:LOVES]->(B)",
					function() {
						var query=params.query || "start n=node(*) return n";
						post("/console/cypher", query);
						$("#form input").val(query);
						viz();
					}
				);
			});
            $("#form").submit(function () {
                var query = $("#form input").val();
				if (isCypher(query)) {
					post("/console/cypher", query);
				}
				else {
         			post("/console/geoff", query);
				}
				viz();
                return false;
            });
			$("#form input").focus();
        })
    </script>
   <style type="text/css">
	 body,html,div {
		margin:0px;
	  }
     .console {
		width:100%;color:white;background-color: black;
		font-family: monospace;
		height:10%;
		margin:0px;
		border:none;
	}
	  #output {
		overflow: auto;height:90%;
	  }
	  #graph {
/*		display:none;*/
		width:100%;
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
	  .link { stroke: #ccc; }
	
	  .selected {
	    stroke: red;
	    stroke-width: 1.5px;
	  }
	  #info {
		position:absolute;
		top:25%;
		left:25%;
		display:none;
		background:#E0E0E0;
		width:50%;
		height:50%;
		z-index:20;
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
   </style>
</head>
<body>
<div>
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
		Run <a target="_blank" href="http://geoff.nigelsmall.net/hello-subgraph">Geoff</a> or <a href="http://docs.neo4j.org/chunked/milestone/cypher-query-lang.html" target="_blank">Cypher</a>.<br/>
		<a href="#" onclick="reset()">Reset</a> or <a href="#" onclick="share()">Share</a> the database.
	</div>
</div>
</body>
</html>