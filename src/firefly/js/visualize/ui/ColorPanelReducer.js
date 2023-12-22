/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Validate from '../../util/Validate.js';
import FieldGroupCntlr, {FORCE_FIELD_GROUP_REDUCER, INIT_FIELD_GROUP} from '../../fieldGroup/FieldGroupCntlr.js';
import ImagePlotCntlr from '../ImagePlotCntlr.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import RangeValues, {STRETCH_LINEAR, STRETCH_ASINH, PERCENTAGE, ABSOLUTE, SIGMA, ZSCALE} from './../RangeValues.js';


const cloneWithValue= (field,v) => ({...field, value:v});


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
                return (!fields[key].mounted || key==='upperRange' || key==='lowerRange') ? true :  fields[key].valid;
            } );
            if (!valid) return fields;
            return syncFields(fields,makeRangeValuesFromFields(fields),fitsData);

        case ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW:
        case INIT_FIELD_GROUP:
        case FORCE_FIELD_GROUP_REDUCER:
        case ImagePlotCntlr.ANY_REPLOT:
            if (!plottedRV && !fitsData) return fields;
            // no update if hue-preserving
            if ((plottedRV?.rgbPreserveHue ?? 0) > 0) return fields;
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
    const newFields= {...fields};
    if (Number.parseInt(rv.lowerWhich)!==ZSCALE) {
        newFields.lowerRange= {...fields.lowerRange, ...computeLowerRangeField(fields,rv,fitsData)};
        newFields.upperRange= {...fields.upperRange, ...computeUpperRangeField(fields,rv,fitsData)};
    }

    newFields.algorithm= {...fields.algorithm, value: rv.algorithm};
    newFields.lowerWhich=   {...fields.lowerWhich, value: rv.lowerWhich};
    newFields.upperWhich=   {...fields.upperWhich, value: rv.upperWhich};

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
    const resetDefault= (Number(fields[lowerWhich].value)!==Number(rv.lowerWhich));
    let retval;
    switch (rv.lowerWhich) {
        case PERCENTAGE:
        default :
            retval= {validator: Validate.floatRange.bind(null, 0, 100, 1, 'Lower range'),
                value : resetDefault ? 1 : fields[lowerRange].value
            };
            break;
        case ABSOLUTE:
            retval= {
                validator:  fitsData.dataMin && fitsData.dataMax ?
                    Validate.floatRange.bind(null,fitsData.dataMin, fitsData.dataMax, 3, 'Lower range') :
                    () => ({valid:true, message:''}),
                value : resetDefault ? fitsData.dataMin : fields[lowerRange].value
            };
            break;
        case SIGMA:
            retval= {validator: Validate.isFloat.bind(null,'Lower range'),
                value : resetDefault ? -2 : fields[lowerRange].value
            };
            break;
    }
    retval= {...retval, ...retval.validator(retval.value)};
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
    const resetDefault= (Number(fields.upperWhich.value)!==Number(rv.upperWhich));
    switch (rv.upperWhich) {
        case PERCENTAGE:
        default :
            retval= {validator: Validate.floatRange.bind(null, 0, 100, 1, 'Upper range'),
                value : resetDefault ? 99 : fields.upperRange.value
            };
            break;

        case ABSOLUTE:
            retval= {
                validator: fitsData.dataMin && fitsData.dataMax ?
                    Validate.floatRange.bind(null,fitsData.dataMin, fitsData.dataMax, 3, 'Upper range') :
                    () => ({valid:true, message:''}),
            value : resetDefault ? fitsData.dataMax : fields.upperRange.value
            };
            break;
        case SIGMA:
            retval= {validator: Validate.isFloat.bind(null,'Upper range'),
                value : resetDefault ? 10 : fields.upperRange.value
            };
            break;
    }
    retval= {...retval, ...retval.validator(retval.value)};
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
    fields= {...fields};
    fields.lowerWhich= cloneWithValue(fields.lowerWhich, rv.lowerWhich===ZSCALE ? PERCENTAGE : rv.lowerWhich);
    fields.lowerRange= cloneWithValue(fields.lowerRange, rv.lowerValue);
    fields.upperWhich= cloneWithValue(fields.upperWhich, rv.lowerWhich===ZSCALE ? PERCENTAGE : rv.upperWhich);
    fields.upperRange= cloneWithValue(fields.upperRange, rv.upperValue);
    fields.asinhQ= cloneWithValue(fields.asinhQ, rv.asinhQValue);
    fields.gamma= cloneWithValue(fields.gamma, rv.gammaValue);
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
            label: 'Lower range'
        },
        upperRange: {
            fieldKey: 'upperRange',
            value: '99',
            validator: Validate.floatRange.bind(null, 0, 100, 1, 'Upper range'),
            tooltip: 'Upper range of the Stretch',
            label: 'Upper range'
        },
        zscaleContrast: {
            fieldKey: 'zscaleContrast',
            value: '25',
            validator: Validate.intRange.bind(null, 1, 100, 'Contrast (%)'),
            tooltip: 'Zscale algorithm contrast parameter',
            label: 'Contrast (%)'
        },
        zscaleSamples: {
            fieldKey: 'zscaleSamples',
            value: '600',
            validator: Validate.intRange.bind(null, 1, 1000, 'Number of Samples'),
            tooltip: 'Number of Samples',
            label: 'Number of Samples'
        },
        zscaleSamplesPerLine: {
            fieldKey: 'zscaleSamplesPerLine',
            value: '120',
            validator: Validate.intRange.bind(null, 1, 500, 'Samples per line'),
            tooltip: 'Number of Samples per line',
            label: 'Samples per line'
        },

        asinhQ: {
            fieldKey: 'asinhQ',
            validator: Validate.floatRange.bind(null, 0, Number.MAX_VALUE, 4, 'Q'),
            tooltip: 'The asinh softening parameter. Small Q values result in a linear stretch. Large values make a log stretch.',
            label: 'Q'
        },
        gamma: {
            fieldKey: 'gamma',
            value: '2.0',
            validator: Validate.floatRange.bind(null, 0, 100, 4, 'Gamma'),
            tooltip: 'The Gamma value of the Power Law Gamma Stretch',
            label: 'Gamma'
        },

        algorithm: {
            tooltip: 'Choose Stretch algorithm',
            label: 'Stretch Type:',
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
            disabled: true,
            style: {background: '#F0F0F0'},
            validator: Validate.floatRange.bind(null, 0, Number.MAX_VALUE, 1, 'Stretch'),
            tooltip: 'The asinh stretch parameter. (The difference between min and max intensity.)',
            label: 'Stretch:'
        },
        kRed: {
            value: '1',
            validator: Validate.floatRange.bind(null, -1, 1, 0, 'Red scaling coefficient'),
            tooltip: 'Scaling coefficient for the red flux',
            label: 'Red:'
        },
        kGreen: {
            value: '1',
            validator: Validate.floatRange.bind(null, -1, 1, 0, 'Green scaling coefficient'),
            tooltip: 'Scaling coefficient for the green flux',
            label: 'Green:'
        },
        kBlue: {
            value: '1',
            validator: Validate.floatRange.bind(null, -1, 1, 0, 'Blue scaling coefficient'),
            tooltip: 'Scaling coefficient for the blue flux',
            label: 'Blue:'
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

        case FORCE_FIELD_GROUP_REDUCER:
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
    const newFields= {...fields};
    [['lowerWhichRed','lowerRangeRed', 'kRed'],
        ['lowerWhichGreen','lowerRangeGreen', 'kGreen'],
        ['lowerWhichBlue', 'lowerRangeBlue', 'kBlue']].forEach(
            ([lowerWhich,lowerRange,scalingK],i) => {
        if (Number.parseInt(rvAry[i].lowerRange)!==ZSCALE) {
            newFields[lowerRange]= {...fields[lowerRange], ...computeLowerRangeField(fields,rvAry[i],fitsDataAry[i],lowerWhich,lowerRange)};
        }
        newFields[lowerWhich]=   {...fields[lowerWhich],value: rvAry[i].lowerWhich};
        if (newFields[lowerWhich].value===ZSCALE) newFields[lowerWhich].value= PERCENTAGE;
        //scalingK is between 0.1 and 10, while range slider values are from -1 to 1
        newFields[scalingK].value = scalingKToFieldVal(rvAry[i].scalingK);
    });
    return  newFields;
}

