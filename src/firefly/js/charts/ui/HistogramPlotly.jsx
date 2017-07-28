/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';
import {get} from 'lodash';
import {logError} from '../../util/WebUtil.js';
import {PlotlyWrapper} from './PlotlyWrapper.jsx';
import {toMaxFixed} from '../../util/MathUtil.js';
import Color from '../../util/Color.js';

const HIST_DEC = 6;
const A = 0.85;
const FSIZE = 12;
const LOG = 'log';
const LINEAR = 'linear';
const REV = 'reversed';
const OPP = 'opposite';

export class HistogramPlotly extends Component {

    /**
     * @summary React Component to display histogram using Plotly
     *
     * @param {Object} props
     * @param {Array.number[]} props.data - array of numbers [0] - nInBin, [1] - binMin, [2] - binMax
     * @param {number} props.width - width of the chart in pixels
     * @param {number} props.height - height of the chart in pixels
     * @param {string} [props.logs] - can have values 'x', 'y', or 'xy'
     * @param {Object} [props.xAxis] - xAxis properties ('opposite' and 'reversed')
     * @param {Object} [props.yAxis] - yAxis properties ('opposite' and 'reversed')
     * @param {string} [props.binColor='#d1d1d1'] - darker bin color
     * @param {string} props.desc - description
     * @public
     * @memberof firefly.ui
     */
    constructor(props) {
        super(props);
        this.validateData = this.validateData.bind(this);
/*
        props.data.forEach((bin, i)=> {
            bin[0] = bin[0] * Math.pow(10, (i*2-2)) ;
        });
*/
        const yOpposite = get(this.props, ['yAxis', OPP], false);
        this.afterRedraw = this.afterRedraw.bind(this);
        this.notTitleSideMargin = 30;
        this.titleSideMargin = 60;
        this.state = {dataUpdate: null, layoutUpdate: null,
                      leftMargin: yOpposite ? this.notTitleSideMargin :  this.titleSideMargin,
                      rightMargin: yOpposite ? this.titleSideMargin :  this.notTitleSideMargin,
                      config: {displayModeBar: false}};
    }


    createXY(data, binColor) {
        var  emptyData = {
            x: [], y: [], text:[], borderColor: '',  binWidth: [], color: [], colorScale: []
        };

        if (!this.validateData(data)) {
            emptyData.valid = false;
            return emptyData;
        } else {
            emptyData.valid = true;
        }

        const getRGBAStr = (rgbStr, a) => {
            return Color.toRGBAString(Color.toRGBA(rgbStr.slice(1), a));
        };

        const lightColor = getRGBAStr(Color.shadeColor(binColor, 0.2), A);
        emptyData.borderColor = binColor && binColor === '#000000' ? Color.shadeColor(binColor, 0.5) : 'black';
        emptyData.colorScale = [[0, getRGBAStr(binColor, A)], [1, lightColor]];

        // compute bin width for the bin has the same xMin & xMan
        var startW = data[data.length-1][2] - data[0][1];
        startW = (startW === 0.0 ? 1.0 : startW) * 4/(this.props.width ? this.props.width : 200);

        var   minWidth = data.find((row) => {return row[1]===row[2];}) ?
                            data.reduce((prev, d) => {
                                if (d[1] !== d[2]) {
                                    const dist = (d[2] - d[1])/2;

                                    if (dist < prev) {
                                        prev = dist;
                                    }
                                }
                                return prev;
                            }, startW) : 1.0;



        var lastX = data[0][1]-1.0;
        var prevColor = 0;

        var addBin = (xySeries, x1, x2, y) => {
            const xVal = (x1 + x2) / 2;

            xySeries.x.push(xVal);
            xySeries.y.push(y);
            xySeries.binWidth.push(x1 === x2 ? minWidth: x2-x1);

            prevColor = (x1 <= lastX) ? (prevColor+1)%2 : 0;  // when two bars are next to each other, color is changed
            xySeries.color.push(prevColor);

            xySeries.text.push(
                `<span> ${x1 !== x2 ? '<b>Bin center: </b>' + toMaxFixed(xVal, HIST_DEC) + '<br>' : ''}` +
                `${x1 !== x2 ? '<b>Range: </b>' + toMaxFixed(x1, HIST_DEC) + ' to ' + toMaxFixed(x2, HIST_DEC) + '<br>' : ''}` +
                `<b>Count:</b> ${y}</span>`);

            lastX = x2;
        };

        return data.reduce((xy, oneData) => {
            addBin(xy, oneData[1], oneData[2], oneData[0]);
            return xy;
        }, emptyData);
    }

