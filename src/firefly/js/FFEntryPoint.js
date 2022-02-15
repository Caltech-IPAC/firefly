/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get} from 'lodash';
import {firefly} from './Firefly.js';
import {mergeObjectOnly} from './util/WebUtil.js';
import {getFireflyViewerWebApiCommands} from './api/webApiCommands/ViewerWebApiCommands';
import {getLcCommands} from './api/webApiCommands/LcWebApiCommands.js';
import FFTOOLS_ICO from 'html/images/fftools-logo-offset-small-42x42.png';
import {getDefaultMOCList} from 'firefly/visualize/HiPSMocUtil.js';


/**
 * @example
 *   This is an example of how to startup FireflyViewer with 2 menu items in an image and table view.
 *   <script type="text/javascript" language='javascript'>
 *      const menu = [{label:'Search', action:'TestSearches'},
 *                    {label:'Images', action:'ImageSelectDropDownCmd'}];
 *      window.firefly = {app: {views: 'images | tables', menu}};
 *   </script>
 */
let props = {
    appTitle: 'Firefly',
    initLoadingMessage: window?.firefly?.options?.initLoadingMessage,
    appIcon : FFTOOLS_ICO
};

props = mergeObjectOnly(props, get(window, 'firefly.app', {}));
const {template}= props;

let options = {
    MenuItemKeys: {maskOverlay:true},
    catalogSpatialOp: 'polygonWhenPlotExist',
    workspace : {showOptions: false},
    imageMasterSourcesOrder: ['WISE', '2MASS', 'Spitzer'],
    charts: {
        singleTraceUI: false
    },
    image : {
        canCreateExtractionTable: (template==='FireflyViewer' || template==='FireflySlate'),
    },
    hips : {
        useForImageSearch: true,
        hipsSources: 'irsa,cds',
        defHipsSources: {source: 'irsa', label: 'IRSA Featured'},
        adhocMocSource: {
            sources: getDefaultMOCList(),
            label: 'Featured MOC '
        },
        mergedListPriority: 'Irsa'
    },
    coverage : { // example of using DSS and wise combination for coverage (not that anyone would want to combination)
    }
};

options = mergeObjectOnly(options, get(window, 'firefly.options', {}));
let apiCommands;

if (template==='FireflyViewer' || template==='FireflySlate') apiCommands= getFireflyViewerWebApiCommands();
else if (template==='LightCurveViewer') apiCommands= getLcCommands();

firefly.bootstrap(props, options, apiCommands);

