import {uniq} from 'lodash';
import {dispatchAddTaskCount, dispatchRemoveTaskCount} from '../core/AppDataCntlr';
import {dispatchComponentStateChange} from '../core/ComponentCntlr';
import {dispatchAddActionWatcher} from '../core/MasterSaga';
import {flux} from '../core/ReduxFlux';
import {ang2pixNest, radecToPolar} from '../externalSource/aladinProj/HealpixIndex';
import {logger} from '../util/Logger';
import {blockWhileAsyncIdWaiting, synchronizeAsyncFunctionById} from '../util/SynchronizeAsync';
import {createBackgroundRunner, isDefined} from '../util/WebUtil';
import {DEFAULT_COVERAGE_PLOT_ID} from '../visualize/PlotViewUtil';
import {makeWorldPt} from '../visualize/Point';
import {convertCelestial} from '../visualize/VisUtil';
import {findTableCenterColumns, makeWorldPtUsingCenterColumns} from '../voAnalyzer/TableAnalysis';
import {getTableModel} from '../voAnalyzer/VoCoreUtils';
import {cloneRequest, MAX_ROW} from './TableRequestUtil';
import {TABLE_LOADED, TABLE_REMOVE, TABLE_SELECT} from './TablesCntlr';
import {
    doFetchTable, fetchSpacialBinary, getColumnIdx, getSelectedData, getTblById, isTableUsingRadians
} from './TableUtil';

export const DATA_NORDER= 12;
export const MIN_NORDER= 4;
export const MIN_NORDER_TO_ALWAYS_GROUP= 4;
export const MIN_NORDER_FOR_COVERAGE= 2;
export const MIN_ROWS_FOR_HIERARCHICAL= 1000;
export const MIN_TOTAL_ROWS_FOR_PARTIAL= 50000;
const DATA_NSIDE= 2**DATA_NORDER;
const UINT_SCALE= 10**7;
const MAX_MAP= 16_000_000;
export const COVERAGE_WAITING_MSG= 'COVERAGE_WAITING_MSG';
export const HPX_WORKING_KEY= 'HPX_WORKING';

const maxTiles= {
    9: 3_145_728,
    10: 12_582_912,
    11: 50_331_648,
    12: 201_326_592,
    13: 805_306_368,
};

/**
 * @typedef {Object} TableHpxData
 *
 * @summary TableHpxData
 *
 * @prop {String} tbl_id
 * @prop {boolean} ready
 * @prop {boolean} partialIndexData
 * @prop {number} maxInitialLoadNorder
 * @prop {Float32Array} lonAry
 * @prop {Float32Array} latAry
 * @prop {CoordinateSys} csys
 * @prop {boolean} tableUsingRadians
 * @prop {Object.<number, HealpixLevel>} orderData - object {number, HealpixLevel}
 * @prop {Object} selectionOrderData - object {number, HealpixLevel}
 */


/**
 * @typedef {Object} HealpixLevel
 *
 * @summary Data for Healpix data
 *
 * @prop {Number} norder
 * @prop {Object} histogramInfo
 * @prop  {Array.<Map.<Number,TileEntry>>} tiles at this level, which index in array is determined by Math.trunc(tileNumber/MAX_MAP)
 */


/**
 * @typedef {Object} TileEntry
 *
 * @summary pixel tiles
 *
 * @prop {Number} pixel
 * @prop {boolean} summaryTile - true for all levels except bottom
 * @prop {boolean} indexesLoaded
 * @prop {Number} count
 * @prop {Array.<Number>} tableIndexes - indexes of the table, used with full indexing
 * @prop {Map} precomputedTileWP - map of table indexes to world point, used with partial indexing
 */


export const SPACIAL_HPX_INDX_PREFIX= 'HpxIndexCntlr';

export const ADD_TABLE_IDX= `${SPACIAL_HPX_INDX_PREFIX}.AddTableIdx`;
export const CLEAR_IDX= `${SPACIAL_HPX_INDX_PREFIX}.ClearIdx`;
export const INDEX_REMOVE= `${SPACIAL_HPX_INDX_PREFIX}.IndexRemove`;
export const ENABLE_HPX_INDEX= `${SPACIAL_HPX_INDX_PREFIX}.EnableHpxIndex`;
export const ADD_SELECTION_HPX_INDEX= `${SPACIAL_HPX_INDX_PREFIX}.AddSelectionHpxIndex`;
export const ORDER_DATA_READY= `${SPACIAL_HPX_INDX_PREFIX}.OrderDataReady`;
export const PARTIAL_INDEX_DATA= `${SPACIAL_HPX_INDX_PREFIX}.partialIndexData`;

