/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*globals ffgwt*/

import {application} from '../../core/Application.js';
import Validate from '../../util/Validate.js';
import InputFormBaseStore from '../';
import ImagePlotsStore from '../';
import ImagePlotsActions from '../../actions/ImagePlotsActions.js'
import RangeValues from '../../visualize/RangeValues.js'
import {PERCENTAGE, MAXMIN, ABSOLUTE,SIGMA,ZSCALE} from '../../visualize/RangeValues.js'
import {STRETCH_LINEAR, STRETCH_LOG, STRETCH_LOGLOG, STRETCH_EQUAL} from '../../visualize/RangeValues.js'
import {STRETCH_SQUARED, STRETCH_SQRT, STRETCH_ASINH, STRETCH_POWERLAW_GAMMA} from '../../visualize/RangeValues.js'
import {getCurrentPlot} from '../../visualize/VisUtil.js';

var {AllPlots, Band } = ffgwt.Visualize;


class ColorPanelStore extends InputFormBaseStore {
    constructor(band) {
        super();
        this.band= band;
        this.fields= {
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
            stretchType: {
                tooltip: 'Choose Stretch algorithm',
                label: 'Stretch Type: ',
                value: STRETCH_LINEAR
            },
            lowerType : {
                tooltip: 'Type of lower',
                label : ''
            },
            upperType : {
                tooltip: 'Type of upper',
                label : ''
            },
            zscale : {
                value: '',
                tooltip: 'Use ZScale for bounds instead of entering them manually ',
                label : ''
            },
        };
        this.formKey = 'BandPanel-'+band.toString();

        this.bindListeners({
            handleImageAnyChange: ImagePlotsActions.anyChange,
        });
    }

    handleImageAnyChange(payload) {
        "use strict";
        var plot= getCurrentPlot();
        if (!plot) return;

        var fitsData= plot.getFitsDataByBand(this.band)
        var rvs= plot.getRangeValuesSerialized(this.band);
        var rv= RangeValues.parse(rvs);

        if (!rv) return;

        var fields= this.fields;
        var newFields= Object.assign({},fields);

        if (rv.lowerWhich===ZSCALE) {
           // todo: update zscale fields from range values
        }
        else {
            var editLower= this.updateLower(fields,rv,fitsData);
            var editUpper= this.updateUpper(fields,rv,fitsData);
            newFields.lowerRange= Object.assign({},fields.lowerRange, editLower);
            newFields.upperRange= Object.assign({},fields.upperRange, editUpper);
        }



        newFields.stretchType= Object.assign({},fields.lowerType, {value: rv.algorithm});
        newFields.lowerType=   Object.assign({},fields.lowerType, {value: rv.lowerWhich});
        newFields.upperType=   Object.assign({},fields.upperType, {value: rv.upperWhich});

        this.fields= newFields;
    }

    /**
     * @param rv RangeValues
     */
    updateZscale(rv) {

    }

    /**
     * @param rv {RangeValues}
     * @param fitsData WebFitsData
     */
    updateLower(fields,rv,fitsData) {
        var resetDefault= (fields.lowerRange.lowerType!==rv.lowerWhich);
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
    }

    updateUpper(fields,rv,fitsData) {
        var retval;
        var resetDefault= (fields.lowerRange.upperType!==rv.upperWhich);
        switch (rv.upperWhich) {
            case PERCENTAGE:
            default :
                retval= {validator: Validate.floatRange.bind(null, 0, 100, 1, 'Upper range'),
                         value : resetDefault ? 99 : fields.lowerRange.value
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
    }

}

//var colorDialogStore= application.alt.createStore(ColorDialogStore, 'ColorDialogStore' );

export var noBandStore= application.alt.createStore(ColorPanelStore , 'ColorDialogStoreNoBand',Band.NO_BAND );
export var redBandStore= application.alt.createStore(ColorPanelStore , 'ColorDialogStoreRED',Band.RED );
export var greenBandStore= application.alt.createStore(ColorPanelStore , 'ColorDialogStoreGREEN',Band.GREEN );
export var blueBandStore= application.alt.createStore(ColorPanelStore , 'ColorDialogStoreBLUE',Band.BLUE );
