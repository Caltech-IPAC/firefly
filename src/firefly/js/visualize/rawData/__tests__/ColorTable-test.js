import BrowserInfo from '../../../util/BrowserInfo.js';
import {
    baseIdMatchesForOrRev, findAContrastColor, getCbarNumIds, getCbarTip, getColorModel, getColorModelBase,
    getColorModelByGPUType, getColorTableMap, isReversedColor, NO_COLOR_TABLE, packColor, PACKED, PERCENT,
    reverseId, RGB
} from '../ColorTable.js';


describe('Test color table ids', () => {

    test('getCbarNumIds returns one string id per color table', () => {
        const numIds= getCbarNumIds();
        expect(numIds.length).toBeGreaterThan(0);
        expect(numIds[0]).toEqual('0');
        expect(numIds[numIds.length-1]).toEqual(numIds.length-1+'');
        numIds.forEach( (id) => expect(getColorTableMap()[id]).toBeDefined());
    });

    test('getCbarTip works with a numeric id or a name', () => {
        expect(getCbarTip('0')).toEqual('Gray Scale');
        expect(getCbarTip('grayscale')).toEqual('Gray Scale');
        expect(getCbarTip('not-a-table')).toBeUndefined();
    });

    test('isReversedColor is true only for an existing id with an R suffix', () => {
        expect(isReversedColor('grayscaleR')).toBe(true);
        expect(isReversedColor('0R')).toBe(true);
        expect(isReversedColor('grayscale')).toBe(false);
        expect(isReversedColor('0')).toBe(false);
        expect(isReversedColor('notATableR')).toBe(false);
        // ends in 'd', the word 'Reversed' in the name does not count
        expect(isReversedColor('falseColorReversed')).toBe(false);
    });

    test('reverseId toggles the R suffix', () => {
        expect(reverseId('grayscale')).toEqual('grayscaleR');
        expect(reverseId('grayscaleR')).toEqual('grayscale');
        expect(reverseId('5')).toEqual('5R');
        expect(reverseId('5R')).toEqual('5');
        expect(reverseId('notATable')).toBeUndefined();
    });

    test('baseIdMatchesForOrRev matches the base id and its reversed id only', () => {
        expect(baseIdMatchesForOrRev('grayscale', 'grayscale')).toBe(true);
        expect(baseIdMatchesForOrRev('grayscale', 'grayscaleR')).toBe(true);
        expect(baseIdMatchesForOrRev('grayscale', 'spectrum')).toBe(false);
        expect(baseIdMatchesForOrRev('grayscale', 'spectrumR')).toBe(false);
        expect(baseIdMatchesForOrRev('grayscaleR', 'grayscaleR')).toBe(false);
    });
});


describe('Test packColor', () => {

    test('packColor writes a,b,g,r from high byte to low byte', () => {
        expect(packColor(1, 2, 3, 4)).toEqual(0x04030201);
    });

    test('packColor defaults alpha to 255', () => {
        expect(packColor(1, 2, 3) >>> 0).toEqual(0xFF030201);
    });

    test('packColor reads back as r,g,b,a bytes from a little-endian byte array', () => {
        const bytes= new Uint8Array(Uint32Array.of(packColor(10, 20, 30, 40)).buffer);
        expect(Array.from(bytes)).toEqual([10, 20, 30, 40]);
    });
});


