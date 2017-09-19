/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get} from 'lodash';
import {getTblById, cloneRequest, doFetchTable, makeTblRequest, MAX_ROW} from '../../tables/TableUtil.js';
import {dispatchChartUpdate, dispatchChartHighlighted, getChartData} from '../ChartsCntlr.js';
import {getDataChangesForMappings, getPointIdx, updateSelected} from '../ChartUtil.js';

/**
 * This function creates table source entries to get plotly chart data from the server
 * (The search processor knows how to handle expressions and eliminates null mapping key)
 *
 */

/**
 * This function creates table source entries to get plotly chart data from the server
 * (The search processor knows how to handle expressions and eliminates null mapping key)
 * For the plotly chart which type is not recognized by Firefly
 * @param traceTS
 * @returns {{options: *, fetchData: fetchData}}
 */
export function getTraceTSEntries({traceTS}) {
    const {mappings} = traceTS || {};
    const options = {};

    if (mappings) {
        Object.keys(mappings).forEach((key) => {
            options[`${key}ColOrExp`] = mappings[key];
        });
        return {options, fetchData};
    } else {
        return {};
    }
}

function fetchData(chartId, traceNum, tablesource) {

    const {tbl_id, options, mappings} = tablesource;
    const originalTableModel = getTblById(tbl_id);
    const {request, highlightedRow, selectInfo} = originalTableModel;
    const sreq = cloneRequest(request, {startIdx: 0, pageSize: MAX_ROW});

    const req = makeTblRequest('XYGeneric');
    req.searchRequest = JSON.stringify(sreq);
    req.startIdx = 0;
    req.pageSize = MAX_ROW;
    Object.entries(options).forEach(([k,v]) => v && (req[k]=v));

    doFetchTable(req).then((tableModel) => {
        if (tableModel.tableData && tableModel.tableData.data) {
            const {tableMeta} = tableModel;
            const validCols = Object.keys(mappings);

            tableModel.tableData.columns.forEach((col) => {
                const name = col.name;
                if (validCols.includes(name) && tableMeta[name]) {
                    col.name = tableMeta[name];
                }
            });

            const changes = getDataChangesForMappings({tableModel, mappings, traceNum});

            dispatchChartUpdate({chartId, changes});
            const traceData = get(getChartData(chartId), `data.${traceNum}`);
            dispatchChartHighlighted({chartId, highlighted: getPointIdx(traceData,highlightedRow)});   // update highlighted point in chart
            updateSelected(chartId, selectInfo);
        }
    }).catch(
        (reason) => {
            console.error(`Failed to fetch data for ${chartId} trace ${traceNum}: ${reason}`);
        }
    );
}

