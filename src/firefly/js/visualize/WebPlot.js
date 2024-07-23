/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {ZoomType} from 'firefly/visualize/ZoomType.js';
import {isArray, isBoolean, isEmpty, isNumber, isUndefined} from 'lodash';
import {memorizeLastCall} from '../util/WebUtil';
import CoordinateSys from './CoordSys.js';
import {CysConverter} from './CsysConverter.js';
import PlotState, {makePlotStateShimForHiPS} from './PlotState';
import {makeImagePt} from './Point';
import {makeDevicePt, makeScreenPt, makeWorldPt} from './Point.js';
import {
    HIPS_AITOFF, HIPS_DATA_HEIGHT, HIPS_DATA_WIDTH, makeHiPSProjection, makeProjectionNew, UNRECOGNIZED, UNSPECIFIED
} from './projection/Projection.js';
import {makeDirectFileAccessData, parseSpacialHeaderInfo} from './projection/ProjectionHeaderParser.js';
import {parseWavelengthHeaderInfo} from './projection/WavelengthHeaderParser.js';
import {convertCelestial} from './VisUtil';

export const BLANK_HIPS_URL= 'blank';
export const DEFAULT_BLANK_HIPS_TITLE= 'Blank HiPS Projection';

/**
 * Related data Constants
 * @type {{IMAGE_OVERLAY: string, TABLE: string, IMAGE_MASK: string, WAVELENGTH_TABLE: string, WAVELENGTH_TABLE_RESOLVED: string, SUPPORTED_DATATYPES: string[]}}
 */
