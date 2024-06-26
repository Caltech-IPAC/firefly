/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {IconButton, Stack, Typography} from '@mui/joy';
import React from 'react';
import {object,number} from 'prop-types';
import Enum from 'enum';
import {CatalogType} from 'firefly/drawingLayers/Catalog.js';

import {isEmpty, startCase} from 'lodash';
import {dispatchHideDialog, isDialogVisible} from '../core/ComponentCntlr.js';
import {INFO_POPUP, showInfoPopup} from '../ui/PopupUtil.jsx';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {DataTypes} from '../visualize/draw/DrawLayer.js';
import {dispatchChangeVisibility, dispatchModifyCustomField, GroupingScope} from '../visualize/DrawLayerCntlr.js';
import {isDrawLayerVisible} from '../visualize/PlotViewUtil.js';
import {InfoButton} from '../visualize/ui/Buttons.jsx';

import PriorityHighRoundedIcon from '@mui/icons-material/PriorityHighRounded';

export const TableSelectOptions = new Enum(['all', 'selected', 'highlighted']);
export const getUIComponent = (drawLayer,pv,maxTitleChars) =>
                         <CatalogUI drawLayer={drawLayer} pv={pv} maxTitleChars={maxTitleChars}/>;


function CatalogUI({drawLayer,pv}) {

    const options= [ {label: 'All', value: 'GROUP'},
                   {label: 'Row', value: 'SUBGROUP'},
                   {label: 'Image', value: 'SINGLE'}
    ];

    const showTableOptions = () => {
        const {selectOption, catalogType, columns} = drawLayer;

        const tOptions = TableSelectOptions.enums.reduce((prev, eItem) => {
            prev.push({label: startCase(eItem.key), value: eItem.key});
            return prev;
        }, []);

        const subTitle= catalogType===CatalogType.REGION ? (
                <Stack>
                    <Typography level='body-sm'>
                        {columns.type === 'region' ? `column: ${columns.regionCol}` : `columns: ${columns.lonCol}, ${columns.latCol}`}
                    </Typography>
                </Stack>
            ) : undefined;

        let tableOptions;
        // for region layer
        if (selectOption && tOptions.find((oneOp) => oneOp.value === selectOption)) {
            const message = composeRegionMessage(drawLayer, selectOption);
            const helpRef = 'http://www.ivoa.net/documents/TAP/20100327/REC-TAP-1.0.pdf';

            const errorIcon = !message ? null : (
                            <IconButton size='sm' color='danger' sx={{ml: 1}}
                                 onClick={() => showErrorPopup(message, 'region column error', helpRef)} >
                                <PriorityHighRoundedIcon/>
                            </IconButton>
                    );

            tableOptions = (
                <Stack mb={1/2}>
                    <Stack {...{direction:'row', alignItems:'center'}}>
                        {subTitle}
                        {errorIcon}
                    </Stack>
                    <RadioGroupInputFieldView
                        options={tOptions} value={selectOption} buttonGroup={true}
                        onChange={(ev) => changeTableSelection(drawLayer, pv, ev.target.value, selectOption)}/>
                </Stack>
            );
        } else {
            tableOptions = subTitle;
        }
        return tableOptions;
    };

    if (!drawLayer.supportSubgroups) {
        return showTableOptions();
    }

    const value= drawLayer.groupingScope ? drawLayer.groupingScope.toString() : 'GROUP';
    return (
        <Stack>
            {showTableOptions()}
            <Stack direction='row' spacing={1}>
                <Typography level={'body-sm'}>Overlay:</Typography>
                <RadioGroupInputFieldView options={options}  value={value}
                                          onChange={(ev) => changeVisibilityScope(drawLayer,pv,ev.target.value)} />
            </Stack>
        </Stack>
    );
}

const showErrorPopup = (message, title, helpRef) => {
    const content = (
        <Stack direction='row' spacing={1} alignItems='center'>
            <Typography> {message} </Typography>
            <InfoButton onClick={() => window.open(helpRef,'_blank')}/>
        </Stack>
    );
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

CatalogUI.propTypes= {
    drawLayer     : object.isRequired,
    pv            : object.isRequired,
    maxTitleChars : number
};


