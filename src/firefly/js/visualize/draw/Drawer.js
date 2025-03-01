/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isFunction, isEmpty, isArray} from 'lodash';
import {makeScreenPt,makeImagePt} from '../Point.js';
import {dispatchAddTaskCount, dispatchRemoveTaskCount, makeTaskId, getTaskCount} from '../../core/AppDataCntlr.js';
import BrowserInfo, {Browser} from '../../util/BrowserInfo.js';
import {Style} from './DrawingDef.js';
import DrawUtil from './DrawUtil.js';
import {getRGBA, toRGBAString} from '../../util/Color.js';
import CsysConverter, {CCUtil} from '../CsysConverter.js';
import {POINT_DATA_OBJ} from './PointDataObj.js';
import {DrawingType} from './DrawObj.js';
import DrawOp from './DrawOp.js';
import {isHiPS, isImage} from '../WebPlot';
import {hasWCSProjection} from '../PlotViewUtil';


let drawerCnt=0;

export class Drawer {

    constructor() {
        this.drawingDef= undefined;
        this.selectedIndexes=[];

        this.plot= undefined;
        this.primaryCanvas= undefined;
        this.selectCanvas= undefined;
        this.highlightCanvas= undefined;
        this.drawingCanceler= undefined;
        this.plotTaskId= makeTaskId('drawer');
        this.isPointData= false;
        this.drawerId= drawerCnt++; // only used for debugging
        this.deferredDrawingCompletedCB= undefined;
    }


    dispose() {
        this.primaryCanvas= undefined;
        this.selectCanvas= undefined;
        this.highlightCanvas= undefined;
        this.data= undefined;
    }

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
            selectedData= selectedIndexes.map( (dataIdx)=> data[dataIdx]);
        }
        else if (isFunction(selectedIndexes)) {
            selectedData= data.filter( (d,idx)=> selectedIndexes(idx));
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
            const drawData= data;
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


function makeDrawingDeferred(drawer,params) {
    // let i=0;
    const id= window.setInterval( () => {
        if (params.done) window.clearInterval(id);
        // console.time('drawing ' +i);
        drawer.doDrawing(params);
        // console.timeEnd('drawing ' +i);
        // i++;
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
    let funcAry;

    if (drawList[0].style===Style.DESTINATION_OUTLINE) {

        DrawUtil.beginPath(ctx,params.drawingDef.color,3);
        DrawUtil.recordDrawing();
        for(let i= 0; (i<drawList.length); i++) {
            drawObj(ctx, params.drawingDef, params.csysConv, {...drawList[i],style:Style.STANDARD}, params.vpPtM, true);
        }
        funcAry= DrawUtil.endRecordDrawing();
        DrawUtil.stroke(ctx);

        ctx.globalCompositeOperation='destination-out';
        DrawUtil.beginPath(ctx,params.drawingDef.color,1);
        funcAry.forEach( (f) => f());
        ctx.stroke();

        const rgba= getRGBA(params.drawingDef.color);
        if (rgba[3]<1) rgba[3]=1;
        drawDefForFinalDraw= {...params.drawingDef, color: toRGBAString(rgba)};
    }

    DrawUtil.beginPath(ctx,params.drawingDef.color,params.drawingDef.lineWidth);
    for(let i= 0; (i<drawList.length); i++) {
        drawObj(ctx, drawDefForFinalDraw, params.csysConv, drawList[i],params.vpPtM, true);
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
    const lineWidth=  obj?.lineWidth || drawingDef.lineWidth || 1;

    for(i= 0; (!params.next.done && i<params.maxChunk ); ) {
        obj= params.next.value;
        params.next= params.iterator.next();
        if (shouldDrawObj(params.csysConv, obj)) {
            drawList.push(obj);
            if (optimize) {
                objLineWidth= obj.lineWidth || lineWidth;
                objColor= obj.color || color;
                optimize= (DrawOp.usePathOptimization(obj,drawingDef) &&  //eslint-disable-line
                    lineWidth===objLineWidth &&
                    color===objColor);
            }
            i++;
        }
    }
    return {drawList,optimize};
}

function getMaxChunk(drawData,isPointData) {
    if (!drawData?.length) return 1;
    if (isPointData) {
        return BrowserInfo.isBrowser(Browser.SAFARI) || BrowserInfo.isBrowser(Browser.CHROME) ? 2000 : 500;
    }
    else {
        return BrowserInfo.isBrowser(Browser.SAFARI) || BrowserInfo.isBrowser(Browser.CHROME) ? 2000 : 500;
    }
}


function updateCanvasSize(w,h,...cAry) {
    cAry.forEach( (c) => {
        if (!c) return;
        if (c.width!==w) c.width= w;
        if (c.height!==h) c.height= h;
    });
}
