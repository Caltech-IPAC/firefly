/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField, dispatchForceDrawLayerUpdate} from '../visualize/DrawLayerCntlr.js';

export const getUIComponent = (drawLayer,pv) => <NorthUpCompassUI drawLayer={drawLayer} pv={pv}/>;

/*     TODO: NOT USED BUT KEPT FOR FUTURE USE
       Component that goes in layer option windows attached to the northup compass overlay
       Kept here in case we need it.
       To be called when creating a drawing factory passed in as 'getComponentUI' in NorthUpCompass.js
*/
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
                    <RadioGroupInputFieldView options={options}  value={pref.value}
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

