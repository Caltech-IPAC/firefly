/*eslint-env node, jest */

// PlotlyToolbar pulls in canvas-based UI that jsdom can't load; BasicOptions only needs ActiveTraceSelect from it
jest.mock('firefly/charts/ui/PlotlyToolbar.jsx', () => ({ActiveTraceSelect: () => null}));
jest.mock('firefly/charts/ChartsCntlr.js', () => ({
    ...jest.requireActual('firefly/charts/ChartsCntlr.js'),
    getChartData: jest.fn(),
}));

import {getChartData} from 'firefly/charts/ChartsCntlr.js';
import {evalChangesFromFields} from 'firefly/charts/ui/options/BasicOptions.jsx';
import {evalAxisRangeChanges} from 'firefly/charts/AxisRangeUtil.js';

/**
 * Pick the axis range related keys present in the changes, distinguishing "not changed" (key absent) from "set to undefined"
 * @param {object} changes
 * @param {string} a - axis: 'x' or 'y'
 * @returns {object}
 */
function axisKeys(changes, a) {
    const out = {};
    ['range', 'autorange', 'type'].forEach((p) => {
        const k = `layout.${a}axis.${p}`;
        if (k in changes) out[p] = changes[k];
    });
    return out;
}

/**
 * Run evalChangesFromFields against a chart with the given stored layout and pick out its axis range keys
 * @param {object} layout - chart layout in the store (as synced from plotly)
 * @param {object} fields - options dialog fields
 * @returns {{x: object, y: object, changes: object}} per-axis range keys, plus all changes
 */
function evalChangesFromFieldsAxes(layout, fields) {
    getChartData.mockReturnValue({layout, data: []});
    const changes = evalChangesFromFields('test-chart', undefined, fields);
    return {x: axisKeys(changes, 'x'), y: axisKeys(changes, 'y'), changes};
}

const AUTO = {range: [1, 9], autorange: true};          // initial chart: plotly-computed range synced into the store
const FIXED = {range: [1, 9], autorange: false};        // e.g. after a previous Apply with bounds
const REV = {range: [9, 1], autorange: false};          // reversed
const ZOOMED = {range: [3, 4], autorange: false};       // after a zoom
const LOG = {type: 'log', range: [0, 1], autorange: true};  // log axis showing 1..10 (range is in log10 units)

const bounds = (min, max) => ({
    ...(min !== undefined && {'fireflyLayout.yaxis.min': `${min}`}),
    ...(max !== undefined && {'fireflyLayout.yaxis.max': `${max}`}),
});

