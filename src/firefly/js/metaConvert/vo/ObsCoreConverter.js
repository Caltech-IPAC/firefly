import {isEmpty, isUndefined} from 'lodash';
import { getCellValue, getMetaEntry, hasRowAccess } from '../../tables/TableUtil.js';
import {logger} from '../../util/Logger';
import {
    getObsCoreAccessURL, getObsCoreCloudAccess, getObsReleaseDate, getProdTypeGuess, getSearchTarget, isFormatDataLink,
    isFormatPng, isFormatVoTable, makeWorldPtUsingCenterColumns, obsCoreTableHasOnlyImages
} from '../../voAnalyzer/TableAnalysis.js';
import {getServiceDescriptors, isDataLinkServiceDesc} from '../../voAnalyzer/VoDataLinkServDef.js';
import {makePngEntry, uploadAndAnalyze} from '../AnalysisUtils.js';
import {GROUP_BY_DATALINK_RESULT, GROUP_BY_RELATED_COLUMNS, IMAGE_ONLY} from '../DataProductConst';
import {dispatchUpdateActiveKey} from '../DataProductsCntlr.js';
import { dpdtFromMenu, dpdtMessageWithDownload, dpdtSimpleMsg, } from '../DataProductsType.js';
import {makeDataTooLargeDLMessage} from '../VoUITitles';
import {createGuessDataType} from './DataLinkProcessor.js';
import {
    createGridResult, datalinkDescribeThreeColor, getDatalinkRelatedImageGridProduct,
    getDatalinkSingleDataProduct, getObsCoreRelatedDataProductByFilter, makeDlUrl
} from './DatalinkProducts.js';

import {createServDescMenuRet} from './ServDescProducts.js';
import {makeObsCoreRequest} from './VORequest.js';

const GIG= 1048576 * 1024;
export const OBSCORE_DEF_MAX_PLOTS= 8;


/**
 *
 * @param {TableModel} table
 * @param {DataProductsConvertType} converterTemplate
 * @param {DataProductsFactoryOptions} options
 * @return {DataProductsConvertType}
 */
export function makeObsCoreConverter(table,converterTemplate,options={}) {
    if (!table) return converterTemplate;

    const {maxPlots, initialLayout='single', relatedGridImageOrder, threeColor, canGrid, hasRelatedBands}= converterTemplate;
    const hasRelatedBandsConfirmed= hasRelatedBands && confirmHasRelatedBands(table,options);
    const canGridConfirmed= hasRelatedBands || (canGrid && ensurePrimaryProductOnlyImages(table,options));
    const threeColorConfirmed= isUndefined(threeColor) ? hasRelatedBands : threeColor;
    return {
        ...converterTemplate,
        initialLayout,
        describeThreeColor: threeColor ? describeObsThreeColor : undefined,
        threeColor:threeColorConfirmed,
        canGrid:canGridConfirmed,
        hasRelatedBands:hasRelatedBandsConfirmed,
        maxPlots: canGrid ? maxPlots : 1,
        converterId: `ObsCore-${table.tbl_id}`,
        relatedGridImageOrder,
    };
}

/**
 * returning true means that we think that the primary product is an image, in the datalink case there may be other
 * products that are not
 * @param table
 * @param options
 * @return {boolean}
 */
function ensurePrimaryProductOnlyImages(table, options) {
    const {guaranteeOnlyImages=false, limitViewerDisplay}= options;
    return guaranteeOnlyImages || obsCoreTableHasOnlyImages(table) || limitViewerDisplay===IMAGE_ONLY;
}

function confirmHasRelatedBands(table,options) {
    const {relatedBandMethod=GROUP_BY_DATALINK_RESULT}= options;
    const {prodType,dataSource}= getObsCoreRowMetaInfo(table,table.highlightedRow);
    const anyError= Boolean(doErrorChecks(table,table.highlightedRow,prodType,dataSource));
    const methodSet= relatedBandMethod===GROUP_BY_DATALINK_RESULT || relatedBandMethod===GROUP_BY_RELATED_COLUMNS;
    return methodSet && prodType==='image' && ensurePrimaryProductOnlyImages(table,options) && !anyError;
}

