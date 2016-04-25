/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isPlotNorth} from '../VisUtil.js';
import {encodeServerUrl} from '../../util/WebUtil.js';
import {getRootURL} from '../../util/BrowserUtil.js';
import {DrawerComponent} from '../draw/DrawerComponent.jsx';
import {makeScreenPt,makeImagePt,makeWorldPt} from '../Point.js';
import {CysConverter} from '../CsysConverter.js';
import DirectionArrowDrawObj from '../draw/DirectionArrowDrawObj.js';
import ShapeDataObj from '../draw/ShapeDataObj.js';
import {COLOR_DRAW_1, COLOR_DRAW_2,Style} from '../draw/DrawingDef.js';
import {getScrollSize} from '../reducer/PlotView.js';
import {primePlot} from '../PlotViewUtil.js';
import {WebPlot} from '../WebPlot.js';
import {EventLayer} from './../iv/EventLayer.jsx';
import {MouseState} from '../VisMouseCntlr.js';
import {dispatchProcessScroll} from '../ImagePlotCntlr.js';




export class ThumbnailView extends Component {

    constructor(props) {
        super(props);
        this.drawData= null;
        this.getDrawData= this.getDrawData.bind(this);
        this.eventCallBack= this.eventCallBack.bind(this);
        this.setSimpleUpdateNotify= this.setSimpleUpdateNotify.bind(this);
        this.updateFunc= null;
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }
    
    getDrawData() {
        return this.drawData;
    }

    setSimpleUpdateNotify(f) {
        this.updateFunc=f;
    }

    componentDidUpdate() {
        if (this.updateFunc) this.updateFunc(this.drawData);
    }

    eventCallBack(plotId,mouseState,pt) {
        const {plotView:pv}= this.props;
        var plot= primePlot(pv);
        if (!plot) return;
        var {width,height}= plot.serverImages.thumbnailImage;
        eventCB(mouseState,pt,pv,width,height);
    }
    
    render() {
        const {plotView:pv}= this.props;
        
        var s= {
            width: 70,
            height: 70,
            display: 'inline-block',
            position: 'relative'
        };

        if (!pv) return  <div style={s}></div>;

        s.border= '1px solid rgb(187, 187, 187)';
        var plot= primePlot(pv);
        if (!plot) return  <div style={s}></div>;
        var {width,height}= plot.serverImages.thumbnailImage;
        var vp= WebPlot.makeViewPort(0,0,width,height);
        this.drawData= makeDrawing(pv,width,height);



        return (
            <div style={s}>
                {makeImageTag(plot)}
                <DrawerComponent width={width} height={height} getDrawLayer={this.getDrawData}
                                 setSimpleUpdateNotify={this.setSimpleUpdateNotify}
                />
                <EventLayer viewPort={vp} width={width} height={height}
                            eventCallback={this.eventCallBack} />
            </div>
        );
        
    }
}

ThumbnailView.propTypes= {
    plotView: React.PropTypes.object
};

function eventCB(mouseState,pt,pv,width,height) {
    if (pt && pv) {
        switch (mouseState) {
            case MouseState.DOWN :
            case MouseState.DRAG :
                scrollPlot(pt,pv,width,height);
                //console.log(`sx:${pt.x}, sy:${pt.y}`);
                break;
        }
    }
}

function scrollPlot(pt,pv,width,height) {

    var plot= primePlot(pv);
    var fact= getThumbZoomFact(plot,width,height);


    var cc= CysConverter.make(plot);
    var {tsWidth, tsHeight}= getScrollBoxInfo(pv, width, height);

    var halfBoxWidth= tsWidth/2;
    var halfBoxHeight= tsHeight/2;


    var x= Math.min(pt.x-halfBoxWidth, width-halfBoxHeight);
    var y= Math.min(pt.y-halfBoxHeight,height-halfBoxHeight);

    var ipt= cc.getImageWorkSpaceCoords(makeScreenPt(x,y),fact);
    var spt= cc.getScreenCoords(ipt);


    dispatchProcessScroll(pv.plotId,spt);
    //_pv.fireMouseMove(spt,null); ===>todo: dispatch a mouse readout action here
}


function makeImageTag(plot) {
    var {url,width,height}= plot.serverImages.thumbnailImage;
    var s= { position : 'absolute', left : 0, top : 0, width, height };
    var params= {
        file : url,
        type : 'thumbnail',
        state : plot.plotState.toJson()
    };

    var imageURL=  encodeServerUrl(getRootURL() + 'sticky/FireFly_ImageDownload', params);

    return (
        <img src={imageURL} style={s}/>
    );

}

