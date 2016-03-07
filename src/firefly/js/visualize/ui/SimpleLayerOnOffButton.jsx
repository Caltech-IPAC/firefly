/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {getDrawLayerByType, isDrawLayerAttached } from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {dispatchCreateDrawLayer,
    dispatchAttachLayerToPlot,
    dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';


export function SimpleLayerOnOffButton({plotView:pv,tip,dlAry,typeId,iconOn,iconOff,visible,todo, isIconOn, onClick}) {
    var enabled= pv ? true : false;
    var isOn= isIconOn;
    if (typeId) {
        const distLayer= getDrawLayerByType(dlAry,typeId);
        isOn=  distLayer && isDrawLayerAttached(distLayer,pv.plotId);
    }

    return (
        <ToolbarButton icon={isOn ? iconOn : iconOff}
                       tip={tip}
                       enabled={enabled}
                       horizontal={true}
                       visible={visible}
                       todo={todo}
                       onClick={() => onClick ? onClick(pv,!isOn) : onOff(pv,dlAry,typeId,todo)}/>
    );
}

SimpleLayerOnOffButton.propTypes= {
    plotView : PropTypes.object,
    dlAry : PropTypes.arrayOf(React.PropTypes.object),
    typeId :  PropTypes.string,
    tip : PropTypes.string,
    iconOn : PropTypes.string,
    visible : PropTypes.bool.isRequired,
    todo: PropTypes.bool,
    iconOff : PropTypes.string,
    onClick : PropTypes.func,
    isIconOn : PropTypes.bool
};

SimpleLayerOnOffButton.defaultProps= {
    todo : false
};



function onOff(pv,dlAry,typeId,todo) {
    if (!pv || !dlAry || !typeId) return;

    if (todo) {
        console.log('todo');
        return;
    }

    var dl= getDrawLayerByType(dlAry, typeId);
    if (!dl) {
        dispatchCreateDrawLayer(typeId);
    }

    if (!isDrawLayerAttached(dl,pv.plotId)) {
        dispatchAttachLayerToPlot(typeId,pv.plotId,true);
    }
    else {
        dispatchDetachLayerFromPlot(typeId,pv.plotId,true);
    }
}

