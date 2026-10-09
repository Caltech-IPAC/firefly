/*eslint-env node, jest */
import {formatColExpr, makeShapeHoverTrace, makeShapesAutorangeLayout} from 'firefly/charts/ChartUtil.js';


describe('ChartUtil', () => {
    test('formatColExpr does not treat a digit as the edge of a column name', () => {
        const colNames = ['val', 'val2'];
        expect(formatColExpr({colOrExpr: 'val + val2', quoted: true, colNames})).toBe('"val"+"val2"');
        expect(formatColExpr({colOrExpr: 'val2 + val', quoted: true, colNames})).toBe('"val2"+"val"');

        // the digits around e belong to the number 1e5
        expect(formatColExpr({colOrExpr: '1e5*e', quoted: true, colNames: ['e']})).toBe('1e5*"e"');
    });
});

const vLine = (xPos, extra={}) => ({type: 'line', x0: xPos, x1: xPos, y0: 0, y1: 1, xref: 'x', yref: 'paper', ...extra});

describe('makeShapeHoverTrace', () => {
    test('builds a hover trace only for vertical paper-y lines with hovertext', () => {
        const shapes = [
            vLine(5, {hovertext: 'a'}),
            vLine(6), // no hovertext
            {type: 'line', x0: 1, x1: 2, y0: 0, y1: 1, xref: 'x', yref: 'paper', hovertext: 'slanted'},
        ];
        const {traces} = makeShapeHoverTrace(shapes);
        expect(traces).toHaveLength(1);
        expect(traces[0].x.filter((xVal) => xVal !== null)).toEqual(Array(10).fill(5));
    });
});

describe('makeShapesAutorangeLayout', () => {
    const data = [{x: [10, 20]}];
    const noClip = {xaxis: {autorangeoptions: {clipmin: null, clipmax: null}}};

    test('returns {} for a chart without shapes', () => {
        expect(makeShapesAutorangeLayout(data, {})).toEqual({});
        expect(makeShapesAutorangeLayout(data, {xaxis: {type: 'log'}})).toEqual({});
    });

    test('returns null bounds (to clear a previous clip) without vertical paper-y line shapes', () => {
        expect(makeShapesAutorangeLayout(data, {shapes: []})).toEqual(noClip);
        expect(makeShapesAutorangeLayout(data, {shapes: [{type: 'line', x0: 0, x1: 100, y0: 0.5, y1: 0.5, xref: 'x', yref: 'paper'}]})).toEqual(noClip);
        expect(makeShapesAutorangeLayout(data, {shapes: [{type: 'rect', x0: 0, x1: 100, y0: 0, y1: 1, xref: 'x', yref: 'paper'}]})).toEqual(noClip);
        expect(makeShapesAutorangeLayout(data, {shapes: [vLine(0.5, {xref: 'paper'})]})).toEqual(noClip);
    });

    test('clips to the padded data x extent across traces, with or without hovertext on shapes', () => {
        expect(makeShapesAutorangeLayout([{x: [12, 20]}, {x: [10, 15]}], {shapes: [vLine(500)]}))
            .toEqual({xaxis: {autorangeoptions: {clipmin: 9.5, clipmax: 20.5}}});
        expect(makeShapesAutorangeLayout(data, {shapes: [vLine(500, {hovertext: 'a'})]}))
            .toEqual({xaxis: {autorangeoptions: {clipmin: 9.5, clipmax: 20.5}}});
    });

    test('widens the extent by x error bars', () => {
        const symmetric = [{x: [10, 20], error_x: {array: [1, 2]}}];
        expect(makeShapesAutorangeLayout(symmetric, {shapes: [vLine(500)]}))  // extent [9, 22], pad 0.65
            .toEqual({xaxis: {autorangeoptions: {clipmin: expect.closeTo(8.35), clipmax: expect.closeTo(22.65)}}});

        const asymmetric = [{x: [10, 20], error_x: {symmetric: false, array: [0, 4], arrayminus: [4, 0]}}];
        expect(makeShapesAutorangeLayout(asymmetric, {shapes: [vLine(500)]}))  // extent [6, 24], pad 0.9
            .toEqual({xaxis: {autorangeoptions: {clipmin: expect.closeTo(5.1), clipmax: expect.closeTo(24.9)}}});

        const hiddenErr = [{x: [10, 20], error_x: {visible: false, array: [5, 5]}}];
        expect(makeShapesAutorangeLayout(hiddenErr, {shapes: [vLine(500)]}))
            .toEqual({xaxis: {autorangeoptions: {clipmin: 9.5, clipmax: 20.5}}});
    });

    test('ignores hidden traces and non-finite x values', () => {
        const traces = [{x: [10, NaN, null, 20]}, {x: [-100, 100], visible: 'legendonly'}, {x: [-100, 100], visible: false}];
        expect(makeShapesAutorangeLayout(traces, {shapes: [vLine(500)]}))
            .toEqual({xaxis: {autorangeoptions: {clipmin: 9.5, clipmax: 20.5}}});
    });

    test('pads in log space and skips x <= 0 on a log axis', () => {
        const {clipmin, clipmax} = makeShapesAutorangeLayout([{x: [-1, 0, 1, 100]}], {shapes: [vLine(5000)], xaxis: {type: 'log'}})
            .xaxis.autorangeoptions;
        expect(Math.log10(clipmin)).toBeCloseTo(-0.1);
        expect(Math.log10(clipmax)).toBeCloseTo(2.1);
    });

    test('returns null bounds when data has no finite x extent of non-zero width', () => {
        expect(makeShapesAutorangeLayout([{x: [NaN, null]}], {shapes: [vLine(500)]})).toEqual(noClip);
        expect(makeShapesAutorangeLayout([{x: [7, 7]}], {shapes: [vLine(500)]})).toEqual(noClip);
        expect(makeShapesAutorangeLayout([], {shapes: [vLine(500)]})).toEqual(noClip);
    });
});
