/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import numeral from 'numeral';
import {logError} from '../util/WebUtil.js';
import {getPixScaleArcSec, PlotAttribute, isImage} from './WebPlot.js';
import {primePlot, getFoV} from './PlotViewUtil.js';
import VisUtil from './VisUtil.js';


export const levels= [.03125, .0625, .125,.25,.5, .75, 1,2,3, 4,5, 6, 7,8, 9, 10, 11, 12, 13, 14, 15, 16, 32];

// const hiPSLevelsOLD= [0.000000007445312,
//                    0.00000000930664,
//                    0.000000011167969,
//                    0.00000002978125,
//                    0.0000000595625,
//                    0.000000119125,
//                    0.00000357421875, 0.00000023825, 0.0000004765, 0.000000953,0.000001906,
//                    0.0000038125, 0.000002859375, 0.000007625, 0.00001525, 0.0000305,
//                    0.0000610, 0.000122070, 0.000244140625, 0.00048828125,
//                    0.0009765625, .001953125, 0.00390625,  .0078125,
//                    .015625, .03125, .0625, .125,.25, .5, .75, 1,2, 4, 8, 10,
//                    14, 16, 32, 64, 128, 256, 512, 768, 1024, 1536, 2048, 3072, 4096, 6144, 8192,
//                    10240, 16364, 24556, 32728, 57284, 65456, 122740, 130928 ];

const hiPSLevels=  (()=> {
    const upMax= 3;
    const downMax= 62;
    const up= [1];
    const down= [];
    down[downMax]= .75;
    for(let i=1; i<upMax; i++)  up[i]= up[i-1] * 1.5;
    for(let j=downMax-1; j>=0; j--) down[j]= down[j+1]*.75;
    return [...down, ...up];
})();

// const hiPSLevels= computeHipsLevels();

const IMAGE_ZOOM_MAX= levels[levels.length-1];
const HIPS_ZOOM_MAX= hiPSLevels[hiPSLevels.length-1];

/**
 * can be 'UP','DOWN', 'FIT', 'FILL', 'ONE', 'LEVEL', 'WCS_MATCH_PREV'
 * @public
 * @global
 */
export const UserZoomTypes= new Enum(['UP','DOWN', 'FIT', 'FILL', 'ONE', 'LEVEL', 'WCS_MATCH_PREV'], { ignoreCase: true });



/**
 *
 * @param {WebPlot} plot
 * @param {ZoomType} zoomType
 * @return {number}
 */
export function getNextZoomLevel(plot, zoomType) {
    const {zoomFactor}= plot;
    const availableLevels= isImage(plot) ? levels : hiPSLevels;
    const zoomMax= isImage(plot)  ? IMAGE_ZOOM_MAX : HIPS_ZOOM_MAX;
    let newLevel= 1;
    if (zoomType===UserZoomTypes.UP) {
        newLevel= zoomFactor>=zoomMax ? zoomMax : availableLevels.find( (l) => l>zoomFactor);
    }
    else if (zoomType===UserZoomTypes.ONE) {
        newLevel= 1;
    }
    else if (zoomType===UserZoomTypes.DOWN) {
        newLevel= availableLevels[0];
        let found= false;
        for(let i= availableLevels.length-1; (i>=0); i--) {
            found= (availableLevels[i]<zoomFactor);
            if (found) {
                newLevel= availableLevels[i];
                break;
            }
        }
    }
    else {
        logError('unsupported zoomType');
    }
    return newLevel;
}


/**
 *
 * @param plot
 * @param screenDim
 * @param fullType
 * @param tryMinFactor
 */
export function getEstimatedFullZoomFactor(plot, screenDim, fullType, tryMinFactor=-1) {
    const {width,height} = screenDim;
    let overrideFullType= fullType;

    if (plot.attributes[PlotAttribute.EXPANDED_TO_FIT_TYPE]) {
        const s= plot.attributes[PlotAttribute.EXPANDED_TO_FIT_TYPE];
        if (VisUtil.FullType.has(s)) overrideFullType= VisUtil.FullType.get(s);
    }
    return VisUtil.getEstimatedFullZoomFactor(overrideFullType, plot.dataWidth, plot.dataHeight,
                                              width,height, tryMinFactor);
}


export function getZoomMax(plot) { return isImage(plot) ? levels[levels.length-1] : hiPSLevels[hiPSLevels.length-1]; }



function getOnePlusLevelDesc(level) {
    let retval;
    const remainder= level % 1;
    if (remainder < .1 || remainder>.9) {
        retval= Math.round(level)+'x';
    }
    else {
        retval= numeral(level).format('0.000')+'x';
    }
    return retval;
}

/**
 * Get the scale in arcsec / pixel of a plot as the given zoom factor
 * @param plot
 * @param zoomFact
 * @return {number}
 */
export function getArcSecPerPix(plot, zoomFact) {
    return getPixScaleArcSec(plot) / zoomFact;
}

export function getZoomLevelForScale(plot, arcsecPerPix) {
    return getPixScaleArcSec(plot) / arcsecPerPix;
}

export function convertZoomToString(level) {
    let retval;
    const zfInt= Math.floor(level*10000);

    if (zfInt>=10000)      retval= getOnePlusLevelDesc(level); // if level > then 1.0
    else if (zfInt===39)   retval= '1/256x';    // 1/256
    else if (zfInt===78)   retval= '1/128x';    // 1/128
    else if (zfInt===156)  retval= '1/64x';     // 1/64
    else if (zfInt===312)  retval= '1/32x';     // 1/32
    else if (zfInt===625)  retval= '1/16x';     // 1/16
    else if (zfInt===1250) retval= '1/8x';      // 1/8
    else if (zfInt===2500) retval= String.fromCharCode(188) +'x'; // 1/4
    else if (zfInt===7500) retval= String.fromCharCode(190) +'x';   // 3/4
    else if (zfInt===5000) retval= String.fromCharCode(189) +'x';   // 1/2
    else if (level<.125)   retval= numeral(level).format('0.000')+'x';
    else                   retval= numeral(level).format('0.0')+'x';
    return retval;
}

const formatFOV = (charCode, fNum) => {
    const nFormat = (fNum > 10.0) ? '0' : ((fNum >= 1.0) ? '0.0' : '0.00');
    return `${numeral(fNum).format(nFormat)}${charCode}`;
};

export function makeFoVString(fov) {
    let fovFormatted;
    if (fov > 1.0) {
        fovFormatted= formatFOV(String.fromCharCode(176), fov);
    } else {
        const fovMin = VisUtil.convertAngle('deg', 'arcmin', fov);

        fovFormatted= fovMin > 1.0 ? formatFOV("'", fovMin) :
            formatFOV('"', VisUtil.convertAngle('arcmin', 'arcsec', fovMin));
    }
    return fovFormatted;
}

/**
 *
 * @param {PlotView} pv
 * @return {{fovFormatted:string,zoomLevelFormatted:string}}
 */
export function getZoomDesc(pv) {
    const plot= primePlot(pv);
    if (!plot || !plot.viewDim) return {fovFormatted:'',zoomLevelFormatted:''};

    const zoomLevelFormatted= isImage(plot) ? convertZoomToString(plot.zoomFactor) : '';
    const fov= getFoV(pv);
    const fovFormatted= makeFoVString(fov);

    return {fovFormatted,zoomLevelFormatted};

}


