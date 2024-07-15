/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {MultiImageViewer} from '../../visualize/ui/MultiImageViewer.jsx';
import {DEFAULT_FITS_VIEWER_ID, NewPlotMode} from '../../visualize/MultiViewCntlr.js';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {dispatchChangeExpandedMode, ExpandType} from '../../visualize/ImagePlotCntlr.js';
import {LcImageToolbarView} from './LcImageToolbarView.jsx';

/**
 * A wrapper component for MultiImageViewer where expended mode is supported.
 * @param obj
 * @param obj.viewerId  MultiImageViewer's viewerId.
 * @param obj.imageExpandedMode  if true, then imageExpandedMode overrides everything else
 * @param {boolean} obj.closeable    if true, expanded view can be closed.
 * @param {boolean} obj.insideFlex  true if it's used inside a css flex box.  Defaults to false.
 * @param {number} obj.forceRowSize
 * @param {string} obj.activeTableId
 * @param {Object} obj.Toolbar  the toolbar for the image multi viewer
 * @returns {Object}
 * @constructor
 */
export function LcImageViewerContainer({viewerId=DEFAULT_FITS_VIEWER_ID, imageExpandedMode=false, closeable=true, insideFlex=false,
                                        forceRowSize, activeTableId, Toolbar= LcImageToolbarView}) {
    
    if (imageExpandedMode) {
        return (
            <MultiImageViewer Toolbar={Toolbar}
                              viewerId={viewerId}
                              insideFlex={insideFlex}
                              showWhenExpanded={true}
                              defaultDecoration={false}
                              closeFunc={closeable ? closeExpanded : null}
            />
        );
    } else {
        return ( <MultiImageViewer Toolbar = {Toolbar}
                                   viewerId = {viewerId}
                                   insideFlex= {insideFlex}
                                   forceRowSize={forceRowSize}
                                   canReceiveNewPlots= {NewPlotMode.create_replace.key}
                                   tableId={activeTableId}
            />
        );
    }
}


function closeExpanded() {
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
    dispatchChangeExpandedMode(ExpandType.COLLAPSE);
}

LcImageViewerContainer.propTypes = {
    viewerId: PropTypes.string,
    imageExpandedMode : PropTypes.bool,
    closeable: PropTypes.bool,
    insideFlex: PropTypes.bool,
    forceRowSize: PropTypes.number,
    activeTableId: PropTypes.string,
    Toolbar: PropTypes.element
};

