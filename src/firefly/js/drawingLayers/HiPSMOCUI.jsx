
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {object} from 'prop-types';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField} from '../visualize/DrawLayerCntlr.js';
import {Style} from '../visualize/draw/DrawingDef.js';

const options= [
    {label: 'Outline', value: Style.DESTINATION_OUTLINE.key},
    {label: 'Fill', value: Style.FILL.key},
    {label: 'Auto', value: Style.AUTO.key},
    {label: 'MOC Tile Outline', value: Style.STANDARD.key},
];


export const getUIComponent = (drawLayer,pv) => <HiPSMOCUI drawLayer={drawLayer} pv={pv}/>;

function HiPSMOCUI({drawLayer:dl,pv}) {
    const style = dl?.requestedStyle ?? dl?.mocStyle?.[pv.plotId] ?? dl.drawingDef?.style ?? Style.DESTINATION_OUTLINE;

    return (
        <RadioGroupInputFieldView options={options}  value={style.key}
                                  buttonGroup={true}
                                  onChange={(ev) => changeMocPref(dl,pv,ev.target.value, style.key)} />
    );
}

function changeMocPref(drawLayer,pv,value, prevValue) {
    if (prevValue !== value) {
        dispatchModifyCustomField(drawLayer.drawLayerId, {fillStyle: value, targetPlotId: pv.plotId}, pv.plotId);
    }
}

HiPSMOCUI.propTypes= {
    drawLayer     : object.isRequired,
    pv            : object.isRequired
};

