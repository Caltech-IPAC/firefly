/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';
//import PlotView from '../PlotView.js';
import CsysConverter from '../CsysConverter.js';
import {makeScreenPt} from '../Point.js';
import {encodeUrl, ParamType} from '../../util/WebUtil.js';
import {getRootURL} from '../../util/BrowserUtil.js';


const BG_IMAGE= 'image-working-background-24x24.png';
const BACKGROUND_STYLE = `url(+ ${BG_IMAGE} ) top left repeat`;

const containerStyle={position:'absolute',
                      overflow:'hidden',
                      left: 0,
                      right: 0,
                      background: BACKGROUND_STYLE
};


function isTileVisible(tile, x, y, w, h) {

    var tileX= tile.xoff;
    var tileY= tile.yoff;

    return (x + w > tileX &&
            y + h > tileY &&
            x < tileX  + tile.width &&
            y < tileY + tile.height);
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
 * @param {number} opacity
 * @return {object}
 */
const makeImageFromTile= function(src, vpPt, width, height, opacity) {
    var s= {
        position : 'absolute',
        left : vpPt.x,
        top : vpPt.y,
        width,
        height,
        background: BACKGROUND_STYLE,
        opacity
    };
    return (
        <img src={src} style={s}/>
    );

};



const createImageUrl= function(plot, tile) {
    var params = {
        file: tile.url,
        state: plot.plotState.toJson(),
        type: 'tile',
        x: tile.xoff,
        y: tile.yoff,
        width: tile.width,
        height: tile.height
    };
    return encodeUrl(getRootURL() + 'sticky/FireFly_ImageDownload', ParamType.QUESTION_MARK, params);
};





var TileDrawer= React.createClass(
{


    mixins : [React.addons.PureRenderMixin],

    propTypes: {
        x : React.PropTypes.number.isRequired,
        y : React.PropTypes.number.isRequired,
        width : React.PropTypes.number.isRequired,
        height : React.PropTypes.number.isRequired,
        tileData : React.PropTypes.object.isRequired,
        zoomFactor : React.PropTypes.number.isRequired,
        plot : React.PropTypes.object.isRequired,
        opacity : React.PropTypes.number
    },



    getDefaultProps()  {
        return { opacity : 1};
    },


    getInitialState() {
        return { };
    },


    componentWillUnmount() {
    },

    componentDidMount() {
    },




    getTilesForArea() {
        var {x,y,width,height,tileData,plot,opacity}=  this.props;
        const screenToVP= makeScreenToVPConverter(plot);

        return tileData.images
            .filter( (tile) => isTileVisible(tile,x,y,width,height))
            .map( (tile) => {
                var vpPt= screenToVP(tile.xoff, tile.yoff);
                return makeImageFromTile(createImageUrl(plot,tile), vpPt, tile.width, tile.height, opacity);
            });
    },

    render() {
        var {width,height}= this.props;
        var style=Object.assign({},containerStyle, {width,height});

        return (
            <div className='tile-drawer'  style={style}>
                {this.getTilesForArea()}
            </div>
        );
    }



});

export default TileDrawer;
