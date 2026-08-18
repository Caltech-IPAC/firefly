import {isArray, isNumber, isString} from 'lodash';
import {ReservedParams} from '../../api/WebApi.js';
import {sprintf} from '../../externalSource/sprintf.js';
import {splitByWhiteSpace} from '../../util/WebUtil.js';
import {makeWorldPt, parseWorldPt} from '../../visualize/Point.js';
import {isSIAStandardID} from '../../voAnalyzer/VoCoreUtils';
import {
    AREA, CHECKBOX, CIRCLE, ENUM, FLOAT, INT, makeAreaDef, makeCircleDef, makeEnumDef, makeFloatDef, makeIntDef,
    makeObsCoreOps, makePointDef, makePolygonDef, makeRangeDef, makeTargetDef, makeUnknownDef, makeWavelengthDef, POINT,
    POLYGON, POSITION, RANGE, UNKNOWN
} from './DynamicDef.js';

/**
 * @param {Array.<FieldDef>} fieldDefAry
 * @param {string} type
 * @return {Boolean}
 */
export const hasType = (fieldDefAry, type) => Boolean(fieldDefAry.find((e) => e.type === type));

/**
 * @param {Array.<FieldDef>} fieldDefAry
 * @param {string} type
 * @return {FieldDef}
 */
export const findFieldDefType = (fieldDefAry, type) => fieldDefAry.find((e) => e.type === type);

export const hasAnySpacial= (fieldDefAry) =>
    hasType(fieldDefAry,POLYGON) || hasType(fieldDefAry,CIRCLE) ||
    hasType(fieldDefAry,POSITION) || hasType(fieldDefAry,POINT) ||
    hasType(fieldDefAry,AREA);


const isCircleField = ({type, arraySize, xtype = '', units = ''}) =>
    isFloating(type) && Number(arraySize) === 3 && isXtype(xtype,CIRCLE) && isDeg(units);

const isCircleFieldLenient = ({xtype, units}) => isXtype(xtype,CIRCLE) && isDeg(units);
const isPointField = ({xtype, units}) => isXtype(xtype,POINT) && isDeg(units);
const isPolygonField = ({type, xtype, units}) => isFloating(type) && isXtype(xtype,POLYGON) && isDeg(units);
const isPolygonFieldLenient = ({xtype, units}) => (isXtype(xtype,POLYGON) && isDeg(units));
const isRangeField = ({xtype, units}) => isXtype(xtype,RANGE) && isDeg(units);
const isAreaField = ({UCD, units}) => UCD?.toLowerCase().startsWith('phys.size') && isDeg(units);
const isDeg= (units='') => units.toLowerCase() === 'deg' || units === '';
const isXtype= (xtype='',type) => xtype.toLowerCase() === type;
const isFloating = (type='') => (type.toLowerCase() === 'float' || type.toLowerCase() === 'double');

function getCircleValues(s) {
    const strAry = splitByWhiteSpace(s);
    if (strAry.length === 1 && !isNaN(Number(strAry[0]))) return [Number(strAry[0])];
    if (strAry.length !== 3 || strAry.find((s) => isNaN(Number(s)))) return [];
    return strAry.map((s) => Number(s));
}

const getCircleInfo = ({minValue = '', maxValue = '', value = ''}) => {
    const matchStr = [value, minValue, maxValue].find((s) => getCircleValues(s).length === 3 || getCircleValues(s).length === 1);
    if (!matchStr) return {};
    const valueAry = getCircleValues(matchStr);
    if (isNumber(value) || (isArray(value) && value.length===1)) {
        const v= isArray(value) ? Number(value[0]) : Number(value);
        const minAry = getCircleValues(minValue);
        const maxAry = getCircleValues(maxValue);
        const minNum = minAry.length === 1 ? minAry[0] : .000277778;
        const maxNum = maxAry.length === 1 ? maxAry[0] : undefined;
        return {radius: v, minValue: minNum, maxValue: maxNum};
    }
    if (valueAry.length === 1) {
        const minAry = getCircleValues(minValue);
        const maxAry = getCircleValues(maxValue);
        const minNum = minAry.length === 1 ? minAry[0] : .000277778;
        const maxNum = maxAry.length === 1 ? maxAry[0] : undefined;
        return {radius: valueAry[0], minValue: minNum, maxValue: maxNum};
    }
    if (valueAry.length === 3) {
        const minNum = getCircleValues(minValue)[2] ?? .000277778;
        const maxNum = getCircleValues(maxValue)[2];
        return {wpt: makeWorldPt(valueAry[0], valueAry[1]), radius: valueAry[2], minValue: minNum, maxValue: maxNum};
    }
    return {};
};

