/*
 L. Zhang Initial version 3/16/16
 DM-4494:FITS Visualizer porting: Show FITS Header
 */
import React from 'react';
import {get} from 'lodash';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {closeButtonStyle, helpIdStyle} from './FitsHeaderView.jsx';
import {getTblById} from '../../tables/TableUtil.js';
import {primePlot} from '../PlotViewUtil.js';

const popupIdRoot = 'hipsProperty';

//define the table style only in the table div
const tableStyle = {boxSizing: 'border-box', paddingLeft:5,paddingRight:5, width: '100%', height: 'calc(100% - 50px)', overflow: 'hidden', flexGrow: 1, display: 'flex', resize:'none'};


const popupPanelNoPropsResizableStyle = {
    width: 400,
    minWidth: 300,
    height: 300,
    minHeight: 200,
    paddingTop: 30,
    resize: 'both',
    overflow: 'hidden',
    position: 'relative',
    textAlign: 'center'
};

const popupPanelResizableStyle = {
    width: 450,
    minWidth: 450,
    height: 380,
    minHeight: 300,
    marginTop: 30,
    resize: 'both',
    overflow: 'hidden',
    position: 'relative'
};

export function HiPSPropertyView(pv,element) {
    const plot = primePlot(pv);

    if (plot) {
        showHiPSPropsPopup(plot,element);
    }
}

function showHiPSPropsPopup(plot, element) {
    const tableId = plot.title.replace(/\s/g, '').replace(/[^a-zA-Z0-9]/g, '_') + '_HiPS';
    const popupId = popupIdRoot + '_' + tableId;
    const popTitle = 'HiPS Properties : ' + plot.title;

    var popup = (
        <PopupPanel title={popTitle}>
            {popupForm(plot, tableId, popupId)}
        </PopupPanel>
    );

    DialogRootContainer.defineDialog(popupId, popup,element);
    dispatchShowDialog(popupId);
}

function popupForm(plot, tableId, popupId) {
    let   tableModel = getTblById(tableId);
    const hipsProps = get(plot, 'hipsProperties');

    if (!tableModel && hipsProps) {
        const columns = [{name: 'Property', width: 20, type: 'char'},
                         {name: 'Value', width: 30, type: 'char'}];
        const data = Object.keys(hipsProps).reduce((prev, propKey) => {
            prev.push([propKey, hipsProps[propKey]]);
            return prev;
        }, []);

        tableModel = {totalRows: data.length,
                      tbl_id: tableId,
                      tableData:{columns, data},
                      highlightedRow: 0
                      };
    }

    return tableModel ? (
                    <div style={popupPanelResizableStyle}>
                        {renderTable(tableModel)}
                        {renderCloseAndHelpButtons(popupId)}
                    </div>) :
                    (<div style={popupPanelNoPropsResizableStyle}>
                        HiPS properties not found
                     </div>);
}

function renderCloseAndHelpButtons(popupId){
    return(
    <div>
        <div style={closeButtonStyle}>
            <CompleteButton
                text='close'
                onClick={()=>dispatchHideDialog( popupId)}
                dialogId={popupId}
            />
        </div>
        <div style={helpIdStyle}>
            <HelpIcon helpId={'visualization.hipsViewer'}/>
        </div>
    </div>
);
}


/**
 * display the data into a tabular format
 * @param tableModel
 * @returns {XML}
 */
function renderTable(tableModel) {

    const myTableStyle= tableStyle;
    const tbl_ui_id = tableModel.tbl_id + '-ui';
    return (
        <div style={ myTableStyle}>
           <TablePanel
               key={tableModel.tbl_id}
               tbl_ui_id = {tbl_ui_id}
               tableModel={tableModel}
               height='calc(100% - 42px)'
               showToolbar={false}
               selectable={false}
               showOptionButton={false}
           />

        </div>
    );

}
