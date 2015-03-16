/*
	MIT License http://www.opensource.org/licenses/mit-license.php
	Author Tobias Koppers @sokra
*/
var SourceMapNodeFastSource = require("./SourceMapNodeFastSource");
var SourceNode = require("source-map").SourceNode;

function ConcatSource() {
	this.children = Array.prototype.slice.call(arguments);
	SourceMapNodeFastSource.call(this);
}
module.exports = ConcatSource;

ConcatSource.prototype = Object.create(SourceMapNodeFastSource.prototype);
ConcatSource.prototype.constructor = ConcatSource;

ConcatSource.prototype._bakeSource = function() {
	return this.children.map(function(item) {
		return typeof item === "string" ? item : item.source();
	}).join("");
};

ConcatSource.prototype._bake = function() {
	var node = new SourceNode(null, null, null, this.children.map(function(item) {
		return typeof item === "string" ? item : item.node();
	}));
	return node;
};

ConcatSource.prototype.add = function(item) {
	this.children.push(item);
	if(this._node) {
		if(typeof item === "string")
			this._node.add(item);
		else
			this._node.add(item.node());
	}
};
