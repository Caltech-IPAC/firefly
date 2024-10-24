/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

// import FlipOutlinedIcon from '@mui/icons-material/FlipOutlined.js';
import React from 'react';
import PropTypes, {bool, element, object, oneOfType, string} from 'prop-types';
import {getDrawLayerByType, isDrawLayerAttached, primePlot } from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';
import {dispatchCreateDrawLayer,
        getDlAry,
        dispatchAttachLayerToPlot,
        dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';
import {clearModalEndInfo, setModalEndInfo} from './ToolbarToolModalEnd.js';


export function SimpleLayerOnOffButton({plotView:pv,sx, tip,typeId,iconOn,iconOff,SvgIconComponent,visible=true,
                                           useDropDownIndicator, dropPosition,
                                           modalEndInfo, endText, modalLayer= false,
                                            text, color, variant, iconButtonSize,
                                            plotTypeMustMatch= false, style={}, enabled= true, imageStyle,
                                            isIconOn, onClick, dropDown, allPlots= true }) {
    const enableButton= Boolean(primePlot(pv)) && enabled;
    let isOn= isIconOn;
    if (typeId && pv) {
        const distLayer= getDrawLayerByType(getDlAry(),typeId);
        isOn=  distLayer && isDrawLayerAttached(distLayer,pv.plotId);
    }
    let icon;
    let sxToUse= sx;
    if (SvgIconComponent || !iconOn) {
        sxToUse= (theme) => ({
            'button' :{
                background: isOn ? theme.vars.palette.primary?.softDisabledColor : undefined,
            },
            ...sx
        });
        icon= SvgIconComponent ?? iconOff;
    }
    else {
        icon= isOn ? iconOn : iconOff;
    }

    if (dropDown && !isOn) {
        return (
            <DropDownToolbarButton  {...{icon, tip, text, color, variant, enabled:enableButton,
                                    useDropDownIndicator, dropPosition,
                                    visible, imageStyle, dropDown }}/>
        );
    } else {
        return (
            <ToolbarButton {...{
                icon, iconButtonSize,useDropDownIndicator, dropPosition,
                color, variant, sx:sxToUse,
                tip, text, enabled:enableButton, visible, style, imageStyle,
                onClick:() => onClick ? onClick(pv,!isOn) :
                    onOff(pv,typeId,allPlots,plotTypeMustMatch,modalEndInfo, endText,modalLayer)
            }}/>
        );
    }
}

SimpleLayerOnOffButton.propTypes= {
    plotView : PropTypes.object,
    typeId :  PropTypes.string,
    tip : PropTypes.string,
    text : PropTypes.string,
    color : PropTypes.string,
    variant : PropTypes.string,
    visible : PropTypes.bool,
    iconOn : oneOfType([element,string]),
    iconOff : oneOfType([element,string]),
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
    iconButtonSize : PropTypes.string,
    useDropDownIndicator: bool,
    dropPosition: object,
    sx : PropTypes.object,
};

function off(dl, pv,typeId,allPlots) {
    dispatchDetachLayerFromPlot(typeId,pv.plotId,allPlots,dl?.destroyWhenAllDetached||dl?.destroyWhenAllUserDetached);
    clearModalEndInfo();
}


export function onOff(pv,typeId,allPlots, plotTypeMustMatch, modalEndInfo, endText, modalLayer= false, forceOff) {
    if (!pv || !typeId) return;

    const dl= getDrawLayerByType(getDlAry(), typeId);

    if (forceOff) {
        off(dl, pv,typeId,allPlots);
        return;
    }


    if (!dl) {
        dispatchCreateDrawLayer(typeId);
    }

    if (!isDrawLayerAttached(dl,pv.plotId)) {
        dispatchAttachLayerToPlot(typeId,pv.plotId,allPlots,true, plotTypeMustMatch);
        if (modalLayer) {
            modalEndInfo?.closeLayer?.();
            setModalEndInfo?.({
                closeLayer: () => {
                    onOff(pv,typeId,allPlots,plotTypeMustMatch,modalEndInfo, endText, modalLayer, true);
                },
                closeText: endText,
                offOnNewPlot: true,
            });
        }
    }
    else {
        off(dl, pv,typeId,allPlots, plotTypeMustMatch);
    }
}