    regenData(props) {
        var {data, series, xAxis, yAxis, logs, desc, binColor, xUnit, width, height} = props;
        if (!data) {
            data = series && series.data;
        }

        var {x, y, binWidth, color, text, colorScale, borderColor} = this.createXY(data, binColor);
        var {leftMargin, rightMargin} = this.state;
        const plotlyData = [{
                displaylogo: false,
                type: 'bar',
                x,
                y,
                text,
                hoverinfo: 'text+x',
                width: binWidth,
                marker: {
                    color,
                    colorscale: colorScale,
                    line: {width: 1, color: borderColor}
                }
            }];

        const isOneBar = get(plotlyData, [0, 'x', 'length']) === 1;

        return {
            plotlyDivStyle: {
                border: '#a5a5a5',
                borderWidth: 1,
                width: '100%',
                height: '100%'
            },
            plotlyData,
            plotlyLayout: {
                height,
                width,
                hovermode: 'x',
                 xaxis: {
                    title: `${desc} ` + (xUnit ? `(${xUnit})` : ''),
                    type: (logs && logs.includes('x') ? LOG : LINEAR),
                    tickcolor: '#ccc',
                    zeroline: false,
                    showline: true,
                    linecolor: '#999',
                    tickmode: isOneBar ? 'array' : 'auto',
                    tickvals: isOneBar ? [get(plotlyData, [0, 'x', 0])] : 0.0,
                    autorange: get(xAxis, REV) ? REV : true,
                    side: get(xAxis, OPP) ? 'top' : 'bottom',
                    titlefont: {
                        size: FSIZE
                    },
                     exponentformat:'e'
                 },
                yaxis: {
                    title: 'Number',
                    gridLineWidth: 1,
                    type: (logs && logs.includes('y') ? LOG : LINEAR),
                    tickcolor: '#ccc',
                    zeroline: false,
                    autorange: get(yAxis, REV) ? REV : true,
                    side: get(yAxis, OPP) ? 'right' : 'left',
                    titlefont: {
                        size: FSIZE
                    },
                    exponentformat:'e'
                },
                margin: {
                    l: leftMargin,
                    r: rightMargin,
                    b: get(xAxis, OPP) ? this.notTitleSideMargin : this.titleSideMargin,
                    t: get(xAxis, OPP) ? this.titleSideMargin : this.notTitleSideMargin
                }
            }
        };
    }

    shouldComponentUpdate(nextProps, ns) {
        const bUpdate = !shallowequal(this.props,nextProps) || !shallowequal(this.state,ns);

        if (bUpdate && nextProps !== this.props) {
            this.chartingInfo = this.regenData(nextProps);
        }
        return bUpdate;
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
                    } else if (data[i][0] < 0) {
                        logError(`Histogram data row ${i} count is less than zero, ${data[i][0]}`);
                        valid=false;
                    }
                }
                if (this.props.logs && this.props.logs.includes('x') && data[0][1]<=0) {
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

    componentWillMount() {
        this.chartingInfo = this.regenData(this.props);
    }


    afterRedraw(chart, pl) {

        // get the element in the plotly chart containing the y tick text
        const ytickTexts = chart.querySelectorAll('g.ytick text');
        var maxYTickLen = this.notTitleSideMargin;
        for (let i = 0; i < ytickTexts.length; i++) {
            if (ytickTexts[i].clientWidth > maxYTickLen) {
                maxYTickLen = ytickTexts[i].clientWidth;
            }
        }

        //update the margin on the y axis title side based on the tick text width
        maxYTickLen = Math.max(maxYTickLen - this.notTitleSideMargin, 0) + this.titleSideMargin;
        const opp = get(this.props.yAxis, OPP);
        const leftMargin = opp ? this.notTitleSideMargin : maxYTickLen;
        const rightMargin = opp ? maxYTickLen : this.notTitleSideMargin;

        if (leftMargin !== this.state.leftMargin || rightMargin !== this.state.rightMargin) {
            var margin = get(this.chartingInfo, ['plotlyLayout', 'margin']);

            if (margin) {
                margin = Object.assign(margin, {l: leftMargin, r: rightMargin});
                this.setState({leftMargin, rightMargin, layoutUpdate: {margin}});
            }
        }
    }

    render() {
        this.error = undefined;
        const {dataUpdate, layoutUpdate, config} = this.state;
        const {plotlyData, plotlyDivStyle, plotlyLayout}= this.chartingInfo;

        return (
            <PlotlyWrapper chartId={this.props.chartId} data={plotlyData} layout={plotlyLayout}  style={plotlyDivStyle}
                           dataUpdate={dataUpdate}
                           layoutUpdate={layoutUpdate}
                           divUpdateCB={(div) => this.chart= div}
                           config={config}
                           autoDetectResizing={true}
                           newPlotCB={this.afterRedraw}
            />
        );
    }
}

HistogramPlotly.defaultProps = {
    desc: 'Sample Distribution',
    binColor: '#d1d1d1'
};


HistogramPlotly.propTypes = {
    chartId: PropTypes.string,
    series: PropTypes.arrayOf(PropTypes.object), // array of objects with data, binColor, and name properties
    xAxis: PropTypes.object,
    yAxis: PropTypes.object,
    xUnit: PropTypes.string,
    width: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    height: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    logs: PropTypes.oneOf(['x','y','xy']),
    desc: PropTypes.string,
    data: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)), // array of numbers [0] - nInBin, [1] - binMin, [2] - binMax
    binColor(props, propName, componentName) {
        if (props[propName] && !/^#[0-9a-f]{6}/.test(props[propName])) {
            return new Error(`Invalid bin color in ${componentName}, should be hex with exactly 7 characters long.`);
        }
    }
};
