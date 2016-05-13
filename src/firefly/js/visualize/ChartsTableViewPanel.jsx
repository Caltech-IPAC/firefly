/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
// import {deepDiff} from '../util/WebUtil.js';

import {get, debounce, defer} from 'lodash';
import Resizable from 'react-component-resizable';


import {flux} from '../Firefly.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import * as TblUtil from '../tables/TableUtil.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {FilterInfo} from '../tables/FilterInfo.js';

import * as TableStatsCntlr from '../visualize/TableStatsCntlr.js';
import * as HistogramCntlr from '../visualize/HistogramCntlr.js';
import * as XYPlotCntlr from '../visualize/XYPlotCntlr.js';

import {LO_EXPANDED, dispatchSetLayoutMode} from '../core/LayoutCntlr.js';

import {getHighlighted} from './ChartUtil.js';
import XYPlotOptions from '../visualize/XYPlotOptions.jsx';
import {XYPlot} from '../visualize/XYPlot.jsx';

import HistogramOptions from '../visualize/HistogramOptions.jsx';
import Histogram from '../visualize/Histogram.jsx';

import {PopupPanel} from '../ui/PopupPanel.jsx';
import DialogRootContainer from '../ui/DialogRootContainer.jsx';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../core/ComponentCntlr.js';

import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
import SETTINGS from 'html/images/icons-2014/24x24_GearsNEW.png';
import ZOOM_IN from 'html/images/icons-2014/24x24_ZoomIn.png';
import ZOOM_ORIGINAL from 'html/images/icons-2014/Zoom1x-24x24-tmp.png';
import SELECT_ROWS from 'html/images/icons-2014/24x24_Checkmark.png';
import UNSELECT_ROWS from 'html/images/icons-2014/24x24_CheckmarkOff_Circle.png';
import FILTER_IN from 'html/images/icons-2014/24x24_FilterAdd.png';
import CLEAR_FILTERS from 'html/images/icons-2014/24x24_FilterOff_Circle.png';
import LOADING from 'html/images/gxt/loading.gif';


const SCATTER = 'scatter';
const HISTOGRAM = 'histogram';
const OPTIONS_WIDTH = 330;

const PREF_CHART_TYPE = 'pref.chartType';
const PREF_OPTIONS_SHOWN = 'pref.chartOptionsShown';

const selectionBtnStyle = {verticalAlign: 'top', paddingLeft: 20, cursor: 'pointer'};

