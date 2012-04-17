function append(element, text) {
    if (!text) return;
    if (typeof(text) == 'object') text = JSON.stringify(text);
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
            append($("#output"), "Error: "+error+"\n" + data.responseText+"");
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

function generate_url() {
    var init = $('#share_init').val();
    var query = $('#share_query').val();
    var uri = 'http://console.neo4j.org?init='+encodeURIComponent(init)+'&query='+encodeURIComponent(query);
    console.log(uri);
    $('#share_url').val(uri);
    var frame = '<iframe width="600" height="300" src="'+uri+'"/>';
    $('#share_iFrame').val(frame);
    addthis.update('share', 'url', uri);
    addthis.update('share', 'title', 'Look at this Neo4j graph: ');
    //addthis.update('config', 'ui_cobrand', 'New Cobrand!');
}

function toggleGraph() {
    $('#graph').toggle();
    if ($('#graph').is(":visible")) {
        viz();
    }
}

function toggleShare() {
    $.ajax("console/to_geoff", {
        type:"GET",
        success:function (data) {
            $('#share_init').val(data);
            $('#share_query').val($("#form input").val());
            $('#shareUrl').toggle();
        },
        error:function (data, error) {
            append($("#output"), "Error: "+error+"\n" + data.responseText+"");
        }
    });
}

$(document).ready(function () {
        console.log("parameters"+window.location.search);
        $.ajax("/console/init"+window.location.search, {type:"GET",
            success:function (json) {
                var data = $.parseJSON(json);
                if (data["init"]) append($("#output"), "Graph Setup:\n"+data["init"]);
                // append($("#output"), data["geoff"]);
                append($("#output"), data["result"]);
                if (data["query"]) $("#form input").val(data["query"]);
                if (data["error"]) append($("#output"), "Error: "+data["error"]);
                if (data["visualization"]) viz(data.visualization);
            },
            error:function (data, error) {
                append($("#output"), "Error: "+error+"\n" + data.responseText+"");
            }
        });
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
    $("#form").submit(function () {
        var query = $("#form input").val();
        var url = isCypher(query) ? "/console/cypher" : "/console/geoff";
        post(url, query, function () {
            viz();
        });
        return false;
    });
    $("#form input").focus();
});