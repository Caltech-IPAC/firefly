/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState, useEffect} from 'react';
import {isEmpty, get, uniq, isEqual} from 'lodash';
import {useStoreConnector} from '../../ui/SimpleComponent';
import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {visRoot, dispatchChangeActivePlotView, dispatchDeletePlotView} from '../ImagePlotCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {
    getMultiViewRoot, getExpandedViewerItemIds, dispatchReplaceViewerItems,
    EXPANDED_MODE_RESERVED, IMAGE
} from '../MultiViewCntlr.js';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {TablePanel} from '../../tables/ui/TablePanel';
import {dispatchTableAddLocal, TABLE_SORT} from '../../tables/TablesCntlr';
import {getTblById, getTblInfoById, getSelectedData, processRequest, watchTableChanges} from '../../tables/TableUtil';
import {getFormattedWaveLengthUnits, getPlotViewAry, getPlotViewById, isPlotViewArysEqual} from '../PlotViewUtil';
import {PlotAttribute} from '../PlotAttribute';
import {TABLE_LOADED, TABLE_FILTER, TABLE_FILTER_SELROW} from '../../tables/TablesCntlr';
import {getViewerItemIds} from '../MultiViewCntlr';
import {HelpIcon} from '../../ui/HelpIcon';
import {SelectInfo} from '../../tables/SelectInfo';
import {addActionListener} from '../../api/ApiUtil';
import {TABLE_SELECT} from '../../tables/TablesCntlr';


const TABLE_ID = 'active-image-view-list-table';



