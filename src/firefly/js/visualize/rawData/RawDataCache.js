import {getCmdSrvSyncURL, getRootURL} from '../../util/WebUtil';
import {Band} from '../Band.js';
import {postToWorker, removeWorker} from '../../threadWorker/WorkerAccess.js';
import {RawDataThreadActions} from '../../threadWorker/WorkerThreadActions.js';



/**
 * @typedef RawDataStoreEntry
 *
 * @prop {String} plotImageId
 * @prop {boolean} initialized - true if initialized
 * @prop {Canvas} thumbnailEncodedImage
 * @prop {Array.<{x:number,y:number,width:number,height:number,local:boolean}>} localScreenTileDefList
 * @prop {RawTileDataGroup}
 *
 * @prop {number} loadingPromise - a promise to the current load
 */

export const STRETCH_ONLY= 'STRETCH_ONLY';
export const CLEARED= 'CLEARED';

export const {addRawDataToCache, getEntry, removeRawData,addLoadingPromise, markOutOfMemory}= (() => {

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
            entry.initialized= true;
        }
        else {
            rawDataStore.push({plotImageId, [band.key]:bandEntry, workerKey, dataType, loadingPromise:undefined});
        }
    };

    /**
     *
     * @param {String} plotImageId
     * @param {PlotState} plotState
     */
    const removeRawData= (plotImageId, plotState) => {
        const entry= getEntry(plotImageId,false);
        if (!entry) return;
        rawDataStore= rawDataStore.filter( (s) => s.plotImageId!==plotImageId);
        if (entry.initialized) {
            const action= {
                type:RawDataThreadActions.REMOVE_RAW_DATA,
                workerKey:entry.workerKey,
                callKey:'',
                payload:{
                    plotStateSerialized: plotState.toJson(false),
                    plotImageId,
                    cmdSrvUrl: getCmdSrvSyncURL(),
                }};
            postToWorker(action).then(({entryCnt}) => {
                if (entryCnt===0) removeWorker(entry.workerKey);
            });
        }
    };
    const getEntry= (plotImageId,create=true) => {
        if (!plotImageId) return create ? {initialized:false} : undefined;
        let entry= rawDataStore.find( (e)  => e.plotImageId===plotImageId);
        if (!entry && create) {
            entry= {plotImageId, loadingPromise:undefined, initialized: false, outOfMemory:false};
            rawDataStore.push(entry);
        }
        return entry;
    };

    const addLoadingPromise= (plotImageId,loadingPromise) => {
        const entry= getEntry(plotImageId);
        if (!entry) return;
        entry.loadingPromise= loadingPromise;

        const clearPromise= () => {
            if (loadingPromise===entry.loadingPromise) entry.loadingPromise=undefined;
        };

        loadingPromise
            .then(() => clearPromise(loadingPromise) )
            .catch(() => clearPromise(loadingPromise) );
    };

    const markOutOfMemory= (plotImageId) => {
        const workerKey= getEntry(plotImageId,false)?.workerKey;
        if (!workerKey) return;
        rawDataStore.forEach( (e)  => {
            if (e.workerKey===workerKey) e.outOfMemory= true;
        });
    };

    return { addRawDataToCache, removeRawData, getEntry,addLoadingPromise, markOutOfMemory};

})();

