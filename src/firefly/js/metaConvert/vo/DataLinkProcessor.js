import {getCellValue} from '../../tables/TableUtil.js';
import {
    getObsCoreProdType, getObsCoreSRegion, getServiceDescriptors, makeWorldPtUsingCenterColumns
} from '../../util/VOAnalyzer.js';
import {GIG} from '../../util/WebUtil.js';
import {getSearchTarget} from '../../visualize/saga/CatalogWatcher.js';
import {makeAnalysisActivateFunc} from '../AnalysisUtils.js';
import {dispatchUpdateActiveKey, getActiveMenuKey, getCurrentActiveKeyID} from '../DataProductsCntlr.js';
import {
    dpdtAnalyze, dpdtDownload, dpdtDownloadMenuItem, dpdtFromMenu, dpdtImage, dpdtMessage, dpdtPNG, dpdtTable, DPtypes
} from '../DataProductsType.js';
import {createSingleImageActivate, createSingleImageExtraction} from '../ImageDataProductsUtil.js';
import {createTableActivate, createTableExtraction} from '../TableDataProductUtils.js';
import {makeServiceDefDataProduct} from './ServDescProducts.js';
import {makeObsCoreRequest} from './VORequest.js';


export const USE_ALL= 'useAllAlgorithm';
export const RELATED_IMAGE_GRID= 'relatedImageGridAlgorithm';
export const IMAGE= 'imageAlgorithm';
export const SPECTRUM= 'spectrumAlgorithm';


function isGridImageData(dataLinkData) {
    const semCnt= dataLinkData
        .map( (dl) => analyzeSemantic(dl.semantics, dl.contentType))
        .filter( (sa) => sa.isImage && sa.isGrid);
    return semCnt.length>1;
}

const isThisSem= (semantics) => semantics==='#this';
const isTarType= (ct) => Boolean((ct?.includes('tar')));
const isGzipType= (ct) => Boolean((ct?.includes('gz')));
const isSimpleImageType= (ct) => (ct.includes('jpeg') || ct.includes('png') || ct.includes('jpg') || ct.includes('gig'));
const isDownloadType= (ct) => isTarType(ct) || isGzipType(ct) || ct.includes('octet-stream');
const isAnalysisType= (ct) => (ct==='' || analysisTypes.some( (a) => ct.includes(a)));
const isTooBig= (size) => size>GIG;


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
 * @param {String} [params.options] - which type of DL data
 * @param {string} [params.baseTitle]
 * @return {DataProductsDisplayType}
 */
export function processDatalinkTable({sourceTable, row, datalinkTable, activateParams, baseTitle=undefined,
                                     additionalServiceDescMenuList, dlTableUrl, doFileAnalysis=true,
                                         options, parsingAlgorithm = USE_ALL}) {
    const dataLinkData= getDataLinkData(datalinkTable);
    const isImageGrid= isGridImageData(dataLinkData);
    // const activateServiceDef= getBooleanMetaEntry(sourceTable, MetaConst.ACTIVATE_SERVICE_DEF, false);
    const menu=  dataLinkData.length &&
        createDataLinkMenuRet({dlTableUrl,dataLinkData,sourceTable, sourceRow:row, activateParams, baseTitle,
            additionalServiceDescMenuList, doFileAnalysis, parsingAlgorithm, options});

    const canShow= menu.length>0 && menu.some( (m) => m.displayType!==DPtypes.DOWNLOAD && m.size<GIG);
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
    const positionWP= getSearchTarget(sourceTable.request,sourceTable) ?? makeWorldPtUsingCenterColumns(sourceTable,sourceRow);
    const sRegion= getObsCoreSRegion(sourceTable,sourceRow);
    const prodType= getObsCoreProdType(sourceTable,sourceRow);
    const contentType= dlData.contentType.toLowerCase();
    return {positionWP,contentType, sRegion,prodType, activeMenuLookupKey:dlTableUrl,menuKey:'dlt-'+idx};
}

function makeDLServerDefMenuEntry({dlTableUrl, dlData,idx, baseTitle, sourceTable, sourceRow, options,
                        name, activateParams}) {
    const {serDef, semantics,size,serviceDefRef}= dlData;
    const {positionWP,sRegion,prodType,
        activeMenuLookupKey,menuKey}= getDLMenuEntryData({dlTableUrl, dlData,idx,sourceTable,sourceRow});

    const {title:servDescTitle=''}= serDef;
    const titleStr= baseTitle ? `${baseTitle} (${dlData.description||servDescTitle})` : (dlData.description||servDescTitle);

    return makeServiceDefDataProduct({
        serDef, sourceTable, sourceRow, idx, positionWP, activateParams, options, name,
                                               titleStr, activeMenuLookupKey, menuKey,
        datalinkExtra: {semantics, size, sRegion, prodTypeHint: dlData.contentType || prodType, serviceDefRef}
    });
}

