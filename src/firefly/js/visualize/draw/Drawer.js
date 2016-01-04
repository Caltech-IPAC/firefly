/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import Enum from 'enum';
import Point, {makeViewPortPt,makeImagePt,pointEquals} from '../Point.js';
import AppDataCntlr from '../../core/AppDataCntlr.js';
import BrowserInfo, {Browser} from '../../util/BrowserInfo.js';
import DrawUtil from './DrawUtil.js';
import Color from '../../util/Color.js';
import CsysConverter, {CCUtil} from '../CsysConverter.js';
import {POINT_DATA_OBJ} from './PointDataObj.js';
import DrawOp from './DrawOp.js';
import join from 'underscore.string/join';



const ENABLE_COLORMAP= false;
//const DataType = new Enum (['VERY_LARGE', 'NORMAL']);
var drawerCnt=0;
const DEFAULT_DEFAULT_COLOR= 'red';



class Drawer {


    constructor(drawingDef) {
        this.drawerID;
        this.drawingDef= drawingDef;
        this.data;
        this.highlightData;
        this.drawConnect= null;
        this.selectedIdxAry=[];

        this.plot= null;
        this.primaryCanvas= null;
        this.selectCanvas= null;
        this.highlightCanvas= null;
        this.drawingCanceler= null;
        this.plotTaskId= null;
        //this.dataUpdater= null;
        this.isPointData= false;
        this.decimate= false;

        this.decimatedData= null;
        this.decimateDim= null;
        this.lastDecimationPt= null;
        this.lastDecimationColor= null;
        this.drawTextAry= [];
        this.textUpdateCallback= null;
        //this.highPriorityLayer= false;
        this.drawerId= drawerCnt++;
    }




    setDataTypeHint(dataTypeHint) { this.dataTypeHint= dataTypeHint; }

    setHighPriorityLayer(highPriorityLayer) { this.highPriorityLayer = highPriorityLayer; }


    /**
     * when the image resize, like a zoom, then fire events to redraw
     * Certain types of data will need to recompute the data when the image size changes so this
     * methods disables the default automactic handling
     * By default, this property is true
     * @param h handle image changes
     */
    setHandleImageChanges(h) { this.handleImagesChanges = h; }



    dispose() {
        this.primaryCanvas= null;
        this.selectCanvas= null;
        this.highlightCanvas= null;
        this.data= null;
        this.decimatedData= null;
    }


    setPointConnector(connector) { this.drawConnect= connector; }

    setEnableDecimationDrawing(d) { this.decimate= d; }

    setPlotChangeDataUpdater(dataUpdater) { this.dataUpdater= dataUpdater; }


    cancelRedraw() {
        if (this.drawingCanceler) {
            this.drawingCanceler();
            this.drawingCanceler= null;
        }
    }


