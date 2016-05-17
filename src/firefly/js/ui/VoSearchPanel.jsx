/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {SizeInputFields, sizeFromDeg} from './SizeInputField.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import './VoSearchPanel.css';

export class VoSearchPanel extends Component {

    constructor(props) {
        super(props);
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = FieldGroupUtils.bindToStore('CATALOG_PANEL', (fields) => {
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
                </div>
            </div>
        );

    }

}

function targetPanelArea() {
    return (
        <div className={'intarget'}>
            <TargetPanel groupKey={'CATALOG_PANEL'}/>
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
        <SizeInputFields fieldKey='sizefield' showFeedback={true}
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

function voSearchArea() {
    return (
        <div>Search registry box, table result and cone search field here!</div>
    );
}
