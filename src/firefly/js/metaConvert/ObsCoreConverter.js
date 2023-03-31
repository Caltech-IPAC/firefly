import {get, isEmpty, uniqueId} from 'lodash';
import {getAppOptions} from '../core/AppDataCntlr.js';
import {sprintf} from '../externalSource/sprintf.js';
import {tokenSub} from '../util/WebUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {RangeValues, SIGMA, STRETCH_LINEAR} from '../visualize/RangeValues.js';
import {WebPlotRequest, TitleOptions} from '../visualize/WebPlotRequest.js';
import {getCellValue, doFetchTable, hasRowAccess, getColumn, getColumns, getMetaEntry} from '../tables/TableUtil.js';
import {
    getObsCoreAccessURL,
    getObsCoreProdType,
    getServiceDescriptors,
    isDataLinkServiceDesc, isFormatPng
} from '../util/VOAnalyzer.js';
import {
    getObsCoreProdTypeCol,
    isFormatDataLink,
    isFormatVoTable,
    makeWorldPtUsingCenterColumns
} from '../util/VOAnalyzer';
import {makeFileRequest} from '../tables/TableRequestUtil';
import {ZoomType} from '../visualize/ZoomType.js';
import {createGridImagesActivate, createSingleImageExtraction} from './ImageDataProductsUtil.js';
import {makeAnalysisGetSingleDataProduct} from './MultiProductFileAnalyzer';
import {dpdtFromMenu, dpdtImage, dpdtMessage, dpdtMessageWithDownload, dpdtPNG, DPtypes,} from './DataProductsType';
import {createGuessDataType, processDatalinkTable} from './DataLinkProcessor';
import {createServDescMenuRet} from './ServDescConverter.js';
import {dispatchUpdateActiveKey} from './DataProductsCntlr.js';

const GIG= 1048576 * 1024;

/**
 *
 * @param {TableModel} table
 * @param {DataProductsConvertType} converterTemplate
 * @return {DataProductsConvertType}
 */
export function makeObsCoreConverter(table,converterTemplate) {
    if (!table) return converterTemplate;
    const baseRetOb= {...converterTemplate,
        canGrid:false, maxPlots:1, hasRelatedBands:false, converterId: `ObsCore-${table.tbl_id}`};

    const propTypeCol= getObsCoreProdTypeCol(table);
    if (propTypeCol.enumVals) {
        const pTypes= propTypeCol.enumVals.split(',');
        if (pTypes.every( (s) => s.toLocaleLowerCase()==='image' || s.toLocaleLowerCase()==='cube')) {
            return {...baseRetOb, canGrid:true,maxPlots:8};
        }
    }

    if (get(table, 'request.filters')) {
        const fList= table.request.filters.split(';');
        const pTFilter= fList.find( (f) => f.includes(propTypeCol.name) && f.includes('IN'));
        if (pTFilter) {
            const inList=  pTFilter.substring( pTFilter.indexOf('(')+1, pTFilter.indexOf(')')).split(',');
            if (inList.every( (s) => s.toLocaleLowerCase()==='\'image\'' || s.toLocaleLowerCase()==='\'cube\'')) {
                return {...baseRetOb, canGrid:true,maxPlots:8};
            }
        }
    }

    return baseRetOb;
}


/**
 *
 * @param table
 * @param plotRows
 * @param activateParams
 * @return {Promise.<DataProductsDisplayType>}
 */
export function getObsCoreGridDataProduct(table, plotRows, activateParams) {
    const pAry= plotRows.map( (pR) => getObsCoreSingleDataProduct(table,pR.row,activateParams,undefined,false));
    return Promise.all(pAry).then ( (resultAry) => {
        const {imageViewerId}= activateParams;
        const requestAry= resultAry
            .filter( (result) => result && result.request && (
                result.displayType===DPtypes.IMAGE ||
                result.displayType===DPtypes.PROMISE ||
                result.displayType===DPtypes.ANALYZE) )
            .map( (result) => result.request);
        const activate= createGridImagesActivate(requestAry,imageViewerId, table.tbl_id, plotRows);
        const extraction= createSingleImageExtraction(requestAry);
        return dpdtImage('image grid', activate,extraction, 'image-grid-0',{requestAry, extractionText:'Pin Image'});
    });
}



