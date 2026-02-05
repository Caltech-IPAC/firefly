import {isUndefined} from 'lodash';
import {Format, TableDataType} from '../../data/FileAnalysis';
import {getPreferCutout} from '../../ui/tap/Cutout';
import { getSearchTarget, obsCoreTableHasOnlyImages } from '../../voAnalyzer/TableAnalysis.js';
import { getDataLinkData, isSimpleImageType, isVoTable } from '../../voAnalyzer/VoDataLinkServDef.js';
import {getSizeAsString, GIG} from '../../util/WebUtil.js';
import {
    doFileNameAndTypeAnalysis,
    isNonServerAnalysisType, isUsableDownloadType, makeAnalysisActivateFunc, makeDownloadType
} from '../AnalysisUtils.js';
import {
    dispatchUpdateActiveKey, getActiveMenuKey, getCurrentActiveKeyID
} from '../DataProductsCntlr.js';
import {
    dpdtAnalyze, dpdtChartTable, dpdtDownload, dpdtFromMenu, dpdtImage, dpdtMessage,
    dpdtMessageWithError, dpdtPNG, dpdtTable, DPtypes
} from '../DataProductsType.js';
import {createSingleImageActivate, createSingleImageExtraction} from '../ImageDataProductsUtil.js';
import {
    createChartTableActivate, createTableActivate, createTableExtraction, makeMultiTableActivate,
    makeMultiTableExtraction
} from '../TableDataProductUtils.js';
import {createObsCoreProductTitle, getBasicTitling, getSpectrumTitle, makeDatalinkTitles} from '../VoUITitles';
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
 * @param {boolean} [params.useForTableGrid] - this result is part of a table grid Result
 * @return {DataProductsDisplayType}
 */
export function processDatalinkTable({sourceTable, row, datalinkTable, activateParams,
                                     additionalServiceDescMenuList, dlTableUrl, doFileAnalysis=true,
                                         options, parsingAlgorithm = USE_ALL, useForTableGrid}) {
    const dataLinkData= getDataLinkData(datalinkTable,false, sourceTable,row);
    const preferCutout= getPreferCutout(options.dataProductsComponentKey,sourceTable?.tbl_id);
    const isRelatedImageGrid= options.hasRelatedBands && dataLinkData.filter( (dl) => dl.dlAnalysis.isImage && dl.dlAnalysis.isGrid).length>1;
    const isMultiTableSpectrum= dataLinkData.filter( (dl) => dl.dlAnalysis.isThis && dl.dlAnalysis.isGrid && dl.dlAnalysis.isSpectrum).length>1;
    const originalParsingAlgorithm= parsingAlgorithm;
    if (parsingAlgorithm===USE_ALL) {
        if (isMultiTableSpectrum) parsingAlgorithm= SPECTRUM;
        else if (obsCoreTableHasOnlyImages(sourceTable,dataLinkData)) parsingAlgorithm= IMAGE;
    }

    let menu=  dataLinkData.length &&
        createDataLinkMenuRet({dlTableUrl,dataLinkData,sourceTable, sourceRow:row, activateParams,
            additionalServiceDescMenuList, doFileAnalysis, parsingAlgorithm, options, preferCutout});

    if (!menu.length && dataLinkData.length && originalParsingAlgorithm===USE_ALL && parsingAlgorithm!==USE_ALL) {
        menu= createDataLinkMenuRet({dlTableUrl,dataLinkData,sourceTable, sourceRow:row, activateParams,
            additionalServiceDescMenuList, doFileAnalysis, USE_ALL, options, preferCutout});
    }

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
                if (hasBandAndLabelInMenuKey(lastKey)) {
                    index= menu.findIndex( (m) => menuKeysMatchBandLabel(lastKey, m.menuKey));
                }
                else {
                    index= menu.findIndex( (m) => m.menuKey===lastKey);
                }
            }
            if (index<0) index= menu.findIndex( (m) => m.menuKey===activeMenuKey);
            if (index<0) index= 0;
            dispatchUpdateActiveKey({dpId, activeMenuKeyChanges:{[activeMenuLookupKey]:menu[index].menuKey}});
        }
        if (options.datalinkDisableMoreDrop) return dpdtFromMenu([menu[0]],index<0?0:index,dlTableUrl);
        return dpdtFromMenu(menu,index<0?0:index,dlTableUrl);
    }

    return dpdtMessage('No data available for this row',undefined,{activeMenuLookupKey});
}

