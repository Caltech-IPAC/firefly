/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import Enum from 'enum';
import {get,isEmpty,flattenDeep,values} from 'lodash';
import {MetaConst} from '../../data/MetaConst.js';
import {TitleOptions} from '../WebPlotRequest.js';
import {CoordinateSys} from '../CoordSys.js';
import {cloneRequest} from '../../tables/TableUtil.js';
import {TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT,
        TABLE_REMOVE, TBL_RESULTS_ACTIVE, TABLE_SORT} from '../../tables/TablesCntlr.js';
import ImagePlotCntlr, {visRoot, dispatchPlotImage, dispatchDeletePlotView} from '../ImagePlotCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {REINIT_RESULT_VIEW} from '../../core/AppDataCntlr.js';
import {doFetchTable, getTblById, getActiveTableId, getColumnIdx, getTableInGroup} from '../../tables/TableUtil.js';
import MultiViewCntlr, {getViewerPlotIds, dispatchAddImages, getMultiViewRoot, getViewer} from '../MultiViewCntlr.js';
// import {serializeDecimateInfo} from '../../tables/Decimate.js'; //todo need to support
import {DrawSymbol} from '../draw/PointDataObj.js';
import {computeCentralPointAndRadius} from '../VisUtil.js';
import {makeWorldPt, pointEquals} from '../Point.js';
import {getCoverageRequest} from './CoverageChooser.js';
import {logError} from '../../util/WebUtil.js';
import DrawLayerCntlr, {dispatchCreateDrawLayer,dispatchDestroyDrawLayer, dispatchAttachLayerToPlot} from '../DrawLayerCntlr.js';
import Catalog from '../../drawingLayers/Catalog.js';
import {getNextColor} from '../draw/DrawingDef.js';

export const CoverageType = new Enum(['X', 'BOX', 'BOTH', 'GUESS']);
export const FitType=  new Enum (['WIDTH', 'WIDTH_HEIGHT']);

const DEF_CORNER_COLS= ['ra1;dec1', 'ra2;dec2', 'ra3;dec3', 'ra4;dec4'];
// const DEF_CENTER_COL= 'ra;dec;EQ_J2000';

const COVERAGE_TARGET = 'COVERAGE_TARGET';
const COVERAGE_RADIUS = 'COVERAGE_RADIUS';
const COVERAGE_TABLE = 'COVERAGE_TABLE';

const PLOT_ID= 'CoveragePlot';

const defOptions= {
    title: 'Coverage',
    tip: 'Coverage',
    getCoverageBaseTitle : (table) => '',   // eslint-disable-line no-unused-vars
    coverageType : CoverageType.BOTH,
    shape : DrawSymbol.SQUARE,
    symbolSize : 5,
    color : 'red',
    highlightedColor : 'blue',
    multiCoverage : true,
    gridOn : false,
    useBlankPlot : false,
    fitType : FitType.WIDTH_HEIGHT,
    canDoCorners : defaultCanDoCorners,
    getQueryCenter,
    hasCoverageData,
    getCornersColumns,
    getCenterColumns,
    getExtraColumns: () => []   // eslint-disable-line no-unused-vars
};

const overlayCoverageDrawing= makeOverlayCoverageDrawing();


/**
 * this saga does the following:
 * <ul>
 *     <li>Then loops:
 *     <ul>
 *         <li>
 *         <li>
 *     </ul>
 * </ul>
 * @param viewerId
 * @param options
 */

