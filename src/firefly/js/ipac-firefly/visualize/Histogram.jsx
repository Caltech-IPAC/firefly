/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/
/*jshint esnext:true*/

'use strict';
import React from 'react/addons';
import Highcharts from 'react-highcharts';
import numeral from 'numeral';
import {getFormatString} from '../util/MathUtil.js';

import {ServerRequest} from '../data/ServerRequest.js';
import {getRawDataSet} from '../rpc/SearchServicesJson.js';
import {parseRawDataSet} from '../util/DataSetParser.js';

module.exports= React.createClass(
    {
        propTypes: {
            //data: React.PropTypes.array.isRequired
            data: React.PropTypes.array,
            source: React.PropTypes.string,
            desc: React.PropTypes.string
        },

        getInitialState: function() {
            // if user passes the data as property, use it
            // otherwise, asynchronously get the data from the source
            return {
                userData : (this.props.data ? this.props.data : [])
            };
        },

        componentDidMount: function() {
            if (!this.props.data && this.props.source) {
                let extdata = [];
                this.getData().then(function (rawDataSet) {
                        let dataSet = parseRawDataSet(rawDataSet);
                        let model = dataSet.getModel();
                        for (var i = 0; i < model.size(); i++) {
                            let arow = model.getRow(i);
                            extdata.push(arow.map(val=>Number(val)));
                        }
                        if (this.isMounted()) {
                            this.setState({
                                userData: extdata
                            });
                        }
                    }.bind(this)
                ).catch(function (reason) {
                        console.log('Histogram failure: ' + reason);
                    }
                );
            }
        },

        /*
         * @return {Promise}
         */
        getData() {
            let req = new ServerRequest('IpacTableFromSource');
            req.setParam({name : 'source', value : 'http://localhost/demo/histdata.tbl'});
            req.setParam({name : 'startIdx', value : '0'});
            req.setParam({name : 'pageSize', value : '10000'});
            return getRawDataSet(req);
        },

        render() {
            var xrange = 0;
            if (this.state.userData.length > 1) {
                xrange = this.state.userData[1][0]-this.state.userData[0][0];
            }

            var config = {
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
                    borderWidth: 0.5,
                    borderColor: '#666',
                    pointPadding: 0.015,
                    groupPadding: 0,
                    color: '#e3e3e3'
                }],
                credits: {
                    enabled: false // removes a reference to Highcharts.com from the chart
                }
            };

            if (this.props.desc) {
                config.series[0].name = this.props.desc;
            }

            if (this.state.userData && this.state.userData.length > 0) {
                config.series[0].data = this.state.userData;
                if (this.state.userData.length > 1) {
                    // find the x range of the bin - assuming equal size
                    config.series[0].pointRange = xrange;
                }
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
