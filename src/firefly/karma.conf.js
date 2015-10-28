var path = require('path');

var firefly_root = path.resolve(__dirname, '../..');

/* global config:true */

//config = {
//	files : 'js/**/*-test.js',
//	preprocessors : {
//		'js/**/*-test.js' : [ 'webpack' ]
//	}
//}
module.exports = require(firefly_root + '/buildScript/karma.conf.js');
