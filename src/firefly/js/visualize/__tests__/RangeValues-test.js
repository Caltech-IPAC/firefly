import RangeValues, {
    ABSOLUTE, PERCENTAGE, SIGMA, STRETCH_ASINH, STRETCH_LINEAR, STRETCH_LOG, STRETCH_LOGLOG, STRETCH_POWERLAW_GAMMA,
    STRETCH_SQRT, ZSCALE
} from '../RangeValues.js';


describe('Test RangeValues constructor', () => {

    test('constructor with no parameters uses the defaults', () => {
        const rv= new RangeValues();
        expect(rv.lowerWhich).toEqual(PERCENTAGE);
        expect(rv.lowerValue).toEqual(1);
        expect(rv.upperWhich).toEqual(PERCENTAGE);
        expect(rv.upperValue).toEqual(99);
        expect(rv.asinhQValue).toBeNaN();
        expect(rv.gammaValue).toEqual(2);
        expect(rv.algorithm).toEqual(STRETCH_LINEAR);
        expect(rv.zscaleContrast).toEqual(25);
        expect(rv.zscaleSamples).toEqual(600);
        expect(rv.zscaleSamplesPerLine).toEqual(120);
        expect(rv.rgbPreserveHue).toEqual(0);
        expect(rv.asinhStretch).toBeNaN();
        expect(rv.scalingK).toEqual(1);
        expect(rv.bias).toEqual(0.5);
        expect(rv.contrast).toEqual(1);
    });

    test('constructor converts string parameters to numbers', () => {
        const rv= new RangeValues('90', '2.5', '90', '300.75', '8', '3', '45', '30', '500', '100', '0', '4', '2', '0.4', '1.2');
        expect(rv.lowerWhich).toEqual(ABSOLUTE);
        expect(rv.lowerValue).toEqual(2.5);
        expect(rv.upperWhich).toEqual(ABSOLUTE);
        expect(rv.upperValue).toEqual(300.75);
        expect(rv.asinhQValue).toEqual(8);
        expect(rv.gammaValue).toEqual(3);
        expect(rv.algorithm).toEqual(STRETCH_LOG);
        expect(rv.zscaleContrast).toEqual(30);
        expect(rv.zscaleSamples).toEqual(500);
        expect(rv.zscaleSamplesPerLine).toEqual(100);
        expect(rv.rgbPreserveHue).toEqual(0);
        expect(rv.asinhStretch).toEqual(4);
        expect(rv.scalingK).toEqual(2);
        expect(rv.bias).toEqual(0.4);
        expect(rv.contrast).toEqual(1.2);
    });

    test('constructor truncates integer parameters', () => {
        const rv= new RangeValues(90.7, 1, 88.2, 99, NaN, 2, 45.9, 25.5);
        expect(rv.lowerWhich).toEqual(ABSOLUTE);
        expect(rv.upperWhich).toEqual(PERCENTAGE);
        expect(rv.algorithm).toEqual(STRETCH_LOG);
        expect(rv.zscaleContrast).toEqual(25);
    });

    test('constructor with rgbPreserveHue forces asinh algorithm and zscale upper bound', () => {
        const rv= new RangeValues(PERCENTAGE, 1, PERCENTAGE, 99, NaN, 2, STRETCH_LOG, 25, 600, 120, 1);
        expect(rv.rgbPreserveHue).toEqual(1);
        expect(rv.algorithm).toEqual(STRETCH_ASINH);
        expect(rv.upperWhich).toEqual(ZSCALE);
        expect(rv.lowerWhich).toEqual(PERCENTAGE);
    });
});


describe('Test RangeValues.make()', () => {

    test('make() with no parameters equals the constructor defaults', () => {
        expect(RangeValues.make()).toEqual(new RangeValues());
    });

    test('make() using parameters (lowerWhich, lowerValue, upperWhich, upperValue, asinhQValue, gammaValue, algorithm)', () => {
        const rv= RangeValues.make(SIGMA, -2, SIGMA, 10, 5, 2.2, STRETCH_SQRT);
        expect(rv).toBeInstanceOf(RangeValues);
        expect(rv.lowerWhich).toEqual(SIGMA);
        expect(rv.lowerValue).toEqual(-2);
        expect(rv.upperWhich).toEqual(SIGMA);
        expect(rv.upperValue).toEqual(10);
        expect(rv.asinhQValue).toEqual(5);
        expect(rv.gammaValue).toEqual(2.2);
        expect(rv.algorithm).toEqual(STRETCH_SQRT);
    });
});


