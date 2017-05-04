/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

import {ServerParams} from '../data/ServerParams.js';
import {doJsonRequest} from '../core/JsonUtils.js';
import {PlotState} from '../visualize/PlotState.js';



/**
 *
 * @return {Promise}
 */
export const callGetColorHistogram= function(state,band,width,height) {
    var paramList = [];
    paramList.push({name:ServerParams.STATE, value: state.toJson()});
    paramList.push({name:ServerParams.WIDTH, value: width+''});
    paramList.push({name:ServerParams.HEIGHT, value: height+''});
    paramList.push({name:ServerParams.BAND, value: band.key});
    paramList.push({name:ServerParams.JSON_DEEP,value:'true'});

    return doJsonRequest(ServerParams.HISTOGRAM, paramList, true);
};

/**
 * @param {WebPlotRequest} redRequest
 * @param {WebPlotRequest} greenRequest
 * @param {WebPlotRequest} blueRequest
 * @return {Promise}
 */
export function callGetWebPlot3Color(redRequest, greenRequest, blueRequest) {
    var paramList = [];
    if (redRequest) paramList.push({name:ServerParams.RED_REQUEST, value:redRequest.toString()});
    if (greenRequest) paramList.push({name:ServerParams.GREEN_REQUEST, value:greenRequest.toString()});
    if (blueRequest) paramList.push({name:ServerParams.BLUE_REQUEST, value:blueRequest.toString()});
    paramList.push({name:ServerParams.JSON_DEEP,value:'true'});
    return doJsonRequest(ServerParams.CREATE_PLOT, paramList, true);
};

/**
 * @param {WebPlotRequest} request
 * @return {Promise}
 */
export function callGetWebPlot(request) {
    var paramList = [{name: ServerParams.NOBAND_REQUEST, value:request.toString()}];
    paramList.push({name:ServerParams.JSON_DEEP,value:'true'});
    return doJsonRequest(ServerParams.CREATE_PLOT, paramList,true);
};

export function callGetWebPlotGroup(reqAry,  requestKey) {
    var paramList = {};
    paramList[ServerParams.PROGRESS_KEY]= requestKey;
    paramList[ServerParams.JSON_DEEP]= 'true';
    paramList= reqAry.reduce( (obj,req, idx) => {
        obj[ServerParams.REQUEST+idx]= req.toString();
        return obj;
    }, paramList);
    return doJsonRequest(ServerParams.CREATE_PLOT_GROUP, paramList,true);
}

/**
 *
 * @param stateAry
 * @param rotate
 * @param angle
 * @param newZoomLevel
 */
export function callRotateToAngle(stateAry, rotate, angle, newZoomLevel) {
    var params = makeParamsWithStateAry(stateAry,[
                       {name: ServerParams.ROTATE, value: rotate + ''},
                       {name: ServerParams.ANGLE, value: angle + ''},
                       {name: ServerParams.ZOOM, value: newZoomLevel + ''},
                   ]);
    return doJsonRequest(ServerParams.ROTATE_ANGLE, params, true);
}


export function callGetAreaStatistics(state, ipt1, ipt2, ipt3, ipt4) {
    var params= {
        [ServerParams.STATE]: state.toJson(),
        [ServerParams.JSON_DEEP]:'true',
        [ServerParams.PT1]: ipt1.toString(),
        [ServerParams.PT2]: ipt2.toString(),
        [ServerParams.PT3]: ipt3.toString(),
        [ServerParams.PT4]: ipt4.toString()
    };
    return doJsonRequest(ServerParams.STAT, params, true);
}


/**
 *
 * @param {Array} stateAry
 * @param {number} level
 * @param {boolean} isFullScreen hint, will only make on file
 */
export function callSetZoomLevel(stateAry, level, isFullScreen) {
    var params= makeParamsWithStateAry(stateAry,[
        {name:ServerParams.LEVEL, value:level},
        {name:ServerParams.FULL_SCREEN, value : isFullScreen},
    ]);
    return doJsonRequest(ServerParams.ZOOM, params, true);
}


