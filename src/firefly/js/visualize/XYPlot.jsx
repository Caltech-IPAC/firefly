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
    xyRatio : PropTypes.string,
    stretch : PropTypes.oneOf(['fit','fill']),
    selection : selectionShape,
    zoom : selectionShape,
    x : axisParamsShape,
    y : axisParamsShape
});

const plotDataShape = PropTypes.shape({
    rows: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)),
    decimateKey: PropTypes.string,
    xMin: PropTypes.number,
    xMax: PropTypes.number,
    yMin: PropTypes.number,
    yMax: PropTypes.number,
    weightMin: PropTypes.number,
    weightMax: PropTypes.number,
    idStr: PropTypes.string
});

const UNSELECTED = 'unselected';
const SELECTED = 'selected';
const HIGHLIGHTED = 'highlighted';

const unselectedColor = 'rgba(63, 127, 191, 0.5)';
const selectedColor = 'rgba(21, 138, 15, 0.5)';
const highlightedColor = 'rgba(250, 243, 40, 1)';
const selectionRectColor = 'rgba(165, 165, 165, 0.5)';

const toNumber = (val)=>Number(val);

/*
 @param weight for a given point
 @param minWeight minimum weight for all points
 @param maxWeight maximum weigh for all points
 @param logShading - if true use log color scale
 @return{number} from 1 to 6, 1 for 1 pt series
 */
const getWeightBasedGroup = function(weight, minWeight, maxWeight, logShading=true) {
    if (weight == 1) return 1; // 1pt;
    else {
        if (logShading) {
            //use log scale for shade assignment
            let max; //min=2, max;
            const base = Math.pow(maxWeight+0.5, 0.2);
            for (var e = 1; e <=5; e++) {
                max = Math.round(Math.pow(base, e));
                if (weight <= max) {
                    if (max > maxWeight) { max = maxWeight; }
                    return e+1; //(min==max ? min : (min+'-'+max))+'pts';
                }
                //min = max+1;
            }
        } else {
            // use linear scale for order assignment
            const range =  maxWeight-minWeight-1;
            let n=2;
            let max;// min=2, max;
            // 5 groups incr=0.20
            for (let incr = 0.20; incr <=1; incr += 0.20) {
                max = Math.round(minWeight+1+incr*range);
                if (weight <= max) {
                    return n; //(min==max ? min : (min+'-'+max))+'pts';
                }
                //min = max+1;
                n++;
            }
        }
    }
    // should not reach
};

const parseDecimateKey = function(str) {
    if (!str) return;
    let v = str.replace('decimate_key', '');
    if (v.length < 3) return null;
    v = v.substring(1,v.length-1); // remove outer braces
    const parts = v.split(',');
    if (parts.length == 8) {
        const xColNameOrExpr= parts[0];
        const yColNameOrExpr = parts[1];
        const xMin = Number(parts[2]);
        const yMin = Number(parts[3]);
        const nX = Number(parts[4]);
        const nY = Number(parts[5]);
        const xUnit = Number(parts[6]);
        const yUnit = Number(parts[7]);
        return {xColNameOrExpr, yColNameOrExpr, xMin, yMin, nX, nY, xUnit, yUnit};
    }
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
    const hD = !isUndefined(options.hD) ? options.hD : Math.round(h/4);
    // SVG path for the rectangle
    return [
        'M', x, y+hD,
        'L', x+w, y+hD,
        x+w, y+h-hD,
        x, y+h-hD,
        'Z'];
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
        this.makeSeries = this.makeSeries.bind(this);
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

    componentDidUpdate() {
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
                const {data} = this.props;
                if (data.decimateKey) {
                    // update marker's size
                    const {xUnitPx, yUnitPx} = this.getDeciSymbolSize(chart, data.decimateKey);
                    chart.series.forEach((series) => {
                        series.name.includes(UNSELECTED) && series.update({
                            marker: {radius: xUnitPx / 2, hD: (xUnitPx - yUnitPx) / 2}}, false);
                    });
                }
                const {chartWidth, chartHeight} = this.calculateChartSize(newWidth, newHeight);

                chart.setSize(chartWidth, chartHeight, this.shouldAnimate() );

                if (data.decimate) {chart.redraw();}
            }
        }, 500);
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
            const {data, selectInfo, highlighted, onHighlightChange} = this.props;
            const {rows, decimateKey, weightMin, weightMax, idStr} = data;

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
                    // split data into selected and unselected
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
                const numericData = rows.reduce((numdata, arow, idx) => {
                    const nrow = arow.map(toNumber);
                    pushFunc(numdata, nrow, idx);
                    return numdata;
                }, {selected: [], unselected: []});

                marker = {symbol: 'circle'};
                allSeries = [
                    {
                        id: UNSELECTED,
                        name: UNSELECTED,
                        color: unselectedColor,
                        data: numericData.unselected,
                        marker,
                        turboThreshold: 0,
                        point
                    },
                    {
                        id: SELECTED,
                        name: SELECTED,
                        color: selectedColor,
                        data: numericData.selected,
                        marker,
                        turboThreshold: 0,
                        point
                    }
                ];
            } else {
                const {xUnitPx, yUnitPx} = this.getDeciSymbolSize(chart, decimateKey);
                marker = {symbol: 'rectangle', radius: xUnitPx/2, hD: (xUnitPx-yUnitPx)/2};

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
                const numericDataArr = rows.reduce((numdata, arow) => {
                    const nrow = arow.map(toNumber);
                    const weight = nrow[3];
                    const group = getWeightBasedGroup(weight, weightMin, weightMax);
                    const {x,y} = getCenter(nrow[0], nrow[1]);
                    numdata[group-1].push({x, y, rowIdx: nrow[2], weight});
                    return numdata;
                }, [[],[],[],[],[],[]]);

                // 5 colors (use http://colorbrewer2.org)
                const weightBasedColors = ['#d9d9d9', '#BDBDBD', '#969696', '#737373', '#525252', '#252525'];

                allSeries = numericDataArr.map((numericData, idx) => {
                    return {
                        id: UNSELECTED+idx,
                        name: UNSELECTED+idx,
                        color: weightBasedColors[idx],
                        data: numericData,
                        marker,
                        turboThreshold: 0,
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
                data: highlightedData
            }, true, false);
        }
    }

    getDeciSymbolSize(chart, decimateKeyStr) {
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
    }

    render() {

        const {data, params, width, height, onSelection, desc} = this.props;
        const onSelectionEvent = this.onSelectionEvent;

        const {chartWidth, chartHeight} = this.calculateChartSize(width, height);

        const {xTitle, xGrid, xReversed, xLog} = getXAxisOptions(params);
        const {yTitle, yGrid, yReversed, yLog} = getYAxisOptions(params);
        const {xMin, xMax, yMin, yMax} = getZoomSelection(params);
        const {xMin:xDataMin, xMax:xDataMax, yMin:yDataMin, yMax:yDataMax} = data;

        const makeSeries = this.makeSeries;

        var config = {
            chart: {
                animation: this.shouldAnimate(),
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
                enabled: false
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
                data: [[selFinite(xMin, xDataMin), selFinite(yMin,yDataMin)], [selFinite(xMax, xDataMax), selFinite(yMax,yDataMax)]]
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

