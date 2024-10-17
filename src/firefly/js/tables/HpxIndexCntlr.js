import {dispatchAddActionWatcher} from '../core/MasterSaga';
import {flux} from '../core/ReduxFlux';
import {ang2pixNest, radecToPolar} from '../externalSource/aladinProj/HealpixIndex';
import {logger} from '../util/Logger';
import {makeWorldPt} from '../visualize/Point';
import {convertCelestial} from '../visualize/VisUtil';
import {findTableCenterColumns} from '../voAnalyzer/TableAnalysis';
import {cloneRequest, MAX_ROW} from './TableRequestUtil';
import {TABLE_LOADED, TABLE_REMOVE, TABLE_SELECT} from './TablesCntlr';
import {doFetchTable, fetchSpacialBinary, getColumnIdx, getTblById, isTableUsingRadians} from './TableUtil';

export const DATA_NORDER= 12;
export const MIN_NORDER= 4;
export const MIN_NORDER_TO_ALWAYS_GROUP= 6;
export const MIN_NORDER_FOR_COVERAGE= 2;
export const MIN_ROWS_FOR_HIERARCHICAL= 1000;
const DATA_NSIDE= 2**DATA_NORDER;
const UINT_SCALE= 10**7;



/**
 * @typedef {Object} TableHpxData
 *
 * @summary TableHpxData
 *
 * @prop {String} tbl_id
 * @prop {boolean} ready
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
 * @prop  {Map.<Number,TileEntry>} tiles at this level
 */


/**
 * @typedef {Object} TileEntry
 *
 * @summary pixel tiles
 *
 * @prop {Number} pixel
 * @prop {boolean} summaryTile - true for all levels except bottom
 * @prop {Number} count
 * @prop {Array.<Number>} tableIndexes - summary tiles only have one entry
 *
 */


export const SPACIAL_HPX_INDX_PREFIX= 'HpxIndexCntlr';

export const ADD_TABLE_IDX= `${SPACIAL_HPX_INDX_PREFIX}.AddTableIdx`;
export const CLEAR_IDX= `${SPACIAL_HPX_INDX_PREFIX}.ClearIdx`;
export const INDEX_REMOVE= `${SPACIAL_HPX_INDX_PREFIX}.IndexRemove`;
export const ENABLE_HPX_INDEX= `${SPACIAL_HPX_INDX_PREFIX}.EnableHpxIndex`;
export const ADD_SELECTION_HPX_INDEX= `${SPACIAL_HPX_INDX_PREFIX}.AddSelectionHpxIndex`;
export const ORDER_DATA_READY= `${SPACIAL_HPX_INDX_PREFIX}.OrderDataReady`;

export function idxRoot() { return flux.getState()[SPACIAL_HPX_INDX_PREFIX]; }


const reducers= () => ({ [SPACIAL_HPX_INDX_PREFIX]: reducer, });

export function dispatchEnableHpxIndex({tbl_id}) {
    flux.process({type: ENABLE_HPX_INDEX, payload: {tbl_id} });
}


function actionCreators() {
    return {
        [ENABLE_HPX_INDEX]: makeTableOrderDataAction,
    };
}

export default { reducers, actionCreators, };

function reducer(state={}, action={}) {
    const {type,payload} = action;
    const {ready=true,csys,orderData,lonAry,latAry,selectionOrderData,
        selectAll,tableUsingRadians=false,tbl_id}= payload ?? {};


    switch (type) {

        case ADD_TABLE_IDX:
            return (!state[tbl_id]) ? {...state,[tbl_id]: {ready:false} } : state;

        case ENABLE_HPX_INDEX:
            return {...state,[tbl_id]:
                    {ready,orderData,lonAry,latAry, csys,selectionOrderData, selectAll, tableUsingRadians} };

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
    }

    return state;
}


export const makeTableOrderDataAction= (rawAction) => (dispatcher) =>  setupTableMonitoring(rawAction,dispatcher);

export const getHpxTblIdAry= (root) => [...Object.keys(root)];


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


const tableRunIds= {};


 const shouldAbort= (tbl_id, runId) => runId!==tableRunIds[tbl_id];
 function initRunId(tbl_id) {
     const runId= Date.now();
     tableRunIds[tbl_id]= runId;
     return runId;
}


