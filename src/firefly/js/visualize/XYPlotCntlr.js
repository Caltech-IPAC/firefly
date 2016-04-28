import {flux} from '../Firefly.js';

import update from 'react-addons-update';
import {get, has, omit, omitBy, isUndefined, isString} from 'lodash';


import {doFetchTable, findTblById} from '../tables/TableUtil.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {serializeDecimateInfo} from '../tables/Decimate.js';
import {logError} from '../util/WebUtil.js';

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
         decimatedUnzoomed: boolean // tells that unzoomed data are decimated
         xyPlotData: {
                    rows: [[x: string, y: string, rowIdx: string]*] ,
                    decimateKey: string,
                    xMin: string,
                    xMax: string,
                    yMin: string,
                    yMax: string,
                    weightMin: string,
                    weightMax: string,
                    idStr: string
         }
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
export function dispatchLoadPlotData(xyPlotParams, searchRequest) {
    flux.process({type: LOAD_PLOT_DATA, payload: {xyPlotParams, searchRequest}});
}

/*
 * Update xy plot data
 * @param {boolean} isPlotDataReady - flags that xy plot data are available
 * @param {Number[][]} xyPlotData - an array of the number arrays with rowIdx, x, y, [error]
 * @param {Object} xyPlotParams - XY plot options (column names, etc.)
 * @param {String} tblId - table id
 */
export function dispatchUpdatePlotData(isPlotDataReady, xyPlotData, xyPlotParams, tblId) {
    flux.process({type: UPDATE_PLOT_DATA, payload: {isPlotDataReady, xyPlotData, xyPlotParams, tblId}});
}

export function dispatchSetSelection(tblId, selection) {
    flux.process({type: SET_SELECTION, payload: {tblId, selection}});
}

export function dispatchSetZoom(tblId, selection) {
    flux.process({type: SET_ZOOM, payload: {tblId, selection}});
}

export function dispatchResetZoom(tblId) {
    flux.process({type: RESET_ZOOM, payload: {tblId}});
}


/*
 * @param rawAction (its payload should contain searchRequest to get source table and histogram parameters)
 * @returns function which loads plot data (x, y, rowIdx, etc.)
 */
