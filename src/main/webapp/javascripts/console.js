'use strict';

var inputeditor;
//var visualizer = new GraphVisualization();
var visualizer = new Neod3Renderer();

var graphgistWindow;

function append(element, text, doHighlight) {
    if (!text) {
        return;
    }
    if (typeof ( text ) == 'object') {
        text = JSON.stringify(text);
    }

    text = "\n" + text;
    if (doHighlight) {
        text = highlight(text);
    }
    element.append($("<span class='textblock'>" + text + "</span>"));
    element.prop("scrollTop", element.prop("scrollHeight") - element.height());
}

function highlight(text) {
    var newtext = "";

    function appendToNewtext(text, typeClass) {
        if (typeClass == null) {
            newtext = newtext + text;
        }
        else {
            newtext = newtext + "<span data-lang='"+typeClass+"' class='code cm-" + typeClass + "'>" + text + "</span>";
        }
    }

    CodeMirror.runMode(text, "cypher", appendToNewtext);
    return newtext;
}

function post(uri, data, done, dataType) {
    data = data.trim();
    console.log("Post data: " + data);
    $.ajax(uri, {
        type: "POST",
        data: data,
        redirect: "follow",
        dataType: dataType || "text",
        beforeSend: setSessionHeader,
        success: function (data) {
            if (dataType === "text") {
                append($("#output"), data);
            }
            if (done) {
                done(data);
            }
        },
        error: function (data, error) {
            append($("#output"), "Error: " + error + "\n" + data.responseText + "");
        }
    });
}

function getParameters() {
    var pairs = window.location.search.substr(1).split('&');
    if (!pairs) {
        return {};
    }
    var result = {};
    for (var i = 0; i < pairs.length; ++i) {
        var pair = pairs[i].split('=');
        //console.log( pair );
        if (pair.length !== 2) {
            continue;
        }
        result[pair[0]] = decodeURIComponent(pair[1].replace(/\+/g, " "));
    }
    return result;
}

function share(fn) {
    $.ajax("console/url", {
        type: "GET",
        redirect: "follow",
        success: fn
    });
}

function isCypher(query) {
    return query && query.match(/\b(start|merge|match|call|load|foreach|drop|create|delete|relate|return)\b/i);
}

function viz(data) {
    if (!vizVisible) {
        return;
    }
    var output = $("#output");
    output.children('#graph').remove();
    var container = $("<div id='graph'></div>");
    output.append(container);
    if (data) {
        var h = output.height();
        var show_result_only = false; // todo
        var style = null; // todo custom styles like from adoc
        //if (style == "{style}") style = null;
        var selectedVisualization = handleSelection(data, show_result_only);
        visualizer.render("graph", container, selectedVisualization, style);
        //visualizer.visualize("output", output.width(), h, data);
    }
    else {
        var query = getQuery();
        if (!isCypher(query)) {
            query = "";
        }
        $.ajax("console/visualization?query=" + encodeURIComponent(query), {
            type: "GET",
            redirect: "follow",
            beforeSend: setSessionHeader,
            dataType: "json",
            success: function (data) {
                var show_result_only = false; // todo
                var style = null; // todo custom styles like from adoc
                var selectedVisualization = handleSelection(data, show_result_only);
                visualizer.render("graph", container, selectedVisualization, style);

                //visualizer.visualize("output", output.width(), output.height(),data)
            }
        });
    }
    // graph.show();
}

function setSessionHeader(request) {
    request.setRequestHeader("X-Session", session_id)
}
function reset(done) {
    $.ajax("console", {
        beforeSend: setSessionHeader,
        type: "DELETE",
        redirect: "follow",
        success: done
    });
    return false;
}

function share_yuml(query) {
    if (!query) {
        return;
    }
    $.ajax("console/to_yuml?query=" + encodeURIComponent(query), {
        type: "GET",
        redirect: "follow",
        beforeSend: setSessionHeader,
        dataType: "text",
        success: function (data) {
            $('#share_yuml').attr("href", data);
        }
    });
}

