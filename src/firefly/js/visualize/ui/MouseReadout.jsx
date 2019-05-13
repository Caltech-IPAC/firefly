/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Fragment,memo} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {getNonFluxDisplayElements, getFluxInfo} from './MouseReadoutUIUtil.js';
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {dispatchChangeLockByClick} from '../../visualize/MouseReadoutCntlr.js';

import './MouseReadout.css';


export function MouseReadout({readout, readoutData}){

    if (!readoutData.readoutItems) return (<div className='mouseReadoutDisplaySpace'/>);

    const title= get(readoutData, 'readoutItems.title.value','');

    const displayEle= getNonFluxDisplayElements(readoutData.readoutItems,  readout.readoutPref, false);
    const {readout1, readout2, pixelSize, showReadout1PrefChange, showReadout2PrefChange, showPixelPrefChange}= displayEle;

    const fluxArray = getFluxInfo(readoutData);

    return (
        <div className='mouseReadoutImageGrid mouseReadoutDisplaySpace'>
            <div style={{gridArea:'plotTitle', paddingLeft:4}}> {title}  </div>

            <DataReadoutItem lArea='pixReadoutTopLabel' vArea='pixReadoutTopValue' label={readout1.label} value={readout1.value}
                             prefChangeFunc={showReadout1PrefChange}/>
            <DataReadoutItem lArea='pixReadoutBottomLabel' vArea='pixReadoutBottomValue'  label={readout2.label} value={readout2.value}
                             prefChangeFunc={showReadout2PrefChange}/>
            <DataReadoutItem lArea='pixSizeLabel' vArea='pixSizeValue' label={pixelSize.label}
                             value={pixelSize.value} prefChangeFunc={showPixelPrefChange}/>
                             
            <DataReadoutItem lArea='redLabel' vArea='redValue' label={fluxArray[0].label} value={fluxArray[0].value}/>
            <DataReadoutItem lArea='greenLabel' vArea='greenValue' label={fluxArray[1].label} value={fluxArray[1].value}/>
            <DataReadoutItem lArea='blueLabel' vArea='blueValue' label={fluxArray[2].label} value={fluxArray[2].value}/>

            <MouseReadoutLock gArea='lock' lockByClick={readout.lockByClick} />
        </div>
    );
}

MouseReadout.propTypes = {
    readout: PropTypes.object,
    readoutData: PropTypes.object
};


export const MouseReadoutLock= memo(({gArea, style={}, lockByClick}) => {
    const s= gArea ? {gridArea:gArea,...style} : style;
    return (
        <div style={s} title='Click on an image to lock the display at that point.'>
            <input type='checkbox' name='aLock' value='lock' checked={lockByClick}
                   onChange={() => {
                       dispatchChangePointSelection('mouseReadout', !lockByClick);
                       dispatchChangeLockByClick(!lockByClick);

                   }}
            />
            Lock by click
        </div>
    );
});

MouseReadoutLock.propTypes = {
    style:       PropTypes.object,
    gArea:       PropTypes.string, // grid Area used with css grid-template-areas
    lockByClick: PropTypes.bool.isRequired
};



export const DataReadoutItem= memo(({lArea, vArea, labelStyle={}, valueStyle={},
                                        label='', value='', prefChangeFunc=undefined}) => {
    const lS= lArea ? {gridArea:lArea,...labelStyle} : labelStyle;
    const vS= vArea ? {gridArea:vArea,...valueStyle} : valueStyle;
    const labelClass= prefChangeFunc ? 'mouseReadoutLabel mouseReadoutClickLabel' : 'mouseReadoutLabel';
    return (
        <Fragment>
            <div className={labelClass} style={lS} onClick={prefChangeFunc}>{label}</div>
            <div style={vS}> {value} </div>
        </Fragment>
    );
});

DataReadoutItem.propTypes = {
    lArea:          PropTypes.string,   //  label grid Area used with css grid-template-areas
    vArea:          PropTypes.string,   //  value grid Area used with css grid-template-areas
    labelStyle:     PropTypes.object,
    valueStyle:     PropTypes.object,
    label:          PropTypes.string,
    value:          PropTypes.string,
    prefChangeFunc: PropTypes.func,
};
