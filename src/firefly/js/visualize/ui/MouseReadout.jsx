/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Fragment,memo, useState} from 'react';
import {number,string,oneOfType,object,func,bool} from 'prop-types';
import {getNonFluxDisplayElements, getFluxInfo} from './MouseReadoutUIUtil.js';
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {dispatchChangeLockByClick} from '../MouseReadoutCntlr.js';
import {copyToClipboard} from '../../util/WebUtil';
import {ToolbarButton} from '../../ui/ToolbarButton';

import CLIPBOARD from 'html/images/12x12_clipboard.png';
import CHECKED from 'html/images/12x12_clipboard-checked.png';
import './MouseReadout.css';


export function MouseReadout({readout, readoutData}){

    if (!readoutData.readoutItems) return (<div className='mouseReadoutDisplaySpace'/>);
    const {threeColor}= readoutData;

    const title= readoutData?.readoutItems?.title?.value ?? '';

    const displayEle= getNonFluxDisplayElements(readoutData.readoutItems,  readout.readoutPref, false);
    const {readout1, readout2, pixelSize, showReadout1PrefChange, showWavelengthFailed,
        showReadout2PrefChange, showPixelPrefChange, waveLength}= displayEle;
    const showCopy= readout.lockByClick;

    const fluxArray = getFluxInfo(readoutData);
    const gridClasses= `mouseReadoutImageGrid ${showCopy?'mouseReadoutImageGrid-withclip' :''} mouseReadoutDisplaySpace`;

    return (
        <div className={gridClasses}>
            <div style={{gridArea:'plotTitle', paddingLeft:4}}> {title}  </div>

            <DataReadoutItem lArea='pixReadoutTopLabel' vArea='pixReadoutTopValue' cArea='clipboardIconTop'
                             label={readout1.label} value={readout1.value} copyValue={readout1.copyValue} showCopy={showCopy}
                             prefChangeFunc={showReadout1PrefChange}/>
            <DataReadoutItem lArea='pixReadoutBottomLabel' vArea='pixReadoutBottomValue'  cArea='clipboardIconBottom'
                             label={readout2.label} value={readout2.value} copyValue={readout2.copyValue} showCopy={showCopy}
                             prefChangeFunc={showReadout2PrefChange}/>
            <DataReadoutItem lArea='pixSizeLabel' vArea='pixSizeValue' label={pixelSize.label}
                             value={pixelSize.value} prefChangeFunc={showPixelPrefChange}/>
                             
            <DataReadoutItem lArea='redLabel' vArea='redValue' label={fluxArray[0].label} value={fluxArray[0].value}/>
            {threeColor && <DataReadoutItem lArea='greenLabel' vArea='greenValue' label={fluxArray[1].label} value={fluxArray[1].value}/>}
            {waveLength && <DataReadoutItem lArea='greenLabel' vArea='greenValue' label={waveLength.label} value={waveLength.value}
                prefChangeFunc={showWavelengthFailed} /> }
            <DataReadoutItem lArea='blueLabel' vArea='blueValue' label={fluxArray[2].label} value={fluxArray[2].value}/>

            <MouseReadoutLock gArea='lock' lockByClick={readout.lockByClick} />
        </div>
    );
}

MouseReadout.propTypes = {
    readout: object,
    readoutData: object
};


export const MouseReadoutLock= memo(({gArea, gAreaLabel, style={}, lockByClick}) => {
    const s= gArea ? {gridArea:gArea,...style} : style;
    return (
        <React.Fragment>
            <div style={s} title='Click on an image to lock the display at that point.'>
                <input type='checkbox' name='aLock' value='lock' checked={lockByClick}
                       onChange={() => {
                           dispatchChangePointSelection('mouseReadout', !lockByClick);
                           dispatchChangeLockByClick(!lockByClick);

                       }}
                />
                {!gAreaLabel && <span style={{position:'relative', top:-2}}>Lock by click</span>}
            </div>
            {gAreaLabel &&
            <span style={
                {
                    gridArea: gAreaLabel,
                    position:'relative',
                    whiteSpace:'nowrap',
                    textOverflow: 'ellipsis',
                    overflow:'hidden',
                    paddingRight:3
                } }>
                Lock by click
            </span>}
        </React.Fragment>
);
});

MouseReadoutLock.propTypes = {
    style:       object,
    gArea:       string, // grid Area used with css grid-template-areas
    gAreaLabel:  string, // grid Area used with css grid-template-areas
    lockByClick: bool.isRequired
};

const baseVS={whiteSpace:'nowrap', overflow:'hidden', textOverflow: 'ellipsis'};
const baseLS={whiteSpace:'nowrap', overflow:'hidden', textOverflow: 'ellipsis'};


export const DataReadoutItem= memo(({lArea, vArea, cArea, labelStyle={}, valueStyle={}, showCopy=false,
                                        label='', value='', copyValue='', prefChangeFunc=undefined}) => {
    const lS= lArea ? {gridArea:lArea,...baseLS,...labelStyle} : {...baseLS,...labelStyle};
    const vS= vArea ? {gridArea:vArea,...baseVS, ...valueStyle} : {...baseVS,...valueStyle};
    const cS= cArea ? {gridArea:cArea, overflow:'hidden', height:13} : undefined;
    const labelClass= prefChangeFunc ? 'mouseReadoutLabel mouseReadoutClickLabel' : 'mouseReadoutLabel';
    const copyTitle= `Copy to clipboard: ${copyValue||value}`;

    let clipComponent= undefined;
    if (cArea) {
        clipComponent= (value && showCopy) ?
            <CopyToClipboard style={cS} title={copyTitle} value={copyValue||value} /> : <div style={cS}/>;
    }


    return (
        <Fragment>
            <div className={labelClass} title={value+''} style={lS} onClick={prefChangeFunc}>{label}</div>
            <div style={vS} title={value+''}> {value} </div>
            {clipComponent}
        </Fragment>
    );
});

DataReadoutItem.propTypes = {
    lArea:          string,   // label grid Area used with css grid-template-areas
    vArea:          string,   // value grid Area used with css grid-template-areas
    cArea:          string,   // clipboard grid Area used with css grid-template-areas
    showCopy:       bool,     // show the copy icon
    labelStyle:     object,
    valueStyle:     object,
    label:          string,
    value:          oneOfType([number,string]),
    copyValue:      string,   // for copy to clipboard, if specified, us this value other use value
    prefChangeFunc: func,
};



const buttonStyle= {
    borderRadius:2,
    backgroundColor:'rgba(255,255,255,.9',
    border:'solid transparent',
    borderWidth: '0 0 1px 0'
};


export function CopyToClipboard({value, title, style}) {
    const [clipIcon, setClipIcon] = useState(CLIPBOARD);

    const doCopy= (str) => {
        copyToClipboard(str);
        setTimeout( () => {
            setClipIcon(CHECKED);
            setTimeout( () => setClipIcon(CLIPBOARD),750);
        }, 10);
    };

    return (
        <div style={style}>
            <ToolbarButton icon={clipIcon} tip={title} bgDark={true} style={buttonStyle}
                           horizontal={true} onClick={() => doCopy(value)} />
        </div>
    );

}




