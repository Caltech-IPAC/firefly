/*
	MIT License http://www.opensource.org/licenses/mit-license.php
	Author Tobias Koppers @sokra
*/
var SourceNode = require("source-map").SourceNode;
var SourceMapConsumer = require("source-map").SourceMapConsumer;
var SourceMapNodeSource = require("./SourceMapNodeSource");

function CheapOriginalSource(value, name) {
	var lines = value.split("\n");
	var node = new SourceNode(null, null, null,
		lines.map(function(line, idx) {
			var pos = 0;
			return new SourceNode(idx+1, 0, name,
				(line + (idx != lines.length-1 ? "\n" : ""))
			);
		})
	);
	this._value = value;
	SourceMapNodeSource.call(this, node, name);
}

module.exports = CheapOriginalSource;

CheapOriginalSource.prototype = Object.create(SourceMapNodeSource.prototype);
CheapOriginalSource.prototype.constructor = CheapOriginalSource;
CheapOriginalSource.prototype.source = function() {
	return this._value;
};
CheapOriginalSource.prototype.updateHash = function(hash) {
	hash.update(this._value);
	hash.update(this._name);
};
