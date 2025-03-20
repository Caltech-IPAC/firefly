import {getPreferCutout} from '../../ui/tap/Cutout';
import {getSearchTarget, obsCoreTableHasOnlyImages} from '../../voAnalyzer/TableAnalysis.js';
import {
    getDataLinkData, isDownloadType, isGzipType, isSimpleImageType, isTarType, isVoTable
} from '../../voAnalyzer/VoDataLinkServDef.js';
import {getSizeAsString, GIG} from '../../util/WebUtil.js';
import {makeAnalysisActivateFunc} from '../AnalysisUtils.js';
import {
    dispatchUpdateActiveKey, getActiveMenuKey, getCurrentActiveKeyID
} from '../DataProductsCntlr.js';
import {
    dpdtAnalyze, dpdtChartTable, dpdtDownload, dpdtDownloadMenuItem, dpdtFromMenu, dpdtImage, dpdtMessage,
    dpdtMessageWithError, dpdtPNG, dpdtTable, DPtypes
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
const MAX_SIZE= 2*GIG;
const WARN_SIZE= GIG;


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
 * @param {boolean} [params.useForTableGrid] - this result is part of a table grid Result
 * @return {DataProductsDisplayType}
 */
export function processDatalinkTable({sourceTable, row, datalinkTable, activateParams, baseTitle=undefined,
                                     additionalServiceDescMenuList, dlTableUrl, doFileAnalysis=true,
                                         options, parsingAlgorithm = USE_ALL, useForTableGrid}) {
    const dataLinkData= getDataLinkData(datalinkTable,sourceTable,row);
    const preferCutout= getPreferCutout(options.dataProductsComponentKey,sourceTable?.tbl_id);
    const isRelatedImageGrid= options.hasRelatedBands && dataLinkData.filter( (dl) => dl.dlAnalysis.isImage && dl.dlAnalysis.isGrid).length>1;
    const isMultiTableSpectrum= dataLinkData.filter( (dl) => dl.dlAnalysis.isThis && dl.dlAnalysis.isGrid && dl.dlAnalysis.isSpectrum).length>1;
    if (parsingAlgorithm===USE_ALL) {
        if (isMultiTableSpectrum) parsingAlgorithm= SPECTRUM;
        else if (obsCoreTableHasOnlyImages(sourceTable)) parsingAlgorithm= IMAGE;
    }

    const menu=  dataLinkData.length &&
        createDataLinkMenuRet({dlTableUrl,dataLinkData,sourceTable, sourceRow:row, activateParams, baseTitle,
            additionalServiceDescMenuList, doFileAnalysis, parsingAlgorithm, options, preferCutout});

    const canShow= menu.length>0 && menu.some( (m) => m.displayType!==DPtypes.DOWNLOAD && (!m.size || m.size<MAX_SIZE));
    const activeMenuLookupKey= dlTableUrl;


    if (canShow) {
        let index= -1;
        const {dpId}= activateParams;
        const activeMenuKey= getActiveMenuKey(dpId, activeMenuLookupKey);
        if (!useForTableGrid) {
            if (isRelatedImageGrid) {
                const lastSource= getCurrentActiveKeyID(dpId);
                const lastKey= getActiveMenuKey(dpId, lastSource);
                index= menu.findIndex( (m) => m.menuKey===lastKey);
            }
            if (index<0) index= menu.findIndex( (m) => m.menuKey===activeMenuKey);
            if (index<0) index= 0;
            dispatchUpdateActiveKey({dpId, activeMenuKeyChanges:{[activeMenuLookupKey]:menu[index].menuKey}});
        }
        return dpdtFromMenu(menu,index,dlTableUrl);
    }

    return dpdtMessage('No data available for this row',undefined,{activeMenuLookupKey});
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

function getDLMenuEntryData({dlTableUrl, dlData={}, idx, sourceTable}) {
    return {
        positionWP: getSearchTarget(sourceTable?.request,sourceTable),
        contentType: dlData.contentType?.toLowerCase(),
        sRegion: dlData.sourceObsCoreData?.s_region,
        prodType: dlData.sourceObsCoreData?.dataproduct_type,
        activeMenuLookupKey:dlTableUrl??`no-table-${idx}`,
        menuKey:'dlt-'+idx
    };
}

function makeDLServerDefMenuEntry({dlTableUrl, dlData,idx, baseTitle, sourceTable, sourceRow, options,
                        name, activateParams}) {
    const {serDef}= dlData;
    const {positionWP, activeMenuLookupKey,menuKey}= getDLMenuEntryData({dlTableUrl, dlData,idx,sourceTable,sourceRow});
    const {title:servDescTitle=''}= serDef;
    const titleStr= baseTitle ? `${baseTitle} (${dlData.description||servDescTitle})` : (dlData.description||servDescTitle);

    return makeServiceDefDataProduct({
        serDef, sourceTable, sourceRow, idx, positionWP, activateParams, options, name,
                                               titleStr, activeMenuLookupKey, menuKey, dlData,
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
            dpdtDownloadMenuItem('Download file: '+name,url,menuKey,fileType,{semantics, size, activeMenuLookupKey,dlData}) :
            dpdtDownload('Download file: '+name,url,menuKey,fileType,{semantics, size, activeMenuLookupKey, dlData});
    }
    else if (isSimpleImage) {
        return dpdtPNG('Show PNG image: '+name,url,menuKey,{semantics, size, activeMenuLookupKey, dlData});
    }
    else if (isTooBig(size)) {
        return dpdtDownload('Download: '+name + '(too large to show)',url,menuKey,'fits',{semantics, size, activeMenuLookupKey, dlData});
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
        return dpdtChartTable('Show: ' + description, activate, extract, menuKey, {extractionText: 'Pin Table', paIdx:0, tbl_id,chartId, dlData});
    }
    else if (isAnalysisType(contentType)) {
        if (doFileAnalysis) {
            const dataTypeHint= dlData.dlAnalysis.isSpectrum ? 'spectrum' : prodType;
            const prodTypeHint= dlData.dlAnalysis.isSpectrum ? 'spectrum' : (dlData.contentType || prodType);
            const request= makeObsCoreRequest(url,positionWP,name,sourceTable,sourceRow);
            const activate= makeAnalysisActivateFunc({table:sourceTable,row:sourceRow, request,
                activateParams,menuKey, dataTypeHint, options, dlData});
            return dpdtAnalyze({name:'Show: '+name,
                activate,url,menuKey, semantics, size, activeMenuLookupKey,request, sRegion, prodTypeHint, dlData});
        }
        else {
            return createGuessDataType(name,menuKey,url,contentType,semantics, activateParams, positionWP,sourceTable,sourceRow,size, dlData);
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
 * @param {boolean} preferCutout
 * @return {Array.<DatalinkData>}
 */
export function filterDLList(parsingAlgorithm, dataLinkData, preferCutout) {
    if (parsingAlgorithm===USE_ALL) return dataLinkData;
    if (parsingAlgorithm===IMAGE) {
        return dataLinkData.filter( ({dlAnalysis}) => dlAnalysis.maybeImage);
    }
    if (parsingAlgorithm===RELATED_IMAGE_GRID) {
        const relatedGrid= dataLinkData.filter( ({dlAnalysis}) => dlAnalysis.isGrid && dlAnalysis.maybeImage);

        return relatedGrid.filter( (g) => (
            g.dlAnalysis.cutoutFullPair && !g.dlAnalysis.isCutout) || !g.dlAnalysis.cutoutFullPair);
    }
    if (parsingAlgorithm===SPECTRUM) {
        return dataLinkData.filter( ({dlAnalysis}) => dlAnalysis.isSpectrum);
    }
    return dataLinkData;
}


function sortMenu(menu, relatedGridImageOrder) {
    if (relatedGridImageOrder?.length && menu.every( (m) => m.dlData.labelDLExt)) {
        return sortRelatedGrid(menu, relatedGridImageOrder);
    }
    else {
       return basicSortMenu(menu);
    }
}

function basicSortMenu(menu) {
    return menu
        .sort( (m1,m2) => {
            const isThis1= m1.dlData?.dlAnalysis?.isThis ?? false;
            const isThis2= m2.dlData?.dlAnalysis?.isThis ?? false;
            const n1= m1.name;
            const n2= m2.name;

            if (isThis1) {
                if (isThis2) {
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

function sortRelatedGrid(menu, relatedGridImageOrder) {
    const sortedMenu= [];
    relatedGridImageOrder.forEach( (item) => {
        const foundEntries= menu.filter( (m) => m.dlData.labelDLExt===item);
        sortedMenu.push(...foundEntries);
    });
    const foundEntries= menu.filter( (m) => !relatedGridImageOrder.includes(m.dlData.labelDLExt) );
    sortedMenu.push(...foundEntries);
    return sortedMenu;
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
 * @param obj.preferCutout
 * @param {DataProductsFactoryOptions} obj.options
 * @param obj.parsingAlgorithm
 * @param obj.baseTitle
 * @return {Array.<DataProductsDisplayType>}
 */
function createDataLinkMenuRet({dlTableUrl, dataLinkData, sourceTable, sourceRow, activateParams, baseTitle,
                               additionalServiceDescMenuList=[], doFileAnalysis=true, preferCutout,
                               options, parsingAlgorithm=USE_ALL}) {
    const auxTot= dataLinkData.filter( (e) => e.semantics==='#auxiliary').length;
    let auxCnt=0;
    let primeCnt=0;

    const menu= filterDLList(parsingAlgorithm,dataLinkData, preferCutout)
        .map( (dlData) => {
            const {url,error_message,
                dlAnalysis:{isAux,isThis,cutoutFullPair,isCounterpart,isCutout}}= dlData;
            const idx= dlData.rowIdx;
            const name= makeName(dlData, url, auxTot, auxCnt, primeCnt, baseTitle);
            if (error_message) {
                const edp= dpdtMessageWithError(error_message);
                edp.complexMessage= false;
                edp.menuKey='dlt-'+idx;
                edp.name= `Error in related data (datalink) row ${dlData.rowIdx}`;
                return edp;
            }

            const menuParams= {dlTableUrl,dlData,idx, baseTitle, sourceTable,
                            sourceRow, options, name, doFileAnalysis, activateParams};

            if (cutoutFullPair) {
                if (isCutout) return;
                if (preferCutout && (isThis || isCounterpart)) {
                    if (isWarnSize(dlData.size)) {
                        dlData.relatedDLEntries.cutout.cutoutToFullWarning=
                            `Warning: Full image file is ${getSizeAsString(dlData.size)}, it might take awhile to load`;
                    }
                    menuParams.dlData = dlData.relatedDLEntries.cutout;
                }
            }
            const menuEntry= makeMenuEntry(menuParams);
            if (isAux) auxCnt++;
            if (isThis) primeCnt++;
            return menuEntry;
        })
        .filter(Boolean);

    if (parsingAlgorithm===SPECTRUM && menu.length>1) {
        const singleItemMenu= menu.filter( (m) => m.displayType===DPtypes.CHOICE_CTI && m.tbl_id);
        const activateObj= Object.fromEntries(singleItemMenu.map( ({tbl_id,activate,chartId}) => [tbl_id,{activate,chartId}]));
        const extractionObj= Object.fromEntries(singleItemMenu.map( (m) => [m.tbl_id,m.extraction]));
        const activate= makeMultiTableActivate(activateObj, activateParams);
        const extraction= makeMultiTableExtraction(extractionObj, activateParams);

        return [dpdtChartTable( 'Show: Spectrum', activate, extraction, 'multi-table',
            {extractionText: 'Pin Table', paIdx:0})];
    }

    if (menu?.length) {
        menu.forEach( (m) => {
            const {labelDLExt, bandpassNameDLExt}= m.dlData ?? {};
            if (labelDLExt || bandpassNameDLExt) {
                m.menuKey= makeBandLabelMenuKey(labelDLExt,bandpassNameDLExt);
            }
        });
    }

    if (parsingAlgorithm===USE_ALL) {
        menu.push(...additionalServiceDescMenuList);
    }

    return sortMenu(menu, options.relatedGridImageOrder);
}

const BAND_MARKER= '__BAND:';
const LABEL_MARKER= '__LABEL:';

function makeBandLabelMenuKey(label='',band='') {
    let v= '--';
    if (label) v+= LABEL_MARKER+label;
    if (band) v+= BAND_MARKER+band;
    return v;
}

export const hasBandInMenuKey= (menuKey='',band='') => menuKey.endsWith(`${BAND_MARKER}${band}`);
export const hasLabelInMenuKey= (menuKey='',label='') => menuKey.includes(`${LABEL_MARKER}${label}`);
export const findMenuKeyWithName= (keyAry,name) => name && keyAry.find( (k) => k.includes(LABEL_MARKER+name));


export function createDataLinkSingleRowItem({dlData, activateParams, baseTitle, options}) {
    const {semantics,error_message, serDef, serviceDefRef}= dlData;
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


function makeName(dlData, url, auxTot, autCnt, primeCnt=0, baseTitle) {
    const {id,semantics,labelDLExt, dlAnalysis:{isThis,isAux}}= dlData;
    if (labelDLExt) return labelDLExt;
    if (baseTitle) return makeNameWithBaseTitle(dlData,auxTot,autCnt,primeCnt,baseTitle);
    const baseTitleFromId= getBaseTitleFromId(id);
    let name= semantics[0]==='#' ? semantics.substring(1) : 'unknown';
    if (baseTitleFromId) {
        if (isThis) return `${baseTitleFromId}`;
        if (isAux) return `${baseTitleFromId}: auxiliary ${auxTot>1?autCnt+'':''}`;
        else return `${baseTitleFromId}: ${name}`;
    }
    else {
        name= (isThis && primeCnt>0) ? '#this '+primeCnt  : name;
        name= isThis ? `Primary product (${name})` : name;
        name= (isAux && auxTot>1) ? `${name}: ${autCnt}` : name;
        return name || url;
    }
}

function getBaseTitleFromId(id) {
    if (!id?.toLowerCase().startsWith('ivo:')) return;
    try {
        const url= new URL(id);
        if (!url) return;
        const sp= url.searchParams;
        if (sp.size) {
            const keyNames= [...sp.keys()];
            if (keyNames.length===1) return keyNames[0];
            return;
        }
        if (url.pathname.length>1) {
            return url.pathname.substring(1);
        }
    }
    catch {
        // do nothing
    }


}


function makeNameWithBaseTitle(dlData, auxTot, autCnt, primeCnt=0, baseTitle) {
    const {semantics,dlAnalysis:{isThis,isAux}}= dlData;
    if (!semantics) return baseTitle;
    if (isThis) {
       return primeCnt<1 ? `${baseTitle} (#this)` : `${baseTitle} (#this ${primeCnt})`;
    }
    if (isAux) return `auxiliary${auxTot>0?' '+autCnt:''}: ${baseTitle}`;
    return semantics[0]==='#' ? `${semantics.substring(1)}: ${baseTitle}` : `${semantics}: ${baseTitle}`;
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
 * @param dlData
 * @return {DataProductsDisplayType}
 */
export function createGuessDataType(name, menuKey, url,ct,semantics, activateParams, positionWP, table,row,size,dlData) {
    const {imageViewerId}= activateParams;
    if (ct.includes('image') || ct.includes('fits') || ct.includes('cube')) {
        const request= makeObsCoreRequest(url,positionWP,name,table,row);
        return dpdtImage({name,
            activate: createSingleImageActivate(request,imageViewerId,table.tbl_id,row),
            extraction: createSingleImageExtraction(request, dlData?.sourceObsCoreData, dlData),
            menuKey, request,url, semantics,size,dlData});
    }
    else if (ct.includes('table') || ct.includes('spectrum') || semantics.includes('auxiliary')) {
        return dpdtTable(name,
            createTableActivate(url, semantics, activateParams, ct),
            menuKey,{url,semantics,size,dlData} );
    }
    else if (isSimpleImageType(ct)) {
        return dpdtPNG(name,url,menuKey,{semantics,dlData});
    }
    else if (isDownloadType(ct)) {
        let fileType;
        if (isTarType(ct)) fileType= 'tar';
        if (isGzipType('gz')) fileType= 'gzip';
        return dpdtDownload(name,url,menuKey,fileType,{semantics,dlData});
    }
}


const isAnalysisType= (ct) => (ct==='' || analysisTypes.some( (a) => ct.includes(a)));
const isTooBig= (size) => size>MAX_SIZE;
export const isWarnSize= (size) => size>WARN_SIZE;


