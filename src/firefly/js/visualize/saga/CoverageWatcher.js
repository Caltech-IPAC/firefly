/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {get,isEmpty,isObject, flattenDeep, values, isUndefined} from 'lodash';
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
import {findTableCenterColumns, isCatalog, isTableWithRegion, hasCoverageData, findTableRegionColumn} from '../../util/VOAnalyzer.js';
import {parseObsCoreRegion} from '../../util/ObsCoreSRegionParser.js';
import {getAppOptions} from '../../core/AppDataCntlr';
import {getSearchTarget} from './CatalogWatcher';
import {MetaConst} from '../../data/MetaConst.js';
import {getPlotViewById} from '../PlotViewUtil';
import {isHiPS} from '../WebPlot';

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
    overlayPosition: undefined,
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
    paused: true
};


const overlayCoverageDrawing= makeOverlayCoverageDrawing();


export function startCoverageWatcher(options) {
    dispatchAddTableTypeWatcherDef( { ...coverageWatcherDef, options });
}


/** @type {TableWatcherDef} */
export const coverageWatcherDef = {
    id : 'CoverageWatcher',
    testTable : (table) => hasCoverageData(table),
    sharedData: { preparedTables: {}, tblCatIdMap: {}},
    watcher : watchCoverage,
    allowMultiples: false,
    actions: [TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_REMOVE, TBL_RESULTS_ACTIVE,
        ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_HIPS,
        MultiViewCntlr.ADD_VIEWER, MultiViewCntlr.VIEWER_MOUNTED, MultiViewCntlr.VIEWER_UNMOUNTED]
};

const getOptions= (inputOptions) => ({...defOptions, ...cleanUpOptions(inputOptions)});
const centerId = (tbl_id) => (tbl_id+'_center');
const regionId = (tbl_id) => (tbl_id+'_region');

/**
 * Action watcher callback: watch the tables and update coverage display
 * @callback actionWatcherCallback
 * @param tbl_id
 * @param action
 * @param cancelSelf
 * @param params
 * @param params.options read-only
 * @param params.sharedData read-only
 * @param params.sharedData.preparedTables
 * @param params.sharedData.tblCatIdMap
 * @param params.sharedData.preferredHipsSourceURL
 * @param params.paused
 */
function watchCoverage(tbl_id, action, cancelSelf, params) {



    const {sharedData} = params;
    const options= getOptions(params.options);
    const {viewerId}= options;
    let paused = isUndefined(params.paused) ? options.paused : params.paused;
    const {preparedTables, tblCatIdMap}= sharedData;

    if (paused) {
        paused= !get(getViewer(getMultiViewRoot(), viewerId),'mounted', false);
    }
    if (!action) {
        if (!paused) {
            preparedTables[tbl_id]= undefined;
            sharedData.preferredHipsSourceURL= findPreferredHiPS(tbl_id, sharedData.preferredHipsSourceURL, options.hipsSourceURL, preparedTables[tbl_id]);
            updateCoverage(tbl_id, viewerId, sharedData.preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL);
        }
        return;
    }

    const {payload}= action;
    if (payload.tbl_id && payload.tbl_id!==tbl_id) return;
    if (payload.viewerId && payload.viewerId!==viewerId) return;



    if (action.type===REINIT_APP) {
        sharedData.preparedTables= {};
        cancelSelf();
        return;
    }

    if (action.type===MultiViewCntlr.VIEWER_MOUNTED || action.type===MultiViewCntlr.ADD_VIEWER)  {
        sharedData.preferredHipsSourceURL= findPreferredHiPS(tbl_id, sharedData.preferredHipsSourceURL, options.hipsSourceURL, preparedTables[tbl_id]);
        updateCoverage(tbl_id, viewerId, preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL);
        return {paused:false};
    }

    if (action.type===TABLE_LOADED) preparedTables[tbl_id]= undefined;
    if (paused) return {paused};

    switch (action.type) {

        case TABLE_LOADED:
            if (!getTableInGroup(tbl_id)) return {paused};
            sharedData.preferredHipsSourceURL= findPreferredHiPS(tbl_id, sharedData.preferredHipsSourceURL, options.hipsSourceURL, preparedTables[tbl_id]);
            updateCoverage(tbl_id, viewerId, preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL);
            break;

        case TBL_RESULTS_ACTIVE:
            if (!getTableInGroup(tbl_id)) return {paused};
            sharedData.preferredHipsSourceURL= findPreferredHiPS(tbl_id, sharedData.preferredHipsSourceURL, options.hipsSourceURL, preparedTables[tbl_id]);
            updateCoverage(tbl_id, viewerId, preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL);
            break;

        case TABLE_REMOVE:
            removeCoverage(payload.tbl_id, preparedTables);
            if (!isEmpty(preparedTables)) {
                sharedData.preferredHipsSourceURL= findPreferredHiPS(tbl_id, sharedData.preferredHipsSourceURL, options.hipsSourceURL, preparedTables[tbl_id]);
                updateCoverage(getActiveTableId(), viewerId, preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL);
            }
            cancelSelf();
            break;

        case TABLE_SELECT:
            tblCatIdMap[tbl_id].forEach(( cId ) => {
                dispatchModifyCustomField(cId, {selectInfo: action.payload.selectInfo});
            });
            break;

        case TABLE_HIGHLIGHT:
        case TABLE_UPDATE:
            tblCatIdMap[tbl_id].forEach(( cId ) => {
                dispatchModifyCustomField(cId, {highlightedRow: action.payload.highlightedRow});
            });
            break;

        case MultiViewCntlr.VIEWER_UNMOUNTED:
            paused = true;
            break;

        case ImagePlotCntlr.PLOT_IMAGE:
        case ImagePlotCntlr.PLOT_HIPS:
            if (action.payload.plotId===PLOT_ID) overlayCoverageDrawing(preparedTables,options, tblCatIdMap);
            break;
            
    }
    return {paused};
}



