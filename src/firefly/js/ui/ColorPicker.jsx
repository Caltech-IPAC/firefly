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



export function showColorPickerDialog(color, callbackOnOKOnly, callbackOnBoth, cb, Id = '' ) {
    const popup= (
        <PopupPanel title={'Color Picker'+ (Id ? ` - ${Id}`: '')} >
            <ColorPickerWrapper callback={cb} color={color} callbackOnOKOnly={callbackOnOKOnly} callbackOnBoth={callbackOnBoth}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ColorPickerDialog', popup);
    setTimeout( () => dispatchShowDialog('ColorPickerDialog'), 0);
}

var lastEv;

function ColorPickerWrapper ({callback,color,callbackOnOKOnly, callbackOnBoth}) {
    const [currentColor, setCurrentColor] = useState(color);

    const onChange = (ev) => {
        lastEv=ev;
        const {r,g,b,a}= ev.rgb;
        setCurrentColor(`rgba(${r},${g},${b},${a})`);
        if (!callbackOnOKOnly && callback) callback(ev,false);
    };

    return (
        <div>
            <SketchPicker onChange={onChange}
                         color={currentColor} />
            <CompleteButton onSuccess={() => callbackOnOKOnly||callbackOnBoth ? callback(lastEv,true): null}
                            dialogId='ColorPickerDialog'/>
        </div>
    );
}

ColorPickerWrapper.propTypes= {
    callback: PropTypes.func.isRequired,
    color: PropTypes.string.isRequired,
    callbackOnOKOnly: PropTypes.bool.isRequired,
    callbackOnBoth: PropTypes.bool.isRequired
};
