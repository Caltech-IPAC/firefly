/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty, pickBy, cloneDeep, set} from 'lodash';

import {getTblById, getColumn, doFetchTable, getColumnIdx} from '../../tables/TableUtil.js';
import {getSpectrumDM, REF_POS} from '../../voAnalyzer/SpectrumDM.js';
import {dispatchChartUpdate, dispatchError} from '../ChartsCntlr.js';
import {getDataChangesForMappings, updateHighlighted, updateSelection, getMinScatterGLRows, isSpectralOrder} from '../ChartUtil.js';
import {addOtherChanges, createChartTblRequest, getTraceTSEntries as genericTSGetter} from './FireflyGenericData.js';

import {quoteNonAlphanumeric} from '../../util/expr/Variable.js';
import {getXLabel, getUnitInfo} from './SpectrumUnitConversion.js';

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

    if (!isSpectralOrder(chartId)) {
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
        updateSelection(chartId, selectInfo);
    } else {
        dispatchError(chartId, traceNum, 'No data');
    }
}



export function spectrumPlot({tbl_id, spectrumDM}) {
    const tableModel = getTblById(tbl_id);
    const type = tableModel?.totalRows >= getMinScatterGLRows() ? 'scattergl' : 'scatter';

    const {xLabel, yLabel,  xUnit, yUnit, spectralAxis, mode, spectralFrame, derivedRedshift, target, ...more} = getSpectrumProps(tbl_id, spectrumDM);

    // append tables:: to these props
    const {x, y, xErrArray, xErrArrayMinus, yErrArray, yErrArrayMinus, xMax, xMin, yMax, yMin} = Object.fromEntries(Object.entries(more).map( ([k,v]) => [k, v && `tables::${v}`]));

    const error_x = pickBy({array: xErrArray, arrayminus: xErrArrayMinus});
    const error_y = pickBy({array: yErrArray, arrayminus: yErrArrayMinus});

    const firefly = { dataType: spectrumType, useSpectrum: 'true', xMax, xMin, yMax, yMin, xUnit, yUnit, spectralFrame, derivedRedshift, target};
    const data = [ pickBy({tbl_id, type, x, y, error_x, error_y, firefly, mode}, (a) => !isEmpty(a))];

    if (!isEmpty(error_x)) set(firefly, 'error_x.visible', 'true');
    if (!isEmpty(error_y)) set(firefly, 'error_y.visible', 'true');

    const layout = {
        xaxis: {
            title: xLabel,
        },
        yaxis: {
            title: yLabel,
        }
    };

    if (spectralAxis.order) {
        const orderCol = getColumn(tableModel, spectralAxis.order);
        const orig = data.pop();
        orderCol?.enumVals?.split(',').forEach((v) => {
            const order = cloneDeep(orig);
            set(order, 'name', v);
            set(order, 'mode', 'lines+markers');
            set(order, 'firefly.spectralOrder', spectralAxis.order);
            set(order, 'firefly.filters', `"${orderCol.name}" = '${v}'`);
            data.push(order);
        });
        set(layout, 'legend.title.text', spectralAxis.order);
    }

    return {data, layout};
}

export function getSpectrumProps(tbl_id, spectrumDM) {

    const tbl= getTblById(tbl_id);
    spectrumDM = spectrumDM || getSpectrumDM(tbl);
    const {spectralAxis={}, fluxAxis={}, isSED, spectralFrame={}, derivedRedshift={}, target={}} = spectrumDM || {};

    const x = quoteNonAlphanumeric(spectralAxis.value);
    const y = quoteNonAlphanumeric(fluxAxis.value);

    const xErrArray = spectralAxis.statErrHigh || spectralAxis.statError;
    const xErrArrayMinus = spectralAxis.statErrLow;
    const yErrArray = fluxAxis.statErrHigh || fluxAxis.statError;
    let yErrArrayMinus = fluxAxis.statErrLow;
    if (yErrArray && fluxAxis.statErrLow) {
        const hiErrCol= getColumnIdx(tbl,yErrArray);
        const lowErrCol= getColumnIdx(tbl,fluxAxis.statErrLow);
        let allEmpty=true;
        const reverse= tbl.tableData.data.every( (row) => {
            const lVal= row[lowErrCol];
            const hVal= row[hiErrCol];
            if (isNaN(hVal) || isNaN(lVal) || hVal===null || lVal===null) return true;
            allEmpty= false;
            return hVal>=0 && lVal<=0;
        });
        if (reverse && !allEmpty) yErrArrayMinus= '-'+fluxAxis.statErrLow;
    }
    const xMax = spectralAxis.binHigh;
    const xMin = spectralAxis.binLow;
    const yMax = fluxAxis.upperLimit;
    const yMin = fluxAxis.lowerLimit;

    const xUnit = spectralAxis.unit;
    const yUnit = fluxAxis.unit;

    // get default spectral frame and labels, needed to initialize spectrum
    // (in future, move redshift processing functions & defaults from SpectrumOptions to a separate file where they can be exported to avoid redundancy)
    const refPos = spectralFrame.refPos.toUpperCase();
    const sfLabel = refPos===REF_POS.TOPOCENTER ? 'Observed Frame'
        : refPos===REF_POS.CUSTOM ? 'Rest Frame' : `${refPos} Spectral Frame`;
    const redshiftLabel = refPos===REF_POS.CUSTOM ? `Custom Redshift = ${spectralFrame.redshift}` : '';

    const xLabel = getXLabel(spectralAxis.value, spectralAxis.unit, sfLabel, redshiftLabel);
    const yLabel = getUnitInfo(fluxAxis.unit, y)?.label;

    const mode = isSED ? 'markers' : 'lines+markers';

    return {spectralAxis, fluxAxis, mode, x, y, xErrArray, xErrArrayMinus, yErrArray, yErrArrayMinus,
            xMax, xMin, yMax, yMin, xUnit, yUnit, xLabel, yLabel, isSED, spectralFrame, derivedRedshift, target};
}