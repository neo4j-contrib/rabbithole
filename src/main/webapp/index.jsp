<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Neo4j Console</title>
    <script src="http://code.jquery.com/jquery-1.6.4.min.js"></script>
    <script type="text/javascript">
        function post(uri, data) {
            console.log("Post data: "+data);
            $.ajax(uri, {
                    type:"POST",
                    data:data,
                    dataType:"text",
                    success:function (data) {
                        $("#output").html(data);
                    },
                    error: function(data,error) {
                        $("#output").html("Error: \n"+data);
                    }
                });
		}
		function initData() {
			var uri = document.location.href;
            var idx = uri.indexOf("?");
 			if (idx==-1) return "(A) {\"name\":\"Neo\"}; (B) {\"name\" : \"Trinity\"}; (A)-[:LOVES]->(B)";
			return decodeURI(uri.slice(idx+1));
		}
		function reset() {
			$.ajax("/console", { type:"DELETE" });
			return false;
		}
        $(document).ready(function () {
			post("/console/geoff", initData());
            $("#form").submit(function () {
                var query = $("#form input").val();
				if (query.indexOf("start") == -1)
         			post("/console/geoff", query);
				else
         			post("/console/cypher", query);
                return false;
            })
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
		margin:0px;
		border:none;
	}
	  #output {
		overflow: auto;height:80%;
	  }
   </style>
</head>
<body>

<div>
    <pre id="output" class="console">
    </pre>
	<form id="form" action="#">
	    <input class="console" type="text" name="text" size="180" value="start n=node(*) return n"/>
	</form>
</div>
Run <a target="_blank" href="http://geoff.nigelsmall.net/hello-subgraph">Geoff</a> or <a href="http://docs.neo4j.org/chunked/milestone/cypher-query-lang.html" target="_blank">Cypher</a>.
<a href="#" onclick="reset()">Reset</a> the database.
</body>
</html>