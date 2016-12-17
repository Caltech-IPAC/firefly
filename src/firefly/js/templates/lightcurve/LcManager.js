/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isUndefined, get,isNil} from 'lodash';
import {take} from 'redux-saga/effects';

import {LO_VIEW, LO_MODE, SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo, dispatchUpdateLayoutInfo, dropDownHandler} from '../../core/LayoutCntlr.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_HIGHLIGHT} from '../../tables/TablesCntlr.js';
import {getCellValue, getTblById, makeTblRequest} from '../../tables/TableUtil.js';
import {updateSet} from '../../util/WebUtil.js';
import {dispatchPlotImage, visRoot, dispatchDeletePlotView,
        dispatchChangeActivePlotView,
        WcsMatchType, dispatchWcsMatch} from '../../visualize/ImagePlotCntlr.js';
import {getPlotViewById} from '../../visualize/PlotViewUtil.js';
import {getMultiViewRoot, dispatchReplaceViewerItems, getViewer} from '../../visualize/MultiViewCntlr.js';
import {WebPlotRequest,TitleOptions} from '../../visualize/WebPlotRequest.js';
import {dispatchTableToIgnore} from '../../visualize/DrawLayerCntlr.js';
import Catlog from '../../drawingLayers/Catalog.js';
import {ServerRequest} from '../../data/ServerRequest.js';
import {CHANGE_VIEWER_LAYOUT} from '../../visualize/MultiViewCntlr.js';
import {LcPFOptionsPanel, grpkey} from './LcPhaseFoldingPanel.jsx';
import FieldGroupUtils, {revalidateFields} from '../../fieldGroup/FieldGroupUtils';
import {makeWorldPt} from '../../visualize/Point.js';
import {CoordinateSys} from '../../visualize/CoordSys.js';

export const LC = {
    RAW_TABLE: 'raw_table',
    PHASE_FOLDED: 'phase_folded',
    PERIODOGRAM: 'periodogram',
    PEAK_TABLE: 'peak_table',
    PERIOD_CNAME: 'Period',
    POWER_CNAME: 'Power',
    PEAK_CNAME: 'Peak',
    PHASE_CNAME: 'phase',

    IMG_VIEWER_ID: 'lc_image_viewer',
    MAX_IMAGE_CNT: 7,
    DEF_IMAGE_CNT: 5,

    META_TIME_CNAME: 'timeCName',
    META_FLUX_CNAME: 'fluxCName',
    DEF_TIME_CNAME: 'mjd',
    DEF_FLUX_CNAME: 'w1mpro_ep',
};

const plotIdRoot= 'LC_FRAME-';


var webplotRequestCreator;
/**
 * A function to create a WebPlotRequest from the given parameters
 * @callback WebplotRequestCreator
 * @param {TableModel} tableModel
 * @param {number} hlrow
 * @param {number} cutoutSize
 */

/**
 * This event manager is custom made for light curve viewer.
 * @param {Object} params
 * @props {WebplotRequestCreator} params.webplotRequestCreator
 */
