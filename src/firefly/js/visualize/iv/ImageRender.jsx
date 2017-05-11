/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {Component} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';
import {SimpleCanvas}  from '../draw/SimpleCanvas.jsx';
import {initTileDrawer}  from './CanvasTileDrawer.js';

const BG_IMAGE= 'image-working-background-24x24.png';
const BACKGROUND_STYLE = `url(+ ${BG_IMAGE} ) top left repeat`;

const containerStyle={position:'absolute',
                      overflow:'hidden',
                      left: 0,
                      right: 0,
                      background: BACKGROUND_STYLE
};


export class ImageRender extends Component {


    constructor(props) {
        super(props);

        this.drawInit= (canvas) => {
            this.tileDrawer= initTileDrawer(canvas);
            const {plot, opacity,plotView, tileAttributes, shouldProcess, processor}= this.props;
            this.tileDrawer(plot, opacity,plotView, tileAttributes, shouldProcess, processor);
        };

    }


    shouldComponentUpdate(np,ns) {
        const {props}= this;
        const {plotView:pv}= props;
        const {width:targetWidth, height:targetHeight}= props.plotView.viewDim;
        const {plotView:nPv}= np;

        if (pv.scrollX===nPv.scrollX && pv.scrollY===nPv.scrollY &&
            targetWidth===np.plotView.viewDim.width && targetHeight===np.plotView.viewDim.height &&
            props.tileAttributes===np.tileAttributes &&
            props.plot===np.plot && props.opacity===np.opacity ) {
            return false;
        }

        const result = !shallowequal(this.props,np) || !shallowequal(this.state,ns);
        return result;
    }


    render() {
        const {plot, opacity,plotView:pv, tileAttributes, shouldProcess, processor}= this.props;
        const {width, height}= pv.viewDim;
        const tileData = plot.serverImages;

        const scale = plot.zoomFactor/ plot.plotState.getZoomLevel();
        const style = Object.assign({}, containerStyle, {width, height});
        if (scale < .5 && tileData.images.length > 5) {
            return false;
        }
        else {
            if (this.tileDrawer) this.tileDrawer(plot, opacity,pv, tileAttributes, shouldProcess, processor);

            return (
                <div className='tile-drawer' style={style}>
                    <SimpleCanvas drawIt={this.drawInit} width={width} height={height} />
                </div>
            );
        }
    }
}


ImageRender.propTypes= {
    plot : PropTypes.object.isRequired,
    opacity : PropTypes.number.isRequired,
    plotView : PropTypes.object.isRequired,
    tileAttributes : PropTypes.object,
    shouldProcess : PropTypes.func,
    processor : PropTypes.func,
};
