/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {getActivePlotView,
    getDrawLayerByType,
    getPlotViewById,
    isDrawLayerAttached,
    getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {ToolbarButton, ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {LayerButton} from './VisToolbarView.jsx';

import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
import DELETE from 'html/images/blue_delete_10x10.png';




function expand(pv) {
    console.log('todo- expand:' + pv.primaryPlot.title);
}
function deletePlot(pv) {
    console.log('todo- delete:' + pv.primaryPlot.title);
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
        display: 'inline-block',
        position: 'relative',
        verticalAlign: 'top',
        whiteSpace: 'nowrap'
    };

    return (
        <div style={rS}>
            <LayerButton plotView={pv} dlAry={dlAry} visible={showLayer}/>

            <ToolbarButton icon={OUTLINE_EXPAND}
                           tip='Expand this panel to take up a larger area'
                           horizontal={true}
                           visible={showExpand}
                           onClick={() => expand(pv)}/>

            <div style={{ position: 'relative', display: 'inline-block', float: 'right', }}>
                <ToolbarButton icon={DELETE}
                               tip='Delete Image'
                               horizontal={true}
                               visible={showDelete}
                               onClick={() => deletePlot(pv)}/>
            </div>

        </div>
    );
}


VisInlineToolbarView.propTypes= {
    plotView : PropTypes.object.isRequired,
    dlAry : PropTypes.arrayOf(React.PropTypes.object),
    extensionAry : PropTypes.arrayOf(React.PropTypes.object),
    showLayer : PropTypes.bool,
    showExpand : PropTypes.bool,
    showDelete : PropTypes.bool
};







