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
import CompleteButton from '../../ui/CompleteButton.jsx';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import FieldGroupUtils, {getFieldGroupResults} from '../../fieldGroup/FieldGroupUtils.js';
import {useFieldGroupValue} from '../../ui/SimpleComponent.jsx';
import { dispatchChangeReadoutPrefs} from '../../visualize/MouseReadoutCntlr.js';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {isHiPS} from '../WebPlot.js';

//define the labels and values for the radio options
const coordOptions= [
	{label: 'Equatorial J2000 HMS', value: 'eqj2000hms'},
	{label: 'Equatorial J2000 decimal', value: 'eqj2000DCM' },
	{label: 'Galactic', value: 'galactic'},
	{label: 'Equatorial B1950', value: 'eqb1950'},
	{label: 'Ecliptic J2000', value: 'eclJ2000'},
	{label: 'Ecliptic B1950', value: 'eclB1950'},
	{label: 'FITS Image Pixel', value: 'fitsIP'},
    {label: 'Zero based Image Pixel', value: 'zeroIP'}
];

const hipsCoordOptions= [
    {label: 'Equatorial J2000 HMS', value: 'eqj2000hms'},
    {label: 'Equatorial J2000 decimal', value: 'eqj2000DCM' },
    {label: 'Galactic', value: 'galactic'},
    {label: 'Equatorial B1950', value: 'eqb1950'},
	{label: 'Ecliptic J2000', value: 'eclJ2000'},
	{label: 'Ecliptic B1950', value: 'eclB1950'},
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

export function showMouseReadoutFluxRadixDialog(readoutPrefs) {
	const popup = (
		<PopupPanel title={'Choose Pixel readout radix'}  >
			<FluxRadixDialog readoutPrefs={readoutPrefs} dialogId='fluxRadixDialog'/>
		</PopupPanel>
	);
	DialogRootContainer.defineDialog('fluxRadixDialog', popup);
	dispatchShowDialog('fluxRadixDialog');
}


/**
 * this method dispatcher the action to the store.
 * @param fieldGroup
 * @param fieldKey
 */
function doDispatch(fieldGroup,  fieldKey, dialogKey, hide= true){
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
		if (hide) dispatchHideDialog(dialogKey??fieldKey);
	},0);
}

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


function FluxRadixDialog({readoutPrefs, dialogId}) {

	const groupKey= 'radixPrefs';
	const [getIntFluxValueRadix, setIntFluxValueRadix]= useFieldGroupValue('intFluxValueRadix',groupKey);
	const [getFloatFluxValueRadix, setFloatFluxValueRadix]= useFieldGroupValue('floatFluxValueRadix',groupKey);

	useEffect(() => {
		setIntFluxValueRadix(readoutPrefs.intFluxValueRadix);
		setFloatFluxValueRadix(readoutPrefs.floatFluxValueRadix);
	},[]);

	return (
		<FieldGroup groupKey={groupKey} style={{...dialogStyle, flexDirection:'column'}} keepState={false}>
			<div style={ {display:'flex', alignItems:'center'}}
				 onClick={ () => doDispatch(groupKey, 'intFluxValueRadix', 'fluxRadixDialog', false) } >
				<div style={{width:'15em'}} title='Select Value Optoins'>Integer data readout radix</div>
				<RadioGroupInputField
					wrapperStyle={rightColumn}
					options={
						[ {label: 'Decimal', value: '10'}, {label: 'Hexadecimal', value: '16'}, ]
					}
					alignment={'vertical'} fieldKey='intFluxValueRadix'
					initialState={{ tooltip: 'Please select an option', value:getIntFluxValueRadix()}} />
			</div>
			<div style={{display:'flex', alignItems:'center', paddingTop:15}}
				 onClick={ () => doDispatch(groupKey, 'floatFluxValueRadix', 'fluxRadixDialog', false) } >
				<div style={{width:'15em'}} title='Select Value Optoins'>Floating Point data readout radix</div>
				<RadioGroupInputField
					wrapperStyle={rightColumn}
					options={
						[ {label: 'Decimal', value: '10'}, {label: 'Hexadecimal', value: '16'}, ]
					}
					alignment={'vertical'} fieldKey='floatFluxValueRadix'
					initialState={{ tooltip: 'Please select an option', value:getFloatFluxValueRadix()}} />
			</div>
			<div style={{padding:'25px 10px 10px 5px'}}>
				Choosing hexadecimal display will suppress all application of rescaling corrections (i.e. BZERO and BSCALE).
				<br/><br/>
				Hexadecimal will show the raw number in the file.
			</div>
			<div style={{width:'100%', borderTop:'1px solid rgba(0,0,0,.1)', paddingTop: 5}}/>
			<div style={{display:'flex', width:'100%', justifyContent:'space-between'}}>
				<CompleteButton style={{alignSelf:'flex-start'}} dialogId={dialogId} text='Close'/>
				<HelpIcon helpId={'visualization.radixOptions'} style={{alignSelf:'flex-end'}}/>
			</div>
		</FieldGroup>
		);
}




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
