/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {difference,get, flatten, values, uniq, isEmpty} from 'lodash';
import {getPlotViewIdListInGroup, getPlotViewById, operateOnOthersInGroup} from './PlotViewUtil.js';
import {WPConst} from './WebPlotRequest.js';
import {RDConst} from './WebPlot.js';
import {visRoot, dispatchPlotMask, dispatchOverlayPlotChangeAttributes, dispatchPlotMaskLazyLoad} from './ImagePlotCntlr.js';




//--------------------------------------------------------------
//--------------------------------------------------------------
//--------- related data and OverlayPlotView functions
//--------------------------------------------------------------
//--------------------------------------------------------------

export function findRelatedData(pv) {
    if (!get(pv,'plots')) return [];

    const relatedData= flatten(pv.plots.map( (p) =>  p.relatedData))
        .filter( (r,idx) => dataTypeMatches(r,idx,pv));
    return relatedData;
}

function dataTypeMatches(r,idx, pv) {
    const dataType= get(r,'dataType');
    if (!RDConst.SUPPORTED_DATATYPES.includes(dataType)) return false;

    if (dataType===RDConst.IMAGE_MASK) {
        const opvAry= pv.overlayPlotViews.filter((opv) => opv.relatedDataId===r.relatedDataId);
        if (!opvAry.length) return true;
        const maskNumberAry= values(r.availableMask);
        if (maskNumberAry.length!==opvAry.length) return true;
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
    const opvList= flatten(getPlotViewIdListInGroup(vr, opv.plotId)
        .map( (id) => getPlotViewById(vr,id).overlayPlotViews))
        .filter( (aOpv) => get(aOpv,'title')===opv.title);

    opvList.forEach( (aOpv) => func(aOpv));
}

export function setMaskVisible(opv, visible) {
    if (!visible || opv.plot) {
        dispatchOverlayPlotChangeAttributes({plotId:opv.plotId, imageOverlayId:opv.imageOverlayId, attributes:{visible}});
    }
    else if (visible && opv.lazyLoadPayload) {
        dispatchPlotMaskLazyLoad(opv.lazyLoadPayload);
    }
}




//===========  Note the following set of related data functions might should go in another file.
//===========  maybe even an action creator, not sure so for now I am going to keep it like this.

/**
 * Do processing to turn this related data into a drawing layer
 * @param {VisRoot}  vr
 * @param {PlotView} pv
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
            break;
    }
}



function enableRelatedDataLayerMaskInGroup(vr, pv,relatedData) {
    enableRelatedDataLayerMask(pv,relatedData);

    operateOnOthersInGroup(vr, pv, (aPv) => {
        const rd= findRelatedData(aPv).filter( (aRd) => aRd.dataType===RDConst.IMAGE_MASK);
        if (rd[0]) enableRelatedDataLayerMask(aPv,rd[0]);
    } );
}


function enableRelatedDataLayerMask(pv, relatedData) {
    const hdu= relatedData.searchParams[WPConst.MULTI_IMAGE_IDX];
    const fileKey= relatedData.searchParams[WPConst.FILE];


    const availMaskValues= values(relatedData.availableMask).map( (v) => Number(v));
    const activeMaskValues= pv.overlayPlotViews.map( (opv) => Number(opv.maskNumber));

    difference(availMaskValues,activeMaskValues)
        .sort( (v1,v2) => v1-v2)
        .forEach(  (v) =>
            {
                addMaskLayer(pv, v, hdu, fileKey, relatedData);
            }
        );


}

const maskIdRoot= 'AUTO_LOADED_MASK';
var maskCnt= 0;

function addMaskLayer(pv, maskNumber, hdu, fileKey, relatedData) {

    const {relatedDataId}= relatedData;

    const title= makeMaskTitle(maskNumber, relatedData.availableMask);

    dispatchPlotMask({plotId:pv.plotId,
        imageOverlayId:`${maskIdRoot}_#${maskNumber}_${maskCnt}`,
        fileKey, maskNumber, maskValue:Math.pow(2,Number(maskNumber)),
        uiCanAugmentTitle:false, imageNumber:hdu, title,
        relatedDataId, lazyLoad:true});
    maskCnt++;
}

function makeMaskTitle(maskNumber, availableMask) {
    const titleRoot= 'bit # '+maskNumber;
    var maskDesc= Object.keys(availableMask)
        .filter( (k) => k.includes('MP'))
        .find( (k) => parseInt(availableMask[k])===maskNumber);
    if (maskDesc) {
        if (maskDesc.startsWith('HIERARCH')) maskDesc= maskDesc.substring(9);
        return `${titleRoot} - ${maskDesc}`;
    }
    else {
        return titleRoot;
    }
}

function enableRelatedDataLayerImageOverlay(pv, relatedData) {
    //todo
    console.log('todo: ImageOverlay');
}

function enableRelatedDataLayerTableOverlay(pv, relatedData) {
    //todo
    console.log('todo: artifact overlay');
}


/**
 * search matchOverlayPlotViews array and find any related data in the passed PlotView. Enable the layers
 * that match.
 * This function has side effect of dispatching actions
 * @param {PlotView} pv the plot view the contains the related data
 * @param {OverlayPlotView[]} matchOverlayPlotViews array of OverlayPlotView that must be matched
 */
export function enableMatchingRelatedData(pv, matchOverlayPlotViews) {
    const overTypes= uniq(matchOverlayPlotViews.map( (opv) => opv.opvType));
    const relatedData= findRelatedData(pv).filter( (rd) => overTypes.includes(rd.dataType));
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

