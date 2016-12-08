/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {padStart} from 'lodash';
import {isDrawLayerVisible, getAllDrawLayersForPlot, getLayerTitle}  from '../PlotViewUtil.js';
import DrawLayerItemView from './DrawLayerItemView.jsx';
import {ColorChangeType} from '../draw/DrawLayer.js';
import {dispatchChangeDrawingDef, dispatchChangeVisibility,
    dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';
import {dispatchOverlayPlotChangeAttributes, dispatchDeleteOverlayPlot} from '../ImagePlotCntlr.js';
import {showColorPickerDialog} from '../../ui/ColorPicker.jsx';
import {showPointShapeSizePickerDialog} from '../../ui/PointShapeSizePicker.jsx';


function DrawLayerPanelView({dlAry, plotView, mouseOverMaskValue, drawLayerFactory}) {
    var style= {width:'calc(100% - 12px)',
        height:'100%',
        padding: 6,
        position: 'relative',
        overflow:'hidden'};

    var layers= getAllDrawLayersForPlot(dlAry,plotView.plotId);
    var maxTitleChars= layers.reduce( (max,l) => {
        var t= getLayerTitle(plotView.plotId,l);
        return Math.max(max, t?t.length:0);
    },20);

    return (
        <div style={style}>
            {makeImageLayerItemAry(plotView,maxTitleChars,layers.length===0, mouseOverMaskValue)}
            {makeDrawLayerItemAry(layers,plotView,maxTitleChars, drawLayerFactory)}
        </div>
    );
}


DrawLayerPanelView.propTypes= {
    plotView : React.PropTypes.object.isRequired,
    dlAry : React.PropTypes.array.isRequired,
    dialogId : React.PropTypes.string.isRequired,
    drawLayerFactory : React.PropTypes.object.isRequired,
    mouseOverMaskValue : React.PropTypes.number.isRequired
};


function getUIComponent(dl,pv, factory) {
    return factory.getGetUIComponentFunc(dl) && factory.getGetUIComponentFunc(dl)(dl,pv);
}


function makeDrawLayerItemAry(layers,pv, maxTitleChars, factory) {
    var last= layers.length-1;
    return layers.map( (l,idx) => <DrawLayerItemView key={l.drawLayerId}
                                                     maxTitleChars={maxTitleChars}
                                                     helpLine={l.helpLine}
                                                     lastItem={idx===last}
                                                     canUserDelete={l.canUserDelete}
                                                     canUserChangeColor={l.canUserChangeColor}
                                                     isPointData={l.isPointData}
                                                     drawingDef={l.drawingDef}
                                                     color={l.drawingDef.color}
                                                     title= {getLayerTitle(pv.plotId,l)}
                                                     visible={isDrawLayerVisible(l,pv.plotId)}
                                                     modifyColor={() => modifyColor(l,pv.plotId)}
                                                     modifyShape={() => modifyShape(l,pv.plotId)}
                                                     deleteLayer={() => deleteLayer(l,pv.plotId)}
                                                     changeVisible={() => changeVisible(l,pv.plotId)}
                                                     UIComponent={getUIComponent(l,pv,factory)}
    />);
}

function makeImageLayerItemAry(pv, maxTitleChars, hasLast, mouseOverMaskValue) {
    if (!pv.overlayPlotViews) return [];
    var last= pv.overlayPlotViews.length-1;
    const retAry= pv.overlayPlotViews.map( (opv,idx) =>
        <DrawLayerItemView key={'MaskControl-'+idx}
                           maxTitleChars={maxTitleChars}
                           helpLine={''}
                           lastItem={hasLast ? idx===last : false}
                           canUserDelete={true}
                           canUserChangeColor={true}
                           isPointData={false}
                           color={opv.color}
                           title= {makeOverlayTitle(opv, (mouseOverMaskValue & opv.maskValue)) }
                           visible={opv.visible}
                           modifyColor={() => modifyMaskColor(opv)}
                           deleteLayer={() => deleteMaskLayer(opv)}
                           changeVisible={() => changeMaskVisible(opv)}
                           UIComponent={null}
    />);
    return retAry;

}

function makeOverlayTitle(opv,mouseOn) {
    var {title, color, maskNumber}= opv;
    if (opv.plot) {
        const {header}= opv.plot.projection;
        const titleKey= Object.keys(header)
            .filter( (k) => k.includes('MP'))
            .find( (k) => parseInt(header[k])===maskNumber);
        var maskDesc= titleKey;
        if (maskDesc) {
            if (maskDesc.startsWith('HIERARCH')) maskDesc= maskDesc.substring(9);
            title= `${title} - ${maskDesc}`;
        }
    }

    mouseOn= Boolean(mouseOn);

    return (
        <div style={{color : mouseOn ? color : 'inherit'}}>{title}
        {mouseOn && <span style={{fontStyle: 'italic'}}> over</span>}
        </div>
    );
}


function modifyColor(dl,plotId) {
    showColorPickerDialog(dl.drawingDef.color, dl.canUserChangeColor===ColorChangeType.STATIC, false,
        (ev) => {
            var {r,g,b,a}= ev.rgb;
            var rgbStr= `rgba(${r},${g},${b},${a})`;
            dispatchChangeDrawingDef(dl.displayGroupId, Object.assign({},dl.drawingDef,{color:rgbStr}),plotId);
        });
}

const hexC= (v) =>  padStart(v.toString(16),2,'0');

function modifyMaskColor(opv) {

    const {color}= opv;
    const rV= parseInt(color.substring(1,3),16);
    const gV= parseInt(color.substring(3,5),16);
    const bV= parseInt(color.substring(5,7),16);


    var rgbStr= `rgba(${rV},${gV},${bV},${opv.opacity})`;
    showColorPickerDialog(rgbStr, false, true,
        (ev, okPushed) => {
            const {plotId, imageOverlayId} = opv;
            var {r,g,b,a}= ev.rgb;
            const newColor= `#${hexC(r)}${hexC(g)}${hexC(b)}`;
            const doReplot= okPushed && (newColor.toUpperCase()!==opv.color.toUpperCase());
            dispatchOverlayPlotChangeAttributes({plotId, imageOverlayId,doReplot,
                    attributes:{color:newColor,opacity:a}});
        });
}

function modifyShape(dl, plotId) {
    showPointShapeSizePickerDialog(dl, plotId);
}

function deleteLayer(dl,plotId) {
    dispatchDetachLayerFromPlot(dl.displayGroupId,plotId,true, true, dl.destroyWhenAllDetached);
}


function deleteMaskLayer(opv) {
    const {plotId, imageOverlayId} = opv;
    dispatchDeleteOverlayPlot({plotId, imageOverlayId});
}


function changeVisible(dl, plotId) {
    dispatchChangeVisibility(dl.displayGroupId, !isDrawLayerVisible(dl,plotId),plotId );
}


function changeMaskVisible(opv) {
    const {plotId, imageOverlayId, visible} = opv;
    dispatchOverlayPlotChangeAttributes({plotId, imageOverlayId, attributes:{visible:!visible}});
}


export default DrawLayerPanelView;
