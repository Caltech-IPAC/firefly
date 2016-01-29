/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {ExpandedTools} from './ExpandedTools.jsx';
import numeral from 'numeral';
import {getActivePlotView} from '../PlotViewUtil.js';
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
import {ImageViewer} from './../iv/ImageViewer.jsx';

export function ExpandedSingleView({allPlots}) {
    if (allPlots.expandedMode===ExpandType.COLLAPSE) return <div></div>;
    var pv= getActivePlotView(allPlots);

    if (!pv || !pv.primaryPlot) return <div></div>;


    return (
        <div className='ExpandedSingleView' style={{flex:'auto', position:'relative'}}>
            <ExpandedTools  allPlots={allPlots}/>
            <div style={{position:'absolute', top:70,left:0,right:0,bottom:0}}>
                <ImageViewer plotId={pv.plotId}/>
            </div>


        </div>
    );
}
//{`Hello, this is single view, plotView count: ${allPlots.plotViewAry.length}`}
//
//{makeInlineTitle(visRoot,pv)}

ExpandedSingleView.propTypes= {
    allPlots : PropTypes.object.isRequired
};

