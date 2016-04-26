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
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {logError} from '../../util/WebUtil.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {getSizeAsString} from '../../util/WebUtil.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {Band} from '../Band.js';

const popupIdRoot = 'fitsHeader';

const popupPanelResizableStyle = {
    width: 450,
    minWidth: 450,
    height: 400,
    minHeight: 300,
    resize: 'both',
    overflow: 'hidden',
    position: 'relative'
};


//define the display style for the file size and pixel information and the table in the same div
const tableAndTitleInfoStyle = {width: '100%', height: 'calc(100% - 40px)', display: 'flex', resize:'none'};

//define the table style only in the table div
const tableStyle = {boxSizing: 'border-box', paddingLeft:5,paddingRight:5, width: '100%', height: 'calc(100% - 70px)', overflow: 'hidden', flexGrow: 1, display: 'flex', resize:'none'};


const tableOnTabStyle = {boxSizing: 'border-box',paddingLeft:5,paddingRight:5, width: '100%', height: 'calc(100% - 30px)', overflow: 'hidden', flexGrow: 1, display: 'flex', resize:'none'};//
//define the size of the text on the tableInfo style in the title div
const titleStyle = {width: '100%', height: 30};

//define the first column in the textStyle div
const textColumn1 = {paddingTop:5,
    width: 100, paddingLet: 2, textAlign: 'left', color: 'Black', fontWeight: 'bold',
    display: 'inline-block',paddingLeft:10
};

//define the second column in the textStyle div
const textColumn2 = {paddingTop:5,paddingLeft:40, width: 250, display: 'inline-block', color: 'Black', fontWeight: 'bold'};

//define the complete button
const closeButtonStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginTop:10, width: '90%'};
//define the helpButton
const helpIdStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginRight: 20};



//3-color styles
const tabStyle =  {width: '100%',height:'100%'};


function popupForm(plot, fitsHeaderInfo, popupId) {

    if (fitsHeaderInfo && plot.plotState.getBands().length===1 ) {
        return renderSingleBandFitsHeader(plot, fitsHeaderInfo, popupId);
    }
    else {
        return renderColorBandsFitsHeaders(plot, fitsHeaderInfo, popupId);
    }
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

function renderSingleBandFitsHeader(plot, fitsHeaderInfo, popupId){
    const band = plot.plotState.getBands()[0];
    return (
        <div style={ popupPanelResizableStyle}>
            {renderFileSizeAndPixelSize(plot, band, fitsHeaderInfo)}
            { renderTable( band,fitsHeaderInfo, false)}
            { renderCloseAndHelpButtons(popupId)}
        </div>
    );

}


function renderColorBandsFitsHeaders(plot, fitsHeaderInfo, popupId) {

    const bands = plot.plotState.getBands();

    var colorBandTabs;
    switch (bands.length){
        case 2:
        colorBandTabs = (
                <Tabs defaultSelected={0} >
                    {renderSingleTab(plot, bands[0],fitsHeaderInfo )}
                    {renderSingleTab(plot, bands[1],fitsHeaderInfo )}
            </Tabs>
         );
            break;
        case 3:
            colorBandTabs = (
                <Tabs defaultSelected={0} >
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
                    {renderFileSizeAndPixelSize(plot, band, fitsHeaderInfo)}
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
            <HelpIcon helpid={'visualization.fitsDownloadOptions'}/>
        </div>
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
function renderTable(band, fitsHeaderInfo, isPlacedOnTab) {

    const tableModel = fitsHeaderInfo[band];
    var myTableStyle= isPlacedOnTab?tableOnTabStyle:tableStyle;
    return (
        <div style={ myTableStyle}>
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
