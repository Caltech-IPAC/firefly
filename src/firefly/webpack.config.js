/* eslint-env node */

require('babel/register');
var path = require('path');

var firefly_root = path.resolve(__dirname, '../..');

/* global config:true */

config = {
	name : 'firefly',
	src : __dirname,
	entry : {firefly: path.resolve(firefly_root, 'src/firefly/js/FFEntryPoint.js')},
	firefly_dir : path.resolve(firefly_root, 'src/firefly')
};

module.exports = require(firefly_root + '/buildScript/webpack.config.js')(config);
