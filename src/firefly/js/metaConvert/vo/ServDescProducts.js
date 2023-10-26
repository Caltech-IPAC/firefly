import {isEmpty} from 'lodash';
import {getComponentState} from '../../core/ComponentCntlr.js';
import {getCellValue} from '../../tables/TableUtil.js';

import {isDataLinkServiceDesc} from '../../voAnalyzer/VoDataLinkServDef.js';
import {isDefined} from '../../util/WebUtil.js';
import {makeAnalysisActivateFunc} from '../AnalysisUtils.js';
import {DEFAULT_DATA_PRODUCTS_COMPONENT_KEY} from '../DataProductsCntlr.js';
import {dpdtAnalyze} from '../DataProductsType.js';
import {makeObsCoreRequest} from './VORequest.js';



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
    const {semantics, size, sRegion, prodTypeHint, serviceDefRef} = datalinkExtra;

    if (activateServiceDef && noInputRequired) {
        const url= makeUrlFromParams(accessURL, serDef, idx, getComponentInputs(serDef,options));
        const request = makeObsCoreRequest(url, positionWP, titleStr, sourceTable, sourceRow);
        const activate = makeAnalysisActivateFunc({table:sourceTable, row:sourceRow, request, activateParams,
            menuKey, dataTypeHint:prodTypeHint, options});
        return dpdtAnalyze({
            name:'Show: ' + (titleStr || name), activate, url:request.getURL(), menuKey,
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

/**
 * return a list of inputs from the user that will go into the service descriptor URL
 * @param serDef
 * @param {DataProductsFactoryOptions} options
 * @return {{[p: string]: *}|{}}
 */
function getComponentInputs(serDef, options) {
    const key= options.dataProductsComponentKey ?? DEFAULT_DATA_PRODUCTS_COMPONENT_KEY;
    const valueObj= getComponentState(key,{});
    if (isEmpty(valueObj)) return {};
    const {serDefParams}= serDef;
    const {paramNameKeys= [], ucdKeys=[], utypeKeys=[]}= options;
    const userInputParams= paramNameKeys.reduce( (obj, key) => {
        if (isDefined(valueObj[key])) obj[key]= valueObj[key];
        return obj;
    },{});
    const ucdParams= ucdKeys.reduce( (obj, key) => {
        if (isDefined(valueObj[key])) return obj;
        const foundParam= serDefParams.find((p) => p.UCD===key);
        if (foundParam) obj[foundParam.name]= valueObj[key];
        return obj;
    },{});
    const utypeParams= utypeKeys.reduce( (obj, key) => {
        if (isDefined(valueObj[key])) return obj;
        const foundParam= serDefParams.find((p) => p.utype===key);
        if (foundParam) obj[foundParam.name]= valueObj[key];
        return obj;
    },{});
    return {...ucdParams, ...utypeParams, ...userInputParams};
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
    serDef?.serDefParams
        ?.filter(({value}) => isDefined(value))
        .forEach(({name, value}) => sendParams[name] = value);
    serDef?.serDefParams
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
