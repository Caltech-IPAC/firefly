import {formatNumber} from '../../../util/MathUtil';
import {getExtName} from '../../FitsHeaderUtil.js';
import {
    getAllWaveLengthsForCube, getHDU, getPtWavelength, hasWCSProjection, hasWLInfo, primePlot
} from '../../PlotViewUtil.js';
import {getFluxUnits} from '../../WebPlot.js';

const plotlyDivStyle = {width: '100%', height: '100%'};


export const POINTS_CURVE_NUM=0;

function makePlotlyLayoutObj(title, xAxis, yAxis, reversed = false) {
    const font = {size: 12};
    return {
        hovermode: 'closest',
        title: {text: title, font},
        showlegend: false,
        xaxis: {
            title: {text: xAxis, font},
            type: 'linear',
            linecolor: '#e9e9e9',
            showline: true,
            zeroline: false,
            tickfont: font,
            exponentformat: 'e',
            autorange: reversed ? 'reversed' : true,
        },
        yaxis: {
            title: {text: yAxis, font},
            type: 'linear',
            linecolor: '#e9e9e9',
            showline: true,
            exponentformat: 'e'
        },
        margin: {l: 50, r: 50, b: 50, t: 30, pad: 2}
    };
}


function makePlotlyDataObj(xDataAry, yDataAry, x, y, ttXStr, ttYStr, makeXDesc = (i) => formatNumber(xDataAry[i],6),
                           highlightXDesc = formatNumber(x,6), makeYDesc = (i) => formatNumber(yDataAry[i],6)) {
    const dataRoot = {mode: 'markers', hoverinfo: 'text'};
    const result = [
        {
            ...dataRoot,
            type: xDataAry?.length > 6000 ? 'scattergl' : 'scatter',
            marker: {symbol: 'circle', size: 6, color: 'rgba(63, 127, 191, 0.5)'},
            x: xDataAry,
            y: yDataAry,
            hovertext: Array.from(yDataAry).map((d, i) => `<span> ${ttXStr}: ${makeXDesc(i)}  <br> ${ttYStr}: ${makeYDesc(i)}   </span>`),
        },
        {
            ...dataRoot,
            type: 'scatter',
            marker: {symbol: 'circle', size: 6, color: 'rgba(255, 200, 0, 1)'},
            x: [x],
            y: [y],
            hovertext: [`<span> ${ttXStr}: ${highlightXDesc}  <br> ${ttYStr}: ${formatNumber(y,6)}   </span>`],
        }
    ];
    return result;
}

/**
 * @typedef {Object} ExtractionChartParams
 * Everything needed to render an extraction preview chart with PlotlyWrapper.
 * @prop {Object} plotlyDivStyle - style for the chart's container div
 * @prop {Array.<Object>} plotlyData - two plotly traces: all the extracted points, then the highlighted point
 * @prop {Object} plotlyLayout - plotly layout
 */

/**
 * Make the chart for a line extraction: the value at each point along the line vs. the offset from the start.
 * The offset is in arcsec when the image has a WCS projection, otherwise in image pixels.
 * @param {WebPlot|undefined} plot
 * @param {Array.<number>} xDataAry - offset of each point along the line
 * @param {Float32Array} yDataAry - extracted value at each point
 * @param {number} x - offset of the highlighted point
 * @param {number} y - value of the highlighted point
 * @param {number} pointSize - width of the extraction box in pixels; when > 1 the y-axis title shows the box size and combineOp
 * @param {string} combineOp - how the values in the box are combined: 'AVG', 'SUM', or 'OR'
 * @param {string} title - chart title
 * @param {boolean} reversed - if true, reverse the x-axis
 * @return {ExtractionChartParams}
 */
export function genSliceChartData(plot, xDataAry, yDataAry, x, y, pointSize, combineOp, title, reversed) {
    const unitStr = hasWCSProjection(plot) ? ' (arcsec)' : '';
    const yUnits = getFluxUnits(plot);
    const yAxisLabel = getExtName(plot) || 'HDU# ' + getHDU(plot);
    return {
        plotlyDivStyle,
        plotlyData: makePlotlyDataObj(xDataAry, yDataAry, x, y, 'Offset' + unitStr, yAxisLabel),
        plotlyLayout: makePlotlyLayoutObj(title, 'Offset' + unitStr,
            `${yAxisLabel} (${yUnits})${pointSize > 1 ? ` (${pointSize}x${pointSize} ${combineOp.toLowerCase()})` : ''}`, reversed),
    };
}

