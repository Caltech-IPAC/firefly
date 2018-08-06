/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import Validate from '../../util/Validate.js';
import FieldGroupCntlr, {INIT_FIELD_GROUP} from '../../fieldGroup/FieldGroupCntlr.js';
import ImagePlotCntlr from '../ImagePlotCntlr.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import RangeValues, {STRETCH_LINEAR, STRETCH_ASINH, PERCENTAGE, ABSOLUTE, SIGMA, ZSCALE} from './../RangeValues.js';


const clone = (obj,params={}) => Object.assign({},obj,params);
const cloneWithValue= (field,v) => Object.assign({}, field, {value:v});


export const RED_PANEL= 'redPanel';
export const GREEN_PANEL= 'greenPanel';
export const BLUE_PANEL= 'bluePanel';
export const RGB_HUEPRESERVE_PANEL= 'rgbHuePreservePanel';
export const NO_BAND_PANEL= 'nobandPanel';



/**
 * Reducer entry point. This function returns a reducer initialized to work with the passed band.
 * @param {Band} band
 * @return a reducer function. a standard type reducer function.
 */
export function colorPanelChange(band) {
    return (fields,action) => {
        if (!fields || !Object.keys(fields).length) fields= getFieldInit();
        const plot= primePlot(visRoot());
        if (!plot || !plot.plotState.isBandUsed(band)) return fields;

        const fitsData= plot.webFitsData[band.value];
        const plottedRV= plot.plotState.getRangeValues(band);
        if (!fitsData || !plottedRV) return fields;

        return computeColorPanelState(fields, plottedRV, fitsData, band, action);
    };
}


/**
 *
 * Recompute the state. If there is a field value change then update the fields based on that.
 * If there is a replot or a init the update all the fields based on the current plot
 * @param fields
 * @param plottedRV
 * @param fitsData
 * @param band
 * @param action
 * @return {*}
 */
function computeColorPanelState(fields, plottedRV, fitsData, band, action) {

    switch (action.type) {
        case FieldGroupCntlr.VALUE_CHANGE:
            const valid= Object.keys(fields).every( (key) => {
                return !fields[key].mounted ? true :  fields[key].valid;
            } );
            if (!valid) return fields;
            return syncFields(fields,makeRangeValuesFromFields(fields),fitsData);

        case ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW:
        case INIT_FIELD_GROUP:
        case ImagePlotCntlr.ANY_REPLOT:
            if (!plottedRV && !fitsData) return fields;
            // no update if hue-preserving: gammaOrStretch is a reused field
            if (get(plottedRV, 'rgbPreserveHue', 0) > 0) return fields;
            const newFields = updateFieldsFromRangeValues(fields,plottedRV);
            return syncFields(newFields,plottedRV,fitsData);

    }
    return fields;

}


/**
 * Sync the fields so that are consistent with the passed RangeValues object.
 * @param fields
 * @param rv
 * @param fitsData
 */
function syncFields(fields,rv,fitsData) {
    const newFields= clone(fields);
    if (Number.parseInt(rv.lowerWhich)!==ZSCALE) {
        newFields.lowerRange= clone(fields.lowerRange, computeLowerRangeField(fields,rv,fitsData));
        newFields.upperRange= clone(fields.upperRange, computeUpperRangeField(fields,rv,fitsData));
    }

    newFields.algorithm= clone(fields.algorithm, {value: rv.algorithm});
    newFields.lowerWhich=   clone(fields.lowerWhich, {value: rv.lowerWhich});
    newFields.upperWhich=   clone(fields.upperWhich, {value: rv.upperWhich});

    if (newFields.lowerWhich.value===ZSCALE) newFields.lowerWhich.value= PERCENTAGE;
    if (newFields.upperWhich.value===ZSCALE) newFields.upperWhich.value= PERCENTAGE;

    return  newFields;
}

