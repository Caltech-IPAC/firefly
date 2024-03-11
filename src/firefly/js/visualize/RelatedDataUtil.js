/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isEmpty, difference,get, flatten, values, uniq} from 'lodash';
import { primePlot, getPlotViewIdListInOverlayGroup, getPlotViewById, operateOnOthersInOverlayColorGroup} from './PlotViewUtil.js';
import {WPConst} from './WebPlotRequest.js';
import {RDConst} from './WebPlot.js';
import {
    visRoot, dispatchPlotMask, dispatchPlotMaskLazyLoad, dispatchChangeImageVisibility
} from './ImagePlotCntlr.js';
import {dispatchCreateDrawLayer, dispatchAttachLayerToPlot} from './DrawLayerCntlr.js';
import Artifact from '../drawingLayers/Artifact.js';








//--------------------------------------------------------------
//--------------------------------------------------------------
//--------- related data and OverlayPlotView functions
//--------------------------------------------------------------
//--------------------------------------------------------------

/**
 * Find any related data that is associated with the PlotView that is supported has not be activated
 * (activated means turned into an overlay).
 * @param {PlotView} pv
 * @return {RelatedData[]} all the unactivated related data the the UI supports
 */
export function findUnactivatedRelatedData(pv) {
    const plot= primePlot(pv);
    if (!plot) return [];
    const relatedData= flatten(pv.plots
        .map( (p) =>  p.relatedData))
        .filter( (r,idx) => unactivatedDataTypeMatches(r,idx,pv));
    return relatedData;
}


/**
 *
 * @param {PlotView} pv
 * @param {String} relatedDataId
 * @return {Array.<RelatedData>}
 */
export function getRelatedDataById(pv, relatedDataId) {
    if (!pv) return undefined;
    return flatten(pv.plots.map( (p) =>  p.relatedData))
        .find( (r) => get(r,'relatedDataId') === relatedDataId);
}



/**
 * For the related data that is passed check to see if is supported and has not been completely activated.
 * Mask type related data might be partially activated.
 * @param {RelatedData} r
 * @param idx
 * @param {PlotView} pv
 * @return {boolean}
 */
function unactivatedDataTypeMatches(r,idx, pv) {
    const dataType= get(r,'dataType');
    if (!RDConst.SUPPORTED_DATATYPES.includes(dataType)) return false;

    if (dataType===RDConst.IMAGE_MASK) {
        const opvAry= pv.overlayPlotViews.filter((opv) => opv.relatedDataId===r.relatedDataId);
        if (!opvAry.length) return true;
        const maskNumberAry= values(r.availableMask);

        const maskDataWidth= opvAry.find( (opv) => opv?.plot?.dataWidth)?.plot?.dataWidth;
        const maskDataHeight= opvAry.find( (opv) => opv?.plot?.dataHeight)?.plot?.dataHeight;

        if (maskDataHeight && maskDataHeight) {
            if (maskNumberAry.length!==opvAry.length ||
                maskDataWidth !== primePlot(pv)?.dataWidth ||
                maskDataHeight !== primePlot(pv)?.dataHeight) {
                return true;
            }
        }
        else if (maskNumberAry.length!==opvAry.length) {
            return true;
        }
    }
    else if (dataType===RDConst.TABLE) {
        return false;
    }
    else {
        return true;
    }
    return false;
}

/**
 * apply a function to all OverlayPlotView that match the passed, match is determined by same group and title
 * @param {VisRoot}  vr
 * @param {OverlayPlotView} opv
 * @param {Function} func  an OverlayPlotView is passed as parameter
 */
export function operateOnOverlayPlotViewsThatMatch(vr, opv, func) {
    const opvList= flatten(getPlotViewIdListInOverlayGroup(vr, opv.plotId)
        .map( (id) => getPlotViewById(vr,id).overlayPlotViews))
        .filter( (aOpv) =>
            aOpv?.title===opv.title &&
            aOpv?.plot?.dataWidth===opv?.plot?.dataWidth &&
            aOpv?.plot?.dataHeight===opv?.plot?.dataHeight);

    opvList.forEach( (aOpv) => func(aOpv));
}

/**
 * Set a mask layer visible, this means either change the visibility attributes or calling a dispatch to fetch the data.
 * @param {OverlayPlotView} opv
 * @param {boolean} visible
 */
export function setMaskVisible(opv, visible) {
    if (!visible || opv.plot) {
        dispatchChangeImageVisibility({plotId:opv.plotId, imageOverlayId:opv.imageOverlayId,visible});
    }
    else if (visible && opv.lazyLoadPayload) {
        dispatchPlotMaskLazyLoad(opv.lazyLoadPayload);
    }
}



/**
 * Do processing to turn this related data into a drawing layer
 * @param {VisRoot}  vr
 * @param {PlotView|undefined} pv
 * @param {RelatedData} relatedData
 */
