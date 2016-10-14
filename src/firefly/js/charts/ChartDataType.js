/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {logError} from '../util/WebUtil.js';

/**
 * @global
 * @public
 * @typedef {Object} ChartDataType - an object which specifies how to get chart data
 * @prop {string} id - unique chart data type id
 * @prop {Function} fetchData - function to load chart data element data: fetchData(dispatch, chartId, chartDataElementId)
 * @prop {Function} fetchParamsChanged - function to determine if fetch is necessary: fetchParamsChanged(oldOptions, newOptions)
 * @prop {Function} getUpdatedOptions - function to resolve the options, which depend on table or chart data getUpdatedOptions(options, tblId, data, meta)
 * @prop {boolean} [fetchOnTblSort=true] - if false, don't re-fetch data on tbl sort
 */

const chartDataTypes = [];


/**
 * Get chart data type
 * @param id
 * @returns {ChartDataType}
 */
function getChartDataType(id) {
    return chartDataTypes.find((el) => {
        return el.id === id;
    });
}

/**
 * Add chart data type
 * @param {ChartDataType} chartDataType
 */
function addChartDataType(chartDataType) {
    const id = {chartDataType};
    if (!id) {
        logError('[ChartDataTypes] unable to add: missing id');
        return;
    }
    if (chartDataTypes.find((el) => {
            return el.id === id;
        })) {
        logError(`[ChartDataTypes] unable to add: id ${id} is already used`);
        return;
    }
    // more validation?
    chartDataTypes.push(chartDataType);
}

/**
 * Create a factory to manage chart data types
 * @param {Array<ChartDataType>} predefinedTypes
 * @return {{getChartDataType:Function, addChartDataType:Function}}
 */
export function chartDataTypeFactory(predefinedTypes)
{
    predefinedTypes.forEach((cdt) => addChartDataType(cdt));
    return {
        getChartDataType,
        addChartDataType
    };
}