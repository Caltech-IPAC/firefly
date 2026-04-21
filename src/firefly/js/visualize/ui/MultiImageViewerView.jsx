/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack} from '@mui/joy';
import React, {useRef} from 'react';
import {object, arrayOf, bool, func, number, oneOf, string, elementType, any} from 'prop-types';
import {omit} from 'lodash';
import {checkProps} from '../../ui/SimpleComponent';
import {SINGLE, GRID, getMultiViewRoot, getViewer} from '../MultiViewCntlr.js';
import {getPlotViewById, getPlotViewProxyById, primePlot} from '../PlotViewUtil.js';
import {MultiItemViewerView} from './MultiItemViewerView.jsx';
import {ImageViewer, ImageViewerPlaceHolder} from '../iv/ImageViewer';
import {useMouseStoreConnector} from 'firefly/visualize/ui/MouseStoreConnector.jsx';
import {lastMouseCtx, lastMouseImageReadout} from 'firefly/visualize/VisMouseSync.js';
import {isLockByClick, readoutRoot} from 'firefly/visualize/MouseReadoutCntlr.js';
import {MouseReadoutBottomLine} from 'firefly/visualize/ui/MouseReadoutBottomLine.jsx';
import {isDialogVisible} from 'firefly/core/ComponentCntlr.js';
import {MOUSE_READOUT_DIALOG_ID} from 'firefly/visualize/ui/MouseReadPopoutAll.jsx';
import {getFluxRadix} from 'firefly/visualize/ui/MouseReadoutUIUtil';


function makeState() {
    return {
        readoutData:lastMouseImageReadout(),
        readout:readoutRoot(),
        readoutShowing:!isDialogVisible(MOUSE_READOUT_DIALOG_ID)
    };
}


function makeViewer(visRoot, plotId, makeToolbar, makeLegend, showWhenExpanded, PlotViewProxy) {
    if (getPlotViewById(visRoot, plotId)) {
        return ( <ImageViewer {...{plotId, key:plotId, makeToolbar, makeLegend, showWhenExpanded}} /> );
    }
    const proxy= getPlotViewProxyById(visRoot, plotId);
    return proxy ? <PlotViewProxy {...{...proxy}}/> : <div/>;
}

export function MultiImageViewerView(props)  {
    checkProps(props, MultiImageViewerView);

    const {current:elementWrapper}= useRef({element:undefined});
    const {readout, readoutData, readoutShowing}= useMouseStoreConnector(makeState);
    const {Toolbar, Legend, PlotViewProxy= ImageViewerPlaceHolder, visRoot, viewerPlotIds=[],
        showWhenExpanded=false, mouseReadoutEmbedded=true,
        handleToolbar=true, layoutType=GRID, scrollGrid=false, viewerId, ref}= props;

    const viewer= getViewer(getMultiViewRoot(),viewerId) ?? {};
    const {bottomUIComponent}= viewer ?? {};

    const makeToolbar = Toolbar && (() => (<Toolbar {...{...props, containerElement:elementWrapper?.element}} />));
    const makeLegend =  Legend && ((plotId) => (<Legend {...{...props,plotId}}/>));
    const makeItemViewer = (plotId) =>
        makeViewer(visRoot, plotId, makeToolbar, makeLegend, showWhenExpanded, PlotViewProxy);

    const newProps = {...omit(props, ['Toolbar', 'visRoot', 'viewerPlotIds', 'showWhenExpanded']),
        activeItemId: visRoot.activePlotId, viewerItemIds: viewerPlotIds,
        makeToolbar:handleToolbar?makeToolbar:undefined, makeItemViewer, makeItemViewerFull:makeItemViewer};

    const insideStyle= props.insideFlex ? {flex:'1 1 auto', maxWidth:'100%'} : {width:'100%', height:'100%'};
    const style= { display: 'flex', flexDirection: 'column', position: 'relative', ...insideStyle, ...props.style };

    const pvToUseForReadout= isLockByClick(readout) ? primePlot(visRoot) : primePlot(visRoot,lastMouseCtx().plotId);
    const one= layoutType===SINGLE || viewerPlotIds?.length===1;
    const sStyle= {
        position: 'absolute',
        left: 3,
        bottom:one ? 2 : 3, 
        right: one ? 3 : scrollGrid? 15 : 6, 
    };

    return (
        <div className='MultiImageViewer' style={style} ref={(e) => elementWrapper.element= e}>
            <MultiItemViewerView {...{...newProps, ref, insideFlex:true, style:props.style}} />
            <Stack spacing={one?1:undefined} style={mouseReadoutEmbedded? sStyle:{}} >
                {bottomUIComponent?.()}
                <MouseReadoutBottomLine {...{readout, readoutData, scrollGrid,
                                        readoutShowing: readoutShowing && viewerPlotIds.includes(readoutData?.plotId),
                                        showOnInactive:!mouseReadoutEmbedded,
                                        radix: getFluxRadix(readout.readoutPref, pvToUseForReadout),
                                        slightlyTransparent: mouseReadoutEmbedded  }}/>
            </Stack>
        </div>
    );
}

MultiImageViewerView.propTypes= {
    visRoot : object,
    viewerPlotIds : arrayOf(string).isRequired,
    showWhenExpanded : bool,
    Toolbar : elementType,
    Legend : elementType,
    PlotViewProxy: elementType,
    viewerId : string.isRequired,
    style : object,
    defaultDecoration : bool,
    layoutType : oneOf([GRID,SINGLE]),
    forceRowSize : number,   //optional - force a certain number of rows
    forceColSize : number,  //optional - force a certain number of columns
    gridDefFunc : func,  // optional - a function to return the grid definition
    gridComponent : object,  // a React element to define the grid - not implemented, just an idea
    insideFlex :  bool,
    handleToolbar: bool,
    scrollGrid: bool,
    mouseReadoutEmbedded: bool,
    ref: any,
};
