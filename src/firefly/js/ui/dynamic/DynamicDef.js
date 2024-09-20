/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

export const CONE_AREA_KEY = 'CONE_AREA_KEY_RESERVED';
export const POSITION = 'position';
export const POINT = 'point';
export const ENUM = 'enum';
export const CHECKBOX = 'checkbox';
export const INT = 'int';
export const FLOAT = 'float';
export const UNKNOWN = 'unknown';
export const AREA = 'area';
export const POLYGON = 'polygon';
export const UPLOAD = 'upload';
export const RANGE = 'range';
export const CIRCLE = 'circle';

/**
 * @param {Object} obj
 * @param {String} obj.raKey
 * @param {String} obj.decKey
 * @param {String} obj.hipsUrl
 * @param {WorldPt} obj.centerPt
 * @param {number} obj.hipsFOVInDeg
 * @param {CoordinateSys} obj.coordinateSys
 * @param {Array.<{mocUrl:String, mocColor:String, title:String}>} obj.mocList
 * @param {boolean} obj.nullAllowed
 * @param {number} obj.minValue
 * @param {number} obj.maxValue
 * @param {String} obj.targetPanelExampleRow1
 * @param {String} obj.targetPanelExampleRow2
 * @param {String} sRegion
 * @return {FieldDef}
 */
export function makeTargetDef({
                                  hipsUrl, centerPt, hipsFOVInDeg, coordinateSys, mocList, nullAllowed,
                                  minValue, maxValue,
                                  targetPanelExampleRow1, targetPanelExampleRow2, raKey, decKey,
                                  sRegion,

                              }) {
    const defTargetKey= 'UserTargetWorldPt';
    return {
        type: POSITION,
        key: defTargetKey,
        nullAllowed,
        minValue,
        maxValue,
        targetDetails: {
            raKey, decKey, hipsUrl, centerPt, hipsFOVInDeg, coordinateSys, mocList,
            targetKey: defTargetKey, sRegion,
            targetPanelExampleRow1, targetPanelExampleRow2
        }
    };
}

export function makePointDef(obj) {
    return {...makeTargetDef(obj), type: POINT, key:obj.key};
}

/**
 * @param {Object} obj
 * @param {String} obj.key
 * @param {String}  obj.desc
 * @param {String} obj.tooltip
 * @param {String} obj.units
 * @param {String} obj.initValue
 * @param {Array.<{label:string,value:string}>} obj.enumValues
 * @return {FieldDef}
 */
export const makeEnumDef = ({key, desc, tooltip, units, initValue, enumValues}) =>
    ({type: ENUM, key, desc, tooltip, units, initValue, enumValues});

/**
 *
 * @param {Object} obj
 * @param {String} obj.key
 * @param {String} obj.desc
 * @param {String} obj.tooltip
 * @param {String} obj.units
 * @param {boolean} obj.initValue
 * @return {FieldDef}
 */
export const makeCheckboxDef = ({key, desc, tooltip, units, initValue}) =>
    ({type: CHECKBOX, key, desc, tooltip, units, initValue});

/**
 * @param {Object} obj
 * @param {String} obj.key
 * @param {String} obj.desc
 * @param {String} obj.tooltip
 * @param {String} obj.units
 * @param {number} obj.initValue
 * @param {number} obj.minValue
 * @param {number} obj.maxValue
 * @param {boolean} obj.nullAllowed
 * @return {FieldDef}
 */
export const makeIntDef = ({key, minValue, maxValue, desc, tooltip, units, initValue, nullAllowed}) =>
    ({type: INT, key, desc, tooltip, units, initValue, minValue, maxValue, nullAllowed});


/**
 * @param {Object} obj
 * @param {String} obj.key
 * @param {String} obj.desc
 * @param {String} obj.tooltip
 * @param {String} obj.units
 * @param {number} obj.initValue
 * @param {number} obj.minValue
 * @param {number} obj.maxValue
 * @param {number} obj.precision
 * @param {boolean} obj.nullAllowed
 * @return {FieldDef}
 */
export const makeFloatDef = ({key, minValue, maxValue, precision, desc, tooltip, units, initValue, nullAllowed}) =>
    ({type: FLOAT, key, desc, tooltip, units, initValue, minValue, maxValue, precision, nullAllowed});

/**
 * @param {Object} obj
 * @param {String} obj.key
 * @param {String} obj.desc
 * @param {String} obj.tooltip
 * @param {String} obj.units
 * @param {string} obj.initValue
 * @param {boolean} obj.nullAllowed
 * @param {boolean} obj.hide
 * @return {FieldDef}
 */
export const makeUnknownDef = ({key, desc, tooltip, units, initValue, nullAllowed, hide}) =>
    ({type: UNKNOWN, key, desc, tooltip, units, initValue, nullAllowed, hide});

