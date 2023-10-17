/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get, isFunction, isEmpty, isArray} from 'lodash';
import Point, {makeScreenPt,makeImagePt,pointEquals} from '../Point.js';
import {dispatchAddTaskCount, dispatchRemoveTaskCount, makeTaskId, getTaskCount} from '../../core/AppDataCntlr.js';
import BrowserInfo, {Browser} from '../../util/BrowserInfo.js';
import {Style} from './DrawingDef.js';
import DrawUtil from './DrawUtil.js';
import Color, {getRGBA, toRGBAString} from '../../util/Color.js';
import CsysConverter, {CCUtil} from '../CsysConverter.js';
import {POINT_DATA_OBJ} from './PointDataObj.js';
import {DrawingType} from './DrawObj.js';
import DrawOp from './DrawOp.js';
import {isHiPS, isImage} from '../WebPlot';
import {hasWCSProjection} from '../PlotViewUtil';


const ENABLE_COLORMAP= false;
let drawerCnt=0;

export class Drawer {


    constructor() {
        this.drawerID;
        this.drawingDef= null;
        this.data;
        this.highlightData;
        this.selectedIndexes=[];

        this.plot= null;
        this.primaryCanvas= null;
        this.selectCanvas= null;
        this.highlightCanvas= null;
        this.drawingCanceler= null;
        this.plotTaskId= makeTaskId('drawer');
        this.isPointData= false;
        this.decimate= false;

        this.decimatedData= null;
        this.decimateDim= null;
        this.lastDecimationPt= null;
        this.lastDecimationColor= null;
        //this.highPriorityLayer= false;
        this.drawerId= drawerCnt++; // only used for debugging
        this.deferredDrawingCompletedCB= null;
    }


    dispose() {
        this.primaryCanvas= null;
        this.selectCanvas= null;
        this.highlightCanvas= null;
        this.data= null;
        this.decimatedData= null;
    }


    setEnableDecimationDrawing(d) { this.decimate= d; } // future use maybe

    cancelRedraw() {
        if (this.drawingCanceler) {
            this.drawingCanceler();
            DrawUtil.clearCanvas(this.primaryCanvas);
            this.drawingCanceler= null;
        }
    }


