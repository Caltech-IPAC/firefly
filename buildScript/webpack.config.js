/* eslint-env node */

import webpack from 'webpack';
import HtmlWebpackPlugin from 'html-webpack-plugin';
import ExtractTextPlugin from 'extract-text-webpack-plugin';
import ProgressBarPlugin from 'progress-bar-webpack-plugin';
import RewireWebpackPlugin from 'rewire-webpack';
import path from 'path';
import fs from 'fs';


var exclude_dirs = [/node_modules/, /java/, /python/, /config/, /test/];

function makeWebpackConfig(config) {
    
    // setting defaults
    config.firefly_dir = config.firefly_dir || config.src;
    config.project = config.project || path.resolve(config.src, '../../');

    var def_config = {
        env         : process.env.NODE_ENV || 'development',
        dist        : process.env.WP_BUILD_DIR || path.resolve(config.project, `build/${config.name}/gwt/${config.name}`),
        version_tag   : process.env.__VERSION_TAG__ || 'unknown',
        do_lint     : process.env.DO_LINT || process.env.DO_LINT_STRICT || false,
        index_html  : 'index.html',
        html_dir    : 'html',
        use_loader  : true,
        filename    : '[name]-dev.js',
        deploy_dir  : (process.env.HYDRA_ROOT || '/hydra') + `/server/tomcat/webapps/${config.name}`,
        alias       : {
            firefly : path.resolve(config.firefly_dir, 'js'),
            styles : path.resolve(config.firefly_dir, 'html', 'css'),
            html : path.resolve(config.firefly_dir, 'html')
        }
    };

    config.alias = Object.assign(def_config.alias, config.alias);
    config = Object.assign(def_config, config);

    var script_names = [];
    Object.keys(config.entry).forEach( (v) => {
        script_names.push(v + '.js');
    });
    const globals = {
        'process.env'   : {NODE_ENV : JSON.stringify(config.env)},
        NODE_ENV        : config.env,
        __SCRIPT_NAME__ : JSON.stringify(script_names),
        __MODULE_NAME__ : JSON.stringify(config.name),
        __VERSION_TAG__ : JSON.stringify(config.version_tag)
    };

    const DEBUG    = config.env === 'development' && process.env.DEBUG;
    const PROD      = config.env !== 'development';

    /*
     * creating the webpackConfig based on the project's config for webpack to work on.
     *
     */

    /*------------------------ OUTPUT -----------------------------*/
    var out_path = DEBUG ? config.deploy_dir : config.dist;
    var filename = config.use_loader ? '[name]-dev.js' : '[name].js';
    if (PROD) {
        filename = config.use_loader ? '[name]-[hash].js' : '[name].js';
    }
    const output =  {filename, path: out_path};

    /*------------------------ PLUGIINS -----------------------------*/
    const plugins = [ new webpack.DefinePlugin(globals),
                      new ExtractTextPlugin(`${config.name}.css`),
                      new RewireWebpackPlugin()
                    ];
    if (DEBUG) {
        plugins.push(
            new ProgressBarPlugin({
                callback : progressDone
            })
        );
    }

    if (config.use_loader) {
        plugins.push(
            firefly_loader(path.resolve(config.firefly_dir, '../../buildScript/loadScript.js'),
                out_path, DEBUG)
        );
    }

    if (PROD) {
        plugins.push(
            new webpack.optimize.OccurrenceOrderPlugin(),
            new webpack.optimize.DedupePlugin(),
            new webpack.optimize.UglifyJsPlugin({
                compress : {
                    warnings  : false,
                    unused    : true,
                    dead_code : true
                }
            })
        );
    }

    // if index_html exists, insert script tag to load built javascript bundles(s).
    if (fs.existsSync(path.resolve(config.dist, config.index_html))) {
        plugins.push(
            new HtmlWebpackPlugin({
                template : path.resolve(config.src, config.html_dir, config.index_html),
                hash     : false,
                filename : config.index_html,
                minify   : PROD,
                inject   : 'body'
            })
        );
    }


    /*------------------------ MODULE -----------------------------*/
    var loaders = [
                    {   test : /\.(js|jsx)$/,
                        exclude: exclude_dirs,
                        loaders : ['babel-loader']
                    },
                    {   test    : /\.css$/,
                        exclude: exclude_dirs,
                        loaders : [
                            'style-loader',
                            `css-loader?root=${path.resolve(config.firefly_dir, 'html')}`,
                            'autoprefixer?browsers=last 2 version'
                        ]
                    },
                    {   test: /\.(png|jpg|gif)$/,
                        loader: `url-loader?root=${path.resolve(config.firefly_dir, 'html')}`
                    }
                ];

    // commented out for now.. may want to use it later on.
    // Compile CSS to its own file in production.
    // loaders = loaders.map(loader => {
    //    if (/css/.test(loader.test)) {
    //        const [first, ...rest] = loader.loaders;
    //
    //        loader.loader = ExtractTextPlugin.extract(first, rest.join('!'));
    //        delete loader.loaders;
    //    }
    //    return loader;
    //});
    var preLoaders = [];

    if (config.do_lint) {
        let eslint_options = '';
        if (process.env.DO_LINT_STRICT) {
            // in addition to .eslintrc, extra rules are defined in .eslint-strict.json
            const eslint_strict_path = path.resolve(config.project, '.eslint-strict.json');
            if (fs.existsSync(eslint_strict_path)) {
                eslint_options = '?' + JSON.stringify(JSON.parse(fs.readFileSync(eslint_strict_path)));
                console.log('eslint-loader' + eslint_options);
            } else {
                console.log('ERROR: No .eslint-strict.json found - excluding lint');
                console.log('----------------------------------------------------');
                config.do_lint = false;
            }
        }
        if (config.do_lint) {
            preLoaders.push(
                {
                    test : /\.(js|jsx)$/,
                    exclude: exclude_dirs,
                    loaders: ['eslint-loader' + eslint_options]
                }
            );
        }
    }

    const module = {loaders, preLoaders};

    /*------------------------ ESLINT -----------------------------*/
    const eslint = {
        configFile  : path.resolve(config.project,'.eslintrc'),
        failOnError : false,
        emitWarning : false
        };


    const webpack_config = {
        name    : config.name,
        target  : 'web',
        devtool : 'source-map',
        entry   : config.entry,
        resolve : {
            extensions : ['', '.js', '.jsx'],
            alias : config.alias
        },
        module,
        output,
        plugins,
        eslint
    };

    // console.log (JSON.stringify(webpack_config, null, 2));
    return webpack_config;
}
// ----------------------------------
// Vendor Bundle Configuration
// ----------------------------------
//webpackConfig.entry.vendor = [
//    'history',
//    'immutable',
//    'react',
//    'react-redux',
//    'react-router',
//    'redux',
//    'redux-devtools',
//    'redux-devtools/lib/react'
//];
//
//// NOTE: this is a temporary workaround. I don't know how to get Karma
//// to include the vendor bundle that webpack creates, so to get around that
//// we remove the bundle splitting when webpack is used with Karma.
//const commonChunkPlugin = new webpack.optimize.CommonsChunkPlugin(
//    'vendor', '[name].[hash].js'
//);
//commonChunkPlugin.__KARMA_IGNORE__ = true;
//webpackConfig.plugins.push(commonChunkPlugin);





