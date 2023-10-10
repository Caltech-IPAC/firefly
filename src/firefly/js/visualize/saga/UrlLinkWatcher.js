/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {once} from 'lodash';
import {TABLE_LOADED} from '../../tables/TablesCntlr.js';
import {findTableAccessURLColumn} from '../../voAnalyzer/TableAnalysis.js';
import {getTblById} from '../../tables/TableUtil.js';
import {dispatchTableUpdate} from '../../tables/TablesCntlr.js';
import {clone} from '../../util/WebUtil.js';


//TODO: for testTable, more logic could be added if the API of setting table meta for any link-like column is supported
/** @type {TableWatcherDef} */
export const getUrlLinkWatcherDef= once(() =>  ({
    id : 'URLLinkWatcher',
    watcher : watchURLLinkColumns,
    testTable : (table) => findTableAccessURLColumn(table),
    allowMultiples: false,
    actions: [TABLE_LOADED]
}));



/**
 * Action watcher callback: watch the tables with access_url column and update table column attributes for
 * table rendering which displays access_url column as a link.
 * @param tbl_id
 * @param action
 * @param cancelSelf
 * @param params
 * @return {*}
 */
export function watchURLLinkColumns(tbl_id, action, cancelSelf, params) {
    if (!action) {
        return params;
    }
    const {payload}= action;
    if (payload.tbl_id && payload.tbl_id!==tbl_id) return params;

    switch (action.type) {
        case TABLE_LOADED:
            handleLinkColumnUpdate(tbl_id);
            break;
    }
    return params;
}

function handleLinkColumnUpdate(tbl_id) {
    const tbl = getTblById(tbl_id);
    const linkCol = findTableAccessURLColumn(tbl_id);

    if (linkCol && (!linkCol.type || linkCol.type === 'char')) {
        linkCol.type = 'location';
        //linkCol.links = [ { }];
        dispatchTableUpdate(clone(tbl));
    }
}