function makeDLAccessUrlMenuEntry({dlTableUrl, dlData,idx, sourceTable, sourceRow,
                                      doFileAnalysis, name, activateParams}) {

    const {semantics,size,url, isThis, }= dlData;
    const {positionWP,sRegion,prodType, activeMenuLookupKey,menuKey, contentType}=
        getDLMenuEntryData({dlTableUrl, dlData,idx,sourceTable,sourceRow});

    if (isDownloadType(contentType)) {
        let fileType;
        if (isTarType(contentType)) fileType= 'tar';
        if (isGzipType(contentType)) fileType= 'gzip';
        return isThis ?
            dpdtDownloadMenuItem('Download file: '+name,url,menuKey,fileType,{semantics, size, activeMenuLookupKey}) :
            dpdtDownload('Download file: '+name,url,menuKey,fileType,{semantics, size, activeMenuLookupKey});
    }
    else if (isSimpleImageType(contentType)) {
        return dpdtPNG('Show PNG image: '+name,url,menuKey,{semantics, size, activeMenuLookupKey});
    }
    else if (isTooBig(size)) {
        return dpdtDownload('Download: '+name + '(too large to show)',url,menuKey,'fits',{semantics, size, activeMenuLookupKey});
    }
    else if (isAnalysisType(contentType)) {
        if (doFileAnalysis) {
            const request= makeObsCoreRequest(url,positionWP,name,sourceTable,sourceRow);
            const activate= makeAnalysisActivateFunc(sourceTable,sourceRow, request, positionWP,activateParams,menuKey, prodType);
            return dpdtAnalyze({name:'Show: '+name,
                activate,url,menuKey, semantics, size, activeMenuLookupKey,request, sRegion,
                prodTypeHint:dlData.contentType || prodType});
        }
        else {
            return createGuessDataType(name,menuKey,url,contentType,semantics, activateParams, positionWP,sourceTable,sourceRow,size);
        }
    }
}

function makeMenuEntry({dlTableUrl, dlData,idx, baseTitle, sourceTable, sourceRow, options,
                        name, doFileAnalysis, activateParams}) {

    if (dlData.serDef) {
        return makeDLServerDefMenuEntry({dlTableUrl, dlData,idx, baseTitle, sourceTable, sourceRow, options,
                                name, doFileAnalysis, activateParams});
    }
    else if (dlData.url) {
        return makeDLAccessUrlMenuEntry({dlTableUrl, dlData,idx, baseTitle, sourceTable, sourceRow,
            name, doFileAnalysis, activateParams});
    }
}

export function filterDLList(parsingAlgorithm, dataLinkData) {
    if (parsingAlgorithm===USE_ALL) return dataLinkData;
    if (parsingAlgorithm===IMAGE) {
        return dataLinkData.filter( (dlData) => dlData.isImage);
    }
    if (parsingAlgorithm===RELATED_IMAGE_GRID) {
        return dataLinkData.filter( (dlData) => dlData.isThis && dlData.isGrid && dlData.isImage);
    }
    if (parsingAlgorithm===SPECTRUM) {
        return dataLinkData.filter( (dlData) => dlData.isSpectrum);
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
 * @param obj.dataLinkData
 * @param {TableModel} obj.sourceTable
 * @param {number} obj.sourceRow
 * @param {ActivateParams} obj.activateParams
 * @param {Array.<DataProductsDisplayType>} [obj.additionalServiceDescMenuList]
 * @param obj.doFileAnalysis
 * @param obj.options
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
        .map( (dlData,idx) => {
            const {semantics,url, isAux, isThis}= dlData;
            const name= makeName(semantics, url, auxTot, auxCnt, primeCnt, baseTitle);
            const menuEntry= makeMenuEntry({dlTableUrl,dlData,idx, baseTitle, sourceTable,
                sourceRow, options, name, doFileAnalysis, activateParams});
            if (isAux) auxCnt++;
            if (isThis) primeCnt++;
            return menuEntry;
        })
        .filter((menuEntry) => menuEntry);

    if (parsingAlgorithm===USE_ALL) {
        menu.push(...additionalServiceDescMenuList,...addDataLinkEntries(dlTableUrl,activateParams));
    }

    return sortMenu(menu);
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

function analyzeSemantic(semantics='',contentType='') {
    const isImage= contentType?.toLowerCase()==='image/fits';
    const semLower=semantics.toLowerCase();
    const isThis= semLower.includes('#this') ?? false;
    const isAux= semLower==='#auxiliary';
    const isGrid= semLower.includes('-grid');
    const isCutout= semLower.includes('-cutout');
    const isSpectrum= semLower.includes('spectrum');
    const rBand= semLower.includes('-red');
    const gBand= semLower.includes('-green');
    const bBand= semLower.includes('-blue');
    return {isThis, isImage, isGrid, isAux, isSpectrum, isCutout, rBand, gBand, bBand};
}


/**
 * @param {TableModel} dataLinkTable - a TableModel that is a datalink call result
 * @return {Array.<{url, contentType, size, semantics, isThis, isGrid, isCutout, isImage, serviceDefRef, serDef}>} array of object with important data link info
 */
export function getDataLinkData(dataLinkTable) {
    return (dataLinkTable?.tableData?.data ?? [])
        .map( (row,idx) => {

            const serviceDefRef= getCellValue(dataLinkTable,idx,'service_def' );
            const servDescriptorsAry= getServiceDescriptors(dataLinkTable);
            let serDef;
            if (serviceDefRef && servDescriptorsAry) {
                const serDefFound= servDescriptorsAry.find( ({ID}) => ID===serviceDefRef);
                if (serDefFound) serDef = {...serDefFound, dataLinkTableRowIdx: idx};
            }

            const semantics= getCellValue(dataLinkTable,idx,'semantics' );
            const contentType= getCellValue(dataLinkTable,idx,'content_type' ) ||'';
            return {
                contentType,
                semantics,
                ...analyzeSemantic(semantics,contentType),
                url: getCellValue(dataLinkTable,idx,'access_url' ),
                description: getCellValue(dataLinkTable,idx,'description' ),
                size: Number(getCellValue(dataLinkTable,idx,'content_length' )),
                serviceDefRef,
                serDef,
                rowIdx: idx,
            };
        })
        .filter( ({url, serviceDefRef}) =>
            serviceDefRef || url?.startsWith('http') || url?.startsWith('ftp') );
}
