/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*eslint-env node, jest */

import {getDefaultChartProps} from '../../charts/ChartUtil.js';
import {CHART_UI_EXPANDED, dispatchChartAdd, dispatchChartRemove} from '../../charts/ChartsCntlr.js';
import {ensureDefaultChart} from '../../charts/ui/ChartsContainer';
import {pinChart} from '../../charts/ui/PinnedChartContainer';
import ComponentCntlr from '../../core/ComponentCntlr.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../../core/MasterSaga.js';
import {ChartType, TableDataType} from '../../data/FileAnalysis.js';
import {MetaConst} from '../../data/MetaConst.js';
import {
    dispatchActiveTableChanged, dispatchTableRemove, dispatchTableSearch, TBL_RESULTS_ACTIVE, TBL_UI_EXPANDED
} from '../../tables/TablesCntlr.js';
import {getActiveTableId, getCellValue, getMetaEntry, getTblById, onTableLoaded} from '../../tables/TableUtil.js';
import {showPinMessage} from '../../ui/PopupUtil.jsx';
import {dispatchUpdateCustom, getViewer} from '../../visualize/MultiViewCntlr.js';
import {makeWorldPt} from '../../visualize/Point.js';
import {UPDATE_VIEWER_CUSTOM_DATA} from '../../visualize/VisConst.js';
import {
    getObsCoreAccessURL, getObsCoreSRegion, getSearchTarget, isFormatDataLink, makeWorldPtUsingCenterColumns
} from '../../voAnalyzer/TableAnalysis.js';
import {getServiceDescriptors} from '../../voAnalyzer/VoDataLinkServDef.js';
import {
    createChartSingleRowArrayActivate, createChartTableActivate, createTableActivate, createTableExtraction,
    extractDatalinkTable, findGridTableRows, getExtractionText, makeMultiTableActivate, makeMultiTableExtraction
} from '../TableDataProductUtils.js';
import {makeDlUrl} from '../vo/DatalinkProducts.js';
import {findDataLinkServeDescs} from '../vo/ServDescConverter.js';

// The chart UI modules can't load in Jest (validator/es, canvas), so they are replaced with stubs.
jest.mock('../../charts/ChartUtil.js', () => ({getDefaultChartProps: jest.fn()}));
jest.mock('../../charts/ui/ChartsContainer', () => ({ensureDefaultChart: jest.fn()}));
jest.mock('../../charts/ui/PinnedChartContainer', () => ({pinChart: jest.fn()}));
jest.mock('../../ui/PopupUtil.jsx', () => ({showPinMessage: jest.fn()}));
// __jest__/InitTest.js replaces LayoutCntlr with a stub for every test, and that stub has no layout constants,
// so they are added to it here
Object.assign(jest.requireMock('../../core/LayoutCntlr.js'), {
    LO_MODE: {expanded: 'expanded', standard: 'standard'},
    LO_VIEW: {none: 'none', tables: 'tables', images: 'images', xyPlots: 'xyPlots'},
    SET_LAYOUT_MODE: 'layout.setLayoutMode',
});
// the table, chart and viewer stores; each test sets what it needs
jest.mock('../../charts/ChartsCntlr.js', () => ({
    ...jest.requireActual('../../charts/ChartsCntlr.js'),
    dispatchChartAdd: jest.fn(),
    dispatchChartRemove: jest.fn(),
}));
jest.mock('../../core/MasterSaga.js', () => ({
    ...jest.requireActual('../../core/MasterSaga.js'),
    dispatchAddActionWatcher: jest.fn(),
    dispatchCancelActionWatcher: jest.fn(),
}));
jest.mock('../../tables/TablesCntlr.js', () => ({
    ...jest.requireActual('../../tables/TablesCntlr.js'),
    dispatchActiveTableChanged: jest.fn(),
    dispatchTableRemove: jest.fn(),
    dispatchTableSearch: jest.fn(),
}));
jest.mock('../../tables/TableUtil.js', () => ({
    ...jest.requireActual('../../tables/TableUtil.js'),
    getActiveTableId: jest.fn(),
    getCellValue: jest.fn(),
    getMetaEntry: jest.fn(),
    getTblById: jest.fn(),
    onTableLoaded: jest.fn(),
}));
jest.mock('../../visualize/MultiViewCntlr.js', () => ({
    ...jest.requireActual('../../visualize/MultiViewCntlr.js'),
    dispatchUpdateCustom: jest.fn(),
    getMultiViewRoot: jest.fn(),
    getViewer: jest.fn(),
}));
jest.mock('../../voAnalyzer/TableAnalysis.js', () => ({
    getObsCoreAccessURL: jest.fn(),
    getObsCoreSRegion: jest.fn(),
    getSearchTarget: jest.fn(),
    isFormatDataLink: jest.fn(),
    makeWorldPtUsingCenterColumns: jest.fn(),
}));
jest.mock('../../voAnalyzer/VoDataLinkServDef.js', () => ({getServiceDescriptors: jest.fn()}));
jest.mock('../vo/DatalinkProducts.js', () => ({makeDlUrl: jest.fn()}));
jest.mock('../vo/ServDescConverter.js', () => ({findDataLinkServeDescs: jest.fn()}));

