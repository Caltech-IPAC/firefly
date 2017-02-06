/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isNil, has} from 'lodash';
import {take} from 'redux-saga/effects';
import {SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo,
        dispatchUpdateLayoutInfo, dropDownHandler} from '../../core/LayoutCntlr.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_HIGHLIGHT,
        dispatchTableRemove, dispatchTableHighlight} from '../../tables/TablesCntlr.js';
import {getCellValue, getTblById, getTblIdsByGroup, getActiveTableId} from '../../tables/TableUtil.js';
import {updateSet, clone} from '../../util/WebUtil.js';
import ImagePlotCntlr, {dispatchPlotImage, visRoot, dispatchDeletePlotView,
        dispatchChangeActivePlotView,
        WcsMatchType, dispatchWcsMatch} from '../../visualize/ImagePlotCntlr.js';
import {getPlotViewById} from '../../visualize/PlotViewUtil.js';
import {getMultiViewRoot, dispatchReplaceViewerItems, getViewer} from '../../visualize/MultiViewCntlr.js';
import {WebPlotRequest,TitleOptions} from '../../visualize/WebPlotRequest.js';
import {ServerRequest} from '../../data/ServerRequest.js';
import {CHANGE_VIEWER_LAYOUT} from '../../visualize/MultiViewCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {VALUE_CHANGE, dispatchValueChange, dispatchMultiValueChange, dispatchRestoreDefaults}
        from '../../fieldGroup/FieldGroupCntlr.js';
import {makeWorldPt} from '../../visualize/Point.js';
import {CoordinateSys} from '../../visualize/CoordSys.js';
import {MetaConst} from '../../data/MetaConst.js';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {CHART_ADD, getChartDataElement} from '../../charts/ChartsCntlr.js';

export const LC = {
    RAW_TABLE: 'raw_table',          // raw table id
    PHASE_FOLDED: 'phase_folded',    // phase folded table id
    PERIODOGRAM_TABLE: 'periodogram',// periodogram table id
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
    META_ERROR_COLUMN: 'errorColumn',
    DEF_TIME_CNAME: 'mjd',
    DEF_FLUX_CNAME: 'w1mpro_ep',

    RESULT_PAGE: 'result',          // result layout
    PERIOD_PAGE: 'period',          // period finding layout with periodogram button
    PERGRAM_PAGE: 'periodogram',    // period finding layout with peak and periodogram tables/charts

    FG_FILE_FINDER: 'LC_FILE_FINDER',
    FG_VIEWER_FINDER: 'LC_VIWER_FINDER',
    FG_PERIOD_FINDER: 'LC_PERIOD_FINDER',         // period finding group for the form
    FG_PERIODOGRAM_FINDER: 'LC_PERIODOGRAM_FINDER',// periodogram finding group
    FG_TIMESERIES_FINDER: 'LC_TIMESERIES_FINDER', // result layout panel finding group
    PERIODOGRAM_GROUP: 'LC_PERIODOGRAM_TBL',    // table container, table group

    META_TIME_NAMES: 'timeNames',
    META_FLUX_NAMES: 'fluxNames',
    META_MISSION: MetaConst.DATASET_CONVERTER,

    MISSION_DATA: 'missionEntries',
    GENERAL_DATA:'generalEntries',

    FULL_TABLE_SIZE: 500
};

const plotIdRoot= 'LC_FRAME-';

export function updateLayoutDisplay(displayMode, periodState) {
    var updateObj = periodState ? {displayMode, periodState} : {displayMode};
    var newLayoutInfo = Object.assign({}, getLayouInfo(), updateObj);

    dispatchUpdateLayoutInfo(newLayoutInfo);
}

export function removeTablesFromGroup(tbl_group_id = 'main') {
    const tblAry = getTblIdsByGroup(tbl_group_id);

    tblAry&&tblAry.forEach((tbl_id) => {
        dispatchTableRemove(tbl_id);
    });
}

