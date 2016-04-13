/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import Validate from '../../util/Validate.js';
import {panelCatalogs} from './ImageSelectPanelProp.js';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';
import {keyMap, computeLabelWidth, rgbFieldGroup, isTargetNeeded,
        IRSA, TWOMASS, WISE, MSX, DSS, SDSS, FITS, URL, BLANK, NONE} from './ImageSelectPanel.jsx';
import {sizeFromDeg} from '../../ui/sizeInputFields.jsx';
import {get} from 'lodash';

// get unit (string), min (float) and max (float) from the data file
var getRangeItem = (crtCatalogId, rangeItem) => {
    if (crtCatalogId === NONE ||
        !panelCatalogs[crtCatalogId].hasOwnProperty('range')) {
        if (rangeItem === 'unit') {
            return 'deg';
        } else {
            return 0.0;
        }
    } else {
        return panelCatalogs[crtCatalogId]['range'][rangeItem];
    }
};

// get default size (string) from the data file
var getSize = (crtCatalogId) => {
    if (crtCatalogId !== NONE && panelCatalogs[crtCatalogId].hasOwnProperty('size')) {
        return panelCatalogs[crtCatalogId]['size'].toString();
    } else {
        return '';
    }
};

// tab fields initialization
function initTabFields(crtCatalogId) {
    return (
    {
        [keyMap['catalogtab']]: {
            fieldKey: keyMap['catalogtab'],
            value: panelCatalogs[crtCatalogId].CatalogId.toString()
        },
        [keyMap['irsatypes']]: {
            fieldKey: keyMap['irsatypes'],
            label: panelCatalogs[IRSA].types.Title,
            value: panelCatalogs[IRSA].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[IRSA].types.Title)
        },
        [keyMap['twomasstypes']]: {
            fieldKey: keyMap['twomasstypes'],
            label: panelCatalogs[TWOMASS].types.Title,
            value: panelCatalogs[TWOMASS].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[TWOMASS].types.Title)
        },
        [keyMap['wisetypes']]: {
            fieldKey: keyMap['wisetypes'],
            label: panelCatalogs[WISE].types.Title,
            value: panelCatalogs[WISE].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[WISE].types.Title)
        },
        [keyMap['wisebands']]: {
            fieldKey: keyMap['wisebands'],
            label: panelCatalogs[WISE].bands.Title,
            value: panelCatalogs[WISE].bands.Default,
            labelWidth: computeLabelWidth(panelCatalogs[WISE].bands.Title)
        },
        [keyMap['msxtypes']]: {
            fieldKey: keyMap['msxtypes'],
            label: panelCatalogs[MSX].types.Title,
            value: panelCatalogs[MSX].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[MSX].types.Title)
        },
        [keyMap['dsstypes']]: {
            fieldKey: keyMap['dsstypes'],
            label: panelCatalogs[DSS].types.Title,
            value: panelCatalogs[DSS].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[DSS].types.Title)
        },
        [keyMap['sdsstypes']]: {
            fieldKey: keyMap['sdsstypes'],
            label: panelCatalogs[SDSS].types.Title,
            value: panelCatalogs[SDSS].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[SDSS].types.Title)
        },
        [keyMap['fitslist']]: {
            fieldKey: keyMap['fitslist'],
            label: panelCatalogs[FITS].list.Title,
            value: panelCatalogs[FITS].list.Default,
            labelWidth: computeLabelWidth(panelCatalogs[FITS].list.Title)
        },
        [keyMap['fitsextinput']]: {
            fieldKey: keyMap['fitsextinput'],
            label: panelCatalogs[FITS].extinput.Title,
            value: '0',
            labelWidth: computeLabelWidth(panelCatalogs[FITS].extinput.Title)
        },
        [keyMap['blankinput']]: {
            fieldKey: keyMap['blankinput'],
            label: panelCatalogs[BLANK].input.Title,
            value: panelCatalogs[BLANK].input.Default.toString(),
            validator: Validate.floatRange.bind(null,
                panelCatalogs[BLANK].input.range.min,
                panelCatalogs[BLANK].input.range.max, 1.0, 'a float field'),
            labelWidth: computeLabelWidth(panelCatalogs[BLANK].input.Title)
        },
        [keyMap['urlinput']]: {
            fieldKey: keyMap['urlinput'],
            label: panelCatalogs[URL].input.Title,
            validator: Validate.validateUrl.bind(null, 'a url field'),
            value: '',
            labelWidth: computeLabelWidth(panelCatalogs[URL].input.Title)
        },
        [keyMap['urllist']]: {
            fieldKey: keyMap['urllist'],
            label: panelCatalogs[URL].list.Title,
            value: panelCatalogs[URL].list.Default,
            labelWidth: computeLabelWidth(panelCatalogs[URL].list.Title)

        },
        [keyMap['urlextinput']]: {
            fieldKey: keyMap['urlextinput'],
            label: panelCatalogs[URL].extinput.Title,
            value: '0',
            labelWidth: computeLabelWidth(panelCatalogs[URL].extinput.Title)
        }
    });
}

// size range adjustment: find the overlapping range and pick the range at the lower end if no overlapping
function adjustSizeRange(min1, max1, min2, max2) {
    var min = (max2 <= min1 || min2 >= max1) ? Math.min(min1, min2) : Math.max(min1, min2);
    var max = Math.min(max1, max2);

    return {min, max};
}


