/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import { primePlot} from '../PlotViewUtil.js';
import { RangeValues} from '../RangeValues.js';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {
    ToolbarButton,
    DropDownVerticalSeparator} from '../../ui/ToolbarButton.jsx';
import {dispatchStretchChange} from '../ImagePlotCntlr.js';
import {
    PERCENTAGE,
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
import {showColorDialog} from './ColorDialog.jsx';



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


function changeStretch(pv,rv) {

    const serRv= RangeValues.serializeRV(rv);
    const p= primePlot(pv);
    const stretchData= p.plotState.getBands().map( (b) =>
        ({ band : b.key, rv :  serRv, bandVisible: true }) );
    dispatchStretchChange({plotId:pv.plotId,stretchData});
}


/**
 *
 * @param pv
 * @param currRV
 * @param algorithm
 */
function stretchByZscaleAlgorithm(pv,currRV,algorithm) {
    var newRv= Object.assign({},currRV);
    newRv.algorithm= algorithm;
    newRv.upperWhich= ZSCALE;
    newRv.lowerWhich= ZSCALE;
    newRv.upperValue= 1;
    newRv.lowerValue= 1;
    newRv.zscaleContrast= 25;
    newRv.zscaleSamples= 600;
    newRv.zscaleSamplesPerLine= 120;
    changeStretch(pv,newRv);
}

/**
 *
 * @param pv
 * @param currRV
 * @param sType
 * @param min
 * @param max
 */
function stretchByType(pv,currRV,sType,min,max) {
    var newRv= Object.assign({},currRV);
    newRv.upperWhich= sType;
    newRv.lowerWhich= sType;
    newRv.upperValue= max;
    newRv.lowerValue= min;
    changeStretch(pv,newRv);
}


export function StretchDropDownView({plotView:pv}) {
    var enabled= pv ? true : false;
    var rv= primePlot(pv).plotState.getRangeValues();
    return (
        <SingleColumnMenu>
            <ToolbarButton text='Color stretch...'
                           tip='Change the background image stretch'
                           enabled={enabled} horizontal={false}
                           onClick={() => showColorDialog()}/>
            <DropDownVerticalSeparator/>

            <ToolbarButton text='Z Scale Linear Stretch'
                           tip='Z Scale Linear Stretch'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByZscaleAlgorithm(pv,rv,STRETCH_LINEAR)}/>
            <ToolbarButton text='Z Scale Log Stretch'
                           tip='Z Scale Log Stretch'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByZscaleAlgorithm(pv,rv,STRETCH_LOG)}/>
            <ToolbarButton text='Z Scale Log-Log Stretch'
                           tip='Z Scale Log-Log Stretch'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByZscaleAlgorithm(pv,rv,STRETCH_LOGLOG)}/>
            <DropDownVerticalSeparator/>

            <ToolbarButton text={getLabel(rv,'Stretch to 99%')}
                           tip='Stretch range 1% to 99%'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(pv,rv,PERCENTAGE,1,99)}/>
            <ToolbarButton text={getLabel(rv,'Stretch to 98%')}
                           tip='Stretch range 2% to 98%'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(pv,rv,PERCENTAGE,2,98)}/>
            <ToolbarButton text={getLabel(rv,'Stretch to 97%')}
                           tip='Stretch range 3% to 97%'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(pv,rv,PERCENTAGE,3,97)}/>
            <ToolbarButton text={getLabel(rv,'Stretch to 95%')}
                           tip='Stretch range 5% to 95%'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(pv,rv,PERCENTAGE,5,95)}/>
            <ToolbarButton text={getLabel(rv,'Stretch to 85%')}
                           tip='Stretch range 15% to 85%'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(pv,rv,PERCENTAGE,15,85)}/>
            <ToolbarButton text={getLabel(rv,'Stretch -2 Sigma to 10 Sigma')}
                           tip='Stretch -2 Sigma to 10 Sigma'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(pv,rv,SIGMA,-2,10)}/>
            <ToolbarButton text={getLabel(rv,'Stretch -1 Sigma to 30 Sigma')}
                           tip='Stretch -1 Sigma to 30 Sigma'
                           enabled={enabled} horizontal={false}
                           onClick={() => stretchByType(pv,rv,SIGMA,-1,30)}/>
        </SingleColumnMenu>
        );

}

StretchDropDownView.propTypes= {
    plotView : PropTypes.object
};
