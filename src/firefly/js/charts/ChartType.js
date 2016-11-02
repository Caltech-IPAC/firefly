/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {logError} from '../util/WebUtil.js';

/**
 * @global
 * @public
 * @typedef {Object} ChartType - an object which specifies how to render a chart type
 * @prop {string} id - unique chart type id
 * @prop {Function} Chart - React functional component to render chart part: Chart({chartId, ...chartProps, widthPx, heightPx})
 * @prop {Function} Options - React functional component to render chart options: Options({chartId, optionsKey})
 * @prop {Function} Toolbar - React functional component to render toolbar: Toolbar({chartId, expandable, expandedMode, toggleOptions})
 * @prop {Function} getChartProperties - function to get chart properties: getChartProperties(chartId)
 * @prop {Function} updateOnStoreChange - function with returns true if chart needs to update on store update: updateOnStoreChange(prevChartProps)
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