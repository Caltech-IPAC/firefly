/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import ReactHighcharts from 'react-highcharts';
import numeral from 'numeral';
import {get, set} from 'lodash';
import {getFormatString} from '../../util/MathUtil.js';
import {logError} from '../../util/WebUtil.js';

/*
 * @param {String} color - hex color, exactly seven characters log, starting with '#'
 * @param {Number} percentage (0.1 means 10 percent lighter, -0.1 - 10 percent darker)
 * @return {String} lighter or darker shade of the given hex color
 * from http://stackoverflow.com/questions/5560248/programmatically-lighten-or-darken-a-hex-color-or-rgb-and-blend-colors
 */
function shadeColor(color, percent) {
    var f=parseInt(color.slice(1),16),t=percent<0?0:255,p=percent<0?percent*-1:percent,R=f>>16,G=f>>8&0x00FF,B=f&0x0000FF;
    return `#${(0x1000000+(Math.round((t-R)*p)+R)*0x10000+(Math.round((t-G)*p)+G)*0x100+(Math.round((t-B)*p)+B)).toString(16).slice(1)}`;
}

function padRight(num) {
    return num+Math.abs(num*Math.pow(10,-14));
}

function padLeft(num, pad=0) {
    const thePad = pad ? Math.abs(pad) : Math.abs(num*Math.pow(10,-14));
    return num-thePad;
}

function getMinY(data) {
    if (data.length > 0) {
        return data.reduce((minVal, row) => {
            return Math.min(minVal, row[0]);
        }, data[0][0]);
    }
}

function allBinsZeroWidth(data) {
    return data.every((row) => {return row[1]===row[2];});
}

export class Histogram extends React.Component {

    /**
     * @summary React Component to display histogram.
     *
     * @param {Object} props
     * @param {Array.number[]} props.data - array of numbers [0] - nInBin, [1] - binMin, [2] - binMax
     * @param {number} props.width - width of the chart in pixels
     * @param {number} props.height - height of the chart in pixels
     * @param {string} [props.logs] - can have values 'x', 'y', or 'xy'
     * @param {string} [props.binColor='#d1d1d1'] - darker bin color
     * @param {string} props.desc - description
     * @public
     * @memberof firefly.ui
     */
    constructor(props) {
        super(props);
        this.addDataSeries = this.addDataSeries.bind(this);
        this.validateData = this.validateData.bind(this);
    }

