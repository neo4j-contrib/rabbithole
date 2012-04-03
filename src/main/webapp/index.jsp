<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Cypher Query</title>
    <script src="http://code.jquery.com/jquery-1.6.4.min.js"></script>
    <script type="text/javascript">
        $(document).ready(function () {
            var uri = document.location.href;
            var idx = uri.indexOf("?");
            var geoff = "(A){\"name\":\"Alice\"}";
            if (idx > -1) {
              geoff = decodeURI(uri.slice(idx+1));
            }
            console.log(geoff)
            $.ajax("/console/geoff", {
                    type:"POST",
                    data:geoff,
                    success:function (data) {
                        $("#output").html(data);
                    },
                    error: function(data,error) {
                        $("#output").html("Error: "+data);
                    }
                });
            $("#form").submit(function () {
                var value = $("#form input").val();
                $.ajax("/console/cypher", {
                    type:"POST",
                    data:value,
                    success:function (data) {
                        $("#output").html(data);
                    },
                    error: function(data,error) {
                        $("#output").html("Error: "+data);
                    }
                });
                return false;
            }).focus();
        })
    </script>
</head>
<body>

<form id="form" action="#">
    Run <input type="text" name="text" size="180" value="start n=node(0) return n"/>.
</form>
<div>
    <pre style="width:500;height:400;color:white;background-color: black;overflow: auto;" id="output">

    </pre>
</div>
</body>
</html>