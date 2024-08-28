import {
    getObsCoreProdType, getObsCoreSRegion, getSearchTarget, makeWorldPtUsingCenterColumns
} from '../../voAnalyzer/TableAnalysis.js';
import {
    getDataLinkData, isDownloadType, isGzipType, isSimpleImageType, isTarType, isVoTable
} from '../../voAnalyzer/VoDataLinkServDef.js';
import {GIG} from '../../util/WebUtil.js';
import {makeAnalysisActivateFunc} from '../AnalysisUtils.js';
import {dispatchUpdateActiveKey, getActiveMenuKey, getCurrentActiveKeyID} from '../DataProductsCntlr.js';
import {
    dpdtAnalyze, dpdtChartTable, dpdtDownload, dpdtDownloadMenuItem, dpdtFromMenu, dpdtImage, dpdtMessage,
    dpdtMessageWithError, dpdtPNG,
    dpdtTable, DPtypes
} from '../DataProductsType.js';
import {createSingleImageActivate, createSingleImageExtraction} from '../ImageDataProductsUtil.js';
import {
    createChartTableActivate, createTableActivate, createTableExtraction, makeMultiTableActivate,
    makeMultiTableExtraction
} from '../TableDataProductUtils.js';
import {makeServiceDefDataProduct} from './ServDescProducts.js';
import {makeObsCoreRequest} from './VORequest.js';

export const USE_ALL= 'useAllAlgorithm';
export const RELATED_IMAGE_GRID= 'relatedImageGridAlgorithm';
export const IMAGE= 'imageAlgorithm';
export const SPECTRUM= 'spectrumAlgorithm';


/**
 *
 * @param {Object} params
 * @param {TableModel} params.sourceTable
 * @param {number} params.row
 * @param {TableModel} params.datalinkTable
 * @param {ActivateParams} params.activateParams
 * @param {Array.<DataProductsDisplayType>} [params.additionalServiceDescMenuList]
 * @param {string} params.dlTableUrl datalink url - url of the datalink Table
 * @param {boolean} [params.doFileAnalysis]
 * @param {String} [params.parsingAlgorithm] - which type of DL data
 * @param {DataProductsFactoryOptions} [params.options] - which type of DL data
 * @param {string} [params.baseTitle]
 * @return {DataProductsDisplayType}
 */
export function processDatalinkTable({sourceTable, row, datalinkTable, activateParams, baseTitle=undefined,
                                     additionalServiceDescMenuList, dlTableUrl, doFileAnalysis=true,
                                         options, parsingAlgorithm = USE_ALL}) {
    const dataLinkData= getDataLinkData(datalinkTable);
    const isImageGrid= options.allowImageRelatedGrid &&  dataLinkData.filter( (dl) => dl.dlAnalysis.isImage && dl.dlAnalysis.isGrid).length>1;
    const isMultiTableSpectrum= dataLinkData.filter( (dl) => dl.dlAnalysis.isThis && dl.dlAnalysis.isGrid && dl.dlAnalysis.isSpectrum).length>1;
    if (parsingAlgorithm===USE_ALL && isMultiTableSpectrum) parsingAlgorithm= SPECTRUM; // todo this is probably temporary for testing

    const menu=  dataLinkData.length &&
        createDataLinkMenuRet({dlTableUrl,dataLinkData,sourceTable, sourceRow:row, activateParams, baseTitle,
            additionalServiceDescMenuList, doFileAnalysis, parsingAlgorithm, options});

    const canShow= menu.length>0 && menu.some( (m) => m.displayType!==DPtypes.DOWNLOAD && (!m.size || m.size<GIG));
    const activeMenuLookupKey= dlTableUrl;


    if (canShow) {
        let index= -1;
        const {dpId}= activateParams;
        const activeMenuKey= getActiveMenuKey(dpId, dlTableUrl);
        if (isImageGrid) {
            const lastSource= getCurrentActiveKeyID(dpId);
            const lastKey= getActiveMenuKey(dpId, lastSource);
            index= menu.findIndex( (m) => m.menuKey===lastKey);
        }
        if (index<0) index= menu.findIndex( (m) => m.menuKey===activeMenuKey);
        if (index<0) index= 0;
        dispatchUpdateActiveKey({dpId, activeMenuKeyChanges:{[activeMenuLookupKey]:menu[index].menuKey}});
        return dpdtFromMenu(menu,index,dlTableUrl);
    }

    if (menu.length>0) {
        const dMenu= menu.length && convertAllToDownload(menu);

        const msgMenu= [
            ...dMenu,
            dpdtTable('Show Datalink VO Table for list of products',
                createTableActivate(dlTableUrl,'Datalink VO Table', activateParams),
                createTableExtraction(dlTableUrl,'Datalink VO Table'),
                'nd0-showtable', {url:dlTableUrl}),
            dpdtDownload ( 'Download Datalink VO Table for list of products', dlTableUrl, 'nd1-downloadtable', 'vo-table' ),
        ];
        const msg= dMenu.length?
            'You may only download data for this row - nothing to display':
            'No displayable data available for this row';
        return dpdtMessage( msg, msgMenu, {activeMenuLookupKey,singleDownload:true});
    }
    else {
        return dpdtMessage('No data available for this row',undefined,{activeMenuLookupKey});
    }

}


