/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import {isNumber} from 'lodash';
import HpxCatalog from '../../drawingLayers/hpx/HpxCatalog';
import {
    clearHpxFilterCatalog, filterHpxCatalog, selectHpxCatalog, unselectHxpCatalog
} from '../../drawingLayers/hpx/HpxCatalogUtil';
import {
    getAllDrawLayersForPlot,
    getDrawLayersByType,
    getPlotViewById,
    isDrawLayerAttached,
    isMultiImageFitsWithSameArea,
    primePlot
} from '../PlotViewUtil';
import {CysConverter} from '../CsysConverter';
import {PlotAttribute} from '../PlotAttribute';
import {makeDevicePt, makeImagePt, makeScreenPt, parseImagePt} from '../Point';
import Catalog, {
    clearFilterCatalog,
    filterCatalog,
    getSelectedShape,
    selectCatalog,
    unselectCatalog
} from '../../drawingLayers/Catalog';
import {
    dispatchChangeCenterOfProjection,
    dispatchCrop,
    dispatchProcessScroll,
    dispatchZoom,
    visRoot
} from '../ImagePlotCntlr';
import LSSTFootprint, {
    clearFilterFootprint,
    filterFootprint,
    selectFootprint,
    unselectFootprint
} from '../../drawingLayers/ImageLineBasedFootprint';
import {callGetAreaStatistics} from '../../rpc/PlotServicesJson';
import {showImageAreaStatsPopup} from './ImageStatsPopup';
import {logger} from '../../util/Logger.js';
import CoordUtil from '../CoordUtil';
import {isImageOverlayLayersActive} from '../RelatedDataUtil';
import {showInfoPopup} from '../../ui/PopupUtil';
import {detachSelectArea, isOutlineImageForSelectArea, SELECT_AREA_TITLE} from './SelectAreaDropDownView';
import ImageOutline from '../../drawingLayers/ImageOutline';
import {convertAngle} from '../VisUtil';
import ShapeDataObj from '../draw/ShapeDataObj';
import {dispatchAttachLayerToPlot, dispatchCreateDrawLayer, dispatchDestroyDrawLayer, dlRoot} from '../DrawLayerCntlr';
import {UserZoomTypes} from '../ZoomUtil';
import {isHiPS, isImage} from '../WebPlot';
import {findScrollPtToCenterImagePt} from '../reducer/PlotView';
import {SelectedShape} from '../../drawingLayers/SelectedShape';



//todo move the statistics constants to where they are needed
const Metrics= {MAX:'MAX', MIN:'MIN', CENTROID:'CENTROID', FW_CENTROID:'FW_CENTROID', MEAN:'MEAN',
    STDEV:'STDEV', INTEGRATED_FLUX:'INTEGRATED_FLUX', NUM_PIXELS:'NUM_PIXELS', PIXEL_AREA:'PIXEL_AREA'};



/**
 * @param pv
 */
export function stats(pv) {
    const p= primePlot(pv);
    const cc= CysConverter.make(p);
    const sel= p.attributes[PlotAttribute.SELECTION];

    const ip0=  cc.getDeviceCoords(sel.pt0);
    const ip2=  cc.getDeviceCoords(sel.pt1);
    const ip1=  makeDevicePt(ip2.x,ip0.y);
    const ip3=  makeDevicePt(ip0.x,ip2.y);
    const shape = getSelectedShape(pv);

    callGetAreaStatistics(p.plotState,
        cc.getImageCoords(ip0),
        cc.getImageCoords(ip1),
        cc.getImageCoords(ip2),
        cc.getImageCoords(ip3),
        shape, (shape === SelectedShape.rect.key ? 0.0 : pv.rotation))
        .then( (wpResult) => {      // result of area stats

            // tabularize stats data
            const tblData = tabulateStatics(wpResult, cc);

            //console.log(wpResult);
            showImageAreaStatsPopup(p.title, tblData, pv.plotId);
        })
        .catch ( (e) => {
            logger.error(`error, stat , plotId: ${p.plotId}`, e);
        });
}


/**
 * representing the number into decimal or exponential format
 * @param {number} [num]
 * @param {number} [fractionDigits=7] fractional digits after the decimal point
 * @return {string}
 */