const getPointInfo = ({value = ''}) => {
    const [raStr='', decStr='']= splitByWhiteSpace(value);
    return (raStr && decStr) ? makeWorldPt(raStr,decStr) : undefined;
};

const getPolygonInfo = ({minValue = '', maxValue = '', value = ''}) => {
    const vStr = value || minValue || maxValue;
    const validAryStr = splitByWhiteSpace(vStr).filter((s) => !isNaN(Number(s))).map((s) => sprintf('%.5f', Number(s)));
    if (validAryStr.length % 2 !== 0) return {value: ''};
    return {value: validAryStr.reduce((s, num, idx) => idx !== 0 && idx % 2 === 0 ? `${s}, ${num}` : `${s} ${num}`, '')};
};

const isNumberField = ({type, minValue, maxValue, value}) =>
    (type === 'int' || isFloating(type)) ||
    (value || minValue || maxValue) &&
    (!isNaN(Number(value)) || !isNaN(Number(minValue)) || !isNaN(Number(maxValue)));

/**
 *
 * @param {Array.<ServiceDescriptorInputParam>} serDefParams
 * @param {SearchAreaInfo} searchAreaInfo
 * @return {Object}
 */
function prefilterRADec(serDefParams, searchAreaInfo = {}) {
    const foundRa= findParamByUCDOrName(serDefParams,'pos.eq.ra', 'ra');
    const foundDec= findParamByUCDOrName(serDefParams,'pos.eq.dec', 'dec');
    if (!foundRa?.name || !foundDec?.name) return {filteredParams: serDefParams, posDef: undefined};

    const filteredParams = serDefParams.filter(({UCD,name}) =>
        UCD !== 'pos.eq.ra' && UCD !== 'pos.eq.dec' && name !== 'ra' && name !== 'dec');

    const posDef = makeTargetDef({
        centerPt: searchAreaInfo.centerWp,
        raKey: foundRa.name,
        decKey: foundDec.name,
        hipsFOVInDeg: searchAreaInfo.hips_initial_fov,
        mocList: searchAreaInfo.mocList,
        hipsUrl: searchAreaInfo.HiPS,
        coordinateSys: searchAreaInfo.coordinateSys,
        targetPanelExampleRow1: searchAreaInfo.examples ? searchAreaInfo.examples.split('|') : undefined
    });
    return {posDef, filteredParams};
}

function prefilterWavelength(serDefParams, standardID) {
    const foundWlBand =  findParamByUCDAndName(serDefParams,'em.wl','BAND');
    if (!isSIAStandardID(standardID) || !foundWlBand?.name) {
        return {filteredParams: serDefParams, wlDef: undefined};
    }
    const filteredParams = serDefParams.filter(({UCD,name}) => UCD !== 'em.wl' && name !== 'BAND');
    const wlDef= makeWavelengthDef({key: foundWlBand.name});
    return {filteredParams, wlDef};
}

function prefilterObsCoreOps(serDefParams, standardID) {
    if (!isSIAStandardID(standardID)) return {filteredParams: serDefParams, obsDef: undefined};
    const useCalibrationLevel= Boolean(findParamByUCDAndName(serDefParams,'meta.code;obs.calib','CALIB'));
    const useProductType= Boolean(findParamByUCDAndName(serDefParams,'meta.id','DPTYPE'));
    const useSubType= Boolean(findParamByUCDAndName(serDefParams,'meta.id','DPSUBTYPE'));
    const useFacility=  Boolean(findParamByUCDAndName(serDefParams,'meta.id;instr.tel','FACILITY'));
    const useInstrumentName=  Boolean(findParamByUCDAndName(serDefParams,'meta.id;instr','INSTRUMENT'));
    const useCollection= Boolean(findParamByUCDAndName(serDefParams,'meta.id','COLLECTION'));


    if (!useCalibrationLevel && !useProductType && !useFacility && !useInstrumentName && !useCollection) {
        return {filteredParams: serDefParams, obsDef: undefined};
    }
    const filteredParams = serDefParams.filter(({name,UCD}) =>
        !((UCD==='meta.code;obs.calib' && name==='CALIB') ||
            (UCD==='meta.id' && name==='DPTYPE') ||
            (UCD==='meta.id' && name==='DPSUBTYPE') ||
            (UCD==='meta.id;instr.tel' && name==='FACILITY') ||
            (UCD==='meta.id;instr' && name==='INSTRUMENT') ||
            (UCD==='meta.id' && name==='COLLECTION'))
    );

    const obsDef= makeObsCoreOps({
        useCalibrationLevel,
        useProductType,
        useSubType,
        useFacility,
        useInstrumentName,
        useCollection } );

    return {filteredParams, obsDef};
}

