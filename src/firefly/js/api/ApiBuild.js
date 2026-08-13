/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {ApiToolbarImageDisplay} from 'firefly/visualize/ui/ApiToolbarImageDisplay.jsx';
import {isString} from 'lodash';
import React from 'react';
import * as ChartsCntlr from '../charts/ChartsCntlr.js';

// Used for dispatch and action type constants
import * as TableStatsCntlr from '../charts/TableStatsCntlr.js';
import {ChartPanel} from '../charts/ui/ChartPanel.jsx';
import {ChartsContainer} from '../charts/ui/ChartsContainer.jsx';
import {MultiChartViewer} from '../charts/ui/MultiChartViewer.jsx';
import {PlotlyWrapper} from '../charts/ui/PlotlyWrapper.jsx';
import * as AppDataCntlr from '../core/AppDataCntlr.js';
import {dispatchOnAppReady} from '../core/AppDataCntlr.js';
import {APP_DATA_PATH} from '../core/CoreConst';
import {dispatchAddCell, dispatchEnableSpecialViewer, dispatchRemoveCell} from '../core/LayoutCntlr.js';
import {dispatchAddActionWatcher, dispatchAddSaga, dispatchAddTableTypeWatcherDef} from '../core/MasterSaga.js';
import {ServerRequest} from '../data/ServerRequest.js';
import {getJsonData} from '../rpc/SearchServicesJson.js';
import * as HpxIndexCntlr from '../tables/HpxIndexCntlr.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {TablePanel} from '../tables/ui/TablePanel.jsx';
import {TablesContainer} from '../tables/ui/TablesContainer.jsx';

import {startTTFeatureWatchers} from '../templates/common/ttFeatureWatchers.js';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {showWorkspaceDialog, WorkspacePickerPopup} from '../ui/WorkspaceViewer.jsx';
import * as DrawLayerDispatch from '../visualize/DrawLayerDispatch';
import * as ImPlotDispatch from '../visualize/ImagePlotDispatch';
import {ImageExpandedMode} from '../visualize/iv/ImageExpandedMode.jsx';
import {ImageViewer} from '../visualize/iv/ImageViewer.jsx';
import * as ReadoutCntlr from '../visualize/MouseReadoutCntlr.js';
import * as MultiViewCntlr from '../visualize/MultiViewCntlr.js';
import {getActiveRowToImageDef} from '../visualize/saga/ActiveRowToImageWatcher.js';
import {getMocWatcherDef} from '../visualize/saga/MOCWatcher.js';
import {ApiExpandedDisplay} from '../visualize/ui/ApiExpandedDisplay.jsx';
import {ApiFullImageDisplay} from '../visualize/ui/ApiFullImageDisplay.jsx';
import {ImageMetaDataToolbar} from '../visualize/ui/ImageMetaDataToolbar.jsx';

// UI component
import {MultiImageViewer} from '../visualize/ui/MultiImageViewer.jsx';
import {MultiViewStandardToolbar} from '../visualize/ui/MultiViewStandardToolbar.jsx';
import * as VisConst from '../visualize/VisConst.js';
import {DRAWLAYER_PREFIX, IMAGE_MULTI_VIEW_PREFIX, PLOTS_PREFIX} from '../visualize/VisConst.js';
import {ApiExpandedView} from './ApiExpandedView.jsx';


// builds the highlevel api
import {buildHighLevelApi} from './ApiHighlevelBuild.js';


// Parts of the lowlevel api
import * as ApiUtil from './ApiUtil.js';
import * as ApiUtilChart from './ApiUtilChart.jsx';
import moreChartApi from './ApiUtilChart.jsx';
import * as ApiUtilImage from './ApiUtilImage.jsx';
import * as ApiUtilTable from './ApiUtilTable.jsx';
import {buildViewerApi} from './ApiViewer.js';


/**
 * High-level API.
 * @public
 * @namespace firefly
 */

/**
 * Actions and action dispatchers.
 * @public
 * @namespace firefly.action
 */

/**
 * React components.
 * @public
 * @namespace firefly.ui
 */

/**
 * Utilities.
 * @public
 * @namespace firefly.util
 */

/**
 * Chart utilities.
 * @public
 * @namespace firefly.util.chart
 */

/**
 * @namespace firefly.util.data
 */

/**
 * Image utilities.
 * @public
 * @namespace firefly.util.image
 */

/**
 * Table utilities.
 * @public
 * @namespace firefly.util.table
 */


/**
 * Start in api mode. Will create the api and call window.onFireflyLoaded(firefly)
 * @ignore
 */
export function initApi(props) {
    const lowlevelApi= buildLowlevelAPI();
    const viewInterface= buildViewerApi();

    const highLevelApi= buildHighLevelApi(lowlevelApi);

    // a method to get JSON data from external task launcher
    const getJsonFromTask = function(launcher, task, taskParams) {
        const req = new ServerRequest('JsonFromExternalTask');
        req.setParam({name : 'launcher', value : launcher});
        req.setParam({name : 'task', value : task});
        req.setParam({name : 'taskParams', value : JSON.stringify(taskParams)});
        return getJsonData(req);
    };

    if (!window.firefly) window.firefly= {getJsonFromTask};
    window.firefly.ignoreHistory = true;
    window.firefly.originalAppProps= props;
    Object.assign(window.firefly, lowlevelApi, highLevelApi, viewInterface, React);
    const firefly= window.firefly;
    dispatchOnAppReady(() => {
        window.onFireflyLoaded && window.onFireflyLoaded(firefly);
    });
    startTTFeatureWatchers([getActiveRowToImageDef().id, getMocWatcherDef().id]);
    initExpandedView();
}



