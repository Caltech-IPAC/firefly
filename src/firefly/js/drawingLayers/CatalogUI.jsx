/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {IconButton, Stack, Tooltip} from '@mui/joy';
import React, {useState} from 'react';
import PropTypes from 'prop-types';
import Enum from 'enum';
import {isEmpty, startCase} from 'lodash';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField, dispatchChangeVisibility} from '../visualize/DrawLayerCntlr.js';
import {isDrawLayerVisible} from '../visualize/PlotViewUtil.js';
import {DataTypes} from '../visualize/draw/DrawLayer.js';
import {showInfoPopup, INFO_POPUP} from '../ui/PopupUtil.jsx';
import {dispatchRecenter} from '../visualize/ImagePlotCntlr';
import {GroupingScope} from '../visualize/DrawLayerCntlr.js';
import {isDialogVisible, dispatchHideDialog} from '../core/ComponentCntlr.js';
import {formatLonLatToString, formatWorldPt} from '../visualize/ui/WorldPtFormat';
import {copyToClipboard} from '../util/WebUtil';
import {makeColorChange, makeShape} from '../visualize/ui/DrawLayerUIComponents';
import {showColorPickerDialog} from '../ui/ColorPicker';
import {showPointShapeSizePickerDialog} from '../ui/PointShapeSizePicker';

import infoIcon from 'html/images/info-icon.png';
import EXCLAMATION from 'html/images/exclamation16x16.gif';
import CLIPBOARD from 'html/images/20x20_clipboard.png';
import CHECKED from 'html/images/20x20_clipboard-checked.png';
import CENTER from 'html/images/20x20-center-small.png';
import {convert} from '../visualize/VisUtil';
import CoordinateSys from '../visualize/CoordSys';
import {CatalogType} from 'firefly/drawingLayers/Catalog.js';
import Point from 'firefly/visualize/Point.js';

export const TableSelectOptions = new Enum(['all', 'selected', 'highlighted']);
export const getUIComponent = (drawLayer,pv,maxTitleChars) =>
                         <CatalogUI drawLayer={drawLayer} pv={pv} maxTitleChars={maxTitleChars}/>;


function CatalogUI({drawLayer,pv}) {

    const options= [ {label: 'All', value: 'GROUP'},
                   {label: 'Row', value: 'SUBGROUP'},
                   {label: 'Image', value: 'SINGLE'}
    ];

    const showTableOptions = () => {
        let tableOptions = null;
        const {selectOption, catalogType, columns} = drawLayer;

        const tOptions = TableSelectOptions.enums.reduce((prev, eItem) => {
            prev.push({label: startCase(eItem.key), value: eItem.key});
            return prev;
        }, []);

        const searchTargetUI= drawLayer.searchTarget ? (<SearchTargetUI drawLayer={drawLayer} PlotView={pv}/>) : false;

        let subTitle;
        if (catalogType===CatalogType.REGION) {
            subTitle= (
                <div>
                    {searchTargetUI}
                    {columns.type === 'region' ? `column: ${columns.regionCol}` : `columns: ${columns.lonCol}, ${columns.latCol}`}
                </div>
            );
        }
        else {
            subTitle= searchTargetUI;
        }




        // for region layer
        if (selectOption && tOptions.find((oneOp) => oneOp.value === selectOption)) {
            const message = composeRegionMessage(drawLayer, selectOption);
            const helpRef = 'http://www.ivoa.net/documents/TAP/20100327/REC-TAP-1.0.pdf';

            const errorIcon = !message ? null : (
                            <div style={{width: 16, height: 16, marginLeft: 10}}
                                 onClick={() => showErrorPopup(message, 'region column error', helpRef)} >
                                <img src={EXCLAMATION}/>
                            </div>
                    );

            tableOptions = (
                <div>
                    <div style={{marginBottom: 8, display:'flex'}}>
                        {subTitle}
                        {errorIcon}
                    </div>
                    <div>
                        <div style={{display:'inline-block', padding: '2px 3px 2px 3px',
                                     border: '1px solid rgba(60,60,60,.2', borderRadius: '5px'}}>
                            <RadioGroupInputFieldView options={tOptions} value={selectOption}
                                                      buttonGroup={true}
                                                      onChange={(ev) => changeTableSelection(drawLayer, pv, ev.target.value, selectOption)}/>
                        </div>
                    </div>
                </div>
            );
        } else {
            tableOptions = subTitle;
        }
        return tableOptions;
    };

    if (!drawLayer.supportSubgroups) {
        return (
          showTableOptions()
        );
    }

    const value= drawLayer.groupingScope ? drawLayer.groupingScope.toString() : 'GROUP';
    return (
        <div>
            {showTableOptions()}
            <div>
                Overlay:
                <div style={{display:'inline-block', paddingLeft:7}}>
                    <RadioGroupInputFieldView options={options}  value={value}
                                              onChange={(ev) => changeVisibilityScope(drawLayer,pv,ev.target.value)} />
                </div>
            </div>
        </div>
    );
}

const showErrorPopup = (message, title, helpRef) => {
    const InfoIcon = () => helpRef && (
        <a onClick={(e) => e.stopPropagation()} target='_blank' href={helpRef}>
                <img style={{width:'14px'}} src={infoIcon} alt='info'/></a>
    );

    const content = (<div> {message} {InfoIcon()} </div>);
    showInfoPopup(content, title);
};

