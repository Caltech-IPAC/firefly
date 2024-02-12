/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Checkbox, Stack, Typography} from '@mui/joy';
import React from 'react';
import PropTypes from 'prop-types';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField, dispatchForceDrawLayerUpdate} from '../visualize/DrawLayerCntlr.js';
import * as AppDataCntlr from '../core/AppDataCntlr.js';
import {DIST_READOUT, getUnitPreference, getUnitStyle, UNIT_NO_PIXEL, UNIT_ALL} from './DistanceTool.js';
import {hasWCSProjection} from '../visualize/PlotViewUtil';
import CsysConverter from '../visualize/CsysConverter';


export const getUIComponent = (drawLayer,pv) => <DistanceToolUI drawLayer={drawLayer} pv={pv}/>;
const worldUnit = [ {label: 'degrees', value: 'deg'},
    {label: 'arcminutes', value: 'arcmin'},
    {label: 'arcseconds', value: 'arcsec'}];
const pixelUnit = [{label: 'pixels', value: 'pixel'}];

function DistanceToolUI({drawLayer,pv}) {
    const checked= drawLayer.offsetCal;
    const plot = pv.plots[pv.primeIdx];
    const cc = CsysConverter.make(plot);
    const isWorld = hasWCSProjection(cc);
    const unitStyle = getUnitStyle(cc, isWorld);
    const pref = getUnitPreference(unitStyle);
    const options = (unitStyle === UNIT_ALL) ? worldUnit.concat(pixelUnit)
                                             : ((unitStyle === UNIT_NO_PIXEL)? worldUnit : pixelUnit);

    return (
        <Stack direction='column' spacing={1}>
            <Checkbox {...{checked, label:'Offset Calculation',
                onChange:() => dispatchModifyCustomField( drawLayer.displayGroupId,
                    {offsetCal:!checked}, pv.plotId )
            }} />
            <Stack direction='row' spacing={1} alignItems='center'>
                <Typography level='body-sm'>Unit:</Typography>
                <RadioGroupInputFieldView options={options}  value={pref}
                                          orientation='horizontal'
                                          onChange={(ev) => changeReadoutPref(drawLayer,pv,ev.target.value)} />
            </Stack>
        </Stack>
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

