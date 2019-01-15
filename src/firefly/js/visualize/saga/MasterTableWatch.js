import {get} from 'lodash';
import { TABLE_SEARCH } from '../../tables/TablesCntlr';
import {dispatchAddActionWatcher} from '../../core/MasterSaga';
import {onTableLoaded} from '../../tables/TableUtil';

/**
 * @global
 * @public
 * @typedef {Object} TableWatcherDef
 *
 * @summary Define a table watch definition
 *
 * @param {Function} startWatcher(table)
 * @param {Object} watcherData
 * @param {Function} testTable  - testTable(table) return 'watch' 'exclusiveWatch' or false
 *
 *
 *
 */

/**
 * Start the master table Watcher
 * @param {Array.<TableWatcherDef>} tableWatcherDefList
 */
export function startMasterTableWatcher(tableWatcherDefList) {


    const actions = [TABLE_SEARCH];

    dispatchAddActionWatcher({
        actions,
        id: 'masterTableWatcher',
        callback: masterTableWatcher,
        params: {tableWatcherDefList}});

}

function masterTableWatcher(action, cancelSelf, params) {

    const {tableWatcherDefList}= params;
    const tbl_id = get(action.payload, 'request.tbl_id');
    if (!tbl_id) return;

    switch (action.type) {
        case TABLE_SEARCH:
            onTableLoaded(tbl_id).then( (table) =>
                !table.error &&  evaluateTableAndStartWatchers(table, tableWatcherDefList));
            break;

    }

}


function evaluateTableAndStartWatchers(table, tableWatcherDefList) {

    console.log(`new loaded table: ${table.tbl_id}`);
}