const ACTIVATE_PARAMS = {tableGroupViewerId: 'dp-tables', chartViewerId: 'dp-charts'};
const SPECTRUM_URL = 'https://irsa.ipac.caltech.edu/data/example/spectrum.fits';
const TABLE_SEARCH_OPTIONS = {
    logHistory: false, removable: false, tbl_group: 'dp-tables', backgroundable: false, showFilters: true, showInfoButton: true
};

/** let the promise callbacks waiting on onTableLoaded run */
const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

/**
 * The watcher callback that createChartTableActivate added, called with one action.
 * @param {string} type
 * @param {Object} payload
 */
function sendToTableWatcher(type, payload) {
    const {callback} = dispatchAddActionWatcher.mock.calls[0][0];
    callback({type, payload});
}

beforeEach(() => {
    jest.resetAllMocks();
    onTableLoaded.mockResolvedValue(undefined);
});

describe('createTableActivate', () => {
    // createTableActivate always uses the table id part-result-tbl, so each test calls the cleanup at the end
    // to clear it from the loaded table cache

    test('loads the source as a table, with no chart, in the table group', () => {
        const cleanup = createTableActivate(SPECTRUM_URL, 'Spectrum', ACTIVATE_PARAMS, 'application/fits', 1)();
        expect(dispatchTableSearch).toHaveBeenCalledWith({
            id: 'IpacTableFromSource', source: SPECTRUM_URL, tbl_id: 'part-result-tbl', tbl_index: 1, cubePlane: 0,
            startIdx: 0,
            META_INFO: {[MetaConst.DATA_SOURCE]: 'false', [MetaConst.CATALOG_OVERLAY_TYPE]: 'false',
                title: 'Spectrum', tbl_id: 'part-result-tbl'},
        }, TABLE_SEARCH_OPTIONS);
        expect(dispatchChartAdd).not.toHaveBeenCalled();
        cleanup();
    });

    test('a spectrum content type, in any case, adds the spectrum data type hint', () => {
        const cleanup = createTableActivate(SPECTRUM_URL, 'Spectrum', ACTIVATE_PARAMS, 'SPECTRUM')();
        expect(dispatchTableSearch.mock.calls[0][0].META_INFO[MetaConst.DATA_TYPE_HINT]).toBe(TableDataType.Spectrum);
        cleanup();
    });
});

