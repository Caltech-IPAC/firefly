/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {get,isEmpty,isObject, flattenDeep, values, isUndefined, set} from 'lodash';
import {WebPlotRequest, TitleOptions} from '../WebPlotRequest.js';
import {TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_UPDATE,
        TABLE_REMOVE, TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import ImagePlotCntlr, {visRoot, dispatchDeletePlotView, dispatchPlotImageOrHiPS} from '../ImagePlotCntlr.js';
import {primePlot, getDrawLayerById} from '../PlotViewUtil.js';
import {REINIT_APP} from '../../core/AppDataCntlr.js';
import {doFetchTable, getTblById, getActiveTableId, getTableInGroup, isTableUsingRadians} from '../../tables/TableUtil.js';
import {cloneRequest, makeTableFunctionRequest, MAX_ROW } from '../../tables/TableRequestUtil.js';
import MultiViewCntlr, {getMultiViewRoot, getViewer} from '../MultiViewCntlr.js';
import {serializeDecimateInfo} from '../../tables/Decimate.js';
import {DrawSymbol} from '../draw/PointDataObj.js';
import {computeCentralPtRadiusAverage, toDegrees} from '../VisUtil.js';
import {makeWorldPt, pointEquals} from '../Point.js';
import {logError} from '../../util/WebUtil.js';
import {getCornersColumns} from '../../tables/TableInfoUtil.js';
import {dispatchCreateDrawLayer,dispatchDestroyDrawLayer, dispatchModifyCustomField,
                         dispatchAttachLayerToPlot, getDlAry} from '../DrawLayerCntlr.js';
import Catalog from '../../drawingLayers/Catalog.js';
import {TableSelectOptions} from '../../drawingLayers/CatalogUI.jsx';
import {getNextColor} from '../draw/DrawingDef.js';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga';
import {findTableCenterColumns, isCatalog, hasCoverageData, findTableRegionColumn} from '../../util/VOAnalyzer.js';
import {parseObsCoreRegion} from '../../util/ObsCoreSRegionParser.js';

export const CoverageType = new Enum(['X', 'BOX', 'REGION', 'ALL', 'GUESS']);
export const FitType=  new Enum (['WIDTH', 'WIDTH_HEIGHT']);

const COVERAGE_TARGET = 'COVERAGE_TARGET';
const COVERAGE_RADIUS = 'COVERAGE_RADIUS';
const COVERAGE_TABLE = 'COVERAGE_TABLE';
export const COVERAGE_CREATED = 'COVERAGE_CREATED';

export const PLOT_ID= 'CoveragePlot';



/**
 * @global
 * @public
 * @typedef {Object} CoverageOptions
 * @summary options of coverage
 *
 * @prop {string} title
 * @prop {string} tip
 * @prop {string} coverageType - one of 'GUESS', 'BOX', 'REGION', 'ALL', or 'X' default is 'ALL'
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
    coverageType : CoverageType.ALL,
    symbol : DrawSymbol.SQUARE,
    symbolSize : 5,
    overlayPosition: null,
    color : null,
    highlightedColor : 'blue',
    multiCoverage : true, // this is not longer supported, we now always do multi coverage
    gridOn : false,
    useBlankPlot : false,
    fitType : FitType.WIDTH_HEIGHT,
    ignoreCatalogs:false,

    useHiPS: true,
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
    viewerId:'DefCoverageId',
    paused: true,
};


const overlayCoverageDrawing= makeOverlayCoverageDrawing();


export function startCoverageWatcher(options) {
    dispatchAddTableTypeWatcherDef( { ...coverageWatcherDef, options });
}


/** @type {TableWatcherDef} */
export const coverageWatcherDef = {
    id : 'CoverageWatcher',
    testTable : (table) => hasCoverageData(table),
    sharedData: { decimatedTables: {}},
    watcher : watchCoverage,
    actions: [TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_REMOVE, TBL_RESULTS_ACTIVE,
        ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_HIPS,
        MultiViewCntlr.ADD_VIEWER, MultiViewCntlr.VIEWER_MOUNTED, MultiViewCntlr.VIEWER_UNMOUNTED]
};

const getOptions= (inputOptions) => ({...defOptions, ...cleanUpOptions(inputOptions)});


/**
 * Action watcher callback: watch the tables and update coverage display
 * @callback actionWatcherCallback
 * @param tbl_id
 * @param action
 * @param cancelSelf
 * @param params
 * @param params.options read-only
 * @param params.decimatedTables read-only
 * @param params.paused
 * @param params.tbl_id
 * @param params.displayedTableId
 */
function watchCoverage(tbl_id, action, cancelSelf, params) {



    const {sharedData} = params;
    const options= getOptions(params.options);
    const {viewerId}= options;
    let paused = isUndefined(params.paused) ? options.paused : params.paused;
    const {decimatedTables}= sharedData;

    if (paused) {
        paused= !get(getViewer(getMultiViewRoot(), viewerId),'mounted', false);
    }
    if (!action) {
        if (!paused) {
            decimatedTables[tbl_id]= undefined;
            updateCoverage(tbl_id, viewerId, sharedData.decimatedTables, options);
        }
        return params;
    }

    const {payload}= action;
    if (payload.tbl_id && payload.tbl_id!==tbl_id) return params;
    if (payload.viewerId && payload.viewerId!==viewerId) return params;



    if (action.type===REINIT_APP) {
        sharedData.decimatedTables= {};
        cancelSelf();
        return;
    }

    if (action.type===MultiViewCntlr.VIEWER_MOUNTED || action.type===MultiViewCntlr.ADD_VIEWER)  {
        updateCoverage(tbl_id, viewerId, decimatedTables, options);
        return {paused:false};
    }

    if (paused) return {paused};

    switch (action.type) {

        case TABLE_LOADED:
            if (!getTableInGroup(tbl_id)) return {paused};
            decimatedTables[tbl_id]= undefined;
            updateCoverage(tbl_id, viewerId, decimatedTables, options);
            break;

        case TBL_RESULTS_ACTIVE:
            if (!getTableInGroup(tbl_id)) return {paused};
            updateCoverage(tbl_id, viewerId, decimatedTables, options);
            break;

        case TABLE_REMOVE:
            removeCoverage(payload.tbl_id, decimatedTables);
            if (!isEmpty(decimatedTables)) updateCoverage(getActiveTableId(), viewerId, decimatedTables, options);
            cancelSelf();
            break;

        case TABLE_SELECT:
            const covType = getCoverageType(options, decimatedTables[tbl_id]);
            if (covType === CoverageType.REGION) {
                dispatchModifyCustomField(tbl_id + '_center', {selectInfo: action.payload.selectInfo});
                dispatchModifyCustomField(tbl_id + '_region', {selectInfo: action.payload.selectInfo});
            } else {
                dispatchModifyCustomField(tbl_id, {selectInfo: action.payload.selectInfo});
            }
            break;

        case TABLE_HIGHLIGHT:
        case TABLE_UPDATE:
            const cType = getCoverageType(options, decimatedTables[tbl_id]);

            if (cType === CoverageType.REGION) {
                dispatchModifyCustomField(tbl_id+'_center', {highlightedRow: action.payload.highlightedRow});
                dispatchModifyCustomField(tbl_id+'_region', {highlightedRow: action.payload.highlightedRow});
            } else {
                dispatchModifyCustomField(tbl_id, {highlightedRow: action.payload.highlightedRow});
            }
            break;

        case MultiViewCntlr.VIEWER_UNMOUNTED:
            paused = true;
            break;

        case ImagePlotCntlr.PLOT_IMAGE:
        case ImagePlotCntlr.PLOT_HIPS:
            if (action.payload.plotId===PLOT_ID) overlayCoverageDrawing(decimatedTables,options);
            break;
            
    }
    return {paused};
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
 */
function updateCoverage(tbl_id, viewerId, decimatedTables, options) {

    try {
        const table = getTblById(tbl_id);
        if (!table) return;
        if (decimatedTables[tbl_id] === 'WORKING') return;


        const params = {
            startIdx: 0,
            pageSize: MAX_ROW,
            inclCols: getCovColumnsForQuery(options, table)
        };

        let req = cloneRequest(table.request, params);
        if (table.totalRows > 10000) {
            const cenCol = findTableCenterColumns(table);
            if (!cenCol) return;
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
                    decimatedTables[tbl_id] = undefined;
                    logError(`Failed to catalog plot data: ${reason}`, reason);
                }
            );

        }
    } catch (e) {
        logError('Error updating coverage');
        console.log(e);
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
    const {maxRadius, avgOfCenters}= computeSize(options, decimatedTables, usesRadians);
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
            [COVERAGE_CREATED]: true
        }
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
 * @param usesRadians
 * @return {*}
 */
function computeSize(options, decimatedTables, usesRadians) {
    const ary= values(decimatedTables);
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
                case CoverageType.REGION:
                    ptAry = getRegionAryFromTable(options, t, usesRadians);
                    break;

            }
            return flattenDeep(ptAry);
    } );

    return computeCentralPtRadiusAverage(testAry);
}

