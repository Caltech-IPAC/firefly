import {parseSpacialHeaderInfo} from '../ProjectionHeaderParser.js';

describe('ProjectionHeaderParser PC Matrix Tests', () => {
    test('should parse 1996 PC matrix keywords', () => {
        const header = {
            'SIMPLE': {value: 'T', comment: 'file does conform to FITS standard'},
            'BITPIX': {value: '-32', comment: 'number of bits per data pixel'},
            'NAXIS': {value: '2', comment: 'number of data axes'},
            'NAXIS1': {value: '100', comment: 'length of data axis 1'},
            'NAXIS2': {value: '100', comment: 'length of data axis 2'},
            'CRPIX1': {value: '50.0', comment: 'reference pixel on axis 1'},
            'CRPIX2': {value: '50.0', comment: 'reference pixel on axis 2'},
            'CRVAL1': {value: '180.0', comment: 'coordinate 1 value at reference pixel'},
            'CRVAL2': {value: '0.0', comment: 'coordinate 2 value at reference pixel'},
            'CDELT1': {value: '0.1', comment: 'coordinate 1 increment at reference pixel'},
            'CDELT2': {value: '0.1', comment: 'coordinate 2 increment at reference pixel'},
            'CTYPE1': {value: 'RA---TAN', comment: 'coordinate type/projection for axis 1'},
            'CTYPE2': {value: 'DEC--TAN', comment: 'coordinate type/projection for axis 2'},
            // 1996 format PC matrix keywords
            'PC001001': {value: '-1.0', comment: 'PC matrix element'},
            'PC001002': {value: '0.0', comment: 'PC matrix element'},
            'PC002001': {value: '0.0', comment: 'PC matrix element'},
            'PC002002': {value: '1.0', comment: 'PC matrix element'}
        };

        const params = parseSpacialHeaderInfo(header);

        // Verify that PC matrix values were properly converted to CD matrix
        // cd1_1 = cdelt1 * pc1_1 = 0.1 * -1.0 = -0.1
        expect(params.cd1_1).toBeCloseTo(-0.1, 10);
        expect(params.cd1_2).toBeCloseTo(0.0, 10);
        expect(params.cd2_1).toBeCloseTo(0.0, 10);
        expect(params.cd2_2).toBeCloseTo(0.1, 10);
        expect(params.using_cd).toBe(true);
    });
});
