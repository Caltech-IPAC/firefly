import {makeFileRequest} from '../../tables/TableRequestUtil.js';
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
export async function fetchDatalinkTable(url) {
    const tableFromCache = cacheGet(url);
    if (tableFromCache) return tableFromCache;
    // const request = makeFileRequest('dl table', url);
    // const table = await doFetchTable(request);
    const table= await doMultRequestTableFetch(url);
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

function clearAll(url) {
    loadBegin.delete(url);
    waitingResolvers.delete(url);
    waitingRejectors.delete(url);
}

/**
 * This function supports doing a table fetch with the same url concurrently while only make one call the the server
 * @param url
 * @return {Promise<TableModel>}
 */
async function doMultRequestTableFetch(url) {

    if (!waitingResolvers.has(url)) waitingResolvers.set(url,[]);
    if (!waitingRejectors.has(url)) waitingRejectors.set(url,[]);

    if (!loadBegin.get(url)) {
        loadBegin.set(url,true);
        const request = makeFileRequest('dl table', url);
        doFetchTable(request).then( (table) => {
            if (table) {
                (waitingResolvers.get(url)??[]).forEach((r) => r(table));
            } else {
                (waitingRejectors.get(url)??[]).forEach( (r) => r(Error(LOAD_ERR_MSG)));
            }
            clearAll(url);
        }).catch( (err) => {
            (waitingRejectors.get(url)??[]).forEach( (r) => r(err));
            clearAll(url);
        });
    }
    return new Promise( function(resolve, reject) {
        waitingResolvers.get(url).push(resolve);
        waitingRejectors.get(url).push(reject);
    });
};

