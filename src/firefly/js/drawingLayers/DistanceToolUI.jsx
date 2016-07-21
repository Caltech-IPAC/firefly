/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField, dispatchForceDrawLayerUpdate} from '../visualize/DrawLayerCntlr.js';
import * as AppDataCntlr from '../core/AppDataCntlr.js';
import {DIST_READOUT,ARC_SEC,ARC_MIN,DEG} from './DistanceTool.js';


export const getUIComponent = (drawLayer,pv) => <DistanceToolUI drawLayer={drawLayer} pv={pv}/>;

function DistanceToolUI({drawLayer,pv}) {

    const tStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap',
        minWidth: '3em',
        paddingLeft : 5
    };
    var checked= drawLayer.posAngle;



    var options= [ {label: 'Degree', value: 'deg'},
                   {label: 'Arcminute', value: 'arcmin'},
                   {label: 'Arcsecond', value: 'arcsec'}
    ];


    var pref= AppDataCntlr.getPreference(DIST_READOUT) || DEG;

    return (
        <div>

            <div style={{padding:'5px 0 9px 0'}}>
                <input type='checkbox'
                       checked={checked}
                       onChange={() => dispatchModifyCustomField( drawLayer.displayGroupId,
                                                              {posAngle:!checked}, pv.plotId )}
                />
                <div style={tStyle}>Offset Calculation</div>
            </div>
            <div>
                Unit:
                <div style={{display:'inline-block', paddingLeft:7}}>
                    <RadioGroupInputFieldView options={options} fieldKey='units' value={pref}
                                              onChange={(ev) => changeReadoutPref(drawLayer,pv,ev.target.value)} />
                </div>
            </div>
        </div>
    );
}


function changeReadoutPref(drawLayer,pv,value) {
    AppDataCntlr.dispatchAddPreference(DIST_READOUT,value);
    dispatchForceDrawLayerUpdate( drawLayer.displayGroupId, pv.plotId);
}

DistanceToolUI.propTypes= {
    drawLayer     : React.PropTypes.object.isRequired,
    pv            : React.PropTypes.object.isRequired
};

