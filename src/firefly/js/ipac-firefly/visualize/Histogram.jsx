/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/
/*jshint esnext:true*/

"use strict";
import React from 'react/addons';
import Highcharts from 'react-highcharts';
import numeral from 'numeral';
import {getFormatString} from 'ipac-firefly/util/MathUtil.js';


module.exports= React.createClass(
    {
        propTypes: {
            data: React.PropTypes.array.isRequired
        },

        render() {
            var xrange = 0;
            if (this.props.data.length > 1) {
                xrange = this.props.data[1][0]-this.props.data[0][0];
            }

            var config =  {
                chart: {
                    renderTo: 'container',
                    type: 'column',
                    alignTicks: false,
                    marginTop: 25,
                    showAxes: true
                },
                exporting: {
                    enabled: true
                },
                title: {
                    text: ''
                },
                tooltip: {
                    borderWidth: 1,
                    //pointFormat: "<b>Center:</b> {point.x:.2f}<br><b>Count:</b> {point.y}"
                    formatter: function () {
                        return '<b>Range:</b> (' +
                                   numeral(this.x-xrange/2.0).format(getFormatString(xrange,2)) + ','+
                                   numeral(this.x+xrange/2.0).format(getFormatString(xrange,2))+')<br/>' +
                            '<b>Count:</b> ' + this.y;
                    }
                },
                plotOptions: {
                    series: {
                        minPointLength: 1,
                        shadow: false,
                        marker: {
                            enabled: false
                        }
                    }
                },
                xAxis: {
                    lineColor: '#999',
                    tickColor: '#ccc'
                },
                yAxis: {
                    title: {
                        text: ''
                    },
                    gridLineColor: '#e9e9e9',
                    tickWidth: 1,
                    tickLength: 3,
                    tickColor: '#ccc',
                    lineColor: '#ccc',
                    endOnTick: false
                },
                series: [{
                    name: 'Sample Distribution',
                    data: [], // to be defined
                    borderWidth: .5,
                    borderColor: '#666',
                    pointPadding: .015,
                    groupPadding: 0,
                    color: '#e3e3e3'
                }],
                credits: {
                    enabled: false // removes a reference to Highcharts.com from the chart
                }
            };

            config.series[0].data = this.props.data;
            if (this.props.data.length > 1) {
                // find the x range of the bin - assuming equal size
                config.series[0].pointRange = xrange;
            }

            /* jshint ignore:start */
            return (
                <div>
                    <Highcharts config={config}></Highcharts>
                </div>
            );
            /* jshint ignore:end */
        }
    });