export function showExpandedOptionsPopup(plotViewAry) {
    const popup = (
        <PopupPanel title={'Loaded Images'}>
            <ImageViewOptionsPanel plotViewAry={plotViewAry}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ExpandedOptionsPopup', popup);
    dispatchShowDialog('ExpandedOptionsPopup');
}


const [NAME_IDX, WAVE_LENGTH_UM, PID_IDX, STATUS, PROJ_TYPE_DESC, WAVE_TYPE, DATA_HELP_URL] = [0, 1, 2, 3, 4, 5, 6];

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
// columnsTemplate[DATA_HELP_URL]= {name: 'Help', type: 'location', width: 35};
columnsTemplate[DATA_HELP_URL] = {name: 'Help', type: 'location', width: 7, links: [{href: '${Help}', value: 'help'}]};


const getAttribute = (attributes, attribute) => get(attributes, [attribute], '');

const makeEnumValues = (data, idx) => uniq(data.map((d) => d[idx]).filter((d) => d)).join(',');


function makeModel(tbl_id, plotViewAry, expandedIds, oldModel) {
    const data = plotViewAry.map((pv) => {
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
        row[WAVE_LENGTH_UM] = getAttribute(attributes, PlotAttribute.WAVE_LENGTH_UM);
        row[DATA_HELP_URL] = getAttribute(attributes, PlotAttribute.DATA_HELP_URL);
        return row;
    });

    const columns = [...columnsTemplate];
    columns[PROJ_TYPE_DESC].enumVals = makeEnumValues(data, PROJ_TYPE_DESC);
    columns[WAVE_TYPE].enumVals = makeEnumValues(data, WAVE_TYPE);
    columns[STATUS].enumVals = makeEnumValues(data, STATUS);
    columns[WAVE_LENGTH_UM].enumVals = makeEnumValues(data, WAVE_LENGTH_UM);


    const newSi = SelectInfo.newInstance({rowCount: 0});
    let newFilters;
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
        if (get(oldModel, 'request.filters') && newSi.data.rowCount > 0) {
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

function dialogComplete(tbl_id) {
    const model = getTblById(tbl_id);
    if (!model) return;
    const plotIdAry = model.tableData.data.map((d) => d[PID_IDX]);
    if (isEmpty(plotIdAry)) return;

    const currentPlotIdAry = getViewerItemIds(getMultiViewRoot(), EXPANDED_MODE_RESERVED);
    if (plotIdAry.join('') === currentPlotIdAry.join('')) return;
    if (!plotIdAry.includes(visRoot().activePlotId)) {
        dispatchChangeActivePlotView(plotIdAry[0]);
    }
    dispatchReplaceViewerItems(EXPANDED_MODE_RESERVED, plotIdAry, IMAGE);
}

const deleteFailedNEW = () => {
    getPlotViewAry(visRoot()).forEach((pv) => {
        if (pv.serverCall === 'fail') {
            dispatchDeletePlotView({plotId: pv.plotId});
        }
    });
};


const RemoveSelected = () => {
    const selectedPlotIds = getSelectedPlotIds();
    selectedPlotIds.forEach((plotId) => {
        dispatchDeletePlotView({plotId});
    });
}


function getSelectedPlotIds(){
    const {tableModel, selectInfo} = getTblInfoById(TABLE_ID);

    return selectInfo.selectAll ?
        tableModel.tableData.data.map(row => !selectInfo.exceptions.has(parseInt(row[7])) ? row[2] : '')
        :
        Array.from(selectInfo.exceptions).map(idx =>
            tableModel.tableData.data[idx][2])
}



const pvKeys = ['plotId', 'request', 'serverCall', 'plottingStatusMsg'];
const plotKeys = ['plotImageId'];


function getPvAry(oldPvAry) {
    const pvAry = getPlotViewAry(visRoot());
    if (!oldPvAry) return pvAry;
    return isPlotViewArysEqual(oldPvAry, pvAry, pvKeys, plotKeys) ? oldPvAry : pvAry;
}

function getExpandedIds(oldIdAry) {
    const expandedIds = getExpandedViewerItemIds(getMultiViewRoot());
    return isEqual(oldIdAry, expandedIds) ? oldIdAry : expandedIds;
}


function ImageViewOptionsPanel() {

    const tbl_ui_id = TABLE_ID + '-ui';


    const [plotViewAry, expandedIds] = useStoreConnector(getPvAry, getExpandedIds);
    const [model, setModel] = useState(undefined);


    useEffect(() => {
        const oldModel = getTblById(TABLE_ID);
        if (!oldModel) {
            watchTableChanges(TABLE_ID, [TABLE_LOADED, TABLE_FILTER, TABLE_FILTER_SELROW, TABLE_SORT], () => dialogComplete(TABLE_ID));
        }
        setModel(makeModel(TABLE_ID, plotViewAry, expandedIds, oldModel));
    }, [plotViewAry, expandedIds]);

    if (!model) return null;
    //const someFailed= plotViewAry.some( (pv) => pv.serverCall==='fail');

    // const hideFailed= () => {
    //     if (isEmpty(plotViewAry)) return;
    //     const plotIdAry= plotViewAry
    //         .filter( (pv) => pv.serverCall!=='fail')
    //         .map( (pv) => pv.plotId);
    //     if (!plotIdAry.includes(visRoot().activePlotId)) {
    //         dispatchChangeActivePlotView(plotIdAry[0]);
    //     }
    //     dispatchReplaceViewerItems(EXPANDED_MODE_RESERVED, plotIdAry, IMAGE);
    // };
    //
    /* const deleteFailed= () => {
         plotViewAry.forEach( (pv) => {
             if (pv.serverCall==='fail') {
                 dispatchDeletePlotView({plotId:pv.plotId}) ;
             }
         });
     };*/


    const deleteFailedButton = () => (
        <button type='button' className='button std hl'
                onClick={() => deleteFailedNEW()}>Delete Failed
        </button>
    );

    //addActionListener(TABLE_SELECT, handleSelect, {divName: 'TablePanel__table'});

    return (
        <div style={{
            resize: 'both', overflow: 'hidden', display: 'flex', flexDirection: 'column',
            width: 625, height: 450, minWidth: 250, minHeight: 200
        }}>

            <div style={{position: 'relative', width: '100%', height: 'calc(100% - 30px)'}}>
                <div className='TablePanel'>
                    <div className={'TablePanel__wrapper--border'}>
                        <div className='TablePanel__table' style={{top: 0}}>
                            {deleteFailedButton}
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


            <div style={{display: 'flex', /*justifyContent:'space-between'*/}}>
                <CompleteButton
                    style={{padding: 5, marginRight: '50%'}} text={'Done'}
                    onSuccess={() => dialogComplete(model.tbl_id)}
                    dialogId='ExpandedOptionsPopup'/>


                <div style={{display: 'flex', padding: 5}}>
                    <button type='button' className='button std hl'
                            onClick={() => RemoveSelected()}>Remove Selected
                    </button>

                </div>


                <div style={{display: 'flex', padding: 5}}>
                    <button type='button' className='button std hl'
                            onClick={() => deleteFailedNEW()}>Delete Failed
                    </button>

                </div>

                <HelpIcon helpId={'visualization.loaded-images'} style={{padding: '8px 9px 0 0'}}/>
            </div>
        </div>
    );
}

