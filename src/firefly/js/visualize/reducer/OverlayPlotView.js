/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {RDConst} from '../WebPlot.js';

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
 * @param hduNumber
 * @param maskNumber
 * @param maskValue
 * @param {string} color the color, if overlay is a mask
 * @param drawingDef
 * @param {string} [relatedDataId] a related data id if one exist
 * @param {string} [fileKey] a file on the server
 * @return {OverlayPlotView}
 */
export function makeOverlayPlotView(imageOverlayId, plotId, title, hduNumber, maskNumber,
                                    maskValue, color, drawingDef, relatedDataId, fileKey) {

    const opv= {
        imageOverlayId, // id of the overlay plot view analogous to plotId
        opvType: RDConst.IMAGE_MASK,
        plotId, // plotId of the base plot
        title,
        plot: null,
        primeIdx: 0,
        plots: [],
        cube: false,
        makeOverlay : true,
        visible: true,
        wasVisibleWhenReplace: false,
        maskNumber,
        maskValue,
        imageNumber:hduNumber,
        colorAttributes : {color, srcImageColor: color},
        relatedDataId,
        opacity: .58,
        plotCounter:0, // index of how many plots, used for making next ID
        fileKey,
        plottingStatusMsg:'',
        serverCall:'success'
    };

    return opv;
}

export function initOverlayPlots(opv) {

    opv= {...opv};
    opv.plots.forEach( (plot) => {
        plot.plotImageId=
            opv.cube
                ? `${opv.imageOverlayId}---plane-${plot.cubeCtx.cubePlane}---${opv.plotCounter}`
                : `${opv.imageOverlayId}--${opv.plotCounter}`;
        opv.plotCounter++;
    } );
    opv.plot= opv.plots[opv.primeIdx];
    opv.visible= true;
    opv.plottingStatusMsg='';
    opv.serverCall='success';

    return opv;
}