async function addTableIndex(tbl_id,dispatcher) {
    const table= getTblById(tbl_id);
    const {lonCol,latCol,csys} = findTableCenterColumns(table);

    const runId= initRunId(tbl_id);

    if (!table.totalRows) {
        dispatcher( { type: ENABLE_HPX_INDEX, payload:{ tbl_id, csys, ready:false }});
        return;
    }


    let lonAry;
    let latAry;
    let tableUsingRadians= false;
    if (useTableArrayFetch) { // this is the normal case
        const req = makeTblReq(table,{lonCol,latCol});
        const buffer= await fetchSpacialBinary(req);
        const byteSize= table.totalRows*4;
        lonAry= new Uint32Array(buffer.slice(0,byteSize));
        latAry= new Int32Array(buffer.slice(byteSize,2*byteSize));
        tableUsingRadians= isTableUsingRadians(table, [lonCol,latCol]);
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

    if (shouldAbort(tbl_id,runId)) return;

    const orderData= await createHealPixIndexAsync(lonAry, latAry, csys, runId);
    const selectAll= table.selectInfo?.selectAll??false;
    if (shouldAbort(tbl_id,runId)) return;
    dispatcher( { type: ORDER_DATA_READY, payload:{ready:false,tbl_id} });
    const selectionOrderData=
        await createSelectionHealPixIndexAsync(lonAry, latAry, csys, table.selectInfo?.exceptions??new Set(), selectAll, runId);
    if (shouldAbort(tbl_id,runId)) return;
    dispatcher( { type: ENABLE_HPX_INDEX,
        payload:{ tbl_id, orderData, tableUsingRadians, lonAry, latAry, csys, selectionOrderData, selectAll} });
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
            const {selectInfo}= action.payload;
            dispatcher( { type: ORDER_DATA_READY, payload:{ready:false,tbl_id} });
            const selectAll= selectInfo?.selectAll??false;
            const {lonAry, latAry, csys}= idxRoot()[tbl_id] ?? {};
            createSelectionHealPixIndexAsync(lonAry, latAry, csys, selectInfo?.exceptions??new Set(), selectAll)
                    .then( (selectionOrderData) => {
                        dispatcher( { type: ADD_SELECTION_HPX_INDEX,
                            payload:{ready:true,selectionOrderData, selectAll,tbl_id} });
                    });
            break;

    }
    return params;
}







//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

/**
 *
 * @param {object} orderData
 * @param {number} norder
 * @param {number} tileNumber
 * @return {TileEntry}
 */
export function getTile(orderData, norder, tileNumber) {
    return orderData?.[norder]?.tiles?.get(tileNumber);
}

export function getHistgramForNorder(idxData, norder) {
    return idxData?.orderData?.[norder]?.histogramInfo;
}

export function getAllTilesAtNorder(orderData,norder) {
    return [...orderData?.[norder]?.tiles?.values()];
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
    const tile= getTile(orderData, norder, tileNumber);
    if (!tile) return [];
    if (norder===DATA_NORDER) {
        if (!tile?.tableIndexes?.length) return [];
        return tile.tableIndexes;
    }
    const pgIdxs= getProGradeTileNumbers(tileNumber);
    return [
        ...getAllIndexes(orderData,norder+1,pgIdxs[0]),
        ...getAllIndexes(orderData,norder+1,pgIdxs[1]),
        ...getAllIndexes(orderData,norder+1,pgIdxs[2]),
        ...getAllIndexes(orderData,norder+1,pgIdxs[3])
    ];
}

/**
 *
 * @param orderData
 * @param norder
 * @param tileNumber
 * @return {Array<Number>}
 */
// export function getFirstIndex(orderData, norder, tileNumber) {
//     const tileAry = getProGradeTileNumbersForRange(tileNumber, norder, DATA_NORDER);
//     const tile = tileAry.find((t) => getTile(orderData, DATA_NORDER, t)?.tableIndexes?.length);
//     if (!tile) return [];
//     return [getTile(orderData, norder, tile)?.tableIndexes[0]];
// }
export function getFirstIndex(orderData, norder, tileNumber) {
    const tile= getTile(orderData, norder, tileNumber);
    if (!tile) return [];
    if (norder===DATA_NORDER) {
        if (!tile?.tableIndexes?.length) return [];
        return [tile.tableIndexes[0]];
    }
    const pgIdxs= getProGradeTileNumbers(tileNumber);

    let idxAry= getFirstIndex(orderData,norder+1,pgIdxs[0]);
    if (!idxAry.length) idxAry= getFirstIndex(orderData,norder+1,pgIdxs[1]);
    if (!idxAry.length) idxAry= getFirstIndex(orderData,norder+1,pgIdxs[2]);
    if (!idxAry.length) idxAry= getFirstIndex(orderData,norder+1,pgIdxs[3]);
    return idxAry;
}

/**
 *
 * @param {TableHpxData} hpxIndexData
 * @param {Number} idx
 * @return {WorldPt}
 */
