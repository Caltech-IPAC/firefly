/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

import ServerParams from '../data/ServerParams.js';
import {doService} from '../core/JsonUtils.js';
import {parse} from '../visualize/WebPlotResultParser.js';
import PlotState from '../visualize/PlotState.js';


const doJsonP= function() {
    return false;
    //return application.networkMode===NetworkMode.JSON;
};


/**
 *
 * @return {Promise}
 */
const getColorHistogram= function(state,band,width,height) {
    var paramList = [];
    paramList.push({name:ServerParams.STATE, value: state.serialize()});
    paramList.push({name:ServerParams.WIDTH, value: width+''});
    paramList.push({name:ServerParams.HEIGHT, value: height+''});
    paramList.push({name:ServerParams.BAND, value: band.toString()});
    paramList.push({name:ServerParams.JSON_DEEP,value:'true'});

    return doService(doJsonP(), ServerParams.HISTOGRAM, paramList
    ).then((data) => parse(data) );
};

/**
 * @param {WebPlotRequest} redRequest
 * @param {WebPlotRequest} greenRequest
 * @param {WebPlotRequest} blueRequest
 * @return {Promise}
 */
const getWebPlot3Color= function(redRequest, greenRequest, blueRequest) {
    var paramList = [];
    if (redRequest) paramList.push({name:ServerParams.RED_REQUEST, value:redRequest.toString()});
    if (greenRequest) paramList.push({name:ServerParams.GREEN_REQUEST, value:greenRequest.toString()});
    if (blueRequest) paramList.push({name:ServerParams.BLUE_REQUEST, value:blueRequest.toString()});
    paramList.push({name:ServerParams.JSON_DEEP,value:'true'});
    return doService(doJsonP(), ServerParams.CREATE_PLOT, paramList)
        .then((data) => parse(data) );
};

/**
 * @param {WebPlotRequest} request
 * @return {Promise}
 */
export const callGetWebPlot= function(request) {
    var paramList = [{name: ServerParams.NOBAND_REQUEST, value:request.toString()}];
    paramList.push({name:ServerParams.JSON_DEEP,value:'true'});
    return doService(doJsonP(), ServerParams.CREATE_PLOT, paramList);
};


/**
 *
 * @param stateAry
 * @param north
 * @param newZoomLevel
 */
export function callRotateNorth(stateAry, north, newZoomLevel) {
    var params =  makeParamsWithStateAry(stateAry,[
                   {name: ServerParams.NORTH, value: north + ''},
                   {name: ServerParams.ZOOM, value: newZoomLevel + ''},
                 ]);
    return doService(doJsonP(), ServerParams.ROTATE_NORTH, params);
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
    return doService(doJsonP(), ServerParams.ROTATE_ANGLE, params);
}


export function callGetAreaStatistics(state, ipt1, ipt2, ipt3, ipt4) {
    var params= {
        [ServerParams.STATE]: JSON.stringify(PlotState.convertToJSON(state)),
        [ServerParams.JSON_DEEP]:'true',
        [ServerParams.PT1]: ipt1.toString(),
        [ServerParams.PT2]: ipt2.toString(),
        [ServerParams.PT3]: ipt3.toString(),
        [ServerParams.PT4]: ipt4.toString()
    };
    return doService(doJsonP(), ServerParams.STAT, params);
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
    return doService(doJsonP(), ServerParams.ZOOM, params);
}


export function callChangeColor(state, colorTableId) {
    var params= [
        {name:ServerParams.STATE, value: JSON.stringify(PlotState.convertToJSON(state))},
        {name:ServerParams.JSON_DEEP,value:'true'},
        {name:ServerParams.COLOR_IDX, value:colorTableId}
    ];
    return doService(doJsonP(), ServerParams.CHANGE_COLOR, params);
}

export function callRecomputeStretch(state, stretchDataAry) {
    var params= {
        [ServerParams.STATE]: JSON.stringify(PlotState.convertToJSON(state)),
        [ServerParams.JSON_DEEP]: true
    };
    stretchDataAry.forEach( (sd,idx) => params[ServerParams.STRETCH_DATA+idx]=  JSON.stringify(sd));
    return doService(doJsonP(), ServerParams.STRETCH, params);
}



function flipImageOnY(stateAry) {
    return doService(doJsonP(), ServerParams.FLIP_Y, makeParamsWithStateAry(stateAry));
}




const getWebPlotGroup= function(requestList, progressKey) {
    //todo
};

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
        return {name:'state'+idx, value: JSON.stringify(PlotState.convertToJSON(s)) };
    } );
}




export var PlotServicesJson= {getColorHistogram, getWebPlot3Color, getWebPlotGroup, getOneFileGroup};
export default PlotServicesJson;
