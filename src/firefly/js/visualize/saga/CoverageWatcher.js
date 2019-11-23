/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {get,isEmpty,isObject, isString, flattenDeep, values, isUndefined} from 'lodash';
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
import {DrawSymbol} from '../draw/DrawSymbol.js';
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
import {isHiPS, isImage} from '../WebPlot';
import {PlotAttribute} from '../PlotAttribute.js';
import {getDrawLayersByType, isDrawLayerVisible} from '../PlotViewUtil';
import SearchTarget from '../../drawingLayers/SearchTarget.js';
import {darker} from '../../util/Color';

export const CoverageType = new Enum(['X', 'BOX', 'REGION', 'ALL', 'GUESS']);
export const FitType=  new Enum (['WIDTH', 'WIDTH_HEIGHT']);

const COVERAGE_TARGET = 'COVERAGE_TARGET';
const COVERAGE_RADIUS = 'COVERAGE_RADIUS';

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
const searchTargetId = (tbl_id) => (tbl_id+'_searchTarget');

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

        // case TBL_RESULTS_ACTIVE:
        //     if (!getTableInGroup(tbl_id)) return {paused};
        //     sharedData.preferredHipsSourceURL= findPreferredHiPS(tbl_id, sharedData.preferredHipsSourceURL, options.hipsSourceURL, preparedTables[tbl_id]);
        //     updateCoverage(tbl_id, viewerId, preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL);
        //     break;

        case TABLE_REMOVE:
            removeCoverage(payload.tbl_id, preparedTables);
            if (!isEmpty(preparedTables)) {
                sharedData.preferredHipsSourceURL= findPreferredHiPS(tbl_id, sharedData.preferredHipsSourceURL, options.hipsSourceURL, preparedTables[tbl_id]);
                updateCoverage(getActiveTableId(), viewerId, preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL, payload.tbl_id);
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
            if (action.payload.plotId===PLOT_ID) {
                setTimeout( () => overlayCoverageDrawing(preparedTables,options, tblCatIdMap, undefined), 5);
            }
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
 * @param {string} deletedTblId
 */
function updateCoverage(tbl_id, viewerId, preparedTables, options, tblCatIdMap, preferredHipsSourceURL, deletedTblId= undefined) {

    try {
        const table = getTblById(tbl_id);
        if (!table && deletedTblId) {
            overlayCoverageDrawing(preparedTables, options, tblCatIdMap, deletedTblId);
        }
        if (!table || table.isFetching) return;
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

        if (preparedTables[tbl_id]) { //todo support decimated data
            updateCoverageWithData(viewerId, table, options, tbl_id, preparedTables[tbl_id], preparedTables,
                isTableUsingRadians(table), tblCatIdMap, preferredHipsSourceURL, deletedTblId );
        }
        else {
            preparedTables[tbl_id] = 'WORKING';
            doFetchTable(req).then(
                (allRowsTable) => {
                    // EMPTY_TBL
                    // if (get(allRowsTable, 'tableData')) { // empty tables would have tableData.data undefined
                    if (get(allRowsTable, ['tableData', 'data'], []).length > 0) {
                        preparedTables[tbl_id] = allRowsTable;
                        const isRegion = isTableWithRegion(allRowsTable);
                        //const isCatalog = findTableCenterColumns(allRowsTable);

                        tblCatIdMap[tbl_id] = (isRegion) ? [centerId(tbl_id), regionId(tbl_id)] : [tbl_id, searchTargetId(tbl_id)];
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
 * @param {string} deletedTblId - only defined if a table has been deleted
 */
function updateCoverageWithData(viewerId, table, options, tbl_id, allRowsTable, preparedTables,
                                usesRadians, tblCatIdMap, preferredHipsSourceURL, deletedTblId ) {
    const {maxRadius, avgOfCenters}= computeSize(options, preparedTables, usesRadians);

    // EMPTY_TBL
    // if (!avgOfCenters || maxRadius<=0) {
    //     // coverage drawing might need to be cleared
    //     overlayCoverageDrawing(preparedTables, options, tblCatIdMap);
    //     return;
    // }
    if (!avgOfCenters || maxRadius<=0) return;

    const plot= primePlot(visRoot(), PLOT_ID);

    // if (plot &&
    //     pointEquals(avgOfCenters,plot.attributes[COVERAGE_TARGET]) && plot.attributes[COVERAGE_RADIUS]===maxRadius ) {
    //     overlayCoverageDrawing(preparedTables, options, tblCatIdMap, true);
    //     return;
    // }
    overlayCoverageDrawing(preparedTables, options, tblCatIdMap, deletedTblId||table.tbl_id);

    if (deletedTblId) return;

    const commonSearchTarget= getCommonSearchTarget(Object.values(preparedTables),options);

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

    const tblIdAry= Object.keys(preparedTables).filter( (v) => !isString(preparedTables[v]));

    const attributes= {
        [COVERAGE_TARGET]: avgOfCenters,
        [COVERAGE_RADIUS]: maxRadius,
        [PlotAttribute.VISUALIZED_TABLE_IDS]: tblIdAry,
        [PlotAttribute.COVERAGE_CREATED]: true,
        [PlotAttribute.REPLOT_WITH_NEW_CENTER]: true,
    };
    if (commonSearchTarget) attributes[PlotAttribute.CENTER_ON_FIXED_TARGET]= commonSearchTarget;

    dispatchPlotImageOrHiPS({
        plotId: PLOT_ID, viewerId, hipsRequest, imageRequest, allSkyRequest,
        fovDegFallOver, fovMaxFitsSize, autoConvertOnZoom, plotAllSkyFirst,
        pvOptions: {userCanDeletePlots:false, displayFixedTarget:false},
        attributes,
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
     * @param {String} affectedTblId - the table id that is affected
     */
    return (preparedTables, options, tblCatIdMap, affectedTblId) => {
        const plot=  primePlot(visRoot(),PLOT_ID);
        if (!plot && !affectedTblId) return;
        const tblIdAry=  plot ? plot.attributes[PlotAttribute.VISUALIZED_TABLE_IDS] : [affectedTblId];
        if (isEmpty(tblIdAry)) return;
        let visible= true;

        tblIdAry.forEach( (tbl_id) => {
            if (!tbl_id) return;
            const table= getTblById(tbl_id);

            if (!table) {
                if (tblCatIdMap[tbl_id]) {
                    tblCatIdMap[tbl_id].forEach((cId) => {
                        const layer = getDrawLayerById(getDlAry(), cId);
                        if (layer) dispatchDestroyDrawLayer(cId);
                    });
                }
            }

            if (isCatalog(table) && options.ignoreCatalogs) return; // let the catalog watcher just handle the drawing overlays

            if (tblCatIdMap[tbl_id]) {
                const searchTargetDL= getDrawLayerById(getDlAry(), searchTargetId(table.tbl_id));
                searchTargetDL && dispatchDestroyDrawLayer(searchTargetDL.drawLayerId);

                tblCatIdMap[tbl_id].forEach((cId) => {
                    const layer = getDrawLayerById(getDlAry(), cId);
                    const tableRemoved= !Boolean(getTblById(tbl_id));
                    if (layer && (tableRemoved || tbl_id===affectedTblId)) {
                        drawingOptions[cId] = layer.drawingDef;    // drawingDef and selectOption is stored as layer based
                        selectOps[cId] = layer.selectOption;
                        visible= plot ? isDrawLayerVisible(layer, plot.plotId) : true;
                        dispatchDestroyDrawLayer(cId);
                    }
                });
            }

            const overlayAry=  Object.keys(preparedTables);

            if (!plot) return;

            overlayAry.forEach( (id) => {
                tblCatIdMap[id] && tblCatIdMap[id].forEach((cId) => {
                    if (!drawingOptions[cId]) drawingOptions[cId] = {};
                    if (!drawingOptions[cId].color) {
                        drawingOptions[cId].color = preparedTables[id].tableMeta[MetaConst.DEFAULT_COLOR] || lookupOption(options, 'color', cId) || getNextColor();
                    }
                    if (selectOps[cId]) drawingOptions[cId].selectOption = selectOps[cId];
                });
                const oriTable= getTblById(id);
                const arTable= preparedTables[id];
                if (oriTable && arTable) addToCoverageDrawing(PLOT_ID, options, oriTable, arTable, drawingOptions, visible);

            });
        });
    };
}

function isUsingRadians(dataType,table,columns) {
    if (dataType=== CoverageType.X) {
        return isTableUsingRadians(table, [columns.lonCol,columns.latCol]);
    }
    else if (dataType=== CoverageType.BOX) {
        const cAry= columns.map( (c) => [c.lonCol,c.latCol]).flat();
        return  isTableUsingRadians(table, cAry);
    }
}

function getColumns(dataType,covType,allRowsTable) {
    return (
        dataType === CoverageType.REGION ?
            findTableRegionColumn(allRowsTable) :
            (covType === CoverageType.BOX ?
                getCornersColumns(allRowsTable) :
                findTableCenterColumns(allRowsTable))
    );
}


/**
 *
 * @param {string} plotId
 * @param {CoverageOptions} options
 * @param {TableData} table
 * @param {TableData} allRowsTable
 * @param {string} drawOp
 * @param {boolean} addVisible - when added it is visible
 */
function addToCoverageDrawing(plotId, options, table, allRowsTable, drawOp, addVisible) {

    if (allRowsTable==='WORKING') return;
    const covType= getCoverageType(options,allRowsTable);
    const {tableMeta, tableData}= allRowsTable;
    const {showCatalogSearchTarget}= getAppOptions();
    const {tbl_id}= table;
    const layersPanelLayoutId= 'catgroup-'+ table.tbl_id;
    const searchTarget= showCatalogSearchTarget ? getSearchTarget(table.request,
                                                   lookupOption(options, 'searchTarget', table.tbl_id),
                                                   lookupOption(options, 'overlayPosition', table.tbl_id)) : undefined;

    const createDrawLayer = (cId, dataType, visible, isFromRegion=false) => {
        const columns= getColumns(dataType,covType,allRowsTable);
        const dl = getDlAry().find((dl) => dl.drawLayerTypeId === Catalog.TYPE_ID && dl.catalogId === cId);

        if (dl || isEmpty(columns)) return;

        dispatchCreateDrawLayer(Catalog.TYPE_ID, {
            catalogId: cId,
            layersPanelLayoutId,
            tblId: tbl_id,
            title: `Coverage: ${table.title || tbl_id}` +
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
            angleInRadian: isUsingRadians(dataType,table,columns),
            dataTooBigForSelection: table.totalRows > 10000,
            tableSelection: (dataType === CoverageType.REGION) ? (drawOp[cId].selectOption || TableSelectOptions.all.key) : null,
        });
        dispatchAttachLayerToPlot(cId, plotId, false, visible);
    };

    if (covType === CoverageType.BOX) {
        createDrawLayer(tbl_id, covType, addVisible);
    } else if (covType === CoverageType.X) {
        createDrawLayer(tbl_id, covType, addVisible);
    } else if (covType === CoverageType.REGION) {
        const layerType = [CoverageType.X, CoverageType.REGION];
        const layerId = [centerId(tbl_id), regionId(tbl_id)];

        layerType.forEach((type, idx) => createDrawLayer(layerId[idx], layerType[idx], addVisible, true));
    }

    if (searchTarget) {
        let newDL = getDrawLayerById(getDlAry(), searchTargetId(tbl_id));
        if (!newDL) {
            newDL = dispatchCreateDrawLayer(SearchTarget.TYPE_ID,
                {
                    color: darker(drawOp[covType === CoverageType.REGION? centerId(tbl_id) : tbl_id].color),
                    drawLayerId: searchTargetId(tbl_id),
                    plotId,
                    searchTargetWP: searchTarget,
                    layersPanelLayoutId,
                    titlePrefix: '',
                    canUserDelete: true,
                });
            dispatchAttachLayerToPlot(newDL.drawLayerId, plotId, false);
        }
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
    const tblData = get(table, 'tableData.data', []);
    const dataCnt= tblData.reduce( (tot, row) =>
        cornerColumns.every( (cDef) => row[cDef.lonIdx]!=='' && row[cDef.latIdx]!=='') ? tot+1 : tot
    ,0);
    return dataCnt > 0;
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
    return get(table, 'tableData.data', [])
        .map( (row) =>
            (row[lonIdx]!=='' && row[latIdx]!=='') ? makePt(row[lonIdx], row[latIdx], csys, usesRadians) : undefined )
        .filter( (v) => v);
}

function getBoxAryFromTable(options,table, usesRadians){
    const cDefAry= getCornersColumns(table);
    return get(table, 'tableData.data', [])
        .map( (row) => cDefAry
            .map( (cDef) =>
                (row[cDef.lonIdx]!=='' && row[cDef.latIdx]!=='') ? makePt(row[cDef.lonIdx], row[cDef.latIdx], cDef.csys, usesRadians) : undefined))
        .filter( (row) => row.every( (v) => v));
}


function getRegionAryFromTable(options, table, usesRadians) {
    const rCol = findTableRegionColumn(table);

    return get(table, 'tableData.data', [])
        .map((row) => {
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

    const table = getTblById(tbl_id);
    if (!table || table.isFetching) return optionHipsSourceURL;
    if (!preparedTable) { // if a new table then the meta takes precedence
        if (table && table.tableMeta[MetaConst.COVERAGE_HIPS]) return table.tableMeta[MetaConst.COVERAGE_HIPS];
    }
    const plot= primePlot(visRoot(), PLOT_ID);
    if (isHiPS(plot)) {
        return plot.hipsUrlRoot;
    }
    if (prevPreferredHipsSourceURL) return prevPreferredHipsSourceURL;
    if (optionHipsSourceURL) return optionHipsSourceURL;
}



function getCommonSearchTarget(tableAry,options) {
    const searchTargetAry= tableAry
        .filter((table) => table && table!=='WORKING')
        .map ( (table) => getSearchTarget(table.request,
                                              lookupOption(options, 'searchTarget', table.tbl_id),
                                              lookupOption(options, 'overlayPosition', table.tbl_id)));
    if (!searchTargetAry.length) return;

    return searchTargetAry.find( (target) => !pointEquals(target,searchTargetAry[0])) ? false : searchTargetAry[0];
}