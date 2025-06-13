/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {sprintf} from '../../externalSource/sprintf.js';
import {WAVELENGTH_UNITS} from 'firefly/visualize/VisUtil';

/**
 * Return true if 'from' unit can be converted to 'to'.
 * @param p
 * @param p.from
 * @param p.to
 * @returns {boolean}
 */
export function canUnitConv({from, to}) {
    const fromKey = normalizeUnit(from);
    const toKey = normalizeUnit(to);
    return !!(fromKey && toKey && UnitXref?.[fromKey]?.[toKey]);
}

/**
 * Return the SQL-like expression for unit conversion use cases.
 * @param p         parameters
 * @param p.cname   the column name of the value to be converted
 * @param p.from    from unit
 * @param p.to      to unit
 * @param p.alias   the name of this new column
 * @param p.args    any additional arguments used in the conversion formula.
 * @returns {string}
 */
export function getUnitConvExpr({cname, from, to, alias, args=[]}) {
    const fromKey = normalizeUnit(from);
    const toKey = normalizeUnit(to);
    const formula = UnitXref?.[fromKey]?.[toKey] ?? '';

    let colOrExpr = cname;
    if (formula) {
        colOrExpr = sprintf(formula.replace(/(%([0-9]\$)?s)/g, '"$1"'), cname, ...args);
    }
    colOrExpr = alias ? `${colOrExpr}  as ${alias}` : colOrExpr;
    return colOrExpr;
}

/**
 * returns an object containing the axis label and an array of options for unit conversion.
 * @param {string} unit         the unit to get the info for
 * @param {string} cname        the name (or expression) of the column being evaluated
 * @returns {Object}
 */
export function getUnitInfo(unit, cname) {
    let options = [];
    let label = '';

    const unitKey = normalizeUnit(unit);
    if (unitKey) {
        const measurementKey = UnitMetadata[unitKey]?.type;
        options = Object.entries(UnitMetadata)
            .filter(([,meta]) => meta?.type === measurementKey) // can only convert units of the same measurement type
            .map(([key, meta]) => ({value: key, label: meta.label || key}));

        const unitLabel = UnitMetadata[unitKey]?.label || unitKey;
        const formattedLabel = Measurement[measurementKey]?.axisLabel;
        if (formattedLabel) label = sprintf(formattedLabel, unitLabel);
    }
    else {
        cname = cname?.match(/^"(.+)"$/)?.[1] ?? cname;  // remove enclosing double-quotes if exists
        if (cname) label = cname + (unit ? ` [${unit}]` : '');
    }

    return {options, label};
}


/**
 * returns X axis label using the required information like unit, spectral frame options, redshift, etc.
 * @param {string} cname         the name of the column being evaluated
 * @param {string} unit          the unit to get the info for
 * @param {string} sfLabel       the label of spectral frame selected
 * @param {string} redshiftLabel the label of redshift if rest frame, optional
 * @returns {string}
 */
export const getXLabel = (cname, unit, sfLabel, redshiftLabel='') => {
    const unitLabel = getUnitInfo(unit, cname).label;
    return `${sfLabel} ${unitLabel}${redshiftLabel ? `<br>(${redshiftLabel})` : ''}`;
};