/**

 Structure of API
     {
                //--- High level API , all high level api are in the root
       all high level functions....

              //--- Low level API, lowlevel api are under action, ui, util
       action : { all dispatch functions...
                 type: {all action type constants}
                }
       ui: { high level react components }
       util { renderDom, unrenderDom, isDebug, debug       // built by ApiUtil.js
              image : {image utility routines}             // imported from ApiUtilImage.js
              chart : {chart utility routines}             // imported from ApiUtilChart.js
              table : {table utility routines}             // imported from ApiUtilTable.js
              data : {data utility routines???? }          //todo do we need this?????
       }
     }
 */

/**
 * Return the api object.
 * @return {{action:{},ui:{},util:{}}}
 * @ignore
 */
export function buildLowlevelAPI() {


    const type= Object.assign({},
        findActionType(TableStatsCntlr,TableStatsCntlr.TBLSTATS_DATA_KEY),
        findActionType(ChartsCntlr, ChartsCntlr.DATA_PREFIX ),
        findActionType(TablesCntlr, TablesCntlr.DATA_PREFIX),
        findActionType(TablesCntlr, TablesCntlr.RESULTS_PREFIX),
        findActionType(TablesCntlr, TablesCntlr.UI_PREFIX),
        findActionType(ReadoutCntlr, ReadoutCntlr.READOUT_PREFIX),
        findActionType(VisConst, IMAGE_MULTI_VIEW_PREFIX),
        findActionType(VisConst, PLOTS_PREFIX),
        findActionType(VisConst, DRAWLAYER_PREFIX),
        findActionType(HpxIndexCntlr, HpxIndexCntlr.SPACIAL_HPX_INDX_PREFIX),
        findActionType(AppDataCntlr, APP_DATA_PATH),
    );


    const action= Object.assign({},
        {type},
        findDispatch(TableStatsCntlr),
        findDispatch(ChartsCntlr),
        findDispatch(TablesCntlr),
        findDispatch(ReadoutCntlr),
        findDispatch(MultiViewCntlr),
        findDispatch(ImPlotDispatch),
        findDispatch(HpxIndexCntlr),
        findDispatch(AppDataCntlr),
        findDispatch(DrawLayerDispatch),
        {dispatchAddCell, dispatchRemoveCell, dispatchEnableSpecialViewer},
        {dispatchAddSaga, dispatchAddActionWatcher, dispatchAddTableTypeWatcherDef}
    );

    const ui= {
        ImageViewer,
        MultiImageViewer,
        MultiViewStandardToolbar,
        ApiExpandedDisplay,
        ApiFullImageDisplay,
        ApiToolbarImageDisplay,
        ImageExpandedMode,
        ImageMetaDataToolbar,
        TablesContainer,
        TablePanel,
        ChartsContainer,
        MultiChartViewer,
        ChartPanel,
        PlotlyWrapper,
        showWorkspaceDialog,
        WorkspacePickerPopup: fieldGroupWrap(WorkspacePickerPopup)
    };

    const util= Object.assign({}, ApiUtil, {image:ApiUtilImage}, {chart:{...ApiUtilChart, ...moreChartApi}}, {table:ApiUtilTable}, {data:{}} );

    return { action, ui, util };
}


/**
 * pull all the dispatch functions out of the object
 * @param {Object} obj
 * @return {*}
 * @ignore
 */
function findDispatch(obj) {
   return Object.keys(obj).reduce( (res,key) => {
        if (key.startsWith('dispatch')) res[key]= obj[key];
        return res;
    },{} );
}


/**
 * pull all the action type constants out of the object
 * @param {Object} obj
 * @param {string} prefix
 * @return {*}
 * @ignore
 */
function findActionType(obj,prefix) {
    return Object.keys(obj).reduce( (res,key) => {
        if (isString(obj[key]) && obj[key].startsWith(prefix) && obj[key].length>prefix.length) {
            res[key]= obj[key];
        }
        return res;
    },{} );
}


function initExpandedView(div){

    const EXPANDED_DIV= 'expandedArea';
    var expandedDivEl;
    if (div) {
        expandedDivEl= isString(div) ? document.getElementById(div) : div;
    } else {
        expandedDivEl= document.createElement('div');
        document.body.appendChild(expandedDivEl);
        expandedDivEl.id= EXPANDED_DIV;
    }
    
    ApiUtil.renderDOM(expandedDivEl, ApiExpandedView, undefined, false);
}


function fieldGroupWrap(Component, groupKey='firefly-api-fieldgroup') {

    return (props) => {
        return (
            <FieldGroup groupKey={groupKey}>
                <Component {...props}/>
            </FieldGroup>
        );
    };

}