function addDataLinkEntries(dlTableUrl,activateParams) {
    return [
        dpdtTable('Show Datalink VO Table for list of products',
            createTableActivate(dlTableUrl,'Datalink VO Table', activateParams),
            createTableExtraction(dlTableUrl,'Datalink VO Table'),
            'datalink-entry-showtable', {url:dlTableUrl}),
        dpdtDownload ( 'Download Datalink VO Table for list of products', dlTableUrl, 'datalink-entry-downloadtable', 'vo-table' )
    ];

}



function convertAllToDownload(menu) {
    return menu.map( (d) =>  {
        if (d.displayType===DPtypes.DOWNLOAD) return {...d};
        if (d.url) return {...d, displayType:DPtypes.DOWNLOAD};
        if (d.request && d.request.getURL && d.request.getURL()) {
            return {...d,displayType:DPtypes.DOWNLOAD, url:d.request.getURL()};
        }
        else {
            return {};
        }
    }).filter( (d) => d.displayType);
}

function getDLMenuEntryData({dlTableUrl, dlData,idx, sourceTable, sourceRow}) {
    const positionWP= getSearchTarget(sourceTable?.request,sourceTable) ?? makeWorldPtUsingCenterColumns(sourceTable,sourceRow);
    const sRegion= getObsCoreSRegion(sourceTable,sourceRow);
    const prodType= getObsCoreProdType(sourceTable,sourceRow);
    const contentType= dlData.contentType.toLowerCase();
    return {positionWP,contentType, sRegion,prodType, activeMenuLookupKey:dlTableUrl??`no-table-${idx}`,menuKey:'dlt-'+idx};
}

function makeDLServerDefMenuEntry({dlTableUrl, dlData,idx, baseTitle, sourceTable, sourceRow, options,
                        name, activateParams}) {
    const {serDef, semantics,size,serviceDefRef,dlAnalysis}= dlData;
    const {positionWP,sRegion,prodType,
        activeMenuLookupKey,menuKey}= getDLMenuEntryData({dlTableUrl, dlData,idx,sourceTable,sourceRow});

    const {title:servDescTitle=''}= serDef;
    const titleStr= baseTitle ? `${baseTitle} (${dlData.description||servDescTitle})` : (dlData.description||servDescTitle);

    return makeServiceDefDataProduct({
        serDef, sourceTable, sourceRow, idx, positionWP, activateParams, options, name,
                                               titleStr, activeMenuLookupKey, menuKey,
        datalinkExtra: {semantics, size, sRegion, prodTypeHint: dlData.contentType || prodType, serviceDefRef, dlAnalysis}
    });
}

/**
 *
 * @param {Object} p
 * @param {String} p.dlTableUrl
 * @param {DatalinkData} p.dlData
 * @param {number} p.idx
 * @param {TableModel} p.sourceTable
 * @param p.sourceRow
 * @param {DataProductsFactoryOptions} p.options
 * @param p.doFileAnalysis
 * @param p.name
 * @param {ActivateParams} p.activateParams
 * @return {DataProductsDisplayType|{displayType: string, menuKey: string, name: *, singleDownload: boolean, url: *, fileType: *}}
 */
