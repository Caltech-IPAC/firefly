import {Band} from '../Band.js';
import {postToWorker, removeWorker} from '../../threadWorker/WorkerAccess.js';
import {RawDataThreadActions} from '../../threadWorker/WorkerThreadActions.js';



/**
 * @typedef RawDataStoreEntry
 *
 * @prop plotImageId
 * @prop {Canvas} thumbnailEncodedImage
 * @prop {Array.<{x:number,y:number,width:number,height:number,local:boolean}>} localScreenTileDefList
 * @prop {RawTileDataGroup}
 *
 * @prop {number} loadingCnt - number of times this is loading, it should be 0,1,2
 */

export const STRETCH_ONLY= 'STRETCH_ONLY';
export const CLEARED= 'CLEARED';

export const {addRawDataToCache, getEntry, removeRawData}= (() => {

    let rawDataStore= [];

    /**
     *
     * @param plotImageId
     * @param processHeader
     * @param workerKey
     * @param band
     * @param dataType
     */
    const addRawDataToCache= (plotImageId, processHeader, workerKey, band= Band.NO_BAND, dataType='FULL') => {
        const bandEntry= {processHeader, rawTileDataAry:[], thumbnailEncodedImage: undefined};
        const entry= rawDataStore.find( (e) => e.plotImageId===plotImageId);
        if (entry) {
            entry[band.key]= bandEntry;
            entry.workerKey= workerKey;
            entry.dataType= dataType;
        }
        else {
            rawDataStore.push({plotImageId, [band.key]:bandEntry, workerKey, dataType, loadingCnt:0});
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

