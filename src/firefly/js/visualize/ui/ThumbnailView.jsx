/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box} from '@mui/joy';
import React, {memo, useEffect, useRef, useState} from 'react';
import PropTypes from 'prop-types';
import {getCenterPtOfPlot, isPlotNorth} from '../WebPlotAnalysis';
import {DrawerComponent} from '../draw/DrawerComponent.jsx';
import {makeScreenPt,makeWorldPt, makeDevicePt} from '../Point.js';
import {CysConverter} from '../CsysConverter.js';
import DirectionArrowDrawObj from '../draw/DirectionArrowDrawObj.js';
import FootprintObj from '../draw/FootprintObj.js';
import {COLOR_DRAW_1, COLOR_DRAW_2,Style} from '../draw/DrawingDef.js';
import {hasLocalStretchByteData, primePlot} from '../PlotViewUtil.js';
import {EventLayer} from '../iv/EventLayer.jsx';
import {MouseState} from '../VisMouseSync.js';
import {dispatchProcessScroll} from '../ImagePlotCntlr.js';
import {makeMouseStatePayload,fireMouseCtxChange} from '../VisMouseSync.js';
import {makeTransform,makeThumbnailTransformCSS} from '../PlotTransformUtils.js';
import {findScrollPtToCenterImagePt} from '../reducer/PlotView.js';
import {getPixScaleDeg, isCelestialImage, isHiPS} from '../WebPlot.js';
import {getEntry} from '../rawData/RawDataCache.js';
import {SimpleCanvas} from '../draw/SimpleCanvas.jsx';


export const ThumbnailView = memo(({plotView:pv, defSize=70, loadedSize= 70}) => {
    const plot= primePlot(pv);
    const localData= hasLocalStretchByteData(plot);
    const size= localData ? loadedSize : defSize;
    const s= { width: size, height: size, display: 'inline-block', position: 'relative', overflow: 'hidden' };

    const [dim, setDim] = useState({imWidth:0,imHeight:0});
    const dataRef = useRef({updateFunc:undefined, drawData:undefined});
    useEffect(() => {
       dataRef.current.updateFunc?.(dataRef.current.drawData);
    }, [dataRef.current.drawData]);

    if ( (!plot?.tileData && !localData) || isHiPS(plot)) return <div style={s}/>;

    s.border= '1px solid rgb(187, 187, 187)';
    let width= size;
    let height= size;
    if (!localData && plot.tileData?.thumbnailImage) {
        width= plot.tileData.thumbnailImage.width;
        height= plot.tileData.thumbnailImage.height;
    }
    dataRef.current.drawData= makeDrawing(pv,width,height);

    const affTrans= makeTransform(0,0,0,0,pv.rotation, pv.flipX, pv.flipY, {width,height});

    const eventCallBack= (plotId,mouseState,pt) => {
        if ( (!plot?.tileData && !localData) || isHiPS(plot)) return;
        eventCB(mouseState,pt,pv,width,height);
    };

    const setSimpleUpdateNotify= (f) => dataRef.current.updateFunc= f;
    const onImageLoad= (e) => e && (e.onload= () => setDim( {imWidth: e.width, imHeight:e.height }));

    return (
        <Box style={s}>
            {makeImageTag(pv, onImageLoad, size)}
            <DrawerComponent width={width} height={height} getDrawLayer={() => dataRef.current.drawData}
                             setSimpleUpdateNotify={setSimpleUpdateNotify} />
            <EventLayer width={width} height={height} transform={affTrans} eventCallback={eventCallBack} />
        </Box>
    );
});




ThumbnailView.propTypes= {
    plotView: PropTypes.object
};

function eventCB(mouseState,pt,pv,width,height) {
    if (pt && pv) {
        switch (mouseState) {
            case MouseState.DOWN :
            case MouseState.DRAG :
                scrollPlot(pt,pv,width,height);
                break;
            default:
                updateMove(mouseState,pt,pv,width,height);
        }
    }
}

