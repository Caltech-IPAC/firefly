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
import {adjustMax} from '../../ui/RangeSliderView.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {dispatchRestoreDefaults} from '../../fieldGroup/FieldGroupCntlr.js';
import {makeTblRequest,getTblById, tableToText, makeFileRequest} from '../../tables/TableUtil.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import Validate from '../../util/Validate.js';
import {RAW_TABLE, PHASE_FOLDED} from './LcManager.js';
import ReactHighcharts from 'react-highcharts';
import {cloneDeep, get, set, omit, slice, replace} from 'lodash';

const popupId = 'PhaseFoldingPopup';
const fKeyDef = { time: {fkey: 'timeCol', label: 'Time Column'},
                  flux: {fkey: 'flux', label: 'Flux Column'},
                  fluxerr: {fkey: 'fluxerror', label: 'Flux Error Column'},
                  tz: {fkey:'tzero', label: 'Zero Point Time'},
                  period: {fkey: 'period', label: 'Period (day)'}};

const pfkey = 'LC_PF_Panel';
var phaseCol = 'phase';
var fluxCol;
var fluxerrCol;
var timeColName;

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

function getTypeData(key, val='', tip = '', labelV='', labelW) {
    return {
        fieldKey: key,
        label: labelV,
        value: val,
        tooltip: tip,
        labelWidth: labelW
    };
}

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
    periodMin: Object.assign(getTypeData('periodMin', '3', 'this is a tip for period maximum'),
                            {validator: Validate.floatRange.bind(null, 0.0, Number.MAX_VALUE, 'period miniimum')}),
    periodMax: Object.assign(getTypeData('periodMax', '3', 'this is a tip for period maximum'),
                            {validator: Validate.floatRange.bind(null, 0.0, Number.MAX_VALUE, 'period maximum')}),
    periodSteps: Object.assign(getTypeData('periodSteps', '3',  'this is a tip for total period steps'),
                            {validator: Validate.intRange.bind(null, 1, 2000, 'period total step')}),
    [fKeyDef.time.fkey]: Object.assign(getTypeData(fKeyDef.time.fkey, '',
                                                    'time column, mjd, col1, or col2',
                                                    `${fKeyDef.time.label}:`, 100),
                            {validator:  (val) => {
                                let retVal = {valid: true, message: ''};
                                if (!validTimeSuggestions.includes(val)) {
                                    retVal = {valid: false, message: `${val} is not a valid time column`};
                                }
                                return retVal;
                            }}),
    [fKeyDef.flux.fkey]: Object.assign(getTypeData(fKeyDef.flux.fkey, '',
                                        'Flux column name', `${fKeyDef.flux.label}:`, 100),
                            {validator:  (val) => {
                                let retVal = {valid: true, message: ''};
                                if (!validFluxSuggestions.includes(val)) {
                                    retVal = {valid: false, message: `${val} is not a valid flux column`};
                                }
                                return retVal;
                            }}),
    [fKeyDef.fluxerr.fkey]: Object.assign(getTypeData(fKeyDef.fluxerr.fkey, '',
                                'Flux Error column name', `${fKeyDef.fluxerr.label}:`, 100),
                            {validator:  (val) => {
                                let retVal = {valid: true, message: ''};
                                if (!validFluxErrSuggestions.includes(val)) {
                                    retVal = {valid: false, message: `${val} is not a valid flux error column`};
                                }
                                return retVal;
                            }}),
    [fKeyDef.tz.fkey]: Object.assign(getTypeData(fKeyDef.tz.fkey, '', '', `${fKeyDef.tz.label}:`, 100),
                            {validator: null}),
    [fKeyDef.period.fkey]: Object.assign(getTypeData(fKeyDef.period.fkey, '', '', `${fKeyDef.period.label}:`, 100),
                            {validator: null})
};

