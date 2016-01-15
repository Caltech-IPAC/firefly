/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {getActivePlotView,
    getDrawLayerByType,
    getPlotViewById,
    isDrawLayerAttached,
    getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {
    ToolbarButton,
    DropDownVerticalSeparator,
    DropDownToolbarButton} from '../../ui/ToolbarButton.jsx';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {SimpleLayerOnOffButton} from './SimpleLayerOnOffButton.jsx';
import {showDrawingLayerPopup} from './DrawLayerPanel.jsx';
import {defMenuItemKeys} from '../MenuItemKeys.js';
import {
    PERCENTAGE,
    MAXMIN,
    ABSOLUTE,
    ZSCALE,
    SIGMA,
    STRETCH_LINEAR,
    STRETCH_LOG,
    STRETCH_LOGLOG,
    STRETCH_EQUAL,
    STRETCH_SQUARED,
    STRETCH_SQRT,
    STRETCH_ASINH,
    STRETCH_POWERLAW_GAMMA,
} from '../RangeValues.js';



function getLabel(rv,baseLabel) {
    var algor= rv.algorithm;
    switch (algor) {
        case STRETCH_LINEAR : return 'Linear: '+baseLabel;
        case STRETCH_LOG : return 'Log: '+baseLabel;
        case STRETCH_LOGLOG : return 'Log-Log: '+baseLabel;
        case STRETCH_EQUAL : return 'Histogram Equalization: '+baseLabel;
        case STRETCH_SQUARED : return 'Squared: '+baseLabel;
        case STRETCH_SQRT : return 'Square Root: '+baseLabel;
        case STRETCH_ASINH : return 'Asinh: '+baseLabel;
        case STRETCH_POWERLAW_GAMMA : return 'PowerLawGamma: '+baseLabel;
    }
    return baseLabel;
}

function stretchByZscaleAlgorithm(currRV,algorithm) {

}

function stretchByType(currRV,sType,min,max) {

}



export function StretchDropDownView({plotView:pv}) {
    var enabled= pv ? true : false;
    var rv= pv.primaryPlot.plotState.getRangeValues();
    return (
        <SingleColumnMenu>
            <ToolbarButton text='Color stretch...'
                           tip='Change the background image stretch'
                           enabled={enabled}
                           horizontal={false}
                           onClick={() => console.log('show color stretch dialog')}/>
            <DropDownVerticalSeparator/>

            <ToolbarButton text='Z Scale Linear Stretch'
                           tip='Z Scale Linear Stretch'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByZscaleAlgorithm(rv,STRETCH_LINEAR)}/>
            <ToolbarButton text='Z Scale Log Stretch'
                           tip='Z Scale Log Stretch'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByZscaleAlgorithm(rv,STRETCH_LOG)}/>
            <ToolbarButton text='Z Scale Log-Log Stretch'
                           tip='Z Scale Log-Log Stretch'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByZscaleAlgorithm(rv,STRETCH_LOGLOG)}/>
            <DropDownVerticalSeparator/>

            <ToolbarButton text={getLabel(rv,'Stretch to 99%')}
                           tip='Stretch range 1% to 99%'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(rv,PERCENTAGE,1,99)}/>
            <ToolbarButton text={getLabel(rv,'Stretch to 98%')}
                           tip='Stretch range 2% to 98%'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(rv,PERCENTAGE,1,98)}/>
            <ToolbarButton text={getLabel(rv,'Stretch to 97%')}
                           tip='Stretch range 3% to 97%'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(rv,PERCENTAGE,1,97)}/>
            <ToolbarButton text={getLabel(rv,'Stretch to 95%')}
                           tip='Stretch range 5% to 95%'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(rv,PERCENTAGE,1,95)}/>
            <ToolbarButton text={getLabel(rv,'Stretch to 85%')}
                           tip='Stretch range 15% to 85%'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(rv,PERCENTAGE,1,85)}/>
        </SingleColumnMenu>
        );

}

StretchDropDownView.propTypes= {
    plotView : PropTypes.object,
};