    /**
     *
     * @param {[]} data the list of DataObj
     * @param plot
     * @param width
     * @param height
     */
    setData(data,plot,width,height) {
        if (data && !Array.isArray(data)) data= [data];
        var cWidth= 0;
        var cHeight= 0;
        var oldvpX= 0;
        var oldvpY= 0;
        var vpX= 0;
        var vpY= 0;
        var dWidth= 0;
        var oldDWidth= 0;
        var dHeight= 0;
        var oldDHeight= 0;
        var zfact= 0;
        var oldZfact= 0;
        var oldTestPtStr;
        var testPtStr;
        var pt;
        width= Math.floor(width);
        height= Math.floor(height);
        if (this.primaryCanvas) {
            cWidth= this.primaryCanvas.width;
            cHeight= this.primaryCanvas.height;
        }

        if (plot) {
            vpX= plot.viewPort.x;
            vpY= plot.viewPort.y;
            dWidth= plot.dataWidth;
            dHeight= plot.dataHeight;
            zfact= Math.round(plot.zoomFactor*100000)/100000;
            pt= CCUtil.getWorldCoords(plot,makeImagePt(1,1));
            testPtStr= pt ? pt.toString() : '';
        }

        if (this.plot) {
            oldvpX= this.plot.viewPort.x;
            oldvpY= this.plot.viewPort.y;
            oldDWidth= this.plot.dataWidth;
            oldDHeight= this.plot.dataHeight;
            oldZfact= Math.round(this.plot.zoomFactor*100000)/100000;
            pt= CCUtil.getWorldCoords(this.plot,makeImagePt(1,1));
            oldTestPtStr= pt ? pt.toString() : '';
        }


        if (data && this.data && data===this.data &&
            cWidth===width && cHeight===height &&
            oldvpX===vpX && oldvpY===vpY &&
            dWidth===oldDWidth && dHeight===oldDHeight  &&
            zfact===oldZfact  && testPtStr===oldTestPtStr ) {
            return;
        }


        if (data!==this.data || this.plot!==plot) {
            this.decimatedData= null;
        }

        //======== DEBUG =============================
        //var changes= [this.primaryCanvas ? '' : 'no canvas'];
        var changes= [];
        if (data!==this.data) changes.push('data');
        //if (plot!==this.plot) changes.push('plot');
        if (cWidth!==width ) changes.push(`width: ${width}, ${cWidth}`);
        if (cHeight!==height ) changes.push(`height: ${height}, ${cHeight}`);
        if (dWidth!==oldDWidth ) changes.push('data width');
        if (dHeight!==oldDHeight ) changes.push('data height');
        if (zfact!==oldZfact ) changes.push(`zoom factor ${oldZfact}, ${zfact}`);
        if (testPtStr!==oldTestPtStr ) changes.push('test pt');
        if (oldvpX!==vpX ) changes.push('vpX');
        if (oldvpY!==vpY ) changes.push('vpY');
        var changeStr= join(', ',...changes);
        console.log(`Drawer ${this.drawerId}: redraw- changes: ${changeStr}`);
        //=====================================
        this.plot= plot;
        this.data = data;

        this.dataUpdated(width,height);
    }



    setPrimCanvas(c, width, height) {
        if (c && c!=this.primaryCanvas) {
            this.primaryCanvas= c;
            //console.log(`Drawer ${this.drawerId}: redraw primary- canvas update`);
            this.dataUpdated(width,height);
        }
    }

    setHighlightCanvas(c, width, height) {
        if (c && c!=this.highlightCanvas) {
            this.highlightCanvas= c;
            //console.log(`Drawer ${this.drawerId}: redraw highlight- canvas update`);
            updateCanvasSize(width,height,c);
            this.updateDataHighlightLayer(this.highlightData);
        }
    }

    setSelectCanvas(c, width, height) {
        if (c && c!=this.selectCanvas) {
            this.selectCanvas= c;
            //console.log(`Drawer ${this.drawerId}: redraw select- canvas update`);
            updateCanvasSize(width,height,c);
            this.updateDataSelectLayer(this.data,this.selectedIdxAry);
        }
    }

    dataUpdated(width,height) {
        if (!this.primaryCanvas) return;
        this.cancelRedraw();
        updateCanvasSize(width,height,this.primaryCanvas,this.selectCanvas,this.highlightCanvas);
        if (this.data && this.data.length>0) {
            this.redraw();
        }
        else {
            this.clear();
        }
    }

    updateDataSelectLayer(data, selectedIdxAry) {
        this.data = data;
        this.selectedIdxAry= selectedIdxAry;
        var {selectLayerCanvas}= this;
        var sCtx= selectLayerCanvas? selectLayerCanvas.getContext('2d') : null;

        if (sCtx) {
            this.redrawSelected(sCtx, this.plot, this.data, selectedIdxAry,
                selectLayerCanvas.width, selectLayerCanvas.height);
        }
    }

    updateDataHighlightLayer(highlightData) {
        var {highlightLayerCanvas}= this;
        this.highlightData=highlightData;
        var hCtx= highlightLayerCanvas? highlightLayerCanvas.getContext('2d') : null;
        if (hCtx)  {
            this.redrawHighlight(hCtx, this.plot, highlightData,
                highlightLayerCanvas.width,highlightLayerCanvas.height);
        }
    }



    clearSelectLayer() { DrawUtil.clearCanvas(this.selectCanvas); }

    clearHighlightLayer() { DrawUtil.clearCanvas(this.highlightCanvas); }

