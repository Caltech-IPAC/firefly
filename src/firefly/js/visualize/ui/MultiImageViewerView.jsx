/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {omit} from 'lodash';
import {SINGLE, GRID} from '../MultiViewCntlr.js';
import {MultiItemViewerView} from './MultiItemViewerView.jsx';
import {ImageViewer} from './../iv/ImageViewer.jsx';



export function MultiImageViewerView(props) {

    const {Toolbar, visRoot, viewerPlotIds, showWhenExpanded=false}= props;

    const makeItemViewer = (plotId) => (
        <ImageViewer plotId={plotId} key={plotId}
                                 handleInlineTools={false} {...{showWhenExpanded}} />
    );

    const makeItemViewerFull = (plotId) => (
        <ImageViewer plotId={plotId} key={plotId}
                     handleInlineTools={false} {...{showWhenExpanded, handleInlineTools: viewerPlotIds.length===1}} />
    );

    const makeToolbar = Toolbar ? () => (<Toolbar {...props} />) : undefined;

    const newProps = Object.assign(omit(props, ['Toolbar', 'visRoot', 'viewerPlotIds', 'showWhenExpanded']),
        {activeItemId: visRoot.activePlotId, viewerItemIds: viewerPlotIds, makeToolbar, makeItemViewer, makeItemViewerFull});


    return (
        <MultiItemViewerView {...newProps} />
    );
}

//{Toolbar ? <div style={flexContainerStyle}><Toolbar/> </div> : ''}

MultiImageViewerView.propTypes= {
    visRoot : PropTypes.object,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    showWhenExpanded : PropTypes.bool,

    Toolbar : PropTypes.func,
    viewerId : PropTypes.string.isRequired,
    additionalStyle : PropTypes.object,    
    defaultDecoration : PropTypes.bool,
    layoutType : PropTypes.oneOf([GRID,SINGLE]),
    forceRowSize : PropTypes.number,   //optional - force a certain number of rows
    forceColSize : PropTypes.number,  //optional - force a certain number of columns
    gridDefFunc : PropTypes.func,  // optional - a function to return the grid definition
    gridComponent : PropTypes.object,  // a react element to define the grid - not implemented, just an idea
    insideFlex :  PropTypes.bool
};

