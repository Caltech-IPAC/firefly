import chroma from 'chroma-js';
import Color, {
    brighter, darker, getBestContrast, getBWBackground, getRGBA, hexColorWithAlpha, isBestForegroundLight,
    joyVarColorWithAlpha, maximizeOpacity, rateOpacity, toRGB, toRGBA, toRGBAString
} from '../Color.js';

// getBWBackground methods, not exported from Color.js
const LUMI= 0;
const CONTRAST= 1;
const HSP= 2;

const WHITE_BACKGROUND= 'rgba(255, 255, 255, 1.0)';
const BLACK_BACKGROUND= 'rgba(0, 0, 0, 1.0)';


describe('Test color conversion', () => {

    test('toRGBA returns rgba with the given alpha', () => {
        expect(toRGBA('red', 0.5)).toEqual([255, 0, 0, 0.5]);
        expect(toRGBA('#336699')).toEqual([51, 102, 153, 1]);
    });

    test('toRGBA invalid color returns black with the given alpha', () => {
        expect(toRGBA('notAColor', 0.3)).toEqual([0, 0, 0, 0.3]);
    });

    test('toRGB returns rgb', () => {
        expect(toRGB('rgb(10, 20, 30)')).toEqual([10, 20, 30]);
        expect(toRGB('notAColor')).toEqual([0, 0, 0]);
    });

    test('getRGBA reads rgba, rgb and hex strings', () => {
        expect(getRGBA('rgba(10, 20, 30, 0.4)')).toEqual([10, 20, 30, 0.4]);
        expect(getRGBA('rgb(10, 20, 30)')).toEqual([10, 20, 30, 1]);
        expect(getRGBA('#0a141e')).toEqual([10, 20, 30, 1]);
    });

    test('getRGBA invalid color returns opaque black', () => {
        expect(getRGBA('notAColor')).toEqual([0, 0, 0, 1]);
    });

    test('toRGBAString formats a 4 value array', () => {
        expect(toRGBAString([10, 20, 30, 0.5])).toEqual('rgba(10, 20, 30, 0.5)');
    });

    test('toRGBAString adds alpha 1 to a 3 value array', () => {
        expect(toRGBAString([10, 20, 30])).toEqual('rgba(10, 20, 30, 1)');
    });

    test('toRGBAString fills missing values with the last value given', () => {
        expect(toRGBAString([10])).toEqual('rgba(10, 10, 10, 1)');
    });

    test('toRGBAString with no array or an empty array returns an empty string', () => {
        expect(toRGBAString(undefined)).toEqual('');
        expect(toRGBAString([])).toEqual('');
        expect(toRGBAString('red')).toEqual('');
    });

    test('toRGBAString does not change the array passed in', () => {
        const rgb= [10, 20, 30];
        toRGBAString(rgb);
        expect(rgb).toEqual([10, 20, 30]);
    });
});


describe('Test alpha', () => {

    test('hexColorWithAlpha adds the alpha to the hex', () => {
        expect(hexColorWithAlpha('#ff0000', 0.5)).toEqual('#ff000080');
        expect(hexColorWithAlpha('blue', 1)).toEqual('#0000ff');
    });

    test('hexColorWithAlpha invalid color is returned unchanged', () => {
        expect(hexColorWithAlpha('notAColor', 0.5)).toEqual('notAColor');
    });

    test('joyVarColorWithAlpha uses the fallback color of a css var', () => {
        expect(joyVarColorWithAlpha('var(--joy-palette-primary-500, #0b6bcb)', 0.5)).toEqual('#0b6bcb80');
    });

    test('joyVarColorWithAlpha uses a plain color as is', () => {
        expect(joyVarColorWithAlpha('#0b6bcb', 0.5)).toEqual('#0b6bcb80');
    });

    test('rateOpacity multiplies the alpha by the ratio', () => {
        expect(rateOpacity('red', 0.5)).toEqual('rgba(255, 0, 0, 0.5)');
        expect(rateOpacity('rgba(0, 0, 255, 0.5)', 0.5)).toEqual('rgba(0, 0, 255, 0.25)');
    });

    test('rateOpacity keeps the alpha between 0 and 1', () => {
        expect(rateOpacity('rgba(0, 0, 255, 0.5)', 3)).toEqual('rgba(0, 0, 255, 1)');
        expect(rateOpacity('rgba(0, 0, 255, 0.5)', -1)).toEqual('rgba(0, 0, 255, 0)');
    });

    test('maximizeOpacity sets the alpha to 1', () => {
        expect(maximizeOpacity('rgba(255, 0, 0, 0.3)')).toEqual('#ff0000');
    });

    test('maximizeOpacity invalid color returns white', () => {
        expect(maximizeOpacity('notAColor')).toEqual('white');
    });
});


