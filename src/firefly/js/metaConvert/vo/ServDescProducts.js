import {isEmpty, isNumber} from 'lodash';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {getCellValue, getColumnByRef} from '../../tables/TableUtil.js';
import {makeCircleString} from '../../ui/dynamic/DynamicUISearchPanel';
import {isSIAStandardID} from '../../ui/dynamic/ServiceDefTools';
import {makeWorldPt} from '../../visualize/Point';
import {isDatalinkTable, isObsCoreLike} from '../../voAnalyzer/TableAnalysis';
import {CUTOUT_UCDs, DEC_UCDs, RA_UCDs} from '../../voAnalyzer/VoConst';

import {isDataLinkServiceDesc} from '../../voAnalyzer/VoDataLinkServDef.js';
import {isDefined} from '../../util/WebUtil.js';
import {makeAnalysisActivateFunc} from '../AnalysisUtils.js';
import {DEFAULT_DATA_PRODUCTS_COMPONENT_KEY} from '../DataProductsCntlr.js';
import {dpdtAnalyze, dpdtImage} from '../DataProductsType.js';
import {createSingleImageActivate, createSingleImageExtraction} from '../ImageDataProductsUtil';
import {getObsCoreRowMetaInfo} from './ObsCoreConverter';
import {makeObsCoreRequest} from './VORequest.js';


export const SD_CUTOUT_KEY= 'sdCutoutSize';
export const SD_DEFAULT_SPACIAL_CUTOUT_SIZE= .01;
export const SD_DEFAULT_PIXEL_CUTOUT_SIZE= 200;

/**
 *
 * @param {Object} p
 * @param p.name
 * @param p.serDef
 * @param p.sourceTable
 * @param p.sourceRow
 * @param p.idx
 * @param p.positionWP
 * @param p.activateParams
 * @param {DataProductsFactoryOptions} p.options
 * @param p.titleStr
 * @param p.activeMenuLookupKey
 * @param p.menuKey
 * @param [p.datalinkExtra]
 * @return {DataProductsDisplayType}
 */
export function makeServiceDefDataProduct({
                                              name, serDef, sourceTable, sourceRow, idx, positionWP, activateParams,
                                              options, titleStr, activeMenuLookupKey, menuKey,
                                              datalinkExtra = {} }) {
    const {title: servDescTitle = '', accessURL, standardID, serDefParams, ID} = serDef;
    const {activateServiceDef=false}= options;

    const allowsInput = serDefParams.some((p) => p.allowsInput);
    const noInputRequired = serDefParams.some((p) => !p.inputRequired);
    const {semantics, size, sRegion, prodTypeHint, serviceDefRef, dlAnalysis} = datalinkExtra;

    if (dlAnalysis?.isCutout && canMakeCutoutProduct(serDef,positionWP,sourceRow)) {
       return makeCutoutProduct({
           name, serDef, sourceTable, sourceRow, idx, positionWP, activateParams,
           options, titleStr, activeMenuLookupKey, menuKey, datalinkExtra
       });
    }
    else if (activateServiceDef && noInputRequired) {
        const url= makeUrlFromParams(accessURL, serDef, idx, getComponentInputs(serDef,options));
        const request = makeObsCoreRequest(url, positionWP, titleStr, sourceTable, sourceRow);
        const activate = makeAnalysisActivateFunc({table:sourceTable, row:sourceRow, request, activateParams,
            menuKey, dataTypeHint:prodTypeHint, serDef, options});
        return dpdtAnalyze({
            name:'Show: ' + (titleStr || name), activate, url:request.getURL(), serDef, menuKey,
            activeMenuLookupKey, request, sRegion, prodTypeHint, semantics, size, serviceDefRef});
    } else {
        const request = makeObsCoreRequest(accessURL, positionWP, titleStr, sourceTable, sourceRow);
        const activate = makeAnalysisActivateFunc({table:sourceTable, row:sourceRow, request, activateParams, menuKey,
            dataTypeHint:prodTypeHint ?? 'unknown', serDef, originalTitle:name,options});
        const entryName = `Show: ${titleStr || servDescTitle || `Service #${idx}: ${name}`} ${allowsInput ? ' (Input Required)' : ''}`;
        return dpdtAnalyze({
            name:entryName, activate, url:request.getURL(), serDef, menuKey,
            activeMenuLookupKey, request, allowsInput, serviceDefRef, standardID, ID,
            semantics, size, sRegion,
            prodTypeHint: prodTypeHint ?? 'unknown'
            });
    }
}


const CUTOUT_NAME_GUESS_LIST= ['size'];

function canMakeCutoutProduct(serDef, positionWP,sourceRow){
    const {standardID,serDefParams} = serDef;

    if (!positionWP) { // look for ra/dec columns
        const wp= findWorldPtInServiceDef(serDef,sourceRow);
        if (!wp) return false;
    }

    if (isSIAStandardID(standardID) || serDefParams.find( ({xtype}) => xtype?.toLowerCase()==='circle')  ) {
        return true;
    }
    const obsFieldParam= serDefParams.find( ({UCD=''}) =>
        CUTOUT_UCDs.find( (testUcd) => UCD.toLowerCase().includes(testUcd)) );
    if (obsFieldParam) return true;


    const nameGuess= serDefParams.find( ({name=''}) =>
        CUTOUT_NAME_GUESS_LIST.find( (testName) => name.toLowerCase()===testName) );

    return Boolean(nameGuess);
}

