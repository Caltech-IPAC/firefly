/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import './LCPanels.css';
import React, {Component} from 'react';

import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import Validate from '../../util/Validate.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {makeTblRequest,getTblById} from '../../tables/TableUtil.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {dispatchRestoreDefaults} from '../../fieldGroup/FieldGroupCntlr.js';
import {LC} from '../../templates/lightcurve/LcManager.js';

const gkey = 'PFO_PANEL';
const options = [
    {label: 'Lomb‑Scargle ', value: 'ls', proj: 'LCViewer'}
];
const stepoptions = [
    {label: 'Fixed Frequency', value: 'fixedf', proj: 'LCViewer'},
    {label: 'Fixed Period', value: 'fixedp', proj: 'LCViewer'},
    {label: 'Exponential', value: 'exp', proj: 'LCViewer'},
    {label: 'Plavchan', value: 'plav', proj: 'LCViewer'}

];


const defValues = {
    periodMin: {
        fieldKey: 'periodMin',
        //value: '1',
        forceReinit: true,
        validator: Validate.floatRange.bind(null, 10e-10, 1000000, 'periodMin field'),
        tooltip: 'this is a tip for low field',
        label: 'Minimum Period:',
        labelWidth: 150
    },
    periodMax: {
        fieldKey: 'periodMax',
        //value: '3',
        forceReinit: true,
        validator: Validate.floatRange.bind(null, 10e-10, 1000000, 'periodMax field'),
        tooltip: 'this is a tip for periodMax field',
        label: 'Maximum Period:',
        labelWidth: 150
    },
    x: {
        fieldKey: 'x',
        value: 'mjd',
        forceReinit: true,
        label: 'Time column:',
        //validator: Validate.intRange.bind(null, 1, 100, 'periodMax field'),
        labelWidth: 150
    },
    y: {
        fieldKey: 'y',
        value: 'w1mpro_ep',
        forceReinit: true,
        label: 'Flux column:',
        //validator: Validate.intRange.bind(null, 1, 100, 'periodMax field'),
        labelWidth: 150
    },
    peaks: {
        fieldKey: 'peaks',
        value: '50',
        label: 'N peaks:',
        forceReinit: true,
        validator: Validate.intRange.bind(null, 1, 500, 'peaks number field'),
        labelWidth: 150
    },
    periodAlgor: {
        fieldKey: 'periodAlgor',
        value: 'ls',
        label: 'Algorithm:',
        forceReinit: true,
        labelWidth: 150
    }

};

export class PeriodogramOptionsPanel extends Component {

    constructor(props) {
        super(props);
        this.state = {fields: FieldGroupUtils.getGroupFields(gkey)};
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        if (this.unbinder) this.unbinder();
        //if (this.removeListener) this.removeListener();
        //this.iAmMounted= false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.unbinder = FieldGroupUtils.bindToStore(gkey, (fields) => {
            if (fields !== this.state.fields && this.iAmMounted) {
                this.setState({fields});
            }
            //this.iAmMounted= true;
            //this.removeListener= FieldGroupUtils.bindToStore(gkey, (fields) => {
            //    if (this.iAmMounted) this.setState(fields);
        });
    }

    render() {
        // var fields = this.state;
        return (
            <LcPeriodFindingPanel />
        );
    }


}

/**
 *
 * @returns {XML}
 * @constructor
 */
export function LcPeriodFindingPanel() {
    return (
        <div style={{padding:5}}>
            <FieldGroup groupKey={gkey} reducerFunc={periodogramRangeReducer} keepState={true}>
                <InputGroup labelWidth={150}>
                    <ListBoxInputField initialState={{
                              tooltip: 'Select Algorithm',
                              label : 'Algorithm:'
                       }}
                                       options={options }
                                       multiple={false}
                                       fieldKey='periodAlgor'
                    />
                    <br/>
                    <span> <b>Time: </b> </span>
                    <br/><br/>
                    <ValidationField fieldKey='x'
                                     tooltip='Enter Time column name'
                                     label='Time:'
                    />
                    <br/>
                    <span> <b>Flux: </b> </span>
                    <br/><br/>
                    <ValidationField fieldKey='y'
                                     tooltip='Enter Flux column name'
                                     label='Flux:'
                    />
                    <br/>
                    <span> <b>Period Range: </b> </span>
                    <br/><br/>
                    <ValidationField fieldKey='periodMin'/>
                    <br/>
                    <ValidationField fieldKey='periodMax'/>
                    <br/>
                    <span> <b>Period Step Method: </b></span>
                    <br/> <br/>
                    <ListBoxInputField initialState={{
                              tooltip: 'Period Step Method',
                              label : 'Select Method:'
                           }}
                                       options={stepoptions }
                                       multiple={false}
                                       fieldKey='stepMethod'
                    />
                    <br/>
                    <ValidationField fieldKey='stepSize'
                                     tooltip='Enter Step Size'
                                     label='Step Size:'
                    />
                    <br/>
                    <span> <b>Peaks: </b></span>
                    <br/> <br/>
                    <ValidationField fieldKey='peaks'
                                     tooltip='Enter Number of Peaks'
                                     label='Number of Peaks:'
                    />
                    <br/>
                    <button type='button' className='button std hl' onClick={(request) => onSearchSubmit(request)}>
                        <b>Period Finding</b>
                    </button>
                    <button type='button' className='button std hl' onClick={() => resetDefaults()}>
                        <b>Reset</b>
                    </button>

                </InputGroup>
            </FieldGroup>
            <br/>


        </div>

    );
};

