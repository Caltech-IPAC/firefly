import {isEmpty} from 'lodash';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {getCellValue, getColumns, hasRowAccess} from '../../tables/TableUtil.js';
import {
    getObsCoreAccessURL, getObsCoreProdTypeCol, getObsReleaseDate, getObsTitle, getProdTypeGuess, getSearchTarget,
    isFormatDataLink,
    isFormatPng,
    isFormatVoTable, makeWorldPtUsingCenterColumns
} from '../../voAnalyzer/TableAnalysis.js';
import {getServiceDescriptors, isDataLinkServiceDesc} from '../../voAnalyzer/VoDataLinkServDef.js';
import {tokenSub} from '../../util/WebUtil.js';
import {uploadAndAnalyze} from '../AnalysisUtils.js';
import {dispatchUpdateActiveKey} from '../DataProductsCntlr.js';
import {dpdtFromMenu, dpdtMessageWithDownload, dpdtPNG, dpdtSimpleMsg,} from '../DataProductsType.js';
import {createGuessDataType} from './DataLinkProcessor.js';
import {
    createGridResult, datalinkDescribeThreeColor, getDatalinkRelatedGridProduct, getDatalinkSingleDataProduct, makeDlUrl
} from './DatalinkProducts.js';

import {createServDescMenuRet} from './ServDescProducts.js';
import {makeObsCoreRequest} from './VORequest.js';

const GIG= 1048576 * 1024;
const DEF_MAX_PLOTS= 8;


/**
 *
 * @param {TableModel} table
 * @param {DataProductsConvertType} converterTemplate
 * @param {DataProductsFactoryOptions} options
 * @return {DataProductsConvertType}
 */
export function makeObsCoreConverter(table,converterTemplate,options={}) {
    if (!table) return converterTemplate;
    const canRelatedGrid= options.allowImageRelatedGrid?? false;
    const threeColor= converterTemplate.threeColor && options?.allowImageRelatedGrid;
    const baseRetOb= {...converterTemplate,
        initialLayout: options.dataLinkInitialLayout ?? 'single',
        describeThreeColor: (threeColor) ? describeObsThreeColor : undefined,
        threeColor,
        canGrid: false,
        maxPlots:canRelatedGrid?DEF_MAX_PLOTS:1,
        hasRelatedBands:canRelatedGrid,
        converterId: `ObsCore-${table.tbl_id}`};


    const propTypeCol= getObsCoreProdTypeCol(table);
    if (propTypeCol?.enumVals) {
        const pTypes= propTypeCol.enumVals.split(',');
        if (pTypes.every( (s) => s.toLowerCase()==='image' || s.toLowerCase()==='cube')) {
            return {...baseRetOb, canGrid:true,maxPlots:DEF_MAX_PLOTS};
        }
    }

    if (table?.request?.filters) {
        const fList= table.request.filters.split(';');
        const pTFilter= fList.find( (f) => f.includes(propTypeCol.name) && f.includes('IN'));
        if (pTFilter) {
            const inList=  pTFilter.substring( pTFilter.indexOf('(')+1, pTFilter.indexOf(')')).split(',');
            if (inList.every( (s) => s.toLocaleLowerCase()==='\'image\'' || s.toLocaleLowerCase()==='\'cube\'')) {
                return {...baseRetOb, canGrid:true,maxPlots:DEF_MAX_PLOTS};
            }
        }
    }
    return baseRetOb;
}

function describeObsThreeColor(table, row, options) {
    const {dataSource:dlTableUrl,prodType,isVoTable,isDataLinkRow, isPng}= getObsCoreRowMetaInfo(table,row);
    const errMsg= doErrorChecks(table,row,prodType,dlTableUrl,isDataLinkRow,isVoTable);
    if (errMsg || prodType!=='image' || isPng || !isDataLinkRow) return;
    return datalinkDescribeThreeColor(dlTableUrl, table,row, options);
}

/**
 *
 * @param table
 * @param plotRows
 * @param activateParams
 * @param {DataProductsFactoryOptions} options
 * @return {Promise.<DataProductsDisplayType>}
 */
export function getObsCoreGridDataProduct(table, plotRows, activateParams, options) {
        const pAry= plotRows.map( (pR) => getObsCoreSingleDataProduct(
            { table ,row:pR.row,activateParams,doFileAnalysis:false,options} ));
    return createGridResult(pAry,activateParams,table,plotRows);
}


