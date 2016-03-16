/*
 L. Zhang Initial version 3/16/16
 DM-4494:FITS Visualizer porting: Show FITS Header
 */

import React, {PropTypes} from 'react';
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
import {isEmpty, get, cloneDeep, omitBy, isUndefined} from 'lodash';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {SortInfo} from '../../tables/SortInfo';
import {RemoteTableStore, TableStore} from '../../tables//TableStore.js';

import Band from '../Band.js';

const popupIdRoot= 'fitsHeader';

const popupPanelResizableStyle = {
	width: 480,
	minWidth: 450,
	height: 400,
	minHeight: 300,
	resize: 'vertical',
	overflow: 'auto',
	position: 'relative'
};

const popupPanelFixedSizeStyle = {
    width: 510,
    minWidth: 480,
    height: 480,
    minHeight: 480,
    resize: 'vertical',
	//overflow: 'auto',
    position: 'relative'
};
const tabStyle={width: '100%', height: '85%'};//, overflow:'auto'
const tableStyle={width: '100%', height: '100%',  flexGrow: 1, display: 'flex'};

const titleStyle = {width: '100%', height: 20, whiteSpace: 'nowrap', display:'inline-block'};
const closeButtonStyle = {'textAlign':'center', display:'inline-block', marginTop:30, width: '80%'};
const helpIdStyle = {'textAlign':'center', display:'inline-block', marginTop:30,marginRight:10};

const mTableStyle={width: '100%', height: 370, flexGrow: 1, display: 'flex'};


function popupForm (plot, fitsHeaderInfo,  popupId ){

	var stats =  (fitsHeaderInfo && fitsHeaderInfo.NO_BAND) ?
		renderNOBandFitsHeader(plot, fitsHeaderInfo)://renderSingleBandFitsHeader(plot, Band.NO_BAND, fitsHeaderInfo):
		render3BandFitsHeaders(plot, fitsHeaderInfo);
    //the tab panel can not have the table grow with size, thus use a fixed size panel for now
    var panelStyle = (fitsHeaderInfo && fitsHeaderInfo.NO_BAND) ?popupPanelResizableStyle
        :popupPanelFixedSizeStyle;
	return (
		<div  style={panelStyle}>
			 { stats}
			<div>
			    <div style={closeButtonStyle}>
                    < CompleteButton
                        text='close'
                        onClick= {()=>dispatchHideDialog( popupId)}
                        dialogId={popupId}
                    />

                    {/* <input type='button'  value='close' onClick= {()=>dispatchHideDialog( popupId)} />*/}
			   </div>
			   <div style={helpIdStyle}>
				  <HelpIcon helpid={'visualization.fitsDownloadOptions'} />
			   </div>
            </div>
		</div>
	);

};
function showFitsHeaderPopup(plot, tableId, fitsHeaderInfo) {

	var popupId=popupIdRoot+'_'+tableId;

	const popTitle = 'FITS Header : '+plot.title;
	var popup = (<PopupPanel title={popTitle}>
			{popupForm(plot, fitsHeaderInfo, popupId)}
		</PopupPanel>

	);

	DialogRootContainer.defineDialog(popupId, popup);
	dispatchShowDialog(popupId);
}

function render3BandFitsHeaders(plot, fitsHeaderInfo) {

	return (
		<div  >

				<Tabs   defaultSelected={0}>
					<Tab  name='RED'>
						<div style= {tabStyle}>
						    {getTableInfo(plot,Band.RED, fitsHeaderInfo )}
							<div style={mTableStyle}>
					        	{getTable(Band.RED, fitsHeaderInfo )}
							</div>
						</div>

					</Tab>

					<Tab name= 'GREEN' >
						<div style= {tabStyle}>
							{getTableInfo(plot,Band.GREEN, fitsHeaderInfo )}
							<div style={mTableStyle}>
								{getTable(Band.GREEN, fitsHeaderInfo )}
							</div>
						</div>
					</Tab>
					<Tab name='BLUE'>
						<div style= {tabStyle}>
							{getTableInfo(plot,Band.BLUE, fitsHeaderInfo )}
							<div style={mTableStyle}>
								{getTable(Band.BLUE, fitsHeaderInfo )}
							</div>
						</div>
					</Tab>


			</Tabs>

		</div>


	);

}

