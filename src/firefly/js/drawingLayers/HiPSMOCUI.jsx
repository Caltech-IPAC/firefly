
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField} from '../visualize/DrawLayerCntlr.js';
import {Style} from '../visualize/draw/DrawingDef.js';

const options= [ {label: 'Outline', value: Style.DESTINATION_OUTLINE.key},
                 {label: 'Fill', value: Style.FILL.key},
                 {label: 'MOC Tile Outline', value: Style.STANDARD.key},
];


export const getUIComponent = (drawLayer,pv) => <HiPSMOCUI drawLayer={drawLayer} pv={pv}/>;

function HiPSMOCUI({drawLayer,pv}) {
    const style = drawLayer?.mocStyle?.[pv.plotId] ?? drawLayer.drawingDef?.style ?? Style.DESTINATION_OUTLINE;

    return (
            <div style={{display: 'inline-flex', padding: '2px 3px 2px 3px',
                         border: '1px solid rgba(60,60,60,.2', borderRadius: '5px'}}>
                <RadioGroupInputFieldView options={options}  value={style.key}
                                          buttonGroup={true}
                                          onChange={(ev) => changeMocPref(drawLayer,pv,ev.target.value, style.key)} />
            </div>
    );
}

function changeMocPref(drawLayer,pv,value, prevValue) {
    if (prevValue !== value) {
        dispatchModifyCustomField(drawLayer.drawLayerId, {fillStyle: value, targetPlotId: pv.plotId}, pv.plotId);
    }
}

HiPSMOCUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

