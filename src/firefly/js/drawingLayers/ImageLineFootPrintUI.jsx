
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchChangeDrawingDef} from '../visualize/DrawLayerCntlr.js';
import {Style} from '../visualize/draw/DrawingDef.js';

const options= [ {label: 'outline', value: 'outline'},
                 {label: 'outline/text', value: 'outline_text'},
                 {label: 'fill', value: 'fill'}];


export const getUIComponent = (drawLayer,pv) => <ImageLineFootPrintUI drawLayer={drawLayer} pv={pv}/>;

function ImageLineFootPrintUI({drawLayer,pv}) {
    const {style, showText} = drawLayer.drawingDef || {};
    const fillStyle = (!style || style.key === 'STANDARD') ? (showText ? 'outline_text' : 'outline') : 'fill';

    return (
        <RadioGroupInputFieldView
            sx={{p:1/4}}
            options={options}  value={fillStyle}
            buttonGroup={true}
            onChange={(ev) => changeFootprintPref(drawLayer,pv,ev.target.value, fillStyle)} />
    );
}

function changeFootprintPref(drawLayer,pv,value, preValue) {
    if (preValue===value) return;
    const style = value.includes('outline') ? Style.STANDARD : Style.FILL;
    const showText = value.includes('text');
    dispatchChangeDrawingDef(drawLayer.drawLayerId, {...drawLayer.drawingDef, style, showText}, pv.plotId);
}

ImageLineFootPrintUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};
