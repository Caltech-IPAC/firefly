
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField} from '../visualize/DrawLayerCntlr.js';
import {Style} from '../visualize/draw/DrawingDef.js';

const options= [ {label: 'outline', value: 'outline'},
                 {label: 'fill', value: 'fill'}];


export const getUIComponent = (drawLayer,pv) => <HiPSMOCUI drawLayer={drawLayer} pv={pv}/>;

function HiPSMOCUI({drawLayer,pv}) {
    const {style}= drawLayer.drawingDef || {};
    const fillStyle = (!style || style === Style.STANDARD) ? 'outline' : 'fill';
    const {showLabel=false} = drawLayer;

    return (
            <div style={{display: 'flex', width: '48%', padding: '1px 2px 1px 2px',
                         border: '1px solid rgba(60,60,60,.2', borderRadius: '5px'}}>
                <RadioGroupInputFieldView options={options}  value={fillStyle}
                                              buttonGroup={true}
                                              onChange={(ev) => changeMocPref(drawLayer,pv,ev.target.value)} />
                <div style={{paddingLeft: 3}} title={'show nuniq value on cells'}>
                    <input type='checkbox' checked={showLabel} onChange={() => enableLabelDisplay(drawLayer, pv, !showLabel)} />
                    show nuniq
                </div>
            </div>
    );
}



function changeMocPref(drawLayer,pv,value) {
    const {style} = drawLayer.drawingDef || {};

    if ((style === Style.STANDARD && value === 'fill') || (style !== Style.STANDARD && value === 'outline')) {
        dispatchModifyCustomField(drawLayer.drawLayerId, {fillStyle: value}, pv.plotId);
    }
}

function enableLabelDisplay(dl, pv, bDisplay) {
    dispatchModifyCustomField(dl.drawLayerId, {showLabel: bDisplay}, pv.plotId);
}

HiPSMOCUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

