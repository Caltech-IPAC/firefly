/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, { Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {get} from 'lodash';
// import {deepDiff} from '../util/WebUtil.js';

import {flux} from '../Firefly.js';
import * as TblUtil from '../tables/TableUtil.js';
import * as TableStatsCntlr from '../charts/TableStatsCntlr.js';
import * as ChartsCntlr from '../charts/ChartsCntlr.js';
import {uniqueChartId} from '../charts/ChartUtil.js';
import {XYPlotOptions} from '../charts/ui/XYPlotOptions.jsx';
import {DT_XYCOLS} from '../charts/dataTypes/XYColsCDT.js';
import {resultsSuccess as onXYPlotOptsSelected} from '../charts/ui/XYPlotOptions.jsx';
import {HistogramOptions} from '../charts/ui/HistogramOptions.jsx';
import {DT_HISTOGRAM} from '../charts/dataTypes/HistogramCDT.js';
import {resultsSuccess as onHistogramOptsSelected} from '../charts/ui/HistogramOptions.jsx';
//import {uniqueChartId} from '../charts/ChartUtil.js';

import {FormPanel} from './FormPanel.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';

import LOADING from 'html/images/gxt/loading.gif';

const dropdownName = 'ChartSelectDropDownCmd';

const SCATTER = 'scatter';
const HISTOGRAM = 'histogram';
const PREF_CHART_TYPE = 'pref.chartType';

function getFormName(chartType) {
    return chartType+'ChartOpts';
}

/**
 *
 * @param props
 * @return {XML}
 * @constructor
 */
class ChartSelect extends Component {
    constructor(props) {
        super(props);

        this.state = {
            chartType: localStorage.getItem(PREF_CHART_TYPE) || SCATTER
        };

        this.onChartTypeChange = this.onChartTypeChange.bind(this);
    }

    onChartTypeChange(ev) {
        // the value of the group is the value of the selected option
        var val = ev.target.value;
        var checked = ev.target.checked;

        if (checked) {
            if (val !== this.state.chartType) {
                localStorage.setItem(PREF_CHART_TYPE, val);
                this.setState({chartType : val});
            }
        }
    }

    renderChartSelection() {
        const {chartType} = this.state;
        const fieldKey = 'chartType';
        return (
            <div style={{display:'block', whiteSpace: 'nowrap', paddingBottom: 10}}>
                <input type='radio'
                       name={fieldKey}
                       value={SCATTER}
                       defaultChecked={chartType===SCATTER}
                       onChange={this.onChartTypeChange}
                /><span style={{paddingLeft: 3, paddingRight: 8}}>Scatter Plot</span>
                <input type='radio'
                       name={fieldKey}
                       value={HISTOGRAM}
                       defaultChecked={chartType===HISTOGRAM}
                       onChange={this.onChartTypeChange}
                /><span style={{paddingLeft: 3, paddingRight: 8}}>Histogram</span>
            </div>
        );
    }

    render() {
        const {tblId, tblStatsData} = this.props;
        const {chartType} = this.state;
        const formName = getFormName(chartType);

        const resultSuccess = (flds) => {
            //const chartId = uniqueChartId(chartType);
            const chartId = uniqueChartId(chartType); // before chart container is available we support one chart per table
            let onOptionsSelected = undefined;
            let type = undefined;
            switch (chartType) {
                case SCATTER:
                    onOptionsSelected = onXYPlotOptsSelected;
                    type = DT_XYCOLS;
                    break;
                case HISTOGRAM:
                    onOptionsSelected = onHistogramOptsSelected;
                    type = DT_HISTOGRAM;
                    break;
            }
            if (onOptionsSelected) {
                onOptionsSelected((options) => {
                    ChartsCntlr.dispatchChartAdd({chartId, chartType, groupId: tblId, deletable: true,
                        chartDataElements: [{type, options, tblId}]});
                }, flds, tblId);
            }
            hideSearchPanel();
        };

        return (
            <div style={{padding:10, overflow:'auto', maxWidth:600, maxHeight:600}}>
                <FormPanel
                    submitText='OK'
                    groupKey={formName}
                    onSubmit={resultSuccess}
                    onCancel={hideSearchPanel}>
                    {this.renderChartSelection()}
                    <OptionsWrapper  {...{tblStatsData, chartType}}/>
                </FormPanel>

            </div>);
    }
}

ChartSelect.propTypes = {
    tblId: PropTypes.string,
    tblStatsData : PropTypes.object
};


function hideSearchPanel() {
    dispatchHideDropDown();
}

export class OptionsWrapper extends React.Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

    render() {
        const {tblStatsData, chartType} = this.props;

        if (get(tblStatsData,'isColStatsReady')) {
            const formName = getFormName(chartType);
            if (chartType === SCATTER) {
                return (
                    <XYPlotOptions key={formName} groupKey={formName}
                                   colValStats={tblStatsData.colStats}/>
                );
            } else {
                return (
                    <HistogramOptions key={formName} groupKey = {formName}
                                      colValStats={tblStatsData.colStats}/>
                );
            }
        } else {
            return (<img style={{verticalAlign:'top', height: 16, padding: 10, float: 'left'}}
                         title='Loading Options...'
                         src={LOADING}
            />);
        }
    }
}

OptionsWrapper.propTypes = {
    tblStatsData : PropTypes.object,
    chartType: PropTypes.string
};




export class ChartSelectDropdown extends Component {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

    componentDidMount() {
        this.removeListener = flux.addListener(() => this.storeUpdate());
        this.iAmMounted= true;
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        this.removeListener && this.removeListener();
    }

    getNextState() {
        const tblId = TblUtil.getActiveTableId(this.props.tblGroup);
        const tblStatsData = tblId && flux.getState()[TableStatsCntlr.TBLSTATS_DATA_KEY][tblId];
        return {tblId, tblStatsData};
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const {tblId, tblStatsData} = this.getNextState();
            if (tblId !== this.state.tblId || tblStatsData !== this.state.tblStatsData) {
                this.setState(this.getNextState());
            }
        }
    }

    render() {
        const {tblId, tblStatsData} = this.state;
        return tblId ? (
            <ChartSelect {...{tblId, tblStatsData}} {...this.props}/>
        ) : (
            <div style={{padding:20, fontSize:'150%'}}>Charts are not available: no active table.</div>
        );
    }
}

ChartSelectDropdown.propTypes = {
    tblGroup: PropTypes.string, // if not present, default table group is used
    name: PropTypes.oneOf([dropdownName])
};


ChartSelectDropdown.defaultProps = {
    name: dropdownName
};
