/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {drawRegions} from './RegionDrawer.js';
import {getRegionDefault} from './Region.js';
import {has, get, isNil} from 'lodash';

/**
 * get region index from Region object array
 * @param regions
 * @param aRegionObj
 * @returns {*}
 */
export function getRegionIndex(regions, aRegionObj) {
    return aRegionObj ? regions.findIndex( (oneRegion) => isEqualRegion(oneRegion, aRegionObj) ) : -1;
}

/**
 * create region DrawObj if not exist in Region array per Region object
 * @param crtRegions
 * @param aRegionObj
 * @returns {null}
 */
export function addNewRegion( crtRegions, aRegionObj) {
    if (!aRegionObj) return null;

    var index = getRegionIndex( crtRegions, aRegionObj);

    if (index >= 0) return null; // alredy exist
    var newObj = drawRegions([aRegionObj]);

    return newObj ? newObj[0] : null;
}

/**
 * remove Region object from Region array if existing
 * @param crtRegions
 * @param aRegionObj
 * @returns {{index: *, regions: *}}
 */
export function removeRegion( crtRegions, aRegionObj) {
    var index = getRegionIndex ( crtRegions, aRegionObj );
    if (index >= 0) {
        crtRegions.splice(index, 1);
        return {index, regions: crtRegions};
    } else {
        return {index, regions: crtRegions};
    }
}

/**\
 * check if two Region object are the same
 * @param region1
 * @param region2
 * @returns {*}
 */
export function  isEqualRegion(region1, region2) {
    return (isSameType(region1, region2) &&
            isSamePosition(region1, region2) &&
            isSameSize(region1, region2) &&
            isSameAngle(region1, region2) &&
            isSameOptions(region1, region2));
}

/**
 * check how many Region in an array are without property, 'key'
 * @param key
 * @param rgAry
 * @returns {number}
 */
function countRegionWithout(key,rgAry) {

    return rgAry.reduce( (prev, rg) => {
        if (!has(rg, key)) prev++;
        return prev;
    }, 0);
}

/**
 * check if two Region are with the same type
 * @param region1
 * @param region2
 * @returns {boolean}
 */
function isSameType(region1, region2) {
    return countRegionWithout('type', [region1, region2]) >= 1 ? false : region1.type === region2.type;
}


/**
 * check if two Regions are at the same position
 * @param region1
 * @param region2
 * @returns {boolean}
 */
function isSamePosition(region1, region2) {
    if (countRegionWithout('wpAry', [region1, region2])  >= 1 || region1.wpAry.length !== region2.wpAry.length) {
        return false;
    }

    var p1Set = new Set();
    var p2Set = new Set();

    region1.wpAry.forEach((r1Pos, index) => {
        var r2Index = region2.wpAry.findIndex((r2Pos, index) => (!p2Set.has(index) && r2Pos.type === r1Pos.type && r2Pos.x === r1Pos.x && r2Pos.y === r1Pos.y));

        if (!isNil(r2Index) && r2Index !== -1 ) {
            p2Set.add(r2Index);
        }

        p1Set.add(index);
    });

    return (p1Set.size === p2Set.size);
}

/**
 * check if two Regions are with the same size or dimension
 * @param region1
 * @param region2
 * @returns {boolean}
 */
function isSameSize(region1, region2) {
    var p1Set = new Set();
    var p2Set = new Set();
    var r2Index;

    if (countRegionWithout('radiusAry', [region1, region2]) === 0) {
        region1.radiusAry.forEach((radius1, index) => {
            r2Index = region2.radiusAry.findIndex((radius2, index) => (!p2Set.has(index) && isSameRegionValue(radius1, radius2)));

            if (!isNil(r2Index) && r2Index !== -1) {
                p2Set.add(r2Index);
            }
            p1Set.add(index);
        });
    } else if (countRegionWithout('dimensionAry', [region1, region2]) === 0) {
        region1.dimensionAry.forEach((dim1, index) => {
            r2Index = region2.dimensionAry.findIndex((dim2, index) => (!p2Set.has(index) && isSameRegionDimension(dim1, dim2)));

            if (!isNil(r2Index) && r2Index !== -1) {
                p2Set.add(r2Index);
            }
            p1Set.add(index);
        });
    }

    return (p1Set.size === p2Set.size);
}

/**
 * check if two RegionValue are with the same value and unit
 * @param v1
 * @param v2
 * @returns {boolean}
 */
function isSameRegionValue(v1, v2) {
    return (v1.unit === v2.unit && v1.value === v2.value);
}

/**
 * check if two RegionDimenstion are the same
 * @param d1
 * @param d2
 * @returns {boolean}
 */
function isSameRegionDimension(d1, d2) {
    return ( isSameRegionValue(d1.width, d2.width) && isSameRegionValue(d1.height, d2.height));
}


/**
 * check if two Regions are with the same angle
 * @param region1
 * @param region2
 * @returns {boolean}
 */
function isSameAngle(region1, region2) {
    var count = countRegionWithout('angle', [region1, region2]);

    if (count === 2) return true;    // both regions have no angle
    if (count === 1) return false;

    return isSameRegionValue(region1.angle, region2.angle);
}

var getOptionValue = (options, optionProp) => get(options, optionProp, getRegionDefault(optionProp));

/**
 * union option property from two Regions
 * @param optionSet1
 * @param optionSet2
 * @returns {Array}
 */
function mergeOptionProps(optionSet1, optionSet2) {
    var propSet = new Set([...optionSet1,...optionSet2]);
    var propArray = [],
        it = propSet.values(),
        ele = it.next();

    while(!ele.done) {
        propArray.push(ele.value);
        ele = it.next();
    }
    return propArray;
}

/**
 * check two Regions are with the same options
 * @param region1
 * @param region2
 * @returns {boolean}
 */
function isSameOptions(region1, region2) {
    var keys = mergeOptionProps(Object.keys(region1.options), Object.keys(region2.options));
    var notSameProp =  keys.findIndex( (prop) => {
        var v1 = getOptionValue(region1.options, prop);
        var v2 = getOptionValue(region2.options, prop);

        return (v1 !== v2);
    });

    //var notSameProp =  keys.findIndex( (prop) => ( getOptionValue(region1.options, prop) !== getOptionValue(region2.options, prop) ));

    return (notSameProp < 0);
}