/**
 * Make the chart for a point extraction: the value at each selected point vs. its image x or y.
 * @param {WebPlot} plot
 * @param {Array.<number>} dataAry - extracted value at each point
 * @param {Array.<ImagePt>} imPtAry - the selected points
 * @param {number} x - image x or y of the highlighted point, matching chartXAxis
 * @param {number} y - value of the highlighted point
 * @param {number} pointSize - width of the extraction box in pixels; when > 1 the y-axis title shows the box size and combineOp
 * @param {string} combineOp - how the values in the box are combined: 'AVG', 'SUM', or 'OR'
 * @param {string} title - chart title
 * @param {string} chartXAxis - 'imageX' to plot against image x, any other value to plot against image y
 * @param {number} activeIdx - index into imPtAry of the highlighted point
 * @return {ExtractionChartParams}
 */
export function genPointChartData(plot, dataAry, imPtAry, x, y, pointSize, combineOp, title, chartXAxis, activeIdx) {

    const xAxis = chartXAxis === 'imageX' ? imPtAry.map((pt) => pt.x) : imPtAry.map((pt) => pt.y);
    const xAxisTitle = chartXAxis === 'imageX' ? 'Image X' : 'Image Y';
    const xLabel = (i) => `(${imPtAry[i].x},${imPtAry[i].y})`;
    const yUnits = getFluxUnits(plot);
    const yAxisLabel = getExtName(plot) || 'HDU# ' + getHDU(plot);

    return {
        plotlyDivStyle,
        plotlyData: makePlotlyDataObj(xAxis, dataAry, x, y, xAxisTitle, yAxisLabel,
            xLabel, xLabel(activeIdx)),
        plotlyLayout: makePlotlyLayoutObj(title, xAxisTitle, `${yAxisLabel} (${yUnits})${pointSize > 1 ? ` (${pointSize}x${pointSize} ${combineOp.toLowerCase()})` : ''}`),
    };
}

/**
 * Make the chart for a z-axis extraction from a cube: the value at one image point in every plane.
 * The x-axis is wavelength when the cube has wavelength info, otherwise the plane number (starting at 1).
 * @param {ImagePt} imPt - the extracted point
 * @param {PlotView} pv
 * @param {Array.<number>} dataAry - extracted value for each plane
 * @param {number} x - zero-based index of the highlighted plane
 * @param {number} y - value at the highlighted plane
 * @param {number} pointSize - width of the extraction box in pixels; when > 1 the y-axis title shows the box size and combineOp
 * @param {string} combineOp - how the values in the box are combined: 'AVG', 'SUM', or 'OR'
 * @param {string} title - chart title
 * @return {ExtractionChartParams}
 */
export function genZAxisChartData(imPt, pv, dataAry, x, y, pointSize, combineOp, title) {
    const plot = primePlot(pv);
    const hasWL = hasWLInfo(plot);
    const xAry = hasWL ? getAllWaveLengthsForCube(pv, imPt) : dataAry.map((d, i) => i + 1);
    const highlightX = hasWL ? getPtWavelength(plot, imPt, x) : x + 1;
    const highlightXLabel = `${formatNumber(highlightX,4,'f')} (plane: ${x + 1})`;

    const xLabel = (i) => `${formatNumber(xAry[i],4,'f')} (plane: ${i + 1})`;

    const plotlyData = hasWL ?
        makePlotlyDataObj(xAry, dataAry, highlightX, y, 'Wavelength', 'Z-Axis', xLabel, highlightXLabel) :
        makePlotlyDataObj(xAry, dataAry, highlightX, y, 'Cube Plane', 'Z-Axis');

    return {
        plotlyDivStyle,
        plotlyData,
        plotlyLayout: makePlotlyLayoutObj(title, hasWL ? 'Wavelength' : 'Cube Plane', `Z-Axis${pointSize > 1 ? ` (${pointSize}x${pointSize} ${combineOp.toLowerCase()})` : ''}`),
    };
}