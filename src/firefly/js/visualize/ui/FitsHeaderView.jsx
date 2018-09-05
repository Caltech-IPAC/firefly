/*
 L. Zhang Initial version 3/16/16
 DM-4494:FITS Visualizer porting: Show FITS Header
 */
import React from 'react';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {primePlot} from '../PlotViewUtil.js';
import {Tabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import {logError} from '../../util/WebUtil.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {getSizeAsString} from '../../util/WebUtil.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {getPixScaleArcSec} from '../WebPlot.js';
import {Band} from '../Band.js';
import {get} from 'lodash';
import {dispatchAddActionWatcher} from '../../core/MasterSaga.js';
import numeral from 'numeral';
import ImagePlotCntlr, {visRoot} from '../ImagePlotCntlr.js';

const popupIdRoot = 'directFileAccessData';
const popupPanelResizableStyle = {
    width: 450,
    minWidth: 450,
    height: 400,
    minHeight: 300,
    resize: 'both',
    overflow: 'hidden',
    position: 'relative'
};

//const rgba='rgba(238, 238, 238, 0.25)';
const bgColor= '#e3e3e3';

//define the first label column in the textStyle div
const labelColumn1 = { paddingTop:5, width:80, textAlign: 'right', color: 'Black', fontWeight: 'bold', display: 'inline-block'};
//define the second label column in the textStyle div
const labelColumn2 = { paddingTop:5, width:70, textAlign: 'right',display: 'inline-block', color: 'Black', fontWeight: 'bold'};
//define the text data style
const textStyle={ paddingTop:5,paddingLeft:3, width:140, color: 'Black', fontWeight: 'normal', display: 'inline-block'};

//define the display style for the file size and pixel information and the table in the same div
const tableAndTitleInfoStyle = {width: '100%', height: 'calc(100% - 40px)', display: 'flex', resize:'none'};

//define the table style only in the table div
const tableStyle = {boxSizing: 'border-box', paddingLeft:5,paddingRight:5, width: '100%', height: 'calc(100% - 70px)', overflow: 'hidden', flexGrow: 1, display: 'flex', resize:'none'};

const tableOnTabStyle = {boxSizing: 'border-box',paddingLeft:5,paddingRight:5, width: '100%', height: 'calc(100% - 30px)', overflow: 'hidden', flexGrow: 1, display: 'flex', resize:'none'};//
//define the size of the text on the tableInfo style in the title div

//define the complete button
export const closeButtonStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginTop:10, width: '90%'};
//define the helpButton
export const helpIdStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginRight: 20};


//3-color styles
const tabStyle =  {width: '100%',height:'100%', display: 'inline-block', background:bgColor};


function popupForm(plot, fitsHeaderInfo, popupId) {


    if (fitsHeaderInfo && plot.plotState.getBands().length===1 ) {
        return renderSingleBandFitsHeader(plot, fitsHeaderInfo, popupId);
    }
    else {
        return renderColorBandsFitsHeaders(plot, fitsHeaderInfo, popupId);
    }
}

const FITSHEADER_DIALOGID = popupIdRoot+'_fitsHeader';

function showFitsHeaderPopup(plot, fitsHeaderInfo, element) {

    //var popupId = popupIdRoot + '_' + tableId;

    const popupId = FITSHEADER_DIALOGID;
    const getTitle =  (p) => {
         return 'FITS Header : ' + p.title;
    };

    const getPopup = (aPlot, fitsHeaderTbl) => {
        return (<PopupPanel title={getTitle(aPlot)}>
                {popupForm(aPlot, fitsHeaderTbl, popupId)}
            </PopupPanel>
        );
    };

    const watchActivePlotChange = (action, cancelSelf) => {
        if (!isDialogVisible(popupId)) {
            cancelSelf();
        } else {
            const {plotId} = action.payload;
            const crtPlot = primePlot(visRoot(), plotId);
            const newTableModel = createFitsHeaderTable(null, crtPlot);

            DialogRootContainer.defineDialog(popupId, getPopup(crtPlot, newTableModel), element);
            dispatchShowDialog(popupId, plot.plotId);
        }
    };


    if (!isDialogVisible(popupId)) {
        dispatchAddActionWatcher({actions: [ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW, ImagePlotCntlr.CHANGE_PRIME_PLOT],
                                  callback:  watchActivePlotChange});
        DialogRootContainer.defineDialog(popupId, getPopup(plot, fitsHeaderInfo), element);
        dispatchShowDialog(popupId, plot.plotId);
    }
}

