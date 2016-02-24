
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   1/20/16
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 */

import React, { PropTypes} from 'react';
import AppDataCntlr from '../../core/AppDataCntlr.js';
import InputGroup from '../../ui/InputGroup.jsx';
import RadioGroupInputField from '../../ui/RadioGroupInputField.jsx';
import FieldGroup from '../../ui/FieldGroup.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import PopupPanel from '../../ui/PopupPanel.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import InputFieldLabel from '../../ui/InputFieldLabel.jsx';
import CoordinateSys from '../CoordSys.js';
import {dispatchChangeMouseReadout} from '../ImagePlotCntlr.js';

//define the labels and values for the radio options
const coordOptions= [
	{label: 'EQ J2000 HMS', value: 'eqj2000hms'},
	{label: 'EQ J2000 decimal', value: 'eqj2000DCM' },
	{label: 'Galactic', value: 'galactic'},
	{label: 'EQ B1950', value: 'eqb1950'},
	{label: 'FITS Image Pixel', value: 'fitsIP'}
];
const pixelOptions = [

	{label: 'Pixel Size', value: 'pixelSize'},
	{label: 'Screen Pixel Size', value: 'sPixelSize' }
];

const groupKeys={
	mouseReadout1:'COORDINATE_OPTION_FORM',
	mouseReadout2:'COORDINATE_OPTION_FORM',
	pixelSize: 'PIXEL_OPTION_FORM'
};

const leftColumn = { display: 'inline-block', paddingLeft:80, verticalAlign:'middle', paddingBottom:75};

const rightColumn = {display: 'inline-block',  paddingLeft:18};

const dialogStyle = { minWidth : 300, minHeight: 100 , padding:5};


/**
 *
 * @param fieldKey - string: a key for the field group
 * @param radioValue: string: a option value of the radio group
 * @returns {XML}
 */
function getDialogBuilder(fieldKey, radioValue) {


	//name a groupKey based on the input fieldKey
	var groupKey = groupKeys[fieldKey];

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
 * this method dispatcher the action to the store.
 * @param request
 * @param groupKey
 * @param fieldKey
 */
function doDispatch(fieldGroup,  fieldKey){

	FieldGroupUtils.validate(fieldGroup, (valid) => {
		if(valid) {
			var result = FieldGroupUtils.getResults(fieldGroup);
			dispatchChangeMouseReadout(fieldKey,result[fieldKey] );

		}
		AppDataCntlr.hideDialog(fieldKey);
	});

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
		const {groupKey,fieldKey,radioValue}= this.props;

		if (this.props.groupKey==='PIXEL_OPTION_FORM'){
			form=  <PixelSizeOptionDialogForm
				    groupKey={groupKey}
					fieldKey={fieldKey}
					radioValue={radioValue}
			/>;
		}
		else {

			form= <CoordinateOptionDialogForm
				groupKey={groupKey}
				fieldKey={fieldKey}
				radioValue={radioValue}
			/>;
		}
		return form;


	}


}

// ------------ React component
function CoordinateOptionDialogForm({ groupKey,fieldKey,radioValue}) {


	return (

		<FieldGroup groupKey={groupKey} keepState={true}>
			<div style={ dialogStyle} onClick={ () => doDispatch(groupKey, fieldKey) } >
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
	groupKey:  PropTypes.string.isRequired,
	filedKey:  PropTypes.string,
	radioValue: PropTypes.string.isRequired
};


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


	return (
		<FieldGroup groupKey={groupKey} keepState={true}>
			<div style={ dialogStyle} onClick={ () => doDispatch(groupKey, fieldKey) }>
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
	groupKey: PropTypes.string.isRequired,
	radioValue: PropTypes.string.isRequired,
	filedKey: PropTypes.string
};

