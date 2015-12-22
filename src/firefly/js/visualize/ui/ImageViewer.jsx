/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import sCompare from 'react-addons-shallow-compare';
import PlotViewUtil from '../PlotViewUtil.js';
import ImageViewerDecorate from './ImageViewerDecorate.jsx';
import {flux} from '../../Firefly.js';



class ImageViewer extends React.Component {


    constructor(props) {
        super(props);
        this.state= {plotView:PlotViewUtil.getPlotViewById(this.props.plotId)};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
    }


    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        var state= this.state;
        var allPlots= PlotViewUtil.getAllPlots();
        var allDraw= PlotViewUtil.getAllDrawLayersStore();
        var drawLayersAry= PlotViewUtil.getAllDrawLayers(this.props.plotId);
        if (allPlots!==state.allPlots  ||
            (allDraw!==state.allDraw &&
            drawLayersDiffer(drawLayersAry,state.drawLayersAry))) {
            var {plotId}= this.props;
            var plotView= PlotViewUtil.getPlotViewById(plotId);
            this.setState({plotView,
                           allDraw,
                           drawLayersAry:drawLayersAry.filter( (dl) => dl.plotIdAry.includes(plotId)),
                           allPlots:PlotViewUtil.getAllPlots()});
        }
    }

    render() {
        var {plotView,allPlots,drawLayersAry}= this.state;
        if (!plotView) return false;
        return (
            <ImageViewerDecorate plotView={plotView} drawLayersAry={drawLayersAry}
                                 allPlots={allPlots}/>
        );
    }
}

ImageViewer.propTypes= {
    plotId : React.PropTypes.string.isRequired
};



function drawLayersDiffer(dlAry1, dlAry2) {
    return true;
}



export default ImageViewer;