export const RDConst= {
    IMAGE_OVERLAY: 'IMAGE_OVERLAY',
    IMAGE_MASK: 'IMAGE_MASK',
    TABLE: 'TABLE',
    WAVELENGTH_TABLE: 'WAVELENGTH_TABLE',
    WAVELENGTH_TABLE_RESOLVED: 'WAVELENGTH_TABLE_RESOLVED',
    SUPPORTED_DATATYPES: ['IMAGE_MASK', 'TABLE']
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
 * @prop {number} colorTableId
 * @prop {Object} header
 * @prop {{cubePlane,cubeHeaderAry}} cubeCtx
 * @prop {number} cubeIdx
 * @prop {PlotState} plotState - the plot state, immutable
 * @prop {number} dataWidth - the width of the image data
 * @prop {number} dataHeight - the height of the image data
 * @prop {number} zoomFactor - the zoom factor
 * @prop {boolean} blank - true if the is a blank plot, default to false
 * @prop {string} title - title of the plot
 * @prop {WebFitsData} webFitsData -  needs documentation
 * @prop {ImageTileData} tileData -  object contains the image tile information
 * @prop {CoordinateSys} imageCoordSys - the image coordinate system
 * @prop {Dimension} screenSize - width/height in screen pixels
 * @prop {Projection} projection - projection routines for this projections
 * @prop {Object} wlData - data object to wave length conversions, if defined then this conversion is available
 * @prop {Object} vradData - data object to vrad conversions, if defined then this conversion is available
 * @prop {{width:number, height:number}} viewDim  size of viewable area  (div size: offsetWidth & offsetHeight)
 * @prop {Object} spectralData - data object to spectral wcs conversions, if defined then this conversion is available
 * @prop {Object} affTrans - the affine transform
 * @prop {Array.<RawData>} rawData
 * @prop {{width:number, height:number}} viewDim  size of viewable area  (div size: offsetWidth & offsetHeight)
 * @prop {Array.<Object>}
 * @prop {Object} attributes
 * directFileAccessDataAry - object of parameters to get flux from the FITS file
 *
 * @see PlotView
 */

/**
 * @global
 * @public
 * @typedef {Object} WebFitsData
 *
 * @prop {number} dataMin
 * @prop {number} dataMax
 * @prop {number} largeBinPercent,
 * @prop {number} fitsFileSize
 * @prop {number} fluxUnits
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

/** // todo - this is mostly wrong, I need to clean it up
 * @typedef {Object} RawData
 *
 * @prop {ThumbnailImage} thumbnailData
 * @prop {number} datamin
 * @prop {number} datamax
 * @prop {Object} processHeader
 * @prop {Object} imageTileDataGroup
 */

/**
 * @typedef ScreenTileDef
 * @summary a single screen tile
 * @prop {number} x - pixel offset of this tile
 * @prop {number} y - pixel offset of this tile
 * @prop {number} width - width of this tile
 * @prop {number} height - height of this tile
 * @prop {number} index - index of this tile
 * @prop {boolean} local - true if generated locally or false it tile is retrieved with url
 * @prop {string} url - file key to use in the service to retrieve this tile
 * @prop {string} key
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

/**
 * @typedef {Object} WebPlotInitializer
 * see java class edu.caltech.ipac.firefly.visualize.WebPlotInitializer
 * @prop imageCoordSys
 * @prop dataWidth
 * @prop dataHeight
 * @prop plotState
 * @prop fitsData
 * @prop desc
 * @prop dataDesc
 * @prop {Array.<Header>} headerAry passed with non-cube images, length 1 for normal images, up to 3 for 3 color images
 * @prop {Array.<Header>} zeroHeaderAry  passed with non-cube images, length 1 for normal images, up to 3 for 3 color images
 * @prop {Array.<RelatedData>} relatedData
 */

/**
 * @typedef {Object} CubeCtx
 * Information common to all cubes
 *
 * @prop {number} cubePlane
 * @prop {number} cubeLength
 * @prop {Array.<Header>} cubeHeaderAry
 * @prop {Array.<RelatedData>} relatedData
 * @prop {number} dataWidth
 * @prop {number} dataHeight
 * @prop imageCoordSys
 * @prop processHeader
 * @prop wlTableRelatedAry
 * @prop wlData
 * @prop getFitsFileSize
 * @prop fluxUnits
 */


const relatedIdRoot= '-Related-';

export const isHiPS= (plot) => Boolean(plot?.plotType==='hips');
export const isImage= (plot) => Boolean(plot?.plotType==='image');
export const isCelestialImage= (plot) => isImage(plot) && Boolean(plot?.projection?.coordSys?.isCelestial());
export const isKnownType= (plot) => Boolean(plot?.plotType==='image' || plot?.plotType==='hips');

/**
 * @param {HipsProperties} hipsProperties
 * @return {string}
 */
export const getHiPsTitleFromProperties= (hipsProperties) => hipsProperties.obs_title || hipsProperties.label || 'HiPS';

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
        blank      : false,
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

function processAllWavelengthAltWcs(header,wlTableRelatedAry) {
    const availableAry= getAtlProjectionIDs(header);
    if (isEmpty(availableAry)) return {};

    return availableAry.reduce( (obj, altChar) => {
        const wlData= parseWavelengthHeaderInfo(header, altChar, undefined, wlTableRelatedAry);
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
     * @param {Dimension} viewDim
     * @param {WebPlotInitializer} wpInit init data returned from server
     * @param {object} attributes any attributes to initialize
     * @param {boolean} asOverlay
     * @param {CubeCtx} [cubeCtx]
     * @param {WebPlotRequest} [request0] - only used when this is part of a cube
     * @param {RangeValues} [rv0] - only used when this is part of a cube
     * @return {WebPlot} the plot
     */
    makeWebPlotData(plotId, viewDim, wpInit, attributes= {}, asOverlay= false, cubeCtx, request0, rv0) {

        const relatedData = cubeCtx ? cubeCtx.relatedData : wpInit.relatedData;
        const plotState= PlotState.makePlotStateWithJson(wpInit.plotState,request0, rv0);
        if (!request0) request0= plotState.getWebPlotRequest();
        const headerAry= !cubeCtx ? wpInit.headerAry : [cubeCtx.cubeHeaderAry[0]];
        const header= headerAry[plotState.firstBand().value];
        const zeroHeader= wpInit.zeroHeaderAry[0];

        let processHeader;
        let wlTableRelatedAry;
        let wlData;
        if (cubeCtx?.processHeader) {
            processHeader= cubeCtx.processHeader;
            wlTableRelatedAry= cubeCtx.wlTableRelatedAry;
            wlData= cubeCtx.wlData;
        }
        else {
            const headerInfo= processHeaderData(wpInit);
            processHeader= headerInfo.processHeader;
            wlTableRelatedAry= headerInfo.wlTableRelatedAry;
            wlData= headerInfo.wlData;
        }
        let projection= makeProjectionNew(processHeader, processHeader.imageCoordSys);
        const processHeaderAry= !plotState.isThreeColor() ?
                                   [processHeader] :
                                    headerAry.map( (h,idx) => parseSpacialHeaderInfo(h,'',wpInit.zeroHeaderAry[idx]));
        const fluxUnitAry= processHeaderAry.map( (p) => p.fluxUnits);
        const rawData= {
            useRed: true, useGreen: true, useBlue:true,
            bandData:processHeaderAry.map( (pH) => ({processHeader:pH, datamin: pH.datamin, datamax:pH.datamax, bias:.5,contrast:1}))
        };


        const allWCSMap= processAllSpacialAltWcs(header);
        const allWlMap= processAllWavelengthAltWcs(header, wlTableRelatedAry);
        if (wlData) {
            allWlMap['']= wlData;
        }
        else if (!isEmpty(allWlMap)) {
            wlData= Object.values(allWlMap)[0];
            allWlMap['']= undefined;
        }

        // if main projection is not available, consider an alternate
        if (!projection.isSpecified() || !projection.isImplemented()) {
            const sortedAltWCSKeys = Object.keys(allWCSMap).sort();
            for (const altWCSKey of sortedAltWCSKeys) {
                const altProjection = allWCSMap[altWCSKey];
                if (altProjection.isSpecified() && altProjection.isImplemented()) {
                    projection = altProjection;
                    break;
                }
            }
        }
        allWCSMap['']= projection;


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
        const zf= getInitZoomLevel(viewDim, request0, dataWidth, dataHeight, projection.getPixelScaleDegree());

        // noinspection JSUnresolvedVariable
        const imagePlot= {
            tileData    : undefined,
            relatedData     : null,
            colorTableId: request0?.getInitialColorTable() ?? 0,
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
            plotDesc        : cubeCtx ? cubeCtx.desc : wpInit.desc,
            dataDesc        : wpInit.dataDesc,
            webFitsData     : isArray(wpInit.fitsData) ? wpInit.fitsData :  wpInit.fitsData ? [wpInit.fitsData] : [{}],
            //=== Mutable =====================
            screenSize: {width:Math.trunc(dataWidth*zf)||1, height:Math.trunc(dataHeight*zf)||1},
            zoomFactor: zf,
            attributes,
            rawData,
            directFileAccessDataAry,
            dataRequested: false,
            cubeIdx: cubeCtx?.cubePlane ?? -1,
            //=== End Mutable =====================
        };
        if (imagePlot.webFitsData) {
            imagePlot.webFitsData=
                imagePlot.webFitsData.map( (wfd) => {
                    let newWfd=  { ...wfd, dataMin: wfd?.dataMin ?? 0, dataMax: wfd?.dataMax ?? 0 };
                    if (cubeCtx) newWfd= {...newWfd, fluxUnits:cubeCtx.fluxUnits, getFitsFileSize:cubeCtx.getFitsFileSize};
                    return newWfd;
                });
        }
        plot= {...plot, ...imagePlot};
        if (relatedData) {
            plot.relatedData= relatedData.map( (d) =>
                ({...d,relatedDataId: plotId+relatedIdRoot+d.dataKey+'-'+dataWidth+'-'+dataHeight}));
        }

        if ((!cubeCtx || cubeCtx.cubePlane===0) && wlData && wlData.failReason)  {
            console.warn(`ImagePlot (${plotId}): Wavelength projection parse error: ${wlData.failReason}`);
        }

        return plot;
    },

    /**
     *
     * @param plotId
     * @param {String} hipsUrlRoot - the url of the hips repository
     * @param wpRequest
     * @param {HipsProperties} hipsProperties
     * @param {PlotAttribute|object} attributes
     * @param {boolean} proxyHips - if true use the proxy (firefly server) to get the hips tailes
     * @return {WebPlot} the new WebPlot object for HiPS
     */
    makeWebPlotDataHIPS(plotId, hipsUrlRoot, wpRequest, hipsProperties, attributes= {}, proxyHips) {

        const blank= isBlankHiPSURL(wpRequest.getHipsRootUrl());
        const hipsCoordSys= makeHiPSCoordSys(hipsProperties);
        const lon= blank ? 0 : Number(hipsProperties.hips_initial_ra) || 0;
        const lat= blank ? 0 : Number(hipsProperties.hips_initial_dec) || 0;
        const projection= makeHiPSProjection(hipsCoordSys, lon,lat, false);
        const plot= makePlotTemplate(plotId,'hips',false, hipsCoordSys);
        const zoomFactor= .0001;

        const hipsPlot= {
            //HiPS specific
            nside: 3,
            colorTableId: -1,
            hipsUrlRoot,
            dataCoordSys : hipsCoordSys,
            hipsProperties,
            proxyHips,

            /// other
            plotState: makePlotStateShimForHiPS(wpRequest),
            projection,
            allWCSMap: {'':projection},
            dataWidth: HIPS_DATA_WIDTH,
            dataHeight: HIPS_DATA_HEIGHT,

            title : blank ? DEFAULT_BLANK_HIPS_TITLE : getHiPsTitleFromProperties(hipsProperties),
            plotDesc: 'a hips plot',
            dataDesc        : hipsProperties.label || 'HiPS',
            blank,
            blankColor: 'rgba(55,55,55,1)',
            cubeDepth: Number(hipsProperties?.hips_cube_depth) || 1,
            //=== Mutable =====================
            screenSize: {width:HIPS_DATA_WIDTH*zoomFactor, height:HIPS_DATA_HEIGHT*zoomFactor},
            cubeIdx: Number(hipsProperties?.hips_cube_firstframe) || 0,
            rawData: {
                useRed: true, useGreen: true, useBlue:true,
                bandData:[{bias:.5,contrast:1},undefined,undefined]
            },
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
     * @param {number} zoomFactor
     * @param {ImageTileData} [rawData]
     * @param {Number} [colorTableId]
     * @param {Number} [bias]
     * @param {Number} [contrast]
     * @param {boolean|undefined} useRed
     * @param {boolean|undefined} useGreen
     * @param {boolean|undefined} useBlue
     * @return {WebPlot}
     */
    replacePlotValues(plot, stateJson, zoomFactor, rawData, colorTableId, bias, contrast, useRed,useGreen, useBlue) {
        const plotState= stateJson ? PlotState.makePlotStateWithJson(stateJson) : plot.plotState;
        const screenSize= {width:plot.dataWidth*zoomFactor, height:plot.dataHeight*zoomFactor};

        //keep the plotState populated with the fitsHeader information, this is only used with get flux calls
        //todo: i think is could be cached on the server side so we don't need to be send it back and forth
        const {bandStateAry}= plotState;
        for(let i=0; (i<bandStateAry.length);i++) {
            if (bandStateAry[i] && isEmpty(bandStateAry[i].directFileAccessData)) {
                bandStateAry[i].directFileAccessData= plot.directFileAccessDataAry[i];
            }
        }

        plot= {...plot,...{plotState, zoomFactor,screenSize}};
        plot.tileData= undefined;
        if (colorTableId>-1) plot.colorTableId= colorTableId;
        if (rawData) plot.rawData= {...plot.rawData};

        if (isNumber(bias) || isNumber(contrast) || isArray(bias) || isArray(contrast) ) {
            const {bandData:oldBandData}= plot.rawData;
            const bandData= oldBandData.map( (entry)  => ({...entry,
                bias:  isNumber(bias) || isArray(bias) ? bias : entry.bias,
                contrast:  isNumber(contrast) || isArray(contrast)? contrast : entry.contrast,
            }));
            plot.rawData= {...plot.rawData,bandData};
        }
        if (isBoolean(useRed) && isBoolean(useGreen) && isBoolean(useBlue) && plot.plotState.isThreeColor()) {
            plot.rawData= {...plot.rawData,useRed,useGreen,useBlue};
        }
        return plot;
    },
};


export const isHiPSAitoff= (plot) => isHiPS(plot) && plot.projection.header.maptype===HIPS_AITOFF;


/**
 * @param {Dimension} viewDim
 * @param {WebPlotRequest} req
 * @param {number} dataWidth
 * @param {number} dataHeight
 * @param {number} pixelScaleDeg
 */
function getInitZoomLevel(viewDim,  req, dataWidth, dataHeight, pixelScaleDeg) {
    const {width,height}= viewDim ?? {};
    let zt= (width && height) ? req.getZoomType() : ZoomType.LEVEL;
    if (zt === ZoomType.ARCSEC_PER_SCREEN_PIX && (!isFinite(pixelScaleDeg) || req.getZoomArcsecPerScreenPix() === 0)) {
        // we can not support ZoomType.ARCSEC_PER_SCREEN_PIX
        zt = ZoomType.TO_WIDTH_HEIGHT;
    }
    switch (zt) {
        case ZoomType.TO_HEIGHT: // this is not implemented, do FULL_SCREEN
        case ZoomType.FULL_SCREEN:
        case ZoomType.TO_WIDTH_HEIGHT:
            return Math.min(width /  dataWidth, height /  dataHeight);
        case ZoomType.TO_WIDTH:
            return width / dataWidth;
        case ZoomType.ARCSEC_PER_SCREEN_PIX:
            return pixelScaleDeg / req.getZoomArcsecPerScreenPix()*3600;
        case ZoomType.LEVEL:
        case ZoomType.STANDARD:
        default:
            return req.getInitialZoomLevel();
    }
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
    const projection= makeHiPSProjection(makeHiPSCoordSys(hipsProperties), wp.x, wp.y, isHiPSAitoff(plot));
    const {coordSys}= projection;
    return { ...plot, imageCoordSys: coordSys, dataCoordSys: coordSys, projection, allWCSMap: {'':projection} };
}

/**
 * @param {WebPlot|undefined} plot
 * @param {Projection} projection
 * @return {WebPlot}
 */
export function replaceProjection(plot, projection) {
    return { ...plot, conversionCache: new Map(), projection, allWCSMap: {'':projection} };
}

/**
 * Return true if this is a WebPlot obj
 * @param {object} o
 * @return boolean - true if this object is a WebPlot
 */
export const isPlot= (o) => Boolean(o && o.plotType && o.plotId && o.plotImageId && o.conversionCache);

/**
 * @param {WebPlot|undefined} plot
 * @param {number} zoomFactor
 * @return {WebPlot}
 */
export const clonePlotWithZoom= (plot,zoomFactor) =>
        plot && {...plot,zoomFactor,screenSize:{width:plot.dataWidth*zoomFactor, height:plot.dataHeight*zoomFactor}};


/**
 * @param {WebPlot} plot
 * @return {{value: number, unit: string}}
 */
export function getScreenPixScale(plot) {
    const screenPixScaleArcsec = getScreenPixScaleArcSec(plot);
    if (!Number.isFinite(screenPixScaleArcsec)) {
        const header = plot?.projection?.header;
        const pixScale = header?.cdelt1 || 0;
        const screenPixScale = pixScale / plot.zoomFactor;
        const unit = header?.cunit1 || '';
        return {value: screenPixScale, unit};
    } else {
        return {value: screenPixScaleArcsec, unit: 'arcsec'};
    }
}

/**
 * @param {WebPlot|CysConverter} plot
 * @param {WorldPt} wp new center of projection
 */
export const changeHiPSProjectionCenter = (plot, wp) => changeHiPSProjectionCenterAndType(plot, wp);

/**
 * @param {WebPlot|CysConverter} plot
 * @param {WorldPt} [wp] new center of projection, if undefined then it will keep the same center point
 * @param {boolean} [fullSky] true if this is a fullSky projection (AITOFF), if undefined then it will stay the same
 * @return {WebPlot}
 */
export function changeHiPSProjectionCenterAndType(plot, wp, fullSky) {
    if (!plot) return undefined;
    const fullSkyToUse = isUndefined(fullSky) ? isHiPSAitoff(plot) : fullSky;
    const cWp = convertCelestial(wp, plot.projection.coordSys);
    const x = cWp?.x ?? plot.projection.header.crval1;
    const y = cWp?.y ?? plot.projection.header.crval2;
    return replaceProjection(plot, makeHiPSProjection(plot.projection.coordSys, x, y, fullSkyToUse));
}

export const getScreenPixScaleArcSec= memorizeLastCall((plot) => {
    if (!plot || !plot.projection || !isKnownType(plot)) return 0;
    if (isImage(plot)) {
        return plot.projection.getPixelScaleArcSec() / plot.zoomFactor;
    }
    else if (isHiPS(plot)) {
        const pt00= makeWorldPt(0,0, plot.imageCoordSys);
        const tmpPlot= changeHiPSProjectionCenter(plot, pt00);
        const cc= CysConverter.make(tmpPlot);
        const scrP= cc.getScreenCoords( pt00);
        const pt2= cc.getWorldCoords( makeScreenPt(scrP.x-1, scrP.y), plot.imageCoordSys);
        return Math.abs(0-pt2.x)*3600; // note don't have to use angular distance formula here, because of the location of the point
    }
},8);


export const getFluxUnits= (plot,band) => (!plot || !band || !isImage(plot)) ? '' : plot.fluxUnitAry[band.value];


/**
 * @param {WebPlot} plot
 * @return {{value: number, unit: string}}
 */
export function getPixScale(plot) {
    const pixScaleArcsec = getPixScaleArcSec(plot);
    if (!Number.isFinite(pixScaleArcsec)) {
        const header = plot?.projection?.header;
        const pixScale = header?.cdelt1 || 0;
        const unit = header?.cunit1 || '';
        return {value: pixScale, unit};
    } else {
        return {value: pixScaleArcsec, unit: 'arcsec'};
    }
}

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
        const tmpPlot= changeHiPSProjectionCenter(plot, pt00);
        const cc= CysConverter.make(tmpPlot);
        const imP= cc.getImageCoords( pt00);
        const pt2= cc.getWorldCoords( makeImagePt(imP.x-1, imP.y), plot.imageCoordSys);
        return Math.abs(0-pt2.x);
    }
    return 0;
}

export function getDevPixScaleDeg(plot) {
    if (!plot?.projection || !isKnownType(plot) ) return 0;
    if (isImage(plot)) {
        return (plot.projection.getPixelScaleArcSec()/3600) / plot.zoomFactor;
    }
    else if (isHiPS(plot)) {
        const pt00= makeWorldPt(0,0, plot.imageCoordSys);
        const tmpPlot= changeHiPSProjectionCenter(plot, pt00);
        const cc= CysConverter.make(tmpPlot);
        const devP= cc.getDeviceCoords( pt00);
        if (!devP) return 0;
        const pt2= cc.getWorldCoords( makeDevicePt(devP.x-1, devP.y), plot.imageCoordSys);
        if (!pt2) return 0;
        return Math.abs(0-pt2.x);
    }
    return 0;
}

export function getImagePixScaleDeg(plot) {
    if (!plot?.projection || !isKnownType(plot) ) return 0;
    if (isImage(plot)) {
        return (plot.projection.getPixelScaleArcSec()/3600) / plot.zoomFactor;
    }
    else if (isHiPS(plot)) {
        const pt00= makeWorldPt(0,0, plot.imageCoordSys);
        const tmpPlot= changeHiPSProjectionCenter(plot, pt00);
        const cc= CysConverter.make(tmpPlot);
        const devP= cc.getImageCoords( pt00);
        const pt2= cc.getWorldCoords( makeImagePt(devP.x-1, devP.y), plot.imageCoordSys);
        return Math.abs(0-pt2.x);
    }
    return 0;
}

export const isBlankHiPSURL= (url) => url.toLowerCase()===BLANK_HIPS_URL;

/**
 * @param {WebPlotInitializer} pC
 * @return {{processHeader:Object, wlData: Object, wlTableRelatedAry:Array}}
 */
export function processHeaderData(pC) {
    const relatedData= pC.relatedData;
    const wlTableRelatedAry= relatedData && relatedData.filter( (r) => r.dataType===RDConst.WAVELENGTH_TABLE_RESOLVED);

    return {
        processHeader: parseSpacialHeaderInfo(pC.headerAry[0],'',pC.zeroHeaderAry[0]),
        wlData: parseWavelengthHeaderInfo(pC.headerAry[0],'',pC.zeroHeaderAry[0], wlTableRelatedAry),
        wlTableRelatedAry
    };
}
