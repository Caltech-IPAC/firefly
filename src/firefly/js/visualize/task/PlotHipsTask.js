/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isEmpty} from 'lodash';
import ImagePlotCntlr, {visRoot, IMAGE_PLOT_KEY,
    dispatchChangeCenterOfProjection, dispatchZoom,
    dispatchAttributeChange,
    dispatchPlotProgressUpdate, dispatchPlotImage, dispatchPlotHiPS,
    dispatchChangeHiPS} from '../ImagePlotCntlr.js';
import {getArcSecPerPix, getZoomLevelForScale, UserZoomTypes} from '../ZoomUtil.js';
import {WebPlot, PlotAttribute} from '../WebPlot.js';
import {fetchUrl, clone, loadImage} from '../../util/WebUtil.js';
import {getPlotGroupById} from '../PlotGroup.js';
import {primePlot, getPlotViewById, hasGroupLock} from '../PlotViewUtil.js';
import {dispatchAddActionWatcher} from '../../core/MasterSaga.js';
import {getHiPSZoomLevelToFit} from '../HiPSUtil.js';
import {getCenterOfProjection, findCurrentCenterPoint, getCorners,
        getDrawLayerByType, getDrawLayersByType, getOnePvOrGroup, getFoV} from '../PlotViewUtil.js';
import {findAllSkyCachedImage, addAllSkyCachedImage} from '../iv/HiPSTileCache.js';
import {makeHiPSAllSkyUrl, makeHiPSAllSkyUrlFromPlot,
         makeHipsUrl, resolveHiPSConstant, getPointMaxSide, getPropertyItem} from '../HiPSUtil.js';
import {ZoomType} from '../ZoomType.js';
import {CCUtil} from '../CsysConverter.js';
import {ensureWPR, determineViewerId, getHipsImageConversion,
        initBuildInDrawLayers, addDrawLayers} from './PlotImageTask.js';
import {dlRoot, dispatchAttachLayerToPlot,
        dispatchCreateDrawLayer, dispatchDetachLayerFromPlot, getDlAry} from '../DrawLayerCntlr.js';
import ImageOutline from '../../drawingLayers/ImageOutline.js';
import Artifact from '../../drawingLayers/Artifact.js';
import {isHiPS, isImage} from '../WebPlot';
import HiPSGrid from '../../drawingLayers/HiPSGrid.js';
import ActiveTarget from '../../drawingLayers/ActiveTarget.js';
import {resolveHiPSIvoURL} from '../HiPSListUtil.js';
import {addNewMocLayer, makeMocTableId, isMOCFitsFromUploadAnalsysis, MOCInfo, UNIQCOL} from '../HiPSMocUtil.js';
import HiPSMOC from '../../drawingLayers/HiPSMOC.js';
import {doUpload} from '../../ui/FileUpload.jsx';

const PROXY= true;


//======================================== Exported Functions =============================
//======================================== Exported Functions =============================

function hipsFail(dispatcher, plotId, wpRequest, reason) {
    dispatcher( {
        type: ImagePlotCntlr.PLOT_HIPS_FAIL,
        payload:{
            description: 'HiPS display failed: '+ reason,
            plotId,
            wpRequest
        }});
}

function hipsChangeFail(pv, reason) {
    dispatchPlotProgressUpdate(pv.plotId, 'Failed to change HiPS display',true, pv.request.getRequestKey(), false);
}

function parseProperties(str) {
    if (!str) {
        throw new Error('Could not retrieve HiPS properties file');
    }
    const hipsProperties= str.split('\n')
        .map( (s) => s.trim())
        .filter( (s) => !s.startsWith('#') && s)
        .map( (s) => s.split('='))
        .reduce( (obj, sAry) => {
            if (sAry.length===2) obj[sAry[0].trim()]= sAry[1].trim();
            return obj;
        },{});
    validateProperties(hipsProperties);
    return hipsProperties;
}