function formatNumber(num, fractionDigits=7) {
    if (!isNumber(num)) return '';
    const SigDig = 7;
    const thred = Math.min(Math.pow(10, SigDig - fractionDigits), 1.0);
    return (Math.abs(num) >= thred) ? num.toFixed(fractionDigits) : num.toExponential(fractionDigits);
}

/**
 * tabularize the area statistics into summary and stats two sections for popup display
 * @param {object} wpResult
 * @param {object} cc
 * @returns { object } {statsSummary: array (for summary items), statsTable: array (for table rows)}}
 */
function tabulateStatics(wpResult, cc) {

    const SSummary = 'statsSummary';
    const STable   = 'statsTable';
    const ipMetrics = [Metrics.MIN, Metrics.MAX, Metrics.CENTROID, Metrics.FW_CENTROID];
    const bands  = wpResult.Band_Info;
    const noBand = 'NO_BAND';

    const bandData = {};

    function getOneStatSet(b) {

        const tblData = {};

        tblData[SSummary] = [
            [b.MEAN.desc, formatNumber(b.MEAN.value) + ' ' + b.MEAN.units],
            [b.STDEV.desc, formatNumber(b.STDEV.value) + ' ' + b.STDEV.units],
            [b.INTEGRATED_FLUX.desc,
                formatNumber(b.INTEGRATED_FLUX.value) + ' ' + b.INTEGRATED_FLUX.units]
        ];

        tblData[STable] = [{'cells': ['', 'Position', 'Value']}];

        for (let i = 0; i < ipMetrics.length; i++) {
            if (!b.hasOwnProperty(ipMetrics[i])) {
                continue;
            }

            const item = b[ipMetrics[i]];
            const statsRow = [];

            // item title
            statsRow.push(item.desc);

            // item location
            const ipt = parseImagePt(item.ip);
            const wpt = cc.getWorldCoords(ipt);
            const hmsRA = CoordUtil.convertLonToString(wpt.getLon(), wpt.getCoordSys());
            const hmsDec = CoordUtil.convertLatToString(wpt.getLat(), wpt.getCoordSys());
            statsRow.push(`RA: ${hmsRA}\nDEC: ${hmsDec}`);

            // item value
            if (item.value === null || item.value === undefined) {
                statsRow.push('');
            } else {
                statsRow.push(`${formatNumber(item.value)} ${item.units}`);
            }

            tblData[STable].push({'cells': statsRow, 'worldPt': wpt});
        }
        return tblData;
    }

    if (bands.hasOwnProperty(noBand)) {
        bandData[noBand] = getOneStatSet(bands[noBand]);
    } else {
        const bandName = ['Blue', 'Red', 'Green'];

        bandName.forEach((value) => {
            if (bands[value]) {
                bandData[value] = getOneStatSet(bands[value]);
            }
        });
    }

    return bandData;
}


export function crop(pv) {
    if (isImageOverlayLayersActive(pv)) {
        showInfoPopup('Crop not yet supported with mask layers');
        return;
    }

    const p= primePlot(pv);
    const cc= CysConverter.make(p);
    const sel= p.attributes[PlotAttribute.IMAGE_BOUNDS_SELECTION];
    const ip0=  cc.getImageCoords(sel.pt0);
    const ip1=  cc.getImageCoords(sel.pt1);
    const cropMultiAll= isMultiImageFitsWithSameArea(pv);
    dispatchCrop({plotId:pv.plotId, imagePt1:ip0, imagePt2:ip1, cropMultiAll});
    attachImageOutline(pv, 'Crop Outline');
    detachSelectArea(pv, true);
}