export function idxRoot() { return flux.getState()[SPACIAL_HPX_INDX_PREFIX]; }


const reducers= () => ({ [SPACIAL_HPX_INDX_PREFIX]: reducer, });

export function dispatchEnableHpxIndex({tbl_id}) {
    flux.process({type: ENABLE_HPX_INDEX, payload: {tbl_id} });
}

function dispatchAddPartialData({tbl_id,idxTable,raColName,csys,decColName}) {
    flux.process({type: PARTIAL_INDEX_DATA, payload: {tbl_id,idxTable,raColName,csys,decColName} });
}


function actionCreators() {
    return {
        [ENABLE_HPX_INDEX]: makeTableOrderDataAction,
    };
}

export default { reducers, actionCreators, };

function reducer(state={}, action={}) {
    const {type,payload} = action;
    const {ready=true,csys,orderData,lonAry,latAry,selectionOrderData,maxInitialLoadNorder,
        selectAll,tableUsingRadians=false,tbl_id, partialIndexData}= payload ?? {};


    switch (type) {
        case ADD_TABLE_IDX:
            return (!state[tbl_id]) ? {...state,[tbl_id]: {ready:false} } : state;

        case ENABLE_HPX_INDEX:
            return {...state,[tbl_id]:
                    {ready,orderData,lonAry,latAry, csys, selectionOrderData,
                        selectAll, tableUsingRadians, partialIndexData, maxInitialLoadNorder} };

        case ADD_SELECTION_HPX_INDEX:
            const tblHpxData= {...state[tbl_id],selectionOrderData,selectAll,ready};
            return {...state,[tbl_id]: tblHpxData };

        case CLEAR_IDX:
            const newState= {...state};
            delete newState[tbl_id];
            return newState;

        case ORDER_DATA_READY:
            return {...state,[tbl_id]: {...state[tbl_id], ready} };

        case INDEX_REMOVE:
        case TABLE_REMOVE:
            const retState= {...state};
            delete retState[tbl_id];
            return retState;
        case PARTIAL_INDEX_DATA:
            return ingestPartialIndexData(state,action);
    }
    return state;
}

function ingestPartialIndexData(state, action) {
    const {tbl_id,idxTable,raColName,csys,decColName}= action.payload;

    const raIdx= getColumnIdx(idxTable,raColName);
    const decIdx= getColumnIdx(idxTable,decColName);
    const rowNumIdx= getColumnIdx(idxTable,'ROW_NUM');
    const rows= idxTable.tableData.data;
    const lowestNorder= Number(idxTable.request.order);
    let ra, dec, idx;
    const idxRoot= {...state[tbl_id]};

    for(let i=0;(i<idxTable.totalRows);i++) {
        ra= rows[i][raIdx];
        dec= rows[i][decIdx];
        idx= rows[i][rowNumIdx];
        const wp= makeWorldPt(ra,dec,csys);
        addPartialOrderRow(idxRoot.orderData,wp,idx,lowestNorder);
    }
    return {...state, [tbl_id]:idxRoot};
}


export const makeTableOrderDataAction= (rawAction) => (dispatcher) =>  setupTableMonitoring(rawAction,dispatcher);

async function setupTableMonitoring(rawAction,dispatcher) {
    const {tbl_id}= rawAction.payload;
    const table= getTblById(tbl_id);
    if (idxRoot()[tbl_id] || !table) return;
    dispatcher( { type: ADD_TABLE_IDX, payload:{ tbl_id} });

    try {
        await addTableIndex(tbl_id, dispatcher);
        dispatchAddActionWatcher({
            actions:[ TABLE_LOADED, TABLE_REMOVE, TABLE_SELECT],
            callback: watchTable,
            params: {tbl_id,dispatcher}
        });
    } catch (reason) {
        logger.error(`Failed index table ${tbl_id}: ${reason}`, reason);
        dispatcher( { type: INDEX_REMOVE, payload:{ tbl_id} });
    }

}

const useTableArrayFetch= true; // when true the new table array fetch otherwise use the classic table model

function makeTblReq(table,params) {
    const allParams= { startIdx : 0, pageSize : MAX_ROW, ...params};
    return {...cloneRequest(table.request, allParams), tbl_id: `hpxIdx-${table.tbl_id}`};
}

