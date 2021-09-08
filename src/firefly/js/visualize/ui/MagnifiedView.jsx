/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {isEmpty} from 'lodash';
import {makeScreenPt} from '../Point.js';
import {MouseState} from '../VisMouseSync.js';
import {makeImageFromTile,createImageUrl,isTileVisible} from '../iv/TileDrawHelper.jsx';
import {isBlankImage, isHiPS} from '../WebPlot.js';
import {hasLocalStretchByteData, primePlot} from '../PlotViewUtil.js';
import {makeThumbnailTransformCSS} from '../PlotTransformUtils.js';
import {getLocalScreenTileAtZoom} from '../rawData/RawDataOps.js';
import {SimpleCanvas} from '../draw/SimpleCanvas.jsx';


const defStyle= {
    width: 70,
    height: 70,
    display: 'inline-block',
    overflow : 'hidden',
    position: 'relative',
    border: '1px solid transparent'
};

const magMouse= [MouseState.DRAG_COMPONENT.key, MouseState.DRAG.key,
    MouseState.MOVE.key, MouseState.DOWN.key, MouseState.CLICK.key];

const makeEmpty= (size) => (<div style={{...defStyle, width:size, height:size}}/>);

export const MagnifiedView= memo(({plotView:pv,size,mouseState}) => {
    if (!pv || !mouseState?.screenPt) return makeEmpty(size);
    const p= primePlot(pv);
    if (!p || isHiPS(p) ) return makeEmpty(size);
    if (!magMouse.includes(mouseState.mouseState?.key)) return makeEmpty(size);

    return (
        <div style={{...defStyle, width:size, height:size, border: '1px solid rgb(187, 187, 187)'}}>
            {showMag(mouseState.screenPt,pv, p,size)}
        </div>
    );
});

MagnifiedView.propTypes= {
    plotView: PropTypes.object,
    size: PropTypes.number.isRequired,
    mouseState: PropTypes.object
};


/**
 *
 * @param {WebPlot} plot
 * @param {Point} spt
 * @param {number} size
 * @return {Object}
 */
function getImagesAt(plot, spt, size) {

    if (!plot.tileData?.images) return {};

    const scale= plot.zoomFactor / plot.plotState.getZoomLevel();

    const tiles= plot.tileData.images
        .filter( (tile) => isTileVisible(tile,spt.x,spt.y,size,size,scale))
        .sort(compareFourTileSort);

    if (!tiles.length) return {};

    const newX = spt.x - tiles[0].x;
    const newY = spt.y - tiles[0].y;
    return {tiles, newX, newY};
}


/**
 * This Comparator is for the very specific case that you want to arrange 4 tiles in a specific order
 * @param o1
 * @param o2
 * @return {number}
 */
function compareFourTileSort(o1, o2) {
    const {x:x1, y:y1}= o1;
    const {x:x2, y:y2}= o2;
    if (x1===x2) {
        if (y1===y2)      return 0;
        else if (y1 < y2) return -1;
        else              return 1;
    }
    else if (x1 < x2)   return -1;
    else return 1;
}

const showTooHighZoomMessage= () => (
    <div style={
        {
            width:'100%',
            height:'100%',
            display:'flex',
            flexDirection:'column',
            lineHeight:'12pt',
            alignItems: 'center',
            justifyContent: 'center',
        } } >
        <div>Zoom Factor Too High</div>
        <div>Magnifier Off</div>
    </div> );


function showMag(spt,pv, plot,size) {
    if (!plot || isBlankImage(plot)) return false;
    if (plot.zoomFactor > 6) return showTooHighZoomMessage();

    const {width:screenW, height:screenH }= plot.screenSize;
    let sizeOffX, sizeOffY;

    let x = spt.x - size / 2;
    let y = spt.y - size / 2;

    if (x < 0) {
        x = 0;
        sizeOffX = spt.x;
    } else if (x > screenW - size) {
        x = screenW - size;
        sizeOffX = (size / 2) + ((size / 2) - (screenW - spt.x));
    } else {
        sizeOffX = size / 2;
    }

    if (y < 0) {
        y = 0;
        sizeOffY = spt.y;
    } else if (y > screenH - size) {
        y = screenH - size;
        sizeOffY = (size / 2) + ((size / 2) - (screenH - spt.y));
    } else {
        sizeOffY = size / 2;
    }

    return hasLocalStretchByteData(plot) ?
        showMagUsingLocal(x,y,pv,plot,size,sizeOffX,sizeOffY) :
        showMagUsingRemote(x,y,pv,plot,size,sizeOffX,sizeOffY);

}

