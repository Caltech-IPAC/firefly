/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React from 'react';
import CsysConverter from '../CsysConverter.js';
import {makeScreenPt} from '../Point.js';
import {makeImageFromTile,createImageUrl,isTileVisible} from './TileDrawHelper.jsx';


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






function makeScreenToVPConverter(plot) {
    var cc= CsysConverter.make(plot);
    return (x,y) => cc.getViewPortCoords(makeScreenPt(x,y));
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