    clear() {
        this.cancelRedraw();
        var {primaryCanvas,selectLayerCanvas,highlightLayerCanvas}= this;

        DrawUtil.clearCanvas(primaryCanvas);
        DrawUtil.clearCanvas(selectLayerCanvas);
        DrawUtil.clearCanvas(highlightLayerCanvas);
        this.removeTask();
    }


    redraw() {
        var {primaryCanvas,selectLayerCanvas,highlightLayerCanvas}= this;
        if (!primaryCanvas) return;

        var pCtx= primaryCanvas ? primaryCanvas.getContext('2d') : null;
        var sCtx= selectLayerCanvas? selectLayerCanvas.getContext('2d') : null;
        var hCtx= highlightLayerCanvas? highlightLayerCanvas.getContext('2d') : null;

        var w= primaryCanvas.width;
        var h= primaryCanvas.height;

        this.redrawPrimary(primaryCanvas, this.plot, pCtx, this.data, this.drawingDef, w,h);
        this.redrawHighlight(hCtx, this.plot, this.highlightData, this.drawingDef, w,h);
        this.redrawSelected(sCtx, this.plot, this.data, this.selectedIdxAry, w, h);
    }


    redrawSelected(ctx, plot, data, selectedIdxAry,w,h) {
        if (!ctx) return;
        var selectedData= [];
        DrawUtil.clear(ctx,w,h);
        if (canDraw(ctx,data)) {
            var cc= plot ? null : CsysConverter.make(plot);
            var vpPtM= makeViewPortPt(0,0);
            selectedData= this.decimateData(data.filter( (pt) => pt.isSelected() ),null,false);
            selectedData.forEach( (pt) => drawObj(ctx, null, this.drawingDef, cc, pt, vpPtM, false));
        }
    }

    redrawHighlight(ctx, plot, highlightData,drawingDef,w,h) {
        if (!ctx) return;
        DrawUtil.clear(ctx,w,h);
        if (!highlightData || !highlightData.length) return;

        if (canDraw(ctx,highlightData)) {
            var cc= plot ? CsysConverter.make(plot) : null;
            var vpPtM= makeViewPortPt(0,0);
            highlightData.forEach( (pt) => drawObj(ctx, null, drawingDef, cc, pt, vpPtM, false) );
        }
    }



    redrawPrimary(canvas, plot, ctx, data, drawingDef,w,h) {
        if (!ctx) return;
        this.clear();
        var params;
        if (canDraw(ctx,data)) {
            var cc= CsysConverter.make(plot);
            var drawData= this.decimateData(data, true);
            this.drawTextAry= [];
            if (drawData.length>500) {
                params= makeDrawingParams(canvas, this.drawTextAry, drawingDef,cc,drawData,
                                         this.drawConnect, getMaxChunk(drawData,this.isPointData));
                this.cancelRedraw();
                this.drawingCanceler= makeDrawingDeferred(this,params);
                this.removeTask();
                if (drawData.length>15000) this.addTask();
            }
            else {
                params= makeDrawingParams(canvas, this.drawTextAry, drawingDef,
                                          cc,drawData,this.drawConnect, Number.MAX_SAFE_INTEGER);
                this.doDrawing(params);
                if (this.textUpdateCallback) this.textUpdateCallback(this.drawTextAry);
            }
        }
        else {
            this.removeTask();
        }
    }


    decimateData(inData, useColormap) {
        if (this.decimate && inData.length>150) {
            this.decimatedData= this.doDecimateData(inData,this.decimatedData, useColormap);
            return this.decimatedData;
        }
        else {
            return inData;
        }
    }

    doDecimateData(inData, oldDecimatedData, useColormap) {
        var retData= inData;
        var plot= this.plot;
        if (this.decimate && inData.length>150 ) {
            var dim = plot.viewPort.dim;
            var spt= CCUtil.getScreenCoords(plot,makeViewPortPt(0,0));
            var defCol= this.drawingDef.color;
            if (!oldDecimatedData ||
                dim.width!==this.decimateDim.width ||
                dim.height!==this.decimateDim.height ||
                    defCol!==this.lastDecimationColor ||
                    !pointEquals(spt,this.lastDecimationPt))  {
                retData= doDecimation(inData, plot, useColormap);
                this.lastDecimationColor= defCol;
                this.lastDecimationPt=spt;
                this.decimateDim= dim;
            }
            else if (this.decimatedData) {
                retData= this.decimatedData;
            }
        }
        return retData;
    }



