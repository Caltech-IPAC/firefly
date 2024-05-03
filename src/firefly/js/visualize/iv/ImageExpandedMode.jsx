/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {memo, useEffect} from 'react';
import PropTypes from 'prop-types';
import {visRoot, dispatchChangeExpandedMode, ExpandType, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {
    getMultiViewRoot, getViewer, getExpandedViewerItemIds, EXPANDED_MODE_RESERVED, DEFAULT_FITS_VIEWER_ID
} from '../MultiViewCntlr.js';
import {ExpandedTools} from './ExpandedTools.jsx';
import {MultiImageViewerView} from '../ui/MultiImageViewerView.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {getActivePlotView, getPlotViewAry, isImageExpanded} from 'firefly/visualize/PlotViewUtil.js';

export const ImageExpandedMode= memo(({closeFunc,insideFlex=true,viewerId, forceExpandedMode=true}) => {

    const vr            = useStoreConnector(visRoot);
    const multiViewRoot = useStoreConnector(getMultiViewRoot);
    useEffect(() => {
        if (!getActivePlotView(vr)?.plotViewCtx?.useForSearchResults) {
            const pinnedAry= getPlotViewAry(vr,DEFAULT_FITS_VIEWER_ID);
            if (pinnedAry.length) dispatchChangeActivePlotView(pinnedAry[0].plotId);
        }
        forceExpandedMode && !isImageExpanded(vr.expandedMode) && dispatchChangeExpandedMode(true);
        return () => forceExpandedMode && dispatchChangeExpandedMode(ExpandType.COLLAPSE);
    },[]);

    if (vr.expandedMode===ExpandType.COLLAPSE) return false;
    const layoutType= vr.expandedMode===ExpandType.GRID ? 'grid' : 'single';
    const viewer=  getViewer(getMultiViewRoot(),EXPANDED_MODE_RESERVED);
    const foundViewerId= viewerId || viewer?.viewerId;
    return (
        <MultiImageViewerView viewerPlotIds={getExpandedViewerItemIds(multiViewRoot)}
                              layoutType={layoutType} Toolbar={ExpandedTools}
                              viewerId={foundViewerId} visRoot={vr}
                              scrollGrid={viewer?.scroll ?? false}
                              style={{flex:'1 1 auto', marginBottom:1,marginLeft:1}} closeFunc={closeFunc}
                              defaultDecoration={false} showWhenExpanded={true}
                              insideFlex={insideFlex}
        />
    );
});

ImageExpandedMode.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    insideFlex: PropTypes.bool,
    viewerId: PropTypes.string
};