export function* lcManager(params={}) {
    webplotRequestCreator = params.WebplotRequestCreator || getWebPlotRequestViaUrl;

    while (true) {
        const action = yield take([
            TBL_RESULTS_ADDED, TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_HIGHLIGHT, SHOW_DROPDOWN, SET_LAYOUT_MODE,
            CHANGE_VIEWER_LAYOUT
        ]);

        /**
         * This is the current state of the application.  Action handlers should return newLayoutInfo if state changes
         * If state has changed, it will be dispacthed into the flux.
         * @type {LayoutInfo}
         * @prop {boolean}  layoutInfo.showForm    show form panel
         * @prop {boolean}  layoutInfo.showTables  show tables panel
         * @prop {boolean}  layoutInfo.showCharts  show charts panel
         * @prop {boolean}  layoutInfo.showImages  show images panel
         * @prop {string}   layoutInfo.searchDesc  optional string describing search criteria used to generate this result.
         * @prop {Object}   layoutInfo.images      images specific states
         * @prop {string}   layoutInfo.images.activeTableId  last active table id that images responded to
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
                newLayoutInfo = handleTableHighlight(newLayoutInfo, action);
                break;
            case CHANGE_VIEWER_LAYOUT:
                newLayoutInfo = handleChangeMultiViewLayout(newLayoutInfo, action);
                break;
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
    layoutInfo =  Object.assign({}, layoutInfo, {showTables: true, showXyPlots: true});
    if (isImageEnabledTable(tbl_id)) {
        layoutInfo = updateSet(layoutInfo, 'showImages', true);
        layoutInfo = updateSet(layoutInfo, 'images.activeTableId', tbl_id);
        setupImages(tbl_id);
    }
    return layoutInfo;
}



function handleTableActive(layoutInfo, action) {
    const {tbl_id} = action.payload;
    if (isImageEnabledTable(tbl_id)) {
        layoutInfo = updateSet(layoutInfo, 'images.activeTableId', tbl_id);
        setupImages(tbl_id);
    }
    return layoutInfo;
}

function handleTableHighlight(layoutInfo, action) {
    const {tbl_id} = action.payload;
    if (isImageEnabledTable(tbl_id)) {
        setupImages(tbl_id);
    }
}

function isImageEnabledTable(tbl_id) {
    return [LC.PHASE_FOLDED, LC.RAW_TABLE].includes(tbl_id);
}

function handleChangeMultiViewLayout(layoutInfo, action) {
    const activeTableId = get(layoutInfo, 'images.activeTableId');
    const tbl= getTblById(activeTableId);
    if (get(tbl, 'totalRows',0)>0) setupImages(activeTableId);
    return layoutInfo;
}

function getWebPlotRequest(tableModel, hlrow) {
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
    return addCommonReqParams(reqParams, frameId, makeWorldPt(ra,dec,CoordinateSys.EQ_J2000));
}

function getWebPlotRequestViaUrl(tableModel, hlrow, cutoutSize) {
    const ra = getCellValue(tableModel, hlrow, 'ra');
    const dec = getCellValue(tableModel, hlrow, 'dec');
    const frameId = getCellValue(tableModel, hlrow, 'frame_id');
    var   wise_sexp_ibe = /(\d+)([0-9][a-z])(\w+)/g;
    var   res = wise_sexp_ibe.exec(frameId);
    const scan_id = res[1] + res[2];
    const scangrp = res[2];
    const frame_num = res[3];

    /*the following should be from reading in the url column returned from LC search
     we are constructing the url for wise as the LC table does
     not have the url colume yet
     It is only for WISE, using default cutout size 0.3 deg
    const url = `http://irsa.ipac.caltech.edu/ibe/data/wise/merge/merge_p1bm_frm/${scangrp}/${scan_id}/${frame_num}/${scan_id}${frame_num}-w1-int-1b.fits`;
    */
    const serverinfo = 'http://irsa.ipac.caltech.edu/ibe/data/wise/merge/merge_p1bm_frm/';
    const centerandsize = cutoutSize ? `?center=${ra},${dec}&size=${cutoutSize}&gzip=false` : '';
    const url = `${serverinfo}${scangrp}/${scan_id}/${frame_num}/${scan_id}${frame_num}-w1-int-1b.fits${centerandsize}`;
    const plot_desc = `WISE-${frameId}`;
    const reqParams = WebPlotRequest.makeURLPlotRequest(url, plot_desc);
    const title= 'WISE-'+ frameId + (cutoutSize ? ` size: ${cutoutSize}(deg)` : '');
    return addCommonReqParams(reqParams, title, makeWorldPt(ra,dec,CoordinateSys.EQ_J2000));
}

function addCommonReqParams(inWpr,title,wp) {
    const retWpr= inWpr.makeCopy();
    retWpr.setTitle(title);
    retWpr.setTitleOptions(TitleOptions.NONE);
    retWpr.setGroupLocked(true);
    retWpr.setPlotGroupId('LightCurveGroup');
    retWpr.setPreferenceColorKey('light-curve-color-pref');
    retWpr.setOverlayPosition(wp);
    return retWpr;
}

export function setupImages(tbl_id) {
    try {
        const viewer=  getViewer(getMultiViewRoot(),LC.IMG_VIEWER_ID);
        const count= get(viewer, 'layoutDetail.count',LC.DEF_IMAGE_CNT);
        const tableModel = getTblById(tbl_id);
        if (!tableModel || isNil(tableModel.highlightedRow)) return;
        var vr= visRoot();
        const hasPlots= vr.plotViewAry.length>0;
        const newPlotIdAry= makePlotIds(tableModel.highlightedRow, tableModel.totalRows,count);
        const maxPlotIdAry= makePlotIds(tableModel.highlightedRow, tableModel.totalRows,LC.MAX_IMAGE_CNT);

        const cutoutSize = get(FieldGroupUtils.getGroupFields(grpkey), ['cutoutSize', 'value'], null);

        newPlotIdAry.forEach( (plotId) => {
            if (!getPlotViewById(vr,plotId)) {
                const rowNum= Number(plotId.substring(plotIdRoot.length));
                const webPlotReq = webplotRequestCreator(tableModel,rowNum, cutoutSize);
                dispatchPlotImage({plotId, wpRequest:webPlotReq,
                                           setNewPlotAsActive:false,
                                           holdWcsMatch:true,
                                           pvOptions: { userCanDeletePlots: false}});
            }
        });


        dispatchReplaceViewerItems(LC.IMG_VIEWER_ID, newPlotIdAry);
        const newActivePlotId= plotIdRoot+tableModel.highlightedRow;
        dispatchChangeActivePlotView(newActivePlotId);


        vr= visRoot();
        if (!vr.wcsMatchType && !hasPlots) {
            dispatchWcsMatch({matchType:WcsMatchType.Target, plotId:newActivePlotId});
        }

        vr= visRoot();

        vr.plotViewAry
            .filter( (pv) => pv.plotId.startsWith(plotIdRoot))
            .filter( (pv) => pv.plotId!==vr.mpwWcsPrimId)
            .filter( (pv) => !maxPlotIdAry.includes(pv.plotId))
            .forEach( (pv) => dispatchDeletePlotView({plotId:pv.plotId, holdWcsMatch:true}));
    } catch (E){
        console.log(E.toString());
    }
}


function makePlotIds(highlightedRow, totalRows, totalPlots)  {
    const plotIds= [];
    const beforeCnt= totalPlots%2===0 ? totalPlots/2-1 : (totalPlots-1)/2;
    const afterCnt= totalPlots%2===0 ? totalPlots/2    : (totalPlots-1)/2;
    var j=0;
    var endRow= Math.min(totalRows-1, highlightedRow+afterCnt);
    var startRow= Math.max(0,highlightedRow-beforeCnt);
    if (startRow===0) endRow= Math.min(totalRows-1, totalPlots-1);
    if (endRow===totalRows-1) startRow= Math.max(0, totalRows-totalPlots);

    for(var i= startRow; i<=endRow; i++) plotIds[j++]= plotIdRoot+i;
    return plotIds;
}


