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
            data: React.PropTypes.array, // array of numbers [0] - nInBin, [1] - binMin, [2] - binMax
            source: React.PropTypes.string, // url with the histogram table in IPAC Table or CVS format
            desc: React.PropTypes.string,
            binColor: React.PropTypes.string
        },

        getDefaultProps : function() {
            return {
                data: undefined,
                source: undefined,
                desc: 'Sample Distribution',
                binColor: '#d1d1d1'
            };
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
            req.setParam({name : 'source', value : this.props.source});
            req.setParam({name : 'startIdx', value : '0'});
            req.setParam({name : 'pageSize', value : '10000'});
            return getRawDataSet(req);
        },

        /*
         * @param {String} color - hex color, exactly seven characters log, starting with '#'
         * @param {Number} persentage (0.1 means 10 percent lighter, -0.1 - 10 percent darker)
         * @return {String} lighter or darker shade of the given hex color
         * from http://stackoverflow.com/questions/5560248/programmatically-lighten-or-darken-a-hex-color-or-rgb-and-blend-colors
         */
        shadeColor(color, percent) {
            var f=parseInt(color.slice(1),16),t=percent<0?0:255,p=percent<0?percent*-1:percent,R=f>>16,G=f>>8&0x00FF,B=f&0x0000FF;
            return '#'+(0x1000000+(Math.round((t-R)*p)+R)*0x10000+(Math.round((t-G)*p)+G)*0x100+(Math.round((t-B)*p)+B)).toString(16).slice(1);
        },


        /*
         * @param config
         * @return {Boolean} f data points are set, false if no points are present
         */
        setChartConfig(config) {
            if (!this.state.userData || this.state.userData.length < 1) {
                return false;
            }
            var points = [], zones=[];
            var lighterColor = this.shadeColor(this.props.binColor, 0.1);
            var error;
            try {
                this.state.userData.forEach(function (value, index) {
                        let xrange = this.state.userData[index][2] - this.state.userData[index][1];
                        let formatStr = getFormatString(xrange,2);
                        let centerStr = numeral(this.state.userData[index][1]+xrange/2.0).format(formatStr);
                        let rangeStr = numeral(this.state.userData[index][1]).format(formatStr)+' to '+numeral(this.state.userData[index][2]).format(formatStr);
                        // a point for the bin's left edge (minimum)
                        points.push({
                            // name - formatted bin center
                            name: centerStr,
                            range: rangeStr,
                            // x - middle of the bin
                            x: this.state.userData[index][1],
                            // y - number of points in the bin
                            y: this.state.userData[index][0]
                        });
                        // a point for the bin's max edge (maximum)
                        points.push({
                            // name - formatted bin center
                            name: centerStr,
                            range: rangeStr,
                            // x - middle of the bin
                            x: this.state.userData[index][2]-xrange/1000.0,
                            // y - number of points in the bin
                            y: this.state.userData[index][0]
                        });
                        // zones allow to separate visually one bin from another
                        zones.push({
                            value: this.state.userData[index][2],
                            color: (index%2===0)?this.props.binColor:lighterColor
                        });
                    }.bind(this)
                );
            }
            catch(e) {
                error = e;
            }
            if (error) {
                return false;
            } else {
                config.series[0].data = points;
                config.plotOptions.area.zones = zones;
            }
        },

        render() {
            var config = {
                chart: {
                    renderTo: 'container',
                    type: 'area',
                    alignTicks: false,
                    marginTop: 25
                },
                exporting: {
                    enabled: true
                },
                title: {
                    text: ''
                },
                tooltip: {
                    followPointer: true,
                    borderWidth: 1,
                    formatter: function () {
                        return '<b>Bin center:</b> '+this.point.name+
                            '<br><b>Range:</b> '+this.point.range+
                            '<br><b>Count:</b> ' + this.y;
                    }
                },
                plotOptions: {
                    series: {
                        threshold: 0
                    },
                    area: {
                        marker: {
                            enabled: false
                        },
                        lineWidth: 2,
                        step: 'left',
                        trackByArea: true,
                        states: {
                            hover: {
                                enabled: false
                            }
                        },
                        zoneAxis: "x",
                        zones: [] // color ajacent bins slightly different by defining zones
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
                    name: this.props.desc,
                    turboThreshhold: 0,
                    color: this.props.binColor,
                    data: []
                }],
                credits: {
                    enabled: false // removes a reference to Highcharts.com from the chart
                }
            };

            this.setChartConfig(config);

            /* jshint ignore:start */
            return (
                <div>
                    <Highcharts config={config}></Highcharts>
                </div>
            );
            /* jshint ignore:end */
        }
    });