function base_url() {
    return document.location.protocol + "//" + document.location.host;
}

function store_graph_info() {
    var init = $('#share_init').val();
    var query = $('#share_query').val();
    query = isCypher(query) ? query : null;
    var version = $('#share_version').val();
    var no_root = $("#share_no_root").is(":checked");
    var id = $('#share_short').val();
    if (id && ( id.length == 0 || id.match("https?://.+")  )) {
        id = null;
    }
    var message = null;
    // var uri =
    // base_url()+'?init='+encodeURIComponent(init)+'&query='+encodeURIComponent(query)+'&version='+encodeURIComponent(version);
    // if (no_root) uri+="&no_root=true";

    $.ajax("/r/share", {
        type: "POST",
        dataType: "text",
        redirect: "follow",
        contentType: "application/json",
        data: JSON.stringify({
            id: id,
            init: init,
            query: query,
            version: version,
            no_root: no_root,
            message: message
        }),
        success: function (data) {
            var uri = ( data.indexOf("http") != 0 ) ? base_url() + "/r/" + data : data;
            // console.log(uri);
            var frame = '<iframe width="600" height="300" src="' + uri + '"/>';
            $('#share_iFrame').val(frame);
            $('#share_url').attr("href", uri);
            $('#share_short').val(uri);
            addthis.update('share', 'url', uri);
        }
    });
    share_yuml(query);
    addthis.update('share', 'title', 'Look at this #Neo4j graph: ');
}

var vizVisible = true;
function toggleGraph() {
    vizVisible = !vizVisible;
    if (vizVisible) {
        $('#graph').show();
        viz();
    } else {
        $('#graph').hide();
    }
}

function toggleShare() {
    $.ajax("console/to_cypher", {
        type: "GET",
        redirect: "follow",
        beforeSend: setSessionHeader,
        dataType: "text",
        success: function (data) {
            $('#share_init').val(data);
            var query = getQuery();
            query = isCypher(query) ? query : null;
            $('#share_query').val(query);
            $('#shareUrl').toggle();
            share_yuml(query);
        },
        error: function (data, error) {
            append($("#output"), "Error: " + error + "\n" + data.responseText + "");
        }
    });
}

function export_graph(format) {
    $.ajax("console/to_" + format, {
        type: "GET",
        redirect: "follow",
        dataType: "text",
        beforeSend: setSessionHeader,
        success: function (data) {
            $('#share_init').val(data);
        },
        error: function (data, error) {
            $('#share_init').val("Error: " + error + "\n" + data.responseText + "");
        }
    });
}

function readable(prefix, map) {
    var col = [];
    for (var key in map) {
        if (map.hasOwnProperty(key) && map[key]) {
            col.push(map[key] + " " + key + ( map[key] > 1 ? "s" : "" ));
        }
    }
    if (!col.length) {
        return "";
    }
    if (col.length == 1) {
        return prefix + " " + col[0] + " ";
    }
    return prefix + " " + col.slice(0, -1).join(", ") + " and " + col[col.length - 1] + " ";
}

function computeInfo(data) {
    var stats = data.stats;
    var info = "Query took " + stats.time + " ms";
    var count = data.json ? stats.rows : 0;
    info += " and returned " + ( count ? count : "no" ) + " rows. ";
    if (stats.containsUpdates) {
        var updates = "\nUpdated the graph - " + readable("created", {
            node: stats.nodesCreated,
            relationship: stats.relationshipsCreated
        }) + readable("deleted", {
            node: stats.nodesDeleted,
            relationship: stats.relationshipsDeleted
        }) + readable("set", {
            property: stats.propertiesSet
        }).replace("propertys", "properties");
        info += updates;
    }
    return info;
}

function inputQuery(query) {
    inputeditor.setValue(query); // .replace(/\n/g, ' ').trim()
    CodeMirror.commands["selectAll"](inputeditor);
    //autoFormatSelection(inputeditor);
    resizeOutput();
}

