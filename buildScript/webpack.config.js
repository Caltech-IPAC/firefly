/* eslint-env node */
/* global process */

import webpack from 'webpack';
import TerserPlugin from 'terser-webpack-plugin';
import path from 'path';
import fs from 'fs';


process.traceDeprecation = true;

/**
 * A helper function to create the webpack config object to be sent to webpack module bundler.
 * @param {Object}  config configuration parameters used to create the webpack config object.
 * @param {string}  config.src  an array of source directories
 * @param {string}  config.firefly_root  Firefly's build root
 * @param {string}  [config.firefly_dir]  Firefly's JS source directory
 * @param {Object}  [config.alias]  additional alias
 * @param {boolean=true} [config.use_loader]  generate a loader to load compiled JS script(s).  Defaults to true
 * @param {string}  [config.project]  project name
 * @param {string}  [config.filename]  name of the generated JS script.
 * @param {string}  [config.baseWarName]  name of the the war file base, defaults to config.name
 * @param {function}  [config.doFirst]  execute with the original config param if given.
 * @param {function}  [config.doLast]   execute with the created webpack_config param if given.
 * @returns {Object} a webpack config object.
 *
 *
 * note- removed unmaintained webpack-visualizer-plugin, maybe replace with webpack-bundle-analyzer in the future
 */
export default function makeWebpackConfig(config) {

    const ENV_DEV_MODE= process.env.DEV_MODE;
    const {BUILD_ENV='local'}   = process.env;
    const localBuild= BUILD_ENV === 'local';

    if (!process.env.NODE_ENV) {
        process.env.NODE_ENV = ['local', 'dev'].includes(BUILD_ENV) ? 'development' : 'production';
    }

    console.log('ENV_DEV_MODE: ' + ENV_DEV_MODE);
    console.log('BUILD_ENV   : ' + BUILD_ENV);
    console.log('NODE_ENV    : ' + process.env.NODE_ENV);

    if (config.doFirst) config.doFirst(config);

    // setting defaults
    config.src = config.src || [process.cwd()];
    config.firefly_root = config.firefly_root || path.resolve(process.cwd(), '../..');
    config.firefly_dir = config.firefly_dir || path.resolve(config.firefly_root, 'src/firefly');
    config.project = config.project || path.resolve(process.cwd(), '../../');
    config.baseWarName = config.baseWarName || config.name;

    const def_config = {
        dist        : process.env.WP_BUILD_DIR || path.resolve(config.project, `build/${config.name}/war`),
        do_lint     : process.env.DO_LINT || process.env.DO_LINT_STRICT || false,
        html_dir    : 'html',
        use_loader  : true,
        loaderPostfix: '_loader.js',
        filename    : '[name]-dev.js',
        deploy_dir  : (process.env.tomcat_home || process.env.CATALINA_BASE || process.env.CATALINA_HOME) + `/webapps/${config.baseWarName}`,
        alias       : {
            firefly : path.resolve(config.firefly_dir, 'js'),
            styles : path.resolve(config.firefly_dir, 'html', 'css'),
            images : path.resolve(config.firefly_dir, 'html', 'images'),
            html : path.resolve(config.firefly_dir, 'html')
        }
    };

    config.alias = Object.assign(def_config.alias, config.alias);
    config = Object.assign(def_config, config);
    const nameRoot= Object.keys(config.entry)[0];

    let script_names = [];
    if (config.use_loader) {
        script_names = [getLoadScript(nameRoot,config.loaderPostfix)];
    } else {
        Object.keys(config.entry).forEach( (v) => {
            script_names.push(v + '.js');
        });
    }

    const globals = {
        __PROPS__       : {
            BUILD_ENV   : JSON.stringify(process.env.BUILD_ENV),
            SCRIPT_NAME : JSON.stringify(script_names),
            MODULE_NAME : JSON.stringify(config.name)
        }

    };

    // add all of the env that starts with 'FF___' as global props
    Object.keys(process.env).filter((k) => k.startsWith('FF___')).forEach((k) => {
        const rkey = k.substring(5).replace(/___/g, '.');
        globals.__PROPS__[rkey] = JSON.stringify(process.env[k]);
        // console.log('<<<<>> ' + rkey + ': ' +  process.env[k]);

    });

    /*
     * creating the webpackConfig based on the project's config for webpack to work on.
     *
     */

    /*------------------------ OUTPUT -----------------------------*/
    const out_path = ENV_DEV_MODE ? config.deploy_dir : config.dist;
    let filename = config.use_loader ? '[name]-dev.js' : '[name].js';
    let workerFilename= '[name].worker.js';
    if (!localBuild) {
        filename = config.use_loader ? '[name]-[fullhash].js' : '[fullhash].js';
        workerFilename= '[name]-[fullhash].worker.js';
    }
    const output =  {filename, path: out_path};

    /*------------------------ PLUGINS -----------------------------*/
    const plugins = [ new webpack.DefinePlugin(globals)];
    if (ENV_DEV_MODE) plugins.push( dev_progress() );

    if (config.use_loader) {
        plugins.push(
            firefly_loader(
                path.resolve(config.firefly_dir, '../../buildScript/loadScript.js'),
                out_path, nameRoot, config.loaderPostfix, localBuild)
        );
    }

    /*------------------------ RULES -----------------------------*/
    const rules = [
        {
            test : /\.(js|jsx)$/,
            include: [config.firefly_dir].concat(config.src),
            use: {
                loader: 'babel-loader',
                // later presets run before earlier for each AST node
                // use 'es2015', {modules: false}] for es5 with es6 modules
                options: {
                    presets: [
                        ['@babel/preset-env',
                            {
                                targets: {
                                    browsers: ['safari >= 15', 'chrome >= 115', 'firefox >= 115', 'edge >= 115']
                                },
                                debug: false,
                                modules: false,  // preserve application module style - in our case es6 modules
                                useBuiltIns : 'usage',
                                corejs: '3.37' // should specify the minor version: https://babeljs.io/docs/babel-preset-env#corejs
                            }
                        ],
                        '@babel/preset-react'
                    ],
                    plugins: [ '@babel/plugin-transform-runtime', 'lodash' ]
                }
            }
        },
        {
            test    : /\.worker\.js$/,
            loader: 'worker-loader',
            options: {
                filename: workerFilename,
                inline: localBuild ? undefined : 'fallback',
            }
        },
        {
            resourceQuery: /raw/, //see https://webpack.js.org/guides/asset-modules/#replacing-inline-loader-syntax
            type: 'asset/source',
        },
        {
            test    : /\.css$/,
            resourceQuery: { not: [/raw/] }, // to exclude raw css assets from being processed by other loaders
            use: [ { loader: 'style-loader' }, { loader: 'css-loader' } ]
        },
        {
            test: /\.svg$/,
            use: ['@svgr/webpack'],
        },
        {
            test: /\.(png|jpg|gif)$/,
            type: 'asset/inline'
        }
    ];

    if (!ENV_DEV_MODE) { // Adding this so we see it in the log file of our builds
        console.log('Building client with Global Props:');
        console.log(globals.__PROPS__);
    }

    const webpack_config = {
        name    : config.name,
        mode    : process.env.NODE_ENV,
        target  : 'web',
        devtool : process.env.NODE_ENV!=='production' ? 'source-map' : false,
        optimization: {
            minimizer: [ new TerserPlugin({ terserOptions: {safari10: true} }) ]
        },
        entry   : config.entry,
        resolve : {
            extensions : ['.js', '.jsx'],
            alias : config.alias
        },
        module: {rules},
        output,
        plugins,
        stats: {
            builtAt: true,
            cached: false,
            children: true,
            excludeModules: () => true,
        },
        performance: { hints: false }  // Warning disabled the references: https://webpack.js.org/guides/code-splitting/
    };

    // console.log('!!! config:\n' + JSON.stringify(webpack_config, '', 2) );

    if (config.doLast) config.doLast(webpack_config);

    return webpack_config;
}

