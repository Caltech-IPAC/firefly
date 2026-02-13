/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {Component, memo, useLayoutEffect, useRef} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';
import {SimpleCanvas}  from '../draw/SimpleCanvas.jsx';
import {initImageDrawer}  from './ImageTileDrawer.js';
import {createHiPSDrawer} from './HiPSTileDrawer.js';
import {isImage} from '../WebPlot.js';
import {CANVAS_IMAGE_ID_START} from '../PlotViewUtil.js';
import {primePlot} from '../PlotViewUtil';

const BG_IMAGE= 'image-working-background-24x24.png';
const BACKGROUND_STYLE = `url(+ ${BG_IMAGE} ) top left repeat`;
const containerStyle={position:'absolute', overflow:'hidden', left: 0, right: 0, background: BACKGROUND_STYLE };


function initTileDrawer(targetCanvas, plot) {
    if (!targetCanvas) return () => undefined;
    return isImage(plot) ? initImageDrawer(targetCanvas) : createHiPSDrawer(targetCanvas);
}

export class ImageRenderOLD extends Component {


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
            pv.lastByteRefreshData===nPv.lastByteRefreshData &&
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


//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++ Experiment +++++++++++++++++++++++++++++++++++++
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

// export const ImageRenderEXPERIMENT=memo(
export const ImageRender=memo(
    (props) =>{
        const {current:draw} = useRef({ tileDrawer:undefined});

        const {current:init} = useRef({
            drawInit: (canvas) => {
                const {plot}= props;
                draw.tileDrawer= initTileDrawer(canvas, plot);
            }
        });


        const {plot, idx, opacity,plotView:pv}= props;
        const {width, height}= pv.viewDim;
        const style = {...containerStyle, width, height};

        useLayoutEffect(() => {
            draw.tileDrawer?.(plot, opacity,pv,props.colorMode);// eslint-disable-line react-hooks/refs
        });



        return (
            <div className='tile-drawer' style={style}>
                <SimpleCanvas drawIt={init.drawInit} width={width} height={height} plotType={plot.plotType}
                              id={`${CANVAS_IMAGE_ID_START}${idx}-${pv.plotId}`}/>
            </div>
        );
    },
    (p,np) => {
        const {plotView:pv}= p;
        const {width:targetWidth, height:targetHeight}= p.plotView.viewDim;
        const {plotView:nPv}= np;

        if (p.colorMode!==np.colorMode) return false;
        if (pv.scrollX===nPv.scrollX && pv.scrollY===nPv.scrollY &&
            pv.lastByteRefreshData===nPv.lastByteRefreshData &&
            targetWidth===np.plotView.viewDim.width && targetHeight===np.plotView.viewDim.height &&
            pv.overlayPlotViews===nPv.overlayPlotViews &&
            p.plot===np.plot && p.opacity===np.opacity ) {
            return true;
        }

        const nextPlot= primePlot(nPv);
        const plot= primePlot(pv);
        if (nextPlot.plotType!==plot.plotType) this.tileDrawer= undefined;

        return shallowequal(p,np);
        // return false;

    }
);
ImageRender.propTypes= {
    plot : PropTypes.object.isRequired,
    opacity : PropTypes.number.isRequired,
    plotView : PropTypes.object.isRequired,
    shouldProcess : PropTypes.func,
    idx : PropTypes.number,
    processor : PropTypes.func,
    colorMode: PropTypes.string
};
