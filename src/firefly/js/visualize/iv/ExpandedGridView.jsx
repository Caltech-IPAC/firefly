/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import numeral from 'numeral';
import {ExpandedTools} from './ExpandedTools.jsx';
import {getPlotGroupById}  from '../PlotGroup.js';
import {RotateType}  from '../PlotState.js';
import {visRoot, ExpandType} from '../ImagePlotCntlr.js';
import {convertZoomToString} from '../ZoomUtil.js';
import PlotViewUtil from '../PlotViewUtil.js';
import './ImageViewerDecorate.css';

export function ExpandedGridView({allPlots}) {
    if (allPlots.expandedMode===ExpandType.COLLAPSE) return <div></div>;
    return (
        <div>
            <ExpandedTools  allPlots={allPlots}/>
            {`Hello, this is grid view, plotView count: ${allPlots.plotViewAry.length}`}
        </div>
    );
}

//{makeInlineTitle(visRoot,pv)}

ExpandedGridView.propTypes= {
    allPlots : PropTypes.object.isRequired
};