export var getValidValueFrom = (fields, valKey) => {
    var val =  get(fields, [valKey, 'valid']) && get(fields, [valKey, 'value']);

    return val ? val : '';
};


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
    webplotRequestCreator = params.WebplotRequestCreator || getWebPlotRequestViaWISEIbe;

    while (true) {
        const action = yield take([
            TBL_RESULTS_ADDED, TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_HIGHLIGHT, SHOW_DROPDOWN, SET_LAYOUT_MODE,
            CHANGE_VIEWER_LAYOUT, VALUE_CHANGE, ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW, CHART_ADD
        ]);

        /**
         * This is the current state of the layout store.  Action handlers should return newLayoutInfo if state changes
         * If state has changed, it will be dispacthed into the flux.
         * @type {LayoutInfo}   layoutInfo
         * @prop {boolean}  layoutInfo.showForm    show form panel
         * @prop {boolean}  layoutInfo.showTables  show tables panel
         * @prop {boolean}  layoutInfo.showXyPlots show charts panel
         * @prop {boolean}  layoutInfo.showImages  show images panel
         * @prop {string}   layoutInfo.searchDesc  optional string describing search criteria used to generate this result.
         * @prop {Object}   layoutInfo.images      images specific states
         * @prop {string}   layoutInfo.images.activeTableId  last active table id that images responded to
         * @prop {string}   layoutInfo.displayMode:'result' (result page), 'period' (period finding page), 'periodogram' or neither
         * @prop {string}   layoutInfo.minnsionEntries mission specific entries on result layout panel
         * @prop {array}    layoutInfo.generalEntries general entries for result layout panel
         * @prop {string}   layoutInfo.periodState  // period or periodogram
         */
        var layoutInfo = getLayouInfo();
        var newLayoutInfo = layoutInfo;

        newLayoutInfo = dropDownHandler(newLayoutInfo, action);
        switch (action.type) {
            case TBL_RESULTS_ADDED:
            case TABLE_LOADED:
                newLayoutInfo = handleTableLoad(newLayoutInfo, action);
                break;
            case TABLE_HIGHLIGHT:
                newLayoutInfo = handleTableHighlight(newLayoutInfo, action);
                break;
            case CHANGE_VIEWER_LAYOUT:
                newLayoutInfo = handleChangeMultiViewLayout(newLayoutInfo);
                break;
            case TBL_RESULTS_ACTIVE :
                newLayoutInfo = handleTableActive(newLayoutInfo, action);
                break;
            case ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW:
                newLayoutInfo = handlePlotActive(newLayoutInfo, action.payload.plotId);
                break;
            case CHART_ADD:
                 newLayoutInfo = handleAddChart(newLayoutInfo, action.payload.chartId);
                break;
            case VALUE_CHANGE:
                newLayoutInfo = handleValueChange(newLayoutInfo, action);
                break;
            default:
                break;
        }

        if (newLayoutInfo !== layoutInfo) {
            dispatchUpdateLayoutInfo(newLayoutInfo);
        }
    }
}

function getMissionName() {
    var mName = get(getLayouInfo(), ['misionEntries', MetaConst.DATASET_CONVERTER]);

    return mName ? mName.toUpperCase() : '';
}

function getFluxColumn() {
    return get(getLayouInfo(), ['misionEntries', LC.META_FLUX_CNAME], '');
}

function getCutoutSize() {
    return get(getLayouInfo(), ['generalEntries', 'cutoutSize'], '0.2');
}

var getImageTitle = (mName,frameId, cutoutSize, fluxCol = '',  webplotRequestCreator = null ) => {
    if (webplotRequestCreator && webplotRequestCreator === getWebPlotRequestViaWISEIbe) {
        const band=`${fluxCol}`.match(/\d/g);
        return 'WISE-W'+ band + '-'+ frameId + (cutoutSize ? ` size: ${cutoutSize}(deg)` : '');
    } else {
        return `${mName}-` + frameId + (cutoutSize ? ` size: ${cutoutSize}(deg)` : '');
    }
};

