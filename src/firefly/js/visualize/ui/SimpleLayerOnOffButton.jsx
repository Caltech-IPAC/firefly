/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {getDrawLayerByType, isDrawLayerAttached, primePlot } from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';
import {dispatchCreateDrawLayer,
        getDlAry,
        dispatchAttachLayerToPlot,
        dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';


export function SimpleLayerOnOffButton({plotView:pv,tip,typeId,iconOn,iconOff,visible,
                                            plotTypeMustMatch= false, style={},
                                            todo= false, isIconOn, onClick, dropDown, allPlots= true }) {
    const enabled= Boolean(primePlot(pv));
    let isOn= isIconOn;
    if (typeId && pv) {
        const distLayer= getDrawLayerByType(getDlAry(),typeId);
        isOn=  distLayer && isDrawLayerAttached(distLayer,pv.plotId);
    }

    if (dropDown && !isOn) {
        return (
            <DropDownToolbarButton  icon={iconOff}
                                    tip='Select an area for cropping or statistics'
                                    enabled={enabled}
                                    horizontal={true}
                                    visible={visible}
                                    dropDown={dropDown} />
        );
    } else {

        return (
            <ToolbarButton icon={isOn ? iconOn : iconOff}
                           tip={tip}
                           enabled={enabled}
                           horizontal={true}
                           visible={visible}
                           todo={todo}
                           style={style}
                           onClick={() => onClick ? onClick(pv,!isOn) : onOff(pv,typeId,allPlots,plotTypeMustMatch,todo)}/>
        );
    }
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
    allPlots: PropTypes.bool,
    plotTypeMustMatch: PropTypes.bool,
    dropDown: PropTypes.object,
    style : PropTypes.object,
};

SimpleLayerOnOffButton.defaultProps= {
    todo : false
};



function onOff(pv,typeId,allPlots, plotTypeMustMatch, todo) {
    if (!pv || !typeId) return;

    if (todo) {
        console.log(`todo: ${typeId}`);
    }
    const dl= getDrawLayerByType(getDlAry(), typeId);
    if (!dl) {
        dispatchCreateDrawLayer(typeId);
    }

    if (!isDrawLayerAttached(dl,pv.plotId)) {
        dispatchAttachLayerToPlot(typeId,pv.plotId,allPlots,true, plotTypeMustMatch);
    }
    else {
        dispatchDetachLayerFromPlot(typeId,pv.plotId,allPlots,dl.destroyWhenAllDetached);
    }
}

