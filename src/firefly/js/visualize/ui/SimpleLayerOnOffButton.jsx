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
import {clearModalEndInfo, setModalEndInfo} from './ToolbarToolModalEnd.js';


export function SimpleLayerOnOffButton({plotView:pv,tip,typeId,iconOn,iconOff,visible=true,
                                           modalEndInfo, endText, modalLayer= false,
                                            plotTypeMustMatch= false, style={}, enabled= true, imageStyle,
                                            isIconOn, onClick, dropDown, allPlots= true }) {
    const enableButton= Boolean(primePlot(pv)) && enabled;
    let isOn= isIconOn;
    if (typeId && pv) {
        const distLayer= getDrawLayerByType(getDlAry(),typeId);
        isOn=  distLayer && isDrawLayerAttached(distLayer,pv.plotId);
    }

    if (dropDown && !isOn) {
        return (
            <DropDownToolbarButton  icon={iconOff}
                                    tip={tip}
                                    enabled={enableButton}
                                    visible={visible}
                                    imageStyle={imageStyle}
                                    dropDown={dropDown} />
        );
    } else {
        return (
            <ToolbarButton icon={isOn ? iconOn : iconOff}
                           tip={tip}
                           enabled={enableButton}
                           visible={visible}
                           style={style}
                           imageStyle={imageStyle}
                           onClick={() => onClick ? onClick(pv,!isOn) :
                               onOff(pv,typeId,allPlots,plotTypeMustMatch,modalEndInfo, endText,modalLayer)}/>
        );
    }
}

SimpleLayerOnOffButton.propTypes= {
    plotView : PropTypes.object,
    typeId :  PropTypes.string,
    tip : PropTypes.string,
    iconOn : PropTypes.string,
    visible : PropTypes.bool,
    iconOff : PropTypes.string,
    onClick : PropTypes.func,
    isIconOn : PropTypes.bool,
    allPlots: PropTypes.bool,
    plotTypeMustMatch: PropTypes.bool,
    dropDown: PropTypes.object,
    enabled: PropTypes.bool,
    modalLayer: PropTypes.bool,
    style : PropTypes.object,
    modalEndInfo: PropTypes.object,
    endText: PropTypes.string,
    imageStyle : PropTypes.object,
};


export function onOff(pv,typeId,allPlots, plotTypeMustMatch, modalEndInfo, endText, modalLayer= false) {
    if (!pv || !typeId) return;

    const dl= getDrawLayerByType(getDlAry(), typeId);
    if (!dl) {
        dispatchCreateDrawLayer(typeId);
    }

    if (!isDrawLayerAttached(dl,pv.plotId)) {
        dispatchAttachLayerToPlot(typeId,pv.plotId,allPlots,true, plotTypeMustMatch);
        if (modalLayer) {
            modalEndInfo?.closeLayer?.();
            setModalEndInfo?.({
                closeLayer: () => {
                    onOff(pv,typeId,allPlots,plotTypeMustMatch,modalEndInfo, endText, modalLayer);
                },
                closeText: endText,
                offOnNewPlot: true,
            });
        }
    }
    else {
        dispatchDetachLayerFromPlot(typeId,pv.plotId,allPlots,dl.destroyWhenAllDetached);
        clearModalEndInfo();
    }
}

