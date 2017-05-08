/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, omit} from 'lodash';
import shallowequal from 'shallowequal';
import React, {PropTypes} from 'react';
import {isPlotly} from '../ChartUtil.js';
import {XYPlotHighcharts} from './XYPlotHighcharts.jsx';
import {XYPlotPlotly} from './XYPlotPlotly.jsx';
import {plotParamsShape, plotDataShape} from './XYPlotPropTypes.js';


export const isLinePlot = function(plotStyle) {
    return plotStyle === 'line' || plotStyle === 'linepoints';
};

/*
 @param {number} weight for a given point
 @param {number} minWeight minimum weight for all points
 @param {number} maxWeight maximum weight for all points
 @param {boolean} logShading - if true use log color scale
 @param {boolean} returnNum - if true return group number rather than group description
 @return {number|string} from 1 to 6, 1 for 1 pt series
 */
export const getWeightBasedGroup = function(weight, minWeight, maxWeight, logShading=false, returnNum=true) {
    if (weight === 1) return returnNum ? 1 : '1pt';
    else {
        if (logShading) {
            //use log scale for shade assignment
            let min=2, max;
            const base = Math.pow(maxWeight+0.5, 0.2);
            for (var e = 1; e <=5; e++) {
                max = Math.round(Math.pow(base, e));
                if (weight <= max) {
                    if (max > maxWeight) { max = maxWeight; }
                    return returnNum ? e+1 : (min===max ? min : (min+'-'+max))+'pts';
                }
                min = max+1;
            }
        } else {
            // use linear scale for order assignment
            const range =  maxWeight-minWeight-1;
            let n=2;
            let min=2, max;
            // 5 groups incr=0.20
            for (let incr = 0.20; incr <=1; incr += 0.20) {
                max = Math.round(minWeight+1+incr*range);
                if (weight <= max) {
                    return returnNum ? n : (min===max ? min : (min+'-'+max))+'pts';
                }
                min = max+1;
                n++;
            }
        }
    }
    // should not reach
};

export const getWeightedDataDescr = function(defaultDescr, numericData, minWeight, maxWeight, logShading) {
    if (numericData.length < 1) { return defaultDescr; }
    return getWeightBasedGroup(numericData[0].weight, minWeight, maxWeight, logShading, false);
};

export const isDataSeries = function(name) {
    return (name === '1pt' || name.endsWith('pts'));
};

export const getXAxisOptions = function(params) {
    const xTitle = params.x.label + (params.x.unit ? ` (${params.x.unit})` : '');
    let xGrid = false, xReversed = false, xOpposite = false, xLog = false;
    const {options:xOptions} = params.x;
    if (xOptions) {
        xGrid = xOptions.includes('grid');
        xReversed = xOptions.includes('flip');
        xOpposite = xOptions.includes('opposite');
        xLog = xOptions.includes('log');
    }
    return {xTitle, xGrid, xReversed, xOpposite, xLog};
};

export const validate = function(params, data) {
    const errors = [];
    const {options:xOptions} = get(params, 'x');
    if (xOptions && xOptions.includes('log')) {
        const min = get(data,'xMin');
        if (Number.isFinite(min)) {
            if (min <= 0) {
                errors.push(`Logarithmic scale can not be used for minimum X value ${min}.`);
            }
        }
    }
    const {options:yOptions} = get(params, 'y');
    if (yOptions && yOptions.includes('log')) {
        const min = get(data,'yMin');
        if (Number.isFinite(min)) {
            if (min <= 0) {
                errors.push(`Logarithmic scale can not be used for minimum Y value ${min}.`);
            }
        }
    }
    return errors;
} ;

export const getYAxisOptions = function(params) {
    const yTitle = params.y.label + (params.y.unit ? ` (${params.y.unit})` : '');

    let yGrid = false, yReversed = false, yOpposite=false, yLog = false;
    const {options:yOptions} = params.y;
    if (params.y.options) {
        yGrid = yOptions.includes('grid');
        yReversed = yOptions.includes('flip');
        yOpposite = yOptions.includes('opposite');
        yLog = yOptions.includes('log');
    }
    return {yTitle, yGrid, yReversed, yOpposite, yLog};
};

export const plotErrors = function(params, axis) {
    return get(params, [axis, 'error']) || get(params, [axis, 'errorLow']) || get(params, [axis, 'errorHigh']);
};

