/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import numeral from 'numeral';
import PropTypes from 'prop-types';
import {isEmpty, get, padEnd} from 'lodash';
import {primePlot,isMultiImageFitsWithSameArea, getPlotViewById,
                getDrawLayersByType, isDrawLayerAttached, getAllDrawLayersForPlot } from '../PlotViewUtil.js';
import {findScrollPtToCenterImagePt} from '../reducer/PlotView.js';
import {CysConverter} from '../CsysConverter.js';
import {PlotAttribute,isHiPS, isImage} from '../WebPlot.js';
import {makeDevicePt, makeScreenPt, makeImagePt} from '../Point.js';
import {callGetAreaStatistics} from '../../rpc/PlotServicesJson.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {logError} from '../../util/WebUtil.js';
import {showImageAreaStatsPopup} from './ImageStatsPopup.jsx';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView.jsx';

import {dispatchCreateDrawLayer, dispatchAttachLayerToPlot} from '../DrawLayerCntlr.js';
import {dispatchCrop, dispatchChangeCenterOfProjection, dispatchChangePrimePlot,
        dispatchZoom, dispatchProcessScroll,
        dispatchChangeHiPS, dispatchChangeHipsImageConversion, visRoot} from '../ImagePlotCntlr.js';
import {makePlotSelectionExtActivateData} from '../../core/ExternalAccessUtils.js';
import {dispatchExtensionActivate} from '../../core/ExternalAccessCntlr.js';
import Catalog, {selectCatalog,unselectCatalog,filterCatalog,clearFilterCatalog} from '../../drawingLayers/Catalog.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import {SelectedShape} from '../../drawingLayers/SelectArea.js';
import {isImageOverlayLayersActive} from '../RelatedDataUtil.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import CoordUtil from '../CoordUtil.js';
import {CoordinateSys} from '../CoordSys.js';
import {parseImagePt} from '../Point.js';
import {ListBoxInputFieldView} from '../../ui/ListBoxInputField';
import {showHiPSSurverysPopup} from '../../ui/HiPSSurveyListDisplay.jsx';
import {HiPSId} from '../HiPSListUtil.js';
import {getSelectedShape} from '../../drawingLayers/Catalog.js';
import ImageOutline from '../../drawingLayers/ImageOutline.js';
import ShapeDataObj from '../draw/ShapeDataObj.js';
import {isOutlineImageForSelectArea, detachSelectArea, SELECT_AREA_TITLE} from './SelectAreaDropDownView.jsx';
import {convertAngle} from '../VisUtil.js';
import {convertToHiPS, convertToImage, doHiPSImageConversionIfNecessary} from '../task/PlotHipsTask.js';
import {RequestType} from '../RequestType.js';
import LSSTFootprint, {selectFootprint, unselectFootprint, filterFootprint, clearFilterFootprint} from '../../drawingLayers/ImageLineBasedFootprint';

import CROP from 'html/images/icons-2014/24x24_Crop.png';
import STATISTICS from 'html/images/icons-2014/24x24_Statistics.png';
import SELECTED from 'html/images/icons-2014/24x24_Checkmark.png';
import UNSELECTED from 'html/images/icons-2014/24x24_CheckmarkOff_Circle.png';
import FILTER from 'html/images/icons-2014/24x24_FilterAdd.png';
import CLEAR_FILTER from 'html/images/icons-2014/24x24_FilterOff_Circle.png';
import PAGE_RIGHT from 'html/images/icons-2014/20x20_PageRight.png';
import PAGE_LEFT from 'html/images/icons-2014/20x20_PageLeft.png';
import SELECTED_ZOOM from 'html/images/icons-2014/ZoomFitToSelectedSpace.png';
import SELECTED_RECENTER from 'html/images/icons-2014/RecenterImage-selection.png';


//todo move the statistics constants to where they are needed
const Metrics= {MAX:'MAX', MIN:'MIN', CENTROID:'CENTROID', FW_CENTROID:'FW_CENTROID', MEAN:'MEAN',
    STDEV:'STDEV', INTEGRATED_FLUX:'INTEGRATED_FLUX', NUM_PIXELS:'NUM_PIXELS', PIXEL_AREA:'PIXEL_AREA'};