/**
 *
 * @param mouseState
 * @param {ScreenPt} pt
 * @param {PlotView} pv
 * @param width
 * @param height
 */
function updateMove(mouseState, pt,pv,width,height) {
    const plot= primePlot(pv);
    const fact= getThumbZoomFact(plot,width,height);
    const cc= CysConverter.make(plot);
    const ipt= cc.getImageWorkSpaceCoords(pt,fact);
    const spt= cc.getScreenCoords(ipt);
    const payload= makeMouseStatePayload(pv.plotId, mouseState,spt,spt.x,spt.y);
    fireMouseCtxChange(payload);
}



function scrollPlot(pt,pv,width,height) {
    const plot= primePlot(pv);
    const fact= plot.zoomFactor / getThumbZoomFact(plot,width,height);
    const cc= CysConverter.make(plot);
    const imPt= cc.getImageCoords(makeScreenPt(pt.x*fact, pt.y*fact));
    const spt= findScrollPtToCenterImagePt(pv, imPt);
    dispatchProcessScroll({plotId:pv.plotId,scrollPt:spt});
}

function makeImageTag(pv, onImageLoad,size) {
    const plot= primePlot(pv);
    const {width=size,height=size}= plot.tileData?.thumbnailImage ?? {};
    const s= { position : 'absolute', left : 0, top : 0, width, height };
    const transFormCss= makeThumbnailTransformCSS(pv.rotation,pv.flipX, pv.flipY);
    
    if (transFormCss) s.transform= transFormCss;
    if (!hasLocalStretchByteData(plot)) return <div/>;

    const thumbnailCanvas= getEntry(plot?.plotImageId)?.thumbnailEncodedImage;

    const drawOnCanvas= (targetCanvas) => {
        if (!targetCanvas) return;
        const ctx= targetCanvas.getContext('2d');
        ctx.clearRect(0, 0, targetCanvas.width, targetCanvas.height);
        ctx.drawImage(thumbnailCanvas,0,0);
    };

    return (
        <div style={s}>
            <SimpleCanvas drawIt={drawOnCanvas} width={thumbnailCanvas.width} height={thumbnailCanvas.height}
                          id={'thumbnail'}/>
        </div>);
}

function getThumbZoomFact(plot, thumbW, thumbH) {
    const tSize= Math.max(thumbW, thumbH);
    const iSize= Math.max( plot.dataWidth, plot.dataHeight);
    return tSize / iSize;
}

function getScrollBoxInfo(pv, thumbW, thumbH) {


    const plot= primePlot(pv);
    const fact= getThumbZoomFact(plot,thumbW,thumbH)/ plot.zoomFactor;
    const {viewDim}= pv;
    const hW= viewDim.width/2*fact;
    const hH= viewDim.height/2*fact;

    const cc= CysConverter.make(plot);
    const cenP= cc.getScreenCoords(makeDevicePt(viewDim.width/2, viewDim.height/2));
    const cScreen= makeScreenPt(cenP.x*fact, cenP.y*fact);


    const thumbnailTrans= makeTransform(0,0,0,0,pv.rotation,pv.flipX,pv.flipY,{width:thumbW,height:thumbH} );
    const thumbCC= CysConverter.make(plot, thumbnailTrans);
    const c= thumbCC.getDeviceCoords(cScreen,getThumbZoomFact(plot,thumbW,thumbH), thumbnailTrans);

    const screenFp= [
        makeScreenPt(c.x-hW,c.y-hH),
        makeScreenPt(c.x+hW,c.y-hH),
        makeScreenPt(c.x+hW,c.y+hH),
        makeScreenPt(c.x-hW,c.y+hH),
    ];

    return screenFp;
}

