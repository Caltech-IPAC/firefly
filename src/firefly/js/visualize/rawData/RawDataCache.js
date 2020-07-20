import {Band} from '../Band.js';
import {postToWorker, removeWorker} from '../../threadWorker/WorkerAccess.js';
import {RawDataThreadActions} from '../../threadWorker/WorkerThreadActions.js';



/**
 * @typedef RawDataStoreEntry
 *
 * @prop plotImageId
 * @prop {Float32Array} float1d
 * @prop {HistogramData} histogram
 * @prop {number} dataWidth
 * @prop {number} dataHeight
 * @prop {Array<Int8Array>} stretchedDataTiles
 * @prop {Array<Int8Array>} color
 * @prop {ThumbnailImage} thumbnailData
 * @prop {Array.<{x:number,y:number,width:number,height:number,local:boolean}>} localScreenTileDefList
 * @prop {number} datamin
 * @prop {number} datamax
 * @prop {Object} processHeader
 * @prop {Object} imageTileDataGroup
 * @prop {Array.<Canvas>} rawDataTiles
 */



export const {addRawDataToCache, getEntry, removeRawData}= (() => {

    let rawDataStore= [];

    /**
     *
     * @param plotImageId
     * @param processHeader
     * @param workerKey
     * @param band
     */
    const addRawDataToCache= (plotImageId, processHeader, workerKey, band= Band.NO_BAND) => {
        const bandEntry= {processHeader, rawTileDataAry:[], thumbnailEncodedImage: undefined};
        const entry= rawDataStore.find( (e) => e.plotImageId===plotImageId);
        if (entry) {
            entry[band.key]= bandEntry;
            entry.workerKey= workerKey;
        }
        else {
            rawDataStore.push({plotImageId, [band.key]:bandEntry, workerKey});
        }
    };

    const updateCacheData= (plot, cacheData) => {
        //todo  this should update the cache
        //       make getEntry clone the results, so updateCacheData must be called
    };

    const removeRawData= (plotImageId) => {
        const entry= getEntry(plotImageId);
        if (entry) {
            rawDataStore= rawDataStore.filter( (s) => s.plotImageId!==plotImageId);
            const action= {type:RawDataThreadActions.REMOVE_RAW_DATA, workerKey:entry.workerKey, payload:{plotImageId}};
            postToWorker(action).then(({entryCnt}) => {
                if (entryCnt===0) removeWorker(entry.workerKey);
            });
        }
    };
    const getEntry= (plotImageId) => rawDataStore.find( (e)  => e.plotImageId===plotImageId);

    return { addRawDataToCache, removeRawData, getEntry};

})();

