/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {logError} from '../util/WebUtil.js';

/**
 * @global
 * @public
 * @typedef {Object} ChartType - an object which specifies how to render a chart type
 * @prop {string} id - unique chart type id
 * @prop {Function} renderChart - function to render chart part: renderChart(chartId)
 * @prop {Function} renderOptions - function to render chart options: renderOptions(chartId)
 * @prop {Function} renderToolbar - function to render toolbar renderToolbar(chartId, expandedMode, expandable)
 */

const chartTypes = [];


/**
 * Get chart data type
 * @param id
 * @returns {ChartType}
 */
function getChartType(id) {
    return chartTypes.find((el) => {
        return el.id === id;
    });
}

/**
 * Add chart data type
 * @param {ChartType} chartType
 */
function addChartType(chartType) {
    const id = {chartType};
    if (!id) {
        logError('[ChartTypes] unable to add: missing id');
        return;
    }
    if (chartTypes.find((el) => {
            return el.id === id;
        })) {
        logError(`[ChartTypes] unable to add: id ${id} is already used`);
        return;
    }
    // more validation?
    chartTypes.push(chartType);
}

/**
 * Create a factory to manage chart data types
 * @param {Array<ChartType>} predefinedTypes
 * @return {{getChartType:Function, addChartType:Function}}
 */
export function chartTypeFactory(predefinedTypes)
{
    predefinedTypes.forEach((cdt) => addChartType(cdt));
    return {
        getChartType,
        addChartType
    };
}