/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Button, Stack, Tooltip, Typography} from '@mui/joy';
import {HelpIcon} from 'firefly/ui/HelpIcon.jsx';
import {toRGB} from 'firefly/util/Color.js';
import {groupBy, padStart} from 'lodash';
import PropTypes from 'prop-types';
import React from 'react';
import ImageRoot from '../../drawingLayers/ImageRoot.js';
import {showColorPickerDialog} from '../../ui/ColorPicker.jsx';
import {showPointShapeSizePickerDialog} from '../../ui/PointShapeSizePicker.jsx';
import {clone} from '../../util/WebUtil.js';
import {dispatchChangeVisibility, dispatchDetachLayerFromPlot, GroupingScope} from '../DrawLayerCntlr.js';
import {dispatchDeleteOverlayPlot, dispatchOverlayPlotChangeAttributes, visRoot} from '../ImagePlotCntlr.js';
import {getPlotViewById} from '../PlotViewUtil';
import {getAllDrawLayersForPlot, getHDU, getLayerTitle, isDrawLayerVisible, primePlot} from '../PlotViewUtil.js';
import {
    enableRelatedDataLayer, findUnactivatedRelatedData, operateOnOverlayPlotViewsThatMatch, setMaskVisible
} from '../RelatedDataUtil.js';
import {DrawLayerItemView} from './DrawLayerItemView.jsx';
import {modifyDrawColor} from './DrawLayerUIComponents';


export function DrawLayerPanelView({dlAry, plotView, mouseOverMaskValue, drawLayerFactory}) {

    const layers= getAllDrawLayersForPlot(dlAry,plotView.plotId);
    const maxTitleChars= layers.reduce( (max,l) => {
        const t= getLayerTitle(plotView.plotId,l);
        let tLen= 0;
        if (t) tLen= l.autoFormatTitle? t.length : 30;
        return Math.max(max, tLen);
    },20);

    return (
        <Stack direction='column' p={1}>
            <Box sx={{overflow:'auto', maxHeight:500}}>
                {makeImageLayerItemAry(plotView,maxTitleChars,layers.length===0, mouseOverMaskValue)}
                {makeDrawLayerItemAry(layers,plotView,maxTitleChars, drawLayerFactory)}
            </Box>
            {makePermInfo(plotView,layers)}
        </Stack>
    );
}


DrawLayerPanelView.propTypes= {
    plotView : PropTypes.object.isRequired,
    dlAry : PropTypes.array.isRequired,
    dialogId : PropTypes.string.isRequired,
    drawLayerFactory : PropTypes.object.isRequired,
    mouseOverMaskValue : PropTypes.number.isRequired
};


function getUIComponent(dl,pv, factory, maxTitleChars) {
    return factory.getGetUIComponentFunc(dl) && factory.getGetUIComponentFunc(dl)(dl,pv,maxTitleChars);
}


function makePermInfo(pv,layers) {
    return (
        <Stack {...{direction:'row', justifyContent: 'space-between', minWidth: 260, alignItems:'center', mt:1}}>
            <Stack {...{direction:'row', spacing:1, ml:.4}}>
                {makeAddRelatedDataAry(pv)}
                <Tooltip  title='Show all drawing layers'>
                    <Button onClick={() => showAllLayers(layers,pv,true)}> Show All </Button>
                </Tooltip>
                <Tooltip  title='Hide all drawing layers'>
                    <Button onClick={() => showAllLayers(layers,pv,false)}> Hide All </Button>
                </Tooltip>
            </Stack>
            <HelpIcon helpId={'visualization.layerPanel'}/>
        </Stack>
    );
}

