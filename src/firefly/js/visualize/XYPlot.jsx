/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React from 'react';
import ReactHighcharts from 'react-highcharts/bundle/highcharts';
//import {getFormatString} from '../util/MathUtil.js';

var XYPlot = React.createClass(
    {
        displayName: 'XYPlot',

        propTypes: {
            data: React.PropTypes.arrayOf(React.PropTypes.arrayOf(React.PropTypes.number)), // array of numbers [0] - nInBin, [1] - binMin, [2] - binMax
            height: React.PropTypes.number,
            params: React.PropTypes.object,
            logs: React.PropTypes.oneOf(['x', 'y', 'xy']),
            reversed: React.PropTypes.oneOf(['x', 'y', 'xy']),
            desc: React.PropTypes.string
        },

        getDefaultProps() {
            return {
                data: undefined,
                height: 300,
                logs: undefined,
                reversed: undefined,
                desc: 'Sample XY Plot'
            };
        },

        render() {

            const {data, params} = this.props;
            const yReversed = (this.props.reversed && this.props.reversed.indexOf('y')>-1 ? true : false);
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
                    reversed: (this.props.reversed && this.props.reversed.indexOf('x')>-1 ? true : false),
                    type: (this.props.logs && this.props.logs.indexOf('x')>-1 ? 'logarithmic' : 'linear')
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
                    type: (this.props.logs && this.props.logs.indexOf('y')>-1 ? 'logarithmic' : 'linear')
                },
                series: [{
                    name: 'data points',
                    turboThreshold: 0,
                    data: numericData
                }],
                credits: {
                    enabled: false // removes a reference to Highcharts.com from the chart
                }
            };


            return (
                <div>
                    <ReactHighcharts config={config} isPureConfig='true' ref='xyplot'/>
                </div>
            );
        }
    }
);

export default XYPlot;
