/*eslint-env node, jest */
import {
    canUnitConv,
    getUnitConvExpr,
    getUnitOptions,
    getXLabel,
    getYLabel,
    getMeasurementLabel
} from 'firefly/charts/dataTypes/SpectrumUnitConversion';


describe('SpectrumUnitConversion', () => {
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

    describe('getUnitOptions', () => {
        test('LAMBDA measurement', () => {
            // recognized units and their aliases
            ['A', 'Angstrom', 'um', 'microns', 'm'].forEach((unit) => {
                expect(getUnitOptions(unit)).toEqual([
                    { value: 'A', label: '$\\mathrm{\\mathring{A}}$' },
                    { value: 'nm', label: '$\\mathrm{nm}$' },
                    { value: 'um', label: '$\\mathrm{\\mu m}$' },
                    { value: 'mm', label: '$\\mathrm{mm}$' },
                    { value: 'cm', label: '$\\mathrm{cm}$' },
                    { value: 'm', label: '$\\mathrm{m}$' }
                ]);
            });

            // unrecognized units
            ['Ang', 'micro'].forEach((unit) => {
                expect(getUnitOptions(unit)).toEqual([]);
            });
        });

        test('F_LAMBDA measurement', () => {
            [// valid recognized units and their alternative representations (note: following are still a subset of all possible representations)
                'erg/s/cm^2/A', 'erg/s/cm^2/Angstrom', 'erg/s/cm**2/Angstrom', 'erg/s/cm2/Angstrom', 'erg.s**-1.cm**-2.Angstrom**-1', // CGS units with Angstrom subunit
                'W/m^2/um', 'W/m^2/microns', 'W/m**2/microns', 'W.m**-2.microns**-1', // SI units with micron subunit
            ].forEach((unit) => {
                expect(getUnitOptions(unit)).toEqual([
                    { value: 'erg/s/cm^2/A', label: '$\\mathrm{erg/s/cm^{2}/\\mathring{A}}$' },
                    { value: 'W/m^2/um', label: '$\\mathrm{W/m^{2}/\\mu m}$' }
                ]);
            });

            [// scalar (1e-16) prefixed units
                '1e-16erg/s/cm^2/Angstrom', '1e-16erg.s**-1.cm**-2.Angstrom**-1',
            ].forEach((unit) => {
                expect(getUnitOptions(unit)).toEqual([
                    { value: '1e-16erg/s/cm^2/A', label: '$10^{-16}\\,\\mathrm{erg/s/cm^{2}/\\mathring{A}}$' },
                    { value: 'erg/s/cm^2/A', label: '$\\mathrm{erg/s/cm^{2}/\\mathring{A}}$' },
                    { value: 'W/m^2/um', label: '$\\mathrm{W/m^{2}/\\mu m}$' }
                ]);
            });

            [// invalid or unrecognized units
                'erg/s/cm^2/Ang', // unrecognized subunit Ang
                '1e-16erg.s**-1.cm^-2.Angstrom**-1', // mixed power expression
                '1abe-16erg.s**-1.cm**-2.Angstrom**-1' // bad scalar prefix
            ].forEach((unit) => {
                expect(getUnitOptions(unit)).toEqual([]);
            });
        });

        test('Other measurements', () => {
            // Following are testing simple cases, complex cases are already covered in previous tests for LAMBDA and F_LAMBDA
            // NU
            ['Hz', 'KHz', 'MHz', 'GHz'].forEach((unit) => {
                expect(getUnitOptions(unit)).toEqual([
                    {value: 'Hz', label: '$\\mathrm{Hz}$'},
                    {value: 'KHz', label: '$\\mathrm{KHz}$'},
                    {value: 'MHz', label: '$\\mathrm{MHz}$'},
                    {value: 'GHz', label: '$\\mathrm{GHz}$'}
                ]);
            });

            // F_NU
            ['erg/s/cm^2/Hz', 'erg.s**-1.cm**-2.Hz**-1'].forEach((unit) => {
                expect(getUnitOptions(unit)).toEqual([
                    { value: 'W/m^2/Hz', label: '$\\mathrm{W/m^{2}/Hz}$' },
                    { value: 'erg/s/cm^2/Hz', label: '$\\mathrm{erg/s/cm^{2}/Hz}$' },
                    { value: 'Jy', label: '$\\mathrm{Jy}$' }
                ]);
            });

            // F
            ['erg/s/cm^2', 'erg.s**-1.cm**-2'].forEach((unit) => {
                expect(getUnitOptions(unit)).toEqual([
                    { value: 'W/m^2', label: '$\\mathrm{W/m^{2}}$' },
                    { value: 'erg/s/cm^2', label: '$\\mathrm{erg/s/cm^{2}}$' },
                    { value: 'Jy*Hz', label: '$\\mathrm{Jy \\cdot Hz}$' }
                ]);
            });
        });
    });

    test('getXLabel', () => {
        // LAMBDA ---
        expect(
            getXLabel('wavelength', 'A', 'Observed Frame')
        ).toBe('$\\text{Observed Frame }\\lambda\\ [\\mathrm{\\mathring{A}}]$');
        expect(
            getXLabel('wavelength', 'Angstrom', 'Observed Frame') // 'Angstrom' is an alias for 'A'
        ).toBe('$\\text{Observed Frame }\\lambda\\ [\\mathrm{\\mathring{A}}]$');
        expect(
            getXLabel('wavelength', 'um', 'Observed Frame')
        ).toBe('$\\text{Observed Frame }\\lambda\\ [\\mathrm{\\mu m}]$');
        expect(
            getXLabel('wavelength', 'microns', 'Observed Frame') // 'microns' is an alias for 'um'
        ).toBe('$\\text{Observed Frame }\\lambda\\ [\\mathrm{\\mu m}]$');
        expect(
            getXLabel('wavelength', 'micro', 'Observed Frame') // 'micro' is an unrecognized lambda unit
        ).toBe('Observed Frame wavelength [micro]'); // falls back to plain string with column name and as-is unit
        expect(
            getXLabel('wavelength', 'A', 'Rest Frame', 'Redshift = 0.456')
        ).toBe('$\\begin{matrix} \\text{Rest Frame }\\lambda\\ [\\mathrm{\\mathring{A}}] \\\\ \\text{(Redshift = 0.456)} \\end{matrix}$');

        // NU ---
        expect(
            getXLabel('frequency', 'Hz', 'Observed Frame')
        ).toBe('$\\text{Observed Frame }\\nu\\ [\\mathrm{Hz}]$');
        expect(
            getXLabel('frequency', 'MHz', 'Observed Frame')
        ).toBe('$\\text{Observed Frame }\\nu\\ [\\mathrm{MHz}]$');
        expect(
            getXLabel('frequency', '1/s', 'Observed Frame') // '1/s' is an unrecognized nu unit
        ).toBe('Observed Frame frequency [1/s]'); // falls back to plain string with column name and as-is unit

    });

    test('getYLabel', () => {
        // F_LAMBDA ---
        [// CGS units with Angstrom subunit
            'erg/s/cm^2/A', 'erg/s/cm^2/Angstrom', 'erg/s/cm**2/Angstrom', 'erg/s/cm2/Angstrom', 'erg.s**-1.cm**-2.Angstrom**-1'
        ].forEach((unit)=>{
            expect(getYLabel(unit, 'signal')).toBe('$F_{\\lambda}\\ [\\mathrm{erg/s/cm^{2}/\\mathring{A}}]$');
        });

        [// scalar (1e-16) prefixed units
            '1e-16erg/s/cm^2/Angstrom', '1e-16erg.s**-1.cm**-2.Angstrom**-1',
        ].forEach((unit)=>{
            expect(getYLabel(unit, 'signal')).toBe('$F_{\\lambda}\\ [10^{-16}\\,\\mathrm{erg/s/cm^{2}/\\mathring{A}}]$');
        });

        [// SI units with micron subunit
            'W/m^2/um', 'W/m^2/microns', 'W/m**2/microns', 'W.m**-2.microns**-1',
        ].forEach((unit)=>{
            expect(getYLabel(unit, 'signal')).toBe('$F_{\\lambda}\\ [\\mathrm{W/m^{2}/\\mu m}]$');
        });

        [// invalid or unrecognized units
            'erg/s/cm^2/Ang', // unrecognized subunit Ang
            '1e-16erg.s**-1.cm^-2.Angstrom**-1', // mixed power expression
            '1abe-16erg.s**-1.cm**-2.Angstrom**-1' // bad scalar prefix
        ].forEach((unit)=>{
            expect(getYLabel(unit, 'signal')).toBe(`signal [${unit}]`); // falls back to plain string with column name and as-is unit
        });

        // Other measurements ---
        [// F_NU in CGS units with Hz subunit
            'erg/s/cm^2/Hz', 'erg.s**-1.cm**-2.Hz**-1'
        ].forEach(()=>{
            expect(getYLabel('erg/s/cm^2/Hz', 'signal')).toBe('$F_{\\nu}\\ [\\mathrm{erg/s/cm^{2}/Hz}]$');
        });
        // F_NU in Jy
        expect(getYLabel('Jy', 'signal')).toBe('$F_{\\nu}\\ [\\mathrm{Jy}]$');
        // F in CGS units
        expect(getYLabel('erg/s/cm^2', 'signal')).toBe('$\\nu \\cdot F_{\\nu}\\ [\\mathrm{erg/s/cm^{2}}]$');
    });

    test('getMeasurementLabel', () => {
        expect(getMeasurementLabel('Hz')).toBe('$\\nu$');
        expect(getMeasurementLabel('A')).toBe('$\\lambda$');
        expect(getMeasurementLabel('Angstrom')).toBe('$\\lambda$'); // 'Angstrom' is an alias for 'A'
        expect(getMeasurementLabel('Ang')).toBe(''); // unrecognized unit
        expect(getMeasurementLabel('m')).toBe('$\\lambda$');
        expect(getMeasurementLabel('erg/s/cm^2/Hz')).toBe('$F_{\\nu}$');
        expect(getMeasurementLabel('Jy')).toBe('$F_{\\nu}$');
        expect(getMeasurementLabel('erg/s/cm^2/A')).toBe('$F_{\\lambda}$');
        expect(getMeasurementLabel('erg/s/cm^2')).toBe('$\\nu \\cdot F_{\\nu}$');
    });
});
