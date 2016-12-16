/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {clone} from '../../util/WebUtil.js'

/**
 * @typedef {Object} OverlayPlotView
 * The container for the overlay plots
 *
 * @prop {String} imageOverlayId, immutable
 * @prop {String} plotId associated plotId, plot id of image that is overlayed
 * @prop {Boolean} maskOverlay true is this overlay is a mask
 * @prop {Boolean} visible the image overlay is visibile
 * @prop {Number} maskValue the mask value to plot
 * @prop {Number} imageNumber the hdu of the image to plot, starts with 0
 * @prop {String} color the color, if overlay is a mask
 * @prop {Number} opacity how transparent the overlay should be displayed
 */


/**
 *
 * @param imageOverlayId
 * @param plotId plot id of image that is overlayed
 * @param {String} title
 * @param imageNumber
 * @param maskNumber
 * @param maskValue
 * @param {string} color the color, if overlay is a mask
 * @param drawingDef
 * @return {OverlayPlotView}
 */
export function makeOverlayPlotView(imageOverlayId, plotId, title, imageNumber, maskNumber, maskValue, color, drawingDef) {

    var opv= {
        imageOverlayId,
        plotId,
        title,
        plot: null,
        drawingDef,
        makeOverlay : true,
        visible: true,
        maskNumber,
        maskValue,
        imageNumber,
        color,
        opacity: .58,
        plotCounter:0, // index of how many plots, used for making next ID
        plottingStatus:'',
        serverCall:'success'
    };

    return opv;
}

export function replaceOverlayPlots(opv, plot) {

    opv= clone(opv, {plot});
    plot.plotImageId= `${opv.imageOverlayId}--${opv.plotCounter}`;
    opv.plotCounter++;

    opv.plottingStatus='';
    opv.serverCall='success';

    return opv;
}