function infoImage(data) {
    var text = "";
    for (var title in data) {
        if (data.hasOwnProperty(title)) {
            text += "<h4>" + title + "</h4>" + data[title] + "\n";
        }
    }
    return $('<button class="btn_pretty">Result Details</button>').attr('title', text).click(function () {
        $('#stats-output').html(text);
        $('#stats').show();
    });
}
function showResults(data) {
    if (data["init"]) {
        append($("#output"), "Graph Setup:");
        append($("#output"), data["init"], true);
    }
    if (data["query"]) {
        append($("#output"), "\nQuery:");
        inputQuery(data["query"]);
        append($("#output"), inputeditor.getValue(), true);
        resizeOutput();
    }
    if (data["result"]) {
        append($("#output"), "\n");
        renderResult("output", data);
        append($("#output"), computeInfo(data));
        $("#output").append(infoImage({
            "Query Results": data.result,
            "Execution Plan": data.plan
        }));
        append($("#output"), "\n");
    }
    if (data["error"]) {
        append($("#output"), "Error: " + data["error"], true);
    }
    if (data["visualization"]) {
        viz(data.visualization);
    }
}

function send(query) {
    post("console/cypher"+getPostQueryParams(), query, showResults, "json");
}

function showVersion(json) {
    $('#version').val(json['version']);
    $('#share_version').val(json['version']);
}

function welcome_msg() {
    return $("#welcome_msg").html();
}

function showWelcome(json) {
    var output = $("#output");

    if (json['message']) {
        if (json['message'] != "none") {
            output.append(json['message']);
        }
    }
    else {
        output.append(welcome_msg());
    }
}

function resizeOutput() {
    var $output = $("#output");
    var editorHeight = Math.min($(".CodeMirror-scroll").height(),128);
    var windowHeight = $(window).height();
//    console.log("editorHeight",editorHeight,"scrollHeight",$(".CodeMirror-scroll").height(),"windowHeight",windowHeight);
    $output.css({
        'height': ( windowHeight - editorHeight - 15 ) + 'px'
    });
    $output.animate({
        scrollTop: $output.prop("scrollHeight")
    }, 10);
    $("#query_button").css({
        'height': editorHeight + 'px'
    });
}

function getSelectedRange(editor) {
    return {
        from: editor.getCursor(true),
        to: editor.getCursor(false)
    };
}

function autoFormatSelection(editor) {
    var range = getSelectedRange(editor);
    editor.autoFormatRange(range.from, range.to);
}

function getQuery() {
    return inputeditor.getValue().replace(/\n/g, " ");
}

function query() {
    inputQuery(getQuery());
    send(getQuery());
}

function cleanDb() {
    if ($('#version').val()=="1.9")
        send("START n=node(*) MATCH (n)-[r?]-() DELETE n, r");
    else
        send("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n, r");
}

function parseMessage(data) {
    var first = data.trim().charAt(0);
    if (first == '{' || first == '[') {
        var result = JSON.parse(data);
        if (result.data && typeof(result.data) == "string") result.data = [result.data];
        return  result;
    }
    if (data.indexOf(' ') != -1 )
        return {data: [data], action: "query"};
    else
        return {data: [data], action: "unknown"};
}

function handleMessage(msg) {
    console.log("msg",msg);
    if (msg.action == "init") {
        sendInit(msg.data, msg.call_id);
        // send init with data
        return;
    }
    if (msg.action == "query" || msg.action == "input") {
        sendNext(msg);
    }
}

function getPostQueryParams() {
    return vizVisible ? "" : "?viz=none";
}

function sendNext(msg) {
    if (msg.data.length == 0) return;
    var _query = msg.data.shift();
    if (msg.data.length == 0 && msg.action == "input") {
        inputQuery(_query)
        return;
    }
    if (_query.trim().length == 0) return;
    post("console/cypher"+getPostQueryParams(), _query, function (res) {
        if (msg.action === "query" && msg.call_id && graphgistWindow) {
            res.call_id = msg.call_id;
//            console.log("res",res);
            graphgistWindow.postMessage(JSON.stringify(res), "*");
        }
        else {
            showResults(res);
        }
        sendNext(msg);
    }, "json");
}

