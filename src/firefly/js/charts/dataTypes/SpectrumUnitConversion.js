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
 * @property {Array<string>} aliases - an object containing aliases for the unit, used for case-insensitive matching. If
 * undefined, only the key (and label) is used for matching.
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
        aliases: ['angstrom', 'angstroms'] //case-insensitive
    },
    nm : {
        type: Measurement.LAMBDA.key,
    },
    um : {
        type: Measurement.LAMBDA.key,
        label: WAVELENGTH_UNITS.um.symbol,
        aliases: ['micron', 'microns'] //case-insensitive
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
        aliases: [], //TODO: generate aliases in dot product notation with **, as it is with ** for powers
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
 * Maps any unit value/label/alias back to a key in UnitXref (and UnitMetadata).
 * @param u {string} - the unit to normalize
 * @return {UnitKey|null} - the key in UnitXref if found, otherwise null
 */
function normalizeUnit(u) {
    if (!u) return null;
    // u is already a key in UnitXref
    if (UnitXref[u]) return u;

    for (const [key, meta] of Object.entries(UnitMetadata)) {
        // u is a case-insensitive alias of a key in UnitXref
        if (meta?.aliases?.some((alias) => alias.toLowerCase() === u.toLowerCase())) return key;

        // u is a label of a key in UnitXref
        if (meta?.label === u) return key;
    }
    return null;
}