function validateProperties(hipsProperties) {
    if (isEmpty(hipsProperties)) {
        throw new Error('Could not retrieve HiPS properties file');
    }
    const {dataproduct_type= 'image'}= hipsProperties;
    if (dataproduct_type!=='image' && dataproduct_type!=='cube') {
        if (dataproduct_type==='catalog') {
            throw new Error('HiPS catalogs are currently unsupported');
        }
        else {
            throw new Error('Currently only HiPS images and cubes are supported');
        }
    }
    return hipsProperties;

}

function initCorrectCoordinateSys(pv) {
    if (!pv) return;
    const plot= primePlot(pv);
    const vr= visRoot();
    const {plotId}= pv;
    const plotGroup= getPlotGroupById(vr, pv.plotGroupId);
    if (hasGroupLock(pv, plotGroup)) {
        const hipsPv = vr.plotViewAry.filter((pv) => isHiPS(primePlot(pv)) && pv.plotId !== plotId)[0];
        const hipsPlot = primePlot(hipsPv);
        if (hipsPlot && hipsPlot.imageCoordSys !== plot.imageCoordSys) {
            dispatchChangeHiPS({plotId, coordSys: hipsPlot.imageCoordSys});
        }
    }
}


function watchForHiPSViewDim(action, cancelSelf, params) {
    const {plotId}= action.payload;
    if (plotId!==params.plotId) return;
    const vr= visRoot();
    const pv= getPlotViewById(vr, plotId);
    const {width,height}= pv.viewDim;
    if (width && height && width>30 && height>30) {
        const plot= primePlot(pv);
        if (!plot) return;
        const wp= pv.request && pv.request.getWorldPt();

        if (!pv.request.getSizeInDeg() && !wp && lockedToOtherHiPS(vr,pv)) { //if nothing enter, match to existing HiPS
            const otherPlot= primePlot(getOtherLockedHiPS(vr,pv));
            dispatchZoom({ plotId, userZoomType: UserZoomTypes.LEVEL, level:otherPlot.zoomFactor });
            const {centerWp}= getPointMaxSide(otherPlot, otherPlot.viewDim);
            dispatchChangeCenterOfProjection({plotId,centerProjPt:centerWp});
        }
        else {
            let size= pv.request.getSizeInDeg()  || Number(plot.hipsProperties.hips_initial_fov) || 180;

            if (size) {
                if (size<.00027 || size>70) { // if size is really small (<1 arcsec) or big then do a fill, small size is probably an error
                    dispatchZoom({ plotId, userZoomType: UserZoomTypes.FILL});
                }
                else {
                    if (size<.0025) size= .0025; //if between 1 arcsec and 9 then set to 9 arcsec
                    const level= getHiPSZoomLevelToFit(pv,size);
                    dispatchZoom({ plotId, userZoomType: UserZoomTypes.LEVEL, level });
                }
            }

            if (wp) dispatchChangeCenterOfProjection({plotId,centerProjPt:wp});
        }


        initCorrectCoordinateSys(pv);
        addDrawLayers(pv.request, plot);

        cancelSelf();
    }
}



function lockedToOtherHiPS(vr, pv) {
    return Boolean(getOtherLockedHiPS(vr,pv));
}

function getOtherLockedHiPS(vr, pv) {
    const plotGroup= getPlotGroupById(vr, pv.plotGroupId);
    const ary= getOnePvOrGroup(vr.plotViewAry, pv.plotId, plotGroup);
    if (ary===1) return false;
    return ary.find( (testPv) => (testPv!==pv  && isHiPS(primePlot(testPv))) );
}


export function addAllSky(plot) {
    const allSkyURL= makeHiPSAllSkyUrlFromPlot(plot);
    const cachedAllSkyImage= findAllSkyCachedImage(allSkyURL);
    if (cachedAllSkyImage) return plot;
    dispatchPlotProgressUpdate(plot.plotId, 'Retrieving HiPS Data', false, null);
    return loadImage(makeHiPSAllSkyUrlFromPlot(plot))
        .then( (allSkyImage) => {
            addAllSkyCachedImage(allSkyURL, allSkyImage);
            return plot;
        });
}