    /**
     *
     * @param {Array} data the list of DataObj
     * @param {number[]} selectedIndexes
     * @param {WebPlot} plot
     * @param {number} width
     * @param {number} height
     * @param {DrawingDef} drawingDef
     * @param {boolean} forceUpdate
     */
    setData(data,selectedIndexes,plot,width,height,drawingDef,forceUpdate= false) {
        if (data && !Array.isArray(data)) data= [data];
        let cWidth, cHeight, dWidth, oldDWidth, dHeight;
        let oldDHeight, zfact, oldZfact, oldTestPtStr, testPtStr, pt;
        let oldProjection, newProjection;

        width= Math.floor(width);
        height= Math.floor(height);
        if (this.primaryCanvas) {
            cWidth= this.primaryCanvas.width;
            cHeight= this.primaryCanvas.height;
        }

        if (plot) {
            dWidth= plot.dataWidth;
            dHeight= plot.dataHeight;
            newProjection= plot.projection;


            zfact= Math.round(plot.zoomFactor*100000)/100000;
            pt= CCUtil.getWorldCoords(plot,makeImagePt(1,1));
            testPtStr= pt ? pt.toString() : '';
        }

        if (this.plot) {
            oldDWidth= this.plot.dataWidth;
            oldDHeight= this.plot.dataHeight;
            oldProjection= this.plot.projection;
            oldZfact= Math.round(this.plot.zoomFactor*100000)/100000;
            pt= CCUtil.getWorldCoords(this.plot,makeImagePt(1,1));
            oldTestPtStr= pt ? pt.toString() : '';
        }

        let viewUpdated= true;

        if (drawingDef===this.drawingDef && oldProjection===newProjection &&
            cWidth===width && cHeight===height &&
            dWidth===oldDWidth && dHeight===oldDHeight  &&
            zfact===oldZfact  && testPtStr===oldTestPtStr ) {
            viewUpdated= false;
        }


        const primaryUpdated= (data && data!==this.data) || viewUpdated;

        const selectedUpdated= (selectedIndexes!==this.selectedIndexes) || viewUpdated;


        if (data!==this.data || this.plot!==plot) {
            this.decimatedData= null;
        }

        if (!primaryUpdated && !selectedUpdated  && !forceUpdate) return;

        this.plot= plot;
        this.data= data;
        this.selectedIndexes = selectedIndexes;
        this.drawingDef = drawingDef;





        // ======== DEBUG =============================
        // var changes= [this.primaryCanvas ? '' : 'no canvas'];
        // var changes= [];
        // if (data!==this.data) changes.push('data');
        // //if (plot!==this.plot) changes.push('plot');
        // if (forceUpdate) changes.push('force update');
        // if (viewUpdated) changes.push('view update');
        // if (cWidth!==width ) changes.push(`width: ${width}, ${cWidth}`);
        // if (cHeight!==height ) changes.push(`height: ${height}, ${cHeight}`);
        // if (dWidth!==oldDWidth ) changes.push(`data width: ${oldDWidth}, ${dWidth}`);
        // if (dHeight!==oldDHeight ) changes.push(`data height: ${oldDHeight}, ${dHeight}`);
        // if (zfact!==oldZfact ) changes.push(`zoom factor ${oldZfact}, ${zfact}`);
        // if (testPtStr!==oldTestPtStr ) changes.push('test pt');
        // // if (oldvpY!==vpY ) changes.push(`vpY: ${oldvpY}, ${vpY}`);
        // if (drawingDef!==this.drawingDef ) changes.push('drawingDef');
        // var changeStr= changes.join();
        // if (true && primaryUpdated) console.log(`Drawer ${this.drawerId}: redraw- changes: ${changeStr}`);
        // =====================================





        if (primaryUpdated || forceUpdate) {
            if (zfact!==oldZfact) {
                this.clear();
            }
            const asyncId= this.dataUpdated(width,height);
            return asyncId;
        }

        if (selectedUpdated || forceUpdate) {
            this.updateDataSelectLayer();
        }




    }



    setPrimCanvas(c, width, height) {
        if (c && c!==this.primaryCanvas) {
            this.primaryCanvas= c;
            //console.log(`Drawer ${this.drawerId}: redraw primary- canvas update`);
            this.dataUpdated(width,height);
        }
    }

    setHighlightCanvas(c, width, height) {
        if (c && c!==this.highlightCanvas) {
            this.highlightCanvas= c;
            //console.log(`Drawer ${this.drawerId}: redraw highlight- canvas update`);
            updateCanvasSize(width,height,c);
            this.updateDataHighlightLayer(this.highlightData);
        }
    }

    setSelectCanvas(c, width, height) {
        if (c && c!==this.selectCanvas) {
            this.selectCanvas= c;
            //console.log(`Drawer ${this.drawerId}: redraw select- canvas update`);
            updateCanvasSize(width,height,c);
            this.updateDataSelectLayer();
        }
    }

    dataUpdated(width,height) {
        if (!this.primaryCanvas) return;
        this.cancelRedraw();
        updateCanvasSize(width,height,this.primaryCanvas,this.selectCanvas,this.highlightCanvas);
        if (this.data && this.data.length>0) {
            const asyncId= this.redraw();
            return asyncId;
        }
        else {
            this.clear();
        }
    }

    updateDataSelectLayer() {
        const {plot,selectCanvas,selectedIndexes,data}= this;
        const cc= CsysConverter.make(plot);
        this.redrawSelected(selectCanvas, cc, data, selectedIndexes);
    }

