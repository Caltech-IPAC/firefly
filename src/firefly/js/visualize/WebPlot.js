/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import {get,isEmpty} from 'lodash';
import {RequestType} from './RequestType.js';
import {clone} from '../util/WebUtil.js';
import CoordinateSys from './CoordSys.js';
import {makeProjection, makeProjectionNew} from './projection/Projection.js';
import PlotState from './PlotState.js';
import BandState from './BandState.js';
import {makeWorldPt, makeScreenPt} from './Point.js';
import {changeProjectionCenter} from './HiPSUtil.js';
import {CysConverter} from './CsysConverter.js';
import {makeImagePt} from './Point';
import {convert} from './VisUtil.js';
import {parseSpacialHeaderInfo, makeDirectFileAccessData} from './projection/ProjectionInfo.js';
import {UNSPECIFIED, UNRECOGNIZED } from './projection/Projection.js';


export const RDConst= {
    IMAGE_OVERLAY: 'IMAGE_OVERLAY',
    IMAGE_MASK: 'IMAGE_MASK',
    TABLE: 'TABLE',
    SUPPORTED_DATATYPES: ['IMAGE_MASK', 'TABLE']
   // SUPPORTED_DATATYPES: ['IMAGE_MASK', 'TABLE', 'IMAGE_OVERLAY']
};

const HIPS_DATA_WIDTH= 100000;
const HIPS_DATA_HEIGHT= 100000;

const alphabetAry= 'ABCDEFGHIJKLMNOPQRSTUVWZYZ'.split('');




export const getHiPsTitleFromProperties= (hipsProperties) => hipsProperties.obs_title || hipsProperties.label || 'HiPS';

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
     *
     */
    INIT_CENTER: 'INIT_CENTER',

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
     * setting for outline image, bounds (for FootprintObj) or drawObj, text, textLoc,
     */
    OUTLINEIMAGE_BOUNDS: 'OUTLINEIMAGE_BOUNDS',
    OUTLINEIMAGE_TITLE: 'OUTLINEIMAGE_TITLE',
    OUTLINEIMAGE_TITLELOC: 'OUTLINEIMAGE_TITLELOC',
    OUTLINEIMAGE_DRAWOBJ: 'OUTLINE_OBJ',
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
 * @prop {ImageTileData} tileData -  object contains the image tile information
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
 * @prop {number} x - pixel offset of this tile
 * @prop {number} y - pixel offset of this tile
 *
 */

/**
 * @global
 * @public
 * @typedef {Object} HiPSTile
 * @summary a single hips image tile
 *  url computed by: NorderK/DirD/NpixN{.ext}
 *  where
 *  K= nOrder
 *  N= tileNumber
 *  D=(N/10000)*10000 (integer division)
 *
 * @prop {Array.<WorldPt>} corners (maybe) in worldPt
 * @prop {Array.<devpt>} devPtCorners  (maybe) in screenPt (keep here?)
 * @prop {string} url - root url (maybe, don't  know if necessary)
 * @props {number} nOrder (K)
 * @props {number} tileNumber (N)
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

export const isHiPS= (plot) => Boolean(plot && plot.plotType==='hips');
export const isImage= (plot) => Boolean(plot && plot.plotType==='image');
export const isKnownType= (plot) => Boolean(plot && (plot.plotType==='image' || plot.plotType==='hips'));

/**
 *
 * @param plotId
 * @param plotType
 * @param asOverlay
 * @param imageCoordSys
 * @return {WebPlot}
 */
