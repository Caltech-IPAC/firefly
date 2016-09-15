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
import Validate from '../../util/Validate.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';

const options= [
    {label: 'Fixed', value:'fixed', proj:'LC'},
    {label: 'Adaptive', value:'adptive', proj:'LC'},

];

export class LcPlotOptions extends Component {

    constructor(props) {
        super(props);
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted= false;
    }

    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= FieldGroupUtils.bindToStore('PLOT_PANEL', (fields) => {
            if (this.iAmMounted) this.setState(fields);
        });
    }

    render() {
        const fields= this.state;
        return (
            <LcPlotOptionsPanel />
        );

    }


}


function hideSearchPanel() {
    dispatchHideDropDown();
}


export const LcPlotOptionsPanel = () =>  {
    const labelWidth = 35;
    return (
        <div style={{padding:5}}>
            <FormPanel
                width='400px' height='200px'
                groupKey='PLOT_PANEL'
                onSubmit={(request) => onSearchSubmit(request)}
                onCancel={hideSearchPanel}>
                <FieldGroup groupKey='PLOT_PANEL' validatorFunc={null} keepState={true}>
                    <InputGroup labelWidth={150}>
                        <ListBoxInputField  initialState= {{
                              tooltip: 'Select Bin Size Method',
                              label : 'Bin Size Method:'
                       }}
                        options={options }
                        multiple={false}
                        fieldKey='plotoption'
                        />
                    </InputGroup>
                </FieldGroup>
                <FieldGroupCollapsible  header='More Options'
                                    initialState= {{ value:'open' }}
                                        groupKey='PLOT_PANEL'>

                                    <div>
                                       <ValidationField
                                           forceReinit={true}
                                           style={{width:55}}
                                           initialState= {{
                                               value: '0.5',
                                               validator: Validate.floatRange.bind(null, 0.25555, 1.22222, 3,'X Min'),
                                               groupKey: 'PLOT_PANEL',
                                               tooltip: 'Minimum X value',
                                               label : 'X Min:'
                                           }}
                                           fieldKey='xMin'
                                           labelWidth={labelWidth}/>
                                       <ValidationField
                                           forceReinit={true}
                                           style={{width:55}}
                                           initialState= {{
                                               value: '0.5',
                                               validator: Validate.floatRange.bind(null, 0.25555, 1.22222, 3,'Y Min'),
                                               groupKey: 'PLOT_PANEL',
                                               tooltip: 'Minimum Y value',
                                               label : 'Y Min:'
                                           }}
                                           fieldKey='yMin'
                                           labelWidth={labelWidth}/>
                                   </div>
                                   <div style={{paddingRight: 5}}>
                                       <ValidationField
                                           forceReinit={true}
                                           style={{width:55}}
                                           initialState= {{
                                               value: '5',
                                               validator: Validate.floatRange.bind(null, 4.55555, 10.22222, 3,'X Max'),
                                               groupKey: 'PLOT_PANEL',
                                               tooltip: 'Maximum X value',
                                               label : 'X Max:'
                                           }}
                                           fieldKey='xMax'
                                           labelWidth={labelWidth}/>
                                       <ValidationField
                                           forceReinit={true}
                                           style={{width:55}}
                                           initialState= {{
                                               value: '5',
                                               validator: Validate.floatRange.bind(null, 4.55555, 10.22222, 3,'Y Max'),
                                               groupKey: 'PLOT_PANEL',
                                               tooltip: 'Maximum Y value',
                                               label : 'Y Max:'
                                           }}
                                           fieldKey='yMax'
                                           labelWidth={labelWidth}/>
                                   </div>
                </FieldGroupCollapsible>
                <br/><br/>
                <div>
                 Parameter Panel changes with the selected Bin Size Method
                </div>
            </FormPanel>
        </div>

    );
};

LcPlotOptionsPanel.propTypes = {
    name: PropTypes.oneOf(['LCPlotOption'])
};

LcPlotOptionsPanel.defaultProps = {
    name: 'LCPlotOption',
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