function updateRawTableChart(layoutInfo, timeCName, fluxCName) {
    var chartX = get(getChartDataElement(LC.RAW_TABLE), ['options', 'x', 'columnOrExpr']);
    var chartY = get(getChartDataElement(LC.RAW_TABLE), ['options', 'y', 'columnOrExpr']);

    if (chartX === timeCName && chartY === fluxCName) return;

    const timeCols = get(layoutInfo, [LC.MISSION_DATA, LC.META_TIME_NAMES], []);
    const fluxCols = get(layoutInfo, [LC.MISSION_DATA, LC.META_FLUX_NAMES], []);

    if (timeCols.includes(timeCName) && fluxCols.includes(fluxCName)) {
        const xyPlotParams = {x: {columnOrExpr: timeCName}, y: {columnOrExpr: fluxCName, options: 'grid,flip'}};
        loadXYPlot({chartId: LC.RAW_TABLE, tblId: LC.RAW_TABLE, xyPlotParams});
    }
}

function updatePhaseTableChart(layoutInfo, flux) {
    var chartY = get(getChartDataElement(LC.RAW_TABLE), ['options', 'y', 'columnOrExpr']);

    if (chartY === flux) return;

    const fluxCols = get(layoutInfo, [LC.MISSION_DATA, LC.META_FLUX_NAMES]);

    if (fluxCols.includes(flux)) {
        const xyPlotParams = {
            userSetBoundaries: {xMax: 2},
            x: {columnOrExpr: LC.PHASE_CNAME, options: 'grid'},
            y: {columnOrExpr: flux, options: 'grid,flip'}
        };
        loadXYPlot({chartId: LC.PHASE_FOLDED, tblId: LC.PHASE_FOLDED, xyPlotParams});
    }
}

/**
 * @summary handle value change on time, flux, cutout size
 * @param {object} layoutInfo
 * @param {object} action
 * @returns {*}
 */
function handleValueChange(layoutInfo, action) {
    var {fieldKey, value} = action.payload;
    var crtValue;

    if ([LC.META_TIME_CNAME, LC.META_FLUX_CNAME].includes(fieldKey)) {
        crtValue = get(layoutInfo, [LC.MISSION_DATA, fieldKey]);

        if (value !== crtValue) {
            var keyOfPeriod = '';

            if (fieldKey === LC.META_FLUX_CNAME &&
                get(layoutInfo, [LC.MISSION_DATA, LC.META_FLUX_NAMES]).includes(value)) {
                const actTbl = getActiveTableId();
                if (actTbl === LC.RAW_TABLE) {
                    updateRawTableChart(layoutInfo,
                                         get(layoutInfo, [LC.MISSION_DATA, LC.META_TIME_CNAME]), value);
                } else if (actTbl === LC.PHASE_FOLDED) {
                    updatePhaseTableChart(layoutInfo, value);
                }
                keyOfPeriod = 'flux';
            } else if (fieldKey === LC.META_TIME_CNAME &&
                       get(layoutInfo, [LC.MISSION_DATA, LC.META_TIME_NAMES]).includes(value)) {
                keyOfPeriod = 'time';   // assume time is unique
            }

            if (keyOfPeriod) {
                layoutInfo = updateSet(layoutInfo, [LC.MISSION_DATA, fieldKey], value);

                // update time or flux for period panel field group if it exists
                if (FieldGroupUtils.getGroupFields(LC.FG_PERIOD_FINDER)) {
                    dispatchValueChange({
                        groupKey: LC.FG_PERIOD_FINDER,
                        fieldKey: keyOfPeriod,
                        value
                    });
                }
            }
        }
    } else if (fieldKey === 'cutoutSize') { // cutoutsize changes
        if ((get(layoutInfo, [LC.GENERAL_DATA, fieldKey]) !== value) && (value > 0.0) ) {
            if (get(layoutInfo, ['displayMode']) === LC.RESULT_PAGE) {
                setupImages(get(layoutInfo, 'images.activeTableId'));
                layoutInfo = updateSet(layoutInfo, [LC.GENERAL_DATA, fieldKey], value);
            }
        }
    }
    return layoutInfo;
}

