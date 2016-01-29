/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PropTypes} from 'react';
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
};;

export function TileDrawer({ x, y, width, height, plot}) {


    var tileData=plot.serverImages;
    var tileZoomFactor=plot.plotState.getZoomLevel();
    var zoomFactor=plot.zoomFactor
    var opacity=plot.percentOpaque;

    const scale= zoomFactor / tileZoomFactor;
    const style=Object.assign({},containerStyle, {width,height});
    if (scale < .5 && tileData.images.length>5) {
        return <div></div>;
    }
    else {
        return (
            <div className='tile-drawer'  style={style}>
                {getTilesForArea(x,y,width,height,tileData,plot,scale,opacity)}
            </div>
        );
    }
}



TileDrawer.propTypes= {
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



function getTilesForArea(x,y,width,height,tileData,plot,scale,opacity) {
    const screenToVP= makeScreenToVPConverter(plot);

    return tileData.images
        .filter( (tile) => isTileVisible(tile,x,y,width,height,scale))
        .map( (tile) => {
            var vpPt= screenToVP(tile.xoff*scale, tile.yoff*scale);
            return makeImageFromTile(createImageUrl(plot,tile), vpPt, tile.width, tile.height, scale, opacity);
        });
}
