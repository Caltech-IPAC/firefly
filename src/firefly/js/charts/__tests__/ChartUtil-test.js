/*eslint-env node, jest */
import {formatColExpr} from 'firefly/charts/ChartUtil.js';


describe('ChartUtil', () => {
    test('formatColExpr treats digits as part of a column name', () => {
        const colNames = ['val', 'val2'];
        expect(formatColExpr({colOrExpr: 'val + val2', quoted: true, colNames})).toBe('"val"+"val2"');
        expect(formatColExpr({colOrExpr: 'val2 + val', quoted: true, colNames})).toBe('"val2"+"val"');

        // the digits around e belong to the number 1e5
        expect(formatColExpr({colOrExpr: '1e5*e', quoted: true, colNames: ['e']})).toBe('1e5*"e"');
    });
});
