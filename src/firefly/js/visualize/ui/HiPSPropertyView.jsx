import React from 'react';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {LayoutType, PopupPanel} from '../../ui/PopupPanel.jsx';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {getTblById} from '../../tables/TableUtil.js';
import {primePlot} from '../PlotViewUtil.js';

const tableStyle = {
    boxSizing: 'border-box', paddingLeft:5,paddingRight:5, width: '100%',
    overflow: 'hidden', flexGrow: 1, display: 'flex', resize:'none'
};

export const HIPS_PROPERTY_POPUP_ID = 'hipsPropertyID';

const popupPanelResizableStyle = {
    minWidth: 450, minHeight: 400, height: 400, marginTop: 5, display:'flex', flexDirection:'column',
    resize: 'both', overflow: 'hidden', position: 'relative'
};

export function showHiPSPropertyView(pv, element, initLeft, initTop, onMove) {
    if (primePlot(pv)) showHiPSPropsPopup(primePlot(pv),element, initLeft,initTop, onMove);
}

function showHiPSPropsPopup(plot, element, initLeft,initTop, onMove) {
    const tableId = plot.title.replace(/\s/g, '').replace(/[^a-zA-Z0-9]/g, '_') + '_HiPS';
    const layoutPosition= isNaN(initLeft)||isNaN(initTop)?LayoutType.TOP_CENTER:LayoutType.USER_POSITION;

    const popup = (
        <PopupPanel {...{ title:'HiPS Properties : ' + plot.title, initLeft, initTop, onMove, layoutPosition}} >
            {popupForm(plot, tableId, HIPS_PROPERTY_POPUP_ID)}
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(HIPS_PROPERTY_POPUP_ID, popup,element);
    dispatchShowDialog(HIPS_PROPERTY_POPUP_ID);
}

function makeHiPSPropModel(hipsProps,hipsUrlRoot, tableId) {
    const columns = [{name: 'Property', width: 20, type: 'char'}, {name: 'Value', width: 60, type: 'char'}];
    const data = [['hips_service_url',hipsUrlRoot], ...Object.entries(hipsProps)];
    return {totalRows: data.length, tbl_id: tableId, tableData:{columns, data}, highlightedRow: 0 };
}

function popupForm(plot, tableId, popupId) {
    if (!plot?.hipsProperties) return <NotFound/>;
    const tableModel= getTblById(tableId) || makeHiPSPropModel(plot.hipsProperties, plot.hipsUrlRoot,tableId);
    return (
        <div style={popupPanelResizableStyle}>
            {renderTable(tableModel)}
            {renderCloseAndHelpButtons(popupId)}
        </div> ) ;
}

const NotFound= () => (
    <div style={{ width: 400, minWidth: 300, height: 300, minHeight: 200, paddingTop: 30,
        resize: 'both', overflow: 'hidden', position: 'relative', textAlign: 'center' }}>
        HiPS properties not found
    </div>
);

const renderCloseAndHelpButtons = (popupId) => (
    <div style={{display:'flex', justifyContent:'space-between', margin:'8px 7px 8px 5px', alignItems:'center'}}>
        <CompleteButton text='Close' onClick={()=>dispatchHideDialog( popupId)} dialogId={popupId} />
        <HelpIcon helpId={'tables'}/>
    </div> );

const renderTable= (tableModel) => (
    <div style={ tableStyle}>
        <TablePanel
            key={tableModel.tbl_id} tbl_ui_id = {tableModel.tbl_id + '-ui'} tableModel={tableModel}
            height='calc(100% - 42px)'
            showToolbar={false} selectable={false} showOptionButton={false} allowUnits={false}
            showFilters={true} showTypes={false} />
    </div> );