    /**
     *
     * @param highlightData
     */
    updateDataHighlightLayer(highlightData) {
        const {highlightCanvas, drawingDef}= this;
        this.highlightData=highlightData;
        if (!highlightCanvas) return;
        const cc= CsysConverter.make(this.plot);
        this.redrawHighlight(highlightCanvas, cc, highlightData,drawingDef);
    }


    clear() {
        this.cancelRedraw();
        const {primaryCanvas,selectCanvas,highlightCanvas}= this;

        DrawUtil.clearCanvas(primaryCanvas);
        DrawUtil.clearCanvas(selectCanvas);
        DrawUtil.clearCanvas(highlightCanvas);
        this.removeTask();
    }


    redraw() {
        const {primaryCanvas,selectCanvas,highlightCanvas}= this;
        if (!primaryCanvas) return;
        const cc= CsysConverter.make(this.plot);
        const asyncId= this.redrawPrimary(primaryCanvas, cc, this.data, this.drawingDef);
        this.redrawHighlight(highlightCanvas, cc, this.highlightData, this.drawingDef);
        this.redrawSelected(selectCanvas, cc, this.data, this.selectedIndexes);
        return asyncId;
    }


    /**
     *
     * @param selectCanvas
     * @param cc
     * @param data
     * @param {Array.<Number> | Function} selectedIndexes
     */
    redrawSelected(selectCanvas, cc, data, selectedIndexes) {
        if (!selectCanvas) return;
        const ctx= selectCanvas.getContext('2d');
        DrawUtil.clear(ctx,selectCanvas.width,selectCanvas.height);
        if (isEmpty(data) || !selectedIndexes) return;
        let selectedData;
        if (isArray(selectedIndexes)) {
            if (isEmpty(selectedIndexes)) return;
            selectedData= this.decimateData(this.decimate,
                selectedIndexes.map( (dataIdx)=> data[dataIdx]), cc,false,null);
        }
        else if (isFunction(selectedIndexes)) {
            selectedData= this.decimateData(this.decimate,
                data.filter( (d,idx)=> selectedIndexes(idx)), cc,false,null);
        }
        else {
            return;
        }
        const selDrawDef= Object.assign({}, this.drawingDef, {color:this.drawingDef.selectedColor, drawMode: 'select'});
        this.doDrawing(makeDrawingParams(selectCanvas, selDrawDef, cc,selectedData));
    }

    /**
     * 
     * @param highlightCanvas
     * @param cc
     * @param highlightData
     * @param drawingDef
     */
    redrawHighlight(highlightCanvas, cc, highlightData,drawingDef) {
        if (!highlightCanvas) return;
        const ctx= highlightCanvas.getContext('2d');
        DrawUtil.clear(ctx,highlightCanvas.width,highlightCanvas.height);
        if (isEmpty(highlightData)) return;
        const sPtM= makeScreenPt(0,0);
        highlightData.forEach( (pt) => drawObj(ctx, drawingDef, cc, pt, sPtM, false) );

    }



    redrawPrimary(canvas, cc, data, drawingDef) {
        if (!canvas) return;
        if (!isEmpty(data)) {
            let params;
            this.decimatedData= this.decimateData(this.decimate, data, cc,true,this.decimatedData);
            const drawData= this.decimatedData;
            if (drawData.length>500) {
                const offscreenCanvas= initOffScreenCanvas(canvas.width, canvas.height);
                params= makeDrawingParams(offscreenCanvas, drawingDef,cc,drawData,
                                         getMaxChunk(drawData,this.isPointData),
                                         this.deferredDrawingCompletedCB, canvas);
                this.cancelRedraw();
                this.drawingCanceler= makeDrawingDeferred(this,params);
                this.removeTask();
                if (drawData.length>15000) this.addTask();
                return params.id;
            }
            else {
                this.clear();
                params= makeDrawingParams(canvas, drawingDef,
                                          cc,drawData,Number.MAX_SAFE_INTEGER);
                this.doDrawing(params);
            }
        }
        else {
            this.removeTask();
        }
    }