/**
 *
 * @param {Array.<ServiceDescriptorInputParam>} serDefParams
 * @param {String} UCD
 * @param {String} name
 * @return {ServiceDescriptorInputParam}
 */
function findParamByUCDOrName(serDefParams, UCD, name){
    const p= serDefParams.find((aParam) => aParam.UCD === UCD);
    if (p) return p;
    return serDefParams.find((aParam) => aParam.name === name);
}

/**
 *
 * @param {Array.<ServiceDescriptorInputParam>} serDefParams
 * @param {String} UCD
 * @param {String} name
 * @return {ServiceDescriptorInputParam}
 */
function findParamByUCDAndName(serDefParams, UCD, name){
    return serDefParams.find((aParam) => aParam.UCD === UCD && aParam.name === name);
}

/**
 *
 * @param {Object} p
 * @param {ServiceDescriptorDef} p.serviceDef
 * @param {String} [p.sRegion]
 * @param {SearchAreaInfo} [p.searchAreaInfo]
 * @param {boolean} [p.hidePredefinedStringFields]
 * @param {String} [p.hipsUrl]
 * @param {Number} [p.fovSize]
 * @returns {Array.<FieldDef>}
 */
export function sdToFieldDefAry({serviceDef, sRegion, searchAreaInfo = {},
                                             hidePredefinedStringFields = true,
                                             hipsUrl, fovSize}) {
    if (!serviceDef?.serDefParams) return [];
    const {filteredParams, fdAry:complexFdAry}= prefilterComplexParams(serviceDef, searchAreaInfo);
    const fdAry = filteredParams
        .filter((serDefParam) => !serDefParam.ref)
        .map((serDefParam) => makeFieldDef({serDefParam,sRegion,searchAreaInfo,hidePredefinedStringFields,hipsUrl,fovSize}) );
    return [...fdAry,...complexFdAry];
}

function prefilterComplexParams(serviceDef, searchAreaInfo) {
    const {filteredParams:f1, posDef} = prefilterRADec(serviceDef.serDefParams, searchAreaInfo);
    const {filteredParams:f2, wlDef} = prefilterWavelength(f1, serviceDef.standardID);
    const {filteredParams, obsDef} = prefilterObsCoreOps(f2, serviceDef.standardID);
    const fdAry= [];
    if (posDef) fdAry.push(posDef);
    if (wlDef) fdAry.push(wlDef);
    if (obsDef) fdAry.push(obsDef);
    return {filteredParams,fdAry};
}

/**
 *
 * @param {Object} p
 * @param {ServiceDescriptorInputParam} p.serDefParam
 * @param {String} [p.sRegion]
 * @param {SearchAreaInfo} [p.searchAreaInfo]
 * @param {boolean} [p.hidePredefinedStringFields]
 * @param {String} [p.hipsUrl]
 * @param {Number} [p.fovSize]
 * @return {FieldDef}
 */
function makeFieldDef({serDefParam, sRegion, searchAreaInfo, hidePredefinedStringFields, hipsUrl, fovSize}) {
        if (!serDefParam) return;
        if (serDefParam.options) {
            return doMakeEnumDef(serDefParam);
        }
        else if (isCircleField(serDefParam) || isCircleFieldLenient(serDefParam)) {
            return doMakeCircleDef(serDefParam,sRegion,searchAreaInfo,hipsUrl,fovSize);
        }
        else if (isPointField(serDefParam)) {
            return doMakePointDef(serDefParam,sRegion,searchAreaInfo,hipsUrl,fovSize);
        }
        else if (isPolygonField(serDefParam) || isPolygonFieldLenient(serDefParam)) {
            return doMakePolygonField(serDefParam,sRegion,searchAreaInfo);
        }
        else if (isRangeField(serDefParam)) {
            return doMakeRangeDef(serDefParam);
        }
        else if (isAreaField(serDefParam)) {
            return doMakeAreaDef(serDefParam);
        }
        else if (isNumberField(serDefParam)) {
            return doMakeNumberDef(serDefParam,hidePredefinedStringFields);
        } else {
            return doMakeUnknownDef(serDefParam,hidePredefinedStringFields);
        }
}


