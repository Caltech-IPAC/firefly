/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import AppDataCntlr from '../core/AppDataCntlr.js';
import PopupPanel from './PopupPanel.jsx';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import ColorPicker from 'react-color';



export function showColorPickerDialog(color,cb) {
    const popup= (
        <PopupPanel title={'Color Picker'} >
            <ColorPickerWrapper callback={cb} color={color}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ColorPickerDialog', popup);
    setTimeout( () => AppDataCntlr.showDialog('ColorPickerDialog'), 0);
}

var lastEv;

function ColorPickerWrapper ({callback,color}) {
    return (
        <div>
            <ColorPicker type='sketch' onChangeComplete={ (ev) => lastEv=ev } color={color} />
            <CompleteButton onSuccess={() => callback(lastEv)} dialogId='ColorPickerDialog'/>
        </div>
    );
}

ColorPickerWrapper.propTypes= {
    callback: React.PropTypes.func.isRequired,
    color: React.PropTypes.string.isRequired
};
