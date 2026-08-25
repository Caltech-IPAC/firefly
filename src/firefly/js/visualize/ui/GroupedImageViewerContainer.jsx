/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useCallback} from 'react';
import PropTypes from 'prop-types';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {DEFAULT_FITS_VIEWER_ID, ExpandType, NewPlotMode} from '../VisConst';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {dispatchChangeExpandedMode} from '../ImagePlotDispatch';
import {currentP} from '../PlotViewUtil';

/**
 * A wrapper component for a group grid view that supports expanded mode
 * @param {Object} props see GroupedImageViewerContainer.propTypes
 */
export const GroupedImageViewerContainer = memo(function GroupedImageViewerContainer(props) {
    const {viewerId=DEFAULT_FITS_VIEWER_ID, imageExpandedMode=false, closeable=true, insideFlex=false,
        threeColorOn, style, sparseGridTitleLocation='top', Toolbar=MultiViewStandardToolbar,
        size=200, gridDefs} = props;

    const layoutGrid = useCallback((plotIdAry) => {
        const pvAry = plotIdAry.map((plotId) => currentP(plotId).pv);

        return gridDefs.map((r) => {
            const plotIdAry = pvAry
                .filter((pv) => pv && pv.drawingSubGroupId === r.subgroup)
                .map((pv) => pv.plotId);
            return {title: r.title, noDataMessage: r.noDataMessage, plotIdAry, size};
        });
    }, [gridDefs, size]);

    if (imageExpandedMode) {
        return (
            <MultiImageViewer
                style={style}
                viewerId={viewerId}
                insideFlex={insideFlex}
                canReceiveNewPlots={NewPlotMode.create_replace.key}
                Toolbar={Toolbar}
                showWhenExpanded={true}
                threeColorOn={threeColorOn}
                mouseReadoutEmbedded={false}
                defaultDecoration={false}
                closeFunc={closeable ? closeExpanded : undefined}
                sparseGridTitleLocation={sparseGridTitleLocation}
                gridDefFunc={layoutGrid}
            />
        );
    } else {
        return (
            <MultiImageViewer
                style={style}
                viewerId={viewerId}
                insideFlex={insideFlex}
                canReceiveNewPlots={NewPlotMode.create_replace.key}
                mouseReadoutEmbedded={false}
                threeColorOn={threeColorOn}
                Toolbar={Toolbar}
                sparseGridTitleLocation={sparseGridTitleLocation}
                gridDefFunc={layoutGrid}
            />
        );
    }
});

function closeExpanded() {
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
    dispatchChangeExpandedMode(ExpandType.COLLAPSE);
}

GroupedImageViewerContainer.propTypes = {
    viewerId: PropTypes.string,
    imageExpandedMode: PropTypes.bool,
    closeable: PropTypes.bool,
    insideFlex: PropTypes.bool,
    Toolbar: PropTypes.func,
    gridDefs: PropTypes.arrayOf(PropTypes.object),
    size: PropTypes.number,
    threeColorOn: PropTypes.bool,
    style: PropTypes.object,
    sparseGridTitleLocation: PropTypes.oneOf(['top', 'left', 'off', '']),
};
