/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isUndefined, debounce, get} from 'lodash';
import shallowequal from 'shallowequal';
import React, {PropTypes} from 'react';
import ReactHighcharts from 'react-highcharts/bundle/highcharts';

import {SelectInfo} from '../tables/SelectInfo.js';
import {parseDecimateKey} from '../tables/Decimate.js';

//import {getFormatString} from '../util/MathUtil.js';

export const axisParamsShape = PropTypes.shape({
    columnOrExpr : PropTypes.string,
    label : PropTypes.string,
    unit : PropTypes.string,
    options : PropTypes.string // ex. 'grid,log,flip'
});

export const selectionShape = PropTypes.shape({
    xMin : PropTypes.number,
    xMax : PropTypes.number,
    yMin : PropTypes.number,
    yMax : PropTypes.number
});

export const plotParamsShape = PropTypes.shape({
    xyRatio : PropTypes.string,
    stretch : PropTypes.oneOf(['fit','fill']),
    selection : selectionShape,
    zoom : selectionShape,
    nbins : PropTypes.shape({x : PropTypes.number, y : PropTypes.number}),
    shading : PropTypes.oneOf(['lin', 'log']),
    x : axisParamsShape,
    y : axisParamsShape
});

const plotDataShape = PropTypes.shape({
    rows: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)), // [x,y,rowIdx,weight]
    decimateKey: PropTypes.string,
    xMin: PropTypes.number,
    xMax: PropTypes.number,
    yMin: PropTypes.number,
    yMax: PropTypes.number,
    weightMin: PropTypes.number,
    weightMax: PropTypes.number,
    idStr: PropTypes.string
});

const DATAPOINTS = 'data';
const SELECTED = 'selected';
const HIGHLIGHTED = 'highlighted';

const datapointsColor = 'rgba(63, 127, 191, 0.5)';
const selectedColor = 'rgba(21, 138, 15, 0.5)';
const highlightedColor = 'rgba(250, 243, 40, 1)';
const selectionRectColor = 'rgba(165, 165, 165, 0.5)';

const toNumber = (val)=>Number(val);

/*
 @param {number} weight for a given point
 @param {number} minWeight minimum weight for all points
 @param {number} maxWeight maximum weight for all points
 @param {boolean} logShading - if true use log color scale
 @param {boolean} returnNum - if true return group number rather than group description
 @return {number|string} from 1 to 6, 1 for 1 pt series
 */
const getWeightBasedGroup = function(weight, minWeight, maxWeight, logShading=true, returnNum=true) {
    if (weight == 1) return returnNum ? 1 : '1pt';
    else {
        if (logShading) {
            //use log scale for shade assignment
            let min=2, max;
            const base = Math.pow(maxWeight+0.5, 0.2);
            for (var e = 1; e <=5; e++) {
                max = Math.round(Math.pow(base, e));
                if (weight <= max) {
                    if (max > maxWeight) { max = maxWeight; }
                    return returnNum ? e+1 : (min==max ? min : (min+'-'+max))+'pts';
                }
                min = max+1;
            }
        } else {
            // use linear scale for order assignment
            const range =  maxWeight-minWeight-1;
            let n=2;
            let min=2, max;
            // 5 groups incr=0.20
            for (let incr = 0.20; incr <=1; incr += 0.20) {
                max = Math.round(minWeight+1+incr*range);
                if (weight <= max) {
                    return returnNum ? n : (min==max ? min : (min+'-'+max))+'pts';
                }
                min = max+1;
                n++;
            }
        }
    }
    // should not reach
};

const getWeightedDataDescr = function(defaultDescr, numericData, minWeight, maxWeight, logShading) {
    if (numericData.length < 1) { return defaultDescr; }
    return getWeightBasedGroup(numericData[0].weight, minWeight, maxWeight, logShading, false);
};

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

const selFinite = (v1, v2) => {return Number.isFinite(v1) ? v1 : v2;};