function describeObsThreeColor(table, row, options) {
    const {dataSource:dlTableUrl,prodType,isDataLinkRow, isPng}= getObsCoreRowMetaInfo(table,row);
    const errMsg= doErrorChecks(table,row,prodType,dlTableUrl);
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
            { table ,row:pR.row,activateParams,doFileAnalysis:false,options, useForTableGrid:true} ));
    return createGridResult(pAry,activateParams,table,plotRows,options);
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

    const {hasRelatedBands:canGrid=false, relatedBandMethod=GROUP_BY_DATALINK_RESULT }= options ?? {};
    if (!canGrid) return Promise.reject('related data products not supported');
    if (relatedBandMethod!==GROUP_BY_DATALINK_RESULT && relatedBandMethod!==GROUP_BY_RELATED_COLUMNS) {
        return dpdtSimpleMsg(`related data products not supported (related band method no supported: ${relatedBandMethod})`);
    }
    const {dataSource,prodType,isDataLinkRow, isPng}= getObsCoreRowMetaInfo(table,row);
    const errMsg= doErrorChecks(table,row,prodType,dataSource);
    if (errMsg) return errMsg;
    if (prodType!=='image') return dpdtSimpleMsg(`${prodType} is not supported for grid`);
    if (isPng) return dpdtSimpleMsg(`${prodType} must be fits for related grid support`);
    if (!isDataLinkRow) return dpdtSimpleMsg('datalink required for supported for related grid');

    if (relatedBandMethod===GROUP_BY_DATALINK_RESULT) {
        return getDatalinkRelatedImageGridProduct({dlTableUrl:dataSource, activateParams,table,row,threeColorOps, options});
    }
    else {
        const s= getMetaEntry(table,'tbl.relatedCols');
        if (!s) dpdtSimpleMsg('meta data tbl.relatedCols is not configured');
        return getObsCoreRelatedDataProductByFilter(table, row, threeColorOps, highlightPlotId, activateParams, options);
    }
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

function doErrorChecks(table, row, prodType, dataSource) {
    if (!dataSource) return dpdtSimpleMsg(`${prodType} is not supported`);
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
 * @param {boolean} [obj.useForTableGrid] - this result is part of a table grid Result
 * @param {DataProductsFactoryOptions} obj.options
 * @return {Promise.<DataProductsDisplayType>}
 */
async function getObsCoreSingleDataProduct({table, row, activateParams, serviceDescMenuList, dlDescriptors,
                                               doFileAnalysis= true, options, useForTableGrid=false}) {

    const {size,dataSource,prodType,isDataLinkRow,cloudAccess,isPng}= getObsCoreRowMetaInfo(table,row);
    const errMsg= doErrorChecks(table,row,prodType,dataSource);
    if (errMsg) return errMsg;

    if (isDataLinkRow) {
        return getDatalinkSingleDataProduct({dlTableUrl:dataSource, options, sourceTable:table, row,
            activateParams, additionalServiceDescMenuList:serviceDescMenuList, doFileAnalysis, useForTableGrid});
    }
    else if (isPng) {
        return makePngEntry(dataSource);
    }
    else if (size>GIG) {
        const {message,titleStr}=  makeDataTooLargeDLMessage(table,row);
        return dpdtMessageWithDownload(message, titleStr, dataSource);
    }
    else { // this row has a data product, may have service descriptors, and may have a datalink service descriptor
        if (dlDescriptors?.length) { // get the DL rows and add them to the menu
            const dlTableUrl= makeDlUrl(dlDescriptors[0],table, row); // only support one DL service descriptor for now
            const dlProd= await getDatalinkSingleDataProduct({
                dlTableUrl, options, sourceTable:table, row,
                activateParams,doFileAnalysis});
            const dlProdMenu= dlProd?.menu?.map( (p) =>  ({...p,menuKey:'extraDl-'+p.menuKey})) ?? [];
            serviceDescMenuList= serviceDescMenuList ? [...(serviceDescMenuList??[]),...dlProdMenu] : dlProdMenu;
        }


        const positionWP= getSearchTarget(table.request,table) ?? makeWorldPtUsingCenterColumns(table,row);
        const request= makeObsCoreRequest({url:dataSource, cloudAccess, positionWP, table,row, expectStaticFile:true});
        const primDPType= doFileAnalysis ?
            await uploadAndAnalyze({request,table,row,activateParams,serviceDescMenuList,originalTitle:request.getTitle()}) :
            createGuessDataType({url:dataSource,cloudAccess, ct:prodType,activateParams, positionWP,table,row,size});
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
    const dataSource= getObsCoreAccessURL(table,row);
    const cloudAccess= getObsCoreCloudAccess(table,row);
    const prodType= getProdTypeGuess(table,row);
    const isVoTable= isFormatVoTable(table, row);
    const isDataLinkRow= isFormatDataLink(table,row);
    const iName= getCellValue(table,row,'instrument_name') || '';
    const obsId= getCellValue(table,row,'obs_id') || '';
    const size= Number(getCellValue(table,row,'access_estsize')) || 0;
    return {iName,obsId,size,dataSource,cloudAccess,prodType,isVoTable,isDataLinkRow,isPng:isFormatPng(table,row)};
}

function relatedBandWarning() {
    logger.warn('ObsCoreConverter: Warning: unable to show related bands for this table');
    logger.warn('ObsCoreConverter: hasRelatedBands is set to true, this table must have only images or options.limitViewerDisplay must be IMAGE_ONLY');
}

