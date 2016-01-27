

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


	//var popup = renderOptionDialog(fieldKey);
	//return popup;
	var groupKey;
	var title;
	switch (fieldKey) {
		case 'coordinateSys':
			groupKey = 'COORDINATE_OPTION_FORM';
			title='Coordinate Option  Dialog';
			break;
		case 'flux':
			groupKey = 'FLUX_OPTION_FORM:';
			break;
	}
	return () => {

		var popup = (
			<PopupPanel title={title}>
				<MouseReadoutOptionDialog groupKey={groupKey}/>
			</PopupPanel>
		);
		DialogRootContainer.defineDialog(fieldKey, popup);

		return popup;

	};
}

export function showMouseReadoutOptionDialog(fieldKey) {

	getDialogBuilder(fieldKey);
	AppDataCntlr.showDialog(fieldKey);
}

function renderOptionDialog(fieldKey) {
	var groupKey;
	var title;
	switch (fieldKey) {
		case 'coordinateSys':
			groupKey = 'COORDINATE_OPTION_FORM';
			title='Coordinate Option  Dialog';
			break;
		case 'flux':
			groupKey = 'FLUX_OPTION_FORM:';
			break;
	}
	return () => {

		var popup = (
			<PopupPanel title={title}>
				<MouseReadoutOptionDialog groupKey={groupKey}/>
			</PopupPanel>
		);
		DialogRootContainer.defineDialog(fieldKey, popup);

		return popup;

	};
}


class MouseReadoutOptionDialog extends React.Component {


	constructor(groupKey) {
		super(groupKey);
		FieldGroupUtils.initFieldGroup(groupKey);
		this.state = {fields: FieldGroupUtils.getGroupFields(groupKey)};

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
		return <CoordinateOptionDialogForm  groupKey={this.props.groupKey}/>;

	}


}


function CoordinateOptionDialogForm(groupKey) {


	var leftColumn = { display: 'inline-block', paddingLeft:125, verticalAlign:'middle', paddingBottom:30};

	var rightColumn = {display: 'inline-block',  paddingLeft:18};

	var dialogStyle = { minWidth : 300, minHeight: 100 , padding:5};
	return (
		<FieldGroup groupKey={groupKey} keepState={true}>
			<div style={ dialogStyle}>
					<div style={leftColumn} title='Please select an option'> Options</div>
					<div style={rightColumn}>
							  <RadioGroupInputField
								initialState={{
                                    tooltip: 'Please select an option'
                                    //move the label as a InputFieldLabel
                                   }}
								options={ [
                                      {label: 'EQ J2000 HMS', value: 'eq2000HMS'},
                                      {label: 'EQ J2000 decimal', value: 'eq2000DCM' },
                                      {label: 'Galactic', value: 'galactic'},
                                      {label: 'EQ B1950', value: 'eqb1950'},
                                      {label: 'Fits Image Pixel', value: 'fitsIP'}
                                    ]}
								alignment={'vertical'}
								fieldKey='option'
							 />
					</div>

			</div>

		</FieldGroup>
	);

}