const getLoadScript= (nameRoot, loaderPostfix) => `${nameRoot}${loaderPostfix}`;


function firefly_loader(loadScript, outpath, nameRoot, loaderPostfix, localBuild) {
    return function ()  {
        this.hooks.done.tap('done',
            (stats) => {
                // now we get the hash from stats.compilation.hash not stats.compilation.fullHash
                // this is what matches [fullhash] for the filename
                // this is not very consistent, so we should watch it in the future
                const hash = localBuild ? 'dev' : stats.compilation.hash;

                const loaderScript= getLoadScript(nameRoot,loaderPostfix);
                let content = fs.readFileSync(loadScript);
                content = content.toString().replace('/*PARAMS_HERE*/', `'${loaderScript}', '${nameRoot}-${hash}.js'`);
                const loader = path.join(outpath, loaderScript);
                fs.writeFileSync(loader, content);
            });
    };
}

function dev_progress() {
    return new webpack.ProgressPlugin(function (percent, msg) {
        if (msg.startsWith('compiling')) {
            process.stdout.write('\n\x1b[1;31m> Compiling new changes\x1b[0m');   // set color to red.  for more options.. import a color lib.
        }
        if  (percent === 1) {
            process.stdout.write('\x1b[0m\n');
            setTimeout(() => process.stdout.write('\n\x1b[32m\x1b[1m> Build completed: ' + new Date().toLocaleTimeString() + '\x1b[0m \n'));
        } else if (percent * 100 % 5 === 0) {
            process.stdout.write('.');
        }
    });
}

