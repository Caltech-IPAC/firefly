/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import {RequestType} from './RequestType.js';
import {clone} from '../util/WebUtil.js';
import CoordinateSys from './CoordSys.js';
import {makeProjection} from './projection/Projection.js';
import PlotState from './PlotState.js';



export const RDConst= {
    IMAGE_OVERLAY: 'IMAGE_OVERLAY',
    IMAGE_MASK: 'IMAGE_MASK',
    TABLE: 'TABLE',
    SUPPORTED_DATATYPES: ['IMAGE_MASK', 'TABLE']
   // SUPPORTED_DATATYPES: ['IMAGE_MASK', 'TABLE', 'IMAGE_OVERLAY']
};

export const PlotAttribute= {




    MOVING_TARGET_CTX_ATTR:   'MOVING_TARGET_CTX_ATTR',

    /**
     * This will probably be a WebMouseReadoutHandler class
     * @see WebMouseReadoutHandler
     */
    READOUT_ATTR: 'READOUT_ATTR',

    READOUT_ROW_PARAMS: 'READOUT_ROW_PARAMS',

    /**
     * This will probably be a WorldPt
     * Used to overlay a target associated with this image
     */
    FIXED_TARGET: 'FIXED_TARGET',

    /**
     * This will probably be a double with the requested size of the plot
     */
    REQUESTED_SIZE: 'REQUESTED_SIZE',


    /**
     * This will probably an object represent a rectangle {pt0: point,pt1: point}
     * @See ./Point.js
     */
    SELECTION: 'SELECTION',

    IMAGE_BOUNDS_SELECTION: 'IMAGE_BOUNDS_SELECTION',

    /**
     * This will probably an object to represent a line {pt0: point,pt1: point}
     * @See ./Point.js
     */
    ACTIVE_DISTANCE: 'ACTIVE_DISTANCE',

    SHOW_COMPASS: 'SHOW_COMPASS',

    /**
     * This will probably an object {pt: point}
     * @See ./Point.js
     */
    ACTIVE_POINT: 'ACTIVE_POINT',


    /**
     * This is a String describing why this plot can't be rotated.  If it is defined then
     * rotating is disabled.
     */
    DISABLE_ROTATE_REASON: 'DISABLE_ROTATE_HINT',

    /**
     * what should happen when multi-fits images are changed.  If set the zoom is set to the same level
     * eg 1x, 2x ect.  If not set then flipping should attempt to make the image the same arcsec/screen pixel.
     */
    FLIP_ZOOM_BY_LEVEL: 'FLIP_ZOOM_BY_LEVEL',

    /**
     * what should happen when multi-fits images are changed.  If set the zoom is set to the same level
     * eg 1x, 2x ect.  If not set then flipping should attempt to make the image the same arcsec/screen pixel.
     */
    FLIP_ZOOM_TO_FILL: 'FLIP_ZOOM_TO_FILL',

    /**
     * if set, when expanded the image will be zoom to no bigger than this level,
     * this should be a subclass of Number
     */
    MAX_EXPANDED_ZOOM_LEVEL : 'MAX_EXPANDED_ZOOM_LEVEL',

    /**
     * if set, this should be the last expanded single image zoom level.
     * this should be a subclass of Number
     */
    LAST_EXPANDED_ZOOM_LEVEL : 'LAST_EXPANDED_ZOOM_LEVEL',

    /**
     * if set, must be one of the string values defined by the enum ZoomUtil.FullType
     * currently is is ONLY_WIDTH, WIDTH_HEIGHT, ONLY_HEIGHT
     */
    EXPANDED_TO_FIT_TYPE : 'MAX_EXPANDED_ZOOM_LEVEL',

    /**
     * if true, the readout will be very small
     */
    MINIMAL_READOUT : 'MINIMAL_READOUT',

    TABLE_ROW : 'TABLE_ROW',


    UNIQUE_KEY : 'UNIQUE_KEY'
};


/**
 * @global
 * @public
 * @typedef {Object} Dimension
 *
 * @prop {number} width
 * @prop {number} height
 *
 */



/**
 * @global
 * @public
 * @typedef {Object} ViewPort
 * @summary Make a viewport object
 * @prop {number} x  - x location that the viewport begins
 * @prop {number} y - y location that the viewport begins
 * @prop {Dimension} dim - dimensions of the viewport
 */

/**
 * @global
 * @public
 * @typedef {Object} WebPlot
 *
 * @summary This class contains plot information.
 * Publicly this class operations in many coordinate system.
 * Some include a Image coordinate system, a world coordinate system, and a screen
 * coordinate system.
 *
 * @prop {String} plotId - plot id, id of the plotView, immutable
 * @prop {String} plotImageId,  - plot image id, id of this WebPlot , immutable
 * @prop {Object} serverImage, immutable
 * @prop {String} title - the title
 * @prop {PlotState} plotState - the plot state, immutable
 * @prop {number} dataWidth - the width of the image data
 * @prop {number} dataHeight - the height of the image data
 * @prop {number} zoomFactor - the zoom factor
 * @prop {string} title - title of the plot
 * @prop {object} webFitsData -  needs documentation
 * @prop {ImageTileData} serverImages -  object contains the image tile information
 * @prop {CoordinateSys} imageCoordSys - the image coordinate system
 * @prop {Dimension} screenSize - width/height in screen pixels
 * @prop {Projection} projection - projection routines for this projections
 * @prop {Object} affTrans - the affine transform
 * @prop {{width:number, height:number}} viewDim  size of viewable area  (div size: offsetWidth & offsetHeight)
 *
 * @see PlotView
 */