    shouldComponentUpdate(nextProps) {
        const {series, data, width, height, logs, reversed, desc, binColor} = this.props;
        // should rerender only if data or bin color has changed
        // otherwise just change the existing chart
        if (series !== nextProps.series || data !== nextProps.data || binColor !== nextProps.binColor) { return true; }
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
     * @param {Object} seriesOptions
     * @param {Array.number[]} seriesOptions.data
     * @param {string} seriesOptions.binColor
     * @param {string} seriesOptions.name
     * @param {number} minY - minimum plot Y
     * @param {number} seriesIdx - index of the series
     * @return {Boolean} f data points are set, false if no points are present
     */
    addDataSeries(config, seriesOptions, minY, nSeries, seriesIdx=0) {

        // how many significant digits should we preserve? ~12?
        // EPSILON 2^(-52)
        const TINY_OFFSET = 100*Number.EPSILON;

        const {name, binColor, data}= seriesOptions;

        if (!data || data.length < 1) {
            return false;
        }

        if (!this.validateData(data)) {
            logError('Invalid histogram data, check console for specifics.');
            return false;
        }

        const points = [], zones=[];
        const lighterColor = shadeColor(binColor, 0.1);
        var error;

        // use column chart for only one point
        var areaPlot = !allBinsZeroWidth(data);

        // zones - color ajacent bins slightly differently
        var doZones = (areaPlot);


        if (!areaPlot) {
            config.plotOptions.column.maxPointWidth = 10;
            // walkaround for Highcharts bug when a single column does not appear in the middle (for n> 10^14)
            if (data.length === 1 && nSeries === 1) {
                // make one column appear in the middle
                config.xAxis.categories = [Number(data[0][1]).toString()];
                config.yAxis.min = minY;
                set(config, ['series',seriesIdx],
                    {
                        type: 'column',
                        color: lighterColor,
                        fillOpacity: 0.8,
                        data: [data[0][0]]
                    });
                return;
            }
        }

        try {

            let lastBinMax = data[0][1];
            if (areaPlot) {

                // point before the first one
                points.push({
                    name: '',
                    range: '',
                    x: padLeft(lastBinMax),
                    y: minY
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
                            x: padRight(lastBinMax),
                            y: minY
                        });
                        points.push({
                            name: gapCenterStr,
                            range: gapRangeStr,
                            x: padLeft(data[index][1]),
                            y: minY
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
                            x: (xrange > TINY_OFFSET) ? padLeft(data[index][2], xrange/100) : padRight(data[index][1]),
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
                    x: padRight(lastBinMax),
                    y: minY
                });

                // zones allow to separate visually one bin from another
                if (doZones) {
                    zones.push({
                        value: padRight(lastBinMax),
                        color: (data.length % 2 === 0) ? binColor : lighterColor
                    });
                }

            }
            config.yAxis.min = minY;
        }
        catch(e) {
            error = e;
        }
        if (error) {
            return false;
        } else {
            const dataSeries = {
                name,
                type: areaPlot ? 'area' : 'column',
                turboThreshold: 0,
                fillOpacity: 0.8,
                color: binColor,
                data: points
            };
            if (doZones) {
                dataSeries.zones = zones;
            }
            set(config, ['series',seriesIdx], dataSeries);
        }
    }

    render() {

        const { binColor, data, desc, width, height, logs, reversed}= this.props;
        let series = this.props.series;
        if (!series) {
            series = [{data, binColor, name: 'data points'}];
        }
        const yReversed = (reversed && reversed.indexOf('y')>-1 ? true : false);


        let minY = 0;
        // what should be the minimum y value be?
        if (logs && logs.includes('y')) {
            let minYData = Number.MAX_VALUE;
            for (let i=0; i< series.length; i++) {
                const data = series.data;
                if (data && data.length>0) {
                    const seriesMinY = getMinY(data);
                    if (seriesMinY < minYData) { minYData = seriesMinY; }
                }
            }
            minY = (minYData === Number.MAX_VALUE) ? 0.1 : minYData/10;
        }

        var config = {
            chart: {
                renderTo: 'container',
                alignTicks: false,
                width: width? Number(width) : undefined,
                height: height? Number(height) : undefined,
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
                //shared: true works, split: true does not work with the current formatter in Highcharts 5
                followPointer: true,
                borderWidth: 1,
                formatter() {
                    if (this.y === minY) {return false;} // don't display tooltip
                    let name, range;
                    // either a point or an array of points are passed depending if the tooltip is shared
                    if (this.point) {
                        name = this.point.name;
                        range = this.point.range;
                    } else {
                        const point = get(this, 'points[0].point');
                        if (!point) {return false;}
                        name = point.name;
                        range = point.range;
                    }
                    return '<span>' + (name ? `<b>Bin center:</b> ${name}<br/>` : '') +
                        (range ? `<b>Range:</b> ${range}<br/>` : '') +
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
                    zones: [] // color adjacent bins slightly different by defining zones
                },
                column: {
                    pointPadding: 0.2
                }
            },
            series: [],
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
                min: minY,
                endOnTick: false,
                title: {
                    text: ''
                },
                reversed: yReversed,
                type: (logs && logs.indexOf('y')>-1 ? 'logarithmic' : 'linear')
            },
            credits: {
                enabled: false // removes a reference to Highcharts.com from the chart
            }
        };

        const nSeries = series.length;
        series.forEach((s, idx) => {
            this.addDataSeries(config, s, minY, nSeries, idx);
        });

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


// when more than one histogram is defined use series
Histogram.propTypes = {
    series: PropTypes.arrayOf(PropTypes.object), // array of objects with data, binColor, and name properties
    width: PropTypes.number,
    height: PropTypes.number,
    logs: PropTypes.oneOf(['x','y','xy']),
    reversed: PropTypes.oneOf(['x','y','xy']),
    desc: PropTypes.string,
    data: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)), // array of numbers [0] - nInBin, [1] - binMin, [2] - binMax
    binColor(props, propName, componentName) {
        if (props[propName] && !/^#[0-9a-f]{6}/.test(props[propName])) {
            return new Error(`Invalid bin color in ${componentName}, should be hex with exactly 7 characters long.`);
        }
    }
};