    //decimateData(decimate, inData, cc, useColormap, oldDecimatedData) {
    //    if (decimate && inData.length>150) {
    //        return this.doDecimateData(inData,oldDecimatedData,cc,useColormap);
    //    }
    //    else {
    //        return inData;
    //    }
    //}

    /**
     *
     * @param decimate
     * @param inData
     * @param cc
     * @param useColormap
     * @param oldDecimatedData
     * @return {*}
     */
    decimateData(decimate, inData, cc, useColormap, oldDecimatedData) {
        if (!decimate || inData.length<=150) return inData;

        let retData= inData;
        const dim = cc.viewDim;
        const spt= cc.getScreenCoords(makeScreenPt(0,0));
        const defCol= this.drawingDef.color;
        if (!oldDecimatedData ||
            dim.width!==this.decimateDim.width ||
            dim.height!==this.decimateDim.height ||
            defCol!==this.lastDecimationColor ||
            !pointEquals(spt,this.lastDecimationPt))  {
            retData= doDecimation(inData, cc, useColormap);
            this.lastDecimationColor= defCol;
            this.lastDecimationPt=spt;
            this.decimateDim= dim;
        }
        else if (oldDecimatedData) {
            retData= oldDecimatedData;
        }
        return retData;
    }



    doDrawing(params) {
        if (params.begin) {
            params.begin= false;
            if (!params.copyToCanvas) {
                params.canvas.style.visibility= 'hidden';
            }
            params.done= false;
        }
        else {
            params.deferCnt++;
        }


        if (!params.done) {
            const nextChunk= getNextChuck(params);
            if (nextChunk.optimize) {
                drawChunkOptimized(nextChunk.drawList, params);
                params.opCnt++;
            }
            else {
                drawChunkNormal(nextChunk.drawList, params);
            }
            if (params.next.done) { //loop finished
                params.done= true;
                this.removeTask();
            }
        }

        if (params.done ) {
            this.drawingCanceler= null;
            if (params.copyToCanvas) {
                DrawUtil.clearCanvas(params.copyToCanvas);
                params.copyToCanvas.getContext('2d').drawImage(params.canvas, 0,0);
                // do the copy
            }
            else {
                params.canvas.style.visibility= 'visible';
            }
            if (params.deferCnt && isFunction(params.deferredDrawingCompletedCB)) params.deferredDrawingCompletedCB();
        }

    }


    removeTask() {
        const {plot,plotTaskId}= this;
        if (plot && plotTaskId) {
            setTimeout( () => getTaskCount(plot.plotId) && dispatchRemoveTaskCount(plot.plotId,plotTaskId) ,0);
        }
    }

    addTask() {
        const {plot}= this;
        if (plot) {
            setTimeout( () => dispatchAddTaskCount(plot.plotId,this.plotTaskId) ,0);
        }
    }




