/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from './ReduxFlux';
import {take, fork, spawn, cancel} from 'redux-saga/effects';
import {isEmpty, get, isFunction, isUndefined, union, isArray, pick} from 'lodash';

import {uniqueID} from '../util/WebUtil.js';
import {Logger} from '../util/Logger.js';
import {TABLE_SEARCH} from '../tables/TablesCntlr.js';
import {getTblIdsByGroup, onTableLoaded} from '../tables/TableUtil';
import {TABLE_FETCH, TABLE_REMOVE} from '../tables/TablesCntlr';

export const ADD_SAGA= 'MasterSaga.addSaga';
export const ADD_ACTION_WATCHER= 'MasterSaga.addActionWatcher';
export const CANCEL_ACTION_WATCHER= 'MasterSaga.cancelActionWatcher';
export const CANCEL_ALL_ACTION_WATCHER= 'MasterSaga.cancelAllActionWatcher';
export const ADD_TABLE_TYPE_WATCHER= 'MasterSaga.addTableTypeWatcher';
export const EDIT_TABLE_TYPE_WATCHER= 'MasterSaga.editTableTypeWatcher';

const logger = Logger('MasterSaga');

/**
 * 
 * @param {generator} saga a generator function that uses redux-saga
 * @param {{}} params this object is passed the to sega as the first parrmeter
 */
export function dispatchAddSaga(saga, params={}) {
    flux.process({ type: ADD_SAGA, payload: { saga,params}});
}

/**
 * Action watcher callback.
 * @callback actionWatcherCallback
 * @param {Action} action - the triggering action
 * @param {function} cancelSelf - a function to cancel this watcher
 * @param {object} params - params passed through dispatchAddActionWatcher or (if not undefined) the last returned value from the watcher callback
 * @param {function} dispatch: flux's dispatcher
 * @param {function} getState: flux's getState function
 */

/**
 * @param {object}   p
 * @param {string}   [p.id]     a unique identifier for this watcher.  This is needed for dispatchCancel*.
 *                              When not given, a unique ID will be created.  You can still cancel this watcher via
 *                              callback's cancelSelf function.
 * @param {string[]} p.actions  an array of action types to watch
 * @param {actionWatcherCallback} p.callback a callback function to handle the action(s).
 *                                It is called with
 *                                (action:Action, cancelSelf:Function, params:Object, dispatch:Function, getState:Function).
 *                                {Action} action: the triggered action
 *                                {Function} cancelSelf: a function to cancel this watcher
 *                                {Object} params: the given params object, it the watcher callback returns a value
 *                                        then on the next call the last returned value
 *                                {Function} dispatch: flux's dispatcher
 *                                {Function} getState: flux's getState function
 * @param {Object} p.params   a pass-along parameter object to be sent when callback is called.
 */
export function dispatchAddActionWatcher({id, actions, callback, params={}}) {
    flux.process({ type: ADD_ACTION_WATCHER, payload: {id, actions, callback, params}});
}

/**
 * @global
 * @public
 * @name TableWatchFunc
 * @function
 * @summary Table watcher function
 * @description Like a normal watch with following exceptions:
 * <ul>
 * <li> one started per table
 * <li> tbl_id is first parameter.
 * <li> The params object with always include a options and a sharedData property. They may be undefined.
 * <li> The options object should be thought of as read only. It should not be modified.
 * <li> It is call the first time with the action undefined.  This can be for initialization.  The table will beloaded
 * </ul>
 * @param {String} tbl_id the table id
 * @param {Action} action - the action
 * @param {Function} cancelSelf - function to cancel, should be call when table is removed
 * @param {Object} params - params contents are created by the watcher with two exceptions, params will always contain:
 *          <ul>
 *          <li> options - The options object is the same as passed to dispatchAddTableTypeWatcherDef
 *          <li> sharedData - if the definition contains a shared data object it will be passed. sharedData provides a way
 *           for all the watcher with the same definition to share some state. This should only be used if absolutely necessary
 *          </ul>
 */

/**
 * @global
 * @public
 * @name TestWatchFunc
 * @function
 * @desc Test to see if we should watch this table
 * @param {TableModel} table
 * @param {Action|undefined} action
 * @param {Object|undefined} options
 * @return {boolean} true if we should watch
 *
 */

/**
 * @global
 * @public
 * @typedef {Object} TableWatcherDef
 *
 * @prop {String} p.id a unique identifier for this watcher
 * @prop {TestWatchFunc} p.testTable - function: testTable(table,action,options), return true, if we should watch this table type
 * @prop {TableWatchFunc} p.watcher - function watcher(tbl_id,action,cancelSelf,params), note- TableWatchFunc is called
 *                        when the first time for initialization for the first call action is null
 * @prop {Object} [p.sharedData]
 * @prop {Object} [p.options]
 * @prop {Array.<String>} p.actions - array of string action id's
 * @prop {Array.<String>} p.excludes- excluded id list. if testTable return true then this
 *                                       list will force any watcher def with an id in the list to not watch
 * @prop {boolean} p.stopPropagation - like excludes but if true not only watcher will be added
 * @prop {boolean} p.enabled - if true this TableTypeWatcher will test and add, if false it will be skipped
 * @prop {boolean} p.allowMultiples - multiple defs of this type are allowed.
 *
 */