function handleAddChart(layoutInfo, chartId) {
    if (chartId === LC.PHASE_FOLDED) {
        if (get(layoutInfo, ['displaMode']) !== 'result') {
            layoutInfo = updateSet(layoutInfo, ['displayMode'], 'result');
        }
    }
    return layoutInfo;
}

/**
 * @summary highlight the table row & chart after change the active plot view
 * @param layoutInfo
 * @param plotId
 * @returns {*}
 */
function handlePlotActive(layoutInfo, plotId) {
    const tableId = get(layoutInfo, ['images', 'activeTableId']);

    var rowNum= Number(plotId.substring(plotIdRoot.length));
    dispatchTableHighlight(tableId, rowNum);
    return layoutInfo;
}
/*
 * field group, LC_VIEWER_FINDER, includes fieldKey from misionAry & generalAry
 * missionAry & generalAy is mission specific, more detail to be fixed after raw table metadata is defined. -- TO DO
 */
function handleRawTableLoad(layoutInfo, tblId) {
    const rawTable = getTblById(tblId);
    var   metaInfo = rawTable && get(rawTable, ['META_INFO']);
    const missionAry = [LC.META_TIME_CNAME, LC.META_FLUX_CNAME, LC.META_TIME_NAMES, LC.META_FLUX_NAMES,
                        MetaConst.DATASET_CONVERTER, LC.META_ERROR_COLUMN];
    const generalEntryAry = {cutoutSize: '0.2'};  // default setting for cutoutsize?

    // TODO - fill in mission information from table's metadata
    metaInfo = {[LC.META_TIME_CNAME]: 'mjd',
                [LC.META_FLUX_CNAME]: 'w1mpro_ep',
                [LC.META_TIME_NAMES]: ['mjd'],
                [LC.META_FLUX_NAMES]: ['w1mpro_ep', 'w2mpro_ep', 'w3mpro_ep', 'w4mpro_ep'],
                [MetaConst.DATASET_CONVERTER]: 'wise',
                [LC.META_ERROR_COLUMN]: ''};

    var missionEntries = missionAry.reduce((prev, key) => {
            prev[key] = metaInfo ? get(metaInfo, key, ''): '';
            return prev;
        }, {});

    var generalEntries = clone(generalEntryAry);
    var {columns, data} = rawTable.tableData;
    var tIdx = columns.findIndex((col) => (col.name === get(missionEntries, [LC.META_TIME_CNAME])));
    var arr = data.reduce((prev, e)=> {
        prev.push(parseFloat(e[tIdx]));
        return prev;
    }, []);

    var [tzero, tzeroMax] = arr.length > 0 ? [Math.min(...arr), Math.max(...arr)] : [0.0, 0.0];
    var max = 365;
    var min = Math.pow(10, -3);   // 0.0001

    var fields = FieldGroupUtils.getGroupFields(LC.FG_VIEWER_FINDER);
    var initState;
    if (fields) {
        initState = Object.keys(fields).reduce((prev, fieldKey) => {
            if (has(missionEntries, fieldKey)) {
                prev.push({fieldKey, value: get(missionEntries, fieldKey)});
            } else if (has(generalEntries,fieldKey)) {
                prev.push({fieldKey, value: get(generalEntries, fieldKey)});
            }
            return prev;
        }, []);
        dispatchMultiValueChange(LC.FG_VIEWER_FINDER, initState);
    }

    fields = FieldGroupUtils.getGroupFields(LC.FG_PERIOD_FINDER);
    if (fields) {
        initState = [
                    {fieldKey: 'time', value: get(missionEntries, [LC.META_TIME_CNAME])},
                    {fieldKey: 'flux', value: get(missionEntries, [LC.META_FLUX_CNAME])},
                    {fieldKey: 'periodMin', value: `${min}`},
                    {fieldKey: 'periodMax', value: `${max}`},
                    {fieldKey: 'period', value: `${min}`},
                    {fieldKey: 'tzero', value: `${tzero}`},
                    {fieldKey: 'tzeroMax', value: `${tzeroMax}`}];

        dispatchMultiValueChange(LC.FG_PERIOD_FINDER, initState);
    }
    fields = FieldGroupUtils.getGroupFields(LC.FG_PERIODOGRAM_FINDER);
    if (fields) {
        dispatchRestoreDefaults(LC.FG_PERIODOGRAM_FINDER);
    }

    return Object.assign(layoutInfo, {missionEntries, generalEntries,
                                      periodRange: {min, max, tzero, tzeroMax}});
}

