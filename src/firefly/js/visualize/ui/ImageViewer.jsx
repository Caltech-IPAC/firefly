/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import PlotViewUtil from '../PlotViewUtil.js';
import ImageViewerDecorate from './ImageViewerDecorate.jsx';
import {flux} from '../../Firefly.js';






var ImageViewer= React.createClass(
{


    mixins : [PureRenderMixin],

    storeListenerRemove : null,


    propTypes: {
        plotId : React.PropTypes.string.isRequired,
    },

    getInitialState() {
        return {plotView:PlotViewUtil.getPlotViewById(this.props.plotId)};
    },


    componentWillUnmount() {
        if (this.formStoreListenerRemove) this.formStoreListenerRemove();
    },


    componentDidMount() {
        this.formStoreListenerRemove= flux.addListener(this.storeUpdate);
    },

    storeUpdate() {
        var allPlots= PlotViewUtil.getAllPlots();
        var allDraw= PlotViewUtil.getAllDrawLayersStore();
        var drawLayersAry= PlotViewUtil.getAllDrawLayers(this.props.plotId);
        if (allPlots!==this.state.allPlots  ||
            (allDraw!==this.state.allDraw &&
            drawLayersDiffer(drawLayersAry,this.state.drawLayersAry))) {
            var {plotId}= this.props;
            var plotView= PlotViewUtil.getPlotViewById(plotId);
            this.setState({plotView,
                           allDraw,
                           drawLayersAry:drawLayersAry.filter( (dl) => dl.plotIdAry.includes(plotId)),
                           allPlots:PlotViewUtil.getAllPlots()});
        }
    },



    render() {



        var {plotView,allPlots,drawLayersAry}= this.state;
        if (plotView) {
            return (
                <ImageViewerDecorate plotView={plotView}
                                     drawLayersAry={drawLayersAry}
                                     allPlots={allPlots}/>
            );
        }
        else {
            return false;
        }
    }



});

function drawLayersDiffer(dlAry1, dlAry2) {
    return true;
}



export default ImageViewer;
