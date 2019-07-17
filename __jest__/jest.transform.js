// Custom Jest transform implementation that wraps babel-jest and injects our
// babel presets, so we don't have to use .babelrc.

/*eslint no-undef: "error"*/
/*eslint-env node*/

module.exports = require('babel-jest').createTransformer({
  presets: [
      '@babel/preset-env',
      '@babel/preset-react',
     ],
    plugins:[
        '@babel/plugin-transform-runtime',
        '@babel/plugin-proposal-class-properties'
    ]
});
