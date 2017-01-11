/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {MultiViewStandardToolbar} from '../../visualize/ui/MultiViewStandardToolbar.jsx';
import {MultiImageViewer} from '../../visualize/ui/MultiImageViewer.jsx';
import {DEFAULT_FITS_VIEWER_ID, NewPlotMode} from '../../visualize/MultiViewCntlr.js';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {dispatchChangeExpandedMode, ExpandType} from '../../visualize/ImagePlotCntlr.js';

/**
 * A wrapper component for MultiImageViewer where expended mode is supported.
 * @param obj
 * @param obj.viewerId  MultiImageViewer's viewerId.
 * @param obj.imageExpandedMode  if true, then imageExpandedMode overrides everything else
 * @param {boolean} obj.closeable    if true, expanded view can be closed.
 * @param {boolean} obj.insideFlex  true if it's used inside a css flex box.  Defaults to false.
 * @param {Object} obj.Toolbar  the toolbar for the image multi viewer
 * @returns {Object}
 * @constructor
 */
export function LcImageViewerContainer({viewerId, imageExpandedMode=false, closeable=true, insideFlex=false,
                                           forceRowSize, Toolbar= MultiViewStandardToolbar}) {
    
    if (imageExpandedMode) {
        return (
            <MultiImageViewer Toolbar={Toolbar}
                              viewerId={viewerId}
                              closeFunc={closeable ? closeExpanded : null}
                              showWhenExpanded={true}
                              defaultDecoration={false}
                              insideFlex={insideFlex}
            />
        );
    } else {
        return ( <MultiImageViewer
                        viewerId = {viewerId}
                        insideFlex= {insideFlex}
                        forceRowSize={forceRowSize}
                        canReceiveNewPlots= {NewPlotMode.create_replace.key}
                        handleInlineToolsWhenSingle= {false}
                        Toolbar = {Toolbar}
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
};

LcImageViewerContainer.defaultProps = {
    viewerId: DEFAULT_FITS_VIEWER_ID
};