/**
 * @summary Add a table type watcher definition. Test every table (existing or new) and if the test is passed then create
 * a table type watcher for that table.
 *
 * @description A table type watcher, by using the payload definition will start a single watch for ever table that is newly loaded
 * or previously loaded that passes the 'testTable' function test.
 * These watchers will have the same life time of the table they are watching.
 * A  TableType watcher is like a normal watcher with following exceptions:
 * <ul>
 * <li> tbl_id is first parameter.
 * <li> The params object with always include a options and a sharedData property. They may be undefined.
 * <li> The options object should be thought of as read only. It should not be modified.
 * <li> It is call the first time with the action undefined.  This can be for initialization.  The table will beloaded
 * </ul>
 *
 * @param {TableWatcherDef} def
 *
 * @see TableWatchFunc
 * @see TestWatchFunc
 *
 * @public
 * @function dispatchAddTableTypeWatcherDef
 * @memberof firefly.action
 */
export function dispatchAddTableTypeWatcherDef({id, actions, excludes= [], testTable= ()=>true,
                                             watcher, options={}, enabled= true, stopPropagation= false,
                                             sharedData, allowMultiples}) {
    flux.process({
        type: ADD_TABLE_TYPE_WATCHER,
        payload: {id, actions, excludes, testTable, watcher, options, enabled, stopPropagation, sharedData,allowMultiples}
    });
}

/**
 * Change any key (but id) defined in dispatchAddTableTypeWatcherDef
 * @param {object} p
 * @param p.id
 * @param p.changes any key that you want to change
 */
export function dispatchEditTableTypeWatcherDef({id, ...changes}) {
    flux.process({ type: EDIT_TABLE_TYPE_WATCHER, payload:{id, changes}});
}


/**
 * cancel the watcher with the given id.
 * @param {string} id  a unique identifier of the watcher to cancel
 */
export function dispatchCancelActionWatcher(id) {
    flux.process({ type: CANCEL_ACTION_WATCHER, payload: {id}});
}


/**
 * Cancel all watchers.  Should only be called during re-init scenarios. 
 */
export function dispatchCancelAllActionWatchers() {
    flux.process({ type: CANCEL_ALL_ACTION_WATCHER});
}



/**
 * This saga launches all the predefined Sagas then loops and waits for any ADD_SAGA actions and launches those Segas
 */
export function* masterSaga() {
    let watchers = {};

    // Start a saga from any action
    while (true) {
        const action= yield take([ADD_SAGA, ADD_ACTION_WATCHER, ADD_TABLE_TYPE_WATCHER, EDIT_TABLE_TYPE_WATCHER,
                                  CANCEL_ACTION_WATCHER, CANCEL_ALL_ACTION_WATCHER]);

        switch (action.type) {
            case ADD_SAGA: {
                const {getState, dispatch}= flux.getRedux();
                const {saga,params}= action.payload;
                // with fork every exception will bubble up from the child to the parent:
                // an unhandled exception in one saga will cancel all sibling sagas
                // with spawn, only the saga with the unhandled error will be cancelled
                // the unhandled errors are caught by middleware and logged to console
                if (isFunction(saga)) {
                    yield spawn(saga, params, dispatch, getState);
                } else {
                    console.error('Can not add saga: callback must be a generator function');
                }
                break;
            }
            case ADD_ACTION_WATCHER: {
                const {getState, dispatch}= flux.getRedux();
                const {actions, callback, params}= action.payload;
                if (actions && isFunction(callback)) {
                    const {id=callback.name+uniqueID()}= action.payload;
                    if (watchers[id]) {
                        yield cancel(watchers[id]);
                    }
                    const watcherSaga = createWatcherSaga({id, actions, callback, params, dispatch, getState});
                    const task = yield fork(watcherSaga, dispatch, getState);
                    watchers[id] = task;
                    logger.info(`watcher ${id} added.  #watcher: ${Object.keys(watchers).length}`);
                } else {
                    logger.error('Can not create action watcher: invalid actions or callback');
                }
                break;
            }
            case CANCEL_ACTION_WATCHER: {
                const {id}= action.payload;
                const task = watchers[id];
                if (task) {
                    yield cancel(task);
                    Reflect.deleteProperty(watchers, id);
                    logger.info(`watcher ${id} cancelled.  #watcher: ${Object.keys(watchers).length}`);
                }
                break;
            }
            case CANCEL_ALL_ACTION_WATCHER: {
                const ids = Object.keys(watchers);
                for (let i = 0; i < ids.length; i++) {
                    const task = watchers[ids[i]];
                    yield cancel(task);
                }
                watchers = {};
                setTimeout(initTTWatcher, 1);
                break;
            }
            case ADD_TABLE_TYPE_WATCHER: {
                addTableTypeWatcherDef(action.payload);
                break;
            }
            case EDIT_TABLE_TYPE_WATCHER: {
                const {changes,id}= action.payload;
                editTTWatcherDef(id,changes, Object.keys(watchers));
                break;
            }
        }
    }
}


