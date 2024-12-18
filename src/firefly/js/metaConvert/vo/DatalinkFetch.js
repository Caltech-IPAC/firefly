import {isEmpty} from 'lodash';
import {cloneRequest, makeFileRequest} from '../../tables/TableRequestUtil.js';
import {doFetchTable} from '../../tables/TableUtil.js';

let dlTableCache = new Map();
const maxEntries = 30;

function cacheCleanup() {
    if (dlTableCache.size < (maxEntries * 1.25)) return;
    const entries = Array.from(dlTableCache.entries()).sort((e1, e2) => e2[1].time - e1[1].time);
    if (entries.length > maxEntries) entries.length = Math.trunc(maxEntries * .8);
    dlTableCache = new Map(entries);
}

function cacheGet(url) {
    const entry = dlTableCache.get(url);
    if (!entry) return undefined;
    cacheSet(url, entry.table);
    return entry.table;
}

const cacheSet = (url, table) => dlTableCache.set(url, {time: Date.now(), table});

//todo - make version of this that supports concurrent all of same url, with on fetch
export async function fetchDatalinkTable(url, requestOptions={}) {
    const tableFromCache = cacheGet(url);
    if (tableFromCache) return tableFromCache;
    const table= await doMultRequestTableFetch(url, requestOptions);
    cacheSet(url, table);
    cacheCleanup();
    return table;
}



//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

const loadBegin= new Map();
const waitingResolvers= new Map();
const waitingRejectors= new Map();
const LOAD_ERR_MSG='table retrieval fail, unknown reason ';

function clearAll(fetchKey) {
    loadBegin.delete(fetchKey);
    waitingResolvers.delete(fetchKey);
    waitingRejectors.delete(fetchKey);
}

function resolveAll(fetchKey, table) {
    (waitingResolvers.get(fetchKey)??[]).forEach((r) => r(table));
    clearAll(fetchKey);
}

function rejectAll(fetchKey, error) {
    (waitingRejectors.get(fetchKey)??[]).forEach( (r) => r(error));
    clearAll(fetchKey);
}


/**
 * This function supports doing a table fetch with the same url concurrently while only make one call the the server
 * @param url
 * @return {Promise<TableModel>}
 */
async function doMultRequestTableFetch(url, requestOptions) {


    const fetchKey= isEmpty(requestOptions) ? url : url+'--' + JSON.stringify(requestOptions);

    if (!waitingResolvers.has(fetchKey)) waitingResolvers.set(fetchKey,[]);
    if (!waitingRejectors.has(fetchKey)) waitingRejectors.set(fetchKey,[]);

    if (!loadBegin.get(fetchKey)) {
        loadBegin.set(fetchKey, true);
        const request = cloneRequest(makeFileRequest('dl table', url), requestOptions);
        doFetchTable(request)
            .then((table) => table ? resolveAll(fetchKey, table) : rejectAll(fetchKey, Error(LOAD_ERR_MSG)))
            .catch((err) => rejectAll(fetchKey, err));
    }
    return new Promise( function(resolve, reject) {
        waitingResolvers.get(fetchKey).push(resolve);
        waitingRejectors.get(fetchKey).push(reject);
    });
}

