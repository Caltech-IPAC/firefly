/* eslint-env node */

require('babel/register');
var path = require('path');

var firefly_root = path.resolve(__dirname, '../..');
var name = 'firefly';
var entry = {firefly: path.resolve(firefly_root, 'src/firefly/js/FFEntryPoint.js')};

module.exports = require(firefly_root + '/buildScript/webpack.config.js')({firefly_root, name, entry});
