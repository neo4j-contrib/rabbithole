var inputeditor;
      
function append(element, text) {
  if (!text) {
    return;
  }
  if (typeof(text) == 'object') {
     text = JSON.stringify(text);
  }
  var newtext = "";
  function appendToNewtext(text, typeClass) {
    if(typeClass == null) {
       newtext = newtext + text;
    } else {
       newtext = newtext + "<span class=\"cm-" + typeClass + "\">" + text + "</span>";
    }
  }
  text = "\n" + text;
  CodeMirror.runMode(text, "cypher", appendToNewtext);

  element.html(element.html() + newtext);
  element.prop("scrollTop", element.prop("scrollHeight") - element.height());
}


function highlight(text) {
  if (!text) {
    return text;
  }
  text = text.replace(/<null>/gi, '&lt;null&gt;');
  if (text[0] == "(" || text[0] == "[") {
    // Geoff
    // colour nodes
    text = text.replace(/(\([0-9A-Za-z_]*?\))/gi, '<span class="node">$1</span>');
    // colour rels
    text = text.replace(/((<?-)?\[[0-9A-Za-z_:\.]*?\](->?)?)/gi, '<span class="relationship">$1</span>');
  } else if (isCypher(text)) {
    // Cypher
    text = text.replace(/\b(start|with|create|delete|relate|skip|limit|order by|set|match|where|return)\b/gi, '$1');
    text = text.replace(/\b(start|with|create|delete|relate|skip|limit|distinct|desc|asc|as|order by|foreach|set|match|where|return)\b/gi, '<span class="keyword">$1</span>');
    text = text.replace(/\b(and|or|not|has|node)\b/gi, '<span class="keyword">$1</span>');
    text = text.replace(/\b(type|collect|sum|sqrt|round|max|min|nodes|count|length|avg|rels)\b\(/gi, '<span class="function">$1</span>(');
  }
  return text;
}

function post(uri, data, done, dataType) {
  data = data.trim();
  //console.log("Post data: " + data);
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
  if (!pairs) {
    return {};
  }
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
  if ($('#graph').is(":hidden")) {
    return;
  }
  var graph = $("#graph");
  graph.empty();
  if (data) {
    visualize("graph", graph.width(), graph.height(),data)
  } else {
    var query = getQuery();
    if (!isCypher(query)) {
      query = "";
    }
    render("graph", graph.width(), graph.height(), "/console/visualization?query=" + encodeURIComponent(query));
  }
  graph.show();
}

function reset(done) {
  $.ajax("/console", { type:"DELETE", success:done });
  return false;
}

function share_yuml(query) {
  if (!query) {
    return;
  }
  $.ajax("/console/to_yuml?query="+encodeURIComponent(query), {
    type: "GET", dataType: "text",
    success: function(data) {
      $('#share_yuml').attr("href",data);
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
  if (id && (id.length==0 || id.match("http://.+"))) {
    id=null;
  }
  var message = null;
  // var uri = base_url()+'?init='+encodeURIComponent(init)+'&query='+encodeURIComponent(query)+'&version='+encodeURIComponent(version);
  // if (no_root) uri+="&no_root=true";

  $.ajax("/r/share", {
    type: "POST",
    dataType: "text",
    contentType: "application/json",
    data: JSON.stringify({
      id : id,
      init : init,
      query : query,
      version : version,
      no_root : no_root,
      message : message
    }),
    success: function(data) {
      var uri = (data.indexOf("http")!=0) ? base_url() + "/r/" + data : data;
      //console.log(uri);
      var frame = '<iframe width="600" height="300" src="'+uri+'"/>';
      $('#share_iFrame').val(frame);
      $('#share_url').attr("href",uri);
      $('#share_short').val(uri);
      addthis.update('share', 'url', uri);
    }
  });
  share_yuml(query);
  addthis.update('share', 'title', 'Look at this #Neo4j graph: ');
}

function toggleGraph() {
  $('#graph').toggle();
  if ($('#graph').is(":visible")) {
    viz();
  }
}

function toggleShare() {
  $.ajax("console/to_cypher", {
    type:"GET",
    dataType: "text",
    success:function (data) {
      $('#share_init').val(data);
      var query = getQuery();
      query = isCypher(query) ? query : null;
      $('#share_query').val(query);
      $('#shareUrl').toggle();
      share_yuml(query)
    },
    error:function (data, error) {
      append($("#output"), "Error: "+error+"\n" + data.responseText+"");
    }
  });
}

function export_graph(format) {
  $.ajax("console/to_"+format, {
    type:"GET",
    dataType: "text",
    success:function (data) {
      $('#share_init').val(data);
    },
    error:function (data, error) {
      $('#share_init').val("Error: "+error+"\n" + data.responseText+"");
    }
  });
}

function showResults(data) {
  if (data["init"]) {
    append($("#output"), "Graph Setup:");
    append($("#output"), data["init"]);
    append($("#output"), "--------------------------------------------------------------------------------");
  }
  if (data["query"]) {
    inputeditor.setValue(data["query"].replace(/\n/g, '').trim());
    CodeMirror.commands["selectAll"](inputeditor);
    autoFormatSelection(inputeditor);
    append($("#output"), inputeditor.getValue());
    resizeOutput();
  }
  if (data["result"]) {
    append($("#output"), data["result"]);
//	    append($("#output"), "--------------------------------------------------------------------------------");
  }
  if (data["error"]) {
    append($("#output"), "Error: " + data["error"]);
  }
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

function showVersion(json) {
  $('#version').val(json['version']);
  $('#share_version').val(json['version']);
}

function welcome_msg() {
  return $("#welcome_msg").html();
}

function showWelcome(json) {
  var output = $("#output");
  output.html(output.html() + welcome_msg());
  if (json['message']) {
    output.html(output.html() + json['message']);
  }
}

function resizeOutput() {
  $("#output").css({'height':($(window).height() - $(".CodeMirror-scroll").height() - 15)+'px'});
  $("#output").animate({ scrollTop: $("#output").prop("scrollHeight") }, 10);
}

function getSelectedRange(editor) {
  return { from: editor.getCursor(true), to: editor.getCursor(false) };
}
     
function autoFormatSelection(editor) {
  var range = getSelectedRange(editor);
  editor.autoFormatRange(range.from, range.to);
}

function getQuery() {
  return inputeditor.getValue();
}

$(document).ready(function () {
  inputeditor = CodeMirror.fromTextArea(document.getElementById("input"), {
    lineNumbers: false,
    readOnly: false,
    mode: "cypher",
    theme: "cypher",
    onKeyEvent: function(inputeditor, e) {
      if(e.type == 'keydown') {
        // resize output while typing...
        resizeOutput();
        if(e.keyCode == 13 && !e.shiftKey) {
          inputeditor.setValue(getQuery().replace(/\n/g, '').trim());
          CodeMirror.commands["selectAll"](inputeditor);
          autoFormatSelection(inputeditor);
          resizeOutput();
          send(getQuery());
          // cancel normal enter (must type shift enter to add lines)
          e.stop();
          return true;
        }
        // allow the rest to go through.
        return false;
      }
    }
  });
  
  post( "/console/init", JSON.stringify( getParameters() ), function ( json ) {
      showResults( json );
      showVersion( json );
      showWelcome( json )
    }, "json" );

  $('#version').change(function() { post("/console/version",$('#version').val(),showVersion,"json")});
  var isInIFrame = window.location != window.parent.location;
  if (!isInIFrame) {
    inputeditor.focus();
  }

  $("body").keyup(function(e) {
    if (e.keyCode == 27) {
      $(".popup").hide();
    }
    return true;
  });
});
