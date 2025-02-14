import {isEmpty} from 'lodash';
import {cloneRequest, makeFileRequest} from '../../tables/TableRequestUtil.js';
import {doFetchTable} from '../../tables/TableUtil.js';
import {synchronizeAsyncFunctionById} from '../../util/SynchronizeAsync';

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