function removeCoverage(tbl_id, preparedTables) {
    if (tbl_id) Reflect.deleteProperty(preparedTables, tbl_id);
    if (isEmpty(Object.keys(preparedTables))) {
        dispatchDeletePlotView({plotId:PLOT_ID});
    }
}

/**
 * @param {string} tbl_id
 * @param {string} viewerId
 * @param preparedTables
 * @param {CoverageOptions} options
 * @param {object} tblCatIdMap
 * @param {string} preferredHipsSourceURL
 */
function updateCoverage(tbl_id, viewerId, preparedTables, options, tblCatIdMap, preferredHipsSourceURL) {

    try {
        const table = getTblById(tbl_id);
        if (!table) return;
        if (preparedTables[tbl_id] === 'WORKING') return;


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

        if (preparedTables[tbl_id] /*&& preparedTables[tbl_id].tableMeta.resultSetID===table.tableMeta.resultSetID*/) { //todo support decimated data
            updateCoverageWithData(viewerId, table, options, tbl_id, preparedTables[tbl_id], preparedTables,
                isTableUsingRadians(table), tblCatIdMap, preferredHipsSourceURL );
        }
        else {
            preparedTables[tbl_id] = 'WORKING';
            doFetchTable(req).then(
                (allRowsTable) => {
                    if (get(allRowsTable, ['tableData', 'data'], []).length > 0) {
                        preparedTables[tbl_id] = allRowsTable;
                        const isRegion = isTableWithRegion(allRowsTable);
                        //const isCatalog = findTableCenterColumns(allRowsTable);

                        tblCatIdMap[tbl_id] = (isRegion) ? [centerId(tbl_id), regionId(tbl_id)] : [tbl_id];
                        updateCoverageWithData(viewerId, table, options, tbl_id, allRowsTable, preparedTables,
                            isTableUsingRadians(table), tblCatIdMap, preferredHipsSourceURL );
                    }
                }
            ).catch(
                (reason) => {
                    preparedTables[tbl_id] = undefined;
                    tblCatIdMap[tbl_id] = undefined;
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
 * @param preparedTables
 * @param usesRadians
 * @param {object} tblCatIdMap
 * @param {string} preferredHipsSourceURL
 */
function updateCoverageWithData(viewerId, table, options, tbl_id, allRowsTable, preparedTables,
                                usesRadians, tblCatIdMap, preferredHipsSourceURL ) {
    const {maxRadius, avgOfCenters}= computeSize(options, preparedTables, usesRadians);
    if (!avgOfCenters || maxRadius<=0) return;

    const plot= primePlot(visRoot(), PLOT_ID);

    if (plot &&
        pointEquals(avgOfCenters,plot.attributes[COVERAGE_TARGET]) && plot.attributes[COVERAGE_RADIUS]===maxRadius ) {
        overlayCoverageDrawing(preparedTables, options, tblCatIdMap);
        return;
    }

    const {fovDegFallOver, fovMaxFitsSize, autoConvertOnZoom,
        imageSourceParams, fovDegMinSize, overlayPosition= avgOfCenters}= options;

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

    const hipsRequest= initRequest(WebPlotRequest.makeHiPSRequest(preferredHipsSourceURL, null),
                       viewerId, PLOT_ID, overlayPosition, avgOfCenters);
    hipsRequest.setSizeInDeg(size);

    dispatchPlotImageOrHiPS({
        plotId: PLOT_ID, viewerId, hipsRequest, imageRequest, allSkyRequest,
        fovDegFallOver, fovMaxFitsSize, autoConvertOnZoom, plotAllSkyFirst,
        pvOptions: {userCanDeletePlots:false, displayFixedTarget:false},
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
 * @param preparedTables
 * @param usesRadians
 * @return {*}
 */
function computeSize(options, preparedTables, usesRadians) {
    const ary= values(preparedTables);
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
     * @param preparedTables
     * @param {CoverageOptions} options
     * @param {object} tblCatIdMap
     */
    return (preparedTables, options, tblCatIdMap) => {
        const plot=  primePlot(visRoot(),PLOT_ID);
        if (!plot) return;
        const tbl_id=  plot.attributes[COVERAGE_TABLE];
        if (!tbl_id || !preparedTables[tbl_id] || !getTblById(tbl_id)) return;
        const table= getTblById(tbl_id);

        if (isCatalog(table) && options.ignoreCatalogs) return; // let the catalog watcher just handle the drawing overlays

        if (tblCatIdMap[tbl_id]) {
            tblCatIdMap[tbl_id].forEach((cId) => {
                const layer = getDrawLayerById(getDlAry(), cId);
                if (layer) {
                    drawingOptions[cId] = layer.drawingDef;    // drawingDef and selectOption is stored as layer based
                    selectOps[cId] = layer.selectOption;
                    dispatchDestroyDrawLayer(cId);
                }
            });
        }

        const overlayAry=  Object.keys(preparedTables);

        overlayAry.forEach( (id) => {
            tblCatIdMap[id] && tblCatIdMap[id].forEach((cId) => {
                if (!drawingOptions[cId]) drawingOptions[cId] = {};
                if (!drawingOptions[cId].color) drawingOptions[cId].color = lookupOption(options, 'color', cId) || getNextColor();
                if (selectOps[cId]) drawingOptions[cId].selectOption = selectOps[cId];
            });
            const oriTable= getTblById(id);
            const arTable= preparedTables[id];
            if (oriTable && arTable) addToCoverageDrawing(PLOT_ID, options, oriTable, arTable, drawingOptions);

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
    const {tableMeta, tableData}= allRowsTable;
    const angleInRadian= isTableUsingRadians(tableMeta);

    const createDrawLayer = (cId, dataType, isFromRegion=false) => {
        const columns = dataType === CoverageType.REGION ? findTableRegionColumn(allRowsTable) :
                        (covType === CoverageType.BOX ? getCornersColumns(allRowsTable) : findTableCenterColumns(allRowsTable));
        if (isEmpty(columns)) return;

        const dl = getDlAry().find((dl) => dl.drawLayerTypeId === Catalog.TYPE_ID && dl.catalogId === cId);
        if (!dl) {
            const {showCatalogSearchTarget}= getAppOptions();
            const searchTarget= showCatalogSearchTarget ? getSearchTarget(table.request,
                                                                lookupOption(options, 'searchTarget', cId),
                                                                lookupOption(options, 'overlayPosition', cId))
                                                         : undefined;
            dispatchCreateDrawLayer(Catalog.TYPE_ID, {
                catalogId: cId,
                tblId: table.tbl_id,
                title: `Coverage: ${table.title || table.tbl_id}` +
                             (isFromRegion ? (dataType===CoverageType.REGION ? ' regions' : ' positions') : ''),
                color:  drawOp[cId].color,
                tableData,
                tableMeta,
                tableRequest: table.request,
                highlightedRow: table.highlightedRow,
                catalog: dataType === CoverageType.X,
                boxData: dataType !== CoverageType.X,
                dataType: dataType.key,
                isFromRegion,
                columns,
                symbol: drawOp.symbol || lookupOption(options, 'symbol', cId),
                size: drawOp.size || lookupOption(options, 'symbolSize', cId),
                selectInfo: table.selectInfo,
                angleInRadian,
                dataTooBigForSelection: table.totalRows > 10000,
                tableSelection: (dataType === CoverageType.REGION) ? (drawOp[cId].selectOption || TableSelectOptions.all.key) : null,
                searchTarget,
            });
            dispatchAttachLayerToPlot(cId, plotId);
        }
    };

    if (covType === CoverageType.BOX) {
        createDrawLayer(table.tbl_id, covType);
    } else if (covType === CoverageType.X) {
        createDrawLayer(table.tbl_id, covType);
    } else if (covType === CoverageType.REGION) {
        const layerType = [CoverageType.X, CoverageType.REGION];
        const layerId = [centerId(table.tbl_id), regionId(table.tbl_id)];

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
         return  isTableWithRegion(table) ? CoverageType.REGION :
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
    const rCol = findTableRegionColumn(table);

    return table.tableData.data.map((row) => {
             const cornerInfo = parseObsCoreRegion(row[rCol.regionIdx], rCol.unit, true);

            return cornerInfo.valid ? cornerInfo.corners : [];
        }).filter((r) => !isEmpty(r));
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


/**
 *
 * @param tbl_id
 * @param prevPreferredHipsSourceURL
 * @param optionHipsSourceURL
 * @param preparedTable
 * @return {*}
 */
function findPreferredHiPS(tbl_id,prevPreferredHipsSourceURL, optionHipsSourceURL, preparedTable) {

    if (!preparedTable) { // if a new table then the meta takes precedence
        const table = getTblById(tbl_id);
        if (table && table.tableMeta[MetaConst.COVERAGE_HIPS]) return table.tableMeta[MetaConst.COVERAGE_HIPS];
    }
    const plot= primePlot(visRoot(), PLOT_ID);
    if (isHiPS(plot)) {
        return plot.hipsUrlRoot;
    }
    if (prevPreferredHipsSourceURL) return prevPreferredHipsSourceURL;
    if (optionHipsSourceURL) return optionHipsSourceURL;
}