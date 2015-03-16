/*
	MIT License http://www.opensource.org/licenses/mit-license.php
	Author Tobias Koppers @sokra
*/
var SourceMapNodeSource = require("./SourceMapNodeSource");

function SourceMapNodeFastSource(name) {
	SourceMapNodeSource.call(this, null, name);
}
module.exports = SourceMapNodeFastSource;

SourceMapNodeFastSource.prototype = Object.create(SourceMapNodeSource.prototype);
SourceMapNodeFastSource.prototype.constructor = SourceMapNodeFastSource;
SourceMapNodeFastSource.prototype._bakeSource = function() {
	throw new Error("Overwrite or pass valid SourceNode to constructor");
};
SourceMapNodeFastSource.prototype.source = function() {
	if(this._backedSource) return this._backedSource;
	if(this._node) return SourceMapNodeSource.prototype.source.call(this);
	return this._backedSource = this._bakeSource();
};
SourceMapNodeFastSource.prototype.size = function() {
	if(this._backedSource) return this._backedSource.length;
	if(this._node) return SourceMapNodeSource.prototype.size.call(this);
	this._backedSource = this._bakeSource();
	return this._backedSource.length;
};