export function getObsCoreDataProduct(table, row, activateParams, doFileAnalysis= true) {
    let descriptors= getServiceDescriptors(table);
    descriptors= descriptors && descriptors.filter( (dDesc) => !isDataLinkServiceDesc(dDesc));

    if (isEmpty(descriptors)) {
        return getObsCoreSingleDataProduct(table, row, activateParams, undefined, doFileAnalysis);
    }

    const positionWP= makeWorldPtUsingCenterColumns(table,row);
    const activeMenuLookupKey= `${descriptors[0].accessURL}--${table.tbl_id}--${row}`;
    let serDescMenu= createServDescMenuRet(descriptors,positionWP,table,row,activateParams,activeMenuLookupKey);

    serDescMenu= serDescMenu.map( (m) =>
        ({...m,
            semantics: m.semantics ?? '#additional-service-descriptor',
            size: m.size ?? 0,
        }));

    return getObsCoreSingleDataProduct(table, row, activateParams, serDescMenu, doFileAnalysis);
}



/**
 * Support ObsCore single product
 * @param table
 * @param row
 * @param {ActivateParams} activateParams
 * @param {Array.<DataProductsDisplayType>} serviceDescMenuList
 * @param {boolean} doFileAnalysis - if true the build a menu if possible
 * @return {Promise.<DataProductsDisplayType>}
 */
export async function getObsCoreSingleDataProduct(table, row, activateParams, serviceDescMenuList, doFileAnalysis= true) {

    const {size,titleStr,dataSource,prodType,isVoTable,isDataLink, isPng}= getObsCoreRowMetaInfo(table,row);

    if (!dataSource) return dpdtMessage(`${prodType} is not supported`);
    if (isDataLink && !isVoTable) dpdtMessage(`${prodType} is not supported`);

    if (!hasRowAccess(table, row)) return dpdtMessage('You do not have access to these data.');
    const positionWP= makeWorldPtUsingCenterColumns(table,row);

    if (isDataLink) {
        try {
            const datalinkTable= await doFetchTable(makeFileRequest('dl table', dataSource));
            return processDatalinkTable(table,row,datalinkTable,positionWP,activateParams, serviceDescMenuList, doFileAnalysis);
        } catch (reason) {
                                  //todo - what about if when the data link fetch fails but there is a serviceDescMenuList - what to do? does it matter?
            dpdtMessageWithDownload(`No data to display: Could not retrieve datalink data, ${reason}`, 'Download File: '+titleStr, dataSource);
        }
    }
    else if (isPng) {
        return dpdtPNG('PNG image',dataSource);
    }
    else {
        if (size>GIG) return dpdtMessageWithDownload('Data is too large to load', 'Download File: '+titleStr, dataSource);
        if (doFileAnalysis) {
            const analyzerFunc= makeAnalysisGetSingleDataProduct(() => makeObsCoreRequest(dataSource, positionWP, titleStr,table,row));
            const result= await analyzerFunc(table,row,activateParams,prodType);
            return singleResultWithServiceDesc(activateParams.dpId, result, size, serviceDescMenuList);
        }
        else {
            const result= createGuessDataType(titleStr,'guess-0',dataSource,prodType,undefined,activateParams, positionWP,table,row,size);
            return singleResultWithServiceDesc(activateParams.dpId, result,size, serviceDescMenuList);
        }
    }
}

