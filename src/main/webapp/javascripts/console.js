function append(element, text) {
    element.html(element.html() + "\n" + text);
    element.prop("scrollTop", element.prop("scrollHeight") - element.height());
}
function post(uri, data, done) {
    console.log("Post data: " + data);
    append($("#output"), "> " + data);
    $.ajax(uri, {
        type:"POST",
        data:data,
        dataType:"text",
        success:function (data) {
            append($("#output"), data);
            if (done) {
                done();
            }
        },
        error:function (data, error) {
            append($("#output"), "Error: "+error+"\n" + data+"");
        }
    });
}

function getParameters() {
    var pairs = window.location.search.substr(1).split('&');
    if (!pairs) return {};
    var result = {};
    for (var i = 0; i < pairs.length; ++i) {
        var pair = pairs[i].split('=');
        console.log(pair);
        if (pair.length != 2) continue;
        result[pair[0]] = decodeURIComponent(pair[1].replace(/\+/g, " "));
    }
    return result;
}

function share(fn) {
    $.ajax("/console/url", { type:"GET", success:fn});
}

function tweet() {
    share(function(uri) {
        var text=encodeURIComponent("A #Neo4j graph database ");
        window.open("https://twitter.com/intent/tweet?original_referer=http%3A%2F%2Fconsole.neo4j.org%2F&source=tweetbutton&text="+text+"&url="+uri+"&via=neo4j","tweet this db","height=400,width=400");
    });
}
function isCypher(query) {
    return query && query.indexOf("start") != -1;
}
function viz() {
    if ($('#graph').is(":hidden")) return;
    var query = $("#form input").val();
    if (!isCypher(query)) query = "";
    var graph = $("#graph");
    graph.empty();
    render("graph", graph.width(), graph.height(), "/console/visualization?query=" + encodeURIComponent(query));
    graph.show();
}
function reset(done) {
    $.ajax("/console", { type:"DELETE", success:done });
    return false;
}

function toggleGraph() {
    $('#graph').toggle();
    if ($('#graph').is(":visible")) {
        viz();
    }
}

$(document).ready(function () {
        console.log("parameters"+window.location.search);

        //initialize the editor
        var editor = ace.edit("editor");
        //editor.setTheme("ace/theme/twilight");
        var CypherMode = require("ace/mode/cypher").Mode;
        editor.getSession().setMode(new CypherMode());

        $.ajax("/console/init"+window.location.search, {type:"GET", success: function(json) {
            var data=$.parseJSON(json);
            append($("#output"), data.init);
            append($("#output"), data.geoff);
            append($("#output"), data.result);
            console.log(data.query);
            editor.getSession().setValue(data.query);
            editor.commands.addCommand({
            name: 'execute',
            bindKey: {
            win: 'Ctrl-E',
            mac: 'Ctrl-E'
            },
                exec: function(editor) {
                   $("#submitQuery").click();
            }
        });
            editor.focus();
            viz(data.vizualization);
        }});
/*
    reset(function () {
        var params = getParameters();
        console.log(params);
        post("/console/geoff", params.init || "(Neo) {\"name\": \"Neo\" }; (Morpheus) {\"name\":\"Morpheus\"}; (Trinity) {\"name\":\"Trinity\"}; (Cypher) {\"name\":\"Cypher\"}; (Smith) {\"name\" : \"Agent Smith\"}; (Architect) {\"name\":\"The Architect\"};(0)-[:ROOT]->(Neo);(Neo)-[:KNOWS]->(Morpheus);(Neo)-[:LOVES]->(Trinity);(Morpheus)-[:KNOWS]->(Trinity);(Morpheus)-[:KNOWS]->(Cypher);(Cypher)-[:KNOWS]->(Smith);(Smith)-[:CODED_BY]->(Architect)",
            function () {
                var query = params.query || "start n=node(*) match n-[r]->m return n,type(r),m";
                post("/console/cypher", query);
                $("#form input").val(query);
                viz();
            }
        );
    });
*/
    $("#submitQuery").click(function (){
        post(isCypher(editor.getSession().getValue()) ? "/console/cypher" : "/console/geoff", editor.getSession().getValue(), function () {
            viz();
        });
        return false;
    });
});