export function* watchCoverage({viewerId, options= {}}) {

    var decimatedTables=  {};
    var tbl_id;
    var paused= !get(getViewer(getMultiViewRoot(), viewerId), 'mounted' , false);
    options= Object.assign(defOptions,options);
    var displayedTableId= null;
    var previousDisplayedTableId;
    while (true) {
        previousDisplayedTableId= displayedTableId;
        const action= yield take([TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_REMOVE,
                                  TBL_RESULTS_ACTIVE, REINIT_RESULT_VIEW,
                                  DrawLayerCntlr.ATTACH_LAYER_TO_PLOT,
                                  ImagePlotCntlr.PLOT_IMAGE,
                                  MultiViewCntlr.ADD_VIEWER, MultiViewCntlr.VIEWER_MOUNTED, 
                                  MultiViewCntlr.VIEWER_UNMOUNTED]);
        
        if (!getViewerPlotIds(getMultiViewRoot(),viewerId).includes(PLOT_ID))  {
            dispatchAddImages(viewerId,[PLOT_ID]);
        }

        if (paused && (action.type!==MultiViewCntlr.VIEWER_MOUNTED && action.type!==MultiViewCntlr.ADD_VIEWER) )  {
            continue;
        }

        const {payload}= action;

        if (action.type===TABLE_REMOVE) {
            tbl_id= getActiveTableId();
        }
        else if (payload.tbl_id) {
            tbl_id= payload.tbl_id; // otherwise use the last one
        }


        switch (action.type) {

            case TABLE_LOADED:
                if (!getTableInGroup(tbl_id)) continue;
                if (get(action, 'payload.invokedBy') !== TABLE_SORT) {
                    // no need to update coverage on table sort.. data have not changed.
                    decimatedTables[tbl_id]= null;
                    displayedTableId = updateCoverage(tbl_id, viewerId, decimatedTables, options);
                    break;
                }

            case TBL_RESULTS_ACTIVE:
                if (!getTableInGroup(tbl_id)) continue;
                displayedTableId = updateCoverage(tbl_id, viewerId, decimatedTables, options);
                break;

            case TABLE_REMOVE:
                if (!getTableInGroup(payload.tbl_id)) continue;
                removeCoverage(payload.tbl_id, decimatedTables);
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
                    tbl_id = getActiveTableId();
                    displayedTableId = updateCoverage(tbl_id, viewerId, decimatedTables, options);
                }
                break;

            case MultiViewCntlr.VIEWER_UNMOUNTED:
                if (action.payload.viewerId === viewerId) paused = true;
                break;
            case ImagePlotCntlr.PLOT_IMAGE:
                if (action.payload.plotId===PLOT_ID) overlayCoverageDrawing(decimatedTables,options);
                break;
        }
        if (!displayedTableId) displayedTableId= previousDisplayedTableId;
    }
}



function removeCoverage(tbl_id, decimatedTables) {
    Reflect.deleteProperty(decimatedTables, tbl_id);
    if (!Object.keys(decimatedTables)) {
        dispatchDeletePlotView(PLOT_ID);
    }
}

/**
 * 
 * @param tbl_id
 * @param viewerId
 * @param decimatedTables
 * @param options
 * @return {Array}
 */
function updateCoverage(tbl_id, viewerId, decimatedTables, options) {

    if (!tbl_id) return null;
    const table= getTblById(tbl_id);
    if (!table) return null;
    if (!options.hasCoverageData(options, table)) return null;
    if (decimatedTables[tbl_id]==='WORKING') return tbl_id;


    const params= {
        startIdx : 0,
        pageSize : 1000000,
        inclCols : getCovColumnsForQuery(options, table)
    };


    const req = cloneRequest(table.request, params);
    req.tbl_id = `cov-${tbl_id}`;

    if (decimatedTables[tbl_id] /*&& decimatedTables[tbl_id].tableMeta.tblFilePath===table.tableMeta.tblFilePath*/) { //todo support decimated data
        updateCoverageWithData(table, options, tbl_id, decimatedTables[tbl_id], decimatedTables);
    }
    else {
        decimatedTables[tbl_id]= 'WORKING';
        doFetchTable(req).then(
            (allRowsTable) => {
                if (get(allRowsTable, ['tableData', 'data'],[]).length>0) {
                    decimatedTables[tbl_id]= allRowsTable;
                    updateCoverageWithData(table, options, tbl_id, allRowsTable, decimatedTables);
                }
            }
        ).catch(
            (reason) => {
                decimatedTables[tbl_id]= null;
                logError(`Failed to catalog plot data: ${reason}`, reason);
            }
        );

    }
    return tbl_id;
}