export function enableRelatedDataLayer(vr, pv, relatedData) {
    if (!relatedData) return;
    switch (relatedData.dataType) {
        case RDConst.IMAGE_MASK:
            enableRelatedDataLayerMaskInGroup(vr, pv,relatedData);
            break;
        case RDConst.IMAGE_OVERLAY:
            break;
        case RDConst.TABLE:
            enableRelatedDataLayerTableOverlay(pv,relatedData);
            break;
    }
}



function enableRelatedDataLayerMaskInGroup(vr, pv,relatedData) {
    enableRelatedDataLayerMask(pv,relatedData);

    operateOnOthersInOverlayColorGroup(vr, pv, (aPv) => {
        const rd= findUnactivatedRelatedData(aPv).filter( (aRd) => aRd.dataType===RDConst.IMAGE_MASK);
        if (rd[0]) enableRelatedDataLayerMask(aPv,rd[0]);
    } );
}


function enableRelatedDataLayerMask(pv, relatedData) {
    const imageNumber= relatedData.searchParams[WPConst.MULTI_IMAGE_IDX];
    const fileKey= relatedData.searchParams[WPConst.FILE];


    const availMaskValues= values(relatedData.availableMask).map( (v) => Number(v));

    availMaskValues
        .sort( (v1,v2) => v1-v2)
        .forEach(  (v) =>
            {
                addMaskLayer(pv, v, imageNumber, fileKey, relatedData);
            }
        );


}

const maskIdRoot= 'AUTO_LOADED_MASK';
let maskCnt= 0;

function addMaskLayer(pv, maskNumber, imageNumber, fileKey, relatedData) {

    const {relatedDataId}= relatedData;

    const title= makeMaskTitle(maskNumber, relatedData.availableMask);

    dispatchPlotMask({plotId:pv.plotId,
        imageOverlayId:`${pv.plotId}-${maskIdRoot}_#${maskNumber}_${maskCnt}`,
        fileKey, maskNumber, maskValue:Math.pow(2,Number(maskNumber)),
        uiCanAugmentTitle:false, imageNumber, title,
        relatedDataId, lazyLoad:true});
    maskCnt++;
}

function makeMaskTitle(maskNumber, availableMask) {
    const titleRoot= 'bit # '+maskNumber;
    let maskDesc= Object.keys(availableMask)
        .filter( (k) => k.includes('MP'))
        .find( (k) => parseInt(availableMask[k])===maskNumber);
    if (maskDesc) {
        if (maskDesc.startsWith('HIERARCH')) maskDesc= maskDesc.substring(9);
        if (maskDesc.startsWith('MP_')) maskDesc= maskDesc.substring(3);
        return `${titleRoot} - ${maskDesc}`;
    }
    else {
        return titleRoot;
    }
}

function enableRelatedDataLayerImageOverlay(pv, relatedData) { // eslint-disable-line no-unused-vars
    //todo
    console.log('todo: ImageOverlay');
}

function enableRelatedDataLayerTableOverlay(pv, relatedData) {
    dispatchCreateDrawLayer(Artifact.TYPE_ID,{relatedDataId:relatedData.relatedDataId,
                                              title:relatedData.desc,
                                              dataKey: relatedData.dataKey,
                                              plotId:pv.plotId,
                                              layersPanelLayoutId: pv.plotId+'-artifacts' } );

    dispatchAttachLayerToPlot(relatedData.relatedDataId, pv.plotId, false, 'inherit');
}


/**
 *
 * search matchOverlayPlotViews array and find any related data in the passed PlotView. Enable the layers
 * that match.
 * This function has side effect of dispatching actions
 * @param {PlotView|undefined} pv the plot view the contains the related data
 * @param {OverlayPlotView[]} matchOverlayPlotViews array of OverlayPlotView that must be matched
 */
export function enableMatchingRelatedData(pv, matchOverlayPlotViews) {
    const overTypes= uniq(matchOverlayPlotViews.map( (opv) => opv.opvType));
    const relatedData= findUnactivatedRelatedData(pv).filter( (rd) => overTypes.includes(rd.dataType));
    if (!relatedData.length) return;
    relatedData.forEach( (r) => enableRelatedDataLayer(visRoot(), pv, r));

    pv= getPlotViewById(visRoot(), pv.plotId);

    //NOTE - only handles mask so far, other types of image overlay still need to be implemented
    matchOverlayPlotViews
        .filter( (moPv) => moPv.visible)
        .forEach( (moPv) => {
            const matchOpv= pv.overlayPlotViews.find( (opv) => moPv.opvType===RDConst.IMAGE_MASK &&
            opv.opvType===RDConst.IMAGE_MASK &&
            moPv.maskNumber === opv.maskNumber );
            if (matchOpv) setMaskVisible(matchOpv,true);
        });
}

/**
 * Are the plot view overlays active
 * @param {VisRoot|PlotView} ref
 */
export function isImageOverlayLayersActive(ref) {
    if (!ref) return false;
    let pv;
    if (ref.plotViewAry) { // I was passed the visRoot, use either plot it or the active plot id
        pv= getPlotViewById(ref, ref.activePlotId);
    }
    else if (ref.plots) {
        pv= ref;
    }
    else {
        return false;
    }
    return !isEmpty(pv.overlayPlotViews);
}
