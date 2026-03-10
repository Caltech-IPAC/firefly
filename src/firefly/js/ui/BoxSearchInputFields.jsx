/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack} from '@mui/joy';
import PropTypes, {bool, number, object, string} from 'prop-types';
import React from 'react';
import {SelectedShape} from '../drawingLayers/SelectedShape';
import {
    DEFAULT_INSTRUCTIONS_RECT, DEFAULT_SELECTION_TEXT, SelectAreaForEmbedded
} from '../visualize/ui/SelectAreaUIComponents';
import {ANGULAR_SIZE_UNITS, SizeInputFields} from './SizeInputField.jsx';


const ROT_MIN = -360;
const ROT_MAX = 360;
const ROT_STEP = 10;

function RotationInputField({fieldKey, label='Rotation', initialState={value:'0'}, sx}) {
    return (
        <SizeInputFields {...{
            fieldKey,
            showFeedback: true,
            nullAllowed: false,
            label,
            quantityName: 'Angle',
            quantityUnitOptions: [{value: 'deg', label: ANGULAR_SIZE_UNITS.deg.name}],
            initialState: {
                unit: 'deg',
                min: ROT_MIN,
                max: ROT_MAX,
                ...initialState,
            },
            sx,
            slotProps: {
                // to show numeric increment/decrement controls in the inner HTML <input> element
                inputField: {
                    slotProps: {
                        input: { // joy-ui Input
                            type: 'number',
                            slotProps: { // HTML <input>
                                input: { min: ROT_MIN, max: ROT_MAX, step: ROT_STEP }
                            }
                        }
                    }
                },
                feedback:{sx: {alignSelf:'center'} },
            }
        }} />
    );
}

export function BoxSearchInputFields({
    boxSizeXKey,
    boxSizeXLabel='Search Box X Size',
    boxSizeYKey,
    boxSizeYLabel='Search Box Y Size',
    boxRotationKey,
    boxRotationLabel='Search Box Y Rotation (E of N)',
    min,
    max,
    initSizeXState,
    initSizeYState,
    initRotationState,
    fieldSx={},
    selectButtonText=DEFAULT_SELECTION_TEXT,
    instructionsText=DEFAULT_INSTRUCTIONS_RECT,
}) {
    const defaultFieldSx = {width: '16rem'};
    return (
        <Stack spacing={1}>
            <Stack spacing={2} direction='row'>
                <SizeInputFields {...{
                    fieldKey: boxSizeXKey, showFeedback: true, nullAllowed: false,
                    label: boxSizeXLabel,
                    initialState: {unit: 'arcsec', ...initSizeXState, min, max},
                    sx: {...defaultFieldSx, ...fieldSx},
                    slotProps: {
                        feedback:{sx: {alignSelf:'center'} },
                    }
                }} />
                <SizeInputFields {...{
                    fieldKey: boxSizeYKey, showFeedback: true, nullAllowed: false,
                    label: boxSizeYLabel,
                    initialState: {unit: 'arcsec', ...initSizeYState, min, max},
                    sx: {...defaultFieldSx, ...fieldSx},
                    slotProps: {
                        feedback:{sx: {alignSelf:'center'} },
                    }
                }} />
            </Stack>
            <Stack spacing={2} direction='row'>
                <RotationInputField fieldKey={boxRotationKey} label={boxRotationLabel}
                                    initialState={initRotationState}
                                    sx={{...defaultFieldSx, ...fieldSx}} />
                {Boolean(selectButtonText) &&
                    <SelectAreaForEmbedded {...{selectButtonText, instructionsText, shape: SelectedShape.rect,
                        sx: { maxWidth: defaultFieldSx.width }}}/>}
            </Stack>
        </Stack>
    );
}

BoxSearchInputFields.propTypes = {
    boxSizeXKey: string,
    boxSizeXLabel: string,
    boxSizeYKey: string,
    boxSizeYLabel: string,
    boxRotationKey: string,
    boxRotationLabel: string,
    min: number,
    max: number,
    initSizeXState: object,
    initSizeYState: object,
    initRotationState: object,
    fieldSx: object,
    selectButtonText: string,
    instructionsText: PropTypes.oneOfType([string, bool]),
};