    doDrawing(params) {
        if (params.begin) {
            params.begin= false;
            params.canvas.style.visibility= 'hidden';
            params.done= false;
        }
        else {
            params.deferCnt++;
        }


        if (!params.done) {
            if (params.drawConnect) params.drawConnect.beginDrawing();
            var nextChunk= getNextChuck(params);
            if (nextChunk.optimize) {
                drawChunkOptimized(nextChunk.drawList, params, params.ctx);
                params.opCnt++;
            }
            else {
                drawChunkNormal(nextChunk.drawList, params, params.ctx);
            }
            if (params.next.done) { //loop finished
                params.done= true;
                this.removeTask();
            }
        }

        if (params.done ) {
            params.canvas.style.visibility= 'visible';
            if (params.drawConnect) {
                params.drawConnect.endDrawing();
            }
//            if (useBuffer) {
//                ((AdvancedGraphics)params._graphics).copyAsImage((AdvancedGraphics)params.drawBuffer);
//            }
        }

    }


    removeTask() {
        var {plot,plotTaskId}= this;
        if (plot && plotTaskId) {
            setTimeout( () => AppDataCntlr.dispatchRemoveTaskCount(plot.plotId,plotTaskId) ,0);
            this.plotTaskId= null;
        }
    }

    addTask() {
        var {plot}= this;
        if (plot) {
            var plotTaskId= AppDataCntlr.makeTaskId();
            setTimeout( () => AppDataCntlr.dispatchAddTaskCount(plot.plotId,plotTaskId) ,0);
            this.plotTaskId= plotTaskId;
        }
    }




    static makeDrawer(drawingDef) {
        return new Drawer(drawingDef);
    }

}



//=======================================================================
//------------------ private static functions
//------------------ private static functions
//------------------ private static functions
//------------------ private static functions
//------------------ private static functions
//=======================================================================


function nextPt(i,fuzzLevel, max) {
    i= Math.trunc(i);
    var remainder= i%fuzzLevel;
    var retval= (remainder===0) ? i : i+(fuzzLevel-remainder);
    if (retval===max) retval= max-1;
    return retval;
}

function makeDrawingParams(canvas, drawTextAry, drawingDef, csysConv, data, drawConnect, maxChunk) {
    var params= {
        canvas,    //const
        ctx : canvas.getContext('2d'),    //const
        drawTextAry,
        drawingDef,    //const
        csysConv,    //const
        data,    //const
        maxChunk,    //const
        drawConnect,    //const
        iterator : data[Symbol.iterator](),
        startTime: Date.now(),    //const
        opCnt: 0, //only for debug
        done : false,
        begin : true,
        deferCnt: 0,
        vpPtM : makeViewPortPt(0,0) //const
    };
    params.next= params.iterator.next();
    return params;
}

/**
 *
 * @param {{x:number,y:number,type:string}} pt
 * @param {{x:number,y:number,type:string}}mVpPt
 * @param {CysConverter} cc
 * @return {*}
 */
function getViewPortCoords(pt, mVpPt, cc) {
    var retval;
    if (pt.type===Point.W_PT) {
        var success= cc.getViewPortCoordsOptimize(pt,mVpPt);
        retval= success ? mVpPt : null;
    }
    else {
        retval= cc.getViewPortCoords(pt);
    }
    return retval;
}



function makeDrawingDeferred(drawer,params) {
    var id= window.setInterval( () => {
        if (params.done) {
            window.clearInterval(id);
            if (drawer.textUpdateCallback) drawer.textUpdateCallback(drawer.drawTextAry);
        }
        drawer.doDrawing(params);
    },0);
    return () => window.clearInterval(id);
}

/**
 *
 * @param ctx canvas context object
 * @param drawTextAry
 * @param def DrawingDef
 * @param csysConv web csysConv
 * @param obj DrawObj
 * @param vpPtM mutable viewport point
 * @param {boolean} onlyAddToPath
 */
function drawObj(ctx, drawTextAry, def, csysConv, obj, vpPtM, onlyAddToPath) {
    DrawOp.draw(obj, ctx, drawTextAry, csysConv, def, vpPtM,onlyAddToPath);
}

