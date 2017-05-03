import {isUndefined, get, has, omit} from 'lodash';
import shallowequal from 'shallowequal';
import React, {PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';

import {PlotlyWrapper} from './PlotlyWrapper.jsx';

import {SelectInfo} from '../../tables/SelectInfo.js';
import {parseDecimateKey} from '../../tables/Decimate.js';

import numeral from 'numeral';
import {getFormatString} from '../../util/MathUtil.js';

import {plotParamsShape, plotDataShape} from './XYPlotPropTypes.js';
import {calculateChartSize, getXAxisOptions, getYAxisOptions, getZoomSelection, formatError,
    isLinePlot, plotErrors, selFiniteMin, selFiniteMax} from './XYPlot.jsx';

const defaultShading = 'lin';

const PLOTLY_CONFIG = {displayModeBar: false};

const DATAPOINTS = 'data';
const DATAPOINTS_HEATMAP = 'data_heatmap';
const SELECTED = 'selected';
const HIGHLIGHTED = 'highlighted';

const datapointsColor = 'rgba(63, 127, 191, 0.5)';
const datapointsColorWithErrors = 'rgba(63, 127, 191, 0.7)';
const selectedColorWithErrors = 'rgba(255, 200, 0, 1)';
const selectedColor = 'rgba(255, 200, 0, 1)';
const highlightedColor = 'rgba(255, 165, 0, 1)';
const selectionRectColor = 'rgba(255, 209, 128, 0.5)';
const selectionRectColorGray = 'rgba(165, 165, 165, 0.5)';

const Y_TICKLBL_PX = 90;
const X_TICKLBL_PX = 60;
const MIN_MARGIN_PX = 10;
const MIN_YLBL_PX = 30;
const FSIZE = 12;


/**
 * Get range for a plotly axis
 * Plotly requires range to be reversed if the axis is reversed,
 * and limits to be log if axis scale is log
 * @param min - minimum value
 * @param max - maximum value
 * @param isLog - true, if an axis uses log scale
 * @param isReversed - true, if the axis should be reversed
 * @returns {Array<number>} an array for axis range property in plotly layout
 */
function getRange(min, max, isLog, isReversed) {
    const [r1, r2] = isReversed ? [max, min] : [min, max];
    return isLog ? [Math.log10(r1), Math.log10(r2)] : [r1, r2];
}

/**
 * A function to make plotly data for the given properties
 * @param props
 * @returns {{plotlyData: Array, selectedRows: Array, toRowIdx: Map}} plotly data array, selected rows, and decimated key to row index map
 */
function makeSeries(props) {
    const {data, params, selectInfo, highlighted} = props;
    const {rows, decimateKey, weightMin, weightMax} = data;
    const plotlyData = [];

    if (rows.length < 1) { return {plotlyData}; }

    let selectedRows = undefined; //the array with selected rows
    let toRowIdx = undefined; //the map from decimate key of a bin to rowIdx

    const highlightedData = [];


    if (!decimateKey) {
        const hasXErrors = plotErrors(params, 'x');
        const hasYErrors = plotErrors(params, 'y');
        const hasErrorBars = hasXErrors || hasYErrors;

        selectedRows = [];
        if (selectInfo) {
            const selectInfoCls = SelectInfo.newInstance(selectInfo, 0);

            selectedRows = rows.reduce((selrows, arow) => {
                if (selectInfoCls.isSelected(arow['rowIdx'])) {
                    selrows.push(arow);
                }
                return selrows;
            }, []);
        }

        const errorAxes = ['x','y'];
        const errors = errorAxes.map((axis) => {
            const err = {visible: false};
            if (get(params, [axis, 'error']) || get(params, [axis, 'errorLow']) || get(params, [axis, 'errorHigh'])) {
                err.visible = true;
                err.type = 'data';
                err.color = datapointsColorWithErrors;
                err.thickness = 1;
                err.width = (rows.length > 20) ? 0 : 3;
                if (get(params, [axis, 'error'])) {
                    err.symmetric = true;
                    err.array = rows.map((r) =>{return r[`${axis}Err`];});
                } else {
                    err.symmetric = false;
                    if (get(params, [axis, 'errorHigh'])) {
                        err.array = rows.map((r) => {return r[`${axis}ErrHigh`];});
                    }
                    if (get(params, [axis, 'errorLow'])) {
                        err.arrayminus = rows.map((r) => {return r[`${axis}ErrLow`];});
                    }
                }
            }
            return err;
        });

        const x = [];
        const y = [];
        rows.forEach((r) => {
            x.push(r.x);
            y.push(r.y);
        });

        plotlyData.push({
            name: DATAPOINTS,
            type: 'scatter',
            hoverinfo: 'text',
            mode: !isLinePlot(params.plotStyle)? 'markers' : (params.plotStyle === 'line' ? 'lines' : 'lines+markers'),
            marker: {
                symbol: 'circle',
                size: 6,
                color: hasErrorBars? datapointsColorWithErrors : datapointsColor
            },
            showlegend: false,
            error_x: errors[0],
            error_y: errors[1],
            x,
            y,
            text: generateTooltips(props, rows)
        });
        plotlyData.push({
            name: SELECTED,
            type: 'scatter',
            hoverinfo: 'text',  // skip? to display the tooltips from the points under
            mode: 'markers',
            marker: {
                symbol: 'circle',
                size: 6,
                color: hasErrorBars? selectedColorWithErrors : selectedColor
            },
            showlegend: false,
            x: selectedRows.map((r)=>r['x']),
            y: selectedRows.map((r)=>r['y']),
            text: generateTooltips(props, selectedRows)
        });
        if (!isUndefined(highlighted)) {
            highlightedData.push(highlighted);
        }
    } else {
        const {xMin, xUnit, nX, yMin, yUnit, nY} = parseDecimateKey(decimateKey);
        // get center point of the bin
        const getCenter = (xval,yval) => {
            return {
                // bitwise operators convert operands to 32-bit integer
                // hence they can be used as a fast way to truncate a float to an integer
                x: xMin+(~~((xval-xMin)/xUnit)+0.5)*xUnit,
                y: yMin+(~~((yval-yMin)/yUnit)+0.5)*yUnit
            };
        };

        const x = [];
        const y = [];
        const z = [];
        rows.forEach((r) => {
            const centerPt = getCenter(r.x, r.y);
            x.push(centerPt.x);
            y.push(centerPt.y);
            z.push(r.weight);
        });

        toRowIdx = new Map();
        // map decimate key (bin identifier in the form 'x:y') to rowIdx
        rows.forEach((r) => {
            toRowIdx.set(r.decimate_key, r.rowIdx);
        });

        //to avoid showing variable cell heatmap,
        //make sure the x and y values are populated for the empty bins
        for (let i=0; i<nX; i++ ) {
            for (let j = 0; j < nY; j++) {
                if (!toRowIdx.has(`${i}:${j}`)) {
                    x.push(xMin+(i+0.5)*xUnit);
                    y.push(yMin+(j+0.5)*yUnit);
                    z.push(undefined);
                }
            }
        }

        // to avoid axis and colorbar overlap,
        // when axis on the right, move colorbar to the left
        const {yOpposite} = getYAxisOptions(params);

        const heatmap = {
            name: DATAPOINTS_HEATMAP,
            type: 'heatmap',
            hoverinfo: 'text',
            showlegend: true,
            colorbar: {
                xanchor: yOpposite ? 'right' : 'left',
                x: yOpposite ? -0.02 : 1.02,
                thickness: 10,
                outlinewidth: 0,
                title: 'pts'
            },
            x,
            y,
            z,
            text: generateTooltips(props, rows)
        };

        if (get(params, 'shading', defaultShading) === defaultShading) {
            heatmap.colorscale = [[0, 'rgb(240,240,240)'], [1, 'rgb(37,37,37)']];
        } else if (weightMax - weightMin > 1) {
            // 9 colors (use http://colorbrewer2.org)
            const base = Math.pow(weightMax, 0.125); // 8 intervals
            const colors9 = ['rgb(247,247,247)', 'rgb(240,240,240)', 'rgb(217,217,217)', 'rgb(189,189,189)',
                'rgb(150,150,150)', 'rgb(115,115,115)', 'rgb(82,82,82)', 'rgb(37,37,37)', 'rgb(0,0,0)'];
            heatmap.colorscale = colors9.map((c, i) => {
                if (i === 0) { return [0, c]; }
                else if (i === 8) { return [1.0, c]; }
                else {return [Math.pow(base,i)/weightMax, c]; }
            });
        }
        plotlyData.push(heatmap);

        // add highlighted with weight
        if (!isUndefined(highlighted)) {
            highlightedData.push(highlightedWithWeight(props, toRowIdx));
        }
    }


    plotlyData.push({
        name: HIGHLIGHTED,
        type: 'scatter',
        hoverinfo: 'text',
        mode: 'markers',
        color: highlightedColor,
        marker: {symbol: 'circle', size: 8, color: highlightedColor, line: {width: 1, color: '#737373'}},
        x: highlightedData.map((r)=>r['x']),
        y: highlightedData.map((r)=>r['y']),
        showlegend: false,
        text: generateTooltips(props, highlightedData)
    });

    return omit({plotlyData, selectedRows, toRowIdx}, isUndefined);
}

/**
 * Returns new highlighted object with weight, if the data are decimated
 * @param props
 * @param toRowIdx
 * @returns {*}
 */
function highlightedWithWeight(props, toRowIdx) {
    const {data, highlighted} = props;
    const {rows, decimateKey} = data;
    if (rows.length > 0 && decimateKey) {
        const {xMin, xUnit, yMin, yUnit} = parseDecimateKey(decimateKey);
        const newHighlighted = Object.assign({}, highlighted);
        const binXIdx = ~~((highlighted.x - xMin) / xUnit);
        const binYIdx = ~~((highlighted.y - yMin) / yUnit);
        const binRowIdx = toRowIdx.get(`${binXIdx}:${binYIdx}`);
        if (binRowIdx >= 0) {
            const binRow = rows.find((r) => (r.rowIdx === binRowIdx));
            newHighlighted.weight = binRow.weight;
        }
        return newHighlighted;
    } else {
        return highlighted;
    }

}

/**
 * Generate tooltips
 * @param props
 * @param rows - data rows, an array of point defining objects
 * @returns {Array.<string>} tooltips
 */
function generateTooltips(props, rows) {
    const {data, params} = props;

    const {decimateKey, x, y} = data;
    const {xMin:xDataMin, xMax:xDataMax, yMin:yDataMin, yMax:yDataMax} = get(params, 'boundaries', {});

    // bin center and expression values need to be formatted
    const xFormat = (decimateKey || (x && x.match(/\W/))) ? getFormatString(Math.abs(xDataMax-xDataMin), 4) : undefined;
    const yFormat = (decimateKey || (y && y.match(/\W/))) ? getFormatString(Math.abs(yDataMax-yDataMin), 4) : undefined;

    return rows.map((point) => {
        const weight = point.weight ? `<br> represents ${point.weight} point${point.weight>1?'s':''}` : '';
        const xval = xFormat ? numeral(point.x).format(xFormat) : point.x;
        const xerr = formatError(point.x, point.xErr, point.xErrLow, point.xErrHigh);
        const yval = yFormat ? numeral(point.y).format(yFormat) : point.y;
        const yerr = formatError(point.y, point.yErr, point.yErrLow, point.yErrHigh);
        return `<span> ${params.x.label} = ${xval} ${xerr} ${params.x.unit} <br>` +
            ` ${params.y.label} = ${yval} ${yerr} ${params.y.unit} ` +
            `${weight} </span>`;
    });
}

/**
 * Create plotly data, layout, and style
 * @param props
 * @returns {{plotlyData: Array, plotlyLayout: Object, plotlyDivStyle: Object, toRowIdx: Map}} - an object with plotly data, layout, div style, and map from decimated key to row index
 */
function getChartingInfo(props) {
    const {params, width, height, desc} = props;

    const {chartWidth, chartHeight} = calculateChartSize(width, height, props);

    const {xTitle, xGrid, xReversed, xOpposite, xLog} = getXAxisOptions(params);
    const {yTitle, yGrid, yReversed, yOpposite, yLog} = getYAxisOptions(params);
    const {xMin, xMax, yMin, yMax} = getZoomSelection(params);
    const {xMin:xDataMin, xMax:xDataMax, yMin:yDataMin, yMax:yDataMax} = get(params, 'boundaries', {});

    const xAxisMin = selFiniteMax(xMin, xDataMin);
    const xAxisMax = selFiniteMin(xMax, xDataMax);
    const yAxisMin = selFiniteMax(yMin,yDataMin);
    const yAxisMax = selFiniteMin(yMax,yDataMax);

    const plotlyDivStyle = {
        border: '1px solid a5a5a5',
        borderRadius: 5,
        width: '100%',
        height: '100%'
    };

    const {plotlyData, selectedRows, toRowIdx} = makeSeries(props);

    const plotlyLayout = {
        height: chartHeight,
        width: chartWidth,
        hovermode: 'closest',
        dragmode: 'select',
        title: desc,
        legend: {
            font: {size: FSIZE},
            orientation: 'v',
            yanchor: 'top'
        },
        xaxis: {
            autorange:false,
            range: getRange(xAxisMin, xAxisMax, xLog, xReversed),
            title: xTitle,
            gridLineWidth: 1,
            type: xLog ? 'log' : 'linear',
            lineColor: '#e9e9e9',
            side: xOpposite ? 'top' : 'bottom',
            tickwidth: 1,
            ticklen: 5,
            showline: true,
            showgrid: xGrid,
            titlefont: {
                size: FSIZE
            },
            tickfont: {
                size: FSIZE
            },
            zeroline: false
        },
        yaxis: {
            autorange: false,
            range: getRange(yAxisMin, yAxisMax, yLog, yReversed),
            title: yTitle,
            gridLineWidth: 1,
            type: yLog ? 'log' : 'linear',
            lineColor: '#e9e9e9',
            side: yOpposite ? 'right' : 'left',
            tickwidth: 1,
            ticklen: 5,
            showline: true,
            showgrid: yGrid,
            titlefont: {
                size: FSIZE
            },
            tickprefix: yOpposite ? '' : '  ',
            ticksuffix: yOpposite ? '  ' : '',
            tickfont: {
                size: FSIZE
            },
            zeroline: false
        },
        margin: {
            l: yOpposite ? MIN_MARGIN_PX : Y_TICKLBL_PX,
            r: yOpposite ? Y_TICKLBL_PX : MIN_MARGIN_PX,
            b: xOpposite ? MIN_MARGIN_PX: X_TICKLBL_PX,
            t: xOpposite ? X_TICKLBL_PX: MIN_MARGIN_PX,
            pad: 2
        }
    };

    return {plotlyData, plotlyLayout, plotlyDivStyle, selectedRows, toRowIdx};

}


/**
 * Return an index of a trace with the given name
 * @param {{plotlyData: Array, plotlyLayout: Object, plotlyDivStyle: Object}} chartingInfo
 * @param {string} name - name of the trace
 * @returns {number} index of the trace
 */
function getTraceIdx(chartingInfo, name) {
    return chartingInfo.plotlyData.findIndex((t) => {return t.name === name;});
}


export class XYPlotPlotly extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            dataUpdateTraces: undefined,
            dataUpdate: undefined,
            layoutUpdate: undefined
        };

        this.afterRedraw = this.afterRedraw.bind(this);
        this.adjustYMargin = this.adjustYMargin.bind(this);
        this.updateSelectionRect = this.updateSelectionRect.bind(this);
        this.onSelectionEvent = this.onSelectionEvent.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        if (this.props === nextProps || !this.chartingInfo) {  return; }

        const propsToOmit = ['onHighlightChange', 'onSelection', 'highlighted'];
        if (shallowequal(omit(this.props, propsToOmit), omit(nextProps, propsToOmit)) &&
            get(this.props,'highlighted.rowIdx') === get(nextProps,'highlighted.rowIdx')) {
            return;
        }

        const {data, width, height, params, highlighted, selectInfo, desc} = this.props;

        // re-calculate charting info when the plot data change or an error occurs
        // shading change for density plot changes series
        if (nextProps.data !== data ||
            get(params, 'plotStyle') !== get(nextProps.params, 'plotStyle') ||
            plotErrors(params, 'x') !== plotErrors(nextProps.params, 'x') ||
            plotErrors(params, 'y') !== plotErrors(nextProps.params, 'y') ||
            get(params, 'shading', defaultShading) !== get(nextProps.params, 'shading', defaultShading)) {

            this.setState({
                dataUpdateTraces: undefined,
                dataUpdate: undefined,
                layoutUpdate: undefined
            });
            this.chartingInfo = null;

        } else {

            if (this.chartingInfo) {
                // parameters are validates in parent XYPlot component

                const {params:newParams, width:newWidth, height:newHeight, highlighted:newHighlighted, selectInfo:newSelectInfo, desc:newDesc } = nextProps;

                if (newDesc !== desc) {
                    this.setState({layoutUpdate: {title: newDesc}});
                }

                // selection change (selection is not supported for decimated data)
                if (data && data.rows && !data.decimateKey && newSelectInfo !== selectInfo) {
                    let selectedData = [];
                    if (newSelectInfo) {
                        const selectInfoCls = SelectInfo.newInstance(newSelectInfo, 0);
                        selectedData = data.rows.reduce((selrows, arow) => {
                            if (selectInfoCls.isSelected(arow['rowIdx'])) {
                                selrows.push(arow);
                            }
                            return selrows;
                        }, []);
                    }
                    const selectedTraceIdx  = getTraceIdx(this.chartingInfo, SELECTED);
                    if (selectedTraceIdx >= 0) {
                        this.setState({
                            dataUpdateTraces: selectedTraceIdx,
                            dataUpdate: {
                                // Arrays need to be wrapped according to restyle docs:
                                // In restyle, arrays are assumed to be used in conjunction with the trace indices provided.
                                // Therefore, to apply an array as a value, you need to wrap it in an additional array.
                                x: [selectedData.map((r) => r.x)],
                                y: [selectedData.map((r) => r.y)],
                                text: [generateTooltips(nextProps, selectedData)]
                            }
                        }, () => {this.chartingInfo.selectedRows = selectedData;});
                    }
                }

                // highlight change
                if (!shallowequal(highlighted, newHighlighted) && !isUndefined(get(newHighlighted, 'rowIdx'))) {
                    const highlightedData = [];
                    if (!isUndefined(newHighlighted)) {
                        if (get(nextProps, ['data', 'decimateKey'])) {
                            // make sure bin's weight is present in highlighted data when data are decimated
                            highlightedData.push(highlightedWithWeight(nextProps, get(this.chartingInfo, 'toRowIdx')));
                        } else {
                            highlightedData.push(newHighlighted);
                        }
                    }
                    const highlightedTraceIdx  = getTraceIdx(this.chartingInfo, HIGHLIGHTED);
                    if (highlightedTraceIdx >= 0) {
                        this.setState({
                            dataUpdateTraces: highlightedTraceIdx,
                            dataUpdate: {
                                x: [highlightedData.map((r) => r.x)],
                                y: [highlightedData.map((r) => r.y)],
                                text: [generateTooltips(nextProps, highlightedData)]
                            }
                        });
                    }
                }

                // plot parameters change
                if (params !== newParams) {
                    const dataUpdate = {};
                    const layoutUpdate = {};
                    let dataUpdateTraces = 0;
                    const newXOptions = getXAxisOptions(newParams);
                    const newYOptions = getYAxisOptions(newParams);
                    const oldXOptions = getXAxisOptions(params);
                    const oldYOptions = getYAxisOptions(params);
                    if (!shallowequal(oldXOptions, newXOptions)) {
                        layoutUpdate['xaxis.title'] = newXOptions.xTitle;
                        layoutUpdate['xaxis.showgrid'] = newXOptions.xGrid;
                        layoutUpdate['xaxis.side'] = newXOptions.xOpposite ? 'top' : 'bottom';
                        layoutUpdate['xaxis.type'] = newXOptions.xLog ? 'log' : 'linear';
                        layoutUpdate['margin.b'] = newXOptions.xOpposite ? MIN_MARGIN_PX : X_TICKLBL_PX;
                        layoutUpdate['margin.t'] = newXOptions.xOpposite ? X_TICKLBL_PX : MIN_MARGIN_PX;
                    }
                    if (!shallowequal(oldYOptions, newYOptions)) {
                        layoutUpdate['yaxis.title'] = newYOptions.yTitle;
                        layoutUpdate['yaxis.showgrid'] = newYOptions.yGrid;
                        layoutUpdate['yaxis.type'] = newYOptions.yLog ? 'log' : 'linear';
                        if (oldYOptions.yOpposite !== newYOptions.yOpposite) {
                            layoutUpdate['yaxis.side'] = newYOptions.yOpposite ? 'right' : 'left';
                            layoutUpdate['yaxis.tickprefix'] = newYOptions.yOpposite ? '' : '  ',
                            layoutUpdate['yaxis.ticksuffix'] = newYOptions.yOpposite ? '  ' : '',
                            layoutUpdate['margin.l'] = newYOptions.yOpposite ? MIN_MARGIN_PX : Y_TICKLBL_PX;
                            layoutUpdate['margin.r'] = newYOptions.yOpposite ? Y_TICKLBL_PX : MIN_MARGIN_PX;
                            if (data.decimateKey) {
                                // color bar should be on the opposite side of axis to avoid overlapping
                                dataUpdateTraces = getTraceIdx(this.chartingInfo, DATAPOINTS_HEATMAP);
                                dataUpdate['colorbar.xanchor'] = newYOptions.yOpposite ? 'right' : 'left';
                                dataUpdate['colorbar.x'] = newYOptions.yOpposite ? -0.02 : 1.02;
                            }
                        }
                    }
                    if (!shallowequal(params.zoom, newParams.zoom) || !shallowequal(params.boundaries, newParams.boundaries) ||
                        oldXOptions.xReversed !== newXOptions.xReversed || oldYOptions.yReversed !== newYOptions.yReversed) {
                        const {xMin, xMax, yMin, yMax} = getZoomSelection(newParams);
                        const {xMin:xDataMin, xMax:xDataMax, yMin:yDataMin, yMax:yDataMax} = get(newParams, 'boundaries', {});
                        const xAxisMin = selFiniteMax(xMin, xDataMin);
                        const xAxisMax = selFiniteMin(xMax, xDataMax);
                        const yAxisMin = selFiniteMax(yMin,yDataMin);
                        const yAxisMax = selFiniteMin(yMax,yDataMax);
                        layoutUpdate['xaxis.autorange'] = false;
                        layoutUpdate['xaxis.range'] = getRange(xAxisMin, xAxisMax, newXOptions.xLog, newXOptions.xReversed); // no change for reverse here
                        layoutUpdate['yaxis.range'] = getRange(yAxisMin, yAxisMax, newYOptions.yLog, newYOptions.yReversed); // no change for reverse here
                    }

                    if (!shallowequal(params.selection, newParams.selection)) {
                        if (newParams.selection) {
                            this.updateSelectionRect(newParams.selection, newXOptions.xLog, newYOptions.yLog);
                            return false;
                        } else {
                            layoutUpdate['shapes'] = [];
                            layoutUpdate['hovermode'] = 'closest'; // enable tooltips
                        }
                    }

                    if (Reflect.ownKeys(layoutUpdate).length > 0) {
                        if (Reflect.ownKeys(dataUpdate).length > 0) {
                            this.setState({layoutUpdate, dataUpdate, dataUpdateTraces});
                        } else {
                            this.setState({layoutUpdate});
                        }
                    }
                }

                // size change
                if (newWidth !== width || newHeight !== height ||
                    newParams.xyRatio !== params.xyRatio || newParams.stretch !== params.stretch) {
                    const {chartWidth, chartHeight} = calculateChartSize(newWidth, newHeight, nextProps);
                    this.setState({layoutUpdate: {height: chartHeight, width: chartWidth }});
                }
            }
        }
        return true;
    }

    shouldComponentUpdate(nextProps, nextState) {
        return sCompare(this, nextProps, nextState);
    }

    /**
     * Plotly does not adjust margins, when tick label length changes.
     * This method is calculating the length of the y tick labels and adjusts
     * the margin so that the tick labels do not averlap with axis title
     * @param chart
     */
    adjustYMargin(chart) {
        if (!chart) { return; }
        const ytickTexts = chart.querySelectorAll('g.ytick text');
        var maxYTickLen = MIN_MARGIN_PX;
        for (let i = 0, len=Math.abs(ytickTexts[i].clientWidth); i < ytickTexts.length; i++) {
            if (len > maxYTickLen) {
                maxYTickLen = len;
            }
        }

        // Firefox has a bug which makes clientWidth tick texts 0
        // https://bugzilla.mozilla.org/show_bug.cgi?id=874811
        // also, when axis on the right, clientWidth is big
        if (maxYTickLen === MIN_MARGIN_PX || maxYTickLen > 200) { return; }

        const newMargin = maxYTickLen + MIN_YLBL_PX;

        const {yOpposite} = getYAxisOptions(this.props.params);
        const layoutUpdate = {};
        let oldMargin;
        if (yOpposite) {
            oldMargin =  get(chart, ['layout', 'margin', 'r']);
            layoutUpdate['margin.r'] = newMargin;
        } else {
            oldMargin = get(chart, ['layout', 'margin', 'l']);
            layoutUpdate['margin.l'] = newMargin;
        }

        if (Math.abs(newMargin-oldMargin) > 2) {
            this.setState({layoutUpdate});
        }
    }

    /**
     * Selection area is represented by a rectangle.
     * User can filter, zoom, or select the points in the selection rectangle.
     * While the selection rectangle is present, tooltips should be disabled
     * @param selection
     * @param {boolean} xLog
     * @param {boolean} yLog
     */
    updateSelectionRect(selection, xLog, yLog) {

        if (this.selectionRect) {
            this.selectionRect.destroy();
            this.selectionRect = undefined;
        }
        if (selection) {
            const {xMin, xMax, yMin, yMax} = selection;
            const selColor = has(this.props, 'data.decimateKey') ? selectionRectColor : selectionRectColorGray;
            const layoutUpdate = {
                shapes: [{
                    layer: 'above',
                    type: 'rect',
                    xref: 'x',
                    yref: 'y',
                    x0: xLog ? Math.log10(xMin) : xMin,
                    y0: yLog ? Math.log10(yMin) : yMin,
                    x1: xLog ? Math.log10(xMax) : xMax,
                    y1: yLog ? Math.log10(yMax) : yMax,
                    fillcolor: selColor,
                    opacity: 0.5,
                    line: {
                        width: 0
                    }
                }],
                hovermode: false // disable tooltips
            };
            this.setState({layoutUpdate});
        }
    }

    onSelectionEvent(event) {
        const xAxis = event.xAxis[0];
        const yAxis = event.yAxis[0];

        if (xAxis && yAxis) {
            this.props.onSelection({xMin: xAxis.min, xMax: xAxis.max, yMin: yAxis.min, yMax: yAxis.max});
        }
    }


    /**
     * This method is called when new plotly chart is created
     * Because of plotly library is loaded asynchronously,
     * and restyle and relayout are called in promise,
     * whatever we usually do in componentDidMount should be done here,
     * whatever we usually do in componentDidUpdate, should be done
     * in plotly_relayout or plotly_restyle handler
     * @param chart
     * @param pl
     */
    afterRedraw(chart, pl) {

        const {params, onHighlightChange, onSelection} = this.props;

        // new plot adjustments
        if (params.selection) {
            this.updateSelectionRect(get(params, 'selection'), getXAxisOptions(params).xLog, getYAxisOptions(params).yLog);
        }
        this.adjustYMargin(chart);

        // handling tooltips in plotly_hover is very slow in Firefox
        // that's why we are pre-generating tooltips when creating data

        // handling highlight change
        if (onHighlightChange) {
            chart.on('plotly_click', (eventData) => {
                const curveNumber = eventData.points[0].curveNumber;
                const pointNumber = eventData.points[0].pointNumber;
                const chartingInfo = this.chartingInfo;
                if (curveNumber === getTraceIdx(chartingInfo, DATAPOINTS)) {
                    const rows = get(this.props, 'data.rows');
                    const point = rows && rows[pointNumber];
                    if (point) {
                        onHighlightChange(point.rowIdx);
                    }
                } else if (curveNumber === getTraceIdx(chartingInfo, SELECTED)) {
                    const selectedRows = chartingInfo.selectedRows;
                    const point = selectedRows && selectedRows[pointNumber];
                    if (point) {
                        onHighlightChange(point.rowIdx);
                    }
                } else if (curveNumber === getTraceIdx(chartingInfo, DATAPOINTS_HEATMAP)) {
                    // pointNumber is an array with y and x values - indexes of heatmap cell
                    const [y,x] = pointNumber;
                    const key = `${x}:${y}`;
                    const highlightedIdx = chartingInfo.toRowIdx.get(key);
                    if (!isUndefined(highlightedIdx)) {
                        onHighlightChange(highlightedIdx);
                    }
                }
            });
        }

        // handling selection (controls display of selection rectangle and selection options)
        if (onSelection) {
            chart.on('plotly_selected', (eventData) => {
                if (eventData && eventData.range) {
                    const [xMin, xMax] = eventData.range.x;
                    const [yMin, yMax] = eventData.range.y;
                    pl.d3.selectAll('.select-outline').remove();
                    if (get(this.props, ['data', 'decimateKey']) || eventData.points.length>0) {
                        onSelection({xMin, xMax, yMin, yMax});
                    } else {
                        const idx = getTraceIdx(this.chartingInfo, DATAPOINTS);
                        if (idx >= 0) {
                            // reset the opacity of the data points
                            const dataSeries = this.chartingInfo.plotlyData[idx];
                            const dataUpdate = {'data.marker': dataSeries.marker.color};
                            this.setState({dataUpdateTraces: idx, dataUpdate});
                        }
                    }
                } else {
                    onSelection(null);
                }
            });
        }

        // whenever relayout occurs, adjust margin to accommodate tick length
        chart.on('plotly_relayout', () => {
            this.adjustYMargin(chart);
        });

    }

    render() {
        if (!this.chartingInfo) {
            this.chartingInfo = getChartingInfo(this.props);
        }
        const {plotlyData, plotlyLayout, plotlyDivStyle} = this.chartingInfo;
        const {dataUpdateTraces, dataUpdate, layoutUpdate} = this.state;

        return (
            <div style={{float: 'left'}}>
                <PlotlyWrapper chartId={this.props.chartId} data={plotlyData} layout={plotlyLayout}  style={plotlyDivStyle}
                               dataUpdateTraces={dataUpdateTraces}
                               dataUpdate={dataUpdate}
                               layoutUpdate={layoutUpdate}
                               config={PLOTLY_CONFIG}
                               newPlotCB={this.afterRedraw}
                />
            </div>
        );
    }
}

XYPlotPlotly.propTypes = {
    chartId: PropTypes.string,
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

XYPlotPlotly.defaultProps = {
    data: undefined,
    params: undefined,
    highlighted: undefined,
    onHighlightChange: undefined,
    onSelection: undefined,
    height: 300,
    desc: 'Sample XY Plot'
};

