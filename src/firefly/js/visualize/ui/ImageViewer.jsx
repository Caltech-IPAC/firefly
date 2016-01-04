/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import sCompare from 'react-addons-shallow-compare';
import PlotViewUtil from '../PlotViewUtil.js';
import ImageViewerDecorate from './ImageViewerDecorate.jsx';
import {visRoot} from '../ImagePlotCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {flux} from '../../Firefly.js';



class ImageViewer extends React.Component {


    constructor(props) {
        super(props);
        this.state= {plotView:PlotViewUtil.getPlotViewById(visRoot(),this.props.plotId)};
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
        var allPlots= visRoot();
        var dlAry= getDlAry();
        var drawLayersAry= PlotViewUtil.getAllDrawLayersForPlot(dlAry,this.props.plotId);
        if (allPlots!==state.allPlots  ||
            (dlAry!==state.dlAry &&
            drawLayersDiffer(drawLayersAry,state.drawLayersAry))) {
            var {plotId}= this.props;
            var plotView= PlotViewUtil.getPlotViewById(allPlots,plotId);
            this.setState({plotView,
                           dlAry,
                           allPlots,
                           drawLayersAry:drawLayersAry.filter( (dl) => dl.plotIdAry.includes(plotId))});
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
    plotId : React.PropTypes.string.isRequired
};



function drawLayersDiffer(dlAry1, dlAry2) {
    return true;
}



export default ImageViewer;