/**
 * @typedef {string} UnitKey
 * Unique identifier for a unit, used as a key in UnitXref and UnitMetadata.
 */

 /**
  Unit conversions, mapping FROM unit -> TO unit, where the formula is an SQL expression.
  The formula is a format string, similar to printf, where the %s is the column name of the value being converted.
  When argument index is needed, it can be referenced as %1$s, %2$s, %3$s, %4$s, etc.

  Note: "outer" layer is the unit you *have*; "inner" layer is the unit you *want*
  @type {Object.<UnitKey, Object.<UnitKey, string>>}
**/
const UnitXref = {
    // frequency -------------
    Hz : {
        Hz  : '%s',
        KHz : '%s / 1000.0',
        MHz : '%s / 1000000.0',
        GHz : '%s / 1000000000.0'
    },
    KHz : {
        Hz  : '%s * 1000.0',
        KHz : '%s',
        MHz : '%s / 1000.0',
        GHz : '%s / 1000000.0'
    },
    MHz : {
        Hz  : '%s * 1000000.0',
        KHz : '%s * 1000.0',
        MHz : '%s',
        GHz : '%s / 1000.0'
    },
    GHz : {
        Hz  : '%s * 1000000000.0',
        KHz : '%s * 1000000.0',
        MHz : '%s * 1000.0',
        GHz : '%s'
    },
    // wavelength -------------
    A : {
        A  : '%s',
        nm : '%s / 10',
        um : '%s / 1.0E+4',
        mm : '%s / 1.0E+7',
        cm : '%s / 1.0E+8',
        m  : '%s / 1.0E+10'
    },
    nm : {
        A  : '%s * 10',
        nm : '%s',
        um : '%s / 1.0E+3',
        mm : '%s / 1.0E+6',
        cm : '%s / 1.0E+7',
        m  : '%s / 1.0E+9'
    },
    um : {
        A  : '%s * 1.0E+4',
        nm : '%s * 1.0E+3',
        um : '%s',
        mm : '%s / 1.0E+3',
        cm : '%s / 1.0E+4',
        m  : '%s / 1.0E+6'
    },
    mm : {
        A  : '%s * 1.0E+7',
        nm : '%s * 1.0E+6',
        um : '%s * 1.0E+3',
        mm : '%s',
        cm : '%s / 10',
        m  : '%s / 1.0E+3'
    },
    cm : {
        A  : '%s * 1.0E+8',
        nm : '%s * 1.0E+7',
        um : '%s * 1.0E+4',
        mm : '%s * 10',
        cm : '%s',
        m  : '%s / 100'
    },
    m  : {
        A  : '%s * 1.0E+10',
        nm : '%s * 1.0E+9',
        um : '%s * 1.0E+6',
        mm : '%s * 1.0E+3',
        cm : '%s * 100',
        m  : '%s'
    },
    // flux density in frequency space -------------
    'W/m^2/Hz' : {
        'W/m^2/Hz' : '%s',
        'erg/s/cm^2/Hz': '%s * 1.0E+3',
        Jy : '%s * 1.0E+26',
    },
    'erg/s/cm^2/Hz' : {
        'W/m^2/Hz': '%s / 1.0E+3',
        'erg/s/cm^2/Hz' : '%s',
        Jy : '%s * 1.0E+23',
    },
    Jy : {
        'W/m^2/Hz' : '%s / 1.0E+26', //SI units
        'erg/s/cm^2/Hz': '%s / 1.0E+23', //CGS units
        Jy : '%s',
    },
    // flux density in wavelength space -------------
    'erg/s/cm^2/A' : {
        'erg/s/cm^2/A' : '%s',
        'W/m^2/um': '%s * 10'
    },
    'W/m^2/um' : {
        'erg/s/cm^2/A' : '%s / 10',
        'W/m^2/um' : '%s',
    },
    // inband flux (independent of frequency or wavelength) -------------
    'W/m^2' : {
        'W/m^2' : '%s',
        'erg/s/cm^2' : '%s * 1.0E+3',
        'Jy*Hz' : '%s * 1.0E+26',
    },
    'erg/s/cm^2' : {
        'W/m^2' : '%s / 1.0E+3',
        'erg/s/cm^2' : '%s',
        'Jy*Hz' : '%s * 1.0E+23',
    },
    'Jy*Hz' : {
        'W/m^2' : '%s / 1.0E+26',
        'erg/s/cm^2' : '%s / 1.0E+23',
        'Jy*Hz' : '%s',
    },
};


/**
 * @typedef {string} MeasurementKey - unique identifier for a measurement type, used as a key in Measurement.
 */