describe('createChartTableActivate', () => {
    // the loaded table cache outlives each test, so each test uses its own table id

    test('a title object gives its titleStr as the table title, and image columns go into the table meta', () => {
        createChartTableActivate({source: SPECTRUM_URL, titleInfo: {titleStr: 'HDU 1'}, activateParams: ACTIVATE_PARAMS,
            tbl_id: 'tbl-image-cols', imageAsTableInfo: {colNames: ['wave', 'flux'], colUnits: ['um', 'Jy']}})();
        const {META_INFO} = dispatchTableSearch.mock.calls[0][0];
        expect(META_INFO.title).toBe('HDU 1');
        expect(META_INFO[MetaConst.IMAGE_AS_TABLE_COL_NAMES]).toBe('wave,flux');
        expect(META_INFO[MetaConst.IMAGE_AS_TABLE_UNITS]).toBe('um,Jy');
    });

    test('with a chart, adds an x/y chart of the two columns once the table loads', async () => {
        createChartTableActivate({chartAndTable: true, source: SPECTRUM_URL, titleInfo: 'Spectrum',
            activateParams: ACTIVATE_PARAMS, tbl_id: 'tbl-xy', chartId: 'chart-xy',
            chartInfo: {xAxis: 'wave', yAxis: 'flux', connectPoints: true}})();
        expect(dispatchChartAdd).not.toHaveBeenCalled();
        await flushPromises();
        expect(onTableLoaded).toHaveBeenCalledWith('tbl-xy');
        expect(dispatchChartAdd).toHaveBeenCalledWith({
            viewerId: 'dp-charts', groupId: 'dp-charts', chartId: 'chart-xy',
            data: [{tbl_id: 'tbl-xy', x: 'tables::wave', y: 'tables::flux', mode: 'lines+markers'}],
            layout: {title: {text: 'Spectrum'}, xaxis: {showgrid: true}, yaxis: {showgrid: true, range: [0, undefined]}},
        });
    });

    test('with the chart chooser, adds the default chart for the table, with the title', async () => {
        getDefaultChartProps.mockReturnValue({data: [{tbl_id: 'tbl-chooser', x: 'tables::wave'}], layout: {}});
        createChartTableActivate({chartAndTable: true, source: SPECTRUM_URL, titleInfo: 'Spectrum',
            activateParams: ACTIVATE_PARAMS, tbl_id: 'tbl-chooser', chartId: 'chart-chooser',
            chartInfo: {xAxis: 'wave', yAxis: 'flux', useChartChooser: true}})();
        await flushPromises();
        expect(getDefaultChartProps).toHaveBeenCalledWith('tbl-chooser', 'wave', 'flux');
        expect(dispatchChartAdd).toHaveBeenCalledWith({
            viewerId: 'dp-charts', groupId: 'dp-charts', chartId: 'chart-chooser', useChartChooser: true,
            data: [{tbl_id: 'tbl-chooser', x: 'tables::wave'}], layout: {title: {text: 'Spectrum'}},
        });
    });

    test('showChartTitle false leaves the chart with no title', async () => {
        getDefaultChartProps.mockReturnValue({data: [], layout: {}});
        createChartTableActivate({chartAndTable: true, source: SPECTRUM_URL, titleInfo: {titleStr: 'table_1'},
            activateParams: ACTIVATE_PARAMS, tbl_id: 'tbl-no-title',
            chartInfo: {useChartChooser: true, showChartTitle: false}})();
        await flushPromises();
        expect(dispatchChartAdd.mock.calls[0][0].layout).toEqual({});
    });

    test('each chart params entry adds a numbered chart: x/y, histogram of a y or x column, or its own traces', async () => {
        const traces = [{x: 'tables::wave', y: 'tables::err', type: 'scatter'}];
        createChartTableActivate({chartAndTable: true, source: SPECTRUM_URL, titleInfo: 'Spectrum',
            activateParams: ACTIVATE_PARAMS, tbl_id: 'tbl-params', chartId: 'chart-params',
            chartInfo: {xAxis: 'wave', yAxis: 'flux', chartParamsAry: [
                {xAxisColName: 'freq', mode: 'markers', layout: {showlegend: false}},
                {simpleChartType: ChartType.Histogram, yAxisColName: 'flux', numBins: 12},
                {simpleChartType: ChartType.Histogram, xAxisColName: 'wave'},
                {simpleData: false, traces, layout: {title: {text: 'Errors'}}, layoutAdds: false},
            ]}})();
        await flushPromises();
        const [xyChart, histogram, xHistogram, traceChart] = dispatchChartAdd.mock.calls.map(([chart]) => chart);
        expect(xyChart).toEqual({viewerId: 'dp-charts', groupId: 'dp-charts', chartId: 'chart-params--1',
            data: [{tbl_id: 'tbl-params', x: 'tables::freq', y: 'tables::flux', mode: 'markers'}],
            layout: {title: {text: 'Spectrum'}, xaxis: {showgrid: true}, yaxis: {showgrid: true, range: [0, undefined]},
                showlegend: false}});
        expect(histogram.chartId).toBe('chart-params--2');
        expect(histogram.data).toEqual([{type: 'fireflyHistogram', firefly: {tbl_id: 'tbl-params', options:
            {algorithm: 'fixedSizeBins', fixedBinSizeSelection: 'numBins', numBins: 12, columnOrExpr: 'flux'}}}]);
        expect(xHistogram.data[0].firefly.options).toMatchObject({numBins: 30, columnOrExpr: 'wave'});
        expect(traceChart.chartId).toBe('chart-params--4');
        expect(traceChart.data).toEqual([{x: 'tables::wave', y: 'tables::err', type: 'scatter', tbl_id: 'tbl-params'}]);
        expect(traceChart.layout).toEqual({title: {text: 'Errors'}});
    });

    test('the cleanup removes the table and charts and stops watching', async () => {
        const cleanup = createChartTableActivate({chartAndTable: true, source: SPECTRUM_URL, titleInfo: 'Spectrum',
            activateParams: ACTIVATE_PARAMS, tbl_id: 'tbl-cleanup', chartId: 'chart-cleanup',
            chartInfo: {xAxis: 'wave', yAxis: 'flux'}})();
        expect(dispatchAddActionWatcher.mock.calls[0][0].id).toBe('actionWatcherForTable-tbl-cleanup');
        getTblById.mockReturnValue({tbl_id: 'tbl-cleanup'});
        cleanup();
        expect(dispatchCancelActionWatcher).toHaveBeenCalledWith('actionWatcherForTable-tbl-cleanup');
        expect(dispatchTableRemove).toHaveBeenCalledWith('tbl-cleanup', false);
        expect(dispatchChartRemove).toHaveBeenCalledWith('chart-cleanup');
    });

    test('an expanded table, its expanded chart, or a change to its tab component keeps the table when it is left', () => {
        const expandActions = [
            {tbl_id: 'tbl-expanded', type: TBL_UI_EXPANDED, payload: {tbl_id: 'tbl-expanded'}},
            {tbl_id: 'tbl-chart-expanded', type: CHART_UI_EXPANDED, payload: {chartId: 'tbl-chart-expanded-chart'}},
            {tbl_id: 'tbl-tab-changed', type: ComponentCntlr.COMPONENT_STATE_CHANGE, payload: {componentId: 'dp-tabs'}},
        ];
        expandActions.forEach(({tbl_id, type, payload}) => {
            dispatchAddActionWatcher.mockClear();
            const cleanup = createChartTableActivate({chartAndTable: true, source: SPECTRUM_URL, titleInfo: 'Spectrum',
                activateParams: ACTIVATE_PARAMS, tbl_id, chartId: `${tbl_id}-chart`, statefulTabComponentKey: 'dp-tabs',
                chartInfo: {xAxis: 'wave', yAxis: 'flux'}})();
            sendToTableWatcher(type, payload);
            getTblById.mockReturnValue({tbl_id});
            cleanup();
        });
        expect(dispatchTableRemove).not.toHaveBeenCalled();
    });

    test('once the expanded view closes, the table is removed again: the next cleanup is deferred, the one after removes it', () => {
        const cleanup = createChartTableActivate({source: SPECTRUM_URL, titleInfo: 'Spectrum',
            activateParams: ACTIVATE_PARAMS, tbl_id: 'tbl-back-to-standard'})();
        sendToTableWatcher(TBL_UI_EXPANDED, {tbl_id: 'tbl-back-to-standard'});
        sendToTableWatcher('layout.setLayoutMode', {mode: 'standard'});
        getTblById.mockReturnValue({tbl_id: 'tbl-back-to-standard'});
        cleanup();
        expect(dispatchTableRemove).not.toHaveBeenCalled();
        cleanup();
        expect(dispatchTableRemove).toHaveBeenCalledWith('tbl-back-to-standard', false);
    });

    test('a table still loading is not removed, but its watcher is stopped', () => {
        const cleanup = createChartTableActivate({source: SPECTRUM_URL, titleInfo: 'Spectrum',
            activateParams: ACTIVATE_PARAMS, tbl_id: 'tbl-loading'})();
        getTblById.mockReturnValue({tbl_id: 'tbl-loading', isFetching: true});
        cleanup();
        expect(dispatchTableRemove).not.toHaveBeenCalled();
        expect(dispatchCancelActionWatcher).toHaveBeenCalledWith('actionWatcherForTable-tbl-loading');
    });

    test('the charts of a table still loading are not added once the product is left', async () => {
        const cleanup = createChartTableActivate({chartAndTable: true, source: SPECTRUM_URL, titleInfo: 'Spectrum',
            activateParams: ACTIVATE_PARAMS, tbl_id: 'tbl-loading-chart', chartId: 'chart-loading',
            chartInfo: {xAxis: 'wave', yAxis: 'flux'}})();
        getTblById.mockReturnValue({tbl_id: 'tbl-loading-chart', isFetching: true});
        cleanup();
        await flushPromises();
        expect(dispatchChartAdd).not.toHaveBeenCalled();
    });

    test('expanding a different table does not keep this one', () => {
        const cleanup = createChartTableActivate({source: SPECTRUM_URL, titleInfo: 'Spectrum',
            activateParams: ACTIVATE_PARAMS, tbl_id: 'tbl-other-expanded'})();
        sendToTableWatcher(TBL_UI_EXPANDED, {tbl_id: 'some-other-table'});
        getTblById.mockReturnValue({tbl_id: 'tbl-other-expanded'});
        cleanup();
        expect(dispatchTableRemove).toHaveBeenCalledWith('tbl-other-expanded', false);
    });

    test('a kept table is reused for the same source and replaced for a new one', () => {
        const activateParams = {source: SPECTRUM_URL, titleInfo: 'Spectrum', activateParams: ACTIVATE_PARAMS,
            tbl_id: 'tbl-kept'};
        const cleanup = createChartTableActivate(activateParams)();
        sendToTableWatcher(TBL_UI_EXPANDED, {tbl_id: 'tbl-kept'});
        cleanup();
        dispatchTableSearch.mockClear();
        createChartTableActivate(activateParams)();
        expect(dispatchTableSearch).not.toHaveBeenCalled();

        const newSource = 'https://irsa.ipac.caltech.edu/data/example/spectrum2.fits';
        createChartTableActivate({...activateParams, source: newSource})();
        expect(dispatchTableSearch.mock.calls[0][0].source).toBe(newSource);
    });
});

