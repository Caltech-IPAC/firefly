/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {makeScreenPt,makeImagePt,makeWorldPt} from '../Point.js';
import {MouseState} from '../VisMouseCntlr.js';
import {makeImageFromTile,createImageUrl,isTileVisible} from './TileDrawHelper.jsx';
import {isBlankImage} from '../WebPlot.js';


var defStyle= {
    width: 70,
    height: 70,
    display: 'inline-block',
    overflow : 'hidden',
    position: 'relative',
    border: '1px solid transparent'
};

const magMouse= [MouseState.DRAG_COMPONENT, MouseState.DRAG, MouseState.MOVE, MouseState.DOWN];
const EMPTY= <div style={defStyle}></div>;

export function MagnifiedView({plotView:pv,size,mouseState}) {
    if (!pv || !mouseState) return EMPTY;
    if (!magMouse.includes(mouseState.mouseState)) return EMPTY;

    var spt= mouseState.screenPt;

    if (!spt) return EMPTY;

    var s= Object.assign({},defStyle, {border: '1px solid rgb(187, 187, 187)'});
    var plot= pv.primaryPlot;

    return (
        <div style={s}>
            {showMag(spt,plot,size)}
        </div>
    );
}



MagnifiedView.propTypes= {
    plotView: React.PropTypes.object,
    size: React.PropTypes.number.isRequired,
    mouseState: React.PropTypes.object
};


/**
 *
 * @param plot
 * @param spt
 * @param size
 * @return {[]}
 */
function getImagesAt(plot, spt, size) {

    if (!plot.serverImages) return null;

    const scale= plot.zoomFactor / plot.plotState.getZoomLevel();

    var tiles= plot.serverImages.images
        .filter( (tile) => isTileVisible(tile,spt.x,spt.y,size,size,scale))
        .sort(compareFourTileSort);

    if (!tiles.length) return null;

    var newX = spt.x - tiles[0].xoff;
    var newY = spt.y - tiles[0].yoff;
    return {tiles, newX, newY};
}



/**
 * This Comparator is for the very specific case that you want to arrange 4 tiles in a specific order
 */
function compareFourTileSort(o1, o2) {
    var {xoff:x1, yoff:y1}= o1;
    var {xoff:x2, yoff:y2}= o2;

    if (x1===x2) {
        if (y1===y2)      return 0;
        else if (y1 < y2) return -1;
        else              return 1;
    }
    else if (x1 < x2)   return -1;
    else return 1;
}



function showMag(spt,plot,size) {

    if (!plot) return false;
    if (isBlankImage(plot)) return false;
    if (plot.zoomFactor > 6) return false;

    var x = spt.x - size / 2;
    var y = spt.y - size / 2;

    var {width:screenW, height:screenH }= plot.screenSize;
    var sizeOffX;

    if (x < 0) {
        x = 0;
        sizeOffX = spt.x;
    } else if (x > screenW - size) {
        x = screenW - size;
        sizeOffX = (size / 2) + ((size / 2) - (screenW - spt.x));
    } else {
        sizeOffX = size / 2;
    }


    var sizeOffY;
    if (y < 0) {
        y = 0;
        sizeOffY = spt.y;
    } else if (y > screenH - size) {
        y = screenH - size;
        sizeOffY = (size / 2) + ((size / 2) - (screenH - spt.y));
    } else {
        sizeOffY = size / 2;
    }



    var {tiles,newX,newY} =getImagesAt(plot,makeScreenPt(x, y), size);
    if (!tiles || !tiles.length) return false;

    var pt1, pt2, pt3, pt4;
    var [t1,t2,t3,t4]= tiles;

    pt1= makeScreenPt(-2 * newX - sizeOffX, -2 * newY - sizeOffY);
    var firstImage= makeImageFromTile(createImageUrl(plot,t1), pt1, t1.width, t1.height, 2);

    if (tiles.length===1) {
        return firstImage;
    }
    else if (tiles.length===2) {
        if (t1.xoff < t2.xoff) {  // tiles are horizontal
            pt2= makeScreenPt(-2 * newX - sizeOffX + t1.width * 2, -2 * newY - sizeOffY); // to the right
        } else { // tiles are vertical
            pt2= makeScreenPt( -2 * newX - sizeOffX, -2 * newY - sizeOffY + t1.height * 2); // below
        }
        return [
            firstImage,
            makeImageFromTile(createImageUrl(plot,t2), pt2, t2.width, t2.height, 2)
        ];

    } else if (tiles.length=== 4) {
        pt2= makeScreenPt( -2 * (newX) - sizeOffX, -2 * (newY) - sizeOffY + t2.height * 2); // south east
        pt3= makeScreenPt( -2 * (newX) - sizeOffX + t1.width * 2, -2 * newY - sizeOffY); // north west
        pt4= makeScreenPt( -2 * (newX) - sizeOffX + t1.width * 2, -2 * newY - sizeOffY + t1.height * 2); // south west

        return [
            firstImage,
            makeImageFromTile(createImageUrl(plot,t2), pt2, t2.width, t2.height, 2),
            makeImageFromTile(createImageUrl(plot,t3), pt3, t3.width, t3.height, 2),
            makeImageFromTile(createImageUrl(plot,t4), pt4, t4.width, t4.height, 2)
        ];
    } else {
        return null;
    }
}