/**
 *
 * @param {object} inFields
 * @param {object} action
 * @return {object}
 */
var periodogramRangeReducer = function (inFields, action) {
    if (!inFields) {
        return defValues;
    }
    else {
        var {periodMin,periodMax}= inFields;
        // inFields= revalidateFields(inFields);
        if (!periodMin.valid || !periodMax.valid) {
            return inFields;
        }
        if (parseFloat(periodMin.value) > parseFloat(periodMax.value)) {
            periodMin = Object.assign({}, periodMin, {
                valid: false,
                message: ' periodMin must be lower than periodMax'
            });
            periodMax = Object.assign({}, periodMax, {
                valid: false,
                message: 'periodMaxer must be higher than periodMin'
            });
            return Object.assign({}, inFields, {periodMin, periodMax});
        }
        else {
            periodMin = Object.assign({}, periodMin, periodMin.validator(periodMin.value));
            periodMax = Object.assign({}, periodMax, periodMax.validator(periodMax.value));
            return Object.assign({}, inFields, {periodMin, periodMax});
        }
    }
};


function onSearchSubmit(request) {
    //console.log(request);
    doPeriodFinding(request);
}

function doPeriodFinding(request) {
    const tbl = getTblById(LC.RAW_TABLE);
    const fields = FieldGroupUtils.getGroupFields(gkey);
    const srcFile = tbl.request.source;
    //console.log(fields);

    var tReq2 = makeTblRequest('LightCurveProcessor', LC.PEAK_TABLE, {
        original_table: srcFile,
        x: fields.x.value || 'mjd',
        y: fields.y.value || 'w1mpro_ep',
        alg: fields.periodAlgor.value,
        pmin: fields.periodMin.value,
        pmax: fields.periodMax.value,
        step_method: fields.stepMethod.value,
        step_size: fields.stepSize.value,
        peaks: fields.peaks.value,
        table_name: LC.PEAK_TABLE,
        sortInfo: sortInfoString('SDE')                 // sort peak table by column SDE
    }, {tbl_id: LC.PEAK_TABLE});

    if (tReq2 !== null) {
        const xyPlotParams = {
            x: {columnOrExpr: LC.PEAK_CNAME, options: 'grid'},
            y: {columnOrExpr: LC.POWER_CNAME, options: 'grid'}
        };
        loadXYPlot({chartId: LC.PEAK_TABLE, tblId: LC.PEAK_TABLE, xyPlotParams});
        dispatchTableSearch(tReq2, {removable: true});
    }

    var tReq = makeTblRequest('LightCurveProcessor', LC.PERIODOGRAM, {
        original_table: srcFile,
        x: fields.x.value || 'mjd',
        y: fields.y.value || 'w1mpro_ep',
        alg: fields.periodAlgor.value,
        pmin: fields.periodMin.value,
        pmax: fields.periodMax.value,
        step_method: fields.stepMethod.value,
        step_size: fields.stepSize.value,
        peaks: fields.peaks.value,
        table_name: LC.PERIODOGRAM
    }, {tbl_id: LC.PERIODOGRAM});

    if (tReq !== null) {
        dispatchTableSearch(tReq, {removable: true});
        const xyPlotParams = {
            userSetBoundaries: {yMin: 0},
            x: {columnOrExpr: LC.PERIOD_CNAME, options: 'grid,log'},
            y: {columnOrExpr: LC.POWER_CNAME, options: 'grid'}
        };
        loadXYPlot({chartId: LC.PERIODOGRAM, tblId: LC.PERIODOGRAM, markAsDefault: true, xyPlotParams});
    }
}

function resetDefaults() {
    dispatchRestoreDefaults(gkey);

}
