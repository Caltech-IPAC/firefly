/*eslint-env node, jest */
import {flux} from 'firefly/core/ReduxFlux.js';
import {
    CHART_ADD, CHART_MOUNTED, CHART_REMOVE, CHART_TRACE_REMOVE, CHART_UNMOUNTED, CHART_UPDATE, reducer
} from 'firefly/charts/ChartsCntlr.js';

// default chart ids are 'default-' + tbl_id, and tbl_id can contain a period
const DOTTED_CHART_ID = 'default-2mass.catalog';

describe('ChartsCntlr reducer with a chart id containing a period', () => {
    const addedState = reducer(undefined,
        {type: CHART_ADD, payload: {chartId: DOTTED_CHART_ID, chartType: 'plot.ly', groupId: 'main'}});

    test('CHART_ADD stores the chart under the whole id, not a nested path', () => {
        expect(addedState.data[DOTTED_CHART_ID]?.chartId).toBe(DOTTED_CHART_ID);
        expect(addedState.data['default-2mass']).toBeUndefined();
    });

    test('CHART_MOUNTED and CHART_UNMOUNTED update the mounted count of that chart', () => {
        const mountedState = reducer(addedState, {type: CHART_MOUNTED, payload: {chartId: DOTTED_CHART_ID}});
        expect(mountedState.data[DOTTED_CHART_ID].mounted).toBe(1);

        const unmountedState = reducer(mountedState, {type: CHART_UNMOUNTED, payload: {chartId: DOTTED_CHART_ID}});
        expect(unmountedState.data[DOTTED_CHART_ID].mounted).toBe(0);
    });

    test('CHART_REMOVE removes the chart', () => {
        const removedState = reducer(addedState, {type: CHART_REMOVE, payload: {chartId: DOTTED_CHART_ID}});
        expect(removedState.data).toEqual({});
    });
});

// CHART_UPDATE and CHART_TRACE_REMOVE read the current chart from the store (getChartData), not from the reducer state
describe('ChartsCntlr reducer with a chart id containing a period, reading chart data from the store', () => {
    let storeChartState;
    beforeEach(() => jest.spyOn(flux, 'getState').mockImplementation(() => ({charts: storeChartState})));
    afterEach(() => jest.restoreAllMocks());

    test('CHART_UPDATE applies changes to that chart', () => {
        storeChartState = reducer(undefined,
            {type: CHART_ADD, payload: {chartId: DOTTED_CHART_ID, chartType: 'plot.ly', groupId: 'main'}});

        const updatedState = reducer(storeChartState,
            {type: CHART_UPDATE, payload: {chartId: DOTTED_CHART_ID, changes: {'layout.title.text': 'J vs H-K'}}});
        expect(updatedState.data[DOTTED_CHART_ID].layout.title.text).toBe('J vs H-K');
        expect(updatedState.data['default-2mass']).toBeUndefined();
    });

    test('CHART_TRACE_REMOVE removes the trace from that chart', () => {
        const twoTraces = [{name: 'trace0', type: 'scatter'}, {name: 'trace1', type: 'scatter'}];
        storeChartState = reducer(undefined,
            {type: CHART_ADD, payload: {chartId: DOTTED_CHART_ID, chartType: 'plot.ly', groupId: 'main', data: twoTraces}});

        const traceRemovedState = reducer(storeChartState,
            {type: CHART_TRACE_REMOVE, payload: {chartId: DOTTED_CHART_ID, traceNum: 0}});
        expect(traceRemovedState.data[DOTTED_CHART_ID].data.map((trace) => trace.name)).toEqual(['trace1']);
        expect(traceRemovedState.data['default-2mass']).toBeUndefined();
    });
});
