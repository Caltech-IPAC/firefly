/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isArray, isEmpty, isObject} from 'lodash';
import {RequestType} from './RequestType.js';
import CoordinateSys from './CoordSys.js';
import {makeProjection, makeProjectionNew, UNRECOGNIZED, UNSPECIFIED} from './projection/Projection.js';
import PlotState from './PlotState.js';
import {makeScreenPt, makeWorldPt} from './Point.js';
import {changeProjectionCenter} from './HiPSUtil.js';
import {CysConverter} from './CsysConverter.js';
import {makeImagePt} from './Point';
import {makeDirectFileAccessData, parseSpacialHeaderInfo} from './projection/ProjectionHeaderParser.js';
import {parseWavelengthHeaderInfo} from './projection/WavelengthHeaderParser.js';
import {TAB} from './projection/Wavelength';
import {memorizeLastCall} from '../util/WebUtil';
import {makePlotStateShimForHiPS} from './PlotState';
import {hasLocalRawDataInStore} from './rawData/RawDataOps.js';


export const RDConst= {
    IMAGE_OVERLAY: 'IMAGE_OVERLAY',
    IMAGE_MASK: 'IMAGE_MASK',
    TABLE: 'TABLE',
    WAVELENGTH_TABLE: 'WAVELENGTH_TABLE',
    SUPPORTED_DATATYPES: ['IMAGE_MASK', 'TABLE']
};

const HIPS_DATA_WIDTH=  10000000000;
const HIPS_DATA_HEIGHT= 10000000000;


/**
 *
 * @param {HipsProperties} hipsProperties
 * @return {string}
 */
export const getHiPsTitleFromProperties= (hipsProperties) => hipsProperties.obs_title || hipsProperties.label || 'HiPS';

/**
 * FITS headers keys
 * todo: add more headers
 */



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
 * @prop {{cubePlane,cubeHeaderAry}} cubeCtx
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
 * @prop {Object} wlData - data object to wave length conversions, if defined then this conversion is available
 * @prop {Object} affTrans - the affine transform
 * @prop {Array.<RawData>} rawData
 * @prop {{width:number, height:number}} viewDim  size of viewable area  (div size: offsetWidth & offsetHeight)
 * @prop {Array.<Object>} directFileAccessDataAry - object of parameters to get flux from the FITS file
 * @prop {boolean} localDataLoaded
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
 * RelatedData array entries for a plot should have a unique dataKey
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
 * @typedef {Object} HiPSTile
 * @summary a single hips image tile
 *  url computed by: NorderK/DirD/NpixN{.ext}
 *  where
 *  K= nOrder
 *  N= tileNumber
 *  D=(N/10000)*10000 (integer division)
 *
 * @prop {Array.<WorldPt>} corners (maybe) in worldPt
 * @prop {Array.<DevicePt>} devPtCorners  (maybe) in screenPt (keep here?)
 * @prop {string} url - root url (maybe, don't  know if necessary)
 * @prop {number} nOrder (K)
 * @prop {number} tileNumber (N)
 *
 */

/** //todo this is not right for 3 color, float1d, processHeader, should be for each band
 * @typedef {Object} RawData
 *
 * Notes - float1d and histogram should never change change.
 * - stretchDataTiles need to change when range values changes (different stretch)
 * - imageTileDataGroup should change when stretchDataTiles
 * - when color table changes, we can reuse stretchDataTiles and imageTileDataGroup
 *
 * @prop {Float32Array} float1d
 * @prop {HistogramData} histogram
 * @prop {number} dataWidth
 * @prop {number} dataHeight
 * @prop {Array<Int8Array>} stretchedDataTiles
 * @prop {Array<Int8Array>} color
 * @prop {ThumbnailImage} thumbnailData
 * @prop {Array.<{x:number,y:number,width:number,height:number,local:boolean}>} localScreenTileDefList
 * @prop {number} datamin
 * @prop {number} datamax
 * @prop {Object} processHeader
 * @prop {Object} imageTileDataGroup
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


/**
 *
 * @typedef {Object} HipsProperties
 * This is some of the properties that are in the HiPS property file.  There can be anything, this is what we
 * are using.
 *
 * @prop hips_initial_ra
 * @prop hips_initial_dec
 * @prop {string} label
 * @prop coordsys
 * @prop hips_cube_depth
 * @prop hips_cube_firstframe
 * @prop hips_frame
 * @prop {string} obs_title
 */


const relatedIdRoot= '-Related-';

export const isHiPS= (plot) => Boolean(plot?.plotType==='hips');
export const isImage= (plot) => Boolean(plot?.plotType==='image');
export const isKnownType= (plot) => Boolean(plot?.plotType==='image' || plot?.plotType==='hips');

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
        plotImageId: plotId+'---NEEDS___INIT',
        tileData   : undefined,
        relatedData: undefined,
        plotState  : undefined,
        projection : undefined,
        dataWidth  : undefined,
        dataHeight : undefined,
        title      : '',
        plotDesc   : '',
        dataDesc   : '',
        webFitsData: undefined,
        //=== Mutable =====================
        screenSize : {width:0, height:0},
        zoomFactor : 1,
        affTrans   : undefined,
        viewDim    : undefined,
        attributes : undefined,

        // a note about conversionCache - the caches (using a map) calls to convert WorldPt to ImagePt
        // have this here breaks the redux paradigm, however it still seems to be the best place. The cache
        // is completely transient. If we start serializing the store there should not be much of an issue.
        conversionCache: new Map(),
        //=== End Mutable =====================
    };
}

