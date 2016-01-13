/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {getDrawLayerByType, isDrawLayerAttached } from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {dispatchCreateDrawLayer,
    dispatchAttachLayerToPlot,
    dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';


export function SimpleLayerOnOffButton({plotView:pv,tip,dlAry,typeId,iconOn,iconOff,visible,todo}) {
    var enabled= pv && dlAry ? true : false;
    const distLayer= getDrawLayerByType(dlAry,typeId);
    var isOn=  distLayer && isDrawLayerAttached(distLayer,pv.plotId);

    return (
        <ToolbarButton icon={isOn ? iconOn : iconOff}
                       tip={tip}
                       enabled={enabled}
                       horizontal={true}
                       visible={visible}
                       todo={todo}
                       onClick={() => onOff(pv,dlAry,typeId,todo)}/>
    );
}

SimpleLayerOnOffButton.propTypes= {
    plotView : React.PropTypes.object,
    dlAry : React.PropTypes.arrayOf(React.PropTypes.object),
    typeId :  React.PropTypes.string.isRequired,
    tip : React.PropTypes.string,
    iconOn : React.PropTypes.string,
    visible : React.PropTypes.bool.isRequired,
    todo: React.PropTypes.bool,
    iconOff : React.PropTypes.string
};

SimpleLayerOnOffButton.defaultProps= {
    todo : false
};



function onOff(pv,dlAry,typeId,todo) {
    if (!pv) return;

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

