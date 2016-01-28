

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   1/20/16
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 */
import React from 'react';
import AppDataCntlr from '../../core/AppDataCntlr.js';
import InputGroup from '../../ui/InputGroup.jsx';
import RadioGroupInputField from '../../ui/RadioGroupInputField.jsx';
import FieldGroup from '../../ui/FieldGroup.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import PopupPanel from '../../ui/PopupPanel.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import InputFieldLabel from '../../ui/InputFieldLabel.jsx';

function getDialogBuilder(fieldKey) {


	var popup = renderOptionDialog(fieldKey);
	return popup;

}

export function showMouseReadoutOptionDialog(fieldKey) {

	getDialogBuilder(fieldKey);
	AppDataCntlr.showDialog(fieldKey);
}

function renderOptionDialog(fieldKey) {
	var groupKey;

	var defaultSelectedField;
	switch (fieldKey) {
		case 'coordinateSys' ||  'imagePixel':
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

export function showSelectedField(groupKey, fieldKey){
	console.log('closing ' + groupKey);
	
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
			form=  <PixelSizeOptionDialogForm  groupKey={this.props.groupKey} fieldKey={this.props.fieldKey}/>;
		}
		else {
			form= <CoordinateOptionDialogForm  groupKey={this.props.groupKey} fieldKey={this.props.fieldKey}/>;
		}
		return form;


	}


}


function CoordinateOptionDialogForm(groupKey, fieldKey) {


	var leftColumn = { display: 'inline-block', paddingLeft:125, verticalAlign:'middle', paddingBottom:75};

	var rightColumn = {display: 'inline-block',  paddingLeft:18};

	var dialogStyle = { minWidth : 300, minHeight: 100 , padding:5};
	var coordinateSysInitialValue='eqj2000Dhms';
	var pixelImageInitialValue='fitsIP';
	if (fieldKey==='coordinateSys'){
		coordinateSysInitialValue=true;
	}
	else {
		pixelImageInitialValue=true;
	}

	return (

		<FieldGroup groupKey={groupKey} keepState={true}>
			<div style={ dialogStyle}  onClick={ (groupKey, fieldKey) => showSelectedField(groupKey, fieldKey) }>
					<div style={leftColumn} title='Please select an option'> Options</div>
					<div style={rightColumn}>
							  <RadioGroupInputField
								initialState={{
                                    tooltip: 'Please select an option'
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

			</div>

		</FieldGroup>

	);

}


function PixelSizeOptionDialogForm(groupKey, fieldKey) {


	var leftColumn = { display: 'inline-block', paddingLeft:125, verticalAlign:'middle', paddingBottom:15};

	var rightColumn = {display: 'inline-block',  paddingLeft:18};

	var dialogStyle = { minWidth : 300, minHeight: 100 , padding:5};
	var coordinateSysInitialValue='eqj2000Dhms';
	var pixelImageInitialValue='fitsIP';
	if (fieldKey==='coordinateSys'){
		coordinateSysInitialValue=true;
	}
	else {
		pixelImageInitialValue=true;
	}

	return (
		<FieldGroup groupKey={groupKey} keepState={true}>
			<div style={ dialogStyle} onClick={ (groupKey, fieldKey) => showSelectedField(groupKey, fieldKey) }>
				<div style={leftColumn} title='Please select an option'> Options</div>
				<div style={rightColumn}>
					<RadioGroupInputField
						initialState={{
                                    tooltip: 'Please select an option'
                                    //move the label as a InputFieldLabel
                                   }}
						options={ [
                                      {label: 'Pixel Size', value: 'pixelSize'},
                                      {label: 'Screen Pixel Size', value: 'sPixelSize' },

                                    ]}
						alignment={'vertical'}
						fieldKey={fieldKey}
					/>
				</div>

			</div>

		</FieldGroup>
	);

}
