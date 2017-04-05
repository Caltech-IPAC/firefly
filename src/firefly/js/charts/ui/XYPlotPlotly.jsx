import {isUndefined, get, has, omit} from 'lodash';
import shallowequal from 'shallowequal';
import React, {PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';

//import {getPlotLy} from '../PlotlyConfig.js';
//import Plotly from '../PlotlyConfig.js';
import {PlotlyWrapper} from './PlotlyWrapper.jsx';

import {SelectInfo} from '../../tables/SelectInfo.js';
import {parseDecimateKey} from '../../tables/Decimate.js';

import numeral from 'numeral';
import {getFormatString} from '../../util/MathUtil.js';

import {plotParamsShape, plotDataShape} from './XYPlotPropTypes.js';
import {calculateChartSize,
    getXAxisOptions, getYAxisOptions, getZoomSelection, formatError, isLinePlot, plotErrors,
    selFiniteMin, selFiniteMax, validate} from './XYPlot.jsx';



const defaultShading = 'lin';

const PLOTLY_CONFIG = {displayModeBar: false};

const DATAPOINTS = 'data';
const DATAPOINTS_HEATMAP = 'data_heatmap';
const SELECTED = 'selected';
const HIGHLIGHTED = 'highlighted';

const datapointsColor = 'rgba(63, 127, 191, 0.5)';
const datapointsColorWithErrors = 'rgba(63, 127, 191, 0.7)';
//const errorBarColor = 'rgba(63, 127, 191, 0.5)'; //'rgba(255, 209, 128, 0.5)';
const selectedColorWithErrors = 'rgba(255, 200, 0, 1)';
const selectedColor = 'rgba(255, 200, 0, 1)';
const highlightedColor = 'rgba(255, 165, 0, 1)';
const selectionRectColor = 'rgba(255, 209, 128, 0.5)';
const selectionRectColorGray = 'rgba(165, 165, 165, 0.5)';


function makeSeries(props) {
    const {data, params, selectInfo, highlighted} = props;
    const {rows, decimateKey, weightMin, weightMax} = data;

    if (rows.length < 1) { return []; }

    const allSeries = [];

    const highlightedData = [];
    if (!isUndefined(highlighted)) {
        highlightedData.push(highlighted);
    }

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

        allSeries.push({
            //id: DATAPOINTS,
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
            y
        });
        allSeries.push({
            //id: SELECTED,
            name: SELECTED,
            type: 'scatter',
            hoverinfo: 'skip',
            mode: 'markers',
            marker: {
                symbol: 'circle',
                size: 6,
                color: hasErrorBars? selectedColorWithErrors : selectedColor
            },
            showlegend: false,
            x: selectedRows.map((r)=>r['x']),
            y: selectedRows.map((r)=>r['y'])
        });
    } else {
        const {xMin, xUnit, yMin, yUnit} = parseDecimateKey(decimateKey);
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
        allSeries.push({
            name: DATAPOINTS_HEATMAP,
            type: 'heatmap',
            colorscale: [[0, 'rgb(216,216,216)'], [1, 'rgb(40,40,40)']],
            hoverinfo: 'text',
            showlegend: true,
            colorbar: {
                thickness: 10,
                outlinewidth: 0,
                title: 'pts'
            },
            x,
            y,
            z
        });
    }


    allSeries.push({
        //id: HIGHLIGHTED,
        name: HIGHLIGHTED,
        type: 'scatter',
        hoverinfo: 'skip',
        mode: 'markers',
        color: highlightedColor,
        marker: {symbol: 'circle', size: 8, lineColor: '#737373', lineWidth: 1, color: highlightedColor},
        x: highlightedData.map((r)=>r['x']),
        y: highlightedData.map((r)=>r['y']),
        showlegend: false
    });

    return allSeries;
}


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

    const plotlyDivStyle= {
        border: '1px solid a5a5a5',
        borderRadius: 5,
        width: '100%',
        height: '100%'
    };

    const plotlyData = makeSeries(props);

    const plotlyLayout = {
        height: chartHeight,
        width: chartWidth,
        hovermode: 'closest',
        dragmode: 'select',
        title: desc,
        //titlefont: {size: 16},
        legend: {
            font: {size: 12},
            orientation: 'v',
            yanchor: 'top'
        },
        xaxis: {
            autorange: xReversed ? 'reversed' : true,
            range: xLog? [Math.log10(xAxisMin), Math.log10(xAxisMax)] : [xAxisMin, xAxisMax],
            title: xTitle,
            gridLineWidth: 1,
            type: xLog ? 'log' : 'linear',
            lineColor: '#e9e9e9',
            mirror: xOpposite ? 'allticks' : false,
            tickwidth: 1,
            ticklen: 5,
            showline: true,
            showgrid: xGrid,
            titlefont: {
                size: 12
            },
            tickfont: {
                size: 12
            }
        },
        yaxis: {
            autorange: yReversed ? 'reversed' : true,
            range: yLog? [Math.log10(yAxisMin), Math.log10(yAxisMax)] : [yAxisMin, yAxisMax],
            title: yTitle,
            gridLineWidth: 1,
            type: yLog ? 'log' : 'linear',
            lineColor: '#e9e9e9',
            mirror: yOpposite ? 'all' : false,
            tickwidth: 1,
            ticklen: 5,
            showline: true,
            showgrid: yGrid,
            titlefont: {
                size: 12
            },
            tickfont: {
                size: 12
            }
        },
        margin: {
            l: 65,
            r: 5,
            b: 55,
            t: 5,
            pad: 2
        }
    };


    //const minMaxSeries = {
    //    // This series is to make sure the axes are created.
    //    // Without actual series, xAxis creation is deferred
    //    // and there is no way to get value to pixel conversion
    //    // for sizing the symbol
    //    //id: MINMAX,
    //    name: MINMAX,
    //    hoverinfo: 'skip',
    //    type: scatter,
    //    mode: 'markers',
    //    marker: {
    //        radius: 1,
    //        color: 'rgba(240, 240, 240, 0.1)'
    //    },
    //    x: decimateKey? [selFiniteMax(xMin, xDataMin), selFiniteMin(xMax, xDataMax)] : [],   //handle logs?
    //    y: decimateKey? [selFiniteMax(yMin,yDataMin), selFiniteMin(yMax,yDataMax)] : [],
    //    showlegend: false,
    //};
    return {plotlyData, plotlyLayout, plotlyDivStyle};

}