export function addAllSkyUsingProperties(hipsProperties, hipsUrlRoot, plotId, proxyHips) {
    const exts= get(hipsProperties, 'hips_tile_format', 'jpg');
    const allSkyURL= makeHiPSAllSkyUrl(hipsUrlRoot, exts, 0);
    const cachedAllSkyImage= findAllSkyCachedImage(allSkyURL);
    if (cachedAllSkyImage) return hipsProperties;
    dispatchPlotProgressUpdate(plotId, 'Retrieving HiPS Data', false, null);
    return loadImage(makeHiPSAllSkyUrl(hipsUrlRoot, exts, 0, proxyHips))
        .then( (allSkyImage) => {
            addAllSkyCachedImage(allSkyURL, allSkyImage);
            return hipsProperties;
        });
}

export function makePlotHiPSAction(rawAction) {
    return (dispatcher) => {

        const {payload}= rawAction;
        const {plotId, attributes, pvOptions, renderTreeId}= payload;
        const wpRequest= ensureWPR(payload.wpRequest);

        const newPayload= clone(payload, {wpRequest, plotType:'hips', wpRequestAry:[wpRequest], renderTreeId});
        newPayload.viewerId= determineViewerId(payload.viewerId, plotId);
        const hipsImageConversion= getHipsImageConversion(payload.hipsImageConversion);
        if (hipsImageConversion) newPayload.pvOptions= clone(pvOptions, {hipsImageConversion});


        if (!getDrawLayerByType(getDlAry(), ActiveTarget.TYPE_ID)) {
            initBuildInDrawLayers();
        }

        resolveHiPSIvoURL(wpRequest.getHipsRootUrl())
            .then( (url) => {
                wpRequest.setHipsRootUrl(url);
                dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_START,payload:newPayload} );
                if (!url) {
                    throw new Error('Empty URL');
                }

                dispatchPlotProgressUpdate(plotId, 'Retrieving Info', false, null);
                return makeHipsUrl(`${url}/properties`, PROXY);

            })
            .then( (url) => {
                return fetchUrl(url, {}, true, PROXY);
            })
            .then( (result)=> {
                if (!result.text) throw new Error('Could not retrieve HiPS properties file');
                return result.text();
            })
            .then( (s)=> parseProperties(s))
            .then( (hipsProperties) => {
                const plot= WebPlot.makeWebPlotDataHIPS(plotId, wpRequest, hipsProperties, 'a hips plot', .0001, attributes, false);
                plot.proxyHips= PROXY;

                createHiPSMocLayer(getPropertyItem(hipsProperties, 'ivoid'), wpRequest.getHipsRootUrl(), plot);
                return plot;
            })
            .then( addAllSky)
            .then( (plot) => {
                createHiPSGridLayer();
                dispatchAddActionWatcher({
                    actions:[ImagePlotCntlr.PLOT_HIPS, ImagePlotCntlr.UPDATE_VIEW_SIZE],
                    callback:watchForHiPSViewDim,
                    params:{plotId}}
                    );
                const pvNewPlotInfoAry= [ {plotId, plotAry: [plot]} ];
                dispatcher( { type: ImagePlotCntlr.PLOT_HIPS, payload: clone(newPayload, {plot,pvNewPlotInfoAry}) });
            })
            .catch( (error) => {
                console.log(error);
                hipsFail(dispatcher, plotId, wpRequest, error.message);
            } );
    };
}

