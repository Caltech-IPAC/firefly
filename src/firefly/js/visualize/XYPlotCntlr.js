import {flux} from '../Firefly.js';

import update from 'react-addons-update';
import {has} from 'lodash';


import {doFetchTable, isTableLoaded} from '../tables/TableUtil.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';

import {DecimateInfo} from '../tables/DecimateInfo.js';

export const XYPLOT_DATA_KEY = 'xyplot';
export const LOAD_PLOT_DATA = `${XYPLOT_DATA_KEY}/LOAD_COL_DATA`;
export const UPDATE_PLOT_DATA = `${XYPLOT_DATA_KEY}/UPDATE_COL_DATA`;
export const SET_SELECTION = `${XYPLOT_DATA_KEY}/SET_SELECTION`;
export const SET_ZOOM = `${XYPLOT_DATA_KEY}/SET_ZOOM`;
export const RESET_ZOOM = `${XYPLOT_DATA_KEY}/RESET_ZOOM`;

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
           selection: {xMin, xMax, yMin, yMax} // currently selected rectangle
           zoom: {xMin, xMax, yMin, yMax} // currently zoomed rectangle
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
 * Load xy plot data
 * @param {Object} xyPlotParams - XY plot options (column names, etc.)
 * @param {ServerRequest} searchRequest - table search request
 */
export const dispatchLoadPlotData = function(xyPlotParams, searchRequest) {
    flux.process({type: LOAD_PLOT_DATA, payload: {xyPlotParams, searchRequest}});
};

/*
 * Update xy plot data
 * @param {boolean} isPlotDataReady - flags that xy plot data are available
 * @param {Number[][]} xyPlotData - an array of the number arrays with rowIdx, x, y, [error]
 * @param {Object} xyPlotParams - XY plot options (column names, etc.)
 * @param {ServerRequest} searchRequest - table search request
 */
export const dispatchUpdatePlotData = function(isPlotDataReady, xyPlotData, xyPlotParams, searchRequest) {
    flux.process({type: UPDATE_PLOT_DATA, payload: {isPlotDataReady, xyPlotData, xyPlotParams, searchRequest}});
};

export const dispatchSetSelection = function(tblId, selection) {
    flux.process({type: SET_SELECTION, payload: {tblId, selection}});
};

export const dispatchSetZoom = function(tblId, selection) {
    flux.process({type: SET_ZOOM, payload: {tblId, selection}});
};

export const dispatchResetZoom = function(tblId) {
    flux.process({type: RESET_ZOOM, payload: {tblId}});
};


/*
 * @param rawAction (its payload should contain searchRequest to get source table and histogram parameters)
 * @returns function which loads plot data (x, y, rowIdx, etc.)
 */
export const loadPlotData = function(rawAction) {
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
        const tblData = state[tblId];
        const newTblData = Object.assign({}, tblData, newProps);
        const newState = Object.assign({}, state);
        newState[tblId] = newTblData;
        return newState;
    }
    return state;
}

export function reducer(state=getInitState(), action={}) {
    switch (action.type) {
        case (TablesCntlr.TABLE_NEW)  :
        {
            const {tbl_id, tableMeta, request} = action.payload;
            if (has(state, tbl_id)) {
                if (isTableLoaded(action.payload) && !state[tbl_id].isTblLoaded) {
                    // use xyPlotParams with cleared selection box
                    const prevXyPlotParams = state[tbl_id].xyPlotParams;
                    const xyPlotParams = update(prevXyPlotParams, {selection: {$set: undefined}});
                    action.sideEffect((dispatch) => fetchPlotData(dispatch, request, xyPlotParams));

                    const newState = Object.assign({}, state);
                    newState[request.tbl_id] = {isPlotDataReady: false};
                    return newState;
                }
            }
            return state;
        }
        case (LOAD_PLOT_DATA)  :
        {
            const {xyPlotParams, searchRequest} = action.payload;
            const newState = Object.assign({}, state);
            newState[searchRequest.tbl_id]= {isPlotDataReady: false};
            return newState;
        }
        case (UPDATE_PLOT_DATA)  :
        {
            const {isPlotDataReady, xyPlotData, xyPlotParams, searchRequest} = action.payload;
            return stateWithNewData(searchRequest.tbl_id, state, {
                isPlotDataReady,
                xyPlotData,
                xyPlotParams,
                searchRequest
            });
        }
        case (SET_SELECTION) :
        {
            const {tblId, selection} = action.payload;
            return update(state, {[tblId] : {xyPlotParams: {selection: {$set: selection}}}});
        }
        case (SET_ZOOM) :
        {
            const {tblId, selection} = action.payload;
            return update(state,
                {[tblId] :
                    {xyPlotParams:
                        {
                            selection: {$set: undefined},
                            zoom: {$set: selection}
                         }
                    }});
        }
        case (RESET_ZOOM) :
        {
            const {tblId} = action.payload;
            return update(state,
                {[tblId] :
                    {xyPlotParams:
                        {
                            selection: {$set: undefined},
                            zoom: {$set: undefined}
                         }
                    }});

        }
        case (TablesCntlr.TABLE_SELECT) :
        {
            const {tbl_id, selectInfo} = action.payload;
            if (has(state, tbl_id)) {
                return update(state,
                    {
                        [tbl_id]: {
                            xyPlotParams: {
                                selection: {$set: undefined}
                            }
                        }
                    });
            } else {
                return state;
            }

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

    const decimateInfoCls = new DecimateInfo(xyPlotParams.x.columnOrExpr, xyPlotParams.y.columnOrExpr, 10000);

    // todo support expressions
    const req = Object.assign({}, activeTableServerRequest, {'startIdx' : 0, 'pageSize' : 1000000,
        'inclCols' : xyPlotParams.x.columnOrExpr+','+xyPlotParams.y.columnOrExpr,
        'decimate' : decimateInfoCls.serialize()
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


