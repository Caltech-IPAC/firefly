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
const popupIdRoot = 'XYplotColOps';

const popupPanelResizableStyle = {
    width: 450,
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


export function showXYPlotColPopup(tableModel,xyPlotParams,fieldKey,groupKey) {

    var popupId = popupIdRoot;
    var popTitle = '';
    var plotCol = '';
    var buttonText ='';
    if (fieldKey === 'x.columnOrExpr') {
        popTitle = 'Select X Col : ';
        plotCol ='x';
        buttonText = 'Set X';
    } else if (fieldKey ==='y.columnOrExpr') {
        popTitle = 'Select Y Col : ';
        plotCol ='y';
        buttonText = 'Set Y';
    }

    var popup = (<PopupPanel title={popTitle}>
            {popupForm(tableModel,xyPlotParams,plotCol,buttonText,groupKey,popupId)}
        </PopupPanel>

    );

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}

function popupForm(tableModel, xyPlotParams, plotCol,buttonText,groupKey,popupId) {
    const tblId = tableModel.tbl_id;
    return (
        <div style={ popupPanelResizableStyle}>
            { renderTable(tableModel,popupId)}
            { renderCloseAndHelpButtons(tblId,xyPlotParams,plotCol,buttonText,groupKey,popupId)}
        </div>
    );

}

/**
 * display the data into a tabular format
 * @param
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

function renderCloseAndHelpButtons(tblId,xyPlotParams,plotCol,buttonText,groupKey,popupId) {

    return(
    <div>
        <div style={closeButtonStyle}>
            < CompleteButton
                text={buttonText}
                onSuccess={()=>setXYColumns(tblId,plotCol,xyPlotParams,groupKey)}
                dialogId={popupId}
            />
        </div>
        <div style={helpIdStyle}>
            <HelpIcon helpid={'visualization.setxyplot'}/>
        </div>
    </div>
);
}

function setXYColumns(tblId,plotCol,xyPlotParams,groupKey) {
    const tableModel = getTblById(tblId);
    var hlRow = getTblInfo(tableModel,1).highlightedRow;
    if (hlRow) {
        var seltopt = tableModel.tableData.data[hlRow];
    } else {
        seltopt = tableModel.tableData.data[0];
    }
    if (plotCol === 'x') {
        xyPlotParams.x = {columnOrExpr: seltopt[0], lable: seltopt[0], unit: seltopt[1]};
    } else if (plotCol === 'y') {
        xyPlotParams.y = {columnOrExpr: seltopt[0], lable: seltopt[0], unit: seltopt[1]};
    }

    setOptions(groupKey,xyPlotParams);
}