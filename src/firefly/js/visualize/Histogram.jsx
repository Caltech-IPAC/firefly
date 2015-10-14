/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';
import Highcharts from 'react-highcharts';
import numeral from 'numeral';
import {getFormatString} from '../util/MathUtil.js';

import {ServerRequest} from '../data/ServerRequest.js';
import {getRawDataSet} from '../rpc/SearchServicesJson.js';
import {parseRawDataSet} from '../util/DataSetParser.js';

var Histogram = React.createClass(
    {
        displayName: 'Histogram',

        propTypes: {
            //data: React.PropTypes.array.isRequired
            data: React.PropTypes.arrayOf(React.PropTypes.arrayOf(React.PropTypes.number)), // array of numbers [0] - nInBin, [1] - binMin, [2] - binMax
            source: React.PropTypes.string, // url with the histogram table in IPAC Table or CVS format
            height: React.PropTypes.number,
            logs: React.PropTypes.oneOf(['x','y','xy']),
            reversed: React.PropTypes.oneOf(['x','y','xy']),
            desc: React.PropTypes.string,
            binColor(props, propName, componentName) {
                if (props[propName] && !/^#[0-9a-f]{6}/.test(props[propName])) {
                    return new Error(`Invalid bin color in ${componentName}, should be hex with exactly 7 characters long.`);
                }
            }
        },

        getDefaultProps() {
            return {
                data: undefined,
                source: undefined,
                height: 300,
                logs: undefined,
                reversed: undefined,
                desc: 'Sample Distribution',
                binColor: '#d1d1d1'
            };
        },

        getInitialState() {
            // if user passes the data as property, use it
            // otherwise, asynchronously get the data from the source
            return {
                userData : (this.props.data ? this.props.data : [])
            };
        },

        fetchDataFromSource(source) {
            const extdata = [];
            this.getData(source).then(function (rawDataSet) {
                    const dataSet = parseRawDataSet(rawDataSet);
                    const model = dataSet.getModel();
                    var toNumber = val=>Number(val);
                    for (let i = 0; i < model.size(); i++) {
                        const arow = model.getRow(i);
                        extdata.push(arow.map(toNumber));
                    }
                    if (this.isMounted()) {
                        this.setState({
                            userData: extdata
                        });
                    }
                }.bind(this)
            ).catch(function (reason) {
                    console.error(`Histogram failure: ${reason}`);
                }
            );
        },

        componentDidMount() {
            if (!this.props.data && this.props.source) {
                this.fetchDataFromSource(this.props.source);
            }
        },

        componentWillReceiveProps(nextProps) {
            if (nextProps.data) {
                this.setState({
                    userData: nextProps.data
                });
            } else if (nextProps.source && this.props.source !== nextProps.source) {
                this.setState({
                    userData: []
                });
                this.fetchDataFromSource(nextProps.source);
            }
         },

        /*
         * @return {Promise}
         */
        getData(source) {
            const req = new ServerRequest('IpacTableFromSource');
            req.setParam({name : 'source', value : source});
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
            return `#${(0x1000000+(Math.round((t-R)*p)+R)*0x10000+(Math.round((t-G)*p)+G)*0x100+(Math.round((t-B)*p)+B)).toString(16).slice(1)}`;
        },

        /*
         * Expecting an 2 dimensional array of numbers
         * each row is an array of 3 values:
         * [0] number of points in a bin,
         * [1] minimum of a bin
         * [2] maximum of a bin
         */
        validateData(data) {
            if (!data) { return false; }
            let valid = true;
            try {
                data.sort(function(row1, row2){
                    // [1] is minimum bin edge
                    return row1[1]-row2[1];
                });
                if (data) {
                    for (var i = 0; i < data.length; i++) {
                        if (data[i].length !== 3) {
                            console.error(`Invalid histogram data in row ${i} [${data[i]}]`);
                            valid = false;
                        } else if (data[i][1]>data[i][2]) {
                            console.error(`Histogram data row ${i}: minimum is more than maximum. [${data[i]}]`);
                            valid=false;
                        } else if (data[i+1] && Math.abs(data[i][2]-data[i+1][1])>Number.EPSILON &&
                                data[i][2]>data[i+1][1]) {
                            console.error(`Histogram data row ${i}: bin range overlaps the following row. [${data[i]}]`);
                            valid=false;
                        }
                    }
                    if (this.props.logs && this.props.logs.indexOf('x')>-1 && data[0][1]<=0) {
                        console.error('Unable to plot histogram: zero or subzero values on logarithmic scale');
                        valid = false;
                    }
                }
            } catch (e) {
                console.error(`Invalid data passed to Histogram: ${e}`);
                valid = false;
            }
            return valid;
        },


        /*
         * @param config
         * @return {Boolean} f data points are set, false if no points are present
         */
        setChartConfig(config) {
            if (!this.state.userData || this.state.userData.length < 1) {
                return false;
            }

            if (!this.validateData(this.state.userData)) {
                console.error('Invalid histogram data, check console for specifics.');
                return false;
            }

            var points = [], zones=[];
            var lighterColor = this.shadeColor(this.props.binColor, 0.1);
            var error;

            // zones mess up log scale - do not do them
            var doZones = (!this.props.logs || this.props.logs.indexOf('x')===-1);

            try {
                let lastBinMax = this.state.userData[0][1];
                // point before the first one
                points.push({
                    name: '',
                    range: '',
                    x: lastBinMax-Number.EPSILON,
                    y: 0
                });
                this.state.userData.forEach(function (value, index) {
                        const xrange = this.state.userData[index][2] - this.state.userData[index][1];
                        const formatStr = getFormatString(xrange,2);
                        const centerStr = numeral(this.state.userData[index][1]+xrange/2.0).format(formatStr);
                        const rangeStr = `${numeral(this.state.userData[index][1]).format(formatStr)} to ${numeral(this.state.userData[index][2]).format(formatStr)}`;

                        // check for gaps and add points in necessary
                        if (Math.abs(this.state.userData[index][1])-lastBinMax > Number.EPSILON &&
                            this.state.userData[index][1]>lastBinMax) {
                            console.warn(`Gap in histogram data before row ${index} [${this.state.userData[index]}]`);
                            const gapRange = this.state.userData[index][1]-lastBinMax;
                            const gapCenterStr = numeral(lastBinMax+gapRange/2.0).format(formatStr);
                            const gapRangeStr = `${numeral(lastBinMax).format(formatStr)} to ${numeral(this.state.userData[index][1]).format(formatStr)}`;

                            points.push({
                                name: gapCenterStr,
                                range: gapRangeStr,
                                x: lastBinMax+Number.EPSILON,
                                y: 0
                            });
                            points.push({
                                name: gapCenterStr,
                                range: gapRangeStr,
                                x: this.state.userData[index][1]-Number.EPSILON,
                                y: 0
                            });
                        }
                        lastBinMax = this.state.userData[index][2];

                        // a point for the bin's left edge (minimum)
                        points.push({
                            // name - formatted bin center
                            name: centerStr,
                            range: rangeStr,
                            // x - bin min
                            x: this.state.userData[index][1],
                            // y - number of points in the bin
                            y: this.state.userData[index][0]
                        });
                        // a point for the bin's max edge (maximum)
                        points.push({
                            // name - formatted bin center
                            name: centerStr,
                            range: rangeStr,
                            // x - binmax
                            x: this.state.userData[index][2]-Number.EPSILON,
                            // y - number of points in the bin
                            y: this.state.userData[index][0]
                        });

                        // zones allow to separate visually one bin from another
                        if (doZones) {
                            zones.push({
                                value: this.state.userData[index][2],
                                color: (index % 2 === 0) ? this.props.binColor : lighterColor
                            });
                        }
                    }.bind(this)
                );
                // point after the last one
                points.push({
                    name: '',
                    range: '',
                    x: lastBinMax+Number.EPSILON,
                    y: 0
                });

            }
            catch(e) {
                error = e;
            }
            if (error) {
                return false;
            } else {
                config.series[0].data = points;
                if (doZones) {
                    config.plotOptions.area.zones = zones;
                }
            }
        },

        render() {
            const yReversed = (this.props.reversed && this.props.reversed.indexOf('y')>-1 ? true : false);

            var config = {
                chart: {
                    renderTo: 'container',
                    type: 'area',
                    alignTicks: false,
                    height: Number(this.props.height)
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
                        return (this.point.name ? `<b>Bin center:</b> ${this.point.name}<br>` : '')+
                            (this.point.range ? `<b>Range:</b> ${this.point.range}<br>` : '')+
                            `<b>Count:</b> ${this.y}`;
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
                        zoneAxis: 'x',
                        zones: [] // color ajacent bins slightly different by defining zones
                    }
                },
                xAxis: {
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
                    color: this.props.binColor,
                    data: []
                }],
                credits: {
                    enabled: false // removes a reference to Highcharts.com from the chart
                }
            };

            this.setChartConfig(config);

            return (
                <div>
                    <Highcharts config={config}/>
                </div>
            );
        }
    });

export default Histogram;
