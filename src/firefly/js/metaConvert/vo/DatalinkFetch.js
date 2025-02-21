import {isEmpty} from 'lodash';
import {cloneRequest, makeFileRequest} from '../../tables/TableRequestUtil.js';
import {doFetchTable} from '../../tables/TableUtil.js';
import {synchronizeAsyncFunctionById} from '../../util/SynchronizeAsync';
import { getObsCoreAccessURL, isFormatDataLink, isObsCoreLike } from '../../voAnalyzer/TableAnalysis';
import {getTableModel} from '../../voAnalyzer/VoCoreUtils';
import {getDataLinkData, getServiceDescriptors, isDataLinkServiceDesc} from '../../voAnalyzer/VoDataLinkServDef';

let dlTableCache = new Map();
const maxEntries = 30;

function cacheCleanup() {
    if (dlTableCache.size < (maxEntries * 1.25)) return;
    const entries = Array.from(dlTableCache.entries()).sort((e1, e2) => e2[1].time - e1[1].time);
    if (entries.length > maxEntries) entries.length = Math.trunc(maxEntries * .8);
    dlTableCache = new Map(entries);
}

function cacheGet(fetchKey) {
    const entry = dlTableCache.get(fetchKey);
    if (!entry) return undefined;
    cacheSet(fetchKey, entry.table);
    return entry.table;
}

const cacheSet = (fetchKey, table) => dlTableCache.set(fetchKey, {time: Date.now(), table});

export async function fetchDatalinkTable(url, requestOptions={}) {
    const fetchKey= isEmpty(requestOptions) ? url : url+'--' + JSON.stringify(requestOptions);
    const tableFromCache = cacheGet(fetchKey);
    if (tableFromCache) return tableFromCache;
    const table= await doMultRequestTableFetch(fetchKey, url, requestOptions);
    cacheSet(fetchKey, table);
    cacheCleanup();
    return table;
}

async function doMultRequestTableFetch(fetchKey, url, requestOptions) {
    const request = cloneRequest(makeFileRequest('dl table', url), requestOptions);
    const table= await synchronizeAsyncFunctionById(fetchKey, () => doFetchTable(request));
    return table;
}

export async function fetchSemanticList(tableOrId,row=0) {
    const table= getTableModel(tableOrId);
    if (!table) return [];
    let url;
    if (isObsCoreLike(table) || isFormatDataLink(table,row)) {
        url= getObsCoreAccessURL(table,row);
    }
    else {
        const dlDescriptor= getServiceDescriptors(table)?.filter( (dDesc) => isDataLinkServiceDesc(dDesc))[0];
        url= dlDescriptor?.accessURL;
    }
    if (!url) return [];
    return await fetchDatalinkTableSemanticList(url);
}

export async function fetchDatalinkTableSemanticList(url, requestOptions={}) {
    const dlTable= await fetchDatalinkTable(url,requestOptions);
    const dataLinkData= getDataLinkData(dlTable);
    return [...dataLinkData.reduce((semSet,{semantics}) => {
        semSet.add(semantics);
        return semSet;
    },new Set())];
}