import {isObject} from 'lodash';
import {getAppOptions} from '../../core/AppDataCntlr';
import {MetaConst} from '../../data/MetaConst';
import {getMetaEntry, getObjectMetaEntry} from '../../tables/TableUtil';

const dsOps= () => getAppOptions().dataServiceOptions ?? {};

/**
 *
 * @param {String} dataServiceId
 * @return {Object}
 */
export const getDataServiceOptions = (dataServiceId) => dsOps()[dataServiceId] ?? dsOps();

/**
 * @param {String} key - option key
 * @param {String} [dataServiceId]
 * @param [defVal]
 * @return {*}
 */
export function getDataServiceOption(key, dataServiceId = undefined, defVal=undefined) {
    const idOps = dsOps()[dataServiceId] ?? {};
    return idOps[key] ?? dsOps()[key] ?? defVal;
}

/**
 *
 * @param key - option key
 * @param {TableModel|String} tableOrId - parameters accepts the table model or tha table id
 * @param [defVal]
 * @return {*}
 */
export function getDataServiceOptionByTable(key, tableOrId, defVal=undefined) {
    const entry= getObjectMetaEntry(tableOrId, MetaConst.DATA_SERVICE_OPTIONS)?.[key];
    if (entry) return entry;
    return getDataServiceOption(key, getMetaEntry(tableOrId,MetaConst.DATA_SERVICE_ID), defVal);
}

/**
 * Get the `enumConfig` (preassigned order/color palette for an enum-like column, e.g. a photometric
 * band) for a table, if one is configured via dataServiceOptions and applies to the given column.
 * @param {TableModel|String} tableOrId - the table model or table id
 * @param {String} columnName - the column being grouped/enumerated
 * @return {{columnNames: String[], order: String[], palette: Object}|undefined}
 */
export function getEnumConfigForColumn(tableOrId, columnName) {
    const enumConfig = getDataServiceOptionByTable('enumConfig', tableOrId);
    return enumConfig?.columnNames?.includes(columnName) ? enumConfig : undefined;
}

export function getDataServiceOptionsFallback(dataServiceId, hostname) {
    const idOps = dsOps()[dataServiceId];
    if (idOps) return idOps;
    if (!hostname) return dsOps();
    const guessKey = Object.entries(dsOps()).find(([k,v]) => isObject(v) && hostname.includes(k))?.[0];
    return getDataServiceOptions(guessKey);
}
