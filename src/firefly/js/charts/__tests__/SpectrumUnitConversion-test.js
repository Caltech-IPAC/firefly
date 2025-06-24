/*eslint-env node, jest */
import {
    canUnitConv,
    getUnitConvExpr,
    getUnitInfo,
    getXLabel,
    getMeasurementLabel
} from 'firefly/charts/dataTypes/SpectrumUnitConversion';


describe('Test SpectrumUnitConversion', () => {
    test('canUnitConv', () => {
        // NU
        expect(canUnitConv({from: 'Hz', to: 'GHz'})).toBe(true);
        expect(canUnitConv({from: 'MHz', to: 'Hz'})).toBe(true);
        expect(canUnitConv({from: 'Hz', to: 'Hz'})).toBe(true); // same unit
        expect(canUnitConv({from: 'Hz', to: 'A'})).toBe(false); //can't convert between NU and LAMBDA

        // LAMBDA
        expect(canUnitConv({from: 'A', to: 'm'})).toBe(true);
        expect(canUnitConv({from: 'Angstrom', to: 'm'})).toBe(true); // 'Angstrom' is an alias for 'A'
        expect(canUnitConv({from: 'm', to: 'Angstrom'})).toBe(true);
        expect(canUnitConv({from: 'm', to: 'Ang'})).toBe(false); // unrecognized unit Ang
        expect(canUnitConv({from: 'm', to: 'm'})).toBe(true); // same unit

        // F_LAMBDA
        expect(canUnitConv({from: 'erg/s/cm^2/A', to: 'W/m^2/um'})).toBe(true);
        expect(canUnitConv({from: 'erg/s/cm^2/Angstrom', to: 'W/m^2/um'})).toBe(true); // alias of subunit 'A' is propagated
        expect(canUnitConv({from: 'erg/s/cm**2/Angstrom', to: 'W/m^2/um'})).toBe(true); // asterisk power expression
        expect(canUnitConv({from: 'erg/s/cm2/Angstrom', to: 'W/m^2/um'})).toBe(true); // invisible power expression
        expect(canUnitConv({from: 'erg.s**-1.cm**-2.Angstrom**-1', to: 'W/m^2/um'})).toBe(true); // multiplication expression
        expect(canUnitConv({from: '1e-16erg.s**-1.cm**-2.Angstrom**-1', to: 'W/m^2/um'})).toBe(true); // unit can be prefixed with a scalar
        expect(canUnitConv({from: '1e-16erg.s**-1.cm**-2.Angstrom**-1', to: 'erg.s**-1.cm**-2.Angstrom**-1'})).toBe(true); // same unit with scalar
        expect(canUnitConv({from: '1e-16erg.s**-1.cm^-2.Angstrom**-1', to: 'W/m^2/um'})).toBe(false); // mixed power expression
        expect(canUnitConv({from: 'erg/s/cm^2/A', to: 'erg/s/cm^2/Hz'})).toBe(false); // can't convert between F_LAMBDA and F_NU

        // F_NU
        expect(canUnitConv({from: 'erg/s/cm^2/Hz', to: 'W/m^2/Hz'})).toBe(true);
        expect(canUnitConv({from: 'erg/s/cm^2/Hz', to: 'Jy'})).toBe(true);
        expect(canUnitConv({from: 'erg.s**-1.cm**-2.Hz**-1', to: 'Jy'})).toBe(true); // multiplication expression, same as above

        // F
        expect(canUnitConv({from: 'erg/s/cm^2', to: 'W/m^2'})).toBe(true);
        expect(canUnitConv({from: 'erg/s/cm^2', to: 'Jy*Hz'})).toBe(true);
        expect(canUnitConv({from: 'erg.s**-1.cm**-2', to: 'Jy.Hz'})).toBe(true); // multiplication expression
        expect(canUnitConv({from: 'erg/s/cm^2', to: 'erg/s/cm^2/Hz'})).toBe(false); // can't convert between F and F_NU
        expect(canUnitConv({from: 'erg/s/cm^2', to: 'erg/s/cm^2/A'})).toBe(false); // can't convert between F and F_LAMBDA
    });

    test('getUnitConvExpr', () => {
        // Following are the same examples as in canUnitConv, but now we expect the conversion expression with cname.
        // If it cannot be converted, we expect cname as it is.

        // NU ---
        expect(
            getUnitConvExpr({cname: 'FREQUENCY', from: 'Hz', to: 'GHz'})
        ).toBe('"FREQUENCY" / 1000000000.0');
        expect(
            getUnitConvExpr({cname: 'FREQUENCY', from: 'MHz', to: 'Hz'})
        ).toBe('"FREQUENCY" * 1000000.0');
        expect(
            getUnitConvExpr({cname: 'FREQUENCY', from: 'Hz', to: 'Hz'})
        ).toBe('"FREQUENCY"'); // same unit
        expect(
            getUnitConvExpr({cname: 'FREQUENCY', from: 'Hz', to: 'A'})
        ).toBe('FREQUENCY'); //can't convert between NU and LAMBDA

        // LAMBDA ---
        expect(
            getUnitConvExpr({cname: 'WAVELENGTH', from: 'A', to: 'm'})
        ).toBe('"WAVELENGTH" / 1.0E+10');
        expect(
            getUnitConvExpr({cname: 'WAVELENGTH', from: 'Angstrom', to: 'm'})
        ).toBe('"WAVELENGTH" / 1.0E+10'); // 'Angstrom' is an alias for 'A'
        expect(
            getUnitConvExpr({cname: 'WAVELENGTH', from: 'm', to: 'Angstrom'})
        ).toBe('"WAVELENGTH" * 1.0E+10');
        expect(
            getUnitConvExpr({cname: 'WAVELENGTH', from: 'm', to: 'Ang'})
        ).toBe('WAVELENGTH'); // unrecognized unit Ang
        expect(
            getUnitConvExpr({cname: 'WAVELENGTH', from: 'm', to: 'm'})
        ).toBe('"WAVELENGTH"'); // same unit

        // F_LAMBDA ---
        expect(getUnitConvExpr(
            {cname: 'FLUX_DENSITY', from: 'erg/s/cm^2/A', to: 'W/m^2/um'})
        ).toBe('"FLUX_DENSITY" * 10');
        expect(
            getUnitConvExpr({cname: 'FLUX_DENSITY', from: 'erg/s/cm^2/Angstrom', to: 'W/m^2/um'})
        ).toBe('"FLUX_DENSITY" * 10'); // alias of subunit 'A' is propagated
        expect(
            getUnitConvExpr({cname: 'FLUX_DENSITY', from: 'erg/s/cm**2/Angstrom', to: 'W/m^2/um'})
        ).toBe('"FLUX_DENSITY" * 10'); // asterisk power expression
        expect(
            getUnitConvExpr({cname: 'FLUX_DENSITY', from: 'erg/s/cm2/Angstrom', to: 'W/m^2/um'})
        ).toBe('"FLUX_DENSITY" * 10'); // invisible power expression
        expect(
            getUnitConvExpr({cname: 'FLUX_DENSITY', from: 'erg.s**-1.cm**-2.Angstrom**-1', to: 'W/m^2/um'})
        ).toBe('"FLUX_DENSITY" * 10'); // multiplication expression
        expect(
            getUnitConvExpr({cname: 'FLUX_DENSITY', from: '1e-16erg.s**-1.cm**-2.Angstrom**-1', to: 'W/m^2/um'})
        ).toBe('"FLUX_DENSITY" * 10 * 1e-16'); // unit can be prefixed with a scalar
        expect(
            getUnitConvExpr({cname: 'FLUX_DENSITY', from: '1e-16erg.s**-1.cm**-2.Angstrom**-1', to: 'erg.s**-1.cm**-2.Angstrom**-1'})
        ).toBe('"FLUX_DENSITY" * 1e-16'); // same unit with scalar
        expect(
            getUnitConvExpr({cname: 'FLUX_DENSITY', from: 'erg.s**-1.cm**-2.Angstrom**-1', to: '1e-16erg.s**-1.cm**-2.Angstrom**-1'})
        ).toBe('"FLUX_DENSITY" / 1e-16'); // same unit with scalar
        expect(
            getUnitConvExpr({cname: 'FLUX_DENSITY', from: '1e-16erg.s**-1.cm^-2.Angstrom**-1', to: 'W/m^2/um'})
        ).toBe('FLUX_DENSITY'); // mixed power expression
        expect(
            getUnitConvExpr({cname: 'FLUX_DENSITY', from: 'erg/s/cm^2/A', to: 'erg/s/cm^2/Hz'})
        ).toBe('FLUX_DENSITY'); // can't convert between F_LAMBDA and F_NU

        // F_NU ---
        expect(
            getUnitConvExpr({cname: 'SIGNAL', from: 'erg/s/cm^2/Hz', to: 'W/m^2/Hz'})
        ).toBe('"SIGNAL" / 1.0E+3');
        expect(
            getUnitConvExpr({cname: 'SIGNAL', from: 'erg/s/cm^2/Hz', to: 'Jy'})
        ).toBe('"SIGNAL" * 1.0E+23');
        expect(
            getUnitConvExpr({cname: 'SIGNAL', from: 'erg.s**-1.cm**-2.Hz**-1', to: 'Jy'})
        ).toBe('"SIGNAL" * 1.0E+23'); // multiplication expression, same as above

        // F ---
        expect(
            getUnitConvExpr({cname: 'FLUX', from: 'erg/s/cm^2', to: 'W/m^2'})
        ).toBe('"FLUX" / 1.0E+3');
        expect(
            getUnitConvExpr({cname: 'FLUX', from: 'erg/s/cm^2', to: 'Jy*Hz'})
        ).toBe('"FLUX" * 1.0E+23');
        expect(
            getUnitConvExpr({cname: 'FLUX', from: 'erg.s**-1.cm**-2', to: 'Jy.Hz'})
        ).toBe('"FLUX" * 1.0E+23'); // multiplication expression
        expect(
            getUnitConvExpr({cname: 'FLUX', from: 'erg/s/cm^2', to: 'erg/s/cm^2/Hz'})
        ).toBe('FLUX'); // can't convert between F and F_NU
        expect(
            getUnitConvExpr({cname: 'FLUX', from: 'erg/s/cm^2', to: 'erg/s/cm^2/A'})
        ).toBe('FLUX'); // can't convert between F and F_LAMBDA
    });

    test('getUnitInfo: LAMBDA', () => {
        const angstromUnits = ['A', 'Angstrom'];
        const micronsUnits = ['um', 'microns'];
        const validUnits = [...angstromUnits, ...micronsUnits, 'm'];
        const invalidUnits = ['Ang', 'micro'];

        const unitInfos = Object.fromEntries([...validUnits, ...invalidUnits].map(
            (unit) => [unit, getUnitInfo(unit, 'WAVELENGTH')]
        ));

        // Test unitInfo.options
        validUnits.forEach((unit)=>{
            expect(unitInfos[unit].options).toEqual([
                { value: 'A', label: 'Å' },
                { value: 'nm', label: 'nm' },
                { value: 'um', label: 'μm' },
                { value: 'mm', label: 'mm' },
                { value: 'cm', label: 'cm' },
                { value: 'm', label: 'm' }
            ]);
        });
        invalidUnits.forEach((unit)=>{
            expect(unitInfos[unit].options).toEqual([]);
        });

        // Test unitInfo.label
        angstromUnits.forEach((unit)=>{
            expect(unitInfos[unit].label).toBe('λ [Å]');
        });
        micronsUnits.forEach((unit)=>{
            expect(unitInfos[unit].label).toBe('λ [μm]');
        });
        expect(
            validUnits.map((unit) => unitInfos[unit].label)
        ).toEqual(
            ['Å', 'Å', 'μm', 'μm', 'm'].map((symbol)=> `λ [${symbol}]`)
        );
        invalidUnits.forEach((unit)=>{
            expect(unitInfos[unit].label).toBe(`WAVELENGTH [${unit}]`); // falls back to column name with as-is unit
        });
    });

    test('getUnitInfo: F_LAMBDA', () => {
        // note: following are still a subset of all possible representations
        const CGSAngstromUnits = ['erg/s/cm^2/A', 'erg/s/cm^2/Angstrom', 'erg/s/cm**2/Angstrom',
            'erg/s/cm2/Angstrom', 'erg.s**-1.cm**-2.Angstrom**-1'];
        const scalarCGSAngstromUnits = ['1e-16erg/s/cm^2/Angstrom', '1e-16erg.s**-1.cm**-2.Angstrom**-1'];
        const SIMicronUnits = ['W/m^2/um', 'W/m^2/microns', 'W/m**2/microns', 'W.m**-2.microns**-1'];
        const validUnits = [...CGSAngstromUnits, ...scalarCGSAngstromUnits, ...SIMicronUnits];
        const invalidUnits = [
            'erg/s/cm^2/Ang', // unrecognized subunit Ang
            '1e-16erg.s**-1.cm^-2.Angstrom**-1', // mixed power expression
            '1abe-16erg.s**-1.cm**-2.Angstrom**-1' // bad scalar prefix
        ];

        const unitInfos = Object.fromEntries([...validUnits, ...invalidUnits].map(
            (unit) => [unit, getUnitInfo(unit, 'FLUX_DENSITY')]
        ));

        const expectedOptions = [
            { value: 'erg/s/cm^2/A', label: 'erg/s/cm²/Å' },
            { value: 'W/m^2/um', label: 'W/m²/μm' }
        ];
        const expectedScalarOptions = [
            { value: '1e-16erg/s/cm^2/A', label: '1e-16 erg/s/cm²/Å' },
            ...expectedOptions
        ];

        // Test unitInfo.options
        [...CGSAngstromUnits, ...SIMicronUnits].forEach((unit)=>{
            expect(unitInfos[unit].options).toEqual(expectedOptions);
        });
        scalarCGSAngstromUnits.forEach((unit)=>{
            expect(unitInfos[unit].options).toEqual(expectedScalarOptions);
        });
        invalidUnits.forEach((unit)=>{
            expect(unitInfos[unit].options).toEqual([]);
        });

        // Test unitInfo.label
        CGSAngstromUnits.forEach((unit)=>{
            expect(unitInfos[unit].label).toBe('Fλ [erg/s/cm²/Å]');
        });
        scalarCGSAngstromUnits.forEach((unit)=>{
            expect(unitInfos[unit].label).toBe('Fλ [1e-16 erg/s/cm²/Å]');
        });
        SIMicronUnits.forEach((unit)=>{
            expect(unitInfos[unit].label).toBe('Fλ [W/m²/μm]');
        });
        invalidUnits.forEach((unit)=>{
            expect(unitInfos[unit].label).toBe(`FLUX_DENSITY [${unit}]`); // falls back to column name with as-is unit
        });
    });

    test('getUnitInfo: other measurements', () => {
        // Following are testing simple cases, complex cases are already covered in previous tests for LAMBDA and F_LAMBDA

        // NU
        ['Hz', 'KHz', 'MHz', 'GHz'].forEach((unit) => {
            expect(getUnitInfo(unit, 'FREQUENCY')).toEqual(
                {
                    options: [
                        {value: 'Hz', label: 'Hz'},
                        {value: 'KHz', label: 'KHz'},
                        {value: 'MHz', label: 'MHz'},
                        {value: 'GHz', label: 'GHz'}
                    ],
                    label: `𝛎 [${unit}]`
                }
            );
        });

        // F_NU
        ['erg/s/cm^2/Hz', 'erg.s**-1.cm**-2.Hz**-1'].forEach((unit) => {
            expect(getUnitInfo(unit, 'FLUX_DENSITY')).toEqual(
                {
                    options: [
                        { value: 'W/m^2/Hz', label: 'W/m²/Hz' },
                        { value: 'erg/s/cm^2/Hz', label: 'erg/s/cm²/Hz' },
                        { value: 'Jy', label: 'Jy' }
                    ],
                    label: 'F𝛎 [erg/s/cm²/Hz]'
                }
            );
        });

        // F
        ['erg/s/cm^2', 'erg.s**-1.cm**-2'].forEach((unit) => {
            expect(getUnitInfo(unit, 'FLUX')).toEqual(
                {
                    options: [
                        { value: 'W/m^2', label: 'W/m²' },
                        { value: 'erg/s/cm^2', label: 'erg/s/cm²' },
                        { value: 'Jy*Hz', label: 'Jy·Hz' }
                    ],
                    label: '𝛎·F𝛎 [erg/s/cm²]'
                }
            );
        });
    });

    test('getXLabel', () => {
        expect(getXLabel('wavelength', 'A', 'Rest Frame', 'Redshift = 0.456')).toBe('Rest Frame λ [Å]<br>(Redshift = 0.456)');
        expect(getXLabel('frequency', 'Hz', 'Observed Frame')).toBe('Observed Frame 𝛎 [Hz]');
    });

    test('getMeasurementLabel', () => {
        expect(getMeasurementLabel('Hz')).toBe('𝛎');
        expect(getMeasurementLabel('A')).toBe('λ');
        expect(getMeasurementLabel('Angstrom')).toBe('λ'); // 'Angstrom' is an alias for 'A'
        expect(getMeasurementLabel('Ang')).toBe(''); // unrecognized unit
        expect(getMeasurementLabel('m')).toBe('λ');
        expect(getMeasurementLabel('erg/s/cm^2/Hz')).toBe('F𝛎');
        expect(getMeasurementLabel('Jy')).toBe('F𝛎');
        expect(getMeasurementLabel('erg/s/cm^2/A')).toBe('Fλ');
        expect(getMeasurementLabel('erg/s/cm^2')).toBe('𝛎·F𝛎');
    });
});
