/* eslint-env node */

import webpack from 'webpack';
import ExtractTextPlugin from 'extract-text-webpack-plugin';
import path from 'path';
import fs from 'fs';


var exclude_dirs = /(node_modules|java|python|config|test)/;

/**
 * A helper function to create the webpack config object to be sent to webpack module bundler. 
 * @param {Object}  config configuration parameters used to create the webpack config object.
 * @param {string}  config.src  source directory
 * @param {string}  config.firefly_root  Firefly's build root
 * @param {string}  [config.firefly_dir]  Firefly's JS source directory
 * @param {Object}  [config.alias]  additional alias
 * @param {boolean=true} [config.use_loader]  generate a loader to load compiled JS script(s).  Defautls to true
 * @param {string}  [config.project]  project name
 * @param {string}  [config.filename]  name of the generated JS script.
 * @returns {Object} a webpack config object.
 */
export default function makeWebpackConfig(config) {

    // setting defaults
    config.src = config.src || process.cwd();
    config.firefly_root = config.firefly_root || path.resolve(config.src, '../..');
    config.firefly_dir = config.firefly_dir || path.resolve(config.firefly_root, 'src/firefly');
    config.project = config.project || path.resolve(config.src, '../../');
    config.baseWarName = config.baseWarName || config.name; 

    var def_config = {
        env         : process.env.NODE_ENV || 'development',
        dist        : process.env.WP_BUILD_DIR || path.resolve(config.project, `build/${config.name}/war`),
        do_lint     : process.env.DO_LINT || process.env.DO_LINT_STRICT || false,
        html_dir    : 'html',
        use_loader  : true,
        filename    : '[name]-dev.js',
        deploy_dir  : (process.env.HYDRA_ROOT || '/hydra') + `/server/tomcat/webapps/${config.baseWarName}`,
        alias       : {
            firefly : path.resolve(config.firefly_dir, 'js'),
            styles : path.resolve(config.firefly_dir, 'html', 'css'),
            html : path.resolve(config.firefly_dir, 'html')
        }
    };

    config.alias = Object.assign(def_config.alias, config.alias);
    config = Object.assign(def_config, config);

    var script_names = [];
    if (config.use_loader) {
        script_names = ['firefly_loader.js'];
    } else {
        Object.keys(config.entry).forEach( (v) => {
            script_names.push(v + '.js');
        });
    }

    const globals = {
        'process.env'   : {NODE_ENV : JSON.stringify(config.env)},
        NODE_ENV        : config.env,
        __PROPS__       : {
            SCRIPT_NAME : JSON.stringify(script_names),
            MODULE_NAME : JSON.stringify(config.name)
        }

    };

    // add all of the env that starts with '__$' as global props
    Object.keys(process.env).filter((k) => k.startsWith('__$')).forEach((k) => {
        globals.__PROPS__[k.substring(3)] = JSON.stringify(process.env[k]);
    });

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
                      new ExtractTextPlugin(`${config.name}.css`)
                    ];
    if (DEBUG) {
        plugins.push(
            dev_progress()
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

    /*------------------------ MODULE -----------------------------*/
    var loaders = [
                    {   test : /\.(js|jsx)$/,
                        include: [config.src, config.firefly_dir,
                                 `${config.firefly_root}/node_modules/react-component-resizable/`],
                        loader: 'babel',
                        query: {
                            presets: ['es2015', 'react', 'stage-2'],
                            plugins: ['transform-runtime']
                        }
                    },
                    {   test    : /\.css$/,
                        exclude: exclude_dirs,
                        loaders : [
                            'style-loader',
                            `css-loader?root=${path.resolve(config.firefly_dir, 'html')}`,
                            'postcss-loader'
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




function firefly_loader(loadScript, outpath, debug=true) {
    return function () {
        this.plugin('done', function (stats) {
            // console.log(Object.keys(stats.compilation));
            var hash = debug ? 'dev' : stats.hash;
            var cxt_name = stats.compilation.name;

            var callback='';
            if (fs.existsSync(path.resolve(outpath, 'jsinterop.nocache.js'))) {
                callback = `,
                    function() {
                        loadScript('firefly_loader.js', 'jsinterop.nocache.js');
                    }`;
            }
            var content = fs.readFileSync(loadScript);
            content += `\nloadScript('firefly_loader.js', 'firefly-${hash}.js'${callback});`;
            var loader = path.join(outpath, 'firefly_loader.js');
            fs.writeFileSync(loader, content);
        });
    };
}

function dev_progress() {
    return new webpack.ProgressPlugin(function (percent, msg) {
        if (msg.startsWith('compile')) {
            process.stdout.write('\n\x1b[1;31m> Compiling new changes\x1b[0m');   // set color to red.  for more options.. import a color lib.
        }
        if  (percent === 1) {
            process.stdout.write('\x1b[0m\n');
            setTimeout(() => process.stdout.write('\n\x1b[32m\x1b[1m> Build completed: ' + new Date().toLocaleTimeString() + '\x1b[0m \n'));
        } else if (percent * 100 % 5 === 0) {
            process.stdout.write('.');
        };
    });
}

