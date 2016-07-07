/*
 */
import React from 'react';
import DialogRootContainer from '../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../ui/PopupPanel.jsx';


import {BasicTable} from '../tables/ui/BasicTable.jsx';
import {getTblById} from '../tables/TableUtil.js';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';
import {setOptions} from './XYPlotOptions.jsx';
import CompleteButton from '../ui/CompleteButton.jsx';
import HelpIcon from '../ui/HelpIcon.jsx';
import {getTblInfo} from  '../tables/TableUtil.js';
const popupId = 'XYColSelect';

const popupPanelResizableStyle = {
    width: 600,
    minWidth: 450,
    height: 450,
    minHeight: 300,
    resize: 'both',
    overflow: 'hidden',
    position: 'relative'
};


//define the table style only in the table div
const tableStyle = {boxSizing: 'border-box', paddingLeft:5,paddingRight:5, width: '100%', height: 'calc(100% - 70px)', overflow: 'hidden', flexGrow: 1, display: 'flex', resize:'none'};

//define the complete button
const closeButtonStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginTop:10, width: '90%'};
//define the helpButton
const helpIdStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginRight: 20};


export function showColSelectPopup(colValStats,xyPlotParams,popupTitle,buttonText,groupKey) {

    const colNames = colValStats.map((colVal) => {return colVal.name;});
    const colUnit =  colValStats.map((colVal) => {return colVal.unit;});
    const colDescr =  colValStats.map((colVal) => {return colVal.descr;});

    // make a local table for plot column selection panel
    var columns = [
                    {name: 'Name',visibility: 'show', prefWidth: 12, fixed: true},
                    {name: 'Unit',visibility: 'show', prefWidth: 8},
                    {name: 'Type',visibility: 'show', prefWidth: 8},
                    {name: 'Description',visibility: 'show', prefWidth: 60}
                ];
    var data = [];
    for (var i = 0; i < colValStats.length; i++) {
            data[i] = [
                        colValStats[i].name,
                        colValStats[i].unit,
                        colValStats[i].type,
                        colValStats[i].descr
            ];
    }
    const request = {pageSize:10000};
    var tableModel = {totalRows: data.length, request, tbl_id:'selectCol', tableData: {columns,  data }, highlightedRow: '0'};


    var popup = (<PopupPanel title={popupTitle}>
            {popupForm(tableModel,xyPlotParams,buttonText,groupKey,popupId)}
        </PopupPanel>

    );

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}

function popupForm(tableModel, xyPlotParams,buttonText,groupKey,popupId) {
    const tblId = tableModel.tbl_id;
    return (
        <div style={ popupPanelResizableStyle}>
            { renderTable(tableModel,popupId)}
            { renderCloseAndHelpButtons(tblId,xyPlotParams,buttonText,groupKey,popupId)}
        </div>
    );

}

/**
 * display the data into a tabular format
 * @param tableModel
 * @param popupId
 * @return table section
 */
function renderTable(tableModel,popupId) {

    return (
        <div style={tableStyle}>
           <BasicTable
               key={popupId}
               tableModel={tableModel}
           />
        </div>
    );

}

function renderCloseAndHelpButtons(tblId,xyPlotParams,buttonText,groupKey,popupId) {

    return(
    <div>
        <div style={closeButtonStyle}>
            < CompleteButton
                text={buttonText}
                onSuccess={()=>setXYColumns(tblId,buttonText,xyPlotParams,groupKey)}
                dialogId={popupId}
            />
        </div>
        {/* comment out the help button for now
            <div style={helpIdStyle}>
                <HelpIcon helpid={'visualization.setxyplot'}/>
            </div>
         */}
    </div>
);
}

function setXYColumns(tblId,buttonText,xyPlotParams,groupKey) {
    const tableModel = getTblById(tblId);
    var hlRow = getTblInfo(tableModel,1).highlightedRow;
    if (hlRow) {
        var seltopt = tableModel.tableData.data[hlRow];
    } else {
        seltopt = tableModel.tableData.data[0];
    }
    if (buttonText === 'Set X') {
        xyPlotParams.x = {columnOrExpr: seltopt[0], lable: seltopt[0], unit: seltopt[1]};
    } else if (buttonText === 'Set Y') {
        xyPlotParams.y = {columnOrExpr: seltopt[0], lable: seltopt[0], unit: seltopt[1]};
    }

    setOptions(groupKey,xyPlotParams);
}