/**
 * representing the number into decimal or exponential format
 * @param {number} [num]
 * @param {number} [fractionDigits=7] fractional digits after the decimal point
 * @return {string}
 */

function formatNumber(num, fractionDigits=7)
{
    let numStr;
    const SigDig = 7;


    if (num === null || num === undefined) {
        return '';
    }
    const thred = Math.min(Math.pow(10, SigDig - fractionDigits), 1.0);

    if (Math.abs(num) >= thred) {
        numStr = num.toFixed(fractionDigits);
    } else {
        numStr = num.toExponential(fractionDigits);
    }
    return numStr;
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

/**
 * @param pv
 * @param dlAry
 */
function stats(pv, dlAry) {
    //console.log('Stats getting: ' + primePlot(pv).title);
    //console.log(Metrics);
    const p= primePlot(pv);
    const cc= CysConverter.make(p);
    /*
    const sel= p.attributes[PlotAttribute.IMAGE_BOUNDS_SELECTION];

    const ip0=  cc.getImageCoords(sel.pt0);
    const ip2=  cc.getImageCoords(sel.pt1);
    const ip1=  makeImagePt(ip2.x,ip0.y);
    const ip3=  makeImagePt(ip0.x,ip2.y);
    */

    const sel= p.attributes[PlotAttribute.SELECTION];

    const ip0=  cc.getDeviceCoords(sel.pt0);
    const ip2=  cc.getDeviceCoords(sel.pt1);
    const ip1=  makeDevicePt(ip2.x,ip0.y);
    const ip3=  makeDevicePt(ip0.x,ip2.y);
    const shape = getSelectedShape(pv, dlAry);

    callGetAreaStatistics(p.plotState,
                            cc.getImageCoords(ip0),
                            cc.getImageCoords(ip1),
                            cc.getImageCoords(ip2),
                            cc.getImageCoords(ip3),
                            getSelectedShape(pv, dlAry),
                            (shape === SelectedShape.rect.key ? 0.0 : pv.rotation))
        .then( (wpResult) => {      // result of area stats

            // tabularize stats data
            const tblData = tabulateStatics(wpResult, cc);

            //console.log(wpResult);
            showImageAreaStatsPopup(p.title, tblData, pv.plotId);
        })
        .catch ( (e) => {
            logError(`error, stat , plotId: ${p.plotId}`, e);
        });

}


function crop(pv, dlAry) {

    if (isImageOverlayLayersActive(pv)) {
        showInfoPopup('Crop not yet supported with mask layers');
        return;
    }


    const p= primePlot(pv);
    const cc= CysConverter.make(p);
    const sel= p.attributes[PlotAttribute.IMAGE_BOUNDS_SELECTION];

    const ip0=  cc.getImageCoords(sel.pt0);
    const ip1=  cc.getImageCoords(sel.pt1);


    const cropMultiAll= pv.plotViewCtx.containsMultiImageFits && isMultiImageFitsWithSameArea(pv);

    dispatchCrop({plotId:pv.plotId, imagePt1:ip0, imagePt2:ip1, cropMultiAll});
    attachImageOutline(pv, dlAry);
    detachSelectArea(pv, true);
}



// attach image outline drawing layer on top of the cropped image, zoom-to-fit image and recenter image
function attachImageOutline(pv, dlAry) {
    const selectedShape = getSelectedShape(pv, dlAry);
    //if (selectedShape === SelectedShape.rect.key) return;

    const outlineAry = getDrawLayersByType(dlAry, ImageOutline.TYPE_ID);
    let   dl = outlineAry.find((dl) => isOutlineImageForSelectArea(dl));

    if (!dl) {
        const title = SELECT_AREA_TITLE;
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
        dl = dispatchCreateDrawLayer(ImageOutline.TYPE_ID,
                                    {drawObj, color: 'red', title, destroyWhenAllDetached: true});
    }

    if (!isDrawLayerAttached(dl, pv.plotId)) {
        dispatchAttachLayerToPlot(dl.drawLayerId, pv.plotId, true);
    }
}


function makeExtensionButtons(extensionAry,pv,dlAry) {
    if (!extensionAry) return false;
    return extensionAry.map( (ext,idx) => {
            return (
                <ToolbarButton icon={ext.imageUrl} text={ext.title}
                               tip={ext.toolTip} key={ext.id}
                               horizontal={true} enabled={true}
                               lastTextItem={idx===(extensionAry.length-1)}
                               onClick={() => dispatchExtensionActivate(ext,makePlotSelectionExtActivateData(ext,pv,dlAry))}/>
                );
        }
    );
}


function recenterToSelection(pv) {
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

    if (p.type==='image') {
        const newScrollPt= makeScreenPt(centerPt.x - viewDim.width/2, centerPt.y - viewDim.height/2);
        dispatchProcessScroll({plotId,scrollPt:newScrollPt});
    }
    else { // hips
        const centerProjPt= cc.getWorldCoords(centerPt, p.imageCoordSys);
        if (centerProjPt) dispatchChangeCenterOfProjection({plotId,centerProjPt});
    }

}


function zoomIntoSelection(pv, dlAry) {

    let p= primePlot(pv);
    if (!p) return;
    const {viewDim,plotId}= pv;
    let cc= CysConverter.make(p);
    const sel= p.attributes[PlotAttribute.IMAGE_BOUNDS_SELECTION];
    if (!sel) return;

    const sp0=  cc.getScreenCoords(sel.pt0);
    const sp2=  cc.getScreenCoords(sel.pt1);

    const level= Math.min(viewDim.width/Math.abs(sp0.x-sp2.x),
                          viewDim.height/Math.abs(sp0.y-sp2.y)) * p.zoomFactor;
    dispatchZoom({ plotId, userZoomType: UserZoomTypes.LEVEL, level});


    if (p.type==='image') {
        pv= getPlotViewById(visRoot(),plotId);
        p= primePlot(pv);
        cc= CysConverter.make(p);
        const ip0=  cc.getImageCoords(sel.pt0);
        const ip2=  cc.getImageCoords(sel.pt1);
        const centerPt= makeImagePt( Math.abs(ip0.x+ip2.x)/2, Math.abs(ip0.y+ip2.y)/2);
        const proposedSP= findScrollPtToCenterImagePt(pv, centerPt);
        dispatchProcessScroll({plotId,scrollPt:proposedSP});
    }
    else {
        const centerPt= makeScreenPt( Math.abs(sp0.x-sp2.x)/2+ Math.min(sp0.x,sp2.x),
                                      Math.abs(sp0.y-sp2.y)/2 + Math.min(sp0.y,sp2.y));
        const centerProjPt= cc.getWorldCoords(centerPt, p.imageCoordSys);
        if (centerProjPt) dispatchChangeCenterOfProjection({plotId,centerProjPt});

    }

    attachImageOutline(pv, dlAry);
    detachSelectArea(pv, true);

}

export function canConvertHipsAndFits(pv) {
    if (!pv || !pv.plotViewCtx.hipsImageConversion) return false;
    const {allSkyRequest, hipsRequestRoot, imageRequestRoot}= pv.plotViewCtx.hipsImageConversion;
    return (hipsRequestRoot && ( allSkyRequest ||imageRequestRoot));
}

function doConvert(pv,target) {
    if (!canConvertHipsAndFits(pv)) return;
    if (target==='fits') {
        convertToImage(pv,false);
    }
    else if (target==='hips') {
        convertToHiPS(pv,pv.request.getRequestType()===RequestType.ALL_SKY);
    }
    else if (target==='allsky') {
        convertToImage(pv,true);
    }

}

function changeAutoConvert(pv, auto) {
    dispatchChangeHipsImageConversion({plotId:pv.plotId, hipsImageConversionChanges:{autoConvertOnZoom:auto}});
    const nextPv= getPlotViewById(visRoot(), pv.plotId);
    if (auto) doHiPSImageConversionIfNecessary(nextPv);
}


function makeConvertButton(pv) {
    const plot= primePlot(pv);
    if (!plot) return null;
    const {allSkyRequest}= pv.plotViewCtx.hipsImageConversion;

    let value;
    if (isHiPS(plot)) {
        value= 'hips';
    }
    else {
        value= (pv.request.getRequestType()===RequestType.ALL_SKY) ? 'allsky' : 'fits';
    }

    const options= [
        {label: 'FITS', value: 'fits', tooltip: 'Scientific pixel data over limited regions'},
        {label: 'HiPS', value: 'hips', tooltip: 'All-sky multi-resolution picture with spherical projection'},
    ];
    allSkyRequest && options.push({label: 'Aitoff', value: 'allsky',
                            tooltip:'All-Sky single-resolution picture with Aitoff projection'});

    const buttonGroupTip= allSkyRequest  ? 'Auto-transition between FITS, HiPS, and Aitoff depending on zoom' :
                                           'Auto-transition between FITS and HiPS depending on zoom';

    const {autoConvertOnZoom:auto}= pv.plotViewCtx.hipsImageConversion;
    return (
            <div style={{display: 'flex', marginLeft: 5, marginRight: 5, marginTop: -6, padding: '1px 2px 1px 2px',
                         border: '1px solid rgba(60,60,60,.2', borderRadius: '5px'}}>
                <RadioGroupInputFieldView options={options}  value={value}
                                          buttonGroup={true}
                                          onChange={(ev) => doConvert(pv,ev.target.value)} />
                <div style={{paddingLeft: 3}} title={buttonGroupTip}>
                    <input type='checkbox' checked={auto} onChange={() => changeAutoConvert(pv, !auto)} />
                    Auto
                </div>
            </div>
    );
}
// Auto-transition

function makeHiPSImageTable(pv, surveysId) {
    const plot= primePlot(pv);
    if (!plot) return null;

    const inputEntry = () => {
        return (
            <div style={{margin: '0 5px 7px 4px'}}>
                <input  type='button'
                        value='Change HiPS'
                        title={'Choose a different HiPS Survey'}
                        onClick={()=>showHiPSSurverysPopup(get(primePlot(pv), 'hipsUrlRoot'),
                                                           pv, surveysId)} />
            </div>
        );
    };

    return (
        <div style={{display:'flex'}}>
            {inputEntry()}
        </div>
    );

}


function makeHiPSCoordSelect(pv) {
    const plot= primePlot(pv);
    if (!plot) return null;

    const options= [
        {label: 'Galactic', value:0, c: CoordinateSys.GALACTIC},
        {label: 'Eq J2000', value:1, c: CoordinateSys.EQ_J2000},
    ];

    let selectedIdx= options.findIndex( (s) => s.c===plot.imageCoordSys);

    if (selectedIdx===-1) selectedIdx= 0;



    return (
        <div style={{marginBottom:7}}>
            <ListBoxInputFieldView

                inline={true}
                value={selectedIdx}
                onChange={(ev) =>
                    dispatchChangeHiPS( {plotId:pv.plotId,  coordSys: options[Number(ev.target.value)].c})}
                labelWidth={0}
                label={' '}
                tooltip={ 'Change HiPS survey coordinate system'}
                options={options}
                multiple={false}
            />
        </div>
    );
}





/**
 *
 * @param pv
 * @param dlAry
 * @param extensionAry
 * @param showSelectionTools
 * @param showCatSelect
 * @param showCatUnSelect
 * @param showFilter
 * @param showClearFilter
 * @param showMultiImageController
 * @return {XML}
 * @constructor
 */
export class VisCtxToolbarView extends PureComponent {

    constructor(props) {
        super(props);
    }
    
    render() {
        const {
            plotView:pv, dlAry, extensionAry, showSelectionTools=false,
            showCatSelect=false, showCatUnSelect=false,
            showFilter=false, showClearFilter=false,
            showMultiImageController=false }= this.props;

        const rS= {
            width: '100% - 2px',
            display:'flex',
            height: 34,
            position: 'relative',
            verticalAlign: 'top',
            whiteSpace: 'nowrap',
            flexDirection:'row',
            flexWrap:'nowrap',
            alignItems: 'center'

        };

        const plot= primePlot(pv);
        const showOptions= showSelectionTools|| showCatSelect|| showCatUnSelect ||
                           showFilter || showClearFilter || !isEmpty(extensionAry) ||
                           isHiPS(plot) || canConvertHipsAndFits(pv);

        return (
            <div style={rS}>
                {showMultiImageController && <MultiImageControllerView plotView={pv} />}
                {showOptions && <div
                    style={{padding: '0 0 5px 2px', fontStyle: 'italic'}}>
                    Options:</div>
                }
                {showSelectionTools && isImage(plot) &&
                <ToolbarButton icon={CROP} tip='Crop the image to the selected area'
                               horizontal={true} onClick={() => crop(pv, dlAry)}/>}

                {showSelectionTools && isImage(plot) &&
                <ToolbarButton icon={STATISTICS} tip='Show statistics for the selected area'
                               horizontal={true} onClick={() => stats(pv, dlAry)}/>}

                {showCatSelect &&
                <ToolbarButton icon={SELECTED} tip='Mark data in area as selected'
                               horizontal={true} onClick={() => selectDrawingLayer(pv,dlAry)}/>}

                {showCatUnSelect &&
                <ToolbarButton icon={UNSELECTED} tip='Mark all data unselected'
                               horizontal={true} onClick={() => unselectDrawingLayer(pv,dlAry)}/>}

                {showFilter &&
                <ToolbarButton icon={FILTER} tip='Filter in the selected area'
                               horizontal={true} onClick={() => filterDrawingLayer(pv,dlAry)}/>}

                {showClearFilter &&
                <ToolbarButton icon={CLEAR_FILTER} tip='Clear all the Filters'
                               horizontal={true} onClick={() => clearFilterDrawingLayer(pv,dlAry)}/>}

                {showSelectionTools &&
                <ToolbarButton icon={SELECTED_ZOOM} tip='Zoom to fit selected area'
                               horizontal={true}
                               onClick={() => zoomIntoSelection(pv, dlAry)}/>}

                { showSelectionTools &&
                <ToolbarButton icon={SELECTED_RECENTER} tip='Recenter image to selected area'
                               horizontal={true} onClick={() => recenterToSelection(pv)}/>}


                {makeExtensionButtons(extensionAry,pv,dlAry)}

                {canConvertHipsAndFits(pv) && makeConvertButton(pv)}
                {isHiPS(plot) && makeHiPSCoordSelect(pv)}
                {isHiPS(plot) && makeHiPSImageTable(pv, HiPSId)}

            </div>
        );
    }
}



VisCtxToolbarView.propTypes= {
    plotView : PropTypes.object.isRequired,
    dlAry : PropTypes.arrayOf(PropTypes.object),
    extensionAry : PropTypes.arrayOf(PropTypes.object),
    showSelectionTools : PropTypes.bool,
    showCatSelect : PropTypes.bool,
    showCatUnSelect : PropTypes.bool,
    showFilter : PropTypes.bool,
    showClearFilter : PropTypes.bool,
    showMultiImageController : PropTypes.bool
};



ToolbarButton.defaultProps= {
    showSelectionTools : false,
    showCatSelect : false,
    showCatUnSelect : false,
    showFilter : false,
    extensionAry : null
};


const leftImageStyle= {
    verticalAlign:'bottom',
    cursor:'pointer',
    flex: '0 0 auto',
    paddingLeft: 3
};




const mulImStyle= {
    display:'inline-flex',
    height: 34,
    position: 'relative',
    verticalAlign: 'top',
    whiteSpace: 'nowrap',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center',
};


export function MultiImageControllerView({plotView:pv}) {

    const {plots,plotId}= pv;

    const plot= primePlot(pv);
    const image= isImage(plot);

    let cIdx;
    let nextIdx;
    let prevIdx;
    let length;
    let desc;

    if (image) {
        cIdx= plots.findIndex( (p) => p.plotImageId===plot.plotImageId);
        if (cIdx<0) cIdx= 0;
        nextIdx= cIdx===plots.length-1 ? 0 : cIdx+1;
        prevIdx= cIdx ? cIdx-1 : plots.length-1;
        length= plots.length;
        desc= plots[cIdx].plotDesc;
    }
    else {
        cIdx= plot.cubeIdx;
        nextIdx= cIdx===plot.cubeDepth-1 ? 0 : cIdx+1;
        prevIdx= cIdx ? cIdx-1 : plot.cubeDepth-1;
        length= plot.cubeDepth;
        // desc= '';
        desc= getHipsCubeDesc(plot);
    }

    if (length<3) leftImageStyle.visibility='hidden';
    if (cIdx<0) cIdx= 0;




    return (
        <div style={mulImStyle}>
            <div style={{fontStyle: 'italic', padding: '0 0 0 5px'}}>Image:</div>
            <img style={{...leftImageStyle}} src={PAGE_LEFT}
                 onClick={() => image ? dispatchChangePrimePlot({plotId,primeIdx:prevIdx}) : dispatchChangeHiPS({plotId, cubeIdx:prevIdx})}/>
            <img style={{verticalAlign:'bottom', cursor:'pointer', float: 'right', paddingLeft:3, flex: '0 0 auto'}}
                 src={PAGE_RIGHT}
                 onClick={() => image ? dispatchChangePrimePlot({plotId,primeIdx:nextIdx}): dispatchChangeHiPS({plotId, cubeIdx:nextIdx})} />
            {desc && <div style={{minWidth: '3em', padding:'0 5px 0 5px', fontWeight:'bold'}}>{desc}</div>}
            <div style={{minWidth: '3em', padding:'0 10px 0 4px'}}>{`${cIdx+1}/${length}`}</div>
        </div>
    );
}


MultiImageControllerView.propTypes= {
    plotView : PropTypes.object.isRequired,
};

const doFormat= (v,precision) => precision>0 ? numeral(v).format(padEnd('0.',precision+2,'0')) : Math.trunc(v)+'';

function getHipsCubeDesc(plot) {
    if (!isHiPS(plot)) return '';
    const {hipsProperties}= plot;
    const {data_cube_crpix3, data_cube_crval3, data_cube_cdelt3, data_cube_bunit3=''}= hipsProperties;
    if (!data_cube_crpix3 || !data_cube_crval3 || !data_cube_cdelt3) return '';
    const crpix3= Number(data_cube_crpix3);
    const crval3= Number(data_cube_crval3);
    const cdelt3= Number(data_cube_cdelt3);
    const dp= Math.abs(cdelt3)>10 ? 0 :  1- Math.trunc(Math.log10(Math.abs(cdelt3))); // number of decimal points suggestion of gpdf
    if (isNaN(crpix3) || isNaN(crval3) || isNaN(cdelt3)) return '';
    const value = crval3 + ( plot.cubeIdx - crpix3 ) * cdelt3;
    const bunit3= (data_cube_bunit3!=='null' && data_cube_bunit3!=='nil' && data_cube_bunit3!=='undefined') ?
                               data_cube_bunit3 : '';
    return `${doFormat(value,dp)} ${bunit3}`;
}

function selectDrawingLayer(pv,dlAry) {
    const allLayers = getAllDrawLayersForPlot(dlAry, pv.plotId, true);
    if (allLayers.length > 0) {
        if (allLayers.some((l) => l.drawLayerTypeId === Catalog.TYPE_ID)) {
            selectCatalog(pv, allLayers);
        }
        if (allLayers.some((l) => l.drawLayerTypeId === LSSTFootprint.TYPE_ID)) {
            selectFootprint(pv, allLayers);
        }
    }
}

function unselectDrawingLayer(pv,dlAry) {
    const allLayers = getAllDrawLayersForPlot(dlAry, pv.plotId, true);
    if (allLayers.length > 0) {
        if (allLayers.some((l) => l.drawLayerTypeId === Catalog.TYPE_ID)) {
            unselectCatalog(pv, allLayers);
        }
        if (allLayers.some((l) => l.drawLayerTypeId === LSSTFootprint.TYPE_ID)) {
            unselectFootprint(pv, allLayers);
        }
    }
}

function filterDrawingLayer(pv,dlAry) {
    const allLayers = getAllDrawLayersForPlot(dlAry, pv.plotId, true);
    if (allLayers.length > 0) {
        if (allLayers.some((l) => l.drawLayerTypeId === Catalog.TYPE_ID)) {
            filterCatalog(pv, allLayers);
        }
        if (allLayers.some((l) => l.drawLayerTypeId === LSSTFootprint.TYPE_ID)) {
            filterFootprint(pv, allLayers);
        }
    }
}

function clearFilterDrawingLayer(pv,dlAry) {
    const allLayers = getAllDrawLayersForPlot(dlAry, pv.plotId, true);
    if (allLayers.length > 0) {
        if (allLayers.some((l) => l.drawLayerTypeId === Catalog.TYPE_ID)) {
            clearFilterCatalog(pv, allLayers);
        }
        if (allLayers.some((l) => l.drawLayerTypeId === LSSTFootprint.TYPE_ID)) {
            clearFilterFootprint(pv, allLayers);
        }
    }
}
