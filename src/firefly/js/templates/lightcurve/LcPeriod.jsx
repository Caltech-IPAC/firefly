/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';

import sCompare from 'react-addons-shallow-compare';
import {cloneDeep, get, set, omit, slice, replace, pick, isEmpty,  debounce, defer} from 'lodash';
import SplitPane from 'react-split-pane';
import {flux} from '../../Firefly.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
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
import {dispatchTableSearch, dispatchActiveTableChanged} from '../../tables/TablesCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {dispatchRestoreDefaults, dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {makeTblRequest,getTblById, tableToText, makeFileRequest, getActiveTableId} from '../../tables/TableUtil.js';
import {LC, updateLayoutDisplay} from './LcManager.js';
import {LcPeriodogram, startPeriodogramPopup, cancelPeriodogram, popupId} from './LcPeriodogram.jsx';
import {LO_VIEW, getLayouInfo} from '../../core/LayoutCntlr.js';
import {isDialogVisible} from '../../core/ComponentCntlr.js';
import ReactHighcharts from 'react-highcharts';
import Resizable from 'react-component-resizable';

const pfinderkey = LC.PERIOD_FINDER;
const labelWidth = 100;

export function getTypeData(key, val='', tip = '', labelV='', labelW) {
    return {
        fieldKey: key,
        label: labelV,
        value: val,
        tooltip: tip,
        labelWidth: labelW
    };
}

export const cPeriodKeyDef = {
    time: {fkey: 'time', label: 'Time Column'},
    flux: {fkey: 'flux', label: 'Flux Column'},
    min: {fkey: 'periodMin', label: 'Period Min'},
    max: {fkey: 'periodMax', label: 'Period Max'},
    timecols: {fkey: 'timeCols', label: ''},
    fluxcols: {fkey: 'fluxCols', label: ''}
};

const PanelResizableStyle = {
    width: 520,
    minWidth: 400,
    minHeight: 300,
    marginLeft: 15,
    overflow: 'auto',
    padding: '2px'
};

const panelSpace = PanelResizableStyle.width/5;

const fKeyDef = Object.assign({}, cPeriodKeyDef,
                                {tz: {fkey: 'tzero', label: 'Zero Point Time'},
                                period: {fkey: 'period', label: 'Period (day)'},
                                tzmax: {fkey: 'tzeroMax', label: ''}});

const STEP = 1;            // step for slider
export const validTimeSuggestions = ['mjd'];      // suggestive time column name
export const validFluxSuggestions = ['w1mpro_ep', 'w2mpro_ep', 'w3mpro_ep', 'w4mpro_ep']; // suggestive flux col name
const Margin = 0.2;        // phase margin reserved at two ends of the phase charts x axis
const DEC_PHASE = 3;       // decimal digit

// defValues used to keep the initial values for phase finding parameters
// time: time column
// flux: flux column
// tz:   zero time
// tzmax: maximum of zero time (not a field)
// min:  minimum period
// max:  maximum period
// period: period for phase folding
// timecols: possible time column names (not a field, shown as suggestion)
// fluxcols: possible flux column names (not a field, shown as suggestion)