    static makeDrawer() {
        return new Drawer();
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
    const remainder= i%fuzzLevel;
    let retval= (remainder===0) ? i : i+(fuzzLevel-remainder);
    if (retval===max) retval= max-1;
    return retval;
}

/**
 *
 * @param canvas
 * @param drawingDef
 * @param {CsysConverter} csysConv
 * @param data
 * @param {number} maxChunk
 * @param {Function} deferredDrawingCompletedCB - called when drawing has completed
 * @param copyToCanvas
 * @return {Object}
 */
function makeDrawingParams(canvas, drawingDef, csysConv, data,
                           maxChunk= Number.MAX_SAFE_INTEGER,
                           deferredDrawingCompletedCB=null, copyToCanvas) {

    const params= {
        canvas,    //const
        drawingDef,    //const
        csysConv,    //const
        data,    //const
        maxChunk,    //const
        iterator : data[Symbol.iterator](),
        startTime: Date.now(),    //const
        opCnt: 0, //only for debug
        done : false,
        begin : true,
        deferCnt: 0,
        vpPtM : makeScreenPt(0,0), //const
        deferredDrawingCompletedCB, //const
        copyToCanvas
    };
    params.next= params.iterator.next();
    return params;
}

function initOffScreenCanvas(width, height) {
    const offscreenCanvas = document.createElement('canvas');
    offscreenCanvas.width = width;
    offscreenCanvas.height =height;
    return offscreenCanvas;
}


/**
 *
 * @param {{x:number,y:number,type:string}} pt
 * @param {{x:number,y:number,type:string}}mSpPt
 * @param {CysConverter} cc
 * @return {*}
 */
function getScreenCoords(pt, mSpPt, cc) {
    if (pt.type===Point.W_PT) {
        const success= cc.getScreenCoordsOptimize(pt,mSpPt);
        return success ? mSpPt : null;
    }
    else {
        return cc.getScreenCoords(pt);
    }
}



function makeDrawingDeferred(drawer,params) {
    const id= window.setInterval( () => {
        if (params.done) {
            window.clearInterval(id);
        }
        drawer.doDrawing(params);
    },0);
    params.id= id;
    return () => window.clearInterval(id);

}

/**
 *
 * @param ctx canvas context object
 * @param def DrawingDef
 * @param csysConv web csysConv
 * @param obj DrawObj
 * @param vpPtM mutable viewport point
 * @param {boolean} onlyAddToPath
 */
function drawObj(ctx, def, csysConv, obj, vpPtM, onlyAddToPath) {
    DrawOp.draw(obj, ctx, csysConv, def, vpPtM,onlyAddToPath);
}

/**
 * An optimization of drawing.  Check is the Object is a PointDataObj (most common and simple) and then checks
 * it is draw on a WebPlot, and if it is in the drawing area.
 * Otherwise it will always return true
 * @param {CysConverter} csysConv the WebPlot to draw on
 * @param {DrawObj} obj the DrawingObj to check
 * @return {boolean} true is it should be drawn
 */
function shouldDrawObj(csysConv, obj) {
    if (!obj) return false;
    if (obj.supportedDrawingTypes === DrawingType.WcsCoordsOnly) {
        if (!csysConv) return false;
        else if (isImage(csysConv) && !hasWCSProjection(csysConv)) return false;
    }
    else if (obj.supportedDrawingTypes===DrawingType.ImageCoordsOnly && isHiPS(csysConv)) {
        return false;
    }

    if (csysConv && obj.pt && obj.type===POINT_DATA_OBJ) {
        return isImage(csysConv) ? csysConv.pointOnDisplay(obj.pt) : csysConv.pointInView(obj.pt);
    }
    return true;
}

function drawChunkOptimized(drawList, params) {
    if (!drawList.length) return;
    const ctx=params.canvas.getContext('2d');

    let drawDefForFinalDraw= params.drawingDef;

    if (drawList[0].style===Style.DESTINATION_OUTLINE) {

        const newDL= drawList.map( (d) => ({...d,style:Style.STANDARD}));
        DrawUtil.beginPath(ctx,params.drawingDef.color,3);
        for(const obj of newDL) {
            drawObj(ctx, params.drawingDef, params.csysConv, obj,params.vpPtM, true);
        }
        DrawUtil.stroke(ctx);

        ctx.globalCompositeOperation='destination-out';
        DrawUtil.beginPath(ctx,params.drawingDef.color,1);
        for(const obj of newDL) {
            drawObj(ctx, params.drawingDef, params.csysConv, obj,params.vpPtM, true);
        }
        ctx.stroke();

        const rgba= getRGBA(params.drawingDef.color);
        if (rgba[3]<1) rgba[3]=1;
        drawDefForFinalDraw= {...params.drawingDef, color: toRGBAString(rgba)};
    }

    DrawUtil.beginPath(ctx,params.drawingDef.color,params.drawingDef.lineWidth);
    for(const obj of drawList) {
        drawObj(ctx, drawDefForFinalDraw, params.csysConv, obj,params.vpPtM, true);
    }
    DrawUtil.stroke(ctx);
    ctx.globalCompositeOperation='source-over';
}

function drawChunkNormal(drawList, params) {
    let lastObj= null;
    const {drawingDef,csysConv,vpPtM, canvas}= params;
    const ctx=canvas.getContext('2d');
    for(const obj of drawList) {
        if (shouldDrawObj(csysConv,obj)) { // doDraw must be call when there is a connector
            drawObj(ctx, drawingDef, csysConv, obj,vpPtM, false);
        }
        lastObj= obj;
    }
}


function getNextChuck(params) {
    const drawList= [];
    let optimize= true;
    let objLineWidth;
    let objColor;
    const {drawingDef}= params;
    let i;


    let obj= params.next.value;
    const color= drawingDef.color;
    const lineWidth=  get(obj,'lineWidth',false) || drawingDef.lineWidth || 1;

    for(i= 0; (!params.next.done && i<params.maxChunk ); ) {
        obj= params.next.value;
        params.next= params.iterator.next();
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
    return {drawList,optimize};
}




function getMaxChunk(drawData,isPointData) {
    let maxChunk= 1;
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
        if (c.width!==w) c.width= w;
        if (c.height!==h) c.height= h;
    });
}

