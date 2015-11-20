/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



const cacheContainer= {};

var SimpleMemCache= {

    addCache(name) {
        cacheContainer[name] = new Map();
    },

    size(name) {
        return cacheContainer[name] ? cacheContainer[name].size : 0;
    },


    clearCache(name) {
        cacheContainer[name] = new Map();
    },


    deleteCache(name) {
        cacheContainer[name] = undefined;
    },


    get(name, key) {
        if (!cacheContainer[name]) this.addCache(name);
        return cacheContainer[name].get(key);
    },


    set(name, key, value) {
        if (!cacheContainer[name]) this.addCache(name);
        return cacheContainer[name].set(key, value);
    }

};

export default SimpleMemCache;
