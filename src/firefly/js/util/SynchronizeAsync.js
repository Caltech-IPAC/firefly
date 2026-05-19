/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * !!!!!!! IMPORTANT !!!!!!!!!!!
 * Should try to keep imports limited. Since it is could be used in workers. It should not import anything from
 * firefly. It may do import of lodash but right now it is unnecessary
 */

const waitingPromises= new Map();


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

    if (waitingPromises.has(pKey)) return waitingPromises.get(pKey);

    const promise= asyncFunction();
    waitingPromises.set(pKey, promise);
    const clear= () => waitingPromises.delete(pKey);
    promise
        .then(() => clear())
        .catch(() => clear());
    return promise;
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
export const blockWhileAsyncIdWaiting= (pKey) =>
    waitingPromises.has(pKey) ? waitingPromises.get(pKey) : Promise.resolve();
