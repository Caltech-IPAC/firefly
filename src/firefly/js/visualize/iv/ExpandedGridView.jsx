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
import {VisCtxToolbarView} from './../ui/VisCtxToolbarView.jsx';
import {VisInlineToolbarView} from './../ui/VisInlineToolbarView.jsx';
import PlotViewUtil from '../PlotViewUtil.js';
import {ImageViewerView}  from './ImageViewerView.jsx';
import {PlotAttribute} from '../WebPlot.js';
import {AnnotationOps} from '../WebPlotRequest.js';
import BrowserInfo from '../../util/BrowserInfo.js';
import {AREA_SELECT,LINE_SELECT,POINT} from '../PlotCmdExtension.js';
import {getTaskCount} from '../../core/AppDataCntlr.js';
import './ImageViewerDecorate.css';
import LOADING from 'html/images/gxt/loading.gif';

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