async function addTableIndex(tbl_id,dispatcher) {
    const table= getTblById(tbl_id);
    const {lonCol,latCol,csys} = findTableCenterColumns(table);
    const runId= initRunId(tbl_id);
    const maxInitialLoadNorder= DATA_NORDER-1;
    const tableUsingRadians= isTableUsingRadians(table, [lonCol,latCol]);
    const selectAll= table.selectInfo?.selectAll??false;

    if (!table.totalRows) {
        dispatcher( { type: ENABLE_HPX_INDEX, payload:{ tbl_id, csys, ready:false }});
        return;
    }
    dispatchAddTaskCount(DEFAULT_COVERAGE_PLOT_ID,HPX_WORKING_KEY);
    dispatcher( { type: ORDER_DATA_READY, payload:{ready:false,tbl_id} });

    if (table.totalRows>1_000_000) {
        dispatchComponentStateChange(COVERAGE_WAITING_MSG, {msg:'Fetching coverage information'});
    }
    let orderData;
    let lonAry= undefined, latAry= undefined;

    let partialIndexData= shouldUsePartialIndexType(table);
    if (partialIndexData) orderData= await fetchPartialTableHealpixIndex(table,maxInitialLoadNorder,runId);
    
    if (!orderData) {
        if (partialIndexData) console.log('HpxIndexCntlr: partial table indexing failed, using full');
        partialIndexData= false;
        const results= await fetchFullTableHealpixIndex(table,runId);
        orderData= results.orderData;
        lonAry= results.lonAry;
        latAry= results.latAry;
    }

    if (shouldAbort(tbl_id,runId)) return;
    const {exceptions}= table.selectInfo;
    const selectionOrderData= partialIndexData ?
        await createPartialSelectionHealPixIndexAsync(tbl_id,exceptions):
        await createSelectionHealPixIndexAsync(tbl_id, lonAry, latAry, csys, exceptions);
    if (shouldAbort(tbl_id,runId)) return;
    dispatcher( { type: ENABLE_HPX_INDEX,
        payload:{ tbl_id, orderData, tableUsingRadians, lonAry, latAry, csys, selectionOrderData,
            selectAll, partialIndexData, maxInitialLoadNorder} });
    dispatchRemoveTaskCount(DEFAULT_COVERAGE_PLOT_ID,HPX_WORKING_KEY);
}

function shouldUsePartialIndexType(table) {
    //TODO determine when best to use full index and when to use partial. Is there any other factor beyond total rows
    return table?.totalRows>MIN_TOTAL_ROWS_FOR_PARTIAL;
}


async function fetchPartialTableHealpixIndex(table,maxInitialLoadNorder,runId) {
    const {lonCol,latCol} = findTableCenterColumns(table);
    const norder= String(maxInitialLoadNorder);
    const req = {
        id:'HealpixIndex',
        mode:'map',
        ra:lonCol,
        dec:latCol,
        searchRequest: table.request,
        order:norder,
        META_INFO: {tbl_id: `${table.tbl_id}-map-hpxIndex-Norder-${norder}`}
    };
    const mapDataTable= await doFetchTable(req);
    if (mapDataTable.error) {
        console.log(`HpxIndexCntlr: partial table indexing error: ${mapDataTable.error}`);
        return;
    }
    if (shouldAbort(table.tbl_id,runId)) return;
    return await createPartialHealpixIndexAsync(table.tbl_id,mapDataTable,runId);
}



async function fetchFullTableHealpixIndex(table, runId) {
    let lonAry;
    let latAry;
    const {lonCol,latCol,csys} = findTableCenterColumns(table);
    if (useTableArrayFetch) { // this is the normal case
        const req = makeTblReq(table,{lonCol,latCol});
        const buffer= await fetchSpacialBinary(req);
        const byteSize= table.totalRows*4;
        lonAry= new Uint32Array(buffer.slice(0,byteSize));
        latAry= new Int32Array(buffer.slice(byteSize,2*byteSize));
    }
    else {
        const req = makeTblReq(table,{inclCols: `"${lonCol}","${latCol}"`});
        const allRowsTable = await doFetchTable(req);
        const lonIdx= getColumnIdx(allRowsTable,lonCol);
        const length= allRowsTable?.tableData?.data?.length ?? 0;
        lonAry= new Uint32Array(length);
        latAry= new Int32Array(length);
        const latIdx= getColumnIdx(allRowsTable,latCol);
        for(let i=0;(i<length);i++) {
            lonAry[i]= allRowsTable.tableData.data[i][lonIdx] * UINT_SCALE;
            latAry[i]= allRowsTable.tableData.data[i][latIdx] * UINT_SCALE;
        }
    }
    if (shouldAbort(table.tbl_id,runId)) return;

    const orderData= await createHealPixIndexAsync(lonAry, latAry, csys, table.tbl_id, runId);
    return {orderData,lonAry,latAry};
}