describe('Test getColorModelBase', () => {

    test('getColorModelBase grayscale RGB is 768 values from black to white', () => {
        const palette= getColorModelBase('grayscale', RGB);
        expect(palette).toBeInstanceOf(Uint32Array);
        expect(palette.length).toEqual(768);
        expect(Array.from(palette.slice(0, 3))).toEqual([0, 0, 0]);
        expect(Array.from(palette.slice(765, 768))).toEqual([255, 255, 255]);
        for (let i = 3; i < 768; i += 3) {
            expect(palette[i]).toBeGreaterThanOrEqual(palette[i-3]);
            expect(palette[i+1]).toEqual(palette[i]);
            expect(palette[i+2]).toEqual(palette[i]);
        }
    });

    test('getColorModelBase grayscale reversed goes from white to black', () => {
        const palette= getColorModelBase('grayscaleR', RGB);
        expect(Array.from(palette.slice(0, 3))).toEqual([255, 255, 255]);
        expect(Array.from(palette.slice(765, 768))).toEqual([0, 0, 0]);
    });

    test('getColorModelBase reversed table matches the forward table read backwards', () => {
        const forwardPalette= getColorModelBase('spectrum', RGB);
        const reversedPalette= getColorModelBase('spectrumR', RGB);
        expect(Array.from(reversedPalette.slice(0, 3))).toEqual(Array.from(forwardPalette.slice(765, 768)));
        expect(Array.from(reversedPalette.slice(765, 768))).toEqual(Array.from(forwardPalette.slice(0, 3)));
    });

    // cividis and magma list 255 colors for slots 0-254, slot 255 is left for the NaN color
    test.each([
        ['cividis', [0, 32, 81], [253, 233, 70]],      // #002051 to #fde946
        ['magma', [0, 0, 4], [252, 251, 189]],         // #000004 to #fcfbbd
    ])('getColorModelBase %s forward starts and ends on its first and last colors', (ctId, firstColor, lastColor) => {
        const palette= getColorModelBase(ctId, RGB);
        expect(Array.from(palette.slice(0, 3))).toEqual(firstColor);
        expect(Array.from(palette.slice(762, 765))).toEqual(lastColor);
    });

    test.each([
        ['cividis', [253, 233, 70], [0, 32, 81]],
        ['magma', [252, 251, 189], [0, 0, 4]],
    ])('getColorModelBase %s reversed starts and ends on its last and first colors', (ctId, firstColor, lastColor) => {
        const palette= getColorModelBase(ctId+'R', RGB);
        expect(Array.from(palette.slice(0, 3))).toEqual(firstColor);
        expect(Array.from(palette.slice(762, 765))).toEqual(lastColor);
    });

    test.each(['cividis', 'magma'])('getColorModelBase %s reversed is every forward color in reverse order', (ctId) => {
        const forwardPalette= getColorModelBase(ctId, RGB);
        const reversedPalette= getColorModelBase(ctId+'R', RGB);
        for (let colorIdx = 0; colorIdx < 255; colorIdx++) {
            const reversedIdx= 3*colorIdx;
            const forwardIdx= 3*(254-colorIdx);
            expect(Array.from(reversedPalette.slice(reversedIdx, reversedIdx+3)))
                .toEqual(Array.from(forwardPalette.slice(forwardIdx, forwardIdx+3)));
        }
    });

    test('getColorModelBase numeric id and name give the same palette', () => {
        expect(Array.from(getColorModelBase('0', RGB))).toEqual(Array.from(getColorModelBase('grayscale', RGB)));
    });

    test('getColorModelBase PERCENT is the RGB palette scaled to 0-1', () => {
        const rgbPalette= getColorModelBase('spectrum', RGB);
        const percentPalette= getColorModelBase('spectrum', PERCENT);
        expect(percentPalette).toBeInstanceOf(Float32Array);
        expect(percentPalette.length).toEqual(768);
        percentPalette.forEach( (value, idx) => expect(value).toBeCloseTo(rgbPalette[idx]/255, 6));
    });

    test('getColorModelBase PACKED is 256 packed colors with full alpha', () => {
        const rgbPalette= getColorModelBase('spectrum', RGB);
        const packedPalette= getColorModelBase('spectrum', PACKED);
        expect(packedPalette).toBeInstanceOf(Uint32Array);
        expect(packedPalette.length).toEqual(256);
        packedPalette.forEach( (packed, idx) => {
            const rgbIdx= idx*3;
            const expected= packColor(rgbPalette[rgbIdx], rgbPalette[rgbIdx+1], rgbPalette[rgbIdx+2]) >>> 0;
            expect(packed).toEqual(expected);
        });
    });

    test('getColorModelBase unknown id returns an empty array', () => {
        const logSpy= jest.spyOn(console, 'log').mockImplementation(() => undefined);
        expect(getColorModelBase('notATable', RGB)).toEqual([]);
        logSpy.mockRestore();
    });
});


describe('Test getColorModel', () => {

    test('getColorModel NO_COLOR_TABLE returns undefined', () => {
        expect(getColorModel(NO_COLOR_TABLE)).toBeUndefined();
    });

    test('getColorModel without a nan color returns the base palette', () => {
        expect(getColorModel('grayscale')).toBe(getColorModelBase('grayscale', RGB));
    });

    test('getColorModel RGB sets the last color to the nan color without changing the base palette', () => {
        const palette= getColorModel('grayscale', [10, 20, 30], RGB);
        expect(Array.from(palette.slice(765, 768))).toEqual([10, 20, 30]);
        expect(Array.from(palette.slice(0, 3))).toEqual([0, 0, 0]);
        expect(Array.from(getColorModelBase('grayscale', RGB).slice(765, 768))).toEqual([255, 255, 255]);
    });

    test('getColorModel PERCENT sets the last color to the nan color scaled to 0-1', () => {
        const palette= getColorModel('grayscale', [51, 102, 255], PERCENT);
        expect(palette).toBeInstanceOf(Float32Array);
        expect(palette[765]).toBeCloseTo(0.2, 6);
        expect(palette[766]).toBeCloseTo(0.4, 6);
        expect(palette[767]).toBeCloseTo(1, 6);
    });

    test('getColorModel PACKED sets the last color to the packed nan color', () => {
        const palette= getColorModel('grayscale', [10, 20, 30], PACKED);
        expect(palette.length).toEqual(256);
        expect(palette[255]).toEqual(packColor(10, 20, 30) >>> 0);
    });

    test('getColorModel uses black when the nan color has fewer than 3 values', () => {
        const palette= getColorModel('grayscale', [10, 20], RGB);
        expect(Array.from(palette.slice(765, 768))).toEqual([0, 0, 0]);
    });
});


describe('Test getColorModelByGPUType', () => {

    // BrowserInfo is mocked globally in __jest__/InitTest.js without supportsWebGpu, so add it per test
    afterEach(() => delete BrowserInfo.supportsWebGpu);

    test('getColorModelByGPUType returns a PACKED palette when WebGPU is supported', async () => {
        BrowserInfo.supportsWebGpu= jest.fn().mockResolvedValue(true);
        const palette= await getColorModelByGPUType('grayscale');
        expect(palette).toBeInstanceOf(Uint32Array);
        expect(palette.length).toEqual(256);
    });

    test('getColorModelByGPUType returns a PERCENT palette when WebGPU is not supported', async () => {
        BrowserInfo.supportsWebGpu= jest.fn().mockResolvedValue(false);
        const palette= await getColorModelByGPUType('grayscale');
        expect(palette).toBeInstanceOf(Float32Array);
        expect(palette.length).toEqual(768);
    });
});


describe('Test findAContrastColor', () => {

    test('findAContrastColor unknown id returns black', () => {
        expect(findAContrastColor('notATable')).toEqual([0, 0, 0]);
    });

    test('findAContrastColor grayscale has no hues so it picks hue 0, a red', () => {
        const [red, green, blue]= findAContrastColor('grayscale');
        expect(red).toBeGreaterThan(green);
        expect(green).toBeCloseTo(blue, 6);
    });
});
