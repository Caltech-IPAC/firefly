/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import {callGetImageMasterData} from '../../rpc/PlotServicesJson.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';


const LOAD_ERR_MSG= 'Load Failed: could not load image data';


const defImageSources= () => get( getAppOptions(), 'imageMasterSources', ['ALL']);
const defSortOrder= () => get( getAppOptions(), 'imageMasterSourcesOrder', '');

function initImageMasterDataRetriever() {
    let dataLoadBegin= false;
    let waitingAry= [];
    let loadedDataFailed= false;
    let loadedData= null;


    /**
     *
     * @param [imageSources]
     * @param [projectSortOrder]
     * @return {Promise}
     */
    const getData= (imageSources= defImageSources(), projectSortOrder= defSortOrder()) => {
        if (loadedData || loadedDataFailed) {
            return loadedData ? Promise.resolve(loadedData) : Promise.reject(Error(LOAD_ERR_MSG));
        }


        if (!dataLoadBegin) {

            dataLoadBegin= true;
            callGetImageMasterData(imageSources,projectSortOrder).then( (result) => {
                loadedData= result.data;
                waitingAry.forEach( (i) => i.resolve(loadedData));
                waitingAry= undefined;
            }).catch( () => {
                waitingAry.forEach( (i) => i.reject(Error(LOAD_ERR_MSG)));
                loadedDataFailed= true;
                waitingAry= undefined;
            });
        }
        return new Promise( function(resolve, reject) {
            waitingAry.push({resolve, reject});
        });
    };
    return getData;
}


/**
 * function to return a promise to PlotLy
 * {function}
 */
export const getImageMasterData= initImageMasterDataRetriever();
