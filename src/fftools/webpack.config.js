/* eslint-env node */

require('babel/register');
var path = require('path');

var firefly_root = path.resolve(__dirname, '../..');
var name = 'fftools';
var entry = {fflib: path.resolve(firefly_root, 'src/firefly/js/fireflyJSLib.js')};

module.exports = require(firefly_root + '/buildScript/webpack.config.js')({firefly_root, name, entry, user_loader: false});