/**
 * This function creates new 'lowerRange' field based on the 'lowerWhich' value. The min and max will be different
 * depending on what 'lowerWhich' is set to.
 * @param {object} fields - map of fields
 * @param {RangeValues} rv
 * @param {WebFitsData} fitsData
 * @param {String} lowerWhich - field key for the type of lower range
 * @param {String} lowerRange - field key for lower range field
*/
function computeLowerRangeField(fields,rv,fitsData,lowerWhich='lowerWhich', lowerRange='lowerRange') {
    const resetDefault= (fields[lowerWhich].value!==rv.lowerWhich);
    let retval;
    switch (rv.lowerWhich) {
        case PERCENTAGE:
        default :
            retval= {validator: Validate.floatRange.bind(null, 0, 100, 1, 'Lower range'),
                value : resetDefault ? 1 : fields[lowerRange].value
            };
            break;
        case ABSOLUTE:
            retval= {validator: Validate.floatRange.bind(null,fitsData.dataMin, fitsData.dataMax, 3, 'Lower range'),
                value : resetDefault ? fitsData.dataMin : fields[lowerRange].value
            };
            break;
        case SIGMA:
            retval= {validator: Validate.isFloat.bind(null,'Lower range'),
                value : resetDefault ? -2 : fields[lowerRange].value
            };
            break;
    }
    return retval;
}

/**
 * This function creates new 'upperRange' field based on the 'upperWhich' value. The min and max will be different
 * depending on what 'upperWhich' is set to.
 * @param fields
 * @param rv
 * @param fitsData
 * @return {*}
 */
function computeUpperRangeField(fields,rv,fitsData) {
    let retval;
    const resetDefault= (fields.upperWhich.value!==rv.upperWhich);
    switch (rv.upperWhich) {
        case PERCENTAGE:
        default :
            retval= {validator: Validate.floatRange.bind(null, 0, 100, 1, 'Upper range'),
                value : resetDefault ? 99 : fields.upperRange.value
            };
            break;

        case ABSOLUTE:
            retval= {validator: Validate.floatRange.bind(null,fitsData.dataMin, fitsData.dataMax, 3, 'Upper range'),
                value : resetDefault ? fitsData.dataMax : fields.upperRange.value
            };
            break;
        case SIGMA:
            retval= {validator: Validate.isFloat.bind(null,'Upper range'),
                value : resetDefault ? 10 : fields.upperRange.value
            };
            break;
    }
    return retval;
}

/**
 * Make a new RangeValue object from the field settings.
 * @param fields
 * @return {RangeValues}
 */
function makeRangeValuesFromFields(fields) {

    const lowerWhich= (fields.zscale.value==='zscale') ? ZSCALE : fields.lowerWhich.value;
    const upperWhich= (fields.zscale.value==='zscale') ? ZSCALE : fields.upperWhich.value;

    return new RangeValues(
            lowerWhich,
            fields.lowerRange.value,
            upperWhich,
            fields.upperRange.value,
            fields.asinhQ.value,
            fields.gamma.value,
            fields.algorithm.value,
            fields.zscaleContrast.value,
            fields.zscaleSamples.value,
            fields.zscaleSamplesPerLine.value);
}

/**
 * Update all the fields from a RangeValues object.
 * @param fields
 * @param {RangeValues} rv
 */