export const getZoomSelection = function(params) {
    return (params.zoom ? params.zoom : {xMin:null, xMax: null, yMin:null, yMax:null});
};


const selFinite = (v1, v2) => {return Number.isFinite(v1) ? v1 : v2;};

export const selFiniteMin = (v1, v2) => {
    if (Number.isFinite(v1) && Number.isFinite(v2)) {
        return Math.min(v1, v2);
    } else {
        return selFinite(v1,v2);
    }
};

export const selFiniteMax = (v1, v2) => {
    if (Number.isFinite(v1) && Number.isFinite(v2)) {
        return Math.max(v1, v2);
    } else {
        return selFinite(v1,v2);
    }
};


export const calculateChartSize = function(widthPx, heightPx, props) {
    const {params} = props;
    let chartWidth = undefined, chartHeight = undefined;
    if (params.xyRatio) {
        if (params.stretch === 'fit') {
            chartHeight = Number(heightPx) - 2;
            chartWidth = Number(params.xyRatio) * Number(chartHeight) + 20;
            if (chartWidth > Number(widthPx)) {
                chartHeight -= 15; // to accommodate scroll bar
            }
        } else {
            chartWidth = Number(widthPx) - 15;
            chartHeight = Number(widthPx) / Number(params.xyRatio);
        }
    } else {
        chartWidth = Number(widthPx);
        chartHeight = Number(heightPx);
    }
    return {chartWidth, chartHeight};
};

export const formatError = function(val, err, errLow, errHigh) {
    if (Number.isFinite(err) || (Number.isFinite(errLow) && Number.isFinite(errHigh))) {
        const symmetricError = Number.isFinite(err);
        const lowErr = symmetricError ? err : errLow;
        const highErr = symmetricError ? err : errHigh;
        // we might want to use format for expressions in future - still hard to tell how many places to save
        //const fmtLow = getFormatString(lowErr, 4);
        if (symmetricError) {
            //return ' \u00B1 '+numeral(lowErr).format(fmtLow); //Unicode U+00B1 is plusmn
            return ' \u00B1 '+lowErr; //Unicode U+00B1 is plusmn
        } else {
            //const fmtHigh = getFormatString(highErr, 4);
            //return `\u002B${numeral(highErr).format(fmtHigh)} / \u2212${numeral(lowErr).format(fmtLow)}`;
            // asymmetric errors format: 8 +4/-2
            return `\u002B${highErr} / \u2212${lowErr}`;
        }
    } else {
        return '';
    }
};


export class XYPlot extends React.Component {

    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nextProps) {

        const propsToOmit = ['onHighlightChange', 'onSelection', 'highlighted'];

        // no update is needed if properties did ot change
        return (
            !(shallowequal(omit(this.props, propsToOmit), omit(nextProps, propsToOmit)) &&
            get(this.props, 'highlighted.rowIdx') === get(nextProps, 'highlighted.rowIdx')) );
    }

    render() {

        const {data, params} = this.props;

        // validate parameters for the given data
        const errors = validate(params, data);
        if (errors.length > 0) {
            return (
                <div style={{position: 'relative', width: '100%', height: '100%'}}>
                    {errors.map((error, i) => {
                        return (
                            <div key={i} style={{padding: 10, textAlign: 'center', overflowWrap: 'normal'}}>
                                <h3>{`${error}`}</h3>
                            </div>
                        );
                    })}
                </div>
            );
        } else {
            const XYPlotInstance=  isPlotly() ? XYPlotPlotly : XYPlotHighcharts;
            return (<XYPlotInstance {...this.props}/>);
        }
    }
}

XYPlot.propTypes = {
    chartId: PropTypes.string,
    data: plotDataShape,
    width: PropTypes.number,
    height: PropTypes.number,
    params: plotParamsShape,
    highlighted: PropTypes.shape({
        x: PropTypes.number,
        y: PropTypes.number,
        rowIdx: PropTypes.number
    }),
    selectInfo: PropTypes.shape({
        selectAll: PropTypes.bool,
        exceptions: PropTypes.instanceOf(Set),
        rowCount: PropTypes.number
    }),
    onHighlightChange: PropTypes.func,
    onSelection: PropTypes.func,
    desc: PropTypes.string
};

XYPlot.defaultProps = {
    data: undefined,
    params: undefined,
    highlighted: undefined,
    onHighlightChange: undefined,
    onSelection: undefined,
    height: 300,
    desc: ''
};