function makeDLAccessUrlMenuEntry({dlTableUrl, dlData,idx, sourceTable, sourceRow, options,
                                      doFileAnalysis, name, activateParams}) {

    const {semantics,size,url, dlAnalysis:{isThis, isDownloadOnly, isTar, isGzip,isSimpleImage}, description }= dlData;
    const {positionWP,sRegion,prodType, activeMenuLookupKey,menuKey, contentType}=
        getDLMenuEntryData({dlTableUrl, dlData,idx,sourceTable,sourceRow});

    if (isDownloadOnly) {
        let fileType;
        if (isTar) fileType= 'tar';
        if (isGzip) fileType= 'gzip';
        return isThis ?
            dpdtDownloadMenuItem('Download file: '+name,url,menuKey,fileType,{semantics, size, activeMenuLookupKey}) :
            dpdtDownload('Download file: '+name,url,menuKey,fileType,{semantics, size, activeMenuLookupKey});
    }
    else if (isSimpleImage) {
        return dpdtPNG('Show PNG image: '+name,url,menuKey,{semantics, size, activeMenuLookupKey});
    }
    else if (isTooBig(size)) {
        return dpdtDownload('Download: '+name + '(too large to show)',url,menuKey,'fits',{semantics, size, activeMenuLookupKey});
    }
    else if (dlData.dlAnalysis.isSpectrum && isVoTable(contentType)) {
        const tbl_id= getTableId(dlData.description,options,idx);
        const chartId= getChartId(dlData.description,options,idx);
        const activate= createChartTableActivate({
            chartAndTable:true,
            source: url,
            titleInfo:{titleStr:description, showChartTitle:true},
            activateParams,
            tbl_id,
            chartInfo:{useChartChooser:true},
            chartId,
        });
        const extract= createTableExtraction(url,description,0);
        return dpdtChartTable('Show: ' + description, activate, extract, menuKey, {extractionText: 'Pin Table', paIdx:0, tbl_id,chartId});
    }
    else if (isAnalysisType(contentType)) {
        if (doFileAnalysis) {
            const dataTypeHint= dlData.dlAnalysis.isSpectrum ? 'spectrum' : prodType;
            const prodTypeHint= dlData.dlAnalysis.isSpectrum ? 'spectrum' : (dlData.contentType || prodType);
            const request= makeObsCoreRequest(url,positionWP,name,sourceTable,sourceRow);
            const activate= makeAnalysisActivateFunc({table:sourceTable,row:sourceRow, request,
                activateParams,menuKey, dataTypeHint, options});
            return dpdtAnalyze({name:'Show: '+name,
                activate,url,menuKey, semantics, size, activeMenuLookupKey,request, sRegion, prodTypeHint});
        }
        else {
            return createGuessDataType(name,menuKey,url,contentType,semantics, activateParams, positionWP,sourceTable,sourceRow,size);
        }
    }
}


const getTableId= (description, options, idx) =>
   options?.tableIdList?.find( (e) => e.description===description)?.tbl_id ??
       (options.tableIdBase??'direct-result-tbl') + `-${idx}`;

const getChartId=  (description, options, idx) =>
    options?.chartIdList?.find( (e) => e.description===description)?.chartId ??
    (options.chartIdBase??'direct-result-chart') +`-${idx}`;


/**
 *
 * @param {Object} p
 * @param {String} p.dlTableUrl
 * @param {DatalinkData} p.dlData
 * @param {number} p.idx
 * @param {string} p.baseTitle
 * @param {TableModel} p.sourceTable
 * @param {number} p.sourceRow
 * @param {DataProductsFactoryOptions} p.options
 * @param {string} p.name
 * @param {boolean} p.doFileAnalysis
 * @param p.activateParams
 * @return {Object}
 */
function makeMenuEntry({dlTableUrl, dlData,idx, baseTitle, sourceTable, sourceRow, options,
                        name, doFileAnalysis, activateParams}) {

    if (dlData.serDef) {
        return makeDLServerDefMenuEntry({dlTableUrl, dlData,idx, baseTitle, sourceTable, sourceRow, options,
                                name, doFileAnalysis, activateParams});
    }
    else if (dlData.url) {
        return makeDLAccessUrlMenuEntry({dlTableUrl, dlData,idx, baseTitle, sourceTable, sourceRow,options,
            name, doFileAnalysis, activateParams});
    }
}

