/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   1/20/16
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 */

import {Divider, Stack, Typography} from '@mui/joy';
import React, {useState,useEffect} from 'react';
import PropTypes from 'prop-types';
import {dispatchAddPreference} from '../../core/AppDataCntlr';
import CompleteButton from '../../ui/CompleteButton.jsx';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import FieldGroupUtils, {getFieldGroupResults} from '../../fieldGroup/FieldGroupUtils.js';
import {useFieldGroupValue, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {
	dispatchChangeReadoutPrefs, readoutRoot,
	MR_ECL1950, MR_ECLJ2000, MR_EQB1950, MR_EQB1950_DCM, MR_EQJ2000_DCM, MR_EQJ2000_HMS,
	MR_FIELD_HIPS_MOUSE_READOUT1, MR_FIELD_HIPS_MOUSE_READOUT2,
	MR_FIELD_IMAGE_MOUSE_READOUT1, MR_FIELD_IMAGE_MOUSE_READOUT2,
	MR_FITS_IP, MR_GALACTIC, MR_PIXEL_SIZE, MR_SPIXEL_SIZE, MR_SUPER_GALACTIC, MR_ZERO_IP,
} from '../MouseReadoutCntlr';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {isCelestialImage, isHiPS} from '../WebPlot.js';

//define the labels and values for the radio options
const celestialCoordOptions= [
	{label: 'Equatorial J2000 HMS', value: MR_EQJ2000_HMS},
	{label: 'Equatorial J2000 Decimal', value: MR_EQJ2000_DCM },
	{label: 'Equatorial B1950 HMS', value: MR_EQB1950},
	{label: 'Equatorial B1950 Decimal', value: MR_EQB1950_DCM},
	{label: 'Galactic', value: MR_GALACTIC},
	{label: 'Super Galactic', value: MR_SUPER_GALACTIC},
	{label: 'Ecliptic J2000', value: MR_ECLJ2000},
	{label: 'Ecliptic B1950', value: MR_ECL1950},
	{label: 'FITS Image Pixel', value: MR_FITS_IP},
	{label: 'Zero based Image Pixel', value: MR_ZERO_IP},
];

const hipsCoordOptions= [
	{label: 'Equatorial J2000 HMS', value: MR_EQJ2000_HMS},
	{label: 'Equatorial J2000 Decimal', value: MR_EQJ2000_DCM},
	{label: 'Equatorial B1950 HMS', value: MR_EQB1950},
	{label: 'Equatorial B1950 Decimal', value: MR_EQB1950_DCM},
	{label: 'Galactic', value: MR_GALACTIC},
	{label: 'Super Galactic', value: MR_SUPER_GALACTIC},
	{label: 'Ecliptic J2000', value: MR_ECLJ2000},
	{label: 'Ecliptic B1950', value: MR_ECL1950},
];


const pixelOptions = [
	{label: 'Pixel Size', value: MR_PIXEL_SIZE},
	{label: 'Screen Pixel Size', value: MR_SPIXEL_SIZE }
];

const groupKeys={
	[MR_FIELD_IMAGE_MOUSE_READOUT1]:'COORDINATE_OPTION_FORM',
	imageMouseNoncelestialReadout1:'COORDINATE_OPTION_FORM',
	[MR_FIELD_IMAGE_MOUSE_READOUT2]:'COORDINATE_OPTION_FORM',
	imageMouseNoncelestialReadout2:'COORDINATE_OPTION_FORM',
	[MR_FIELD_HIPS_MOUSE_READOUT1]:'COORDINATE_OPTION_FORM',
	[MR_FIELD_HIPS_MOUSE_READOUT2]:'COORDINATE_OPTION_FORM',
	pixelSize: 'PIXEL_OPTION_FORM'
};
const copyOptsFieldKey = 'mouseReadoutValueCopy';

const dialogStyle = { minWidth : 300, minHeight: 100 , padding:10};


function getNoncelestialCoordOptions(noncelestialOptionTitle='WCS Coordinates') {
	return [
		{label: noncelestialOptionTitle, value: 'wcsCoords'},
		{label: 'FITS Image Pixel', value: 'fitsIP'},
		{label: 'Zero based Image Pixel', value: 'zeroIP'}
	];
}

export function showMouseReadoutOptionDialog(fieldKey, radioValue, copyOptionValue, title='Choose Option', noncelestialOptionTitle=undefined) {
	const plot = primePlot(visRoot());
	const popup = (
		<PopupPanel title={title}  >
			<MouseReadoutOptionDialog groupKey={groupKeys[fieldKey]} fieldKey={fieldKey}
									  radioValue={radioValue} copyOptionValue={copyOptionValue} isHiPS={isHiPS(plot)}
									  isCelestial={isCelestialImage(plot)}
									  noncelestialOptionTitle={noncelestialOptionTitle}
			/>
		</PopupPanel>
	);
	DialogRootContainer.defineDialog(fieldKey, popup);
	dispatchShowDialog(fieldKey);
}

export function showMouseReadoutFluxRadixDialog(readoutPrefs) {
	const popup = (
		<PopupPanel title={'Choose pixel readout radix'}  >
			<FluxRadixDialog readoutPrefs={readoutPrefs} dialogId='fluxRadixDialog'/>
		</PopupPanel>
	);
	DialogRootContainer.defineDialog('fluxRadixDialog', popup);
	dispatchShowDialog('fluxRadixDialog');
}


/**
 * this method dispatcher the action to the store.
 * @param {String} fieldGroup
 * @param {String} fieldKey
 * @param {String} dialogKey
 * @param {boolean} [hide]
 */
function doDispatch(fieldGroup,  fieldKey, dialogKey, hide= true){
	window.setTimeout(() => { // since the mouse click happens before the store can update, we must defer the actions
		const results= getFieldGroupResults(fieldGroup,true);
		const prefValue= results[fieldKey];
		if (prefValue==='fitsIP' || prefValue==='zeroIP' ) {
            dispatchChangeReadoutPrefs({[fieldKey]:prefValue});
        }
		else if (fieldKey===MR_FIELD_IMAGE_MOUSE_READOUT1 || fieldKey===MR_FIELD_HIPS_MOUSE_READOUT1) {
            dispatchChangeReadoutPrefs({[MR_FIELD_IMAGE_MOUSE_READOUT1]:prefValue, [MR_FIELD_HIPS_MOUSE_READOUT1]:prefValue});
			dispatchAddPreference(MR_FIELD_IMAGE_MOUSE_READOUT1, prefValue);
			dispatchAddPreference(MR_FIELD_HIPS_MOUSE_READOUT1, prefValue);
        }
        else if (fieldKey===MR_FIELD_IMAGE_MOUSE_READOUT2 || fieldKey===MR_FIELD_HIPS_MOUSE_READOUT2) {
            dispatchChangeReadoutPrefs({[MR_FIELD_IMAGE_MOUSE_READOUT2]:prefValue, [MR_FIELD_HIPS_MOUSE_READOUT2]:prefValue});
			dispatchAddPreference(MR_FIELD_IMAGE_MOUSE_READOUT2, prefValue);
			dispatchAddPreference(MR_FIELD_HIPS_MOUSE_READOUT2, prefValue);
        }
        else {
            dispatchChangeReadoutPrefs({[fieldKey]:prefValue});
        }
		if (hide) dispatchHideDialog(dialogKey??fieldKey);
	},0);
}

function MouseReadoutOptionDialog({groupKey, fieldKey, radioValue, copyOptionValue, isHiPS, isCelestial, noncelestialOptionTitle}) {
		const [,setFields]= useState(FieldGroupUtils.getGroupFields(groupKey));
		const options = (isHiPS && hipsCoordOptions) ||
			(!isCelestial && getNoncelestialCoordOptions(noncelestialOptionTitle)) || celestialCoordOptions;

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

		return ( groupKey===groupKeys.pixelSize ?
				<PixelSizeOptionDialogForm groupKey={groupKey} fieldKey={fieldKey} radioValue={radioValue} /> :
				<CoordinateOptionDialogForm {...{groupKey, fieldKey, radioValue, copyOptionValue, isHiPS, isCelestial,
					optionList: options}}/>
		);
}

MouseReadoutOptionDialog.propTypes= {
	groupKey:   PropTypes.string.isRequired,
	fieldKey:   PropTypes.string.isRequired,
	radioValue:   PropTypes.string.isRequired,
	copyOptionValue:   PropTypes.string,
    isHiPS: PropTypes.bool.isRequired,
	isCelestial: PropTypes.bool.isRequired,
	noncelestialOptionTitle: PropTypes.string,
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
		<FieldGroup groupKey={groupKey} style={dialogStyle} keepState={false}>
			<Stack direction='column' spacing={2}>
				<Stack {...{direction:'row', alignItems:'center',
					onClick: () => doDispatch(groupKey, 'intFluxValueRadix', 'fluxRadixDialog', false) }} >
					<Typography {...{whiteSpace:'nowrap',width:'17em',pr:1, textAlign:'right'}}>
						Integer data readout radix:
					</Typography>
					<RadioGroupInputField
						options={
							[ {label: 'Decimal', value: '10'}, {label: 'Hexadecimal', value: '16'}, ]
						}
						alignment={'vertical'} fieldKey='intFluxValueRadix'
						initialState={{value:getIntFluxValueRadix()}} />
				</Stack>
				<Stack {...{direction:'row', alignItems:'center',
					onClick: () => doDispatch(groupKey, 'floatFluxValueRadix', 'fluxRadixDialog', false) }} >
					<Typography {...{whiteSpace:'nowrap',width:'17em',pr:1, textAlign:'right'}}>
						Floating Point data readout radix:
					</Typography>
					<RadioGroupInputField
						options={
							[ {label: 'Decimal', value: '10'}, {label: 'Hexadecimal', value: '16'}, ]
						}
						alignment={'vertical'} fieldKey='floatFluxValueRadix'
						initialState={{value:getFloatFluxValueRadix()}} />
				</Stack>
				<Typography {...{level:'body-xs', width:'45em', textAlign:'center'}}>
					Choosing hexadecimal display will suppress all application of rescaling corrections (i.e. BZERO and BSCALE).
					<br/><br/>
					Hexadecimal will show the raw number in the file.
				</Typography>
				{/*<div style={{width:'100%', borderTop:'1px solid rgba(0,0,0,.1)', paddingTop: 5}}/>*/}
				<Divider orientation='horizontal'/>
				<div style={{display:'flex', width:'100%', justifyContent:'space-between'}}>
					<CompleteButton style={{alignSelf:'flex-start'}} dialogId={dialogId} text='Close'/>
					<HelpIcon helpId={'visualization.fitsViewer'}/>
				</div>
			</Stack>
		</FieldGroup>
		);
}




