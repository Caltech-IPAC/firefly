/* eslint-env node */
/* global config:true */

require('babel/register');
var path = require('path');

var firefly_root = path.resolve(__dirname, '../..');

config = {
    name    : 'fftools',
    src     : __dirname,
    entry   : {
        fftools: path.resolve(__dirname, 'js/fftools.js'),
        fflib: path.resolve(firefly_root, 'src/firefly/js/fireflyJSLib.js')
    },
    firefly_dir : path.resolve(firefly_root, 'src/firefly')
};

module.exports = require(firefly_root + '/buildScript/webpack.config.js');