function makePlotTemplate(plotId, plotType, asOverlay, imageCoordSys) {
    return {
        plotId,
        plotType,
        imageCoordSys,
        asOverlay,
        plotImageId     : plotId+'---NEEDS___INIT',
        tileData    : undefined,
        relatedData     : undefined,
        plotState : undefined,
        projection: undefined,
        dataWidth : undefined,
        dataHeight : undefined,
        imageScaleFactor: undefined,
        title : '',
        plotDesc        : '',
        dataDesc        : '',
        webFitsData     : undefined,
        //=== Mutable =====================
        screenSize: {width:0, height:0},
        zoomFactor: 1,
        affTrans : undefined,
        viewDim  : undefined,
        attributes: undefined,

        // a note about conversionCache - the caches (using a map) calls to convert WorldPt to ImagePt
        // have this here breaks the redux paradigm, however it still seems to be the best place. The cache
        // is completely transient. If we start serializing the store there should not be much of an issue.
        conversionCache: new Map(),
        //=== End Mutable =====================
    };
}


function processAllAltWcs(header) {

    const availableAry= alphabetAry.filter( (c) => header['CTYPE1'+c]);
    if (isEmpty(availableAry)) return {};

    return availableAry.reduce( (obj, altChar) => {
        const processHeader= parseSpacialHeaderInfo(header, altChar);
        const {maptype}= processHeader;
        if (!maptype || maptype===UNSPECIFIED ||  maptype===UNRECOGNIZED) {
            //todo did not find a spacial, do some other type of wcs computation
        }
        if (processHeader.headerType==='spacial') {
            obj[altChar]= makeProjectionNew(processHeader, processHeader.imageCoordSys);
        }
        else {
            obj[altChar]= undefined;
        }
        return obj;
    }, {});
}


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

        const plotState= PlotState.makePlotStateWithJson(wpInit.plotState);
        const headerAry= wpInit.headerAry;
        const header= headerAry[plotState.firstBand().value];
        const processHeader= parseSpacialHeaderInfo(header);
        const projection= makeProjectionNew(processHeader, processHeader.imageCoordSys);
        const allWCSMap= processAllAltWcs(header);
        allWCSMap['']= projection;
        const zf= plotState.getZoomLevel();

        for(let i= 0; (i<3); i++) {
            if (headerAry[i]) plotState.get(i).directFileAccessData= makeDirectFileAccessData(headerAry[i]);
        }

        //original plot state come with header information for getting flux.
        // this is only need for one call, so most time we string it out.
        // keeping clientFitsHeaderAry allows a way to put back the original
        //todo: i think is could be cached on the server side so we don't need to be send it back and forth
        const clientFitsHeaderAry= plotState.getBands().map( (b) => plotState.getHeader(b));

        let plot= makePlotTemplate(plotId,'image',asOverlay, CoordinateSys.parse(wpInit.imageCoordSys));

        const imagePlot= {
            tileData    : wpInit.initImages,
            relatedData     : null,
            header,
            headerAry,
            // processHeader: parseSpacialHeaderInfo(header),
            plotState,
            projection,
            allWCSMap,
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
            attributes,
            clientFitsHeaderAry
            //=== End Mutable =====================
        };
        plot= clone(plot, imagePlot);

        if (wpInit.relatedData) {
            plot.relatedData= wpInit.relatedData.map( (d) => clone(d,{relatedDataId: plotId+relatedIdRoot+d.dataKey}));
        }

        return plot;
    },

    /**
     *
     * @param plotId
     * @param wpRequest
     * @param hipsProperties
     * @param desc
     * @param zoomFactor
     * @param attributes
     * @param asOverlay
     * @return {WebPlot} the new WebPlot object for HiPS
     */
    makeWebPlotDataHIPS(plotId, wpRequest, hipsProperties, desc, zoomFactor=1, attributes= {}, asOverlay= false) {

        const plotState= PlotState.makePlotState();

        const bandState= BandState.makeBandState();

        bandState.plotRequestTmp= wpRequest;
        bandState.rangeValuesSerialize = null; // todo
        bandState.rangeValues= null; //todo
        plotState.bandStateAry= [bandState,null,null];
        plotState.ctxStr=null;
        plotState.zoomLevel= 1;
        plotState.threeColor= false;
        plotState.colorTableId= 0;

        const hipsCoordSys= getHiPSCoordSysFromProperties(hipsProperties);
        const lon= Number(hipsProperties.hips_initial_ra) || 0;
        const lat= Number(hipsProperties.hips_initial_dec) || 0;
        const projection= makeHiPSProjection(hipsCoordSys, lon,lat);

        const plot= makePlotTemplate(plotId,'hips',asOverlay, hipsCoordSys);

        const hipsPlot= {
            //HiPS specific
            nside: 3,
            hipsUrlRoot: wpRequest.getHipsRootUrl(),
            dataCoordSys : hipsCoordSys,
            hipsProperties,

            /// other
            plotState,
            projection,
            allWCSMap: {'':projection},
            dataWidth: HIPS_DATA_WIDTH,
            dataHeight: HIPS_DATA_HEIGHT,
            imageScaleFactor: 1,

            title : getHiPsTitleFromProperties(hipsProperties),
            plotDesc        : desc,
            dataDesc        : hipsProperties.label || 'HiPS',
            //=== Mutable =====================
            screenSize: {width:HIPS_DATA_WIDTH*zoomFactor, height:HIPS_DATA_HEIGHT*zoomFactor},
            cubeDepth: Number(get(hipsProperties, 'hips_cube_depth')) || 1,
            cubeIdx: Number(get(hipsProperties, 'hips_cube_firstframe')) || 0,
            zoomFactor,
            attributes,

            //=== End Mutable =====================

        };

        return clone(plot, hipsPlot);
    },


    /**
     *
     * @param {WebPlot} plot
     * @param {object} stateJson
     * @param {ImageTileData} tileData
     * @return {*}
     */
    setPlotState(plot,stateJson,tileData) {
        const plotState= PlotState.makePlotStateWithJson(stateJson);
        const zf= plotState.getZoomLevel();
        const screenSize= {width:plot.dataWidth*zf, height:plot.dataHeight*zf};

        //keep the plotState populated with the fitsHeader information, this is only used with get flux calls
        //todo: i think is could be cached on the server side so we don't need to be send it back and forth
        const {bandStateAry}= plotState;
        for(let i=0; (i<bandStateAry.length);i++) {
            if (bandStateAry[i] && isEmpty(bandStateAry[i].directFileAccessData)) {
                bandStateAry[i].directFileAccessData= plot.clientFitsHeaderAry[i];
            }
        }

        plot= {...plot,...{plotState, zoomFactor:zf,screenSize}};
        if (tileData) plot.tileData= tileData;
        return plot;
    },


};