const CoordinateOptionDialogForm= ({ groupKey,fieldKey, copyOptionValue, optionList, isCelestial, isHiPS}) => {
	const currentRadioValue = useStoreConnector(()=>readoutRoot()?.readoutPref?.[fieldKey], [fieldKey]);
	const [, setCopyOptionValue] = useFieldGroupValue(copyOptsFieldKey, groupKey);
	const [isSkyCoordDisabled, setIsSkyCoordDisabled] = useState(false);

	const showCopyOpts = isHiPS || isCelestial; //do not show in nonCelestial case

	useEffect(()=>{
		//Image Pixel (IP) options exist in isCelestial case, but they don't have a WorldPt needed for SkyCoord copy option
		if (currentRadioValue?.endsWith('IP')) {
			setIsSkyCoordDisabled(true);
			//revert to 'str' copy option because 'skyCoord' copy option got disabled
			setCopyOptionValue('str');
			dispatchChangeReadoutPrefs({[copyOptsFieldKey]: 'str'});
		}
		else {
			setIsSkyCoordDisabled(false);
		}
	}, [currentRadioValue]);

	const radioStackLayout = {
		direction:'row', spacing: 2, alignItems: 'flex-start'
	};

	const renderReadoutOptions = () => (
		<Stack {...{
			...radioStackLayout,
			onClick:() => doDispatch(groupKey, fieldKey, undefined, false)
		}}>
			<Typography {...{level:'body-md', whiteSpace:'nowrap'}}>Readout Options:</Typography>
			<RadioGroupInputField
				options={optionList} alignment={'vertical'} fieldKey={fieldKey}
				initialState={{ tooltip: 'Please select an option', value: currentRadioValue }} />
		</Stack>
	);

	const renderCopyOptions = () => {
		const copyOptionList = [
			{label: 'Readout values verbatim', value: 'str'},
			{label: '[Python] AstroPy SkyCoord', value: 'skyCoord', disabled: isSkyCoordDisabled},
		];
		return (
			<Stack {...{
				...radioStackLayout,
				onClick: () => doDispatch(groupKey, copyOptsFieldKey, undefined, false)
			}}>
				<Typography {...{level: 'body-md', whiteSpace: 'nowrap'}}>Copy Options:</Typography>
				<RadioGroupInputField
					options={copyOptionList}
					alignment={'vertical'} fieldKey={copyOptsFieldKey}
					initialState={{
						tooltip: 'Please select how to copy the readout coordinates',
						value: copyOptionValue
					}}/>
			</Stack>
		);
	};

	return (
		<FieldGroup groupKey={groupKey} keepState={false} sx={{m: 1.5}}>
			<Stack spacing={2}>
				{renderReadoutOptions()}
				{showCopyOpts && renderCopyOptions()}
				<Divider orientation='horizontal'/>
				<Stack direction='row' sx={{width: 1, justifyContent: 'space-between'}}>
					<CompleteButton style={{alignSelf: 'flex-start'}} dialogId={fieldKey} text='Close'/>
					<HelpIcon helpId={'visualization.fitsViewer'}/>
				</Stack>
			</Stack>
		</FieldGroup>
	);
};

CoordinateOptionDialogForm.propTypes= {
	groupKey:  PropTypes.string.isRequired,
	fieldKey:  PropTypes.string,
	radioValue: PropTypes.string.isRequired,
	copyOptionValue: PropTypes.string,
	optionList: PropTypes.arrayOf(PropTypes.object).isRequired,
	isCelestial: PropTypes.bool.isRequired,
	isHiPS: PropTypes.bool.isRequired,
};

const PixelSizeOptionDialogForm= ( {groupKey,fieldKey, radioValue} ) => (
	<FieldGroup groupKey={groupKey} keepState={false}>
		<Stack {...{direction:'row', spacing:1, alignItems:'center', m:2, onClick:() => doDispatch(groupKey, fieldKey)}}>
			<Typography {...{whiteSpace:'nowrap'}}>Pixel Options:</Typography>
			<RadioGroupInputField
				initialState={{ tooltip: 'Please select an option', value:radioValue }}
				options={ pixelOptions } alignment={'vertical'} fieldKey={fieldKey} />
		</Stack>
	</FieldGroup>
);

PixelSizeOptionDialogForm.propTypes= {
	groupKey: PropTypes.string.isRequired,
	radioValue: PropTypes.string.isRequired,
	fieldKey: PropTypes.string
};