function watchTable(action, cancelSelf, params) {
    const {tbl_id,dispatcher}= params;
    if (action.payload.tbl_id!==tbl_id) return;
    switch (action.type) {
        case TABLE_REMOVE:
            cancelSelf();
            dispatcher( { type: CLEAR_IDX, payload:{ready:false,tbl_id} });
            break;

        case TABLE_LOADED:
            dispatcher( { type: ORDER_DATA_READY, payload:{ready:false,tbl_id} });
            void addTableIndex(tbl_id, dispatcher);
            break;

        case TABLE_SELECT:
            const {selectInfo,context={}}= action.payload;
            const {row:singleDeltaIdx,hpxSelectList}= context;
            void handleSelection(dispatcher,tbl_id,selectInfo,hpxSelectList,singleDeltaIdx);
            break;

    }
    return params;
}

async function handleSelection(dispatcher,tbl_id,selectInfo,hpxSelectList,singleDeltaIdx) {
    dispatcher( { type: ORDER_DATA_READY, payload:{ready:false,tbl_id} });
    const {selectAll=false,exceptions= new Set()}= selectInfo ?? {};
    const {lonAry, latAry, csys, selectionOrderData:currentSelOrderData,partialIndexData }= getHpxIndexData(tbl_id);
    const selectionOrderData= partialIndexData ?
        await createPartialSelectionHealPixIndexAsync(tbl_id,exceptions,hpxSelectList,singleDeltaIdx,currentSelOrderData) :
        await createSelectionHealPixIndexAsync(tbl_id, lonAry, latAry, csys, exceptions);
    dispatcher( { type: ADD_SELECTION_HPX_INDEX, payload:{ready:true,selectionOrderData, selectAll,tbl_id} });
}


export function getHistgramForNorder(idxData, norder) {
    return idxData?.orderData?.[norder]?.histogramInfo;
}

export const getAllSelectedIndexes= (idxData, norder, tileNumber) =>
    getAllIndexes(idxData.selectionOrderData,norder,tileNumber);

export const getAllDataIndexes= (idxData, norder, tileNumber) =>
    getAllIndexes(idxData.orderData,norder,tileNumber);

/**
 *
 * @param orderData
 * @param norder
 * @param tileNumber
 * @return {Array<Number>}
 */
function getAllIndexes(orderData, norder, tileNumber) {
    const tileEntry= getTile(orderData, norder, tileNumber);
    if (!tileEntry) return [];
    if (norder===DATA_NORDER) {
        if (tileEntry?.precomputedTileWP)  return [...tileEntry.precomputedTileWP.keys()];
        if (tileEntry?.tableIndexes?.length) return tileEntry.tableIndexes;
        return [];
    }
    const pgIdxs= getProGradeTilePixels(tileNumber);
    return [
        ...getAllIndexes(orderData,norder+1,pgIdxs[0]),
        ...getAllIndexes(orderData,norder+1,pgIdxs[1]),
        ...getAllIndexes(orderData,norder+1,pgIdxs[2]),
        ...getAllIndexes(orderData,norder+1,pgIdxs[3])
    ];
}

function getAllDataOrderTiles(orderData, norder, tileNumber) {
    const tile= getTile(orderData, norder, tileNumber);
    if (!tile) return [];
    if (norder===DATA_NORDER) {
        if (!tile?.tableIndexes?.length && !tile?.precomputedTileWP?.size) return [];
        return [tile];
    }
    const pgIdxs= getProGradeTilePixels(tileNumber);
    return [
        ...getAllDataOrderTiles(orderData,norder+1,pgIdxs[0]),
        ...getAllDataOrderTiles(orderData,norder+1,pgIdxs[1]),
        ...getAllDataOrderTiles(orderData,norder+1,pgIdxs[2]),
        ...getAllDataOrderTiles(orderData,norder+1,pgIdxs[3])
    ];
}

function findTileByIdx(orderData, idx) {
    for(const tileMap of orderData[DATA_NORDER].tiles) {
        const retTile= tileMap.values().find( (t) => {
            if (t.indexesLoaded) return false;
            if (t.precomputedTileWP) return t.precomputedTileWP.has(idx);
            return t.tableIndexes?.includes(idx);
        });
        if (retTile) return retTile;
    }
}

/**
 *
 * @param {TableHpxData} hpxIndexData
 * @param {Number} idx
 * @return {WorldPt}
 */
