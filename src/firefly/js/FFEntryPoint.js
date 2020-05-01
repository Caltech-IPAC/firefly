/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get} from 'lodash';
import {firefly} from './Firefly.js';
import {mergeObjectOnly} from './util/WebUtil.js';
import {getFireflyViewerWebApiCommands} from './api/webApiCommands/ViewerWebApiCommands';


/**
 * @example
 *   This is an example of how to startup FireflyViewer with 2 menu items in an image and table view.
 *   <script type="text/javascript" language='javascript'>
 *      const menu = [{label:'Search', action:'TestSearches'},
 *                    {label:'Images', action:'ImageSelectDropDownCmd'}];
 *      window.firefly = {app: {views: 'images | tables', menu}};
 *   </script>
 */
var props = {
    appTitle: 'Firefly',
};

var options = {
    MenuItemKeys: {maskOverlay:true},
    catalogSpacialOp: 'polygonWhenPlotExist',
    workspace : {showOptions: false},
    imageMasterSourcesOrder: ['WISE', '2MASS', 'Spitzer'],
    charts: {
        singleTraceUI: false
    },
    hips : {
        useForImageSearch: true,
        hipsSources: 'irsa,cds',
        defHipsSources: {source: 'irsa', label: 'IRSA Featured'},
        mergedListPriority: 'Irsa'
    },
    coverage : { // example of using DSS and wise combination for coverage (not that anyone would want to combination)
    }
};

props = mergeObjectOnly(props, get(window, 'firefly.app', {}));
options = mergeObjectOnly(options, get(window, 'firefly.options', {}));
const {template}= props;
let apiCommands;

if (template==='FireflyViewer' || template==='FireflySlate') apiCommands= getFireflyViewerWebApiCommands();
else if (template==='LightCurveViewer') apiCommands= getFireflyViewerWebApiCommands(['table']); // todo add commands for time series viewer

firefly.bootstrap(props, options, apiCommands);

