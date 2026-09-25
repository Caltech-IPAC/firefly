import {getKeysForOrder, getValuesForOrder} from '../HpxIndexCntlr.js';


describe('HpxIndexCntlr', () => {
    const orderData = {5: {norder: 5, tiles: [new Map([[1, 'a'], [2, 'b']])]}};

    test('getValuesForOrder and getKeysForOrder return the tile entries of an order', () => {
        expect(getValuesForOrder(orderData, 5)).toEqual(['a', 'b']);
        expect(getKeysForOrder(orderData, 5)).toEqual([1, 2]);
    });

    test('getValuesForOrder and getKeysForOrder return an empty array for a missing order', () => {
        expect(getValuesForOrder(orderData, 1)).toEqual([]);
        expect(getKeysForOrder(orderData, 1)).toEqual([]);
    });

    test('getKeysForOrder returns an empty array without orderData', () => {
        expect(getKeysForOrder(undefined, 4)).toEqual([]);
    });
});
