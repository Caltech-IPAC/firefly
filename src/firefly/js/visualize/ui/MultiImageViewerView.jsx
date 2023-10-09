/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {forwardRef} from 'react';
import PropTypes from 'prop-types';
import {omit} from 'lodash';
import {getBixPix} from '../FitsHeaderUtil.js';
import {SINGLE, GRID} from '../MultiViewCntlr.js';
import {getPlotViewById, primePlot} from '../PlotViewUtil.js';
import {MultiItemViewerView} from './MultiItemViewerView.jsx';
import {ImageViewer} from './../iv/ImageViewer.jsx';
import {useMouseStoreConnector} from 'firefly/visualize/ui/MouseStoreConnector.jsx';
import {lastMouseCtx, lastMouseImageReadout} from 'firefly/visualize/VisMouseSync.js';
import {isLockByClick, readoutRoot} from 'firefly/visualize/MouseReadoutCntlr.js';
import {MouseReadoutBottomLine} from 'firefly/visualize/ui/MouseReadoutBottomLine.jsx';
import {isDialogVisible} from 'firefly/core/ComponentCntlr.js';
import {MOUSE_READOUT_DIALOG_ID} from 'firefly/visualize/ui/MouseReadPopoutAll.jsx';


function makeState() {
    return {
        readoutData:lastMouseImageReadout(),
        readout:readoutRoot(),
        readoutShowing:!isDialogVisible(MOUSE_READOUT_DIALOG_ID)
    };
}

export const MultiImageViewerView = forwardRef( (props, ref) => {

    const {readout, readoutData, readoutShowing}= useMouseStoreConnector(makeState);
    const {Toolbar, visRoot, viewerPlotIds=[], showWhenExpanded=false, mouseReadoutEmbedded=true,
        inlineTitle= true, aboveTitle=false, layoutType=GRID}= props;

    const makeItemViewer = (plotId) =>  {
        return (
            <ImageViewer plotId={plotId} key={plotId}
                         {...{showWhenExpanded, inlineTitle, aboveTitle}} />
        );
    };

    const makeItemViewerFull = (plotId) => (
        <ImageViewer plotId={plotId} key={plotId}
                     {...{ showWhenExpanded, inlineTitle, aboveTitle, } } />
    );

    const makeToolbar = Toolbar ? () => (<Toolbar {...props} />) : undefined;
    const doReadoutAndShowing= readoutShowing && viewerPlotIds.includes(readoutData?.plotId);

    const newProps = Object.assign(omit(props, ['Toolbar', 'visRoot', 'viewerPlotIds', 'showWhenExpanded']),
        {activeItemId: visRoot.activePlotId, viewerItemIds: viewerPlotIds, makeToolbar, makeItemViewer, makeItemViewerFull});

    let style= {display:'flex', flexDirection:'column', position:'relative'};
    if (props.insideFlex) {
        style= {...style, flex:'1 1 auto', maxWidth:'100%'};
    }
    else {
        style=  {...style, width:'100%', height:'100%'};
    }
    
    const {readoutPref}= readoutRoot();
    const pvToUse= isLockByClick(readoutRoot()) ? primePlot(visRoot) : getPlotViewById(visRoot,lastMouseCtx().plotId);
    const radix= Number(getBixPix(primePlot(pvToUse))>0 ? readoutPref.intFluxValueRadix : readoutPref.floatFluxValueRadix);

    if (layoutType===SINGLE || viewerPlotIds?.length===1) {
        return (
            <div style={style}>
                <MultiItemViewerView {...{...newProps, ref, insideFlex:true, style:props.style}} />
                <MouseReadoutBottomLine readout={readout} readoutData={readoutData}
                                        slightlyTransparent={mouseReadoutEmbedded}
                                        readoutShowing={doReadoutAndShowing}
                                        showOnInactive={!mouseReadoutEmbedded}
                                        radix={radix}
                                        style={mouseReadoutEmbedded? {position:'absolute', left:0, right:1, bottom:3, margin:'0 3px 0 3px'}:{}} />
            </div>
        );
    }
    else {
        return (
            <div style={style}>
                <MultiItemViewerView {...{...newProps, ref, insideFlex:true, style:props.style}} />
                <MouseReadoutBottomLine readout={readout} readoutData={readoutData}
                                        style={mouseReadoutEmbedded?{position:'absolute', left:4, bottom:4, right:4}:{}}
                                        readoutShowing={doReadoutAndShowing}
                                        showOnInactive={!mouseReadoutEmbedded}
                                        radix={radix}
                                        slightlyTransparent={mouseReadoutEmbedded}
                />
            </div>
        );
    }
});

MultiImageViewerView.propTypes= {
    visRoot : PropTypes.object,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    showWhenExpanded : PropTypes.bool,

    Toolbar : PropTypes.func,
    viewerId : PropTypes.string.isRequired,
    style : PropTypes.object,
    defaultDecoration : PropTypes.bool,
    layoutType : PropTypes.oneOf([GRID,SINGLE]),
    forceRowSize : PropTypes.number,   //optional - force a certain number of rows
    forceColSize : PropTypes.number,  //optional - force a certain number of columns
    gridDefFunc : PropTypes.func,  // optional - a function to return the grid definition
    gridComponent : PropTypes.object,  // a react element to define the grid - not implemented, just an idea
    insideFlex :  PropTypes.bool,
    inlineTitle: PropTypes.bool,
    aboveTitle: PropTypes.bool,
    mouseReadoutEmbedded: PropTypes.bool
};





