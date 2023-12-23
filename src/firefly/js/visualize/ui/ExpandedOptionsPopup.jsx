/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Button, Stack} from '@mui/joy';
import React, {useState, useEffect} from 'react';
import {isEmpty, uniq, isEqual} from 'lodash';
import {useStoreConnector} from '../../ui/SimpleComponent';
import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {visRoot, dispatchChangeActivePlotView, dispatchDeletePlotView} from '../ImagePlotCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {
    getMultiViewRoot, dispatchReplaceViewerItems,
    EXPANDED_MODE_RESERVED, IMAGE, dispatchAddViewer, dispatchAddViewerItems
} from '../MultiViewCntlr.js';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {TablePanel} from '../../tables/ui/TablePanel';
import {dispatchTableAddLocal, TABLE_SORT} from '../../tables/TablesCntlr';
import {getTblById, getTblInfoById, processRequest, watchTableChanges} from '../../tables/TableUtil';
import {getFormattedWaveLengthUnits, getPlotViewAry, getPlotViewById, isPlotViewArysEqual} from '../PlotViewUtil';
import {PlotAttribute} from '../PlotAttribute';
import {TABLE_LOADED, TABLE_FILTER, TABLE_FILTER_SELROW} from '../../tables/TablesCntlr';
import {getViewerItemIds} from '../MultiViewCntlr';
import {HelpIcon} from '../../ui/HelpIcon';
import {SelectInfo} from '../../tables/SelectInfo';


const TABLE_ID = 'active-image-view-list-table';

const HIDDEN='_HIDDEN';