export function loadPlotData (rawAction) {
    return (dispatch) => {
        dispatch({ type : LOAD_PLOT_DATA, payload : rawAction.payload });
        if (rawAction.payload.searchRequest && rawAction.payload.xyPlotParams) {
            fetchPlotData(dispatch, rawAction.payload.searchRequest, rawAction.payload.xyPlotParams);
        }

    };
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

export function reducer(state={}, action={}) {
    switch (action.type) {
        case (TablesCntlr.TABLE_NEW_LOADED)  :
        {
            const {tbl_id, request} = action.payload;
            if (has(state, tbl_id)) {
                // use xyPlotParams with cleared selection box
                const prevXyPlotParams = state[tbl_id].xyPlotParams;
                const xyPlotParamsNext = has(prevXyPlotParams, 'selection') ?
                    update(prevXyPlotParams, {selection: {$set: undefined}}) : prevXyPlotParams;
                action.sideEffect((dispatch) => fetchPlotData(dispatch, request, xyPlotParamsNext));
            }
            return state;
        }
        case (TablesCntlr.TABLE_REMOVE)  :
        {
            const {tbl_id} = action.payload.tbl_id;
            if (has(state, tbl_id)) {
                const newState = Object.assign({}, state);
                Reflect.deleteProperty(newState, [tbl_id]);
                return newState;
            }
            return state;
        }
        case (LOAD_PLOT_DATA)  :
        {
            const {searchRequest} = action.payload;
            const tblId = searchRequest.tbl_id;
            const newState = Object.assign({}, state);
            newState[tblId]= {isPlotDataReady: false};
            // we need to know if the original unzoomed plot was decimated
            if (state[tblId]) {
                newState[tblId].decimatedUnzoomed = state[tblId].decimatedUnzoomed;
            }
            return newState;
        }
        case (UPDATE_PLOT_DATA)  :
        {
            const {isPlotDataReady, decimatedUnzoomed, xyPlotData, xyPlotParams, tblId} = action.payload;
            const decimatedUnzoomedNext = isUndefined(decimatedUnzoomed) ? state[tblId].decimatedUnzoomed : decimatedUnzoomed;
            return stateWithNewData(tblId, state, {
                isPlotDataReady,
                decimatedUnzoomed: decimatedUnzoomedNext,
                xyPlotData,
                xyPlotParams
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
            const newState = update(state,
                {[tblId] :
                    {xyPlotParams:
                        {
                            selection: {$set: undefined},
                            zoom: {$set: selection}
                         }
                    }});
            if  (get(newState[tblId], 'xyPlotData.decimateKey')) {
                const nextPlotParams = newState[tblId].xyPlotParams;
                newState[tblId] = {plotDataReady: false, decimatedUnzoomed: state[tblId].decimatedUnzoomed};
                const request = findTblById(tblId).request;
                action.sideEffect((dispatch) => fetchPlotData(dispatch, request, nextPlotParams));
            }
            return newState;
        }
        case (RESET_ZOOM) :
        {
            const tblId = action.payload.tblId;
            const newState = update(state,
                {[tblId] :
                    {xyPlotParams:
                        {
                            selection: {$set: undefined},
                            zoom: {$set: undefined}
                        }
                    }});
            if  (state[tblId].decimatedUnzoomed || isUndefined(state[tblId].decimatedUnzoomed)) {
                const nextPlotParams = newState[tblId].xyPlotParams;
                newState[tblId] = {plotDataReady: false};
                const request = findTblById(tblId).request;
                action.sideEffect((dispatch) => fetchPlotData(dispatch, request, nextPlotParams));
            }
            return newState;

        }
        case (TablesCntlr.TABLE_SELECT) :
        {
            const tbl_id = action.payload.tbl_id; //also has selectInfo
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

    if (!xyPlotParams) { return; }

    let limits = [];
    if (xyPlotParams.zoom) {
        const {xMin, xMax, yMin, yMax}  = xyPlotParams.zoom;
        limits = [xMin, xMax, yMin, yMax];
    }

    // todo support expressions
    const req = Object.assign({}, omit(activeTableServerRequest, ['tbl_id', 'META_INFO']), {
        'startIdx' : 0,
        'pageSize' : 1000000,
        //'inclCols' : `${xyPlotParams.x.columnOrExpr},${xyPlotParams.y.columnOrExpr}`, // ignored if 'decimate' is present
        'decimate' : serializeDecimateInfo(xyPlotParams.x.columnOrExpr, xyPlotParams.y.columnOrExpr, 10000, 1.0, ...limits)
    });

    const tblId = activeTableServerRequest.tbl_id;
    req.tbl_id = 'xyplot-'+tblId;


    doFetchTable(req).then(
        (tableModel) => {
            if (tableModel.tableData && tableModel.tableData.data) {
                const {tableData, tableMeta} = tableModel;
                const xyPlotData = omitBy({
                    rows: tableData.data,
                    decimateKey: tableMeta['decimate_key'],
                    xMin: tableMeta['decimate.X-MIN'],
                    xMax: tableMeta['decimate.X-MAX'],
                    yMin: tableMeta['decimate.Y-MIN'],
                    yMax: tableMeta['decimate.Y-MAX'],
                    weightMin: tableMeta['decimate.WEIGHT-MIN'],
                    weightMax: tableMeta['decimate.WEIGHT-MAX'],
                    idStr: tableMeta['tbl_id']
                }, isUndefined);

                // convert strings with numbers into numbers
                Object.keys(xyPlotData).forEach( (prop) => {
                    const val = xyPlotData[prop];
                    if (isString(val) && isFinite(val)) { xyPlotData[prop] = Number(val); }
                });

                dispatch(updatePlotData(
                    {
                        isPlotDataReady : true,
                        // when zoomed, we don't know if the unzoomed data are decimated or not
                        decimatedUnzoomed: Boolean(tableMeta['decimate_key']) || (xyPlotParams.zoom ? undefined : false),
                        xyPlotParams,
                        xyPlotData,
                        tblId
                    }));
            }
        }
    ).catch(
        (reason) => {
            logError(`Failed to fetch XY plot data: ${reason}`);
        }
    );

}


