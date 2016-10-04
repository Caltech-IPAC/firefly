
/* eslint-env node */

var webpackConfig = require('./webpack.config.js');
webpackConfig.entry = {};

module.exports = function(config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', 'chai'],
    reporters: ['mocha', 'progress', 'coverage'],
    port: 9876,
    colors: false,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['Chrome'],
    singleRun: false,
    autoWatchBatchDelay: 300,
    coverageReporter: {
      type: 'lcov',
      dir: 'coverage/'
    },
    files: [
      'js/!**!/!*-test.js'
    ],
    preprocessors: {
      'js/!**!/!*-test.js': ['webpack'],
      '*.js': ['coverage']
    },

    webpack: webpackConfig,

    webpackMiddleware: {
      noInfo: true
    }
  });
};
