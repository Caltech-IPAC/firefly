/* eslint-env node */

require('babel/register');
var path = require('path');

var firefly_root = path.resolve(__dirname, '../..');

var config = {
    name    : 'fftools',
    src     : __dirname,
    use_loader: false,
    entry   : {
        fflib: path.resolve(firefly_root, 'src/firefly/js/fireflyJSLib.js')
    },
    firefly_dir : path.resolve(firefly_root, 'src/firefly')
};

module.exports = require(firefly_root + '/buildScript/webpack.config.js')(config);