export function callChangeColor(state, colorTableId) {
    var params= [
        {name:ServerParams.STATE, value: state.toJson()},
        {name:ServerParams.JSON_DEEP,value:'true'},
        {name:ServerParams.COLOR_IDX, value:colorTableId}
    ];
    return doJsonRequest(ServerParams.CHANGE_COLOR, params, true);
}

export function callGetBeta(state) {
    const params= [
        {name:ServerParams.STATE, value: state.toJson()},
    ];
    return doJsonRequest(ServerParams.GET_BETA, params, true);
}

export function callRecomputeStretch(state, stretchDataAry) {
    var params= {
        [ServerParams.STATE]: state.toJson(),
        [ServerParams.JSON_DEEP]: true
    };
    stretchDataAry.forEach( (sd,idx) => params[ServerParams.STRETCH_DATA+idx]=  JSON.stringify(sd));
    return doJsonRequest(ServerParams.STRETCH, params, true);
}



export function callCrop(stateAry, corner1ImagePt, corner2ImagePt, cropMultiAll) {

    var params= makeParamsWithStateAry(stateAry,[
        {name:ServerParams.PT1, value: corner1ImagePt.toString()},
        {name:ServerParams.PT2, value: corner2ImagePt.toString()},
        {name:ServerParams.CRO_MULTI_ALL, value: cropMultiAll +''}
    ]);
    
    return doJsonRequest(ServerParams.CROP, params, true);
    
}
//LZ 3/22/16 DM-4494
export  function  callGetFitsHeaderInfo(plotState, tableId) {

    var params ={ [ServerParams.STATE]: plotState.toJson(),
        [ServerParams.JSON_DEEP]: true,
        tableId
    };

    var result = doJsonRequest(ServerParams.FITS_HEADER, params, true);
    return result;//doJsonRequest(ServerParams.FITS_HEADER, params);
}


export function callGetFileFlux(stateAry, pt) {

    var params =  makeParamsWithStateAry(stateAry,[
        {name: [ServerParams.PT], value: pt.toString()}
    ]);

    return doJsonRequest(ServerParams.FILE_FLUX_JSON, params,true);
}

export function getDS9Region(fileKey) {

    var params= {
        [ServerParams.FILE_KEY]: fileKey,
        [ServerParams.JSON_DEEP]: true
    };
    return doJsonRequest(ServerParams.DS9_REGION, params, true);
}


export function saveDS9RegionFile(regionData) {

    var params= {
        [ServerParams.REGION_DATA]: regionData,
        [ServerParams.JSON_DEEP]: true
    };
    return doJsonRequest(ServerParams.SAVE_DS9_REGION, params, true);
}


/**
 * 
 * @param state
 * @param regionData
 */
export function getImagePng(state, regionData, clientIsNorth, clientRotAngle, clientFlipY) {


    const params= {
        [ServerParams.STATE]: state.toJson(),
        [ServerParams.REGION_DATA]: regionData,
        [ServerParams.CLIENT_IS_NORTH]: clientIsNorth,
        [ServerParams.CLIENT_ROT_ANGLE]: clientRotAngle,
        [ServerParams.CLIENT_FlIP_Y]: clientFlipY,
        [ServerParams.JSON_DEEP]: true
    };
    return doJsonRequest(ServerParams.IMAGE_PNG_REG, params, true);
}




const getOneFileGroup= function(requestList, progressKey) {
    //todo
};

/**
 * not used
 * @param startAry
 */
function makeJsonStateAryString(startAry) {
    return JSON.stringify(startAry.map( (s) => PlotState.convertToJSON(s)));
}

function makeParamsWithStateAry(stateAry, otherParams=[]) {
    return [
        ...makeStateParamAry(stateAry),
        ...otherParams,
        {name:ServerParams.JSON_DEEP,value:'true'}
    ];

}

/**
 *
 * @param {Array} startAry
 * @return {Array}
 */
function makeStateParamAry(startAry) {
    return startAry.map( (s,idx) => {
        return {name:'state'+idx, value: s.toJson() };
    } );
}

