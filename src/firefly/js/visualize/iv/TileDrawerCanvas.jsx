/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PropTypes, Component} from 'react';
import CsysConverter from '../CsysConverter.js';
import sCompare from 'react-addons-shallow-compare';
import {SimpleCanvas}  from '../draw/SimpleCanvas.jsx';
import {makeDevicePt} from '../Point.js';
import {createImageUrl,isTileVisible} from './TileDrawHelper.jsx';
import {makeTransform} from '../PlotTransformUtils.js';
import {primePlot} from '../PlotViewUtil.js';
import {getBoundingBox} from '../VisUtil.js';


const BG_IMAGE= 'image-working-background-24x24.png';
const BACKGROUND_STYLE = `url(+ ${BG_IMAGE} ) top left repeat`;

const containerStyle={position:'absolute',
                      overflow:'hidden',
                      left: 0,
                      right: 0,
                      background: BACKGROUND_STYLE
};

// const debugTiles= true;

export class TileDrawerCanvas extends Component {


    constructor(props) {
        super(props);

        this.canvas= null;
        this.loadedImages= { serverData: null, images: [] };
        this.drawTilesForArea= this.drawTilesForArea.bind(this);


        this.drawInit= (canvas) => {
            this.canvas= canvas;
            this.drawTilesForArea();
        };

    }

    drawTilesForArea()  {
        const {canvas:targetCanvas}= this;
        if (!targetCanvas) return;

        const {plot, opacity,plotView}= this.props;
        const tileData = plot.serverImages;
        const scale = plot.zoomFactor/ plot.plotState.getZoomLevel();

        if (this.loadedImages.serverData!==tileData.images) {
            this.loadedImages= {
                serverData: tileData.images,
                images: [],
                used: []
            };
        }

        this.loadedImages.used= [];

        const {viewDim}= plotView;
        const rootPlot= primePlot(plotView); // bounding box should us main plot not overlay plot
        const boundingBox= computeBounding(rootPlot,viewDim.width,viewDim.height);


        const tilesToLoad= tileData.images.filter( (tile) =>
            isTileVisible(tile,boundingBox.x,boundingBox.y,boundingBox.w,boundingBox.h,scale));

        const offsetX= boundingBox.x>0 ? boundingBox.x : 0;
        const offsetY= boundingBox.y>0 ? boundingBox.y : 0;
        const drawImageFromTile= makeDrawer(plotView, targetCanvas, tilesToLoad.length, this.loadedImages,
            offsetX,offsetY, scale, opacity);

        tilesToLoad.forEach( (tile) => drawImageFromTile(createImageUrl(plot,tile), tile) );
    }


    shouldComponentUpdate(np,ns) {
        const {props}= this;
        const {plotView:pv}= props;
        const {width:targetWidth, height:targetHeight}= props.plotView.viewDim;
        const {plotView:nPv}= np;

        if (pv.scrollX===nPv.scrollX && pv.scrollY===nPv.scrollY &&
            targetWidth===np.plotView.viewDim.width && targetHeight===np.plotView.viewDim.height &&
            props.plot===np.plot && props.opacity===np.opacity ) {
            return false;
        }

        const result= sCompare(this,np,ns);
        return result;
    }


    render() {
        const {plotView:pv, plot}= this.props;
        const {width, height}= pv.viewDim;
        const tileData = plot.serverImages;

        const scale = plot.zoomFactor/ plot.plotState.getZoomLevel();
        const style = Object.assign({}, containerStyle, {width, height});
        if (scale < .5 && tileData.images.length > 5) {
            return false;
        }
        else {
            if (this.canvas) this.drawTilesForArea();
            return (
                <div className='tile-drawer' style={style}>
                    <SimpleCanvas drawIt={this.drawInit} width={width} height={height} />
                </div>
            );
        }
    }
}


TileDrawerCanvas.propTypes= {
    plot : PropTypes.object.isRequired,
    opacity : PropTypes.number.isRequired,
    plotView : PropTypes.object.isRequired,
};


function purgeLoadedImages(loadedImages) {
    let i;
    for(i=0; (i<loadedImages.serverData.length); i++) {
        if (!loadedImages.used[i]) {
            // if (debugTiles && loadedImages.images[i]) {
            //     console.log(`purging tile: ${loadedImages.images[i].src}`);
            // }
            loadedImages.images[i]= null;
        }
    }
}