/**
 * @summary handle on loaded table, update period and periodogram field groups, update chart
 * @param layoutInfo
 * @param action
 * @returns {*}
 */
function handleTableLoad(layoutInfo, action) {
    const {tbl_id} = action.payload;

    layoutInfo = Object.assign({}, layoutInfo, {showTables: true, showXyPlots: true});
    if (tbl_id === LC.RAW_TABLE) {         // a new raw table is loaded
        if (action.type === TABLE_LOADED) {
            layoutInfo = Object.assign(layoutInfo, {displayMode: LC.RESULT_PAGE, periodState: LC.PERIOD_PAGE});
            layoutInfo = handleRawTableLoad(layoutInfo, tbl_id);
        }
    }
    if (isImageEnabledTable(tbl_id)) {
        layoutInfo = updateSet(layoutInfo, 'showImages', true);
        layoutInfo = updateSet(layoutInfo, 'images.activeTableId', tbl_id);
        setupImages(tbl_id);
    }

    return layoutInfo;
}

/**
 * @summary handle on changing the active table, update image and chart
 * @param layoutInfo
 * @param action
 * @returns {*}
 */
function handleTableActive(layoutInfo, action) {
    const {tbl_id} = action.payload;
    if (isImageEnabledTable(tbl_id)) {
        layoutInfo = updateSet(layoutInfo, 'images.activeTableId', tbl_id);
        setupImages(tbl_id);
    }

    const timeCol = get(layoutInfo, [LC.MISSION_DATA, LC.META_TIME_CNAME]);
    const fluxCol = get(layoutInfo, [LC.MISSION_DATA, LC.META_FLUX_CNAME]);

    if (tbl_id === LC.RAW_TABLE) {
        updateRawTableChart(layoutInfo, timeCol, fluxCol);
    } else if (tbl_id === LC.PHASE_FOLDED) {
        updatePhaseTableChart(layoutInfo, fluxCol);
    }

    return layoutInfo;
    //return Object.assign({}, layoutInfo);
}

function handleTableHighlight(layoutInfo, action) {
    const {tbl_id} = action.payload;

    if (tbl_id !== LC.PERIODOGRAM_TABLE && tbl_id !== LC.PEAK_TABLE) {
        if (isImageEnabledTable(tbl_id)) {
            setupImages(tbl_id);
        }
    } else {
        const per = getPeriodFromTable(tbl_id);

        if (per) {
            dispatchValueChange({
                fieldKey: (LC.PERIOD_CNAME).toLowerCase(),
                groupKey: LC.FG_PERIOD_FINDER,
                value: `${parseFloat(per)}`
            });
        }
    }

    return layoutInfo;
    //return Object.assign({}, layoutInfo);
}

/**
 * @summary gets the period from either peak or periodogram table
 * @param {string} tbl_id
 * @returns {string} period value
 */
