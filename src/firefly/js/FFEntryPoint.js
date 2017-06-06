/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get} from 'lodash';
import {firefly, Templates} from './Firefly.js';
import {HELP_LOAD} from './core/AppDataCntlr.js';
import {sampleHydra} from './templates/hydra/SampleHydra.jsx';


/**
 * @global
 * @public
 * @typedef {Object} AppOptions
 *
 * @summary Options for the application
 *
 * @prop {Object} MenuItemKeys globally turns off or on visualization toolbar options
 * @prop {Array.<String>} imageTabs controls the tab order of the image selection panel
 * @prop {String|function} irsaCatalogFilter one of ['lsstFilter', undefined] or function
 * @prop {String} catalogSpacialOp one of ['polygonWhenPlotExist', '']
 */



/**
 * This entry point allows dynamic application loading.  Use firefly.app to configure
 * what this application should do.
 * By default, firefly.js will startup in api mode.
 * If you want it to startup as an application, you need to at least
 * define a firefly.app under window.
 *
 * @namespace firefly
 * @type {object}
 * @prop {Templates} template  the name of the template to use. defaults to 'FireflyViewer'
 * @prop {AppOptions} options  options to load this app with
 * @prop {string}   appTitle  title of this application.
 * @prop {string}   div       the div to load this application into.  defaults to 'app'
 * @prop {Object}   menu         custom menu bar
 * @prop {string}   menu.label   button's label
 * @prop {string}   menu.action  action to fire on button clicked
 * @prop {string}   menu.type    use 'COMMAND' for actions that's not drop-down related.
 * @prop {string}   views     some template may have multiple views.  use this to select specify
 * @example
 *   This is an example of how to startup FireflyViewer with 2 menu items in an image and table view.
 *   <script type="text/javascript" language='javascript'>
 *      const menu = [{label:'Search', action:'TestSearches'},
 *                    {label:'Images', action:'ImageSelectDropDownCmd'}];
 *      window.firefly = {app: {views: 'images | tables', menu}};
 *   </script>
 */
const defaults = {
    div: 'app',
    template: 'FireflyViewer',
    menu: [
        {label:'Search', action:'Search'},
        {label:'Data Sets: Catalogs & Images', action:'TestSearch'},
        {label:'Catalogs CLASSIC', action:'IrsaCatalogDropDown'},
        {label:'Test Searches', action:'TestSearches'},
        {label:'Images', action:'ImageSelectDropDownCmd'},
        {label:'Charts', action:'ChartSelectDropDownCmd'},
        {label:'Help', action:HELP_LOAD, type:'COMMAND'},
        {label:'Example Js Dialog', action:'exampleDialog', type:'COMMAND'}
    ]
};

const app = get(window, 'firefly.app', {});
var viewer, props;
if (app.template) {
    props = Object.assign({}, defaults, app);
    viewer = Templates[props.template];
}

const options= get(window, 'firefly.app.options') || get(window, 'firefly.options');

firefly.bootstrap(options, viewer, props)
    .then(() => {
        if (app.template === 'HydraViewer') sampleHydra();
    });

