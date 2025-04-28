import {isObject} from 'lodash';
import {getAppOptions} from '../../core/AppDataCntlr';
import {MetaConst} from '../../data/MetaConst';
import {getMetaEntry, getObjectMetaEntry} from '../../tables/TableUtil';
import {getServiceMetaOptions} from './SiaUtil';

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

export function getDataServiceOptionsFallback(dataServiceId, hostname) {
    const idOps = dsOps()[dataServiceId];
    if (idOps) return idOps;
    if (!hostname) return dsOps();
    const guessKey = Object.entries(dsOps()).find(([k,v]) => isObject(v) && hostname.includes(k))?.[0];
    return getDataServiceOptions(guessKey);
}