/**
 * return a promise
 * @param table
 * @param row
 * @param threeColorOps
 * @param highlightPlotId
 * @param activateParams
 * @param {DataProductsFactoryOptions} options
 * @return {Promise<DataProductsDisplayType>}
 */
export async function getObsCoreRelatedDataProduct(table, row, threeColorOps, highlightPlotId, activateParams, options) {

    const canGrid= options?.allowImageRelatedGrid ?? false;
    if (!canGrid) return Promise.reject('related data products not supported');
    const {titleStr,dataSource,prodType,isVoTable,isDataLinkRow, isPng}= getObsCoreRowMetaInfo(table,row);
    const errMsg= doErrorChecks(table,row,prodType,dataSource,isDataLinkRow,isVoTable);
    if (errMsg) return errMsg;
    if (prodType!=='image') return dpdtSimpleMsg(`${prodType} is not supported for grid`);
    if (isPng) return dpdtSimpleMsg(`${prodType} must be fits for related grid support`);
    if (!isDataLinkRow) return dpdtSimpleMsg('datalink required for supported for related grid');

    return getDatalinkRelatedGridProduct({dlTableUrl:dataSource, activateParams,table,row,threeColorOps, titleStr,options});
}




export function getObsCoreDataProduct(table, row, activateParams, options) {
    const descriptorsInFile= getServiceDescriptors(table);
    const descriptors= descriptorsInFile && descriptorsInFile?.filter( (dDesc) => !isDataLinkServiceDesc(dDesc));
    const dlDescriptors= descriptorsInFile && descriptorsInFile?.filter( (dDesc) => isDataLinkServiceDesc(dDesc));


    if (isEmpty(descriptors)) {
        return getObsCoreSingleDataProduct({table, row, activateParams, options});
    }

    const positionWP= makeWorldPtUsingCenterColumns(table,row);
    const activeMenuLookupKey= `${descriptors[0].accessURL}--${table.tbl_id}--${row}`;
    let serviceDescMenuList= createServDescMenuRet({descriptors,positionWP,table,row,activateParams,
        activeMenuLookupKey, options});

    serviceDescMenuList= serviceDescMenuList.map( (m) =>
        ({...m,
            semantics: m.semantics ?? '#additional-service-descriptor',
            size: m.size ?? 0,
        }));

    return getObsCoreSingleDataProduct({table, row, activateParams, serviceDescMenuList, dlDescriptors, options});
}


function doErrorChecks(table, row, prodType, dataSource, isDataLink, isVoTable) {
    if (!dataSource) return dpdtSimpleMsg(`${prodType} is not supported`);
    if (isDataLink && !isVoTable) return dpdtSimpleMsg(`${prodType} is not supported`);
    if (!hasRowAccess(table, row)) {
        const rDateStr= getObsReleaseDate(table,row);
        const msg= rDateStr ?
            `Proprietary data: the data publisher states it will become available on ${rDateStr}` :
            'Proprietary data; the data publisher has not provided a release date yet';
        return dpdtSimpleMsg(msg);
    }
    return undefined;
}


/**
 * Support ObsCore single product
 * @param {Object} obj
 * @param obj.table
 * @param obj.row
 * @param {ActivateParams} obj.activateParams
 * @param {Array.<DataProductsDisplayType>} [obj.serviceDescMenuList]
 * @param {ServiceDescriptorDef} [obj.dlDescriptors]
 * @param {boolean} [obj.doFileAnalysis] - if true the build a menu if possible
 * @param {DataProductsFactoryOptions} obj.options
 * @return {Promise.<DataProductsDisplayType>}
 */
