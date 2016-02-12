import React, {PropTypes} from 'react';

import {get, throttle, defer} from 'lodash';
import Resizable from 'react-component-resizable';

import { connect } from 'react-redux';


import * as TablesCntlr from '../tables/TablesCntlr.js';
import * as TblUtil from '../tables/TableUtil.js';
import TableStatsCntlr from '../visualize/TableStatsCntlr.js';
import HistogramCntlr from '../visualize/HistogramCntlr.js';
import XYPlotCntlr from '../visualize/XYPlotCntlr.js';


import XYPlotOptions from '../visualize/XYPlotOptions.jsx';
import XYPlot from '../visualize/XYPlot.jsx';

import HistogramOptions from '../visualize/HistogramOptions.jsx';
import Histogram from '../visualize/Histogram.jsx';

import SETTINGS from 'html/images/icons-2014/24x24_GearsNEW.png';
import ZOOM_IN from 'html/images/icons-2014/24x24_ZoomIn.png';
import ZOOM_ORIGINAL from 'html/images/icons-2014/Zoom1x-24x24-tmp.png';
import SELECT_ROWS from 'html/images/icons-2014/24x24_Checkmark.png';
import UNSELECT_ROWS from 'html/images/icons-2014/24x24_CheckmarkOff_Circle.png';
import FILTER_IN from 'html/images/icons-2014/24x24_FilterAdd.png';
import LOADING from 'html/images/gxt/loading.gif';


const SCATTER = 'scatter';
const HISTOGRAM = 'histogram';
const OPTIONS_WIDTH = 350;


