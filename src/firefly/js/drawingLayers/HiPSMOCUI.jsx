
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField} from '../visualize/DrawLayerCntlr.js';
import {Style} from '../visualize/draw/DrawingDef.js';
import {get} from 'lodash';

const options= [ {label: 'outline', value: 'outline'},
                 {label: 'fill', value: 'fill'}];


export const getUIComponent = (drawLayer,pv) => <HiPSMOCUI drawLayer={drawLayer} pv={pv}/>;

function HiPSMOCUI({drawLayer,pv}) {
    const {mocStyle={}} = drawLayer;
    const style = get(mocStyle, [pv.plotId], get(drawLayer, ['drawingDef', 'style'], Style.STANDARD));
    const fillStyle = (!style || style.key === 'STANDARD') ? 'outline' : 'fill';

    return (
            <div style={{display: 'inline-flex', padding: '2px 3px 2px 3px',
                         border: '1px solid rgba(60,60,60,.2', borderRadius: '5px'}}>
                <RadioGroupInputFieldView options={options}  value={fillStyle}
                                          buttonGroup={true}
                                          onChange={(ev) => changeMocPref(drawLayer,pv,ev.target.value, fillStyle)} />
            </div>
    );
}



function changeMocPref(drawLayer,pv,value, preValue) {
    if (preValue !== value) {
        dispatchModifyCustomField(drawLayer.drawLayerId, {fillStyle: value, targetPlotId: pv.plotId}, pv.plotId);
    }
}

HiPSMOCUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

