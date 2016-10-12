/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {DATATYPE_XYCOLS} from './ChartDataTypeXYCols.js';
import {DATATYPE_HISTOGRAM} from './ChartDataTypeHistogram.js';
import {logError} from '../util/WebUtil.js';

/**
 * @global
 * @public
 * @typedef {Object} ChartDataType - an object which specifies how to get chart data
 * @prop {string} id - unique chart data type id
 * @prop {Function} fetchData - function to load chart data element data: fetchData(dispatch, chartId, chartDataElementId)
 * @prop {Function} fetchParamsChanged - function to determine if fetch is necessary: fetchParamsChanged(oldOptions, newOptions)
 * @prop {Function} getUpdatedOptions - function to resolve the options, which depend on table or chart data getUpdatedOptions(xyPlotParams, tblId, data)
 */

const chartDataTypes = [];

function getChartDataTypes() {
    if (chartDataTypes.length===0) {
        addChartDataType(DATATYPE_XYCOLS);
        addChartDataType(DATATYPE_HISTOGRAM);
    }
    return chartDataTypes;
}

export function getChartDataType(id) {
    return getChartDataTypes().find((el) => {return el.id===id;});
}

/**
 * Add chart
 * @param {ChartDataType} chartDataType
 */
export function addChartDataType(chartDataType) {
    const id = {chartDataType};
    if (!id) {
        logError('[ChartDataTypes] unable to add: missing id');
        return;
    }
    if (chartDataTypes.find((el) => {return el.id===id;})) {
        logError(`[ChartDataTypes] unable to add: id ${id} is already used`);
        return;
    }
    // more validation?
    chartDataTypes.push(chartDataType);
}

