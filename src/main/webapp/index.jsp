<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Cypher Query</title>
    <script src="http://code.jquery.com/jquery-1.6.4.min.js"></script>
    <script type="text/javascript">
        function post(uri, data) {
            console.log("Post data: "+data);
            $.ajax(uri, {
                    type:"POST",
                    data:data,
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
        $(document).ready(function () {
			post("/console/geoff", initData());
            $("#form").submit(function () {
                var query = $("#form input").val();
				if (query.indexOf("start") == -1)
         			post("/console/geoff", query);
				else
         			post("/console/cypher", query);
                return false;
            }).focus();
        })
    </script>
</head>
<body>

<form id="form" action="#">
    Run <input type="text" name="text" size="180" value="start n=node(*) return n"/> Geoff or Cypher.
</form>
<div>
    <pre style="width:500;height:400;color:white;background-color: black;overflow: auto;" id="output">
    </pre>
</div>
</body>
</html>