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
import {getCmdSrvSyncURL} from 'firefly/util/WebUtil.js';
import {fetchUrl} from 'firefly/api/ApiUtil.js';
import {getBixPix} from 'firefly/visualize/FitsHeaderUtil.js';
import {isHiPS} from 'firefly/visualize/WebPlot.js';


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


export function callCrop(stateAry, corner1ImagePt, corner2ImagePt, cropMultiAll) {
    const params= makeParamsWithStateAry(stateAry, {
        [ServerParams.PT1]: corner1ImagePt.toString(),
        [ServerParams.PT2]: corner2ImagePt.toString(),
        [ServerParams.CRO_MULTI_ALL]: cropMultiAll +''
    });
    return doJsonRequest(ServerParams.CROP, params, true);
}

export function callGetFileFlux(stateAry, pt, wpt,isHips, hipsTileUrl, hipsPlane=0) {

    const obj= {
        [ServerParams.PT]: pt.toString(),
    };
    if (isHips) {
        obj[ServerParams.WPT]= wpt.toString();
        obj[ServerParams.IS_HIPS_TILE]= true+'';
        obj[ServerParams.URL]= hipsTileUrl;
        obj[ServerParams.PLANE]= hipsPlane;
    }

    const params=  makeParamsWithStateAry(stateAry,obj );
    return doJsonRequest(ServerParams.FILE_FLUX_JSON, params,true);
}


async function fetchExtraction(plot, inParams, cmd= ServerParams.FITS_EXTRACTION) {
    const use64Bit= getBixPix(plot)===-64;
    const params= {...inParams, [ServerParams.EXTRACTION_FLOAT_SIZE]: use64Bit ? 64 : 32};
    const response= await fetchUrl(getCmdSrvSyncURL()+`?${ServerParams.COMMAND}=${cmd}`,{method:'POST', params },false);
    if (!response.ok) {
        throw(new Error(`Error from Server for getStretchedByteData: code: ${response.status}, text: ${response.statusText}`));
    }
    const arrayBuffer= await response.arrayBuffer();
    return use64Bit ? new Float64Array(arrayBuffer) : new Float32Array(arrayBuffer);
}

/**
 * Get the z-axis values of a pixel from a cube
 * @param {Object} p
 * @param {WebPlot|undefined} p.plot
 * @param {number} p.hduNum
 * @param {ImagePt} p.ipt - for HiPS, the pixel in the FITS cube tile
 * @param {number} p.pointSize
 * @param {String} p.combineOp
 * @param {String} [p.hipsTileUrl] - required for HiPS, url of the FITS cube tile
 * @param {boolean} [p.relatedCubes]
 * @return {Promise<Float32Array|Float64Array>}
 */
export async function callGetCubeDrillDownAry({plot, hduNum, ipt, pointSize, combineOp, hipsTileUrl, relatedCubes=true}) {
    if (!plot) return;
    const fileParams= isHiPS(plot)
        ? {[ServerParams.IS_HIPS_TILE]: true+'', [ServerParams.URL]: hipsTileUrl}
        : {[ServerParams.STATE]: plot.plotState.toJson(false)};
    return fetchExtraction(plot,
        {
            [ServerParams.EXTRACTION_TYPE]: 'z-axis',
            ...fileParams,
            [ServerParams.PT] : ipt.toString(),
            [ServerParams.POINT_SIZE] : pointSize+'',
            [ServerParams.COMBINE_OP] : combineOp,
            [ServerParams.HDU_NUM] : hduNum+'',
            [ServerParams.RELATED_HDUS] : relatedCubes+'',
        });
}

export async function callGetLineExtractionAry(plot, hduNum, plane, pt, pt2, ptSize, combineOp, relatedHDUS=true) {
    return fetchExtraction(plot,
        {
            [ServerParams.EXTRACTION_TYPE]: 'line',
            [ServerParams.STATE]: plot.plotState.toJson(false),
            [ServerParams.PT] : pt.toString(),
            [ServerParams.PT2] : pt2.toString(),
            [ServerParams.POINT_SIZE] : ptSize+'',
            [ServerParams.COMBINE_OP] : combineOp,
            [ServerParams.PLANE] : plane+'',
            [ServerParams.HDU_NUM] : hduNum+'',
            [ServerParams.RELATED_HDUS] : relatedHDUS+'',
        });
}

/**
 * Get the values at a list of points
 * @param {Object} p
 * @param {WebPlot|undefined} p.plot
 * @param {number} p.hduNum
 * @param {number} p.plane
 * @param {Array.<ImagePt>|Array.<{tileUrl:String, tilePt:ImagePt}>} p.ptAry - for an image, an array of image points,
 * for HiPS, an array of the FITS tile location of each point (see getHiPSFitsTileLocation)
 * @param {number} p.ptSizeX
 * @param {number} p.ptSizeY
 * @param {String} p.combineOp
 * @param {boolean} [p.relatedHDUS]
 * @return {Promise<Float32Array|Float64Array>}
 */
export async function callGetPointExtractionAry({plot, hduNum, plane, ptAry, ptSizeX, ptSizeY, combineOp, relatedHDUS=true}) {
    if (!plot) return;
    const ptParams= isHiPS(plot)
        ? {
            [ServerParams.IS_HIPS_TILE]: true+'',
            [ServerParams.HIPS_TILE_URL_ARY]: JSON.stringify(ptAry.map( ({tileUrl}) => tileUrl)),
            [ServerParams.PTARY]: JSON.stringify(ptAry.map( ({tilePt}) => tilePt.toString())),
        }
        : {
            [ServerParams.STATE]: plot.plotState.toJson(false),
            [ServerParams.PTARY]: JSON.stringify(ptAry.map( (pt) => pt.toString())),
        };
    return fetchExtraction(plot,
        {
            [ServerParams.EXTRACTION_TYPE]: 'points',
            ...ptParams,
            [ServerParams.POINT_SIZE_X] : ptSizeX+'',
            [ServerParams.POINT_SIZE_Y] : ptSizeY+'',
            [ServerParams.COMBINE_OP] : combineOp,
            [ServerParams.PLANE] : plane+'',
            [ServerParams.HDU_NUM] : hduNum+'',
            [ServerParams.RELATED_HDUS] : relatedHDUS+'',
        });
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
 * @param otherParams
 * @return {Promise}
 */
function makeParamsWithStateAry(stateAry, otherParams={}) {
    const stateObj= stateAry.reduce( (obj, s,idx) => {
        obj['state'+idx]= s.toJson();
        return obj;
    },{} );
    return { ...stateObj, ...otherParams,
    };
}


