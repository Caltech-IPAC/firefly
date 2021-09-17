/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get} from 'lodash';
import {sprintf} from '../../externalSource/sprintf.js';



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
 * @param {string} isSpectral   true if this unit is for a spectral axis
 * @returns {Object}
 */
export function getUnitInfo(unit, isSpectral=true) {
    const meas =  Object.values(UnitXref.measurement).find((m) => m?.options.find( (o) => o?.value === unit)) || {};
    const label = meas.label ? sprintf(meas.label, unit) : isSpectral ? `𝛎 [${unit || 'Hz'}]` : `F𝛎 [${unit || 'Jy'}]`;
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
        nm : '%s / 10.0',
        um : '%s / 10000.0',
        mm : '%s / 1.00E+7',
        cm : '%s / 1.00E+8',
        m  : '%s / 1.00E+10'
    },
    nm : {
        A  : '%s * 10.0',
        nm : '%s',
        um : '%s / 1000.0',
        mm : '%s / 1.00E+6',
        cm : '%s / 1.00E+7',
        m  : '%s / 1.00E+9'
    },
    um : {
        A  : '%s * 10000.0',
        nm : '%s * 1000.0',
        um : '%s',
        mm : '%s / 1000.0',
        cm : '%s / 10000.0',
        m  : '%s / 1.00E+6'
    },
    mm : {
        A  : '%s * 1.00E+7',
        nm : '%s * 1.00E+6',
        um : '%s * 1000.0',
        mm : '%s',
        cm : '%s / 10.0',
        m  : '%s / 1000.0'
    },
    cm : {
        A  : '%s * 1.00E+8',
        nm : '%s * 1.00E+7',
        um : '%s * 10000.0',
        mm : '%s * 10.0',
        cm : '%s',
        m  : '%s / 100.0'
    },
    m  : {
        A  : '%s * 1.00E+10',
        nm : '%s * 1.00E+9',
        um : '%s * 1.00E+6',
        mm : '%s * 1000.0',
        cm : '%s * 100.0',
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
