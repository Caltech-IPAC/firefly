/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*globals ffgwt*/

import {application} from '../core/Application.js';
import Validate from '../util/Validate.js';
import FieldGroupCntlr from '../fieldGroup/FieldGroupCntlr.js';
import ImagePlotCntlr from './ImagePlotCntlr.js';

import RangeValues from './RangeValues.js';
import {PERCENTAGE, MAXMIN, ABSOLUTE,SIGMA,ZSCALE} from './RangeValues.js';
import {STRETCH_LINEAR, STRETCH_LOG, STRETCH_LOGLOG, STRETCH_EQUAL} from './RangeValues.js';
import {STRETCH_SQUARED, STRETCH_SQRT, STRETCH_ASINH, STRETCH_POWERLAW_GAMMA} from './RangeValues.js';
import {getCurrentPlot} from './VisUtil.js';

var {AllPlots, Band } = window.ffgwt ? window.ffgwt.Visualize : {};


export const RED_PANEL= 'redPanel';
export const GREEN_PANEL= 'greenPanel';
export const BLUE_PANEL= 'bluePanel';
export const NO_BAND_PANEL= 'nobandPanel';


var getFieldInit= function() {
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
        contrast: {
            fieldKey: 'contrast',
            value: '25',
            validator: Validate.intRange.bind(null, 1, 100, 'Contrast (%)'),
            tooltip: 'Zscale algorithm contrast parameter',
            label: 'Contrast (%):'
        },
        numSamp: {
            fieldKey: 'numSamp',
            value: '600',
            validator: Validate.intRange.bind(null, 1, 1000, 'Number of Samples'),
            tooltip: 'Number of Samples',
            label: 'Number of Samples:'
        },
        sampPerLine: {
            fieldKey: 'sampPerLine',
            value: '120',
            validator: Validate.intRange.bind(null, 1, 500, 'Samples per line'),
            tooltip: 'Number of Samples per line',
            label: 'Samples per line:'
        },

        DR: {
            fieldKey: 'DR',
            value: '1.0',
            validator: Validate.floatRange.bind(null, 1, 1000000, 1, 'DR'),
            tooltip: 'The dynamic range scaling factor of data',
            label: 'Dynamic Range:'
        },
        gamma: {
            fieldKey: 'gamma',
            value: '2.0',
            validator: Validate.floatRange.bind(null, 0, 100, 4, 'Gamma'),
            tooltip: ' The Gamma value of the Power Law Gamma Stretch',
            label: 'Gamma:'
        },
        BP: {
            fieldKey: 'BP',
            value: '0',
            validator: Validate.floatRange.bind(null, 0, 1000000, 4, 'Black Point'),
            tooltip: 'black point for image display',
            label: 'Black Point:'
        },
        WP: {
            fieldKey: 'WP',
            value: '1.0',
            validator: Validate.floatRange.bind(null, 0, 1000000, 4, 'White Point'),
            tooltip: 'white point for image display',
            label: 'White Point:'
        },
        algorithm: {
            tooltip: 'Choose Stretch algorithm',
            label: 'Stretch Type: ',
            value: STRETCH_LINEAR
        },
        lowerType: {
            tooltip: 'Type of lower',
            label: '',
            value: PERCENTAGE
        },
        upperType: {
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
};

/**
 *
 * Recompute the state.
 * @param fields
 * @param plottedRV
 * @param fitsData
 * @param band
 * @param actionsConst
 * @return {*}
 */
export const computeColorPanelState= function(fields, plottedRV, fitsData, band, actionsConst) {
    if (!fields || !Object.keys(fields).length) fields= getFieldInit();
    var rv;

    if (actionsConst===FieldGroupCntlr.VALUE_CHANGE) {
        var valid= Object.keys(fields).every( (key) => {
            return !fields[key].mounted ? true :  fields[key].valid;
        } );
        if (!valid) return fields;
        rv= makeRangeValuesFromFields(fields);
    }
    else if (actionsConst===ImagePlotCntlr.ANY_CHANGE) {
        if (!plottedRV && !fitsData) return fields;
        rv= plottedRV;
        updateFieldsFromRangeValues(fields,rv);
    }
    else {
        return fields;
    }


    var newFields= Object.assign({},fields);

    if (Number.parseInt(rv.lowerWhich)===ZSCALE) {
        newFields.sampPerLine= Object.assign({},fields.sampPerLine);
        newFields.contrast= Object.assign({},fields.contrast);
        newFields.numSamp= Object.assign({},fields.numSamp);
        newFields.zscale= Object.assign({},fields.zscale);
    }
    else {
        var editLower= updateLower(fields,rv,fitsData);
        var editUpper= updateUpper(fields,rv,fitsData);
        newFields.lowerRange= Object.assign({},fields.lowerRange, editLower);
        newFields.upperRange= Object.assign({},fields.upperRange, editUpper);
    }



    newFields.algorithm= Object.assign({},fields.algorithm, {value: rv.algorithm});
    newFields.lowerType=   Object.assign({},fields.lowerType, {value: rv.lowerWhich});
    newFields.upperType=   Object.assign({},fields.upperType, {value: rv.upperWhich});

    return newFields;
};

/**
 * Collect any necessary data from store and the recompute the state
 * @return
 */
export const colorPanelChange= function(band) {
    return (fields,action) => {
        var plottedRV= null;
        var fitsData= null;
        var plot= getCurrentPlot();
        if (plot) {
            fitsData= plot.getFitsDataByBand(band);
            var rvs= plot.getRangeValuesSerialized(band);
            plottedRV= RangeValues.parse(rvs);
        }
        return computeColorPanelState(fields, plottedRV, fitsData, band, action.type);
    };
};





/**
 * @param rv RangeValues
 */
const updateZscale= function(rv) {

};

/**
 * @param rv {RangeValues}
 * @param fitsData WebFitsData
*/
const updateLower= function (fields,rv,fitsData) {
    var resetDefault= (fields.lowerType.value!==rv.lowerWhich);
    var retval;
    switch (rv.lowerWhich) {
        case PERCENTAGE:
        default :
            retval= {validator: Validate.floatRange.bind(null, 0, 100, 1, 'Lower range'),
                value : resetDefault ? 1 : fields.lowerRange.value
            };
            break;
        case MAXMIN:
            retval= {validator: Validate.floatRange.bind(null,fitsData.getDataMin(), fitsData.getDataMax(), 3, 'Lower range'),
                value : resetDefault ? fitsData.getDataMin() : fields.lowerRange.value
            };
            break;
        case ABSOLUTE:
            retval= {validator: Validate.floatRange.bind(null,fitsData.getDataMin(), fitsData.getDataMax(), 3, 'Lower range'),
                value : resetDefault ? fitsData.getDataMin() : fields.lowerRange.value
            };
            break;
        case SIGMA:
            retval= {validator: Validate.isFloat.bind(null,'Lower range'),
                value : resetDefault ? -2 : fields.lowerRange.value
            };
            break;
    }
    return retval;
};

const updateUpper= function(fields,rv,fitsData) {
    var retval;
    var resetDefault= (fields.upperType.value!==rv.upperWhich);
    switch (rv.upperWhich) {
        case PERCENTAGE:
        default :
            retval= {validator: Validate.floatRange.bind(null, 0, 100, 1, 'Upper range'),
                value : resetDefault ? 99 : fields.upperRange.value
            };
            break;
        case MAXMIN:
            retval= {validator: Validate.floatRange.bind(null,fitsData.getDataMin(), fitsData.getDataMax(), 3, 'Upper range'),
                value : resetDefault ? fitsData.getDataMax() : fields.upperRange.value
            };
            break;
        case ABSOLUTE:
            retval= {validator: Validate.floatRange.bind(null,fitsData.getDataMin(), fitsData.getDataMax(), 3, 'Upper range'),
                value : resetDefault ? fitsData.getDataMax() : fields.upperRange.value
            };
            break;
        case SIGMA:
            retval= {validator: Validate.isFloat.bind(null,'Upper range'),
                value : resetDefault ? 10 : fields.upperRange.value
            };
            break;
    }
    return retval;
};

const makeRangeValuesFromFields= function(fields) {
    var lowerType= (fields.zscale.value==='zscale') ? ZSCALE : fields.lowerType.value;
    var upperType= (fields.zscale.value==='zscale') ? ZSCALE : fields.upperType.value;
    var rv= new RangeValues(
            lowerType,
            fields.lowerRange.value,
            upperType,
            fields.upperRange.value,
            fields.DR.value,
            fields.BP.value,
            fields.WP.value,
            fields.gamma.value,
            fields.algorithm.value,
            fields.contrast.value,
            fields.numSamp.value,
            fields.sampPerLine.value);
    return rv;
};

/**
 *
 * @param fields
 * @param rv RangeValues
 */
const updateFieldsFromRangeValues= function(fields,rv) {
    fields.lowerType= cloneWithValue(fields.lowerType, rv.lowerWhich===ZSCALE ? PERCENTAGE : rv.lowerWhich);
    fields.lowerRange= cloneWithValue(fields.lowerRange, rv.lowerValue);
    fields.upperType= cloneWithValue(fields.upperType, rv.lowerWhich===ZSCALE ? PERCENTAGE : rv.lowerWhich);
    fields.upperRange= cloneWithValue(fields.upperRange, rv.upperValue);
    fields.DR= cloneWithValue(fields.DR, rv.drValue);
    fields.BP= cloneWithValue(fields.BP, rv.bpValue);
    fields.WP= cloneWithValue(fields.WP, rv.wpValue);
    fields.gamma= cloneWithValue(fields.gamma, rv.gammaValue);
    fields.algorithm= cloneWithValue(fields.algorithm, rv.algorithm);
    fields.contrast= cloneWithValue(fields.contrast, rv.zscaleContrast);
    fields.numSamp= cloneWithValue(fields.numSamp, rv.zscaleSamples);
    fields.sampPerLine= cloneWithValue(fields.sampPerLine, rv.zscaleSamplesPerLine);
    fields.zscale= cloneWithValue(fields.zscale,  rv.lowerWhich===ZSCALE ? 'zscale' : '');
};

const cloneWithValue= function(field,v) {
    return Object.assign({}, field, {value:v});
};



