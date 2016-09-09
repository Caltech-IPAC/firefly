/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {ExpandedModeDisplay} from '../iv/ExpandedModeDisplay.jsx';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {DEFAULT_FITS_VIEWER_ID, NewPlotMode} from '../MultiViewCntlr.js';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';

/**
 * A wrapper component for MultiImageViewer where expended mode is supported.
 * @param viewerId  MultiImageViewer's viewerId.
 * @param imageExpandedMode  if true, then imageExpandedMode overrides everything else
 * @param {boolean} closeable    if true, expanded view can be closed.
 * @param {boolean} insideFlex  true if it's used inside a css flex box.  Defaults to false.
 * @returns {Object}
 * @constructor
 */
export function MultiImageViewerContainer({viewerId, imageExpandedMode=false, closeable=true, insideFlex=false}) {
    
    if (imageExpandedMode) {
        return  ( <ExpandedModeDisplay
                        key='results-plots-expanded'
                        forceExpandedMode={true}
                        closeFunc={closeable ? closeExpanded : null}/>
                );
    } else {
        return ( <MultiImageViewer viewerId = {viewerId}
                        insideFlex = {insideFlex}
                        canReceiveNewPlots = {NewPlotMode.create_replace.key}
                        Toolbar = {MultiViewStandardToolbar}/>
        );
    }
}


function closeExpanded() {
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
}

MultiImageViewerContainer.propTypes = {
    viewerId: PropTypes.string,
    imageExpandedMode : PropTypes.bool,
    closeable: PropTypes.bool,
    insideFlex: PropTypes.bool,
};

MultiImageViewerContainer.defaultProps = {
    viewerId: DEFAULT_FITS_VIEWER_ID
};