export function makeHpxWpt(hpxIndexData, idx) {
    if (!hpxIndexData.partialIndexData) {
        if (!hpxIndexData?.latAry || !hpxIndexData?.latAry) return;
        if (hpxIndexData.lonAry[idx]===0x7fffffff || hpxIndexData.latAry[idx]===0x7fffffff) return null;
        return makeWorldPt(
            hpxIndexData.lonAry[idx]/UINT_SCALE,
            hpxIndexData.latAry[idx]/UINT_SCALE,
            hpxIndexData.csys,
            false,
            hpxIndexData.tableUsingRadians);
    }
    else {
        const tileEntry= findTileByIdx(hpxIndexData.orderData,idx);
        if (!tileEntry) return;
        return tileEntry.precomputedTileWP.get(idx);
    }
}





/**
 *
 * @param hpxIndexData
 * @param {TileEntry} tileEntry
 * @return {{wpAry: Array.<WorldPt>, idxAry: Array.<number>}}
 */
export function getWptsIdxsByTile(hpxIndexData, tileEntry) {
    let wpAry= [];
    let idxAry=[];
    if (hpxIndexData.partialIndexData) {
        if (!tileEntry.indexesLoaded) return {wpAry:[], idxAry:[]};
        if (tileEntry?.precomputedTileWP) {
            wpAry= [...tileEntry.precomputedTileWP.values()];
            idxAry= [...tileEntry.precomputedTileWP.keys()];
        }
    }
    else {
        wpAry= tileEntry.tableIndexes.map((idx) => makeHpxWpt(hpxIndexData, idx));
        idxAry= tileEntry.tableIndexes;
    }
    return {wpAry,idxAry};
}

/**
 *
 * @param {TableHpxData} hpxIndexData
 * @param {number} norder
 * @param {number} tileNumber
 * @return {{wpAry: Array.<WorldPt>, idxAry: Array.<number>}}
 */
 export function getAllWptsIdxsForTile(hpxIndexData, norder, tileNumber) {
    const tileAry= getAllDataOrderTiles(hpxIndexData.orderData, norder, tileNumber);
    const resultAry=[];
    tileAry.forEach( (tileData) => {
        const {wpAry, idxAry}= getWptsIdxsByTile(hpxIndexData, tileData);
        wpAry.forEach( (wp,i) => {
            resultAry.push({wp,idx:idxAry[i], dataNorderTile:tileData});
        });
    } );
    return resultAry;
}







/**
 *
 * @param {String} tbl_id
 * @return {TableHpxData}
 */
export const getHpxIndexData= (tbl_id) => idxRoot()[tbl_id];
export const hasOrderDataReady= (tbl_id) => getHpxIndexData(tbl_id)?.ready ?? false;


export function onOrderDataReady(tbl_id) {
    if (hasOrderDataReady(tbl_id)) return Promise.resolve(getHpxIndexData(tbl_id));

    const watchForLoaded= (action, cancelSelf, params) => {
        const {tbl_id,resolve}= params;
        if (action.payload.tbl_id!==tbl_id) return;
        if (!hasOrderDataReady(tbl_id)) return;
        resolve(getHpxIndexData(tbl_id));
    };

    return new Promise((resolve) => {
        dispatchAddActionWatcher({
            actions:[ ENABLE_HPX_INDEX, ORDER_DATA_READY, ADD_SELECTION_HPX_INDEX],
            params: {tbl_id,resolve}, callback: watchForLoaded});
    });
}



export function getProGradeTilePixels(pixel) {
    const base= pixel*4;
    return [base,base+1,base+2,base+3];
}

const tableRunIds= {};
const shouldAbort= (tbl_id, runId) => runId!==tableRunIds[tbl_id];
function initRunId(tbl_id) {
    const runId= Date.now();
    tableRunIds[tbl_id]= runId;
    return runId;
}


export async function createSelectionHealPixIndexAsync(tbl_id, lonAry, latAry, csys, exceptions) {
    const selectionOrderData= initOrderData();
    if (!exceptions?.size) return selectionOrderData;
    const addRow= (rowIdx) => {
        addOrderRow(selectionOrderData,lonAry,latAry,rowIdx,csys);
    };
    const finished= await createBackgroundRunner({iterator:exceptions.keys(), processValue:addRow});
    return finished ? selectionOrderData : initOrderData();
}



