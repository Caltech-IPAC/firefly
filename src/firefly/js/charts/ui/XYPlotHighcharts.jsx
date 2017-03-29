/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isUndefined, debounce, get, has, omit} from 'lodash';
import shallowequal from 'shallowequal';
import React, {PropTypes} from 'react';
import ReactHighcharts from 'react-highcharts';
import {xyErrorBarExtension} from '../highcharts/XYErrorBars.js';

import {SelectInfo} from '../../tables/SelectInfo.js';
import {parseDecimateKey} from '../../tables/Decimate.js';

import numeral from 'numeral';
import {getFormatString} from '../../util/MathUtil.js';

import {plotParamsShape, plotDataShape} from './XYPlotPropTypes.js';
import {calculateChartSize, getWeightBasedGroup, getWeightedDataDescr,
    getXAxisOptions, getYAxisOptions, getZoomSelection, formatError, isDataSeries, isLinePlot, plotErrors,
    selFiniteMin, selFiniteMax, validate} from './XYPlot.jsx';



xyErrorBarExtension(ReactHighcharts.Highcharts);

const defaultShading = 'lin';

const DATAPOINTS = 'data';
const XERROR = 'xerror';
const YERROR = 'yerror';
const SELECTED = 'selected';
const HIGHLIGHTED = 'highlighted';
const MINMAX = 'minmax';

const datapointsColor = 'rgba(63, 127, 191, 0.5)';
const datapointsColorWithErrors = 'rgba(63, 127, 191, 0.7)';
const errorBarColor = 'rgba(63, 127, 191, 0.5)'; //'rgba(255, 209, 128, 0.5)';
const selectedColorWithErrors = 'rgba(255, 200, 0, 1)';
const selectedColor = 'rgba(255, 200, 0, 1)';
const highlightedColor = 'rgba(255, 165, 0, 1)';
const selectionRectColor = 'rgba(255, 209, 128, 0.5)';
const selectionRectColorGray = 'rgba(165, 165, 165, 0.5)';

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


export class XYPlotHighcharts extends React.Component {

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

        const propsToOmit = ['onHighlightChange', 'onSelection', 'highlighted'];

        // no update is needed if properties did ot change
        if (shallowequal(omit(this.props, propsToOmit), omit(nextProps, propsToOmit)) &&
            get(this.props,'highlighted.rowIdx') === get(nextProps,'highlighted.rowIdx')) {
            return false;
        }

        const {data, width, height, params, highlighted, selectInfo, desc} = this.props;