/**
 * Make a new RangeValue object from the field settings.
 * @param fields
 * @return {Array.<RangeValues>}
 */
function makeHuePreserveRangeValuesFromFields(fields) {
    const useZ = Boolean(fields.zscale.value);

    const rvAry = [['lowerWhichRed','lowerRangeRed','kRed'],
        ['lowerWhichGreen','lowerRangeGreen','kGreen'],
        ['lowerWhichBlue', 'lowerRangeBlue','kBlue']].map(
        ([lowerWhich,lowerRange,scalingK]) => {
            // using lower range values to calculate black points for each band
            const lowerWhichVal= useZ ? ZSCALE : fields[lowerWhich].value;
            //scalingK is between 0.1 and 10, while range slider values are from -1 to 1
            const scalingKVal = scalingKFromFieldVal(fields[scalingK].value);
            return RangeValues.makeRV({
                lowerWhich: lowerWhichVal,
                lowerValue: fields[lowerRange].value,
                upperWhich: ZSCALE,
                upperValue: 99,
                asinhQValue: fields.asinhQ.value,
                scalingK: scalingKVal,
                STRETCH_ASINH});
        });
    return rvAry;
}

/**
 * Update all the fields from a RangeValues object.
 * @param fields
 * @param {Array.<RangeValues>} rvAry
 */