function createWatcherSaga({id, actions=[], callback, params, dispatch, getState}) {
    const cancelSelf = ()=> dispatch({ type: CANCEL_ACTION_WATCHER, payload: {id}});
    const saga = function* () {

        let prevParams= params;
        let returnedParams;

        // loop exits when saga is cancelled
        while (true) {
            const action = yield take(actions);
            try {
                // the same callback can return modified parameters or undefined
                // when undefined is returned use previous parameters
                returnedParams = callback(action, cancelSelf, prevParams, dispatch, getState);
                if (!isUndefined(returnedParams)) {
                    prevParams = returnedParams;
                }
            } catch (e) {
                console.log(`Encounter error while executing watcher: ${id}  error: ${e}`);
                console.log(e);
            }
        }
    };
    return saga;
}


const TTW_PREFIX= 'tableWatch-';
let ttWatcherDefList= [];
const getTTWatcherDefList= () => ttWatcherDefList;
const insertTTWatcherDef= (def) => def && ttWatcherDefList.push(def);

function editTTWatcherDef(id, changes, watchersIdList) {
    if (!id) return;
    ttWatcherDefList= ttWatcherDefList.map( (def) => {
        if (def.id!==id) return def;
        const obj= pick(changes, ['testTable', 'watcher', 'sharedData', 'options',
                                  'actions', 'excludes', 'stopPropagation', 'enabled']);
        return {...def, ...obj};
    });
    watchersIdList
        .filter( (wId) => wId.indexOf(`${TTW_PREFIX}${id}`)===0)
        .forEach( (wId) => dispatchCancelActionWatcher(wId));

    retroactiveTTStart(ttWatcherDefList.find( (def) => def.id===id ));
}

function addTableTypeWatcherDef(def) {
    setTimeout(() => {
        if (isEmpty(getTTWatcherDefList())) initTTWatcher();
        // validate and start
        if (!def.allowMultiples && ttWatcherDefList.find( (d) => d.id===def.id)) return;
        if (isFunction(def.watcher) && isArray(def.actions) && def.id) insertTTWatcherDef(def);
        else console.error('TableTypeWatcher: watcher, actions, and id are required.');
        retroactiveTTStart(def);
    }, 1);
}

function retroactiveTTStart(def) {
    const idAry= getTblIdsByGroup();
    idAry.forEach( (tbl_id) => startTableTypeWatchersForTable(tbl_id,null, () => [def]));
}

const initTTWatcher= () =>
    dispatchAddActionWatcher(
        {
            actions: [TABLE_SEARCH,TABLE_FETCH,TABLE_REMOVE],
            id: 'masterTableTypeWatcher',
            callback: masterTableTypeWatcher,
            params: {getTTWatcherDefList}
        });



/**
 * watcher - for TABLE_FETCH
 * @param action
 * @param cancelSelf
 * @param params
 */
function masterTableTypeWatcher(action, cancelSelf, params) {
    const tbl_id = get(action, 'payload.request.tbl_id');
    if (!tbl_id || action.type!==TABLE_FETCH) return;
    startTableTypeWatchersForTable(tbl_id,action,params.getTTWatcherDefList);
}


// function masterTableTypeWatcher(action, cancelSelf, params) {
//     const tbl_id = get(action, 'payload.request.tbl_id');
//     if (!tbl_id) return params;
//
//     let {idList=[]}= params;
//     const {getTTWatcherDefList}= params;
//     console.log(`>>>>>> got ${action.type} for ${tbl_id}`);
//     switch (action.type) {
//         case TABLE_SEARCH:
//         case TABLE_FETCH:
//             if (!idList.includes(tbl_id)) {
//                 startTableTypeWatchersForTable(tbl_id,action,getTTWatcherDefList);
//                 idList= [...idList, tbl_id];
//             }
//             break;
//         case TABLE_REMOVE:
//             idList= idList.filter( (id) => id!==tbl_id);
//             break;
//     }
//     return {...params,idList};
// }

function startTableTypeWatchersForTable(tbl_id, action, getDefList) {
    onTableLoaded(tbl_id).then( (table) => {
        table= get(table,'tableModel',table);
        if (!table || table.error ||  !table.totalRows) return;
        logger.info(`new loaded table: ${tbl_id}`);
        const defList= getDefList();
        let excludeList=  [];
        let stopProp= false;
        defList.forEach( (d) => {
            const {id, sharedData, options, stopPropagation, enabled= true, excludes=[], actions}= d;
            if (stopProp || !enabled || excludeList.includes(id)) return;
            if (d.testTable(table, action, options)) {
                stopProp= stopPropagation;
                excludeList= union(excludeList, excludes);
                let abort= false;
                const initParams= d.watcher(tbl_id, undefined, ()=> (abort=true), {sharedData, options});
                if (abort) return;
                dispatchAddActionWatcher({
                    id:`${TTW_PREFIX}${id}-${tbl_id}`,
                    actions,
                    callback: (action,cancelSelf,params) => {
                        return d.watcher(tbl_id, action, cancelSelf, {...params, sharedData, options});
                    },
                    params:initParams});
            }
        });
    });
}

