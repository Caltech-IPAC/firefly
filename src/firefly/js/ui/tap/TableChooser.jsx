import React from 'react';
import {Stack} from '@mui/joy';

import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';

import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getTblById, getCellValue} from '../../tables/TableUtil.js';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import CompleteButton from '../../ui/CompleteButton.jsx';

const popupId = 'TableChooserPopup';


export function showTableSelectPopup(title, tableModel, onTableNameSelected) {

    DialogRootContainer.defineDialog(popupId,
        <PopupPanel title={title}>
            <TableChooser tableModel={tableModel} onTableNameSelected={onTableNameSelected}/>
        </PopupPanel> );
    dispatchShowDialog(popupId);
}

function TableChooser({tableModel, onTableNameSelected= () => undefined}) {

    const visable= ['table_name', 'table_desc', 'nrows', 'irsa_nrows'];
    tableModel.tableData.columns.forEach( (c) => {
        if (visable.includes(c.name)) {
            switch (c.name) {
                case 'table_name':
                    c.prefWidth= 22;
                    c.label= 'Table Name';
                    break;
                case 'table_desc':
                    c.prefWidth= 80;
                    c.label= 'Description';
                    break;
                case 'nrows':
                case 'irsa_nrows':
                    c.prefWidth= 10;
                    c.label= 'Rows';
                    break;
            }
        }
        else {
            c.visibility='hidden';
        }
    });

    const tblId = tableModel.tbl_id;
    const tbl_ui_id = (tableModel.tbl_id || 'ColSelectView') + '-ui';
    return (
        <Stack spacing={1} sx={{
            width: '20rem', minWidth: '70rem', height: '32rem', minHeight: '20rem', resize: 'both',
            overflow: 'hidden', position: 'relative' }}>
            <TablePanel
                key={popupId} tbl_ui_id={tbl_ui_id} tableModel={tableModel}
                showToolbar={false} showFilters={true} selectable={false} border={false}
                onRowDoubleClick={() => findTable(tblId,onTableNameSelected)}
            />

            <CompleteButton text='Choose' dialogId={popupId}
                onSuccess={()=> findTable(tblId,onTableNameSelected) } />
        </Stack>
    );

}


function findTable(tblId,onTableNameSelected) {
    const tableModel = getTblById(tblId);
    const hlRow = tableModel.highlightedRow || 0;
    const name= getCellValue(tableModel,hlRow, 'table_name');
    onTableNameSelected(name);
    if (isDialogVisible(popupId)) dispatchHideDialog(popupId);
}