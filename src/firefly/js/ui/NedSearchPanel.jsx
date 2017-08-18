/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {SizeInputFields} from './SizeInputField.jsx';
import {ValidationField} from './ValidationField.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {gkey} from '../visualize/ui/CatalogSelectViewPanel.jsx';
import {HelpIcon} from '../ui/HelpIcon.jsx';

import './VoSearchPanel.css';

export class NedSearchPanel extends PureComponent {

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
                </div>
                <div style={{display:'flex',flexDirection:'column', alignItems:'flex-end'}}>
                    <HelpIcon
                        helpId={'catalogs.ned'}/>
                </div>
            </div>
        );

    }

}

function targetPanelArea() {
    return (
        <div >
            <TargetPanel groupKey={gkey} labelWidth={100}/>
        </div>
    );
}

let sizeArea = () => {
    return (
        <SizeInputFields fieldKey='nedconesize' showFeedback={true}
                         initialState={{
                                           value: parseFloat(10/3600).toString(),
                                           tooltip: 'Please select an option',
                                           unit: 'arcsec',
                                           min:  1/3600,
                                           max:  5
                                 }}
                         label='Radius:'
        />
    );
};