export async function createPartialSelectionHealPixIndexAsync(tbl_id,exceptions,
                                                              hpxSelectList=undefined, singleDeltaIdx=undefined,
                                                              lastSelectionOrderData=undefined) {
    if (!exceptions?.size) return initOrderData();
    if (hpxSelectList) { // optimization: handle selection from plots
        const selectionOrderData= initOrderData();
        const addEntry= (entry) => {
            addPartialOrderRow(selectionOrderData, entry.dataNorderTile.precomputedTileWP.get(entry.idx),
                entry.idx, MIN_NORDER,true);
        };
        const finished= await createBackgroundRunner({iterator:hpxSelectList.values(), processValue:addEntry});
        return finished ? selectionOrderData : initOrderData();
    }
    else if (isDefined(singleDeltaIdx)) { // optimization: handle single row change (from table checkbox click)
        const wp= makeWorldPtUsingCenterColumns(tbl_id,singleDeltaIdx);
        if (wp) {
            exceptions.has(singleDeltaIdx) ?
                addPartialOrderRow(lastSelectionOrderData,wp,singleDeltaIdx,MIN_NORDER,true) :
                removePartialOrderRow(lastSelectionOrderData,wp,singleDeltaIdx,MIN_NORDER);
            return lastSelectionOrderData;
        }
    }

    // worse case handle a selection that is not from tables or plot, normally from charts or a new filter or sort
    // fallback case
    const {lonCol,latCol,csys} = findTableCenterColumns(tbl_id);
    const selectedTable= await getSelectedData(tbl_id, [lonCol,latCol,'ROW_IDX']);
    const selectionOrderData= initOrderData();

    const addRow= (row) => {
        const wp= makeWorldPt(row[0],row[1],csys);
        addPartialOrderRow(selectionOrderData,wp,row[2],MIN_NORDER,true);
    };
    const finished= await createBackgroundRunner({iterator:selectedTable.tableData.data.values(), processValue:addRow});
    return finished ? selectionOrderData : initOrderData();
}

function showProgressMsg(percent) {
    const msg= percent<95 ? `Indexing: ${percent}%` : 'Analyzing';
    dispatchComponentStateChange(COVERAGE_WAITING_MSG, {msg});
}

async function createPartialHealpixIndexAsync(tbl_id, mapDataTable, runId) {

    const orderData= initOrderData();
    const searchNorder= 11;
    const PIXEL_IDX= 0;
    const COUNT_IDX= 1;
    const rows= mapDataTable.tableData.data;

    if (!rows) return orderData;

    const addRow= (v,rowIdx) => {
        addHigherOrdersForPartial(orderData,rows[rowIdx][PIXEL_IDX], rows[rowIdx][COUNT_IDX], searchNorder);
    };

    const finished= await createBackgroundRunner({iterator:rows.values(), processValue:addRow,
        percentUpdate: showProgressMsg, length:rows.length,
        shouldAbort:() => shouldAbort(tbl_id,runId)});
    for(let i= MIN_NORDER_FOR_COVERAGE; (i<DATA_NORDER); i++) {
        orderData[i].histogramInfo=orderData?.[i]?.tiles ?
            getArrayStats( getValuesForOrder(orderData,i).map( ({count}) => count)) : undefined;
    }
    return finished ? orderData : initOrderData();
}

async function createHealPixIndexAsync(lonAry,latAry,csys,tbl_id,runId) {
    const orderData= initOrderData();
    const addRow= (v,rowIdx) => {
        addOrderRow(orderData,lonAry,latAry,rowIdx,csys);
    };
    const finished= await createBackgroundRunner({iterator:lonAry.values(), processValue:addRow,
        percentUpdate: showProgressMsg, length:lonAry.length,
        shouldAbort:() => shouldAbort(tbl_id,runId)});


    for(let i= MIN_NORDER_FOR_COVERAGE; (i<DATA_NORDER); i++) {
        orderData[i].histogramInfo=orderData?.[i]?.tiles ?
            getArrayStats( getValuesForOrder(orderData,i).map( ({count}) => count)) : undefined;
    }
    return finished ? orderData : initOrderData();
}


function getArrayStats(ary) {
    // basic stats
     const stats= ary.reduce( (obj,entry) => {
        if (entry>obj.max) obj.max= entry;
        if (entry<obj.min) obj.min= entry;
         obj.total+= entry;
        return obj;
    },{min:Number.MAX_SAFE_INTEGER, max:0, total:0});


    const mean = stats.total / ary.length;

    // calculate standard deviation
    let prep = 0;
    for(let i=0; (i<ary.length); i++) {
        prep += Math.pow((parseFloat(ary[i]) - mean),2);
    }
    const standDev = Math.sqrt(prep/(ary.length-1));

    return {...stats,standDev,mean};
}

function initOrderData() {
    const orderData= {};
    for(let i= MIN_NORDER_FOR_COVERAGE; (i<=DATA_NORDER); i++) {
        orderData[i]= {norder:i, tiles: undefined};

        if (i<10) {
            orderData[i].tiles= [new Map()];
        }
        else {
            const mapCnt= Math.trunc(maxTiles[i] / MAX_MAP)+1;
            orderData[i].tiles= [];
            for(let j= 0; (j<mapCnt); j++) orderData[i].tiles.push(new Map());
        }
    }
    return orderData;
}

