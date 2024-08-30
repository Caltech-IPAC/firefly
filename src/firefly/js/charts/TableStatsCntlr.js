import {flux} from '../core/ReduxFlux.js';

import {get, has, omit} from 'lodash';

import {updateSet, updateMerge} from '../util/WebUtil.js';
import ColValuesStatistics from './ColValuesStatistics.js';
import {REINIT_APP} from '../core/AppDataCntlr.js';

import {makeTblRequest, cloneRequest, MAX_ROW} from '../tables/TableRequestUtil.js';
import {getTblById, doFetchTable, getColumns, COL_TYPE} from '../tables/TableUtil.js';

import * as TablesCntlr from '../tables/TablesCntlr.js';

/*
 Possible structure of store:
 /tblstats
   tbl_id: Object - the name of this node matches table id
   {
     isColStatsReady: boolean
     colStats: [ColValuesStatistics]
   }
 */


export const TBLSTATS_DATA_KEY = 'tblstats';

export const LOAD_TBL_STATS = `${TBLSTATS_DATA_KEY}/LOAD_TBL_STATS`;
export const UPDATE_TBL_STATS = `${TBLSTATS_DATA_KEY}/UPDATE_TBL_STATS`;


export default {actionCreators, reducers};

function actionCreators() {
    return {
        [LOAD_TBL_STATS]:   loadTblStats
    };
}

function reducers() {
    return {
        [TBLSTATS_DATA_KEY]: reducer
    };
}


/*
 * Get the number of points, min and max values, units and description for each table column
 * @param {ServerRequest} searchRequest - table search request
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchLoadTblStats(searchRequest, dispatcher= flux.process) {
    const tbl_id = get(searchRequest, 'tbl_id');
    if (!tbl_id) return;
    // use resultSetID to determine if a call needs to be placed
    const {resultSetID, numericColCnt} = get(flux.getState(), [TBLSTATS_DATA_KEY, tbl_id], {});
    const resultSetIDNow = get(getTblById(tbl_id), 'tableMeta.resultSetID');
    const curNumericColCnt = getColumns(getTblById(tbl_id), COL_TYPE.NUMBER)?.length;

    if (resultSetID !== resultSetIDNow
        || curNumericColCnt !== numericColCnt) {  // also reload stats if the number of numeric columns changes.
        dispatcher({type: LOAD_TBL_STATS, payload: {searchRequest}});
    }
}

/*
 * The statistics is successfully returned from the server, update the store
 * @param {Number} tblId - table id
 * @param {boolean} isColStatsReady flags that column statistics is now available
 * @param {ColValuesStatistics[]} an array which holds column statistics for each column
const dispatchUpdateTblStats = function(tblId,isColStatsReady,colStats) {
    flux.process({type: UPDATE_TBL_STATS, payload: {tblId,isColStatsReady,colStats}});
};
*/

/*
 * @param rawAction (its payload should contain searchRequest to get source table)
 * @returns function which loads statistics (column name, num. values, range of values) for a source table
 */
export const loadTblStats = function(rawAction) {
    return (dispatch) => {
        if (rawAction.payload.searchRequest) {
            dispatch({ type : LOAD_TBL_STATS, payload : rawAction.payload });
            fetchTblStats(dispatch, rawAction.payload.searchRequest);
        }
    };
};


function getInitState() {
    return {};
}


export function reducer(state=getInitState(), action={}) {
    switch (action.type) {
        case (TablesCntlr.TABLE_REMOVE)  :
        {
            const {tbl_id} = action.payload;
            if (has(state, tbl_id)) {
                return Object.assign({}, omit(state,[tbl_id]));
            }
            return state;
        }
        case (LOAD_TBL_STATS)  :
        {
            const tblId = get(action.payload, ['searchRequest', 'tbl_id']);

            // save original table file path
            const resultSetID = get(getTblById(tblId), 'tableMeta.resultSetID');

            return updateSet(state, tblId, {resultSetID, isColStatsReady: false});
        }
        case (UPDATE_TBL_STATS)  :
        {
            const {tblId, isColStatsReady, colStats} = action.payload;
            return updateMerge(state, tblId, {isColStatsReady, colStats});
        }
        case (REINIT_APP)  :
            return getInitState();
        default:
            return state;
    }
}

/**
 *
 * @param statsData {Object} The table statistics object to merge with the histogram branch under root
 * @returns {{type: string, payload: object}}
 */
function updateTblStats(statsData) {
    return { type : UPDATE_TBL_STATS, payload: statsData };
}


/**
 * fetches histogram data for a column or column expression of an active table
 * set isColStatsReady to true once done.
 * @param dispatch
 * @param activeTableServerRequest table search request to obtain source table
 */
function fetchTblStats(dispatch, activeTableServerRequest) {

    const {tbl_id} = activeTableServerRequest;

    const resultSetIDOnSubmit = get(getTblById(tbl_id), 'tableMeta.resultSetID');

    // searchRequest
    const sreq = cloneRequest(activeTableServerRequest, {'startIdx': 0, 'pageSize': MAX_ROW});

    const req = makeTblRequest('StatisticsProcessor', `Statistics for ${tbl_id}`,
        { searchRequest: JSON.stringify(sreq) },
        {'startIdx': 0, 'pageSize': MAX_ROW});

    doFetchTable(req).then(
        (tableModel) => {

            // if the original table has changed no need to continue
            const resultSetIDOnFetch = get(getTblById(tbl_id), 'tableMeta.resultSetID');
            if (resultSetIDOnSubmit !== resultSetIDOnFetch) {
                return;
            }

            const tblData = get(tableModel, 'tableData.data');
            let colStats = [];
            if (tblData) {
                const columns = get(getTblById(tbl_id), 'tableData.columns', []);
                colStats = tblData.reduce((colstats, arow) => {
                    const r = new ColValuesStatistics(...arow);
                    const col = columns.find((c)=>{return c.name=== r.name;});
                    if (col) {
                        r.unit = col.units && col.units !== 'null' ? col.units : '';
                        r.descr = col.desc ? col.desc: '';
                        r.type = col.type ? col.type : '';
                    }
                    colstats.push(r);
                    return colstats;
                }, []);
            }
            const numericColCnt = getColumns(getTblById(tbl_id), COL_TYPE.NUMBER)?.length;
            dispatch(updateTblStats(
                {
                    tblId: tbl_id,
                    isColStatsReady: true,
                    colStats,
                    numericColCnt
                }));
        }
    ).catch(
        (reason) => {
            console.error(`Failed to fetch table statistics: ${reason}`);
            dispatch(updateTblStats(
                {
                    tblId: tbl_id,
                    isColStatsReady: true,
                    resultSetID: undefined,
                    colStats: undefined
                }));
        }
    );
}

export function getColValStats(tblId) {
    if (!tblId) { return undefined; }
    const tblStatsData = flux.getState()[TBLSTATS_DATA_KEY][tblId];
    if (get(tblStatsData,'isColStatsReady')) {
        return tblStatsData.colStats;
    }
}