function updateHuePreserveFieldsFromRangeValues(fields,rvAry) {
    fields= {...fields};
    [['lowerWhichRed','lowerRangeRed','kRed'],
        ['lowerWhichGreen','lowerRangeGreen','kGreen'],
        ['lowerWhichBlue', 'lowerRangeBlue','kBlue']].forEach(([lowerWhich, lowerRange, scalingK], i) => {
        fields[lowerWhich]= cloneWithValue(fields[lowerWhich], rvAry[i].lowerWhich===ZSCALE ? PERCENTAGE : rvAry[i].lowerWhich);
        fields[lowerRange]= cloneWithValue(fields[lowerRange], rvAry[i].lowerValue);
        fields[scalingK]= cloneWithValue(fields[scalingK], scalingKToFieldVal(rvAry[i].scalingK));
    });
    fields.asinhQ= cloneWithValue(fields.asinhQ, rvAry[0].asinhQValue);
    let asinhStretchVal = Number(rvAry[0].asinhStretch);
    asinhStretchVal = Number.isFinite(asinhStretchVal) ? asinhStretchVal.toFixed(1) : undefined;
    fields.stretch= cloneWithValue(fields.stretch, asinhStretchVal);
    fields.zscale= cloneWithValue(fields.zscale,  rvAry[0].lowerWhich===ZSCALE ? 'zscale' : '');
    return fields;
}

/**
 *  scalingK is between 0.1 and 10, while range slider values are from -1 to 1
 *  @param {string|number} fieldVal - field value (exponent) from -1 to 1
 *  @return {number}
 */
function scalingKFromFieldVal(fieldVal) {
    return Math.pow(10, parseFloat(fieldVal));
}

/**
 *  scalingK is between 0.1 and 10, while range slider values are from -1 to 1
 *  @param {number} scalingK - from range values from 0.1 to 1
 *  @return {number} - field value (exponent) from -1 to 1
 */
function scalingKToFieldVal(scalingK) {
    return !scalingK ? 1: Math.log10(scalingK);
}