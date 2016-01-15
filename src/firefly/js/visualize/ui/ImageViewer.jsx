/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import PlotViewUtil, {getPlotViewById} from '../PlotViewUtil.js';
import ImageViewerDecorate from './ImageViewerDecorate.jsx';
import {visRoot} from '../ImagePlotCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {flux} from '../../Firefly.js';



class ImageViewer extends Component {


    constructor(props) {
        super(props);
        var {plotId}= props;
        var allPlots= visRoot();
        var dlAry= getDlAry();
        var plotView= getPlotViewById(allPlots,plotId);
        var drawLayersAry= PlotViewUtil.getAllDrawLayersForPlot(dlAry,plotId);
        this.state= {plotView, dlAry, allPlots, drawLayersAry};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
    }


    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        var {state}= this;
        var allPlots= visRoot();
        var dlAry= getDlAry();
        var drawLayersAry= PlotViewUtil.getAllDrawLayersForPlot(dlAry,this.props.plotId);
        if (allPlots!==state.allPlots  ||
            (dlAry!==state.dlAry &&
            drawLayersDiffer(drawLayersAry,state.drawLayersAry))) {
            var {plotId}= this.props;
            var plotView= getPlotViewById(allPlots,plotId);
            this.setState({plotView, dlAry, allPlots, drawLayersAry});
        }
    }

    render() {
        var {plotView,allPlots,drawLayersAry}= this.state;
        if (!plotView) return false;
        return (
            <ImageViewerDecorate plotView={plotView} drawLayersAry={drawLayersAry}
                                 visRoot={allPlots}/>
        );
    }
}

ImageViewer.propTypes= {
    plotId : PropTypes.string.isRequired
};



function drawLayersDiffer(dlAry1, dlAry2) {
    return true;
}



export default ImageViewer;
