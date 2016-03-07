/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import RadioGroupInputFieldView from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField, dispatchForceDrawLayerUpdate} from '../visualize/DrawLayerCntlr.js';
import AppDataCntlr from '../core/AppDataCntlr.js';

export const getUIComponent = (drawLayer,pv) => <NorthUpCompassUI drawLayer={drawLayer} pv={pv}/>;

function NorthUpCompassUI({drawLayer,pv}) {

    const tStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap',
        minWidth: '3em',
        paddingLeft : 5
    };

    var options= [ {label: 'all', value: 'all'},
                   {label: 'row', value: 'row'},
                   {label: 'image', value: 'image'},
    ];


    var pref= options[0];

    return (
        <div>
            <div>
                Overlay:
                <div style={{display:'inline-block', paddingLeft:7}}>
                    <RadioGroupInputFieldView options={options} fieldKey='overlay_option' value={pref.value}
                                              onChange={(ev) => changeOverlayPref(drawLayer,pv,ev.target.value)} />
                </div>
            </div>
        </div>
    );
}
function changeOverlayPref(drawLayer,pv,value) {
    throw Error('Not implemented');
}


NorthUpCompassUI.propTypes= {
    drawLayer     : React.PropTypes.object.isRequired,
    pv            : React.PropTypes.object.isRequired
};

