/* eslint-env node */

const path = require('path');
require('@babel/register')({
    presets: ['@babel/preset-env'],
    ignore: [/node_modules/]
});

module.exports= (env, argv) => {
    const firefly_root = path.resolve(__dirname, '../..');
    const name = 'firefly';
    const entry = {firefly: path.resolve(firefly_root, 'src/firefly/js/FFEntryPoint.js')};
    const config= require(firefly_root + '/buildScript/webpack.config.js').default({firefly_root, name, entry});
    return config;
};
