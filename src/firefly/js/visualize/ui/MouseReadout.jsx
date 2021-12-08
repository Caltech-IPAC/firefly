/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Fragment,memo, useState} from 'react';
import {number,string,oneOfType,object,func,bool} from 'prop-types';
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {dispatchChangeLockByClick} from '../MouseReadoutCntlr.js';
import {copyToClipboard} from '../../util/WebUtil';
import {ToolbarButton} from '../../ui/ToolbarButton';

import CLIPBOARD from 'html/images/12x12_clipboard.png';
import CHECKED from 'html/images/12x12_clipboard-checked.png';
import CLIPBOARD_LARGE from 'html/images/20x20_clipboard.png';
import CHECKED_LARGE from 'html/images/20x20_clipboard-checked.png';
import './MouseReadout.css';

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
                    paddingRight:3,
                    minWidth: 20
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



const defButtonStyle= {
    borderRadius:2,
    backgroundColor:'rgba(255,255,255,.9',
    border:'solid transparent',
    borderWidth: '0 0 1px 0'
};


export function CopyToClipboard({value, title, style, size=12, buttonStyle={}}) {
    const uncheckedIco = size > 12 ? CLIPBOARD_LARGE : CLIPBOARD;
    const checkedIco = size > 12 ? CHECKED_LARGE : CHECKED;
    title= title ||  `Copy to clipboard: ${value}`;

    const [clipIcon, setClipIcon] = useState(uncheckedIco);

    const doCopy= (str) => {
        copyToClipboard(str);
        setTimeout( () => {
            setClipIcon(checkedIco);
            setTimeout( () => setClipIcon(uncheckedIco),750);
        }, 10);
    };

    return (
        <div style={style}>
            <ToolbarButton icon={clipIcon} tip={title} bgDark={true} style={{...defButtonStyle, ...buttonStyle}} imageStyle={{height:size, width:size}}
                           horizontal={true} onClick={() => doCopy(value)} />
        </div>
    );

}




