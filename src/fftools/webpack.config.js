/*jshint node:true*/
//var webpack = require('webpack');
"use strict";
var path = require('path');
//var strUtil = require('underscore.string');



var release = (process.env.NODE_ENV === 'production');

//if (release)  {
//  plugins.push(new webpack.DefinePlugin({
//    'process.env': {
//      // This has effect on the react lib size
//      'NODE_ENV': JSON.stringify('production')
//    }
//  }));
//
//  plugins.push(new webpack.optimize.DedupePlugin());
//  plugins.push(new webpack.optimize.UglifyJsPlugin());
//} else {
//  jsxLoader = ['react-hot', 'jsx?harmony'];
//}


var ffRoot= __dirname+ '/../../';
var project = {buildDir : ffRoot+"build/"};

module.exports = {





  entry: __dirname+'/js/demoform.jsx',
  output: {
    path: project.buildDir+'gwt/fftools',
    filename: 'out.js'
  },

  plugins : {},

  resolve: {
      root: [path.resolve(ffRoot+'src/firefly/js'),
             path.resolve(ffRoot+ 'node_modules')],
      extensions: ['', '.js', '.jsx']
  },

  module: {
        loaders: [
          { test: /\.(js|jsx)$/,
            exclude: /node_modules/,
            //loader: 'jsx-loader?harmony'
            loader: 'babel-loader'
          }
        ]
  }
};

//console.log("arg 1"+process.argv[1]);
//var myRoot= process.argv.reduce(function(last,param) {
//    var retval= last;
//    if (strUtil.startsWith(param,"myRoot")) {
//        return param;
//    }
//    return retval;
//},null);
//console.log("myRoot="+myRoot);
//console.log("ffRoot="+ffRoot);
//console.log("ffRoot="+ffRoot);
//console.log("0="+module.exports.resolve.root[0]);
//console.log("output.path="+module.exports.output.path);