describe('createTableExtraction', () => {
    test('pins a copy of the table to the table area', () => {
        createTableExtraction(SPECTRUM_URL, 'Cube table', 2, {colNames: ['wave']}, 5)();
        const [request, options] = dispatchTableSearch.mock.calls[0];
        expect(request).toMatchObject({source: SPECTRUM_URL, tbl_index: 2, cubePlane: 5});
        expect(request.tbl_id).not.toBe('part-result-tbl');
        expect(request.META_INFO).toEqual({title: 'Cube table', tbl_id: request.tbl_id,
            [MetaConst.IMAGE_AS_TABLE_COL_NAMES]: 'wave'});
        expect(options).toEqual({setAsActive: false, logHistory: false, showFilters: true, showInfoButton: true});
        expect(showPinMessage).toHaveBeenCalledWith('Pinning to Table Area');
        expect(ensureDefaultChart).not.toHaveBeenCalled();
    });

    test('a spectrum or light curve is also pinned as a chart once the table loads', async () => {
        ensureDefaultChart.mockReturnValue('pinned-chart');
        createTableExtraction(SPECTRUM_URL, 'Light curve', 0, undefined, 0, TableDataType.LightCurve)();
        const request = dispatchTableSearch.mock.calls[0][0];
        expect(request.META_INFO[MetaConst.DATA_TYPE_HINT]).toBe(TableDataType.LightCurve);
        expect(showPinMessage).toHaveBeenCalledWith('Pinning to Table Area and Pinned Chart tab');
        await flushPromises();
        expect(ensureDefaultChart).toHaveBeenCalledWith(request.tbl_id);
        expect(pinChart).toHaveBeenCalledWith({chartId: 'pinned-chart', displayPinMessage: false});
    });
});