function getThumbZoomFact(plot, thumbW, thumbH) {
    var tSize= Math.max(thumbW, thumbH);
    var iSize= Math.max( plot.dataWidth, plot.dataHeight);
    return tSize / iSize;
}

function getScrollBoxInfo(pv, thumbW, thumbH) {
    var plot= primePlot(pv);
    var fact= getThumbZoomFact(plot,thumbW,thumbH)/ plot.zoomFactor;

    var mx= pv.wcsMarginX;
    var my= pv.wcsMarginY;
    var ss= getScrollSize(pv);

    return {
        tsX: (pv.scrollX-mx)*fact,
        tsY: (pv.scrollY-my)*fact,
        tsWidth: ss.scrollWidth*fact,
        tsHeight: ss.scrollHeight*fact
    };

}



//function drawOnCanvas(c,ary,width,height) {
//    if (!c || !ary) return;
//
//    var [dataN,dataE,scrollBox]= ary;
//    var textDrawAry=[];
//    var drawDef= makeDrawingDef(COLOR_DRAW_1);
//    var ctx= c.getContext('2d');
//    DrawUtil.clear(ctx,width,height);
//    DirectionArrowDrawObj.draw.draw(dataN,ctx,textDrawAry,null,drawDef,null,null);
//    DirectionArrowDrawObj.draw.draw(dataE,ctx,textDrawAry,null,drawDef,null,null);
//    ShapeDataObj.draw.draw(scrollBox,ctx,textDrawAry,null,drawDef,null,null);
//}



function makeDrawing(pv,width,height) {
    var plot= primePlot(pv);
    var {dataWidth,dataHeight}= plot;
    var cc= CysConverter.make(plot);
    var wptC= cc.getWorldCoords(makeImagePt(dataWidth/2,dataHeight/2));
    if (!wptC) return null;

    var arrowLength= (width+height)/3;
    var thumbZoomFact= getThumbZoomFact(plot,width,height);
    var cdelt1 = cc.getImagePixelScaleInDeg();
    var wpt2= makeWorldPt(wptC.getLon(), wptC.getLat() + (Math.abs(cdelt1)/thumbZoomFact)*(arrowLength/1.6));
    var wptE2= makeWorldPt(wptC.getLon()+(Math.abs(cdelt1)/thumbZoomFact)*(arrowLength/2), wptC.getLat());
    var wptE1= wptC;
    var wpt1= wptC;

    var spt1= cc.getScreenCoords(wpt1, thumbZoomFact);
    var spt2= cc.getScreenCoords(wpt2, thumbZoomFact);

    var sptE1= cc.getScreenCoords(wptE1, thumbZoomFact);
    var sptE2= cc.getScreenCoords(wptE2, thumbZoomFact);

    if (!spt1 || !spt2 || !sptE1 || !sptE2) return null;

    var transX= 0;
    var transY= 0;

    if (isPlotNorth(plot)) {
        transY=+20;
        transX=+10;
    }
    else {
        if (spt2.y<15)       transY-=(spt2.y-15);
        if (spt2.y>height-9) transY-=(height-spt2.y+9);
        if (spt2.x<9)        transX-=(spt2.x-9);
        if (spt2.x>width-9)  transX-=(width-spt2.x+9);
    }

    var renderOptions= {
        translation: makeScreenPt(transX, transY),
        shadow: {blur: 2, color: 'white', offX: 1, offY: 1}
    };
    var dataN= DirectionArrowDrawObj.makeDirectionArrowDrawObj(spt1, spt2,'N');
    var dataE= DirectionArrowDrawObj.makeDirectionArrowDrawObj(sptE1, sptE2,'E');
    dataN.renderOptions= renderOptions;
    dataE.renderOptions= renderOptions;
    dataN.color= COLOR_DRAW_1;
    dataE.color= COLOR_DRAW_1;


    //var textDrawAry=[];
    //var drawDef= makeDrawingDef(COLOR_DRAW_1);

    var {tsX, tsY, tsWidth, tsHeight}= getScrollBoxInfo(pv, width, height);

    //var boxDef= makeDrawingDef(COLOR_DRAW_2);
    var scrollBox= ShapeDataObj.makeRectangle(makeScreenPt(tsX,tsY),tsWidth,tsHeight);
    scrollBox.renderOptions= {
        shadow: {blur: 2, color: 'white', offX: 1, offY: 1}
    };
    scrollBox.style= Style.LIGHT;
    scrollBox.color= COLOR_DRAW_2;
    return [dataN,dataE,scrollBox];
}