function createHiPSMocLayer(ivoid, hipsUrl, plot, mocFile = 'Moc.fits') {
    const mocUrl = hipsUrl.endsWith('/') ? hipsUrl + mocFile : hipsUrl+'/'+mocFile;
    const tblId = makeMocTableId(ivoid);
    const dls = getDrawLayersByType(getDlAry(), HiPSMOC.TYPE_ID);

    let   dl = dls.find((oneLayer) => oneLayer.drawLayerId === tblId);
    if (dl) {
        if (plot.plotId) {
            dispatchAttachLayerToPlot(dl.drawLayerId, plot.plotId, false, false);
        }
        return;
    }

    doUpload(mocUrl, {isFromURL: true, fileAnalysis: ()=>{}}).then(({status, cacheKey, analysisResult}) => {
        if (status === '200') {
            const {analysisModel={}, analysisSummary=''} = JSON.parse(analysisResult) || {};

            const isMocFits = isMOCFitsFromUploadAnalsysis(analysisSummary, analysisModel);
            if (isMocFits.valid) {
                dl = addNewMocLayer(tblId, cacheKey, mocUrl, get(isMocFits, [MOCInfo, UNIQCOL]));
                if (dl && plot.plotId) {
                    dispatchAttachLayerToPlot(dl.drawLayerId, plot.plotId, true, false);
                }
            }
        }
    });
}

function createHiPSGridLayer() {
    const dl= getDrawLayerByType(getDlAry(), HiPSGrid.TYPE_ID);
    if (!dl) {
        dispatchCreateDrawLayer(HiPSGrid.TYPE_ID);
    }
}

export function makeChangeHiPSAction(rawAction) {
    return (dispatcher, getState) => {
        const {payload}= rawAction;
        let {hipsUrlRoot}= payload;
        const {plotId}= payload;

        hipsUrlRoot= resolveHiPSConstant(hipsUrlRoot);
        const pv= getPlotViewById(getState()[IMAGE_PLOT_KEY], plotId);
        const plot= primePlot(pv);
        if (!plot) return;
        const {width,height}= pv.viewDim;
        if (!width || !height) return;


        const url= makeHipsUrl(`${hipsUrlRoot}/properties`, true);
        if (hipsUrlRoot) {
            dispatchPlotProgressUpdate(plotId, 'Retrieving Info', false, null);
            fetchUrl(url, {}, true, PROXY)
                .catch( (e) => {
                    console.log('properties not found');
                    throw new Error('Could not retrieve HiPS properties file');
                })
                .then( (result)=> result.text())
                .then( (s)=> parseProperties(s))
                .then ( (hipsProperties) => addAllSkyUsingProperties(hipsProperties, hipsUrlRoot, plotId, true))
                .then( (hipsProperties) => {
                    dispatcher(
                        { type: ImagePlotCntlr.CHANGE_HIPS,
                            payload: clone(payload, {hipsUrlRoot, hipsProperties})
                        });
                    initCorrectCoordinateSys(getPlotViewById(visRoot(),plotId));
                })
                .then( () => {
                    dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload });
                })
                .catch( (error) => {
                    console.log(error);
                    hipsChangeFail(pv, error.message);
                } );
        }
        else {
            dispatcher( { type: ImagePlotCntlr.CHANGE_HIPS, payload });
            dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload });
        }
    };
}


//==============================================================================
//------------------------------------------------------------------------------
//==============================================================================



