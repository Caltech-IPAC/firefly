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

const props = mergeObjectOnly(defProps, window?.firefly?.app ?? {});
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
        mergedListPriority: 'Irsa'
    },
    coverage : { }
};

const options = mergeObjectOnly(defOptions, window?.firefly?.options ?? {});

let apiCommands;
if (template==='FireflyViewer' || template==='FireflySlate') {
    apiCommands= [...getFireflyViewerWebApiCommands(), ...getDatalinkUICommands(false,'DLGeneratedDropDownCmd')];
}
else if (template==='LightCurveViewer') {
    apiCommands= getLcCommands();
}


if (!template || template==='LightCurveViewer') options.searchActions= [];

firefly.bootstrap(props, options, apiCommands);