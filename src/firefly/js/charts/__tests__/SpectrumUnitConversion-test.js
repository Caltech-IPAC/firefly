/*eslint-env node, mocha */
import {canUnitConv} from 'firefly/charts/dataTypes/SpectrumUnitConversion';

describe('Tests for Spectrum Unit Conversion', () => {

    test('canUnitConv', () => {
        expect(canUnitConv({from: 'Hz', to: 'A'})).toBe(false);
        expect(canUnitConv({from: 'Hz', to: 'GHz'})).toBe(true);
    });

    // TODO: write tests for all exported functions?
    test('getUnitConvExpr', () => {

    });
});

