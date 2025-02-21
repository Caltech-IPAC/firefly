/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * !!!!!!! IMPORTANT !!!!!!!!!!!
 * Should try to keep imports limited. Since it is could be used in workers. It should not import anything from
 * firefly. It may do import of lodash but right now it is unnecessary
 */


const loadBegin= new Map();
const waitingResolvers= new Map();
const waitingRejectors= new Map();
const EMPTY_RESULT_ERR_MSG='promise failed, result was undefined';

function clearAll(pKey) {
    loadBegin.delete(pKey);
    waitingResolvers.delete(pKey);
    waitingRejectors.delete(pKey);
}

function resolveAll(pKey, result) {
    (waitingResolvers.get(pKey)??[]).forEach((r) => r(result));
    clearAll(pKey);
}

function rejectAll(pKey, error) {
    (waitingRejectors.get(pKey)??[]).forEach( (r) => r(error));
    clearAll(pKey);
}


/**
 * This function supports doing multiple of the same call and forcing them to all take the first calls results.
 * Any call that has the same pKey is considered the same call
 * This function is best used when your code is calling something more than one time with the same data very close together.
 * It will guarantee that only one call if made and all the calls (with the same pKey) will get the same result.
 *
 * @param {string} pKey - the promise key for this call. This first call with the key will be the result of every call that is made while the first is running
 * @param {Function} asyncFunction - a functino that returns a promise. This is the function that does the work
 * @return {Promise<unknown>}
 */
export function synchronizeAsyncFunctionById(pKey, asyncFunction) {

    if (!waitingResolvers.has(pKey)) waitingResolvers.set(pKey,[]);
    if (!waitingRejectors.has(pKey)) waitingRejectors.set(pKey,[]);

    if (!loadBegin.get(pKey)) {
        loadBegin.set(pKey, true);
        asyncFunction()
            .then((result) => result!==undefined ? resolveAll(pKey, result) : rejectAll(pKey, Error(EMPTY_RESULT_ERR_MSG)))
            .catch((err) => rejectAll(pKey, err));
    }
    return new Promise( function(resolve, reject) {
        waitingResolvers.get(pKey).push(resolve);
        waitingRejectors.get(pKey).push(reject);
    });
}


/**
 * If a call is running associated the pKey then return a promise for the results. If no call is running then return
 * a Promise.resolve().
 * using async/await this call can be used to block until the promise fulfils
 * <code>
 *    await blockWhileAsyncIdWaiting('someKey');
 * </code>
 *
 * @param {string} pKey
 * @return {Promise<unknown>}
 */
export function blockWhileAsyncIdWaiting(pKey) {
    if (!loadBegin.get(pKey)) return Promise.resolve();
    return new Promise( function(resolve, reject) {
        waitingResolvers.get(pKey).push(resolve);
        waitingRejectors.get(pKey).push(reject);
    });
}