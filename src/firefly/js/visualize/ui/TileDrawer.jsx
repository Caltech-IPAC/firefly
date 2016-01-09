/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React from 'react';
import CsysConverter from '../CsysConverter.js';
import {makeScreenPt} from '../Point.js';
import {encodeServerUrl, ParamType} from '../../util/WebUtil.js';
import {getRootURL} from '../../util/BrowserUtil.js';


const BG_IMAGE= 'image-working-background-24x24.png';
const BACKGROUND_STYLE = `url(+ ${BG_IMAGE} ) top left repeat`;

const containerStyle={position:'absolute',
                      overflow:'hidden',
                      left: 0,
                      right: 0,
                      background: BACKGROUND_STYLE
};

var TileDrawer= function({ x, y, width, height, tileData,
                           tileZoomFactor, zoomFactor,
                          plot, opacity=1 }) {

    const scale= zoomFactor / tileZoomFactor;
    const style=Object.assign({},containerStyle, {width,height});
    if (scale < .5 && tileData.images.length>5) {
        return false;
    }
    else {
        return (
            <div className='tile-drawer'  style={style}>
                {getTilesForArea(x,y,width,height,tileData,plot,scale,opacity)}
            </div>
        );
    }
};



TileDrawer.propTypes= {
    x : React.PropTypes.number.isRequired,
    y : React.PropTypes.number.isRequired,
    width : React.PropTypes.number.isRequired,
    height : React.PropTypes.number.isRequired,
    tileData : React.PropTypes.object.isRequired,
    tileZoomFactor : React.PropTypes.number.isRequired,
    zoomFactor : React.PropTypes.number.isRequired,
    plot : React.PropTypes.object.isRequired,
    opacity : React.PropTypes.number
};




/**
 *
 * @param {object} tile - object returned from server the describes the file
 * @param {number} x
 * @param {number} y
 * @param {number} w
 * @param {number} h
 * @param {number} scale
 * @return {boolean}
 */
function isTileVisible(tile, x, y, w, h, scale) {

    var tileX= tile.xoff*scale;
    var tileY= tile.yoff*scale;
    var tileWidth= tile.width*scale;
    var tileHeight= tile.height*scale;

    return (x + w > tileX &&
            y + h > tileY &&
            x < tileX  + tileWidth &&
            y < tileY + tileHeight);
}



function makeScreenToVPConverter(plot) {
    var cc= CsysConverter.make(plot);
    return (x,y) => cc.getViewPortCoords(makeScreenPt(x,y));
}



/**
 *
 * @param {string} src  url of the tile
 * @param {object} vpPt viewPortPt, where to put the tile
 * @param {number} width
 * @param {number} height
 * @param {number} scale
 * @param {number} opacity
 * @return {object}
 */
function makeImageFromTile(src, vpPt, width, height, scale,opacity) {
    var s= {
        position : 'absolute',
        left : vpPt.x,
        top : vpPt.y,
        width: width*scale,
        height: height*scale,
        background: BACKGROUND_STYLE,
        opacity
    };
    return (
        <img src={src} key={src} style={s}/>
    );

}



function createImageUrl(plot, tile) {
    var params = {
        file: tile.url,
        state: plot.plotState.toJson(),
        type: 'tile',
        x: tile.xoff,
        y: tile.yoff,
        width: tile.width,
        height: tile.height
    };
    return encodeServerUrl(getRootURL() + 'sticky/FireFly_ImageDownload', params);
}


function getTilesForArea(x,y,width,height,tileData,plot,scale,opacity) {
    const screenToVP= makeScreenToVPConverter(plot);

    return tileData.images
        .filter( (tile) => isTileVisible(tile,x,y,width,height,scale))
        .map( (tile) => {
            var vpPt= screenToVP(tile.xoff*scale, tile.yoff*scale);
            return makeImageFromTile(createImageUrl(plot,tile), vpPt, tile.width, tile.height, scale, opacity);
        });
}



export default TileDrawer;