class ChartsPanel extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            chartType: localStorage.getItem(PREF_CHART_TYPE) || SCATTER,
            optionsShown: !(localStorage.getItem(PREF_OPTIONS_SHOWN)==='false'),
            immediateResize: false
        };

        const normal = (size) => {
            if (size) {
                var widthPx = size.width - 10;
                var heightPx = size.height;
                //console.log('width: '+widthPx+', height: '+heightPx);
                if (widthPx !== this.state.widthPx || heightPx != this.state.heightPx) {
                    this.setState({widthPx, heightPx, immediateResize: false});
                }
            }
        };
        const debounced = debounce(normal, 100);
        this.onResize =  (size) => {
            if ( this.state.immediateResize) {
                normal(size);
            } else if (this.state.widthPx === 0) {
                defer(normal, size);
            } else {
                debounced(size);
            }
        };

        this.renderXYPlot = this.renderXYPlot.bind(this);
        this.renderHistogram = this.renderHistogram.bind(this);
        this.toggleOptions = this.toggleOptions.bind(this);
        this.displaySelectionOptions = this.displaySelectionOptions.bind(this);
        this.displayZoomOriginal = this.displayZoomOriginal.bind(this);
        this.addSelection = this.addSelection.bind(this);
        this.resetSelection = this.resetSelection.bind(this);
        this.displayClearFilters = this.displayClearFilters.bind(this);
        this.addFilter = this.addFilter.bind(this);
        this.clearFilters = this.clearFilters.bind(this);
        this.selectionNotEmpty = this.selectionNotEmpty.bind(this);
        this.renderSelectionButtons = this.renderSelectionButtons.bind(this);
        this.renderToolbar = this.renderToolbar.bind(this);
        this.renderChartSelection = this.renderChartSelection.bind(this);
        this.onChartTypeChange = this.onChartTypeChange.bind(this);
        this.renderOptions = this.renderOptions.bind(this);
    }

    shouldComponentUpdate(nextProps, nextState) {
        let doUpdate = nextState !== this.state ||
            nextProps.tblStatsData !== this.props.tblStatsData ||
            nextProps.expandedMode !== this.props.expandedMode;
        if (!doUpdate) {
            if (nextState.chartType === SCATTER) {
                // scatter plot
                doUpdate = nextProps.tblPlotData !== this.props.tblPlotData ||
                    (nextProps.tableModel &&
                    (nextProps.tableModel.highlightedRow !== get(this.props, 'tableModel.highlightedRow') ||
                     nextProps.tableModel.selectInfo !== get(this.props, 'tableModel.selectInfo') ));
            } else {
                // histogram
                doUpdate = nextProps.tblHistogramData !== this.props.tblHistogramData;
            }
        }
        return Boolean(doUpdate);
    }

    componentDidMount() {
        this.handlePopups();
    }

    componentDidUpdate() {
        this.handlePopups();
    }

    componentWillUnmount() {
        const {tblId} = this.props;
        const popupId = getChartOptionsPopupId(tblId);
        if (isDialogVisible(popupId)) {
            hideChartOptionsPopup(tblId);
        }
    }

    handlePopups() {
        const {tblId, optionsPopup} = this.props;
        const popupId = getChartOptionsPopupId(tblId);
        if (optionsPopup) {
            const {optionsShown, chartType} = this.state;
            const {tableModel, tblStatsData} = this.props;
            if (optionsShown) {
                // show options popup
                const popupId = getChartOptionsPopupId(tblId);
                let popupTitle = 'Chart Options';

                const reqTitle = get(tableModel, 'tableMeta.title');
                if (reqTitle) { popupTitle += ': '+reqTitle; }

                var popup = (
                    <PopupPanel title={popupTitle} closeCallback={()=>{this.toggleOptions();}}>
                        <div
                            style={{overflow:'auto',width:OPTIONS_WIDTH,height:300,paddingTop:10,paddingLeft:10,verticalAlign:'top'}}>
                            {this.renderChartSelection()}
                            <OptionsWrapper {...{tblId, tableModel, tblStatsData, chartType}}/>
                        </div>
                    </PopupPanel>
                );

                DialogRootContainer.defineDialog(popupId, popup);
                dispatchShowDialog(popupId);

            } else if (isDialogVisible(popupId)) {
                hideChartOptionsPopup(tblId);
            }
        } else if (isDialogVisible(popupId)) {
            hideChartOptionsPopup(tblId);
        }
    }

    // -------------
    // SCATTER PLOT
    // -------------

    renderXYPlot() {
        const {tblId, tableModel, tblPlotData} = this.props;
        if (!tblPlotData) {
            return null;
        }
        const { isPlotDataReady, xyPlotData, xyPlotParams } = tblPlotData;
        var {widthPx, heightPx} = this.state;

        const hRow = getHighlighted(xyPlotParams, tblId);
        const sInfo = tableModel && tableModel.selectInfo;

        if (isPlotDataReady) {
            if (!heightPx || !widthPx) { return (<div/>); }
            return (
                <XYPlot data={xyPlotData}
                        desc=''
                        width={widthPx}
                        height={heightPx}
                        params={xyPlotParams}
                        highlighted={hRow}
                        onHighlightChange={(highlightedRow) => {
                                    TablesCntlr.dispatchTableHighlight(tblId, highlightedRow);
                                }
                           }
                        selectInfo={sInfo}
                        onSelection={(selection) => {
                            if (this.selectionNotEmpty(selection)) {defer(XYPlotCntlr.dispatchSetSelection, tblId, selection);}
                        }}
                />
            );
        } else {
            if (xyPlotParams) {
                return 'Loading XY plot...';
            } else {
                return null;
            }
        }

    }

    // ----------
    // HISTOGRAM
    // ----------


    renderHistogram() {
        if (!this.props.tblHistogramData) {
            return 'Select Histogram Parameters...';
        }
        const { isColDataReady, histogramData, histogramParams } = this.props.tblHistogramData;
        var {widthPx, heightPx} = this.state;

        if (isColDataReady) {
            var logs, reversed;
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

            if (!heightPx || !widthPx) { return (<div/>); }
            return (
                <Histogram data={histogramData}
                           desc={histogramParams.columnOrExpr}
                           binColor='#8c8c8c'
                           height={heightPx}
                           width={widthPx}
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
    }

    // -----------------
    // COMMON RENDERING
    // -----------------

    toggleOptions() {
        const {optionsShown, immediateResize, optionsPopup} = this.state;
        localStorage.setItem(PREF_OPTIONS_SHOWN, !optionsShown);
        this.setState({optionsShown: !optionsShown, immediateResize: optionsPopup?immediateResize:true});
    }

    displaySelectionOptions() {
        if (this.state.chartType === SCATTER) {
            const selection = get(this.props, 'tblPlotData.xyPlotParams.selection');
            return Boolean(selection);
        }
        // for now selection is supported for scatter only
        return false;
    }

    displayZoomOriginal() {
        if (this.state.chartType === SCATTER) {
            const zoom = get(this.props, 'tblPlotData.xyPlotParams.zoom');
            return Boolean(zoom);
        }
        // for now zoom is supported for scatter only
        return false;
    }

    addZoom() {
        if (this.state.chartType === SCATTER) {
            XYPlotCntlr.dispatchZoom(this.props.tblId, get(this.props, 'tblPlotData.xyPlotParams.selection'));
        }
    }

    resetZoom() {
        if (this.state.chartType === SCATTER) {
            XYPlotCntlr.dispatchZoom(this.props.tblId);
        }
    }

    displayUnselectAll  () {
        if (this.state.chartType === SCATTER) {
            const selectInfo = get(this.props, 'tableModel.selectInfo');
            return selectInfo && (selectInfo.selectAll || selectInfo.exceptions.size>0);
        }
    }

    addSelection() {
        if (this.state.chartType === SCATTER) {
            const {tblId, tableModel} = this.props;
            const selection = get(this.props, 'tblPlotData.xyPlotParams.selection');
            const rows = get(this.props, 'tblPlotData.xyPlotData.rows');
            if (tableModel && rows && selection) {
                const {xMin, xMax, yMin, yMax} = selection;
                const selectInfoCls = SelectInfo.newInstance({rowCount: tableModel.totalRows});
                // add all rows which fall into selection
                const xIdx = 0, yIdx = 1;
                rows.forEach((arow, index) => {
                    const x = Number(arow[xIdx]);
                    const y = Number(arow[yIdx]);
                    if (x>=xMin && x<=xMax && y>=yMin && y<=yMax) {
                        selectInfoCls.setRowSelect(index, true);
                    }
                });
                const selectInfo = selectInfoCls.data;
                TablesCntlr.dispatchTableSelect(tblId, selectInfo);
            }
        }
    }

    resetSelection() {
        if (this.state.chartType === SCATTER) {
            const {tblId, tableModel} = this.props;
            if (tableModel) {
                const selectInfoCls = SelectInfo.newInstance({rowCount: tableModel.totalRows});
                TablesCntlr.dispatchTableSelect(tblId, selectInfoCls.data);
            }
        }
    }

    displayClearFilters() {
        const filterInfo = get(this.props, 'tableModel.request.filters');
        const filterCount = filterInfo ? filterInfo.split(';').length : 0;
        return (filterCount > 0);
    }

    addFilter() {
        if (this.state.chartType === SCATTER) {
            const {tblPlotData, tableModel} = this.props;
            const selection = get(tblPlotData, 'xyPlotParams.selection');
            const xCol = get(tblPlotData, 'xyPlotParams.x.columnOrExpr');
            const yCol = get(tblPlotData, 'xyPlotParams.y.columnOrExpr');
            if (selection && xCol && yCol) {
                const {xMin, xMax, yMin, yMax} = selection;
                const filterInfo = get(this.props, 'tableModel.request.filters');
                const filterInfoCls = FilterInfo.parse(filterInfo);
                filterInfoCls.setFilter(xCol, '> '+xMin);
                filterInfoCls.addFilter(xCol, '< '+xMax);
                filterInfoCls.setFilter(yCol, '> '+yMin);
                filterInfoCls.addFilter(yCol, '< '+yMax);
                const newRequest = Object.assign({}, tableModel.request, {filters: filterInfoCls.serialize()});
                TablesCntlr.dispatchTableFetch(newRequest);
            }
        }
    }

    clearFilters() {
        const request = get(this.props, 'tableModel.request');
        if (request && request.filters) {
            const newRequest = Object.assign({}, request, {filters: ''});
            TablesCntlr.dispatchTableFetch(newRequest);
        }
    }

    selectionNotEmpty(selection) {
        const rows = get(this.props, 'tblPlotData.xyPlotData.rows');
        if (rows && selection) {
            const {xMin, xMax, yMin, yMax} = selection;
            const xIdx = 0, yIdx = 1;
            const aPt = rows.find((arow) => {
                const x = Number(arow[xIdx]);
                const y = Number(arow[yIdx]);
                return (x >= xMin && x <= xMax && y >= yMin && y <= yMax);
            });
            return Boolean(aPt);
        } else {
            return false;
        }

    }

    renderSelectionButtons() {
        if (this.displaySelectionOptions()) {
            return (
                <div style={{display:'inline-block', whiteSpace: 'nowrap'}}>
                    <img style={selectionBtnStyle}
                         title='Zoom in the enclosed points'
                         src={ZOOM_IN}
                         onClick={() => this.addZoom()}
                    />
                    {!get(this.props, 'tblPlotData.xyPlotData.decimateKey') && <img style={selectionBtnStyle}
                         title='Select enclosed points'
                         src={SELECT_ROWS}
                         onClick={() => this.addSelection()}
                    />}
                    <img style={selectionBtnStyle}
                         title='Filter in the selected points'
                         src={FILTER_IN}
                         onClick={() => this.addFilter()}
                    />
                </div>
            );
        } else {
            return (
                <div style={{display:'inline-block', whiteSpace: 'nowrap'}}>
                    {this.displayZoomOriginal() && <img style={selectionBtnStyle}
                         title='Zoom out to original chart'
                         src={ZOOM_ORIGINAL}
                         onClick={() => this.resetZoom()}
                    />}
                    {this.displayUnselectAll() && <img style={selectionBtnStyle}
                         title='Unselect all selected points'
                         src={UNSELECT_ROWS}
                         onClick={() => this.resetSelection()}
                    />}
                    {this.displayClearFilters() && <img style={selectionBtnStyle}
                        title='Remove all filters'
                        src={CLEAR_FILTERS}
                        onClick={() => this.clearFilters()}
                    />}
                </div>
            );
        }
    }

    renderToolbar() {
        const {expandable, expandedMode} = this.props;
        return (
            <div style={{height: 30, position: 'absolute', top: 0, left: 0, right: 0}}>
                <img style={{verticalAlign:'top', float: 'left', cursor: 'pointer'}}
                     title='Plot options and tools'
                     src={SETTINGS}
                     onClick={() => this.toggleOptions()}
                />
                <div style={{display:'inline-block', float: 'right'}}>
                    {this.renderSelectionButtons()}
                    { expandable && !expandedMode &&
                    <img style={selectionBtnStyle}
                         title='Expand this panel to take up a larger area'
                         src={OUTLINE_EXPAND}
                         onClick={() => dispatchSetLayoutMode(LO_EXPANDED.xyPlots)}
                    />
                    }
                </div>
            </div>
        );
    }

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

    renderOptions() {
        const {optionsShown, chartType, heightPx} = this.state;
        const { tblId, tableModel, tblStatsData, optionsPopup} = this.props;
        if (optionsShown) {
            if (!optionsPopup) {
                return (
                    <div style={{flex: '0 0 auto',overflow:'auto',width:OPTIONS_WIDTH,height:heightPx,paddingLeft:10,verticalAlign:'top'}}>
                        {this.renderChartSelection()}
                        <OptionsWrapper  {...{tblId, tableModel, tblStatsData, chartType}}/>
                    </div>
                );
            }
        }
        return false;
    }



    render() {
        var {tblStatsData} = this.props;

        if (!(tblStatsData && tblStatsData.isColStatsReady) ) {
            return (<img style={{verticalAlign:'top', height: 16, padding: 10, float: 'left'}}
                         title='Loading Table Statistics...'
                         src={LOADING}/>
            );
        } else {
            var {widthPx, heightPx, chartType} = this.state;
            const knownSize = widthPx && heightPx;

            return (
                <div style={{ display: 'flex', flex: 'auto', flexDirection: 'column', overflow: 'hidden'}}>
                    <div style={{ position: 'relative', flexGrow: 1}}>
                        {this.renderToolbar()}
                        <div style={{display: 'flex', flexDirection: 'row', position: 'absolute', top: 30, bottom: 0, left: 0, right: 0}}>
                            {this.renderOptions()}
                            <Resizable id='chart-resizer' onResize={this.onResize} style={{flexGrow: 1, position: 'relative', width: '100%', height: '100%', overflow: 'hidden'}}>
                                <div style={{overflow:'auto',width:widthPx,height:heightPx,paddingLeft:10}}>
                                    {knownSize ? chartType === SCATTER ? this.renderXYPlot() : this.renderHistogram() : <div/>}
                                </div>
                            </Resizable>
                        </div>
                    </div>
                </div>
            );
        }
    }
}

ChartsPanel.propTypes = {
    expandedMode: PropTypes.bool,
    optionsPopup: PropTypes.bool,
    expandable: PropTypes.bool,
    tblId : PropTypes.string,
    tableModel : PropTypes.object,
    tblStatsData : PropTypes.object,
    tblPlotData : PropTypes.object,
    tblHistogramData : PropTypes.object,
    width : PropTypes.string,
    height : PropTypes.string
};

ChartsPanel.defaultProps = {
    expandedMode: false,
    expandable: true
};

export class ChartsTableViewPanel extends Component {

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
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
    }

    getNextState() {
        const tblId = this.props.tblId || TblUtil.getActiveTableId();
        const tableModel = TblUtil.getTblById(tblId);
        const tblStatsData = flux.getState()[TableStatsCntlr.TBLSTATS_DATA_KEY][tblId];
        const tblHistogramData = flux.getState()[HistogramCntlr.HISTOGRAM_DATA_KEY][tblId];
        const tblPlotData = flux.getState()[XYPlotCntlr.XYPLOT_DATA_KEY][tblId];
        return {tblId, tableModel, tblStatsData, tblHistogramData, tblPlotData};
    }

    storeUpdate() {
        this.setState(this.getNextState());
    }

    render() {
        const {tblId, tableModel, tblStatsData, tblHistogramData, tblPlotData} = this.state;
        return (
            <ChartsPanel {...this.props} {...{tblId, tableModel, tblStatsData, tblHistogramData, tblPlotData}}/>
        );
    }
}