// target and size field initialization
var initTargetSize = (crtCatalogId) => {
    const size = 'Size:';
    var sizeR = {min: 0.0, max: 360.0};
    var dv = '';
    var unit = '';

    // for rgb, crtCatalogId is an array, for regular, it is a number
    if (typeof crtCatalogId === 'object') {
        crtCatalogId.forEach((id) => {
            if (isTargetNeeded(id, true)) {
                sizeR = adjustSizeRange(sizeR.min, sizeR.max, getRangeItem(id, 'min'), getRangeItem(id, 'max'));

                dv = getSize(id);
                if (!Validate.floatRange(sizeR.min, sizeR.max, 1.0, 'a float field', dv).valid) {
                    dv = sizeR.max.toString();
                }
            }
        });
        unit = getRangeItem(crtCatalogId[0], 'unit');
    } else {
        sizeR.min = getRangeItem(crtCatalogId, 'min');
        sizeR.max = getRangeItem(crtCatalogId, 'max');
        dv = getSize(crtCatalogId).toString();
        unit = getRangeItem(crtCatalogId, 'unit');
    }

    return (
    {
        [keyMap['targettry']]: {
            fieldKey: keyMap['targettry'],
            value: 'NED',
            label: ''
        },
        [keyMap['sizefield']]: {
            fieldKey: keyMap['sizefield'],
            label: size,
            labelWidth: computeLabelWidth(size),
            unit,
            min: sizeR.min,
            max: sizeR.max,
            value: dv
        }
    });
};

// reducer for the child field group (fieldgrouptabs for r, g, b)
export var ImageSelPanelChangeOneColor = (inFields, action) => {
    if (!inFields) {
        return initTabFields(IRSA);
    } else {
        return inFields;
    }
};

/*
 *
 * image select pane initial state for all fields
 */
export var ImageSelPanelChange = function (crtCatalogId, isThreeColor) {  // number or array

    var selectedCatalogSizeInfo = ( cId, originalSize, childGroups = false ) => {
        var {value, unit} = originalSize;
        var [min, max] = [getRangeItem(cId, 'min'), getRangeItem(cId, 'max')];

        var isnull = (item) => (typeof(item) === 'undefined' || item === null);

        // update the min, max value for 3 color case
        if (childGroups) {
            var sizeR = {min, max};

            rgbFieldGroup.forEach( (item) => {
                var childstrId = get(childGroups, [ item, keyMap['catalogtab'], 'value']); // could be 0 or '0'
                var childFieldId = !isnull(childstrId) ? parseInt(childstrId) : null;


                if (!isnull(childFieldId) && childFieldId !== cId && isTargetNeeded(childFieldId, true) ) {
                    sizeR = adjustSizeRange(getRangeItem(childFieldId, 'min'),
                                            getRangeItem(childFieldId, 'max'),
                                            sizeR.min, sizeR.max);
                }
            });

            min = sizeR.min;
            max = sizeR.max;
        }

        // update value if current state is no value or the value is out of range while changing tab
        // otherwise keep the value and unit as being left off from previous tab
        if (!value || !(Validate.floatRange(min, max, 1, 'value of radius size in degree', value).valid)) {
            value = getSize(cId);

            // adjust value for 3 color case
            if (childGroups) {
                if (!(Validate.floatRange(min, max, 1, 'value of radius size in degree', value).valid)) {
                    value = max.toString();
                }
            }
        }
        return  Object.assign({}, originalSize,
                                    { min, max, value,
                                      'displayValue': sizeFromDeg(value, unit),
                                      'valid': true});
    };


    return (inFields, action) => {
        if (!inFields) {
            if (isThreeColor) {
                return Object.assign({}, initTargetSize(crtCatalogId), initTabFields(crtCatalogId[0]));
            } else {
                return Object.assign({}, initTargetSize(crtCatalogId[0]), initTabFields(crtCatalogId[0]));
            }
        } else {

            const tabkey = keyMap['catalogtab'];
            var sizeField;
            var catalogId;
            var originalSize = inFields[keyMap['sizefield']];

            switch(action.type) {
                // update the size field in case tab selection is changed
                case FieldGroupCntlr.VALUE_CHANGE:
                    if (action.payload.fieldKey === tabkey) {
                        catalogId = parseInt(inFields[tabkey].value);

                        if ( isTargetNeeded ( catalogId )) {

                            sizeField = selectedCatalogSizeInfo(catalogId, originalSize);
                            return Object.assign({}, inFields, {[keyMap['sizefield']]: sizeField});
                        }
                    }
                    break;

                // update size field in case any of the child field group has tab switched
                case FieldGroupCntlr.CHILD_GROUP_CHANGE:
                    var changedFields = action.payload.childGroups[action.payload.changedGroupKey];

                    catalogId = parseInt(changedFields[tabkey].value);
                    if ( isTargetNeeded( catalogId, true )) {
                        sizeField = selectedCatalogSizeInfo(catalogId, originalSize, action.payload.childGroups);
                        return Object.assign({}, inFields, {[keyMap['sizefield']]: sizeField});
                    }
                    break;
                default:
                    break;
            }
            return inFields;
        }
    };
};