function makeOverlayCoverageDrawing() {
    const drawingOptions= {};
    const selectOps = {};
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

        if (isCatalog(table) && options.ignoreCatalogs) return; // let the catalog watcher just handle the drawing overlays

        const layer= getDrawLayerById(getDlAry(), tbl_id);
        if (layer) {
            drawingOptions[tbl_id]= layer.drawingDef;
            selectOps[tbl_id] = layer.selectOption;
            dispatchDestroyDrawLayer(tbl_id);
        }

        const overlayAry=  Object.keys(decimatedTables);

        overlayAry.forEach( (id) => {
            if (!drawingOptions[id]) drawingOptions[id]= {};
            if (!drawingOptions[id].color) drawingOptions[id].color= lookupOption(options,'color',id) || getNextColor();
            const oriTable= getTblById(id);
            const arTable= decimatedTables[id];
            if (oriTable && arTable) addToCoverageDrawing(PLOT_ID, options, oriTable, arTable, drawingOptions[id], selectOps[id]);

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
 * @param {object} selectOp  select option for the drawing layer
 */
function addToCoverageDrawing(plotId, options, table, allRowsTable, drawOp, selectOp) {

    if (allRowsTable==='WORKING') return;
    const covType= getCoverageType(options,allRowsTable);
    const {tableMeta, tableData}= allRowsTable;
    const angleInRadian= isTableUsingRadians(tableMeta);

    const createDrawLayer = (tbl_id, dataType, isFromRegion=false) => {
        const columns = dataType === CoverageType.REGION ? findTableRegionColumn(allRowsTable) :
                        (covType === CoverageType.BOX ? getCornersColumns(allRowsTable) : findTableCenterColumns(allRowsTable));
        if (isEmpty(columns)) return;

        const dl = getDlAry().find((dl) => dl.drawLayerTypeId === Catalog.TYPE_ID && dl.catalogId === tbl_id);
        if (!dl) {
            dispatchCreateDrawLayer(Catalog.TYPE_ID, {
                catalogId: tbl_id,
                title: `Coverage: ${table.title || table.tbl_id}` +
                             (isFromRegion ? (dataType===CoverageType.REGION ? ' regions' : ' positions') : ''),
                color: drawOp.color,
                tableData,
                tableMeta,
                tableRequest: table.request,
                highlightedRow: table.highlightedRow,
                catalog: (dataType === CoverageType.X || dataType === CoverageType.REGION),
                dataType: dataType.key,
                columns,
                symbol: drawOp.symbol || lookupOption(options, 'symbol', tbl_id),
                size: drawOp.size || lookupOption(options, 'symbolSize', tbl_id),
                boxData: dataType !== CoverageType.X,
                selectInfo: table.selectInfo,
                angleInRadian,
                dataTooBigForSelection: table.totalRows > 10000,
                isFromRegion,
                tableSelection: (dataType === CoverageType.REGION) ? (selectOp || TableSelectOptions.all.key) : null
            });
            dispatchAttachLayerToPlot(tbl_id, plotId);
        }
    };

    if (covType === CoverageType.BOX) {
        createDrawLayer(table.tbl_id, covType);
    } else if (covType === CoverageType.X) {
        createDrawLayer(table.tbl_id, covType);
    } else if (covType === CoverageType.REGION) {
        const layerType = [CoverageType.X, CoverageType.REGION];
        const layerId = [table.tbl_id + '_center', table.tbl_id + '_region'];

        layerType.forEach((type, idx) => createDrawLayer(layerId[idx], layerType[idx], true));
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
        options.coverageType===CoverageType.REGION ||
        options.coverageType===CoverageType.BOX ||
        options.coverageType===CoverageType.ALL) {
         return  hasRegion(table) ? CoverageType.REGION :
                                    (hasCorners(options,table) ? CoverageType.BOX : CoverageType.X);
    }
    return options.coverageType;
}

function hasCorners(options, table) {
    const cornerColumns= getCornersColumns(table);
    if (isEmpty(cornerColumns)) return false;
    const dataCnt= table.tableData.data.reduce( (tot, row) =>
        cornerColumns.every( (cDef) => row[cDef.lonIdx]!=='' && row[cDef.latIdx]!=='') ? tot+1 : tot
    ,0);
    return dataCnt/table.tableData.data.length > .1;
}

function hasRegion(table) {
    const rCol = findTableRegionColumn(table);

    if (isEmpty(rCol)) return false;
    const {data} = table.tableData;

    const dataCnt = data.reduce( (tot, row) => (row[rCol.regionIdx] ? tot+1 : tot), 0);
    return dataCnt/data.length > .1;
}

function toAngle(d, radianToDegree)  {
    const v= Number(d);
    return (!isNaN(v) && radianToDegree) ? toDegrees(v): v;
}

function makePt(lonStr,latStr, csys, radianToDegree) {
    return makeWorldPt(toAngle(lonStr,radianToDegree), toAngle(latStr, radianToDegree), csys);
}

function getPtAryFromTable(options,table, usesRadians){
    const cDef= findTableCenterColumns(table);
    if (isEmpty(cDef)) return [];
    const {lonIdx,latIdx,csys}= cDef;
    return table.tableData.data
        .map( (row) =>
            (row[lonIdx]!=='' && row[latIdx]!=='') ? makePt(row[lonIdx], row[latIdx], csys, usesRadians) : undefined )
        .filter( (v) => v);
}

function getBoxAryFromTable(options,table, usesRadians){
    const cDefAry= getCornersColumns(table);
    return table.tableData.data
        .map( (row) => cDefAry
            .map( (cDef) =>
                (row[cDef.lonIdx]!=='' && row[cDef.latIdx]!=='') ? makePt(row[cDef.lonIdx], row[cDef.latIdx], cDef.csys, usesRadians) : undefined))
        .filter( (row) => row.every( (v) => v));
}


function getRegionAryFromTable(options, table, usesRadians) {
    const cDefAry = [findTableRegionColumn(table)];

    const vertAry = table.tableData.data.map((row) => {
            return cDefAry.map((rCol) => {
                const cornerInfo = parseObsCoreRegion(row[rCol.regionIdx], rCol.unit, true);

                return cornerInfo.valid ? cornerInfo.corners : [];
            }).filter ( (c) => !isEmpty(c));
        }).filter((r) => !isEmpty(r));

    return vertAry;
}

function getCovColumnsForQuery(options, table) {
    const cAry= [...getCornersColumns(table), findTableCenterColumns(table), findTableRegionColumn(table)];
    // column names should be in quotes
    // there should be no duplicates
    const base = cAry.filter((c)=>!isEmpty(c))
            .map( (c)=> (c.type === 'region') ? `"${c.regionCol}"` : `"${c.lonCol}","${c.latCol}"`)
            .filter((v,i,a) => a.indexOf(v) === i)
            .join();
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