export function showExpandedOptionsPopup(title= 'Loaded Images', viewerId= EXPANDED_MODE_RESERVED) {
    dispatchAddViewer(viewerId+HIDDEN, true, IMAGE, false);
    const popup = (
        <PopupPanel title={title}>
            <ImageViewOptionsPanel viewerId={viewerId}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ExpandedOptionsPopup', popup);
    dispatchShowDialog('ExpandedOptionsPopup');
}


const [NAME_IDX, WAVE_LENGTH_UM, PID_IDX, STATUS, PROJ_TYPE_DESC, WAVE_TYPE, DATA_HELP_URL, ROW_IDX] = [0, 1, 2, 3, 4, 5, 6, 7];

const columnsTemplate = [];
columnsTemplate[NAME_IDX] = {name: 'Name', type: 'char', width: 22};
columnsTemplate[PID_IDX] = {name: 'plotId', type: 'char', width: 10, visibility: 'hidden'};
columnsTemplate[STATUS] = {name: 'Status', type: 'char', width: 15};
columnsTemplate[PROJ_TYPE_DESC] = {name: 'Type', type: 'char', width: 8};
columnsTemplate[WAVE_TYPE] = {name: 'Band', type: 'char', width: 8};
columnsTemplate[WAVE_LENGTH_UM] = {
    name: 'Wavelength',
    type: 'double',
    width: 10,
    units: getFormattedWaveLengthUnits('um')
};
columnsTemplate[DATA_HELP_URL] = {name: 'Help', type: 'location', width: 7, links: [{href: '${Help}', value: 'help'}]};

const getAttribute = (attributes, attribute, def='') => attributes?.[attribute] ?? def;

const makeEnumValues = (data, idx) => uniq(data.map((d) => d[idx]).filter((d) => d)).join(',');

function makeModel(tbl_id, plotViewAry, allIds, oldModel) {
    const pvAry= allIds.map( (id) => getPlotViewById(visRoot(),id));
    const data = pvAry.map((pv) => {
        const plot = primePlot(pv);
        const attributes = plot ? plot.attributes : pv.request.getAttributes();
        const {plotId, serverCall, plottingStatusMsg, request} = pv;
        const title = plot ? plot.title : request.getTitle() || 'failed image';
        const row = [];
        let stat;
        if (serverCall === 'success') stat = 'Success';
        else if (serverCall === 'fail') stat = 'Fail';
        else stat = plottingStatusMsg;
        row[NAME_IDX] = title;
        row[PID_IDX] = plotId;
        row[STATUS] = stat;
        row[PROJ_TYPE_DESC] = getAttribute(attributes, PlotAttribute.PROJ_TYPE_DESC);
        row[WAVE_TYPE] = getAttribute(attributes, PlotAttribute.WAVE_TYPE);
        row[WAVE_LENGTH_UM] = parseFloat(getAttribute(attributes, PlotAttribute.WAVE_LENGTH_UM, 0.0));
        row[DATA_HELP_URL] = getAttribute(attributes, PlotAttribute.DATA_HELP_URL);
        return row;
    });

    const columns = [...columnsTemplate];
    columns[PROJ_TYPE_DESC].enumVals = makeEnumValues(data, PROJ_TYPE_DESC);
    columns[WAVE_TYPE].enumVals = makeEnumValues(data, WAVE_TYPE);
    columns[STATUS].enumVals = makeEnumValues(data, STATUS);
    columns[WAVE_LENGTH_UM].enumVals = makeEnumValues(data, WAVE_LENGTH_UM);


    const newSi = SelectInfo.newInstance({rowCount: 0});
    let request;
    if (oldModel) {
        const oldSi = SelectInfo.newInstance(oldModel.selectInfo);
        const vr = visRoot();
        let filterStr = '';
        oldModel.tableData.data.forEach((row, idx) => {
            const plotId = row[PID_IDX];
            if (getPlotViewById(vr, plotId) && oldSi.isSelected(idx)) {
                const newIdx = data.findIndex((r) => r[PID_IDX] === plotId);
                newSi.setRowSelect(newIdx, true);
                newSi.data.rowCount++;
                filterStr += filterStr ? ',' + newIdx : newIdx;
            }
        });
        request = {...oldModel.request};
        if (oldModel?.request?.filters && newSi.data.rowCount > 0) {
            const {filters} = oldModel.request;
            if (filters && filters.indexOf('ROW_IDX' > -1)) {
                request.filters = filters.replace(/"ROW_IDX" IN \(.*\)/, `"ROW_IDX" IN (${filterStr})`);
            }
        }


    }


    let newModel = {
        tbl_id,
        tableData: {columns, data},
        totalRows: data.length, highlightedRow: 0,
        selectInfo: newSi.data,
        tableMeta: {},
        request,
    };
    if (newModel.request) {
        newModel = processRequest(newModel, newModel.request, newModel.highlightedRow);
    }
    dispatchTableAddLocal(newModel, undefined, false);
    return newModel;
}

function dialogComplete(tbl_id, viewerId) {
    const model = getTblById(tbl_id);
    if (!model) return;
    const plotIdAry = model.tableData.data.map((d) => d[PID_IDX]);
    if (isEmpty(plotIdAry)) return;

    const currentPlotIdAry = getViewerItemIds(getMultiViewRoot(), viewerId);
    if (plotIdAry.join('') === currentPlotIdAry.join('')) return;
    if (!plotIdAry.includes(visRoot().activePlotId)) {
        dispatchChangeActivePlotView(plotIdAry[0]);
    }
    dispatchReplaceViewerItems(viewerId, plotIdAry, IMAGE);
}

const deleteFailed = () => {
    getPlotViewAry(visRoot()).forEach((pv) => {
        if (pv.serverCall === 'fail') {
            dispatchDeletePlotView({plotId: pv.plotId});
        }
    });
};


const removeSelected = () => {
    const selectedPlotIds = getSelectedPlotIds();
    selectedPlotIds.forEach((plotId) => {
        dispatchDeletePlotView({plotId});
    });
};


function getSelectedPlotIds(){
    const {tableModel, selectInfo} = getTblInfoById(TABLE_ID);

    return selectInfo.selectAll ?
        tableModel.tableData.data.map((row) => !selectInfo.exceptions.has(parseInt(row[ROW_IDX])) ? row[PID_IDX] : '')
        :
        Array.from(selectInfo.exceptions).map((idx) =>
            tableModel.tableData.data[idx][2]);
}



const pvKeys = ['plotId', 'request', 'serverCall', 'plottingStatusMsg'];
const plotKeys = ['plotImageId'];


function getPvAry(oldPvAry, viewerId) {
    const itemIds= getViewerItemIds(getMultiViewRoot(),viewerId) ?? [];
    const pvAry = getPlotViewAry(visRoot()).filter( (pv) => itemIds.includes(pv.plotId));
    if (!oldPvAry) return pvAry;
    return isPlotViewArysEqual(oldPvAry, pvAry, pvKeys, plotKeys) ? oldPvAry : pvAry;
}

function getAllViewerIds(oldIdAry,viewerId, hiddenViewerId) {
    const itemIds= getViewerItemIds(getMultiViewRoot(),viewerId) ?? [];
    const moreIds= getViewerItemIds(getMultiViewRoot(),hiddenViewerId) ?? [];
    const allIds=
        [...new Set([...itemIds,...moreIds])]
        .filter((id) => Boolean(getPlotViewById(visRoot(),id)));
    return isEqual(oldIdAry, allIds) ? oldIdAry : allIds;
}


function ImageViewOptionsPanel({viewerId}) {

    const tbl_ui_id = TABLE_ID + '-ui';


    const plotViewAry = useStoreConnector((oldPvAry) => getPvAry(oldPvAry,viewerId));
    const allIds = useStoreConnector((old) => getAllViewerIds(old,viewerId, viewerId+HIDDEN));
    const [model, setModel] = useState(undefined);


    useEffect(() => {
        const oldModel = getTblById(TABLE_ID);
        if (!oldModel) {
            watchTableChanges(TABLE_ID, [TABLE_LOADED, TABLE_FILTER, TABLE_FILTER_SELROW, TABLE_SORT],
                () => dialogComplete(TABLE_ID, viewerId));
        }
        setModel(makeModel(TABLE_ID, plotViewAry, allIds, oldModel));
    }, [plotViewAry, allIds]);

    useEffect(() => {
       dispatchAddViewerItems(viewerId+HIDDEN,allIds,IMAGE);
    }, [viewerId, allIds]);

    if (!model) return null;


    return (
        // <Stack sx={{resize: 'both',width: 625, height: 450, minWidth: 250, minHeight: 200}}>
            <Stack {...{ spacing: 2, sx:{p:1, overflow: 'hidden', resize:'both', width: 625, height: 450, minWidth: 250, minHeight: 200} }}>
                <div style={{position: 'relative', width: '100%', height: 'calc(100% - 30px)'}}>
                    <div className='TablePanel'>
                        <div className={'TablePanel__wrapper--border'}>
                            <div className='TablePanel__table' style={{top: 0}}>
                                <TablePanel
                                    tbl_ui_id={tbl_ui_id}
                                    tableModel={model}
                                    showToolbar={true}
                                    showFilters={true}
                                    selectable={true}
                                    showOptionButton={true}
                                    border={false}
                                    showTitle={false}
                                    showPaging={false}
                                    showSave={false}
                                    showTypes={false}
                                    showToggleTextView={false}
                                    expandable={false}
                                    showUnits={true}
                                    rowHeight={23}
                                />
                            </div>
                        </div>
                    </div>
                </div>


                <Stack {...{direction: 'row', justifyContent:'space-between'}}>
                    <CompleteButton
                        text={'Done'}
                        onSuccess={() => dialogComplete(model.tbl_id)}
                        dialogId='ExpandedOptionsPopup'/>

                    <Stack {...{direction: 'row', spacing:1, sx:{'.MuiButton-root':{whiteSpace:'nowrap'}} }}>
                        <Button onClick={() => removeSelected()} >Remove Selected </Button>
                        <Button onClick={() => deleteFailed()}>Delete Failed </Button>
                        <HelpIcon helpId={'visualization.loaded-images'} style={{padding: '8px 9px 0 0'}}/>
                    </Stack>

                </Stack>
            </Stack>

        // </Stack>
    );
}