function close(id) {
    $(id).hide().children("iframe").removeAttr("src");
}

function sendInit(params, callId) {
    post("console/init", JSON.stringify(params), function (json) {
        showResults(json);
        showVersion(json);
        showWelcome(json);
        if (callId && graphgistWindow) {
            json.call_id = callId;
            graphgistWindow.postMessage(JSON.stringify(json), "*");
        }
    }, "json", callId);
}


function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
};

function guid() {
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
        s4() + '-' + s4() + s4() + s4();
}

function handleSelection(data, show_result_only) {
    if (!show_result_only) return data;
    var nodes = [];
    var links = [];
    var i;
    for (i = 0; i < data.nodes.length; i++) {
        var node = data.nodes[i];
        if (node.selected) {
            node['$index'] = nodes.length;
            nodes.push(node);
        }
    }
    var hasSelectedRels = data.links.filter(function(link) { return link.selected; }).length > 0
    for (i = 0; i < data.links.length; i++) {
        var link = data.links[i];
        if (link.selected || (!hasSelectedRels && data.nodes[link.source].selected && data.nodes[link.target].selected)) {
            link.source = data.nodes[link.source]['$index'];
            link.target = data.nodes[link.target]['$index'];
            links.push(link);
        }
    }
    return {nodes: nodes, links: links};
}

var session_id=guid();

$(document).ready(
    function () {
        inputeditor = CodeMirror.fromTextArea(document.getElementById("input"), {
            lineNumbers: false,
            readOnly: false,
            smartIndent: false,
            lineWrapping:true,
            scrollbarStyle:null,
            allowDropFileTypes:["cypher","cql","cyp"],
            mode: "cypher",
            theme: "neo" //,
/*
            onKeyEvent: function (inputeditor, e) {
                console.log(e);
                if (e.type == 'keydown') {
                    // resize output while typing...
                    resizeOutput();
                    if (e.which == 13 && !e.shiftKey) {
                        query();
                        // cancel normal enter (must type shift enter to add lines)
                        e.preventDefault();
                        return true;
                    }
                    // allow the rest to go through.
                    return false;
                }
            }
*/
        });
        inputeditor.setOption("extraKeys", {
            "Enter": function(cm) {cm.execCommand("newlineAndIndent"); },
            "Cmd-Enter": query,
            "Shift-Enter": query,
            "Ctrl-Enter": query
        });
        // set id to align with CSS from Neo4j Browser
        $("#form > div.CodeMirror").first().attr("id", "editor");
        // resize output if window gets resized
        $(window).resize(resizeOutput);

        window.addEventListener("message", function (e) {
            if (e.origin.match(/addthis|cloudfront.net/)) {
                return;
            }
            //console.log( "postMessage", e );
            graphgistWindow = e.source;
            var msg = parseMessage(e.data);
            handleMessage(msg);
        });

//        if (!session) {
            var params = getParameters();
            if (params['viz']=="none") {
                vizVisible=false;
            }
            sendInit(params);
//        }
//        else {
//            query();
//        }

        $('#version').change(function () {
            post("console/version", $('#version').val(), showVersion, "json");
            $(this).hide();
        });
        var isInIFrame = window.location != window.parent.location;
        if (!isInIFrame) {
            inputeditor.focus();
        }

        $("body").keyup(function (e) {
            if (e.which == 27) {
                close(".popup");
            }
            return true;
        });
        $("#video").click(
            function () {
                close("#info");
                var url = $(this).attr("video") + "?badge=0&title=0&portrait=0&autoplay=1&rel=0&byline=0";
                $("#player").show();
                $("#player iframe").attr("width", $("#player").width()).attr("height", $("#player").height())
                    .attr("src", url);
            });
        $(".popup .btn_close").click(function () {
            close($(this).parent());
        });
        //$("code[type=cypher]").each(function() {
	     //  var text=highlight($(this).text());
	     //  $(this).html(text);
		//});
    });
