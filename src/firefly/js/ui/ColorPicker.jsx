/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack, Tooltip, Typography} from '@mui/joy';
import React, {useState} from 'react';
import PropTypes from 'prop-types';
import {dispatchHideDialog, dispatchShowDialog} from '../core/ComponentCntlr.js';
import {PopupPanel} from './PopupPanel.jsx';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {SketchPicker} from 'react-color';
import HelpIcon from 'firefly/ui/HelpIcon.jsx';
import {getRGBA} from 'firefly/util/Color.js';


const DEF_PRESET_COLORS= ['#D0021B', '#F5A623', '#F8E71C', '#8B572A', '#7ED321', '#417505', '#BD10E0',
    '#9013FE', '#4A90E2', '#50E3C2', '#B8E986', '#000000', '#4A4A4A', '#9B9B9B', '#FFFFFF'];

const dialogTip= (
    <Typography component='div' sx={{'ul li': {listStyleType:'circle'}}}>
        Choose a color
        <ul>
            <li>graphically</li>
            <li>as an RGB hex color code (does not support alpha)</li>
            <li>by entering RGB values (0-255) and an alpha in percent (0-100)</li>
            <li>from the swatches at the bottom</li>
        </ul>
    </Typography>);


export const hideColorPickerDialog= () => dispatchHideDialog('ColorPickerDialog');

export function showColorPickerDialog(color, callbackOnOKOnly, callbackOnBoth, cb, Id = '',
                                      helpId, presetAlpha,  presetColors, postTitle) {
    const popup= (
        <PopupPanel title={'Color Picker'+ (Id ? ` - ${Id}`: '') + (postTitle ? ` - ${postTitle}`: '')} >
            <ColorPickerWrapper callback={cb} color={color} helpId={helpId}
                                callbackOnOKOnly={callbackOnOKOnly} callbackOnBoth={callbackOnBoth}
                                presetColors={presetColors}  presetAlpha={presetAlpha} />
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ColorPickerDialog', popup);
    setTimeout( () => dispatchShowDialog('ColorPickerDialog'), 0);
}

var lastEv;

function ColorPickerWrapper ({callback,color,callbackOnOKOnly, callbackOnBoth,
                                 helpId= 'visualization.colorpicker',
                                 presetAlpha,  presetColors=DEF_PRESET_COLORS}) {
    const [currentColor, setCurrentColor] = useState(color);

    const updateStateFromRGBA= (rgba) =>  {
        const {r,g,b,a}= rgba;
        setCurrentColor(`rgba(${r},${g},${b},${a})`);
    };

    const updateColor= (ev) => {
        lastEv=ev;
        updateStateFromRGBA(ev.rgb);
        if (!callbackOnOKOnly) callback?.(ev,false);
    };

    const psColors= !presetAlpha ? presetColors :
        presetColors.map( (c) => {
            const [r,g,b]= getRGBA(c);
            return `rgba(${r},${g},${b},${presetAlpha})`;
        });


    return (
        <Tooltip title={dialogTip} placement='right'>
            <Stack spacing={1}>
                <SketchPicker color={currentColor} presetColors={psColors}
                              styles={{
                                  picker:{
                                      minWidth: '20rem',
                                      padding: 8,
                                  }
                              }}
                              onChangeComplete={updateColor}
                              onChange={(ev) => updateStateFromRGBA(ev.rgb)}/>
                <Stack {...{ direction:'row', justifyContent: 'space-between', alignItems: 'center'}}>
                    <CompleteButton
                        text={(callbackOnOKOnly||callbackOnBoth)? 'OK' : 'Close'}
                        onSuccess={() => {
                        (callbackOnOKOnly||callbackOnBoth) && callback(lastEv,true);
                        dispatchHideDialog('ColorPickerDialog');
                    }} />
                    <div style={{ textAlign:'center'}}>
                        <HelpIcon helpId={helpId} />
                    </div>
                </Stack>
            </Stack>
        </Tooltip>
    );
}

ColorPickerWrapper.propTypes= {
    callback: PropTypes.func.isRequired,
    color: PropTypes.string.isRequired,
    callbackOnOKOnly: PropTypes.bool.isRequired,
    callbackOnBoth: PropTypes.bool.isRequired,
    presetAlpha: PropTypes.number,
    presetColors: PropTypes.arrayOf(PropTypes.string)
};
