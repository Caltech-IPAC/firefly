/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {omit} from 'lodash';
import shallowequal from 'shallowequal';
import {getPlotViewById,getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {ImageViewerView} from './ImageViewerView.jsx';
import {visRoot} from '../ImagePlotCntlr.js';
import {extensionRoot} from '../../core/ExternalAccessCntlr.js';
import {currMouseState,MouseState} from '../VisMouseCntlr.js';
import {getExtensionList} from '../../core/ExternalAccessUtils.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {flux} from '../../Firefly.js';



export class ImageViewer extends Component {


    constructor(props) {
        super(props);
        var {plotId}= props;
        var allPlots= visRoot();
        var dlAry= getDlAry();
        var plotView= getPlotViewById(allPlots,plotId);
        var drawLayersAry= getAllDrawLayersForPlot(dlAry,plotId);
        var extRoot= extensionRoot();
        var mousePlotId= currMouseState().plotId;
        this.alive= true;
        this.state= {plotView, dlAry, allPlots, drawLayersAry,extRoot, mousePlotId};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        this.alive= false;
        if (this.removeListener) this.removeListener();
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.storeUpdate();
    }

    storeUpdate() {
        var {state}= this;
        const allPlots= visRoot();
        const dlAry= getDlAry();
        const extRoot= extensionRoot();
        const mState= currMouseState();
        const {plotId}= this.props;
        var mousePlotId= mState.plotId;
        if (this.timeId) clearTimeout(this.timeId);
        this.timeId= null;
        if (mState.mouseState && mState.mouseState===MouseState.EXIT) {
            if (mousePlotId===this.props.plotId) {
                this.timeId= setTimeout( () => this.delayMouseIdClear(), 10000); // 10 seconds
            }
        }

        var drawLayersAry= getAllDrawLayersForPlot(dlAry,this.props.plotId);
        if (shallowequal(drawLayersAry,state.drawLayersAry)) drawLayersAry= state.drawLayersAry;

        if (changeAffectsPV(plotId,allPlots,state.allPlots)  ||
            mousePlotIdAffectPv(plotId,state.mousePlotId,mousePlotId) ||
            extRoot!==state.extRoot ||
            drawLayersAry!==state.drawLayersAry) {
            var plotView= getPlotViewById(allPlots,plotId);
            this.setState({plotView, dlAry, allPlots, drawLayersAry,extRoot,mousePlotId});
        }
    }

    delayMouseIdClear() {
        this.timeId= null;
        var newState= Object.assign({},this.state,{mousePlotId:null});
        if (this.alive && currMouseState().plotId===this.props.plotId) {
            this.setState(newState);
        }

    }



    render() {
        var {plotView,allPlots,drawLayersAry,mousePlotId}= this.state;
        if (!plotView) return false;

        var {plotId}= this.props;
        if (plotView.plotId!==plotId) {
            allPlots= visRoot();
            plotView= getPlotViewById(allPlots,plotId);
            drawLayersAry= getAllDrawLayersForPlot(getDlAry(),this.props.plotId);
            if (!plotView) return false;
        }

        return (
            <ImageViewerView plotView={plotView}
                             drawLayersAry={drawLayersAry}
                             visRoot={allPlots}
                             mousePlotId={mousePlotId}
                             extensionList={getExtensionList(plotId)} />
        );
    }
}

ImageViewer.propTypes= {
    plotId : PropTypes.string.isRequired
};



function drawLayersDiffer(dlAry1, dlAry2) {
    return true;
}


const omitList= ['plotViewAry','activePlotId'];

function changeAffectsPV(plotId,newVisRoot,oldVisRoot) {
    if (newVisRoot===oldVisRoot) return false;

    if (newVisRoot.activePlotId!==oldVisRoot.activePlotId &&
        (newVisRoot.activePlotId===plotId || oldVisRoot.activePlotId===plotId)) {
        return true;
    }

    const pv1= getPlotViewById(newVisRoot,plotId);
    const pv2= getPlotViewById(oldVisRoot,plotId);

    if (pv1!==pv2) return true;


    if (!shallowequal(omit(newVisRoot,omitList), omit(oldVisRoot,omitList))) {
        return true;
    }
    return false;

}

function mousePlotIdAffectPv(plotId,oldMousePlotId,newMousePlotId) {
    return (oldMousePlotId!==newMousePlotId && (oldMousePlotId===plotId || newMousePlotId===plotId));
}



