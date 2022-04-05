/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {throttle} from 'lodash';
import PropTypes from 'prop-types';
import {getActivePlotView, isThreeColor, primePlot} from '../PlotViewUtil.js';
import { RangeValues} from '../RangeValues.js';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {
    ToolbarButton,
    DropDownVerticalSeparator} from '../../ui/ToolbarButton.jsx';
import {dispatchColorChange, dispatchStretchChange, visRoot} from '../ImagePlotCntlr.js';
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
import {RangeSliderView} from '../../ui/RangeSliderView.jsx';
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
    const newRv= Object.assign({},currRV);
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
 * @param asinhQ
 */
function stretchByType(pv,currRV,sType,min,max,asinhQ) {
    const newRv= Object.assign({},currRV);
    newRv.upperWhich= sType;
    newRv.lowerWhich= sType;
    newRv.upperValue= max;
    newRv.lowerValue= min;
    if (asinhQ) {
        newRv.asinhQValue= asinhQ;
    }
    changeStretch(pv,newRv);
}

const changeBiasContrast= throttle((plot,bias,contrast) => {
    if (isThreeColor(plot)) return;
    dispatchColorChange({
        plotId:plot.plotId,
        cbarId: plot.plotState.getColorTableId(),
        bias,
        contrast
    });
},300);

const biasMarks = { 30: '.3', 40:'.4', 50:'.5', 60:'.6', 70:'.7' };
const contrastMarks = { 0: '0', 5:'.5', 10:'1', 15:'1.5',  20:'2' };


export function StretchDropDownView({toolbarElement}) {
    const pv = useStoreConnector(() => getActivePlotView(visRoot()));
    const enabled= pv ? true : false;
    const plot= primePlot(pv);
    const rv= plot.plotState.getRangeValues();
    const {bias,contrast}= plot.rawData.bandData[0];
    const biasInt= Math.trunc(bias*100);
    const contrastInt= Math.trunc(contrast*10);
    const threeColor= isThreeColor(plot);
    return (
        <SingleColumnMenu>
            <ToolbarButton text='Color stretch...'
                           tip='Change the background image stretch'
                           enabled={enabled} horizontal={false}
                           onClick={() => showColorDialog(toolbarElement)}/>
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
            <ToolbarButton text='Z Scale Asinh Stretch'
                           tip='Z Scale Asinh Stretch'
                           enabled={enabled} horizontal={false}
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
    const enabled = true;
    if (rv.algorithm === STRETCH_ASINH) {
        return (
            <div>
                <ToolbarButton text={getLabel(rv, 'Stretch with Q=4')}
                               tip='Stretch range 1% to 80%, Q=4'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 80, 4)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch with Q=6')}
                               tip='Stretch range 1% to 80%, Q=6'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 80, 6)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch with Q=8')}
                               tip='Stretch range 1% to 80%, Q=8'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 80, 8)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch with Q=10')}
                               tip='Stretch range 1% to 80%, Q=10'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 80, 10)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch with Q=12')}
                               tip='Stretch range 1% to 80%, Q=12'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 80, 12)}/>
            </div>
    );
    } else {
        return (
            <div>
                <ToolbarButton text={getLabel(rv, 'Stretch to 99%')}
                               tip='Stretch range 1% to 99%'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 1, 99)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch to 98%')}
                               tip='Stretch range 2% to 98%'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 2, 98)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch to 97%')}
                               tip='Stretch range 3% to 97%'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 3, 97)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch to 95%')}
                               tip='Stretch range 5% to 95%'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 5, 95)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch to 85%')}
                               tip='Stretch range 15% to 85%'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, PERCENTAGE, 15, 85)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch -2 Sigma to 10 Sigma')}
                               tip='Stretch -2 Sigma to 10 Sigma'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, SIGMA, -2, 10)}/>
                <ToolbarButton text={getLabel(rv, 'Stretch -1 Sigma to 30 Sigma')}
                               tip='Stretch -1 Sigma to 30 Sigma'
                               enabled={enabled} horizontal={false}
                               onClick={() => stretchByType(pv, rv, SIGMA, -1, 30)}/>
            </div>
        );
    }
}
