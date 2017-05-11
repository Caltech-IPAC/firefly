/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isPlotNorth,getCenterPtOfPlot} from '../VisUtil.js';
import {encodeServerUrl} from '../../util/WebUtil.js';
import {getRootURL} from '../../util/BrowserUtil.js';
import {DrawerComponent} from '../draw/DrawerComponent.jsx';
import {makeScreenPt,makeWorldPt, makeDevicePt} from '../Point.js';
import {CysConverter} from '../CsysConverter.js';
import DirectionArrowDrawObj from '../draw/DirectionArrowDrawObj.js';
import FootprintObj from '../draw/FootprintObj.js';
import {COLOR_DRAW_1, COLOR_DRAW_2,Style} from '../draw/DrawingDef.js';
import {primePlot} from '../PlotViewUtil.js';
import {EventLayer} from './../iv/EventLayer.jsx';
import {MouseState} from '../VisMouseSync.js';
import {dispatchProcessScroll} from '../ImagePlotCntlr.js';
import {makeMouseStatePayload,fireMouseCtxChange} from '../VisMouseSync.js';
import {makeTransform,makeThumbnailTransformCSS} from '../PlotTransformUtils.js';
import {findScrollPtToCenterImagePt} from '../reducer/PlotView.js';


export class ThumbnailView extends PureComponent {

    constructor(props) {
        super(props);
        this.drawData= null;
        this.getDrawData= this.getDrawData.bind(this);
        this.onImageLoad= this.onImageLoad.bind(this);
        this.eventCallBack= this.eventCallBack.bind(this);
        this.setSimpleUpdateNotify= this.setSimpleUpdateNotify.bind(this);
        this.updateFunc= null;
        this.state= {imWidth:0,imHeight:0};
    }

    getDrawData() {
        return this.drawData;
    }

    onImageLoad(e) {
        if (e) {
            e.onload= () => {
                this.setState( () => ({imWidth: e.width, imHeight:e.height }));
            };
        }
    }

    setSimpleUpdateNotify(f) {
        this.updateFunc=f;
    }

    componentDidUpdate() {
        if (this.updateFunc) this.updateFunc(this.drawData);
    }

    eventCallBack(plotId,mouseState,pt) {
        const {plotView:pv}= this.props;
        const plot= primePlot(pv);
        if (!plot) return;
        const {width,height}= plot.serverImages.thumbnailImage;
        eventCB(mouseState,pt,pv,width,height);
    }
    
    render() {
        const {plotView:pv}= this.props;
        const {imWidth,imHeight}= this.state;

        const s= {
            width: 70,
            height: 70,
            display: 'inline-block',
            position: 'relative',
            overflow: 'hidden'
        };

        const plot= primePlot(pv);
        if (!plot) return  <div style={s}/>;

        s.border= '1px solid rgb(187, 187, 187)';
        const {width,height}= plot.serverImages.thumbnailImage;
        this.drawData= makeDrawing(pv,width,height);



        const w= imWidth || 70;
        const h= imHeight || 70;

        const affTrans= makeTransform(0,0,0,0,pv.rotation, pv.flipX, pv.flipY, {width:w,height:h});

        return (
            <div style={s}>
                {makeImageTag(pv, this.onImageLoad)}
                <DrawerComponent width={width} height={height} getDrawLayer={this.getDrawData}
                                 setSimpleUpdateNotify={this.setSimpleUpdateNotify}
                />
                <EventLayer width={width} height={height}
                            transform={affTrans}
                            eventCallback={this.eventCallBack} />
            </div>
        );
        
    }
}

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

function makeImageTag(pv, onImageLoad) {
    const plot= primePlot(pv);
    const {url,width,height}= plot.serverImages.thumbnailImage;
    const s= { position : 'absolute', left : 0, top : 0, width, height };
    const transFormCss= makeThumbnailTransformCSS(pv.rotation,pv.flipX, pv.flipY);
    
    if (transFormCss) s.transform= transFormCss;

    const params= {
        file : url,
        type : 'thumbnail',
        state : plot.plotState.toJson()
    };

    const imageURL=  encodeServerUrl(getRootURL() + 'sticky/FireFly_ImageDownload', params);

    return (
        <img src={imageURL} style={s} ref={onImageLoad} />
    );

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

    const arrowLength= (width+height)/3;
    const thumbZoomFact= getThumbZoomFact(plot,width,height);
    const cdelt1 = cc.getImagePixelScaleInDeg();
    const wpt2= makeWorldPt(wptC.getLon(), wptC.getLat() + (Math.abs(cdelt1)/thumbZoomFact)*(arrowLength/1.6));
    //const wptE2= makeWorldPt(wptC.getLon()+(Math.abs(cdelt1)/thumbZoomFact)*(arrowLength/2), wptC.getLat());
    const wptE1= wptC;
    const wpt1= wptC;

    const sptC = cc.getScreenCoords(wptC);
    const sptN = cc.getScreenCoords(wpt2);
    const [x, y] = [(sptN.y - sptC.y) + sptC.x, (-sptN.x + sptC.x)+sptC.y];
    const wptE2 = cc.getWorldCoords(makeScreenPt(x, y));

    const spt1= cc.getDeviceCoords(wpt1, thumbZoomFact, thumbnailTrans);
    const spt2= cc.getDeviceCoords(wpt2, thumbZoomFact, thumbnailTrans);
    const sptE1= cc.getDeviceCoords(wptE1, thumbZoomFact, thumbnailTrans);
    const sptE2= cc.getDeviceCoords(wptE2, thumbZoomFact, thumbnailTrans);


    if (!spt1 || !spt2 || !sptE1 || !sptE2) return null;

    let transX= 0;
    let transY= 0;

    if (isPlotNorth(plot) && !pv.rotation) {
        transY=+20;
        transX=+10;
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


    const fp= getScrollBoxInfo(pv, width, height);
    const scrollBox= FootprintObj.make([fp]);
    scrollBox.renderOptions= {
        shadow: {blur: 2, color: 'white', offX: 1, offY: 1}
    };
    scrollBox.style= Style.LIGHT;
    scrollBox.color= COLOR_DRAW_2;
    return [dataN,dataE,scrollBox];
}


