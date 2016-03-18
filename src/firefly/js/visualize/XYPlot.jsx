/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isUndefined, debounce} from 'lodash';
import shallowequal from 'shallowequal';
import React, {PropTypes} from 'react';
import ReactHighcharts from 'react-highcharts/bundle/highcharts';

import {SelectInfo} from '../tables/SelectInfo.js';
//import {getFormatString} from '../util/MathUtil.js';

const axisParamsShape = PropTypes.shape({
    columnOrExpr : PropTypes.string,
    label : PropTypes.string,
    unit : PropTypes.string,
    options : PropTypes.string, // ex. 'grid,log,flip'
    nbins : PropTypes.number,
    min : PropTypes.number,
    max : PropTypes.number
});

const selectionShape = PropTypes.shape({
    xMin : PropTypes.number,
    xMax : PropTypes.number,
    yMin : PropTypes.number,
    yMax : PropTypes.number
});

const plotParamsShape = PropTypes.shape({
    xyRatio : PropTypes.number,
    stretch : PropTypes.oneOf(['fit','fill']),
    selection : selectionShape,
    zoom : selectionShape,
    x : axisParamsShape,
    y : axisParamsShape
});

const UNSELECTED = 'unselected';
const SELECTED = 'selected';
const HIGHLIGHTED = 'highlighted';

const unselectedColor = 'rgba(63, 127, 191, 0.5)';
const selectedColor = 'rgba(21, 138, 15, 0.5)';
const highlightedColor = 'rgba(250, 243, 40, 1)';
const selectionRectColor = 'rgba(165, 165, 165, 0.5)';

const toNumber = (val)=>Number(val);

const getXAxisOptions = function(params) {
    const xTitle = params.x.label + (params.x.unit ? ', ' + params.x.unit : '');
    let xGrid = false, xReversed = false, xLog = false;
    const {options:xOptions} = params.x;
    if (xOptions) {
        xGrid = xOptions.includes('grid');
        xReversed = xOptions.includes('flip');
        xLog = xOptions.includes('log');
    }
    return {xTitle, xGrid, xReversed, xLog};
};

const getYAxisOptions = function(params) {
    const yTitle = params.y.label + (params.y.unit ? ', ' + params.y.unit : '');

    let yGrid = false, yReversed = false, yLog = false;
    const {options:yOptions} = params.y;
    if (params.y.options) {
        yGrid = yOptions.includes('grid');
        yReversed = yOptions.includes('flip');
        yLog = yOptions.includes('log');
    }
    return {yTitle, yGrid, yReversed, yLog};
};

const getZoomSelection = function(params) {
    return (params.zoom ? params.zoom : {xMin:null, xMax: null, yMin:null, yMax:null});
};

export class XYPlot extends React.Component {

    constructor(props) {
        super(props);
        this.updateSelectionRect = this.updateSelectionRect.bind(this);
        this.adjustPlotDisplay = this.adjustPlotDisplay.bind(this);
        this.calculateChartSize=this.calculateChartSize.bind(this);
        this.debouncedResize = this.debouncedResize.bind(this);
        this.onSelectionEvent = this.onSelectionEvent.bind(this);
        this.shouldAnimate = this.shouldAnimate.bind(this);
    }

    shouldComponentUpdate(nextProps) {
        const {data, width, height, params, highlighted, selectInfo, desc} = this.props;
        if (nextProps.data !== data ||
            nextProps.selectInfo !== selectInfo) {
            return true;
        } else {
            const chart = this.refs.chart && this.refs.chart.getChart();
            if (chart) {
                const {params:newParams, highlighted:newHighlighted, width:newWidth, height:newHeight, desc:newDesc } = nextProps;
                if (newDesc !== desc) {
                    chart.setTitle(newDesc, undefined, false);
                }
                if (!shallowequal(highlighted, newHighlighted)) {
                    const highlightedData = [];
                    if (!isUndefined(newHighlighted)) {
                        highlightedData.push(newHighlighted);
                    }
                    chart.get(HIGHLIGHTED).setData(highlightedData);
                }

                if (params !== newParams) {
                    const xoptions = {};
                    const yoptions = {};
                    const newXOptions = getXAxisOptions(newParams);
                    const newYOptions = getYAxisOptions(newParams);
                    if (!shallowequal(getXAxisOptions(params), newXOptions)) {
                        Object.assign(xoptions, newXOptions);
                    }
                    if (!shallowequal(getYAxisOptions(params), newYOptions)) {
                        Object.assign(yoptions, newYOptions);
                    }
                    if (!shallowequal(params.zoom, newParams.zoom)) {
                        const {xMin, xMax, yMin, yMax} = getZoomSelection(newParams);
                        Object.assign(xoptions, {min:xMin, max:xMax});
                        Object.assign(yoptions, {min:yMin, max:yMax});
                    }
                    const xUpdate = Object.getOwnPropertyNames(xoptions).length > 0;
                    const yUpdate = Object.getOwnPropertyNames(yoptions).length > 0;
                    if (xUpdate || yUpdate) {
                        const animate = this.shouldAnimate();
                        xUpdate && chart.xAxis[0].update(xoptions, !yUpdate, animate);
                        yUpdate && chart.yAxis[0].update(yoptions, true, animate);
                    }

                    if (!shallowequal(params.selection, newParams.selection)) {
                        this.updateSelectionRect(newParams.selection);
                    }

                }

                if (newWidth !== width || newHeight !== height) {
                    if (this.pendingResize) {
                        // if resize is fast (small dataset), the animation will do
                        // if resize is slow, we want to do it only once
                        this.pendingResize.cancel();
                    }
                    this.pendingResize = this.debouncedResize();
                    this.pendingResize(newWidth, newHeight);
                }
                return false;
            }
            return true;
        }
    }