/*
 A symbol to represent decimated series. The size of the rectangle depends of the decimation unit
 @param x lower left corner of the rectangle: ptX-options.radius
 @param y lower left corner of the rectangle: ptY-options.radius
 @param w width of the rectangle (2*options.radius)
 @param h height of the rectangle (2*options.radius)
 @param options {object} has hD - half difference between width and height of the rectangle
 */
ReactHighcharts.Highcharts.SVGRenderer.prototype.symbols.rectangle = function (x, y, w, h, options) {
    const hD = get(options, 'hD', Math.round(h/4));
    // SVG path for the rectangle
    return [
        'M', x, y+hD,
        'L', x+w, y+hD,
        x+w, y+h-hD,
        x, y+h-hD,
        'Z'];
};

/*
 * Since decimated symbol should accurately reflect bin size,
 * the size of the symbol depends on the chart size.
 * @param {object} Highcharts' Chart object
 * @param {string} decimate key string (contains binning info)
 * @returns {object} x and y pixel size if the bin
 */
const getDeciSymbolSize = function(chart, decimateKeyStr) {
    const {xUnit,yUnit} = parseDecimateKey(decimateKeyStr);

    const getPxSize = (axis, unit) => {
        const {min} = axis.getExtremes();
        const max = min+unit;
        const minPx = axis.toPixels(min);
        const maxPx = axis.toPixels(max);
        let unitPx = Math.abs(maxPx-minPx);
        if (unitPx < 2) { unitPx = 2; }
        return unitPx;
    };

    const xUnitPx = getPxSize(chart.xAxis[0], xUnit);
    const yUnitPx = getPxSize(chart.yAxis[0], yUnit);
    return {xUnitPx, yUnitPx};
};

const calculateChartSize = function(widthPx, heightPx, props) {
    const {params} = props;
    let chartWidth = undefined, chartHeight = undefined;
    if (params.xyRatio) {
        if (params.stretch === 'fit') {
            chartHeight = Number(heightPx) - 2;
            chartWidth = Number(params.xyRatio) * Number(chartHeight) + 20;
            if (chartWidth > Number(widthPx)) {
                chartHeight -= 15; // to accommodate scroll bar
            }
        } else {
            chartWidth = Number(widthPx) - 15;
            chartHeight = Number(widthPx) / Number(params.xyRatio);
        }
    } else {
        chartWidth = Number(widthPx);
        chartHeight = Number(heightPx);
    }
    return {chartWidth, chartHeight};
};

export class XYPlot extends React.Component {

    constructor(props) {
        super(props);
        this.updateSelectionRect = this.updateSelectionRect.bind(this);
        this.adjustPlotDisplay = this.adjustPlotDisplay.bind(this);
        this.debouncedResize = this.debouncedResize.bind(this);
        this.onSelectionEvent = this.onSelectionEvent.bind(this);
        this.shouldAnimate = this.shouldAnimate.bind(this);
        this.makeSeries = this.makeSeries.bind(this);
    }