export function makeImageOrHiPSAction(rawAction) {
    return () => {
        const {payload}= rawAction;
        const hipsRequest= ensureWPR(payload.hipsRequest);
        const imageRequest= ensureWPR(payload.imageRequest);
        const allSkyRequest= ensureWPR(payload.allSkyRequest);

        if (!validateHipsAndImage(imageRequest, hipsRequest, payload.fovDegFallOver)) return;


        const {plotId, fovDegFallOver, fovMaxFitsSize, autoConvertOnZoom, renderTreeId,
                pvOptions, attributes, plotAllSkyFirst=false}= payload;
        const viewerId= determineViewerId(payload.viewerId, plotId);
        const size= getSizeInDeg(imageRequest, hipsRequest);
        const groupId= getPlotGroupId(imageRequest, hipsRequest);
        const useImage= (plotAllSkyFirst && allSkyRequest) || (imageRequest.getWorldPt() &&
                                                              (size !== 0) && (size < fovDegFallOver));
        let wpRequest= useImage ? imageRequest.makeCopy() : hipsRequest.makeCopy();

        if (useImage) {
            wpRequest= plotAllSkyFirst ? allSkyRequest.makeCopy() : imageRequest.makeCopy();
        }
        else {
            wpRequest= hipsRequest.makeCopy();
        }

        const hipsImageConversion= {hipsRequestRoot:hipsRequest, imageRequestRoot:imageRequest, fovMaxFitsSize,
                                    autoConvertOnZoom, allSkyRequest, fovDegFallOver, plotAllSkyFirst};


        wpRequest.setSizeInDeg(size);
        wpRequest.setPlotId(plotId);
        wpRequest.setPlotGroupId(groupId);

        if (useImage) {
            dispatchPlotImage({plotId, wpRequest, viewerId, hipsImageConversion, pvOptions, attributes, renderTreeId});
        }
        else {
            dispatchPlotHiPS({plotId, wpRequest, viewerId, hipsImageConversion, pvOptions, attributes, renderTreeId});
        }
    };
}


/**
 * convert to a image defined in  hipsImageConversion
 * @param {PlotView} pv
 * @param {boolean} allSky if true, convert to the allsky image defined in hipsImageConversion
 */
export function convertToImage(pv, allSky= false) {
    const {plotId, plotGroupId,viewDim}= pv;
    const {allSkyRequest, imageRequestRoot, fovMaxFitsSize}= pv.plotViewCtx.hipsImageConversion;
    dispatchDetachLayerFromPlot(ImageOutline.TYPE_ID, plotId);
    dispatchDetachLayerFromPlot(HiPSGrid.TYPE_ID, plotId);
    const convertToAllSky= allSky && allSkyRequest;
    const wpRequest= (convertToAllSky) ? allSkyRequest.makeCopy() : imageRequestRoot.makeCopy();
    const currentFoV= getFoV(pv);
    wpRequest.setPlotId(plotId);
    wpRequest.setPlotGroupId(plotGroupId);
    const plot= primePlot(pv);
    const attributes= clone(plot.attributes, getCornersAttribute(pv) || {});
    const fromImage= isImage(plot) && !plot.projection.isWrappingProjection();
    if (convertToAllSky) {
        if (fromImage) {
            prepFromImageConversion(pv,wpRequest);
            // wpRequest.setZoomType(ZoomType.ARCSEC_PER_SCREEN_PIX);
            // wpRequest.setZoomArcsecPerScreenPix((currentFoV/viewDim.width) * 3600);
            wpRequest.setZoomType(ZoomType.TO_WIDTH);
        }
        else {
            wpRequest.setZoomType(ZoomType.TO_WIDTH);
        }
    }
    else {
        wpRequest.setWorldPt(getCenterPt(pv));
        wpRequest.setSizeInDeg(currentFoV> fovMaxFitsSize ? fovMaxFitsSize : currentFoV);
        if (currentFoV > 5) {
            wpRequest.setZoomType(ZoomType.TO_WIDTH_HEIGHT);
        }
        else {
            wpRequest.setZoomType(ZoomType.ARCSEC_PER_SCREEN_PIX);
            wpRequest.setZoomArcsecPerScreenPix((currentFoV/viewDim.width) * 3600);
        }
    }

    dispatchPlotImage({plotId, wpRequest, hipsImageConversion: pv.plotViewCtx.hipsImageConversion,
                       attributes, enableRestore:false});
}