describe('Test RangeValues.makeRV()', () => {

    test('makeRV() with an empty object equals the constructor defaults', () => {
        expect(RangeValues.makeRV({})).toEqual(new RangeValues());
    });

    test('makeRV() using which sets both lowerWhich and upperWhich', () => {
        const rv= RangeValues.makeRV({which: ABSOLUTE, lowerValue: 10, upperValue: 2000, algorithm: STRETCH_LOGLOG});
        expect(rv.lowerWhich).toEqual(ABSOLUTE);
        expect(rv.upperWhich).toEqual(ABSOLUTE);
        expect(rv.lowerValue).toEqual(10);
        expect(rv.upperValue).toEqual(2000);
        expect(rv.algorithm).toEqual(STRETCH_LOGLOG);
    });

    test('makeRV() using lowerWhich and upperWhich overrides which', () => {
        const rv= RangeValues.makeRV({which: SIGMA, lowerWhich: ABSOLUTE});
        expect(rv.lowerWhich).toEqual(ABSOLUTE);
        expect(rv.upperWhich).toEqual(SIGMA);
    });

    test('makeRV() using bias and contrast', () => {
        const rv= RangeValues.makeRV({bias: 0.3, contrast: 2});
        expect(rv.bias).toEqual(0.3);
        expect(rv.contrast).toEqual(2);
    });
});


describe('Test RangeValues.makeSimple()', () => {

    test('makeSimple() using parameters (boundsType, lowerValue, upperValue, algorithm)', () => {
        const rv= RangeValues.makeSimple('sigma', -2, 10, 'log');
        expect(rv.lowerWhich).toEqual(SIGMA);
        expect(rv.upperWhich).toEqual(SIGMA);
        expect(rv.lowerValue).toEqual(-2);
        expect(rv.upperValue).toEqual(10);
        expect(rv.algorithm).toEqual(STRETCH_LOG);
        expect(rv.gammaValue).toEqual(2);
        expect(rv.asinhQValue).toBeNaN();
    });

    test('makeSimple() boundsType and algorithm ignore case', () => {
        const rv= RangeValues.makeSimple('ABSOLUTE', 0, 500, 'PowerLaw_Gamma');
        expect(rv.lowerWhich).toEqual(ABSOLUTE);
        expect(rv.algorithm).toEqual(STRETCH_POWERLAW_GAMMA);
    });

    test('makeSimple() with unknown boundsType and algorithm falls back to percent and linear', () => {
        const rv= RangeValues.makeSimple('nonsense', 1, 99, 'nonsense');
        expect(rv.lowerWhich).toEqual(PERCENTAGE);
        expect(rv.upperWhich).toEqual(PERCENTAGE);
        expect(rv.algorithm).toEqual(STRETCH_LINEAR);
    });

    test('makeSimple() with no parameters', () => {
        const rv= RangeValues.makeSimple();
        expect(rv.lowerWhich).toEqual(PERCENTAGE);
        expect(rv.lowerValue).toEqual(1);
        expect(rv.upperValue).toEqual(99);
        expect(rv.algorithm).toEqual(STRETCH_LINEAR);
    });
});


describe('Test RangeValues.make2To10SigmaLinear()', () => {

    test('make2To10SigmaLinear()', () => {
        const rv= RangeValues.make2To10SigmaLinear();
        expect(rv.lowerWhich).toEqual(SIGMA);
        expect(rv.lowerValue).toEqual(-2);
        expect(rv.upperWhich).toEqual(SIGMA);
        expect(rv.upperValue).toEqual(10);
        expect(rv.algorithm).toEqual(STRETCH_LINEAR);
    });
});