async function getObsCoreSingleDataProduct({table, row, activateParams, serviceDescMenuList, dlDescriptors, doFileAnalysis= true, options}) {

    const {size,titleStr,dataSource,prodType,isVoTable,isDataLinkRow, isPng}= getObsCoreRowMetaInfo(table,row);
    const errMsg= doErrorChecks(table,row,prodType,dataSource,isDataLinkRow,isVoTable);
    if (errMsg) return errMsg;

    if (isDataLinkRow) {
        return getDatalinkSingleDataProduct({dlTableUrl:dataSource, options, sourceTable:table, row,
            activateParams,titleStr, additionalServiceDescMenuList:serviceDescMenuList, doFileAnalysis});
    }
    else if (isPng) {
        return dpdtPNG('PNG image',dataSource);
    }
    else if (size>GIG) {
        return dpdtMessageWithDownload('Data is too large to load', 'Download File: '+titleStr, dataSource);
    }
    else { // this row has a data product, may have service descriptors, and may have a datalink service descriptor
        if (dlDescriptors?.length) { // get the DL rows and add them to the menu
            const dlTableUrl= makeDlUrl(dlDescriptors[0],table, row); // only support one DL service descriptor for now
            const dlProd= await getDatalinkSingleDataProduct({
                dlTableUrl, options, sourceTable:table, row,
                activateParams,titleStr, undefined, doFileAnalysis});
            const dlProdMenu= dlProd?.menu?.map( (p) =>  ({...p,menuKey:'extraDl-'+p.menuKey})) ?? [];
            serviceDescMenuList= serviceDescMenuList ? [...(serviceDescMenuList??[]),...dlProdMenu] : dlProdMenu;
        }


        const positionWP= getSearchTarget(table.request,table) ?? makeWorldPtUsingCenterColumns(table,row);
        const request= makeObsCoreRequest(dataSource, positionWP, titleStr,table,row);
        const primDPType= doFileAnalysis ?
            await uploadAndAnalyze({request,table,row,activateParams,serviceDescMenuList}) :
            createGuessDataType(titleStr,'guess-0',dataSource,prodType,undefined,activateParams, positionWP,table,row,size);
        return makeSingleDataProductWithMenu(activateParams.dpId, primDPType,size, serviceDescMenuList);
    }
}




export function makeSingleDataProductWithMenu(dpId, primDPType, size, serviceDescMenuList) {
    if (!serviceDescMenuList) return primDPType;
    const activeMenuLookupKey= serviceDescMenuList[0].activeMenuLookupKey;
    primDPType= { ...primDPType, semantics: primDPType.semantics ?? '#this', size, activeMenuLookupKey};
    const menu= [primDPType, ...serviceDescMenuList];
    dispatchUpdateActiveKey({dpId, activeMenuKeyChanges:{[activeMenuLookupKey]:menu[0].menuKey}});
    return dpdtFromMenu(menu,0,activeMenuLookupKey,true);
}



export function getObsCoreRowMetaInfo(table,row) {
    if (!table || row<0) return {};
    const titleStr= createObsCoreTitle(table,row);
    const dataSource= getObsCoreAccessURL(table,row);
    const prodType= getProdTypeGuess(table,row);
    const isVoTable= isFormatVoTable(table, row);
    const isDataLinkRow= isFormatDataLink(table,row);
    const iName= getCellValue(table,row,'instrument_name') || '';
    const obsId= getCellValue(table,row,'obs_id') || '';
    const size= Number(getCellValue(table,row,'access_estsize')) || 0;

    return {iName,obsId,size,titleStr,dataSource,prodType,isVoTable,isDataLinkRow,isPng:isFormatPng(table,row)};
}

function createObsCoreTitle(table,row) {
 // 1. try a template
    const template= getAppOptions().tapObsCore?.productTitleTemplate;
    if (template?.trim()==='') return ''; // setting template to empty string disables all title guessing
    if (!template) {
        const templateColNames= template && getColNameFromTemplate(template);
        const columns= getColumns(table);
        if (templateColNames?.length && columns?.length) {
            const cNames= columns.map( ({name}) => name);
            const colObj= templateColNames.reduce((obj, v) => {
                if (cNames.includes(v)) {
                    obj[v]= getCellValue(table,row,v);
                }
                return obj;
            },{});
            if (Object.keys(colObj).length===templateColNames.length) {
                const titleStr= tokenSub(colObj,template);
                if (titleStr) return titleStr;
            }
        }
    }
 // 2. try obs_title
    if (getObsTitle(table,row)) return getObsTitle(table,row);

 // 3. compute a name
    let obsCollect= getCellValue(table,row,'obs_collection') || '';
    const obsId= getCellValue(table,row,'obs_id') || '';
    const iName= getCellValue(table,row,'instrument_name') || '';
    if (obsCollect===iName) obsCollect= '';
    return `${obsCollect?obsCollect+', ':''}${iName?iName+', ':''}${obsId}`;
}

function getColNameFromTemplate(template) {
    return template.match(/\${[\w -.]+}/g)?.map( (s) => s.substring(2,s.length-1));
}


