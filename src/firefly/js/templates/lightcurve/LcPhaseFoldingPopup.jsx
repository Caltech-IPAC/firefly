/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {doUpload} from '../../ui/FileUpload.jsx';
import {RangeSlider}  from '../../ui/RangeSlider.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {dispatchRestoreDefaults} from '../../fieldGroup/FieldGroupCntlr.js';
import {makeTblRequest,getTblById, tableToText, makeFileRequest} from '../../tables/TableUtil.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import Validate from '../../util/Validate.js';
import {LcPFOptionsPanel} from './LcPhaseFoldingPanel.jsx';
import {RAW_TABLE, PHASE_FOLDED} from './LcManager.js';
import {cloneDeep, get, set, omit, slice, replace, isNil} from 'lodash';

const popupId = 'PhaseFoldingPopup';
const fKeyDef = { time: {fkey: 'timeCol', label: 'Time Column'},
                  flux: {fkey: 'flux', label: 'Flux Column'},
                  fluxerr: {fkey: 'fluxerror', label: 'Flux Error'},
                  tz: {fkey:'tzero', label: 'Zero Point Time'},
                  period: {fkey: 'period', label: 'Period (day)'},
                  periodslider: {fkey: 'periodslider', label: ''}};
const pfkey = 'LC_PF_Panel';

const PanelResizableStyle = {
    width: 400,
    minWidth: 400,
    height: 300,
    minHeight: 300,
    marginLeft: 15,
    overflow: 'auto',
    padding: '2px'
};

const Header = {
    whiteSpace: 'nowrap',
    height: 'calc(100% - 1px)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'left',
    justifyContent: 'space-between',
    padding: '2px',
    fontSize: 'large'
};

const defValues= {
    fieldInt: {
        fieldKey: 'fieldInt',
        value: '3',
        validator: Validate.intRange.bind(null, 1, 10, 'test field'),
        tooltip: 'this is a tip for field 1',
        label: 'Int Value:'
    },
    FluxColumn: {
        fieldKey: 'fluxcolumn',
        value: '',
        validator: Validate.floatRange.bind(null, 0.2, 20.5, 2, 'Flux Column'),
        tooltip: 'Flux Column',
        label: 'Flux Column:',
        nullAllowed : true,
        labelWidth: 100
    },
    field1: {
        fieldKey: 'field1',
        value: '3',
        validator: Validate.intRange.bind(null, 1, 10, 'test field'),
        tooltip: 'this is a tip for field 1',
        label: 'Int Value:'
    },
    field2: {
        fieldKey: 'field2',
        value: '',
        validator: Validate.floatRange.bind(null, 1.5, 50.5, 2, 'a float field'),
        tooltip: 'field 2 tool tip',
        label: 'Float Value:',
        nullAllowed : true,
        labelWidth: 100
    },
    low: {
        fieldKey: 'low',
        value: '1',
        validator: Validate.intRange.bind(null, 1, 100, 'low field'),
        tooltip: 'this is a tip for low field',
        label: 'Low Field:'
    },
    high: {
        fieldKey: 'high',
        value: '3',
        validator: Validate.intRange.bind(null, 1, 100, 'high field'),
        tooltip: 'this is a tip for high field',
        label: 'High Field:'
    },
    periodMin: {
        fieldKey: 'periodMin',
        value: '3',
        validator: Validate.floatRange.bind(null, 0.0, 365.0, 'period minimum'),
        tooltip: 'this is a tip for period minumum',
        label: ''
    },
    periodMax: {
        fieldKey: 'periodMax',
        value: '3',
        validator: Validate.floatRange.bind(null, 0.0, 365.0, 'period maximum'),
        tooltip: 'this is a tip for period maximum',
        label: ''
    },
    periodSteps: {
        fieldKey: 'periodSteps',
        value: '3',
        validator: Validate.intRange.bind(null, 1, 2000, 'period total step'),
        tooltip: 'this is a tip for total period steps',
        label: ''
    }
};

const RES = 10;
const DEC = 8;