export default makeWebpackConfig;



function firefly_loader(loadScript, outpath, debug=true) {
    return function () {
        this.plugin('done', function (stats) {
            // console.log(Object.keys(stats.compilation));
            var hash = debug ? 'dev' : stats.hash;

            console.log('!!!!!!!!' + path.resolve(outpath, `${stats.compilation.name}.nocache.js`));

            var callback='';
            if (fs.existsSync(path.resolve(outpath, `${stats.compilation.name}.nocache.js`))) {
                console.log('good!!!!!!');
                callback = `,
                    function() {
                        loadScript('${stats.compilation.name}.nocache.js');
                    }`;
            }
            var content = fs.readFileSync(loadScript);
            content += `\nloadScript('${stats.compilation.name}-${hash}.js'${callback});`;
            console.log('after good!!!!!!');
            var loader = path.join(outpath, 'firefly_loader.js');
            fs.writeFileSync(loader, content);
        });
    };
}

var buildCnt=0;
function progressDone() {
    buildCnt++;
    process.stdout.write('\n');
    var time = new Date();
    var tStr= time.getHours() + ':' + time.getMinutes() + ':' + time.getSeconds();
    process.stdout.write('-------------------- Begin build #'+ buildCnt +
        ' at '+ tStr +'--------------------------\n');
    //process.stdout.write('Build ' +buildCnt+ ' results: '+ tStr);
}