describe('getExtractionText', () => {
    test('names the pinned chart type for a spectrum or light curve', () => {
        expect(getExtractionText(TableDataType.Spectrum)).toBe('Pin Table/Spectrum');
        expect(getExtractionText(TableDataType.LightCurve)).toBe('Pin Table/LightCurve');
        expect(getExtractionText(TableDataType.NotSpecified)).toBe('Pin Table');
        expect(getExtractionText(undefined)).toBe('Pin Table');
    });
});

describe('extractDatalinkTable', () => {
    const OBSCORE_TABLE = {tbl_id: 'obscore-tbl', request: {UserTargetWorldPt: '10;20;EQ_J2000'}};
    const LINKS_URL = 'https://irsa.ipac.caltech.edu/datalink/links?ID=ivo://irsa/obs-42';

    test('loads the DataLink table of a DataLink-format row, with the search and row positions', async () => {
        const searchTarget = makeWorldPt(10, 20);
        const rowTarget = makeWorldPt(10.5, 20.5);
        isFormatDataLink.mockReturnValue(true);
        getObsCoreAccessURL.mockReturnValue(LINKS_URL);
        getSearchTarget.mockReturnValue(searchTarget);
        getObsCoreSRegion.mockReturnValue('CIRCLE 10 20 0.1');
        makeWorldPtUsingCenterColumns.mockReturnValue(rowTarget);
        getMetaEntry.mockReturnValue('irsa-obscore');

        const result = await extractDatalinkTable(OBSCORE_TABLE, 3, 'Products row 4');
        const [request, options] = dispatchTableSearch.mock.calls[0];
        expect(getObsCoreAccessURL).toHaveBeenCalledWith(OBSCORE_TABLE, 3);
        expect(request.source).toBe(LINKS_URL);
        expect(request.META_INFO).toEqual({title: 'Products row 4', tbl_id: request.tbl_id,
            [MetaConst.SEARCH_TARGET]: searchTarget.toString(), [MetaConst.S_REGION]: 'CIRCLE 10 20 0.1',
            [MetaConst.ROW_TARGET]: rowTarget.toString()});
        expect(options).toEqual({setAsActive: true, logHistory: false, showFilters: true, showInfoButton: true});
        expect(getMetaEntry).toHaveBeenCalledWith('obscore-tbl', MetaConst.DATA_SERVICE_ID);
        expect(result).toEqual({tbl_id: request.tbl_id, serviceId: 'irsa-obscore'});
    });

    test('otherwise uses the first DataLink service descriptor, and can leave the table inactive', async () => {
        const dataLinkServDesc = {accessURL: LINKS_URL};
        isFormatDataLink.mockReturnValue(false);
        getServiceDescriptors.mockReturnValue([{accessURL: 'https://irsa.ipac.caltech.edu/cutout'}, dataLinkServDesc]);
        findDataLinkServeDescs.mockReturnValue([dataLinkServDesc]);
        makeDlUrl.mockReturnValue(LINKS_URL);
        await extractDatalinkTable(OBSCORE_TABLE, 3, 'Products row 4', false);
        expect(makeDlUrl).toHaveBeenCalledWith(dataLinkServDesc, OBSCORE_TABLE, 3);
        const [request, options] = dispatchTableSearch.mock.calls[0];
        expect(request.source).toBe(LINKS_URL);
        expect(request.META_INFO).toEqual({title: 'Products row 4', tbl_id: request.tbl_id});
        expect(options.setAsActive).toBe(false);
    });

    test('does nothing with no service descriptors, no DataLink one, or no URL', () => {
        isFormatDataLink.mockReturnValue(false);
        getServiceDescriptors.mockReturnValue([]);
        expect(extractDatalinkTable(OBSCORE_TABLE, 3, 'Products row 4')).toBeUndefined();
        getServiceDescriptors.mockReturnValue([{accessURL: 'https://irsa.ipac.caltech.edu/cutout'}]);
        findDataLinkServeDescs.mockReturnValue([]);
        expect(extractDatalinkTable(OBSCORE_TABLE, 3, 'Products row 4')).toBeUndefined();
        expect(makeDlUrl).not.toHaveBeenCalled();
        isFormatDataLink.mockReturnValue(true);
        expect(extractDatalinkTable(OBSCORE_TABLE, 3, 'Products row 4')).toBeUndefined();
        expect(dispatchTableSearch).not.toHaveBeenCalled();
    });
});

