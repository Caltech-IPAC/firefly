/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import Enum from 'enum';
import {sprintf} from '../externalSource/sprintf';
import {logger} from '../util/Logger.js';
import {getPixScaleArcSec, isHiPS, isHiPSAitoff, isImage} from './WebPlot.js';
import {PlotAttribute} from './PlotAttribute.js';
import {getFoV, primePlot} from './PlotViewUtil.js';
import {convertAngle} from './VisUtil.js';


/**
 * @typedef FullType
 * enum can be one of
 * @prop ONLY_WIDTH
 * @prop WIDTH_HEIGHT
 * @prop ONLY_HEIGHT
 * @prop {Function} has
 * @type {Enum}
 */
export const FullType = new Enum(['ONLY_WIDTH', 'WIDTH_HEIGHT', 'ONLY_HEIGHT']);

export const imageLevels= [.0125, .025, .05,.1,.25,.3, .4, .5, .75, .8, .9, 1, 1.25, 1.5,2, 2.5,3, 3.5,
    4,5, 6, 7,8, 9, 10, 11, 12, 13, 14, 15, 16, 18,20,22,24,28, 32];
export const imageLevelsReversed= [...imageLevels].reverse();


const hipsLevels=  (()=> {
    const upMax= 3;
    const downMax= 62;
    const up= [1];
    const down= [];
    down[downMax]= .75;
    for(let i=1; i<upMax; i++)  up[i]= up[i-1] * 1.5;
    for(let j=downMax-1; j>=0; j--) down[j]= down[j+1]*.75;
    return [...down, ...up];
})();

export const hipsLevelsReversed= [...hipsLevels].reverse();

const IMAGE_ZOOM_MAX= imageLevels[imageLevels.length-1];
const HIPS_ZOOM_MAX= hipsLevels[hipsLevels.length-1];

/**
 * @typedef UserZoomTypes
 * can be 'UP','DOWN', 'FIT', 'FILL', 'ONE', 'LEVEL', 'WCS_MATCH_PREV'
 * @prop UP,
 * @prop DOWN,
 * @prop FIT,
 * @prop FILL,
 * @prop ONE,
 * @prop LEVEL,
 * @prop WCS_MATCH_PREV,
 * @type {Enum}
 * @public
 * @global
 */
export const UserZoomTypes= new Enum(['UP','DOWN', 'FIT', 'FILL', 'ONE', 'LEVEL', 'WCS_MATCH_PREV'], { ignoreCase: true });

/**
 *
 * @param {WebPlot} plot
 * @param {ZoomType} zoomType
 * @param {number} upDownPercent
 * @return {number}
 */
export function getNextZoomLevel(plot, zoomType, upDownPercent=1) {
    if (zoomType===UserZoomTypes.ONE) return 1;
    const {zoomFactor}= plot;
    const zoomMax= isImage(plot)  ? IMAGE_ZOOM_MAX : HIPS_ZOOM_MAX;
    const availableLevels= isImage(plot) ? imageLevels : hipsLevels;
    if (zoomType===UserZoomTypes.UP) {
        if (zoomFactor>=zoomMax) return zoomMax;
        const idx= availableLevels.findIndex( (l) => l>zoomFactor);
        if (upDownPercent===1 || idx===0) return availableLevels[idx];
        return zoomFactor + (availableLevels[idx]-availableLevels[idx-1])*upDownPercent;
    }
    else if (zoomType===UserZoomTypes.DOWN) {
        if (zoomFactor<=availableLevels[0]) return availableLevels[0];
        const revLevels= isImage(plot) ? imageLevelsReversed : hipsLevelsReversed;
        const idx= revLevels.findIndex((l) => l<zoomFactor );
        if (upDownPercent===1 || idx===0 || idx===revLevels.length-1) return revLevels[idx];
        return zoomFactor - (revLevels[idx]-revLevels[idx+1])*upDownPercent;
    }
    else {
        logger.error('unsupported zoomType');
        return 1;
    }
}



/**
 * @param plot
 * @param screenDim
 * @param fullType
 */