    componentDidMount() {
        this.adjustPlotDisplay();
    }

    componentDidUpdate(pProps, pState, pContext) {
        this.adjustPlotDisplay();
    }

    calculateChartSize(widthPx, heightPx) {
        const {params} = this.props;
        let chartWidth = undefined, chartHeight = undefined;
        if (params.xyRatio) {
            if (params.stretch === 'fit') {
                chartHeight = Number(heightPx) - 2;
                chartWidth = Number(params.xyRatio) * Number(chartHeight) + 20;
            } else {
                chartWidth = Number(widthPx) - 15;
                chartHeight = Number(widthPx) / Number(params.xyRatio);
            }
        } else {
            chartWidth = Number(widthPx);
            chartHeight = Number(heightPx);
        }
        return {chartWidth, chartHeight};
    }

    debouncedResize() {
        return debounce((newWidth, newHeight) => {
            const chart = this.refs.chart && this.refs.chart.getChart();
            if (chart) {
                const {chartWidth, chartHeight} = this.calculateChartSize(newWidth, newHeight);

                chart.setSize(chartWidth, chartHeight, this.shouldAnimate() );
            }
        }, 500);
    }

    shouldAnimate() {
        const {data} = this.props;
        return (!data || data.length <= 250);
    }

    adjustPlotDisplay() {
        const {params, onSelection, desc} = this.props;
        const onSelectionEvent = this.onSelectionEvent;

        // can add more chart events here like
        // chart.bind('selection', (event) => {});

        if (params.selection) {
            this.updateSelectionRect(params.selection);
        }
    }

    updateSelectionRect(selection) {
        const chart = this.refs.chart.getChart();

        if (this.selectionRect) {
            this.selectionRect.destroy();
            this.selectionRect = undefined;
        }
        if (selection) {
            const {xMin, xMax, yMin, yMax} = selection;
            const xMinPx = chart.xAxis[0].toPixels(xMin);
            const xMaxPx = chart.xAxis[0].toPixels(xMax);
            const yMinPx = chart.yAxis[0].toPixels(yMin);
            const yMaxPx = chart.yAxis[0].toPixels(yMax);
            const width = Math.abs(xMaxPx - xMinPx);
            const height = Math.abs(yMaxPx - yMinPx);
            this.selectionRect = chart.renderer.rect(Math.min(xMinPx, xMaxPx), Math.min(yMinPx, yMaxPx), width, height, 1)
                .attr({
                    fill: selectionRectColor,
                    stroke: '#8c8c8c',
                    'stroke-width': 0.5,
                    zIndex: 7 // same as Highcharts' selectionMrker rectangle
                })
                .add();
        }
    }

    onSelectionEvent(event) {
        const xAxis = event.xAxis[0];
        const yAxis = event.yAxis[0];

        this.props.onSelection({xMin: xAxis.min, xMax: xAxis.max, yMin: yAxis.min, yMax: yAxis.max});
    }