describe('Test RangeValues serialization', () => {

    test('serializeRV(), toJSON() and toString() give the comma separated form', () => {
        const rv= RangeValues.make(SIGMA, -2, SIGMA, 10, NaN, 2, STRETCH_LOG);
        const expectedStr= '92,-2,92,10,NaN,2,45,25,600,120,0,NaN,1';
        expect(RangeValues.serializeRV(rv)).toEqual(expectedStr);
        expect(rv.toJSON()).toEqual(expectedStr);
        expect(rv.toString()).toEqual(expectedStr);
        expect(JSON.stringify({rv})).toEqual(`{"rv":"${expectedStr}"}`);
    });

    test('serializeSimple() using parameters (boundsType, lowerValue, upperValue, algorithm)', () => {
        expect(RangeValues.serializeSimple('absolute', 5, 500, 'sqrt')).toEqual('90,5,90,500,NaN,2,49,25,600,120,0,NaN,1');
    });

    test('serializeSimple() with no parameters', () => {
        expect(RangeValues.serializeSimple()).toEqual('88,1,88,99,NaN,2,44,25,600,120,0,NaN,1');
    });
});


describe('Test RangeValues.parse()', () => {

    test('parse() round trips toJSON()', () => {
        const rv= RangeValues.make(ABSOLUTE, 5, ABSOLUTE, 500, 8, 3, STRETCH_ASINH, 30, 500, 100, 0, 4, 2);
        const parsedRv= RangeValues.parse(rv.toJSON());
        expect(parsedRv).toBeInstanceOf(RangeValues);
        expect(parsedRv).toEqual(rv);
    });

    test('parse() does not carry bias and contrast, they reset to the defaults', () => {
        const rv= RangeValues.makeRV({bias: 0.3, contrast: 2});
        const parsedRv= RangeValues.parse(rv.toJSON());
        expect(parsedRv.bias).toEqual(0.5);
        expect(parsedRv.contrast).toEqual(1);
    });

    test('parse() with the older 10 value form uses the defaults for the rest', () => {
        const parsedRv= RangeValues.parse('88,1,88,99,NaN,2,44,25,600,120');
        expect(parsedRv.rgbPreserveHue).toEqual(0);
        expect(parsedRv.asinhStretch).toBeNaN();
        expect(parsedRv.scalingK).toEqual(1);
    });

    test('parse() with a RangeValues-shaped object returns that object', () => {
        const rvObj= {lowerWhich: PERCENTAGE, lowerValue: 1, upperWhich: PERCENTAGE, upperValue: 99, algorithm: STRETCH_LOG};
        expect(RangeValues.parse(rvObj)).toBe(rvObj);

        const rv= RangeValues.make();
        expect(RangeValues.parse(rv)).toBe(rv);
    });

    test('parse() with too few values or non-numeric required values returns undefined', () => {
        expect(RangeValues.parse('a,b')).toBeUndefined();
        expect(RangeValues.parse('88,1,88,99')).toBeUndefined();
        expect(RangeValues.parse('88,x,88,99,NaN,2,44,25,600,120')).toBeUndefined();
        expect(RangeValues.parse('88,1,88,99,NaN,2,linear,25,600,120')).toBeUndefined();
    });

    test('parse() with empty or non-string input returns undefined', () => {
        expect(RangeValues.parse(undefined)).toBeUndefined();
        expect(RangeValues.parse('')).toBeUndefined();
        expect(RangeValues.parse(42)).toBeUndefined();
        expect(RangeValues.parse({lowerWhich: PERCENTAGE, lowerValue: 1})).toBeUndefined();
    });
});


describe('Test RangeValues.clone()', () => {

    test('clone() makes an equal but separate object', () => {
        const rv= RangeValues.make(ABSOLUTE, 5, ABSOLUTE, 500, 8, 3, STRETCH_ASINH, 30, 500, 100, 0, 4, 2, 0.3, 2);
        const clonedRv= rv.clone();
        expect(clonedRv).not.toBe(rv);
        expect(clonedRv).toEqual(rv);
    });
});


describe('Test RangeValues.isAlgorithmSupported()', () => {

    test('isAlgorithmSupported() with supported and unsupported names', () => {
        ['linear', 'log', 'loglog', 'equal', 'squared', 'sqrt', 'asinh', 'powerlaw_gamma']
            .forEach((algorithm) => expect(RangeValues.isAlgorithmSupported(algorithm)).toBe(true));
        expect(RangeValues.isAlgorithmSupported('LINEAR')).toBe(true);
        expect(RangeValues.isAlgorithmSupported('Asinh')).toBe(true);
        expect(RangeValues.isAlgorithmSupported('nonsense')).toBe(false);
        expect(RangeValues.isAlgorithmSupported(undefined)).toBe(false);
    });
});