function updateFieldsFromRangeValues(fields,rv) {
    fields= clone(fields);
    fields.lowerWhich= cloneWithValue(fields.lowerWhich, rv.lowerWhich===ZSCALE ? PERCENTAGE : rv.lowerWhich);
    fields.lowerRange= cloneWithValue(fields.lowerRange, rv.lowerValue);
    fields.upperWhich= cloneWithValue(fields.upperWhich, rv.lowerWhich===ZSCALE ? PERCENTAGE : rv.upperWhich);
    fields.upperRange= cloneWithValue(fields.upperRange, rv.upperValue);
    fields.asinhQ= cloneWithValue(fields.asinhQ, rv.asinhQValue);
    fields.gamma= cloneWithValue(fields.gamma, rv.gammaOrStretch);
    fields.algorithm= cloneWithValue(fields.algorithm, rv.algorithm);
    fields.zscaleContrast= cloneWithValue(fields.zscaleContrast, rv.zscaleContrast);
    fields.zscaleSamples= cloneWithValue(fields.zscaleSamples, rv.zscaleSamples);
    fields.zscaleSamplesPerLine= cloneWithValue(fields.zscaleSamplesPerLine, rv.zscaleSamplesPerLine);
    fields.zscale= cloneWithValue(fields.zscale,  rv.lowerWhich===ZSCALE ? 'zscale' : '');
    return fields;
}


/**
 * defines fields
 * @return {{lowerRange: {fieldKey: string, value: string, validator: (function(this:null)), tooltip: string, label: string}, upperRange: {fieldKey: string, value: string, validator: (function(this:null)), tooltip: string, label: string}, zscaleContrast: {fieldKey: string, value: string, validator: (function(this:null)), tooltip: string, label: string}, zscaleSamples: {fieldKey: string, value: string, validator: (function(this:null)), tooltip: string, label: string}, zscaleSamplesPerLine: {fieldKey: string, value: string, validator: (function(this:null)), tooltip: string, label: string}, asinhQ: {fieldKey: string, value: string, validator: (function(this:null)), tooltip: string, label: string}, gamma: {fieldKey: string, value: string, validator: (function(this:null)), tooltip: string, label: string}, BP: {fieldKey: string, value: string, validator: (function(this:null)), tooltip: string, label: string}, WP: {fieldKey: string, value: string, validator: (function(this:null)), tooltip: string, label: string}, algorithm: {tooltip: string, label: string, value}, lowerWhich: {tooltip: string, label: string, value}, upperWhich: {tooltip: string, label: string, value}, zscale: {value: string, tooltip: string, label: string}}}
 */
function getFieldInit() {

    return {
        lowerRange: {
            fieldKey: 'lowerRange',
            value: '0',
            validator: Validate.floatRange.bind(null, 0, 100, 1, 'Lower range'),
            tooltip: 'Lower range of the Stretch',
            label: 'Lower range:'
        },
        upperRange: {
            fieldKey: 'upperRange',
            value: '99',
            validator: Validate.floatRange.bind(null, 0, 100, 1, 'Upper range'),
            tooltip: 'Upper range of the Stretch',
            label: 'Upper range:'
        },
        zscaleContrast: {
            fieldKey: 'zscaleContrast',
            value: '25',
            validator: Validate.intRange.bind(null, 1, 100, 'Contrast (%)'),
            tooltip: 'Zscale algorithm contrast parameter',
            label: 'Contrast (%):'
        },
        zscaleSamples: {
            fieldKey: 'zscaleSamples',
            value: '600',
            validator: Validate.intRange.bind(null, 1, 1000, 'Number of Samples'),
            tooltip: 'Number of Samples',
            label: 'Number of Samples:'
        },
        zscaleSamplesPerLine: {
            fieldKey: 'zscaleSamplesPerLine',
            value: '120',
            validator: Validate.intRange.bind(null, 1, 500, 'Samples per line'),
            tooltip: 'Number of Samples per line',
            label: 'Samples per line:'
        },

        asinhQ: {
            fieldKey: 'asinhQ',
            validator: Validate.floatRange.bind(null, 0, Number.MAX_VALUE, 4, 'Q'),
            tooltip: 'The asinh softening parameter. Small Q values result in a linear stretch. Large values make a log stretch.',
            label: 'Q:'
        },
        gamma: {
            fieldKey: 'gamma',
            value: '2.0',
            validator: Validate.floatRange.bind(null, 0, 100, 4, 'Gamma'),
            tooltip: 'The Gamma value of the Power Law Gamma Stretch',
            label: 'Gamma:'
        },

        algorithm: {
            tooltip: 'Choose Stretch algorithm',
            label: 'Stretch Type: ',
            value: STRETCH_LINEAR
        },
        lowerWhich: {
            tooltip: 'Type of lower',
            label: '',
            value: PERCENTAGE
        },
        upperWhich: {
            tooltip: 'Type of upper',
            label: '',
            value: PERCENTAGE
        },
        zscale: {
            value: '',
            tooltip: 'Use ZScale for bounds instead of entering them manually ',
            label: ''
        }
    };
}