/**
 *
 * @param {CoordinateSys} coordinateSys
 * @param lon
 * @param lat
 * @return {Projection}
 */
function makeHiPSProjection(coordinateSys, lon=0, lat=0) {
    const header= {
        cdelt1: 180/HIPS_DATA_WIDTH,
        cdelt2: 180/HIPS_DATA_HEIGHT,
        maptype: 5,
        crpix1: HIPS_DATA_WIDTH*.5,
        crpix2: HIPS_DATA_HEIGHT*.5,
        crval1: lon,
        crval2: lat

    };
    return makeProjection({header, coorindateSys:coordinateSys.toString()});
}


function getHiPSCoordSysFromProperties(hipsProperties) {
    switch (hipsProperties.hips_frame) {
        case 'equatorial' : return CoordinateSys.EQ_J2000;
        case 'galactic' :   return CoordinateSys.GALACTIC;
        case 'ecliptic' :   return CoordinateSys.ECL_B1950;
    }
    if (!hipsProperties.hips_frame) {
        switch (hipsProperties.coordsys) { // fallback using old style
            case 'C' : return CoordinateSys.EQ_J2000;
            case 'G' : return CoordinateSys.GALACTIC;
            case 'E' : return CoordinateSys.ECL_B1950;
        }
    }
    return CoordinateSys.GALACTIC;
}

function makeHiPSProjectionUsingProperties(hipsProperties, lon=0, lat=0) {
    return makeHiPSProjection(getHiPSCoordSysFromProperties(hipsProperties), lon,lat);
}


