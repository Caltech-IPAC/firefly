

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

function getDialogBuilder(fieldKey) {


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
			<MouseReadoutOptionDialog groupKey={groupKey} fieldKey={fieldKey}/>
		</PopupPanel>

	);
	DialogRootContainer.defineDialog(fieldKey, popup);

	return popup;


}

export function showMouseReadoutOptionDialog(fieldKey) {

	getDialogBuilder(fieldKey);
	AppDataCntlr.showDialog(fieldKey);
}

function getCoordinateMap(coordinateStr){
	var coord;
	var type;
	switch (coordinateStr) {
		case 'EQ J2000 HMS':
			coord = CoordinateSys.EQ_J2000;
			type = 'hhmmss';
			break;
		case 'EQ J2000 decimal':
			coord = CoordinateSys.EQ_J2000;
			type = 'decimal';
			break;
		case'Galactic':
			coord = CoordinateSys.GALACTIC;
			type = null;
			break;
		case 'EQ B1950':
			coord = CoordinateSys.EQ_B1950;
			type = null;

		case 'Fits Image Pixel':
			coord = CoordinateSys.PIXEL;
			break;
	}
	return {coord, type};
}
function showSelectedField(request, groupKey, fieldKey){
	console.log('closing ' + groupKey);

	if (request.hasOwnProperty('target')){
		var target=request.target;
		var coordinate=getCoordinateMap(target).coord;
		console.log(target);
		var result=target.value;
		console.log(result);
		//TODO dispatch action here
		switch (fieldKey){
			case 'readout1':

				dispatchChangeMouseReadoutReadout1(coordinate);

				break;

		}

	}

	AppDataCntlr.hideDialog(fieldKey);
	return result;
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
			form=  <PixelSizeOptionDialogForm fields={fields}  groupKey={this.props.groupKey} fieldKey={this.props.fieldKey}/>;
		}
		else {
			form= <CoordinateOptionDialogForm fields={fields} groupKey={this.props.groupKey} fieldKey={this.props.fieldKey}/>;
		}
		return form;


	}


}

function CoordinateOptionDialogForm({fields, groupKey,fieldKey}) {


	var leftColumn = { display: 'inline-block', paddingLeft:125, verticalAlign:'middle', paddingBottom:75};

	var rightColumn = {display: 'inline-block',  paddingLeft:18};

	var dialogStyle = { minWidth : 300, minHeight: 100 , padding:5};


	return (

		<FieldGroup groupKey={groupKey} keepState={true}>
			<div style={ dialogStyle} onClick={ (request) => showSelectedField(request, groupKey, fieldKey) } >
					<div style={leftColumn} title='Please select an option'> Options</div>
				{renderReadout1RadioGroup(rightColumn,fieldKey )}
			</div>

		</FieldGroup>

	);

}

function renderReadout1RadioGroup(rightColumn,fieldKey ){
	return(
	<div style={rightColumn} >
		<RadioGroupInputField
			initialState={{
                                    tooltip: 'Please select an option',
                                    value:'fitsIP',
                                    //value: (fields && fields.fieldKey) ? fields.fieldKey.value:'fitsIP'
                                    //move the label as a InputFieldLabel
                                   }}
			options={ [
                                      {label: 'EQ J2000 HMS', value: 'eqj2000Dhms'},
                                      {label: 'EQ J2000 decimal', value: 'eqj2000DCM' },
                                      {label: 'Galactic', value: 'galactic'},
                                      {label: 'EQ B1950', value: 'eqb1950'},
                                      {label: 'Fits Image Pixel', value: 'fitsIP'}
                                    ]}
			alignment={'vertical'}
			fieldKey={fieldKey}
		/>
	</div>
	);
}
CoordinateOptionDialogForm.propTypes= {
	fields: PropTypes.object.isRequired,
	groupKey:React.PropTypes.string.isRequired,
	filedKey:React.PropTypes.string
};

function PixelSizeOptionDialogForm( {groupKey,fieldKey} ) {


	var leftColumn = { display: 'inline-block', paddingLeft:125, verticalAlign:'middle', paddingBottom:15};

	var rightColumn = {display: 'inline-block',  paddingLeft:18};

	var dialogStyle = { minWidth : 300, minHeight: 100 , padding:5};

	return (
		<FieldGroup groupKey={groupKey} keepState={true}>
			<div style={ dialogStyle} onClick={ (request) => showSelectedField(request, groupKey, fieldKey) }>
				<div style={leftColumn} title='Please select an option'> Options</div>
				<div style={rightColumn}>
					<RadioGroupInputField
						initialState={{
                                    tooltip: 'Please select an option'
                                    //move the label as a InputFieldLabel
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
PixelSizeOptionDialogForm.propTypes= {
	groupKey:React.PropTypes.string.isRequired,
	filedKey:React.PropTypes.string
};
