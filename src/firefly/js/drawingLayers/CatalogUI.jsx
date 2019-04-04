/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import Enum from 'enum';
import {isEmpty, startCase} from 'lodash';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField, dispatchChangeVisibility} from '../visualize/DrawLayerCntlr.js';
import {isDrawLayerVisible} from '../visualize/PlotViewUtil.js';
import {GroupingScope} from '../visualize/DrawLayerCntlr.js';
import {DataTypes} from '../visualize/draw/DrawLayer.js';
import {showInfoPopup, INFO_POPUP} from '../ui/PopupUtil.jsx';
import {isDialogVisible, dispatchHideDialog} from '../core/ComponentCntlr.js';
import EXCLAMATION from 'html/images/exclamation16x16.gif';

export const TableSelectOptions = new Enum(['all', 'selected', 'highlighted']);
export const getUIComponent = (drawLayer,pv) => <CatalogUI drawLayer={drawLayer} pv={pv}/>;

function CatalogUI({drawLayer,pv}) {

    const options= [ {label: 'All', value: 'GROUP'},
                   {label: 'Row', value: 'SUBGROUP'},
                   {label: 'Image', value: 'SINGLE'}
    ];

    const showTableOptions = () => {
        let tableOptions = null;
        const {selectOption, isFromRegion, columns} = drawLayer;

        const tOptions = TableSelectOptions.enums.reduce((prev, eItem) => {
            prev.push({label: startCase(eItem.key), value: eItem.key});
            return prev;
        }, []);

        const subTitle = (!isFromRegion) ? null :
            (<div>{columns.type === 'region' ? `column: ${columns.regionCol}` : `columns: ${columns.lonCol}, ${columns.latCol}`}
             </div>);

        // for region layer
        if (selectOption && tOptions.find((oneOp) => oneOp.value === selectOption)) {
            const message = composeRegionMessage(drawLayer, selectOption);
            const errorIcon = !message ? null : (
                            <div style={{width: 16, height: 16, marginLeft: 10}}
                                 onClick={() => showInfoPopup(message, 'region column error')} >
                                <img src={EXCLAMATION}/>
                            </div>
                    );

            tableOptions = (
                <div>
                    <div style={{marginBottom: 8, height: 16, display:'flex'}}>
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


function composeRegionMessage(dl, selectOption) {
    const dd = Object.assign({},dl.drawData);
    const dataAry = (selectOption === TableSelectOptions.highlighted.key) ? dd[DataTypes.HIGHLIGHT_DATA] : dd[DataTypes.DATA];

    const invalidRows = isEmpty(dataAry) ? 0 :
        dataAry.reduce((prev, row) => {
            prev = !row ? prev+1 : prev;
            return prev;
        }, 0);

    return (invalidRows === 0) ? '' :
            `${invalidRows} out of ${dataAry.length} rows are not identified as valid regions due to unsupported` +
            ' or invalid region values';
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

CatalogUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