var periodRange;
var timeZero;
var timeMax;
var currentPhaseFoldedTable;

/**
 * @summary show phase folding popup window
 * @param {string} popTitle
 */

export function showPhaseFoldingPopup(popTitle) {
    var fields = FieldGroupUtils.getGroupFields(pfkey);
    var timeColName = get(fields, [fKeyDef.time.fkey, 'value'], 'mjd');

    periodRange = computePeriodRange(RAW_TABLE, timeColName);

    var popup = (<PopupPanel title={popTitle}>
                    {PhaseFoldingSetting()}
                    <div>
                        <CompleteButton
                            dialogId={popupId}
                            groupKey={[pfkey]}
                            onSuccess={setPFTableSuccess(true)}
                            onFail={setPFTableFail()}
                            text={'Phase Folding Table'}
                        />
                    </div>
                 </PopupPanel>);

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}

/**
 * @summary Phase folding pannel component
 * @returns {XML}
 * @constructor
 */
function PhaseFoldingSetting() {
    var border = '1px solid #a3aeb9';
    var borderRadius = '5px';
    var childStyle = {border, borderRadius, height: 350, margin: 5, padding: 2};

    return (
        <div style={{display: 'flex'}}>
            <div style={{...childStyle}}>
                <LcPFOptionsBox />
           </div>
            <div style={{...childStyle, width: 300}}> XY plot</div>
        </div>
    );
}

/**
 * @summary Phase folding panel component for setting parameters
 */