function doMakeUnknownDef(serDefParam, hidePredefinedStringFields) {
    const {value='', name, desc: tooltip, units = ''} = serDefParam;
    return makeUnknownDef({
        key: name, desc: name, tooltip, units, initValue: value ?? '',
        hide: Boolean(value && hidePredefinedStringFields)
    });
}

function doMakeRangeDef({name, desc: tooltip, units = ''}) {
    return makeRangeDef({key: name, desc: name, tooltip, units});
}

function doMakeEnumDef(serDefParam) {
    const {name, desc: tooltip, options, units = ''} = serDefParam;
    const fieldOps = options.split(',').map((op) => ({label: op, value: op}));
    return makeEnumDef({
        key:name, desc: name, tooltip, units,
        initValue: fieldOps[0].value, enumValues: fieldOps
    });

}

function doMakePolygonField(serDefParam, sRegion, searchAreaInfo) {
    const {name, desc: tooltip, units = ''} = serDefParam;
    const {value} = getPolygonInfo(serDefParam);
    return makePolygonDef({key: name, desc: name, tooltip, units, initValue: value, sRegion,
        ...searchAreaInfo?.polygon_examples});
}

function doMakeCircleDef(serDefParam, sRegion, searchAreaInfo, hipsUrl, fovSize) {
    const {name, desc: tooltip, units = ''} = serDefParam;
    const {wpt: centerPt, radius, minValue, maxValue} = getCircleInfo(serDefParam);
    const hipsFOVInDeg= searchAreaInfo?.hips_initial_fov ?? fovSize ?? radius * 2 + radius * .2;
    return makeCircleDef({
        key:name, desc: name, tooltip, units,
        hipsUrl: searchAreaInfo?.HiPS ?? hipsUrl,
        targetKey: 'circleTarget', sizeKey: 'circleSize',
        initValue: radius,
        centerPt: searchAreaInfo?.centerWp ?? centerPt, minValue, maxValue,
        hipsFOVInDeg,
        coordinateSys: searchAreaInfo?.coordinateSys,
        sRegion,
        mocList: searchAreaInfo.mocList,
        ...searchAreaInfo.examples,
    });
}

function doMakePointDef(serDefParam, sRegion, searchAreaInfo, hipsUrl, fovSize) {
    const {name, desc: tooltip, units = ''} = serDefParam;
    return makePointDef({
        key:name, desc: name, tooltip, units,
        hipsUrl: searchAreaInfo?.HiPS ?? hipsUrl,
        targetKey: 'circleTarget',
        centerPt: getPointInfo(serDefParam),
        hipsFOVInDeg: searchAreaInfo?.hips_initial_fov ?? fovSize ?? 2,
        coordinateSys: searchAreaInfo?.coordinateSys,
        mocList: searchAreaInfo.mocList,
        sRegion, ...searchAreaInfo.examples,
    });
}

function doMakeAreaDef(serDefParam) {
    const {name, value = '', desc: tooltip} = serDefParam;
    const maxNum = Number(serDefParam.maxValue);
    const valNum = Number(value);
    const minValue = Number(serDefParam.minValue) || .000277778;
    const maxValue = !isNaN(maxNum) ? maxNum : !isNaN(valNum) ? valNum : 5;
    const initValue= valNum < maxValue && valNum > minValue ? valNum : maxValue;
    return makeAreaDef({ key:name, desc: tooltip, tooltip, initValue, minValue, maxValue});
}

