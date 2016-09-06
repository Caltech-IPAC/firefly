/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';
import {PopupPanel} from './PopupPanel.jsx';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {SketchPicker} from 'react-color';



export function showColorPickerDialog(color,callbackOnOKOnly, callbackOnBoth, cb ) {
    const popup= (
        <PopupPanel title={'Color Picker'} >
            <ColorPickerWrapper callback={cb} color={color} callbackOnOKOnly={callbackOnOKOnly} callbackOnBoth={callbackOnBoth}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ColorPickerDialog', popup);
    setTimeout( () => dispatchShowDialog('ColorPickerDialog'), 0);
}

var lastEv;

function ColorPickerWrapper ({callback,color,callbackOnOKOnly, callbackOnBoth}) {
    return (
        <div>
            <SketchPicker
                         onChangeComplete={ (ev) => {
                             lastEv=ev;
                             if (!callbackOnOKOnly) callback(ev, false);
                         } }
                         color={color} />
            <CompleteButton onSuccess={() => callbackOnOKOnly||callbackOnBoth ? callback(lastEv,true): null}
                            dialogId='ColorPickerDialog'/>
        </div>
    );
}

ColorPickerWrapper.propTypes= {
    callback: React.PropTypes.func.isRequired,
    color: React.PropTypes.string.isRequired,
    callbackOnOKOnly: React.PropTypes.bool.isRequired,
    callbackOnBoth: React.PropTypes.bool.isRequired
};
