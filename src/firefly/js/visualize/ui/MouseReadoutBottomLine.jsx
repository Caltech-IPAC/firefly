/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box} from '@mui/joy';
import React, {useEffect, useRef, useState} from 'react';
import {object, bool, number} from 'prop-types';
import BrowserInfo from '../../util/BrowserInfo.js';
import {showMouseReadoutFluxRadixDialog} from './MouseReadoutOptionPopups.jsx';
import {getNonFluxDisplayElements, getFluxInfo} from './MouseReadoutUIUtil.js';
import {DataReadoutItem, MouseReadoutLock} from './MouseReadout.jsx';
import {STANDARD_READOUT} from '../MouseReadoutCntlr.js';
import {ToolbarButton} from 'firefly/ui/ToolbarButton.jsx';
import {showMouseReadoutPopout} from 'firefly/visualize/ui/MouseReadPopoutAll.jsx';
import './MouseReadout.css';
import POPOUT_ICON from 'images//pop-out.png';


export function MouseReadoutBottomLine({readout, readoutData, readoutShowing, style, slightlyTransparent=false, showOnInactive= false, radix}){

    const {current:divref}= useRef({element:undefined});

    const [width,setWidth]= useState(() => 200);

    useEffect( () => {
        if (divref.element) {
            const w= divref.element.getBoundingClientRect()?.width;
            setWidth(w);
        }
    });
    const fullSize= width>500;

    const {readoutType}= readoutData;
    const image= readoutType===STANDARD_READOUT;
    if (!readoutData.readoutItems) return (<div style={{height: showOnInactive?20:0, width:showOnInactive?1:0}}/>);
    const {threeColor= false}= readoutData;

    const displayEle= getNonFluxDisplayElements(readoutData.readoutItems,  readout.readoutPref, false);
    const {readout1, showReadout1PrefChange, showWavelengthFailed, waveLength}= displayEle;
    const showCopy= readout.lockByClick;

    const fluxArray = getFluxInfo(readoutData, radix);
    const gridClasses= getGridClass(fullSize, image,threeColor,waveLength);


    const sx= (theme) => ({
        height: '1.3em',
        borderRadius: '5px',
        overflow:'hidden',
        border: '2px solid rgba(0,0,0,.1)',
        ...style,
        backgroundColor: slightlyTransparent && BrowserInfo.supportsCssColorMix() ?
            `color-mix(in srgb, ${theme.vars.palette.neutral.softBg} 90%, transparent)` :
            theme.vars.palette.neutral.softBg,
    });
    if (!readoutShowing) {
        if (showOnInactive) return <Box sx={sx}/>;
        return <div/>;
    }

    return (
        <Box {...{className:gridClasses, sx, ref: (c) => divref.element=c}}>
            <DataReadoutItem lArea='pixReadoutLabel' vArea='pixReadoutValue' cArea='clipboardIcon'
                             label={readout1.label} value={readout1.value} copyValue={readout1.copyValue} showCopy={showCopy}
                             prefChangeFunc={showReadout1PrefChange}/>

            {fullSize && !threeColor && image && <DataReadoutItem lArea='fluxLabel' vArea='fluxValue'
                                                                  label={fluxArray[0].label||'Value:'}
                                                                  value={fluxArray[0].value}
                                                                  unit={fluxArray[0].unit}
                                                                  monoFont={radix===16}
                                                                  prefChangeFunc={() =>showMouseReadoutFluxRadixDialog(readout.readoutPref)}
            />}
            {fullSize && threeColor && image && <DataReadoutItem lArea='fluxLabel' vArea='fluxValue' label={get3CLabel(fluxArray)} value={get3CValue(fluxArray)}
                                                                 prefChangeFunc={() =>showMouseReadoutFluxRadixDialog(readout.readoutPref)}
                                                                 monoFont={radix===16}
            />}
            {fullSize && waveLength && image && <DataReadoutItem lArea='wlLabel' vArea='wlValue' label={waveLength.label} value={waveLength.value}
                                                     prefChangeFunc={showWavelengthFailed} /> }
            {<MouseReadoutLock gArea='lock' gAreaLabel='lockLabel'  lockByClick={readout.lockByClick} />}
            <ToolbarButton icon={POPOUT_ICON}
                           sx={{gridArea:'popout','.ff-toolbar-iconbutton' : {padding:'1px'}}}
                           imageStyle={{width:16,flexGrow:0}}
                           tip='Show expanded readout, thumbnail and magnifier'
                           onClick={() => {
                               showMouseReadoutPopout();
                           }}/>
        </Box>
    );
}

MouseReadoutBottomLine.propTypes = {
    readout: object,
    readoutData: object,
    style: object,
    flux: number,
    slightlyTransparent: bool,
    showOnInactive: bool,
    readoutShowing: bool
};


function getGridClass(fullSize, image, threeColor, waveLength) {
    if (waveLength) return 'miniMouseReadoutWLImageGrid';
    if (fullSize && image) return 'miniMouseReadoutSingleImageGrid';
    return 'miniMouseReadoutNoFluxGrid';

}

function get3CValue(fluxArray) {
    return fluxArray.reduce( (prev,f) => {
        if (!f?.value) return prev;
        return prev ? `${prev}, ${f.value} ${f.unit}`: `${f.value} ${f.unit}`;
    }, '');
}

function getWhichRGBChar(s) {
    if (!s) return '';
    const labLow= s.toLowerCase();
    if (labLow.includes('red')) return 'r';
    if (labLow.includes('green')) return 'g';
    if (labLow.includes('blue')) return 'b';
    return '';
}

function get3CLabel(fluxArray) {
    const label=  fluxArray.reduce( (prev,f) => {
        if (!f?.value) return prev;
        const rgbChar= getWhichRGBChar(f.label);
        if (!rgbChar) return prev;
        return prev ? `${prev}, ${rgbChar}` : rgbChar;
    }, '');
    return label? label+':' : '';
}