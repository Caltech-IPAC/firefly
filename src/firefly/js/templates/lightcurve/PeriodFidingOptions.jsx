/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import './LCPanels.css';
import React, {Component, PropTypes} from 'react';
import {get,omit} from 'lodash';

import {FormPanel} from '../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {FieldGroupUtils} from '../../fieldGroup/FieldGroupUtils';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';

const options= [
    {label: 'Lomb Scarble', value:'LombScarble', proj:'LC'},
    {label: 'Phase Dispersion Minimization', value:'PhaseDispersionMinimization', proj:'LC'},

];


export class PeriodFidingOptionsPanel extends Component {

    constructor(props) {
        super(props);
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted= false;
    }

    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= FieldGroupUtils.bindToStore('PFO_PANEL', (fields) => {
            if (this.iAmMounted) this.setState(fields);
        });
    }

    render() {
        const fields= this.state;
        return (
            <LCPFOPanel />
        );

    }


}


function hideSearchPanel() {
    dispatchHideDropDown();
}


export const LCPFOPanel = () =>  {
    return (
        <div style={{padding:5}}>
            <FormPanel
                width='400px' height='200px'
                groupKey='PFO_PANEL'
                onSubmit={(request) => onSearchSubmit(request)}
                onCancel={hideSearchPanel}>
                <FieldGroup groupKey='PFO_PANEL' validatorFunc={null} keepState={true}>
                    <InputGroup labelWidth={150}>
                        <ListBoxInputField  initialState= {{
                              tooltip: 'Select Period Finding Algorithm',
                              label : 'Period Finding Algorithm:'
                       }}
                        options={options }
                        multiple={false}
                        fieldKey='periodfinding'
                        />
                    </InputGroup>
                </FieldGroup>
                <br/><br/>
                <div>
                 Parameter Panel changes with the selected Algorithm
                </div>
            </FormPanel>
        </div>

    );
};

LCPFOPanel.propTypes = {
    name: PropTypes.oneOf(['LCPFO'])
};

LCPFOPanel.defaultProps = {
    name: 'LCPFO',
};


function onSearchSubmit(request) {
    console.log(request);
    if (request.Tabs==='periodfinding') {
        doPeriodFinding(request);
    }
    else {
        console.log('request no supported');
    }
}

function doPeriodFinding(request) {
    var tReq;
    if (tReq != null) {
        dispatchTableSearch(tReq, {removable: false});
    }
}


