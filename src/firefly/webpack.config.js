require('babel/register');
var path = require('path');

var firefly_root = path.resolve(__dirname, '../..');

/* global config:true */

config = {
	name : 'fftools', // app directory where filename.js should be found
	filename : 'fflib.js', //look ../build/gwt/fftools/
	src : __dirname,
	entry : path.resolve(firefly_root, 'src/firefly/js/fireflyJSLib.js'),
	firefly_dir : path.resolve(firefly_root, 'src/firefly')
};

module.exports = require(firefly_root + '/buildScript/webpack.config.js');
