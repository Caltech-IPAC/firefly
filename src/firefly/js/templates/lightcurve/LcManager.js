/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {get} from 'lodash';

import {LO_VIEW, LO_MODE, SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo, dispatchUpdateLayoutInfo, dropDownHandler} from '../../core/LayoutCntlr.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_HIGHLIGHT} from '../../tables/TablesCntlr.js';
import {getCellValue, getTblById, makeTblRequest} from '../../tables/TableUtil.js';
import {updateSet} from '../../util/WebUtil.js';
import {dispatchPlotImage} from '../../visualize/ImagePlotCntlr.js';
import WebPlotRequest from '../../visualize/WebPlotRequest.js';

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
    const ra = getCellValue(tableModel, tableModel.highlightedRow, 'ra');
    const dec = getCellValue(tableModel, tableModel.highlightedRow, 'dec');

    console.log(`${ra} , ${dec}`);
    //Example: frame-id = "02328b152-w1",
    // see meaning: http://wise2.ipac.caltech.edu/docs/release/allwise/expsup/sec3_1a.html#frame_id

    const frame_id = getCellValue(tableModel, tableModel.highlightedRow, 'frame_id');

    var wise_sexp_ibe = /(\d+)([0-9][a-z])(\w+)/g;

    var res = wise_sexp_ibe.exec(frame_id);
    /*
     will split 'frame_id' into 5 elements array
     02328b152 - full frame-id
     0232 - pre-scanid, (+ scangroup =  scan_id)
     8d - scangrp ,
     152 - frame_num

     see http://irsa.ipac.caltech.edu/ibe/docs/wise/merge/merge_p1bm_frm/#int

     IBE url:
     http://irsa.ipac.caltech.edu/ibe/data/wise/merge/merge_p1bm_frm/{scangrp:s}/{scan_id:s}/{frame_num:03d}/{scan_id:s}{frame_num:03d}-w{band:1d}-int-1b.fits
     */

    const scan_id = res[1]+res[2];
    const scangrp = res[2];
    const frame_num = res[3];
    console.log(`http://irsa.ipac.caltech.edu/ibe/data/wise/merge/merge_p1bm_frm/${scangrp}/${scan_id}/${frame_num}/${scan_id}${frame_num}-w1-int-1b.fits`);
    // return makeWebPlotRequest(tbl_id);  should use the commented code below to retrieve wise images.
    // this is just a placeholder.
    //return  {
    //    plotId: 'lc_images',
    //    Type     : 'SERVICE',
    //    Service  : 'WISE',
    //    Title    : 'Wise',
    //    SurveyKey  : 'Atlas',
    //    SurveyKeyBand  : '2',
    //    WorldPt    : `${ra};${dec};EQ_J2000`,
    //    SizeInDeg  : '.3',
    //    AllowImageSelection : true
    //};

    const url = `http://irsa.ipac.caltech.edu/ibe/data/wise/merge/merge_p1bm_frm/${scangrp}/${scan_id}/${frame_num}/${scan_id}${frame_num}-w1-int-1b.fits`;
    var wpr = WebPlotRequest.makeURLPlotRequest(url);
    return wpr;
}


function makeWebPlotRequest() {
    // for WISE, should convert gwt code from here: edu.caltech.ipac.hydra.server.query.WiseGrid.makeRequest;
}
