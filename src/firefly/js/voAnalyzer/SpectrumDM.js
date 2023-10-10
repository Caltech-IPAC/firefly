/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty} from 'lodash';
import {getColumnByRef} from '../tables/TableUtil.js';


const axisData = {
    value: 'Value',
    statError: 'Accuracy.StatError',
    statErrLow: 'Accuracy.StatErrLow',
    statErrHigh: 'Accuracy.StatErrHigh',
};
const spectralAxisDef = {
    prefix: ['spec:Spectrum.Data.SpectralAxis', 'spec:Data.SpectralAxis', 'ipac:Spectrum.Data.SpectralAxis', 'ipac:Data.SpectralAxis'],
    /** @type {DataAxis} */
    data: {
        ...axisData,
        binLow: 'Accuracy.BinLow',
        binHigh: 'Accuracy.BinHigh',
        order: 'Order',
        relOrder: 'RelOrder'
    }
};
const fluxAxisDef = {
    prefix: ['spec:Spectrum.Data.FluxAxis', 'spec:Data.FluxAxis', 'ipac:Spectrum.Data.FluxAxis', 'ipac:Data.FluxAxis'],
    /** @type {DataAxis} */
    data: {
        ...axisData,
        upperLimit: 'Accuracy.UpperLimit',
        lowerLimit: 'Accuracy.LowerLimit',
    }
};
const timeAxisDef = {
    prefix: ['spec:Spectrum.Data.TimeAxis', 'spec:Data.TimeAxis'],
    /** @type {DataAxis} */
    data: {
        ...axisData,
        binLow: 'Accuracy.BinLow',
        binHigh: 'Accuracy.BinHigh',
    }
};

/**
 *
 * @param tableModel
 * @returns {SpectrumDM|undefined}
 */
export function getSpectrumDM(tableModel) {
    const utype = tableModel?.tableMeta?.utype?.toLowerCase();
    const isSpectrum = utype === 'spec:Spectrum'.toLowerCase();

    const isSED = utype === 'ipac:Spectrum.SED'.toLowerCase();

    if (!isSpectrum && !isSED) return;

    const findAxisData = (axis) => {
        const data = {};
        Object.entries(axis.data).forEach(([key, utype]) => {
            const col = findColByUtype(tableModel, axis.prefix, utype);
            if (col) {
                data[key] = col.name;
                if (key === 'value') {      // defaults to column's attribs if not given as params
                    data.ucd = findParamByUtype(tableModel, axis.prefix, 'UCD')?.value || col.UCD;
                    data.unit = findParamByUtype(tableModel, axis.prefix, 'Unit')?.value || col.units;
                }
            }
        });

        return isEmpty(data) ? undefined : fixStatErr(data);
    };
    const fixStatErr = (axis) => {
        if (!axis) return;
        const {statError, statErrLow, statErrHigh} = axis;
        if (statError) {
            axis.statErrLow = axis.statErrHigh = undefined;         // if statError is defined, ignore low/high
        } else if (!statErrLow !== !statErrHigh) {                  // logical xor equivalent (only one with value)
            axis.statError = statErrLow || statErrHigh;             // treat it as statError
            axis.statErrLow = axis.statErrHigh = undefined;
        }
        return axis;
    };

    const spectralAxis = findAxisData(spectralAxisDef);
    const fluxAxis = findAxisData(fluxAxisDef);
    const timeAxis = findAxisData(timeAxisDef);

    if (spectralAxis && (fluxAxis || timeAxis)) {
        return {spectralAxis, fluxAxis, timeAxis, isSED};
    }
}

function findColByUtype(tableModel, prefixes, suffix) {

    const cols = allRefCols(tableModel, tableModel?.groups) || [];
    for (const p of prefixes) {
        const utype = p + (suffix ? '.' + suffix : '');
        const col = cols.find((c) => c?.utype?.toLowerCase() === utype.toLowerCase());
        if (col) return col;
    }
}

function findParamByUtype(tableModel, prefixes, suffix) {

    const params = allParams(tableModel?.groups) || [];
    for (const p of prefixes) {
        const utype = p + (suffix ? '.' + suffix : '');
        const param = params.find((c) => c?.utype?.toLowerCase() === utype.toLowerCase());
        if (param) return param;
    }
}

function allRefCols(tableModel, groups) {
    if (!Array.isArray(groups)) return;
    let cols = [];
    for (const g of groups) {
        cols = cols.concat(g?.columnRefs?.map((r) => {
            const col = getColumnByRef(tableModel, r?.ref);
            if (col) {
                if (r?.utype) col.utype = r.utype;
                if (r?.UCD) col.UCD = r.UCD;
            }
            return col;
        }) || []);
        if (g?.groups) {
            cols = cols.concat(allRefCols(tableModel, g.groups));
        }
    }
    return cols;
}

function allParams(groups) {
    if (!Array.isArray(groups)) return;
    let params = [];
    for (const g of groups) {
        if (Array.isArray(g?.params)) {
            params = params.concat(g.params);
        }
        if (g?.groups) {
            params = params.concat(allParams(g.groups));
        }
    }
    return params;
}