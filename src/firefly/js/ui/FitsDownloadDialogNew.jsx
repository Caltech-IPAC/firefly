/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   Dec. 2015
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 */
import React from 'react';
import AppDataCntlr from '../core/AppDataCntlr.js';
import {Operation} from '../visualize/PlotState.js';
import {getRootURL} from '../util/BrowserUtil.js';
import {download} from '../util/WebUtil.js';
import InputGroup from './InputGroup.jsx';
import RadioGroupInputField from './RadioGroupInputField.jsx';
import CompleteButton from './CompleteButton.jsx';
import FieldGroup from './FieldGroup.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import PopupPanel from './PopupPanel.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import PlotViewUtil from '../visualize/PlotViewUtil.js';
import Band from '../visualize/Band.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';

function getDialogBuilder() {
	var popup= null;
	return () => {
		if (!popup) {
			const popup= (
					<PopupPanel title={'Fits Download Dialog'} >
						<FitsDownloadDialog  groupKey={'FITS_DOWNLOAD_FORM'} />
					</PopupPanel>
			);
			DialogRootContainer.defineDialog('fitsDownloadDialog', popup);
		}
		return popup;
	};
}


const fitsDialogBuilder= getDialogBuilder();

export function showFitsDownloadDialogNew() {
	fitsDialogBuilder();
	AppDataCntlr.showDialog('fitsDownloadDialog');
}


 function getInitialPlotState( ) {

	//plot and plotState are global variable for this class

	 var plot =  PlotViewUtil.getActivePlotView(visRoot()).primaryPlot;
	 var plotState =plot.plotState;

	var threeColorBandUsed=false;
	var color;
		if (plotState.isThreeColor()) {
		threeColorBandUsed==true;
		var bands = this.plotState.getBands();

		var colorID=bands[0].toString();
		color = Band.valueOf()[colorID].name();

	}



	var isCrop = plotState.hasOperation(Operation.CROP);
	var isRotation =  plotState.hasOperation(Operation.ROTATE);
	var cropNotRotate =  isCrop && !isRotation ? true: false;

	return {
		plotState,
		color,
		hasThreeColorBand:threeColorBandUsed,
		hasOperation: cropNotRotate
	};

}


class FitsDownloadDialog extends React.Component {

	 constructor(props) {
		super(props);
		FieldGroupUtils.initFieldGroup('FITS_DOWNLOAD_FORM');
		this.state = {fields: FieldGroupUtils.getGroupFields('FITS_DOWNLOAD_FORM')};


	  }


		componentWillUnmount()
		{
			if (this.unbinder) this.unbinder();
		}


		componentDidMount()
		{
			this.unbinder = FieldGroupUtils.bindToStore('FITS_DOWNLOAD_FORM', (fields) => {
				if (this.isMounted) this.setState({fields});
			});
		}

		render()
		{
			var {fields}= this.state;
			if (!fields) return false;

			return <FitsDownloadDialogForm />;
		}


}
/// Fits dialog test

function renderOperationOption(hasOperation){

	if (hasOperation) {
		return (

				<RadioGroupInputField
					initialState={{
                           tooltip: 'Please select an option',
                           label : 'FITS file:'
                           }}
					ptions={[
                            { label:'original', value:'fileTypeOrig'},
                            { label:'crop', value:'fileTypeCrop'}

                            ]}
					alignment={'vertical'}
					fieldKey='operationOption'
				/>

		);
	}

}

function renderThreeBand(hasThreeColorBand,  color) {
	if (hasThreeColorBand) {
		return (

				<RadioGroupInputField
					initialState={{
                      tooltip: 'Please select an option',
                      label : 'Color band:'
                  }}
					options={[
                           {  label: color,
                              value: 'colorOpt'
                           }
                         ]}
					fieldKey='threeBandColor'
				/>

		);
	}

}
function FitsDownloadDialogForm( ){


	const { plotState, color, hasThreeColorBand, hasOperation} = getInitialPlotState();

	this.plotState=plotState;

    var renderOperationButtons = renderOperationOption(hasOperation);
	var renderThreeBandButtons = renderThreeBand(hasThreeColorBand, color);
	return (
		<FieldGroup groupKey='FITS_DOWNLOAD_FORM'  keepState={true}>
			<div style={{padding:'5px'}}>
				<div style={{'minWidth': '300', 'minHeight': '100'} }>
					<InputGroup labelWidth={130} >
						<PopupPanel  />
					    	<RadioGroupInputField
							initialState= {{
                                    tooltip: 'Please select an option',
                                    label : 'Type of files:'
                                   }}
							options={ [
                                      {label: 'FITS File', value: 'fits'},
                                      {label: 'PNG File', value: 'png' },
                                       {label: 'Region File', value: 'reg'}
                                    ]}
							alignment={'vertical'}
							fieldKey='fileType'
					    	/>

						    <div>{renderOperationButtons}</div>
						   <div>{renderThreeBandButtons}</div>

						   <div style={{'text-align':'center'}}>
							 < CompleteButton groupKey='FITS_DOWNLOAD_FORM'
								text='Download'
								onSuccess={resultsSuccess}
								onFail={resultsFail}
								dialogId='FitsDownloadDialog'
							 />
						<br/>
						</div>
					</InputGroup>
				</div>
			</div>
		</FieldGroup>
	);

}



FitsDownloadDialogForm.propTypes= {
	fields: React.PropTypes.object.isRequired,
    plotState:React.PropTypes.object,
};


function showResults(success, request) {

	console.log(request);
	var rel={};
	var s= Object.keys(request).reduce(function(buildString,k,idx,array){
		rel[k]=request[k];
		if (idx<array.length-1)  return;

	},'');

	return rel;
}



function resultsFail(request) {
	showResults(false,request);
}

function resultsSuccess(request) {
	var rel = showResults(true,request);



	var ext;
	var bandSelect=null;
	var whichOp=null;
	for (var key in rel){
		var value=rel[key];
		if (key=='fileType'){
			ext = value;

		}
		if (key == 'threeBandColor'){
			bandSelect=value;
		}
		if (key == 'operationOption'){
			whichOp=value;
		}
	}

	var band = Band.NO_BAND;
	if (bandSelect != null) {
		band = Band[bandSelect];
	}


	var fitsFile = FitsDownloadDialogForm.plotState.getOriginalFitsFileStr(band) == 'undefined' || whichOp == null ?
			FitsDownloadDialogForm.plotState.getWorkingFitsFileStr(band) :
			FitsDownloadDialogForm.plotState.getOriginalFitsFileStr(band);

	if (ext.toLowerCase() =='fits') {

		download(getRootURL() + '/servlet/Download?file='+ fitsFile);
	}

}

