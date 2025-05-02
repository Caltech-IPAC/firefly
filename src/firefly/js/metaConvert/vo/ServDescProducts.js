import {isEmpty, isNumber, isUndefined} from 'lodash';
import {getComponentState} from '../../core/ComponentCntlr.js';
import {getCellValue} from '../../tables/TableUtil.js';
import {CONTEXT_PARAMS_STR, makeCircleString} from '../../ui/dynamic/DynamicUISearchPanel';
import {isSIAStandardID} from '../../ui/dynamic/ServiceDefTools';
import {findCutoutTarget, getCutoutErrorStr, getCutoutSize, setCutoutSize} from '../../ui/tap/Cutout';
import {PlotAttribute} from '../../visualize/PlotAttribute';
import {isCatalog, isObsCoreLike} from '../../voAnalyzer/TableAnalysis';
import {CUTOUT_UCDs, DEC_UCDs, RA_UCDs} from '../../voAnalyzer/VoConst';

import {findWorldPtInServiceDef, isDataLinkServiceDesc} from '../../voAnalyzer/VoDataLinkServDef.js';
import {isDefined} from '../../util/WebUtil.js';
import {makeAnalysisActivateFunc} from '../AnalysisUtils.js';
import {dpdtAnalyze, dpdtImage} from '../DataProductsType.js';
import {createSingleImageActivate, createSingleImageExtraction} from '../ImageDataProductsUtil';
import {getObsCoreRowMetaInfo} from './ObsCoreConverter';
import {makeObsCoreRequest} from './VORequest.js';


export const SD_DEFAULT_SPACIAL_CUTOUT_SIZE= .01;
export const SD_DEFAULT_PIXEL_CUTOUT_SIZE= 200;

/**
 *
 * @param {Object} p
 * @param p.name
 * @param p.dropDownText
 * @param p.serDef
 * @param p.sourceTable
 * @param p.sourceRow
 * @param p.idx
 * @param p.positionWP
 * @param p.dlData
 * @param p.activateParams
 * @param {DataProductsFactoryOptions} p.options
 * @param p.titleStr
 * @param p.activeMenuLookupKey
 * @param p.menuKey
 * @return {DataProductsDisplayType}
 */
export function makeServiceDefDataProduct({ dropDownText, name, serDef, sourceTable, sourceRow, idx, positionWP,
                                              activateParams, options, titleStr, activeMenuLookupKey, menuKey, dlData={}}) {

    const {title: servDescTitle = '', accessURL, standardID, serDefParams, ID} = serDef;
    const {activateServiceDef=false}= options;

    const allowsInput = serDefParams.some((p) => p.allowsInput);
    const noInputRequired = serDefParams.some((p) => !p.inputRequired);
    const {semantics, size, serviceDefRef, dlAnalysis, prodTypeHint} = dlData;
    const sRegion= dlData.sourceObsCoreData?.s_region ?? '';

    if (dlAnalysis?.isCutout && canMakeCutoutProduct(serDef,sourceTable,sourceRow,options)) {
       return makeCutoutProduct({
           name, serDef, sourceTable, sourceRow, idx, activateParams,
           options, titleStr, activeMenuLookupKey, menuKey, dlData,
       });
    }
    else if (activateServiceDef && noInputRequired) {
        const url= makeUrlFromParams(accessURL, serDef, idx, getComponentInputs(serDef,options));
        const request = makeObsCoreRequest(url, positionWP, titleStr, sourceTable, sourceRow);
        const activate = makeAnalysisActivateFunc({table:sourceTable, row:sourceRow, request, activateParams,
            menuKey, dataTypeHint:prodTypeHint, serDef, dlData, originalTitle:name, options});
        const tName= (titleStr || name);
        return dpdtAnalyze({
            name:tName, dropDownText: dropDownText ?? 'Show: '+tName,
            activate, url:request.getURL(), serDef, menuKey, dlData,
            activeMenuLookupKey, request, sRegion, prodTypeHint, semantics, size, serviceDefRef});
    } else {
        const request = makeObsCoreRequest(accessURL, positionWP, titleStr, sourceTable, sourceRow);
        const activate = makeAnalysisActivateFunc({table:sourceTable, row:sourceRow, request, activateParams, menuKey,
            dlData, dataTypeHint:prodTypeHint ?? 'unknown', serDef, originalTitle:name,options});
        const tName = `${titleStr || servDescTitle || `Service #${idx}: ${name}`} ${allowsInput ? ' (Input Required)' : ''}`;
        return dpdtAnalyze({
            name:tName, dropDownText: dropDownText ?? 'Show: '+tName,
            activate, url:request.getURL(), serDef, menuKey,
            activeMenuLookupKey, request, allowsInput, serviceDefRef, standardID, ID,
            semantics, size, sRegion, dlData,
            prodTypeHint: prodTypeHint ?? 'unknown'
            });
    }
}


const CUTOUT_NAME_GUESS_LIST= ['size'];

