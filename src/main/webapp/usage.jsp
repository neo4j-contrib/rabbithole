<%@ page session="false" %>
<html>
<head>
    <title>Neo4j Console</title>
    <script src="http://code.jquery.com/jquery-1.6.4.min.js"></script>
	<script type="text/javascript">
//		var base="http://localhost:8080";
		var base="http://rabbithole.heroku.com";
		function start() {
			var val=encodeURI($("#geoff").val().replace(/\s*\n\s*/g,";"));
			$("#window").attr("src",base+"?"+val);
			return false;
		}
	</script>
</head>
<body>
	<div>
		<textarea rows="10" cols="80" id="geoff">
(Neo) {"name" : "Neo"}
(Trinity) {"name" : "Trinity"}
(Neo)-[:LOVES]->(Trinity)
		</textarea><br/>
		<button id="load" onclick="start();">Load Database</button>
	</div>
	<iframe src="http://rabbithole.heroku.com?" width="500" height="400" id="window"/>
</body>
</html>