/**
 * An optimization of drawing.  Check is the Object is a PointDataObj (most common and simple) and then checks
 * it is draw on a WebPlot, and if it is in the drawing area.
 * Otherwise it will always return true
 * @param obj the DrawingObj to check
 * @param csysConv the WebPlot to draw on
 * @return {boolean} true is it should be drawn
 */
function shouldDrawObj(csysConv, obj) {
    var retval= true;
    if (obj && csysConv && obj.pt && obj.type===POINT_DATA_OBJ) {
        if (obj.pt.type === Point.W_PT) retval= csysConv.pointInViewPort(obj.pt);
    }
    return retval;
}

function canDraw(ctx,data) {
    return (ctx && data && data.length);
}

/**
 *
 * @param ctx canvas object
 * @param def DrawingDef
 * @param csysConv web csysConv
 * @param dc drawConnector
 * @param obj DrawObj
 * @param lastObj DrawObj
 */
function drawConnector(ctx, def, csysConv, dc, obj, lastObj) {
    if (!obj && !lastObj) return;
    if (csysConv) {
        var wp1= csysConv.getWorldCoords(DrawOp.getCenterPt(lastObj));
        var wp2= csysConv.getWorldCoords(DrawOp.getCenterPt(obj));
        if (!csysConv.coordsWrap(wp1,wp2)) {
            dc.draw(ctx,csysConv,def, wp1,wp2);
        }
    }
    else {
        if (DrawOp.getCenterPt(lastObj).type===Point.SPT && DrawOp.getCenterPt(obj).type===Point.SPT) {
            dc.draw(ctx,def, DrawOp.getCenterPt(lastObj), DrawOp.getCenterPt(obj));
        }
    }
}

function drawChunkOptimized(drawList, params, ctx) {
    if (!drawList.length) return;
    DrawUtil.beginPath(ctx,params.drawingDef.color,1);
    for(var obj of drawList) {
        drawObj(ctx, params.drawTextAry, params.drawingDef, params.csysConv, obj,params.vpPtM, true);
    }
    DrawUtil.stroke(ctx);
}

function drawChunkNormal(drawList, params, ctx) {
    var lastObj= null;
    var {drawingDef,drawConnect,csysConv,vpPtM}= params;
    for(var obj of drawList) {
        if (drawConnect) { // in this case doDraw was already called
            drawObj(ctx, params.drawTextAry, drawingDef, csysConv, obj,vpPtM, false);
        }
        else  {
            if (shouldDrawObj(csysConv,obj)) { // doDraw must be call when there is a connector
                drawObj(ctx, params.drawTextAry, drawingDef, csysConv, obj,vpPtM, false);
                if (drawConnect) {
                    drawConnector(ctx,drawingDef,csysConv,drawConnect,obj,lastObj);
                }
            }
            lastObj= obj;
        }
    }
}


function getNextChuck(params) {
    var drawList= [];
    var optimize= params.drawConnect?false:true;
    var objLineWidth;
    var objColor;
    var {drawingDef}= params;
    var i;


    var obj= params.next.value;
    var color= drawingDef.color;
    var lineWidth=  obj.lineWidth || drawingDef.lineWidth || 1;

    for(i= 0; (!params.next.done && i<params.maxChunk ); ) {
        obj= params.next.value;
        params.next= params.iterator.next();
        if (!params.drawConnect) {
            if (shouldDrawObj(params.csysConv, obj)) {
                drawList.push(obj);
                if (optimize) {
                    objLineWidth= obj.lineWidth || lineWidth;
                    objColor= obj.color || color;
                    optimize= (DrawOp.usePathOptimization(obj,drawingDef) &&
                               lineWidth===objLineWidth &&
                               color===objColor);
                }
                i++;
            }
        }
        else {
            drawList.push(obj);
            i++;
        }
    }
    return {drawList,optimize};
}




