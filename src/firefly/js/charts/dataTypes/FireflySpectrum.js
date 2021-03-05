/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty, pickBy, cloneDeep, set} from 'lodash';

import {getTblById, getColumn, doFetchTable, getColumnIdx} from '../../tables/TableUtil.js';
import {dispatchChartUpdate, dispatchError} from '../ChartsCntlr.js';
import {getDataChangesForMappings, updateHighlighted, updateSelected, getMinScatterGLRows, isFluxAxisOrder} from '../ChartUtil.js';
import {addOtherChanges, createChartTblRequest, getTraceTSEntries as genericTSGetter} from './FireflyGenericData.js';

import {quoteNonAlphanumeric} from '../../util/expr/Variable.js';
import {getUnitInfo} from './SpectrumUnitConversion.js';

export const spectrumType = 'spectrum';



/**
 * This function creates table source entries to get plotly chart data from the server
 * For the non-firefly plotly chart types
 * @param traceTS
 * @returns {{options: *, fetchData: fetchData}}
 */
export function getTraceTSEntries({traceTS, chartId, traceNum}) {

    const traceEntry = genericTSGetter({traceTS, chartId, traceNum});
    if (isEmpty(traceEntry)) return {};

    if (!isFluxAxisOrder(chartId)) {
        return {options: traceEntry.options, fetchData:traceEntry.fetchData};
    }
    return {options: traceEntry.options, fetchData};

}

async function fetchData(chartId, traceNum, tablesource) {

    const {tbl_id, mappings} = tablesource;
    if (!mappings) {
        return;
    }

    const originalTableModel = getTblById(tbl_id);
    const {highlightedRow, selectInfo} = originalTableModel;

    const sreq = createChartTblRequest(chartId, traceNum, tablesource);
    // set(sreq, 'inclCols', (sreq.inclCols + ', "ROW_IDX"');

    const tableModel = await doFetchTable(sreq).catch((reason) => {
        dispatchError(chartId, traceNum, reason);
    });

    if (tableModel.error) {
        return dispatchError(chartId, traceNum, tableModel.error);
    }

    if (tableModel?.tableData?.data) {
        const changes = getDataChangesForMappings({tableModel, mappings, traceNum});

        // extra changes based on trace type
        addOtherChanges({changes, chartId, traceNum, tablesource, tableModel: originalTableModel});

        // add row_idx to pointIdx mappings
        const origIdx = getColumnIdx(tableModel, 'ORIG_IDX');
        const rowIdx = tableModel.tableData.data.map((row) => row[origIdx]);
        set(changes, [`data.${traceNum}.firefly.rowIdx`], rowIdx);

        dispatchChartUpdate({chartId, changes});
        updateHighlighted(chartId, traceNum, highlightedRow);
        updateSelected(chartId, selectInfo);
    } else {
        dispatchError(chartId, traceNum, 'No data');
    }
}



export function spectrumPlot({tbl_id, spectrumDM}) {
    const tableModel = getTblById(tbl_id);

    const error = ({statError, statErrLow, statErrHigh}) => {
        const arrayminus  = statErrLow && `tables::${statErrLow}`;
        let array = statErrHigh || statError;
        array = array && `tables::${array}`;
        return pickBy({arrayminus, array});
    };

    const type = tableModel?.totalRows >= getMinScatterGLRows() ? 'scattergl' : 'scatter';

    const {spectralAxis={}, fluxAxis={}} = spectrumDM || {};

    const xColName = quoteNonAlphanumeric(spectralAxis.value);
    const yColName = quoteNonAlphanumeric(fluxAxis.value);

    const x = xColName && `tables::${xColName}`;
    const y = yColName && `tables::${yColName}`;
    const error_x = error(spectralAxis);
    const error_y = error(fluxAxis);
    const xMax = spectralAxis.binHigh  && `tables::${spectralAxis.binHigh}`;
    const xMin = spectralAxis.binLow   && `tables::${spectralAxis.binLow}`;
    const yMax = fluxAxis.upperLimit   && `tables::${fluxAxis.upperLimit}`;
    const yMin = fluxAxis.lowerLimit   && `tables::${fluxAxis.lowerLimit}`;

    const xUnit = spectralAxis.unit;
    const yUnit = fluxAxis.unit;

    const xLabel = getUnitInfo(spectralAxis.unit, true)?.label;
    const yLabel = getUnitInfo(fluxAxis.unit, false)?.label;

    const firefly = { dataType: spectrumType, useSpectrum: 'true', xMax, xMin, yMax, yMin, xUnit, yUnit};
    const data = [ pickBy({tbl_id, type, x, y, error_x, error_y, firefly, mode: 'markers'}, (a) => !isEmpty(a))];


    const layout = {
        xaxis: {
            title: xLabel,
        },
        yaxis: {
            title: yLabel,
        }
    };

    if (fluxAxis.order) {
        const orderCol = getColumn(tableModel, fluxAxis.order);
        const orig = data.pop();
        orderCol?.enumVals?.split(',').forEach((v) => {
            const order = cloneDeep(orig);
            set(order, 'name', v);
            set(order, 'mode', 'lines+markers');
            set(order, 'firefly.fluxAxisOrder', fluxAxis.order);
            set(order, 'firefly.filters', `"${orderCol.name}" = '${v}'`);
            data.push(order);
        });
        set(layout, 'legend.title.text', fluxAxis.order);
    }

    return {data, layout};
}