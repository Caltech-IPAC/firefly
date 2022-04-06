/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {memo, useEffect} from 'react';
import PropTypes from 'prop-types';
import {visRoot, dispatchChangeExpandedMode, ExpandType} from '../ImagePlotCntlr.js';
import {getMultiViewRoot, getViewer, getExpandedViewerItemIds, EXPANDED_MODE_RESERVED} from '../MultiViewCntlr.js';
import {ExpandedTools} from './ExpandedTools.jsx';
import {MultiImageViewerView} from '../ui/MultiImageViewerView.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {isImageExpanded} from 'firefly/visualize/PlotViewUtil.js';

export const ImageExpandedMode= memo(({closeFunc,insideFlex=true,viewerId, forceExpandedMode=true}) => {

    const vr            = useStoreConnector(visRoot);
    const multiViewRoot = useStoreConnector(getMultiViewRoot);
    useEffect(() => {
        forceExpandedMode && !isImageExpanded(vr.expandedMode) && dispatchChangeExpandedMode(true);
        return () => forceExpandedMode && dispatchChangeExpandedMode(ExpandType.COLLAPSE);
    },[]);

    if (vr.expandedMode===ExpandType.COLLAPSE) return false;
    const layoutType= vr.expandedMode===ExpandType.GRID ? 'grid' : 'single';
    const foundViewerId= viewerId || getViewer(getMultiViewRoot(),EXPANDED_MODE_RESERVED)?.viewerId;
    return (
        <MultiImageViewerView viewerPlotIds={getExpandedViewerItemIds(multiViewRoot)}
                              layoutType={layoutType} Toolbar={ExpandedTools}
                              viewerId={foundViewerId} visRoot={vr}
                              style={{flex:'1 1 auto'}} closeFunc={closeFunc}
                              defaultDecoration={false} showWhenExpanded={true}
                              inlineTitle={true} aboveTitle={false} insideFlex={insideFlex}
        />
    );
});

ImageExpandedMode.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    insideFlex: PropTypes.bool,
    viewerId: PropTypes.string
};