    shouldComponentUpdate(nextProps) {
        const {data, width, height, params, highlighted, selectInfo, desc} = this.props;
        // only rerender when plot data change
        if (nextProps.data !== data) {
            return true;
        } else {
            const chart = this.refs.chart && this.refs.chart.getChart();
            if (chart) {
                const {params:newParams, highlighted:newHighlighted, width:newWidth, height:newHeight, desc:newDesc } = nextProps;
                if (newDesc !== desc) {
                    chart.setTitle(newDesc, undefined, false);
                }

                // selection change (selection is not supported for decimated data)
                if (data && data.rows && !data.decimateKey && nextProps.selectInfo !== selectInfo) {
                    const selectedData = [];
                    if (nextProps.selectInfo) {
                        const selectInfoCls = SelectInfo.newInstance(nextProps.selectInfo, 0);
                        data.rows.forEach((arow) => {
                            if (selectInfoCls.isSelected(Number(arow[2]))) {
                                const nrow = arow.map(toNumber);
                                selectedData.push({x: nrow[0], y: nrow[1], rowIdx: nrow[2]});
                            }
                        });
                    }
                    chart.get(SELECTED).setData(selectedData);
                }

                // highlight change
                if (!shallowequal(highlighted, newHighlighted)) {
                    const highlightedData = [];
                    if (!isUndefined(newHighlighted)) {
                        highlightedData.push(newHighlighted);
                    }
                    chart.get(HIGHLIGHTED).setData(highlightedData);
                }


                // plot parameters change
                if (params !== newParams) {
                    const xoptions = {};
                    const yoptions = {};
                    const newXOptions = getXAxisOptions(newParams);
                    const newYOptions = getYAxisOptions(newParams);
                    if (!shallowequal(getXAxisOptions(params), newXOptions)) {
                        Object.assign(xoptions, {
                            title: {text: newXOptions.xTitle},
                            gridLineWidth: newXOptions.xGrid ? 1 : 0,
                            reversed: newXOptions.xReversed,
                            opposite: newYOptions.yReversed,
                            type: newXOptions.xLog ? 'logarithmic' : 'linear'
                        });
                    }
                    if (!shallowequal(getYAxisOptions(params), newYOptions)) {
                        Object.assign(yoptions, {
                            title: {text: newYOptions.yTitle},
                            gridLineWidth: newYOptions.yGrid ? 1 : 0,
                            reversed: newYOptions.yReversed,
                            type: newYOptions.yLog ? 'logarithmic' : 'linear'
                        });
                    }
                    if (!shallowequal(params.zoom, newParams.zoom)) {
                        const {xMin, xMax, yMin, yMax} = getZoomSelection(newParams);
                        const {xMin:xDataMin, xMax:xDataMax, yMin:yDataMin, yMax:yDataMax} = nextProps.data;
                        Object.assign(xoptions, {min: selFinite(xMin,xDataMin), max: selFinite(xMax, xDataMax)});
                        Object.assign(yoptions, {min: selFinite(yMin, yDataMin), max: selFinite(yMax, yDataMax)});
                    }
                    const xUpdate = Reflect.ownKeys(xoptions).length > 0;
                    const yUpdate = Reflect.ownKeys(yoptions).length > 0;
                    if (xUpdate || yUpdate) {
                        const animate = this.shouldAnimate();
                        xUpdate && chart.xAxis[0].update(xoptions, !yUpdate, animate);
                        yUpdate && chart.yAxis[0].update(yoptions, true, animate);
                    }

                    if (!shallowequal(params.selection, newParams.selection)) {
                        this.updateSelectionRect(newParams.selection);
                    }

                }

                // size change
                if (newWidth !== width || newHeight !== height ||
                    newParams.xyRatio !== params.xyRatio ||newParams.stretch != params.stretch) {
                    const {chartWidth, chartHeight} = calculateChartSize(newWidth, newHeight, nextProps);
                    chart.setSize(chartWidth, chartHeight, false);

                    if (this.pendingResize) {
                        // if resize is slow, we want to do it only once
                        this.pendingResize.cancel();
                    }
                    this.pendingResize = this.debouncedResize();
                    this.pendingResize();
                }

                return false;
            }
            return true;
        }
    }

    componentDidMount() {
        this.adjustPlotDisplay();
    }

    componentDidUpdate() {
        this.adjustPlotDisplay();
    }

    debouncedResize() {
        return debounce(() => {
            const chart = this.refs.chart && this.refs.chart.getChart();
            if (chart) {
                const {data} = this.props;
                if (data.decimateKey) {
                    // update marker's size
                    const {xUnitPx, yUnitPx} = getDeciSymbolSize(chart, data.decimateKey);
                    chart.series.forEach((series) => {
                        series.name.includes(DATAPOINTS) && series.update({
                            marker: {radius: xUnitPx/2.0, hD: (xUnitPx-yUnitPx)/2.0}}, false);
                    });
                    chart.redraw();
                }
            }
            this.pendingResize = null;
        }, 300);
    }

