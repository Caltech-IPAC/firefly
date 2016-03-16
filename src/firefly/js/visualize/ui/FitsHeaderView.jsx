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

import HelpIcon from '../../ui/HelpIcon.jsx';

import Band from '../Band.js';

const popupIdRoot= 'fitsHeader';

const popupPanelStyle = {
	width: 450,
	minWidth: 450,
	height: 380,
	minHeight: 300,
	resize: 'both',
	overflow: 'auto',
	position: 'relative'
};

const tabStyle={width: '100%', height: '90%', overflow:'auto'};
const tableStyle={width: '100%', height: '100%', overflow:'auto', flexGrow: 1, display: 'flex'};

const titleStyle = {width: '100%', height: 20, whiteSpace: 'nowrap', display:'inline-block'};
const closeButtonStyle = {'textAlign':'center', display:'inline-block', marginTop:10, width: '80%'};
const helpIdStyle = {'textAlign':'center', display:'inline-block', marginTop:10, width: '20%'};
const mTableStyle={width: '100%', height: 380, flexGrow: 1, display: 'flex'};


const popupForm  =  (plot, fitsHeaderInfo,  popupId )=> {

	var stats =  fitsHeaderInfo.hasOwnProperty('NO_BAND') ?
		renderSingleBandFitsHeader(plot, Band.NO_BAND, fitsHeaderInfo):
		render3BandFitsHeaders(plot, fitsHeaderInfo);
	return (
		<div  style={popupPanelStyle}>
			 { stats}
			<div>
			    <div style={closeButtonStyle}>
			        <input type='button'  value='close' onClick= {()=>dispatchHideDialog( popupId)} />
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

	const popTitle = 'FITS Header:'+plot.title;
	var popup = (<PopupPanel title={popTitle}>
			{popupForm(plot, fitsHeaderInfo, popupId)}
		</PopupPanel>

	);

	DialogRootContainer.defineDialog(popupId, popup);
	dispatchShowDialog(popupId);
}

function render3BandFitsHeaders(plot, fitsHeaderInfo) {

	return (
		<div style = {tabStyle} >

				<Tabs   defaultSelected={0}>
					<Tab  name='RED'>
						<div style = { mTableStyle} >
                            {renderSingleBandFitsHeader(plot, Band.RED, fitsHeaderInfo)}
                       </div>
					</Tab>

					<Tab name= 'GREEN' >
						<div style = { mTableStyle} >
						    {renderSingleBandFitsHeader(plot, Band.GREEN, fitsHeaderInfo)}
						</div>
					</Tab>
					<Tab name='BLUE'>
						<div style = { mTableStyle} >
						    {renderSingleBandFitsHeader(plot, Band.BLUE, fitsHeaderInfo)}
						</div>
					</Tab>


			</Tabs>

		</div>


	);

}

function renderSingleBandFitsHeader(plot, band,fitsHeaderInfo){


	const  tableModel = JSON.parse(fitsHeaderInfo[band]);

	const tableData =tableModel.tableData;

	const  data = tableData.data;
	const  columns = tableData.columns;
	const  pt = plot.projection.getPixelScaleArcSec();
	const  pixelSize = pt.toFixed(2)+'"';
	const  meta = tableModel.tableMeta;
	const  fileSize = Math.round(meta.fileSize/1000.0) +'K';

   return (

		  <div style = {tabStyle}>
			 <div style={titleStyle}>  <div style= { {display:'inline-block', width: 100} }>PixelSize:{pixelSize} </div>
				 <div  style= { {display:'inline-block', width: 100} }>  FileSize:{fileSize}</div>
			 </div>
			 <div  style={tableStyle}>
			    <BasicTable
				columns={columns}
				data={data}
				height='calc(100% - 42px)'
		     	/>
			 </div>
		  </div>

	);

}

/**
 * This function calls the server function to calculate the FitsHeader information
 * @param plot
 * @param tableId
 */

function showFitsHeaderData(plot, tableId){


	callGetFitsHeaderInfo(plot.plotState, tableId)
		.then( (result) => {

			console.log(result);

			showFitsHeaderPopup(plot, tableId, result);
		})
		.catch ( (e) => {
			console.log(`fitsHeader error: ${plot.plotId}`, e);

		});
}

/**
 *
 *  This function will return the popup component.  As React conversion, the CamelCase is used.
 * @param plotView
 * @constructor
 */
export function  FitsHeaderView(plotView){

	var plot= primePlot(plotView);
	if  (!plot)  return;

	var str=plot.title.replace(/\s/g, '');
	var tableId= str.replace(/[^a-zA-Z0-9]/g, '_');

	showFitsHeaderData(plot, tableId);


}
FitsHeaderView.propTypes= {
	plotView: PropTypes.object.isRequired
};