function renderSingleBandFitsHeader(plot, fitsHeaderInfo, popupId){
    const band = plot.plotState.getBands()[0];
    return (
        <div style={ popupPanelResizableStyle}>
            { renderFileSizeAndPixelSize(plot, band, fitsHeaderInfo)}
            { renderTable( band,fitsHeaderInfo, false)}
            { renderCloseAndHelpButtons(popupId)}
        </div>
    );

}


function renderColorBandsFitsHeaders(plot, fitsHeaderInfo, popupId) {

    const bands = plot.plotState.getBands();

    let colorBandTabs;
    switch (bands.length){
        case 2:
        colorBandTabs = (
                <Tabs defaultSelected={0} useFlex={true}>
                    {renderSingleTab(plot, bands[0],fitsHeaderInfo )}
                    {renderSingleTab(plot, bands[1],fitsHeaderInfo )}
            </Tabs>
         );
            break;
        case 3:
            colorBandTabs = (
                <Tabs defaultSelected={0} useFlex={true}>
                    {renderSingleTab(plot, bands[0],fitsHeaderInfo )}
                    {renderSingleTab(plot, bands[1],fitsHeaderInfo )}
                    {renderSingleTab(plot, bands[2],fitsHeaderInfo )}
                </Tabs>
            );
            break;
    }

    return (
        <div style={ popupPanelResizableStyle} >
          <div style = {tableAndTitleInfoStyle}>
              {colorBandTabs}
        </div>
            { renderCloseAndHelpButtons(popupId)}
        </div>


    );

}

function renderSingleTab(plot, band, fitsHeaderInfo) {

    return (
    <Tab name = {band.key}  >
        <div style={{position:'relative', flexGrow:1}}>
            <div style={{position:'absolute', top:0, bottom:0, left:0, right:0}}>
                <div style={tabStyle}>
                    { renderFileSizeAndPixelSize(plot, band, fitsHeaderInfo)}
                    { renderTable( band,fitsHeaderInfo,true)}
                </div>
            </div>
        </div>
    </Tab>
  );
}
function renderCloseAndHelpButtons(popupId){
    return(
    <div>
        <div style={closeButtonStyle}>
            < CompleteButton
                text='close'
                onClick={()=>dispatchHideDialog( popupId)}
                dialogId={popupId}
            />
        </div>
        <div style={helpIdStyle}>
            <HelpIcon helpId={'visualization.fitsViewer'}/>
        </div>
    </div>
);
}

function renderFileSizeAndPixelSize(plot, band, fitsHeaderInfo, isOnTab) {

    const tableModel = fitsHeaderInfo[band];
    const pt = getPixScaleArcSec(plot);
    const pixelSize = pt.toFixed(2) + '"';

    const  mSize = get(tableModel, ['tableMeta', 'fileSize'], 0);
    const  fileSize = (mSize && mSize > 0)  ? getSizeAsString(mSize) : '';
    let    fileSizeStr;

    if (fileSize) {
        const flen = fileSize.substring(0, fileSize.length - 1);

        fileSizeStr = `${numeral(flen).format('0.00')}${fileSize.substring(fileSize.length - 1, fileSize.length)}`;
    } else {
        fileSizeStr = '';
    }

    const titleStyleNoTab = {width: '100%', height: 30,display: 'inline-block', background:bgColor};
    const titleStyleOnTab = {width: '100%', height: 30,display: 'inline-block'};
    var titleStyle = isOnTab? titleStyleOnTab:titleStyleNoTab;

   return (
        <div style={titleStyle}>
            <div style={ labelColumn1 }>Pixel Size:</div>
            < div style= {textStyle} >{pixelSize}</div>
            <div style={ labelColumn2}> File Size:</div>
            <div style= {textStyle} >{fileSizeStr}</div>
        </div>
    );
}


