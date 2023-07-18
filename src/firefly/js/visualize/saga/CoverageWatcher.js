/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {flattenDeep, isEmpty, isNil, isObject, isString, isUndefined, pick, values} from 'lodash';
import {getAppOptions} from '../../core/AppDataCntlr';
import {REINIT_APP} from '../../core/AppDataCntlr.js';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga';
import {MetaConst} from '../../data/MetaConst.js';
import Catalog, {CatalogType} from '../../drawingLayers/Catalog.js';
import {TableSelectOptions} from '../../drawingLayers/CatalogUI.jsx';
import SearchTarget from '../../drawingLayers/SearchTarget.js';
import {serializeDecimateInfo} from '../../tables/Decimate.js';
import {getCornersColumns} from '../../tables/TableInfoUtil.js';
import {cloneRequest, makeTableFunctionRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {
    TABLE_HIGHLIGHT, TABLE_LOADED, TABLE_REMOVE, TABLE_SELECT, TABLE_UPDATE, TBL_RESULTS_ACTIVE
} from '../../tables/TablesCntlr.js';
import {
    doFetchTable, getActiveTableId, getBooleanMetaEntry, getMetaEntry, getTableInGroup, getTblById, isTableUsingRadians
} from '../../tables/TableUtil.js';
import {darker} from '../../util/Color';
import {logger} from '../../util/Logger.js';
import {parseObsCoreRegion} from '../../util/ObsCoreSRegionParser.js';
import {isOrbitalPathTable} from '../../util/VOAnalyzer';
import {
    findTableCenterColumns, findTableRegionColumn, hasCoverageData, isCatalog, isTableWithRegion
} from '../../util/VOAnalyzer.js';
import {getNextColor} from '../draw/DrawingDef.js';
import {DrawSymbol} from '../draw/DrawSymbol.js';
import {
    dispatchAttachLayerToPlot, dispatchCreateDrawLayer, dispatchDestroyDrawLayer, dispatchModifyCustomField, getDlAry
} from '../DrawLayerCntlr.js';
import ImagePlotCntlr, {
    dispatchDeletePlotView, dispatchPlotHiPS, dispatchPlotImageOrHiPS, visRoot
} from '../ImagePlotCntlr.js';
import MultiViewCntlr, {getMultiViewRoot, getViewer} from '../MultiViewCntlr.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {DEFAULT_COVERAGE_PLOT_ID, isDrawLayerVisible} from '../PlotViewUtil';
import {getDrawLayerById, primePlot} from '../PlotViewUtil.js';
import {makeWorldPt, pointEquals} from '../Point.js';
import {computeCentralPtRadiusAverage} from '../VisUtil.js';
import {BLANK_HIPS_URL, isBlankHiPSURL, isHiPS} from '../WebPlot';
import {WebPlotRequest} from '../WebPlotRequest.js';
import {getSearchTarget} from './CatalogWatcher';
import {memorizeLastCall} from 'firefly/util/WebUtil';

/**
 * @typedef {Object} CoverageType
 * enum can be one of
 * @prop X
 * @prop BOX
 * @prop REGION
 * @prop ORBITAL_PATH
 * @prop ALL
 * @prop GUESS
 * @type {Enum}
 */
const CoverageType = new Enum(['X', 'BOX', 'REGION', 'ORBITAL_PATH', 'ALL', 'GUESS']);
const FitType=  new Enum (['WIDTH', 'WIDTH_HEIGHT']);

const COVERAGE_TARGET = 'COVERAGE_TARGET';
const COVERAGE_FOV = 'COVERAGE_FOV';


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
 * @prop {boolean} exclusiveHiPS - only show hips for coverage, never fits
 */


const defOptions= {
    title: '2MASS K_s',
    tip: 'Coverage',
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

    exclusiveHiPS: false,
    hipsSourceURL : 'ivo://CDS/P/2MASS/color', // url
    hipsSource360URL : 'ivo://ov-gso/P/DIRBE/ZSMA1', // url
    imageSourceParams: {
        Service : 'TWOMASS',
        SurveyKey: 'asky',
        SurveyKeyBand: 'k',
        title : '2MASS K_s'
    },

    fovDegFallOver: .08,
    fovMaxFitsSize: .2,
    autoConvertOnZoom: false,
    fovDegMinSize: .1,
    viewerId:'DefCoverageId',
    paused: true
};

export const COVERAGE_WATCH_CID= 'COVERAGE_WATCH_CID';
export const COVERAGE_FAIL= 'fail';

const baseAttributePickList= [ PlotAttribute.USER_SEARCH_WP, PlotAttribute.USER_SEARCH_RADIUS_DEG,
    PlotAttribute.POLYGON_ARY, PlotAttribute.USE_POLYGON, ];
const selAttributePickList= [
    PlotAttribute.SELECTION,
    PlotAttribute.SELECTION_SOURCE,
    PlotAttribute.SELECTION_TYPE,
    PlotAttribute.IMAGE_BOUNDS_SELECTION,
];

const overlayCoverageDrawing= makeOverlayCoverageDrawing();


export function startCoverageWatcher(options) {
    dispatchAddTableTypeWatcherDef( { ...getCoverageWatcherDef(), options });
}


/** @type {TableWatcherDef} */
export const getCoverageWatcherDef = () => ({
    id : 'CoverageWatcher',
    testTable : (table) => hasCoverageData(table),
    sharedData: { preparedTables: {}, tblCatIdMap: {}},
    watcher : watchCoverage,
    allowMultiples: false,
    actions: [TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_REMOVE, TBL_RESULTS_ACTIVE,
        ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_HIPS,
        MultiViewCntlr.ADD_VIEWER, MultiViewCntlr.VIEWER_MOUNTED, MultiViewCntlr.VIEWER_UNMOUNTED]
});

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

    const setPreferredHipsOnSharedData= () => {
        const pref= findPreferredHiPS(tbl_id, sharedData.preferredHipsSourceURL,
            options.hipsSourceURL, options.hipsSource360URL, preparedTables[tbl_id]);
        sharedData.preferredHipsSourceURL= pref.preferredHipsSourceURL;
        sharedData.preferredHipsSource360URL= pref.preferredHipsSource360URL;
    };

    if (paused) {
        paused= !(getViewer(getMultiViewRoot(), viewerId)?.mounted ?? false);
    }
    if (!action) {
        if (!paused) {
            preparedTables[tbl_id]= undefined;
            setPreferredHipsOnSharedData();
            updateCoverage(tbl_id, viewerId, sharedData.preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL, sharedData.preferredHipsSource360URL);
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
        setPreferredHipsOnSharedData();
        updateCoverage(tbl_id, viewerId, preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL, sharedData.preferredHipsSource360URL);
        return {paused:false};
    }

    if (action.type===TABLE_LOADED) preparedTables[tbl_id]= undefined;
    if (paused) return {paused};

    switch (action.type) {

        case TABLE_LOADED:
            if (!getTableInGroup(tbl_id)) return {paused};
            setPreferredHipsOnSharedData();
            updateCoverage(tbl_id, viewerId, preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL, sharedData.preferredHipsSource360URL);
            break;

        // case TBL_RESULTS_ACTIVE:
        //     if (!getTableInGroup(tbl_id)) return {paused};
        //     sharedData.preferredHipsSourceURL= findPreferredHiPS(tbl_id, sharedData.preferredHipsSourceURL, options.hipsSourceURL, preparedTables[tbl_id]);
        //     updateCoverage(tbl_id, viewerId, preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL);
        //     break;

        case TABLE_REMOVE:
            removeCoverage(payload.tbl_id, preparedTables);
            if (!isEmpty(preparedTables)) {
                setPreferredHipsOnSharedData();
                updateCoverage(getActiveTableId(), viewerId, preparedTables, options, tblCatIdMap, sharedData.preferredHipsSourceURL, sharedData.preferredHipsSource360URL, payload.tbl_id);
                removeTableComponentStateStatus(payload.tbl_id);
            }
            cancelSelf();
            break;

        case TABLE_SELECT:
            tblCatIdMap[tbl_id]?.forEach(( cId ) => {
                dispatchModifyCustomField(cId, {selectInfo: action.payload.selectInfo});
            });
            break;

        case TABLE_HIGHLIGHT:
        case TABLE_UPDATE:
            tblCatIdMap[tbl_id]?.forEach(( cId ) => {
                dispatchModifyCustomField(cId, {highlightedRow: action.payload.highlightedRow});
            });
            break;

        case MultiViewCntlr.VIEWER_UNMOUNTED:
            paused = true;
            break;

        case ImagePlotCntlr.PLOT_IMAGE:
        case ImagePlotCntlr.PLOT_HIPS:
            if (action.payload.plotId===DEFAULT_COVERAGE_PLOT_ID) {
                setTimeout( () => overlayCoverageDrawing(preparedTables,options, tblCatIdMap, undefined), 5);
            }
            break;
            
    }
    return {paused};
}



function removeCoverage(tbl_id, preparedTables) {
    if (tbl_id) Reflect.deleteProperty(preparedTables, tbl_id);
    if (isEmpty(Object.keys(preparedTables))) {
        dispatchDeletePlotView({plotId:DEFAULT_COVERAGE_PLOT_ID});
    }
}

/**
 * @param {string} tbl_id
 * @param {string} viewerId
 * @param preparedTables
 * @param {CoverageOptions} options
 * @param {object} tblCatIdMap
 * @param {string} preferredHipsSourceURL
 * @param {string} preferredHipsSource360URL
 * @param {string} deletedTblId
 */
function updateCoverage(tbl_id, viewerId, preparedTables, options, tblCatIdMap, preferredHipsSourceURL, preferredHipsSource360URL, deletedTblId= undefined) {

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
                {
                    decimate: !isOrbitalPathTable(tbl_id) ? serializeDecimateInfo(cenCol.lonCol, cenCol.latCol, 10000) : undefined,
                    pageSize: MAX_ROW
                });
        }

        req.tbl_id = `cov-${tbl_id}`;

        if (preparedTables[tbl_id]) { //todo support decimated data
            updateCoverageWithData(viewerId, table, options, tbl_id, preparedTables[tbl_id], preparedTables,
                isTableUsingRadians(table), tblCatIdMap, preferredHipsSourceURL, preferredHipsSource360URL, deletedTblId );
        }
        else {
            preparedTables[tbl_id] = 'WORKING';
            if (isOrbitalPathTable(tbl_id)) req.sortInfo= undefined;
            doFetchTable(req).then(
                (allRowsTable) => {
                    if (allRowsTable?.tableData?.data?.length > 0) {
                        preparedTables[tbl_id] = allRowsTable;
                        const isRegion = isTableWithRegion(allRowsTable);
                        const regionAry = getRegionAryFromTable(table);
                        tblCatIdMap[tbl_id] = (isRegion && regionAry.length > 0) ? [centerId(tbl_id), regionId(tbl_id)] : [tbl_id, searchTargetId(tbl_id)];
                        updateCoverageWithData(viewerId, table, options, tbl_id, allRowsTable, preparedTables,
                            isTableUsingRadians(table), tblCatIdMap, preferredHipsSourceURL, preferredHipsSource360URL);
                    }
                }
            ).catch(
                (reason) => {
                    preparedTables[tbl_id] = undefined;
                    tblCatIdMap[tbl_id] = undefined;
                    logger.error(`Failed to catalog plot data: ${reason}`, reason);
                }
            );

        }
    } catch (e) {
        logger.error('Error updating coverage', e);
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
 * @param {string} preferredHipsSource360URL
 * @param {string} deletedTblId - only defined if a table has been deleted
 */
function updateCoverageWithData(viewerId, table, options, tbl_id, allRowsTable, preparedTables,
                                usesRadians, tblCatIdMap, preferredHipsSourceURL, preferredHipsSource360URL, deletedTblId ) {



    const {fovSize:retFovSize, avgOfCenters}= computeSize(options, preparedTables, usesRadians);
    const fovSize= retFovSize*1.1;

    if (!avgOfCenters || fovSize<=0) {
        updateTableComponentStateStatus(tbl_id,COVERAGE_FAIL);
        return;
    }

    overlayCoverageDrawing(preparedTables, options, tblCatIdMap, deletedTblId||table.tbl_id);

    if (deletedTblId) return;
    const blankHips= isBlankHiPSURL(preferredHipsSourceURL);

    const commonSearchTarget= getCommonSearchTarget(Object.values(preparedTables),options);

    const {fovDegFallOver, fovMaxFitsSize, autoConvertOnZoom, imageSourceParams, overlayPosition= avgOfCenters}= options;

    let plotAllSkyFirst= false;
    let hipsUrl= preferredHipsSourceURL;
    if (fovSize>110 && !blankHips) {
        plotAllSkyFirst= true;
        hipsUrl= preferredHipsSource360URL;
    }
    let imageRequest= WebPlotRequest.makeFromObj(imageSourceParams) ||
                            WebPlotRequest.make2MASSRequest(avgOfCenters, 'asky', 'k', fovSize);
    imageRequest= initRequest(imageRequest, viewerId, DEFAULT_COVERAGE_PLOT_ID, overlayPosition, avgOfCenters);

    const hipsRequest= initRequest(WebPlotRequest.makeHiPSRequest(hipsUrl, null),
                       viewerId, DEFAULT_COVERAGE_PLOT_ID, overlayPosition, avgOfCenters);
    hipsRequest.setSizeInDeg(fovSize>180?300:fovSize);
    if (options.gridOn) {
        imageRequest.setGridOn(options.gridOn);
        hipsRequest.setGridOn(options.gridOn);
    }

    const tblIdAry= Object.keys(preparedTables).filter( (v) => !isString(preparedTables[v]));
    const plot= primePlot(visRoot(), DEFAULT_COVERAGE_PLOT_ID);
    const oldAtt= plot?.attributes ?? {};

    const pickList= (isHiPS(plot)) ? [...baseAttributePickList, ...selAttributePickList] : [...baseAttributePickList];
    const attributes= {
        [COVERAGE_TARGET]: avgOfCenters,
        [COVERAGE_FOV]: fovSize,
        [PlotAttribute.VISUALIZED_TABLE_IDS]: tblIdAry,
        [PlotAttribute.REPLOT_WITH_NEW_CENTER]: true,
        ...pick(oldAtt,pickList)
    };
    if (commonSearchTarget) attributes[PlotAttribute.CENTER_ON_FIXED_TARGET]= commonSearchTarget;

    if (blankHips) {
        dispatchPlotHiPS({plotId: DEFAULT_COVERAGE_PLOT_ID, viewerId, wpRequest:hipsRequest, attributes,
            pvOptions: {userCanDeletePlots:false, displayFixedTarget:false, useForCoverage:true}
        });
    }
    else if (options.exclusiveHiPS) {
        dispatchPlotHiPS({
            plotId: DEFAULT_COVERAGE_PLOT_ID, viewerId, wpRequest:hipsRequest, attributes,
            pvOptions: {userCanDeletePlots:false, displayFixedTarget:false, useForCoverage:true},
        });

    }
    else {
        dispatchPlotImageOrHiPS({
            plotId: DEFAULT_COVERAGE_PLOT_ID, viewerId, hipsRequest, imageRequest,
            fovDegFallOver, fovMaxFitsSize, autoConvertOnZoom, plotAllSkyFirst,
            pvOptions: {userCanDeletePlots:false, displayFixedTarget:false, useForCoverage:true},
            attributes,
        });

    }
    updateTableComponentStateStatus(tbl_id,'success');
}



function updateTableComponentStateStatus(tbl_id, status) {
    const covState= getComponentState(COVERAGE_WATCH_CID, []);
    const tblEntry= covState.find( (entry) => (entry.tbl_id===tbl_id));
    let newCovState;
    if (tblEntry) {
        newCovState=  covState.map( (entry) => (entry.tbl_id===tbl_id) ? {tbl_id,status} : entry);
    }
    else {
        newCovState= [...covState,{tbl_id,status}];
    }
    dispatchComponentStateChange(COVERAGE_WATCH_CID, newCovState);
}

function removeTableComponentStateStatus(tbl_id) {
    const covState= getComponentState(COVERAGE_WATCH_CID,[]);
    dispatchComponentStateChange(COVERAGE_WATCH_CID, covState.filter( (entry) => entry.tbl_id!==tbl_id));
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
                case CoverageType.ORBITAL_PATH:
                    ptAry= getPtAryFromTable(options,t, usesRadians);
                    break;
                case CoverageType.BOX:
                    ptAry= getBoxAryFromTable(options,t, usesRadians);
                    break;
                case CoverageType.REGION:
                    ptAry = getRegionAryFromTable(t);
                    break;

            }
            return flattenDeep(ptAry);
    } );
    return computeCentralPtRadiusAverage(testAry, options.fovDegMinSize);
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
        const plot=  primePlot(visRoot(),DEFAULT_COVERAGE_PLOT_ID);
        if (!plot && !affectedTblId) return;
        const tblIdAry=  plot ? plot.attributes[PlotAttribute.VISUALIZED_TABLE_IDS] : [affectedTblId];
        if (isEmpty(tblIdAry)) return;
        const visibleMap= tblCatIdMap[affectedTblId]?.reduce( (obj, catId) => {
            obj[catId]= true;
            return obj;
        }, {} ) ?? {};

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
                const searchTargetDL= table && getDrawLayerById(getDlAry(), searchTargetId(table.tbl_id));
                searchTargetDL && dispatchDestroyDrawLayer(searchTargetDL.drawLayerId);

                tblCatIdMap[tbl_id].forEach((cId) => {
                    const layer = getDrawLayerById(getDlAry(), cId);
                    const tableRemoved= !Boolean(getTblById(tbl_id));
                    if (layer && (tableRemoved || tbl_id===affectedTblId)) {
                        drawingOptions[cId] = layer.drawingDef;    // drawingDef and selectOption is stored as layer based
                        selectOps[cId] = layer.selectOption;
                        visibleMap[cId]= plot ? isDrawLayerVisible(layer, plot.plotId) : true;
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
                if (oriTable && arTable) addToCoverageDrawing(DEFAULT_COVERAGE_PLOT_ID, options, oriTable, arTable, drawingOptions, visibleMap);

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
                findTableCenterColumns(allRowsTable, true))
    );
}

