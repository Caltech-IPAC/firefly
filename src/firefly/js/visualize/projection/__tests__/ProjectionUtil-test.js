import {wrapLngDeg} from '../ProjectionUtil.js';

describe('Helpers', () => {
    test('Wrap celestial longitude to [0, 360)', () => {
        expect(wrapLngDeg(0)).toBe(0);
        expect(wrapLngDeg(360)).toBe(0);
        expect(wrapLngDeg(-0)).toBe(0);
        expect(wrapLngDeg(-1)).toBeCloseTo(359);
        expect(wrapLngDeg(721)).toBe(1);
    });
});