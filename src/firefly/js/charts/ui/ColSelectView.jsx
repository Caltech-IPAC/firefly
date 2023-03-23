/*
 */
import React from 'react';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {dispatchTableRemove}  from '../../tables/TablesCntlr';
import {clone} from '../../util/WebUtil.js';

import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getTblById, calcColumnWidths, getSelectedData, getColumnValues} from '../../tables/TableUtil.js';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {quoteNonAlphanumeric}  from '../../util/expr/Variable.js';

//import HelpIcon from '../../ui/HelpIcon.jsx';
const popupId = 'XYColSelect';
const TBL_ID ='selectCol';

const popupPanelResizableStyle = {
    width: 300,
    minWidth: 560,
    height: 450,
    minHeight: 300,
    resize: 'both',
    overflow: 'hidden',
    position: 'relative'
};


//define the table style only in the table div
const tableStyle = {boxSizing: 'border-box', width: '100%', height: 'calc(100% - 40px)', overflow: 'hidden', resize:'none'};

//define the complete button
const closeButtonStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginTop:10, width: '90%'};
//define the helpButton
//const helpIdStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginRight: 20};


export function showColSelectPopup(colValStats,onColSelected,popupTitle,buttonText,currentVal,multiSelect=false) {

   if (getTblById(TBL_ID)) {
       hideColSelectPopup();
       dispatchTableRemove(TBL_ID);
}

    colValStats = colValStats.filter((col) => col.visibility !== 'hide' && col.visibility !== 'hidden');
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

    const tableModel = {totalRows: data.length, tbl_id:TBL_ID, tableData: {columns,  data }, highlightedRow: hlRowNum};

    // 360 is the width of table options
    const minWidth = Math.max(columns.reduce((rval, c) => isFinite(c.prefWidth) ? rval+c.prefWidth : rval, 0), 360);
    const popup = (<PopupPanel title={popupTitle}>
            {popupForm(tableModel,onColSelected,buttonText,popupId,minWidth,multiSelect)}
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

function popupForm(tableModel, onColSelected,buttonText,popupId, minWidth,multiSelect) {
    const tblId = tableModel.tbl_id;
    const style= clone(popupPanelResizableStyle, {minWidth: Math.min(minWidth, 560)});
    return (
        <div style={ style}>
            { renderTable(tableModel,popupId,multiSelect)}
            { renderCloseAndHelpButtons(tblId,onColSelected,buttonText,popupId,multiSelect)}
        </div>
    );

}

/**
 * display the data into a tabular format
 * @param tableModel
 * @param popupId
 * @param multiSelect
 * @return table section
 */
function renderTable(tableModel,popupId,multiSelect=false) {
    const tbl_ui_id = (tableModel.tbl_id || 'ColSelectView') + '-ui';
    return (
        <div style={tableStyle}>
            <TablePanel
                key={popupId}
                tbl_ui_id={tbl_ui_id}
                tableModel={tableModel}
                showToolbar={false}
                showFilters={true}
                selectable={multiSelect}
                border={false}
            />
        </div>
    );

}

function renderCloseAndHelpButtons(tblId,onColSelected,buttonText,popupId,multiSelect) {

    return(
    <div>
        <div style={closeButtonStyle}>
            <CompleteButton
                text={buttonText}
                onSuccess={()=> multiSelect? setSelectedColumns(tblId,onColSelected) : setXYColumns(tblId,onColSelected)}
                dialogId={popupId}
            />
        </div>
        {/* comment out the help button for now
            <div style={helpIdStyle}>
                <HelpIcon helpId={'catalogs.xyplots'}/>
            </div>
         */}
    </div>
);
}

//returns (calls the callback fn with) an array of selected column naes
function setSelectedColumns(tblId,onColSelected) {
    getSelectedData(tblId).then((result) => {
        const selectedColNames = getColumnValues(result, 'Name').map((col) => quoteNonAlphanumeric(col)); //create an array of col names
        onColSelected(selectedColNames);
    });
}

//returns (calls the callback fn with) the selected column name
function setXYColumns(tblId,onColSelected) {
    const tableModel = getTblById(tblId);
    const hlRow = tableModel.highlightedRow || 0;
    const selectedColName = quoteNonAlphanumeric(tableModel.tableData.data[hlRow][0]);
    onColSelected(selectedColName);
}

function getHlRow(currentVal,colNames) {
    for(let i = 0; i < colNames.length; i++) {
        if (colNames[i] === currentVal) {
            return i;
        }
    }
}