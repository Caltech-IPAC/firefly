/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, identity, isArray, isUndefined, uniqueId} from 'lodash';
import {logger} from '../../util/Logger.js';
import {COL_TYPE, getColumn, getColumns, getTblById, doFetchTable, stripColumnNameQuotes} from '../../tables/TableUtil.js';
import {cloneRequest, makeTableFunctionRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {dispatchChartUpdate, dispatchError, getChartData} from '../ChartsCntlr.js';
import {formatColExpr} from '../ChartUtil.js';


import {toMaxFixed, getDecimalPlaces} from '../../util/MathUtil.js';
import Color from '../../util/Color.js';

//const HIST_DEC = 6;

/**
 * This function creates table source entries to get aggregated firefly histogram data from the server
 * @param p
 * @param p.traceTS
 * @param p.chartId
 * @param p.traceNum
 * @returns {{options: {}, fetchData: fetchData}}
 */
export function getTraceTSEntries({chartId, traceNum}) {

    const options = {};

    // server call parameters
    const {fireflyData, layout} = getChartData(chartId) || {};
    const histogramParams = get(fireflyData, `${traceNum}.options`);
    options.columnExpression = histogramParams.columnOrExpr;
    if (get(layout, 'xaxis.type') === 'log') {
        options.columnExpression = 'lg('+histogramParams.columnOrExpr+')';
    }
    if (histogramParams.fixedBinSizeSelection) { // fixed size bins
        options.fixedBinSizeSelection=histogramParams.fixedBinSizeSelection;
        if (histogramParams.fixedBinSizeSelection==='numBins'){
            options.binSize =  histogramParams.numBins;
        }else{
            options.binSize =  histogramParams.binWidth;
        }
    }
    if (histogramParams.falsePositiveRate) {  // variable size bins using Bayesian Blocks
        options.falsePositiveRate = histogramParams.falsePositiveRate;
    }
    if (histogramParams.minCutoff) {
        options.min = histogramParams.minCutoff;
    }
    if (histogramParams.maxCutoff) {
        options.max = histogramParams.maxCutoff;
    }

    return {options, fetchData};
}

function fetchData(chartId, traceNum, tablesource) {
    const {tbl_id, options} = tablesource;

    const tableModel = getTblById(tbl_id);
    const numericCols = getColumns(tableModel, COL_TYPE.NUMBER).map((c) => c.name);

    const {request} = tableModel;
    const valueColName = 'columnExpression';
    const sreq = cloneRequest(request, {
        startIdx: 0,
        pageSize: MAX_ROW,
        inclCols: `${formatColExpr({colOrExpr:options.columnExpression, quoted: true, colNames: numericCols})} as "${valueColName}"`,
        sortInfo: `ASC,"${valueColName}"`
    });
    const sreqTblId = uniqueId(request.tbl_id);
    sreq.META_INFO.tbl_id = sreqTblId;
    sreq.tbl_id = sreqTblId;

    const req = makeTableFunctionRequest(sreq, 'HistogramProcessor', 'histogram', {sortedColData: true, pageSize: MAX_ROW});

    Object.entries(options).forEach(([k,v]) => req[k] = v);
    req['columnName'] = valueColName;

    doFetchTable(req).then(
        (tableModel) => {

            if (tableModel.error) {
                dispatchError(chartId, traceNum, tableModel.error);
                return;
            } else if (!get(tableModel, 'tableData.data')) {
                dispatchError(chartId, traceNum, 'No data');
                return;
            }

            let histogramData = [];
            const tblData = get(tableModel, 'tableData.data');
            if (tblData) {
                // if logarithmic values were requested, convert the returned exponents back
                var toNumber = get(options, 'x', '').includes('log') ?
                    (val, i)=> {
                        // first col is n, next two are the boundaries of the bin
                        return (i === 0) ? Number(val) : Math.pow(10, Number(val));
                    } : (val)=>Number(val);
                var nrow, lastIdx;
                histogramData = tblData.reduce((data, arow, i) => {
                    nrow = arow.map(toNumber);
                    lastIdx = data.length - 1;
                    if (i > 0 && data[lastIdx][1] === nrow[1]) {
                        //collapse bins when two bins are the same because of precision loss
                        // (ex. large long on server side truncated to float)
                        data[lastIdx] = [data[lastIdx][0] + nrow[0], data[lastIdx][1], nrow[2]];
                    } else {
                        data.push(nrow);
                    }
                    return data;
                }, []);

            }
            const {data, layout={}} = getChartData(chartId);
            let binColor = get(data, `${traceNum}.marker.color`);
            if (isArray(binColor)) binColor = binColor[0];
            const changes = getChanges({histogramData, binColor, traceNum});

            // set default title if it's the first trace
            // and no title is set by the user
            if (data && data.length===1) {
                const xAxisLabel = get(layout, 'xaxis.title.text');
                if (!xAxisLabel) {
                    // default axis label for the first trace
                    let xLabel = get(options, 'columnExpression', '');
                    const xColumn = getColumn(getTblById(tbl_id), xLabel);
                    const xUnit = get(xColumn, 'units', '');
                    //remove surrounding quotes, if any
                    if (xLabel.startsWith('"')) { xLabel = stripColumnNameQuotes(xLabel); }
                    changes['layout.xaxis.title.text'] = xLabel + (xUnit ? ` (${xUnit})` : '');
                }
                const yAxisLabel = get(layout, 'yaxis.title.text');
                if (!yAxisLabel) {
                    changes['layout.yaxis.title.text'] = 'Number';
                }
            }

            if (!layout.barmode) {
                // use overlay mode for Plotly charts, based on bars
                // this is to make sure plotly histogram displays fine with firefly histogram
                changes['layout.barmode'] = 'overlay';
            }
            changes[`fireflyData.${traceNum}.isLoading`] = false;

            dispatchChartUpdate({chartId, changes});
        }
    ).catch(
        (reason) => {
            dispatchError(chartId, traceNum, reason);
        }
    );
}


function getChanges({histogramData, binColor, traceNum}) {
    var {x, y, binWidth, color, text, colorScale, borderColor} = createXY(histogramData, binColor);
    const changes = {};
    changes[`data.${traceNum}.type`] = 'bar'; // using bar chart to display firefly histogram
    changes[`data.${traceNum}.x`] = x;
    changes[`data.${traceNum}.y`] = y;
    changes[`data.${traceNum}.width`] = binWidth;
    changes[`data.${traceNum}.marker.color`] = color;
    changes[`data.${traceNum}.marker.colorscale`] = colorScale;
    changes[`data.${traceNum}.marker.line`] = {width: 1, color: borderColor};
    changes[`data.${traceNum}.hovertext`] = text;
    changes[`data.${traceNum}.hoverinfo`] = 'text';
    return changes;
}

/*
 * Expecting an 2 dimensional array of numbers
 * each row is an array of 3 values:
 * [0] number of points in a bin,
 * [1] minimum of a bin
 * [2] maximum of a bin
 */
function validateData(histogramData, logScale) {
    if (!histogramData) { return false; }
    let valid = true;
    try {
        histogramData.sort(function(row1, row2){
            // [1] is minimum bin edge
            return row1[1]-row2[1];
        });
        if (histogramData) {
            for (var i = 0; i < histogramData.length; i++) {
                if (histogramData[i].length < 3) {
                    logger.error(`Invalid histogram data in row ${i} [${histogramData[i]}]`);
                    valid = false;
                } else if (histogramData[i][1]>histogramData[i][2]) {
                    logger.error(`Histogram data row ${i}: minimum is more than maximum. [${histogramData[i]}]`);
                    valid=false;
                } else if (histogramData[i+1] && Math.abs(histogramData[i][2]-histogramData[i+1][1])>1000*Number.EPSILON &&
                    histogramData[i][2]>histogramData[i+1][1]) {
                    logger.error(`Histogram data row ${i}: bin range overlaps the following row. [${histogramData[i]}]`);
                    valid=false;
                } else if (histogramData[i][0] < 0) {
                    logger.error(`Histogram data row ${i} count is less than zero, ${histogramData[i][0]}`);
                    valid=false;
                }
            }
            if (logScale && histogramData[0][1]<=0) {
                logger.error('Unable to plot histogram: zero or subzero values on logarithmic scale');
                valid = false;
            }
        }
    } catch (e) {
        logger.error(`Invalid data passed to Histogram: ${e}`);
        valid = false;
    }
    return valid;
}

function createXY(data, binColor) { // removed '#d1d1d1' to use default colors
    var  emptyData = {
        x: [], y: [], text:[], borderColor: '',  binWidth: [], color: [], colorScale: []
    };

    if (!validateData(data)) {
        emptyData.valid = false;
        return emptyData;
    } else {
        emptyData.valid = true;
    }

    //const getRGBAStr = (rgbStr, a) => {
    //    return Color.toRGBAString(Color.toRGBA(rgbStr.slice(1), a));
    //};

    //const lightColor = getRGBAStr(Color.shadeColor(binColor, 0.2), A);
    emptyData.color = binColor; // for multiple traces changing alternating colors are confusing
    emptyData.borderColor = binColor && binColor === '#000000' ? Color.shadeColor(binColor, 0.5) : 'black';
    //emptyData.colorScale = [[0, getRGBAStr(binColor, A)], [1, lightColor]];

    // compute bin width for the bin has the same xMin & xMan
    var startW = data.length && data[data.length-1][2] - data[0][1];
    if (startW === 0.0) startW = 1.0;

    var   minWidth = data.find((row) => {return row[1]===row[2];}) ?
                        data.reduce((prev, d) => {
                            if (d[1] !== d[2]) {
                                const dist = (d[2] - d[1])/2;

                                if (dist < prev) {
                                    prev = dist;
                                }
                            }
                            return prev;
                        }, startW*0.02) : 1.0;

    //var lastX = data[0][1]-1.0;
    //var prevColor = 0;

    var addBin = (xySeries, x1, x2, y) => {
        const xVal = (x1 + x2) / 2;

        xySeries.x.push(xVal);
        xySeries.y.push(y);
        const binWidth = x1 === x2 ? minWidth: x2-x1;
        xySeries.binWidth.push(binWidth);

        //prevColor = (x1 <= lastX) ? (prevColor+1)%2 : 0;  // when two bars are next to each other, color is changed
        //xySeries.color.push(prevColor);

        const numDecimalPlaces = getDecimalPlaces(binWidth, 3);
        const doFmt = isUndefined(numDecimalPlaces) ? identity : (n) => toMaxFixed(n, numDecimalPlaces);
        
        xySeries.text.push(
            `<span> ${x1 !== x2 ? '<b>Bin center: </b>' + doFmt(xVal) + '<br>' : ''}` +
            `${x1 !== x2 ? '<b>Range: </b>' + doFmt(x1) + ' to ' + doFmt(x2) + '<br>' : ''}` +
            `<b>Count:</b> ${y}</span>`);

        //lastX = x2;
    };

    return data.reduce((xy, oneData) => {
        addBin(xy, oneData[1], oneData[2], oneData[0]);
        return xy;
    }, emptyData);
}