/**
 *
 * @param {PlotView} plotView
 * @param {Object} targetCanvas
 * @param {number} totalCnt
 * @param {Object} loadedImages
 * @param {number} offsetX
 * @param {number} offsetY
 * @param {number} scale
 * @param {number} opacity
 * @return {function(*, *)}
 */
function makeDrawer(plotView, targetCanvas, totalCnt, loadedImages, offsetX,offsetY, scale, opacity) {

    if (!targetCanvas) return;
    const offscreenCanvas = document.createElement('canvas');
    const offscreenCtx = offscreenCanvas.getContext('2d');
    const {viewDim:{width,height}}=  plotView;
    if (plotView.rotation) {
        const diagonal= Math.sqrt(width*width + height*height);
        offscreenCanvas.width = diagonal;
        offscreenCanvas.height = diagonal;
    }
    else {
        offscreenCanvas.width = width;
        offscreenCanvas.height = height;
    }


    offsetX= Math.trunc(offsetX);
    offsetY= Math.trunc(offsetY);
    let renderedCnt=0;

    return (src, tile) => {

        const x= Math.trunc((tile.xoff*scale)-offsetX);
        const y= Math.trunc((tile.yoff*scale)-offsetY);
        const w= Math.trunc(tile.width*scale);
        const h= Math.trunc(tile.height*scale);
        const tileIdx= loadedImages.serverData.findIndex( (d) => d.url===tile.url);


        if (loadedImages.images[tileIdx]) {
            offscreenCtx.drawImage(loadedImages.images[tileIdx], x,y, w, h);
            // if (debugTiles) {
            //     offscreenCtx.lineWidth= 1;
            //     offscreenCtx.strokeStyle= 'red';
            //     offscreenCtx.strokeRect(x, y, w-1, h-1);
            //     offscreenCtx.restore();
            // }
            loadedImages.used[tileIdx]= true;
            renderedCnt++;
            if (renderedCnt===totalCnt) {
                renderToScreen(plotView, targetCanvas,offscreenCanvas, opacity, offsetX,offsetY);
                purgeLoadedImages(loadedImages);

            }
        }
        else {
            const im = new Image();
            im.src= src;
            im.onload= () =>  {
                offscreenCtx.drawImage(im, x, y, w, h);
                // if (debugTiles) {
                //     offscreenCtx.lineWidth = 1;
                //     offscreenCtx.strokeStyle = 'blue';
                //     offscreenCtx.strokeRect(x, y, w - 1, h - 1);
                //     offscreenCtx.restore();
                // }


                loadedImages.images[tileIdx]= im;
                loadedImages.used[tileIdx]= true;
                renderedCnt++;
                if (renderedCnt===totalCnt) {
                    renderToScreen(plotView, targetCanvas,offscreenCanvas, opacity, offsetX, offsetY);
                    purgeLoadedImages(loadedImages);
                }
            };
        }
    };

}


function renderToScreen(plotView, targetCanvas, offscreenCanvas, opacity, offsetX, offsetY) {
    window.requestAnimationFrame(() => {
        const ctx= targetCanvas.getContext('2d');
        ctx.save();
        ctx.clearRect(0,0,targetCanvas.width, targetCanvas.height);
        ctx.globalAlpha=opacity;

        const {scrollX, scrollY, flipX,flipY, viewDim, rotation}= plotView;
        if (flipY) offsetX*=-1;

        const affTrans= makeTransform(offsetX,offsetY, scrollX, scrollY, rotation, flipX, flipY, viewDim);
        ctx.setTransform(affTrans.a, affTrans.b, affTrans.c, affTrans.d, affTrans.e, affTrans.f);
        ctx.drawImage(offscreenCanvas, 0,0);
        ctx.restore();
    });
}


function computeBounding(plot,w,h) {
    const ptAry= [];
    const cc= CsysConverter.make(plot);
    ptAry.push(cc.getScreenCoords(makeDevicePt(0,0)));
    ptAry.push(cc.getScreenCoords(makeDevicePt(w,0)));
    ptAry.push(cc.getScreenCoords(makeDevicePt(w,h)));
    ptAry.push(cc.getScreenCoords(makeDevicePt(0,h)));
    return getBoundingBox(ptAry);
}