function getHuePreserveFieldInit() {

    return {
        lowerRangeRed: {
            value: '0',
            validator: Validate.floatRange.bind(null, 0, 100, 1, 'Red pedestal'),
            tooltip: 'Pedestal (black point value) for the red',
            label: 'Red pedestal:'
        },
        lowerRangeGreen: {
            value: '0',
            validator: Validate.floatRange.bind(null, 0, 100, 1, 'Green pedestal'),
            tooltip: 'Pedestal (black point value) for the green',
            label: 'Green pedestal:'
        },
        lowerRangeBlue: {
            value: '0',
            validator: Validate.floatRange.bind(null, 0, 100, 1, 'Blue pedestal'),
            tooltip: 'Pedestal (black point value) for the blue',
            label: 'Blue pedestal:'
        },
        lowerWhichRed: {
            tooltip: 'Type of pedestal value for the red',
            label: '',
            value: PERCENTAGE
        },
        lowerWhichGreen: {
            tooltip: 'Type of pedestal value for the green',
            label: '',
            value: PERCENTAGE
        },
        lowerWhichBlue: {
            tooltip: 'Type of pedestal value for the blue',
            label: '',
            value: PERCENTAGE
        },
        asinhQ: {
            validator: Validate.floatRange.bind(null, 0, Number.MAX_VALUE, 4, 'Q'),
            tooltip: 'The asinh softening parameter. Small Q values result in a linear stretch. Large values make a log stretch.',
            label: 'Q:'
        },
        stretch: {
            validator: Validate.floatRange.bind(null, 0.001, Number.MAX_VALUE, 4, 'Stretch'),
            tooltip: 'The asinh stretch parameter. (The difference between min and max intensity.)',
            label: 'Stretch:'
        },
        zscale: {
            value: '',
            tooltip: 'Use ZScale for bounds instead of entering them manually ',
            label: ''
        }
    };
}


/**
 * Reducer entry point. This function returns a reducer initialized to work with the passed band.
 * @param {Array.<Band>} bands
 * @return a reducer function. a standard type reducer function.
 */
export function rgbHuePreserveChange(bands) {

    return (fields,action) => {
        if (!fields || !Object.keys(fields).length) fields= getHuePreserveFieldInit();
        const plot= primePlot(visRoot());
        if (!plot || bands.some((b) => !plot.plotState.isBandUsed(b))) return fields;

        const fitsDataAry= bands.map((b)=>plot.webFitsData[b.value]);
        const plottedRVAry= bands.map((b)=>plot.plotState.getRangeValues(b));
        if (fitsDataAry.some((fd)=>!fd) || plottedRVAry.some((rv)=>(!rv))) return fields;

        return computeHuePreservePanelState(fields, plottedRVAry, fitsDataAry, bands, action);
    };
}

/**
 * Recompute the state. If there is a field value change then update the fields based on that.
 * If there is a replot or a init the update all the fields based on the current plot
 * @param fields
 * @param plottedRVAry
 * @param fitsDataAry
 * @param bands
 * @param action
 * @returns {*}
 */
