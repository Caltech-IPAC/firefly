/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {ExpandedTools, EXPANDED_TOOL_HEIGHT} from './ExpandedTools.jsx';
import numeral from 'numeral';
import {getActivePlotView} from '../PlotViewUtil.js';
import {getPlotGroupById}  from '../PlotGroup.js';
import {visRoot, ExpandType} from '../ImagePlotCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {ImageViewer} from './../iv/ImageViewer.jsx';

export function ExpandedSingleView({visRoot}) {
    if (visRoot.expandedMode===ExpandType.COLLAPSE) return <div></div>;
    var pv= getActivePlotView(visRoot);

    if (!primePlot(pv)) return <div></div>;


    return (
        <div className='ExpandedSingleView' style={{flex:'auto', position:'relative'}}>
            <ExpandedTools  visRoot={visRoot}/>
            <div style={{position:'absolute', top:EXPANDED_TOOL_HEIGHT,left:0,right:0,bottom:0}}>
               <ImageViewer plotId={pv.plotId}/>
            </div>


        </div>
    );
}
//{`Hello, this is single view, plotView count: ${allPlots.plotViewAry.length}`}
//
//{makeInlineTitle(visRoot,pv)}

ExpandedSingleView.propTypes= {
    visRoot : PropTypes.object.isRequired
};