describe('createChartSingleRowArrayActivate', () => {
    test('loads the table, then charts the array columns of one row', async () => {
        const table = {tbl_id: 'tbl-row-array'};
        const rowArrays = {wave: [1.1, 1.2], flux: [3.5, 3.7]};
        getTblById.mockReturnValue(table);
        getCellValue.mockImplementation((tableModel, row, colName) => rowArrays[colName]);
        createChartSingleRowArrayActivate(SPECTRUM_URL, 'Row spectrum', ACTIVATE_PARAMS,
            {xAxis: 'wave', yAxis: 'flux'}, 2, 1, 'chart-row-array', 'tbl-row-array')();
        const [request, options] = dispatchTableSearch.mock.calls[0];
        expect(request).toMatchObject({source: SPECTRUM_URL, tbl_id: 'tbl-row-array', tbl_index: 1});
        expect(options).toEqual(TABLE_SEARCH_OPTIONS);
        await flushPromises();
        expect(getCellValue).toHaveBeenCalledWith(table, 2, 'wave');
        expect(dispatchChartAdd).toHaveBeenCalledWith({
            viewerId: 'dp-charts', groupId: 'dp-charts', chartId: 'chart-row-array',
            data: [{x: [1.1, 1.2], y: [3.5, 3.7], mode: 'lines+markers'}],
            layout: {title: {text: 'Row spectrum'}, xaxis: {showgrid: true}, yaxis: {showgrid: true, range: [0, undefined]}},
        });
    });

    test('the cleanup removes the chart and the table', () => {
        const cleanup = createChartSingleRowArrayActivate(SPECTRUM_URL, 'Row spectrum', ACTIVATE_PARAMS,
            {xAxis: 'wave', yAxis: 'flux'}, 0, 0, 'chart-row-array', 'tbl-row-array')();
        cleanup();
        expect(dispatchChartRemove).toHaveBeenCalledWith('chart-row-array');
        expect(dispatchTableRemove).toHaveBeenCalledWith('tbl-row-array', false);
    });
});