describe('Test brighter and darker', () => {

    test('brighter returns a brighter color', () => {
        const brighterColor= brighter('#336699');
        expect(chroma(brighterColor).luminance()).toBeGreaterThan(chroma('#336699').luminance());
        expect(brighter('#336699', 0)).toEqual('#336699');
    });

    test('brighter with a higher level is brighter still', () => {
        expect(chroma(brighter('#336699', 2)).luminance()).toBeGreaterThan(chroma(brighter('#336699', 1)).luminance());
    });

    test('brighter invalid color returns undefined', () => {
        expect(brighter('notAColor')).toBeUndefined();
    });

    test('darker returns a darker color', () => {
        const darkerColor= darker('#336699');
        expect(chroma(darkerColor).luminance()).toBeLessThan(chroma('#336699').luminance());
        expect(darker('#336699', 0)).toEqual('#336699');
    });

    test('darker with a higher level is darker still', () => {
        expect(chroma(darker('#336699', 2)).luminance()).toBeLessThan(chroma(darker('#336699', 1)).luminance());
    });

    test('darker invalid color returns undefined', () => {
        expect(darker('notAColor')).toBeUndefined();
    });
});


describe('Test contrast', () => {

    test('isBestForegroundLight is true for a dark background', () => {
        expect(isBestForegroundLight('black')).toBe(true);
        expect(isBestForegroundLight('#000080')).toBe(true);
    });

    test('isBestForegroundLight is false for a light background', () => {
        expect(isBestForegroundLight('white')).toBe(false);
        expect(isBestForegroundLight('yellow')).toBe(false);
    });

    test('getBestContrast returns the color with more contrast to the background', () => {
        expect(getBestContrast('white', 'black', 'yellow')).toEqual('black');
        expect(getBestContrast('white', 'yellow', 'black')).toEqual('black');
        expect(getBestContrast('black', 'navy', 'white')).toEqual('white');
    });
});


describe('Test getBWBackground', () => {

    test('getBWBackground gives white for dark colors and black for light colors', () => {
        expect(getBWBackground('black')).toEqual(WHITE_BACKGROUND);
        expect(getBWBackground('blue')).toEqual(WHITE_BACKGROUND);
        expect(getBWBackground('white')).toEqual(BLACK_BACKGROUND);
        expect(getBWBackground('yellow')).toEqual(BLACK_BACKGROUND);
    });

    test('getBWBackground invalid color is treated as black', () => {
        expect(getBWBackground('notAColor')).toEqual(WHITE_BACKGROUND);
    });

    test('getBWBackground mid gray is light for CONTRAST and HSP but dark for LUMI', () => {
        // luma 150: under the LUMI threshold of 165, over the CONTRAST and HSP threshold of 128
        expect(getBWBackground('rgb(150, 150, 150)', LUMI)).toEqual(WHITE_BACKGROUND);
        expect(getBWBackground('rgb(150, 150, 150)', CONTRAST)).toEqual(BLACK_BACKGROUND);
        expect(getBWBackground('rgb(150, 150, 150)', HSP)).toEqual(BLACK_BACKGROUND);
    });

    test('getBWBackground red is light only for HSP', () => {
        // luma: LUMI 54, CONTRAST 76, HSP 139
        expect(getBWBackground('red', LUMI)).toEqual(WHITE_BACKGROUND);
        expect(getBWBackground('red', CONTRAST)).toEqual(WHITE_BACKGROUND);
        expect(getBWBackground('red', HSP)).toEqual(BLACK_BACKGROUND);
    });

    test('getBWBackground unknown method uses LUMI', () => {
        expect(getBWBackground('rgb(150, 150, 150)', 99)).toEqual(WHITE_BACKGROUND);
    });

    test('getBWBackground is on the default export', () => {
        expect(Color.getBWBackground('black')).toEqual(WHITE_BACKGROUND);
    });
});


describe('Test shadeColor', () => {

    test('shadeColor positive percent moves toward white', () => {
        expect(Color.shadeColor('#000000', 0.5)).toEqual('#808080');
        expect(Color.shadeColor('#000000', 1)).toEqual('#ffffff');
    });

    test('shadeColor negative percent moves toward black', () => {
        expect(Color.shadeColor('#ffffff', -0.5)).toEqual('#808080');
        expect(Color.shadeColor('#ffffff', -1)).toEqual('#000000');
    });

    test('shadeColor 0 percent returns the same color', () => {
        expect(Color.shadeColor('#336699', 0)).toEqual('#336699');
    });
});
