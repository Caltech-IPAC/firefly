/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField, dispatchForceDrawLayerUpdate} from '../visualize/DrawLayerCntlr.js';
import * as AppDataCntlr from '../core/AppDataCntlr.js';
import {DIST_READOUT, getUnitPreference, getUnitStyle, UNIT_NO_PIXEL, UNIT_ALL} from './DistanceTool.js';
import {hasWCSProjection} from '../visualize/PlotViewUtil';
import CsysConverter from '../visualize/CsysConverter';


export const getUIComponent = (drawLayer,pv) => <DistanceToolUI drawLayer={drawLayer} pv={pv}/>;

function DistanceToolUI({drawLayer,pv}) {

    const tStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap',
        minWidth: '3em',
        paddingLeft : 5
    };
    const checked= drawLayer.offsetCal;
    const plot = pv.plots[pv.primeIdx];
    const cc = CsysConverter.make(plot);
    const isWorld = hasWCSProjection(cc);
    const worldUnit = [ {label: 'degrees', value: 'deg'},
                        {label: 'arcminutes', value: 'arcmin'},
                        {label: 'arcseconds', value: 'arcsec'}];
    const pixelUnit = [{label: 'pixels', value: 'pixel'}];
    const unitStyle = getUnitStyle(cc, isWorld);

    const pref = getUnitPreference(unitStyle);
    const options = (unitStyle === UNIT_ALL) ? worldUnit.concat(pixelUnit)
                                             : ((unitStyle === UNIT_NO_PIXEL)? worldUnit : pixelUnit);

    return (
        <div>

            <div style={{padding:'5px 0 9px 0'}}>
                <input type='checkbox'
                       checked={checked}
                       onChange={() => dispatchModifyCustomField( drawLayer.displayGroupId,
                                                              {offsetCal:!checked}, pv.plotId )}
                />
                <div style={tStyle}>Offset Calculation</div>
            </div>
            <div>
                Unit:
                <div style={{display:'inline-block', paddingLeft:7}}>
                    <RadioGroupInputFieldView options={options}  value={pref}
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
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