/**
 * @param {Object} obj
 * @param {String} obj.key
 * @param {String} obj.desc
 * @param {String} obj.tooltip
 * @param {number} obj.initValue
 * @param {number} obj.minValue
 * @param {number} obj.maxValue
 * @param {boolean} [obj.nullAllowed]
 * @return {FieldDef}
 */
export const makeAreaDef = ({key, minValue, maxValue, desc, tooltip, initValue, nullAllowed=false}) =>
    ({type: AREA, key, desc, tooltip, initValue, minValue, maxValue, nullAllowed});

/**
 *
 * @param {Object} obj
 * @param {String} obj.key
 * @param {String} obj.targetKey
 * @param {String} [obj.sizeKey]
 * @param obj.minValue
 * @param obj.maxValue
 * @param {String} obj.desc
 * @param {String} obj.tooltip
 * @param obj.initValue
 * @param obj.nullAllowed
 * @param obj.centerPt
 * @param obj.hipsFOVInDeg
 * @param obj.coordinateSys
 * @param {String} [obj.sRegion]  an obsCore s_region type string
 * @param obj.hipsUrl
 * @param {Array.<{mocUrl:String, mocColor:String, title:String}>} obj.mocList
 * @param {String} [obj.targetPanelExampleRow1]
 * @param {String} [obj.targetPanelExampleRow2]
 * @returns {FieldDef}
 */
export function makeCircleDef({ key, targetKey, sizeKey, minValue, maxValue, desc, tooltip, initValue, nullAllowed,
                                  centerPt, hipsFOVInDeg, coordinateSys, sRegion,
                                  hipsUrl, mocList, targetPanelExampleRow1, targetPanelExampleRow2
                              }) {
    return ({
        type: CIRCLE, key, desc, tooltip, initValue, minValue, maxValue, nullAllowed,
        targetDetails: {
            hipsUrl, centerPt, hipsFOVInDeg, coordinateSys, mocList,
            targetKey, sizeKey, sRegion,
            targetPanelExampleRow1, targetPanelExampleRow2
        }
    });
}

export const makePolygonDef = ({key, desc, tooltip, initValue, targetPanelExampleRow1, targetPanelExampleRow2, sRegion,
                                   nullAllowed}) =>
    ({type: POLYGON, key, desc, tooltip, initValue, nullAllowed,
        targetDetails: {targetPanelExampleRow1,targetPanelExampleRow2,polygonKey:key+'-polygon', sRegion}
    });

export const makeRangeDef = ({key, desc, tooltip,
                                   nullAllowed}) =>
    ({type: RANGE, key, desc, tooltip, nullAllowed, targetDetails: {rangeKey:key+'-range'} });

/**
 * @typedef {Object} FieldDef
 *
 * @prop {String} key - not used with uiSelect
 * @prop {string} type one of float, int, position, area, enum, polygon, circle, checkbox, unknown,
 * @prop {number} minValue only use with int, float, radius
 * @prop {number} maxValue  only use with int,float, radius
 * @prop {string} desc - optional  for position, polygon, radius, not used with ra, dec
 * @prop {boolean} nullAllowed - true if can be null
 * @prop {string} units - will show up in description and tooltip
 * @prop {string} tooltip
 * @prop {number} precision - used with float
 * @prop {number|string} initValue - used with float, int, enum
 * @prop {string} areaType - only used with area - one of square, circleRadius, circleDiameter, none
 * @prop {TargetSearchDetails} targetDetails - only use with ra, dec, position, polygon
 * @prop {Array.<{label:string,value:string}|String>} enumValues only used with enum
 *
 */


/**
 * @typedef {Object} TargetSearchDetails
 *
 * @prop {String} hipsUrl
 * @prop {WorldPt} centerPt
 * @prop {number} hipsFOVInDeg
 * @prop {string} coordinateSys - one of 'GALACTIC' or 'EQ_J2000'
 * @prop {Array.<{mocUrl:String, mocColor:String, title:String}>} mocList
 * @prop {boolean} nullAllowed
 * @prop {String} raKey
 * @prop {String} decKey
 * @prop {String} targetKey
 * @prop {String} sizeKey
 * @prop {String} polygonKey
 * @prop {String} obj.sRegion  an obsCore s_region type string
 * @prop {Array.<String>} targetPanelExampleRow1 eg- [`'62, -37'`, `'60.4 -35.1'`, `'4h11m59s -32d51m59s equ j2000'`, `'239.2 -47.6 gal'`],
 * @prop {Array.<String>} targetPanelExampleRow2  eg= [`'NGC 1532' (NB: DC2 is a simulated sky, so names are not useful)`],
 */