function doMakeNumberDef(serDefParam, hidePredefinedStringFields) {
    const {type, optionalParam: nullAllowed, value = '', name, desc: tooltip, units = ''} = serDefParam;
    const key= name;
    const desc= name;
    const minNum = Number(serDefParam.minValue);
    const maxNum = Number(serDefParam.maxValue);
    let vNum = Number(value);
    let workingType= type;
    let workingValue= value;
    if (type!=='int' && !isFloating(type)
        && (!isNaN(minNum) || !isNaN(maxNum)) && (value==='' || !isNaN(parseFloat(value))) ) {
        workingType= 'double';
        vNum= parseFloat(value);
        workingValue= value==='' ? '' : vNum+'';
    }


    if (workingType === 'int') {
        return makeIntDef({
            key, desc, tooltip, units, precision: 4, nullAllowed,
            initValue: !isNaN(vNum) ? vNum : undefined,
            minValue: !isNaN(minNum) ? minNum : undefined,
            maxValue: !isNaN(maxNum) ? maxNum : undefined,
        });
    }
    else if (isFloating(workingType)) {
        return makeFloatDef({
            key, desc, tooltip, units, precision: 4, nullAllowed,
            initValue: !isNaN(vNum) ? vNum : undefined,
            minValue: !isNaN(minNum) ? minNum : undefined,
            maxValue: !isNaN(maxNum) ? maxNum : undefined,
        });
    }
    else {
        return makeUnknownDef({key: name, desc: name, tooltip, units, initValue: workingValue,
            hide: Boolean(value && hidePredefinedStringFields), nullAllowed:true});
    }

}


/**
 *
 * @param {Array.<FieldDef>} fdAry
 * @param args - object of init values
 * @return {Array.<FieldDef>} array with defaults
 */
export function ingestInitArgs(fdAry, args) {

    return fdAry.map((fd) => {
        const {type,key}= fd;

        switch (type) {
            case FLOAT: case INT: case ENUM: case UNKNOWN: case CHECKBOX:
                return (args[key]) ? {...fd, initValue: args[key]} : fd;
            case POLYGON:
                let v = args[key];
                if (!v || !isString(v)) return fd;
                if (v.toLowerCase().startsWith('polygon')) v= v.substring(8);
                const valStrAry= v.split(' ').filter((s) => s);
                const valNumAry= valStrAry.map( (s) => Number(s) ).filter( (n) => !isNaN(n));
                if (valStrAry.length !== valStrAry.length || valStrAry.length<6 || valStrAry.length%2===1) return fd;
                const finalStr= valNumAry.reduce((str, n, idx) =>
                    str+ ((idx%2 ===0 || idx===valNumAry.length-1) ? `${str.length? ' ':''}${n}` : ' '+n+','), '');

                let sumX=0, sumY=0;
                const len= valNumAry.length;
                for(let i=0; i<len-1; i++) {
                    sumX+=valNumAry[i];
                    sumY+=valNumAry[i+1];
                }
                const cenX= sumX/(len/2);
                const cenY= sumY/(len/2);
                return {...fd,initValue:finalStr, targetDetails:{...fd.targetDetails, centerPt:makeWorldPt(cenX,cenY)}};
            case AREA:
                if (args[ReservedParams.SR.name]) return {...fd, initValue:args[ReservedParams.SR.name]};
                if (args[key] && !isNaN(parseFloat(args[key]))) return {...fd, initValue:parseFloat(args[key])};
                return fd;
            case POSITION:
            case POINT:
                if (args[ReservedParams.POSITION.name]) return {...fd, initValue:args[ReservedParams.POSITION.name]};
                if (args[key] && parseWorldPt(args[key]))  return {...fd, initValue:parseWorldPt(args[POSITION])};
                return fd;
            case CIRCLE:
                if (args[key]) {
                    let v= args[key];
                    if (v.toLowerCase().startsWith('circle')) v= v.substring(7);
                    const cirAry= v.split(' ')
                        .filter((s) => s)
                        .map( (s) => Number(s) )
                        .filter( (n) => !isNaN(n));
                    if (cirAry.length!==3) return fd;
                    return {
                        ...fd, initValue: cirAry[2], targetDetails:{...fd.targetDetails, centerPt: makeWorldPt(cirAry[0],cirAry[1])}
                    };
                }
                else {
                    const newFd= {...fd};
                    if (args[ReservedParams.POSITION.name]) newFd.targetDetails.centerPt= args[ReservedParams.POSITION.name];
                    if (args[ReservedParams.SR.name]) newFd.initValue= args[ReservedParams.SR.name];
                    return newFd;
                }
            default:
                return fd;
        }
    });
}

