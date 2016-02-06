/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
//import {has, get} from 'lodash';
import React, {PropTypes} from 'react';
import ReactHighcharts from 'react-highcharts/bundle/highcharts';
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

const plotParamsShape = PropTypes.shape({
    xyRatio : PropTypes.number,
    stretch : PropTypes.oneOf(['fit','fill']),
    x : axisParamsShape,
    y : axisParamsShape
});

var XYPlot = React.createClass(
    {
        displayName: 'XYPlot',

        propTypes: {
            data: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)), // array of numbers [0] - nInBin, [1] - binMin, [2] - binMax
            width: PropTypes.number,
            height: PropTypes.number,
            params: plotParamsShape,
            highlightedRow: PropTypes.number,
            onHighlightChange: PropTypes.func,
            desc: PropTypes.string
        },

        getDefaultProps() {
            return {
                data: undefined,
                params: undefined,
                highlightedRow: 0,
                onHighlightChange: undefined,
                height: 300,
                desc: 'Sample XY Plot'
            };
        },

        componentDidMount() {
            const {highlightedRow} = this.props;
            if (highlightedRow) {
                const chart = this.refs.chart.getChart();
                chart.series[0].data[highlightedRow].select(true,false);
            }
        },

        componentDidUpdate(pProps, pState, pContext) {
            const chart = this.refs.chart.getChart();
            const prevHighlighted = pProps.highlightedRow;
            if (prevHighlighted) {
                chart.series[0].data[prevHighlighted].select(false,false);
            }

            const {highlightedRow} = this.props;
            if (highlightedRow) {
                chart.series[0].data[highlightedRow].select(true,false);
            }

        },

        render() {

            const {data, params, width, height, highlightedRow, onHighlightChange, desc} = this.props;

            let chartWidth=undefined, chartHeight=undefined;
            if (params.xyRatio) {
                if (params.stretch === 'fit') {
                    chartHeight = Number(height);
                    chartWidth = Number(params.xyRatio)*Number(chartHeight);
                } else {
                    chartWidth = Number(width);
                    chartHeight = chartWidth/Number(params.xyRatio);
                }
            } else {
                chartHeight = Number(height);
            }

            const xTitle = params.x.label + (params.x.unit ? ', '+params.x.unit : '');
            const yTitle = params.y.label + (params.y.unit ? ', '+params.y.unit : '');

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

            const toNumber = (val)=>Number(val);
            const numericData = data.reduce((numdata, arow) => {
                                numdata.push(arow.map(toNumber));
                                return numdata;
                            }, []);


            var config = {
                chart: {
                    renderTo: 'container',
                    type: 'scatter',
                    alignTicks: false,
                    height: chartHeight,
                    width: chartWidth,
                    zoomType: 'xy'
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
                        return '<span> '+`${params.x.label} = ${this.point.x} ${params.x.unit} <br/>`+
                            `${params.y.label} = ${this.point.y} ${params.y.unit} <br/> </span>`;
                    }
                },
                plotOptions: {
                    scatter: {
                        allowPointSelect: true,
                        animation: false,
                        cursor: 'pointer',
                        marker: {
                            states: {
                                select: {
                                    fillColor: 'yellow',
                                    lineWidth: 1,
                                    radius: 7
                                }
                            }
                        },
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
                    title: {text : xTitle},
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
                    title: {text : yTitle},
                    type: yLog ? 'logarithmic' : 'linear'
                },
                series: [{
                    name: 'data points',
                    turboThreshold: 0,
                    data: numericData,
                    point: {
                        events: {
                            select() {
                                if (onHighlightChange) {
                                    var highlighted = this.rowIdx ? this.rowIdx : this.series.data.indexOf(this);
                                    if (highlighted !== highlightedRow) {
                                        onHighlightChange(highlighted);
                                    }
                                }
                            }
                        }
                    }
                }],
                credits: {
                    enabled: false // removes a reference to Highcharts.com from the chart
                }
            };


            return (
                <div>
                    <ReactHighcharts config={config} isPureConfig={true} ref='chart'/>
                </div>
            );
        }
    }
);

export default XYPlot;
