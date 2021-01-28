/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {SizeInputFields} from './SizeInputField.jsx';
import {ValidationField} from './ValidationField.jsx';

import './VoSearchPanel.css';

export const VoSearchPanel = () =>(
        <div className='vopanel__wrapper'>
            <div className='vopanel'>
                <div>
                    <TargetPanel labelWidth={100} nullAllowed={false}/>
                </div>
                <div style={{height: 60}}>
                    <SizeInputFields fieldKey='conesize' showFeedback={true} label='Radius:'
                                     initialState={{
                                         value: (500/3600)+'', unit: 'arcsec', min:  1/3600, max:  1,
                                         tooltip: 'Please select an option'}}/>
                </div>
                <div style={{marginTop: 20, display:'flex', flexDirection: 'column'}}>
                    <VOSearchArea />
                    <a style={{padding:'20px 0 20px 0'}} target='_blank' href='http://vao.stsci.edu/directory/'>Find Astronomical Data Resources </a>
                </div>
            </div>
        </div>
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