function singleResultWithServiceDesc(dpId, primResult, size, serviceDescMenuList) {
    if (!serviceDescMenuList) return primResult;
    const activeMenuLookupKey= serviceDescMenuList[0].activeMenuLookupKey;
    primResult= { ...primResult, semantics: primResult.semantics ?? '#this', size, activeMenuLookupKey};
    const menu= [primResult, ...serviceDescMenuList];
    dispatchUpdateActiveKey({dpId, activeMenuKeyChanges:{[activeMenuLookupKey]:menu[0].menuKey}});
    return dpdtFromMenu(menu,0,activeMenuLookupKey,true);
}



function getObsCoreRowMetaInfo(table,row) {
    if (!table) return {};
    let titleStr;
    const dataSource= getObsCoreAccessURL(table,row);
    const prodType= (getObsCoreProdType(table,row) || '').toLocaleLowerCase();
    const isVoTable= isFormatVoTable(table, row);
    const isDataLink= isFormatDataLink(table,row);
    const iName= getCellValue(table,row,'instrument_name') || '';
    const obsId= getCellValue(table,row,'obs_id') || '';
    const size= Number(getCellValue(table,row,'access_estsize')) || 0;
    const isPng= isFormatPng(table,row);
    let obsCollect= getCellValue(table,row,'obs_collection') || '';
    let obsTitle= getCellValue(table,row,'obs_title') || '';
    if (obsCollect===iName) obsCollect= '';

    const template= getAppOptions().tapObsCore?.productTitleTemplate;
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
            titleStr= tokenSub(colObj,template);
        }
    }
    if (!titleStr) { // no template, use dynamic
        const defTemplate= '${obs_collection}, ${instrument_name}, ${obs_id}'; // todo: should I use?
        titleStr= obsTitle || `${obsCollect?obsCollect+', ':''}${iName?iName+', ':''}${obsId}`;
    }

    return {iName,obsId,size,titleStr,dataSource,prodType,isVoTable,isDataLink,isPng};
}

function getColNameFromTemplate(template) {
    return template.match(/\${[\w -.]+}/g)?.map( (s) => s.substring(2,s.length-1));
}


/**
 *
 * @param dataSource
 * @param positionWP
 * @param titleStr
 * @param {TableModel} table
 * @param {number} row
 * @return {undefined|WebPlotRequest}
 */
export function makeObsCoreRequest(dataSource, positionWP, titleStr, table, row) {
    if (!dataSource) return undefined;


    const r = WebPlotRequest.makeURLPlotRequest(dataSource, 'DataProduct');
    r.setZoomType(ZoomType.FULL_SCREEN);
    if (titleStr.length>7) {
        r.setTitleOptions(TitleOptions.NONE);
        r.setTitle(titleStr);
    }
    else {
        r.setTitleOptions(TitleOptions.FILE_NAME);
    }
    r.setPlotId(uniqueId('obscore-'));

    const col= getColumn(table,'em_max',true);
    const v= col && Number(getCellValue(table, row, 'em_max'));
    if (col && v) {
        const {units}= col;
        let vToUse;
        if (units==='m' || units==='meters') vToUse= v*100000;
        if (units==='um') vToUse= v;
        if (vToUse) {
            r.setAttributes({
                [PlotAttribute.WAVE_LENGTH_UM]:  sprintf('%.2f', vToUse),
            });
        }
    }
    const bandDesc= getMetaEntry(table,'bandDesc');
    if (bandDesc) r.setAttributes({ [PlotAttribute.WAVE_TYPE]:  bandDesc });

    const coverage= getMetaEntry(table,'coverage');
    if (coverage) r.setAttributes({ [PlotAttribute.PROJ_TYPE_DESC]:  coverage });

    const helpUrl= getMetaEntry(table,'helpUrl');
    if (helpUrl) r.setAttributes({ [PlotAttribute.DATA_HELP_URL]:  helpUrl });



    if (positionWP) r.setOverlayPosition(positionWP);
    r.setInitialRangeValues(RangeValues.make2To10SigmaLinear());

    return r;
}