    shouldAnimate() {
        const {data} = this.props;
        return (!data || !data.rows || data.rows.length <= 250);
    }

    adjustPlotDisplay() {
        const {params} = this.props;

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
            const xMinPx = chart.xAxis[0].toPixels(selection.xMin);
            const xMaxPx = chart.xAxis[0].toPixels(selection.xMax);
            const yMinPx = chart.yAxis[0].toPixels(selection.yMin);
            const yMaxPx = chart.yAxis[0].toPixels(selection.yMax);
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

    makeSeries(chart) {
        //const chart = this.refs.chart && this.refs.chart.getChart();
        if (chart) {
            const {data, params, selectInfo, highlighted, onHighlightChange} = this.props;
            const {rows, decimateKey, weightMin, weightMax} = data;

            let allSeries, marker;

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

            if (!decimateKey) {
                let pushFunc;
                if (selectInfo) {
                    const selectInfoCls = SelectInfo.newInstance(selectInfo, 0);
                    // set all and selected data
                    pushFunc = (numdata, nrow) => {
                        numdata.all.push({x: nrow[0], y: nrow[1], rowIdx: nrow[2]});
                        if (selectInfoCls.isSelected(nrow[2])) {
                            numdata.selected.push({x: nrow[0], y: nrow[1], rowIdx: nrow[2]});
                        }
                    };
                } else {
                    pushFunc = (numdata, nrow) => {
                        numdata.all.push({x: nrow[0], y: nrow[1], rowIdx: nrow[2]});
                    };
                }
                const numericData = rows.reduce((numdata, arow) => {
                    const nrow = arow.map(toNumber);
                    pushFunc(numdata, nrow);
                    return numdata;
                }, {selected: [], all: []});


                marker = {symbol: 'circle'};
                allSeries = [
                    {
                        id: DATAPOINTS,
                        name: DATAPOINTS,
                        color: datapointsColor,
                        data: numericData.all,
                        marker,
                        turboThreshold: 0,
                        showInLegend: false,
                        point
                    },
                    {
                        id: SELECTED,
                        name: SELECTED,
                        color: selectedColor,
                        data: numericData.selected,
                        marker,
                        turboThreshold: 0,
                        showInLegend: false,
                        point
                    }
                ];
            } else {
                const {xUnitPx, yUnitPx} = getDeciSymbolSize(chart, decimateKey);
                marker = {symbol: 'rectangle', radius: xUnitPx/2.0, hD: (xUnitPx-yUnitPx)/2.0};

                const {xMin, xUnit, yMin, yUnit} = parseDecimateKey(decimateKey);
                const getCenter = (xval,yval) => {
                    return {
                        // bitwise operators convert operands to 32-bit integer
                        // hence they can be used as a fast way to truncate a float to an integer
                        x: xMin+(~~((xval-xMin)/xUnit)+0.5)*xUnit,
                        y: yMin+(~~((yval-yMin)/yUnit)+0.5)*yUnit
                    };
                };

                // split into 6 groups by weight
                const numericDataArr = [[],[],[],[],[],[]];
                for (var i= 0, l = rows.length; i < l; i++) {
                    const nrow = rows[i].map(toNumber);
                    const weight = nrow[3];
                    const group = getWeightBasedGroup(weight, weightMin, weightMax, params.shading==='log');
                    const {x,y} = getCenter(nrow[0], nrow[1]);
                    numericDataArr[group-1].push({x, y, rowIdx: nrow[2], weight});
                }

                // 5 colors (use http://colorbrewer2.org)
                const weightBasedColors = ['#d9d9d9', '#BDBDBD', '#969696', '#737373', '#525252', '#252525'];

                allSeries = numericDataArr.map((numericData, idx) => {
                    return {
                        id: DATAPOINTS+idx,
                        name: getWeightedDataDescr(DATAPOINTS+idx, numericData, weightMin, weightMax, params.shading==='log'),
                        color: weightBasedColors[idx],
                        data: numericData,
                        marker,
                        turboThreshold: 0,
                        showInLegend: numericData.length>0 && (xUnitPx>2 && xUnitPx<20 && yUnitPx>2 && yUnitPx<20), // legend symbol size can not be adjusted as of now
                        point
                    };
                });


            }
            allSeries.forEach((series) => {chart.addSeries(series,false, false);});

            chart.addSeries({
                id: HIGHLIGHTED,
                name: HIGHLIGHTED,
                color: highlightedColor,
                marker: {symbol: 'circle', lineColor: '#404040', lineWidth: 1, radius: 5},
                data: highlightedData,
                showInLegend: false
            }, true, false);
        }
    }

    render() {

        const {data, params, width, height, onSelection, desc} = this.props;
        const onSelectionEvent = this.onSelectionEvent;

        const {chartWidth, chartHeight} = calculateChartSize(width, height, this.props);

        const {xTitle, xGrid, xReversed, xLog} = getXAxisOptions(params);
        const {yTitle, yGrid, yReversed, yLog} = getYAxisOptions(params);
        const {xMin, xMax, yMin, yMax} = getZoomSelection(params);
        const {xMin:xDataMin, xMax:xDataMax, yMin:yDataMin, yMax:yDataMax, decimateKey} = data;

        const makeSeries = this.makeSeries;

        const component = this;

        var config = {
            chart: {
                animation: this.shouldAnimate(),
                renderTo: 'container',
                type: 'scatter',
                alignTicks: false,
                height: chartHeight,
                width: chartWidth,
                borderColor: '#a5a5a5',
                borderWidth: 1,
                zoomType: 'xy',
                events: {
                    click() {
                        if (component.props.params.selection && onSelection) {
                            onSelection();
                        }
                    },
                    selection(event) {
                        if (onSelection) {
                            onSelectionEvent(event);
                            // prevent the default behavior
                            return false;
                        } else {
                            // do the default behavior - zoom
                            return true;
                        }
                    },
                    load() {
                        makeSeries(this);
                        return true;
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
                enabled: true,
                align: 'right',
                layout: 'vertical',
                verticalAlign: 'top',
                symbolHeight: 12,
                symbolWidth: 12,
                symbolRadius: 6
            },
            title: {
                text: desc
            },
            tooltip: {

                borderWidth: 1,
                formatter() {
                    const weight = this.point.weight ? `represents ${this.point.weight} points <br/>` : '';
                    return '<span> ' + `${params.x.label} = ${this.point.x} ${params.x.unit} <br/>` +
                        `${params.y.label} = ${this.point.y} ${params.y.unit} <br/> ` +
                        `${weight}</span>`;
                },
                shadow: !(decimateKey),
                useHTML: Boolean((decimateKey))
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
                min: selFinite(xMin, xDataMin),
                max: selFinite(xMax, xDataMax),
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
                min: selFinite(yMin,yDataMin),
                max: selFinite(yMax,yDataMax),
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
                // this series is to make sure the axis are created
                // without actual series xAxis creation is deferred
                // and there is no way to get value to pixel conversion
                // for sizing the symbol
                id: 'minmax',
                name: 'minmax',
                color: '#f0f0f0',
                marker: {radius: 2},
                data: [[selFinite(xMin, xDataMin), selFinite(yMin,yDataMin)], [selFinite(xMax, xDataMax), selFinite(yMax,yDataMax)]],
                showInLegend: false
            }],
            credits: {
                enabled: false // removes a reference to Highcharts.com from the chart
            }
        };


        return (
            <div style={chartWidth<width?{float: 'right'}:{}}>
                <ReactHighcharts config={config} isPureConfig={true} ref='chart'/>
            </div>
        );
    }
}

XYPlot.propTypes = {
    data: plotDataShape,
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

