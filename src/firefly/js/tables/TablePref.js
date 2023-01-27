/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import md5 from 'md5';
import {getColumns, getTableUiById, getTblById} from './TableUtil.js';
import BrowserCache from '../util/BrowserCache.js';
import {dispatchTableUiUpdate} from './TablesCntlr.js';
import {getModuleName} from 'firefly/util/WebUtil.js';

export const PREF_KEY = 'tbl_pref_key';
export const PREF_APPLIED = 'prefApplied';

/**
 *
 * @param tbl_ui_id
 * @param uiData
 * @return {boolean} returns true if column preferences were applied
 */
export function applyTblPref(tbl_ui_id, uiData) {
    const prefKey = getTblPrefKey(tbl_ui_id);
    if (!prefKey) return false;

    const cached = BrowserCache.get(prefKey);
    if (cached?.colPref) {
        const colPref = new Map(JSON.parse(cached.colPref));
        uiData.columns?.filter((c) => c.visibility !== 'hidden')
            .forEach((c) => {
                if (colPref.has(c.name))    c.visibility = colPref.get(c.name);
            });
        uiData[PREF_APPLIED] = true;
    }
    return uiData[PREF_APPLIED];
}

/**
 * set the visible columns in this table as the preferred visible columns
 * @param {string} tbl_ui_id
 */
export function setTblPref(tbl_ui_id) {
    const prefKey = getTblPrefKey(tbl_ui_id);
    if (!prefKey) return;

    const {columns=[]} = getTableUiById(tbl_ui_id) || {};
    const colPref = JSON.stringify(columns.map((c) => [c.name, c.visibility || 'show']));  //

    BrowserCache.put(prefKey, {colPref});
}

/**
 * clear this table preferences
 * @param {string} tbl_ui_id
 */
export function clearTblPref(tbl_ui_id) {
    const prefKey = getTblPrefKey(tbl_ui_id);
    if (!prefKey) return;

    BrowserCache.remove(prefKey);
    dispatchTableUiUpdate({tbl_ui_id, [PREF_APPLIED]:false});
}

/**
 * @param tbl_ui_id
 * @return {string} the key used to get column's preferences for the given table.  If a PREF_KEY is not
 * in TableMeta, then return undefined to indicate Preferences is not used for this table.
 */
export function getTblPrefKey(tbl_ui_id) {
    const {tbl_id} = getTableUiById(tbl_ui_id) || {};
    const tableModel = getTblById(tbl_id);
    if (!tableModel) return undefined;

    const prefKey = tableModel.tableMeta?.[PREF_KEY];
        // disable default impl.  will only apply to table with PREF_KEY
        // md5(getColumns(tableModel)
        //     .map( (c) => c.name)
        //     .join('|')
        // );
    return prefKey && `colPref-${getModuleName()}-${prefKey}`;
}