function getPeriodFromTable(tbl_id) {

    if (tbl_id !== LC.PERIODOGRAM_TABLE && tbl_id !== LC.PEAK_TABLE) {
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

function handleChangeMultiViewLayout(layoutInfo) {
    const activeTableId = get(layoutInfo, 'images.activeTableId');
    const tbl= getTblById(activeTableId);
    if (get(tbl, 'totalRows',0)>0) setupImages(activeTableId);
    return layoutInfo;
}

function getWebPlotRequestViaWISEIbe(tableModel, hlrow, cutoutSize, fluxCol) {
    const ra = getCellValue(tableModel, hlrow, 'ra');
    const dec = getCellValue(tableModel, hlrow, 'dec');
    const frameId = getCellValue(tableModel, hlrow, 'frame_id');
    var   wise_sexp_ibe = /(\d+)([0-9][a-z])(\w+)/g;
    var   res = wise_sexp_ibe.exec(frameId);
    const scan_id = res[1] + res[2];
    const scangrp = res[2];
    const frame_num = res[3];
    const band=`${fluxCol}`.match(/\d/g);
    const title= 'WISE-W'+ band + '-'+ frameId + (cutoutSize ? ` size: ${cutoutSize}(deg)` : '');

    const sr= new ServerRequest('ibe_file_retrieve');
    sr.setParam('mission', 'wise');
    sr.setParam('PROC_ID', 'ibe_file_retrieve');
    sr.setParam('ProductLevel',  '1b');
    sr.setParam('ImageSet', 'merge');
    sr.setParam('band', `${band}`);
    sr.setParam('scangrp', `${scangrp}`);
    sr.setParam('scan_id', `${scan_id}`);
    sr.setParam('frame_num', `${frame_num}`);
    sr.setParam('center', `${ra},${dec}`);
    sr.setParam('size', `${cutoutSize}`);
    sr.setParam('subsize', `${cutoutSize}`);
    sr.setParam('in_ra',`${ra}`);
    sr.setParam('in_dec',`${dec}`);

    const reqParams = WebPlotRequest.makeProcessorRequest(sr, 'wise');
    return addCommonReqParams(reqParams, title, makeWorldPt(ra,dec,CoordinateSys.EQ_J2000));
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
    const mName = getMissionName();

    /*the following should be from reading in the url column returned from LC search
     we are constructing the url for wise as the LC table does
     not have the url colume yet
     It is only for WISE, using default cutout size 0.2 deg
    const url = `http://irsa.ipac.caltech.edu/ibe/data/wise/merge/merge_p1bm_frm/${scangrp}/${scan_id}/${frame_num}/${scan_id}${frame_num}-w1-int-1b.fits`;
    */
    const serverinfo = 'http://irsa.ipac.caltech.edu/ibe/data/wise/merge/merge_p1bm_frm/';
    const centerandsize = cutoutSize ? `?center=${ra},${dec}&size=${cutoutSize}&gzip=false` : '';
    const url = `${serverinfo}${scangrp}/${scan_id}/${frame_num}/${scan_id}${frame_num}-w1-int-1b.fits${centerandsize}`;
    const plot_desc = `${mName}-${frameId}`;
    const reqParams = WebPlotRequest.makeURLPlotRequest(url, plot_desc);
    const title= getImageTitle(mName, frameId, cutoutSize);
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


        const cutoutSize = get(FieldGroupUtils.getGroupFields(LC.FG_VIEWER_FINDER), ['cutoutSize', 'value'],
                                                              getCutoutSize());
        const fluxCol = get(FieldGroupUtils.getGroupFields(LC.FG_VIEWER_FINDER), [LC.META_FLUX_CNAME, 'value'],
                                                              getFluxColumn());

        newPlotIdAry.forEach( (plotId) => {
            var pv = getPlotViewById(vr,plotId);
            const rowNum= Number(plotId.substring(plotIdRoot.length));
            var imgTitle = () => {
                return getImageTitle(getMissionName(), getCellValue(tableModel, rowNum, 'frame_id'), cutoutSize, fluxCol, webplotRequestCreator);
            };

            if (!pv || get(pv, ['request', 'params', 'Title']) !== imgTitle()) {
                const webPlotReq = webplotRequestCreator(tableModel,rowNum, cutoutSize,  fluxCol);

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