function getDLMenuEntryData({dlTableUrl, dlData={}, idx, sourceTable}) {
    return {
        positionWP: getSearchTarget(sourceTable?.request,sourceTable),
        sRegion: dlData.sourceObsCoreData?.s_region,
        prodType: dlData.sourceObsCoreData?.dataproduct_type,
        activeMenuLookupKey:dlTableUrl??`no-table-${idx}`,
        menuKey:'dlt-'+idx
    };
}

function makeDLServerDefMenuEntry({dlTableUrl, dlData,idx, sourceTable, sourceRow, options,
                        name, prodTitle, dropDownText, activateParams}) {
    const {serDef}= dlData;
    const {positionWP, activeMenuLookupKey,menuKey}= getDLMenuEntryData({dlTableUrl, dlData,idx,sourceTable,sourceRow});

    return makeServiceDefDataProduct({
        serDef, sourceTable, sourceRow, idx, positionWP, activateParams, options, name:prodTitle??name, dropDownText,
                                               activeMenuLookupKey, menuKey, dlData,
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
 * @param p.dropDownText
 * @param p.name
 * @param p.prodTitle
 * @param {ActivateParams} p.activateParams
 * @return {DataProductsDisplayType|{displayType: string, menuKey: string, name: *, url: *, fileType: *}}
 */
function makeDLAccessUrlMenuEntry({dlTableUrl, dlData,idx, sourceTable, sourceRow, options,
                                      doFileAnalysis, name, prodTitle, dropDownText, activateParams}) {

    const {semantics,size,url, cloudAccess, dlAnalysis:{isSimpleImage}, contentType}= dlData;
    const {positionWP,sRegion,activeMenuLookupKey,menuKey}=
        getDLMenuEntryData({dlTableUrl, dlData,idx,sourceTable,sourceRow});

    if (isSimpleImage) {
        return dpdtPNG( getBasicTitling(name,'',Format.PNG)?.title, url,menuKey,{semantics, size, activeMenuLookupKey, dlData});
    }
    else if (isTooBig(size)) {
        return dpdtDownload(`Download: ${name} (too large to show)`,url,menuKey,'fits',{semantics, size, activeMenuLookupKey, dlData});
    }
    else if (isNonServerAnalysisType(url,contentType)) {
        const item= doFileNameAndTypeAnalysis({url,ct:contentType,wrapWithMessage:false, name});
        item.menuKey= menuKey;
        item.dlData= dlData;
        item.semantics= semantics;
        item.size= size;
        item.activeMenuLookupKey= activeMenuLookupKey;
        return item;
    }
    else if (dlData.dlAnalysis.isSpectrum && isVoTable(contentType)) {
        const tbl_id= getTableId(dlData.description,options,idx);
        const chartId= getChartId(dlData.description,options,idx);
        const activate= createChartTableActivate({
            chartAndTable:true,
            source: url,
            titleInfo:prodTitle,
            activateParams,
            dataTypeHint: TableDataType.Spectrum,
            tbl_id,
            chartInfo:{useChartChooser:true, showChartTitle:true, tableDataType:TableDataType.Spectrum},
            chartId,
            statefulTabComponentKey: options.statefulTabComponentKey
        });
        const extract= createTableExtraction(url,prodTitle,0,undefined,undefined,TableDataType.Spectrum);
        return dpdtChartTable(prodTitle, activate, extract, menuKey, {extractionText: 'Pin Table', paIdx:0, tbl_id,chartId, dlData});
    }
    else if (isAnalysisType(url,contentType)) {
        if (doFileAnalysis) {
            const request= makeObsCoreRequest({url,cloudAccess,positionWP,titleStr:prodTitle??name,
                table:sourceTable,row:sourceRow,expectStaticFile:true});
            const activate= makeAnalysisActivateFunc({table:sourceTable,row:sourceRow, request,
                activateParams, menuKey, activeMenuLookupKey, options, dlData, originalTitle:dropDownText||name, prodTitle});
            return dpdtAnalyze({name, activate,url,menuKey, semantics, size, activeMenuLookupKey,request, sRegion, dlData});
        }
        else {
            return createGuessDataType({name,menuKey,url,cloudAccess, ct:contentType,semantics, activateParams,
                positionWP,table:sourceTable,row:sourceRow,size, dlData});
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
 * @param {TableModel} p.sourceTable
 * @param {number} p.sourceRow
 * @param {DataProductsFactoryOptions} p.options
 * @param {string} p.name
 * @param {string} p.prodTitle
 * @param {string} [p.dropDownText]
 * @param {boolean} p.doFileAnalysis
 * @param p.activateParams
 * @return {Object}
 */
export function makeMenuEntry({dlTableUrl, dlData,idx, sourceTable, sourceRow, options,
                        name, prodTitle, doFileAnalysis, activateParams, dropDownText}) {

    if (dlData.serDef) {
        return makeDLServerDefMenuEntry({dlTableUrl, dlData,idx, sourceTable, sourceRow, options,
                                name, prodTitle:prodTitle??name, dropDownText, doFileAnalysis, activateParams});
    }
    else if (dlData.url) {
        return makeDLAccessUrlMenuEntry({dlTableUrl, dlData,idx, sourceTable, sourceRow,options,
            name, prodTitle:prodTitle??name, doFileAnalysis, activateParams});
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

export function sortRelatedGridUsingRequest(reqAry, relatedGridImageOrder) {
    const sortedReqAry= [];
    relatedGridImageOrder.forEach( (item) => {
        const foundEntries= reqAry.filter( (r) => r.getTitle()===item);
        sortedReqAry.push(...foundEntries);
    });
    const foundEntries= reqAry.filter( (r) => !relatedGridImageOrder.includes(r.getTitle()) );
    sortedReqAry.push(...foundEntries);
    return sortedReqAry;
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
 * @return {Array.<DataProductsDisplayType>}
 */
function createDataLinkMenuRet({dlTableUrl, dataLinkData, sourceTable, sourceRow, activateParams,
                               additionalServiceDescMenuList=[], doFileAnalysis=true, preferCutout,
                               options, parsingAlgorithm=USE_ALL}) {
    const totals={};
    const indices={};

    const filteredDLData= filterDLList(parsingAlgorithm,dataLinkData);
    filteredDLData.forEach((dlData) => {
        totals[dlData.semantics]= isUndefined(totals[dlData.semantics]) ? 0 : totals[dlData.semantics]+1;
    });

    const menu= filteredDLData.map( (dlData) => {
            const {error_message,labelDLExt, bandpassNameDLExt,
                dlAnalysis:{isThis,cutoutFullPair,isCounterpart,isCutout}}= dlData;
            const idx= dlData.rowIdx;

            indices[dlData.semantics]= isUndefined(indices[dlData.semantics]) ? 0 : indices[dlData.semantics]+1;
            const {name,prodTitle,imageRelatedTitle}=
                makeDatalinkTitles({dlData, totals, indices, sourceTable, sourceRow});

            if (error_message) {
                const edp= dpdtMessageWithError(error_message);
                edp.complexMessage= false;
                edp.menuKey='dlt-'+idx;
                edp.name= `Error in related data (datalink) row ${dlData.rowIdx}`;
                return edp;
            }

            const prodTitleIn= parsingAlgorithm===RELATED_IMAGE_GRID ? imageRelatedTitle : prodTitle;

            const menuParams= {dlTableUrl,dlData,idx, sourceTable, dropDownText:name,
                sourceRow, options, name, prodTitle:prodTitleIn, doFileAnalysis, activateParams};

            if (cutoutFullPair) {
                if (isCutout) return;
                if (preferCutout && (isThis || isCounterpart)) {
                    dlData.relatedDLEntries.cutout.cutoutToFullWarning= getCutoutSizeWarning(dlData);
                    menuParams.dlData = dlData.relatedDLEntries.cutout;
                }
            }
            const menuEntry= makeMenuEntry(menuParams);
            if (menuEntry && (labelDLExt || bandpassNameDLExt)) {
                menuEntry.menuKey= makeBandLabelMenuKey(labelDLExt,bandpassNameDLExt,menuEntry.menuKey); //todo - WHY IS THIS HERE???? !!!!!!
            }

            return menuEntry;
        })
        .filter(Boolean);

    if (parsingAlgorithm===SPECTRUM && menu.length>1) { // if I am only doing spectrum then gather them up into one display
        const singleItemMenu= menu.filter( (m) => m.displayType===DPtypes.CHOICE_CTI && m.tbl_id);
        const activateObj= Object.fromEntries(singleItemMenu.map( ({tbl_id,activate,chartId}) => [tbl_id,{activate,chartId}]));
        const extractionObj= Object.fromEntries(singleItemMenu.map( (m) => [m.tbl_id,m.extraction]));
        const activate= makeMultiTableActivate(activateObj, activateParams);
        const extraction= makeMultiTableExtraction(extractionObj, activateParams);

        return [dpdtChartTable( getSpectrumTitle(), activate, extraction, 'multi-table',
            {extractionText: 'Pin Table', paIdx:0})];
    }

    if (parsingAlgorithm===USE_ALL) {
        menu.push(...additionalServiceDescMenuList);
    }

    return sortMenu(menu, options.relatedGridImageOrder);
}

const BAND_MARKER= '__BAND:';
const BAND_MATCH_RE= /__BAND:(.*?)--/;
const LABEL_MARKER= '__LABEL:';
const LABEL_MATCH_RE= /__LABEL:(.*?)--/;

export function makeBandLabelMenuKey(label='',band='',menuKey) {
    let v= '--';
    if (label) v+= LABEL_MARKER+label + '--';
    if (band) v+= BAND_MARKER+band + '--';
    if (menuKey) v+= '__'+menuKey;
    return v;
}

export const hasBandInMenuKey= (menuKey='',band='') => menuKey.includes(`${BAND_MARKER}${band}`);
export const hasLabelInMenuKey= (menuKey='',label='') => menuKey.includes(`${LABEL_MARKER}${label}`);
export const hasBandAndLabelInMenuKey= (menuKey='',label='', band) => hasBandInMenuKey(menuKey,band) && hasLabelInMenuKey(menuKey,label);
export const findMenuKeyWithName= (keyAry,name) => name && keyAry.find( (k) => k.includes(LABEL_MARKER+name));

export function menuKeysMatchBandLabel(key1, key2) {
    const band1= key1.match(BAND_MATCH_RE)?.[1];
    const band2= key2.match(BAND_MATCH_RE)?.[1];
    const label1= key1.match(LABEL_MATCH_RE)?.[1];
    const label2= key2.match(LABEL_MATCH_RE)?.[1];
    return band1===band2 && label1===label2;
}


export function createDataLinkSingleRowItem({dlData, activateParams, options}) {
    const name= dlData.semantics;
    const error= hasError(dlData);
    if (error) {
        const edp= dpdtMessageWithError(error);
        edp.complexMessage= false;
        edp.menuKey='dlt-'+dlData.rowIdx;
        edp.name= `Error in related data (datalink) row ${dlData.rowIdx}`;
        return edp;
    }
    const menuEntry= makeMenuEntry({dlTableUrl:'none',dlData,idx:dlData.rowIdx,
        options, name, doFileAnalysis:true, activateParams});
    return menuEntry;

}

export function getCutoutTotalWarning(dlDataAry, length) {
    const allSize= dlDataAry.map ( (d) => d.size).reduce((tot,v) => tot+v,0) ;
    if (isWarnSize(allSize)) {
        return `Warning: Loading ${length} images with a total size of ${getSizeAsString(allSize)}, it might take awhile to load`;
    }
}

export function getCutoutSizeWarning(dlData) {
    if (isWarnSize(dlData.size)) {
        return `Warning: Full image file is ${getSizeAsString(dlData.size)}, it might take awhile to load`;
    }
}

export function hasError(dlData) {
    const {error_message, serDef, serviceDefRef}= dlData;
    if (error_message) return error_message;
    if (!dlData.dlAnalysis.usableEntry) return 'This (datalink) row is not usable by the application';
    if (serviceDefRef && !serDef)  {
        return 'Datalink row has an unsupported or missing service descriptor (async service descriptors are not supported)';
    }
}


const analysisTypes= ['fits', 'cube', 'table', 'spectrum', 'auxiliary', 'text'];




/**
 *
 * @param {Object} p
 * @param [p.name]
 * @param p.menuKey
 * @param p.url
 * @param {CloudAccessData} p.cloudAccess
 * @param p.ct
 * @param [p.semantics]
 * @param p.activateParams
 * @param p.positionWP
 * @param p.table
 * @param p.row
 * @param p.size
 * @param [p.dlData]
 * @return {DataProductsDisplayType}
 */
export function createGuessDataType({name, menuKey='guess-0', url,cloudAccess,ct,semantics='',
                                        activateParams, positionWP, table,row,size,dlData}) {
    const {imageViewerId}= activateParams;
    name??= createObsCoreProductTitle(table,row);
    if (ct.includes('image') || ct.includes('fits') || ct.includes('cube')) {
        const request= makeObsCoreRequest({url,cloudAccess,positionWP,titleStr:name,table,row, expectStaticFile:true});
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
    else if (isUsableDownloadType(undefined,ct)) {
        // return dpdtDownload(name,url,menuKey,getDownloadTypeDesc(ct),{semantics,dlData});
        return {...makeDownloadType(url,undefined,ct,false), menuKey, semantics, size, dlData};
    }
}


function isAnalysisType(url,ct) {
  const anaFromCt= (ct==='' || analysisTypes.some( (a) => ct.includes(a)));
  if (anaFromCt) return true;
  return url?.includes('.fits');
}
const isTooBig= (size) => size>MAX_SIZE;
export const isWarnSize= (size) => size>WARN_SIZE;


