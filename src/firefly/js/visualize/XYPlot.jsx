/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {has, get} from 'lodash';
import React from 'react';
import ReactHighcharts from 'react-highcharts/bundle/highcharts';
//import {getFormatString} from '../util/MathUtil.js';

var XYPlot = React.createClass(
    {
        displayName: 'XYPlot',

        propTypes: {
            data: React.PropTypes.arrayOf(React.PropTypes.arrayOf(React.PropTypes.string)), // array of numbers [0] - nInBin, [1] - binMin, [2] - binMax
            height: React.PropTypes.number,
            params: React.PropTypes.object,
            highlightedRow: React.PropTypes.number,
            onHighlightChange: React.PropTypes.func,
            desc: React.PropTypes.string
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
                let chart = this.refs.chart.getChart();
                chart.series[0].data[highlightedRow].select(true,false);
            }
        },

        componentDidUpdate(pProps, pState, pContext) {
            let chart = this.refs.chart.getChart();
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

            const {data, params, highlightedRow, onHighlightChange} = this.props;
            const yReversed = params.y.options && params.y.options.includes('flip');
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
                    height: Number(this.props.height),
                    zoomType: 'xy'
                },
                exporting: {
                    enabled: true
                },
                legend: {
                    enabled: false
                },
                title: {
                    text: ''
                },
                tooltip: {
                    followPointer: true,
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
                        }
                    }
                },
                xAxis: {
                    gridLineWidth: 1,
                    lineColor: '#999',
                    tickColor: '#ccc',
                    title: {
                        text: this.props.desc
                    },
                    opposite: yReversed,
                    reversed: (params.x.options && params.x.options.includes('flip')),
                    type: (params.x.options && params.x.options.includes('log') ? 'logarithmic' : 'linear')
                },
                yAxis: {
                    gridLineColor: '#e9e9e9',
                    tickWidth: 1,
                    tickLength: 3,
                    tickColor: '#ccc',
                    lineColor: '#ccc',
                    endOnTick: false,
                    title: {
                        text: ''
                    },
                    reversed: yReversed,
                    type: (params.y.options && params.y.options.includes('log') ? 'logarithmic' : 'linear')
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
