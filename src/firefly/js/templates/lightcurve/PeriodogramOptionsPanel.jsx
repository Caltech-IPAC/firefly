/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import './LCPanels.css';
import React, {Component, PropTypes} from 'react';
import {get,omit} from 'lodash';

import {FormPanel} from '../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {InputField} from '../../ui/InputField.jsx';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {FieldGroupUtils} from '../../fieldGroup/FieldGroupUtils';
import {FieldGroupCollapsible} from '../../ui/panel/CollapsiblePanel.jsx';
import Validate from '../../util/Validate.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {makeTblRequest,getTblById} from '../../tables/TableUtil.js';
import {RAW_TABLE,PERIODOGRAM, PEAK_TABLE} from '../../templates/lightcurve/LcManager.js';

const gkey = 'PFO_PANEL';
const options= [
    {label: 'Lomb Scarble', value:'LombScarble', proj:'LCViewer'},
    {label: 'Phase Dispersion Minimization', value:'PhaseDispersionMinimization', proj:'LCViewer'},

];
const stepoptions= [
    {label: 'Fixed Frequency', value:'fixedfreq', proj:'LCViewer'},
    {label: 'Adaptive Frequency', value:'adaptivefreq', proj:'LCViewer'},
];


const defValues= {
    periodMin: {
        fieldKey: 'periodMin',
        value: '1',
        forceReinit: true,
        validator: Validate.intRange.bind(null, 1, 100, 'low field'),
        tooltip: 'this is a tip for low field',
        label: 'Minimum Period:',
        labelWidth: 150
    },
    periodMax: {
        fieldKey: 'periodMax',
        value: '3',
        forceReinit: true,
        validator: Validate.intRange.bind(null, 1, 100, 'periodMax field'),
        tooltip: 'this is a tip for periodMax field',
        label: 'Maximum Period:',
        labelWidth: 150
    }
};

export class PeriodogramOptionsPanel extends Component {

    constructor(props) {
        super(props);
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted= false;
    }

    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= FieldGroupUtils.bindToStore(gkey, (fields) => {
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
                groupKey={gkey}
                onSubmit={(request) => onSearchSubmit(request)}
                onCancel={hideSearchPanel}>
                <FieldGroup groupKey={gkey} reducerFunc={periodogramRangeReducer} keepState={true}>
                    <InputGroup labelWidth={150}>
                        <ListBoxInputField  initialState= {{
                              tooltip: 'Select Algorithm',
                              label : 'Algorithm:'
                       }}
                        options={options }
                        multiple={false}
                        fieldKey='periodAlgor'
                        />
                        <br/>
                        <span> <b>Period Range: </b> </span>
                        <br/><br/>
                        <ValidationField fieldKey='periodMin'  />
                        <br/>
                        <ValidationField fieldKey='periodMax'  />
                        <br/>
                        <span> <b>Period Step Method: </b></span>
                        <br/> <br/>
                        <ListBoxInputField  initialState= {{
                              tooltip: 'Period Step Method',
                              label : 'Select Method:'
                           }}
                            options={stepoptions }
                            multiple={false}
                            fieldKey='stepMethod'
                            />
                        <br/>
                        <InputField fieldKey='stepSize'
                              tooltip='Enter Step Size'
                              label='Step Size:'
                        />
                        <br/>

                    </InputGroup>
                </FieldGroup>
                <br/>

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


/**
 *
 * @param {object} inFields
 * @param {object} action
 * @return {object}
 */
var periodogramRangeReducer= function(inFields, action) {
    if (!inFields)  {
        return defValues;
    }
    else {
        var {periodMin,periodMax}= inFields;
        // inFields= revalidateFields(inFields);
        if (!periodMin.valid || !periodMax.valid) {
            return inFields;
        }
        if (parseFloat(periodMin.value)> parseFloat(periodMax.value)) {
            periodMin= Object.assign({},periodMin, {valid:false, message:' periodMin must be lower than periodMax'});
            periodMax= Object.assign({},periodMax, {valid:false, message:'periodMaxer must be higher than periodMin'});
            return Object.assign({},inFields,{periodMin,periodMax});
        }
        else {
            periodMin= Object.assign({},periodMin, periodMin.validator(periodMin.value));
            periodMax= Object.assign({},periodMax, periodMax.validator(periodMax.value));
            return Object.assign({},inFields,{periodMin,periodMax});
        }
    }
};


function onSearchSubmit(request) {
    console.log(request);
    //if (request.Tabs==='periodfinding') {
        doPeriodFinding(request);
    //}
    //else {
    //    console.log('request no supported');
    //}
}

function doPeriodFinding(request) {
    //let tbl = getTblById(RAW_TABLE);
    console.log(request);

    var tReq = makeTblRequest('LightCurveProcessor',PERIODOGRAM , {
        'result_table': 'http://web.ipac.caltech.edu/staff/ejoliet/demo/vo-nexsci-result-sample.xml', //For now return result table for non-existing API
        'table_name': PERIODOGRAM
    },  {tbl_id:PERIODOGRAM});

    if (tReq != null) {
        dispatchTableSearch(tReq, {removable: false});
    }

    var tReq2 = makeTblRequest('LightCurveProcessor',PEAK_TABLE , {
        'result_table': 'http://web.ipac.caltech.edu/staff/ejoliet/demo/vo-nexsci-result-sample.xml', //For now return result table for non-existing API
        'table_name': PEAK_TABLE
    },  {tbl_id:PEAK_TABLE});

    if (tReq2 != null) {
        dispatchTableSearch(tReq2, {removable: false});
    }
}