function updateCoverageWithData(table, options, tbl_id, allRowsTable, decimatedTables) {
    const {centralPoint, maxRadius}= computeSize(options, decimatedTables, allRowsTable);

    if (!centralPoint || maxRadius<=0) return;

    const wpRequest= getCoverageRequest(centralPoint,maxRadius,
                                        options.getCoverageBaseTitle(allRowsTable), 
                                        false, options.gridOn);
    wpRequest.setPlotId(PLOT_ID);
    if (options.title) {
        wpRequest.setTitleOptions(TitleOptions.NONE);
        wpRequest.setTitle(options.title);
    }

    const plot= primePlot(visRoot(), PLOT_ID);
    if (plot &&
        pointEquals(centralPoint,plot.attributes[COVERAGE_TARGET]) &&
        plot.attributes[COVERAGE_RADIUS]===centralPoint ) {
        overlayCoverageDrawing(decimatedTables, options);
    }
    else {
        dispatchPlotImage({
            wpRequest,
            attributes:{
                [COVERAGE_TARGET]: centralPoint,
                [COVERAGE_RADIUS]: maxRadius,
                [COVERAGE_TABLE]:  tbl_id
            }}
        );
    }
}

function computeSize(options, decimatedTables,allRowsTable) {
    const ary= options.multiCoverage ? values(decimatedTables) : [allRowsTable];
    var testAry= ary
        .filter( (t) => t && t!=='WORKING')
        .map( (t) => {
            var ptAry= [];
            const covType= getCoverageType(options,t);
            switch (covType) {
                case CoverageType.X:
                    ptAry= getPtAryFromTable(options,t);
                    break;
                case CoverageType.BOX:
                    ptAry= getBoxAryFromTable(options,t);
                    break;

            }
            return flattenDeep(ptAry);
    } );
    testAry= flattenDeep(testAry);
    if (isOnePoint(testAry)) {
        return {centralPoint:testAry[0], maxRadius: .05};
    }
    else {
        return computeCentralPointAndRadius(testAry);
    }
}

function isOnePoint(wpList) {
    if (isEmpty(wpList)) return false;
    return !wpList.some( (wp) => !pointEquals(wp,wpList[0]));
}



function makeOverlayCoverageDrawing() {
    const colors= {};
    /**
     *
     * @param decimatedTables
     * @param options
     */
    return (decimatedTables, options) => {
        const plot=  primePlot(visRoot(),PLOT_ID);
        if (!plot) return;
        const tbl_id=  plot.attributes[COVERAGE_TABLE];
        if (!tbl_id || !decimatedTables[tbl_id] || !getTblById(tbl_id)) return;
        const table= getTblById(tbl_id);
        
        if (table.tableMeta[MetaConst.CATALOG_OVERLAY_TYPE]) return; // let the catalog just handle the drawing overlays

        const allRowsTable= decimatedTables[tbl_id];

        dispatchDestroyDrawLayer(tbl_id);

        const overlayAry=  options.multiCoverage ? Object.keys(decimatedTables) : [allRowsTable.tbl_id];

        overlayAry.forEach( (id) => {
            if (id!==tbl_id) return;
            if (!colors[id]) colors[id]= getNextColor();
            const oriTable= getTblById(id);
            const arTable= decimatedTables[id];
            if (oriTable && arTable) addToCoverageDrawing(PLOT_ID, options, oriTable, arTable, colors[id]);

        });
    };
}