export class OptionsWrapper extends React.Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps) {
        return get(nProps, 'tableModel.tbl_id') !== get(this.props, 'tableModel.tbl_id') ||
            get(nProps, 'tblStatsData.isColStatsReady') !== get(this.props, 'tblStatsData.isColStatsReady') ||
            nProps.chartType != this.props.chartType;
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

    render() {
        const { tblId, tableModel, tblStatsData, chartType} = this.props;

        if (get(tblStatsData,'isColStatsReady')) {
            if (chartType === SCATTER) {
                const formName = 'XYPlotOptionsForm_' + tblId;
                return (
                    <XYPlotOptions key={formName} groupKey={formName}
                                   colValStats={tblStatsData.colStats}
                                   onOptionsSelected={(xyPlotParams) => {
                                                XYPlotCntlr.dispatchLoadPlotData(xyPlotParams, tableModel.request);
                                            }
                                          }/>
                );
            } else {
                const formName = 'HistogramOptionsForm_'+this.props.tblId;
                return (
                    <HistogramOptions key={formName} groupKey = {formName}
                                      colValStats={tblStatsData.colStats}
                                      onOptionsSelected={(histogramParams) => {
                                                HistogramCntlr.dispatchLoadColData(histogramParams, tableModel.request);
                                            }
                                          }/>
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
    tblId : PropTypes.string,
    tableModel : PropTypes.object,
    tblStatsData : PropTypes.object,
    chartType: PropTypes.string
};


function getChartOptionsPopupId(tblId) {
    return `chartOptions_${tblId}`;
}


function hideChartOptionsPopup(tblId) {
    const popupId = getChartOptionsPopupId(tblId);
    dispatchHideDialog(popupId);
}
