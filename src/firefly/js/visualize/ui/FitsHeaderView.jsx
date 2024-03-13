/*
 L. Zhang Initial version 3/16/16
 DM-4494:FITS Visualizer porting: Show FITS Header
 */
import {Stack, Typography} from '@mui/joy';
import React from 'react';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {LayoutType, PopupPanel} from '../../ui/PopupPanel.jsx';
import {primePlot, getPlotViewById, isImageCube, getCubePlaneCnt} from '../PlotViewUtil.js';
import {Tabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import {logger} from '../../util/Logger.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {getSizeAsString} from '../../util/WebUtil.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {getPixScaleArcSec} from '../WebPlot.js';
import {Band} from '../Band.js';
import {dispatchAddActionWatcher} from '../../core/MasterSaga.js';
import ImagePlotCntlr, {visRoot} from '../ImagePlotCntlr.js';
import {getTblById} from '../../tables/TableUtil.js';
import {TABLE_SORT, TABLE_REPLACE, dispatchTableSort} from '../../tables/TablesCntlr.js';
import {getHDU, isThreeColor} from '../PlotViewUtil';
import { getAllValuesOfHeader, } from '../FitsHeaderUtil.js';

const popupPanelResizableSx = {
    width: 550, minWidth: 448, height: 400, minHeight: 300, resize: 'both', overflow: 'hidden', position: 'relative'};


//define the display style for the file size and pixel information and the table in the same div

//define the table style only in the table div
const tableStyle = {boxSizing: 'border-box', paddingLeft:5,paddingRight:5, width: '100%', overflow: 'hidden', flexGrow: 1, resize:'none'};

const tableOnTabStyle = {boxSizing: 'border-box',paddingLeft:5,paddingRight:5, width: '100%', height: 'calc(100% - 30px)', overflow: 'hidden', flexGrow: 1, display: 'flex', resize:'none'};//
//define the size of the text on the} tableInfo style in the title div


//3-color styles
const tabStyle =  {width: '100%',height:'100%'};

const FITSHEADERCONTENT = 'fitsHeader';
let currentSortInfo = '';

export const FITS_HEADER_POPUP_ID = 'FITS_HEADER_POPUP_ID';

function popupForm(plot, fitsHeaderInfo, popupId) {
    return (fitsHeaderInfo && plot.plotState.getBands().length===1) ?
        renderSingleBandFitsHeader(plot, fitsHeaderInfo, popupId) :
        renderColorBandsFitsHeaders(plot, fitsHeaderInfo, popupId);
}



function showFitsHeaderPopup(plot, fitsHeaderInfo, element, initLeft, initTop, onMove) {

    const getTitle =  (p) => {
         const pv = getPlotViewById(visRoot(), p.plotId);
         let   title;
         if (pv.plots.length === 1) {
             title = p.title;
         } else {
             const EXT = ': - ext.';
             const idx = p.title ? p.title.indexOf(EXT) : -1;
             title = (idx >= 0 ?  p.title.slice(0, idx) : p.title) + ' - ' + (pv.primeIdx+1);
         }
         return 'FITS Header : ' + title;
    };

    const getPopup = (aPlot, fitsHeaderTbl, initLeft, initTop) => {
        return (<PopupPanel title={getTitle(aPlot)}
                            onMove={onMove}
                            initLeft={initLeft} initTop={initTop}
                            layoutPosition={isNaN(initLeft)||isNaN(initTop)?LayoutType.TOP_CENTER:LayoutType.USER_POSITION} >
                {popupForm(aPlot, fitsHeaderTbl, FITS_HEADER_POPUP_ID)}
            </PopupPanel>
        );
    };

    const updatePopup = (p, tableInfo) => {
        DialogRootContainer.defineDialog(FITS_HEADER_POPUP_ID, getPopup(p, tableInfo, initLeft, initTop), element);
        dispatchShowDialog(FITS_HEADER_POPUP_ID, p.plotId);
    };


    // update table sort when active image is changed
    const watchActivePlotChange = (action, cancelSelf, params) => {
        let {displayedPlotId, displayedHdu} = params;
        if (!isDialogVisible(FITS_HEADER_POPUP_ID)) {
            cancelSelf();
        } else {
            const crtPlot = primePlot(visRoot());
            const newTableInfo = createFitsHeaderTable(null, crtPlot);
            if (!newTableInfo) return {displayedPlotId, displayedHdu};

            Object.keys(newTableInfo).reduce((prev, oneBand) => {
                prev = prev || updateTableSort(newTableInfo[oneBand]);
                return prev;
            }, false);

            if (action.type===ImagePlotCntlr.PLOT_IMAGE || isThreeColor(plot) || displayedPlotId!==crtPlot.plotId || displayedHdu!==getHDU(crtPlot) ) {
                updatePopup(crtPlot, newTableInfo);
            }
            displayedPlotId= crtPlot.plotId;
            displayedHdu= getHDU(crtPlot);

        }
        return {displayedPlotId, displayedHdu};
    };

    const isFitsHeaderTable = (tbl) => tbl?.tableMeta?.content === FITSHEADERCONTENT;

    // update table sort info when there 'sort' happens, update table sort on new table
    const watchActiveTableChange = (action, cancelSelf) => {
        if (!isDialogVisible(FITS_HEADER_POPUP_ID)) {
            cancelSelf();
        } else {
            let tblModel;
            if (action.type === TABLE_SORT) {
                const {sortInfo='', tbl_id} = action.payload?.request ?? {};
                tblModel = getTblById(tbl_id);

                if (isFitsHeaderTable(tblModel)) {
                    currentSortInfo = sortInfo;          // when sort happens
                }
            } else if (action.type === TABLE_REPLACE) {  // do sorting on newly added table
                tblModel = action.payload;

                if (isFitsHeaderTable(tblModel)) {
                    updateTableSort(tblModel);
                }
              }
        }
    };


    if (!isDialogVisible(FITS_HEADER_POPUP_ID)) {
        dispatchAddActionWatcher({actions: [ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW,
                                            ImagePlotCntlr.CHANGE_PRIME_PLOT,
                                            ImagePlotCntlr.PLOT_IMAGE,
                                            ImagePlotCntlr.DELETE_PLOT_VIEW],
                                  callback:  watchActivePlotChange,
                                  id: 'fits-header-view-watch-active-plot-change'
        });
        dispatchAddActionWatcher({
            actions: [TABLE_SORT, TABLE_REPLACE],
            callback:  watchActiveTableChange,
            id: 'fits-header-view-watch-active-table-change'
        });
        updatePopup(plot, fitsHeaderInfo);
    }
}

// update table sort when table tab is changed
const onBandSelected = (fitsHeaderInfo) => {
    return (name) => {
        const tableModel = fitsHeaderInfo[name];
        if (tableModel) updateTableSort(tableModel);   // already in store, then sort it if needed
    };
};

function renderSingleBandFitsHeader(plot, fitsHeaderInfo, popupId){
    const band = plot.plotState.getBands()[0];
    return (
        <Stack sx={ popupPanelResizableSx}>
            { renderFileSizeAndPixelSize(plot, band, fitsHeaderInfo)}
            { renderTable( band,fitsHeaderInfo, false)}
            { renderCloseAndHelpButtons(popupId)}
        </Stack>
    );
}


function renderColorBandsFitsHeaders(plot, fitsHeaderInfo, popupId) {

    const bands = plot.plotState.getBands();
    return (
        <Stack sx={ popupPanelResizableSx} >
          <Stack {...{width:1, height:1, direction: 'row', resize:'none'}}>
              <Tabs {...{sx:{width:1}, onTabSelect:onBandSelected(fitsHeaderInfo) }}>
                  {renderSingleTab(plot, bands[0],fitsHeaderInfo )}
                  {renderSingleTab(plot, bands[1],fitsHeaderInfo )}
                  {bands.length===3 && renderSingleTab(plot, bands[2],fitsHeaderInfo )}
              </Tabs>
          </Stack>
            { renderCloseAndHelpButtons(popupId)}
        </Stack>
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

const renderCloseAndHelpButtons = (popupId) => (
    <Stack {...{direction:'row', justifyContent:'space-between', my:1/2, mr:1, ml:1/2, alignItems:'center'}}>
        <CompleteButton text='Close' onClick={()=>dispatchHideDialog( popupId)} dialogId={popupId} />
        <HelpIcon helpId={'tables'}/>
    </Stack> );


function renderFileSizeAndPixelSize(plot, band, fitsHeaderInfo, isOnTab) {

    const tableModel = fitsHeaderInfo[band];
    const pixelSize = getPixScaleArcSec(plot).toFixed(2) + '"';

    const  fileSizeStr = getSizeAsString(tableModel?.tableMeta?.fileSize) ?? '';

    let dimStr= `${plot.dataWidth} x ${plot.dataHeight}`;
    if (isImageCube(plot)) dimStr+= ` x ${getCubePlaneCnt(plot)}`;

   return (
        <Stack direction='row' spacing={1} pt={1}>
            <Typography {...{level:'body-sm', pl:1, textAlign: 'right'}}>Pixel Size:</Typography>
            <Typography {...{level:'body-sm', color:'warning'}}>{pixelSize}</Typography>
            <Typography {...{level:'body-sm', pl:2, textAlign: 'right'}}>File Size:</Typography>
            <Typography {...{level:'body-sm', color: 'warning'}}>{fileSizeStr}</Typography>
            <Typography {...{level:'body-sm', pl:2, textAlign: 'right'}}>Dimensions:</Typography>
            <Typography {...{level:'body-sm', color:'warning'}}>{dimStr}</Typography>
        </Stack>
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
    const myTableStyle= isPlacedOnTab?tableOnTabStyle:tableStyle;
    const tbl_ui_id = tableModel.tbl_id + '-ui';
    return (
        <div style={ myTableStyle}>
           <TablePanel
               key={tableModel.tbl_id} tbl_ui_id = {tbl_ui_id} tableModel={tableModel} height='calc(100% - 42px)'
               showToolbar={false} selectable={false} showOptionButton={true}
               allowUnits={false} showFilters={true} showTypes={false} />
        </div>
    );
}

/**
 * This function will return the popup component.  As React conversion, the CamelCase is used.
 * @param plotView
 * @param element
 * @param initLeft
 * @param initTop
 * @param onMove
 */
export function fitsHeaderView(plotView,element, initLeft, initTop, onMove) {
    const plot = primePlot(plotView);
    if (!plot)  return;
    const resultTable = createFitsHeaderTable(null, plot);
    if (!resultTable) logger.error(`fitsHeader error: ${plot.plotId}`);
    showFitsHeaderPopup(plot, resultTable, element, initLeft, initTop, onMove);
}

let tblCnt= 0;

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
            colors= bands[0]===Band.NO_BAND ? '_noBand' : bands[0].key;
            break;
        case 2:
            colors=bands[0].key + '_' + bands[1].key;
            break;
        case 3:
            colors=bands[0].key  + '_' + bands[1].key +'_'+bands[2].key ;
            break;
    }

    const str = plot.plotImageId.replace(/\s/g, '');                   //remove the white places
    const tbl_id= str.replace(/[^a-zA-Z0-9]/g, '_')  + colors+ '--' + tblCnt; //replace the no numeric/alphabet character by _
    tblCnt++;
    return  tbl_id;
}

function createRowData(header,hduStr) {
    return Object.keys(header)
        .reduce((prev, aKey) => {
            const hList= getAllValuesOfHeader(header, aKey);
            hList.forEach((h) => {
                const row= [h.idx??0, aKey, h.value??'', h.comment??''];
                if (hduStr) row.push(hduStr);
                prev.push(row);
            });
            return prev;
        }, [])
        .sort((r1, r2) => r1[0] - r2[0]);
}

/**
 * create fits header table model based on the fits header array from plot
 * @param tableId
 * @param plot
 * @returns {*}
 */
function createFitsHeaderTable(tableId, plot) {
    if (!plot) return null;
    const {headerAry,zeroHeader} = plot;
    if (!headerAry) return null;

    tableId = tableId ? tableId : createTableIdForFitsHeader(plot);
    const bands = plot.plotState.getBands();

    const columns = [
        {name: '#', type: 'int', width: 3},
        {name: 'Keyword', type: 'char', width: 10},
        {name: 'Value', type: 'char', width: 15},
        {name: 'Comments', type: 'char', width: zeroHeader? 32 : 45}
    ];
    const hduStr= 'HDU ' + getHDU(plot);
    const primaryStr= 'Primary';
    if (zeroHeader) columns.push({name: 'HDU #', type: 'char', width: 10, enumVals: `${hduStr},${primaryStr}`});

    const getHeaderData = (header) => {
        const hduRows= zeroHeader?
            [...createRowData(header,hduStr),  ...createRowData(zeroHeader,primaryStr)] : createRowData(header);
        return hduRows;
    };

    return bands.reduce((prev, oneBand) => {
            const tbl_id = oneBand === Band.NO_BAND ? tableId: `${tableId}-${oneBand.key}`;
            const tbl = getTblById(tbl_id);
            if (!tbl) {
                const data = getHeaderData(headerAry?.[oneBand.value]);

                prev[oneBand.key] = {
                    tbl_id, tableData: {columns, data},
                    totalRows: data.length, highlightedRow: 0,
                    tableMeta: {fileSize: plot?.webFitsData?.[oneBand.value]?.getFitsFileSize, content: FITSHEADERCONTENT}
                };
            } else {
                prev[oneBand.key] = tbl;
            }
        return prev;
    }, {});
}

// resort table based on current sort info.
const updateTableSort = (tbl) => {
    if (!getTblById(tbl.tbl_id)) return false;        // check if table is in store yet.
    const sortInfo_add = tbl?.request?.sortInfo ?? '';
    if (sortInfo_add !== currentSortInfo) {
        const req = {...tbl.request, sortInfo: currentSortInfo};
        dispatchTableSort(req, tbl.highlightedRow);
        return true;
    } else {
        return false;
    }
};