        // only re-render when the plot data change or an error occurs
        // shading change for density plot changes series
        if (nextProps.data !== data ||
            get(params, 'plotStyle') !== get(nextProps.params, 'plotStyle') ||
            plotErrors(params, 'x') !== plotErrors(nextProps.params, 'x') ||
            plotErrors(params, 'y') !== plotErrors(nextProps.params, 'y') ||
            get(params, 'shading', defaultShading) !== get(nextProps.params, 'shading', defaultShading)) {
            return true;
        } else {
            const chart = this.refs.chart && this.refs.chart.getChart();
            if (chart && chart.container && !this.error) {
                const {params:newParams, width:newWidth, height:newHeight, highlighted:newHighlighted, selectInfo:newSelectInfo, desc:newDesc } = nextProps;
                try {
                    const errors = validate(newParams, data);
                    if (errors.length > 0) {
                        this.error = errors[0];
                        chart.showLoading(errors[0]);
                        return false;
                    }

                    if (newDesc !== desc) {
                        chart.setTitle(newDesc, undefined, false);
                    }

                    // selection change (selection is not supported for decimated data)
                    if (data && data.rows && !data.decimateKey && newSelectInfo !== selectInfo) {
                        const selectedData = [];
                        if (newSelectInfo) {
                            const selectInfoCls = SelectInfo.newInstance(newSelectInfo, 0);
                            data.rows.forEach((arow) => {
                                if (selectInfoCls.isSelected(arow['rowIdx'])) {
                                    selectedData.push(arow);
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
                                opposite: newXOptions.xOpposite,
                                type: newXOptions.xLog ? 'logarithmic' : 'linear'
                            });
                        }
                        if (!shallowequal(getYAxisOptions(params), newYOptions)) {
                            Object.assign(yoptions, {
                                title: {text: newYOptions.yTitle},
                                gridLineWidth: newYOptions.yGrid ? 1 : 0,
                                reversed: newYOptions.yReversed,
                                opposite: newYOptions.yOpposite,
                                type: newYOptions.yLog ? 'logarithmic' : 'linear'
                            });
                        }
                        if (!shallowequal(params.zoom, newParams.zoom) || !shallowequal(params.boundaries, newParams.boundaries)) {
                            const {xMin, xMax, yMin, yMax} = getZoomSelection(newParams);
                            const {xMin:xDataMin, xMax:xDataMax, yMin:yDataMin, yMax:yDataMax} = get(newParams, 'boundaries', {});
                            Object.assign(xoptions, {min: selFiniteMax(xMin, xDataMin), max: selFiniteMin(xMax, xDataMax)});
                            Object.assign(yoptions, {min: selFiniteMax(yMin, yDataMin), max: selFiniteMin(yMax, yDataMax)});
                            chart.get(MINMAX).setData([[xoptions.min, yoptions.min], [xoptions.max, yoptions.max]], false, false, false);
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
                        newParams.xyRatio !== params.xyRatio || newParams.stretch !== params.stretch) {
                        const {chartWidth, chartHeight} = calculateChartSize(newWidth, newHeight, nextProps);
                        if (Math.abs(chart.chartWidth - chartWidth) > 20 || Math.abs(chart.chartHeight - chartHeight) > 20) {

                            if (get(nextProps, ['data', 'decimateKey'])) {
                                // hide all series for resize
                                chart.series.forEach((series) => {
                                    series.setVisible(false, false);
                                });
                                chart.showLoading('Resizing');
                            }
                            chart.setSize(chartWidth, chartHeight, false);

                            if (this.pendingResize) {
                                // if resize is slow, we want to do it only once
                                this.pendingResize.cancel();
                            }
                            this.pendingResize = this.debouncedResize();
                            this.pendingResize();
                        }
                    }
                } catch (error) {
                    this.error = error;
                    chart.showLoading(error);
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
            if (chart && chart.container) {
                const {data} = this.props;
                if (data.decimateKey) {
                    // update marker's size
                    const {xUnitPx, yUnitPx} = getDeciSymbolSize(chart, data.decimateKey);
                    chart.series.forEach((series) => {
                        isDataSeries(series.name) && series.update({
                            marker: {radius: xUnitPx/2.0, hD: (xUnitPx-yUnitPx)/2.0}}, false);
                        series.setVisible(true, false);
                    });
                    chart.redraw();
                    chart.hideLoading();
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
            const selColor = has(this.props, 'data.decimateKey') ? selectionRectColor : selectionRectColorGray;
            this.selectionRect = chart.renderer.rect(Math.min(xMinPx, xMaxPx), Math.min(yMinPx, yMaxPx), width, height, 1)
                .attr({
                    fill: selColor,
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

        if (xAxis && yAxis) {
            this.props.onSelection({xMin: xAxis.min, xMax: xAxis.max, yMin: yAxis.min, yMax: yAxis.max});
        }
    }

    makeSeries(chart) {
        //const chart = this.refs.chart && this.refs.chart.getChart();
        if (chart && chart.container) {
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
                const hasXErrors = plotErrors(params, 'x');
                const hasYErrors = plotErrors(params, 'y');
                const hasErrorBars = hasXErrors || hasYErrors;

                let selectedRows = [];
                if (selectInfo) {
                    const selectInfoCls = SelectInfo.newInstance(selectInfo, 0);

                    selectedRows = rows.reduce((selrows, arow) => {
                        if (selectInfoCls.isSelected(arow['rowIdx'])) {
                            selrows.push(arow);
                        }
                        return selrows;
                    }, []);
                }

                marker = {symbol: 'circle', radius: 3};

                allSeries = [];
                if (hasXErrors) {
                    const xErrRows = rows.filter((r) => (Number.isFinite(r['left']) && Number.isFinite(r['right'])));
                    xErrRows.sort((r1,r2) => (r1['x']-r2['x']));
                    allSeries.push({
                        id: XERROR,
                        name: XERROR,
                        type: 'error_bar',
                        animation: false,
                        format: 'x',
                        color: errorBarColor,
                        lineWidth: 1,
                        whiskerLength: (xErrRows.length > 20) ? 0 : 3,
                        data: xErrRows,
                        turboThreshold: 0,
                        showInLegend: false,
                        enableMouseTracking: false
                    });
                }
                if (hasYErrors) {
                    const yErrRows = rows.filter((r) => (Number.isFinite(r['low']) && Number.isFinite(r['high'])));
                    yErrRows.sort((r1,r2) => (r1['x']-r2['x']));
                    allSeries.push({
                        id: YERROR,
                        name: YERROR,
                        type: 'error_bar',
                        animation: false,
                        format: 'y',
                        animation: false,
                        color: errorBarColor,
                        lineWidth: 1,
                        whiskerLength: (yErrRows.length > 20) ? 0 : 3,
                        data: yErrRows,
                        turboThreshold: 0,
                        showInLegend: false,
                        enableMouseTracking: false
                    });
                }
                allSeries.push({
                    id: DATAPOINTS,
                    name: DATAPOINTS,
                    type: isLinePlot(params.plotStyle) ? 'line' : 'scatter',
                    color: hasErrorBars? datapointsColorWithErrors : datapointsColor,
                    data: rows,
                    marker,
                    turboThreshold: 0,
                    showInLegend: false,
                    point
                });
                allSeries.push({
                    id: SELECTED,
                    name: SELECTED,
                    color: hasErrorBars? selectedColorWithErrors : selectedColor,
                    data: selectedRows,
                    marker,
                    turboThreshold: 0,
                    showInLegend: false,
                    point
                });
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
                    const {x:ptX,y:ptY,rowIdx, weight} = rows[i];
                    const group = getWeightBasedGroup(weight, weightMin, weightMax, params.shading==='log');
                    const {x,y} = getCenter(ptX, ptY);
                    numericDataArr[group-1].push({x, y, rowIdx, weight});
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

            try {
                allSeries.forEach((series) => {
                    chart.addSeries(series, false, false);
                });

                chart.addSeries({
                    id: HIGHLIGHTED,
                    name: HIGHLIGHTED,
                    color: highlightedColor,
                    marker: {symbol: 'circle', radius: 4, lineColor: '#737373', lineWidth: 1},
                    data: highlightedData,
                    showInLegend: false
                }, true, false);
            } catch (error) {
                this.error = error;
                chart.showLoading(error);
            }
        }
    }

    render() {
        const {data, params, width, height, onSelection, desc} = this.props;


        // render chart
        this.error = undefined;
        const onSelectionEvent = this.onSelectionEvent;
        const {chartWidth, chartHeight} = calculateChartSize(width, height, this.props);

        const {xTitle, xGrid, xReversed, xOpposite, xLog} = getXAxisOptions(params);
        const {yTitle, yGrid, yReversed, yOpposite, yLog} = getYAxisOptions(params);
        const {xMin, xMax, yMin, yMax} = getZoomSelection(params);
        const {decimateKey, x, y} = data;
        const {xMin:xDataMin, xMax:xDataMax, yMin:yDataMin, yMax:yDataMax} = get(params, 'boundaries', {});
        // bin center and expression values need to be formatted
        const xFormat = (decimateKey || (x && x.match(/\W/))) ? getFormatString(Math.abs(xDataMax-xDataMin), 4) : undefined;
        const yFormat = (decimateKey || (y && y.match(/\W/))) ? getFormatString(Math.abs(yDataMax-yDataMin), 4) : undefined;

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
                selectionMarkerFill: decimateKey? selectionRectColor : selectionRectColorGray
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
            plotOptions: {
                series: {
                    animation: false,
                    cursor: 'pointer',
                    stickyTracking: false
                },
                line: {
                    marker: {
                        enabled: params.plotStyle === 'linepoints'
                    },
                    states: {
                        hover: {
                            lineWidthPlus: 0 // do not increase line width when hovering over the series, default is 1
                        }
                    }
                }
            },
            title: {
                text: desc
            },
            tooltip: {
                snap: 10,
                borderWidth: 1,
                formatter() {
                    const weight = this.point.weight ? `represents ${this.point.weight} points <br/>` : '';
                    const xval = xFormat ? numeral(this.point.x).format(xFormat) : this.point.x;
                    const xerr = formatError(this.point.x, this.point.xErr, this.point.xErrLow, this.point.xErrHigh);
                    const yval = yFormat ? numeral(this.point.y).format(yFormat) : this.point.y;
                    const yerr = formatError(this.point.y, this.point.yErr, this.point.yErrLow, this.point.yErrHigh);
                    return '<span> ' + `${params.x.label} = ${xval} ${xerr} ${params.x.unit} <br/>` +
                        `${params.y.label} = ${yval} ${yerr} ${params.y.unit} <br/> ` +
                        `${weight}</span>`;
                },
                shadow: !(decimateKey),
                useHTML: Boolean((decimateKey))
            },
            xAxis: {
                min: selFiniteMax(xMin, xDataMin),
                max: selFiniteMin(xMax, xDataMax),
                gridLineColor: '#e9e9e9',
                gridLineWidth: xGrid ? 1 : 0,
                lineColor: '#999',
                tickColor: '#ccc',
                opposite: xOpposite,
                reversed: xReversed,
                title: {text: xTitle},
                type: xLog ? 'logarithmic' : 'linear'
            },
            yAxis: {
                min: selFiniteMax(yMin,yDataMin),
                max: selFiniteMin(yMax,yDataMax),
                gridLineColor: '#e9e9e9',
                gridLineWidth: yGrid ? 1 : 0,
                tickWidth: 1,
                tickLength: 10,
                tickColor: '#ccc',
                lineWidth: 1,
                lineColor: '#999',
                endOnTick: false,
                opposite: yOpposite,
                reversed: yReversed,
                title: {text: yTitle},
                type: yLog ? 'logarithmic' : 'linear'
            },
            series: [{
                // This series is to make sure the axes are created.
                // Without actual series, xAxis creation is deferred
                // and there is no way to get value to pixel conversion
                // for sizing the symbol
                id: MINMAX,
                name: MINMAX,
                color: 'rgba(240, 240, 240, 0.1)',
                marker: {radius: 1},
                data: decimateKey? [[selFiniteMax(xMin, xDataMin), selFiniteMax(yMin,yDataMin)], [selFiniteMin(xMax, xDataMax), selFiniteMin(yMax,yDataMax)]]:[],
                showInLegend: false,
                enableMouseTracking: false,
                states: {
                    hover: {
                        enabled: false
                    }
                }
            }],
            credits: {
                enabled: false // removes a reference to Highcharts.com from the chart
            }
        };

        return (
            <div style={chartWidth<width?{float: 'left'}:{}}>
                <ReactHighcharts config={config} isPureConfig={true} ref='chart'/>
            </div>
        );
    }
}

XYPlotHighcharts.propTypes = {
    data: plotDataShape(),
    width: PropTypes.number,
    height: PropTypes.number,
    params: plotParamsShape(),
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

XYPlotHighcharts.defaultProps = {
    data: undefined,
    params: undefined,
    highlighted: undefined,
    onHighlightChange: undefined,
    onSelection: undefined,
    height: 300,
    desc: 'Sample XY Plot'
};

