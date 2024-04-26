/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Chip, Stack, Switch, Tooltip, Typography} from '@mui/joy';
import React, {memo, useEffect, useRef, useState} from 'react';
import {object, bool, number} from 'prop-types';
import BrowserInfo from '../../util/BrowserInfo.js';
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {showMouseReadoutFluxRadixDialog} from './MouseReadoutOptionPopups.jsx';
import {getNonFluxDisplayElements, getFluxInfo} from './MouseReadoutUIUtil.js';
import {CopyToClipboard} from './MouseReadout.jsx';
import {dispatchChangeLockByClick, STANDARD_READOUT} from '../MouseReadoutCntlr.js';
import {ToolbarButton} from 'firefly/ui/ToolbarButton.jsx';
import {showMouseReadoutPopout} from 'firefly/visualize/ui/MouseReadPopoutAll.jsx';
import LaunchOutlinedIcon from '@mui/icons-material/LaunchOutlined';

export function MouseReadoutBottomLine({readout, readoutData, readoutShowing, style, slightlyTransparent=false, showOnInactive= false, radix}){

    const {current:divref}= useRef({element:undefined});
    const [width,setWidth]= useState(() => 200);

    useEffect( () => {
        if (divref.element) {
            const w= divref.element.getBoundingClientRect()?.width;
            w && setWidth(w);
        }
    }, [divref?.element]);

    const {readoutType}= readoutData;
    if (!readoutData.readoutItems) return (<div style={{height: showOnInactive?20:0, width:showOnInactive?1:0}}/>);

    const displayEle= getNonFluxDisplayElements(readoutData,  readout.readoutPref, false);
    const {readout1, showReadout1PrefChange, showWavelengthFailed, waveLength}= displayEle;
    const {lockByClick=false}= readout??{};
    const showCopy= lockByClick;

    const fluxArray = getFluxInfo(readoutData, radix);


    const sx= (theme) => ({
        justifyContent: 'space-between',
        alignItems:'center',
        height: '1.5rem',
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

    const fullSize= width>500;
    const {threeColor= false}= readoutData;
    const monoFont= radix===16;

    const checkboxText= width>600 ? lockByClick ? 'Click Lock: on': 'Click Lock: off' : '';
    const doWL= fullSize && waveLength && readoutType===STANDARD_READOUT;
    const doFlux= fullSize && readoutType===STANDARD_READOUT;

    const label3C= threeColor && doFlux ? get3CLabel(fluxArray) : '';
    const value3C= threeColor && doFlux ? get3CValue(fluxArray) : '';
    const labelStand= !threeColor && doFlux ? fluxArray[0].label||'Value:' : '';
    const valueStand= !threeColor && doFlux ? fluxArray[0].value : '';

    const fluxLabel= threeColor ? label3C : labelStand;
    const fluxValue= threeColor ? value3C : valueStand;
    const fluxWidth= threeColor ? '9rem' : '7rem';

    return (
        <Stack {...{direction:'row', sx, ref: (c) => divref.element=c}}>
            <Stack {...{direction:'row', alignItems:'center', sx:{'& .ff-readout-value':{pl:.5}} }}>

                <ToolbarButton icon={<LaunchOutlinedIcon/>}
                               tip='Show expanded readout, thumbnail and magnifier'
                               onClick={() => showMouseReadoutPopout()}/>

                <LabelItem {...{showCopy, label:readout1.label, value:readout1.value, copyValue:readout1.copyValue,
                    sx:{pl:1}, prefChangeFunc:showReadout1PrefChange}}/>
                <DataItem {...{value:readout1.value, sx:{minWidth:'13rem', pl:.5} }}/>

                {doFlux && <LabelItem {...{label:fluxLabel, value:fluxValue, sx:{pl:1},
                           prefChangeFunc:() => showMouseReadoutFluxRadixDialog(readout.readoutPref)}}/> }
                {doFlux && <DataItem {...{value:fluxValue, unit: threeColor? '' :fluxArray[0].unit,
                    sx:{minWidth:fluxWidth, pl:.5}, monoFont}}/> }


                {doWL && <LabelItem {...{label:waveLength.label, value:waveLength.value, sx:{pl:1}}}/> }
                {doWL && <DataItem {...{value:waveLength.value}}/> }

            </Stack>

            <Tooltip placement='top-end' enterDelay={!checkboxText?750:undefined}
                title='Lock by click - mouse readout will only update by clicking on the image'>
                <Switch size='sm' endDecorator={checkboxText} checked={lockByClick}
                          sx={{pr:.5,whiteSpace:'nowrap'}}
                          onChange={() => {
                              dispatchChangePointSelection('mouseReadout', !lockByClick);
                              dispatchChangeLockByClick(!lockByClick);
                          }} />
            </Tooltip>
        </Stack>
    );
}

MouseReadoutBottomLine.propTypes = {
    readout: object,
    readoutData: object,
    style: object,
    flux: number,
    slightlyTransparent: bool,
    showOnInactive: bool,
    readoutShowing: bool,
    radix:number,
};


const LabelItem= memo(({showCopy=false, label='', value='', copyValue='', prefChangeFunc=undefined, sx}) => {
    const copyTitle= `Copy to clipboard: ${copyValue||value}`;

    const textStyle= prefChangeFunc ?
        { cursor: 'pointer', textDecoration: 'underline', fontStyle: 'italic', whiteSpace:'nowrap'} :
        {whiteSpace:'nowrap'};

    const clipComponent= (value&&showCopy) ? <CopyToClipboard title={copyTitle} value={copyValue||value} /> : undefined;
    return (
        <Stack {...{direction:'row', alignItems:'center', className:'ff-readout-label', sx}}>
            {prefChangeFunc ?
                <Chip variant='soft' color='primary' title={value+''} sx={{borderRadius:5}} onClick={prefChangeFunc}>{label}</Chip> :
                <Typography level='body-sm' title={value+''} sx={textStyle}>{label}</Typography>}
            {clipComponent}
        </Stack>
    );
});

const DataItem= memo(({ value='', unit='', monoFont=false, sx}) => {
    const mStyle= monoFont ? {fontFamily:'monospace', whiteSpace:'nowrap'} : {whiteSpace:'nowrap'};
    const vStr=value+'';
    return (
        <Stack {...{direction:'row', className:'ff-readout-value', alignItems:'center', sx }}>
            <Typography level='body-sm' color='warning'  title={vStr} sx={mStyle}>{value}</Typography>
            {unit && <Typography level='body-sm' color='warning' title={vStr} sx={{pl:.25}}>{unit}</Typography>}
        </Stack>
    );
});



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