const alphabetAry= 'ABCDEFGHIJKLMNOPQRSTUVWZYZ'.split('');

/**
 * return an array of all the alt projections in this file.
 * @param header
 * @return {string[]}
 */
const getAtlProjectionIDs= (header) => alphabetAry.filter( (c) => header['CTYPE1'+c]);

function processAllSpacialAltWcs(header) {
    const availableAry= getAtlProjectionIDs(header);
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

function processAllWavelengthAltWcs(header,wlTable) {
    const availableAry= getAtlProjectionIDs(header);
    if (isEmpty(availableAry)) return {};

    return availableAry.reduce( (obj, altChar) => {
        const wlData= parseWavelengthHeaderInfo(header, altChar, undefined, wlTable);
        if (wlData) obj[altChar]= wlData;
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
     * @param {{cubePlane,cubeHeaderAry,relatedData,dataWidth,dataHeight,imageCoordSys}} cubeCtx
     * @return {WebPlot} the plot
     */
    makeWebPlotData(plotId, wpInit, attributes= {}, asOverlay= false, cubeCtx) {

        const relatedData = cubeCtx ? cubeCtx.relatedData : wpInit.relatedData;
        const plotState= PlotState.makePlotStateWithJson(wpInit.plotState);
        const headerAry= !cubeCtx ? wpInit.headerAry : [cubeCtx.cubeHeaderAry[0]];
        const header= headerAry[plotState.firstBand().value];
        const zeroHeader= wpInit.zeroHeaderAry[0];
        const processHeader= parseSpacialHeaderInfo(header,'',zeroHeader);
        const projection= makeProjectionNew(processHeader, processHeader.imageCoordSys);
        const processHeaderAry= !plotState.isThreeColor() ?
                                   [processHeader] :
                                    headerAry.map( (h,idx) => parseSpacialHeaderInfo(h,'',wpInit.zeroHeaderAry[idx]));
        const fluxUnitAry= processHeaderAry.map( (p) => p.fluxUnits);
        const rawData= {bandData:processHeaderAry.map( (pH) => ({processHeader:pH, datamin: pH.datamin, datamax:pH.datamax}))};


        const wlRelated= relatedData && relatedData.find( (r) => r.dataType==='WAVELENGTH_TABLE_RESOLVED');

        let wlData= parseWavelengthHeaderInfo(header, '', zeroHeader, wlRelated?.table);
        const allWCSMap= processAllSpacialAltWcs(header);
        const allWlMap= processAllWavelengthAltWcs(header, wlRelated?.table);
        allWlMap['']= wlData;
        allWCSMap['']= projection;
        if (Object.values(allWlMap).length>0 && wlData?.algorithm!==TAB) {
            wlData= Object.values(allWlMap)[0];
        }
        const zf= plotState.getZoomLevel();

        // because of history we keep directFileAccessData in the plot state, however now we compute it on the client
        // also- we need to keep a copy in plotState for backward compatibility and in the plot to put in back in the plotState
        // when a new one is generated
        for(let i= 0; (i<3); i++) {
            if (headerAry[i]) plotState.get(i).directFileAccessData= makeDirectFileAccessData(headerAry[i], cubeCtx?.cubePlane ?? -1);
        }
        const directFileAccessDataAry= plotState.bandStateAry.map( (bs) => bs.directFileAccessData);

        const imageCoordSys= cubeCtx ? cubeCtx.imageCoordSys : wpInit.imageCoordSys;
        let plot= makePlotTemplate(plotId,'image',asOverlay, CoordinateSys.parse(imageCoordSys));
        const dataWidth= cubeCtx ? cubeCtx.dataWidth : wpInit.dataWidth;
        const dataHeight= cubeCtx ? cubeCtx.dataHeight : wpInit.dataHeight;

        // noinspection JSUnresolvedVariable
        const imagePlot= {
            tileData    : wpInit.initImages,
            relatedData     : null,
            header,
            headerAry,
            zeroHeader,
            fluxUnitAry,
            cubeCtx,
            plotState,
            projection,
            wlData,
            allWCSMap,
            allWlMap,
            dataWidth,
            dataHeight,
            title : '',
            plotDesc        : wpInit.desc,
            dataDesc        : wpInit.dataDesc,
            webFitsData     : isArray(wpInit.fitsData) ? wpInit.fitsData : [wpInit.fitsData],
            //=== Mutable =====================
            screenSize: {width:Math.trunc(dataWidth*zf), height:Math.trunc(dataHeight*zf)},
            zoomFactor: zf,
            attributes,
            rawData,
            directFileAccessDataAry,
            localDataLoaded: false,
            cubeIdx: cubeCtx?.cubePlane ?? -1,
            //=== End Mutable =====================
        };
        plot= {...plot, ...imagePlot};
        if (relatedData) {
            plot.relatedData= relatedData.map( (d) => ({...d,relatedDataId: plotId+relatedIdRoot+d.dataKey}));
        }

        if ((!cubeCtx || cubeCtx.cubePlane===0) && wlData && wlData.failWarning)  {
            console.warn(`ImagePlot (${plotId}): Wavelength projection parse error: ${wlData.failWarning}`);
        }

        return plot;
    },

    /**
     *
     * @param plotId
     * @param wpRequest
     * @param {HipsProperties} hipsProperties
     * @param desc
     * @param zoomFactor
     * @param {PlotAttribute|object} attributes
     * @param asOverlay
     * @return {WebPlot} the new WebPlot object for HiPS
     */
    makeWebPlotDataHIPS(plotId, wpRequest, hipsProperties, desc, zoomFactor=1, attributes= {}, asOverlay= false) {

        const hipsCoordSys= makeHiPSCoordSys(hipsProperties);
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
            plotState: makePlotStateShimForHiPS(wpRequest),
            projection,
            allWCSMap: {'':projection},
            dataWidth: HIPS_DATA_WIDTH,
            dataHeight: HIPS_DATA_HEIGHT,

            title : getHiPsTitleFromProperties(hipsProperties),
            plotDesc        : desc,
            dataDesc        : hipsProperties.label || 'HiPS',
            cubeDepth: Number(hipsProperties?.hips_cube_depth) || 1,
            //=== Mutable =====================
            screenSize: {width:HIPS_DATA_WIDTH*zoomFactor, height:HIPS_DATA_HEIGHT*zoomFactor},
            cubeIdx: Number(hipsProperties?.hips_cube_firstframe) || 0,
            zoomFactor,
            attributes,
            //=== End Mutable =====================

        };
        return {...plot, ...hipsPlot};
    },


    /**
     *
     * @param {WebPlot} plot
     * @param {object} stateJson
     * @param {ImageTileData} [tileData]
     * @param {ImageTileData} [rawData]
     * @return {WebPlot}
     */
    setPlotState(plot,stateJson,tileData,rawData) {
        const plotState= PlotState.makePlotStateWithJson(stateJson);
        const zf= plotState.getZoomLevel();
        const screenSize= {width:plot.dataWidth*zf, height:plot.dataHeight*zf};

        //keep the plotState populated with the fitsHeader information, this is only used with get flux calls
        //todo: i think is could be cached on the server side so we don't need to be send it back and forth
        const {bandStateAry}= plotState;
        for(let i=0; (i<bandStateAry.length);i++) {
            if (bandStateAry[i] && isEmpty(bandStateAry[i].directFileAccessData)) {
                bandStateAry[i].directFileAccessData= plot.directFileAccessDataAry[i];
            }
        }

        plot= {...plot,...{plotState, zoomFactor:zf,screenSize}};
        if (tileData) plot.tileData= tileData;
        if (rawData) plot.rawData= {...plot.rawData, localScreenTileDefList:rawData.localScreenTileDefList};
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
export function makeHiPSProjection(coordinateSys, lon=0, lat=0) {
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



/**
 * @param {HipsProperties} hipsProperties
 * @return {CoordinateSys}
 */
function makeHiPSCoordSys(hipsProperties) {
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

/**
 * replace the hips projection if the coordinate system changes
 * @param {WebPlot} plot
 * @param {HipsProperties} hipsProperties
 * @param {WorldPt} wp
 */
export function replaceHiPSProjectionUsingProperties(plot, hipsProperties, wp= makeWorldPt(0,0)) {
    const projection= makeHiPSProjection(makeHiPSCoordSys(hipsProperties), wp.x, wp.y);
    const {coordSys}= projection;
    return { ...plot, imageCoordSys: coordSys, dataCoordSys: coordSys, projection, allWCSMap: {'':projection} };
}


/**
 * replace the header in the transform of the plot object
 * @param {WebPlot} plot
 * @param {Object} header
 * @return {WebPlot}
 */
export function replaceHeader(plot, header) {
    const projection= makeProjection({header:{...header}, coorindateSys:plot.projection.coordSys.toString()});
    return { ...plot, conversionCache: new Map(), projection, allWCSMap: {'':projection} };
}

/**
 * Return true if this is a WebPlot obj
 * @param {object} o
 * @return boolean - true if this object is a WebPlot
 */
export const isPlot= (o) => isObject(o) && Boolean(o.plotType && o.plotId && o.plotImageId && o.conversionCache);

// noinspection JSUnresolvedVariable
/**
 * @deprecated - we are moving away from blank images. So this function will soon be unnecessary
 * Check if the plot is is a blank image
 * @param {WebPlot} plot - the plot
 * @return {boolean}
 */
export const isBlankImage= (plot) =>
        !plot?.plotState.isThreeColor() && plot?.plotState.getWebPlotRequest()?.getRequestType()===RequestType.BLANK;

/**
 * @param {WebPlot} plot
 * @param {number} zoomFactor
 * @return {WebPlot}
 */
export const clonePlotWithZoom= (plot,zoomFactor) =>
        plot && {...plot,zoomFactor,screenSize:{width:plot.dataWidth*zoomFactor, height:plot.dataHeight*zoomFactor}};

export const getScreenPixScaleArcSec= memorizeLastCall((plot) => {
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
},8);

export const getFluxUnits= (plot,band) => (!plot || !band || !isImage(plot)) ? '' : plot.fluxUnitAry[band.value];

/**
 * @param {WebPlot|CysConverter} plot
 * @return {number}
 */
export const getPixScaleArcSec= (plot) => getPixScaleDeg(plot)*3600;

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

export function hasLocalRawData(plot) {
    return hasLocalRawDataInStore(plot);
}



