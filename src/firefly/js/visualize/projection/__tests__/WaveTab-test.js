import assert from 'assert';
import {searchIndexRange, calculateY_m, calculateC_m, tabInterpolateMultiD} from '../Wavelength.js';

const indexVecUp = [50, 100, 100, 200]; // monotonously increasing
const indexVecDown = [200, 100, 100, 50]; // monotonously decreasing

test('Test searchIndexRange',   ()=> {
    const input = [25, 50, 75, 100, 150, 200, 225];
    const expectedAsc = [[0,1], [0,1], [0,1], [0,1], [2,3], [2,3], [2,3]];
    const expectedDesc = [[2,3], [2,3], [2,3], [0,1], [0,1], [0,1], [0,1]];

    input.forEach((x, i)  => {
        const actual = searchIndexRange(indexVecUp, x);
        assert.ok(expectedAsc[i].every((val, idx) => val === actual[idx]),'searchIndexRange asc ' + i);
    });

    input.forEach((x, i)  => {
        const actual = searchIndexRange(indexVecDown, x);
        assert.ok(expectedDesc[i].every((val, idx) => val === actual[idx]), 'searchIndex desc ' + i);
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

    input.forEach((u_m, i)  => {
        const val = calculateC_m(u_m, indexVecUp, 4);
        assert.equal(val, expectedAsc[i], 'calculateC_m asc '+i);
    });

    input.forEach((u_m, i)  => {
        const val = calculateC_m(u_m, indexVecDown, 4);
        assert.equal(val, expectedDesc[i], 'calculateC_m desc '+i);
    });
});

test('Test tabInterpolateMultiD',   ()=> {

    // 5x5 image
    const indexDataArr = [[1,3,5], [1,5]];
    const coordData = [[[200,2000],[160,1600],[200,2000]], [[100,1000],[60,600],[100,1000]]];
    const expectedVals = [
        [[200,2000],[180,1800],[160,1600],[180,1800],[200,2000]],
        [[175,1750],[155,1550],[135,1350],[155,1550],[175,1750]],
        [[150,1500],[130,1300],[110,1100],[130,1300],[150,1500]],
        [[125,1250],[105,1050],[ 85, 850],[105,1050],[125,1250]],
        [[100,1000],[ 80, 800],[ 60, 600],[ 80, 800],[100,1000]],
    ];

    for (let row = 0; row < 5; row++) {
        for (let col = 0; col < 5; col++) {
            const psi_m = [col+1, row+1];  // FITS image pixels
            const val = tabInterpolateMultiD(indexDataArr, coordData, psi_m);
            assert.deepEqual(val, expectedVals[row][col], `tabInterpolateMultiD psi_m=${psi_m}`);
        }
    }
});