function getMaxChunk(drawData,isPointData) {
    var maxChunk= 1;
    if (!drawData.length) return maxChunk;
    if (isPointData) {
        maxChunk= BrowserInfo.isBrowser(Browser.SAFARI) || BrowserInfo.isBrowser(Browser.CHROME) ? 2000 : 500;
    }
    else {
        maxChunk= BrowserInfo.isBrowser(Browser.SAFARI) || BrowserInfo.isBrowser(Browser.CHROME) ? 1000 : 200;
    }
    return maxChunk;
}


function updateCanvasSize(w,h,...cAry) {
    cAry.forEach( (c) => {
        if (!c) return;
        c.width= w;
        c.height= h;
    });
}

function makeColorMap(mapSize,color) {
    return Color.makeSimpleColorMap(color,mapSize,true);
}


function setupColorMap(data, maxEntry) {
    var colorMap= makeColorMap(maxEntry);
    if (colorMap)  {
        var cnt;
        var obj= null;
        var idx;
        if (maxEntry>colorMap.length) {
            var maxCnt = maxEntry+1; // to include draw obj with cnt==maxEntry into the last color band
            for(obj of data) {
                cnt= obj.representCnt || 1;
                idx = cnt*colorMap.length/maxCnt;
                obj.color=colorMap[idx];
            }
        }  else {
            for(obj of data) {
                cnt= obj.representCnt || 1;
                //if (cnt>colorMap.length) cnt=colorMap.length;
                obj.color=colorMap[cnt-1];
            }
        }
    }
}

function doDecimation(inData, plot, useColormap) {
    var i,j;
    var dim = plot.viewPort.dim;

    var supportCmap= useColormap && ENABLE_COLORMAP;

    //var drawArea= dim.width*dim.height;
    //var percentCov= inData.length/drawArea;

    var fuzzLevel= 5;
    //var start = Date.now();

    var {width,height}= dim;

    var decimateObs= new Array(width);
    for(i=0; (i<decimateObs.length);i++) decimateObs[i]= new Array(height);

    var seedPt= makeViewPortPt(0,0);
    var vpPt;
    var pt;
    var maxEntry= -1;
    var entryCnt;

//        GwtUtil.getClientLogger().log(Level.INFO,"doDecimation: " + (enterCnt++) + ",data.size= "+ _data.size() +
//                ",drawID="+drawerID+
//                ",data="+Integer.toHexString(_data.hashCode()));

    var cc= CsysConverter.make(plot);
    var first200= [];
    var decimatedAddCnt= 0;
    var totalInViewPortCnt= 0;

    for(var obj of inData) {
        if (obj) {
            pt= DrawOp.getCenterPt(obj);
            if (pt.type==Point.W_PT) {
                vpPt= cc.pointInPlotRoughGuess(pt) ? getViewPortCoords(pt,seedPt,cc) : null;
            }
            else {
                vpPt= getViewPortCoords(pt,seedPt,cc);
            }

        }
        else {
            vpPt= null;
        }
        if (vpPt) {
            i= nextPt(vpPt.x,fuzzLevel,width);
            j= nextPt(vpPt.y, fuzzLevel,height);
            if (i>=0 && j>=0 && i<width && j<height) {
                if (!decimateObs[i][j]) {
                    decimateObs[i][j]= supportCmap ? Object.assign({},obj) : obj;
                    if (supportCmap) {
                        decimateObs[i][j].representCnt= obj.representCnt;
                        entryCnt= decimateObs[i][j].representCnt;
                        if (entryCnt>maxEntry) maxEntry= entryCnt;
                    }
                    decimatedAddCnt++;
                }
                else {
                    if (supportCmap) {
                        decimateObs[i][j].representCnt+=(obj.representCnt||1);
                        entryCnt= decimateObs[i][j].representCnt;
                        if (entryCnt>maxEntry) maxEntry= entryCnt;
                    }
                }
                if (totalInViewPortCnt<200) first200.push(obj);
                totalInViewPortCnt++;
            }
        }
    }

    var retData;
    if (totalInViewPortCnt<200) {
        retData= first200;
    }
    else {
        retData= [];
        for(i= 0; (i<decimateObs.length); i++) {
            for(j= 0; (j<decimateObs[i].length); j++) {
                if (decimateObs[i][j]) retData.push(decimateObs[i][j]);
            }
        }
    }



    if (supportCmap) setupColorMap(retData,maxEntry);

    return retData;
}

export default Drawer;
