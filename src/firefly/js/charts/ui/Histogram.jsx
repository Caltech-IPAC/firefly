/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import ReactHighcharts from 'react-highcharts';
import numeral from 'numeral';
import {getFormatString} from '../../util/MathUtil.js';
import {logError} from '../../util/WebUtil.js';

/*
 * @param {String} color - hex color, exactly seven characters log, starting with '#'
 * @param {Number} persentage (0.1 means 10 percent lighter, -0.1 - 10 percent darker)
 * @return {String} lighter or darker shade of the given hex color
 * from http://stackoverflow.com/questions/5560248/programmatically-lighten-or-darken-a-hex-color-or-rgb-and-blend-colors
 */
function shadeColor(color, percent) {
    var f=parseInt(color.slice(1),16),t=percent<0?0:255,p=percent<0?percent*-1:percent,R=f>>16,G=f>>8&0x00FF,B=f&0x0000FF;
    return `#${(0x1000000+(Math.round((t-R)*p)+R)*0x10000+(Math.round((t-G)*p)+G)*0x100+(Math.round((t-B)*p)+B)).toString(16).slice(1)}`;
}


export class Histogram extends React.Component {

    constructor(props) {
        super(props);
        this.setChartConfig = this.setChartConfig.bind(this);
        this.validateData = this.validateData.bind(this);
    }

