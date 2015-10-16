var webpack = require('webpack');
const RewireWebpackPlugin = require("rewire-webpack"); // injection dependency for use later

module.exports = function(config) {
  config.set({
    basePath: '.',

    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['Chrome'], //'PhantomJS', 'Firefox'

    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: false,
    
    coverageReporter: {
      type: 'lcov',
      dir: 'coverage/'
    },
    files: [
      'js/**/*-test.js'
    ],
    frameworks: ['mocha', 'chai', 'sinon-chai'],
    preprocessors: {
    	 'js/**/*-test.js': ['webpack'],
    	 '*.js': ['coverage']
    },
    reporters: ['mocha', 'progress', 'coverage'],
    webpack: {
    	devtool: 'inline-source-map',
      resolve: {
        extensions: [ '', '.js', '.jsx' ]
      },
      module: {
        loaders: [{
          test:  /\.(js|jsx)$/,
          exclude: /node_modules/,
          loader: 'babel-loader'
        }, 
        
        /*{
          test: /\.less$/,
          loader: 'style-loader!css-loader?modules&importLoaders=1' +
              '&localIdentName=[name]__[local]___[hash:base64:5]' +
              '!less-loader'
        }
        */
        ],
        // for coverage 
        postLoaders: [{
          test: /\.jsx?$/,
          exclude: /(node_modules|tests)\//,
          loader: 'istanbul-instrumenter'
        }]
      },
     plugins:[ 
          new webpack.DefinePlugin({
        	  'process.env.NODE_ENV' : JSON.stringify('test')
          }),
          new RewireWebpackPlugin()
          ]
    },
    webpackServer: {
    	noInfo: false
    },
 // web server port
    port: 9877,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_ERROR,

    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,
  });
};