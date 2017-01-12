/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';

import sCompare from 'react-addons-shallow-compare';
import {cloneDeep, get, set, omit, slice, replace, pick} from 'lodash';
import SplitPane from 'react-split-pane';
import {flux} from '../../Firefly.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FormPanel} from './../../ui/FormPanel.jsx';
import {DEC_PHASE} from '../../ui/RangeSliderView.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {doUpload} from '../../ui/FileUpload.jsx';
import {RangeSlider}  from '../../ui/RangeSlider.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {createContentWrapper} from '../../ui/panel/DockLayoutPanel.jsx';
import Validate from '../../util/Validate.js';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {dispatchRestoreDefaults} from '../../fieldGroup/FieldGroupCntlr.js';
import {makeTblRequest,getTblById, tableToText, makeFileRequest} from '../../tables/TableUtil.js';
import {LC} from './LcManager.js';
import {LcPeriodogram, pKeyDef} from './LcPeriodogram.jsx';
import {dispatchLayoutDisplayMode, LO_VIEW, getLayouInfo} from '../../core/LayoutCntlr.js';
import ReactHighcharts from 'react-highcharts';

const pfinderkey = LC.PERIOD_FINDER;



export function getTypeData(key, val='', tip = '', labelV='', labelW) {
    return {
        fieldKey: key,
        label: labelV,
        value: val,
        tooltip: tip,
        labelWidth: labelW
    };
}

const cPeriodKeyDef = {
    time: {fkey: 'time', label: 'Time Column'},
    flux: {fkey: 'flux', label: 'Flux Column'},
    min: {fkey: 'periodMin', label: 'Period Min'},
    max: {fkey: 'periodMax', label: 'Period Max'},
    timecols: {fkey: 'timeCols', label: ''},
    fluxcols: {fkey: 'fluxCols', label: ''}
};

const fKeyDef = Object.assign({}, cPeriodKeyDef,
                                {tz: {fkey: 'tzero', label: 'Zero Point Time'},
                                period: {fkey: 'period', label: 'Period (day)'},
                                periodsteps: {fkey: 'periodSteps', label: ''},
                                tzmax: {fkey: 'tzeroMax', label: ''}});

const STEP = 1;     // step for slider
export const validTimeSuggestions = ['mjd'];      // suggestive time column name
export const validFluxSuggestions = ['w1mpro_ep', 'w2mpro_ep', 'w3mpro_ep', 'w4mpro_ep'];
const Margin = 0.2;
const DEC = 8;       // decimal digit

const defValues= {
    [fKeyDef.time.fkey]: Object.assign(getTypeData(fKeyDef.time.fkey, '',
        'time column name',
        `${fKeyDef.time.label}:`, 100),
        {validator:  (val) => {
            let retVal = {valid: true, message: ''};
            if (!validTimeSuggestions.includes(val)) {
                retVal = {valid: false, message: `${val} is not a valid time column`};
            }
            return retVal;
        }}),
    [fKeyDef.flux.fkey]: Object.assign(getTypeData(fKeyDef.flux.fkey, '',
        'flux column name', `${fKeyDef.flux.label}:`, 100),
        {validator:  (val) => {
            let retVal = {valid: true, message: ''};
            if (!validFluxSuggestions.includes(val)) {
                retVal = {valid: false, message: `${val} is not a valid flux column`};
            }
            return retVal;
        }}),
    [fKeyDef.tz.fkey]: Object.assign(getTypeData(fKeyDef.tz.fkey, '', 'zero time', `${fKeyDef.tz.label}:`, 100),
        {validator: null}),
    [fKeyDef.tzmax.fkey]: Object.assign(getTypeData(fKeyDef.tzmax.fkey, '', 'maximum for zero time'),
        {validator: null}),
    [fKeyDef.min.fkey]: Object.assign(getTypeData(fKeyDef.min.fkey, '', 'minimum period', `${fKeyDef.min.label}:`, 50),
        {validator: null}),
    [fKeyDef.max.fkey]: Object.assign(getTypeData(fKeyDef.max.fkey, '', 'maximum period', `${fKeyDef.max.label}:`, 50),
        {validator: null}),
    [fKeyDef.period.fkey]: Object.assign(getTypeData(fKeyDef.period.fkey, '', '', `${fKeyDef.period.label}:`, 100),
        {validator: null}),
    [fKeyDef.timecols.fkey]: Object.assign(getTypeData(fKeyDef.timecols.fkey, [],  'time column suggestion'),
        {validator: null}),
    [fKeyDef.fluxcols.fkey]: Object.assign(getTypeData(fKeyDef.fluxcols.fkey, [],  'flux column suggestion'),
        {validator: null}),
    [fKeyDef.periodsteps.fkey]: Object.assign(getTypeData(fKeyDef.periodsteps.fkey, '3',  'total period steps'),
        {validator: Validate.intRange.bind(null, 1, 2000, 'period total step')})
};