function addOrderRow(orderData,lonAry,latAry,rowIdx,csys) {
    if (lonAry[rowIdx]===0x7fffffff || latAry[rowIdx]===0x7fffffff) return;
    const wp= convertCelestial(makeWorldPt( lonAry[rowIdx]/UINT_SCALE, latAry[rowIdx]/UINT_SCALE, csys));
    if (!wp) return;
    const pixel= getIpixForWp(wp,DATA_NSIDE);
    const tileEntry= getOrInitDataTile(orderData,DATA_NORDER,pixel);
    tileEntry.tableIndexes.push(rowIdx);
    tileEntry.count= tileEntry.tableIndexes.length;
    incrementHigherOrders(orderData, pixel);
}



function addPartialOrderRow(orderData,wp,rowIdx,lowestNorder,addSummaryTiles=false) {
    const pixel= getIpixForWp(wp,DATA_NSIDE);
    const tileEntry= getOrInitDataTile(orderData,DATA_NORDER,pixel,true);
    const hasIdx= tileEntry.precomputedTileWP.has(rowIdx);
    if (addSummaryTiles && hasIdx) return;

    if (!hasIdx) {
        tileEntry.precomputedTileWP.set(rowIdx,wp);
        tileEntry.count= tileEntry.precomputedTileWP.size;
        if (lowestNorder===DATA_NORDER) return;
        addSummaryTiles ?
            buildRetroIndex(orderData,lowestNorder,pixel) :
            markRetroLoaded(orderData,lowestNorder,pixel);
    }
}

function markRetroLoaded(orderData, lowestNorder,pixel) {
    let nextPixel= pixel;
    for(let norder= DATA_NORDER-1; (norder>=lowestNorder); norder--) {
        nextPixel = getRetroGradeIpix(nextPixel);
        getAndMarkLoadedTile(orderData, norder, nextPixel);
    }
}

function buildRetroIndex(orderData, lowestNorder,pixel) {
    let nextPixel= pixel;
    for(let norder= DATA_NORDER-1; (norder>=lowestNorder); norder--) {
        nextPixel = getRetroGradeIpix(nextPixel);
        getOrInitSummaryTile(orderData,norder,nextPixel).count++;
    }
}


function removePartialOrderRow(orderData,wp,rowIdx,lowestNorder) {
    const pixel= getIpixForWp(wp,DATA_NSIDE);
    const tileEntry= getTile(orderData,DATA_NORDER,pixel);
    if (!tileEntry || !tileEntry.precomputedTileWP.has(rowIdx)) return;
    // tileEntry.tableIndexes.splice(rowIdx,1);
    tileEntry.precomputedTileWP.delete(rowIdx);

    let nextPixel= pixel;
    for(let norder= DATA_NORDER-1; (norder>=lowestNorder); norder--) {
        nextPixel = getRetroGradeIpix(nextPixel);
        const tile = getTile(orderData, norder, nextPixel);
        if (tile && tile.count>0) tile.count--;
    }
}


export function getIpixForWp(wp,nside) {
    const {x,y}= convertCelestial(wp);
    const polar= radecToPolar(x,y);
    return ang2pixNest(polar.theta, polar.phi,nside);
}



/**
 *
 * @param {object} orderData
 * @param {number} norder
 * @param {number} tileNumber
 * @return {TileEntry}
 */
export function getTile(orderData, norder, tileNumber) {
    const mapIdx= Math.trunc(tileNumber/MAX_MAP);
    const tileMap= orderData[norder].tiles[mapIdx];
    return tileMap && tileMap.get(tileNumber);
}

function getOrInitSummaryTile(orderData, norder, pixel, indexesLoaded=true) {
    let tile = getTile(orderData, norder, pixel);
    if (tile) {
        tile.indexesLoaded= indexesLoaded;
        return tile;
    }
    tile= {pixel, count: 0, indexesLoaded};
    setPixelTile(orderData,norder,pixel,tile);
    return tile;
}

function getOrInitDataTile(orderData, norder, pixel, useWpMap) {
    let tile = getTile(orderData, norder, pixel);
    if (tile) return tile;
    tile= {pixel, count: 0, indexesLoaded:true};
    if (useWpMap) tile.precomputedTileWP= new Map();
    else  tile.tableIndexes= [];
    setPixelTile(orderData,norder,pixel,tile);
    return tile;
}


export function isSummaryTile(tileData) {
    return !(tileData.tableIndexes || tileData.precomputedTileWP);
}

