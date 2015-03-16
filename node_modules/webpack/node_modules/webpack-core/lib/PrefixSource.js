/*
	MIT License http://www.opensource.org/licenses/mit-license.php
	Author Tobias Koppers @sokra
*/
var SourceMapNodeFastSource = require("./SourceMapNodeFastSource");
var SourceNode = require("source-map").SourceNode;

function PrefixSource(prefix, source) {
	this._source = source;
	this._prefix = prefix;
	SourceMapNodeFastSource.call(this);
}
module.exports = PrefixSource;

PrefixSource.prototype = Object.create(SourceMapNodeFastSource.prototype);
PrefixSource.prototype.constructor = PrefixSource;

PrefixSource.prototype._bakeSource = function() {
	var node = typeof this._source === "string" ? this._source : this._source.source();
	var prefix = this._prefix;
	return prefix + node.replace(/\n(.)/g, "\n" + prefix + "$1");
};

PrefixSource.prototype._bake = function() {
	var node = this._source.node();
	var append = [this._prefix];
	return new SourceNode(null, null, null, [
		this._cloneAndPrefix(node, this._prefix, append)
	]);
};

PrefixSource.prototype._cloneAndPrefix = function cloneAndPrefix(node, prefix, append) {
	if(typeof node === "string") {
		var result = node.replace(/\n(.)/g, "\n" + prefix + "$1");
		if(append.length > 0) result = append.pop() + result;
		if(/\n$/.test(node)) append.push(prefix);
		return result;
	} else {
		var newNode = new SourceNode(
			node.line,
			node.column,
			node.source,
			node.children.map(function(node) {
				return cloneAndPrefix(node, prefix, append);
			}),
			node.name
		);
		newNode.sourceContents = node.sourceContents;
		return newNode;
	}
};
