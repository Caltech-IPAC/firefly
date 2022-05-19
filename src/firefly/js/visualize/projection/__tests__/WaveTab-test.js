import assert from 'assert';
import {searchIndex, calculateY_m, calculateC_m} from '../Wavelength.js';

const indexVecUp = [50, 100, 100, 200]; // monotonously increasing
const indexVecDown = [200, 100, 100, 50]; // monotonously decreasing

test('Test searchIndex',   ()=> {
    const input = [25, 50, 75, 100, 150, 200, 225];
    const expectedAsc = [-1, 0, 0, 0, 2, 2, -1];
    const expectedDesc = [-1, 2, 2, 0, 0, 0, -1];

    input.forEach((x, i)  => {
        const val = searchIndex(indexVecUp, x, 1);
        assert.equal(val, expectedAsc[i], 'searchIndex asc ' + i);
    });

    input.forEach((x, i)  => {
        const val = searchIndex(indexVecDown, x, -1);
        assert.equal(val, expectedDesc[i], 'searchIndex desc ' + i);
    });
});

test('Test calculateY_m',   ()=> {
    const input = [24, 25, 50, 75, 100, 150, 200, 250, 251];
    const expectedAsc = [undefined, 0.5, 1, 1.5, 2, 3.5, 4, 4.5, undefined];
    const expectedDesc = [undefined, 4.5, 4, 3.5, 2, 1.5, 1, 0.5, undefined];

    input.forEach((x, i)  => {
        const val = calculateY_m(indexVecUp, x);
        assert.equal(val, expectedAsc[i], 'calculateY_m asc ' + i);
    });

    input.forEach((x, i)  => {
        const val = calculateY_m(indexVecDown, x);
        assert.equal(val, expectedDesc[i], 'calculateY_m desc ' + i);
    });
});

test('Test calculateC_m',   ()=> {
    const input = [0.4, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 4.6];
    const expectedAsc = [undefined, 25, 50, 75, 100, 100, 100, 150, 200, 250, undefined];
    const expectedDesc = [undefined, 250, 200, 150, 100, 100, 100, 75, 50, 25, undefined];

    input.forEach((x, i)  => {
        const val = calculateC_m(x, indexVecUp, 4);
        assert.equal(val, expectedAsc[i], 'calculateC_m asc '+i);
    });

    input.forEach((x, i)  => {
        const val = calculateC_m(x, indexVecDown, 4);
        assert.equal(val, expectedDesc[i], 'calculateC_m desc '+i);
    });
});