/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState} from 'react';
import PropTypes from 'prop-types';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';
import {PopupPanel} from './PopupPanel.jsx';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {SketchPicker} from 'react-color';
import HelpIcon from 'firefly/ui/HelpIcon.jsx';
import {getRGBA} from 'firefly/util/Color.js';


const DEF_PRESET_COLORS= ['#D0021B', '#F5A623', '#F8E71C', '#8B572A', '#7ED321', '#417505', '#BD10E0',
    '#9013FE', '#4A90E2', '#50E3C2', '#B8E986', '#000000', '#4A4A4A', '#9B9B9B', '#FFFFFF'];

const dialogTip=
`Choose a color:
- graphically
- as an RGB hex color code (does not support alpha)
- by entering RGB values (0-255) and an alpha in percent (0-100)
- from the swatches at the bottom`;


export function showColorPickerDialog(color, callbackOnOKOnly, callbackOnBoth, cb, Id = '',
                                      helpId, presetAlpha,  presetColors) {
    const popup= (
        <PopupPanel title={'Color Picker'+ (Id ? ` - ${Id}`: '')} >
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
        <div>
            <div title={dialogTip}>
                <SketchPicker color={currentColor} presetColors={psColors}
                              onChangeComplete={updateColor}
                              onChange={(ev) => updateStateFromRGBA(ev.rgb)}/>
            </div>
            <div style={{
                display:'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '3px 5px 3px 6px'
            }}>
                <CompleteButton onSuccess={() => (callbackOnOKOnly||callbackOnBoth) && callback(lastEv,true)}
                                dialogId='ColorPickerDialog'/>
                <div style={{ textAlign:'center'}}>
                    <HelpIcon helpId={helpId} />
                </div>
            </div>
        </div>
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
