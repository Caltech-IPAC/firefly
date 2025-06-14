import {isArray, isNumber, isString} from 'lodash';
import {ReservedParams} from '../../api/WebApi.js';
import {sprintf} from '../../externalSource/sprintf.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {makeFileRequest, makeTblRequest, setNoCache} from '../../tables/TableRequestUtil.js';
import {cisxAdhocServiceUtype, standardIDs} from '../../voAnalyzer/VoConst.js';
import {splitByWhiteSpace, tokenSub} from '../../util/WebUtil.js';
import CoordinateSys from '../../visualize/CoordSys.js';
import {makeWorldPt, parseWorldPt} from '../../visualize/Point.js';
import {
    AREA, CHECKBOX, CIRCLE, ENUM, FLOAT, INT, makeAreaDef, makeCircleDef, makeEnumDef, makeFloatDef, makeIntDef,
    makeObsCoreOps,
    makePointDef, makePolygonDef, makeRangeDef, makeTargetDef, makeUnknownDef, makeWavelengthDef,
    POINT, POLYGON, POSITION, RANGE, UNKNOWN
} from './DynamicDef.js';
import {getServiceDescriptors, isDataLinkServiceDesc} from 'firefly/voAnalyzer/VoDataLinkServDef';



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

export const isSIAStandardID = (standardID) => standardID?.toLowerCase().startsWith(standardIDs.sia);
export const isSSAStandardID = (standardID) => standardID?.toLowerCase().startsWith(standardIDs.ssa);
export const isSODAStandardID = (standardID) => standardID?.toLowerCase().startsWith(standardIDs.soda);
export const isTAPStandardID = (standardID) => standardID?.toLowerCase().startsWith(standardIDs.tap);

export function getStandardIdType(standardID) {
    if (isSIAStandardID(standardID))  return standardIDs.sia;
    if (isSSAStandardID(standardID))  return standardIDs.ssa;
    if (isSODAStandardID(standardID)) return standardIDs.soda;
    if (isTAPStandardID(standardID)) return standardIDs.tap;
}

export const isCisxTapStandardID = (standardID, utype) =>
    standardID.toLowerCase().startsWith(standardIDs.tap) && utype.toLowerCase() === cisxAdhocServiceUtype;


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
        mocList: getMOCList(searchAreaInfo),
        hipsUrl: searchAreaInfo.HiPS,
        coordinateSys: searchAreaInfo.coordinateSys,
        targetPanelExampleRow1: searchAreaInfo.examples ? searchAreaInfo.examples.split('|') : undefined
    });
    return {posDef, filteredParams};
}

function prefilterWavelength(serDefParams, standardID) {
    // const foundWlMin =  findParamByUCDAndName(serDefParams,'em.wl;stat.min','em_min');
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

function findParamByUCDOrName(serDefParams, UCD, name){
    const p= serDefParams.find((aParam) => aParam.UCD === UCD);
    if (p) return p;
    return serDefParams.find((aParam) => aParam.name === name);
}

function findParamByUCDAndName(serDefParams, UCD, name){
    return serDefParams.find((aParam) => aParam.UCD === UCD && aParam.name === name);
}

function makeExamples(inExample) {
    if (!inExample) return {targetPanelExampleRow1:undefined, targetPanelExampleRow2:undefined};
    const examples = inExample.split('|');
    if (examples?.length>1) {
        const cnt= examples.length;
        return {
            targetPanelExampleRow1: examples.slice(0,Math.trunc(cnt/2)),
            targetPanelExampleRow2: examples.slice(Math.trunc(cnt/2))
        };
    }
    else {
        return { targetPanelExampleRow1: [inExample], targetPanelExampleRow2: [] };
    }
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

/**
 *
 * @param {SearchAreaInfo} searchAreaInfo
 * @return {Array.<{mocUrl: String, mocColor: String, title:String}>|undefined}
 */
function getMOCList(searchAreaInfo) {
    const mocAry= Object.keys(searchAreaInfo)
        .filter( (k) => k.toLowerCase()==='moc' || k.match(/moc\d+$/i) )
        .map( (k) => {
            const cnt= k.substring(3,k.length);
            return {
                mocUrl : searchAreaInfo[k],
                title : searchAreaInfo['mocDesc'+cnt] ?? 'MOC'+cnt,
                mocColor: searchAreaInfo['mocColor'+cnt],
                maxFetchDepth: searchAreaInfo['maxFetchDepth'+cnt]
            };
        });
    return mocAry.length ? mocAry : undefined;
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
    const {targetPanelExampleRow1,targetPanelExampleRow2}= makeExamples(searchAreaInfo?.polygon_examples);
    const {value} = getPolygonInfo(serDefParam);
    return makePolygonDef({key: name, desc: name, tooltip, units, initValue: value, sRegion,
        targetPanelExampleRow1,targetPanelExampleRow2});
}

function doMakeCircleDef(serDefParam, sRegion, searchAreaInfo, hipsUrl, fovSize) {
    const {targetPanelExampleRow1,targetPanelExampleRow2}= makeExamples(searchAreaInfo?.examples );
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
        mocList: getMOCList(searchAreaInfo),
        targetPanelExampleRow1,
        targetPanelExampleRow2
    });
}

