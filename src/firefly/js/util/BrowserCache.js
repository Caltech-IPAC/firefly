/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import ls from 'local-storage';


function makeCacheEntry(time,data,lifeSpanSecs=0) {
    return  {time:Date.now(),data,lifeSpanSecs};
}

const SEP = "___";
const  NO_EXPIRATION= 0;

/**
 * @author Trey Roby
 */


class BrowserCache {
    static isCached(key) { return BrowserCache.get(key) ? true : false; }

    static get(key) {
        var retval= null;
        var entry= ls.get(key);

        if (entry) {
            var lifeSpanMills= entry.lifeSpanSecs * 1000;
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
}

export default BrowserCache;
