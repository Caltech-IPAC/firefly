/* eslint-env node */

require('@babel/register')({
    presets: ['@babel/preset-env'],
    ignore: [/node_modules/]
});

var path = require('path');
var firefly_root = path.resolve(__dirname, '../..');
var dist = path.resolve(firefly_root, 'build/firefly/war/onlinehelp');
var deploy_dir =  dist;
var name = 'onlinehelp';
var entry = {firefly: path.resolve(firefly_root, 'src/onlinehelp/src/index.js')};

const builder = require(firefly_root + '/src/onlinehelp/src/builder.js').default;
const toc = require(firefly_root + '/src/onlinehelp/src/toc').toc;

const doLast = function (config) { builder(config, toc); };
module.exports = require(firefly_root + '/buildScript/webpack.config.js').default({firefly_root, deploy_dir, dist, name, entry, doLast});
