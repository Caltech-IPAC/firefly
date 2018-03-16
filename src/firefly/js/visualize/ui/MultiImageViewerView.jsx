/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import {omit} from 'lodash';
import {SINGLE, GRID} from '../MultiViewCntlr.js';
import {MultiItemViewerView} from './MultiItemViewerView.jsx';
import {ImageViewer} from './../iv/ImageViewer.jsx';


export function MultiImageViewerView(props) {

    const {Toolbar, visRoot, viewerPlotIds, showWhenExpanded=false,
        handleInlineToolsWhenSingle=true, inlineTitle= true, aboveTitle=false}= props;

    const makeItemViewer = (plotId) =>  {
        return (
            <ImageViewer plotId={plotId} key={plotId}
                         handleInlineTools={false} {...{showWhenExpanded, inlineTitle, aboveTitle}} />
        );
    };

    const makeItemViewerFull = (plotId) => (
        <ImageViewer plotId={plotId} key={plotId}
                     handleInlineTools={false} {...{
                         showWhenExpanded, inlineTitle, aboveTitle,
                         handleInlineTools: viewerPlotIds.length===1 && handleInlineToolsWhenSingle
                     }
                     } />
    );

    const makeToolbar = Toolbar ? () => (<Toolbar {...props} />) : undefined;

    const newProps = Object.assign(omit(props, ['Toolbar', 'visRoot', 'viewerPlotIds', 'showWhenExpanded']),
        {activeItemId: visRoot.activePlotId, viewerItemIds: viewerPlotIds, makeToolbar, makeItemViewer, makeItemViewerFull});


    return (
        <MultiItemViewerView {...newProps} />
    );
}

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
    handleInlineToolsWhenSingle :  PropTypes.bool,
    inlineTitle: PropTypes.bool,
    aboveTitle: PropTypes.bool
};