function getTableInfo(plot,band, fitsHeaderInfo ){

	const  tableModel = JSON.parse(fitsHeaderInfo[band]);
	const  pt = plot.projection.getPixelScaleArcSec();
	const  pixelSize = pt.toFixed(2)+'"';
	const  meta = tableModel.tableMeta;
	const  fileSize = getSizeAsString(meta.fileSize);
	return (
	<div style={titleStyle}>  <div style= { {display:'inline-block', width: 100} }>Pixel Size: {pixelSize} </div>
		<div  style= { {display:'inline-block', width: 100} }>  File Size: {fileSize}</div>
	</div>
	);
}


function getTable(band, fitsHeaderInfo ){
	const  tableModel = JSON.parse(fitsHeaderInfo[band]);

	const tableData =tableModel.tableData;

	const  data = tableData.data;
	const  columns = tableData.columns;
	const  meta = tableModel.tableMeta;
	var columnNames=[];
	for (var i=0; i<columns.length; i++){
		columnNames[i]=columns[i].name;
	}
	const sortInfo = SortInfo.newInstance('', columnNames).serialize();
	var request={};
	request['sortInfo']=sortInfo;
	meta['request']=request;
	var newTableModel = Object.assign({}, tableModel, {request:request });

	const tableStore =  TableStore.newInstance(newTableModel);
	return (
		<BasicTable
			columns={columns}
			data={data}
			height='calc(100% - 42px)'
			sortInfo={sortInfo}
			tableStore={tableStore}
		/>
	);
}
function renderNOBandFitsHeader(plot, fitsHeaderInfo){

	return (

		  <div style = {tabStyle}>
			  {getTableInfo(plot,Band.NO_BAND, fitsHeaderInfo )}
			 <div  style={tableStyle}>
				 {getTable(Band.NO_BAND, fitsHeaderInfo )}
			 </div>
		  </div>

	);

}

/*function renderSingleBandFitsHeader(plot, band,fitsHeaderInfo){


	const  tableModel = JSON.parse(fitsHeaderInfo[band]);

	const tableData =tableModel.tableData;

	const  data = tableData.data;
	const  columns = tableData.columns;
	const  pt = plot.projection.getPixelScaleArcSec();
	const  pixelSize = pt.toFixed(2)+'"';
	const  meta = tableModel.tableMeta;
	const  fileSize = getSizeAsString(meta.fileSize);
	//const sortInfo = get(tableModel, 'request.sortInfo');
	var columnNames=[];
	for (var i=0; i<columns.length; i++){
		columnNames[i]=columns[i].name;
	}
	const sortInfo = SortInfo.newInstance('', columnNames).serialize();
	var request={};
	request['sortInfo']=sortInfo;
	meta['request']=request;
	var newTableModel = Object.assign({}, tableModel, {request:request });

	const tableStore =  TableStore.newInstance(newTableModel);
	return (

		<div style = {tabStyle}>
			<div style={titleStyle}>  <div style= { {display:'inline-block', width: 100} }>Pixel Size: {pixelSize} </div>
				<div  style= { {display:'inline-block', width: 100} }>  File Size: {fileSize}</div>
			</div>
			<div  style={tableStyle}>
				<BasicTable
					columns={columns}
					data={data}
					height='calc(100% - 42px)'
					sortInfo={sortInfo}
					tableStore={tableStore}
				/>
			</div>
		</div>

	);

}*/

/**
 * This function calls the server function to calculate the FitsHeader information
 * @param plot
 * @param tableId
 */

function showFitsHeaderData(plot, tableId){


	callGetFitsHeaderInfo(plot.plotState, tableId)
		.then( (result) => {

			showFitsHeaderPopup(plot, tableId, result);
		})
		.catch ( (e) => {
			logError(`fitsHeader error: ${plot.plotId}`, e);

		});
}

/**
 *
 *  This function will return the popup component.  As React conversion, the CamelCase is used.
 * @param plotView
 * @constructor
 */
export function  fitsHeaderView(plotView){

	var plot= primePlot(plotView);
	if  (!plot)  return;

	var str=plot.title.replace(/\s/g, '');
	var tableId= str.replace(/[^a-zA-Z0-9]/g, '_');

	showFitsHeaderData(plot, tableId);


}
