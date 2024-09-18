/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack} from '@mui/joy';
import React from 'react';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {SizeInputFields} from './SizeInputField.jsx';

export const NedSearchPanel= () => {
    return (
        <Stack sx={{width: 700, m:3}}>
            <Stack spacing={2} >
                <TargetPanel/>
                <SizeInputFields fieldKey='nedconesize' showFeedback={true} label='Radius:'
                                 initialState={{
                                     value: parseFloat(10/3600).toString(),
                                     tooltip: 'Please select an option',
                                     unit: 'arcsec', min:  1/3600, max:  5
                                 }} />
            </Stack>
        </Stack>
    );
};