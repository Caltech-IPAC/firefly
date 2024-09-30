import {sprintf} from '../../../externalSource/sprintf';
import {Band} from '../../Band.js';
import {getExtName} from '../../FitsHeaderUtil.js';
import {
    getAllWaveLengthsForCube, getHDU, getPtWavelength, hasWCSProjection, hasWLInfo, primePlot
} from '../../PlotViewUtil.js';
import {getFluxUnits} from '../../WebPlot.js';

const plotlyDivStyle = {border: '1px solid a5a5a5', borderRadius: 5, width: '100%', height: '100%'};

function makePlotlyLayoutObj(title, xAxis, yAxis, reversed = false) {
    const font = {size: 12};
    return {
        hovermode: 'closest',
        title: {text: title, font},
        showlegend: false,
        xaxis: {
            title: {text: xAxis, font},
            gridLineWidth: 1,
            type: 'linear',
            lineColor: '#e9e9e9',
            zeroline: false,
            tickfont: font,
            exponentformat: 'e',
            autorange: reversed ? 'reversed' : true,
        },
        yaxis: {
            title: {text: yAxis, font},
            gridLineWidth: 1,
            type: 'linear',
            lineColor: '#e9e9e9',
            exponentformat: 'e'
        },
        margin: {l: 50, r: 50, b: 50, t: 30, pad: 2}
    };
}

const dataRoot = {displaylogo: false, mode: 'markers', hoverinfo: 'text', hovermode: 'closest'};

function makePlotlyDataObj(xDataAry, yDataAry, x, y, ttXStr, ttYStr, makeXDesc = (i) => xDataAry[i], highlightXDesc = x + '') {
    const result = [
        {
            ...dataRoot,
            type: xDataAry?.length > 6000 ? 'scattergl' : 'scatter',
            marker: {symbol: 'circle', size: 6, color: 'rgba(63, 127, 191, 0.5)'},
            x: xDataAry,
            y: yDataAry,
            hovertext: Array.from(yDataAry).map((d, i) => `<span> ${ttXStr} = ${makeXDesc(i)}  <br> ${ttYStr}= ${d}   </span>`),
            textfont: {color: 'rgba(31,119,180,0.5)'},
        },
        {
            ...dataRoot,
            type: 'scatter',
            marker: {symbol: 'circle', size: 6, color: 'rgba(255, 200, 0, 1)'},
            x: [x],
            y: [y],
            hovertext: [`<span> ${ttXStr} = ${highlightXDesc}  <br> ${ttYStr} = ${y}   </span>`],
        }
    ];
    return result;
}

export function genSliceChartData(plot, ipt1, ipt2, xDataAry, yDataAry, x, y, pointSize, combineOp, title, reversed) {
    const unitStr = hasWCSProjection(plot) ? ' (arcsec)' : '';
    const yUnits = getFluxUnits(plot, Band.NO_BAND);
    const yAxisLabel = getExtName(plot) || 'HDU# ' + getHDU(plot);
    return {
        plotlyDivStyle,
        plotlyData: makePlotlyDataObj(xDataAry, yDataAry, x, y, 'Offset' + unitStr, yAxisLabel),
        plotlyLayout: makePlotlyLayoutObj(title, 'Offset' + unitStr,
            `${yAxisLabel} (${yUnits})${pointSize > 1 ? ` (${pointSize}x${pointSize} ${combineOp.toLowerCase()})` : ''}`, reversed),
    };
}

export function genPointChartData(plot, dataAry, imPtAry, x, y, pointSize, combineOp, title, chartXAxis, activeIdx) {

    const xAxis = chartXAxis === 'imageX' ? imPtAry.map((pt) => pt.x) : imPtAry.map((pt) => pt.y);
    const xAxisTitle = chartXAxis === 'imageX' ? 'Image X' : 'Image Y';
    const xLabel = (i) => `(${imPtAry[i].x},${imPtAry[i].y})`;
    const yUnits = getFluxUnits(plot, Band.NO_BAND);
    const yAxisLabel = getExtName(plot) || 'HDU# ' + getHDU(plot);

    return {
        plotlyDivStyle,
        plotlyData: makePlotlyDataObj(xAxis, dataAry, x, y, xAxisTitle, yAxisLabel,
            xLabel, xLabel(activeIdx)),
        plotlyLayout: makePlotlyLayoutObj(title, xAxisTitle, `${yAxisLabel} (${yUnits})${pointSize > 1 ? ` (${pointSize}x${pointSize} ${combineOp.toLowerCase()})` : ''}`),
    };
}

export function genZAxisChartData(imPt, pv, dataAry, x, y, pointSize, combineOp, title) {
    const plot = primePlot(pv);
    const hasWL = hasWLInfo(plot);
    const xAry = hasWL ? getAllWaveLengthsForCube(pv, imPt) : dataAry.map((d, i) => i + 1);
    const highlightX = hasWL ? getPtWavelength(primePlot(pv), imPt, x) : x + 1;
    const highlightXLabel =
        isNaN(highlightX) ?
            `${highlightX + ''} (plane: ${x + 1})` :
            `${sprintf('%5.4f', highlightX)} (plane: ${x + 1})`;

    const format = (v) => isNaN(v) ? '' : sprintf('%5.4f', v);
    const xLabel = (i) => `${format(getPtWavelength(primePlot(pv), imPt, x))} (plane: ${i + 1})`;

    const plotlyData = hasWL ?
        makePlotlyDataObj(xAry, dataAry, highlightX, y, 'Wavelength', 'z-azis', xLabel, highlightXLabel) :
        makePlotlyDataObj(xAry, dataAry, highlightX, y, 'Plane', 'z-azis');

    return {
        plotlyDivStyle,
        plotlyData,
        plotlyLayout: makePlotlyLayoutObj(title, hasWL ? 'Wavelength' : 'cube plane', `Z-Axis${pointSize > 1 ? ` (${pointSize}x${pointSize} ${combineOp.toLowerCase()})` : ''}`),
    };
}