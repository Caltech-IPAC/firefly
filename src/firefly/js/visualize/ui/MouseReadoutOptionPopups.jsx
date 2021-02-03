/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   1/20/16
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 */

import React, {useState,useEffect} from 'react';
import PropTypes from 'prop-types';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import FieldGroupUtils, {getFieldGroupResults} from '../../fieldGroup/FieldGroupUtils.js';
import { dispatchChangeReadoutPrefs} from '../../visualize/MouseReadoutCntlr.js';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {isHiPS} from '../WebPlot.js';

//define the labels and values for the radio options
const coordOptions= [
	{label: 'EQ J2000 HMS', value: 'eqj2000hms'},
	{label: 'EQ J2000 decimal', value: 'eqj2000DCM' },
	{label: 'Galactic', value: 'galactic'},
	{label: 'EQ B1950', value: 'eqb1950'},
	{label: 'ECL J2000', value: 'eclJ2000'},
	{label: 'FITS Image Pixel', value: 'fitsIP'},
    {label: 'Zero based Image Pixel', value: 'zeroIP'}
];

const hipsCoordOptions= [
    {label: 'EQ J2000 HMS', value: 'eqj2000hms'},
    {label: 'EQ J2000 decimal', value: 'eqj2000DCM' },
    {label: 'Galactic', value: 'galactic'},
    {label: 'EQ B1950', value: 'eqb1950'},
	{label: 'ECL J2000', value: 'eclJ2000'},
];


const pixelOptions = [
	{label: 'Pixel Size', value: 'pixelSize'},
	{label: 'Screen Pixel Size', value: 'sPixelSize' }
];

const groupKeys={
	imageMouseReadout1:'COORDINATE_OPTION_FORM',
	imageMouseReadout2:'COORDINATE_OPTION_FORM',
    hipsMouseReadout1:'COORDINATE_OPTION_FORM',
    hipsMouseReadout2:'COORDINATE_OPTION_FORM',
	pixelSize: 'PIXEL_OPTION_FORM'
};

const leftColumn = {paddingLeft:20};
const rightColumn = {paddingLeft:18};
const dialogStyle = { minWidth : 300, minHeight: 100 , padding:10, display:'flex', alignItems:'center'};


export function showMouseReadoutOptionDialog(fieldKey,radioValue) {
	const popup = (
		<PopupPanel title={'Choose Option'}  >
			<MouseReadoutOptionDialog groupKey={groupKeys[fieldKey]} fieldKey={fieldKey}
									  radioValue={radioValue} isHiPS={isHiPS(primePlot(visRoot()))}/>
		</PopupPanel>
	);
	DialogRootContainer.defineDialog(fieldKey, popup);
	dispatchShowDialog(fieldKey);
}


/**
 * this method dispatcher the action to the store.
 * @param fieldGroup
 * @param fieldKey
 */
function doDispatch(fieldGroup,  fieldKey){
	window.setTimeout(() => { // since the mouse click happens before the store can update, we must defer the actions
		const results= getFieldGroupResults(fieldGroup,true);
		const prefValue= results[fieldKey];
		if (prefValue==='fitsIP' || prefValue==='zeroIP' ) {
            dispatchChangeReadoutPrefs({[fieldKey]:prefValue});
        }
		else if (fieldKey==='imageMouseReadout1' || fieldKey==='hipsMouseReadout1') {
            dispatchChangeReadoutPrefs({imageMouseReadout1:prefValue});
            dispatchChangeReadoutPrefs({hipsMouseReadout1:prefValue});
        }
        else if (fieldKey==='imageMouseReadout2' || fieldKey==='hipsMouseReadout2') {
            dispatchChangeReadoutPrefs({imageMouseReadout2:prefValue});
            dispatchChangeReadoutPrefs({hipsMouseReadout2:prefValue});
        }
        else {
            dispatchChangeReadoutPrefs({[fieldKey]:prefValue});
        }

		dispatchHideDialog(fieldKey);
	},0);
}

/**
 *  create a popup dialog
 */
function MouseReadoutOptionDialog({groupKey,fieldKey,radioValue, isHiPS}) {
		const [,setFields]= useState(FieldGroupUtils.getGroupFields(groupKey));

		useEffect(() => {
			let enabled= true;
			const unbinder= FieldGroupUtils.bindToStore(groupKey, (fields) => {
				enabled && setFields(fields);
			});
			return () => {
				unbinder();
				enabled= false;
			};
		},[groupKey]);

		return ( groupKey==='PIXEL_OPTION_FORM' ?
				<PixelSizeOptionDialogForm groupKey={groupKey} fieldKey={fieldKey} radioValue={radioValue} /> :
				<CoordinateOptionDialogForm groupKey={groupKey} fieldKey={fieldKey} radioValue={radioValue}
											optionList={isHiPS ? hipsCoordOptions : coordOptions} />
		);
}

MouseReadoutOptionDialog.propTypes= {
	groupKey:   PropTypes.string.isRequired,
	fieldKey:   PropTypes.string.isRequired,
	radioValue:   PropTypes.string.isRequired,
    isHiPS: PropTypes.bool.isRequired
};

const CoordinateOptionDialogForm= ({ groupKey,fieldKey,radioValue, optionList}) => (
		<FieldGroup groupKey={groupKey} keepState={false}>
			<div style={ dialogStyle} onClick={ () => doDispatch(groupKey, fieldKey) } >
				<div style={leftColumn} title='Please select an option'> Readout Options</div>
				<RadioGroupInputField
					wrapperStyle={rightColumn} options={optionList} alignment={'vertical'} fieldKey={fieldKey}
					initialState={{ tooltip: 'Please select an option', value:radioValue }} />
			</div>
		</FieldGroup>
	);

CoordinateOptionDialogForm.propTypes= {
	groupKey:  PropTypes.string.isRequired,
	fieldKey:  PropTypes.string,
	radioValue: PropTypes.string.isRequired,
	optionList: PropTypes.arrayOf(PropTypes.object).isRequired
};

const PixelSizeOptionDialogForm= ( {groupKey,fieldKey, radioValue} ) => (
	<FieldGroup groupKey={groupKey} keepState={false}>
		<div style={ dialogStyle} onClick={ () => doDispatch(groupKey, fieldKey) }>
			<div style={leftColumn} title='Please select an option'> Pixel Options</div>
			<RadioGroupInputField
				initialState={{ tooltip: 'Please select an option', value:radioValue }}
				wrapperStyle={rightColumn} options={ pixelOptions } alignment={'vertical'} fieldKey={fieldKey} />
		</div>
	</FieldGroup>
);

PixelSizeOptionDialogForm.propTypes= {
	groupKey: PropTypes.string.isRequired,
	radioValue: PropTypes.string.isRequired,
	fieldKey: PropTypes.string
};