class LcPFOptionsBox extends Component {
    constructor(props) {
        super(props);
        this.state = {fields: FieldGroupUtils.getGroupFields(pfkey)};
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {
        this.iAmMounted= true;
        this.unbinder= FieldGroupUtils.bindToStore(pfkey, (fields) => {
            if (fields!==this.state.fields && this.iAmMounted) {
                this.setState({fields});
            }
        });
    }

    render() {
        var {fields} = this.state;

        return (
          <div>
              <LcPFOptions fields={fields}/>
          </div>
        );

    }
}

function LcPFOptions({fields}) {
    const validSuggestions = ['mjd','col1','col2'];
    const {periodMax, periodMin, periodSteps, tzero} = fields || {};
    var   sRange, step, min, max, zeroT;

    min = periodMin ? periodMin.value : periodRange.min;
    max = periodMax ? periodMax.value : periodRange.max;
    zeroT = tzero ? tzero.value : timeZero;

    sRange = [0.0, max];
    step = periodSteps ? (max - min)/periodSteps.value : (max-min)/periodRange.steps;

    const periodErr = `Period error: must be a float and not less than ${periodRange.min} (day)`;
    const timeErr = `time zero error: must be a float and within [${timeZero}, ${timeMax}]`;

    return (

        <FieldGroup style= {PanelResizableStyle} groupKey={pfkey} initValues={{timeCol:'mjd',field1:'4'}}
                    reducerFunc={LcPFReducer} keepState={true}>
            <InputGroup labelWidth={110}>

                <span style={Header}>Phase Folding</span>
                <br/><br/>
                <SuggestBoxInputField
                    fieldKey={fKeyDef.time.fkey}
                    initialState= {{
                            fieldKey: fKeyDef.time.fkey,
                            value: 'mjd',
                            validator:  (val) => {
                                let retVal = {valid: true, message: ''};
                                if (!validSuggestions.includes(val)) {
                                    retVal = {valid: false, message: `${val} is not a valid time column`};
                                }
                                return retVal;
                            },
                            tooltip: 'time column, mjd, col1, or col2',
                            label : `${fKeyDef.time.label}:`,
                            labelWidth : 100
                        }}
                    getSuggestions = {(val)=>{
                            const suggestions = validSuggestions.filter((el)=>{return el.startsWith(val);});
                            return suggestions.length > 0 ? suggestions : validSuggestions;
                        }}

                />

                <br/>
                <ValidationField fieldKey={fKeyDef.flux.fkey}
                                 forceReinit={true}
                                 initialState= {{
                                  fieldKey: fKeyDef.flux.fkey,
                                  value: '2.0',
                                  validator: floatValidator.bind(null, 2.0, 5.5, 3,'Flux Column error'),
                                  tooltip: 'Flux Column, between 2.0 to 5.5',
                                  label : `${fKeyDef.flux.label}:`,
                                  labelWidth : 100
                          }} />

                <br/>

                <ValidationField fieldKey={fKeyDef.fluxerr.fkey}
                                 forceReinit={true}
                                 initialState= {{
                                  fieldKey: fKeyDef.fluxerr.fkey,
                                  value: '0.02',
                                  validator: floatValidator.bind(null, 0.01, 0.5, 3,'Flux Error'),
                                  tooltip: 'Flux Error, between 0.01 to 0.5',
                                  label : `${fKeyDef.fluxerr.label}:`,
                                  labelWidth : 100
                         }} />
                <br/>

                <ValidationField fieldKey={fKeyDef.tz.fkey}
                                 forceReinit={true}
                                 initialState= {{
                                  fieldKey: fKeyDef.tz.fkey,
                                  value: `${zeroT}`,
                                  validator: timezeroValidator.bind(null, 0.0, Number.MAX_VALUE, 8, timeErr),
                                  tooltip: `time zero, within [${timeZero}, ${timeMax}]`,
                                  label : `${fKeyDef.tz.label}:`,
                                  labelWidth : 100
                         }} />
                <br/>

                <RangeSlider fieldKey={fKeyDef.period.fkey}
                             initialState= {{
                                  fieldKey: fKeyDef.period.fkey,
                                  value: `${min}`,
                                  label: `${fKeyDef.period.label}:`,
                                  validator: periodValidator.bind(null, 0.0, Number.MAX_VALUE, 8, periodErr),
                                  tooltip: `Period for phase folding, within [${periodRange.min} (i.e. 1 sec), ${periodRange.max}(suggestion)]`,
                                  labelWidth: 100,
                                  errMsg: periodErr
                             }}
                             onValueChange={() => doPFCalculate()}
                             min={sRange[0]}
                             max={sRange[1]}
                             minStop={min}
                             marks={{[min]: {style: { left: '0%', marginLeft: -20, width: 40}, label: `${min}`},
                                     [sRange[1]]: {style: { left: '100%', marginLeft: -20, width: 40}, label: `${sRange[1]}`}}}
                             step={step}
                             defaultValue={min}
                             wrapperStyle={{marginBottom: 20, width: PanelResizableStyle.width*4/5}}
                             sliderStyle={{marginLeft: 10, marginTop: 20}}
                />

                <br/> <br/>

                <button type='button' className='button std hl'  onClick={() => resetDefaults()}>
                    <b>Reset</b>
                </button>

                <br/>
            </InputGroup>
        </FieldGroup>
    );
}

LcPFOptions.propTypes = {
    fields: PropTypes.object
};

/**
 * @summary validator for periopd value
 * @param {number} min
 * @param {number} max
 * @param {number} precision
 * @param {string} description
 * @param {string} valStr
 * @returns {{valid: boolean, message: string}}
 */
var periodValidator = function(min, max, precision, description, valStr) {
    var retRes = Validate.floatRange(min, max, precision,  description, valStr);

    if (retRes.valid) {
        if (parseFloat(valStr) < periodRange.min) {
            return {valid: false, message: description};
        }
    } else {
        retRes.message = description;
    }
    return retRes;
};


/**
 * @summary validator for float value string, empty string is invalid
 * @param {number} min
 * @param {number} max
 * @param {number} precision
 * @param {string} description
 * @param {string} valStr
 * @returns {{valid: boolean, message: *}}
 */
var floatValidator = function(min, max, precision, description, valStr) {
    var retRes = Validate.floatRange(min, max, precision,  description, valStr);

    if (retRes.valid) {
        if (!valStr) {    // empty string
            return {valid: false,  message: `${description}: not specified`};
        }
    }
    return retRes;
};

/**
 * @summary time zero validator
 * @param {number} min
 * @param {number} max
 * @param {number} precision
 * @param {string} description
 * @param {string} valStr
 * @returns {{valid: boolean, message: string}}
 */
var timezeroValidator = function(min, max, precision, description, valStr) {
    var retRes = Validate.floatRange(min, max, precision,  description, valStr);

    if (retRes.valid) {
        if (!valStr || parseFloat(valStr) < timeZero || parseFloat(valStr) > timeMax) {  // empty string or not in range
            return {valid: false,
                    message: description};
        }
    } else {
        retRes.message = description;
    }
    return retRes;
};

/**
 * @summary phase folding parameter reducer  // TODO: not sure how this works
 * @param {object} inFields
 * @param {object} action
 * @return {object}
 */
var LcPFReducer= function(inFields, action) {
    if (!inFields)  {
        var defV = Object.assign({}, defValues);
        defV.periodMin.value = periodRange.min;
        defV.periodMax.value = periodRange.max;
        defV.periodSteps.value = periodRange.steps;

        return defV;
    }
    else {
        var {low,high}= inFields;
        // inFields= revalidateFields(inFields);
        if (!low.valid || !high.valid) {
            return inFields;
        }
        if (parseFloat(low.value)> parseFloat(high.value)) {
            low= Object.assign({},low, {valid:false, message:'must be lower than high'});
            high= Object.assign({},high, {valid:false, message:'must be higher than low'});
            return Object.assign({},inFields,{low,high});
        }
        else {
            low= Object.assign({},low, low.validator(low.value));
            high= Object.assign({},high, high.validator(high.value));
            return Object.assign({},inFields,{low,high});
        }
    }
};

/**
 * @summary reset the setting
 */
function resetDefaults() {
    dispatchRestoreDefaults(pfkey);
}

/**
 * @summary duplicate the phase cycle to the phase folded table
 * @param {string} timeCol
 * @param {number} period
 * @param (TableModel} phaseTable
 */
function repeatDataCycle(timeCol, period, phaseTable) {
    var tIdx = phaseTable.tableData.columns.findIndex((c) => (c.name === timeCol));
    var fIdx = phaseTable.tableData.columns.findIndex((c) => (c.name === 'phase'));
    var {totalRows, tbl_id, title} = phaseTable;


    slice(phaseTable.tableData.data, 0, totalRows).forEach((d) => {
        var newRow = slice(d);

        newRow[tIdx] = `${parseFloat(d[tIdx]) + period}`;
        newRow[fIdx] = `${parseFloat(d[fIdx]) + 1}`;
        phaseTable.tableData.data.push(newRow);
    });

    totalRows *= 2;

    set(phaseTable, 'totalRows', totalRows);
    set(phaseTable, ['tableMeta', 'RowsRetrieved'], `${totalRows}`);

    set(phaseTable, ['tableMeta', 'tbl_id'], tbl_id);
    set(phaseTable, ['tableMeta', 'title'], title);

    var col = phaseTable.tableData.columns.length;
    var sqlMeta = get(phaseTable, ['tableMeta', 'SQL']);
    if (sqlMeta) {
        set(phaseTable, ['tableMeta', 'SQL'], replace(sqlMeta, `${col-1}`, `${col}`));
    }

}

/**
 * @summary upload the table with phase column to the server and display the table and the xy chart
 * @param {boolean} hideDropDown
 * @returns {Function}
 */
function setPFTableSuccess(hideDropDown = false) {
    return (request) => {
        var reqData = get(request, pfkey);
        var timeName = get(reqData, fKeyDef.time.fkey, 'mjd');
        var period = get(reqData, fKeyDef.period.fkey);

        if (!currentPhaseFoldedTable) {
            doPFCalculate();
        }

        repeatDataCycle(timeName, parseFloat(period), currentPhaseFoldedTable);

        const {tableData, tbl_id, tableMeta, title} = currentPhaseFoldedTable;
        const ipacTable = tableToText(tableData.columns, tableData.data, true, tableMeta);
        const file = new File([new Blob([ipacTable])], `${tbl_id}.tbl`);

        doUpload(file).then(({status, cacheKey}) => {
            const tReq = makeFileRequest(title, cacheKey, null, {tbl_id});
            dispatchTableSearch(tReq, {removable: false});

            let xyPlotParams = {
                userSetBoundaries: {xMax: 2},
                x: {columnOrExpr: 'phase', options: 'grid'},
                y: {columnOrExpr: 'w1mpro_ep', options: 'grid'}
            };
            loadXYPlot({chartId: PHASE_FOLDED, tblId: PHASE_FOLDED, xyPlotParams});
        });

        if (hideDropDown) {
            dispatchHideDropDown(popupId);
        }
    };
}

function setPFTableFail() {
    return (request) => {
        return showInfoPopup('Phase folding parameter setting error');
    };
}

/**
 * @summary callback to add phase column to raw table on slider's change
 */
function doPFCalculate() {
    let fields = FieldGroupUtils.getGroupFields(pfkey);

    let rawTable = getTblById(RAW_TABLE);
    currentPhaseFoldedTable = rawTable && addPhaseToTable(rawTable, fields);
}


/**
 * @summary create table request object for phase folded table
 * @param {Object} fields
 * @param {TableModel} tbl
 * @returns {TableRequest}
 */
function getPhaseFoldingRequest(fields, tbl) {
    return makeTblRequest('PhaseFoldedProcessor', PHASE_FOLDED, {
        'period_days': fields.period.value,
        'table_name': 'folded_table',
        'time_col_name':fields.timeCol.value,
        'original_table': tbl.tableMeta.tblFilePath
    },  {tbl_id:PHASE_FOLDED});

}
/**
 * @summary create a table model with phase column
 * @param {TableModel} tbl
 * @param {Object} fields
 * @returns {TableModel}
 */
function addPhaseToTable(tbl, fields) {
    var {timeCol, period, tzero} = fields;
    var {columns, data} = tbl.tableData;
    var tIdx = timeCol&&timeCol.value ? columns.findIndex((c) => (c.name === timeCol.value)) : -1;

    if (tIdx < 0 || !tzero || !period)  return null;
    var pd = parseFloat(period.value);
    var tz = parseFloat(tzero.value);


    var tPF = Object.assign(cloneDeep(tbl), {tbl_id: PHASE_FOLDED, title: 'Phase Folded'},
                                            {request: getPhaseFoldingRequest(fields, tbl)},
                                            {highlightedRow: 0});
    tPF = omit(tPF, ['hlRowIdx', 'isFetching']);

    var phaseC = {desc: 'number of period elapsed since starting time.',
                  name: 'phase', type: 'double', width: 20 };

    tPF.tableData.columns.push(phaseC);          // add phase column

    tPF.tableData.data.forEach((d, index) => {   // add phase value (in string) to each data
        var t = d[tIdx];
        var q = (t - tz)/pd;
        var p = q >= 0  ? (q - Math.floor(q)) : (q + Math.floor(-q));

        tPF.tableData.data[index].push(`${p.toFixed(DEC)}`);
    });

    return tPF;
}

/**
 * @summary compute the period minimum and maximum based on table data
 * @param {string} tbl_id
 * @param {string} timeColName
 * @returns {Object}
 */
function computePeriodRange(tbl_id, timeColName) {
    var tbl = getTblById(tbl_id);
    var {columns, data} = tbl.tableData;
    var minTime = Number.MAX_VALUE;
    var maxTime = 0;
    var tIdx;
    var min, max;

    tIdx = columns.findIndex((col) => (col.name === timeColName));

    let arr=[]; data.map((e)=> arr.push(e[tIdx]));
    minTime = Math.min(...arr);
    maxTime = Math.max(...arr);

    timeZero = minTime;
    timeMax = maxTime;
    max =  parseFloat(((maxTime - minTime) * 2).toFixed(DEC));
    min = parseFloat((1.0/86400).toFixed(DEC));    // set one second as the minimum value for period

    return {min, max, steps: data.length*RES};
}