/* eslint-env node */

require('babel-core/register')({presets: ['es2015', 'react', 'stage-2']});
var path = require('path');

var firefly_root = path.resolve(__dirname, '../..');
var name = 'firefly';
var entry = {firefly: path.resolve(firefly_root, 'src/firefly/js/FFEntryPoint.js')};

module.exports = require(firefly_root + '/buildScript/webpack.config.js').default({firefly_root, name, entry});
