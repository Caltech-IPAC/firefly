/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {SizeInputFields, sizeFromDeg} from './SizeInputField.jsx';
import {ValidationField} from './ValidationField.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {gkey} from '../visualize/ui/CatalogSelectViewPanel.jsx';
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
            <div className={'vopanel'}>
                <div className={'section'}>
                    {targetPanelArea()}
                </div>
                <div className={'size'}>
                    { sizeArea()}
                </div>
                <div className={'voarea'}>
                    { voSearchArea() }
                    <div style={{padding:'20px 0 20px 0'}}>
                        <a target='_blank' href='http://nvo.stsci.edu/vor10/index.aspx'>Find Astronomical Data
                            Resources </a>
                    </div>
                </div>
            </div>
        );

    }

}

function targetPanelArea() {
    return (
        <div className={'intarget'}>
            <TargetPanel groupKey={gkey}/>
            <ListBoxInputField
                fieldKey='targettry'
                options={[
                            {label: 'Try NED then Simbad', value: 'NED'},
                            {label: 'Try Simbad then NED', value: 'simbad'}
                         ]}
                multiple={false}
                labelWidth={3}
            />
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
            fieldKey={'vourl'}
            /*validator={urlValidator}*/
            tooltip='Enter the VO cone search URL directly (or use the link below to open external NVO search and find the VO cone search URL)'
            label='VO URL:'
            size={75}
            actOn={['blur','enter']}
            wrapperStyle={{margin: '5px 0'}}
            labelWidth={50}
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