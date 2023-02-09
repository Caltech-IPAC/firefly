/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import ls from 'local-storage';


function makeCacheEntry(data,lifeSpanSecs=0) {
    return  {time:Date.now(),data,lifeSpanSecs};
}

const  NO_EXPIRATION= 0;

/**
 * @author Trey Roby
 */
class BrowserCache {
    static isCached(key) { return Boolean(BrowserCache.get(key)); }

    static get(key) {
        let retval= null;
        const entry= ls.get(key);

        if (entry) {
            const lifeSpanMills= entry.lifeSpanSecs * 1000;
            if (lifeSpanMills===NO_EXPIRATION) {
                retval= entry.data;
            }
            else {
                if ((entry.time+lifeSpanMills) > Date.now()) {
                    retval= entry.data;
                }
                else {
                    ls.remove(key);
                }
            }
        }
        return retval;
    }

    static put(key, data, lifespanInSecs= 0) {
        ls.set(key, makeCacheEntry(data, lifespanInSecs));
    }

    static remove(key) {
        ls.remove(key);
    }
}

export default BrowserCache;
