/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';

import {LO_VIEW, LO_MODE, SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo, dispatchUpdateLayoutInfo, dropDownHandler} from '../../core/LayoutCntlr.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_HIGHLIGHT} from '../../tables/TablesCntlr.js';
import {getCellValue, getTblById, makeTblRequest} from '../../tables/TableUtil.js';
import {updateSet} from '../../util/WebUtil.js';
import {dispatchPlotImage} from '../../visualize/ImagePlotCntlr.js';
import {makeProcessorRequest} from '../../visualize/WebPlotRequest.js';

export const RAW_TABLE = 'raw_table';
export const PHASE_FOLDED = 'phase_folded';
export const PERIODOGRAM = 'periodogram';
export const PEAK_TABLE = 'peak_table';

export const IMG_VIEWER_ID = 'lc_image_viewer';

/**
 *  This event manager is custom made for light curve viewer.
 */
export function* lcManager() {
    while (true) {
        const action = yield take([
            TBL_RESULTS_ADDED, TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_HIGHLIGHT, SHOW_DROPDOWN, SET_LAYOUT_MODE
        ]);

        /**
         * This is the current state of the application.  Depending on what action is yielded, modify
         * this object accordingly then update it via dispatch.
         * @type {LayoutInfo} layoutInfo
         * @prop {boolean}  layoutInfo.showForm    show form panel
         * @prop {boolean}  layoutInfo.showTables  show tables panel
         * @prop {boolean}  layoutInfo.showCharts  show charts panel
         * @prop {boolean}  layoutInfo.showImages  show images panel
         * @prop {string}   layoutInfo.searchDesc  optional string describing search criteria used to generate this result.
         */
        var layoutInfo = getLayouInfo();
        var newLayoutInfo = layoutInfo;

        newLayoutInfo = dropDownHandler(newLayoutInfo, action);
        switch (action.type) {
            case TBL_RESULTS_ADDED:
            case TABLE_LOADED :
                newLayoutInfo = handleTableLoad(newLayoutInfo, action);
                break;
            case TABLE_HIGHLIGHT:
                handleTableHighlight(newLayoutInfo, action);
            case TBL_RESULTS_ACTIVE :
                newLayoutInfo = handleTableActive(newLayoutInfo, action);
                break;
        }

        if (newLayoutInfo !== layoutInfo) {
            dispatchUpdateLayoutInfo(newLayoutInfo);
        }
    }
}


function handleTableLoad(layoutInfo, action) {
    const {tbl_id} = action.payload;
    layoutInfo =  updateSet(layoutInfo, 'showTables', true);
    if ( [RAW_TABLE, PEAK_TABLE, PHASE_FOLDED, PERIODOGRAM].includes(tbl_id) ) {
        layoutInfo = updateSet(layoutInfo, 'showXyPlots', true);
    }
    if ( [PHASE_FOLDED].includes(tbl_id) ){
        layoutInfo = updateSet(layoutInfo, 'showImages', true);
        handleTableHighlight(layoutInfo, action);
    }
    return layoutInfo;
}

function handleTableActive(layoutInfo, action) {
    return layoutInfo;
}

function handleTableHighlight(layoutInfo, action) {
    const {tbl_id} = action.payload;
    if (tbl_id === PHASE_FOLDED) {
        const webPlotReq = getWebPlotRequest(tbl_id);
        const plotId = webPlotReq.plotId;
        dispatchPlotImage({plotId, wpRequest:webPlotReq, viewerId:IMG_VIEWER_ID});
    }
}

function getWebPlotRequest(tbl_id) {
    const tableModel = getTblById(tbl_id);
    const ra = getCellValue(tableModel, tableModel.highlightedRow, 'ra');
    const dec = getCellValue(tableModel, tableModel.highlightedRow, 'dec');
    // return makeWebPlotRequest(tbl_id);  should use the commented code below to retrieve wise images.
    // this is just a placeholder.
    return  {
        plotId: 'lc_images',
        Type     : 'SERVICE',
        Service  : 'WISE',
        Title    : 'Wise',
        SurveyKey  : 'Atlas',
        SurveyKeyBand  : '2',
        WorldPt    : `${ra};${dec};EQ_J2000`,
        SizeInDeg  : '.3',
        AllowImageSelection : true
    };
}


function makeWebPlotRequest() {
    // for WISE, should convert gwt code from here: edu.caltech.ipac.hydra.server.query.WiseGrid.makeRequest;
}
