/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack} from '@mui/joy';
import React from 'react';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {SizeInputFields} from './SizeInputField.jsx';
import {ValidationField} from './ValidationField.jsx';

export const VoSearchPanel = () =>(
    <Stack sx={{width: 700, m:3}}>
        <TargetPanel labelWidth={100} nullAllowed={false}/>
        <SizeInputFields fieldKey='conesize' showFeedback={true} label='Radius:'
                         initialState={{
                             value: (500/3600)+'', unit: 'arcsec', min:  1/3600, max:  1,
                             tooltip: 'Please select an option'}}/>
        <Stack {...{mt: 3}}>
            <VOSearchArea />
            <a style={{padding:'20px 0 20px 0'}} target='_blank' href='http://vao.stsci.edu/directory/'>Find Astronomical Data Resources </a>
        </Stack>
    </Stack>
    );


const VOSearchArea = () => (
        <ValidationField
            fieldKey='vourl' size={60} actOn={['blur','enter']} wrapperStyle={{margin: '5px 0'}}
            initialState={{
                              fieldKey: 'vourl',
                              value: '',
                              tooltip:'Enter the VO Simple Cone Search URL directly (or use the link below to open external NVO search and find the VO cone search URL)',
                              label:'Cone Search URL:',
                              labelWidth : 100,
                              nullAllowed:false,
                          }}
            placeholder='Ex. https://irsa.ipac.caltech.edu/SCS?table=allwise_p3as_psd&' />
    );