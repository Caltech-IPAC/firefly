/*
 */
import React from 'react';
import {Stack} from '@mui/joy';
import {merge} from 'lodash';

import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {dispatchTableRemove}  from '../../tables/TablesCntlr';

import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getTblById, calcColumnWidths, getSelectedData, getColumnValues} from '../../tables/TableUtil.js';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {quoteNonAlphanumeric}  from '../../util/expr/Variable.js';
import {SelectInfo} from 'firefly/tables/SelectInfo';

//import HelpIcon from '../../ui/HelpIcon.jsx';
const popupId = 'XYColSelect';
const TBL_ID ='selectCol';


export function showColSelectPopup(colValStats,onColSelected,popupTitle,buttonText,currentVal,multiSelect=false,colTblId,doQuoteNonAlphanumeric=true) {

   if (getTblById(TBL_ID)) {
       hideColSelectPopup();
       dispatchTableRemove(TBL_ID);
    }

    colValStats = colValStats.filter((col) => col.visibility !== 'hidden');
    const colNames = colValStats.map((colVal) => {return colVal.name;});
    const hlRowNum = getHlRow(currentVal,colNames) || 0;

    // make a local table for plot column selection panel
    const columns = [
        {name: 'Name'},
        {name: 'Unit'},
        {name: 'Type'},
        {name: '', visibility: 'hidden'},
        {name: '', visibility: 'hidden'}
    ];
    const data = [];
    for (let i = 0; i < colValStats.length; i++) {
        data[i] = [
            colValStats[i].name,
            colValStats[i].units,
            colValStats[i].type,
            colValStats[i].ucd || '',
            colValStats[i].desc
        ];
    }

    const widths = calcColumnWidths(columns, data);
    columns[0].prefWidth = Math.min(widths[0], 30);  // adjust width of column for optimum display.
    columns[1].prefWidth = Math.min(widths[1], 15);
    columns[2].prefWidth = Math.min(widths[2], 15);
    let idx = 3;
    if (widths[idx]) {
        columns[idx] = {name: 'UCD', prefWidth: widths[idx], visibility: 'show'};
    }
    idx++;
    if (widths[idx]) {
        columns[idx] = {name: 'Description', prefWidth: widths[idx], visibility: 'show'};
    }

    const tableModel = {totalRows: data.length, tbl_id:colTblId ?? TBL_ID, tableData: {columns,  data}, highlightedRow: hlRowNum};

    if (multiSelect) { //select all columns where use:true
        const selectInfoCls = SelectInfo.newInstance({rowCount: tableModel.totalRows});
        tableModel.tableData.data.forEach((row, index) => {
            if (colValStats[index].use) {
                selectInfoCls.setRowSelect(index, true);
            }
        });
        const selectInfo = selectInfoCls.data;
        merge(tableModel, {selectInfo});
    }

    // 360 is the width of table options
    const minWidth = Math.max(columns.reduce((rval, c) => isFinite(c.prefWidth) ? rval+c.prefWidth : rval, 0), 360);
    const popup = (<PopupPanel title={popupTitle}>
            {popupForm(tableModel,onColSelected,buttonText,popupId,minWidth,multiSelect,doQuoteNonAlphanumeric)}
        </PopupPanel>

    );

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}

export function hideColSelectPopup() {
    if (isDialogVisible(popupId)) {
        dispatchHideDialog(popupId);
    }
}

function popupForm(tableModel, onColSelected,buttonText,popupId, minWidth,multiSelect,doQuoteNonAlphanumeric) {
    const tblId = tableModel.tbl_id;
    const tbl_ui_id = (tableModel.tbl_id || 'ColSelectView') + '-ui';
    return (
        <Stack spacing={1} sx={{
            width: '20rem',
            minWidth: Math.min(minWidth, '40rem'),
            height: '32rem',
            minHeight: '20rem',
            resize: 'both',
            overflow: 'hidden',
            position: 'relative'
        }}>
            <TablePanel
                key={popupId}
                tbl_ui_id={tbl_ui_id}
                tableModel={tableModel}
                showToolbar={false}
                showFilters={true}
                selectable={multiSelect}
                border={false}
                onRowDoubleClick={() => setXYColumns(tblId,onColSelected)}
            />

            <CompleteButton
                text={buttonText}
                onSuccess={()=> multiSelect?
                    setSelectedColumns(tblId,onColSelected,doQuoteNonAlphanumeric) :
                    setXYColumns(tblId,onColSelected,doQuoteNonAlphanumeric)}
                dialogId={popupId}
            />
        </Stack>
    );

}


//returns (calls the callback fn with) an array of selected column naes
function setSelectedColumns(tblId,onColSelected,doQuoteNonAlphanumeric) {
    getSelectedData(tblId).then((result) => {
        const selectedColNames = getColumnValues(result, 'Name').map((col) => doQuoteNonAlphanumeric? quoteNonAlphanumeric(col) : col); //create an array of col names
        onColSelected(selectedColNames);
    });
}

//returns (calls the callback fn with) the selected column name
function setXYColumns(tblId,onColSelected,doQuoteNonAlphanumeric) {
    const tableModel = getTblById(tblId);
    const hlRow = tableModel.highlightedRow || 0;
    const selectedColName = doQuoteNonAlphanumeric ?
        quoteNonAlphanumeric(tableModel.tableData.data[hlRow][0]) :tableModel.tableData.data[hlRow][0];
    onColSelected(selectedColName);
    hideColSelectPopup();
}

function getHlRow(currentVal,colNames) {
    for(let i = 0; i < colNames.length; i++) {
        if (colNames[i] === currentVal) {
            return i;
        }
    }
}