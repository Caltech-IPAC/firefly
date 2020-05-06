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
import {primePlot} from '../PlotViewUtil.js';
import {makeThumbnailTransformCSS} from '../PlotTransformUtils.js';


const defStyle= {
    width: 70,
    height: 70,
    display: 'inline-block',
    overflow : 'hidden',
    position: 'relative',
    border: '1px solid transparent'
};

const magMouse= [MouseState.DRAG_COMPONENT, MouseState.DRAG, MouseState.MOVE, MouseState.DOWN];
const EMPTY= <div style={defStyle}/>;

export const MagnifiedView= memo(({plotView:pv,size,mouseState}) => {
    if (!pv || !mouseState?.screenPt) return EMPTY;
    const p= primePlot(pv);
    if (!p || isHiPS(p) ) return EMPTY;
    if (!magMouse.includes(mouseState.mouseState)) return EMPTY;

    return (
        <div style={{...defStyle, border: '1px solid rgb(187, 187, 187)'}}>
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

    if (!plot.tileData) return {};

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

function showMag(spt,pv, plot,size) {
    if (!plot || isBlankImage(plot) || plot.zoomFactor > 6) return false;

    let x = spt.x - size / 2;
    let y = spt.y - size / 2;

    const {width:screenW, height:screenH }= plot.screenSize;
    let sizeOffX;

    if (x < 0) {
        x = 0;
        sizeOffX = spt.x;
    } else if (x > screenW - size) {
        x = screenW - size;
        sizeOffX = (size / 2) + ((size / 2) - (screenW - spt.x));
    } else {
        sizeOffX = size / 2;
    }


    let sizeOffY;
    if (y < 0) {
        y = 0;
        sizeOffY = spt.y;
    } else if (y > screenH - size) {
        y = screenH - size;
        sizeOffY = (size / 2) + ((size / 2) - (screenH - spt.y));
    } else {
        sizeOffY = size / 2;
    }



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