/**
 * display the data into a tabular format
 * @param band
 * @param fitsHeaderInfo
 * @param isPlacedOnTab
 * @returns {XML}
 */
function renderTable(band, fitsHeaderInfo, isPlacedOnTab) {

    const tableModel = fitsHeaderInfo[band];
    var myTableStyle= isPlacedOnTab?tableOnTabStyle:tableStyle;
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
               showOptionButton={true}
           />

        </div>
    );

}

/**
 * This function will return the popup component.  As React conversion, the CamelCase is used.
 * @param plotView
 * @param element
 */
export function fitsHeaderView(plotView,element) {

    var plot = primePlot(plotView);
    if (!plot)  return;

    const resultTable = createFitsHeaderTable(null, plot);
    if (resultTable) {
        showFitsHeaderPopup(plot, resultTable, element);
    } else {
        logError(`fitsHeader error: ${plot.plotId}`);
    }

}

/**
 * produce table Id based on band info
 * @param plot
 * @returns {*}
 */
function createTableIdForFitsHeader(plot) {
    let   colors;
    const bands = plot.plotState.getBands();

    switch (bands.length){
        case 1:
            if (bands[0]===Band.NO_BAND){
                colors='_noBand';
            }
            else {
                colors=bands[0].key;
            }
            break;
        case 2:
            colors=bands[0].key + '_' + bands[1].key;
            break;
        case 3:
            colors=bands[0].key  + '_' + bands[1].key +'_'+bands[2].key ;
            break;

    }

    const str = plot.plotImageId.replace(/\s/g, '');                   //remove the white places
    return  str.replace(/[^a-zA-Z0-9]/g, '_')  + colors; //replace the no numeric/alphabet character by _
}

/**
 * create fits header table model based on the fits header array from plot
 * @param tableId
 * @param plot
 * @returns {*}
 */
function createFitsHeaderTable(tableId, plot) {
    const {headerAry} = plot;
    if (!headerAry) return null;

    tableId = tableId ? tableId : createTableIdForFitsHeader(plot);
    const bands = plot.plotState.getBands();

    const columns = [{name: '#', type: 'int', width: 3},
                     {name: 'Keyword', type: 'char', width: 10},
                     {name: 'Value', type: 'char', width: 15},
                     {name: 'Comments', type: 'char', width: 32}];
    const headerComment = 'comment';
    const headerValue = 'value';

    const getHeaderData = (header) => {
        const headerKeys = Object.keys(header).sort(sortHeaderKey(header));

        return headerKeys.reduce((prev, aKey, idx) => {
            if (aKey !== 'COMMENT') {
                let val = get(header, [aKey, headerValue]);
                let cmt = get(header, [aKey, headerComment]);

                if (!val) val = '';
                if (!cmt) cmt = '';

                prev.push([`${idx}`, aKey, val, cmt]);
            }
            return prev;
        }, []);
    };

    return bands.reduce((prev, oneBand) => {
            const tbl_id = oneBand === Band.NO_BAND ? tableId: `${tableId}-${oneBand.key}`;
            const data = getHeaderData(get(headerAry, [oneBand]));

            prev[oneBand.key] = {tbl_id, tableData: {columns, data},
                                 totalRows: data.length, highlightedRow: 0,
                                 tableMeta: {fileSize: get(plot, ['webFitsData', oneBand, 'getFitsFileSize'])}};
        return prev;
    }, {});
}

/**
 * sort the header key/value based on the original order in fits header
 * @param header
 * @returns {Function}
 */
function sortHeaderKey(header) {
    return (a, b) => {
        return header[a].idx - header[b].idx;
    };
}