export function computeHuePreservePanelState(fields, plottedRVAry, fitsDataAry, bands, action) {

    switch (action.type) {
        case FieldGroupCntlr.VALUE_CHANGE:
            const valid= Object.keys(fields).every( (key) => {
                return !fields[key].mounted ? true :  fields[key].valid;
            } );
            if (!valid) return fields;
            return syncFieldsHuePreserve(fields,makeHuePreserveRangeValuesFromFields(fields),fitsDataAry);

        case ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW:
        case INIT_FIELD_GROUP:
        case ImagePlotCntlr.ANY_REPLOT:
            if (plottedRVAry.some((rv)=>!rv) && fitsDataAry.some((fd)=>!fd)) return fields;
            const newFields = updateHuePreserveFieldsFromRangeValues(fields,plottedRVAry);
            return syncFieldsHuePreserve(newFields,plottedRVAry,fitsDataAry);
    }
    return fields;
}

/**
 * Sync the fields so that are consistent with the passed RangeValues.
 * @param fields
 * @param {Array} rvAry
 * @param {Array} fitsDataAry
 */
function syncFieldsHuePreserve(fields,rvAry,fitsDataAry) {
    const newFields= clone(fields);
    [['lowerWhichRed','lowerRangeRed'],
        ['lowerWhichGreen','lowerRangeGreen'],
        ['lowerWhichBlue', 'lowerRangeBlue']].forEach(
            ([lowerWhich,lowerRange],i) => {
        if (Number.parseInt(rvAry[i].lowerRange)!==ZSCALE) {
            newFields[lowerRange]= clone(fields[lowerRange], computeLowerRangeField(fields,rvAry[i],fitsDataAry[i],lowerWhich,lowerRange));
        }
        newFields[lowerWhich]=   clone(fields[lowerWhich], {value: rvAry[i].lowerWhich});
        if (newFields[lowerWhich].value===ZSCALE) newFields[lowerWhich].value= PERCENTAGE;
    });
    newFields.algorithm= clone(fields.algorithm, {value: rvAry[0].algorithm});
    return  newFields;
}

/**
 * Make a new RangeValue object from the field settings.
 * @param fields
 * @return {Array.<RangeValues>}
 */
function makeHuePreserveRangeValuesFromFields(fields) {
    const useZ = Boolean(fields.zscale.value);

    const rvAry = [['lowerWhichRed','lowerRangeRed'],
        ['lowerWhichGreen','lowerRangeGreen'],
        ['lowerWhichBlue', 'lowerRangeBlue']].map(
        ([lowerWhich,lowerRange]) => {
            // using lower range values to calculate black points for each band
            const lowerWhichVal= useZ ? ZSCALE : fields[lowerWhich].value;
            return new RangeValues(
                lowerWhichVal,
                fields[lowerRange].value,
                ZSCALE,
                99,
                fields.asinhQ.value,
                fields.stretch.value,
                STRETCH_ASINH);
        });
    return rvAry;
}

/**
 * Update all the fields from a RangeValues object.
 * @param fields
 * @param {Array.<RangeValues>} rvAry
 */
function updateHuePreserveFieldsFromRangeValues(fields,rvAry) {
    fields= clone(fields);
    [['lowerWhichRed','lowerRangeRed'],
        ['lowerWhichGreen','lowerRangeGreen'],
        ['lowerWhichBlue', 'lowerRangeBlue']].forEach(([lowerWhich, lowerRange], i) => {
        fields[lowerWhich]= cloneWithValue(fields[lowerWhich], rvAry[i].lowerWhich===ZSCALE ? PERCENTAGE : rvAry[i].lowerWhich);
        fields[lowerRange]= cloneWithValue(fields[lowerRange], rvAry[i].lowerValue);
    });
    fields.asinhQ= cloneWithValue(fields.asinhQ, rvAry[0].asinhQValue);
    fields.stretch= cloneWithValue(fields.stretch, Number(rvAry[0].gammaOrStretch).toFixed(1));
    fields.zscale= cloneWithValue(fields.zscale,  rvAry[0].lowerWhich===ZSCALE ? 'zscale' : '');
    return fields;
}