function showMagUsingLocal(x,y,pv, plot,size,sizeOffX,sizeOffY) {
    const style= {
        transform :makeThumbnailTransformCSS(pv.rotation,pv.flipX, pv.flipY),
        width: size,
        height: size,
        position: 'relative'
    };
    const magFactor=2;
    const magCanvas= getLocalScreenTileAtZoom(plot,(x*magFactor+sizeOffX),(y*magFactor+sizeOffY),size,size,plot.zoomFactor*magFactor);
    if (!magCanvas) return <div/>;
    // const dataUrl=  magCanvas.toDataURL();

    const drawOnCanvas= (targetCanvas) => {
        if (!targetCanvas) return;
        const ctx= targetCanvas.getContext('2d');
        ctx.clearRect(0, 0, targetCanvas.width, targetCanvas.height);
        targetCanvas.getContext('2d').drawImage(magCanvas,0,0);
    };

    // const s= { position : 'absolute', left : 0, top : 0};
    return (
        <div style={style}>
            <SimpleCanvas drawIt={drawOnCanvas} width={size} height={size}
                          id={'magnifier'}/>
        </div>
    );
}

// function showMagUsingLocal(x,y,pv, plot,size,sizeOffX,sizeOffY) {
//     const style= {
//         transform :makeThumbnailTransformCSS(pv.rotation,pv.flipX, pv.flipY),
//         width: size,
//         height: size,
//         position: 'relative'
//     };
//     const magFactor=2;
//     const magCanvas= getLocalScreenTileAtZoom(plot,(x*magFactor+sizeOffX),(y*magFactor+sizeOffY),size,size,plot.zoomFactor*magFactor);
//     if (!magCanvas) return <div/>;
//     const dataUrl=  magCanvas.toDataURL();
//
//     const s= { position : 'absolute', left : 0, top : 0};
//     return (
//         <div style={style}>
//             <img src={dataUrl} style={s}/>
//         </div>
//     );
// }

function showMagUsingRemote(x,y,pv, plot,size,sizeOffX,sizeOffY) {
    const {tiles,newX,newY} =getImagesAt(plot,makeScreenPt(x, y), size);
    if (isEmpty(tiles)) return false;

    let pt2, pt3, pt4;
    const [t1,t2,t3,t4]= tiles;

    const pt1= makeScreenPt(-2 * newX - sizeOffX, -2 * newY - sizeOffY);
    const firstImage= makeImageFromTile(createImageUrl(plot,t1), pt1, t1.width, t1.height, 2);

    let results;
    if (tiles.length===1) {
        results= firstImage;
    }
    else if (tiles.length===2) {
        if (t1.x < t2.x) {  // tiles are horizontal
            pt2= makeScreenPt(-2 * newX - sizeOffX + t1.width * 2, -2 * newY - sizeOffY); // to the right
        } else { // tiles are vertical
            pt2= makeScreenPt( -2 * newX - sizeOffX, -2 * newY - sizeOffY + t1.height * 2); // below
        }
        results= [
            firstImage,
            makeImageFromTile(createImageUrl(plot,t2), pt2, t2.width, t2.height, 2)
        ];

    } else if (tiles.length=== 4) {
        pt2= makeScreenPt( -2 * (newX) - sizeOffX, -2 * (newY) - sizeOffY + t2.height * 2); // south east
        pt3= makeScreenPt( -2 * (newX) - sizeOffX + t1.width * 2, -2 * newY - sizeOffY); // north west
        pt4= makeScreenPt( -2 * (newX) - sizeOffX + t1.width * 2, -2 * newY - sizeOffY + t1.height * 2); // south west

        results= [
            firstImage,
            makeImageFromTile(createImageUrl(plot,t2), pt2, t2.width, t2.height, 2),
            makeImageFromTile(createImageUrl(plot,t3), pt3, t3.width, t3.height, 2),
            makeImageFromTile(createImageUrl(plot,t4), pt4, t4.width, t4.height, 2)
        ];
    } else {
        return null;
    }

    const style= {
        transform :makeThumbnailTransformCSS(pv.rotation,pv.flipX, pv.flipY),
        width: size,
        height: size,
    };
    return ( <div style={style}> {results} </div> );
}
