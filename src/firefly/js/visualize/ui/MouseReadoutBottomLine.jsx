/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Chip, Stack, Switch, Tooltip, Typography} from '@mui/joy';
import React, {memo, useEffect, useRef, useState} from 'react';
import {object, bool, number} from 'prop-types';
import BrowserInfo from '../../util/BrowserInfo.js';
import {EMPTY_BUNIT_DEFAULT} from '../FitsHeaderUtil';
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {showMouseReadoutFluxRadixDialog} from './MouseReadoutOptionPopups.jsx';
import {getNonFluxDisplayElements, getFluxInfo} from './MouseReadoutUIUtil.js';
import {CopyToClipboard} from './MouseReadout.jsx';
import {dispatchChangeLockByClick, HIPS_STANDARD_READOUT} from '../MouseReadoutCntlr.js';
import {ToolbarButton} from 'firefly/ui/ToolbarButton.jsx';
import {showMouseReadoutPopout} from 'firefly/visualize/ui/MouseReadPopoutAll.jsx';
import LaunchOutlinedIcon from '@mui/icons-material/LaunchOutlined';

export function MouseReadoutBottomLine({readout, readoutData, readoutShowing, style, slightlyTransparent=false, showOnInactive= false, radix}){

    const {current:divref}= useRef({element:undefined});
    const [haveDivRef,setHaveDivRef]= useState(false);

    useEffect( () => {
        setHaveDivRef(Boolean(divref.element));
    }, [divref?.element,haveDivRef]);


    const width= divref?.element?.getBoundingClientRect()?.width ?? 200;

    const {readoutType}= readoutData;
    if (!readoutData.readoutItems) return (<div style={{height: showOnInactive?20:0, width:showOnInactive?1:0}}/>);

    const isHiPS= readoutType===HIPS_STANDARD_READOUT;
    const displayEle= getNonFluxDisplayElements(readoutData,  readout.readoutPref, isHiPS);
    const {readout1, showReadout1PrefChange, waveLength, bandWidth}= displayEle;
    const r1Value= readout1?.value ??'';
    const wlValue= waveLength?.value ??'';
    const bwValue= bandWidth?.value ??'';
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

    const fullSize= width>520;
    const {threeColor= false}= readoutData;
    const monoFont= radix===16;

    const doWL= ((r1Value && fullSize) || !r1Value) && waveLength && !isHiPS;
    const doFlux= fullSize && !isHiPS;

    const lockByClickLabelWidth= isHiPS ? 450 : threeColor ?  750 : 600;
    const checkboxText= width>lockByClickLabelWidth ? lockByClick ? 'Click Lock: on': 'Click Lock: off' : '';

    const label3C= threeColor && doFlux ? get3CLabel(fluxArray) : '';
    const value3C= threeColor && doFlux ? get3CValue(fluxArray) : '';
    const labelStand= !threeColor && doFlux ? fluxArray[0].label||'Value:' : '';
    const valueStand= !threeColor && doFlux ? fluxArray[0].value : '';

    const fluxLabel= threeColor ? label3C : labelStand;
    const fluxValue= threeColor ? value3C : valueStand;
    const fluxWidth= threeColor ? '9.5rem' : '8rem';

    let wlStr= doWL ? `${wlValue}${bwValue?' / ':''}${bwValue||''}` : '';

    return (
        <Stack {...{direction:'row', sx, ref: (c) => divref.element=c}}>
            <Stack {...{direction:'row', alignItems:'center', sx:{'& .ff-readout-value':{pl:.25}} }}>

                <ToolbarButton icon={<LaunchOutlinedIcon/>}
                               tip='Show expanded readout, thumbnail and magnifier'
                               onClick={() => showMouseReadoutPopout()}/>

                <LabelItem {...{showCopy, label:readout1.label, value:r1Value, copyValue:readout1.copyValue,
                    sx:{pl:1}, prefChangeFunc:showReadout1PrefChange}}/>
                <DataItem {...{value:r1Value,
                    sx:{
                        minWidth: r1Value.length<3&&wlValue ? '2.5rem' : '13rem',
                        } }}/>

                {doFlux && <LabelItem {...{label:fluxLabel, value:fluxValue, sx:{pl:1},
                           prefChangeFunc:() => showMouseReadoutFluxRadixDialog(readout.readoutPref)}}/> }
                {doFlux && <DataItem {...{value:fluxValue, unit: threeColor? '' :fluxArray[0].unit,
                    sx:{minWidth:fluxWidth}, monoFont}}/> }


                {doWL && <LabelItem {...{label:waveLength.label, value:wlValue, sx:{pl:1}}}/> }
                {doWL && <DataItem {...{value:wlStr}}/> }

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
    const isEmptyUnit= unit===EMPTY_BUNIT_DEFAULT;
    const mStyle= monoFont ? {fontFamily:'monospace', whiteSpace:'nowrap'} : {whiteSpace:'nowrap'};
    const vStr=isEmptyUnit ? value + ' (no units defined in file)'  : value+' ' + unit;
    return (
        <Stack {...{direction:'row', className:'ff-readout-value', alignItems:'center', sx }}>
            <Typography level='body-sm' color='warning'  title={vStr} sx={mStyle}>{value}</Typography>
            {unit && <Typography level='body-sm' color={!isEmptyUnit ? 'warning' : undefined}  title={vStr}
                                 sx={{pl:.25, whiteSpace: 'nowrap', opacity:isEmptyUnit?.35:1}}>{unit}</Typography>}
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