/**
 * @typedef {Object} Measurement
 * @property {MeasurementKey} key - the key of the measurement type
 * @property {string} value - the value of the measurement type
 * @property {string} axisLabel - the label for the axis, formatted with a placeholder for the unit
 */

/**Type of measurements by which units are grouped.
 * @type {Object.<MeasurementKey, Measurement>}
 */
const Measurement = {
    NU: {key: 'NU', value: 'frequency', axisLabel: '𝛎 [%s]'},
    LAMBDA: {key: 'LAMBDA', value: 'wavelength', axisLabel: 'λ [%s]'},
    F_NU: {key: 'F_NU', value: 'flux_density_frequency', axisLabel: 'F𝛎 [%s]'},
    F_LAMBDA: {key: 'F_LAMBDA', value: 'flux_density_wavelength', axisLabel: 'Fλ [%s]'},
    F: {key: 'F', value: 'inband_flux', axisLabel: '𝛎·F𝛎 [%s]'},
};


/**
 * @typedef {Object} UnitMetadata
 * @property {MeasurementKey} type - the type of measurement this unit belongs to, one of the keys in `Measurement`.
 * @property {string} label - the label for the unit, used in dropdown options and axis labels. If undefined, the key
 * is used as the label.
 * @property {Array<string>} aliases - list of aliases for the unit, used for matching besides the key and label.
 */

/**
 * Metadata of each unit, including its type, label, and aliases. Keys are the same as in UnitXref.
 * @type {Object.<UnitKey, UnitMetadata>}
 * **/
const UnitMetadata = {
    // frequency -------------
    Hz : {
        type: Measurement.NU.key,
    },
    KHz : {
        type: Measurement.NU.key,
    },
    MHz : {
        type: Measurement.NU.key,
    },
    GHz : {
        type: Measurement.NU.key,
    },
    // wavelength -------------
    A : {
        type: Measurement.LAMBDA.key,
        label: WAVELENGTH_UNITS.angstrom.symbol,
        aliases: ['Angstrom', 'angstrom']
    },
    nm : {
        type: Measurement.LAMBDA.key,
    },
    um : {
        type: Measurement.LAMBDA.key,
        label: WAVELENGTH_UNITS.um.symbol,
        aliases: ['micron', 'microns']
    },
    mm : {
        type: Measurement.LAMBDA.key,
    },
    cm : {
        type: Measurement.LAMBDA.key,
    },
    m  : {
        type: Measurement.LAMBDA.key,
    },
    // flux density in frequency space -------------
    'W/m^2/Hz' : {
        type: Measurement.F_NU.key,
        label: 'W/m²/Hz',
    },
    'erg/s/cm^2/Hz' : {
        type: Measurement.F_NU.key,
        label: 'erg/s/cm²/Hz',
    },
    Jy : {
        type: Measurement.F_NU.key,
    },
    // flux density in wavelength space -------------
    'erg/s/cm^2/A' : {
        type: Measurement.F_LAMBDA.key,
        label: `erg/s/cm²/${WAVELENGTH_UNITS.angstrom.symbol}`,
    },
    'W/m^2/um' : {
        type: Measurement.F_LAMBDA.key,
        label: `W/m²/${WAVELENGTH_UNITS.um.symbol}`,
    },
    // inband flux (independent of frequency or wavelength) -------------
    'W/m^2' : {
        type: Measurement.F.key,
        label: 'W/m²',
    },
    'erg/s/cm^2' : {
        type: Measurement.F.key,
        label: 'erg/s/cm²',
    },
    'Jy*Hz' : {
        type: Measurement.F.key,
        label: 'Jy·Hz',
    },
};


/**
 * Maps any unit representation (value/label/alias) back to a key in UnitXref (and UnitMetadata).
 * @param u {string} - the unit to normalize
 * @return {UnitKey|null} - the key in UnitXref if found, otherwise null
 */