    render() {

        const {data, params, width, height, selectInfo, highlighted, onHighlightChange, onSelection, desc} = this.props;
        const onSelectionEvent = this.onSelectionEvent;

        const {chartWidth, chartHeight} = this.calculateChartSize(width, height);

        const {xTitle, xGrid, xReversed, xLog} = getXAxisOptions(params);
        const {yTitle, yGrid, yReversed, yLog} = getYAxisOptions(params);
        const {xMin, xMax, yMin, yMax} = getZoomSelection(params);

        // split data into selected and unselected
        let pushFunc;
        if (selectInfo) {
            const selectInfoCls = SelectInfo.newInstance(selectInfo, 0);
            pushFunc = (numdata, nrow, idx) => {
                selectInfoCls.isSelected(idx) ?
                    numdata.selected.push({x: nrow[0], y: nrow[1], rowIdx: nrow[2]}) :
                    numdata.unselected.push({x: nrow[0], y: nrow[1], rowIdx: nrow[2]});
            };
        } else {
            pushFunc = (numdata, nrow) => {
                numdata.unselected.push({x: nrow[0], y: nrow[1], rowIdx: nrow[2]});
            };
        }
        const numericData = data.reduce((numdata, arow, idx) => {
            const nrow = arow.map(toNumber);
            pushFunc(numdata, nrow, idx);
            return numdata;
        }, {selected: [], unselected: []});

        const highlightedData = [];
        if (!isUndefined(highlighted)) {
            highlightedData.push(highlighted);
        }

        const point = {
            events: {
                click() {
                    if (onHighlightChange) {
                        var highlightedIdx = this.rowIdx ? this.rowIdx : this.series.data.indexOf(this);
                        if (highlightedIdx !== highlighted.rowIdx) {
                            onHighlightChange(highlightedIdx);
                        }
                    }
                }
            }
        };


        var config = {
            chart: {
                animation: data && data.length < 250,
                renderTo: 'container',
                type: 'scatter',
                alignTicks: false,
                height: chartHeight,
                width: chartWidth,
                borderColor: '#a5a5a5',
                borderWidth: 3,
                zoomType: 'xy',
                events: {
                    selection(event) {
                        if (onSelection) {
                            onSelectionEvent(event);
                            // prevent the default behavior
                            return false;
                        } else {
                            // do the default behavior - zoom
                            return true;
                        }
                    }
                },
                resetZoomButton: {
                    theme: {
                        display: 'none'
                    }
                },
                selectionMarkerFill: selectionRectColor
            },
            exporting: {
                enabled: true
            },
            legend: {
                enabled: false
            },
            title: {
                text: desc
            },
            tooltip: {

                borderWidth: 1,
                formatter() {
                    return '<span> ' + `${params.x.label} = ${this.point.x} ${params.x.unit} <br/>` +
                        `${params.y.label} = ${this.point.y} ${params.y.unit} <br/> </span>`;
                }
            },
            plotOptions: {
                scatter: {
                    animation: false,
                    cursor: 'pointer',
                    snap: 10, // proximity to the point for mouse events
                    stickyTracking: false
                }
            },
            xAxis: {
                min: xMin,
                max: xMax,
                gridLineColor: '#e9e9e9',
                gridLineWidth: xGrid ? 1 : 0,
                lineColor: '#999',
                tickColor: '#ccc',
                opposite: yReversed,
                reversed: xReversed,
                title: {text: xTitle},
                type: xLog ? 'logarithmic' : 'linear'
            },
            yAxis: {
                min: yMin,
                max: yMax,
                gridLineColor: '#e9e9e9',
                gridLineWidth: yGrid ? 1 : 0,
                tickWidth: 1,
                tickLength: 10,
                tickColor: '#ccc',
                lineWidth: 1,
                lineColor: '#999',
                endOnTick: false,
                reversed: yReversed,
                title: {text: yTitle},
                type: yLog ? 'logarithmic' : 'linear'
            },
            series: [
                {
                    id: UNSELECTED,
                    name: UNSELECTED,
                    color: unselectedColor,
                    data: numericData.unselected,
                    marker: {symbol: 'circle'},
                    turboThreshold: 0,
                    point
                },
                {
                    id: SELECTED,
                    name: SELECTED,
                    color: selectedColor,
                    data: numericData.selected,
                    marker: {symbol: 'circle'},
                    turboThreshold: 0,
                    point
                },
                {
                    id: HIGHLIGHTED,
                    name: HIGHLIGHTED,
                    color: highlightedColor,
                    marker: {
                        lineColor: '#404040',
                        lineWidth: 1,
                        radius: 5,
                        symbol: 'circle'
                    },
                    data: highlightedData

                }],
            credits: {
                enabled: false // removes a reference to Highcharts.com from the chart
            }
        };


        return (
            <div style={{float: 'right'}}>
                <ReactHighcharts config={config} isPureConfig={true} ref='chart'/>
            </div>
        );
    }
}

XYPlot.propTypes = {
    data: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)), // array of numbers [0] - nInBin, [1] - binMin, [2] - binMax
    width: PropTypes.number,
    height: PropTypes.number,
    params: plotParamsShape,
    highlighted: PropTypes.shape({
        x: PropTypes.number,
        y: PropTypes.number,
        rowIdx: PropTypes.number
    }),
    selectInfo: PropTypes.shape({
        selectAll: PropTypes.bool,
        exceptions: PropTypes.instanceOf(Set),
        rowCount: PropTypes.number
    }),
    onHighlightChange: PropTypes.func,
    onSelection: PropTypes.func,
    desc: PropTypes.string
};

XYPlot.defaultProps = {
    data: undefined,
    params: undefined,
    highlighted: undefined,
    onHighlightChange: undefined,
    onSelection: undefined,
    height: 300,
    desc: 'Sample XY Plot'
};