export function getEstimatedFullZoomFactor(plot, screenDim, fullType) {
    const {width,height} = screenDim;
    let overrideFullType= fullType;

    if (plot.attributes[PlotAttribute.EXPANDED_TO_FIT_TYPE]) {
        const s= plot.attributes[PlotAttribute.EXPANDED_TO_FIT_TYPE];
        if (FullType.has(s)) overrideFullType= FullType.get(s);
    }
    return getEstimatedFullZoomFactorDetails(overrideFullType, plot.dataWidth, plot.dataHeight,
                                              width,height, isHiPS(plot) && isHiPSAitoff(plot));
}

function getEstimatedFullZoomFactorDetails(fullType, dataWidth, dataHeight,
                                           screenWidth, screenHeight, hipsAitoff) {
    const zFactW =  screenWidth /  (dataWidth* (hipsAitoff?2.7:1));
    const zFactH =  screenHeight / dataHeight;

    if (hipsAitoff) {
        return fullType===FullType.ONLY_WIDTH ? zFactW : zFactH*.7;
    }
    if (fullType===FullType.ONLY_WIDTH || screenHeight <= 0 || dataHeight <= 0) {
        return  zFactW;
    } else if (fullType===FullType.ONLY_HEIGHT || screenWidth <= 0 || dataWidth <= 0) {
        return zFactH;
    } else {
        return Math.min(zFactW, zFactH);
    }
}


export const getZoomMax= (plot) => isImage(plot) ? imageLevels[imageLevels.length-1] : hipsLevels[hipsLevels.length-1];

function getOnePlusLevelDesc(level) {
    const remainder= level % 1;
    if (remainder < .1 || remainder>.9) return Math.round(level)+'x';
    else return sprintf('%.3fx',level);
}

/**
 * Get the scale in arcsec / pixel of a plot as the given zoom factor
 * @param plot
 * @param zoomFact
 * @return {number}
 */
export const getArcSecPerPix= (plot, zoomFact) => getPixScaleArcSec(plot) / zoomFact;

export const getZoomLevelForScale= (plot, arcsecPerPix) => getPixScaleArcSec(plot) / arcsecPerPix;

export function convertZoomToString(level) {
    const zfInt= Math.floor(level*10000);
    if (zfInt>=10000)      return getOnePlusLevelDesc(level); // if level > then 1.0
    else if (zfInt===39)   return '1/256x';    // 1/256
    else if (zfInt===78)   return '1/128x';    // 1/128
    else if (zfInt===156)  return '1/64x';     // 1/64
    else if (zfInt===312)  return '1/32x';     // 1/32
    else if (zfInt===625)  return '1/16x';     // 1/16
    else if (zfInt===1250) return '1/8x';      // 1/8
    else if (zfInt===2500) return String.fromCharCode(188) +'x'; // 1/4
    else if (zfInt===7500) return String.fromCharCode(190) +'x';   // 3/4
    else if (zfInt===5000) return String.fromCharCode(189) +'x';   // 1/2
    else if (level<.125)   return sprintf('%.3fx',level);
    else                   return sprintf('%.1fx',level);
}

const formatFOV = (charCode, fNum) => {
    const nFormat = (fNum > 10.0) ? '%d' : ((fNum >= 1.0) ? '%.1f' : '%.2f');
    const str= sprintf(`${nFormat}`,fNum);
    return `${str}${charCode}`;
};

export function makeFoVString(fov) {
    if (isNaN(fov)) return '';
    if (fov > 1) {
        return formatFOV(String.fromCharCode(176), fov);
    } else {
        const fovMin = convertAngle('deg', 'arcmin', fov);
        return fovMin > 1 ? formatFOV("'", fovMin) : formatFOV('"', convertAngle('arcmin', 'arcsec', fovMin));
    }
}

/**
 *
 * @param {PlotView} pv
 * @return {{fovFormatted:string,zoomLevelFormatted:string}}
 */
export function getZoomDesc(pv) {
    const plot= primePlot(pv);
    if (!plot?.viewDim) return {fovFormatted:'',zoomLevelFormatted:''};
    return {
        zoomLevelFormatted: isImage(plot) ? convertZoomToString(plot.zoomFactor) : '',
        fovFormatted: makeFoVString(getFoV(pv))
    };

}
