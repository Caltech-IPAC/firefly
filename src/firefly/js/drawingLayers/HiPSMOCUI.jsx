
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {object} from 'prop-types';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {Style} from '../visualize/draw/DrawingDef.js';
import {dispatchModifyCustomField} from '../visualize/DrawLayerDispatch';
import {mocUIDisplayOptions} from '../visualize/HiPSMocUtil';




export const getUIComponent = (drawLayer,pv) => <HiPSMOCUI drawLayer={drawLayer} pv={pv}/>;

function HiPSMOCUI({drawLayer:dl,pv}) {
    const style = dl?.requestedStyle ?? dl?.mocStyle?.[pv.plotId] ?? dl.drawingDef?.style ?? Style.DESTINATION_OUTLINE;

    return (
        <RadioGroupInputFieldView options={mocUIDisplayOptions} value={style.key}
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

