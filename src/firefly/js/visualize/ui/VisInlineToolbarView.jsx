/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {primePlot } from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {LayerButton} from './VisToolbarView.jsx';
import {dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {dispatchChangeExpandedMode, dispatchDeletePlotView} from '../ImagePlotCntlr.js';
import {LO_EXPANDED} from '../../core/LayoutCntlr.js';

import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
import DELETE from 'html/images/blue_delete_10x10.png';




function expand(pv) {
    //console.log('todo- expand:' + primePlot(pv).title);
    dispatchSetLayoutMode( LO_EXPANDED.images );
    dispatchChangeExpandedMode(true);
}


/**
 *
 * @param pv
 * @param dlAry
 * @param showLayer
 * @param showExpand
 * @param showDelete
 * @return {XML}
 * @constructor
 */
export function VisInlineToolbarView({plotView:pv, dlAry, showLayer, showExpand, showDelete}) {

    var rS= {
        width: '100% - 2px',
        height: 34,
        position: 'relative',
        verticalAlign: 'top',
        whiteSpace: 'nowrap',
        display:'inline-flex',
        flexDirection:'row',
        flexWrap:'nowrap',
        alignItems: 'center'
    };

    return (
        <div style={rS}>
            <LayerButton plotView={pv} dlAry={dlAry} visible={showLayer}/>

            <ToolbarButton icon={OUTLINE_EXPAND}
                           tip='Expand this panel to take up a larger area'
                           horizontal={true}
                           visible={showExpand}
                           onClick={() => expand(pv)}/>

            <ToolbarButton icon={DELETE}
                           tip='Delete Image'
                           additionalStyle={{alignSelf:'flex-start'}}
                           horizontal={true}
                           visible={showDelete}
                           onClick={() => dispatchDeletePlotView(pv.plotId)}/>

        </div>
    );
}

// <div style={{ position: 'relative', display: 'inline-block', float: 'right' }}>
// </div>

VisInlineToolbarView.propTypes= {
    plotView : PropTypes.object.isRequired,
    dlAry : PropTypes.arrayOf(React.PropTypes.object),
    extensionAry : PropTypes.arrayOf(React.PropTypes.object),
    showLayer : PropTypes.bool,
    showExpand : PropTypes.bool,
    showDelete : PropTypes.bool
};