const defValues= {
    [fKeyDef.time.fkey]: Object.assign(getTypeData(fKeyDef.time.fkey, '',
        'time column name',
        `${fKeyDef.time.label}:`, labelWidth),
        {validator:  (val) => {
            let retVal = {valid: true, message: ''};
            if (!validTimeSuggestions.includes(val)) {
                retVal = {valid: false, message: `${val} is not a valid time column`};
            }
            return retVal;
        }}),
    [fKeyDef.flux.fkey]: Object.assign(getTypeData(fKeyDef.flux.fkey, '',
        'flux column name', `${fKeyDef.flux.label}:`, labelWidth),
        {validator:  (val) => {
            let retVal = {valid: true, message: ''};
            if (!validFluxSuggestions.includes(val)) {
                retVal = {valid: false, message: `${val} is not a valid flux column`};
            }
            return retVal;
        }}),
    [fKeyDef.tz.fkey]: Object.assign(getTypeData(fKeyDef.tz.fkey, '', 'zero time', `${fKeyDef.tz.label}:`, labelWidth),
        {validator: null}),
    [fKeyDef.tzmax.fkey]: Object.assign(getTypeData(fKeyDef.tzmax.fkey, '', 'maximum for zero time'),
        {validator: null}),
    [fKeyDef.min.fkey]: Object.assign(getTypeData(fKeyDef.min.fkey, '', 'minimum period', `${fKeyDef.min.label}:`, labelWidth-panelSpace/3),
        {validator: null}),
    [fKeyDef.max.fkey]: Object.assign(getTypeData(fKeyDef.max.fkey, '', 'maximum period', `${fKeyDef.max.label}:`, labelWidth-panelSpace/3),
        {validator: null}),
    [fKeyDef.period.fkey]: Object.assign(getTypeData(fKeyDef.period.fkey, '', '', `${fKeyDef.period.label}:`, labelWidth-10),
        {validator: null}),
    [fKeyDef.timecols.fkey]: Object.assign(getTypeData(fKeyDef.timecols.fkey, [],  'time column suggestion'),
        {validator: null}),
    [fKeyDef.fluxcols.fkey]: Object.assign(getTypeData(fKeyDef.fluxcols.fkey, [],  'flux column suggestion'),
        {validator: null})
};

var periodRange;        // prediod range based on raw table, set based on the row table, and unchangable
var currentPhaseFoldedTable;   // table with phase column
var SliderMin = 0.0;           // min on slider
var SliderMax;                 // max on slider
var periodErr;       // error message for period setting

function lastFrom(strAry) {
    return strAry.length > 0 ? strAry[strAry.length - 1] : '';
}
/**
 * class for creating component for light curve period finding
 */
export class LcPeriod extends Component {

    constructor(props) {
        super(props);

        periodRange = computePeriodRange(LC.RAW_TABLE, props.timeColName);
        const currentPeriod = this.getCurrentValidPeriod() || `${periodRange.min}`;   // the first time
        const {displayMode} = props;

        this.state = Object.assign({}, pick(getLayouInfo(), ['mode']), {displayMode, currentPeriod, periodList:[]});
        this.revertPeriod = this.revertPeriod.bind(this);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        this.removeListener && this.removeListener();
    }

    getCurrentValidPeriod() {
        var fields = FieldGroupUtils.getGroupFields(pfinderkey);
        var period =  get(fields, ['period', 'valid']) && get(fields, ['period', 'value']);

        return period ? period : '';
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const nextState = pick(getLayouInfo(), ['mode']);
            const cState = pick(this.state, ['mode']);
            const nextLayout = pick(getLayouInfo(), ['displayMode']);
            const cLayout = pick(this.state, ['displayMode']);

            if (!isEmpty(nextState) && cState !== nextState) {
               this.setState(nextState);
            }
            if (!isEmpty(nextLayout) && nextLayout !== cLayout) {
                this.setState(nextLayout);
            }

            var {periodList, currentPeriod} = this.state;
            var newPeriod = this.getCurrentValidPeriod();
            var bRevert = (lastFrom(periodList) === 'revert');

            if (bRevert) {
                currentPeriod = newPeriod;
                periodList.pop();          // remove 'revert' flag on the top
                this.setState({currentPeriod,
                               lastPeriod: lastFrom(periodList),
                               periodList});
            } else {
                var bUpdate = false;

                if (!newPeriod) {     // period: empty string
                    this.setState({currentPeriod: ''});       // non valid current
                    if (currentPeriod) bUpdate = true;        // current->last
                } else if (newPeriod !== currentPeriod) {
                    this.setState({currentPeriod: newPeriod}); // valid current
                    if (currentPeriod) {                       // current->last
                        bUpdate = true;
                    } else {                                   // new period is the same as last period
                        if (lastFrom(periodList) === newPeriod) {
                            periodList.pop();
                            this.setState({lastPeriod: lastFrom(periodList), periodList});
                        }
                    }
                }
                if (bUpdate) {      // current -> top of history if not exist
                    if (currentPeriod !== lastFrom(periodList)) {
                        periodList.push(currentPeriod);
                        this.setState({lastPeriod: currentPeriod, periodList});
                    }
                }
            }
        }
    }

    revertPeriod() {
        var {periodList, lastPeriod} = this.state;

        if ((periodList.length >= 1) && (periodList[periodList.length-1] === lastPeriod)) {
            periodList[periodList.length-1] = 'revert';      // a flag to block adding new item to the list in store{

            dispatchValueChange({
                    fieldKey: fKeyDef.period.fkey,
                    groupKey: pfinderkey,
                    value: lastPeriod
                });
        }
    }

    render() {
        const {mode, displayMode, lastPeriod, currentPeriod} = this.state;
        var {expanded} = mode || {};

        expanded = LO_VIEW.get(expanded) || LO_VIEW.none;
        var standardProps = {...this.props, lastPeriod, currentPeriod, displayMode, revertPeriod: this.revertPeriod};
        var expandProps = {expanded, displayMode};

        return (
            expanded === LO_VIEW.none
                ? <PeriodStandardView key='res-std-view' {...standardProps} />
                : <PeriodExpandedView key='res-exp-view' {...expandProps} />
        );
    }
}