const getCatalogType= (dataType) => {
    switch (dataType) {
        case CoverageType.REGION: return CatalogType.REGION;
        case CoverageType.X: return CatalogType.POINT;
        case CoverageType.ORBITAL_PATH: return CatalogType.ORBITAL_PATH;
        case CoverageType.BOX: return CatalogType.BOX;
        default: return CatalogType.POINT;
    }
};

/**
 *
 * @param {string} plotId
 * @param {CoverageOptions} options
 * @param {TableData} table
 * @param {TableData|String} allRowsTable
 * @param {string} drawOp
 * @param visibleMap - when added it is visible
 */
function addToCoverageDrawing(plotId, options, table, allRowsTable, drawOp, visibleMap) {

    if (allRowsTable==='WORKING') return;
    const covType= getCoverageType(options,allRowsTable);
    const {tableMeta, tableData}= allRowsTable;
    const {showCatalogSearchTarget}= getAppOptions();
    const {tbl_id}= table;
    const layersPanelLayoutId= 'catgroup-'+ table.tbl_id;
    const searchTarget= showCatalogSearchTarget ? getSearchTarget(undefined, table,
                                                   lookupOption(options, 'searchTarget', table.tbl_id),
                                                   lookupOption(options, 'overlayPosition', table.tbl_id)) : undefined;


    const createDrawLayer = (cId, dataType, visible, titleAddition='') => {
        const columns= getColumns(dataType,covType,allRowsTable);
        const dl = getDlAry().find((dl) => dl.drawLayerTypeId === Catalog.TYPE_ID && dl.catalogId === cId);

        if (dl || isEmpty(columns)) return;

        dispatchCreateDrawLayer(Catalog.TYPE_ID, {
            catalogId: cId,
            layersPanelLayoutId,
            tblId: tbl_id,
            title: `Coverage: ${table.title || tbl_id}${titleAddition}`,
            color:  drawOp[cId].color,
            tableData,
            tableMeta,
            tableRequest: table.request,
            highlightedRow: table.highlightedRow,
            catalogType: getCatalogType(dataType),
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
        createDrawLayer(tbl_id, covType, visibleMap[tbl_id] ?? true);
    } else if (covType === CoverageType.X) {
        createDrawLayer(tbl_id, covType, visibleMap[tbl_id] ?? true);
    } else if (covType === CoverageType.ORBITAL_PATH) {
        createDrawLayer(tbl_id, covType, visibleMap[tbl_id] ?? true);
    } else if (covType === CoverageType.REGION) {
        const layerType = [CoverageType.X, CoverageType.REGION];
        const layerId = [centerId(tbl_id), regionId(tbl_id)];
        const titleAddition=[' positions', ' regions'];
        const visible= layerId.map( (lId) => visibleMap[lId] ?? true);

        layerType.forEach((type, idx) => createDrawLayer(layerId[idx], layerType[idx], visible[idx], titleAddition[idx]));
    }

    if (searchTarget) {
        let newDL = getDrawLayerById(getDlAry(), searchTargetId(tbl_id));
        if (!newDL) {
            newDL = dispatchCreateDrawLayer(SearchTarget.TYPE_ID,
                {
                    color: darker(drawOp[covType === CoverageType.REGION? centerId(tbl_id) : tbl_id].color),
                    drawLayerId: searchTargetId(tbl_id),
                    plotId,
                    searchTargetPoint: searchTarget,
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

    if (isOrbitalPathTable(table)) return CoverageType.ORBITAL_PATH;
    const x = hasCorners(options, table);

    if (options.coverageType===CoverageType.GUESS ||
        options.coverageType===CoverageType.REGION ||
        options.coverageType===CoverageType.BOX ||
        options.coverageType===CoverageType.ALL) {
        const regionAry = getRegionAryFromTable(table);
        if (isTableWithRegion(table) && regionAry.length === 0) {
            //helpful note for developer/user in the console
            console.log(`Note: We could not parse s_region correctly, possibly because we do not support this format yet, 
            hence it is not displayed in the coverage map.`);
        }
        return  (isTableWithRegion(table) && regionAry.length>0) ? CoverageType.REGION :
                                    (hasCorners(options,table) ? CoverageType.BOX : CoverageType.X);
    }
    return options.coverageType;
}

const isValidNum= (n) => !isNil(n) && !isNaN(Number(n));

function hasCorners(options, table) {
    const cornerColumns= getCornersColumns(table);
    if (isEmpty(cornerColumns)) return false;
    const tblData = table?.tableData?.data ?? [];
    const dataCnt= tblData.reduce( (tot, row) =>
        cornerColumns.every( (cDef) => isValidNum(row[cDef.lonIdx]) && isValidNum(row[cDef.lonIdx]) ) ? tot+1 : tot
    ,0);
    return dataCnt > 0;
}

function getPtAryFromTable(options,table, usesRadians){
    const cDef= findTableCenterColumns(table, true);
    if (isEmpty(cDef)) return [];
    const {lonIdx,latIdx,csys}= cDef;
    return (table?.tableData?.data ?? [])
        .map( (row) =>
            lonIdx!==latIdx ?
                makeWorldPt(row[lonIdx], row[latIdx], csys, true, usesRadians) :
                makeWorldPt(row[lonIdx][0], row[latIdx][1], csys, true, usesRadians)
            
        ).filter( (v) => v);
}

function getBoxAryFromTable(options,table, usesRadians){
    const cDefAry= getCornersColumns(table);
    return (table?.tableData?.data ?? [])
        .map( (row) => cDefAry
            .map( (cDef) => makeWorldPt(row[cDef.lonIdx], row[cDef.latIdx], cDef.csys, true, usesRadians) ))
        .filter( (row) => row.every( (v) => v));
}

const getRegionAryFromTable = memorizeLastCall( (table) => {
    const rCol = findTableRegionColumn(table);
    if (!rCol) return [];
    return (table?.tableData?.data ?? [])
        .map((row) => {
             const cornerInfo = parseObsCoreRegion(row[rCol.regionIdx], rCol.unit, true);

            return cornerInfo.valid ? cornerInfo.corners : [];
        }).filter((r) => !isEmpty(r));
});

function getCovColumnsForQuery(options, table) {
    const cAry= [...getCornersColumns(table), findTableCenterColumns(table,true), findTableRegionColumn(table)];
    // column names should be in quotes
    // there should be no duplicates
    const base = cAry.filter((c)=>!isEmpty(c))
            .map( (c)=> (c.type === 'region') ?
                `"${c.regionCol}"` : c.lonCol!==c.latCol ?
                    `"${c.lonCol}","${c.latCol}"` : `"${c.lonCol}"`)
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
 * @param optionHipsSource360URL
 * @param preparedTable
 * @return {*}
 */
function findPreferredHiPS(tbl_id,prevPreferredHipsSourceURL, optionHipsSourceURL, optionHipsSource360URL, preparedTable) {

    const makeRet= (hNormal,h360) => ({preferredHipsSourceURL:hNormal, preferredHipsSource360URL:h360});

    const table = getTblById(tbl_id);
    if (!table || table.isFetching) return makeRet(optionHipsSourceURL,optionHipsSource360URL);
    if (!preparedTable) { // if a new table then the meta takes precedence
        const sim= getBooleanMetaEntry(table, MetaConst.SIMULATED_TABLE, false);
        const covHips= getMetaEntry(table,MetaConst.COVERAGE_HIPS);
        if (covHips) return makeRet(covHips,covHips);
        if (sim) return makeRet(BLANK_HIPS_URL, BLANK_HIPS_URL);
    }
    const plot= primePlot(visRoot(), DEFAULT_COVERAGE_PLOT_ID);
    if (isHiPS(plot)) {
        return makeRet(plot.hipsUrlRoot, plot.hipsUrlRoot);
    }
    if (prevPreferredHipsSourceURL) return makeRet(prevPreferredHipsSourceURL, prevPreferredHipsSourceURL);
    if (optionHipsSourceURL) return makeRet(optionHipsSourceURL, optionHipsSource360URL);
}

// function findPreferred360HiPS(tbl_id,prevPreferredHipsSourceURL, optionHipsSourceURL, optionHipsSource360URL, preparedTable) {
//     const normalUrl= findPreferredHiPS(tbl_id,prevPreferredHipsSourceURL, optionHipsSourceURL, preparedTable);
//     const table = getTblById(tbl_id);
//     if (!preparedTable) { // if a new table then the meta takes precedence
//         const sim= getBooleanMetaEntry(table, MetaConst.SIMULATED_TABLE, false);
//         const covHips= getMetaEntry(table,MetaConst.COVERAGE_HIPS);
//         if (covHips) return covHips;
//         if (sim) return BLANK_HIPS_URL;
//     }
//     const plot= primePlot(visRoot(), PLOT_ID);
//     if (isHiPS(plot)) {
//         return plot.hipsUrlRoot;
//     }
//     return optionHipsSource360URL ?? optionHipsSourceURL;
//
// }
//

function getCommonSearchTarget(tableAry,options) {
    const searchTargetAry= tableAry
        .filter((table) => table && table!=='WORKING')
        .map ( (table) => getSearchTarget(undefined, table,
                                              lookupOption(options, 'searchTarget', table.tbl_id),
                                              lookupOption(options, 'overlayPosition', table.tbl_id)));
    if (!searchTargetAry.length) return;

    return searchTargetAry.find( (target) => !pointEquals(target,searchTargetAry[0])) ? false : searchTargetAry[0];
}