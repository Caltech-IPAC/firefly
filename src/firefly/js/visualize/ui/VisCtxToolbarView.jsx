/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../../Firefly.js';
import {isEmpty, get} from 'lodash';
import {primePlot,isMultiImageFitsWithSameArea, getPlotViewById} from '../PlotViewUtil.js';
import {findScrollPtToCenterImagePt} from '../reducer/PlotView.js';
import {CysConverter} from '../CsysConverter.js';
import {PlotAttribute,isHiPS, isImage} from '../WebPlot.js';
import {makeDevicePt, makeScreenPt, makeImagePt} from '../Point.js';
import {callGetAreaStatistics} from '../../rpc/PlotServicesJson.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {logError} from '../../util/WebUtil.js';
import {showImageAreaStatsPopup} from './ImageStatsPopup.jsx';
import {getDrawLayersByType, isDrawLayerAttached } from '../PlotViewUtil.js';

import {dispatchCreateDrawLayer,
        dispatchAttachLayerToPlot} from '../DrawLayerCntlr.js';
import {dispatchCrop, dispatchChangeCenterOfProjection, dispatchChangePrimePlot,
        dispatchZoom, dispatchProcessScroll, dispatchChangeHiPS, visRoot} from '../ImagePlotCntlr.js';
import {makePlotSelectionExtActivateData} from '../../core/ExternalAccessUtils.js';
import {dispatchExtensionActivate} from '../../core/ExternalAccessCntlr.js';
import {selectCatalog,unselectCatalog,filterCatalog,clearFilterCatalog} from '../../drawingLayers/Catalog.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import {SelectedShape} from '../../drawingLayers/SelectArea.js';
import {isImageOverlayLayersActive} from '../RelatedDataUtil.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import CoordUtil from '../CoordUtil.js';
import {CoordinateSys} from '../CoordSys.js';
import { parseImagePt } from '../Point.js';
import {getDefaultHiPSSurveys} from '../HiPSUtil.js';
import {ListBoxInputFieldView} from '../../ui/ListBoxInputField';
import {showHiPSSurverysPopup} from '../../ui/HiPSSurveyListDisplay.jsx';
import {isLoadingHiPSSurverys, HiPSId} from '../HiPSCntlr.js';
import {getSelectedShape} from '../../drawingLayers/Catalog.js';
import ImageOutline from '../../drawingLayers/ImageOutline.js';
import ShapeDataObj from '../draw/ShapeDataObj.js';
import {isOutlineImageForSelectArea, detachSelectArea, SELECT_AREA_TITLE} from './SelectAreaDropDownView.jsx';
import {convertAngle} from '../VisUtil.js';

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

import LOADING from 'html/images/gxt/loading.gif';


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


function makeHiPSImageSelect(pv) {
    const plot= primePlot(pv);
    if (!plot) return null;


    const surveyList= plot.surveyList || getDefaultHiPSSurveys();


    let selectedIdx= surveyList.findIndex( (s) => s.url===plot.hipsUrlRoot);
    if (selectedIdx===-1) selectedIdx= 0;

    const options= surveyList.map( (s,idx) => ({label: s.label, value:idx}) );


    return (
        <ListBoxInputFieldView

            inline={true}
            value={selectedIdx}
            onChange={(ev) =>
                dispatchChangeHiPS( {plotId:pv.plotId,  hipsUrlRoot:surveyList[Number(ev.target.value)].url})}
            labelWidth={10}
            label={' '}
            tooltip={ 'Choose a different HiPS survey'}
            options={options}
            multiple={false}
        />
        );
}

