/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PropTypes, Component} from 'react';
import CsysConverter from '../CsysConverter.js';
import sCompare from 'react-addons-shallow-compare';
import {SimpleCanvas}  from '../draw/SimpleCanvas.jsx';
import {debounce}  from 'lodash';
import {makeScreenPt} from '../Point.js';
import {makeImageFromTile,createImageUrl,isTileVisible} from './TileDrawHelper.jsx';


// EXPERIMENTAL
// EXPERIMENTAL
// EXPERIMENTAL
//                This class should not be used. Just for trying an alternative tile drawer.
//                So far the performance is not better than laying out image elements (see TileDrawer.jsx)
//                I am keeping it here for reference
// EXPERIMENTAL
// EXPERIMENTAL
// EXPERIMENTAL
// EXPERIMENTAL




const BG_IMAGE= 'image-working-background-24x24.png';
const BACKGROUND_STYLE = `url(+ ${BG_IMAGE} ) top left repeat`;

const containerStyle={position:'absolute',
                      overflow:'hidden',
                      left: 0,
                      right: 0,
                      background: BACKGROUND_STYLE
};

export class TileDrawerCanvas extends Component {


    constructor(props) {
        super(props);

        this.canvas= null;

        this.loadedImages= {
            serverData: null,
            images: []
        };


        this.drawInit= (canvas) => {
            console.log('new canvas');
            this.canvas= canvas;
            this.drawTilesForArea();
        };


        this.drawTilesForArea= debounce(() => {
            var {canvas}= this;
            if (!canvas) return;

            var { x, y, width, height, plot}= this.props;
            var tileData = plot.serverImages;
            var tileZoomFactor = plot.plotState.getZoomLevel();
            var zoomFactor = plot.zoomFactor;
            var opacity = plot.percentOpaque;
            const scale = zoomFactor / tileZoomFactor;

            if (this.loadedImages.serverData!==tileData.images) {
                this.loadedImages= {
                    serverData: tileData.images,
                    images: Array(tileData.images.length)
                };
            }

            var sw= Math.floor(width*scale);
            var sh= Math.floor(height*scale);
            if (canvas.width != sw || canvas.height!=sh) {
                canvas.width=sw;
                canvas.height=sh;
            }


            //console.log(`drawing: ${plot.plotId}`);
            const screenToVP= makeScreenToVPConverter(plot);

            var tilesToLoad= tileData.images.filter( (tile) => isTileVisible(tile,x,y,width,height,scale));

            var drawImageFromTile= makeDrawer(canvas,width, height, tilesToLoad.length, this.loadedImages);
            tilesToLoad.forEach( (tile, idx) => {
                var vpPt= screenToVP(tile.xoff*scale, tile.yoff*scale);
                return drawImageFromTile(createImageUrl(plot,tile), idx,vpPt, tile.width, tile.height, scale, opacity);
            });
        }, 60);
    }

    //drawImageFromTile(canvas, src, vpPt, width, height, scale,opacity=1) {
    //    if (!canvas) return;
    //    console.log(`src: ${src}`);
    //    var ctx = canvas.getContext('2d');
    //    var im = new Image();
    //    im.src= src;
    //    im.onload= () =>  {
    //        ctx.drawImage(im, vpPt.x,vpPt.y, width*scale, height*scale);
    //        this.loadedCnt++;
    //    };
    //}


    shouldComponentUpdate(np,ns) {
        //if (np.x!==this.props.x) console.log('diff x');
        //if (np.y!==this.props.x) console.log('diff y');
        //if (np.width!==this.props.width) console.log('diff width');
        //if (np.height!==this.props.height) console.log('diff height');
        //if (np.plot!==this.props.plot) console.log('diff plot');
        //if (np.plot.plotState!==this.props.plot.plotState) console.log('diff plot plotState');
        //if (np.plot.viewPort!==this.props.plot.viewPort) console.log('diff plot viewport');
        //if (np.plot.zoomFactor!==this.props.plot.zoomFactor) console.log('diff plot zoomFactor');
        var result= sCompare(this,np,ns);
        //if (result) console.log('will update');
        return result;
    }


    render() {
        var {width, height, plot, x, y}= this.props;
        var zoomFactor = plot.zoomFactor;
        var tileZoomFactor = plot.plotState.getZoomLevel();
        var tileData = plot.serverImages;

        const scale = zoomFactor / tileZoomFactor;
        const style = Object.assign({}, containerStyle, {width, height});
        if (scale < .5 && tileData.images.length > 5) {
            return false;
        }
        else {
            if (this.canvas) this.drawTilesForArea();
            return (
                <div className='tile-drawer' style={style}>
                    <SimpleCanvas drawIt={this.drawInit} width={1} height={1} />
                </div>
            );
        }
    }
}

//<SimpleCanvas drawIt={this.drawTilesForArea}
//              x={x} y={y}
//              width={Math.floor(width*scale)} height={Math.floor(height*scale)}
//              tileZoomFactor={tileZoomFactor}
//              tileData={tileData}
//              plot={this.plot} />

//<SimpleCanvas drawIt={this.drawTilesForArea} width={width} height={height}/>

TileDrawerCanvas.propTypes= {
    x : PropTypes.number.isRequired,
    y : PropTypes.number.isRequired,
    width : PropTypes.number.isRequired,
    height : PropTypes.number.isRequired,
    plot : PropTypes.object.isRequired
};

function makeScreenToVPConverter(plot) {
    var cc= CsysConverter.make(plot);
    return (x,y) => cc.getViewPortCoords(makeScreenPt(x,y));
}



function makeDrawer(targetCanvas, width, height, totalCnt, loadedImages) {

    if (!targetCanvas) return;
    var loadedCnt=0;
    var offscreenCanvas = document.createElement('canvas');
    offscreenCanvas.width = width;
    offscreenCanvas.height = height;
    var offscreenCtx = offscreenCanvas.getContext('2d');


    return (src, idx, vpPt, width, height, scale,opacity=1) => {
        //console.log(`src: ${src}`);
        if (false && loadedImages.images[idx]) {
            offscreenCtx.drawImage(loadedImages.images[idx], vpPt.x,vpPt.y, width*scale, height*scale);
            loadedCnt++;
            //console.log(`Cached: ${src} loadedCnt:${loadedCnt}, totalCnt:${totalCnt}`);
            if (loadedCnt===totalCnt) {
                targetCanvas.getContext('2d').drawImage(offscreenCanvas, 0, 0);
            }
        }
        else {
            var im = new Image();
            im.src= src;
            im.onload= () =>  {
                //console.log(`reload: ${src}`);
                offscreenCtx.drawImage(im, vpPt.x,vpPt.y, width*scale, height*scale);
                loadedImages.images[idx]= im;
                loadedCnt++;
                //if (loadedCnt===totalCnt) {
                    window.requestAnimationFrame(() => {
                        targetCanvas.getContext('2d').drawImage(offscreenCanvas, 0, 0);
                    });
                //}
            };
        }
    };

}