LcPeriod.propTypes = {
    timeColName: PropTypes.string,
    fluxColName: PropTypes.string,
    validTimeColumns: PropTypes.arrayOf(PropTypes.string),
    validFluxColumns: PropTypes.arrayOf(PropTypes.string),
    displayMode: PropTypes.string
};

LcPeriod.defaultProps = {
    timeColName: 'mjd',
    fluxColName: validFluxSuggestions[0],
    validTimeColumns: validTimeSuggestions,
    validFluxColumns: validFluxSuggestions,
    displayMode: 'period'
};

/**
 * @summary stardard mode of period layout
 * @param props
 * @returns {XML}
 * @constructor
 */
const PeriodStandardView = (props) => {
    var {displayMode, lastPeriod, currentPeriod, revertPeriod} = props;
    const fields = FieldGroupUtils.getGroupFields(pfinderkey);
    var initState = {};

    // when field is not set yet, set the init value
    if (!fields) {
        var {timeColName, fluxColName, validTimeColumns, validFluxColumns} = props;
        initState = Object.assign({time: timeColName,
                                   flux: fluxColName,
                                   timeCols: validTimeColumns,
                                   fluxCols: validFluxColumns}, {...periodRange});
    }

    const acceptPeriodTxt = `Accept Period: ${currentPeriod ? currentPeriod : ''}`;
    const revertPeriodTxt = `Back to Period: ${lastPeriod ? lastPeriod : ''}`;
    const space = 5;
    const aroundButton = {marginLeft: space, marginRight: space};

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
                                {createContentWrapper(<PhaseFoldingChart />)}
                            </SplitPane>
                            {createContentWrapper(<LcPeriodogram displayMode={displayMode} groupKey={pfinderkey}/>)}
                        </SplitPane>
                        <div style={{width: '100%', position: 'absolute',
                                     height: 20, marginTop: 5, marginBottom: 5,
                                     display: 'flex', justifyContent: 'flex-end'}}>
                            {displayMode===LC.PERGRAM_PAGE &&
                                <div style={aroundButton}>
                                    <button type='button'
                                            className='button std hl'
                                            onClick={startPeriodogramPopup(pfinderkey)}>Change Periodogram</button>
                                </div>}
                            <div style={aroundButton}>
                                <button type='button' className='button std hl' onClick={revertPeriod}>
                                        {revertPeriodTxt}
                                </button>
                            </div>
                            <CompleteButton
                                    style={aroundButton}
                                    groupKey={[pfinderkey]}
                                    onSuccess={setPFTableSuccess()}
                                    onFail={setPFTableFail()}
                                    text={acceptPeriodTxt}
                                    includeUnmounted={true}
                            />
                            <div style={{marginLeft: space}}>
                                <button type='button' className='button std hl' onClick={cancelPeriodSetting()}>
                                        Cancel
                                </button>
                            </div>
                        </div>
                    </SplitPane>
                    </FieldGroup>
                </div>
            </div>
        </div>
    );
};


PeriodStandardView.propTypes = {
    displayMode: PropTypes.string,
    lastPeriod: PropTypes.string,
    currentPeriod: PropTypes.string,
    revertPeriod: PropTypes.func,
    timeColName: PropTypes.string,
    fluxColName: PropTypes.string,
    validTimeColumns: PropTypes.arrayOf(PropTypes.string),
    validFluxColumns: PropTypes.arrayOf(PropTypes.string)
};


