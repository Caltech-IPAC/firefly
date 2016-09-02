/* eslint-env node */

require('babel-core/register')({presets: ['es2015']});
var path = require('path');

var firefly_root = path.resolve(__dirname, '../..');
var name = 'fftools';
var entry = {fflib: path.resolve(firefly_root, 'src/firefly/js/fireflyJSLib.js')};

module.exports = require(firefly_root + '/buildScript/webpack.config.js').default({firefly_root, name, entry, use_loader: false});
