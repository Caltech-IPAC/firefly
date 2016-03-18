/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isUndefined, debounce} from 'lodash';
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

const unselectedColor = 'rgba(63, 127, 191, 0.5)';
const selectedColor = 'rgba(21, 138, 15, 0.5)';
const highlightedColor = 'rgba(250, 243, 40, 1)';
const selectionRectColor = 'rgba(165, 165, 165, 0.5)';

const toNumber = (val)=>Number(val);


export class XYPlot extends React.Component {

    constructor(props) {
        super(props);
        this.adjustPlotDisplay = this.adjustPlotDisplay.bind(this);
        this.onSelectionEvent = this.onSelectionEvent.bind(this);
        this.debouncedResize = this.debouncedResize.bind(this);
    }

    shouldComponentUpdate(nextProps) {
        const {data, width, height, params, highlightedRow, selectInfo, desc} = this.props;
        if (nextProps.data !== data ||
            nextProps.params !== params ||
            nextProps.selectInfo !== selectInfo) {
            return true;
        } else {
            const chart = this.refs.chart && this.refs.chart.getChart();
            if (chart) {
                const {highlightedRow:newHighlighted, width:newWidth, height:newHeight, desc:newDesc} = nextProps;
                if (newDesc !== desc) {
                    chart.setTitle(newDesc, undefined, false);
                }
                if (newHighlighted !== highlightedRow) {
                    const highlightedData = [];
                    if (!isUndefined(newHighlighted)) {
                        const hrow = data[newHighlighted].map(toNumber);
                        highlightedData.push({x: hrow[0], y: hrow[1], rowIdx: newHighlighted});
                    }

                    chart.get('highlighted').setData(highlightedData);
                }
                if (nextProps.width !== width || nextProps.height !== height) {
                    if (this.pendingResize) {
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

    debouncedResize() {
        return debounce((newWidth, newHeight) => {
            const chart = this.refs.chart && this.refs.chart.getChart();
            if (chart) {
                const params = this.props.params;
                const widthPx = newWidth;
                const heightPx = newHeight;

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
                const noAnimation = (chart.series[0].data && chart.series[0].data.length>500) ||
                    (chart.series[1].data && chart.series[1].data.length>500);
                chart.setSize(chartWidth, chartHeight, !noAnimation );
            }
        }, 500);
    }

    componentDidMount() {
        this.adjustPlotDisplay();
    }

    componentDidUpdate(pProps, pState, pContext) {
        this.adjustPlotDisplay();
    }

    adjustPlotDisplay() {
        const {params, onSelection, desc} = this.props;
        const onSelectionEvent = this.onSelectionEvent;

        const chart = this.refs.chart.getChart();

        //setting chart.events.selection
        chart.bind('selection', (event) => {
            if (onSelection) {
                onSelectionEvent(event);
                // prevent the default behavior
                return false;
            } else {
                // do the default behavior - zoom
                return true;
            }
        });

        chart.setTitle(desc, undefined, false);

        if (params.zoom) {
            const {xMin, xMax, yMin, yMax} = params.zoom;
            // redraw=true, animation=false
            chart.xAxis[0].setExtremes(xMin, xMax, false, false);
            chart.yAxis[0].setExtremes(yMin, yMax, true, false);
        }

        if (params.selection) {
            const {xMin, xMax, yMin, yMax} = params.selection;
            const xMinPx = chart.xAxis[0].toPixels(xMin);
            const xMaxPx = chart.xAxis[0].toPixels(xMax);
            const yMinPx = chart.yAxis[0].toPixels(yMin);
            const yMaxPx = chart.yAxis[0].toPixels(yMax);
            const width = Math.abs(xMaxPx - xMinPx);
            const height = Math.abs(yMaxPx - yMinPx);
            chart.renderer.rect(Math.min(xMinPx, xMaxPx), Math.min(yMinPx, yMaxPx), width, height, 1)
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

        const {data, params, width, height, selectInfo, highlightedRow, onHighlightChange, desc} = this.props;

        const widthPx = width;
        const heightPx = height;

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

        const xTitle = params.x.label + (params.x.unit ? ', ' + params.x.unit : '');
        const yTitle = params.y.label + (params.y.unit ? ', ' + params.y.unit : '');

        let xGrid = false, xReversed = false, xLog = false,
            yGrid = false, yReversed = false, yLog = false;
        const {options:yOptions} = params.y;
        if (params.y.options) {
            yGrid = yOptions.includes('grid');
            yReversed = yOptions.includes('flip');
            yLog = yOptions.includes('log');
        }
        const {options:xOptions} = params.x;
        if (xOptions) {
            xGrid = xOptions.includes('grid');
            xReversed = xOptions.includes('flip');
            xLog = xOptions.includes('log');
        }

        // split data into selected and unselected
        let pushFunc;
        if (selectInfo) {
            const selectInfoCls = SelectInfo.newInstance(selectInfo, 0);
            pushFunc = (numdata, nrow, idx) => {
                selectInfoCls.isSelected(idx) ?
                    numdata.selected.push({x: nrow[0], y: nrow[1], rowIdx: idx}) :
                    numdata.unselected.push({x: nrow[0], y: nrow[1], rowIdx: idx});
            };
        } else {
            pushFunc = (numdata, nrow) => {
                numdata.unselected.push(nrow);
            };
        }
        const numericData = data.reduce((numdata, arow, idx) => {
            const nrow = arow.map(toNumber);
            pushFunc(numdata, nrow, idx);
            return numdata;
        }, {selected: [], unselected: []});

        const highlightedData = [];
        if (!isUndefined(highlightedRow)) {
            const hrow = data[highlightedRow].map(toNumber);
            highlightedData.push({x: hrow[0], y: hrow[1], rowIdx: highlightedRow});
        }

        const point = {
            events: {
                click() {
                    if (onHighlightChange) {
                        var highlighted = this.rowIdx ? this.rowIdx : this.series.data.indexOf(this);
                        if (highlighted !== highlightedRow) {
                            onHighlightChange(highlighted);
                        }
                    }
                }
            }
        };


        var config = {
            chart: {
                renderTo: 'container',
                type: 'scatter',
                alignTicks: false,
                height: chartHeight,
                width: chartWidth,
                borderColor: '#a5a5a5',
                borderWidth: 3,
                zoomType: 'xy',
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
            series: [{
                name: 'unselected',
                color: unselectedColor,
                data: numericData.unselected,
                marker: {symbol: 'circle'},
                turboThreshold: 0,
                point
            },
                {
                    name: 'selected',
                    color: selectedColor,
                    data: numericData.selected,
                    marker: {symbol: 'circle'},
                    turboThreshold: 0,
                    point
                },
                {
                    id: 'highlighted',
                    name: 'highlighted',
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
    highlightedRow: PropTypes.number,
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
    highlightedRow: 0,
    onHighlightChange: undefined,
    onSelection: undefined,
    height: 300,
    desc: 'Sample XY Plot'
};