/**
 * replace the hips projection if the coordinate system changes
 * @param {WebPlot} plot
 * @param hipsProperties
 * @param {WorldPt} wp
 */
export function replaceHiPSProjectionUsingProperties(plot, hipsProperties, wp= makeWorldPt(0,0)) {
    const projection= makeHiPSProjectionUsingProperties(hipsProperties, wp.x, wp.y);
    const retPlot= clone(plot);
    retPlot.imageCoordSys= projection.coordSys;
    retPlot.dataCoordSys= projection.coordSys;
    retPlot.projection= projection;
    retPlot.allWCSMap= {'':projection};
    return retPlot;
}

/**
 * replace the hips projection if the coordinate system changes
 * @param {WebPlot} plot
 * @param coordinateSys
 * @param {WorldPt} wp
 */
export function replaceHiPSProjection(plot, coordinateSys, wp= makeWorldPt(0,0)) {
    const newWp= convert(wp, coordinateSys);
    const projection= makeHiPSProjection(coordinateSys, newWp.x, newWp.y);
    const retPlot= clone(plot);
    retPlot.imageCoordSys= projection.coordSys;
    //note- the dataCoordSys stays the same
    retPlot.projection= projection;
    retPlot.allWCSMap= {'':projection};
    return retPlot;
}


/**
 * replace the header in the transform of the plot object
 * @param {WebPlot} plot
 * @param {Object} header
 * @return {WebPlot}
 */
export function replaceHeader(plot, header) {
    const retPlot= clone(plot);
    retPlot.conversionCache= new Map();
    retPlot.projection= makeProjection({header:clone(header), coorindateSys:plot.projection.coordSys.toString()});
    retPlot.allWCSMap= {'':retPlot.projection};
    return retPlot;
}





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


/**
 *
 * @param {WebPlot|CysConverter} plot
 * @return {number}
 */
export function getScreenPixScaleArcSec(plot) {
    if (!plot || !plot.projection || !isKnownType(plot)) return 0;
    if (isImage(plot)) {
        return plot.projection.getPixelScaleArcSec() / plot.zoomFactor;
    }
    else if (isHiPS(plot)) {
        const pt00= makeWorldPt(0,0, plot.imageCoordSys);
        const tmpPlot= changeProjectionCenter(plot, pt00);
        const cc= CysConverter.make(tmpPlot);
        const scrP= cc.getScreenCoords( pt00);
        const pt2= cc.getWorldCoords( makeScreenPt(scrP.x-1, scrP.y), plot.imageCoordSys);
        return Math.abs(0-pt2.x)*3600; // note have to use angular distance formula here, because of the location of the point
    }
    return 0;
}


export function getFluxUnits(plot,band) {
    if (!band) return '';
    return get(plot,['webFitsData',band.value,'fluxUnits'], '');
}


/**
 *
 * @param {WebPlot|CysConverter} plot
 * @return {number}
 */
export function getPixScaleArcSec(plot) {
    return getPixScaleDeg(plot)*3600;
}

/**
 *
 * @param {WebPlot|CysConverter} plot
 * @return {number}
 */
export function getPixScaleDeg(plot) {
    if (!plot || !plot.projection || !isKnownType(plot) ) return 0;
    if (!plot || !plot.projection) return 0;
    if (isImage(plot)) {
        return plot.projection.getPixelScaleDegree();
    }
    else if (isHiPS(plot)) {
        const pt00= makeWorldPt(0,0, plot.imageCoordSys);
        const tmpPlot= changeProjectionCenter(plot, pt00);
        const cc= CysConverter.make(tmpPlot);
        const imP= cc.getImageCoords( pt00);
        const pt2= cc.getWorldCoords( makeImagePt(imP.x-1, imP.y), plot.imageCoordSys);
        return Math.abs(0-pt2.x);
    }
    return 0;
}


