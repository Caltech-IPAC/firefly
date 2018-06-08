/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {get,isEmpty,isObject, flattenDeep, values, isUndefined} from 'lodash';
import {MetaConst} from '../../data/MetaConst.js';
import {WebPlotRequest, TitleOptions} from '../WebPlotRequest.js';
import {TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_UPDATE,
        TABLE_REMOVE, TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import ImagePlotCntlr, {visRoot, dispatchDeletePlotView, dispatchPlotImageOrHiPS} from '../ImagePlotCntlr.js';
import {primePlot, getDrawLayerById} from '../PlotViewUtil.js';
import {REINIT_APP} from '../../core/AppDataCntlr.js';
import {doFetchTable, getTblById, getTblIdsByGroup, getActiveTableId, getTableInGroup, isTableUsingRadians} from '../../tables/TableUtil.js';
import {cloneRequest, makeTableFunctionRequest, MAX_ROW } from '../../tables/TableRequestUtil.js';
import MultiViewCntlr, {getMultiViewRoot, getViewer} from '../MultiViewCntlr.js';
import {serializeDecimateInfo} from '../../tables/Decimate.js';
import {DrawSymbol} from '../draw/PointDataObj.js';
import {computeCentralPtRadiusAverage, toDegrees} from '../VisUtil.js';
import {makeWorldPt, pointEquals} from '../Point.js';
import {logError} from '../../util/WebUtil.js';
import {getCornersColumns, getCenterColumns} from '../../tables/TableInfoUtil.js';
import DrawLayerCntlr, {dispatchCreateDrawLayer,dispatchDestroyDrawLayer, dispatchModifyCustomField,
                         dispatchAttachLayerToPlot, getDlAry} from '../DrawLayerCntlr.js';
import Catalog from '../../drawingLayers/Catalog.js';
import {getNextColor} from '../draw/DrawingDef.js';
import {dispatchAddActionWatcher} from '../../core/MasterSaga.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';

export const CoverageType = new Enum(['X', 'BOX', 'BOTH', 'GUESS']);
export const FitType=  new Enum (['WIDTH', 'WIDTH_HEIGHT']);

const COVERAGE_TARGET = 'COVERAGE_TARGET';
const COVERAGE_RADIUS = 'COVERAGE_RADIUS';
const COVERAGE_TABLE = 'COVERAGE_TABLE';
const COVERAGE_CREATED = 'COVERAGE_CREATED';

const PLOT_ID= 'CoveragePlot';



/**
 * @global
 * @public
 * @typedef {Object} CoverageOptions
 * @summary options of coverage
 *
 * @prop {string} title
 * @prop {string} tip
 * @prop {string} coverageType - one of 'GUESS', 'BOX', 'BOTH', or 'X' default is 'BOTH'
 * @prop {string} overlayPosition search position point to overlay, e.g '149.08;68.739;EQ_J2000'
 * @prop {string|Object.<String,String>} symbol - symbol name one of 'X','SQUARE','CROSS','DIAMOND','DOT','CIRCLE','BOXCIRCLE', 'ARROW'
 * @prop {string|Object.<String,Number>} symbolSize - a number of the symbol size or an object keyed by table id and value the symbol size
 * @prop {string|Object.<String,String>} color - a color the symbol size or an object keyed by table id and color
 * @prop {number} fovMaxFitsSize how big this fits image can be (in degrees)
 * @prop {number} fovDegMinSize - minimum field of view size in degrees
 * @prop {number} fovDegFallOver - the field of view size to determine when to move between and HiPS and an image
 * @prop {boolean} multiCoverage - overlay more than one table  on the coverage
 * @prop {string} gridOn - one of 'FALSE','TRUE','TRUE_LABELS_FALSE'
 */


const defOptions= {
    title: '2MASS K_s',
    tip: 'Coverage',
    getCoverageBaseTitle : (table) => '',   // eslint-disable-line no-unused-vars
    coverageType : CoverageType.BOTH,
    symbol : DrawSymbol.SQUARE,
    symbolSize : 5,
    overlayPosition: null,
    color : null,
    highlightedColor : 'blue',
    multiCoverage : true,
    gridOn : false,
    useBlankPlot : false,
    fitType : FitType.WIDTH_HEIGHT,
    ignoreCatalogs:false,

    useHiPS: true, // useHiPS it initialized in the startCoverageWatcher
    hipsSourceURL : 'ivo://CDS/P/2MASS/color', // url
    imageSourceParams: {
        Service : 'TWOMASS',
        SurveyKey: 'asky',
        SurveyKeyBand: 'k',
        title : '2MASS K_s'
    },

    fovDegFallOver: .13,
    fovMaxFitsSize: .2,
    autoConvertOnZoom: false,
    fovDegMinSize: 100/3600, //defaults to 100 arcsec


    hasCoverageData,
    getCornersColumns,
    getCenterColumns,
};


const overlayCoverageDrawing= makeOverlayCoverageDrawing();


export function startCoverageWatcher(options) {

    const {viewerId='DefCoverageId'}= options;
    let {paused=true}= options;
    const decimatedTables=  {};

    if (paused) {
        paused= !get(getViewer(getMultiViewRoot(), viewerId),'mounted', false);
    }
    options= Object.assign({}, defOptions, cleanUpOptions(options));
    let displayedTableId= null;

    if (paused) {
        const firstId= getActiveTableId();
        if (firstId) displayedTableId = updateCoverage(firstId, viewerId, decimatedTables, options);
    }

    const params = {options, decimatedTables, paused, displayedTableId};

    const actions = [TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_REMOVE,
        TBL_RESULTS_ACTIVE,
        DrawLayerCntlr.ATTACH_LAYER_TO_PLOT,
        ImagePlotCntlr.PLOT_IMAGE,
        ImagePlotCntlr.PLOT_HIPS,
        MultiViewCntlr.ADD_VIEWER, MultiViewCntlr.VIEWER_MOUNTED,
        MultiViewCntlr.VIEWER_UNMOUNTED];
    
    dispatchAddActionWatcher({id: `cw-${viewerId}`, actions, callback: watchCoverage, params});
}

/**
 * Action watcher callback: watch the tables and update coverage display
 * @callback actionWatcherCallback
 * @param action
 * @param cancelSelf
 * @param params
 * @param params.options read-only
 * @param params.decimatedTables read-only
 * @param params.paused
 * @param params.tbl_id
 * @param params.displayedTableId
 */
function watchCoverage(action, cancelSelf, params) {


    let {paused, tbl_id, displayedTableId} = params;

    if (action.type===REINIT_APP) {
        cancelSelf();
        return;
    }

    if (paused && (action.type!==MultiViewCntlr.VIEWER_MOUNTED && action.type!==MultiViewCntlr.ADD_VIEWER) )  {
        return;
    }

    const {payload}= action;

    if (action.type===TABLE_REMOVE) {
        tbl_id= getActiveTableId();
    }
    else if (payload.tbl_id) {
        tbl_id= payload.tbl_id; // otherwise use the last one
    }

    const {options, decimatedTables} = params;
    const {viewerId='DefCoverageId'}= options;

    let previousDisplayedTableId= displayedTableId;

    switch (action.type) {

        case TABLE_LOADED:
            if (!getTableInGroup(tbl_id)) return;
            decimatedTables[tbl_id]= null;
            displayedTableId = updateCoverage(tbl_id, viewerId, decimatedTables, options);
            break;

        case TBL_RESULTS_ACTIVE:
            if (!getTableInGroup(tbl_id)) return;
            displayedTableId = updateCoverage(tbl_id, viewerId, decimatedTables, options);
            break;

        case TABLE_REMOVE:
            removeCoverage(payload.tbl_id, decimatedTables);
            if (!getTableInGroup(payload.tbl_id)) return;
            displayedTableId = null;
            previousDisplayedTableId = null;
            tbl_id = getActiveTableId();
            if (!isEmpty(decimatedTables)) {
                displayedTableId = updateCoverage(tbl_id, viewerId, decimatedTables, options);
            }
            break;

        case MultiViewCntlr.ADD_VIEWER:
        case MultiViewCntlr.VIEWER_MOUNTED:
            if (action.payload.viewerId === viewerId) {
                paused = false;
                const tblIdAry = getTblIdsByGroup();
                tblIdAry.forEach((tbl_id)=>updateCoverage(tbl_id, viewerId, decimatedTables, options));
            }
            break;

        case TABLE_SELECT:
            dispatchModifyCustomField(tbl_id, {selectInfo:action.payload.selectInfo});
            break;

        case TABLE_HIGHLIGHT:
        case TABLE_UPDATE:
            dispatchModifyCustomField(tbl_id, {highlightedRow:action.payload.highlightedRow});
            break;

        case MultiViewCntlr.VIEWER_UNMOUNTED:
            if (action.payload.viewerId === viewerId) paused = true;
            break;

        case ImagePlotCntlr.PLOT_IMAGE:
        case ImagePlotCntlr.PLOT_HIPS:
            if (action.payload.plotId===PLOT_ID) overlayCoverageDrawing(decimatedTables,options);
            break;
            
    }
    if (!displayedTableId) displayedTableId= previousDisplayedTableId;

    return {paused, decimatedTables, tbl_id, options, displayedTableId};
}



function removeCoverage(tbl_id, decimatedTables) {
    if (tbl_id) Reflect.deleteProperty(decimatedTables, tbl_id);
    if (isEmpty(Object.keys(decimatedTables))) {
        dispatchDeletePlotView({plotId:PLOT_ID});
    }
}

/**
 * @param {string} tbl_id
 * @param {string} viewerId
 * @param decimatedTables
 * @param {CoverageOptions} options
 * @return {Array}
 */
function updateCoverage(tbl_id, viewerId, decimatedTables, options) {

    if (!tbl_id) return null;
    try {
        const table = getTblById(tbl_id);
        if (!table) return null;
        if (!options.hasCoverageData(options, table)) return null;
        if (decimatedTables[tbl_id] === 'WORKING') return tbl_id;


        const params = {
            startIdx: 0,
            pageSize: MAX_ROW,
            inclCols: getCovColumnsForQuery(options, table)
        };

        let req = cloneRequest(table.request, params);
        if (table.totalRows > 10000) {
            const cenCol = options.getCenterColumns(table);
            if (!cenCol) return null;
            const sreq = cloneRequest(table.request, {inclCols: `"${cenCol.lonCol}","${cenCol.latCol}"`});
            req = makeTableFunctionRequest(sreq, 'DecimateTable', 'coverage',
                {decimate: serializeDecimateInfo(cenCol.lonCol, cenCol.latCol, 10000), pageSize: MAX_ROW});
        }

        req.tbl_id = `cov-${tbl_id}`;

        if (decimatedTables[tbl_id] /*&& decimatedTables[tbl_id].tableMeta.resultSetID===table.tableMeta.resultSetID*/) { //todo support decimated data
            updateCoverageWithData(viewerId, table, options, tbl_id, decimatedTables[tbl_id], decimatedTables, isTableUsingRadians(table));
        }
        else {
            decimatedTables[tbl_id] = 'WORKING';
            doFetchTable(req).then(
                (allRowsTable) => {
                    if (get(allRowsTable, ['tableData', 'data'], []).length > 0) {
                        decimatedTables[tbl_id] = allRowsTable;
                        updateCoverageWithData(viewerId, table, options, tbl_id, allRowsTable, decimatedTables, isTableUsingRadians(table));
                    }
                }
            ).catch(
                (reason) => {
                    decimatedTables[tbl_id] = null;
                    logError(`Failed to catalog plot data: ${reason}`, reason);
                }
            );

        }
        return tbl_id;
    } catch (e) {
        logError('Error updating coverage');
        e.stack;

    }
}


/**
 *
 * @param {string} viewerId
 * @param {TableData} table
 * @param {CoverageOptions} options
 * @param {string} tbl_id
 * @param allRowsTable
 * @param decimatedTables
 * @param usesRadians
 */
function updateCoverageWithData(viewerId, table, options, tbl_id, allRowsTable, decimatedTables, usesRadians) {
    const {maxRadius, avgOfCenters}= computeSize(options, decimatedTables, allRowsTable, usesRadians);
    if (!avgOfCenters || maxRadius<=0) return;

    const plot= primePlot(visRoot(), PLOT_ID);

    if (plot &&
        pointEquals(avgOfCenters,plot.attributes[COVERAGE_TARGET]) && plot.attributes[COVERAGE_RADIUS]===maxRadius ) {
        overlayCoverageDrawing(decimatedTables, options);
        return;
    }

    const {fovDegFallOver, fovMaxFitsSize, autoConvertOnZoom, hipsSourceURL,
        imageSourceParams, useHiPS, fovDegMinSize, overlayPosition= avgOfCenters}= options;

    let plotAllSkyFirst= false;
    let allSkyRequest= null;
    const size= Math.max(maxRadius*2.2, fovDegMinSize);
    if (size>160) {
        allSkyRequest= WebPlotRequest.makeAllSkyPlotRequest();
        allSkyRequest.setTitleOptions(TitleOptions.PLOT_DESC);
        allSkyRequest= initRequest(allSkyRequest, viewerId, PLOT_ID, overlayPosition);
        plotAllSkyFirst= true;
    }
    let imageRequest= WebPlotRequest.makeFromObj(imageSourceParams) ||
                            WebPlotRequest.make2MASSRequest(avgOfCenters, 'asky', 'k', size);
    imageRequest= initRequest(imageRequest, viewerId, PLOT_ID, overlayPosition, avgOfCenters);

    const hipsRequest= initRequest(WebPlotRequest.makeHiPSRequest(hipsSourceURL, null),
                       viewerId, PLOT_ID, overlayPosition, avgOfCenters);
    hipsRequest.setSizeInDeg(size);

    dispatchPlotImageOrHiPS({
        plotId: PLOT_ID, viewerId, hipsRequest, imageRequest, allSkyRequest,
        fovDegFallOver, fovMaxFitsSize, autoConvertOnZoom, plotAllSkyFirst,
        attributes: {
            [COVERAGE_TARGET]: avgOfCenters,
            [COVERAGE_RADIUS]: maxRadius,
            [COVERAGE_TABLE]: tbl_id,
            [COVERAGE_CREATED]: true,
        },
    });
}

/**
 *
 * @param r
 * @param viewerId
 * @param plotId
 * @param overlayPos
 * @param wp
 * @return {*}
 */
function initRequest(r,viewerId,plotId, overlayPos, wp) {
    if (!r) return undefined;
    r= r.makeCopy();
    r.setPlotGroupId(viewerId);
    r.setPlotId(plotId);
    r.setOverlayPosition(overlayPos);
    if (wp) r.setWorldPt(wp);
    return r;
}



/**
 *
 * @param {CoverageOptions} options
 * @param decimatedTables
 * @param allRowsTable
 * @param usesRadians
 * @return {*}
 */
function computeSize(options, decimatedTables,allRowsTable, usesRadians) {
    const ary= options.multiCoverage ? values(decimatedTables) : [allRowsTable];
    const testAry= ary
        .filter( (t) => t && t!=='WORKING')
        .map( (t) => {
            let ptAry= [];
            const covType= getCoverageType(options,t);
            switch (covType) {
                case CoverageType.X:
                    ptAry= getPtAryFromTable(options,t, usesRadians);
                    break;
                case CoverageType.BOX:
                    ptAry= getBoxAryFromTable(options,t, usesRadians);
                    break;

            }
            return flattenDeep(ptAry);
    } );

    return computeCentralPtRadiusAverage(testAry);
}

function makeOverlayCoverageDrawing() {
    const drawingOptions= {};
    /**
     *
     * @param decimatedTables
     * @param {CoverageOptions} options
     */
    return (decimatedTables, options) => {
        const plot=  primePlot(visRoot(),PLOT_ID);
        if (!plot) return;
        const tbl_id=  plot.attributes[COVERAGE_TABLE];
        if (!tbl_id || !decimatedTables[tbl_id] || !getTblById(tbl_id)) return;
        const table= getTblById(tbl_id);

        if (table.tableMeta[MetaConst.CATALOG_OVERLAY_TYPE] && options.ignoreCatalogs) return; // let the catalog just handle the drawing overlays

        const allRowsTable= decimatedTables[tbl_id];

        const layer= getDrawLayerById(getDlAry(), tbl_id);
        if (layer) {
            drawingOptions[tbl_id]= layer.drawingDef;
            dispatchDestroyDrawLayer(tbl_id);
        }

        const overlayAry=  options.multiCoverage ? Object.keys(decimatedTables) : [allRowsTable.tbl_id];

        overlayAry.forEach( (id) => {
            // if (id!==tbl_id) return;
            if (!drawingOptions[id]) drawingOptions[id]= {};
            if (!drawingOptions[id].color) drawingOptions[id].color= lookupOption(options,'color',id) || getNextColor();
            const oriTable= getTblById(id);
            const arTable= decimatedTables[id];
            if (oriTable && arTable) addToCoverageDrawing(PLOT_ID, options, oriTable, arTable, drawingOptions[id]);

        });
    };
}


/**
 *
 * @param {string} plotId
 * @param {CoverageOptions} options
 * @param {TableData} table
 * @param {TableData} allRowsTable
 * @param {string} drawOp
 */
function addToCoverageDrawing(plotId, options, table, allRowsTable, drawOp) {

    if (allRowsTable==='WORKING') return;
    const covType= getCoverageType(options,allRowsTable);

    const boxData= covType===CoverageType.BOTH || covType===CoverageType.BOX;
    const {tbl_id}= table;
    const {tableMeta, tableData}= allRowsTable;
    const columns = boxData ? options.getCornersColumns(table) : options.getCenterColumns(table);
    if (isEmpty(columns)) { return; }
    const angleInRadian= isTableUsingRadians(tableMeta);
    const dl= getDlAry().find( (dl) => dl.drawLayerTypeId===Catalog.TYPE_ID && dl.catalogId===table.tbl_id);
    if (!dl) {
        dispatchCreateDrawLayer(Catalog.TYPE_ID, {
            catalogId: table.tbl_id,
            title: `Coverage: ${table.title || table.tbl_id}`,
            color: drawOp.color,
            tableData,
            tableMeta,
            tableRequest: table.request,
            highlightedRow: table.highlightedRow,
            catalog: !boxData,
            columns,
            symbol: drawOp.symbol || lookupOption(options,'symbol',tbl_id),
            size: drawOp.size || lookupOption(options,'symbolSize',tbl_id),
            boxData,
            selectInfo: table.selectInfo,
            angleInRadian,
            dataTooBigForSelection: table.totalRows>10000
        });
        dispatchAttachLayerToPlot(table.tbl_id, plotId);
    }
}


/**
 * look up a value from the CoverageOptions
 * @param {CoverageOptions} options
 * @param {string} key
 * @param {string} tbl_id
 * @return {*}
 */
function lookupOption(options, key, tbl_id) {
    const value= options[key];
    if (!value) return undefined;
    return isObject(value) ? value[tbl_id] : value;
}

function getCoverageType(options,table) {
    if (options.coverageType===CoverageType.GUESS ||
        options.coverageType===CoverageType.BOX ||
        options.coverageType===CoverageType.BOTH) {
         return hasCorners(options,table) ? CoverageType.BOX : CoverageType.X;
    }
    return options.coverageType;
}

const hasCorners= (options, table) =>!isEmpty(options.getCornersColumns(table));

function toAngle(d, radianToDegree)  {
    const v= Number(d);
    return (!isNaN(v) && radianToDegree) ? toDegrees(v): v;
}

function makePt(lonStr,latStr, csys, radianToDegree) {
    return makeWorldPt(toAngle(lonStr,radianToDegree), toAngle(latStr, radianToDegree), csys);
}

function getPtAryFromTable(options,table, usesRadians){
    const cDef= options.getCenterColumns(table);
    if (isEmpty(cDef)) return [];
    const {lonIdx,latIdx,csys}= cDef;
    return table.tableData.data.map( (row) => makePt(row[lonIdx], row[latIdx], csys, usesRadians) );
}

function getBoxAryFromTable(options,table, usesRadians){
    const cDefAry= options.getCornersColumns(table);
    return table.tableData.data
        .map( (row) => cDefAry
            .map( (cDef) => makeWorldPt(row[cDef.lonIdx], row[cDef.latIdx], cDef.csys)) );
}

/**
 * @summary check if there is center column or corner columns defined
 * @param options
 * @param table
 * @returns {boolean}
 */
function hasCoverageData(options, table) {
    if (!get(table, 'totalRows')) return false;
    if (!options.multiCoverage && table.tableMeta[MetaConst.CATALOG_OVERLAY_TYPE]) return false;
    return !isEmpty(options.getCenterColumns(table)) || !isEmpty(options.getCornersColumns(table));
}

function getCovColumnsForQuery(options, table) {
    const cAry= [...options.getCornersColumns(table), options.getCenterColumns(table)];
    const base = cAry.filter((c)=>!isEmpty(c)).map( (c)=> `"${c.lonCol}","${c.latCol}"`).join();     // column names should be in quotes
    return base+',"ROW_IDX"';
}

function cleanUpOptions(options) {
    const opStrList= Object.keys(defOptions);
    return Object.keys(options).reduce( (result, key) => {
        const properKey= opStrList.find( (testKey) => testKey.toLowerCase()===key.toLowerCase());
        result[properKey||key]= options[key];
        return result;
    },{});
}