describe('findGridTableRows', () => {
    test('gives the rows on the page of the highlighted row, and marks it', () => {
        const table = {tbl_id: 'obscore-tbl', totalRows: 10, highlightedRow: 6, request: {}};
        expect(findGridTableRows(table, 4, 'obscore-grid')).toEqual([
            {plotId: 'obscore-grid-row-4', row: 4, highlight: false},
            {plotId: 'obscore-grid-row-5', row: 5, highlight: false},
            {plotId: 'obscore-grid-row-6', row: 6, highlight: true},
            {plotId: 'obscore-grid-row-7', row: 7, highlight: false},
        ]);
    });

    test('the last page may be short', () => {
        const table = {tbl_id: 'obscore-tbl', totalRows: 10, highlightedRow: 9, request: {}};
        expect(findGridTableRows(table, 4, 'obscore-grid').map(({row, highlight}) => [row, highlight]))
            .toEqual([[8, false], [9, true]]);
    });
});

describe('makeMultiTableActivate', () => {
    /**
     * Two spectrum tables, each with its chart; their activate functions return the cleanups passed in.
     * @param {Function} cleanup1
     * @param {Function} cleanup2
     * @return {Object}
     */
    function makeActivateObj(cleanup1, cleanup2) {
        return {
            'tbl-1': {activate: jest.fn(() => cleanup1), chartId: 'chart-1'},
            'tbl-2': {activate: jest.fn(() => cleanup2), chartId: 'chart-2'},
        };
    }

    /**
     * The watcher callback that makeMultiTableActivate added, called with one action.
     * @param {string} type
     * @param {Object} payload
     */
    function sendToMultiTableWatcher(type, payload) {
        dispatchAddActionWatcher.mock.calls[0][0].callback({type, payload});
    }

    // the table active when a multi-table product was last left is kept by the module; this test runs before any is left
    test('activates every table, and none is made active when no product was left before', async () => {
        const activateObj = makeActivateObj(jest.fn(), jest.fn());
        makeMultiTableActivate(activateObj, ACTIVATE_PARAMS)();
        expect(activateObj['tbl-1'].activate).toHaveBeenCalled();
        expect(activateObj['tbl-2'].activate).toHaveBeenCalled();
        expect(dispatchAddActionWatcher.mock.calls[0][0]).toMatchObject(
            {id: 'tbl-1-tbl-2', actions: [UPDATE_VIEWER_CUSTOM_DATA, TBL_RESULTS_ACTIVE]});
        await flushPromises();
        expect(onTableLoaded.mock.calls.map(([tbl_id]) => tbl_id)).toEqual(['tbl-1', 'tbl-2']);
        expect(dispatchActiveTableChanged).not.toHaveBeenCalled();
    });

    test('showing a chart makes its table active', () => {
        makeMultiTableActivate(makeActivateObj(jest.fn(), jest.fn()), ACTIVATE_PARAMS)();
        getActiveTableId.mockReturnValue('tbl-1');
        sendToMultiTableWatcher(UPDATE_VIEWER_CUSTOM_DATA, {customData: {activeItemId: 'chart-1'}});
        sendToMultiTableWatcher(UPDATE_VIEWER_CUSTOM_DATA, {customData: {activeItemId: 'some-other-chart'}});
        expect(dispatchActiveTableChanged).not.toHaveBeenCalled();
        sendToMultiTableWatcher(UPDATE_VIEWER_CUSTOM_DATA, {customData: {activeItemId: 'chart-2'}});
        expect(dispatchActiveTableChanged).toHaveBeenCalledWith('tbl-2', 'dp-tables');
    });

    test('making a table active in the group shows its chart', () => {
        makeMultiTableActivate(makeActivateObj(jest.fn(), jest.fn()), ACTIVATE_PARAMS)();
        getViewer.mockReturnValue({customData: {activeItemId: 'chart-2'}});
        sendToMultiTableWatcher(TBL_RESULTS_ACTIVE, {tbl_group: 'dp-tables', tbl_id: 'tbl-2'});
        sendToMultiTableWatcher(TBL_RESULTS_ACTIVE, {tbl_group: 'main', tbl_id: 'tbl-1'});
        expect(dispatchUpdateCustom).not.toHaveBeenCalled();
        sendToMultiTableWatcher(TBL_RESULTS_ACTIVE, {tbl_group: 'dp-tables', tbl_id: 'tbl-1'});
        expect(dispatchUpdateCustom).toHaveBeenCalledWith('dp-charts', {activeItemId: 'chart-1'});
    });

    test('making active a table that is not one of this product\'s leaves the chart alone', () => {
        makeMultiTableActivate(makeActivateObj(jest.fn(), jest.fn()), ACTIVATE_PARAMS)();
        getViewer.mockReturnValue({customData: {activeItemId: 'chart-2'}});
        sendToMultiTableWatcher(TBL_RESULTS_ACTIVE, {tbl_group: 'dp-tables', tbl_id: 'tbl-other'});
        expect(dispatchUpdateCustom).not.toHaveBeenCalled();
    });

    test('a chart viewer with no custom data yet still gets the chart shown', () => {
        makeMultiTableActivate(makeActivateObj(jest.fn(), jest.fn()), ACTIVATE_PARAMS)();
        getViewer.mockReturnValue({});
        sendToMultiTableWatcher(TBL_RESULTS_ACTIVE, {tbl_group: 'dp-tables', tbl_id: 'tbl-1'});
        expect(dispatchUpdateCustom).toHaveBeenCalledWith('dp-charts', {activeItemId: 'chart-1'});
    });

    test('the cleanup cleans up each table and the next activate makes the last active table active again', async () => {
        const cleanup1 = jest.fn();
        const cleanup2 = jest.fn();
        const activateObj = makeActivateObj(cleanup1, cleanup2);
        const cleanup = makeMultiTableActivate(activateObj, ACTIVATE_PARAMS)();
        getActiveTableId.mockReturnValue('tbl-2');
        getTblById.mockReturnValue({tbl_id: 'tbl-2'});
        cleanup();
        expect(dispatchCancelActionWatcher).toHaveBeenCalledWith('tbl-1-tbl-2');
        expect(cleanup1).toHaveBeenCalled();
        expect(cleanup2).toHaveBeenCalled();

        makeMultiTableActivate(activateObj, ACTIVATE_PARAMS)();
        await flushPromises();
        expect(dispatchActiveTableChanged).toHaveBeenCalledWith('tbl-2', 'dp-tables');
    });

    // runs after the test above, so the last active table kept by the module is tbl-2
    test('the last active table is not made active by a product that does not have it', async () => {
        makeMultiTableActivate({'tbl-3': {activate: jest.fn(), chartId: 'chart-3'}}, ACTIVATE_PARAMS)();
        await flushPromises();
        expect(dispatchActiveTableChanged).not.toHaveBeenCalled();
    });
});

describe('makeMultiTableExtraction', () => {
    test('pins the active table, or nothing when no table of the product is active', () => {
        const extractionObj = {'tbl-1': jest.fn(), 'tbl-2': jest.fn()};
        getActiveTableId.mockReturnValue('tbl-2');
        makeMultiTableExtraction(extractionObj, ACTIVATE_PARAMS)();
        expect(getActiveTableId).toHaveBeenCalledWith('dp-tables');
        expect(extractionObj['tbl-2']).toHaveBeenCalled();
        expect(extractionObj['tbl-1']).not.toHaveBeenCalled();
        getActiveTableId.mockReturnValue('some-other-table');
        makeMultiTableExtraction(extractionObj, ACTIVATE_PARAMS)();
        expect(extractionObj['tbl-2']).toHaveBeenCalledTimes(1);
    });
});