function makeColorMap(mapSize,color) {
    return Color.makeSimpleColorMap(color,mapSize,true);
}


function setupColorMap(data, maxEntry) {
    const colorMap= makeColorMap(maxEntry);
    if (colorMap)  {
        let cnt;
        let obj= null;
        let idx;
        if (maxEntry>colorMap.length) {
            const maxCnt = maxEntry+1; // to include draw obj with cnt==maxEntry into the last color band
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

function doDecimation(inData, cc, useColormap) {
    let i,j;
    const dim = cc.viewDim;

    const supportCmap= useColormap && ENABLE_COLORMAP;

    //var drawArea= dim.width*dim.height;
    //var percentCov= inData.length/drawArea;

    const fuzzLevel= 5;
    //var start = Date.now();

    const {width,height}= dim;

    const decimateObs= new Array(width);
    for(i=0; (i<decimateObs.length);i++) decimateObs[i]= new Array(height);

    const seedPt= makeScreenPt(0,0);
    let sPt;
    let pt;
    let maxEntry= -1;
    let entryCnt;

//        GwtUtil.getClientLogger().log(Level.INFO,"doDecimation: " + (enterCnt++) + ",data.size= "+ _data.size() +
//                ",drawID="+drawerID+
//                ",data="+Integer.toHexString(_data.hashCode()));

    const first200= [];
    // let decimatedAddCnt= 0;
    let totalInViewPortCnt= 0;

    for(const obj of inData) {
        if (obj) {
            pt= DrawOp.getCenterPt(obj);
            if (pt.type===Point.W_PT) {
                sPt= cc.pointInPlotRoughGuess(pt) ? getScreenCoords(pt,seedPt,cc) : null;
            }
            else {
                sPt= getScreenCoords(pt,seedPt,cc);
            }

        }
        else {
            sPt= null;
        }
        if (sPt) {
            i= nextPt(sPt.x,fuzzLevel,width);
            j= nextPt(sPt.y, fuzzLevel,height);
            if (i>=0 && j>=0 && i<width && j<height) {
                if (!decimateObs[i][j]) {
                    decimateObs[i][j]= supportCmap ? Object.assign({},obj) : obj;
                    if (supportCmap) {
                        decimateObs[i][j].representCnt= obj.representCnt;
                        entryCnt= decimateObs[i][j].representCnt;
                        if (entryCnt>maxEntry) maxEntry= entryCnt;
                    }
                    // decimatedAddCnt++;
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

    let retData;
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

