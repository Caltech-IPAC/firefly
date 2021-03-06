/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

import {isArray} from 'lodash';
import {ServerParams} from '../data/ServerParams.js';
import {doJsonRequest} from '../core/JsonUtils.js';
import {SelectedShape} from '../drawingLayers/SelectedShape';


/**
 *
 * @param state
 * @param band
 * @return {Promise}
 */
export const callGetColorHistogram= (state,band) =>
    doJsonRequest(ServerParams.HISTOGRAM,
        {[ServerParams.STATE] : state.toJson(false), [ServerParams.BAND]: band.key}, true);

/**
 * @param {WebPlotRequest} redRequest
 * @param {WebPlotRequest} greenRequest
 * @param {WebPlotRequest} blueRequest
 * @return {Promise}
 */
export function callGetWebPlot3Color(redRequest, greenRequest, blueRequest) {
    const params= {};
    if (redRequest) params[ServerParams.RED_REQUEST]= redRequest.toString();
    if (greenRequest) params[ServerParams.GREEN_REQUEST]= greenRequest.toString();
    if (blueRequest) params[ServerParams.BLUE_REQUEST]= blueRequest.toString();
    return doJsonRequest(ServerParams.CREATE_PLOT, params, true);
}

/**
 * @param {WebPlotRequest} request
 * @return {Promise}
 */
export const callGetWebPlot= (request) =>
      doJsonRequest(ServerParams.CREATE_PLOT, {[ServerParams.NOBAND_REQUEST]: request.toString()},true);

export function callGetWebPlotGroup(reqAry,  requestKey) {
    const paramList= reqAry.reduce( (obj,req, idx) => {
        obj[ServerParams.REQUEST+idx]= req.toString();
        return obj;
    }, {[ServerParams.PROGRESS_KEY]: requestKey});
    return doJsonRequest(ServerParams.CREATE_PLOT_GROUP, paramList,true);
}


export const callGetAreaStatistics= (state, ipt1, ipt2, ipt3, ipt4, areaShape = SelectedShape.rect.key, rotation = 0) =>
   doJsonRequest(ServerParams.STAT, {
                [ServerParams.STATE]: state.toJson(false),
                [ServerParams.PT1]: ipt1.toString(),
                [ServerParams.PT2]: ipt2.toString(),
                [ServerParams.PT3]: ipt3.toString(),
                [ServerParams.PT4]: ipt4.toString(),
                [ServerParams.GEOSHAPE]: areaShape,
                [ServerParams.ROTATION]: rotation }, true);


/**
 *
 * @param {Array} stateAry
 * @param {number} level
 * @param {boolean} isFullScreen hint, will only make on file
 */
export function callSetZoomLevel(stateAry, level, isFullScreen) {
    const params= makeParamsWithStateAry(stateAry,false, [
        {name:ServerParams.LEVEL, value:level},
        {name:ServerParams.FULL_SCREEN, value : isFullScreen},
    ]);
    return doJsonRequest(ServerParams.ZOOM, params, true);
}


export function callChangeColor(state, colorTableId) {
    const params= [
        {name:ServerParams.STATE, value: state.toJson(false)},
        {name:ServerParams.COLOR_IDX, value:colorTableId}
    ];
    return doJsonRequest(ServerParams.CHANGE_COLOR, params, true);
}


export function callRecomputeStretch(state, stretchDataAry) {
    const params= { [ServerParams.STATE]: state.toJson(false)};
    stretchDataAry.forEach( (sd,idx) => params[ServerParams.STRETCH_DATA+idx]=  JSON.stringify(sd));
    return doJsonRequest(ServerParams.STRETCH, params, true);
}

export function callCrop(stateAry, corner1ImagePt, corner2ImagePt, cropMultiAll) {
    const params= makeParamsWithStateAry(stateAry,false, [
        {name:ServerParams.PT1, value: corner1ImagePt.toString()},
        {name:ServerParams.PT2, value: corner2ImagePt.toString()},
        {name:ServerParams.CRO_MULTI_ALL, value: cropMultiAll +''}
    ]);
    return doJsonRequest(ServerParams.CROP, params, true);
}

export function callGetFileFlux(stateAry, pt) {
    const params =  makeParamsWithStateAry(stateAry,true, [ {name: [ServerParams.PT], value: pt.toString()} ]);
    return doJsonRequest(ServerParams.FILE_FLUX_JSON, params,true);
}


/**
 *
 * @param {Array.<string>|string} imageSources -
 * @param {Array.<string>|string} projectSortOrder -  a arrays of projects that you want on top, any project not in
 * list will be add at the bottom
 * @return {Promise} the json image data
 */
export function callGetImageMasterData(imageSources, projectSortOrder='') {
    const sortOrderStr= isArray(projectSortOrder) ? projectSortOrder.join(',') : projectSortOrder.toString();
    const imageSourcesStr= isArray(imageSources) ? imageSources.join(',') : imageSources.toString();
    return doJsonRequest(ServerParams.GET_IMAGE_MASTER_DATA,
                {[ServerParams.IMAGE_SOURCES] : imageSourcesStr,
                 [ServerParams.SORT_ORDER] : sortOrderStr
                },
                true);
}

export const getDS9Region= (fileKey) =>
    doJsonRequest(ServerParams.DS9_REGION, { [ServerParams.FILE_KEY]: fileKey}, true);

export const saveDS9RegionFile= (regionData) =>
    doJsonRequest(ServerParams.SAVE_DS9_REGION, { [ServerParams.REGION_DATA]: regionData}, true);




/**
 * @param stateAry
 * @param includeDirectAccessData
 * @param otherParams
 * @return {Promise}
 */
function makeParamsWithStateAry(stateAry, includeDirectAccessData, otherParams=[]) {
    return [
        ...makeStateParamAry(stateAry,includeDirectAccessData),
        ...otherParams,
    ];
}

/**
 * @param {Array} startAry
 * @param {boolean} includeDirectAccessData
 * @return {Array}
 */
function makeStateParamAry(startAry, includeDirectAccessData= true) {
    return startAry.map( (s,idx) => {
        return {name:'state'+idx, value: s.toJson(includeDirectAccessData) };
    } );
}