var color = d3.scale.category20();
var color2 = d3.scale.category10();

function hash(s) {
	if (!s) return 0;
	for(var ret = 0, i = 0, len = s.length; i < len; i++) {
    	ret = (31 * ret + s.charCodeAt(i)) << 0;
  	}
	return ret;
}

var ignore = { source: 1, target:1, type:1, selected:1, index:1, x:1, y:1, weight:1, px:1,py:1}
function propertyHash(ob) {
	var ret=0;
	for (var prop in ob) {
		console.log(prop)
		if (ignore.hasOwnProperty(prop)) continue;
		if (ob.hasOwnProperty(prop)) {
			ret += hash(prop);
		}
	}
	return ret;
}

function toString(ob) {
	var ret="";
	for (var prop in ob) {
		if (ignore.hasOwnProperty(prop)) continue;
		if (ob.hasOwnProperty(prop)) {
			ret += prop +": "+ob[prop]+" ";
		}
	}
	return ret;
}

function render(id,w,h,url) {
d3.json(url, function(data) {
  var vis = d3.select("#"+id).append("svg")
    .attr("width", w)
    .attr("height", h);

    var force = self.force = d3.layout.force()
        .nodes(data.nodes)
        .links(data.links)
        .gravity(.05)
        .distance(100)
        .charge(-100)
        .size([w, h])
        .start();

    var link = vis.selectAll("line.link")
        .data(data.links)
        .enter().append("svg:line")
        .attr("class", "link")
        .attr("x1", function(d) { return d.source.x; })
        .attr("y1", function(d) { return d.source.y; })
        .attr("x2", function(d) { return d.target.x; })
        .attr("y2", function(d) { return d.target.y; });

    var node = vis.selectAll("g.node")
        .data(data.nodes)
        .enter().append("circle")
        .attr("class", "node")
	    .attr("r", 5)
	    .style("fill", function(d) { return color(propertyHash(d) % 20); })
	  	.style("stroke", function(d) { var sel = d["selected"]; return sel ? "red" /* was d3.rgb(color2(hash(sel) % 20)).brighter() */ : null; })
        .call(force.drag);

	  node.append("title")
	      .text(function(d) { return toString(d); });

/*
    node.append("svg:image")
        .attr("class", "circle")
        .attr("xlink:href", "https://d3nwyuy0nl342s.cloudfront.net/images/icons/public.png")
        .attr("x", "-8px")
        .attr("y", "-8px")
        .attr("width", "16px")
        .attr("height", "16px");

    node.append("svg:text")
        .attr("class", "nodetext")
        .attr("dx", 12)
        .attr("dy", ".35em")
        .text(function(d) { return d.name });
*/

    force.on("tick", function() {
      link.attr("x1", function(d) { return d.source.x; })
          .attr("y1", function(d) { return d.source.y; })
          .attr("x2", function(d) { return d.target.x; })
          .attr("y2", function(d) { return d.target.y; });

      node.attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });
	});
});
}