function doMakePointDef(serDefParam, sRegion, searchAreaInfo, hipsUrl, fovSize) {
    const {targetPanelExampleRow1,targetPanelExampleRow2}= makeExamples(searchAreaInfo?.examples );
    const {name, desc: tooltip, units = ''} = serDefParam;
    return makePointDef({
        key:name, desc: name, tooltip, units,
        hipsUrl: searchAreaInfo?.HiPS ?? hipsUrl,
        targetKey: 'circleTarget',
        centerPt: getPointInfo(serDefParam),
        hipsFOVInDeg: searchAreaInfo?.hips_initial_fov ?? fovSize ?? 2,
        coordinateSys: searchAreaInfo?.coordinateSys,
        mocList: getMOCList(searchAreaInfo),
        sRegion, targetPanelExampleRow1, targetPanelExampleRow2
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
 * @param {Object} cisxUI
 * @param {number} defaultMaxMOCFetchDepth
 * @return {SearchAreaInfo}
 */
export function makeSearchAreaInfo(cisxUI, defaultMaxMOCFetchDepth) {
    if (!cisxUI) return;
    const tmpObj = cisxUI.reduce((obj, {name, value, UCD}) => {
        switch (name) {
            case 'hips_initial_fov':
                obj[name] = Number(value);
                break;
            case 'hips_initial_dec':
            case 'hips_initial_ra':
                obj[name] = Number(value);
                obj.ptIsGalactic= UCD?.includes('galactic');
                break;
            default:
                if (!name?.startsWith('moc')) obj[name] = value;
                break;
        }
        return obj;
    }, {});

    const mocsObj= findMocs(cisxUI);
    const {hips_initial_ra, hips_initial_dec, hips_frame, ptIsGalactic} = tmpObj;
    const hipsProjCsys = hips_frame?.trim().toLowerCase()==='galactic' ? CoordinateSys.GALACTIC : CoordinateSys.EQ_J2000;
    const ptCsys= ptIsGalactic ? CoordinateSys.GALACTIC : CoordinateSys.EQ_J2000;
    const centerWp = makeWorldPt(hips_initial_ra, hips_initial_dec, ptCsys);
    return {...tmpObj, ...mocsObj, centerWp,
        coordinateSys: hipsProjCsys.toString(), maxFetchDepth:defaultMaxMOCFetchDepth};
}


function findMocs(cisxUI) {
    const mocColor= cisxUI.filter( (obj) => obj?.name?.toLowerCase().startsWith('moc_color'));
    const moc=  cisxUI.filter( (obj) => obj?.name?.toLowerCase()==='moc' || obj?.name?.match(/moc\d+$/i) );
    const combinedObj= [...mocColor,...moc].reduce( (obj, {name, value, desc}) => {
        if (name.startsWith('moc_color')) {
            obj['mocColor'+ name.substring(9,name.length)]= value;
        }
        else {
            obj[name]= value;
            obj['mocDesc'+ name.substring(3,name.length)] = desc;
        }
        return obj;
    },{});
    return combinedObj;
}




let tblCnt=1;

export function makeServiceDescriptorSearchRequest(request, siaConstraints,serviceDescriptor, extraMeta={}) {
    const {standardID = '', accessURL, utype, serDefParams, title, cisxUI=[]} = serviceDescriptor;
    const hiddenColumns= cisxUI.find( (e) => e.name==='hidden_columns')?.value;
    const tblSortOrder= cisxUI.find( (e) => e.name==='table_sort_order')?.value;
    const MAXREC = 50000;
    const tblTitle= `${title} - ${tblCnt++}`;

    const hideObj= hiddenColumns ?
        Object.fromEntries(hiddenColumns.split(',').map((c) => [ `col.${c}.visibility`, 'hide'] )) : {};

    const sAry= tblSortOrder?.match(/([^,]+),(.+)/);
    let sortObj= {};
    if (sAry) {
        const [,dir,sortBy] = sAry;
        sortObj= {sortInfo: sortInfoString(sortBy, dir?.toUpperCase()==='ASC')};
    }

    const options= {...sortObj, META_INFO: { ...hideObj, ...extraMeta }};

    if (isSIAStandardID(standardID)) {
        const reqParams= new URLSearchParams(request);
        const siaParams= new URLSearchParams();
        siaConstraints.forEach( (s) => {
            const [k,v]= s.split('=');
            if (k && v) siaParams.append(k,v);
        });
        const params= new URLSearchParams([...reqParams, ...siaParams]);
        const url= params.size ? accessURL+'?'+params.toString() : accessURL;
        return makeFileRequest(tblTitle, url, undefined, options);   //todo- figure out title
    }
    else if (isSSAStandardID(standardID)) {
        const url = accessURL + '?' + new URLSearchParams(request).toString();
        return makeFileRequest(tblTitle, url, undefined, options);   //todo- figure out title
    }
    else if (isCisxTapStandardID(standardID, utype)) {
        const doAsync = standardID.toLowerCase().includes('async');
        const query = serDefParams.find(({name}) => name === 'QUERY')?.value;
        const finalQuery = tokenSub(request, query);
        let serviceUrl = accessURL;
        if (accessURL.endsWith('/sync')) serviceUrl = accessURL.substring(0, accessURL.length - 5);
        if (accessURL.endsWith('/async')) serviceUrl = accessURL.substring(0, accessURL.length - 6);

        if (!query) return;
        if (doAsync) {
            const asyncReq = makeTblRequest('AsyncTapQuery', tblTitle, {serviceUrl, QUERY: finalQuery, MAXREC}, options);
            setNoCache(asyncReq);
            return asyncReq;
        } else {
            const serParam = new URLSearchParams({QUERY: finalQuery, REQUEST: 'doQuery', LANG: 'ADQL', MAXREC});
            const completeUrl = serviceUrl + '/sync?' + serParam.toString();
            return makeFileRequest(title, completeUrl,undefined, options);   //todo- figure out title
        }

    }
    else {
        //todo: we should to call file analysis first
        const url = accessURL + '?' + new URLSearchParams(request).toString();
        return makeFileRequest(tblTitle, url, undefined,options);   //todo- figure out title
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

//check for and return a datalink service descriptor url and input params, if one is found
export function checkForDatalinkServDesc(tblModel) {
    const serviceDescriptors = getServiceDescriptors(tblModel);
    if (!serviceDescriptors) return null;

    for (const sd of serviceDescriptors) {
        //serviceDescriptors.forEach((sd) => {
        const isDatalinkSerDesc = isDataLinkServiceDesc(sd);
        if (isDatalinkSerDesc) {
            const productUrl = {
                accessURL: sd?.accessURL || '',
                inputParams: {}
            };

            if (sd?.serDefParams) {
                for (const param of sd.serDefParams) {
                    productUrl.inputParams[param?.name] = {value: param.value, ref: param.ref};
                }
            }

            //return only when valid datalink access url is found
            if (productUrl.accessURL || Object.keys(productUrl.inputParams).length > 0) {
                return productUrl;
            }

        }
    }
    return null; //no valid datalink service descriptor found
}
