/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {getActivePlotView, primePlot} from '../PlotViewUtil.js';
import { RangeValues} from '../RangeValues.js';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import { ToolbarButton, DropDownVerticalSeparator} from '../../ui/ToolbarButton.jsx';
import {dispatchStretchChange, visRoot} from '../ImagePlotCntlr.js';
import { PERCENTAGE, ZSCALE, SIGMA, STRETCH_LINEAR, STRETCH_LOG, STRETCH_LOGLOG, STRETCH_EQUAL,
    STRETCH_SQUARED, STRETCH_SQRT, STRETCH_ASINH, STRETCH_POWERLAW_GAMMA, } from '../RangeValues.js';
import {showColorDialog} from './ColorDialog.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';


function getLabel(rv,baseLabel) {
    const algor= rv.algorithm;
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
    const newRv= {...currRV,
        algorithm,
        upperWhich: ZSCALE, lowerWhich: ZSCALE,
        upperValue: 1, lowerValue: 1,
        zscaleContrast: 25, zscaleSamples: 600, zscaleSamplesPerLine: 120};
    changeStretch(pv,newRv);
}

/**
 *
 * @param pv
 * @param currRV
 * @param sType
 * @param lowerValue
 * @param upperValue
 * @param asinhQ
 */
function stretchByType(pv,currRV,sType,lowerValue,upperValue,asinhQ) {
    const newRv= {...currRV,
        upperWhich: sType, lowerWhich: sType,
        upperValue, lowerValue, asinhQValue: asinhQ };
    changeStretch(pv,newRv);
}


export function StretchDropDownView({toolbarElement}) {
    const pv = useStoreConnector(() => getActivePlotView(visRoot()));
    const enabled= Boolean(pv);
    const plot= primePlot(pv);
    if (!plot) return <div/>;
    const rv= plot?.plotState?.getRangeValues();

    const zscaleStretchMatches= (rv, algorithm) => rv.upperWhich===ZSCALE && algorithm===rv.algorithm;

    return (
        <SingleColumnMenu>
            <ToolbarButton text='Color stretch...'
                           tip='Change the background image stretch'
                           onClick={() => showColorDialog(toolbarElement)}/>
            <DropDownVerticalSeparator/>

            <ToolbarButton text='Z Scale Linear Stretch'
                           tip='Z Scale Linear Stretch'
                           hasCheckBox={true} checkBoxOn={zscaleStretchMatches(rv,STRETCH_LINEAR)}
                           onClick={() => stretchByZscaleAlgorithm(pv,rv,STRETCH_LINEAR)}/>
            <ToolbarButton text='Z Scale Log Stretch'
                           tip='Z Scale Log Stretch'
                           hasCheckBox={true} checkBoxOn={zscaleStretchMatches(rv,STRETCH_LOG)}
                           onClick={() => stretchByZscaleAlgorithm(pv,rv,STRETCH_LOG)}/>
            <ToolbarButton text='Z Scale Log-Log Stretch'
                           tip='Z Scale Log-Log Stretch'
                           hasCheckBox={true} checkBoxOn={zscaleStretchMatches(rv,STRETCH_LOGLOG)}
                           onClick={() => stretchByZscaleAlgorithm(pv,rv,STRETCH_LOGLOG)}/>
            <ToolbarButton text='Z Scale Asinh Stretch'
                           tip='Z Scale Asinh Stretch'
                           hasCheckBox={true} checkBoxOn={zscaleStretchMatches(rv,STRETCH_ASINH)}
                           onClick={() => stretchByZscaleAlgorithm(pv,rv,STRETCH_ASINH)}/>
            <DropDownVerticalSeparator/>

            {enabled && renderAlgorithmDependentItems({pv,rv})}
        </SingleColumnMenu>
        );

}

StretchDropDownView.propTypes= {
    plotView : PropTypes.object,
    toolbarElement : PropTypes.object
};

function renderAlgorithmDependentItems({pv,rv}) {

    const percentStretchMatches= (lower,upper) =>
        rv.lowerValue===lower && PERCENTAGE===rv.lowerWhich &&
        rv.upperValue===upper && PERCENTAGE===rv.upperWhich && isNaN(rv.asinhQValue);

    const sigmaStretchMatches= (lower,upper) =>
        rv.lowerValue===lower && SIGMA===rv.lowerWhich &&
        rv.upperValue===upper && SIGMA===rv.upperWhich && isNaN(rv.asinhQValue);

    const percentAsinhStretchMatches= (lower,upper,asinhQValue) =>
        rv.lowerValue===lower && PERCENTAGE===rv.lowerWhich &&
        rv.upperValue===upper && PERCENTAGE===rv.upperWhich && rv.asinhQValue===asinhQValue;

    const enabled = true;
    if (rv.algorithm === STRETCH_ASINH) {
        return (
            <div>
                <ToolbarButton text={getLabel(rv, 'Stretch with Q=4')}
                               tip='Stretch range 1% to 80%, Q=4'
                               hasCheckBox={true} checkBoxOn={percentAsinhStretchMatches(1,80,4)}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 80, 4)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch with Q=6')}
                               tip='Stretch range 1% to 80%, Q=6'
                               hasCheckBox={true} checkBoxOn={percentAsinhStretchMatches(1,80,6)}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 80, 6)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch with Q=8')}
                               tip='Stretch range 1% to 80%, Q=8'
                               hasCheckBox={true} checkBoxOn={percentAsinhStretchMatches(1,80,8)}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 80, 8)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch with Q=10')}
                               tip='Stretch range 1% to 80%, Q=10'
                               hasCheckBox={true} checkBoxOn={percentAsinhStretchMatches(1,80,10)}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 80, 10)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch with Q=12')}
                               tip='Stretch range 1% to 80%, Q=12'
                               hasCheckBox={true} checkBoxOn={percentAsinhStretchMatches(1,80,12)}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 80, 12)}/>
            </div>
    );
    } else {
        return (
            <div>
                <ToolbarButton text={getLabel(rv, 'Stretch to 99%')}
                               tip='Stretch range 1% to 99%'
                               hasCheckBox={true} checkBoxOn={percentStretchMatches(1,99)}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 99)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch to 98%')}
                               tip='Stretch range 2% to 98%'
                               hasCheckBox={true} checkBoxOn={percentStretchMatches(2,98)}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 2, 98)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch to 97%')}
                               tip='Stretch range 3% to 97%'
                               hasCheckBox={true} checkBoxOn={percentStretchMatches(3,97)}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 3, 97)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch to 95%')}
                               tip='Stretch range 5% to 95%'
                               hasCheckBox={true} checkBoxOn={percentStretchMatches(5,95)}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 5, 95)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch to 85%')}
                               tip='Stretch range 15% to 85%'
                               hasCheckBox={true} checkBoxOn={percentStretchMatches(15,85)}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 15, 85)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch -2 Sigma to 10 Sigma')}
                               tip='Stretch -2 Sigma to 10 Sigma'
                               hasCheckBox={true} checkBoxOn={sigmaStretchMatches(-2,10)}
                               onClick={() => stretchByType(pv, rv, SIGMA, -2, 10)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch -1 Sigma to 30 Sigma')}
                               tip='Stretch -1 Sigma to 30 Sigma'
                               hasCheckBox={true} checkBoxOn={sigmaStretchMatches(-1,30)}
                               onClick={() => stretchByType(pv, rv, SIGMA, -1, 30)}/>
            </div>
        );
    }
}