var currentPhaseFoldedTable;   // table with phase column
var periodRange;
var SliderMin = 0.0;
var periodErr;       // error message for period setting
var timeErr;         // error message for time zero setting


const PanelResizableStyle = {
    width: 550,
    minWidth: 400,
    height: 300,
    minHeight: 300,
    marginLeft: 15,
    overflow: 'auto',
    padding: '2px'
};


export class LcPeriod extends Component {

    constructor(props) {
        super(props);
        this.state = pick(getLayouInfo(), ['mode']);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
    }

    storeUpdate() {
        if (this && this.iAmMounted) {
            const nextState = pick(getLayouInfo(), ['mode']);
            var stateMode = get(this.state, 'mode', null);
            var nextMode = get(nextState, 'mode', null);

            if (!stateMode && !nextMode && stateMode !== nextMode) {
               this.setState(nextState);
            }
        }
    }


    render() {
        const {display} = this.props;
        const {mode} = this.state;
        var {expanded, standard} = mode || {};

        expanded = LO_VIEW.get(expanded) || LO_VIEW.none;
        var standardProps = {...this.props};

        return (
            expanded === LO_VIEW.none
                ? <PeriodStandardView key='res-std-view' {...standardProps} />
                : <PeriodExpandedView key='res-exp-view' expanded={expanded} display={display}/>
        );
    }
}


LcPeriod.propTypes = {
    timeColName: PropTypes.string,
    fluxColName: PropTypes.string,
    validTimeColumns: PropTypes.arrayOf(PropTypes.string),
    validFluxColumns: PropTypes.arrayOf(PropTypes.string),
    display: PropTypes.string
};

LcPeriod.defaultProps = {
    timeColName: 'mjd',
    fluxColName: validFluxSuggestions[0],
    validTimeColumns: validTimeSuggestions,
    validFluxColumns: validFluxSuggestions,
    display: 'period'
};

const PeriodStandardView = (props) => {
    const {display} = props;
    var fields = FieldGroupUtils.getGroupFields(pfinderkey);
    var initState = {};

    if (!fields) {
        var {timeColName:time, fluxColName:flux, validTimeColumns:timeCols, validFluxColumns:fluxCols} = props;
        periodRange = computePeriodRange(LC.RAW_TABLE, time);
        periodErr = `Period error: must be a float and not less than ${periodRange.min} (day)`;
        timeErr = `time zero error: must be a float and within [0.0, ${periodRange.tzeroMax}]`;

        initState = Object.assign({time, flux, timeCols, fluxCols}, {...periodRange});
    }


    return (
        <div style={{display: 'flex', flexDirection: 'column', flexGrow: 1, position: 'relative'}}>
            <div style={{flexGrow: 1, position: 'relative'}}>
                <div style={{position: 'absolute', top: 0, right: 0, bottom: 0, left: 0}}>
                    <FieldGroup groupKey={pfinderkey}
                                reducerFunc={LcPFReducer(initState)}  keepState={true}>
                    <SplitPane split='horizontal' minSize={100} defaultSize={'90%'}>
                        <SplitPane split='vertical' minSize={20} defaultSize={PanelResizableStyle.width}>
                            <SplitPane split='horizontal' minSize={20} defaultSize={'45%'}>
                                {createContentWrapper(<LcPFOptionsBox />)}
                                {createContentWrapper(<PhaseFoldingChart height={350}
                                                                         width={PanelResizableStyle.width}/>)}
                            </SplitPane>
                            {createContentWrapper(<LcPeriodogram display={display} groupKey={pfinderkey}/>)}
                        </SplitPane>
                        <div style={{height: 20, marginTop: 5, marginBottom: 5, display: 'flex'}}>
                            <CompleteButton
                                groupKey={[pfinderkey]}
                                onSuccess={setPFTableSuccess()}
                                onFail={setPFTableFail()}
                                text={'Phase Folded Table'}
                            />
                            <button type='button' className='button std hl' onClick={cancelPeriodSetting()}>
                                Cancel
                            </button>
                        </div>
                    </SplitPane>
                    </FieldGroup>
                </div>
            </div>
        </div>
    );
};