describe('evalChangesFromFields: axis range changes', () => {

    describe('reverse without bounds', () => {
        test('unchanged direction keeps the range and leaves autorange alone', () => {
            expect(evalChangesFromFieldsAxes({yaxis: AUTO}, {__yoptions: ''}).y).toEqual({range: [1, 9]});
            expect(evalChangesFromFieldsAxes({yaxis: REV}, {__yoptions: 'flip'}).y).toEqual({range: [9, 1]});
        });
        test('reverse swaps the kept range and fixes it', () => {
            expect(evalChangesFromFieldsAxes({yaxis: AUTO}, {__yoptions: 'flip'}).y)
                .toEqual({range: [9, 1], autorange: false});
        });
        test('un-reverse swaps the kept range back', () => {
            expect(evalChangesFromFieldsAxes({yaxis: REV}, {__yoptions: ''}).y)
                .toEqual({range: [1, 9], autorange: false});
        });
        test('no stored range uses autorange', () => {
            expect(evalChangesFromFieldsAxes({}, {__yoptions: ''}).y).toEqual({autorange: true, range: undefined});
            expect(evalChangesFromFieldsAxes({}, {__yoptions: 'flip'}).y).toEqual({autorange: 'reversed', range: undefined});
        });
        test('zoomed range is kept when applying unrelated options', () => {
            expect(evalChangesFromFieldsAxes({yaxis: ZOOMED}, {__yoptions: ''}).y).toEqual({range: [3, 4]});
        });
        test('clearing bounds keeps the range they set (indistinguishable from a zoom)', () => {
            const fromBounds = {range: [2, 8], autorange: false};
            expect(evalChangesFromFieldsAxes({yaxis: fromBounds}, {__yoptions: ''}).y).toEqual({range: [2, 8]});
        });
    });

    describe('bounds', () => {
        test('both bounds, other axis untouched', () => {
            const {x, y} = evalChangesFromFieldsAxes({xaxis: FIXED, yaxis: FIXED}, {__yoptions: '', ...bounds(2, 8)});
            expect(y).toEqual({range: [2, 8], autorange: false});
            expect(x).toEqual({});
        });
        test('reverse with bounds (FIREFLY-2103)', () => {
            expect(evalChangesFromFieldsAxes({yaxis: FIXED}, {__yoptions: 'flip', ...bounds(2, 8)}).y)
                .toEqual({range: [8, 2], autorange: false});
        });
        test('un-reverse with bounds (FIREFLY-2103)', () => {
            expect(evalChangesFromFieldsAxes({yaxis: REV}, {__yoptions: '', ...bounds(2, 8)}).y)
                .toEqual({range: [2, 8], autorange: false});
        });
        test('missing bound is filled from the current range', () => {
            expect(evalChangesFromFieldsAxes({yaxis: FIXED}, {__yoptions: '', ...bounds(2)}).y)
                .toEqual({range: [2, 9], autorange: false});
            expect(evalChangesFromFieldsAxes({yaxis: FIXED}, {__yoptions: 'flip', ...bounds(undefined, 8)}).y)
                .toEqual({range: [8, 1], autorange: false});
        });
        test('bounds are kept in fireflyLayout (used by heatmap binning and the dialog)', () => {
            const {changes} = evalChangesFromFieldsAxes({yaxis: FIXED}, {__yoptions: '', ...bounds(2, 8)});
            expect(changes['fireflyLayout.yaxis.min']).toBe('2');
            expect(changes['fireflyLayout.yaxis.max']).toBe('8');
        });
    });

    describe('axis column change (reset)', () => {
        test('no bounds: range is dropped for autorange in the requested direction', () => {
            expect(evalChangesFromFieldsAxes({yaxis: REV}, {__yoptions: '', __yreset: 'true'}).y)
                .toEqual({autorange: true, range: undefined});
            expect(evalChangesFromFieldsAxes({yaxis: FIXED}, {__yoptions: 'flip', __yreset: 'true'}).y)
                .toEqual({autorange: 'reversed', range: undefined});
        });
        test('bounds: direction comes from the reverse option, not the old range', () => {
            expect(evalChangesFromFieldsAxes({yaxis: REV}, {__yoptions: '', __yreset: 'true', ...bounds(2, 8)}).y)
                .toEqual({range: [2, 8], autorange: false});
            expect(evalChangesFromFieldsAxes({yaxis: FIXED}, {__yoptions: 'flip', __yreset: 'true', ...bounds(2, 8)}).y)
                .toEqual({range: [8, 2], autorange: false});
        });
        test('a single bound with nothing to fill the other from, autoscales in the requested direction', () => {
            expect(evalChangesFromFieldsAxes({yaxis: FIXED}, {__yoptions: '', __yreset: 'true', ...bounds(2, undefined)}).y)
                .toEqual({autorange: true, range: undefined});
            expect(evalChangesFromFieldsAxes({yaxis: FIXED}, {__yoptions: 'flip', __yreset: 'true', ...bounds(undefined, 8)}).y)
                .toEqual({autorange: 'reversed', range: undefined});
        });
        test('both axes reset together (histogram column change)', () => {
            const {x, y} = evalChangesFromFieldsAxes({xaxis: FIXED, yaxis: REV},
                {__xoptions: '', __yoptions: '', __xreset: 'true', __yreset: 'true'});
            expect(x).toEqual({autorange: true, range: undefined});
            expect(y).toEqual({autorange: true, range: undefined});
        });
    });

    describe('log scale', () => {
        test('linear to log converts the current range to log10 units', () => {
            const c = evalChangesFromFieldsAxes({yaxis: AUTO}, {__yoptions: 'log'}).y;
            expect(c.type).toBe('log');
            expect(c.autorange).toBe(false);
            expect(c.range[0]).toBeCloseTo(0);
            expect(c.range[1]).toBeCloseTo(Math.log10(9));
        });
        test('log to linear converts the current range back to data units', () => {
            const c = evalChangesFromFieldsAxes({yaxis: LOG}, {__yoptions: ''}).y;
            expect(c.type).toBe('linear');
            expect(c.autorange).toBe(false);
            expect(c.range[0]).toBeCloseTo(1);
            expect(c.range[1]).toBeCloseTo(10);
        });
        test('scale switch keeps a reversed axis reversed', () => {
            const c = evalChangesFromFieldsAxes({yaxis: REV}, {__yoptions: 'flip,log'}).y;
            expect(c.autorange).toBe(false);
            expect(c.range[0]).toBeCloseTo(Math.log10(9));
            expect(c.range[1]).toBeCloseTo(0);
        });
        test('log, reverse and bounds together', () => {
            const c = evalChangesFromFieldsAxes({yaxis: FIXED}, {__yoptions: 'flip,log', ...bounds(2, 8)}).y;
            expect(c.range[0]).toBeCloseTo(Math.log10(8));
            expect(c.range[1]).toBeCloseTo(Math.log10(2));
        });
        test('missing bound is filled from a log range in data units', () => {
            const c = evalChangesFromFieldsAxes({yaxis: LOG}, {__yoptions: 'log', ...bounds(2)}).y;
            expect(c.range[0]).toBeCloseTo(Math.log10(2));
            expect(c.range[1]).toBeCloseTo(1);
        });
        test('staying on log keeps the range and leaves autorange alone', () => {
            // used to rebuild the range and set autorange false on every Apply, freezing an autoscaled log axis
            expect(evalChangesFromFieldsAxes({yaxis: LOG}, {__yoptions: 'log'}).y).toEqual({type: 'log', range: [0, 1]});
        });
        test('linear to log with a range reaching 0 or below falls back to autorange', () => {
            // used to produce a NaN range
            expect(evalChangesFromFieldsAxes({yaxis: {range: [-5, 9], autorange: true}}, {__yoptions: 'log'}).y)
                .toEqual({type: 'log', autorange: true, range: undefined});
            expect(evalChangesFromFieldsAxes({yaxis: {range: [-5, 9], autorange: true}}, {__yoptions: 'log,flip'}).y)
                .toEqual({type: 'log', autorange: 'reversed', range: undefined});
        });
    });

    describe('fields without the axis options (e.g. spectrum unit conversion)', () => {
        test('no axis range fields leave the range alone', () => {
            expect(evalChangesFromFieldsAxes({yaxis: REV}, {'layout.yaxis.title.text': 'flux'}).y).toEqual({});
        });
        test('bounds without the reverse option keep the current direction', () => {
            expect(evalChangesFromFieldsAxes({yaxis: REV}, bounds(2, 8)).y).toEqual({range: [8, 2], autorange: false});
            expect(evalChangesFromFieldsAxes({yaxis: FIXED}, bounds(2, 8)).y).toEqual({range: [2, 8], autorange: false});
        });
    });

    test('x axis behaves the same', () => {
        const c = evalChangesFromFieldsAxes({xaxis: FIXED},
            {__xoptions: 'flip', 'fireflyLayout.xaxis.min': '2', 'fireflyLayout.xaxis.max': '8'}).x;
        expect(c).toEqual({range: [8, 2], autorange: false});
    });
});

describe('evalAxisRangeChanges', () => {
    test('reset without axis options or bounds leaves the axis alone', () => {
        expect(evalAxisRangeChanges({a: 'y', fields: {__yreset: 'true'}, axisLayout: REV})).toEqual({});
    });
    test('axis options key present with no value means nothing checked', () => {
        expect(evalAxisRangeChanges({a: 'y', fields: {__yoptions: undefined}, axisLayout: REV}))
            .toEqual({'layout.yaxis.range': [1, 9], 'layout.yaxis.autorange': false});
    });
    test('bounds without axis options use the current scale', () => {
        const c = evalAxisRangeChanges({a: 'y', fields: bounds(2, 8), axisLayout: LOG});
        expect(c['layout.yaxis.range'][0]).toBeCloseTo(Math.log10(2));
        expect(c['layout.yaxis.range'][1]).toBeCloseTo(Math.log10(8));
    });
    test('no axis layout at all', () => {
        expect(evalAxisRangeChanges({a: 'y', fields: {__yoptions: 'flip'}}))
            .toEqual({'layout.yaxis.autorange': 'reversed', 'layout.yaxis.range': undefined});
    });
});