function findWorldPtInServiceDef(serDef,sourceRow) {
    const {serDefParams,sdSourceTable, dataLinkTableRowIdx} = serDef;
    const raParam= serDefParams.find( ({UCD=''}) =>
        RA_UCDs.find( (testUcd) => UCD.toLowerCase().includes(testUcd)) );
    const decParam= serDefParams.find( ({UCD=''}) =>
        DEC_UCDs.find( (testUcd) => UCD.toLowerCase().includes(testUcd)) );
    if (!raParam && !decParam) return;

    let raVal= raParam.value;
    let decVal= decParam.value;

    if (raVal && decVal) return makeWorldPt(raVal,decVal);
    if (!sdSourceTable) return;

    const hasDLTable= isDatalinkTable(sdSourceTable);
    const hasDLRow= isDefined(dataLinkTableRowIdx);
    const hasSourceRow= isDefined(sourceRow);
    const row= hasDLTable && hasDLRow ? dataLinkTableRowIdx : hasSourceRow ? sourceRow : undefined;

    if (!raVal && raParam.ref) {
        const col = getColumnByRef(sdSourceTable, raParam.ref);
        if (col && row > -1) raVal = getCellValue(sdSourceTable, row, col.name);
    }

    if (!decVal && decParam.ref) {
        const col = getColumnByRef(sdSourceTable, decParam.ref);
        if (col && row > -1) decVal = getCellValue(sdSourceTable, row, col.name);
    }

    return (raVal && decVal) ? makeWorldPt(raVal,decVal) : undefined;
}

function makeCutoutProduct({ name, serDef, sourceTable, sourceRow, idx, positionWP, activateParams,
                             options, titleStr, menuKey}) {

    const {accessURL, standardID, serDefParams, sdSourceTable} = serDef;
    const key= options.dataProductsComponentKey ?? DEFAULT_DATA_PRODUCTS_COMPONENT_KEY;
    const cutoutSize= getComponentState(key,{})[SD_CUTOUT_KEY] ?? 0.0213;
    if (cutoutSize<=0) return; // must be greater than 0
    if (!positionWP) {
        positionWP= findWorldPtInServiceDef(serDef,sourceRow);
    }
    if (!positionWP) return;  // this must exist, should check in calling function

    let titleToUse= titleStr;

    if (isDefined(serDef.dataLinkTableRowIdx) && isObsCoreLike(sourceTable)) { // this service def, from datalink, in obscore (normal cawse)
        titleToUse= getObsCoreRowMetaInfo(sourceTable,sourceRow)?.titleStr ?? titleStr;
    }
    let params;
    const cutoutOptions= {...options};
    const {xtypeKeys=[],ucdKeys=[],paramNameKeys=[]}= cutoutOptions;
    let pixelBasedCutout= false;
    if (isSIAStandardID(standardID) || serDefParams.find( ({xtype}) => xtype?.toLowerCase()==='circle')  ) {
        cutoutOptions.xtypeKeys= [...xtypeKeys,'circle'];
        params= {circle : makeCircleString(positionWP.x,positionWP.y,cutoutSize,standardID)};
    }
    else {
        const obsFieldParam= serDefParams.find( ({UCD=''}) =>
                              CUTOUT_UCDs.find( (testUcd) => UCD.toLowerCase().includes(testUcd)) );
        if (obsFieldParam) {
            const ucd= obsFieldParam.UCD;
            const sdSizeValue= Number(obsFieldParam.value);
            cutoutOptions.ucdKeys= [...ucdKeys,ucd];

            // note: the size is set as a number, if is a string it is coming from the dialog
            if (isNumber(cutoutSize) && cutoutSize===SD_DEFAULT_SPACIAL_CUTOUT_SIZE && sdSizeValue!==cutoutSize) {
                params= {[ucd] : sdSizeValue};
                dispatchComponentStateChange(key,{ [SD_CUTOUT_KEY]: sdSizeValue } );
            }
            else {
                params= {[ucd] : cutoutSize};
            }
        }
        else { // handle pixel based cutout
            const nameGuess= serDefParams.find( ({name=''}) =>
                CUTOUT_NAME_GUESS_LIST.find( (testName) => name.toLowerCase()===testName) );
            cutoutOptions.paramNameKeys= [...paramNameKeys,nameGuess.name];
            const sizeStr= cutoutSize ? cutoutSize+'' : '';
            if (sizeStr.endsWith('px')) {
                params= {[nameGuess.name] : cutoutSize.substring(0,sizeStr.length-2)};
            }
            else {
                const valNum= parseInt(nameGuess.value) || SD_DEFAULT_PIXEL_CUTOUT_SIZE;
                params= {[nameGuess.name] : valNum};
                dispatchComponentStateChange(key,{ [SD_CUTOUT_KEY]: valNum+'px' } );
            }
            pixelBasedCutout= true;
        }
    }
    const url= makeUrlFromParams(accessURL, serDef, idx, getComponentInputs(serDef,cutoutOptions,params));
    const request = makeObsCoreRequest(url, positionWP, titleToUse, sourceTable, sourceRow);

    const tbl= sourceTable ?? sdSourceTable;
    const activate= createSingleImageActivate(request,activateParams.imageViewerId, tbl?.tbl_id,
        tbl?.highlightedRow);
    return dpdtImage({
        name:'Show: Cutout: ' + (titleToUse || name),
        activate, menuKey,
        extraction: createSingleImageExtraction(request), enableCutout:true, pixelBasedCutout,
        request, override:false, interpretedData:false, requestDefault:false});
}


