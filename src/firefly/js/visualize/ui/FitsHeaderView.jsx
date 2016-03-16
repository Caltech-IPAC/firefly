/*
 L. Zhang Initial version 3/16/16
 DM-4494:FITS Visualizer porting: Show FITS Header
 */
import React from 'react';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {primePlot} from '../PlotViewUtil.js';
import {Tabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {callGetFitsHeaderInfo} from '../../rpc/PlotServicesJson.js';
import {BasicTable} from '../../tables/ui/BasicTable.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/DialogCntlr.js';
import {logError} from '../../util/WebUtil.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {getSizeAsString} from '../../util/WebUtil.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {SortInfo} from '../../tables/SortInfo';
import {TableStore} from  '../../tables/TableStore.js';

import Band from '../Band.js';

const popupIdRoot = 'fitsHeader';

const popupPanelResizableStyle = {
    width: 450,
    minWidth: 450,
    height: 400,
    minHeight: 300,
    resize: 'both',
    overflow: 'auto',
    position: 'relative'
};

const popupPanelFixedSizeStyle = {
    width: 450,
    minWidth: 480,
    height: 480,
    minHeight: 480,
    resize: 'horizontal',
    overflow: 'auto',
    position: 'relative'
};
const tabStyle = {width: '100%', height: '85%'};
const tableStyle = {width: '100%', height: '100%',  overflow: 'auto', flexGrow: 1, display: 'flex'};

const titleStyle = {width: '100%', height: 20, whiteSpace: 'nowrap', display: 'inline-block'};

const textColumn1 = {
    width: 200, paddingLet: 2, textAlign: 'left', color: 'Black', fontWeight: 'bold',
    display: 'inline-block'
};
const textColumn2 = {width: 100, display: 'inline-block', color: 'Black', fontWeight: 'bold'};

const closeButtonStyle = {'textAlign': 'center', display: 'inline-block', marginTop: 30, width: '90%'};
const helpIdStyle = {'textAlign': 'center', display: 'inline-block', marginTop: 30, marginRight: 20};

const mTableStyle = {width: '100%', height: 370, overflow: 'auto', flexGrow: 1, display: 'flex'};


function popupForm(plot, fitsHeaderInfo, popupId) {

    var stats = (fitsHeaderInfo && fitsHeaderInfo.NO_BAND) ?
        renderSingleBandFitsHeader(plot, Band.NO_BAND,fitsHeaderInfo) :
        render3BandFitsHeaders(plot, fitsHeaderInfo);
    //the tab panel can not have the table grow with size, thus use a fixed size panel for now
    var panelStyle = (fitsHeaderInfo && fitsHeaderInfo.NO_BAND) ? popupPanelResizableStyle
        : popupPanelFixedSizeStyle;
    return (
        <div style={panelStyle}>
            { stats}
            <div>
                <div style={closeButtonStyle}>
                    < CompleteButton
                        text='close'
                        onClick={()=>dispatchHideDialog( popupId)}
                        dialogId={popupId}
                    />
                </div>
                <div style={helpIdStyle}>
                    <HelpIcon helpid={'visualization.fitsDownloadOptions'}/>
                </div>
            </div>
        </div>
    );

}
function showFitsHeaderPopup(plot, tableId, fitsHeaderInfo) {

    var popupId = popupIdRoot + '_' + tableId;

    const popTitle = 'FITS Header : ' + plot.title;
    var popup = (<PopupPanel title={popTitle}>
            {popupForm(plot, fitsHeaderInfo, popupId)}
        </PopupPanel>

    );

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}


function renderSingleBandFitsHeader(plot,band, fitsHeaderInfo) {

    var myTableStyle = band===Band.NO_BAND?tableStyle:mTableStyle;
    return (

        <div style={tabStyle}>
            {getFileSizeAndPixelSize(plot, band, fitsHeaderInfo)}
            <div style={myTableStyle}>
                {getTable(band, fitsHeaderInfo)}
            </div>
        </div>

    );

}

function render3BandFitsHeaders(plot, fitsHeaderInfo) {

    return (
        <div  >
            <Tabs defaultSelected={0}>
                <Tab name='RED'>
                    {renderSingleBandFitsHeader(plot, Band.RED, fitsHeaderInfo)}
                </Tab>

                <Tab name='GREEN'>
                    {renderSingleBandFitsHeader(plot, Band.GREEN, fitsHeaderInfo)}
                </Tab>
                <Tab name='BLUE'>
                    {renderSingleBandFitsHeader(plot, Band.BLUE, fitsHeaderInfo)}
                </Tab>
            </Tabs>

        </div>


    );

}

function getFileSizeAndPixelSize(plot, band, fitsHeaderInfo) {

    const tableModel = JSON.parse(fitsHeaderInfo[band]);
    const pt = plot.projection.getPixelScaleArcSec();
    const pixelSize = pt.toFixed(2) + '"';
    const meta = tableModel.tableMeta;
    const fileSize = getSizeAsString(meta.fileSize);
    return (
        <div style={titleStyle}>
            <div style={ textColumn1 }>Pixel Size: {pixelSize} </div>
            <div style={ textColumn2}> File Size: {fileSize}</div>
        </div>
    );
}

/**
 * this method prepare the data needs to make the table
 * @param band
 * @param fitsHeaderInfo
 * @returns {{columns: *, data: *, sortInfo: (*|string|{id, name, isReady}), tableStore: TableStore}}
 */
function prepareData(band, fitsHeaderInfo) {
    const tableModel = JSON.parse(fitsHeaderInfo[band]);

    const tableData = tableModel.tableData;
    const data = tableData.data;
    const columns = tableData.columns;
    const meta = tableModel.tableMeta;
    var columnNames = [];
    for (var i = 0; i < columns.length; i++) {
        columnNames[i] = columns[i].name;
    }
    const sortInfo = SortInfo.newInstance('', columnNames).serialize();

    /*var request = {};
    request['sortInfo'] = sortInfo;
    meta['request'] = request;
    //var newTableModel = Object.assign({}, tableModel, {request:request });

    tableModel['request'] = request;

   const tableStore = TableStore.newInstance(tableModel);//this is not working
    return {columns, data, sortInfo, tableStore};

    const tableStore = TableStore.newInstance(tableModel);//this is not working*/
    return {columns, data, sortInfo};//, tableStore};
}

/**
 * display the data into a tabular format
 * @param band
 * @param fitsHeaderInfo
 * @returns {XML}
 */
function getTable(band, fitsHeaderInfo) {


   // const {columns, data, sortInfo, tableStore} = prepareData(band, fitsHeaderInfo);
    const {columns, data, sortInfo} = prepareData(band, fitsHeaderInfo);
    return (
        <BasicTable
            columns={columns}
            data={data}
            height='calc(100% - 42px)'
            sortInfo={sortInfo}
            //tableStore={tableStore}
        />
    );
}

/**
 *
 *  This function will return the popup component.  As React conversion, the CamelCase is used.
 * @param plotView
 * @constructor
 */
export function fitsHeaderView(plotView) {

    var plot = primePlot(plotView);
    if (!plot)  return;

    var str = plot.title.replace(/\s/g, '');//remove the white places
    var tableId = str.replace(/[^a-zA-Z0-9]/g, '_');//replace the no numeric/alphabet character by _


    callGetFitsHeaderInfo(plot.plotState, tableId)
        .then((result) => {

            showFitsHeaderPopup(plot, tableId, result);
        })
        .catch((e) => {
                logError(`fitsHeader error: ${plot.plotId}`, e);
            }
        );

}