function addToCoverageDrawing(plotId, options, table, allRowsTable, color) {

    if (allRowsTable==='WORKING') return;
    const covType= getCoverageType(options,allRowsTable);

    const boxData= covType===CoverageType.BOTH || covType===CoverageType.BOX;
    const {tableMeta, tableData}= allRowsTable;
    const columns = boxData ? options.getCornersColumns(table) : options.getCenterColumns(table);
    dispatchCreateDrawLayer(Catalog.TYPE_ID, {
        catalogId: table.tbl_id,
        title: `Coverage: ${table.title || table.tbl_id}`,
        color,
        tableData,
        tableMeta,
        tableRequest: allRowsTable.request,
        highlightedRow: table.highlightedRow,
        catalog: false,
        columns,
        boxData
    });
    dispatchAttachLayerToPlot(table.tbl_id, plotId);
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


function getPtAryFromTable(options,table){
    const cDef= options.getCenterColumns(table);
    const {lonIdx,latIdx,csys}= cDef;
    return table.tableData.data.map( (row) => makeWorldPt(row[lonIdx], row[latIdx], csys) );
}

function getBoxAryFromTable(options,table){
    const cDefAry= options.getCornersColumns(table);
    return table.tableData.data
        .map( (row) => cDefAry
            .map( (cDef) => makeWorldPt(row[cDef.lonIdx], row[cDef.latIdx], cDef.csys)) );
}


function hasCoverageData(options, table) {
    if (!get(table, 'totalRows')) return false;
    if (!options.multiCoverage && table.tableMeta[MetaConst.CATALOG_OVERLAY_TYPE]) return false;
    return !isEmpty(options.getCenterColumns(table));
}



function defaultCanDoCorners(table) {// eslint-disable-line no-unused-vars
    return true;
}

function getCovColumnsForQuery(options, table) {
    const cAry= [...options.getCornersColumns(table), options.getCenterColumns(table)];
    return cAry.reduce( (s,c,idx)=> s+`${idx>0?',':''}${c.lonCol},${c.latCol}`,'');
}

function getCornersColumns(table) {
    if (!table) return [];
    const {tableMeta}= table;
    if (!tableMeta) return [];
    if (tableMeta[MetaConst.ALL_CORNERS]) {
        return makeCoordColAry(tableMeta[MetaConst.ALL_CORNERS].split(','),table);
    }
    return makeCoordColAry(DEF_CORNER_COLS,table);
}

function getCenterColumns(table) {
    if (!table) return [];
    const {tableMeta:meta}= table;
    if (!meta) return [];

    if (meta[MetaConst.CENTER_COLUMN]) return makeCoordCol(meta[MetaConst.CENTER_COLUMN],table);
    if (meta[MetaConst.CATALOG_COORD_COLS]) return makeCoordCol(meta[MetaConst.CATALOG_COORD_COLS],table);
    const defCol= guessDefColumns(table);

    return makeCoordCol(defCol,table);
}



function guessDefColumns(table) {
    const DEF_CENTER_COL= 'ra;dec;EQ_J2000';
    const {columns}= table.tableData;
    const colList= columns.map( (c) => c.name.toLowerCase());
    if (colList.includes('ra') && colList.includes('dec')) return 'ra;dec;EQ_J2000';
    if (colList.includes('lon') && colList.includes('lat')) return 'lon;lat;EQ_J2000';
    // if (colList.includes('crval1') && colList.includes('crval2')) return 'crval1;crval2;EQ_J2000';
    return 'ra;dec;EQ_J2000';
}

function getQueryCenter(table) { // eslint-disable-line no-unused-vars  //todo not supported yet,
}

const makeCoordColAry= (cAry, table) => cAry.map( (c) => makeCoordCol(c,table)).filter( (cCol) => cCol);

function makeCoordCol(def, table) {
    const s = def.split(';');
    if (s.length!== 3 && s.length!==2) return null;
    const s0Idx= getColumnIdx(table,s[0]);
    const s1Idx= getColumnIdx(table,s[1]);
    if (s0Idx===-1 || s0Idx=== -1) return null;
    return {
        lonCol: s[0],
        latCol: s[1],
        lonIdx: s0Idx,
        latIdx: s1Idx,
        csys : s[2] ? CoordinateSys.parse(s[2]) : CoordinateSys.EQ_J2000
    };
    
}
