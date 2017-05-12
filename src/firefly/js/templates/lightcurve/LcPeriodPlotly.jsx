/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';

import {get, set,  pick,  debounce, defer} from 'lodash';
import SplitPane from 'react-split-pane';
import {flux} from '../../Firefly.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {RangeSlider}  from '../../ui/RangeSlider.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {showInfoPopup, INFO_POPUP} from '../../ui/PopupUtil.jsx';
import {SplitContent} from '../../ui/panel/DockLayoutPanel.jsx';
import Validate from '../../util/Validate.js';
import {dispatchActiveTableChanged} from '../../tables/TablesCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import FieldGroupCntlr, {dispatchValueChange, dispatchMultiValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {getActiveTableId, getColumnIdx} from '../../tables/TableUtil.js';
import {LC, updateLayoutDisplay, getValidValueFrom, getFullRawTable} from './LcManager.js';
import {doPFCalculate, getPhase} from './LcPhaseTable.js';
import {LcPeriodogram, cancelPeriodogram, popupId} from './LcPeriodogram.jsx';
import {ReadOnlyText, getTypeData} from './LcUtil.jsx';
import {LO_VIEW, getLayouInfo} from '../../core/LayoutCntlr.js';
import {isDialogVisible, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {updateSet} from '../../util/WebUtil.js';
import {PlotlyWrapper} from '../../charts/ui/PlotlyWrapper.jsx';
import Resizable from 'react-component-resizable';
import './LCPanels.css';

const pfinderkey = LC.FG_PERIOD_FINDER;
const labelWidth = 100;

export const highlightBorder = '1px solid #a3aeb9';


export var isBetween = (a, b, c) => (c >= a && c <= b);

const PanelResizableStyle = {
    width: 550,
    minWidth: 400,
    minHeight: 300,
    marginLeft: 15,
    overflow: 'auto',
    padding: '2px'
};

const panelSpace = PanelResizableStyle.width/5;
const fKeyDef = {
    time: {fkey: 'time', label: 'Time Column'},
    flux: {fkey: 'flux', label: 'Value Column'},
    min: {fkey: 'periodMin', label: 'Period Min (day)'},
    max: {fkey: 'periodMax', label: 'Period Max (day)'},
    tz: {fkey: 'tzero', label: 'Zero Point Time'},
    period: {fkey: 'period', label: 'Period (day)'},
    tzmax: {fkey: 'tzeroMax', label: ''}
};

const STEP = 1;            // step for slider
const DEC_PHASE = 3;       // decimal digit

// defValues used to keep the initial values for parameters in phase finding field group
// time: time column
// flux: flux column
// tz:   zero time
// tzmax: maximum of zero time (not a field)
// min:  minimum period
// max:  maximum period
// period: period for phase folding

const defValues= {
    [fKeyDef.time.fkey]: Object.assign(getTypeData(fKeyDef.time.fkey, '',
        'Time column name',
        `${fKeyDef.time.label}:`, labelWidth), {validator:  null}),
    [fKeyDef.flux.fkey]: Object.assign(getTypeData(fKeyDef.flux.fkey, '',
        'Value column name', `${fKeyDef.flux.label}:`, labelWidth), {validator:  null}),
    [fKeyDef.tz.fkey]: Object.assign(getTypeData(fKeyDef.tz.fkey, '', 'zero time', `${fKeyDef.tz.label}:`, labelWidth),
        {validator: null}),
    [fKeyDef.tzmax.fkey]: Object.assign(getTypeData(fKeyDef.tzmax.fkey, '', 'maximum for zero time'),
        {validator: null}),
    [fKeyDef.min.fkey]: Object.assign(getTypeData(fKeyDef.min.fkey, '', 'minimum period', '', 0),
        {validator: null}),
    [fKeyDef.max.fkey]: Object.assign(getTypeData(fKeyDef.max.fkey, '', 'maximum period', '', 0),
        {validator: null}),
    [fKeyDef.period.fkey]: Object.assign(getTypeData(fKeyDef.period.fkey, '', '', `${fKeyDef.period.label}:`, labelWidth-10),
        {validator: null})
};

var defPeriod = {
    [fKeyDef.tz.fkey]: {value: ''},
    [fKeyDef.tzmax.fkey]: {value: ''},
    [fKeyDef.min.fkey]: {value: ''},
    [fKeyDef.max.fkey]: {value: ''},
    [fKeyDef.period.fkey]: {value: ''}
};

var periodRange;        // prediod range based on raw table, set based on the row table, and unchangable
var periodErr;          // error message for period setting


function lastFrom(strAry) {
    return strAry.length > 0 ? strAry[strAry.length - 1] : '';
}

/**
 * class for creating component for light curve period finding
 */
export class LcPeriodPlotly extends PureComponent {

    constructor(props) {
        super(props);

        const layoutInfo = getLayouInfo();
        periodRange = get(layoutInfo, 'periodRange');

        const fields = FieldGroupUtils.getGroupFields(pfinderkey);
        this.state = Object.assign({}, pick(layoutInfo, ['mode']), {displayMode: props.displayMode, fields});

        this.getNextState = () => {
            const layout = pick(getLayouInfo(), ['mode', 'displayMode']);
            const fields = FieldGroupUtils.getGroupFields(pfinderkey);

            return Object.assign({}, layout, {fields});
        };
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        this.removeListener && this.removeListener();
    }


    storeUpdate() {
        if (this.iAmMounted) {
            const nextState = this.getNextState();

            const {displayMode} = nextState;
            if (displayMode && displayMode.startsWith('period') && nextState) {
                this.setState(nextState);
            }
        }
    }


    render() {
        const {mode, displayMode, fields} = this.state;
        var {expanded} = mode || {};

        expanded = LO_VIEW.get(expanded) || LO_VIEW.none;
        var standardProps = {...this.props,  displayMode, fields};  // period is for accept period display
        var expandProps = {expanded, displayMode};

        return (
            expanded === LO_VIEW.none
                ? <PeriodStandardView key='res-std-view' {...standardProps} />
                : <PeriodExpandedView key='res-exp-view' {...expandProps} />
        );
    }
}


LcPeriodPlotly.propTypes = {
    timeColName: PropTypes.string.isRequired,
    fluxColName: PropTypes.string.isRequired,
    displayMode: PropTypes.string
};

LcPeriodPlotly.defaultProps = {
    displayMode: 'period'
};

/**
 * @summary cancel from stardard view
 */
function cancelStandard() {
    if (isDialogVisible(popupId)) {
        cancelPeriodogram();
    }

    updateLayoutDisplay(LC.RESULT_PAGE);
    dispatchActiveTableChanged(getActiveTableId()||LC.RAW_TABLE);
    resetPeriodDefaults(defPeriod);
}

/**
 * @summary stardard mode of period layout
 * @param props
 * @returns {XML}
 * @constructor
 */
const PeriodStandardView = (props) => {
    var {displayMode, fields} = props;
    var currentPeriod = fields && getValidValueFrom(fields, 'period');
    var initState = {};

    // when field is not set yet, set the init value
    if (!fields) {
        var {timeColName, fluxColName} = props;
        initState = Object.assign({time: timeColName,
                                   flux: fluxColName}, {...periodRange});
        currentPeriod = initState.min;
    }

    const acceptPeriodTxt = `Accept Period: ${currentPeriod ? currentPeriod : ''}`;
    const space = 5;
    const aroundButton = {margin: space};

    return (
        <FieldGroup groupKey={pfinderkey} style={{display: 'flex', flexDirection: 'column', position: 'relative', flexGrow: 1, minHeight: 500}}
                    reducerFunc={LcPFReducer(initState)}  keepState={true}>
            <div style={{flexGrow: 1, position: 'relative'}}>
                <SplitPane split='horizontal' primary='second' maxSize={-100} minSize={100} defaultSize={400}>
                    <SplitContent>
                        <div className='phaseFolded'>
                            <div className='phaseFolded__options'>
                                <LcPFOptionsBox/>
                            </div>
                            <PhaseFoldingChart/>
                        </div>
                    </SplitContent>
                    <LcPeriodogram displayMode={displayMode}/>
                </SplitPane>
            </div>
        </FieldGroup>
    );
};


PeriodStandardView.propTypes = {
    displayMode: PropTypes.string,
    timeColName: PropTypes.string,
    fluxColName: PropTypes.string,
    fields: PropTypes.object
};


const PeriodExpandedView = ({expanded, displayMode}) => {
    const expandedProps = {expanded, displayMode};

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
class PhaseFoldingChart extends PureComponent {
    constructor(props) {
        super(props);

        var fields = FieldGroupUtils.getGroupFields(pfinderkey);
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
        this.afterRedraw= this.afterRedraw.bind(this);

        this.chartingInfo= this.regenData(fields);
        this.state = {
            fields,
            dataUpdate: null
        };
    }

    regenData(fields) {
        var {data, period, flux} = getPhaseFlux(fields);
        this.lastData= data;
        this.lastFluxCol= flux;
        return {
            plotlyDivStyle: {
                border: '1px solid a5a5a5',
                borderRadius: 5,
                width: '100%',
                height: '100%'
            },


            plotlyData: [{
                displaylogo: false,
                type: 'scatter',
                mode: 'markers',
                // text: data.map((d) => {
                //     return ('<span>' +
                //     `${LC.PHASE_CNAME} = ${d[0].toFixed(DEC_PHASE)} <br>` +
                //     `${flux} = ${d[1]} mag <br>` +
                //     '</span>');
                // }),
                marker: {
                    symbol: 'circle',
                    size: 6,
                    //color: 'blue',
                    color: 'rgba(63, 127, 191, 0.5)'
                },
                x: data&&data.map((d) => d[0]),
                y: data&&data.map((d) => d[1]),
                hoverinfo: 'text'
            }],
            plotlyLayout: {
                hovermode: 'closest',
                title: period ? `period=${period} day` : 'period=',
                titlefont: {size: 16},
                xaxis: {
                    title: `${LC.PHASE_CNAME}`,
                    //range: [minPhase - Margin, minPhase + 2.0 + Margin],
                    gridLineWidth: 1,
                    type: 'linear',
                    lineColor: '#e9e9e9',
                    zeroline: false,
                    titlefont: {
                          size: 12
                        },
                    tickfont: {
                        size: 12
                    }
                },
                yaxis: {
                    title: `${flux} (mag)`,
                    gridLineWidth: 1,
                    type: 'linear',
                    lineColor: '#e9e9e9',
                    // tickwidth: 1,
                    // ticklen: 1,
                    autorange: 'reversed',
                    titlefont: {
                        size: 12
                    }
                },
                margin: {
                    l: 50,
                    r: 5,
                    b: 50,
                    t: 50,
                    pad: 2
                  }
            }
        };
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        var isPhaseChanged = (newFields) => {
            var fieldsToCheck = ['tz', 'period'];
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
        this.unbinder= FieldGroupUtils.bindToStore(pfinderkey, (fields) => {
            if (this && this.iAmMounted && isPhaseChanged(fields, this.state.fields)) {
                this.setState({fields});
                // chart.setTitle({text: period ? `period=${period}(day)`:'period='});
                // chart.series[0].setData(data);
                // chart.xAxis[0].update({min: minPhase - Margin,
                //     max: minPhase + 2.0 + Margin});
                // chart.yAxis[0].setTitle({text: `${flux}(mag)`});
                this.chartingInfo= this.regenData(fields);
            }
        });
    }

    afterRedraw(chart, pl) {
        const {lastData}= this;  // data can also be access from chart.data ??
        chart.on('plotly_hover', (eventData) => {
            const pointNumber= eventData.points[0].pointNumber;
            const str= '<span>' +
            `${LC.PHASE_CNAME} = ${lastData[pointNumber][0].toFixed(DEC_PHASE)} <br>` +
            `${this.lastFluxCol} = ${lastData[pointNumber][1]} mag <br>` +
            '</span>';

            this.setState( {
                dataUpdate: { text : str }
            } );
        });

    }


    render() {
        const {dataUpdate} = this.state;
        const {plotlyData, plotlyDivStyle, plotlyLayout}= this.chartingInfo;

        return (
            <Resizable className='ChartPanel__chartresizer'
                       onResize={this.onResize}>
                <PlotlyWrapper data={plotlyData} layout={plotlyLayout}  style={plotlyDivStyle}
                               dataUpdate={dataUpdate}
                               divUpdateCB={(div) => this.chart= div}
                               newPlotCB={this.afterRedraw}
                />
            </Resizable>
        );
    }

}


//<ReactHighcharts config={this.state.config} isPureConfig={true} ref='chart'/>

PhaseFoldingChart.propTypes = {
    showTooltip: PropTypes.bool
};


/**
 * @summary get flux series in terms of folded phase
 * @param {Object} fields
 * @returns {*}
 */
function getPhaseFlux(fields) {
    const fc = get(fields, ['flux', 'value'], '');
    var rawTable = getFullRawTable();
    var {data} = get(rawTable, ['tableData'], {});

    if (!data) return {flux: fc};

    var {time, period, tzero} = fields;
    const tc = time ? time.value : '';

    const tIdx = tc ? getColumnIdx(rawTable, tc) : -1;  // find time column
    const fIdx = fc ? getColumnIdx(rawTable, fc) : -1;  // find flux column

    if (tIdx < 0 || fIdx < 0) return {};

    var pd, tz;

    if (period){
        if (!period.valid) return {flux: fc};    // invalid period
        pd = parseFloat(period.value);
    } else {
        pd = periodRange.min;
    }
    if (tzero) {
        if (!tzero.valid) return {flux: fc};      // invalid tzero
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
class LcPFOptionsBox extends PureComponent {
    constructor(props) {
        super(props);
        var fields = FieldGroupUtils.getGroupFields(pfinderkey);

        const period = getValidValueFrom(fields, 'period');       // same as for 'accept period'
        const lastPeriod = '';                                    // used for 'revert to' button display
        this.state = {fields, period, lastPeriod, periodList: []};
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {
        this.iAmMounted= true;
        this.unbinder= FieldGroupUtils.bindToStore(pfinderkey, (fields) => {
            if (this && this.iAmMounted && fields !== this.state.fields) {

                var {periodList, period, lastPeriod} = this.state;
                var newPeriod = getValidValueFrom(fields, 'period');
                var bRevert = (lastFrom(periodList) === 'revert');
                var periodToList = '';

                if (bRevert) {                 // revert from last period
                    period = newPeriod;
                    periodList.pop();          // remove 'revert' flag on the top
                    lastPeriod = lastFrom(periodList);
                } else {
                    if (!newPeriod) {         // new period is empty string
                        if (period) periodToList = period;   // save current period
                        period = '';
                    } else if (newPeriod !== period) {      // new period not empty string, different from current
                        if (period) {                       // save current period
                            periodToList = period;
                        } else {                            // current period is empty
                            if (lastFrom(periodList) === newPeriod) {
                                periodList.pop();
                                lastPeriod = lastFrom(periodList);
                            }
                        }
                        period = newPeriod;
                    }
                    if (periodToList && periodToList !== lastFrom(periodList)) {      // current -> top of history if not exist
                        periodList.push(periodToList);
                        lastPeriod = periodToList;
                    }
                }

                var newState = {fields, lastPeriod, period, periodList};

                this.setState(newState);
            }
        });
    }


    render() {
        var optionProps = pick(this.state, ['fields', 'lastPeriod', 'periodList']);

        return (
                <LcPFOptions {...optionProps} />
        );
    }
}


/**
 * @summary light curve phase folding FieldGroup rendering
 */

function LcPFOptions({fields, lastPeriod, periodList=[]}) {

    if (!fields) return <span />;   // when there is no field defined in the beginning

    const maxPeriod = getValidValueFrom(fields, 'periodMax');
    const minPeriod = getValidValueFrom(fields, 'periodMin');

    const revertPeriodTxt = `Undo: ${lastPeriod ? lastPeriod : ''}`;
    var minPerN = minPeriod ? parseFloat(minPeriod) : periodRange.min;
    var maxPerN = maxPeriod ? parseFloat(maxPeriod) : periodRange.max;
    var period = get(fields, [fKeyDef.period.fkey, 'value']) || `${periodRange.min}`;

    // marks on the slider, marks on two ends: min and SliderMax
    var getSliderMarks = (periodMin, periodMax, sliderMin, sliderMax, noMarks) => {
        const sliderSize = sliderMax - sliderMin;
        const intMark = (periodMax - periodMin)/(noMarks - 1);

        var markStyle = (per, val) => ({style: {left: `${per}%`, marginLeft: -16, width: 40}, label: `${val}`});

        // make an array like [0, 1, 2, ...] and produce marks between two ends of Slider min and max
        return [...new Array(noMarks).keys()].reduce((prev, m) => {
            var val = parseFloat((intMark * m + periodMin).toFixed(DEC_PHASE));
            var per = 100*(val - sliderMin)/sliderSize;

            prev = Object.assign(prev, {[val]: markStyle(per, val)});
            return prev;
        }, {});
    };

    var revertPeriod = () => {
        if ((periodList.length >= 1) && (periodList[periodList.length-1] === lastPeriod)) {
            periodList[periodList.length-1] = 'revert';      // a flag to block adding new item to the list in store{

            dispatchValueChange({
                fieldKey: fKeyDef.period.fkey,
                groupKey: pfinderkey,
                value: lastPeriod
            });
        }
    };

    const NOMark = 5;
    const {min:sliderMin, max:sliderMax, stepSize} = adjustSliderEnds(minPerN, maxPerN);  //number
    var marks = getSliderMarks(minPerN, maxPerN, sliderMin, sliderMax, NOMark);
    var offset = 16;
    var highlightW = PanelResizableStyle.width-panelSpace - offset;
    var pSize = panelSpace/2 - 5;
    let styleItem = {display: 'list-item', marginLeft: '10px', paddingBottom:'20px'};
    let innerItem = {display: 'inline-flex', maxWidth: '100%', alignItems: 'center'};
    return (<div style={{width: PanelResizableStyle.width - offset}}>
                <div style={{display:'flex', flexDirection:'row-reverse', justifyContent:'space-between'}}>
                    <HelpIcon helpId={'findpTSV.settings'}/>
                </div>
            {'Vary period and time offset while visualizing phase folded plot. ' +
            'Optionally calculate periodogram. ' +
            'When satisfied, click Accept to return to Time Series Viewer with phase folded table.'}
                <br/>
            <br/>
            <br/>
            {ReadOnlyText({
                label: get(defValues, [fKeyDef.time.fkey, 'label']),
                               labelWidth: get(defValues, [fKeyDef.time.fkey, 'labelWidth']),
                content: get(fields, [fKeyDef.time.fkey, 'value'])
            })}
                <br/>
            {ReadOnlyText({
                label: get(defValues, [fKeyDef.flux.fkey, 'label']),
                               labelWidth: get(defValues, [fKeyDef.flux.fkey, 'labelWidth']),
                content: get(fields, [fKeyDef.flux.fkey, 'value'])
            })}
                <br/>
            <h3>{'Set Period'}</h3>
            <div style={styleItem}>
                <div style={innerItem}>
                    <ValidationField fieldKey={fKeyDef.period.fkey} label='Enter manually:'/>
                    <button type='button' className='button std hl'
                            onClick={() => resetPeriodDefaults(defPeriod)}>
                        <b>Reset</b>
                    </button>
                </div>
            </div>
            <div style={styleItem}>
                <div>{'Slide to select:'}</div>
                <div style={innerItem}>
                    <ValidationField fieldKey={fKeyDef.min.fkey} style={{width: pSize}}/>
                    <RangeSlider fieldKey={fKeyDef.period.fkey}
                                 min={sliderMin}
                                 max={sliderMax}
                                 minStop={minPerN}
                                 maxStop={maxPerN}
                                 marks={marks}
                                 step={stepSize}
                                 tooptip={'slide to set period value'}
                                 slideValue={period}
                                 defaultValue={minPerN}
                                 wrapperStyle={{marginBottom: 20, marginLeft: 9, marginRight: 25, width: highlightW}}
                                 decimalDig={DEC_PHASE}
                    />
                    <ValidationField fieldKey={fKeyDef.max.fkey} style={{width: pSize}}/>
                </div>
                        </div>
            <div style={styleItem}>
                <div style={innerItem}>
                    {'Calculate periodogram (below) and click to select period.'}
                    </div>
                    </div>
            <div style={{display:'flex', alignItems:'center'}}>
                <h3>{'Set Time Offset:'}</h3><ValidationField style={{marginLeft:'20px'}} fieldKey={fKeyDef.tz.fkey} label=''/>
            </div>
            <br/>
            <div style={{display: 'flex', alignItems:'center'}}>
                <CompleteButton
                    groupKey={[pfinderkey]}
                    onSuccess={setPFTableSuccess()}
                    onFail={setPFTableFail()}
                    text={'Accept'}
                    includeUnmounted={true}
                />
                <div style={{margin: 5}}>
                    <button type='button' className='button std hl' onClick={()=>cancelStandard()}>Cancel</button>
                </div>
                <HelpIcon helpId={'findpTSV.acceptp'}/>
                </div>
            </div>
    );
}

LcPFOptions.propTypes = {
    lastPeriod: PropTypes.string,
    periodList: PropTypes.arrayOf(PropTypes.string),
    fields: PropTypes.object
};

/**
 * @summary phase folding parameter reducer  // TODO: not sure how this works
 * @param {object} initState
 * @return {object}
 */
var LcPFReducer= (initState) => {
        var initPeriodValues = (fromFields) => {
            Object.keys(defPeriod).forEach((key) => {
                set(defPeriod, [key, 'value'], get(fromFields, [key, 'value']));
            });
        };

        return (inFields, action) => {
            if (!inFields) {
                var defV = Object.assign({}, defValues);
                const {min, max, time, flux, tzero,  tzeroMax} = initState || {};

                set(defV, [fKeyDef.min.fkey, 'value'], `${min}`);
                set(defV, [fKeyDef.max.fkey, 'value'], `${max}`);
                set(defV, [fKeyDef.time.fkey, 'value'], time);
                set(defV, [fKeyDef.flux.fkey, 'value'], flux);
                //set(defV, [fKeyDef.period.fkey, 'value'], `${min}`);
                set(defV, [fKeyDef.tz.fkey, 'value'], `${tzero}`);
                set(defV, [fKeyDef.tzmax.fkey, 'value'], `${tzeroMax}`);

                set(defV, [fKeyDef.period.fkey, 'tooltip'], `Period for phase folding, within [${min} (i.e. 1 sec), ${max}(suggestion)]`);
                set(defV, [fKeyDef.tz.fkey, 'tooltip'], `time zero, within [${tzero}, ${tzeroMax}`);

                set(defV, [fKeyDef.min.fkey, 'validator'], periodMinValidator('minimum period'));
                set(defV, [fKeyDef.max.fkey, 'validator'], periodMaxValidator('maximum period'));
                set(defV, [fKeyDef.tz.fkey, 'validator'], timezeroValidator('time zero'));
                set(defV, [fKeyDef.period.fkey, 'validator'], periodValidator('pediod'));

                set(defV, [fKeyDef.period.fkey, 'errMsg'], periodErr);

                initPeriodValues(defV);
                return defV;
            } else {
                switch (action.type) {
                    case FieldGroupCntlr.MOUNT_FIELD_GROUP:
                        initPeriodValues(inFields);
                        break;
                    case FieldGroupCntlr.VALUE_CHANGE:
                        var period = getValidValueFrom(inFields, fKeyDef.period.fkey);
                        const maxPer = getValidValueFrom(inFields, fKeyDef.max.fkey);
                        const minPer = getValidValueFrom(inFields, fKeyDef.min.fkey);


                        // key: period: increase max in case updated period is greater than max or less than min
                        //      max:    decrease period in case updated max is less than period
                        //      min:    increase period in case updated min is greater than min

                        if (period && (action.payload.fieldKey === fKeyDef.max.fkey)) {
                            if (maxPer && (parseFloat(period) > parseFloat(maxPer))) {
                                inFields = updateSet(inFields, [fKeyDef.period.fkey, 'value'], maxPer);
                            }
                        } else if (period && (action.payload.fieldKey === fKeyDef.min.fkey)) {
                            if (minPer && parseFloat(period) < parseFloat(minPer)) {
                                inFields = updateSet(inFields, [fKeyDef.period.fkey, 'value'], minPer);
                            }
                        } else if (period && (action.payload.fieldKey === fKeyDef.period.fkey)) {
                            if (maxPer && (parseFloat(period) > parseFloat(maxPer))) {
                                inFields = updateSet(inFields, [fKeyDef.max.fkey, 'value'], period);
                            } else if (minPer && isBetween(periodRange.min, parseFloat(minPer), parseFloat(period))) {
                                inFields = updateSet(inFields, [fKeyDef.min.fkey, 'value'], period);
                            }
                        }
                        var retval;
                        period = get(inFields, [fKeyDef.period.fkey, 'value']);

                        var validatePeriod = (valStr, min, max) => {
                            var ret = Validate.isFloat('period', valStr);

                            if (!ret.valid || !valStr) return {valid: false, message: 'period must be a float'};
                            var minV = parseFloat(min);
                            var maxV = parseFloat(max);
                            var val = parseFloat(valStr);

                            return (val >= minV && val <= maxV) ? {valid: true} :
                                    {valid:false, message: `period must be between ${minV} and ${maxV}`};

                        };

                        // recheck period min & period after period max is updated as valid
                        if (maxPer && (action.payload.fieldKey === fKeyDef.max.fkey)) {
                            var minP = get(inFields, [fKeyDef.min.fkey, 'value']);

                            retval = isPeriodMinValid(minP, 'minimum period');
                            inFields = updateSet(inFields, [fKeyDef.min.fkey, 'valid'], retval.valid);
                            inFields = updateSet(inFields, [fKeyDef.min.fkey, 'message'], retval.message);

                            if (retval.valid) {
                                retval = validatePeriod(period, minP, maxPer);
                                inFields = updateSet(inFields, [fKeyDef.period.fkey, 'valid'], retval.valid);
                                inFields = updateSet(inFields, [fKeyDef.period.fkey, 'message'], retval.message);
                            }

                        }
                        // recheck period max & period after period min is updated as valid
                        if (minPer && (action.payload.fieldKey === fKeyDef.min.fkey)) {
                            var maxP = get(inFields, [fKeyDef.max.fkey, 'value']);

                            retval = isPeriodMaxValid(maxP, 'maximum period');
                            inFields = updateSet(inFields, [fKeyDef.max.fkey, 'valid'], retval.valid);
                            inFields = updateSet(inFields, [fKeyDef.max.fkey, 'message'], retval.message);

                            if (retval.valid) {
                                retval = validatePeriod(period, minPer, maxP);
                                inFields = updateSet(inFields, [fKeyDef.period.fkey, 'valid'], retval.valid);
                                inFields = updateSet(inFields, [fKeyDef.period.fkey, 'message'], retval.message);
                            }
                        }
                        break;
                    default:
                        break;
                } 
            }
            return Object.assign({}, inFields);
        };
    };

/**
 * @summary validate minimum period
 * @param valStr
 * @param description
 * @returns {*}
 */
var isPeriodMinValid = (valStr, description) => {
    var retval;

    retval = Validate.isFloat(description, valStr);
    if (!retval.valid || !valStr) return {valid: false, message: `${description}: must be a float`};

    var val = parseFloat(valStr);
    var max = getValidValueFrom(FieldGroupUtils.getGroupFields(pfinderkey), fKeyDef.max.fkey);
    var mmax = max ? parseFloat(max) : periodRange.max;

    return (val >= periodRange.min && val < mmax) ? {valid: true} :
            {valid: false, message: `${description}: must be between ${periodRange.min} and ${max}`};

};

/**
 * @summary validate maximum period
 * @param valStr
 * @param description
 * @returns {*}
 */
var isPeriodMaxValid = (valStr, description) => {
    var retval;

    retval = Validate.isFloat(description, valStr);
    if (!retval.valid || !valStr) return {valid: false, message: `${description}: must be a float`};

    var val = parseFloat(valStr);
    var min = getValidValueFrom(FieldGroupUtils.getGroupFields(pfinderkey), fKeyDef.min.fkey);
    var mmin = min ? parseFloat(min) : periodRange.min;

    return (val > mmin) ? {valid: true} :
                          {valid: false, message: `${description}: must be greater than ${mmin}`};
};

/**
 * @summary validator for periopd value
 * @param {string} description
 * @returns {function}
 */
function periodValidator(description) {
    return (valStr) => {
        var retRes = Validate.isFloat(description, valStr);

        if (retRes.valid) {
            if (!valStr || parseFloat(valStr) < periodRange.min) {
                return {
                    valid: false,
                    message:  `period: must be greater than ${periodRange.min}`
                    //message: `period: must be between ${min} and ${max}`
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
        return isPeriodMinValid(valStr, description);
    };
}

/**
 * @summary validator for maximum period
 * @param description
 * @returns {Function}
 */
function periodMaxValidator(description) {
    return (valStr) => {
        return isPeriodMaxValid(valStr, description);
    };
}
/**
 * @summary time zero validator
 * @param {string} description
 * @returns {function}
 */
function timezeroValidator(description) {
    return (valStr) => {
        var retRes = Validate.isFloat(description, valStr);

        if (retRes.valid) {
            if (!valStr || parseFloat(valStr) < 0) {
                return {
                    valid: false,
                    message: `${description}: must be greater than 0.0`
                };
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
        const reqData = get(request, pfinderkey);
        const timeName = get(reqData, fKeyDef.time.fkey);
        const period = get(reqData, fKeyDef.period.fkey);
        const flux = get(reqData, fKeyDef.flux.fkey);
        const tzero = get(reqData, fKeyDef.tz.fkey);

        doPFCalculate(flux, timeName, period, tzero);

        if (isDialogVisible(popupId)) {
            cancelPeriodogram();
        }
        if (isDialogVisible(INFO_POPUP)) {
            dispatchHideDialog(INFO_POPUP);
        }
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
 * @summary adjust the step and the ends for slider
 * @param {number} min
 * @param {number} max
 * @param {number} startStep
 * @returns {{steps: number, max: number}}
 */
function adjustSliderEnds(min, max, startStep = STEP) {
    var s;
    var newMax, newMin;
    const  MIN_TOTAL_STEP = 200;

    s = startStep;
    newMin = Math.round(min);

    while (true) {
        if ((max - newMin)/s < MIN_TOTAL_STEP) {
            s = s/2;
        } else {
            break;
        }
    }

    var totalSteps = Math.ceil((max - newMin)/s);
    newMax = newMin + totalSteps * s;

    return { min: newMin, max: newMax, steps: totalSteps, stepSize: s};
}

/**
 * @summary reset the period parameter setting
 * @param {object} defPeriod
 */
export function resetPeriodDefaults(defPeriod) {
    const fields = FieldGroupUtils.getGroupFields(pfinderkey);

    var multiVals = Object.keys(defPeriod).reduce((prev, fieldKey) => {
        var val = get(defPeriod, [fieldKey, 'value']);

        if (get(fields, [fieldKey, 'value']) !== val) {
            prev.push({fieldKey, value: val});
        }
        return prev;
    }, []);

    if (multiVals.length > 0) {
        dispatchMultiValueChange(pfinderkey, multiVals);
    }
}