/**
 *
 * @param parsingAlgorithm
 * @param {Array.<DatalinkData>} dataLinkData
 * @return {Array.<DatalinkData>}
 */
export function filterDLList(parsingAlgorithm, dataLinkData) {
    if (parsingAlgorithm===USE_ALL) return dataLinkData;
    if (parsingAlgorithm===IMAGE) {
        return dataLinkData.filter( ({dlAnalysis}) => dlAnalysis.isImage);
    }
    if (parsingAlgorithm===RELATED_IMAGE_GRID) {
        return dataLinkData.filter( ({dlAnalysis}) => dlAnalysis.isGrid && dlAnalysis.isImage);
    }
    if (parsingAlgorithm===SPECTRUM) {
        return dataLinkData.filter( ({dlAnalysis}) => dlAnalysis.isSpectrum);
    }
    return dataLinkData;
}


function sortMenu(menu) {
    return menu
        .sort(({semantics:sem1,name:n1},{semantics:sem2,name:n2}) => {
            if (isThisSem(sem1)) {
                if (isThisSem(sem2)) {
                    if (n1?.includes('(#this)')) return -1;
                    else if (n2?.includes('(#this)')) return 1;
                    else if (n1<n2) return -1;
                    else if (n1>n2) return 1;
                    else return 0;
                }
                else return -1;
            }
            else {
                return 0;
            }
        })
        .sort((s1) => s1.name==='(#this)' ? -1 : 0);
}

/**
 *
 * @param obj
 * @param obj.dlTableUrl
 * @param {Array.<DatalinkData>} obj.dataLinkData
 * @param {TableModel} obj.sourceTable
 * @param {number} obj.sourceRow
 * @param {ActivateParams} obj.activateParams
 * @param {Array.<DataProductsDisplayType>} [obj.additionalServiceDescMenuList]
 * @param obj.doFileAnalysis
 * @param {DataProductsFactoryOptions} obj.options
 * @param obj.parsingAlgorithm
 * @param obj.baseTitle
 * @return {Array.<DataProductsDisplayType>}
 */
function createDataLinkMenuRet({dlTableUrl, dataLinkData, sourceTable, sourceRow, activateParams, baseTitle,
                               additionalServiceDescMenuList=[], doFileAnalysis=true,
                               options, parsingAlgorithm=USE_ALL}) {
    const auxTot= dataLinkData.filter( (e) => e.semantics==='#auxiliary').length;
    let auxCnt=0;
    let primeCnt=0;

    const menu= filterDLList(parsingAlgorithm,dataLinkData)
        .map( (dlData) => {
            const {semantics,url,error_message, dlAnalysis:{isAux,isThis}}= dlData;
            const name= makeName(semantics, url, auxTot, auxCnt, primeCnt, baseTitle);
            if (error_message) {
                const edp= dpdtMessageWithError(error_message);
                edp.complexMessage= false;
                edp.menuKey='dlt-'+dlData.rowIdx;
                edp.name= `Error in related data (datalink) row ${dlData.rowIdx}`;
                return edp;
            }
            const menuEntry= makeMenuEntry({dlTableUrl,dlData,idx:dlData.rowIdx, baseTitle, sourceTable,
                sourceRow, options, name, doFileAnalysis, activateParams});
            if (isAux) auxCnt++;
            if (isThis) primeCnt++;
            return menuEntry;
        })
        .filter((menuEntry) => menuEntry);

    if (parsingAlgorithm===SPECTRUM && menu.length>1) {

        const singleItemMenu= menu.filter( (m) => m.displayType===DPtypes.CHOICE_CTI && m.tbl_id);
        const activateObj= Object.fromEntries(singleItemMenu.map( ({tbl_id,activate,chartId}) => [tbl_id,{activate,chartId}]));
        const extractionObj= Object.fromEntries(singleItemMenu.map( (m) => [m.tbl_id,m.extraction]));
        const activate= makeMultiTableActivate(activateObj, activateParams);
        const extraction= makeMultiTableExtraction(extractionObj, activateParams);

        return [dpdtChartTable( 'Show: Spectrum', activate, extraction, 'multi-table',
            {extractionText: 'Pin Table', paIdx:0})];
    }

    if (parsingAlgorithm===USE_ALL) {
        menu.push(...additionalServiceDescMenuList,...addDataLinkEntries(dlTableUrl,activateParams));
    }

    return sortMenu(menu);
}

