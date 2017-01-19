/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isUndefined, get,isNil, has, set, lowerCase} from 'lodash';
import {take} from 'redux-saga/effects';
import {flux} from '../../Firefly.js';
import {LO_VIEW, LO_MODE, SHOW_DROPDOWN, SET_LAYOUT_MODE, SET_LAYOUT, getLayouInfo,
        dispatchUpdateLayoutInfo, dropDownHandler, dispatchLayoutDisplayMode} from '../../core/LayoutCntlr.js';
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
import {VALUE_CHANGE, dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {makeWorldPt} from '../../visualize/Point.js';
import {CoordinateSys} from '../../visualize/CoordSys.js';
import {getMenu, dispatchSetMenu} from '../../core/AppDataCntlr.js';

export const LC = {
    RAW_TABLE: 'raw_table',          // raw table id
    PHASE_FOLDED: 'phase_folded',    // phase folded table id
    PERIODOGRAM: 'periodogram',      // periodogram table id
    PEAK_TABLE: 'peak_table',        // peak table id
    PERIOD_CNAME: 'Period',          // period column
    POWER_CNAME: 'Power',            // power column
    PEAK_CNAME: 'Peak',              // peak column
    PHASE_CNAME: 'phase',

    IMG_VIEWER_ID: 'lc_image_viewer',
    MAX_IMAGE_CNT: 7,
    DEF_IMAGE_CNT: 5,

    META_TIME_CNAME: 'timeCName',
    META_FLUX_CNAME: 'fluxCName',
    DEF_TIME_CNAME: 'mjd',
    DEF_FLUX_CNAME: 'w1mpro_ep',

    RESULT_PAGE: 'result',          // result layout
    PERIOD_PAGE: 'period',          // period finding layout with periodogram button
    PERGRAM_PAGE: 'periodogram',    // period finding layout with peak and periodogram tables/charts

    PERIOD_FINDER: 'LC_PERIOD_FINDER',         // period finding group for the form
    PERIODOGRAM_GROUP: 'LC_PERIODOGRAM_TBL'    // table container, table group
};

const plotIdRoot= 'LC_FRAME-';
export var menuHas = (menu, action) => {
    return  menu && has(menu, 'menuItems') && menu.menuItems.find((item)=> item.action === action);
};


export function periodPageMode(mode) {
    const menu = getMenu();

    if (menu) {
        var newMenu = Object.assign({}, menu);
        var periodItem = newMenu.menuItems.find((item) => item.action === SET_LAYOUT);

        if (periodItem) {
            set(periodItem, ['payload', 'displayMode'], mode);
        } else {
            newMenu.menuItems.push({action: SET_LAYOUT,
                                    label: 'Period Finding',
                                    type: 'COMMAND',
                                    payload: {displayMode: mode}});
        }
        dispatchSetMenu(newMenu);
    }
}

export function updateLayoutDisplay(displayMode) {
    var layoutInfo = getLayouInfo();
    var newLayoutInfo = Object.assign({}, layoutInfo, {displayMode});

    if (newLayoutInfo !== layoutInfo) {
        dispatchUpdateLayoutInfo(newLayoutInfo);
    }
}

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
            CHANGE_VIEWER_LAYOUT, VALUE_CHANGE
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
         * @prop {string}   layoutInfo.displayMode:'result' (result page), 'period' (period finding page), 'periodogram' or neither
         */
        var layoutInfo = getLayouInfo();
        var newLayoutInfo = layoutInfo;
        var bInitPeriod = false;

        newLayoutInfo = dropDownHandler(newLayoutInfo, action);
        switch (action.type) {
            case TBL_RESULTS_ADDED:
            case TABLE_LOADED :
                newLayoutInfo = handleTableLoad(newLayoutInfo, action);
                bInitPeriod = handleMenuSetting(action);
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
            case VALUE_CHANGE:
                if (['time', 'flux', 'periodMin', 'periodMax'].includes(action.payload.fieldKey)) {
                    // re-validate period value
                    if (action.payload.fieldKey.startsWith('period')) { // periodMin or periodMax is changed
                        const fields = FieldGroupUtils.getGroupFields(LC.PERIOD_FINDER);
                        const per = fields && get(fields, ['period', 'value']);

                        dispatchValueChange({
                            fieldKey: lowerCase(LC.PERIOD_CNAME),
                            groupKey: LC.PERIOD_FINDER,
                            value: per
                        });
                    }
                }
                break;
        }

        if (newLayoutInfo !== layoutInfo) {
            dispatchUpdateLayoutInfo(newLayoutInfo);
        }
        if (bInitPeriod) {
            periodPageMode(LC.PERIOD_PAGE);
        }
    }
}

function handleMenuSetting(action) {
    return (get(action, ['payload', 'tbl_id']) === LC.RAW_TABLE);
}

function handleTableLoad(layoutInfo, action) {
    const {tbl_id} = action.payload;

    layoutInfo =  Object.assign({}, layoutInfo, {showTables: true, showXyPlots: true});
    if (tbl_id === LC.RAW_TABLE) {
        layoutInfo = Object.assign({}, layoutInfo, {displayMode: LC.RESULT_PAGE});
    }

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

    const per = getPeriodFromTable(tbl_id);
    if (per) {
        dispatchValueChange({fieldKey: lowerCase(LC.PERIOD_CNAME), groupKey: LC.PERIOD_FINDER, value: `${parseFloat(per)}`});
    }

}

/**
 * gets the period from either peak or periodogram table, these tables don't necessarily have a period column and same name
 * @param {string} tbl_id
 * @returns {string} period value
 */
function getPeriodFromTable(tbl_id) {

    if (tbl_id !== LC.PERIODOGRAM && tbl_id !== LC.PEAK_TABLE) {
        return '';
    }

    const tableModel = getTblById(tbl_id);

    if (!tableModel || isNil(tableModel.highlightedRow)) {
        return '';
    }

    return getCellValue(tableModel, tableModel.highlightedRow, LC.PERIOD_CNAME);
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


