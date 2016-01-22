/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import PlotViewUtil, {getPlotViewById} from '../PlotViewUtil.js';
import ImageViewerDecorate from './ImageViewerDecorate.jsx';
import {visRoot} from '../ImagePlotCntlr.js';
import {extensionRoot} from '../../core/ExternalAccessCntlr.js';
import {getExtensionList} from '../../core/ExternalAccessUtils.js';
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
        var extRoot= extensionRoot();
        this.state= {plotView, dlAry, allPlots, drawLayersAry,extRoot};
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
        var extRoot= extensionRoot();
        var drawLayersAry= PlotViewUtil.getAllDrawLayersForPlot(dlAry,this.props.plotId);
        if (allPlots!==state.allPlots  ||
            extRoot!==state.extRoot ||
            (dlAry!==state.dlAry &&
            drawLayersDiffer(drawLayersAry,state.drawLayersAry))) {
            var {plotId}= this.props;
            var plotView= getPlotViewById(allPlots,plotId);
            this.setState({plotView, dlAry, allPlots, drawLayersAry,extRoot});
        }
    }

    render() {
        var {plotView,allPlots,drawLayersAry}= this.state;
        if (!plotView) return false;
        return (
            <ImageViewerDecorate plotView={plotView}
                                 drawLayersAry={drawLayersAry}
                                 visRoot={allPlots}
                                 extensionList={getExtensionList(plotView.plotId)} />
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