function canMakeCutoutProduct(serDef,table,sourceRow,options){
    const {standardID,serDefParams} = serDef;

    const {positionWP}= findCutoutTarget(options.dataProductsComponentKey,serDef,table,sourceRow);
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


function makeCutoutProduct({ name, serDef, sourceTable, sourceRow, idx, activateParams, dlData,
                             options, titleStr, menuKey}) {

    const {accessURL, standardID, serDefParams, sdSourceTable} = serDef;
    const key= options.dataProductsComponentKey;
    const cutoutSize= getCutoutSize(key);

    if (cutoutSize<=0) return; // must be greater than 0

    const {requestedType,foundType,positionWP}= findCutoutTarget(key,serDef,sourceTable,sourceRow);
    if (!positionWP) return;  // positionWP must exist

    let titleToUse= titleStr;

    if (isDefined(serDef.dataLinkTableRowIdx)) {
        titleToUse= getObsCoreRowMetaInfo(sourceTable,sourceRow).titleStr || name || titleStr;
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
            const raParam= serDefParams.find( ({UCD=''}) =>
                RA_UCDs.find( (testUcd) => UCD.toLowerCase().includes(testUcd)) );
            const decParam= serDefParams.find( ({UCD=''}) =>
                DEC_UCDs.find( (testUcd) => UCD.toLowerCase().includes(testUcd)) );
            const obsfieldUcd= obsFieldParam.UCD;
            const sdSizeValue= Number(obsFieldParam.value);
            cutoutOptions.ucdKeys= [...ucdKeys,obsfieldUcd];

            // note: the size is set as a number, if is a string it is coming from the dialog
            if (isNumber(cutoutSize) && cutoutSize===SD_DEFAULT_SPACIAL_CUTOUT_SIZE && sdSizeValue!==cutoutSize) {
                params= {[obsfieldUcd] : sdSizeValue};
                setCutoutSize(key,sdSizeValue, sourceTable?.tbl_id);
            }
            else {
                params= {[obsfieldUcd] : cutoutSize};
            }
            if (raParam) {
                cutoutOptions.ucdKeys.push(raParam.UCD);
                params[raParam.UCD]= positionWP.x;
            }
            if (decParam) {
                cutoutOptions.ucdKeys.push(decParam.UCD);
                params[decParam.UCD]= positionWP.y;
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
                setCutoutSize(key,valNum+'px', sourceTable?.tbl_id);
            }
            pixelBasedCutout= true;
        }
    }
    const url= makeUrlFromParams(accessURL, serDef, dlData?.rowIdx ?? idx, getComponentInputs(serDef,cutoutOptions,params));
    const request = makeObsCoreRequest(url, positionWP, titleToUse, sourceTable, sourceRow);
    if (foundType!==requestedType) {
        request.setAttributes({[PlotAttribute.USER_WARNINGS]: getCutoutErrorStr(foundType,requestedType)});
    }

    const tbl= sourceTable ?? sdSourceTable;
    const activate= createSingleImageActivate(request,activateParams.imageViewerId, tbl?.tbl_id,
        tbl?.highlightedRow);
    return dpdtImage({
        name:'Show: Cutout: ' + (titleToUse || name),
        activate, menuKey, dlData,
        extraction: createSingleImageExtraction(request, dlData?.sourceObsCoreData), enableCutout:true, pixelBasedCutout,
        request, override:false, interpretedData:false, requestDefault:false});
}


/**
 * return a list of inputs from the user that will go into the service descriptor URL
 * @param serDef
 * @param {DataProductsFactoryOptions} options
 * @param moreParams
 * @return {Object.<string, *>}
 */
function getComponentInputs(serDef, options, moreParams={}) {
    const key= options.dataProductsComponentKey;
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
                                        activateParams, activeMenuLookupKey, options,
                                      }) {

    let options0= options;
    if (isUndefined(options.activateServiceDef) && !isObsCoreLike(table) && isCatalog(table)) {
        // common case: a catalog table, activateServiceDef not specifically set
        // go ahead and call the first service descriptor, it is probably a time series or a spectrum
        options0={...options,activateServiceDef:true};
    }

    return descriptors
        .filter((sDesc) => !isDataLinkServiceDesc(sDesc))
        .map((serDef, idx) => {
            return makeServiceDefDataProduct({
                name: serDef.title,
                dropDownText: 'Show: ' + serDef.title,
                serDef, positionWP,
                sourceTable: table, sourceRow: row, idx: row,
                activateParams,
                options: idx===0 ? options0 : options,
                activeMenuLookupKey,
                titleStr: serDef.title, menuKey: 'serdesc-dlt-' + idx
            });
        });
}

export function makeUrlFromParams(url, serDef, rowIdx, userInputParams = {}) {
    if (!url) return undefined;
    const sendParams = new URLSearchParams();
    serDef?.serDefParams  // if it is defaulted, then set it
        ?.filter(({value}) => isDefined(value))
        .forEach(({name, value}) => sendParams.set(name, value));
    serDef?.serDefParams // if it is referenced, then set it
        ?.filter(({ref}) => ref)
        .forEach((p) => sendParams.set(p.name, getCellValue(serDef.sdSourceTable, rowIdx, p.colName)));
    Object.entries(userInputParams)
        .forEach(([k, v]) => v && k!==CONTEXT_PARAMS_STR && sendParams.set(k,v));

    const inputURL= new URL(url);
    const additionalParams= new URLSearchParams(userInputParams?.[CONTEXT_PARAMS_STR]);
    const params= new URLSearchParams([...inputURL.searchParams, ...sendParams, ...additionalParams]);
    const newUrl= params.size ? inputURL.toString().split('?')[0]+'?'+params.toString() : url;
    logServiceDescriptor(newUrl, params, newUrl);
    return newUrl;
}
function logServiceDescriptor(baseUrl, params, newUrl) {
     // console.log(`service descriptor base URL: ${baseUrl}`);
     // console.log(`service descriptor new URL: ${newUrl}`);
}