function showAllLayers(layers, pv, visible) {

    const plot= primePlot(pv);
    pv.overlayPlotViews.forEach( (opv) => {
        const match= !opv.plot || (opv.plot?.dataWidth===plot?.dataWidth && opv.plot?.dataHeight===plot?.dataHeight);
        if (match) setMaskVisibleInGroup(opv,visible);
    });
    const overlayLayers = layers.filter( (l)=> l.drawLayerTypeId!==ImageRoot.TYPE_ID );
    return overlayLayers.forEach( (dl) => {
        changeVisible(dl, visible,pv.plotId );
    });
}


function makeAddRelatedDataAry(pv) {
    if (!pv.plots) return null;

    const relatedData= findUnactivatedRelatedData(pv);

    return relatedData
        .filter( (d) => d.primaryHduIdx===getHDU(primePlot(pv)))
        .map( (d,idx) => {
            return (
                <Stack {...{spacing:1, direction:'row', pr: 2, alignItems:'center', key:idx+''}}>
                    <Typography {...{color:'warning', mr:.5}}>
                        {`${d.desc} Layer found :`}
                    </Typography>
                    <Button color='warning' onClick={() => enableRelatedDataLayer(visRoot(), pv, d)}> Enable</Button>
                </Stack>
            );
        });
}






function makeDrawLayerItemAry(layers,pv, maxTitleChars, factory) {
    const last= layers.length-1;
    const sortedLayer= layers.sort( (l1,l2) => {
            if (l1.drawLayerTypeId===ImageRoot.TYPE_ID && l2.drawLayerTypeId!==ImageRoot.TYPE_ID) return -1;
            if (l1.drawLayerTypeId!==ImageRoot.TYPE_ID && l2.drawLayerTypeId===ImageRoot.TYPE_ID) return 1;
            return 0;
        });

    const sortedGroupedObj= groupBy([...sortedLayer], 'layersPanelLayoutId' );
    const sortedGroupedLayer= Object.values(sortedGroupedObj).flat(1);

    return sortedGroupedLayer.map( (l,idx) => (
        <DrawLayerItemView {...{
            key:l.drawLayerId,
            maxTitleChars,
            helpLine: l.helpLine,
            lastItem: idx===last,
            canUserDelete: l.canUserDelete,
            canUserHide: l.canUserHide,
            canUserChangeColor:l.canUserChangeColor,
            isPointData: l.isPointData,
            drawingDef: l.drawingDef,
            color: l.drawingDef.color,
            packWithNext: idx!==last && l.layersPanelLayoutId && l.layersPanelLayoutId===sortedGroupedLayer?.[idx+1]?.layersPanelLayoutId,
            autoFormatTitle: l.autoFormatTitle,
            title: getLayerTitle(pv.plotId,l),
            visible: isDrawLayerVisible(l,pv.plotId),
            modifyColor: () => modifyDrawColor(l,pv.plotId,l.tbl_id),
            modifyShape: () => modifyShape(l,pv.plotId),
            deleteLayer: () => deleteLayer(l,pv.plotId),
            changeVisible: () => flipVisible(l,pv.plotId),
            UIComponent: getUIComponent(l,pv,factory, maxTitleChars) }}
        />));
}

function makeImageLayerItemAry(pv, maxTitleChars, hasLast, mouseOverMaskValue) {
    if (!pv.overlayPlotViews) return [];
    const {dataWidth=0,dataHeight=0}= primePlot(pv)??{};
    const last= pv.overlayPlotViews.length-1;
    const retAry= pv.overlayPlotViews.map( (opv,idx) => (
        <DrawLayerItemView key={'MaskControl-'+idx}
                           maxTitleChars={maxTitleChars}
                           helpLine={''}
                           lastItem={hasLast ? idx===last : false}
                           canUserDelete={true}
                           canUserChangeColor={true}
                           isPointData={false}
                           packWithNext= {idx!==last}
                           color={opv.colorAttributes.color}
                           autoFormatTitle={true}
                           title= {makeOverlayTitle(opv, Boolean(mouseOverMaskValue & opv.maskValue), dataWidth, dataHeight) }
                           visible={opv.visible}
                           modifyColor={() => modifyMaskColor(opv)}
                           deleteLayer={() => deleteMaskLayer(opv)}
                           changeVisible={() => setMaskVisibleInGroup(opv, !opv.visible)}
                           UIComponent={null}
    />));
    return retAry;
}


