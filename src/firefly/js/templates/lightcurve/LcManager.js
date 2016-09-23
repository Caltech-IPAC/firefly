/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isUndefined, get} from 'lodash';
import {take} from 'redux-saga/effects';

import {LO_VIEW, LO_MODE, SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo, dispatchUpdateLayoutInfo, dropDownHandler} from '../../core/LayoutCntlr.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_HIGHLIGHT} from '../../tables/TablesCntlr.js';
import {getCellValue, getTblById, makeTblRequest} from '../../tables/TableUtil.js';
import {updateSet} from '../../util/WebUtil.js';
import {dispatchPlotImage} from '../../visualize/ImagePlotCntlr.js';
import {WebPlotRequest,TitleOptions} from '../../visualize/WebPlotRequest.js';
import {makeWiseLcPlotRequest} from '../../metaConvert/WiseLcViewRequestList';
import {converters, converterFactory} from '../../metaConvert/ConverterFactory.js';
import {ServerRequest} from '../../data/ServerRequest.js';

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
         * @type {LayoutInfo}
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
        try {
            const webPlotReq = getWebPlotRequest(tbl_id);
            const plotId = get(webPlotReq, plotId, 'lc_images');
            dispatchPlotImage({plotId, wpRequest:webPlotReq, viewerId:IMG_VIEWER_ID});
        } catch (E){
            console.log(E.toString());
        }

    }
}

function getWebPlotRequest(tbl_id) {
    const tableModel = getTblById(tbl_id);
    const hlrow = tableModel.highlightedRow || 0;
    const ra = getCellValue(tableModel, hlrow, 'ra');
    const dec = getCellValue(tableModel, hlrow, 'dec');
    const frameId = getCellValue(tableModel, hlrow, 'frame_id');
    var   wise_sexp_ibe = /(\d+)([0-9][a-z])(\w+)/g;
    var   res = wise_sexp_ibe.exec(frameId);
    const scan_id = res[1] + res[2];
    const scangrp = res[2];
    const frame_num = res[3];

    const sr= new ServerRequest('ibe_file_retrieve');
    sr.setParam('mission', 'wise');
    sr.setParam('plotId', 'lc_images');
    sr.setParam('PROC_ID', 'ibe_file_retrieve');
    sr.setParam('ProductLevel',  '1b');
    sr.setParam('ImageSet', 'allsky-4band');
    sr.setParam('band', 2);
    sr.setParam('scangrp', `${scangrp}`);
    sr.setParam('scan_id', `${scan_id}`);
    sr.setParam('frame_num', `${frame_num}`);
    sr.setParam('center', `${ra},${dec}`);
    sr.setParam('size', '0.3');
    sr.setParam('subsize', '0.3');
    sr.setParam('in_ra',`${ra}`);
    sr.setParam('in_dec',`${dec}`);

    const reqParams = WebPlotRequest.makeProcessorRequest(sr, 'wise');
    reqParams.setTitle('WISE-'+ frameId);
//    reqParams.setInitialZoomLevel(0.5);
    return reqParams;



}


function makeWebPlotRequest(tbl_id) {
//todo make grid plotting with hlrow+1, hlrow, hlrow-1
    // for WISE, should convert gwt code from here: edu.caltech.ipac.hydra.server.query.WiseGrid.makeRequest;
}
