/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {SizeInputFields} from './SizeInputField.jsx';
import {ValidationField} from './ValidationField.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {gkey} from '../visualize/ui/CatalogSelectViewPanel.jsx';
import {HelpIcon} from '../ui/HelpIcon.jsx';

import './VoSearchPanel.css';

export class VoSearchPanel extends React.Component {

    constructor(props) {
        super(props);
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = FieldGroupUtils.bindToStore(gkey, (fields) => {
            if (this.iAmMounted) this.setState(fields);
        });
    }

    render() {
        const fields = this.state;
        return (
            <div className='vopanel__wrapper'>
                <div className='vopanel'>
                    <div>
                        {targetPanelArea()}
                    </div>
                    <div style={{height: 60}}>
                        { sizeArea()}
                    </div>
                    <div style={{marginTop: 20}}>
                        { voSearchArea() }
                        <div style={{padding:'20px 0 20px 0'}}>
                            <a target='_blank' href='http://nvo.stsci.edu/vor10/index.aspx'>Find Astronomical Data
                                Resources </a>
                        </div>
                    </div>
                </div>
                <div style={{display:'flex',flexDirection:'column', alignItems:'flex-end'}}>
                    <HelpIcon
                        helpId={'catalogs.vo'}/>
                </div>
            </div>
        );

    }

}

function targetPanelArea() {
    return (
        <div >
            <TargetPanel groupKey={gkey} labelWidth={100}>
                <ListBoxInputField
                    fieldKey='targettry'
                    initialState={{
                                  fieldKey:'targettry',
                                  label : '',
                                  labelWidth: 0
                              }}
                    label={''}
                    options={[
                            {label: 'Try NED then Simbad', value: 'NED'},
                            {label: 'Try Simbad then NED', value: 'simbad'}
                         ]}
                    multiple={false}
                />
            </TargetPanel>
        </div>
    );
}

var sizeArea = () => {
    return (
        <SizeInputFields fieldKey='conesize' showFeedback={true}
                         initialState={{
                                           value: parseFloat(500/3600).toString(),
                                           tooltip: 'Please select an option',
                                           unit: 'arcsec',
                                           min:  1/3600,
                                           max:  1
                                 }}
                         label='Radius:'
        />
    );
};

var voSearchArea = () => {
    return (
        <ValidationField
            fieldKey='vourl'
            initialState={{
                              fieldKey: 'vourl',
                              value: '',
                              tooltip:'Enter the VO cone search URL directly (or use the link below to open external NVO search and find the VO cone search URL)',
                              label:'Cone Search URL:',
                              labelWidth : 100,
                              nullAllowed:false,
                              /*validator: {urlValidator}*/
                          }}
            size={60}
            actOn={['blur','enter']}
            wrapperStyle={{margin: '5px 0'}}
        />
    );
};

function urlValidator(val) {
    //Check value that match
    const regEx = new RegExp('#\b(([\w-]+://?|www[.])[^\s()<>]+(?:\([\w\d]+\)|([^[:punct:]\s]|/)))#iS');
    let valid = true;//isValid...
    if (!regEx.test(val)) {
        valid = false;
    }
    return {valid, value: val, message: 'VO cone search should be a well-defined URL string'};
}