const PeriodExpandedView = ({expanded, displayMode}) => {
    const expandedProps = {expanded, displayMode, groupKey: pfinderkey};

    return (
        <LcPeriodogram  {...expandedProps} />
    );
};

PeriodExpandedView.propTypes = {
    expanded: PropTypes.object,
    displayMode: PropTypes.string
};

/**
 * @summary 2D xyplot component on phase folding
 */
class PhaseFoldingChart extends Component {
    constructor(props) {
        super(props);

        var fields = FieldGroupUtils.getGroupFields(pfinderkey);
        var {showTooltip=true} = props;
        var {data, minPhase = 0.0, period, flux} = getPhaseFlux(fields);

        const normal = (size) => {
            if (size && this.iAmMounted) {
                var widthPx = size.width;
                var heightPx = size.height;

                if (widthPx !== this.state.widthPx || heightPx !== this.state.heightPx) {
                    this.setState({widthPx, heightPx});
                }
            }
        };
        const debounced = debounce(normal, 100);
        this.onResize = (size) => {
            if (this.state.widthPx === 0) {
                defer(normal, size);
            } else {
                debounced(size);
            }
        };

        this.state = {
            fields,
            config: {
                chart: {
                    type: 'scatter',
                    borderColor: '#a5a5a5',
                    borderWidth: 1,
                    borderRadius: 5,
                    zoomType: 'xy'
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
        return sCompare(np, ns);
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
        const {widthPx, heightPx} = this.state;

        if (this.refs.chart) {
            var  chart = this.refs.chart.getChart();

            chart.setSize(widthPx, heightPx);
        }

        return (
            <Resizable className='ChartPanel__chartresizer'
                       onResize={this.onResize}>
                <ReactHighcharts config={this.state.config} isPureConfig={true} ref='chart'/>
            </Resizable>
        );
    }

}

PhaseFoldingChart.propTypes = {
    showTooltip: PropTypes.bool
};


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
 * @summary Phase folding finder component containing period finding parameters
 */
class LcPFOptionsBox extends Component {
    constructor(props) {
        super(props);

        const fields = FieldGroupUtils.getGroupFields(pfinderkey);
        const maxPeriod = fields && get(fields, ['periodMax', 'value']);
        const minPeriod = fields && get(fields, ['periodMin', 'value']);
        const period = fields && get(fields, ['period', 'value']);
        this.state = {fields: FieldGroupUtils.getGroupFields(pfinderkey),
                      minPeriod, maxPeriod, period};
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {
        this.iAmMounted= true;
        this.unbinder= FieldGroupUtils.bindToStore(pfinderkey, (fields) => {
            if (this && this.iAmMounted && fields!==this.state.fields) {

                const maxPeriod = fields && get(fields, ['periodMax', 'valid']) && get(fields, ['periodMax', 'value']);
                const minPeriod = fields && get(fields, ['periodMin', 'valid']) && get(fields, ['periodMin', 'value']);
                const period =  fields && get(fields, ['period', 'valid']) && get(fields, ['period', 'value']);
                var   newState = {fields};

                if (maxPeriod) {
                    newState = Object.assign({}, newState, {maxPeriod});
                }
                if (minPeriod) {
                    newState = Object.assign({}, newState, {minPeriod});
                }
                if (period) {
                    newState = Object.assign({}, newState, {period});
                }

                this.setState(newState);
            }
        });
    }

    render() {
        var {minPeriod, maxPeriod, period} = this.state;   // only pass valid minPeriod & maxPeriod defined previously

        return (
                <LcPFOptions minPeriod={minPeriod} maxPeriod={maxPeriod} period={period}/>
        );

    }
}

/**
 * @summary light curve phase folding FieldGroup rendering
 */

function LcPFOptions({period, minPeriod, maxPeriod}) {

    if (!maxPeriod || !minPeriod || !period) return <span />;   // when there is no field defined in the beginning

    const step = STEP;
    var min = parseFloat(minPeriod);                                     // number, stop min
    var minFixed = parseFloat(parseFloat(minPeriod).toFixed(DEC_PHASE)); // number, fixed decimal of min (<= min)
    var max = parseFloat(maxPeriod);                                     // number, stop max

    SliderMax = adjustMax(max, SliderMin, step).max;    // number, [SliderMin, SliderMax] covers [min, max]

    function  markStyle(per, val) {
        return {style: {left: `${per}%`, marginLeft: -16, width: 40}, label: `${val}`};
    }

    // marks on the slider, marks on two ends: min and SliderMax
    var getMarks = (sliderMin, sliderMax, noMarks) => {
        const sliderSize = sliderMax - sliderMin;
        const intMark = (sliderSize)/(noMarks - 1);

        // min sits at the first mark, all other marks evenly spread on the slider
        var marks = Object.assign({}, {[minFixed]: markStyle(100*(minFixed-sliderMin)/sliderSize, minFixed)});

        // make an array like [0, 1, 2, ...] and produce marks between two ends of Slider min and max
        marks = [...Array(noMarks).keys()].slice(1).reduce((prev, m) => {
            var val = parseFloat((intMark * m + sliderMin).toFixed(DEC_PHASE));
            var per = 100*(val - sliderMin)/sliderSize;

            prev = Object.assign(prev, {[val]: markStyle(per, val)});
            return prev;
        }, marks);

        return marks;
    };

    const NOMark = 5;
    var marks = getMarks(SliderMin, SliderMax, NOMark);

    return (<div>
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
                             min={SliderMin}
                             max={SliderMax}
                             minStop={min}
                             maxStop={max}
                             marks={marks}
                             step={step}
                             tooptip={'slide to set period value'}
                             slideValue={period}
                             defaultValue={min}
                             wrapperStyle={{marginBottom: 20, width: PanelResizableStyle.width*4/5}}
                             sliderStyle={{marginLeft: 10, marginTop: 20}}
                             decimalDig={DEC_PHASE}
                />
                <br/>
                <br/>
                <br/>
                <div style={{display: 'flex'}}>
                    <ValidationField fieldKey={fKeyDef.min.fkey} />
                    <ValidationField fieldKey={fKeyDef.max.fkey} />
                </div>
                <br/>
                <div style={{marginTop: 20, width: PanelResizableStyle.width,
                             display: 'flex', justifyContent: 'center'}}>
                    <ValidationField fieldKey={fKeyDef.period.fkey} />
                </div>
                <br/>
                <br/>
                <div style={{marginTop: 25}}>
                    <button type='button' className='button std hl'  onClick={() => resetDefaults()}>
                        <b>Reset</b>
                    </button>
                </div>
                <br/>
        </div>
    );
}

LcPFOptions.propTypes = {
    minPeriod: PropTypes.string,
    maxPeriod: PropTypes.string,
    period: PropTypes.string
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
                const {min, max, time, flux, timeCols, fluxCols, tzero,  tzeroMax} = initState || {};

                set(defV, [fKeyDef.min.fkey, 'value'], `${min}`);
                set(defV, [fKeyDef.max.fkey, 'value'], `${max}`);
                set(defV, [fKeyDef.time.fkey, 'value'], time);
                set(defV, [fKeyDef.flux.fkey, 'value'], flux);
                set(defV, [fKeyDef.period.fkey, 'value'], `${min}`);
                set(defV, [fKeyDef.tz.fkey, 'value'], `${tzero}`);
                set(defV, [fKeyDef.timecols.fkey, 'value'], timeCols);
                set(defV, [fKeyDef.fluxcols.fkey, 'value'], fluxCols);
                set(defV, [fKeyDef.tzmax.fkey, 'value'], `${tzeroMax}`);

                set(defV, [fKeyDef.period.fkey, 'tooltip'], `Period for phase folding, within [${min} (i.e. 1 sec), ${max}(suggestion)]`);
                set(defV, [fKeyDef.tz.fkey, 'tooltip'], `time zero, within [${tzero}, ${tzeroMax}`);

                set(defV, [fKeyDef.min.fkey, 'validator'], periodMinValidator('minimum period'));
                set(defV, [fKeyDef.max.fkey, 'validator'], periodMaxValidator('maximum period'));
                set(defV, [fKeyDef.tz.fkey, 'validator'], timezeroValidator(8, 'time zero'));
                set(defV, [fKeyDef.period.fkey, 'validator'], periodValidator(8, 'pediod'));

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
        var retRes = Validate.isFloat(description, valStr);

        if (retRes.valid) {
            var fields = FieldGroupUtils.getGroupFields(pfinderkey);
            var max = get(fields, [fKeyDef.max.fkey, 'valid']) && get(fields, [fKeyDef.max.fkey, 'value']);
            max = max ? parseFloat(max) : periodRange.max;
            var min = get(fields, [fKeyDef.min.fkey, 'valid']) && get(fields, [fKeyDef.min.fkey, 'value']);
            min = min ? parseFloat(min) : periodRange.min;

            retRes = Validate.floatRange(min, max, precision, description, valStr);
            if (!valStr || !retRes.valid) {
                return {
                    valid: false,
                    message: `period: must be between ${min} and ${max}`
                };
            }
        }
        return retRes;
    };
}

/**
 * @summary validator for minimum period
 * @param description
 * @returns {Function}
 */
function periodMinValidator(description) {
    return (valStr) => {
        var retRes = Validate.isFloat(description, valStr);

        if (retRes.valid) {
            var val = valStr && parseFloat(valStr);
            var max = get(FieldGroupUtils.getGroupFields(pfinderkey), [fKeyDef.max.fkey, 'value']);
            max = max ? parseFloat(max) : periodRange.max;

            if (!valStr || (val < periodRange.min || val >= max)) {    // can not be less than the computed minimum
                return {valid: false, message: description + `: must be between ${periodRange.min} and ${max}`} ;
            }
        }
        return retRes;
    };
}

/**
 * @summary validator for maximum period
 * @param description
 * @returns {Function}
 */
function periodMaxValidator(description) {
    return (valStr) => {
        var retRes = Validate.isFloat(description, valStr);

        if (retRes.valid) {
            var val = valStr && parseFloat(valStr);
            var min = get(FieldGroupUtils.getGroupFields(pfinderkey), [fKeyDef.min.fkey, 'value']);
            min = min ? parseFloat(min) : periodRange.min;

            if (!valStr || (val <= min || val > periodRange.max) ) {   // can not be greater than computed maximum
                return {valid: false, message: description + `: must be between ${min} and ${periodRange.max}`};
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
        var retRes = Validate.isFloat(description, valStr);

        if (retRes.valid) {
            retRes = Validate.floatRange(periodRange.tzero, periodRange.tzeroMax, precision, description, valStr);

            if (retRes.valid) {
                if (!valStr) {
                    return {
                        valid: false,
                        message: description + `: must be between ${periodRange.tzero} and ${periodRange.tzeroMax}`
                    };
                }
            }
        }
        return retRes;
    };
}


/**
 * @summary callback to create table with phase folding column and phase folding chart based on the accepted period value
 * @returns {Function}
 */
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

            const xyPlotParams = {
                userSetBoundaries: {xMax: 2},
                x: {columnOrExpr: LC.PHASE_CNAME, options: 'grid'},
                y: {columnOrExpr: flux, options: 'grid,flip'}
            };
            loadXYPlot({chartId: LC.PHASE_FOLDED, tblId: LC.PHASE_FOLDED, xyPlotParams});
        });

        updateLayoutDisplay(LC.RESULT_PAGE);
    };
}

/**
 * @summary phase folding parameter error handler
 * @returns {Function}
 */
function setPFTableFail() {
    return (request) => {
        return showInfoPopup('Phase folding parameter setting error');
    };
}

/**
 * @summary adding phase column to raw table
 */
function doPFCalculate() {
    const fields = FieldGroupUtils.getGroupFields(pfinderkey);
    const rawTable = getTblById(LC.RAW_TABLE);

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
    var min = Math.pow(10, -3);   // 0.0001
    //max = 19;

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
 * @summary reset the period parameter setting
 */
function resetDefaults() {
    dispatchRestoreDefaults(pfinderkey);
}

/**
 * @summary cancel the period setting
 * @returns {Function}
 */
function cancelPeriodSetting() {
    return () => {
        if (isDialogVisible(popupId)) {
            cancelPeriodogram(pfinderkey, popupId)();
        }

        dispatchActiveTableChanged(getActiveTableId()||LC.RAW_TABLE);
        updateLayoutDisplay(LC.RESULT_PAGE);
    };
}
