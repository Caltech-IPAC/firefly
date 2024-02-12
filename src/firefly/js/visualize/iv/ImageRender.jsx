/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {Component} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';
import {SimpleCanvas}  from '../draw/SimpleCanvas.jsx';
import {initImageDrawer}  from './ImageTileDrawer.js';
import {createHiPSDrawer} from './HiPSTileDrawer.js';
import {isImage} from '../WebPlot.js';
import {CANVAS_IMAGE_ID_START} from '../PlotViewUtil.js';
import {primePlot} from '../PlotViewUtil';
import {getGpuJsImmediate} from '../rawData/GpuJsConfig.js';

const BG_IMAGE= 'image-working-background-24x24.png';
const BACKGROUND_STYLE = `url(+ ${BG_IMAGE} ) top left repeat`;

const containerStyle={position:'absolute',
                      overflow:'hidden',
                      left: 0,
                      right: 0,
                      background: BACKGROUND_STYLE
};




/**
 * Return a function that should be called on every render to draw the image
 * @param targetCanvas
 * @param {WebPlot} plot
 * @return {*}
 */
function initTileDrawer(targetCanvas, plot) {
    if (!targetCanvas) return () => undefined;
    if (isImage(plot)) {
        return initImageDrawer(targetCanvas);
    }
    else {
        return createHiPSDrawer(targetCanvas, getGpuJsImmediate());
    }
}




export class ImageRender extends Component {


    constructor(props) {
        super(props);

        this.drawInit= (canvas) => {
            const {plot, opacity,plotView}= this.props;
            this.tileDrawer= initTileDrawer(canvas, plot);
            this.tileDrawer(plot, opacity,plotView, this.props.colorMode);
        };
    }


    shouldComponentUpdate(np,ns) {
        const {props}= this;
        const {plotView:pv}= props;
        const {width:targetWidth, height:targetHeight}= props.plotView.viewDim;
        const {plotView:nPv}= np;

        if (props.colorMode!==np.colorMode) return true;
        if (pv.scrollX===nPv.scrollX && pv.scrollY===nPv.scrollY &&
            targetWidth===np.plotView.viewDim.width && targetHeight===np.plotView.viewDim.height &&
            pv.overlayPlotViews===nPv.overlayPlotViews &&
            props.plot===np.plot && props.opacity===np.opacity ) {
            return false;
        }

        const nextPlot= primePlot(nPv);
        const plot= primePlot(pv);
        if (nextPlot.plotType!==plot.plotType) this.tileDrawer= undefined;

        const result = !shallowequal(this.props,np) || !shallowequal(this.state,ns);
        return result;
    }


    render() {
        const {plot, idx, opacity,plotView:pv}= this.props;
        const {width, height}= pv.viewDim;
        const style = {...containerStyle, width, height};
        this.tileDrawer?.(plot, opacity,pv,this.props.colorMode);

        return (
            <div className='tile-drawer' style={style}>
                <SimpleCanvas drawIt={this.drawInit} width={width} height={height} plotType={plot.plotType}
                              id={`${CANVAS_IMAGE_ID_START}${idx}-${pv.plotId}`}/>
            </div>
        );
    }
}


ImageRender.propTypes= {
    plot : PropTypes.object.isRequired,
    opacity : PropTypes.number.isRequired,
    plotView : PropTypes.object.isRequired,
    shouldProcess : PropTypes.func,
    idx : PropTypes.number,
    processor : PropTypes.func,
    colorMode: PropTypes.string
};
