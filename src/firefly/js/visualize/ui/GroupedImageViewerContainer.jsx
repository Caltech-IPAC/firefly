/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {DEFAULT_FITS_VIEWER_ID, NewPlotMode} from '../MultiViewCntlr.js';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {dispatchChangeExpandedMode, ExpandType} from '../ImagePlotCntlr.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {getPlotViewById} from './../PlotViewUtil.js';

/**
 * A wrapper component for a group grid view that supported expanded mode
 */
export class GroupedImageViewerContainer extends PureComponent {

    constructor(props) {
        super(props);
        this.state = {viewer: null};
        this.layoutGrid= this.layoutGrid.bind(this);
    }

    layoutGrid(plotIdAry) {
        const {gridDefs, size= 200}= this.props;
        const vr= visRoot();
        const pvAry= plotIdAry.map( (plotId) => getPlotViewById(vr, plotId));

        return gridDefs
            .map( (r) => {
                const plotIdAry= pvAry
                    .filter( (pv) => pv && pv.drawingSubGroupId===r.subgroup)
                    .map( (pv) => pv.plotId);
                return {title: r.title, noDataMessage: r.noDataMessage, plotIdAry, size};
            } );
            // .filter( (rAry) => rAry.plotIdAry.length);
    }

    render() {
        const {viewerId, imageExpandedMode=false, closeable=true, insideFlex=false,
               sparseGridTitleLocation= 'top', threeColorOn,
               Toolbar= MultiViewStandardToolbar}= this.props;

        if (imageExpandedMode) {
            return (
                <MultiImageViewer
                    viewerId = {viewerId}
                    insideFlex= {insideFlex}
                    canReceiveNewPlots= {NewPlotMode.create_replace.key}
                    handleInlineToolsWhenSingle= {false}
                    Toolbar = {Toolbar}
                    showWhenExpanded={true}
                    threeColorOn={threeColorOn}
                    defaultDecoration={false}
                    closeFunc={closeable ? closeExpanded : null}
                    sparseGridTitleLocation={sparseGridTitleLocation}
                    gridDefFunc= {this.layoutGrid}
                />
            );
        } else {
            return (
                <MultiImageViewer
                    viewerId = {viewerId}
                    insideFlex= {insideFlex}
                    canReceiveNewPlots= {NewPlotMode.create_replace.key}
                    handleInlineToolsWhenSingle= {false}
                    threeColorOn={threeColorOn}
                    Toolbar = {Toolbar}
                    sparseGridTitleLocation={sparseGridTitleLocation}
                    gridDefFunc= {this.layoutGrid}
                />
            );
        }

    }
}


function closeExpanded() {
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
    dispatchChangeExpandedMode(ExpandType.COLLAPSE);
}

GroupedImageViewerContainer.propTypes = {
    viewerId: PropTypes.string,
    imageExpandedMode : PropTypes.bool,
    closeable: PropTypes.bool,
    insideFlex: PropTypes.bool,
    Toolbar: PropTypes.func,
    gridDefs: PropTypes.arrayOf(PropTypes.object),
    size : PropTypes.number,
    threeColorOn : PropTypes.bool,
    sparseGridTitleLocation : PropTypes.oneOf(['top', 'left', 'off', ''])
};

GroupedImageViewerContainer.defaultProps = {
    viewerId: DEFAULT_FITS_VIEWER_ID
};
