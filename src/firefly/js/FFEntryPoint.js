/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {firefly} from './Firefly.js';
import {mergeObjectOnly} from './util/WebUtil.js';
import {getFireflyViewerWebApiCommands} from './api/webApiCommands/ViewerWebApiCommands';
import {getLcCommands} from './api/webApiCommands/LcWebApiCommands.js';
import {getDatalinkUICommands} from './api/webApiCommands/DatalinkUICommands.js';
import {getDefaultMOCList} from 'firefly/visualize/HiPSMocUtil.js';
import APP_ICON from 'html/images/fftools-logo-offset-small-42x42.png';
import {ROUTER} from 'firefly/templates/router/RouteHelper';
import {alertviewer} from 'firefly/apps/alertviewer/alertviewer';



/**
 * Pages are spa within a webapp.  In this case, a page can be alertviewer and a webapp is firefly(applications).
 * @type {object}
 * @prop {function} init  an init function to call after firefly completes bootstrap.
 * @prop {object[]} menu  a list of menu for this page.
 */
const pages = {
    alertviewer,
};


/**
 * @example
 *   This is an example of how to startup FireflyViewer with 2 menu items in an image and table view.
 *   <script type="text/javascript" language='javascript'>
 *      const menu = [{label:'Search', action:'TestSearches'},
 *                    {label:'Images', action:'ImageSelectDropDownCmd'}];
 *      window.firefly = {app: {views: 'images | tables', menu}};
 *   </script>
 */
const defProps = {
    appTitle: 'Firefly',
    initLoadingMessage: window?.firefly?.options?.initLoadingMessage,
    appIcon: <img src={APP_ICON} style={{width:38}}/>
};

let props = mergeObjectOnly(defProps, window?.firefly?.app ?? {});
const {template}= props;


props.fileDropEventAction= template!=='LightCurveViewer' ? 'FileUploadDropDownCmd' : 'LCUpload';

const defOptions = {
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
        adhocMocIncludeAdditionSources: 'irsa',
        mergedListPriority: 'Irsa'
    },
    coverage : { }
};

let options = mergeObjectOnly(defOptions, window?.firefly?.options ?? {});

let apiCommands, initApp;
if (template==='FireflyViewer' || template==='FireflySlate') {
    apiCommands= [...getFireflyViewerWebApiCommands(), ...getDatalinkUICommands(false,'DLGeneratedDropDownCmd')];
} else if (template==='LightCurveViewer') {
    apiCommands= getLcCommands();
} else if (template === ROUTER) {
    const pageDef= pages[props.page]; // page id should be defined in html file firefly.app.page
    props = mergeObjectOnly(props, pageDef?.props);
    options = mergeObjectOnly(options, pageDef?.options);
    if (pageDef?.menu) props.menu = pageDef?.menu; // if the page defines a menu then use it
    apiCommands = pageDef?.webApiCommands;
    initApp = pageDef?.init;
}


if (!template || template==='LightCurveViewer') options.searchActions= [];

firefly.bootstrap(props, options, apiCommands).then(() => initApp?.());