// attach image outline drawing layer on top of the cropped image, zoom-to-fit image and recenter image
function attachImageOutline(pv, title) {
    const selectedShape = getSelectedShape(pv);
    //if (selectedShape === SelectedShape.rect.key) return;

    getDrawLayersByType(dlRoot(), ImageOutline.TYPE_ID)
        ?.filter( (dl) => dl.title===title )
        .forEach( (dl) => dispatchDestroyDrawLayer(dl.drawLayerId));

    const plot = primePlot(pv);
    const cc= CysConverter.make(plot);
    const sel = plot.attributes[PlotAttribute.SELECTION];
    const devPt0= cc.getDeviceCoords(sel.pt0);
    const devPt2= cc.getDeviceCoords(sel.pt1);
    const devPt1= makeDevicePt(devPt2.x, devPt0.y);

    // create ellipse dimension on image domain
    const imgPt = [devPt0, devPt1, devPt2].map((devP) => cc.getImageCoords(devP));
    const dist = (dx, dy) => {
        return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
    };

    const r1 = dist((imgPt[0].x - imgPt[1].x), (imgPt[0].y - imgPt[1].y));
    const r2 = dist((imgPt[1].x - imgPt[2].x), (imgPt[1].y - imgPt[2].y));
    const center = cc.getWorldCoords(makeImagePt((imgPt[0].x + imgPt[2].x)/2, (imgPt[0].y + imgPt[2].y)/2));
    const rotArc = pv.rotation === 0.0 ? 0.0 : convertAngle('deg', 'arcsec', (360 - pv.rotation));


    const drawObj = selectedShape === SelectedShape.rect.key ?
        ShapeDataObj.makeRectangleByCenter(center, r1, r2, ShapeDataObj.UnitType.IMAGE_PIXEL,
            rotArc,  ShapeDataObj.UnitType.ARCSEC, false, true) :
        ShapeDataObj.makeEllipse(center, r1/2, r2/2, ShapeDataObj.UnitType.IMAGE_PIXEL,
            rotArc, ShapeDataObj.UnitType.ARCSEC, false);
    const dl = dispatchCreateDrawLayer(ImageOutline.TYPE_ID,
        {drawObj, color: 'red', title, destroyWhenAllDetached: true});

    if (!isDrawLayerAttached(dl, pv.plotId)) {
        dispatchAttachLayerToPlot(dl.drawLayerId, pv.plotId, false);
    }
}

export function zoomIntoSelection(pv, try2=false) {

    let p= primePlot(pv);
    if (!p) return;
    const {viewDim,plotId}= pv;
    let cc= CysConverter.make(p);
    const sel= p.attributes[PlotAttribute.IMAGE_BOUNDS_SELECTION];
    if (!sel) return;

    const sp0=  cc.getScreenCoords(sel.pt0);
    const sp2=  cc.getScreenCoords(sel.pt1);
    const userSearchWP= p.attributes[PlotAttribute.USER_SEARCH_WP];

    if (!sp0 || !sp2) {
        if (isImage(p) || try2) return;
        dispatchChangeCenterOfProjection({plotId,centerProjPt:sel.pt0});
        zoomIntoSelection(getPlotViewById(pv.plotId), true);
        return;
    }

    const level= Math.min(viewDim.width/Math.abs(sp0.x-sp2.x),
        viewDim.height/Math.abs(sp0.y-sp2.y)) * p.zoomFactor;
    dispatchZoom({ plotId, userZoomType: UserZoomTypes.LEVEL, level});


    if (isImage(p)) {
        pv= getPlotViewById(visRoot(),plotId);
        p= primePlot(pv);
        cc= CysConverter.make(p);
        const ip0=  cc.getImageCoords(sel.pt0);
        const ip2=  cc.getImageCoords(sel.pt1);
        const centerPt= makeImagePt( Math.abs(ip0.x+ip2.x)/2, Math.abs(ip0.y+ip2.y)/2);
        const proposedSP= findScrollPtToCenterImagePt(pv, centerPt);
        dispatchProcessScroll({plotId,scrollPt:proposedSP});
    }
    else if (isHiPS(p)) {
        if (userSearchWP) {
            dispatchChangeCenterOfProjection({plotId,centerProjPt:userSearchWP});
        }
        else {
            p= primePlot(visRoot(),pv.plotId);
            dispatchChangeCenterOfProjection({plotId,centerProjPt:sel.pt2});
            cc= CysConverter.make(p);
            const dev0=  cc.getDeviceCoords(sel.pt0);
            const dev2=  cc.getDeviceCoords(sel.pt1);
            const centerPt= makeDevicePt( Math.abs(dev0.x-dev2.x)/2+ Math.min(dev0.x,dev2.x),
                Math.abs(dev0.y-dev2.y)/2 + Math.min(dev0.y,dev2.y));
            const centerProjPt= cc.getWorldCoords(centerPt, p.imageCoordSys);
            if (centerProjPt) dispatchChangeCenterOfProjection({plotId,centerProjPt});
        }
    }
    else {
        return;
    }

    if (!userSearchWP) attachImageOutline(pv, 'Last Zoom to Selection'); // if userSearchWP, we are in click-to-search mode
    detachSelectArea(pv, true);

}

