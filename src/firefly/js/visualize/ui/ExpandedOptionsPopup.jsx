/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Button, Stack} from '@mui/joy';
import {isEqual} from 'lodash';
import React, {useEffect, useState} from 'react';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {TABLE_FILTER, TABLE_FILTER_SELROW, TABLE_LOADED, TABLE_SORT} from '../../tables/TablesCntlr.js';
import {getTblById, watchTableChanges} from '../../tables/TableUtil.js';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {CompleteButton} from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {
    EXPANDED_OPTIONS_DIALOG_ID, HIDDEN, IMAGE_VIEW_TABLE_ID,
    getCombinedItemIds, getPvAryInViewer, isOnlyPlotSortOutOfSync,
    isPlotFilterOrSortOutOfSync, makeImViewDisplayModel, removeImageViewDisplaySelected, syncSort,
    updateImageViewDisplay, addExpandFilterSyncWatcher
} from '../ImViewFilterDisplay.js';
import {dispatchAddViewer, dispatchAddViewerItems, EXPANDED_MODE_RESERVED, IMAGE} from '../MultiViewCntlr.js';
import {deleteAllFailedPlots, isPlotViewArysEqual} from '../PlotViewUtil.js';
import {ListViewButton} from './Buttons.jsx';


export function ViewOptionsButton({ title, viewerId=EXPANDED_MODE_RESERVED }) {
    const outOfSyncTip= 'Click to re-add filters after image change';
    const inSyncTip= 'Choose which images to show';
    const outOfSync= isPlotFilterOrSortOutOfSync(viewerId);
    const onlySortOutOfSync= isOnlyPlotSortOutOfSync(viewerId);

    useEffect(() => {
        addExpandFilterSyncWatcher();
        onlySortOutOfSync && syncSort(viewerId);
    }, [onlySortOutOfSync]);

    useEffect(() => {
    },[]);

    return (
        <ListViewButton title= {outOfSync ? outOfSyncTip : inSyncTip}
                        slotProps={ {tooltip: {color:outOfSync?'danger' : undefined}} }
                        badgeAlert={outOfSync}
                        onClick={() =>showExpandedOptionsPopup(title,viewerId) }/>
    );
}

function showExpandedOptionsPopup(title= 'Loaded Images', viewerId= EXPANDED_MODE_RESERVED) {
    dispatchAddViewer(viewerId+HIDDEN, true, IMAGE, false);
    const popup = (
        <PopupPanel title={title}>
            <ImageViewOptionsPanel viewerId={viewerId}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(EXPANDED_OPTIONS_DIALOG_ID, popup);
    dispatchShowDialog(EXPANDED_OPTIONS_DIALOG_ID);
}


const pvKeys = ['plotId', 'request', 'serverCall', 'plottingStatusMsg'];
const plotKeys = ['plotImageId'];


function getPvAry(oldPvAry, viewerId) {
    const pvAry= getPvAryInViewer(viewerId);
    if (!oldPvAry) return pvAry;
    return isPlotViewArysEqual(oldPvAry, pvAry, pvKeys, plotKeys) ? oldPvAry : pvAry;
}

function getAllViewerIds(oldIdAry,viewerId, hiddenViewerId) {
    const allIds= getCombinedItemIds(viewerId,hiddenViewerId);
    return isEqual(oldIdAry, allIds) ? oldIdAry : allIds;
}

function ImageViewOptionsPanel({viewerId}) {

    const tbl_ui_id = IMAGE_VIEW_TABLE_ID + '-ui';


    const plotViewAry = useStoreConnector((oldPvAry) => getPvAry(oldPvAry,viewerId));
    const allIds = useStoreConnector((old) => getAllViewerIds(old,viewerId, viewerId+HIDDEN));
    const [tableModel, setTableModel] = useState(undefined);
    const [filterChangeCnt, setFilterChangeCnt] = useState(0);


    useEffect(() => {
        return watchTableChanges(IMAGE_VIEW_TABLE_ID, [TABLE_LOADED, TABLE_FILTER, TABLE_FILTER_SELROW, TABLE_SORT],
            (action) => {
                updateImageViewDisplay(IMAGE_VIEW_TABLE_ID, viewerId);
                if (action.type===TABLE_FILTER || action.type===TABLE_FILTER_SELROW) setFilterChangeCnt(filterChangeCnt+1);
            });
    }, []);


    useEffect(() => {
        const oldModel = getTblById(IMAGE_VIEW_TABLE_ID);
        setTableModel(makeImViewDisplayModel(IMAGE_VIEW_TABLE_ID, plotViewAry, allIds, oldModel));
    }, [plotViewAry?.length, allIds?.length, filterChangeCnt]); // only fire effect when filter change or plot count changes



    useEffect(() => {
       dispatchAddViewerItems(viewerId+HIDDEN,allIds,IMAGE);
    }, [viewerId, allIds]);

    if (!tableModel) return null;


    return (
        <Stack {...{ spacing: 2,
            sx:{p:1, overflow: 'hidden', resize:'both', width: 660, height: 450, minWidth: 250, minHeight: 200} }}>
            <Box {...{position: 'relative', width: 1, height: 1}}>
                <Stack sx={{m: '1px', position: 'absolute', inset: 0}}>
                    <TablePanel {...{
                        tbl_ui_id, tableModel, rowHeight:23,
                        showToolbar:true, showFilters:true, selectable:true, showOptionButton:true,
                        border:false, showTitle:false, showPaging:false, showSave:false,
                        showTypes:false, showToggleTextView:false, expandable:false, showUnits:true,
                    }} />
                </Stack>
            </Box>

            <Stack {...{direction: 'row', justifyContent: 'space-between'}}>
                <CompleteButton text='Done'
                                onSuccess={() => updateImageViewDisplay(tableModel.tbl_id)}
                                dialogId='ExpandedOptionsPopup'/>
                <Stack {...{direction: 'row', spacing: 1, sx: {'& .MuiButton-root': {whiteSpace: 'nowrap'}}}}>
                    <Button onClick={() => removeImageViewDisplaySelected()}>Remove Selected</Button>
                    <Button onClick={() => deleteAllFailedPlots()}>Delete Failed</Button>
                    <HelpIcon helpId={'visualization.loaded-images'}/>
                </Stack>
            </Stack>
        </Stack>
    );
}