    shouldComponentUpdate(nextProps) {
        const {data, width, height, logs, reversed, desc, binColor} = this.props;
        // should rerender only if data or bin color has changed
        // otherwise just change the existing chart
        if (data != nextProps.data || binColor !== nextProps.binColor) { return true; }
        const chart = this.refs.chart && this.refs.chart.getChart();
        if (chart) {
            let doUpdate = false;
            if (height !== nextProps.height || width !== nextProps.width ) {
                chart.setSize(nextProps.width, nextProps.height, false);
                return false;
            }

            if (desc !== nextProps.desc) {
                chart.xAxis[0].setTitle(nextProps.desc, false);
                doUpdate = true;
            }
            const nreversed = nextProps.reversed;
            if (reversed !== nreversed){
                const yReversed = Boolean(nreversed && nreversed.indexOf('y')>-1);
                const xReversed = Boolean(nreversed && nreversed.indexOf('x')>-1);
                chart.xAxis[0].update({reversed : xReversed, opposite: yReversed}, false);
                chart.yAxis[0].update({reversed : yReversed}, false);
                doUpdate = true;
            }
            const nlogs = nextProps.logs;
            if (logs !== nextProps.logs){
                const xtype = nlogs && nlogs.indexOf('x')>-1 ? 'logarithmic' : 'linear';
                const ytype = nlogs && nlogs.indexOf('y')>-1 ? 'logarithmic' : 'linear';
                chart.xAxis[0].update({type : xtype}, false);
                chart.yAxis[0].update({type : ytype}, false);
                doUpdate = true;
            }
            if (doUpdate) { chart.redraw(false); }
            return false;
        }
    }

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
                        logError(`Invalid histogram data in row ${i} [${data[i]}]`);
                        valid = false;
                    } else if (data[i][1]>data[i][2]) {
                        logError(`Histogram data row ${i}: minimum is more than maximum. [${data[i]}]`);
                        valid=false;
                    } else if (data[i+1] && Math.abs(data[i][2]-data[i+1][1])>1000*Number.EPSILON &&
                        data[i][2]>data[i+1][1]) {
                        logError(`Histogram data row ${i}: bin range overlaps the following row. [${data[i]}]`);
                        valid=false;
                    }
                }
                if (this.props.logs && this.props.logs.indexOf('x')>-1 && data[0][1]<=0) {
                    logError('Unable to plot histogram: zero or subzero values on logarithmic scale');
                    valid = false;
                }
            }
        } catch (e) {
            logError(`Invalid data passed to Histogram: ${e}`);
            valid = false;
        }
        return valid;
    }

    /*
     * @param config
     * @return {Boolean} f data points are set, false if no points are present
     */
    setChartConfig(config) {
        const TINY_OFFSET = 100*Number.EPSILON;

        const {binColor, data}= this.props;

        if (!data || data.length < 1) {
            return false;
        }

        if (!this.validateData(data)) {
            logError('Invalid histogram data, check console for specifics.');
            return false;
        }

        var points = [], zones=[];
        var lighterColor = shadeColor(binColor, 0.1);
        var error;

        // use column chart for only one point
        var areaPlot = (data.length > 1);

        // zones mess up log scale - do not do them
        var doZones = (areaPlot); // && (!logs || logs.indexOf('x')===-1));

        if (!areaPlot && data.length === 1) {
            const xrange = data[0][2] - data[0][1];
            if (xrange <= TINY_OFFSET) {
                config.plotOptions.column.maxPointWidth = 10;
            } else {
                config.plotOptions.column.maxPointWidth = 50;
            }
        }

        try {
            let lastBinMax = data[0][1];
            if (areaPlot) {
                // point before the first one
                points.push({
                    name: '',
                    range: '',
                    x: lastBinMax - 2*TINY_OFFSET,
                    y: 0
                });
            }
            data.forEach(function (value, index) {
                    const xrange = data[index][2] - data[index][1];
                    const formatStr = getFormatString(xrange, 2);
                    const centerStr = numeral(data[index][1] + xrange / 2.0).format(formatStr);
                    const rangeStr = `${numeral(data[index][1]).format(formatStr)} to ${numeral(data[index][2]).format(formatStr)}`;

                    // check for gaps and add points in necessary
                    if (Math.abs(data[index][1]) - lastBinMax > TINY_OFFSET &&
                        data[index][1] > lastBinMax) {
                        //console.warn(`Gap in histogram data before row ${index} [${data[index]}]`);
                        const gapRange = data[index][1] - lastBinMax;
                        const gapCenterStr = numeral(lastBinMax + gapRange / 2.0).format(formatStr);
                        const gapRangeStr = `${numeral(lastBinMax).format(formatStr)} to ${numeral(data[index][1]).format(formatStr)}`;

                        points.push({
                            name: gapCenterStr,
                            range: gapRangeStr,
                            x: lastBinMax + TINY_OFFSET,
                            y: 0
                        });
                        points.push({
                            name: gapCenterStr,
                            range: gapRangeStr,
                            x: data[index][1] - TINY_OFFSET,
                            y: 0
                        });
                    }
                    lastBinMax = data[index][2];

                    if (areaPlot) {
                        // a point for the bin's left edge (minimum)
                        points.push({
                            // name - formatted bin center
                            name: centerStr,
                            range: rangeStr,
                            // x - bin min
                            x: data[index][1],
                            // y - number of points in the bin
                            y: data[index][0]
                        });


                        // a point for the bin's right edge (maximum)
                        points.push({
                            // name - formatted bin center
                            name: centerStr,
                            range: rangeStr,
                            // x - binmax
                            x: (xrange > TINY_OFFSET) ? (data[index][2] - TINY_OFFSET) : (data[index][1] + TINY_OFFSET),
                            // y - number of points in the bin
                            y: data[index][0]
                        });
                    } else { // column plot - one point
                        points.push({
                            // name - formatted bin center
                            name: centerStr,
                            range: rangeStr,
                            // x - bin min
                            x: (xrange > TINY_OFFSET) ? data[index][1]+xrange/2.0 : data[index][1],
                            // y - number of points in the bin
                            y: data[index][0]
                        });
                    }

                    // zones allow to separate visually one bin from another
                    if (doZones) {
                        zones.push({
                            value: data[index][2],
                            color: (index % 2 === 0) ? binColor : lighterColor
                        });
                    }

                }.bind(this)
            );
            if (areaPlot) {
                // point after the last one
                points.push({
                    name: '',
                    range: '',
                    x: lastBinMax + 2*TINY_OFFSET,
                    y: 0
                });
            }
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
    }

    render() {

        const { binColor, data, desc, width, height, logs, reversed }= this.props;
        const yReversed = (reversed && reversed.indexOf('y')>-1 ? true : false);

        var chartType;
        if (data.length < 2) {
            chartType = 'column';
        } else {
            chartType = 'area';
        }


        var config = {
            chart: {
                renderTo: 'container',
                type: chartType,
                alignTicks: false,
                width: Number(width),
                height: Number(height),
                borderColor: '#a5a5a5',
                borderWidth: 1
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
                    return '<span>'+(this.point.name ? `<b>Bin center:</b> ${this.point.name}<br/>` : '')+
                        (this.point.range ? `<b>Range:</b> ${this.point.range}<br/>` : '')+
                        `<b>Count:</b> ${this.y}</span>`;
                }
            },
            plotOptions: {
                series: {
                    threshold: 0,
                    animation: false
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
                },
                column: {
                    pointPadding: 0.2
                }
            },
            xAxis: {
                lineColor: '#999',
                tickColor: '#ccc',
                title: {
                    text: desc
                },
                opposite: yReversed,
                reversed: (reversed && reversed.indexOf('x')>-1 ? true : false),
                type: (logs && logs.indexOf('x')>-1 ? 'logarithmic' : 'linear')
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
                type: (logs && logs.indexOf('y')>-1 ? 'logarithmic' : 'linear')
            },
            series: [{
                name: 'data points',
                turboThreshold: 0,
                color: binColor,
                data: []
            }],
            credits: {
                enabled: false // removes a reference to Highcharts.com from the chart
            }
        };

        if (data.length > 0) {
            this.setChartConfig(config);
        }

        return (
            <div>
                <ReactHighcharts config={config} isPureConfig={true} ref='chart'/>
            </div>
        );
    }
}

Histogram.defaultProps = {
    desc: 'Sample Distribution',
    binColor: '#d1d1d1'
};

Histogram.propTypes = {
    data: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)), // array of numbers [0] - nInBin, [1] - binMin, [2] - binMax
    width: PropTypes.number,
    height: PropTypes.number,
    logs: PropTypes.oneOf(['x','y','xy']),
    reversed: PropTypes.oneOf(['x','y','xy']),
    desc: PropTypes.string,
    binColor(props, propName, componentName) {
        if (props[propName] && !/^#[0-9a-f]{6}/.test(props[propName])) {
            return new Error(`Invalid bin color in ${componentName}, should be hex with exactly 7 characters long.`);
        }
    }
};