export function createDataLinkSingleRowItem({dlData, activateParams, baseTitle, options}) {
    const {semantics,url,error_message, dlAnalysis:{isAux,isThis}, serDef, serviceDefRef}= dlData;
    const name= semantics;
    if (error_message) {
        const edp= dpdtMessageWithError(error_message);
        edp.complexMessage= false;
        edp.menuKey='dlt-'+dlData.rowIdx;
        edp.name= `Error in related data (datalink) row ${dlData.rowIdx}`;
        return edp;
    }
    if (serviceDefRef && !serDef) {
        const edp= dpdtMessageWithError('Datalink row has an unsupported or missing service descriptor (async service descriptors are not supported)');
        edp.complexMessage= false;
        edp.menuKey='dlt-'+dlData.rowIdx;
        edp.name= `Error in related data (datalink) row ${dlData.rowIdx}`;
        return edp;
    }
    const menuEntry= makeMenuEntry({dlTableUrl:'none',dlData,idx:dlData.rowIdx, baseTitle,
        options, name, doFileAnalysis:true, activateParams});
    return menuEntry;

}


const analysisTypes= ['fits', 'cube', 'table', 'spectrum', 'auxiliary'];


function makeName(s='', url, auxTot, autCnt, primeCnt=0, baseTitle) {
    if (baseTitle) return makeNameWithBaseTitle(s,auxTot,autCnt,primeCnt,baseTitle);
    let name= (s==='#this' && primeCnt>0) ? '#this '+primeCnt  : s;
    name= s.startsWith('#this') ? `Primary product (${name})` : s;
    name= name[0]==='#' ? name.substring(1) : name;
    name= (name==='auxiliary' && auxTot>1) ? `${name}: ${autCnt}` : name;
    return name || url;
}

function makeNameWithBaseTitle(s='', auxTot, autCnt, primeCnt=0, baseTitle) {
    if (!s) return baseTitle;
    if (s.startsWith('#this')) {
       return primeCnt<1 ? `${baseTitle} (#this)` : `${baseTitle} (#this ${primeCnt})`;
    }
    if (s==='auxiliary' || s==='#auxiliary') return `auxiliary${auxTot>0?' '+autCnt:''}: ${baseTitle}`;
    return s[0]==='#' ? `${s.substring(1)}: ${baseTitle}` : `${s}: ${baseTitle}`;
}


/**
 *
 * @param name
 * @param menuKey
 * @param url
 * @param ct
 * @param semantics
 * @param activateParams
 * @param positionWP
 * @param table
 * @param row
 * @param size
 * @return {DataProductsDisplayType}
 */
export function createGuessDataType(name, menuKey, url,ct,semantics, activateParams, positionWP, table,row,size) {
    const {imageViewerId}= activateParams;
    if (ct.includes('image') || ct.includes('fits') || ct.includes('cube')) {
        const request= makeObsCoreRequest(url,positionWP,name,table,row);
        return dpdtImage({name,
            activate: createSingleImageActivate(request,imageViewerId,table.tbl_id,row),
            extraction: createSingleImageExtraction(request),
            menuKey, request,url, semantics,size});
    }
    else if (ct.includes('table') || ct.includes('spectrum') || semantics.includes('auxiliary')) {
        return dpdtTable(name,
            createTableActivate(url, semantics, activateParams, ct),
            menuKey,{url,semantics,size} );
    }
    else if (isSimpleImageType(ct)) {
        return dpdtPNG(name,url,menuKey,{semantics});
    }
    else if (isDownloadType(ct)) {
        let fileType;
        if (isTarType(ct)) fileType= 'tar';
        if (isGzipType('gz')) fileType= 'gzip';
        return dpdtDownload(name,url,menuKey,fileType,{semantics});
    }
}



const isThisSem= (semantics) => semantics==='#this';
const isAnalysisType= (ct) => (ct==='' || analysisTypes.some( (a) => ct.includes(a)));
const isTooBig= (size) => size>GIG;


