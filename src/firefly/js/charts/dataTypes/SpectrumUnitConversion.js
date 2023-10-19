/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get} from 'lodash';
import {sprintf} from '../../externalSource/sprintf.js';

/**
 * Return true if 'from' unit can be converted to 'to'.
 * @param p
 * @param p.from
 * @param p.to
 * @returns {boolean}
 */
export function canUnitConv({from, to}) {
    return !! get(UnitXref, [from, to]);
}

/**
 * Return the SQL-like expression for unit conversion use cases.
 * @param p         parameters
 * @param p.cname   the column name of the value to be converted
 * @param p.from    from unit
 * @param p.to      to unit
 * @param p.alias   the name of this new column
 * @param p.colNames    a list of all the columns in the table.  This is used to format the expression so that column names are quoted correctly.
 * @param p.args    any additional arguments used in the conversion formula.
 * @returns {string}
 */
export function getUnitConvExpr({cname, from, to, alias, args=[]}) {
    const formula = get(UnitXref, [from, to]);
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
 * @param {string} cname        the name of the column being evaluated
 * @returns {Object}
 */
export function getUnitInfo(unit, cname) {
    cname = cname?.match(/^"(.+)"$/)?.[1] ?? cname;          // remove enclosing double-quotes if exists
    const meas =  Object.values(UnitXref.measurement).find((m) => m?.options.find( (o) => o?.value === unit)) || {};
    let label = meas.label ? sprintf(meas.label, unit) : '';
    if (!label && cname) {
        label = cname + (unit ? `[${unit}]` : '');
    }
    return {options: meas.options, label};
}


/*
  Unit conversions, mapping FROM unit -> TO unit, where the formula is an SQL expression.
  The formula is a format string, similar to printf, where the %s is the column name of the value being converted.
  When argument index is needed, it can be referenced as %1$s, %2$s, %3$s, %4$s, etc.
*/

const UnitXref = {
    measurement: {
        frequency: {
            options: [{value:'Hz'}, {value:'KHz'}, {value:'MHz'}, {value:'GHz'}],
            label: '𝛎 [%s]'
        },
        wavelength: {
            options: [{value: 'A'}, {value: 'nm'}, {value:'um'}, {value: 'mm'}, {value:'cm'}, {value:'m'}],
            label: 'λ [%s]'
        },
        flux_density: {
            options: [{value:'W/m^2/Hz'}, {value:'Jy'}],
            label: 'F𝛎 [%s]'
        },
        inband_flux: {
            options: [{value:'W/m^2'}, {value:'Jy*Hz'}],
            label: '𝛎*F𝛎 [%s]'
        }
    },

    // Unit Conversions follow
    // "outer" layer is the unit you *have*; "inner" layer is the unit you *want*

    // frequency
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
    // wavelength
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
    //  flux density
    'W/m^2/Hz' : {
        'W/m^2/Hz' : '%s',
        Jy : '%s * 1.0E+26',
    },
    Jy : {
        'W/m^2/Hz' : '%s / 1.0E+26',
        Jy : '%s',
    },
    //  inband flux
    'W/m^2' : {
        'W/m^2' : '%s',
        'Jy*Hz' : '%s * 1.0E+26',
    },
    'Jy*Hz' : {
        'W/m^2' : '%s / 1.0E+26',
        'Jy*Hz' : '%s',
    }

};

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