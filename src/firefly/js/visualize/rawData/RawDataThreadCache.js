import {Band} from '../Band.js';


/**
 * @typedef RawDataThreadStoreEntry
 *
 * @prop plotImageId
 * @prop {Float32Array} float1d
 * @prop {HistogramData} histogram
 * @prop {number} dataWidth
 * @prop {number} dataHeight
 * @prop {number} datamin
 * @prop {number} datamax
 * @prop {Object} processHeader
 */



export const {addRawDataToCache, getEntry, getEntryByBand, removeRawData}= (() => {

    let rawDataStore= [];

    /**
     *
     * @param plotImageId
     * @param float1d
     * @param datamin
     * @param datamax
     * @param processHeader
     * @param band
     */
    const addRawDataToCache= (plotImageId, float1d, datamin, datamax, processHeader, band= Band.NO_BAND) => {
        const bandEntry= {float1d, datamin, datamax, processHeader, rawTileDataAry:[], thumbnailEncodedImage: undefined};
        const entry= rawDataStore.find( (e) => e.plotImageId===plotImageId);
        if (entry) {
            entry[band.key]= bandEntry;
        }
        else {
            rawDataStore.push({plotImageId, [band.key]:bandEntry});
        }
    };

    const updateCacheData= (plot, cacheData) => {
        //todo  this should update the cache
        //       make getEntry clone the results, so updateCacheData must be called
    };

    const removeRawData= (plotImageId) => {
        rawDataStore= rawDataStore.filter( (s) => s.plotImageId!==plotImageId);
        return rawDataStore.length;
    };
    const getEntry= (plotImageId) => rawDataStore.find( (e)  => e.plotImageId===plotImageId);
    const getEntryByBand= (plotImageId,band) => getEntry(plotImageId)?.[band.key];

    return { addRawDataToCache, removeRawData, getEntry, getEntryByBand};

})();

