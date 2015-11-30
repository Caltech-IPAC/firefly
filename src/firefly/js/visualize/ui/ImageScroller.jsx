/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import TileDrawer from './TileDrawer.jsx';
import PlotViewUtil from '../PlotViewUtil.js';
import WebPlot from '../WebPlot.js';
import {flux} from '../../Firefly.js';




var ImageScroller= React.createClass(
{


    mixins : [PureRenderMixin],


    propTypes: {
        x : React.PropTypes.number.isRequired,
        y : React.PropTypes.number.isRequired,
        width : React.PropTypes.number.isRequired,
        height : React.PropTypes.number.isRequired,
        tileData : React.PropTypes.object.isRequired,
        zoomFactor : React.PropTypes.number.isRequired,
        plotData : React.PropTypes.object.isRequired,
        opacity : React.PropTypes.number
    },


    getDefaultProps: function () {
        return { y: 1 };
    },


    getInitialState() {
        var {plotId}= this.props;
        var pv= PlotViewUtil.getPlotViewById(plotId);
        var plotData= pv.primaryPlot;

        return {pv, plotData};
    },


    render() {
        var {plotData}= this.state;
        return (
            <div className='web-plot-view-scr' >
                <div className='plot-view-scr-view-window' >
                    <div className='plot-view-master-panel' >
                        <TileDrawer x={0} y={0} width={5} height={5}
                                    tileData={plotData.serverImages}
                                    zoomFactor={plotData.plotState.getZoomFactor()}
                                    plotData={plotData}
                                    opacity={plotData.percentOpaque}
                            />
                        <div className='event-layer' >
                        </div>
                        <div className='drawingArea' >
                            <div className='drawingLayer'></div>
                            <div className='drawingLayer'></div>
                            <div className='drawingLayer'></div>
                            <div className='drawingLayer'></div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }



});

export default ImageScroller;