function getTraceIdx(chartingInfo, name) {
    return chartingInfo.plotlyData.findIndex((t) => {return t.name === name;});
}


export class XYPlotPlotly extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            dataUpdateTraces: undefined,
            dataUpdate: null,
            layoutUpdate: null
        };

        this.afterRedraw= this.afterRedraw.bind(this);
        this.updateSelectionRect = this.updateSelectionRect.bind(this);
        this.adjustPlotDisplay = this.adjustPlotDisplay.bind(this);
        this.onSelectionEvent = this.onSelectionEvent.bind(this);
        this.shouldAnimate = this.shouldAnimate.bind(this);
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

            this.setState({});
            this.chartingInfo = null;

        } else {

            if (this.chartingInfo) {
                const {params:newParams, width:newWidth, height:newHeight, highlighted:newHighlighted, selectInfo:newSelectInfo, desc:newDesc } = nextProps;
                const errors = validate(newParams, data);
                if (errors.length > 0) {
                    this.error = errors[0];
                    this.setState({error: errors[0]});
                    return;
                }

                if (newDesc !== desc) {
                    this.setState({layoutUpdate: {title: newDesc}});
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
                    const selectedTraceIdx  = getTraceIdx(this.chartingInfo, SELECTED);
                    if (selectedTraceIdx >= 0) {
                        this.setState({
                            dataUpdateTraces: selectedTraceIdx,
                            dataUpdate: {
                                x: [selectedData.map((r) => r.x)],
                                y: [selectedData.map((r) => r.y)]
                            }
                        });
                    }
                }

                // highlight change
                if (!shallowequal(highlighted, newHighlighted) && !isUndefined(get(newHighlighted, 'rowIdx'))) {
                    const highlightedData = [];
                    if (!isUndefined(newHighlighted)) {
                        highlightedData.push(newHighlighted);
                    }
                    const highlightedTraceIdx  = getTraceIdx(this.chartingInfo, HIGHLIGHTED);
                    if (highlightedTraceIdx >= 0) {
                        this.setState({
                            dataUpdateTraces: highlightedTraceIdx,
                            dataUpdate: {
                                x: [highlightedData.map((r) => r.x)],
                                y: [highlightedData.map((r) => r.y)]
                            }
                        });
                    }
                }

                // plot parameters change
                if (params !== newParams) {
                    const updates = {};
                    //const xoptions = {};
                    //const yoptions = {};
                    const newXOptions = getXAxisOptions(newParams);
                    const newYOptions = getYAxisOptions(newParams);
                    if (!shallowequal(getXAxisOptions(params), newXOptions)) {
                        updates['xaxis.title'] = newXOptions.xTitle;
                        updates['xaxis.showgrid'] = newXOptions.xGrid;
                        updates['xaxis.autorange'] = newXOptions.xReversed ? 'reversed' : true;
                        updates['xaxis.mirror'] = newXOptions.xOpposite ? 'allticks' : false;
                        updates['xaxis.type'] = newXOptions.xLog ? 'log' : 'linear';
                    }
                    if (!shallowequal(getYAxisOptions(params), newYOptions)) {
                        updates['yaxis.title'] = newYOptions.yTitle;
                        updates['yaxis.showgrid'] = newYOptions.yGrid;
                        updates['yaxis.autorange'] = newYOptions.yReversed ? 'reversed' : true;
                        updates['yaxis.mirror'] = newYOptions.yOpposite ? 'allticks' : false;
                        updates['yaxis.type'] = newYOptions.yLog ? 'log' : 'linear';
                    }
                    if (!shallowequal(params.zoom, newParams.zoom) || !shallowequal(params.boundaries, newParams.boundaries)) {
                        const {xMin, xMax, yMin, yMax} = getZoomSelection(newParams);
                        const {xMin:xDataMin, xMax:xDataMax, yMin:yDataMin, yMax:yDataMax} = get(newParams, 'boundaries', {});
                        const xAxisMin = selFiniteMax(xMin, xDataMin);
                        const xAxisMax = selFiniteMin(xMax, xDataMax);
                        const yAxisMin = selFiniteMax(yMin,yDataMin);
                        const yAxisMax = selFiniteMin(yMax,yDataMax);
                        updates['xaxis.range'] = newXOptions.xLog ? [Math.log10(xAxisMin), Math.log10(xAxisMax)] : [xAxisMin, xAxisMax];
                        updates['yaxis.range'] = newYOptions.yLog ? [Math.log10(yAxisMin), Math.log10(yAxisMax)] : [yAxisMin, yAxisMax];
                    }

                    if (!shallowequal(params.selection, newParams.selection)) {
                        if (newParams.selection) {
                            this.updateSelectionRect(newParams.selection, newXOptions.xLog, newYOptions.yLog);
                        } else {
                            updates['shapes'] = [];
                        }
                    }

                    if (Reflect.ownKeys(updates).length > 0) {
                        this.setState({
                            layoutUpdate: updates
                        });
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
        return sCompare(nextProps, nextState);
    }

    componentDidMount() {
        this.adjustPlotDisplay();
    }

    componentDidUpdate() {
        this.adjustPlotDisplay();
    }


    shouldAnimate() {
        const {data} = this.props;
        return (!data || !data.rows || data.rows.length <= 250);
    }

    adjustPlotDisplay() {
        //const {params} = this.props;

        // TODO?
        //if (params.selection) {
        //    this.updateSelectionRect(params.selection);
        //}
    }

    updateSelectionRect(selection, xLog, yLog) {

        if (this.selectionRect) {
            this.selectionRect.destroy();
            this.selectionRect = undefined;
        }
        if (selection) {
            const {xMin, xMax, yMin, yMax} = selection;
            const selColor = has(this.props, 'data.decimateKey') ? selectionRectColor : selectionRectColorGray;
            this.setState({
                layoutUpdate: {
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
                    }]
                }
            });
        }
    }

    onSelectionEvent(event) {
        const xAxis = event.xAxis[0];
        const yAxis = event.yAxis[0];

        if (xAxis && yAxis) {
            this.props.onSelection({xMin: xAxis.min, xMax: xAxis.max, yMin: yAxis.min, yMax: yAxis.max});
        }
    }


    // todo: hover and select should not be triggered under selection rectangle
    afterRedraw(chart, pl) {

        const {data, params, highlighted, onHighlightChange, onSelection} = this.props;

        const {decimateKey, x, y} = data;
        const {xMin:xDataMin, xMax:xDataMax, yMin:yDataMin, yMax:yDataMax} = get(params, 'boundaries', {});

        // update selection rectangle
        this.updateSelectionRect(get(params, 'selection'), getXAxisOptions(params).xLog, getYAxisOptions(params).yLog);

        // bin center and expression values need to be formatted
        const xFormat = (decimateKey || (x && x.match(/\W/))) ? getFormatString(Math.abs(xDataMax-xDataMin), 4) : undefined;
        const yFormat = (decimateKey || (y && y.match(/\W/))) ? getFormatString(Math.abs(yDataMax-yDataMin), 4) : undefined;

        const rows = get(this.props, 'data.rows');
        const chartingInfo = this.chartingInfo;

        // handling tooltips
        chart.on('plotly_hover', (eventData) => {
            const curveNumber = eventData.points[0].curveNumber;
            if (curveNumber === getTraceIdx(chartingInfo, DATAPOINTS)) {
                const pointNumber = eventData.points[0].pointNumber;
                const point = rows && rows[pointNumber];

                if (point) {
                    const weight = point.weight ? `<br> represents ${point.weight} points ` : '';
                    const xval = xFormat ? numeral(point.x).format(xFormat) : point.x;
                    const xerr = formatError(point.x, point.xErr, point.xErrLow, point.xErrHigh);
                    const yval = yFormat ? numeral(point.y).format(yFormat) : point.y;
                    const yerr = formatError(point.y, point.yErr, point.yErrLow, point.yErrHigh);
                    const str = `<span> ${params.x.label} = ${xval} ${xerr} ${params.x.unit} <br>` +
                        `${params.y.label} = ${yval} ${yerr} ${params.y.unit} ` +
                        `${weight} </span>`;


                    this.setState({
                        dataUpdate: {text: str},
                        dataUpdateTraces: curveNumber
                    });
                }
            }
        });

        // handling highlight change
        var highlightedIdx = highlighted.rowIdx;
        if (onHighlightChange) {
            chart.on('plotly_click', (eventData) => {
                const curveNumber = eventData.points[0].curveNumber;
                const pointNumber = eventData.points[0].pointNumber;
                if (curveNumber === getTraceIdx(chartingInfo, DATAPOINTS)) {
                    const point = rows && rows[pointNumber];
                    if (point && point.rowIdx !== highlightedIdx) {
                        highlightedIdx = point.rowIdx;
                        onHighlightChange(highlightedIdx);
                    }
                }
            });
        }

        // handling selection (controls display of selection rectangle and selection options)
        if (onSelection) {
            chart.on('plotly_selected', (eventData) => {
                console.log(eventData);
                if (onSelection) {
                    if (eventData && eventData.range) {
                        const [xMin, xMax] = eventData.range.x;
                        const [yMin, yMax] = eventData.range.y;
                        pl.d3.selectAll('.select-outline').remove();
                        onSelection({xMin, xMax, yMin, yMax});
                    } else {
                        onSelection(null);
                    }
                }
            });
        }
    }

    render() {
        if (!this.chartingInfo) {
            this.chartingInfo = getChartingInfo(this.props);
        }
        const {plotlyData, plotlyLayout, plotlyDivStyle} = this.chartingInfo;
        const {dataUpdateTraces, dataUpdate, layoutUpdate} = this.state;

        // render chart
        this.error = undefined;

        return (
            <div style={{float: 'left'}}>
                <PlotlyWrapper data={plotlyData} layout={plotlyLayout}  style={plotlyDivStyle}
                               dataUpdateTraces={dataUpdateTraces}
                               dataUpdate={dataUpdate}
                               layoutUpdate={layoutUpdate}
                               config={PLOTLY_CONFIG}
                               divUpdateCB={(div) => this.chartDiv = div}
                               newPlotCB={this.afterRedraw}
                />
            </div>
        );
    }
}

XYPlotPlotly.propTypes = {
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

