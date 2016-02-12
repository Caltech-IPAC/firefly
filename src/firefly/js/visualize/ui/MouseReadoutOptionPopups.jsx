

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

const coordOptions= [
	{label: 'EQ J2000 HMS', value: 'eqj2000Dhms'},
	{label: 'EQ J2000 decimal', value: 'eqj2000DCM' },
	{label: 'Galactic', value: 'galactic'},
	{label: 'EQ B1950', value: 'eqb1950'},
	{label: 'Fits Image Pixel', value: 'fitsIP'}
];
function getDialogBuilder(fieldKey, radioValue) {


	var groupKey;
	switch (fieldKey) {
		case 'readout1' ||  'readout2':
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

function valueToLabel(radioValue){
	for (var i=0; i<coordOptions.length; i++){
		if (coordOptions[i].value===radioValue){
			return coordOptions[i].label;
		}

	}
}
/**
 *
 * This method map the value in coordinate option popup to its label
 * @param coordinateRadioValue
 * @returns {{coordinate: *, type: *}}
 */
export function getCoordinateMap(coordinateRadioValue){
	var coordinate;
	var type;
	var coordinateLabel = valueToLabel(coordinateRadioValue);
	switch (coordinateLabel) {
		case 'EQ J2000 HMS':
			coordinate = CoordinateSys.EQ_J2000;
			type = 'hms';
			break;
		case 'EQ J2000 decimal':
			coordinate = CoordinateSys.EQ_J2000;
			type = 'decimal';
			break;
		case'Galactic':
			coordinate = CoordinateSys.GALACTIC;
			type = null;
			break;
		case 'EQ B1950':
			coordinate = CoordinateSys.EQ_B1950;
			type = null;

		case 'Fits Image Pixel':
			coordinate = CoordinateSys.PIXEL;
			break;

	}
	return {coordinate, type};
}
function doDispatch( request, groupKey, fieldKey){
	console.log('closing ' + groupKey);

	if (request.hasOwnProperty('target')){
		var target=request.target;

		console.log(target);
		var newRadioValue=target.value;

		console.log(newRadioValue);

		switch (fieldKey){
			case 'readout1':

				dispatchChangeMouseReadoutReadout1(	newRadioValue);
				break;
			case 'readout2':
				dispatchChangeMouseReadoutReadout2(	newRadioValue);
				break;
			case 'pixelSize':
				 dispatchChangeMouseReadoutPixel(newRadioValue);
				break;
		}

	}

	AppDataCntlr.hideDialog(fieldKey);

}

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
			     	fields={fields}
				    groupKey={this.props.groupKey}
					fieldKey={this.props.fieldKey}
					radioValue={this.props.radioValue}
			/>;
		}
		else {
			form= <CoordinateOptionDialogForm
				fields={fields}
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


	this.radioValue = radioValue;

	var radioGroup = fieldKey==='readout1'?renderReadout1RadioGroup(rightColumn,fieldKey,radioValue ):
		renderReadout2RadioGroup(rightColumn,fieldKey,radioValue);

	return (

		<FieldGroup groupKey={groupKey} keepState={true}>
			<div style={ dialogStyle} onClick={ (request) => doDispatch(request, groupKey, fieldKey) } >
					<div style={leftColumn} title='Please select an option'> Options</div>
				{radioGroup}
			</div>

		</FieldGroup>

	);

}
/*CoordinateOptionDialogForm.propTypes= {
   // visRoot : React.PropTypes.object.isRequired,
	groupKey:React.PropTypes.string.isRequired,
	filedKey:React.PropTypes.string,
	radioValue:React.PropTypes.string.isRequired
};*/

function renderReadout1RadioGroup(rightColumn,fieldKey, radioValue ){
	return(
		<div style={rightColumn} >
			<RadioGroupInputField
				initialState={{
                                    tooltip: 'Please select an option',
                                    value: radioValue,
                                    //value: (fields && fields.fieldKey) ? fields.fieldKey.value:'fitsIP'
                                    //move the label as a InputFieldLabel
                                   }}
				options={ coordOptions }
				alignment={'vertical'}

				fieldKey={fieldKey}
			/>
		</div>
	);
}

function renderReadout2RadioGroup(rightColumn,fieldKey, radioValue ){
	return(
		<div style={rightColumn} >
			<RadioGroupInputField
				initialState={{
                                    tooltip: 'Please select an option',
                                    value:radioValue,
                                    //value: (fields && fields.fieldKey) ? fields.fieldKey.value:'fitsIP'
                                    //move the label as a InputFieldLabel
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


	var leftColumn = { display: 'inline-block', paddingLeft:80, verticalAlign:'middle', paddingBottom:15};

	var rightColumn = {display: 'inline-block',  paddingLeft:18};

	var dialogStyle = { minWidth : 300, minHeight: 100 , padding:5};

	return (
		<FieldGroup groupKey={groupKey} keepState={true}>
			<div style={ dialogStyle} onClick={ (request) => doDispatch(request, groupKey, fieldKey) }>
				<div style={leftColumn} title='Please select an option'> Options</div>
				<div style={rightColumn}>
					<RadioGroupInputField
						initialState={{
                                    tooltip: 'Please select an option',
                                    //move the label as a InputFieldLabel
                                     value:radioValue,
                                   }}
						options={ [
                                      {label: 'Pixel Size', value: 'pixelSize'},
                                      {label: 'Screen Pixel Size', value: 'sPixelSize' }

                                    ]}
						alignment={'vertical'}
						fieldKey={fieldKey}
					/>
				</div>

			</div>

		</FieldGroup>
	);

}
/*
PixelSizeOptionDialogForm.propTypes= {
	groupKey:React.PropTypes.string.isRequired,
	radioValue:React.PropTypes.string.isRequired,
	filedKey:React.PropTypes.string
};
*/
