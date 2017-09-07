/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {padStart} from 'lodash';
import {isDrawLayerVisible, getAllDrawLayersForPlot,
    getLayerTitle, getDrawLayersByDisplayGroup}  from '../PlotViewUtil.js';
import {operateOnOverlayPlotViewsThatMatch, enableRelatedDataLayer,
               findUnactivatedRelatedData, setMaskVisible } from '../RelatedDataUtil.js';
import DrawLayerItemView from './DrawLayerItemView.jsx';
import {ColorChangeType} from '../draw/DrawLayer.js';
import {GroupingScope} from '../DrawLayerCntlr.js';
import {clone} from '../../util/WebUtil.js';
import {dispatchChangeDrawingDef, dispatchChangeVisibility,
        dispatchDetachLayerFromPlot, getDlAry} from '../DrawLayerCntlr.js';
import {visRoot, dispatchOverlayPlotChangeAttributes, dispatchDeleteOverlayPlot} from '../ImagePlotCntlr.js';
import {showColorPickerDialog} from '../../ui/ColorPicker.jsx';
import {showPointShapeSizePickerDialog} from '../../ui/PointShapeSizePicker.jsx';
import {getPlotViewById} from '../PlotViewUtil';




const dlPanelViewStyle= {
    width:'calc(100% - 12px)',
    height:'100%',
    padding: 6,
    position: 'relative',
    overflow:'hidden'
};



export function DrawLayerPanelView({dlAry, plotView, mouseOverMaskValue, drawLayerFactory}) {

    const layers= getAllDrawLayersForPlot(dlAry,plotView.plotId);
    const maxTitleChars= layers.reduce( (max,l) => {
        const t= getLayerTitle(plotView.plotId,l);
        return Math.max(max, t?t.length:0);
    },20);

    return (
        <div style={dlPanelViewStyle}>
            <div style={{overflow:'auto', maxHeight:500}}>
                {makeImageLayerItemAry(plotView,maxTitleChars,layers.length===0, mouseOverMaskValue)}
                {makeDrawLayerItemAry(layers,plotView,maxTitleChars, drawLayerFactory)}
            </div>
            {makePermInfo(plotView,layers)}
        </div>
    );
}


DrawLayerPanelView.propTypes= {
    plotView : PropTypes.object.isRequired,
    dlAry : PropTypes.array.isRequired,
    dialogId : PropTypes.string.isRequired,
    drawLayerFactory : PropTypes.object.isRequired,
    mouseOverMaskValue : PropTypes.number.isRequired
};


function getUIComponent(dl,pv, factory) {
    return factory.getGetUIComponentFunc(dl) && factory.getGetUIComponentFunc(dl)(dl,pv);
}


function makePermInfo(pv,layers) {
    return (
        <div style={{minWidth: 260, borderTop:'1px solid rgba(0, 0, 0, 0.298039)', paddingTop: 5, margin: '4px 5px 0 5px'}}>
            {makeAddRelatedDataAry(pv)}
            <div style={{paddingTop:5}}>
                <button style={{display : 'inline-block'}} type='button'
                        className='button'
                        onClick={() => showAllLayers(layers,pv,true)}>
                    {'Show All'}
                </button>
                <button style={{display : 'inline-block'}} type='button'
                        className='button'
                        onClick={() => showAllLayers(layers,pv,false)}>
                    {'Hide All'}
                </button>

            </div>
        </div>
    );
}

function showAllLayers(layers, pv, visible) {
    pv.overlayPlotViews.forEach( (opv) => {
        setMaskVisibleInGroup(opv,visible);
    });

    return layers.forEach( (dl) => {
        changeVisible(dl, visible,pv.plotId );
    });
}


function makeAddRelatedDataAry(pv) {
    if (!pv.plots) return null;

    const relatedData= findUnactivatedRelatedData(pv);

    return relatedData.map( (d,idx) => {
        return (
            <div key={`button- ${idx}`}>
                <div style={{display : 'inline-block', width: 150, marginRight:10, fontStyle: 'italic', textAlign:'right'}}>
                    {`${d.desc} Layer found :`}
                </div>
                <button style={{display : 'inline-block'}} type='button'
                        className='button'
                        onClick={() => enableRelatedDataLayer(visRoot(), pv, d)}>
                    {'Enable'}
                </button>
            </div>
        );
    });
}






function makeDrawLayerItemAry(layers,pv, maxTitleChars, factory) {
    const last= layers.length-1;
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
                                                     changeVisible={() => flipVisible(l,pv.plotId)}
                                                     UIComponent={getUIComponent(l,pv,factory)}
    />);
}