export function convertToHiPS(pv, fromAllSky= false) {
    const {plotId, plotGroupId}= pv;
    const wpRequest= pv.plotViewCtx.hipsImageConversion.hipsRequestRoot.makeCopy();
    wpRequest.setPlotId(plotId);
    wpRequest.setPlotGroupId(plotGroupId);
    wpRequest.setSizeInDeg(getFoV(pv));
    const plot= primePlot(pv);


    const attributes= clone(plot.attributes, getCornersAttribute(pv) || {});
    wpRequest.setWorldPt(getCenterPt(pv));
    if (!fromAllSky) {
        prepFromImageConversion(pv,wpRequest);
    }

    dispatchPlotHiPS({plotId, wpRequest, attributes, hipsImageConversion: pv.plotViewCtx.hipsImageConversion,
        enableRestore:false});
}

function getCenterPt(pv) {
    const plot= primePlot(pv);
    if (isHiPS(plot)) {
        return getCenterOfProjection(plot);
    }
    else {
        return CCUtil.getWorldCoords(plot,findCurrentCenterPoint(pv));
    }
}


/**
 * This function has a lot of side effects, it modified wpRequest and dispatch drawing
 * @param pv
 * @param wpRequest
 */
function prepFromImageConversion(pv, wpRequest) {
    const {plotId}= pv;
    wpRequest.setWorldPt(getCenterPt(pv));
    // wpRequest.setSizeInDeg(pv.plotViewCtx.hipsImageConversion.fovDegFallOver);
    wpRequest.setSizeInDeg(getFoV(pv));
    const dl = getDrawLayerByType(dlRoot(), ImageOutline.TYPE_ID);
    if (!dl) dispatchCreateDrawLayer(ImageOutline.TYPE_ID);
    dispatchAttachLayerToPlot(ImageOutline.TYPE_ID, plotId);
    const artAry= getDrawLayersByType(dlRoot(), Artifact.TYPE_ID);
    artAry.forEach( (a) => dispatchDetachLayerFromPlot(a.drawLayerId,plotId));
}





/**
 * Add add a image outline to some HiPS display and attempts to zoom to the same scale.
 * @param {PlotView} pv
 * @param {Array.<string>} hipsPVidAry
 */
export function matchHiPSToImage(pv, hipsPVidAry) {
    if (!pv || isEmpty(hipsPVidAry)) return;
    const attributes=  getCornersAttribute(pv);
    const plot= primePlot(pv);
    const wpCenter= CCUtil.getWorldCoords(plot,findCurrentCenterPoint(pv));
    const dl = getDrawLayerByType(dlRoot(), ImageOutline.TYPE_ID);
    if (!dl) dispatchCreateDrawLayer(ImageOutline.TYPE_ID);
    const asPerPix= getArcSecPerPix(plot,plot.zoomFactor);
    hipsPVidAry.forEach( (id) => {
        Object.entries(attributes).forEach( (entry) => dispatchAttributeChange(id, false, entry[0], entry[1]));
        dispatchAttachLayerToPlot(ImageOutline.TYPE_ID, id);
        dispatchChangeCenterOfProjection({plotId:id, centerProjPt:wpCenter});
        dispatchChangeHiPS( {plotId:id,  coordSys:plot.imageCoordSys});
        const hipsPv= getPlotViewById(visRoot(), id);
        const hipsPlot= primePlot(hipsPv);
        // const {width,height}= pv.viewDim;
        // const cc= CysConverter.make(hipsPlot);
        // const sp0=  cc.getScreenCoords(attributes[PlotAttribute.OUTLINEIMAGE_BOUNDS][0]);
        // const sp2=  cc.getScreenCoords(attributes[PlotAttribute.OUTLINEIMAGE_BOUNDS][2]);;
        // const level= Math.min(width/Math.abs(sp0.x-sp2.x),
        //                       height/Math.abs(sp0.y-sp2.y)) * hipsPlot.zoomFactor;
        // dispatchZoom({ plotId:id, userZoomType: UserZoomTypes.LEVEL, level});

        //---
        const level= getZoomLevelForScale(hipsPlot,asPerPix);
        dispatchZoom({ plotId:id, userZoomType: UserZoomTypes.LEVEL, level});
    }) ;
}

