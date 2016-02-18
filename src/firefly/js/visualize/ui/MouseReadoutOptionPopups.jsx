
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   1/20/16
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 */

import React, {Component, PropTypes} from 'react';
import AppDataCntlr from '../../core/AppDataCntlr.js';
import InputGroup from '../../ui/InputGroup.jsx';
import RadioGroupInputField from '../../ui/RadioGroupInputField.jsx';
import FieldGroup from '../../ui/FieldGroup.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import PopupPanel from '../../ui/PopupPanel.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import InputFieldLabel from '../../ui/InputFieldLabel.jsx';
import CoordinateSys from '../CoordSys.js';
import {dispatchChangeMouseReadoutReadout1, dispatchChangeMouseReadoutReadout2, dispatchChangeMouseReadoutPixel} from '../ImagePlotCntlr.js';

//define the labels and values for the radio options
const coordOptions= [
	{label: 'EQ J2000 HMS', value: 'eqj2000hms'},
	{label: 'EQ J2000 decimal', value: 'eqj2000DCM' },
	{label: 'Galactic', value: 'galactic'},
	{label: 'EQ B1950', value: 'eqb1950'},
	{label: 'Fits Image Pixel', value: 'fitsIP'}
];
const pixelOptions = [

	{label: 'Pixel Size', value: 'pixelSize'},
	{label: 'Screen Pixel Size', value: 'sPixelSize' }
];

/**
 *
 * @param fieldKey - string: a key for the field group
 * @param radioValue: string: a option value of the radio group
 * @returns {XML}
 */
function getDialogBuilder(fieldKey, radioValue) {


	//name a groupKey based on the input fieldKey
	var groupKey;
	switch (fieldKey) {
		case 'readout1':
		case 'readout2':
			groupKey = 'COORDINATE_OPTION_FORM';
			break;
		case  'pixelSize':
			groupKey = 'PIXEL_OPTION_FORM';
			break;
	}


	var popup = (

		<PopupPanel title={'Choose Option'}  >
			<MouseReadoutOptionDialog groupKey={groupKey} fieldKey={fieldKey} radioValue={radioValue}/>
		</PopupPanel>

	);
	DialogRootContainer.defineDialog(fieldKey, popup);

	return popup;


}

export function showMouseReadoutOptionDialog(fieldKey,radioValue) {

	getDialogBuilder(fieldKey, radioValue);
	AppDataCntlr.showDialog(fieldKey);
}

/**
 *
 * This method map the value in coordinate option popup to its value
 * @param coordinateRadioValue : the value in the radio button
 * @returns {{coordinate: *, type: *}}
 */
export function getCoordinateMap(coordinateRadioValue){
	var coordinate;
	var type;
	switch (coordinateRadioValue) {
		case 'eqj2000hms':
			coordinate = CoordinateSys.EQ_J2000;
			type = 'hms';
			break;
		case 'eqj2000DCM':
			coordinate = CoordinateSys.EQ_J2000;
			type = 'decimal';
			break;
		case'galactic':
			coordinate = CoordinateSys.GALACTIC;
			type = null;
			break;
		case 'eqb1950':
			coordinate = CoordinateSys.EQ_B1950;
			type = null;
			break;

		case 'pixelSize':
			coordinate = CoordinateSys.PIXEL;
			break;
		case 'sPixelSize':
			coordinate = CoordinateSys.SCREEN_PIXEL;
			break;
		default:
			coordinate=CoordinateSys.UNDEFINED;
			break;



	}
	return {coordinate, type};
}
/**
 * this method dispatcher the action to the store.
 * @param request
 * @param groupKey
 * @param fieldKey
 */
function doDispatch( request,  fieldKey){

	if (request.hasOwnProperty('target')){
		var target=request.target;
		var newRadioValue=target.value;
		switch (fieldKey){
			case 'readout1':
				
               // console.log('dispatch readout1 '+ newRadioValue);
				dispatchChangeMouseReadoutReadout1(	newRadioValue);
				break;
			case 'readout2':
				//console.log('dispatch readout2 '+ newRadioValue);
				dispatchChangeMouseReadoutReadout2(	newRadioValue);
				break;
			case 'pixelSize':
				//console.log('dispatch pixelSize '+ newRadioValue);
				 dispatchChangeMouseReadoutPixel(newRadioValue);
				break;
		}

	}

	AppDataCntlr.hideDialog(fieldKey);

}
/**
 *  create a popup dialog
 */
