import {flux} from '../Firefly.js';

import {has, get, set} from 'lodash';


import {doFetchTable} from '../tables/TableUtil.js';
//import * from TableUtil from '../tables/TableUtil.js';
//import * from TablesCntlr from '../tables/TablesCntlr.js';


const XYPLOT_DATA_KEY = 'xyplot';
const LOAD_PLOT_DATA = `${XYPLOT_DATA_KEY}/LOAD_COL_DATA`;
const UPDATE_PLOT_DATA = `${XYPLOT_DATA_KEY}/UPDATE_COL_DATA`;

/*
 Possible structure of store:
  /xyplot
    tbl_id: Object - the name of this node matches table id
    {
         // tblXYPlotData
         isPlotDataReady: boolean
         xyPlotData: [[rowIdx: int, x: string, y: string]*]  or [[row]
         xyPlotParams: {
           title: string
           xyRatio: number
           stretch: string (fit|fill)
           x: {
                columnOrExpr
                label
                unit
                options: [grid,log,flip]
                nbins
                min
                max
              }
           y: {
                columnOrExpr
                label
                unit
                options: [grid,log,flip]
                nbins
                min
                max
           }
     }
 */

/*
 * Get column histogram data
 * @param {Object} xyPlotParams - XY plot options (column names, etc.)
 * @param {ServerRequest} searchRequest - table search request
 */
const dispatchLoadPlotData = function(xyPlotParams, searchRequest) {
    flux.process({type: LOAD_PLOT_DATA, payload: {xyPlotParams, searchRequest}});
};

/*
 * Get xy plot data
 * @param {boolean} isPlotDataReady - flags that xy plot data are available
 * @param {Number[][]} xyPlotData - an array of the number arrays with rowIdx, x, y, [error]
 * @param {Object} xyPlotParams - XY plot options (column names, etc.)
 * @param {ServerRequest} searchRequest - table search request
 */
const dispatchUpdatePlotData = function(isPlotDataReady, xyPlotData, xyPlotParams, searchRequest) {
    flux.process({type: UPDATE_PLOT_DATA, payload: {isPlotDataReady, xyPlotData, xyPlotParams, searchRequest}});
};


/*
 * @param rawAction (its payload should contain searchRequest to get source table and histogram parameters)
 * @returns function which loads statistics (column name, num. values, range of values) for a source table
 */
const loadPlotData = function(rawAction) {
    return (dispatch) => {
        dispatch({ type : LOAD_PLOT_DATA, payload : rawAction.payload });
        if (rawAction.payload.searchRequest && rawAction.payload.xyPlotParams) {
            fetchPlotData(dispatch, rawAction.payload.searchRequest, rawAction.payload.xyPlotParams);
        }

    };
};

function getInitState() {
    return {};
}

/*
 Get the new state related to a particular table (if it's tracked)
 @param tblId {string} table id
 @param state {object} histogram store
 @param newProps {object} new properties
 @return {object} new state
 */
function stateWithNewData(tblId, state, newProps) {
    if (has(state, tblId)) {
        const tblData = get(state, tblId);
        const newTblData = Object.assign({}, tblData, newProps);
        const newState = Object.assign({}, state);
        set(newState, tblId, newTblData);
        return newState;
    }
    return state;
}

function reducer(state=getInitState(), action={}) {
    switch (action.type) {
        case (LOAD_PLOT_DATA)  :
        {
            let {xyPlotParams, searchRequest} = action.payload;
            const newState = Object.assign({}, state);
            set(newState, searchRequest.tbl_id, {isPlotDataReady: false});
            return newState;
        }
        case (UPDATE_PLOT_DATA)  :
        {
            let {isPlotDataReady, xyPlotData, xyPlotParams, searchRequest} = action.payload;
            return stateWithNewData(searchRequest.tbl_id, state, {
                isPlotDataReady,
                xyPlotData,
                xyPlotParams,
                searchRequest
            });
        }
        default:
            return state;
    }
}


/**
 *
 * @param data {Object} the data to merge with the xyplot branch
 * @returns {{type: string, payload: object}}
 */
function updatePlotData(data) {
    return { type : UPDATE_PLOT_DATA, payload: data };
}



/**
 * fetches xy plot data
 * set isColStatsReady to true once done.
 * @param dispatch
 * @param activeTableServerRequest table search request to obtain source table
 * @param xyPlotParams object, which contains xy plot parameters

 */
function fetchPlotData(dispatch, activeTableServerRequest, xyPlotParams) {

    // todo support expressions
    const req = Object.assign({}, activeTableServerRequest, {'startIdx' : 0, 'pageSize' : 1000000,
        'inclCols' : xyPlotParams.x.columnOrExpr+','+xyPlotParams.y.columnOrExpr
        });

    req.tbl_id = activeTableServerRequest.tbl_id;

    doFetchTable(req).then(
        (tableModel) => {
            if (tableModel.tableData && tableModel.tableData.data) {
                const xyPlotData = tableModel.tableData.data;

                dispatch(updatePlotData(
                    {
                        isPlotDataReady : true,
                        xyPlotParams,
                        xyPlotData,
                        searchRequest : req
                    }));
            }
        }
    ).catch(
        (reason) => {
            console.error(`Failed to fetch XY plot data: ${reason}`);
        }
    );
}



var XYPlotCntlr = {
    XYPLOT_DATA_KEY,
    reducer,
    dispatchLoadPlotData,
    loadPlotData,
    LOAD_PLOT_DATA,
    UPDATE_PLOT_DATA };
export default XYPlotCntlr;
