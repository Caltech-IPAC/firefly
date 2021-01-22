/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {SizeInputFields} from './SizeInputField.jsx';

import './VoSearchPanel.css';

export const NedSearchPanel= () => {
    return (
        <div className='vopanel__wrapper'>
            <div className='vopanel'>
                <div>
                    <TargetPanel labelWidth={100}/>
                </div>
                <div style={{height: 60}}>
                    <SizeInputFields fieldKey='nedconesize' showFeedback={true} label='Radius:'
                                                              initialState={{
                                                                  value: parseFloat(10/3600).toString(),
                                                                  tooltip: 'Please select an option',
                                                                  unit: 'arcsec', min:  1/3600, max:  5
                                                              }} />
                </div>
            </div>
        </div>
    );
};