export function recenterToSelection(pv) {
    const p= primePlot(pv);
    if (!p) return;
    const {viewDim,plotId}= pv;
    const cc= CysConverter.make(p);
    const sel= p.attributes[PlotAttribute.IMAGE_BOUNDS_SELECTION];
    if (!sel) return;

    const sp0=  cc.getScreenCoords(sel.pt0);
    const sp2=  cc.getScreenCoords(sel.pt1);
    const centerPt= makeScreenPt( Math.abs(sp0.x-sp2.x)/2+ Math.min(sp0.x,sp2.x),
        Math.abs(sp0.y-sp2.y)/2 + Math.min(sp0.y,sp2.y));
    const userSearchWP= p.attributes[PlotAttribute.USER_SEARCH_WP];

    if (isImage(p)) {
        const newScrollPt= makeScreenPt(centerPt.x - viewDim.width/2, centerPt.y - viewDim.height/2);
        dispatchProcessScroll({plotId,scrollPt:newScrollPt});
    }
    else { // hips
        const centerProjPt = userSearchWP ? userSearchWP : cc.getWorldCoords(centerPt, p.imageCoordSys);
        if (centerProjPt) dispatchChangeCenterOfProjection({plotId,centerProjPt});
    }

}

export function selectDrawingLayer(pv) {
    const allLayers = getAllDrawLayersForPlot(dlRoot(), pv.plotId, true);
    if (allLayers.length > 0) {
        if (allLayers.some((l) => l.drawLayerTypeId === Catalog.TYPE_ID)) {
            selectCatalog(pv);
        }
        selectHpxCatalog(pv);
        if (allLayers.some((l) => l.drawLayerTypeId === LSSTFootprint.TYPE_ID)) {
            selectFootprint(pv);
        }
    }
}

export function unselectDrawingLayer(pv) {
    const allLayers = getAllDrawLayersForPlot(dlRoot(), pv.plotId, true);
    if (allLayers.length > 0) {
        if (allLayers.some((l) => l.drawLayerTypeId === Catalog.TYPE_ID)) {
            unselectCatalog(pv, allLayers);
        }
        unselectHxpCatalog(pv, allLayers);
        if (allLayers.some((l) => l.drawLayerTypeId === LSSTFootprint.TYPE_ID)) {
            unselectFootprint(pv, allLayers);
        }
    }
}

export function filterDrawingLayer(pv) {
    const allLayers = getAllDrawLayersForPlot(dlRoot(), pv.plotId, true);
    if (allLayers.length > 0) {
        if (allLayers.some((l) => l.drawLayerTypeId === Catalog.TYPE_ID)) {
            filterCatalog(pv, allLayers);
        }
        filterHpxCatalog(pv, allLayers);
        if (allLayers.some((l) => l.drawLayerTypeId === LSSTFootprint.TYPE_ID)) {
            filterFootprint(pv, allLayers);
        }
    }
}

export function clearFilterDrawingLayer(pv) {
    const allLayers = getAllDrawLayersForPlot(dlRoot(), pv.plotId, true);
    if (allLayers.length > 0) {
        if (allLayers.some((l) => l.drawLayerTypeId === Catalog.TYPE_ID)) {
            clearFilterCatalog(pv, allLayers);
        }
        clearHpxFilterCatalog(pv, allLayers);
        if (allLayers.some((l) => l.drawLayerTypeId === LSSTFootprint.TYPE_ID)) {
            clearFilterFootprint(pv, allLayers);
        }
    }
}
