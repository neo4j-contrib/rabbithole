function append(element, text) {
    if (!text) return;
    if (typeof(text) == 'object') text = JSON.stringify(text);
    text = highlight(text);
    element.html(element.html() + "\n" + "<div class='data'>" + text + "</div>");
    element.prop("scrollTop", element.prop("scrollHeight") - element.height());
}

function highlight(text) {
    if (!text) return text;
    var prompt = ""
    if (text.substr(0, 2) == "> ") {
        prompt = "> ";
        text = text.substr(2);
    }
    if (text[0] == "(" || text[0] == "[") {
        // Geoff
        // colour nodes
        text = text.replace(/(\([0-9A-Za-z_]*?\))/gi, '<span class="node">$1</span>');
        // colour rels
        text = text.replace(/((<?-)?\[[0-9A-Za-z_:\.]*?\](->?)?)/gi, '<span class="relationship">$1</span>');
    } else if (isCypher(text)) {
        // Cypher
        text = text.replace(/\b(start|with|create|delete|rel|relate|skip|limit|distinct|desc|asc|as|order by|foreach|set|match|where|return)\b/gi, '<span class="keyword">$1</span>');
        text = text.replace(/\b(and|or|not|has|node)\b/gi, '<span class="keyword">$1</span>');
        text = text.replace(/\b(type|collect|sum|sqrt|round|max|min|nodes|count|length|avg|rels)\b\(/gi, '<span class="function">$1</span>(');
    }
    return prompt + text;
}

function post(uri, data, done, dataType) {
    console.log("Post data: " + data);
    // append($("#output"), "> " + data.trim());
    $.ajax(uri, {
        type:"POST",
        data:data,
        dataType: dataType || "text",
        success:function (data) {
            if (dataType=="text") append($("#output"), data);
            if (done) {
                done(data);
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
        // console.log(pair);
        if (pair.length != 2) continue;
        result[pair[0]] = decodeURIComponent(pair[1].replace(/\+/g, " "));
    }
    return result;
}

function share(fn) {
    $.ajax("/console/url", { type:"GET", success:fn});
}

function isCypher(query) {
    return query && query.match(/\b(start|create|delete|relate)\b/i);
}
function viz(data) {
    if ($('#graph').is(":hidden")) return;
    var graph = $("#graph");
    graph.empty();
    if (data) {
        visualize("graph", graph.width(), graph.height(),data)
    } else {
        var query = $("#input").val();
        if (!isCypher(query)) query = "";
        render("graph", graph.width(), graph.height(), "/console/visualization?query=" + encodeURIComponent(query));
    }
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
            $('#share_query').val($("#input").val());
            $('#shareUrl').toggle();
        },
        error:function (data, error) {
            append($("#output"), "Error: "+error+"\n" + data.responseText+"");
        }
    });
}

function showResults(data) {
    if (data["init"]) {
        append($("#output"), "Graph Setup:");
        append($("#output"), data["init"]);
    }
    if (data["query"]) {
	    append($("#output"), "> " + data["query"].trim());
        $("#input").val(data["query"]+"\n\n\n");
    }
    if (data["result"]) {
        append($("#output"), data["result"]);
    }
    if (data["error"]) append($("#output"), "Error: " + data["error"]);
    if (data["visualization"]) {
        viz(data.visualization);
    }
}

function send(query) {
    if (isCypher(query)) {
        post("/console/cypher", query, showResults,"json");
    } else {
        post("/console/geoff", query, function () {
            viz();
        });
    }
}

$(document).ready(function () {
    post("/console/init", JSON.stringify(getParameters()), showResults,"json");
    var input=$("#input");
//    $("#form").submit(function () {
//        send(input.val());
//        return false;
//    });
    var isInIFrame = window.location != window.parent.location;
    if (!isInIFrame) $("#input").focus();

    $("body").keyup(function(e) {
        if (e.keyCode == 27) $(".popup").hide();
		return true;
    });
    input.keyup(function(e) {
		 if (e.keyCode == 40) { // arrow down, add line
		 }
         if (e.keyCode == 13) { // return, send
             send(input.val());
			return true;
         }
     });
});