const RES = 10;      // factor for defining total steps for period
const DEC = 3;       // decimal digit
const validTimeSuggestions = ['mjd'];      // suggestive time column name
const validFluxSuggestions = ['w1mpro_ep', 'w2mpro_ep', 'w3mpro_ep', 'w4mpro_ep'];
const validFluxErrSuggestions = ['w1sigmpro_ep', 'w2sigmpro_ep', 'w3sigmpro_ep', 'w4sigmpro_ep'];
const Margin = 0.2;                                  // left and right margin for xAxis on flux vs. phase plot
const STEP = 1;     // step for slider

var SliderMin;

var periodErr;       // error message for period setting
var timeErr;         // error message for time zero setting

var periodRange;     // period minumum and maximum
var timeZero;        // time zero
var timeMax;         // time zero maximum
var currentPhaseFoldedTable;   // table with phase column

/**
 * @summary show phase folding popup window
 * @param {string} popTitle
 * @param {string} phaseColumnName
 * @param {string[]} timeList
 * @param {string[]} fluxList
 * @param {string[]} fluxerrList
 * @param {string} timeColumnName
 * @param {string} fluxColumnName
 * @param {string} fluxerrColumnName
 */
export function showPhaseFoldingPopup(popTitle, phaseColumnName = 'phase',
                                      timeList=validTimeSuggestions,
                                      fluxList=validFluxSuggestions,
                                      fluxerrList=validFluxErrSuggestions,
                                      timeColumnName='', fluxColumnName='', fluxerrColumnName='' ) {

    timeColName = timeColumnName ? timeColumnName : timeList[0];
    fluxCol = fluxColumnName ? fluxColumnName : fluxList[0];
    fluxerrCol = fluxerrColumnName ? fluxerrColumnName : fluxerrList[0];
    phaseCol = phaseColumnName;
    periodRange = computePeriodRange(RAW_TABLE, timeColName);
    periodErr = `Period error: must be a float and not less than ${periodRange.min} (day)`;
    timeErr = `time zero error: must be a float and within [0.0, ${timeMax}]`;

    var popup = (<PopupPanel title={popTitle}>
                    {PhaseFoldingSetting()}
                    <div>
                        <CompleteButton
                            dialogId={popupId}
                            groupKey={[pfkey]}
                            onSuccess={setPFTableSuccess(true)}
                            onFail={setPFTableFail()}
                            text={'Phase Folded Table'}
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
    var width = 550;

    return (
        <div style={{display: 'flex'}}>
            <div style={{...childStyle}}>
                <LcPFOptionsBox />
           </div>
            <div style={{...childStyle}}>
                <PhaseFoldingChart height={childStyle.height} width={width}/>
            </div>
        </div>
    );
}

/**
 * @summary calculate phase and return as text
 * @param {number} time
 * @param {number} timeZero
 * @param {number} period
 * @param {number} dec
 * @returns {string}  folded phase (positive or negative) with 'dec'  decimal digits
 */
function getPhase(time, timeZero, period,  dec=DEC) {
    var q = (time - timeZero)/period;
    var p = q >= 0  ? (q - Math.floor(q)) : (q + Math.floor(-q));

    return p.toFixed(dec);
}

/**
 * @summary get flux series in terms of folded phase
 * @param {Object} fields
 * @returns {*}
 */
function getPhaseFlux(fields) {
    var rawTable = getTblById(RAW_TABLE);
    var {columns, data} = rawTable.tableData;    // column names and data

    var {timeCol, period, tzero, flux} = fields;
    const tc = timeCol ? timeCol.value : '';
    const fc = flux ? flux.value: '';
    const tIdx = tc ? columns.findIndex((c) => (c.name === tc)) : -1;  // find time column
    const fIdx = fc ? columns.findIndex((c) => (c.name === fc)) : -1;  // find flux column

    if (tIdx < 0 || fIdx < 0) return {};

    var pd, tz;

    if (period){
        if (!period.valid) return {};    // invalid period
        pd = parseFloat(period.value);
    } else {
        pd = periodRange.min;
    }
    if (tzero) {
        if (!tzero.valid) return {};      // invalid period
        tz = parseFloat(tzero.value);
    } else {
        tz = timeZero;
    }

    var phaseV = data.map((d) => {
        return parseFloat(getPhase(d[tIdx], tz, pd));
    });

    var minPhase = Math.min(...phaseV);
    var maxPhase = Math.max(...phaseV) + 1.0;

    var xy = data.reduce((prev, d, idx) => {
        var phase1, phase2, fluxV;

        fluxV = parseFloat(d[fIdx]);
        phase1 = phaseV[idx];
        phase2 = phase1 + 1.0;
        prev.push([phase1, fluxV], [phase2, fluxV]);

        return prev;
    }, []);

    return Object.assign({data: xy}, {minPhase, maxPhase, period: pd.toFixed(DEC), flux: fc});
}

/**
 * @summary 2D xyplot component on phase folding panel
 */
class PhaseFoldingChart extends Component {
    constructor(props) {
        super(props);

        var fields = FieldGroupUtils.getGroupFields(pfkey);
        var {width, height, showTooltip=true} = props;
        var {data, minPhase = 0.0, period, flux} = getPhaseFlux(fields);

        this.state = {
            fields,
            config: {
                chart: {
                    type: 'scatter',
                    borderColor: '#a5a5a5',
                    borderWidth: 1,
                    borderRadius: 5,
                    zoomType: 'xy',
                    height,
                    width
                },
                title: {
                    fontSize: '16px',
                    text: period ? `period=${period}(day)`:'period='
                },
                xAxis: {
                    min: minPhase - Margin,
                    max: minPhase + 2.0 + Margin,
                    title: {text: `${phaseCol}`},
                    gridLineWidth: 1,
                    gridLineColor: '#e9e9e9'
                },
                yAxis: {
                    title: {text: `${flux}(mag)`},
                    lineWidth: 1,
                    tickWidth: 1,
                    tickLength: 10,
                    gridLineColor: '#e9e9e9',
                    reversed: true
                },
                tooltip: showTooltip ? {enabled: true,
                                        formatter() {
                                            return ('<span>' +
                                            `${phaseCol} = ${this.x.toFixed(DEC)} <br/>` +
                                            `${flux} = ${this.y} mag <br/>` +
                                            '</span>');
                                        }}
                                     : {enabled: false},
                series: [{
                    showInLegend: false,
                    marker: {radius: 2},
                    color: 'blue',
                    data
                }],
                credits: {
                    enabled: false // removes a reference to Highcharts.com from the chart
                }
            }
        };
    }

    shouldComponentUpdate(np,ns) {
        return (this.props !== np) || (this.state.fields !== ns.fields);
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        var isPhaseChanged = (newFields) => {
            var fieldsToCheck = ['time', 'flux', 'tz', 'period'];
            var fieldsInfo = fieldsToCheck.map((f) => {
                return [get(newFields, [fKeyDef[f].fkey, 'value'], ''),
                        get(this.state.fields, [fKeyDef[f].fkey, 'value'], '')];
            });

            var idx = fieldsToCheck.findIndex((f, idx) => {
                return !fieldsInfo[idx][0] || (fieldsInfo[idx][0] !== fieldsInfo[idx][1]);
            });

            return (idx !== -1);
        };

        this.iAmMounted = true;
        var  chart = this.refs.chart.getChart();

        this.unbinder= FieldGroupUtils.bindToStore(pfkey, (fields) => {
            if (isPhaseChanged(fields, this.state.fields) && this.iAmMounted) {
                this.setState({fields});

                var {data, minPhase = 0.0, period, flux} = getPhaseFlux(fields);
                chart.setTitle({text: period ? `period=${period}(day)`:'period='});
                chart.series[0].setData(data);
                chart.xAxis[0].update({min: minPhase - Margin,
                                       max: minPhase + 2.0 + Margin});
                chart.yAxis[0].setTitle({text: `${flux}(mag)`})
            }
        });
    }

    render() {
        //console.log('rerender hicharts');
        return (
            <div>
                <ReactHighcharts config={this.state.config} isPureConfig={true} ref='chart'/>
           </div>
        );
    }

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

    const {periodMax, periodMin} = fields || {};
    const step = STEP;
    var min = periodMin ? periodMin.value : periodRange.min;
    var max = periodMax ? periodMax.value : periodRange.max;

    min = parseFloat(min.toFixed(DEC));
    max = adjustMax(max, SliderMin, step).max;

    const sRange = [SliderMin, max];
    const sliderSize = sRange[1] - sRange[0];
    const NOMark = 5;
    const INTMark = (sliderSize)/(NOMark - 1);

    function  markStyle(per, val) {
        return {style: {left: `${per}%`, marginLeft: -16, width: 40}, label: `${val}`};
    }

    var marks = Object.assign({}, {[min]: markStyle(100*(min-SliderMin)/sliderSize, min)});

    marks = [...Array(NOMark).keys()].slice(1).reduce((prev, m) => {
                var val = parseFloat((INTMark * m + sRange[0]).toFixed(DEC));
                var per = 100*(val - SliderMin)/sliderSize;

                prev = Object.assign(prev, {[val]: markStyle(per, val)});
                return prev;
            }, marks);


    //var step = periodSteps ? (max - min)/periodSteps.value : (max-min)/periodRange.steps;

    return (

        <FieldGroup style= {PanelResizableStyle} groupKey={pfkey} initValues={{timeCol:'mjd',field1:'4'}}
                    reducerFunc={LcPFReducer} keepState={true}>
            <InputGroup labelWidth={110}>

                <span style={Header}>Phase Folding</span>
                <br/><br/>
                <SuggestBoxInputField
                    fieldKey={fKeyDef.time.fkey}
                    getSuggestions = {(val)=>{
                            const suggestions = validTimeSuggestions.filter((el)=>{return el.startsWith(val);});
                            return suggestions.length > 0 ? suggestions : validTimeSuggestions;
                        }}

                />

                <br/>
                <SuggestBoxInputField
                    fieldKey={fKeyDef.flux.fkey}
                    getSuggestions = {(val)=>{
                            const suggestions = validFluxSuggestions.filter((el)=>{return el.startsWith(val);});
                            return suggestions.length > 0 ? suggestions : validFluxSuggestions;
                        }}

                />
                <br/>

                <SuggestBoxInputField
                    fieldKey={fKeyDef.fluxerr.fkey}
                    getSuggestions = {(val)=>{
                            const suggestions = validFluxErrSuggestions.filter((el)=>{return el.startsWith(val);});
                            return suggestions.length > 0 ? suggestions : validFluxErrSuggestions;
                        }}

                />
                <br/>

                <ValidationField fieldKey={fKeyDef.tz.fkey} />
                <br/>

                <RangeSlider fieldKey={fKeyDef.period.fkey}
                             min={sRange[0]}
                             max={sRange[1]}
                             minStop={min}
                             marks={marks}
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
 * @returns {function}
 */
function periodValidator(min, max, precision, description) {
    return (valStr) => {
        var retRes = Validate.floatRange(min, max, precision, description, valStr);

        if (retRes.valid) {
            if (parseFloat(valStr) < periodRange.min) {
                return {valid: false, message: description};
            }
        } else {
            retRes.message = description;
        }
        return retRes;
    };
}

/**
 * @summary time zero validator
 * @param {number} min
 * @param {number} max
 * @param {number} precision
 * @param {string} description
 * @returns {function}
 */
function timezeroValidator(min, max, precision, description) {
    return (valStr) => {
        var retRes = Validate.floatRange(min, max, precision, description, valStr);

        if (retRes.valid) {
            if (!valStr || parseFloat(valStr) > timeMax) {  // empty string or not in range
                return {
                    valid: false,
                    message: description
                };
            }
        } else {
            retRes.message = description;
        }
        return retRes;
    };
}

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
        set(defV, [fKeyDef.time.fkey, 'value'], timeColName);
        set(defV, [fKeyDef.flux.fkey, 'value'], fluxCol);
        set(defV, [fKeyDef.fluxerr.fkey, 'value'], fluxerrCol);
        set(defV, [fKeyDef.period.fkey, 'value'],`${periodRange.min}`);
        set(defV, [fKeyDef.tz.fkey, 'value'],`${timeZero}`);

        set(defV, [fKeyDef.period.fkey, 'tooltip'], `Period for phase folding, within [${periodRange.min} (i.e. 1 sec), ${periodRange.max}(suggestion)]`);
        set(defV, [fKeyDef.tz.fkey, 'tooltip'], `time zero, within [0.0, ${timeMax}`);

        set(defV, [fKeyDef.tz.fkey, 'validator'], timezeroValidator( 0.0, Number.MAX_VALUE, 8, timeErr));
        set(defV, [fKeyDef.period.fkey, 'validator'], periodValidator(0.0, Number.MAX_VALUE, 8, periodErr));

        set(defV, [fKeyDef.period.fkey, 'errMsg'], periodErr);
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
 * @param {TableModel} phaseTable
 */
function repeatDataCycle(timeCol, period, phaseTable) {
    var tIdx = phaseTable.tableData.columns.findIndex((c) => (c.name === timeCol));
    var fIdx = phaseTable.tableData.columns.findIndex((c) => (c.name === phaseCol));
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
        doPFCalculate();
        var reqData = get(request, pfkey);
        var timeName = get(reqData, fKeyDef.time.fkey);
        var period = get(reqData, fKeyDef.period.fkey);
        var flux = get(reqData, fKeyDef.flux.fkey);

        repeatDataCycle(timeName, parseFloat(period), currentPhaseFoldedTable);

        const {tableData, tbl_id, tableMeta, title} = currentPhaseFoldedTable;
        const ipacTable = tableToText(tableData.columns, tableData.data, true, tableMeta);
        const file = new File([new Blob([ipacTable])], `${tbl_id}.tbl`);

        doUpload(file).then(({status, cacheKey}) => {
            const tReq = makeFileRequest(title, cacheKey, null, {tbl_id});
            dispatchTableSearch(tReq, {removable: false});

            let xyPlotParams = {
                userSetBoundaries: {xMax: 2},
                x: {columnOrExpr: phaseCol, options: 'grid'},
                y: {columnOrExpr: flux, options: 'grid,flip'}
            };
            loadXYPlot({chartId: PHASE_FOLDED, tblId: PHASE_FOLDED, xyPlotParams});
        });

        if (hideDropDown) {
            dispatchHideDropDown(popupId);
        }
    };
}

/**
 * @summary callback when there is failure on clicking complete button
 * @returns {Function}
 */
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
        'period_days': fields&&get(fields, 'period.value'),
        'table_name': 'folded_table',
        'time_col_name': fields&&get(fields, 'timeCol.value'),
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
    var {columns} = tbl.tableData;
    var tc = timeCol ? timeCol.value : 'mjd';
    var tIdx = tc ? columns.findIndex((c) => (c.name === tc)) : -1;

    if (tIdx < 0) return null;

    var pd = period ? parseFloat(period.value) : periodRange.min;
    var tz = tzero ? parseFloat(tzero.value) : timeZero;
    var tPF = Object.assign(cloneDeep(tbl), {tbl_id: PHASE_FOLDED, title: 'Phase Folded'},
                                            {request: getPhaseFoldingRequest(fields, tbl)},
                                            {highlightedRow: 0});
    tPF = omit(tPF, ['hlRowIdx', 'isFetching']);

    var phaseC = {desc: 'number of period elapsed since starting time.',
                  name: phaseCol, type: 'double', width: 20 };

    tPF.tableData.columns.push(phaseC);          // add phase column

    tPF.tableData.data.forEach((d, index) => {   // add phase value (in string) to each data

        tPF.tableData.data[index].push(getPhase(parseFloat(d[tIdx]), tz, pd));
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
    var tIdx = columns.findIndex((col) => (col.name === timeColName));
    var arr = data.reduce((prev, e)=> {
        prev.push(parseFloat(e[tIdx]));
        return prev;
    }, []);
    var factor = 1;

    timeZero = Math.min(...arr);
    timeMax = Math.max(...arr);

    var max = parseFloat(((timeMax - timeZero)*factor).toFixed(DEC));
    var min = Math.pow(10, -DEC);

    SliderMin = 0.0;
    var newMax = adjustMax(max, SliderMin, STEP);

    return {min, max: newMax.max, steps: newMax.steps};
}