function makeDrawing(pv,width,height) {
    const plot= primePlot(pv);
    const thumbnailTrans= makeTransform(0,0,0,0,pv.rotation,pv.flipX,pv.flipY,{width,height} );
    const cc= CysConverter.make(plot, thumbnailTrans);
    const wptC= getCenterPtOfPlot(plot);
    if (!wptC) return null;

    const fp= getScrollBoxInfo(pv, width, height);
    const scrollBox= FootprintObj.make([fp]);
    scrollBox.renderOptions= {
        shadow: {blur: 2, color: 'white', offX: 1, offY: 1}
    };
    scrollBox.style= Style.LIGHT;
    scrollBox.color= COLOR_DRAW_2;

    if (!isCelestialImage(plot)) {
        return [undefined, undefined, scrollBox];
    }

    // E and N arrows
    const arrowLength= (Math.min(width,70)+Math.min(height,70))/3;
    const thumbZoomFact= getThumbZoomFact(plot,width,height);
    const cdelt1 = getPixScaleDeg(plot);
    const wpt2= makeWorldPt(wptC.getLon(), wptC.getLat() + (Math.abs(cdelt1)/thumbZoomFact)*(arrowLength/1.6));
    //const wptE2= makeWorldPt(wptC.getLon()+(Math.abs(cdelt1)/thumbZoomFact)*(arrowLength/2), wptC.getLat());
    const wptE1= wptC;
    const wpt1= wptC;
    const wpt3= makeWorldPt(wptC.getLon() +
                            (Math.abs(cdelt1)/(Math.cos(wptC.getLat()*Math.PI/180.)/thumbZoomFact)*(arrowLength/1.6)),
                            wptC.getLat());


    const sptC = cc.getScreenCoords(wptC);
    const sptN = cc.getScreenCoords(wpt2);
    if (!sptC || !sptN) return null;
    const spt3= cc.getScreenCoords(wpt3);
    // don't use spt3 because of funny effects near the celestial poles
    // the sign of the cross product of compass vectors tells us if the image is mirror-reversed from the sky
    const cross_product= spt3 ? ((spt3.x - sptC.x)*(sptN.y - sptC.y) - (spt3.y - sptC.y)*(sptN.x - sptC.x)) : 1;
    const sign= Math.sign(cross_product);

    const [x, y] = [sign*(sptN.y - sptC.y) + sptC.x, sign*(-sptN.x + sptC.x)+sptC.y];
    const wptE2 = cc.getWorldCoords(makeScreenPt(x, y));

    const spt1= cc.getDeviceCoords(wpt1, thumbZoomFact, thumbnailTrans);
    const spt2= cc.getDeviceCoords(wpt2, thumbZoomFact, thumbnailTrans);
    const sptE1= cc.getDeviceCoords(wptE1, thumbZoomFact, thumbnailTrans);
    const sptE2= cc.getDeviceCoords(wptE2, thumbZoomFact, thumbnailTrans);


    if (!spt1 || !spt2 || !sptE1 || !sptE2) return null;

    let transX= 0;
    let transY= 0;

    if (isPlotNorth(plot) && !pv.rotation) {
        transY+=20;
        transX+=10;
    }
    else {
        if (spt2.y<15)       transY-=(spt2.y-15);
        if (spt2.y>height-9) transY-=(height-spt2.y+9);
        if (spt2.x<9)        transX-=(spt2.x-9);
        if (spt2.x>width-9)  transX-=(width-spt2.x+9);
    }

    const renderOptions= {
        translation: makeScreenPt(transX, transY),
        shadow: {blur: 2, color: 'white', offX: 1, offY: 1}
    };
    const dataN= DirectionArrowDrawObj.makeDirectionArrowDrawObj(spt1, spt2,'N');
    const dataE= DirectionArrowDrawObj.makeDirectionArrowDrawObj(sptE1, sptE2,'E');
    dataN.renderOptions= renderOptions;
    dataE.renderOptions= renderOptions;
    dataN.color= COLOR_DRAW_1;
    dataE.color= COLOR_DRAW_1;

    return [dataN,dataE,scrollBox];
}