function getCornersAttribute(pv) {
    const plot= primePlot(pv);
    const cAry= getCorners(plot);
    if (!cAry) return null;
    return {
        [PlotAttribute.OUTLINEIMAGE_BOUNDS]: cAry,
        [PlotAttribute.OUTLINEIMAGE_TITLE]: plot.title
    };
}


/**
 * This function will convert between HiPS and FITS or FITS and Hips depend on hipsImageConversion settings and zoom
 * direction.
 * @param {PlotView} pv
 * @param {number} [prevZoomLevel] - previous zoom level
 * @param {number} [nextZoomLevel] - next zoom level
 * @return {boolean}
 */
export function doHiPSImageConversionIfNecessary(pv, prevZoomLevel, nextZoomLevel) {
    if (!pv.plotViewCtx.hipsImageConversion) return false;
    const plot= primePlot(pv);
    const {fovDegFallOver, allSkyRequest}=  pv.plotViewCtx.hipsImageConversion;
    const {width,height}= pv.viewDim;
    const fov= getFoV(pv);
    if (!nextZoomLevel || !prevZoomLevel) {
        nextZoomLevel = plot.zoomFactor;
        prevZoomLevel = plot.zoomFactor;
    }

    if (isHiPS(plot) ) {
        const {screenSize}= plot;
        if (fovDegFallOver && prevZoomLevel<=nextZoomLevel && fov < fovDegFallOver) { // zooming in hips FOV passes fovDegFallOver
            convertToImage(pv, false);
            return true;
        }
        else if (fov>179 &&  prevZoomLevel>=nextZoomLevel &&   // zooming out, hips image getting small
            screenSize.width<width-10 && screenSize.height<height-10 && screenSize.width>50 &&
            allSkyRequest){
            convertToImage(pv, true);
            return true;
        }
    }
    else if (isImage(plot)) {
        if (plot.projection.isWrappingProjection() && prevZoomLevel<=nextZoomLevel && fov < 200) {// zooming in, all sky FOV less than 180
            convertToHiPS(pv);
            return true;
        }
        else if (prevZoomLevel>=nextZoomLevel &&
            (width-10)>plot.dataWidth*nextZoomLevel && (height-10) >plot.dataHeight*nextZoomLevel ) { //zoom out image getting small
            convertToHiPS(pv);
            return true;
        }
    }
    return false;
}





function validateHipsAndImage(imageRequest, hipsRequest, fovDegFallOver) {
    if (!fovDegFallOver) {
        console.log('You must define fovDegFallOver to the degree field of view to switch between HiPS and Image');
        return false;
    }
    if (!hipsRequest || !imageRequest) {
        console.log('You must define both hipsRequest and imageRequest');
        return false;
    }
    /*
    if (!getSizeInDeg(imageRequest, hipsRequest)) {
        console.log('You must call setSizeInDeg in either the hipsRequest or the imageRequest');
        return false;
    }
    */
    if (!getPlotGroupId(imageRequest, hipsRequest)) {
        console.log('You must call setPlotGroupId in either the hipsRequest or the imageRequest');
        return false;
    }
    return true;
}


function getSizeInDeg(imageRequest, hipsRequest) {
    if (imageRequest && imageRequest.getSizeInDeg()) return imageRequest.getSizeInDeg();
    if (hipsRequest && hipsRequest.getSizeInDeg()) return hipsRequest.getSizeInDeg();
    return 0;
}

function getPlotGroupId(imageRequest, hipsRequest) {
    if (imageRequest && imageRequest.getPlotGroupId()) return imageRequest.getPlotGroupId();
    if (hipsRequest && hipsRequest.getPlotGroupId()) return hipsRequest.getPlotGroupId();
}