function getAndMarkLoadedTile(orderData, norder, pixel) {
    const tile = getTile(orderData, norder, pixel);
    if (tile) tile.indexesLoaded = true;
    return tile;
}

export function isIndexesLoaded(orderData, norder, tileData) {
    if (!tileData) return false;
    if (tileData.indexesLoaded) return true;
    return false;
}

export function getTileTableIndexes(tileData) {
    if (!tileData) return [];
    return tileData.precomputedTileWP ? [...tileData.precomputedTileWP.keys()] : tileData.tableIndexes;
}

export function setPixelTile(orderData,norder,pixel,tile) {
    const mapIdx= Math.trunc(pixel/MAX_MAP);
    orderData[norder].tiles[mapIdx].set(pixel, tile );
}

export function getPixelCount(orderData,norder) {
    const tileMapAry= orderData[norder].tiles;
    return tileMapAry.reduce((sum,map) => sum+map.size, 0);
}

export function getValuesForOrder(orderData,norder) {
    if (!orderData) return [];
    if (norder<11) return [...orderData[norder]?.tiles[0]?.values()];
    const joinAry= [];
    for(let i=0; (i<orderData[norder]?.tiles.length);i++) {
       joinAry.push(Array.from(orderData[norder]?.tiles[i].values()));
    }
    return joinAry.flat();
}

export function getKeysForOrder(orderData,norder) {
    if (norder<11) return [...orderData[norder]?.tiles[0]?.keys()];
    const joinAry= [];
    for(let i=0; (i<orderData[norder]?.tiles.length);i++) {
        joinAry.push(Array.from(orderData[norder]?.tiles[i].keys()));
    }
    return joinAry.flat();
}


export const getRetroGradeIpix= (ipix, retroCnt=1) => retroCnt ? Math.trunc(ipix/(4**retroCnt)) : ipix;
export const getProGradeIpix= (ipix, proCnt=1) => proCnt ? ipix*(4**proCnt) : ipix;


function incrementHigherOrders(orderData, progradePixel) {
    let pixel= progradePixel;
    for(let norder= DATA_NORDER-1; (norder>=MIN_NORDER_FOR_COVERAGE); norder--) {
        pixel= getRetroGradeIpix(pixel);
        const tile= getOrInitSummaryTile(orderData,norder,pixel);
        tile.count++;
    }
}

function addHigherOrdersForPartial(orderData,pixel, inCount, partialStartOrder) {
    for(let norder= partialStartOrder; (norder>=MIN_NORDER_FOR_COVERAGE); norder--) {
        const tile= getOrInitSummaryTile(orderData,norder,pixel,false);
        tile.count+= inCount;
        pixel= getRetroGradeIpix(pixel);
    }
}


export async function ensureDataForSelection(tableOrId, norder, selectedTiles) {
    const table = getTableModel(tableOrId);
    const idxData = getHpxIndexData(table.tbl_id);
    if (!idxData.partialIndexData) return idxData;
    await fetchPartialData(table.tbl_id, norder, selectedTiles);
    return getHpxIndexData(table.tbl_id); // new idx data when completed
}


export async function fetchPartialData(tableOrId, norder, tiles) {

    const table = getTableModel(tableOrId);
    const {tbl_id}= table;
    await onOrderDataReady(tbl_id);
    const idxData = getHpxIndexData(tbl_id);

    const searchTiles= tiles.filter( (t) => !isIndexesLoaded(idxData.orderData, norder, t));
    if (!searchTiles.length) return;
    const missingPixels= searchTiles.map((t) => t.pixel);

    const pixels=  (missingPixels.length> 10) ? uniq(missingPixels.map( (ipix) => getRetroGradeIpix(ipix) )) : missingPixels;
    const finalNorder= (searchTiles.length> 10) ? norder-1 : norder;


    const pixStr=pixels.join(',');
    const asyncKey= 'fetchPartialData-'+tbl_id+'--' + pixStr;
    await blockWhileAsyncIdWaiting(asyncKey);
    const {lonCol,latCol,csys} = findTableCenterColumns(table);
    const request = {
        id:'HealpixIndex',
        mode:'points',
        order:finalNorder,
        ra:lonCol,
        dec:latCol,
        pixels: pixStr,
        searchRequest: table.request,
    };

    await synchronizeAsyncFunctionById(asyncKey, () => fetchAndIngest(tbl_id,request,lonCol,latCol,csys));
}

async function fetchAndIngest(tbl_id, request, raColName, decColName, csys) {
    const idxTable= await doFetchTable(request);
    dispatchAddPartialData({tbl_id,idxTable,raColName,csys,decColName});
    return idxTable;
}