/**
 * return a list of inputs from the user that will go into the service descriptor URL
 * @param serDef
 * @param {DataProductsFactoryOptions} options
 * @return {Object.<string, *>}
 */
function getComponentInputs(serDef, options, moreParams={}) {
    const key= options.dataProductsComponentKey ?? DEFAULT_DATA_PRODUCTS_COMPONENT_KEY;
    const valueObj= {...getComponentState(key,{}), ...moreParams};
    if (isEmpty(valueObj)) return {};
    const {serDefParams}= serDef;
    const {paramNameKeys= [], ucdKeys=[], utypeKeys=[], xtypeKeys=[]}= options;
    const userInputParams= paramNameKeys.reduce( (obj, key) => {
        if (isDefined(valueObj[key])) obj[key]= valueObj[key];
        return obj;
    },{});
    const ucdParams= ucdKeys.reduce( (obj, key) => {
        if (!isDefined(valueObj[key])) return obj;
        const foundParam= serDefParams.find((p) => p.UCD===key);
        if (foundParam) obj[foundParam.name]= valueObj[key];
        return obj;
    },{});
    const utypeParams= utypeKeys.reduce( (obj, key) => {
        if (!isDefined(valueObj[key])) return obj;
        const foundParam= serDefParams.find((p) => p.utype===key);
        if (foundParam) obj[foundParam.name]= valueObj[key];
        return obj;
    },{});
    const xtypeParams= xtypeKeys.reduce( (obj, key) => {
        if (!isDefined(valueObj[key])) return obj;
        const foundParam= serDefParams.find((p) => p.xtype===key);
        if (foundParam) obj[foundParam.name]= valueObj[key];
        return obj;
    },{});
    return {...ucdParams, ...utypeParams, ...userInputParams, ...xtypeParams};
}

/**
 *
 * @param {Object} p
 * @param {Array.<ServiceDescriptorDef>} p.descriptors
 * @param {WorldPt|undefined} p.positionWP
 * @param {TableModel} p.table
 * @param {number} p.row
 * @param {ActivateParams} p.activateParams
 * @param {String} p.activeMenuLookupKey
 * @param {DataProductsFactoryOptions} p.options
 * @return {Array.<DataProductsDisplayType>}
 */
export function createServDescMenuRet({ descriptors, positionWP, table, row,
                                          activateParams, activeMenuLookupKey, options }) {
    return descriptors
        .filter((sDesc) => !isDataLinkServiceDesc(sDesc))
        .map((serDef, idx) => {
            return makeServiceDefDataProduct({
                name: 'Show: ' + serDef.title,
                serDef, positionWP,
                sourceTable: table, sourceRow: row, idx: row,
                activateParams, options, activeMenuLookupKey,
                titleStr: serDef.title, menuKey: 'serdesc-dlt-' + idx
            });
        });
}

export function makeUrlFromParams(url, serDef, rowIdx, userInputParams = {}) {
    const sendParams = {};
    serDef?.serDefParams  // if it is defaulted, then set it
        ?.filter(({value}) => isDefined(value))
        .forEach(({name, value}) => sendParams[name] = value);
    serDef?.serDefParams // if it is referenced, then set it
        ?.filter(({ref}) => ref)
        .forEach((p) => sendParams[p.name] = getCellValue(serDef.sdSourceTable, rowIdx, p.colName));
    userInputParams && Object.entries(userInputParams).forEach(([k, v]) => v && (sendParams[k] = v));
    const newUrl = new URL(url);
    if (!newUrl) return undefined;
    Object.entries(sendParams).forEach(([k, v]) => newUrl.searchParams.append(k, v));
    logServiceDescriptor(newUrl, sendParams, newUrl.toString());
    return newUrl.toString();
}
function logServiceDescriptor(baseUrl, sendParams, newUrl) {
    // console.log(`service descriptor base URL: ${baseUrl}`);
    // Object.entries(sendParams).forEach(([k,v]) => console.log(`param: ${k}, value: ${v}`));
    console.log(`service descriptor new URL: ${newUrl}`);
}