function normalizeUnit(u) {
    if (!u) return null;
    // u is already a key in UnitXref
    if (UnitXref[u]) return u;

    for (const [key, meta] of Object.entries(UnitMetadata)) {
        // u is an alias of a key in UnitXref
        const aliases = meta?.aliases ?? [];
        aliases.push(...getFluxAliases(key));
        if (aliases.some((alias) => alias === u)) return key;

        // u is a label of a key in UnitXref
        if (meta?.label === u) return key;
    }
    return null;
}

/* Get all possible representations (key, label, aliases) for a given unitKey */
function getAllRepresentations(unitKey, includeLabel=true) {
    const meta = UnitMetadata[unitKey];
    if (!meta) return []; //invalid unitKey
    const reps = [unitKey]; // include the key itself
    if (includeLabel && meta.label && meta.label !== unitKey) reps.push(meta.label);
    if (Array.isArray(meta.aliases)) reps.push(...meta.aliases);
    return reps;
}

/**
 * Decomposes a unit string into its base units and their powers.
 * @param unit {string} - the unit string to decompose, e.g. 'erg/s/cm^2/A'
 * @returns {Map<any, any>} - a Map where keys are base units and values are their powers, e.g.
 * `Map{'erg'=> 1, 's'=> -1, 'cm'=> -2, 'A'=> -1}`
 */
function decomposeUnit(unit) {
    const pattern = /([*./]?)([A-Za-z]+)(?:\^([+-]?\d+))?/g;
    const result = new Map(); // to preserve the order of units
    const matches = unit.matchAll(pattern);
    for (const match of matches) {
        const [, operator, baseUnit, exponent] = match;
        let power = exponent ? Number(exponent) : 1;
        if (operator === '/') power = -power; // '/' implies negative exponent, '*' or '.' implies positive exponent
        const prevPower = result.get(baseUnit) || 0;
        result.set(baseUnit, prevPower + power);
    }
    return result;
}

/**
 * Get all possible aliases for a flux unit, this includes permutations of its expression notations and its consisting
 * units' aliases.
 * @param unitKey {UnitKey}
 * @returns {Array<string>}
 */
const getFluxAliases = (unitKey) => {
    if (!UnitMetadata[unitKey]?.type.startsWith('F')) return []; //not a flux unit

    let fluxRepresentations;
    if (UnitMetadata[unitKey]?.type.startsWith('F_')) { //flux density (in nu or lambda)
        const nuOrLambdaUnitKey = unitKey.substring(unitKey.lastIndexOf('/')+1); // extract the last part after '/'
        // nu or lambda's representations propagate in the flux density representations
        // e.g. erg/s/cm^2/A -> erg/s/cm^2/Angstrom, erg/s/cm^2/angstrom, because of the 'A' unit's alternative representations
        fluxRepresentations = getAllRepresentations(nuOrLambdaUnitKey, false).map(
            (rep) => unitKey.replace(nuOrLambdaUnitKey, rep));
    }
    else {
        fluxRepresentations = [unitKey]; // inband flux remains the same since it is independent of nu or lambda
    }

    const aliases = [];
    for (const fluxUnit of fluxRepresentations) { // generate all possible expression notations for the flux unit
        const asteriskPowerExpr = fluxUnit.replaceAll('^', '**'); // e.g. erg/s/cm**2/A
        const invisiblePowerExpr = fluxUnit.replaceAll('^', ''); // e.g. erg/s/cm2/A

        const multiplicationExpr = Array.from(decomposeUnit(fluxUnit).entries())
            .map(([unit, power]) => power === 1 ? unit : `${unit}**${power}`)
            .join('.'); // e.g. erg.s**-1.cm**-2.A**-1

        // TODO: also support numerical scale-factor in section 2.10 in  https://ivoa.net/documents/VOUnits/20231215/REC-VOUnits-1.1.pdf
        //  possibly alias function can return a boolean to do such matching
        aliases.push(fluxUnit, asteriskPowerExpr, invisiblePowerExpr, multiplicationExpr);
    }
    return Array.from(new Set(aliases)); // remove duplicates
};