function makeOverlayTitle(opv,mouseOn, dataWidth, dataHeight) {
    const match= !opv.plot || (opv.plot?.dataWidth===dataWidth && opv.plot?.dataHeight===dataHeight);
    let {title, maskNumber}= opv;
    const { colorAttributes:{color}, plot, visible, uiCanAugmentTitle}= opv;
    maskNumber= Number(maskNumber);
    if (plot && uiCanAugmentTitle) {
        const {header}= plot;
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
    if (!match) title= `Disabled non-matching: ${title}`;

    return (
        <div style={{color : match && mouseOn ? color : 'inherit'}}>{title}
            {mouseOn && <span style={{fontStyle: 'italic'}}> over</span>}
        </div>
    );
}


const hexC= (v) =>  padStart(v.toString(16),2,'0');


function modifyMaskColor(opv) {

    const [rV,gV,bV]= toRGB(opv.colorAttributes.color);
    const rgbStr= `rgba(${rV},${gV},${bV},${opv.opacity})`;
    showColorPickerDialog(rgbStr, false, true,
        (ev, okPushed) => {
            if (!ev?.rgb) return;
            const {r,g,b,a}= ev.rgb;
            const newColor= `#${hexC(r)}${hexC(g)}${hexC(b)}`;


            operateOnOverlayPlotViewsThatMatch(visRoot(),opv, (aOpv) => {
                const {plotId, imageOverlayId} = aOpv;
                const colorAttributes= aOpv.colorAttributes.color!==newColor ?
                                  clone(aOpv.colorAttributes, {color:newColor} ) : aOpv.colorAttributes;
                dispatchOverlayPlotChangeAttributes({plotId, imageOverlayId,doReplot:false,
                    attributes:{colorAttributes,opacity:a}});
            });

        }, '', '', .58);
}

function modifyShape(dl, plotId) {
    showPointShapeSizePickerDialog(dl, plotId);
}

function deleteLayer(dl,plotId) {
    dispatchDetachLayerFromPlot(dl.displayGroupId,plotId,true, dl.destroyWhenAllDetached||dl.destroyWhenAllUserDetached);
}

function deleteMaskLayer(opv) {
    operateOnOverlayPlotViewsThatMatch(visRoot(),opv, (aOpv) => {
        const {plotId, imageOverlayId} = aOpv;
        dispatchDeleteOverlayPlot({plotId, imageOverlayId});
    });
}

/**
 *
 * @param {DrawLayer} dl
 * @param {string} plotId
 */
function flipVisible(dl, plotId) {
    changeVisible(dl, !isDrawLayerVisible(dl,plotId),plotId );
}


/**
 *
 * @param {DrawLayer} dl
 * @param {boolean} visible
 * @param {string} plotId
 */
function changeVisible(dl, visible, plotId) {
    const {displayGroupId, groupingScope, supportSubgroups}= dl;
    const pv= getPlotViewById(visRoot(),plotId);
    if (groupingScope===GroupingScope.GROUP || !supportSubgroups || !groupingScope || !pv || !pv.drawingSubGroupId)  {
        dispatchChangeVisibility( {id:displayGroupId, visible,plotId, matchTitle:dl.titleMatching} );
    }
    else {
        switch (groupingScope) {
            case GroupingScope.SUBGROUP : // change all, then put only subgroup back
                dispatchChangeVisibility({id:displayGroupId, visible,plotId,subGroupId:pv.drawingSubGroupId});
                break;
            case GroupingScope.SINGLE : // change all, then put only image back
                dispatchChangeVisibility({id:displayGroupId, visible,plotId, useGroup:false});
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