var ChartsPanel = React.createClass({

    propTypes: {
        tblId : PropTypes.string,
        table : PropTypes.object,
        tblStatsData : PropTypes.object,
        tblPlotData : PropTypes.object,
        tblHistogramData : PropTypes.object,
        width : PropTypes.string,
        height : PropTypes.string
    },

    getInitialState() {
        return {
            chartType : SCATTER,
            optionsShown : true,
            widthPx: 700,
            heightPx: 300,
            throttledResize: throttle(this.onResize, 500, {'leading':false, 'trailing':true})};
    },


    shouldComponentUpdate(nextProps, nextState) {
        let doUpdate = nextState !== this.state || nextProps.tblStatsData !== this.props.tblStatsData;
        if (!doUpdate) {
            if (nextState.chartType === SCATTER) {
                // scatter plot
                doUpdate = nextProps.tblPlotData !== this.props.tblPlotData ||
                    (nextProps.table && nextProps.table.highlightedRow !== this.props.table.highlightedRow);
            } else {
                // histogram
                doUpdate = nextProps.tblHistogramData !== this.props.tblHistogramData;
            }
        }
        return doUpdate;
    },

    onResize(size) {
        if (size) {
            this.setState({ widthPx: (size.width-10), heightPx: (size.height-30), throttledResize: this.state.throttledResize });
        }
    },

    componentDidMount() {
        this.onResize();
    },


    // -------------
    // SCATTER PLOT
    // -------------

    renderXYPlotOptions() {
        const { searchRequest, isColStatsReady, colStats } = this.props.tblStatsData;

        if (isColStatsReady) {
            const formName = 'XYPlotOptionsForm_'+this.props.tblId;
            return (
                <XYPlotOptions groupKey = {formName}
                                  colValStats={colStats}
                                  onOptionsSelected={(xyPlotParams) => {
                                            XYPlotCntlr.dispatchLoadPlotData(xyPlotParams, searchRequest);
                                        }
                                      }/>
            );
        } else {
            return (<img style={{verticalAlign:'top', height: 16, padding: 10, float: 'left'}}
                title='Loading Options...'
                src={LOADING}
            />);
        }

    },

    renderXYPlot() {
        const {tblId, table, tblPlotData} = this.props;
        if (!tblPlotData) {
            return 'Select XY plot parameters...';
        }
        const { isPlotDataReady, xyPlotData, xyPlotParams } = tblPlotData;
        var {widthPx, heightPx, optionsShown} = this.state;

        const hRow = table ? table.highlightedRow : undefined;
        const chartWidth = optionsShown ? widthPx-OPTIONS_WIDTH : widthPx;

        if (isPlotDataReady) {
            return (
                <XYPlot data={xyPlotData}
                        desc=''
                        width={chartWidth}
                        height={heightPx}
                        params={xyPlotParams}
                        highlightedRow={hRow}
                        onHighlightChange={(highlightedRow) => {
                                    TablesCntlr.dispatchHighlightRow(tblId, highlightedRow);
                                }
                           }
                        onSelection={(selection) => {
                            defer(XYPlotCntlr.dispatchSetSelection, tblId, selection);
                        }}
                />
            );
        } else {
            if (xyPlotParams) {
                return 'Loading XY plot...';
            } else {
                return 'Select XY plot parameters';
            }
        }

    },

    // ----------
    // HISTOGRAM
    // ----------

    renderHistogramOptions() {
        const { searchRequest, isColStatsReady, colStats } = this.props.tblStatsData;

        if (isColStatsReady) {
            const formName = 'HistogramOptionsForm_'+this.props.tblId;
            return (
                <HistogramOptions groupKey = {formName}
                                  colValStats={colStats}
                                  onOptionsSelected={(histogramParams) => {
                                            //console.log(histogramParams);
                                            HistogramCntlr.dispatchLoadColData(histogramParams, searchRequest);
                                        }
                                      }/>
            );
        } else {
            return 'Loading Options...';
        }

    },

    renderHistogram() {
        if (!this.props.tblHistogramData) {
            return 'Select Histogram Parameters...';
        }
        const { isColDataReady, histogramData, histogramParams } = this.props.tblHistogramData;
        var {heightPx} = this.state;

        if (isColDataReady) {
            var logs = undefined;
            var reversed = undefined;
            if (histogramParams) {
                var logvals = '';
                if (histogramParams.x.includes('log')) { logvals += 'x';}
                if (histogramParams.y.includes('log')) { logvals += 'y';}
                if (logvals.length>0) { logs = logvals;}

                var rvals = '';
                if (histogramParams.x.includes('flip')) { rvals += 'x';}
                if (histogramParams.y.includes('flip')) { rvals += 'y';}
                if (rvals.length>0) { reversed = rvals;}

            }
            return (
                <Histogram data={histogramData}
                           desc={histogramParams.columnOrExpr}
                           binColor='#8c8c8c'
                           height={heightPx}
                           logs={logs}
                           reversed={reversed}
                />
            );
        } else {
            if (histogramParams) {
                return 'Loading Histogram...';
            } else {
                return 'Select Histogram Parameters';
            }
        }
    },

    // -----------------
    // COMMON RENDERING
    // -----------------

    toggleOptions() {
        this.setState({optionsShown : !this.state.optionsShown});
    },

    displaySelectionOptions() {
        if (this.state.chartType === SCATTER) {
            const selection = get(this.props, 'tblPlotData.xyPlotParams.selection', null);
            return Boolean(selection);
        }
        // for now selection is supported for scatter only
        return false;
    },

    displayZoomOriginal() {
        if (this.state.chartType === SCATTER) {
            const zoom = get(this.props, 'tblPlotData.xyPlotParams.zoom');
            return Boolean(zoom);
        }
        // for now zoom is supported for scatter only
        return false;
    },

    addZoom() {
        if (this.state.chartType === SCATTER) {
            XYPlotCntlr.dispatchSetZoom(this.props.tblId, get(this.props, 'tblPlotData.xyPlotParams.selection'));
        }
    },

    resetZoom() {
        if (this.state.chartType === SCATTER) {
            XYPlotCntlr.dispatchResetZoom(this.props.tblId);
        }
    },

    addSelection() {

    },

    addFilter() {

    },

    renderSelectionButtons() {
        // todo: handling unselect
        const btnStyle = {verticalAlign: 'top', paddingLeft: 20};
        if (this.displaySelectionOptions()) {
            return (
                <div style={{display:'inline-block', whiteSpace: 'nowrap', float: 'right'}}>
                    <img style={btnStyle}
                         title='Zoom in the enclosed points'
                         src={ZOOM_IN}
                         onClick={() => this.addZoom()}
                    />
                    <img style={btnStyle}
                         title='Select enclosed points - ToDo'
                         src={SELECT_ROWS}
                         onClick={() => this.addSelection()}
                    />
                    <img style={btnStyle}
                         title='Filter in the selected points - ToDo'
                         src={FILTER_IN}
                         onClick={() => this.addFilter()}
                    />
                </div>
            );
        } else if (this.displayZoomOriginal()) {
            return (
                <div style={{display:'inline-block', whiteSpace: 'nowrap', float: 'right'}}>
                    <img style={btnStyle}
                         title='Zoom out to original chart'
                         src={ZOOM_ORIGINAL}
                         onClick={() => this.resetZoom()}
                    />
                </div>
            );
        } else {
            return null;
        }
    },

    renderToolbar() {
        return (
            <div style={{display:'block', whiteSpace: 'nowrap', height: 30}}>
                <img style={{verticalAlign:'top', float: 'left'}}
                     title='Plot options and tools'
                     src={SETTINGS}
                     onClick={() => this.toggleOptions()}
                />
                {this.renderSelectionButtons()}
            </div>
        );
    },

    renderChartSelection() {
        const {tblId} = this.props;
        const {chartType} = this.state;
        const fieldKey = 'chartType_'+tblId;
        return (
            <div style={{display:'block', whiteSpace: 'nowrap'}}>
                <input type='radio'
                       name={fieldKey}
                       value={SCATTER}
                       defaultChecked={chartType===SCATTER}
                       onChange={this.onChartTypeChange}
                /> Scatter Plot&nbsp;&nbsp;
                <input type='radio'
                       name={fieldKey}
                       value={HISTOGRAM}
                       defaultChecked={chartType===HISTOGRAM}
                       onChange={this.onChartTypeChange}
                /> Histogram&nbsp;&nbsp;
            </div>
        );
    },

    onChartTypeChange(ev) {
        // the value of the group is the value of the selected option
        var val = ev.target.value;
        var checked = ev.target.checked;

        if (checked) {
            if (val !== this.state.chartType) {
                this.setState({chartType : val});
            }
        }
    },

    renderOptions() {
        const {optionsShown, chartType, heightPx} = this.state;
        if (optionsShown) {
            return (
                <div style={{display:'inline-block',overflow:'auto',width:(OPTIONS_WIDTH-20),height:heightPx,border:'0px solid black', marginLeft:10}}>
                    {this.renderChartSelection()}
                    {chartType === SCATTER ? this.renderXYPlotOptions() : this.renderHistogramOptions()}
                </div>
            );
        } else {
            return null;
        }
    },

    render() {
        var {tblStatsData, width, height} = this.props;
        width = width || '100%';

        if (!tblStatsData) {
            return (<div>.....</div>);
        } else if (!tblStatsData.isTblLoaded) {
            return (<img style={{verticalAlign:'top', height: 16, padding: 10, float: 'left'}}
                         title='Loading Table...'
                         src={LOADING}/>
            );
        } else {
            var {widthPx, heightPx, chartType, optionsShown} = this.state;
            const chartWidth = optionsShown ? widthPx-OPTIONS_WIDTH : widthPx;
            return (
                <Resizable id='xyplot-resizer' style={{width, height}}
                           onResize={this.state.throttledResize} {...this.props} >
                    <div style={{display:'inline-block', verticalAlign:'bottom'}}>
                        <div style={{display:'block', whiteSpace: 'nowrap'}}>
                            {this.renderToolbar()}
                        </div>
                        <div style={{display:'block', whiteSpace: 'nowrap'}}>
                            {this.renderOptions()}
                            <div style={{display:'inline-block',overflow:'auto', width:chartWidth,height:heightPx,border:'0px solid black', marginLeft:10}}>
                                {chartType === SCATTER ? this.renderXYPlot() : this.renderHistogram()}
                            </div>
                        </div>
                    </div>
                </Resizable>
            );
        }
    }
});

const connector = function(state, ownProps) {
    return {
        tableModel: TblUtil.findById(ownProps.tblId),
        tblStatsData: get(state[TableStatsCntlr.TBLSTATS_DATA_KEY], ownProps.tblId),
        tblHistogramData: get(state[HistogramCntlr.HISTOGRAM_DATA_KEY], ownProps.tblId),
        tblPlotData: get(state[XYPlotCntlr.XYPLOT_DATA_KEY], ownProps.tblId)
    };
};

const ChartTableViewPanel = connect(connector)(ChartsPanel);

export default ChartTableViewPanel;

