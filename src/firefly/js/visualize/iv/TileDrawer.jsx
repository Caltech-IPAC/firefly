/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {Component, PropTypes} from 'react';
// import {omit,pick} from 'lodash';
// import shallowequal from 'shallowequal';
import CsysConverter from '../CsysConverter.js';
import sCompare from 'react-addons-shallow-compare';
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


export class TileDrawer extends Component {
    constructor(props) {
        super(props);
    }
    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    // shouldComponentUpdate(np) {
    //     const {props}= this;
    //     const update= !shallowequal(omit(props, exPlot), omit(np,exPlot));
    //     if (update) {
    //         return ( !shallowequal(omit(props.plot,exFromPlotPlot), omit(np.plot,exFromPlotPlot)) );
    //     }
    //     return false;
    // }

    render() {
        const { x, y, width, height, plot, rootPlot, opacity}= this.props;
        var tileData=plot.serverImages;
        var tileZoomFactor=plot.plotState.getZoomLevel();
        var zoomFactor=plot.zoomFactor;

        const scale= zoomFactor / tileZoomFactor;
        const style=Object.assign({},containerStyle, {width,height});
        if (scale < .3 && tileData.images.length>7) {
            return <div></div>;
        }
        else {
            return (
                <div className='tile-drawer'  style={style}>
                    {getTilesForArea(x,y,width,height,tileData,plot,scale,opacity, rootPlot)}
                </div>
            );
        }

    }

}

TileDrawer.propTypes= {
    x : PropTypes.number.isRequired,
    y : PropTypes.number.isRequired,
    width : PropTypes.number.isRequired,
    height : PropTypes.number.isRequired,
    plot : PropTypes.object.isRequired,
    rootPlot : PropTypes.object.isRequired,
    opacity : PropTypes.number.isRequired,
};






function makeScreenToVPConverter(plot) {
    var cc= CsysConverter.make(plot);
    return (x,y) => cc.getViewPortCoords(makeScreenPt(x,y));
}



function getTilesForArea(x,y,width,height,tileData,plot,scale,opacity, rootPlot) {
    const screenToVP= makeScreenToVPConverter(rootPlot);

    return tileData.images
        .filter( (tile) => isTileVisible(tile,x,y,width,height,scale))
        .map( (tile) => {
            var vpPt= screenToVP(tile.xoff*scale, tile.yoff*scale);
            return makeImageFromTile(createImageUrl(plot,tile), vpPt, tile.width, tile.height, scale, opacity);
        });
}