const PeriodExpandedView = ({expanded, display}) => {
    const expandedProps = {expanded, display, groupKey: pfinderkey};

    return (
        <div>
            <LcPeriodogram  {...expandedProps} />
        </div>
    );
};

/**
 * @summary 2D xyplot component on phase folding panel
 */
class PhaseFoldingChart extends Component {
    constructor(props) {
        super(props);

        var fields = FieldGroupUtils.getGroupFields(pfinderkey);
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
                    title: {text: `${LC.PHASE_CNAME}`},
                    gridLineWidth: 1,
                    gridLineColor: '#e9e9e9'
                },
                yAxis: {
                    title: {text: `${flux}(mag)`},
                    lineWidth: 1,
                    tickWidth: 1,
                    tickLength: 10,
                    gridLineColor: '#e9e9e9'
                },
                tooltip: showTooltip ? {enabled: true,
                    formatter() {
                        return ('<span>' +
                        `${LC.PHASE_CNAME} = ${this.x.toFixed(DEC_PHASE)} <br/>` +
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

        this.unbinder= FieldGroupUtils.bindToStore(pfinderkey, (fields) => {
            if (this && this.iAmMounted && isPhaseChanged(fields, this.state.fields)) {
                this.setState({fields});

                var {data, minPhase = 0.0, period, flux} = getPhaseFlux(fields);
                chart.setTitle({text: period ? `period=${period}(day)`:'period='});
                chart.series[0].setData(data);
                chart.xAxis[0].update({min: minPhase - Margin,
                    max: minPhase + 2.0 + Margin});
                chart.yAxis[0].setTitle({text: `${flux}(mag)`});
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
 * @summary get flux series in terms of folded phase
 * @param {Object} fields
 * @returns {*}
 */
function getPhaseFlux(fields) {
    var rawTable = getTblById(LC.RAW_TABLE);
    var {columns, data} = rawTable.tableData;    // column names and data

    var {time, period, tzero, flux} = fields;
    const tc = time ? time.value : '';
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
        tz = periodRange.tzero;
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

    return Object.assign({data: xy}, {minPhase, maxPhase, period: pd.toFixed(DEC_PHASE), flux: fc});
}


/**
 * @summary Phase folding panel component for setting parameters
 */
class LcPFOptionsBox extends Component {
    constructor(props) {
        super(props);
        this.state = {fields: FieldGroupUtils.getGroupFields(pfinderkey)};
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {
        this.iAmMounted= true;
        this.unbinder= FieldGroupUtils.bindToStore(pfinderkey, (fields) => {
            if (this && this.iAmMounted && fields!==this.state.fields) {
                this.setState({fields});
            }
        });
    }

    render() {
        var {fields} = this.state;

        return (
            <div style={PanelResizableStyle}>
                <LcPFOptions {...fields}/>
            </div>
        );

    }
}

function LcPFOptions(fields) {

    const {periodMax, periodMin, period} = fields || {};

    if ((!periodMin || !periodMin || !period)) return <span />;
    const step = STEP;
    var min = parseFloat(parseFloat(periodMin.value).toFixed(DEC_PHASE)); // number
    var max = parseFloat(periodMax.value);

    max = adjustMax(max, SliderMin, step).max;    // number

    function  markStyle(per, val) {
        return {style: {left: `${per}%`, marginLeft: -16, width: 40}, label: `${val}`};
    }

    var getMarks = (sliderMin, sliderMax, noMarks) => {
        const sliderSize = sliderMax - sliderMin;
        const intMark = (sliderSize)/(noMarks - 1);

        // min sits at the first mark, all other marks evenly spread on the slider
        var marks = Object.assign({}, {[min]: markStyle(100*(min-sliderMin)/sliderSize, min)});

        // make an array like [0, 1, 2, ...] and produce marks between two ends of sRange
        marks = [...Array(noMarks).keys()].slice(1).reduce((prev, m) => {
            var val = parseFloat((intMark * m + sliderMin).toFixed(DEC_PHASE));
            var per = 100*(val - sliderMin)/sliderSize;

            prev = Object.assign(prev, {[val]: markStyle(per, val)});
            return prev;
        }, marks);

        return marks;
    };

    const NOMark = 5;
    const sRange = [SliderMin, max];
    var marks = getMarks(sRange[0], sRange[1], NOMark);


    //var step = periodSteps ? (max - min)/periodSteps.value : (max-min)/periodRange.steps;

    return (

            <InputGroup labelWidth={110}>
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
                <ValidationField fieldKey={fKeyDef.tz.fkey} />
                <br/>

                <RangeSlider fieldKey={fKeyDef.period.fkey}
                             min={sRange[0]}
                             max={sRange[1]}
                             minStop={min}
                             maxStop={max}
                             marks={marks}
                             step={step}
                             tooptip={'slide to set period value'}
                             value={period.value}
                             defaultValue={min}
                             wrapperStyle={{marginBottom: 20, width: PanelResizableStyle.width*3/4}}
                             sliderStyle={{marginLeft: 10, marginTop: 20}}
                />
                <br/>
                <br/>
                <div style={{display: 'flex'}}>
                    <ValidationField fieldKey={fKeyDef.min.fkey} />
                    <ValidationField fieldKey={fKeyDef.max.fkey} />
                </div>
                <br/>
                <ValidationField fieldKey={fKeyDef.period.fkey} />
                <br/>
                <div style={{marginTop: 15}}>
                    <button type='button' className='button std hl'  onClick={() => resetDefaults()}>
                        <b>Reset</b>
                    </button>
                </div>
                <br/>
            </InputGroup>
    );
}

LcPFOptions.propTypes = {
    fields: PropTypes.object
};

/**
 * @summary phase folding parameter reducer  // TODO: not sure how this works
 * @param {object} initState
 * @return {object}
 */
var LcPFReducer= (initState) => {
        return (inFields, action) => {
            if (!inFields) {
                var defV = Object.assign({}, defValues);
                            defV.periodSteps.value = periodRange.steps;
                const {min, max, time, flux, timeCols, fluxCols, tzero, steps, tzeroMax} = initState || {};
                set(defV, [fKeyDef.min.fkey, 'value'], `${min}`);
                set(defV, [fKeyDef.max.fkey, 'value'], `${max}`);
                set(defV, [fKeyDef.time.fkey, 'value'], time);
                set(defV, [fKeyDef.flux.fkey, 'value'], flux);
                set(defV, [fKeyDef.period.fkey, 'value'], `${min}`);
                set(defV, [fKeyDef.tz.fkey, 'value'], `${tzero}`);
                set(defV, [fKeyDef.timecols.fkey, 'value'], timeCols);
                set(defV, [fKeyDef.fluxcols.fkey, 'value'], fluxCols);
                set(defV, [fKeyDef.periodsteps.fkey, 'value'], `${steps}`);
                set(defV, [fKeyDef.tzmax.fkey, 'value'], `${tzeroMax}`);

                set(defV, [fKeyDef.period.fkey, 'tooltip'], `Period for phase folding, within [${min} (i.e. 1 sec), ${max}(suggestion)]`);
                set(defV, [fKeyDef.tz.fkey, 'tooltip'], `time zero, within [0.0, ${tzeroMax}`);

                set(defV, [fKeyDef.min.fkey, 'validator'], periodMinValidator('minimum period'));
                set(defV, [fKeyDef.max.fkey, 'validator'], periodMaxValidator('maximum period'));
                set(defV, [fKeyDef.tz.fkey, 'validator'], timezeroValidator(8, timeErr));
                set(defV, [fKeyDef.period.fkey, 'validator'], periodValidator(8, periodErr));

                set(defV, [fKeyDef.period.fkey, 'errMsg'], periodErr);
                return defV;
            }
            return Object.assign({}, inFields);
        };
    };

/**
 * @summary validator for periopd value
 * @param {number} precision
 * @param {string} description
 * @returns {function}
 */
function periodValidator(precision, description) {
    return (valStr) => {
        var max = get(FieldGroupUtils.getGroupFields(pfinderkey), [fKeyDef.max.fkey, 'value']);
        max = max ? parseFloat(max) : periodRange.max;
        var min = get(FieldGroupUtils.getGroupFields(pfinderkey), [fKeyDef.min.fkey, 'value']);
        min = min ? parseFloat(min) : periodRange.min;

        var retRes = Validate.floatRange(min, max, precision, description, valStr);
        if (!valStr && retRes.valid) {
            return {
                valid: false,
                message: description
            };
        }
        return retRes;
    };
}


function periodMinValidator(description) {
    return (valStr) => {
        var retRes = Validate.isFloat(description, valStr);

        if (retRes.valid) {
            var val = valStr && parseFloat(valStr);
            var max = get(FieldGroupUtils.getGroupFields(pfinderkey), [fKeyDef.max.fkey, 'value']);
            max = max ? parseFloat(max) : periodRange.max;

            if (!valStr || (val < periodRange.min || val >= max)) {    // can not be less than the computed minimum
                return {valid: false, message: 'invalid '+ description};
            }
        } else {
            retRes.message = description;
        }
        return retRes;
    };
}

function periodMaxValidator(description) {
    return (valStr) => {
        var retRes = Validate.isFloat(description, valStr);

        if (retRes.valid) {
            var val = valStr && parseFloat(valStr);
            var min = get(FieldGroupUtils.getGroupFields(pfinderkey), [fKeyDef.min.fkey, 'value']);
            min = min ? parseFloat(min) : periodRange.min;

            if (!valStr || (val <= min || val > periodRange.max) ) {   // can not be greater than computed maximum
                return {valid: false, message: 'invalid ' + description};
            }
        }
        return retRes;
    };
}
/**
 * @summary time zero validator
 * @param {number} precision
 * @param {string} description
 * @returns {function}
 */
function timezeroValidator(precision, description) {
    return (valStr) => {
        var retRes = Validate.floatRange(periodRange.tzero, periodRange.tzeroMax, precision, description, valStr);

        if (retRes.valid) {
            if (!valStr) {
                return {
                    valid: false,
                    message: description
                };
            }
         }
        return retRes;
    };
}



function setPFTableSuccess() {
    return (request) => {
        doPFCalculate();
        var reqData = get(request, pfinderkey);
        var timeName = get(reqData, fKeyDef.time.fkey);
        var period = get(reqData, fKeyDef.period.fkey);
        var flux = get(reqData, fKeyDef.flux.fkey);

        repeatDataCycle(timeName, parseFloat(period), currentPhaseFoldedTable);

        const {tableData, tbl_id, tableMeta, title} = currentPhaseFoldedTable;
        const ipacTable = tableToText(tableData.columns, tableData.data, true, tableMeta);
        const file = new File([new Blob([ipacTable])], `${tbl_id}.tbl`);

        doUpload(file).then(({status, cacheKey}) => {
            const tReq = makeFileRequest(title, cacheKey, null, {tbl_id, sortInfo:sortInfoString(LC.PHASE_CNAME)});
            dispatchTableSearch(tReq, {removable: false});

            let xyPlotParams = {
                userSetBoundaries: {xMax: 2},
                x: {columnOrExpr: LC.PHASE_CNAME, options: 'grid'},
                y: {columnOrExpr: flux, options: 'grid,flip'}
            };
            loadXYPlot({chartId: LC.PHASE_FOLDED, tblId: LC.PHASE_FOLDED, xyPlotParams});
        });

        dispatchLayoutDisplayMode(LC.RESULT_PAGE);
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
    let fields = FieldGroupUtils.getGroupFields(pfinderkey);
    let rawTable = getTblById(LC.RAW_TABLE);

    currentPhaseFoldedTable = rawTable && addPhaseToTable(rawTable, fields);
}

/**
 * @summary create a table model with phase column
 * @param {TableModel} tbl
 * @param {Object} fields
 * @returns {TableModel}
 */
function addPhaseToTable(tbl, fields) {
    var {time, period, tzero} = fields;
    var {columns} = tbl.tableData;
    var tc = time ? time.value : 'mjd';
    var tIdx = tc ? columns.findIndex((c) => (c.name === tc)) : -1;

    if (tIdx < 0) return null;

    var pd = period ? parseFloat(period.value) : periodRange.min;
    var tz = tzero ? parseFloat(tzero.value) : periodRange.tzero;
    var tPF = Object.assign(cloneDeep(tbl), {tbl_id: LC.PHASE_FOLDED, title: 'Phase Folded'},
        {request: getPhaseFoldingRequest(fields, tbl)},
        {highlightedRow: 0});
    tPF = omit(tPF, ['hlRowIdx', 'isFetching']);

    var phaseC = {desc: 'number of period elapsed since starting time.',
        name: LC.PHASE_CNAME, type: 'double', width: 20 };

    tPF.tableData.columns.push(phaseC);          // add phase column

    tPF.tableData.data.forEach((d, index) => {   // add phase value (in string) to each data

        tPF.tableData.data[index].push(getPhase(parseFloat(d[tIdx]), tz, pd));
    });

    return tPF;
}

/**
 * @summary create table request object for phase folded table
 * @param {Object} fields
 * @param {TableModel} tbl
 * @returns {TableRequest}
 */
function getPhaseFoldingRequest(fields, tbl) {
    return makeTblRequest('PhaseFoldedProcessor', LC.PHASE_FOLDED, {
        'period_days': fields&&get(fields, 'period.value'),
        'table_name': 'folded_table',
        'x': fields&&get(fields, 'time.value'),
        'original_table': tbl.tableMeta.tblFilePath
    },  {tbl_id:LC.PHASE_FOLDED});

}

/**
 * @summary calculate phase and return as text
 * @param {number} time
 * @param {number} timeZero
 * @param {number} period
 * @param {number} dec
 * @returns {string}  folded phase (positive or negative) with 'dec'  decimal digits
 */
function getPhase(time, timeZero, period,  dec=DEC_PHASE) {
    var q = (time - timeZero)/period;
    var p = q >= 0  ? (q - Math.floor(q)) : (q + Math.floor(-q));

    return p.toFixed(dec);
}

/**
 * @summary duplicate the phase cycle to the phase folded table
 * @param {string} timeCol
 * @param {number} period
 * @param {TableModel} phaseTable
 */
function repeatDataCycle(timeCol, period, phaseTable) {
    var tIdx = phaseTable.tableData.columns.findIndex((c) => (c.name === timeCol));
    var fIdx = phaseTable.tableData.columns.findIndex((c) => (c.name === LC.PHASE_CNAME));
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

    var timeZero = Math.min(...arr);
    var timeMax = Math.max(...arr);

    var max = parseFloat(((timeMax - timeZero)*factor).toFixed(DEC_PHASE));
    var min = Math.pow(10, -DEC_PHASE);
    var newMax = adjustMax(max, SliderMin, STEP);

    return {min, max: newMax.max, steps: newMax.steps, tzero: timeZero, tzeroMax: timeMax};
}

/**
 * @summary adjust the maximum to be the multiple of the step resolution if the maximum on the slider is updated
 * @param {number} max
 * @param {number} min
 * @param {number} step
 * @returns {{steps: number, max: number}}
 */
export function adjustMax(max, min, step) {
    var newTotalSteps = Math.ceil((max - min) / step);
    var newMax = parseFloat((newTotalSteps*step + min).toFixed(DEC_PHASE));
    var res = (newMax - min)/newTotalSteps;

    return {steps: newTotalSteps, max: newMax, res};
}


/**
 * @summary reset the setting
 */
function resetDefaults() {
    dispatchRestoreDefaults(pfinderkey);
}


function cancelPeriodSetting() {
    return () => {
        dispatchLayoutDisplayMode(LC.RESULT_PAGE);
    };
}

