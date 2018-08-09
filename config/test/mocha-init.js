/* eslint-env node */
require('babel-core/register')(
    {
        'presets': [ 'env','stage-3'],
        'plugins': ['transform-runtime']
    }
);