function composeRegionMessage(dl, selectOption) {
    const dd = Object.assign({},dl.drawData);
    const dataAry = (selectOption === TableSelectOptions.highlighted.key) ? dd[DataTypes.HIGHLIGHT_DATA] : dd[DataTypes.DATA];

    const invalidRows = isEmpty(dataAry) ? 0 :
        dataAry.reduce((prev, row) => {
            prev = !row ? prev+1 : prev;
            return prev;
        }, 0);

    return (invalidRows === 0) ? '' :
            `${invalidRows} out of ${dataAry.length} rows are not displayable as regions due to unsupported` +
            ' or invalid s_region values.';
}

function changeTableSelection(drawLayer, pv, value, preValue) {
    if (value !== preValue) {
        if (isDialogVisible(INFO_POPUP)) {
            dispatchHideDialog(INFO_POPUP);
        }
        dispatchModifyCustomField(drawLayer.drawLayerId, {selectOption: value}, pv.plotId);
    }
}

function changeVisibilityScope(drawLayer,pv,value) {
    const groupingScope= GroupingScope.get(value);
    const {drawLayerId}= drawLayer;
    const {plotId, drawingSubGroupId}= pv;
    if (!drawingSubGroupId) return;
    dispatchModifyCustomField( drawLayerId, {groupingScope}, plotId );
    const visible= isDrawLayerVisible(drawLayer,plotId);
    switch (groupingScope) {
        case GroupingScope.GROUP : //make sure all images match the visibility of the plotId
            dispatchChangeVisibility({id:drawLayerId, visible,plotId});
            break;
        case GroupingScope.SUBGROUP : // change all, then put only subgroup back
            if (visible) dispatchChangeVisibility({id:drawLayerId, visible:false,plotId});
            dispatchChangeVisibility({id:drawLayerId, visible,plotId,subGroupId:drawingSubGroupId});
            break;
        case GroupingScope.SINGLE : // change all, then put only image back
            if (visible) dispatchChangeVisibility({id:drawLayerId, visible:false, plotId});
            dispatchChangeVisibility({id:drawLayerId, visible,plotId, useGroup:false});
            break;
    }
}

function SearchTargetUI({drawLayer:dl, PlotView:pv}) {
    const {searchTarget:wp, drawLayerId}= dl;
    const drawingDef= dl.searchTargetDrawingDef;

    const modifyColor= () => {
        showColorPickerDialog(drawingDef.color, false, false,
            (ev) => {
                const {r,g,b,a}= ev.rgb;
                const color= `rgba(${r},${g},${b},${a})`;
                dispatchModifyCustomField(drawLayerId, {searchTargetDrawingDef:{...drawingDef,color}}, pv.plotId);
            }, drawLayerId+'-searchTarget');
    };


    const modifyShape= () => {
        showPointShapeSizePickerDialog(dl, pv.plotId, drawingDef,
            (id, newDrawingDef) => dispatchModifyCustomField(id,
                   {searchTargetDrawingDef:{...drawingDef,symbol:newDrawingDef.symbol, size:newDrawingDef.size}}, pv.plotId),
            (drawLayer) => drawLayer.searchTargetDrawingDef.color
        );
    };

    return (
        <div style={{display: 'flex', justifyContent: 'space-between', alignItems:'center'}}>
            <div style={{display: 'flex', alignItems: 'center'}}>
                <span style={{paddingRight: 5}} >Search: </span>
                <div> {formatWorldPt(wp,3,false)} </div>
                <FixedPtControl pv={pv} wp={wp} />
                <input type='checkbox'
                       checked={dl.searchTargetVisible}
                       onChange={() => dispatchModifyCustomField( dl.displayGroupId,
                           {searchTargetVisible:!dl.searchTargetVisible}, pv.plotId )}
                />
            </div>
            <div style={{paddingRight: 68}}>
                {makeShape(drawingDef, modifyShape)}
                {makeColorChange(drawingDef.color, modifyColor, {paddingRight: 14})}
            </div>
        </div>
    );

}

CatalogUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired,
    maxTitleChars : PropTypes.number
};


export function FixedPtControl({pv, wp, sx={}}) {
    const llStr= wp.type===Point.W_PT ?
        formatLonLatToString(convert(wp, CoordinateSys.EQ_J2000)) : `${Math.round(wp.x)},${Math.round(wp.y)}`;
    const [clipIcon, setClipIcon] = useState(CLIPBOARD);

    const doCopy= (str) => {
        copyToClipboard(str);
        setTimeout( () => {
            setClipIcon(CHECKED);
            setTimeout( () => setClipIcon(CLIPBOARD),750);
        }, 10);
    };

    return (
        <Stack direction='row' sx={sx}>
            <Tooltip title={`Copy to the clipboard: ${llStr}`}>
                <IconButton  onClick={() => doCopy(llStr)}>
                    <img src={clipIcon}/>
                </IconButton>
            </Tooltip>
            <Tooltip title='Center on this position'>
                <IconButton  onClick={() => pv && dispatchRecenter({plotId:pv.plotId, centerPt:wp}) } >
                    <img src={CENTER}/>
                </IconButton>
            </Tooltip>
        </Stack>
    );
}

FixedPtControl.propTypes= {
    pv : PropTypes.object,
    wp : PropTypes.object.isRequired,
    sx : PropTypes.object
};