function makeImageLayerItemAry(pv, maxTitleChars, hasLast, mouseOverMaskValue) {
    if (!pv.overlayPlotViews) return [];
    const last= pv.overlayPlotViews.length-1;
    const retAry= pv.overlayPlotViews.map( (opv,idx) =>
        <DrawLayerItemView key={'MaskControl-'+idx}
                           maxTitleChars={maxTitleChars}
                           helpLine={''}
                           lastItem={hasLast ? idx===last : false}
                           canUserDelete={true}
                           canUserChangeColor={true}
                           isPointData={false}
                           color={opv.colorAttributes.color}
                           title= {makeOverlayTitle(opv, (mouseOverMaskValue & opv.maskValue)) }
                           visible={opv.visible}
                           modifyColor={() => modifyMaskColor(opv)}
                           deleteLayer={() => deleteMaskLayer(opv)}
                           changeVisible={() => setMaskVisibleInGroup(opv, !opv.visible)}
                           UIComponent={null}
    />);
    return retAry;
}


function makeOverlayTitle(opv,mouseOn) {
    let {title, maskNumber}= opv;
    const { colorAttributes:{color}, plot, visible, uiCanAugmentTitle}= opv;
    maskNumber= Number(maskNumber);
    if (plot && uiCanAugmentTitle) {
        const {header}= plot.projection;
        const titleKey= Object.keys(header)
            .filter( (k) => k.includes('MP'))
            .find( (k) => parseInt(header[k])===maskNumber);
        let maskDesc= titleKey;
        if (maskDesc) {
            if (maskDesc.startsWith('HIERARCH')) maskDesc= maskDesc.substring(9);
            title= `${title} - ${maskDesc}`;
        }
    }
    if (!plot && visible) {
        title= `${title} - loading`;
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
            const {r,g,b,a}= ev.rgb;
            const rgbStr= `rgba(${r},${g},${b},${a})`;
            dl = getDrawLayersByDisplayGroup(getDlAry(), dl.displayGroupId);
            dispatchChangeDrawingDef(dl.displayGroupId, Object.assign({},dl.drawingDef,{color:rgbStr}),plotId, dl.titleMatching);
        }, dl.drawLayerId);
}

const hexC= (v) =>  padStart(v.toString(16),2,'0');


function modifyMaskColor(opv) {

    const {color}= opv.colorAttributes;
    const rV= parseInt(color.substring(1,3),16);
    const gV= parseInt(color.substring(3,5),16);
    const bV= parseInt(color.substring(5,7),16);


    const rgbStr= `rgba(${rV},${gV},${bV},${opv.opacity})`;
    showColorPickerDialog(rgbStr, false, true,
        (ev, okPushed) => {
            const {r,g,b,a}= ev.rgb;
            const newColor= `#${hexC(r)}${hexC(g)}${hexC(b)}`;


            operateOnOverlayPlotViewsThatMatch(visRoot(),opv, (aOpv) => {
                const {plotId, imageOverlayId} = aOpv;
                const colorAttributes= aOpv.colorAttributes.color!==newColor ?
                                  clone(aOpv.colorAttributes, {color:newColor} ) : aOpv.colorAttributes;
                dispatchOverlayPlotChangeAttributes({plotId, imageOverlayId,doReplot:false,
                    attributes:{colorAttributes,opacity:a}});
            });

        });
}

function modifyShape(dl, plotId) {
    showPointShapeSizePickerDialog(dl, plotId);
}

function deleteLayer(dl,plotId) {
    dispatchDetachLayerFromPlot(dl.displayGroupId,plotId,true, dl.destroyWhenAllDetached);
}

function deleteMaskLayer(opv) {
    operateOnOverlayPlotViewsThatMatch(visRoot(),opv, (aOpv) => {
        const {plotId, imageOverlayId} = aOpv;
        dispatchDeleteOverlayPlot({plotId, imageOverlayId});
    });
}

/**
 *
 * @param {DrawingLayer} dl
 * @param {string} plotId
 */
function flipVisible(dl, plotId) {
    changeVisible(dl, !isDrawLayerVisible(dl,plotId),plotId );
}


/**
 *
 * @param {DrawingLayer} dl
 * @param {boolean} visible
 * @param {string} plotId
 */
function changeVisible(dl, visible, plotId) {
    const {drawLayerId, groupingScope, supportSubgroups}= dl;
    const pv= getPlotViewById(visRoot(),plotId);
    if (groupingScope===GroupingScope.GROUP || !supportSubgroups || !groupingScope || !pv || !pv.drawingSubGroupId)  {
        dispatchChangeVisibility( {id:drawLayerId, visible,plotId, matchTitle:dl.titleMatching} );
    }
    else {
        switch (groupingScope) {
            case GroupingScope.SUBGROUP : // change all, then put only subgroup back
                dispatchChangeVisibility({id:drawLayerId, visible,plotId,subGroupId:pv.drawingSubGroupId});
                break;
            case GroupingScope.SINGLE : // change all, then put only image back
                dispatchChangeVisibility({id:drawLayerId, visible,plotId, useGroup:false});
                break;
            default :
                console.log('DrawLayerPanelView.changeVisible show never happen');
                break;
        }

    }
}




function setMaskVisibleInGroup(opv, visible) {
    operateOnOverlayPlotViewsThatMatch(visRoot(),opv, (aOpv) => setMaskVisible(aOpv,visible));
}