/**
 * @global
 * @public
 * @typedef {Object} RelatedData
 * @summary overlay data that is associated with the image data
 *
 * @prop {string} relatedDataId - a globally unique id made from the plotId and the dataKey - this is added by the client and does
 * not come from the server
 * @prop {string} dataKey - should be a unique string key an array of plot of RelatedData, that is all
 * RelatedData array entries for a plot should have a unqiue dataKey
 * @prop {string} dataType - one of 'IMAGE_OVERLAY', 'IMAGE_MASK', 'TABLE'
 * @prop {string} desc - user description of the data
 * @prop {Object.<string, string>} searchParams - map of search parameters to get the related data
 * @prop {Object.<string, string>} availableMask - only used for masks- key is the bit number, value is the description
 *
 */



/**
 * @global
 * @public
 * @typedef {Object} ThumbnailImage
 * @summary the thumbnail information
 *
 * @prop {number} width - width of thumbnail
 * @prop {number} height - height of thumbnail
 * @prop {string} url - file key to use in the service to retrieve this tile
 *
 */

/**
 * @global
 * @public
 * @typedef {Object} ImageTile
 * @summary a single image tile
 *
 * @prop {number} width - width of this tile
 * @prop {number} height - height of this tile
 * @prop {number} index - index of this tile
 * @prop {string} url - file key to use in the service to retrieve this tile
 * @prop {number} xoff - pixel offset of this tile
 * @prop {number} yoff - pixel offset of this tile
 *
 */

/**
 * @global
 * @public
 * @typedef {Object} ImageTileData
 * @summary The information about all the image tiles
 *
 * @prop {Array.<ImageTile>} images
 * @prop {number} screenWidth - width of all the tiles
 * @prop {number} screenHeight - height of all the tiles
 * @prop {String} templateName - template name (not used)
 * @prop {number} zfact - zoom factor
 * @prop {ThumbnailImage} thumbnailImage - information about the thumbnail
 *
 */


const relatedIdRoot= '-Related-';


/**
 *
 */
export const WebPlot= {

    /**
     *
     * @param {string} plotId
     * @param wpInit init data returned from server
     * @param {object} attributes any attributes to initialize
     * @param {boolean} asOverlay
     * @return {WebPlot} the plot
     */
    makeWebPlotData(plotId, wpInit, attributes= {}, asOverlay= false) {

        const projection= makeProjection(wpInit.projectionJson);
        const plotState= PlotState.makePlotStateWithJson(wpInit.plotState);
        const zf= plotState.getZoomLevel();

        const plot= {
            plotId,
            plotImageId     : plotId+'---NEEDS___INIT',
            serverImages    : wpInit.initImages,
            imageCoordSys   : CoordinateSys.parse(wpInit.imageCoordSys),
            relatedData     : null,
            plotState,
            projection,
            dataWidth       : wpInit.dataWidth,
            dataHeight      : wpInit.dataHeight,
            imageScaleFactor: wpInit.imageScaleFactor,

            title : '',
            plotDesc        : wpInit.desc,
            dataDesc        : wpInit.dataDesc,
            webFitsData     : wpInit.fitsData,
            //=== Mutable =====================
            screenSize: {width:wpInit.dataWidth*zf, height:wpInit.dataHeight*zf},
            zoomFactor: zf,
            alive    : true,
            affTrans : null,
            viewDim  : null,
            attributes,

                 // a note about conversionCache - the caches (using a map) calls to convert WorldPt to ImagePt
                 // have this here breaks the redux paradigm, however it still seems to be the best place. The cache
                 // is completely transient. If we start serializing the store there should not be much of an issue.
            conversionCache: new Map(),
            //=== End Mutable =====================
            asOverlay
        };

        if (wpInit.relatedData) {
            plot.relatedData= wpInit.relatedData.map( (d) => clone(d,{relatedDataId: plotId+relatedIdRoot+d.dataKey}));
        }

        return plot;
    },


    /**
     *
     * @param {WebPlot} wpData
     * @param {object} stateJson
     * @param {object} serverImages
     * @return {*}
     */
    setPlotState(wpData,stateJson,serverImages) {
        const plotState= PlotState.makePlotStateWithJson(stateJson);
        const zf= plotState.getZoomLevel();
        const screenSize= {width:wpData.dataWidth*zf, height:wpData.dataHeight*zf};
        const plot= Object.assign({},wpData,{plotState, zoomFactor:zf,screenSize});
        if (serverImages) plot.serverImages= serverImages;
        return plot;
    },


};


/**
 * Check if the plot is is a blank image
 * @param {WebPlot} plot - the plot
 * @return {boolean}
 */
export function isBlankImage(plot) {
    if (plot.plotState.isThreeColor()) return false;
    const req= plot.plotState.getWebPlotRequest();
    return (req && req.getRequestType()===RequestType.BLANK);
}

/**
 *
 * @param {WebPlot} plot
 * @param {number} zoomFactor
 * @return {WebPlot}
 */
export function clonePlotWithZoom(plot,zoomFactor) {
    if (!plot) return null;
    const screenSize= {width:plot.dataWidth*zoomFactor, height:plot.dataHeight*zoomFactor};
    return Object.assign({},plot,{zoomFactor,screenSize});
}

