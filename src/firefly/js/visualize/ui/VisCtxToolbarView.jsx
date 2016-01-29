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


import {makeExtActivateData} from '../PlotCmdExtension.js';
import {dispatchExtensionActivate} from '../../core/ExternalAccessCntlr.js';


import CROP from 'html/images/icons-2014/24x24_Crop.png';
import STATISTICS from 'html/images/icons-2014/24x24_Statistics.png';
import SELECTED from 'html/images/icons-2014/24x24_Checkmark.png';
import UNSELECTED from 'html/images/icons-2014/24x24_CheckmarkOff_Circle.png';
import FILTER from 'html/images/icons-2014/24x24_FilterAdd.png';




function crop(pv) {
    console.log('todo- crop:' + pv.primaryPlot.title);
}

function stats(pv) {
    console.log('todo- stats:' + pv.primaryPlot.title);
}

function select(pv,dlAry) {
    console.log('todo- select:' + pv.primaryPlot.title);
}

function unselect(pv,dlAry) {
    console.log('todo- unselect:' + pv.primaryPlot.title);
}

function filter(pv,dlAry) {
    console.log('todo- filter:' + pv.primaryPlot.title);
}



function makeExtensionButtons(extensionAry,pv,dlAry) {
    if (!extensionAry) return false;
    return extensionAry.map( (ext,idx) => (
            <ToolbarButton icon={ext.imageUrl} text={ext.title}
                           tip={ext.toolTip} key={ext.id}
                           horizontal={true} enabled={true}
                           visible={true}
                           lastTextItem={idx===(extensionAry.length-1)}
                           onClick={() => dispatchExtensionActivate(ext,makeExtActivateData(ext,pv,dlAry))}/>
        )
    );
}


/**
 *
 * @param pv
 * @param dlAry
 * @param extensionAry
 * @param showCrop
 * @param showStats
 * @param showSelect
 * @param showUnSelect
 * @param showFilter
 * @param extensionAry
 * @return {XML}
 * @constructor
 */
export function VisCtxToolbarView({plotView:pv, dlAry, extensionAry, showCrop, showStats,
                                   showSelect, showUnSelect, showFilter}) {

    var rS= {
        width: '100% - 2px',
        height: 34,
        display: 'inline-block',
        position: 'relative',
        verticalAlign: 'top',
        whiteSpace: 'nowrap'

    };
    var showSeparator= (showCrop|| showStats|| showSelect|| showUnSelect|| showFilter)
                        && extensionAry && extensionAry.length;

    return (
        <div style={rS}>
            <div
                style={{display: 'inline-block', padding: '8px 7px 0 5px', float : 'left', fontStyle: 'italic'}}>
                Options:</div>
            <ToolbarButton icon={CROP}
                           tip='Crop the image to the selected area'
                           horizontal={true}
                           visible={showCrop}
                           todo={true}
                           onClick={() => crop(pv)}/>

            <ToolbarButton icon={STATISTICS}
                           tip='Show statistics for the selected area'
                           horizontal={true}
                           visible={showStats}
                           todo={true}
                           onClick={() => stats(pv)}/>

            <ToolbarButton icon={SELECTED}
                           tip='Mark data in area as selected'
                           horizontal={true}
                           visible={showSelect}
                           todo={true}
                           onClick={() => select(pv,dlAry)}/>

            <ToolbarButton icon={UNSELECTED}
                           tip='Mark all data unselected'
                           horizontal={true}
                           visible={showUnSelect}
                           todo={true}
                           onClick={() => unselect(pv,dlAry)}/>

            <ToolbarButton icon={FILTER}
                           tip='Filter in the selected area'
                           horizontal={true}
                           visible={showFilter}
                           todo={true}
                           onClick={() => filter(pv,dlAry)}/>


            {makeExtensionButtons(extensionAry,pv,dlAry)}
        </div>
    );
}


VisCtxToolbarView.propTypes= {
    plotView : PropTypes.object.isRequired,
    dlAry : PropTypes.arrayOf(React.PropTypes.object),
    extensionAry : PropTypes.arrayOf(React.PropTypes.object),
    showCrop : PropTypes.bool,
    showStats : PropTypes.bool,
    showSelect : PropTypes.bool,
    showUnSelect : PropTypes.bool,
    showFilter : PropTypes.bool
};



ToolbarButton.defaultProps= {
    showCrop : false,
    showStats : false,
    showSelect : false,
    showUnSelect : false,
    showFilter : false,
    extensionAry : null
};




