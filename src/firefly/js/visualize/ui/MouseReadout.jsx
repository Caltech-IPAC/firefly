/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Chip, Stack, Switch, Typography} from '@mui/joy';
import React, {Fragment,memo, useState} from 'react';
import {number,string,oneOfType,object,func,bool} from 'prop-types';
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {dispatchChangeLockByClick} from '../MouseReadoutCntlr.js';
import {copyToClipboard} from '../../util/WebUtil';
import {ToolbarButton} from '../../ui/ToolbarButton';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import AssignmentTurnedInOutlinedIcon from '@mui/icons-material/AssignmentTurnedInOutlined';


export const MouseReadoutLock= memo(({gArea, gAreaLabel, style={}, lockByClick}) => {
    const s= gArea ? {gridArea:gArea,...style} : style;
    const label= lockByClick ? 'Click Lock: on' : 'Click Lock: off' ;
    return (
        <React.Fragment>
            <Stack direction='row' style={s} alignSelf='center' title='Click on an image to lock the display at that point.'>
                <Switch size='sm' endDecorator={gAreaLabel?'':label} checked={lockByClick}
                          onChange={() => {
                              dispatchChangePointSelection('mouseReadout', !lockByClick);
                              dispatchChangeLockByClick(!lockByClick);
                          }} />
            </Stack>
            {gAreaLabel &&
            <Typography level='body-sm'
                sx={ {
                    gridArea: gAreaLabel,
                    position:'relative',
                    whiteSpace:'nowrap',
                    textOverflow: 'ellipsis',
                    overflow:'hidden',
                    pr:.5,
                    minWidth: 20
                } }>
                {label}
            </Typography>}
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
const baseLS={whiteSpace:'nowrap', textOverflow: 'ellipsis'};


export const DataReadoutItem= memo(({lArea, vArea, cArea, labelStyle={}, valueStyle={}, showCopy=false,
                                        label='', value='', unit='', copyValue='', prefChangeFunc=undefined, monoFont=false}) => {
    const lS= lArea ? {gridArea:lArea,...baseLS,...labelStyle} : {...baseLS,...labelStyle};
    const vS= vArea ? {gridArea:vArea,...baseVS, ...valueStyle} : {...baseVS,...valueStyle};
    const cS= cArea ? {gridArea:cArea, overflow:'hidden', justifySelf:'center'} : undefined;
    const mouseReadoutLabelSx = {
        cursor: 'default',
        justifySelf: 'end',
        ...(prefChangeFunc && {
            cursor: 'pointer',
            textDecoration: 'underline',
            fontStyle: 'italic',
            })
    };
    const copyTitle= `Copy to clipboard: ${copyValue||value}`;

    let clipComponent= undefined;
    if (cArea) {
        clipComponent= (value && showCopy) ?
            <CopyToClipboard style={cS} title={copyTitle} value={copyValue||value} /> : <div style={cS}/>;
    }

    const mStyle= monoFont ? {fontFamily:'monospace'} : {};

    return (
        <Fragment>
            {prefChangeFunc
                ? <Chip variant='soft' color='neutral' title={value+''} sx={{borderRadius:5, ...lS}} onClick={prefChangeFunc}>{label}</Chip>
                : <Typography level='body-sm' title={value+''} sx={{...mouseReadoutLabelSx, ...lS}} onClick={prefChangeFunc}>{label}</Typography>
            }
            <Typography level='body-sm' color='warning' sx={{...vS, ...mStyle}} title={value+''}> {value} </Typography>
            <Typography level='body-sm' color='warning' sx={vS} title={value+''}>
                <span style={mStyle}> {value}</span>
                <span> {unit}</span>
            </Typography>
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
    monoFont:       bool,
};

export function CopyToClipboard({value, title, style, size=12, buttonStyle={}}) {
    const uncheckedIco = <ContentPasteOutlinedIcon sx={{width:size,height:size}}/>;
    const checkedIco = <AssignmentTurnedInOutlinedIcon sx={{width:size,height:size}}/>;
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
            <ToolbarButton icon={clipIcon} tip={title} imageStyle={{height:size, width:size}}
                           sx={{...buttonStyle,'& .ff-toolbar-iconbutton' : {padding:'0'}}}
                           onClick={() => doCopy(value)} />
        </div>
    );

}




