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


//define the display style for the file size and pixel information and the table in the same div
const tableAndTitleInfoStyle = {width: '100%', height: 'calc(100% - 40px)'};

//define the table style only in the table div
const tableStyle = {width: '100%', height: 'calc(100% - 30px)',  overflow: 'auto', flexGrow: 1, display: 'flex'};

//define the size of the text on the tableInfo style in the title div
const titleStyle = {width: '100%', height: 20};

//define the first column in the textStyle div
const textColumn1 = {
    width: 200, paddingLet: 2, textAlign: 'left', color: 'Black', fontWeight: 'bold',
    display: 'inline-block'
};
//define the second column in the textStyle div
const textColumn2 = {width: 100, display: 'inline-block', color: 'Black', fontWeight: 'bold'};

//define the complete button
const closeButtonStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginTop:8, width: '90%'};
//define the helpButton
const helpIdStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginRight: 20};



//3-color styles
const tabStyle =  {width: '100%',height:'100%'};//,  display: 'inline-block',  overflow: 'auto', flexGrow: 1};



function popupForm(plot, fitsHeaderInfo, popupId) {
    var fitsHeaderRenderInfo = (fitsHeaderInfo && fitsHeaderInfo.NO_BAND) ?
        renderSingleBandFitsHeader(plot, Band.NO_BAND,fitsHeaderInfo) :
        render3BandFitsHeaders(plot, fitsHeaderInfo);

    return (
        <div style={popupPanelResizableStyle}>
            {fitsHeaderRenderInfo}
            {renderCloseAndHelpButtons(popupId)}

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


function render3BandFitsHeaders(plot, fitsHeaderInfo) {

    return (
          <div style = {tableAndTitleInfoStyle}>
            <Tabs defaultSelected={0} >
                <Tab name='RED'  >
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
            <HelpIcon helpid={'visualization.fitsDownloadOptions'}/>
        </div>
    </div>
);
}
function renderSingleBandFitsHeader(plot,band, fitsHeaderInfo) {


    var fitsHeaderInfoStyle = band==Band.NO_BAND? tableAndTitleInfoStyle: tabStyle;

    return (

        <div style={fitsHeaderInfoStyle}>
            {renderFileSizeAndPixelSize(plot, band, fitsHeaderInfo)}
            { renderTable( band,fitsHeaderInfo)}
        </div>

    );
}

function renderFileSizeAndPixelSize(plot, band, fitsHeaderInfo) {

    const tableModel = fitsHeaderInfo[band];
    const pt = plot.projection.getPixelScaleArcSec();
    const pixelSize = pt.toFixed(2) + '"';

    const  meta = tableModel.tableMeta;
    const fileSize = getSizeAsString(meta.fileSize);
    return (
        <div style={titleStyle}>
            <div style={ textColumn1 }>Pixel Size: {pixelSize} </div>
            <div style={ textColumn2}> File Size: {fileSize}</div>
        </div>
    );
}


/**
 * display the data into a tabular format
 * @param band
 * @param fitsHeaderInfo
 * @returns {XML}
 */
function renderTable(band, fitsHeaderInfo) {

    const tableModel = fitsHeaderInfo[band];
    return (
        <div style={tableStyle }>
           <BasicTable
               key={tableModel.tbl_id}
               tableModel={tableModel}
               height='calc(100% - 42px)'
           />

        </div>
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
