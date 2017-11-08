/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {isEmpty} from 'lodash';
import {getDrawLayerByType, isDrawLayerAttached } from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {dispatchCreateDrawLayer,
     getDlAry,
    dispatchAttachLayerToPlot,
    dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';


export function SimpleLayerOnOffButton({plotView:pv,tip,typeId,iconOn,iconOff,visible,
                                            todo, isIconOn, onClick,allPlots= true }) {
    var enabled= pv ? true : false;
    var isOn= isIconOn;
    if (typeId && pv) {
        const distLayer= getDrawLayerByType(getDlAry(),typeId);
        isOn=  distLayer && isDrawLayerAttached(distLayer,pv.plotId);
    }

    return (
        <ToolbarButton icon={isOn ? iconOn : iconOff}
                       tip={tip}
                       enabled={enabled}
                       horizontal={true}
                       visible={visible}
                       todo={todo}
                       onClick={() => onClick ? onClick(pv,!isOn) : onOff(pv,typeId,allPlots,todo)}/>
    );
}

SimpleLayerOnOffButton.propTypes= {
    plotView : PropTypes.object,
    typeId :  PropTypes.string,
    tip : PropTypes.string,
    iconOn : PropTypes.string,
    visible : PropTypes.bool.isRequired,
    todo: PropTypes.bool,
    iconOff : PropTypes.string,
    onClick : PropTypes.func,
    isIconOn : PropTypes.bool,
    allPlots: PropTypes.bool
};

SimpleLayerOnOffButton.defaultProps= {
    todo : false
};



function onOff(pv,typeId,allPlots, todo) {
    if (!pv || !typeId) return;

    if (todo) {
        console.log(`todo: ${typeId}`);
    }
    const dl= getDrawLayerByType(getDlAry(), typeId);
    if (!dl) {
        dispatchCreateDrawLayer(typeId);
    }

    if (!isDrawLayerAttached(dl,pv.plotId)) {
        dispatchAttachLayerToPlot(typeId,pv.plotId,allPlots);
    }
    else {
        dispatchDetachLayerFromPlot(typeId,pv.plotId,allPlots,dl.destroyWhenAllDetached);
    }
}