function makeHiPSImageTable(pv, surveysId, isUpdatingHips) {
    const plot= primePlot(pv);
    if (!plot) return null;

    const inputEntry = () => {
        return (
            <div style={{marginLeft: 10, marginRight: 5}}>
                <input  type='button'
                        value='Find HiPS Plot'
                        onClick={()=>showHiPSSurverysPopup(get(pv, ['request', 'params', 'hipsRootUrl']),
                                                           pv, surveysId)} />
            </div>
        );
    };

    const styleLoading = {width:14,height:14};
    return (
        <div style={{display:'flex'}}>
            {inputEntry()}
            {isUpdatingHips ? <img style={styleLoading} src={LOADING}/> :
                              <div style={styleLoading}/>}
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
        <ListBoxInputFieldView

            inline={true}
            value={selectedIdx}
            onChange={(ev) =>
                dispatchChangeHiPS( {plotId:pv.plotId,  coordSys: options[Number(ev.target.value)].c})}
            labelWidth={10}
            label={' '}
            tooltip={ 'Choose a different HiPS survey'}
            options={options}
            multiple={false}
        />
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

        this.hipsId = HiPSId;
        if (this.hipsId) {
            const isUpdatingHips = isLoadingHiPSSurverys(this.hipsId);

            this.state = {isUpdatingHips};
        }
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(()=>this.storeUpdate());
    }

    storeUpdate() {

        if (this.iAmMounted) {
            const isUpdatingHips = isLoadingHiPSSurverys(this.hipsId);

            if (isUpdatingHips !== get(this.state, 'isUpdatingHips', false)) {
                this.setState({isUpdatingHips});
            }
        }
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

        const {isUpdatingHips} = this.state || {};
        const plot= primePlot(pv);
        const showOptions= showSelectionTools|| showCatSelect|| showCatUnSelect ||
                           showFilter || showClearFilter || !isEmpty(extensionAry) || isHiPS(plot);

        return (
            <div style={rS}>
                {showMultiImageController && <MultiImageControllerView plotView={pv} />}
                {showOptions && <div
                    style={{display: 'inline-block', padding: '8px 7px 0 8px', alignSelf:'flex-start',
                        float : 'left', fontStyle: 'italic'}}>
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
                               horizontal={true} onClick={() => selectCatalog(pv,dlAry)}/>}

                {showCatUnSelect &&
                <ToolbarButton icon={UNSELECTED} tip='Mark all data unselected'
                               horizontal={true} onClick={() => unselectCatalog(pv,dlAry)}/>}

                {showFilter &&
                <ToolbarButton icon={FILTER} tip='Filter in the selected area'
                               horizontal={true} onClick={() => filterCatalog(pv,dlAry)}/>}

                {showClearFilter &&
                <ToolbarButton icon={CLEAR_FILTER} tip='Clear all the Filters'
                               horizontal={true} onClick={() => clearFilterCatalog(pv,dlAry)}/>}

                {showSelectionTools &&
                <ToolbarButton icon={SELECTED_ZOOM} tip='Zoom to fit selected area'
                               horizontal={true}
                               onClick={() => zoomIntoSelection(pv, dlAry)}/>}

                { showSelectionTools &&
                <ToolbarButton icon={SELECTED_RECENTER} tip='Recenter image to selected area'
                               horizontal={true} onClick={() => recenterToSelection(pv)}/>}


                {makeExtensionButtons(extensionAry,pv,dlAry)}

                {isHiPS(plot) && makeHiPSImageTable(pv, this.hipsId, isUpdatingHips)}
                {isHiPS(plot) && makeHiPSCoordSelect(pv)}

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
        desc= '';
    }

    if (length<3) leftImageStyle.visibility='hidden';
    if (cIdx<0) cIdx= 0;




    return (
        <div style={mulImStyle}>
            <div style={{fontStyle: 'italic', padding: '8px 0 0 5px', alignSelf:'flex-start'}}>Image:</div>
            <img style={leftImageStyle} src={PAGE_LEFT}
                 onClick={() => image ? dispatchChangePrimePlot({plotId,primeIdx:prevIdx}) : dispatchChangeHiPS({plotId, cubeIdx:prevIdx})}/>
            <img style={{verticalAlign:'bottom', cursor:'pointer', float: 'right', paddingLeft:3, flex: '0 0 auto'}}
                 src={PAGE_RIGHT}
                 onClick={() => image ? dispatchChangePrimePlot({plotId,primeIdx:nextIdx}): dispatchChangeHiPS({plotId, cubeIdx:nextIdx})} />
            {desc && <div style={{minWidth: '3em', padding:'0 10px 0 5px', fontWeight:'bold'}}>{desc}</div>}
            <div style={{minWidth: '3em', paddingLeft:4}}>{`${cIdx+1}/${length}`}</div>
        </div>
    );
}


MultiImageControllerView.propTypes= {
    plotView : PropTypes.object.isRequired,
};