export function makeHpxWpt(hpxIndexData, idx) {
    if (!hpxIndexData?.latAry || !hpxIndexData?.latAry) return;
    return makeWorldPt(
        hpxIndexData.lonAry[idx]/UINT_SCALE,
        hpxIndexData.latAry[idx]/UINT_SCALE,
        hpxIndexData.csys,
        false,
        hpxIndexData.tableUsingRadians);
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



//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

// function hasTile(hpxIndexData, norder, tileNumber) {
//     return Boolean(hpxIndexData?.orderData[norder]?.tiles?.has(tileNumber));
// }
//
export function getProGradeTileNumbers(tileNumber) {
    const base= tileNumber*4;
    return [base,base+1,base+2,base+3];
}

// function getProGradeTileNumbersForRange(tileNumber,fromNorder, toNorder) {
//     const tileNumberAry=  isArray(tileNumber) ? tileNumber : [tileNumber];
//     if (fromNorder===toNorder) return tileNumberAry;
//
//     const nextAry=[];
//     tileNumberAry.forEach( (pix) => {
//         nextAry.push(...getProGradeTileNumbers(pix));
//     });
//     return getProGradeTileNumbersForRange(nextAry,fromNorder+1,toNorder);
//
// }





export function createSelectionHealPixIndexAsync(lonAry,latAry,csys,exceptions,selectAll,runId) {
    return new Promise( (resolve) => doCreateSelectionHealPixWork(resolve,exceptions,selectAll,lonAry,latAry,csys,runId) );
}

function doCreateSelectionHealPixWork(resolve, exceptions, selectAll, lonAry, latAry, csys, runId) {
    const selectionOrderData= initOrderData();
    const iterator = exceptions.keys();
    let i=0;
    let isDone= false;
    const id = window.setInterval(
        () => {
            if (shouldAbort(runId)) {
                isDone=true;
                resolve(undefined);
                window.clearInterval(id);
                return;
            }
            if (isDone) window.clearInterval(id);
            for (; (!isDone);) {
                const {value:rowIdx, done}= iterator.next();
                isDone= done;
                if (!isDone) {
                    addOrderRow(selectionOrderData,lonAry,latAry,rowIdx,csys);
                    i++;
                    if (i % 10000 === 0) return;
                }
            }
            resolve(selectionOrderData);
        });
}



export function createHealPixIndexAsync(lonAry,latAry,csys,runId) {
    return new Promise( (resolve) => doCreateHealPixWork(resolve,lonAry,latAry,csys,runId) );
}

function doCreateHealPixWork(resolve, lonAry, latAry, csys, runId) {
    const orderData= initOrderData();
    let rowIdx=0;
    const length= lonAry.length;
    let done= false;
    const id = window.setInterval(
        () => {
            if (shouldAbort(runId)) {
                done=true;
                resolve(undefined);
            }
            if (done) {
                window.clearInterval(id);
                return;
            }
            for (; (rowIdx < length);) {
                addOrderRow(orderData, lonAry, latAry, rowIdx, csys);
                rowIdx++;
                if (rowIdx % 10000 === 0) return;
            }
            done= true;

            for(let i= MIN_NORDER_FOR_COVERAGE; (i<DATA_NORDER); i++) {
                orderData[i].histogramInfo=orderData?.[i]?.tiles ?
                    getArrayStats( [...orderData?.[i]?.tiles?.values()]?.map( ({count}) => count)) : undefined;
            }
            resolve(orderData);
        });
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
        orderData[i]= {norder:i, tiles: new Map()};
    }
    return orderData;
}

function addOrderRow(orderData,lonAry,latAry,rowIdx,csys) {
    const wp= convertCelestial(makeWorldPt( lonAry[rowIdx]/UINT_SCALE, latAry[rowIdx]/UINT_SCALE, csys));
    const pixel= getIpixForWp(wp,DATA_NSIDE);
    const tileEntry= orderData[DATA_NORDER].tiles.get(pixel);
    if (tileEntry) {
        tileEntry.tableIndexes.push(rowIdx);
        tileEntry.count= tileEntry.tableIndexes.length;
        addHigherOrders(orderData, pixel);
    }
    else {
        const tile= {pixel, summaryTile: false, count: 1, tableIndexes: [rowIdx]};
        orderData[DATA_NORDER].tiles.set(pixel, tile );
        addHigherOrders(orderData, pixel);
    }
}



export function getIpixForWp(wp,nside) {
    const {x,y}= convertCelestial(wp);
    const polar= radecToPolar(x,y);
    return ang2pixNest(polar.theta, polar.phi,nside);
}




const getRetroGradeTileNumber= (tileNumber) => Math.trunc(tileNumber/4);

function addHigherOrders(orderData,progradePixel) {
    let pixel= progradePixel;
    for(let norder= DATA_NORDER-1; (norder>=MIN_NORDER_FOR_COVERAGE); norder--) {
        pixel= getRetroGradeTileNumber(pixel);
        let tile= orderData[norder].tiles.get(pixel);
        if (!tile) {
            tile= {pixel, summaryTile: true, count: 0};
            orderData[norder].tiles.set(pixel,tile);
        }
        tile.count++;
    }
}