class MouseReadoutOptionDialog extends React.Component {


	constructor(props) {
		super(props);
		FieldGroupUtils.initFieldGroup(props.groupKey);
		this.state = {fields: FieldGroupUtils.getGroupFields(props.groupKey)};

	}


	componentWillUnmount() {

		if (this.unbinder) this.unbinder();
	}


	componentDidMount() {

		this.unbinder = FieldGroupUtils.bindToStore(this.props.groupKey, (fields) => {
			this.setState({fields});
		});
	}


	render() {

		var {fields}= this.state;
		if (!fields) return false;
		var form;

		if (this.props.groupKey==='PIXEL_OPTION_FORM'){
			form=  <PixelSizeOptionDialogForm
				    groupKey={this.props.groupKey}
					fieldKey={this.props.fieldKey}
					radioValue={this.props.radioValue}
			/>;
		}
		else {
			form= <CoordinateOptionDialogForm
				groupKey={this.props.groupKey}
				fieldKey={this.props.fieldKey}
				radioValue={this.props.radioValue}
			/>;
		}
		return form;


	}


}

// ------------ React component
function CoordinateOptionDialogForm({ groupKey,fieldKey,radioValue}) {


	var leftColumn = { display: 'inline-block', paddingLeft:80, verticalAlign:'middle', paddingBottom:75};

	var rightColumn = {display: 'inline-block',  paddingLeft:18};

	var dialogStyle = { minWidth : 300, minHeight: 100 , padding:5};


	//var radioGroup = fieldKey==='readout1'?renderReadout1RadioGroup(rightColumn,fieldKey,radioValue ):
	//	renderReadout2RadioGroup(rightColumn,fieldKey,radioValue);

	return (

		<FieldGroup groupKey={groupKey} keepState={true}>
			<div style={ dialogStyle} onClick={ (request) => doDispatch(request, fieldKey) } >
					<div style={leftColumn} title='Please select an option'> Options</div>
			     	{renderCoordinateRadioGroup(rightColumn,fieldKey,radioValue)}
			</div>

		</FieldGroup>

	);

}
/**
 * property of the CoordinateOptionDialogForm
 * @type {{groupKey: *, filedKey: *, radioValue: *}}
 */
CoordinateOptionDialogForm.propTypes= {
	groupKey:React.PropTypes.string.isRequired,
	filedKey:React.PropTypes.string,
	radioValue:React.PropTypes.string.isRequired
};

/*
function renderReadout1RadioGroup(rightColumn,fieldKey, radioValue ){
	return(
		<div style={rightColumn} >
			<RadioGroupInputField
				initialState={{
                                    tooltip: 'Please select an option',
                                    value: radioValue,
                                     }}
				options={ coordOptions }
				alignment={'vertical'}
				fieldKey={fieldKey}
			/>
		</div>
	);
}
*/

function renderCoordinateRadioGroup(rightColumn,fieldKey, radioValue ){
	return(
		<div style={rightColumn} >
			<RadioGroupInputField
				initialState={{
                                    tooltip: 'Please select an option',
                                    value:radioValue,
                                    }}
				options={coordOptions}
				alignment={'vertical'}
				fieldKey={fieldKey}
			/>
		</div>
	);
}


// ------------ React component
function PixelSizeOptionDialogForm( {groupKey,fieldKey, radioValue} ) {


	var leftColumn = { display: 'inline-block', paddingLeft:50, verticalAlign:'middle', paddingBottom:15};

	var rightColumn = {display: 'inline-block',  paddingLeft:18};

	var dialogStyle = { minWidth : 300, minHeight:54 , padding:5};

	return (
		<FieldGroup groupKey={groupKey} keepState={true}>
			<div style={ dialogStyle} onClick={ (request) => doDispatch(request, fieldKey) }>
				<div style={leftColumn} title='Please select an option'> Options</div>
				<div style={rightColumn}>
					<RadioGroupInputField
						initialState={{
                                    tooltip: 'Please select an option',
                                    //move the label as a InputFieldLabel
                                     value:radioValue,
                                   }}
						options={ pixelOptions }
						alignment={'vertical'}
						fieldKey={fieldKey}
					/>
				</div>

			</div>

		</FieldGroup>
	);

}

PixelSizeOptionDialogForm.propTypes= {
	groupKey:React.PropTypes.string.isRequired,
	radioValue:React.PropTypes.string.isRequired,
	filedKey:React.PropTypes.string
};

