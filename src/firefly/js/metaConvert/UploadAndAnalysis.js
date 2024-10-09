import {RequestType} from 'firefly/api/ApiUtilImage.jsx';
import {isArray} from 'lodash';
import {getAppOptions} from '../core/AppDataCntlr';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../core/MasterSaga';
import {DataProductTypes, FileAnalysisType} from '../data/FileAnalysis';
import {MetaConst} from '../data/MetaConst';
import {upload} from '../rpc/CoreServices.js';
import {getMetaEntry, getTblRowAsObj} from '../tables/TableUtil';
import {hashCode} from '../util/WebUtil';
import {isDefined} from '../util/WebUtil.js';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr';
import {
    dataProductRoot, dispatchUpdateActiveKey, dispatchUpdateDataProducts,
    getActiveFileMenuKeyByKey, getDataProducts
} from './DataProductsCntlr';
import {
    dpdtImage, dpdtMessage, dpdtMessageWithDownload, dpdtMessageWithError, dpdtPNG, dpdtUploadError, DPtypes,
} from './DataProductsType';
import {dpdtSendToBrowser} from './DataProductsType.js';
import {createSingleImageActivate, createSingleImageExtraction} from './ImageDataProductsUtil';
import {analyzePart} from './PartAnalyzer';
import {makeSingleDataProductWithMenu} from './vo/ObsCoreConverter.js';
import {makeUrlFromParams} from './vo/ServDescProducts.js';


const uploadedCache= {};


const parseAnalysis= (serverCacheFileKey, analysisResult) =>
                            serverCacheFileKey && analysisResult && JSON.parse(analysisResult);



/**
 * This is the core function to upload and analyze the data
 * @param obj
 * @param {TableModel} obj.table table of data products
 * @param {number} obj.row active row number
 * @param {WebPlotRequest} obj.request - used for image or just downloading files
 * @param {ActivateParams} obj.activateParams
 * @param {String} [obj.dataTypeHint]  stuff like 'spectrum', 'image', 'cube', etc
 * @param {DataProductsFactoryOptions} obj.options
 * @param {Array.<Object>} [obj.menu]
 * @param {ServiceDescriptorDef} [obj.serDef]
 * @param {Object} [obj.userInputParams]
 * @param {Function} [obj.analysisActivateFunc]
 * @param {string} [obj.originalTitle]
 * @param {string} [obj.menuKey]
 * @return {Promise.<DataProductsDisplayType>}
 */
export async function doUploadAndAnalysis({ table, row, request, activateParams={}, dataTypeHint='', options, menuKey,
                                 menu, serDef, userInputParams, analysisActivateFunc, originalTitle,serviceDescMenuList}) {

    const {dpId}= activateParams;


    const processAnalysis= (serverCacheFileKey, fileFormat, fileAnalysis) => {
        if (!fileAnalysis.parts || fileFormat==='UNKNOWN') {
            return dpdtMessageWithDownload('No displayable data available for this row: Unknown file type',
                                                  fileAnalysis.fileName&&'Download File', request.getURL());
        }
        const result=  processAnalysisResult({ table, row, request, activateParams, serverCacheFileKey,
            fileAnalysis, dataTypeHint, analysisActivateFunc, serDef, originalTitle, options, menuKey});
        if (serviceDescMenuList && result) {
             return makeSingleDataProductWithMenu(activateParams.dpId,result,1,serviceDescMenuList);
        }
        return result;
    };



    //-----
    //----- if this is a service descriptor and there are additional params then add them to the URL
    //----- todo: we need to add the more sophisticated service descriptor url building from
    //----  todo:        ServiceDefTools.js, makeServiceDescriptorSearchRequest
    //-----
    if (request.getURL() && (serDef?.serDefParams || userInputParams)) {
        request= request.makeCopy();
        const rowIdx= isDefined(serDef.dataLinkTableRowIdx) ? serDef.dataLinkTableRowIdx : row;
        const newUrl= makeUrlFromParams(request.getURL(), serDef, rowIdx, userInputParams);
        request.setURL(newUrl.toString());
    }


    //-----
    //----- if we have the analysis already cached then process it and return
    //-----

    const cacheKey= makeCacheKey(request,userInputParams);
    if (uploadedCache[cacheKey] && uploadedCache[cacheKey].dataTypes!==FileAnalysisType.ErrorResponse) {
        const {serverCacheFileKey, fileFormat, fileAnalysis}= uploadedCache[cacheKey];
        return processAnalysis(serverCacheFileKey, fileFormat, fileAnalysis);
    }


    //-----
    //---- load the url and analyze the result
    //-----

    startUpdateWatcher(request.getURL(),dpId);
    try {
        const {cacheKey:serverCacheFileKey, fileFormat, analysisResult}=
                                    await upload(request, 'Details',getDataProductAnalysisParams(table));
        endUpdateWatcher(request.getURL());
        const fileAnalysis= clientSideDataProductAnalysis(parseAnalysis(serverCacheFileKey, analysisResult));
        if (fileAnalysis) uploadedCache[cacheKey]= {serverCacheFileKey,fileFormat,fileAnalysis};
        return processAnalysis(serverCacheFileKey, fileFormat, fileAnalysis);
    }
    catch (e) {
        endUpdateWatcher(request.getURL());
        console.log('Call to Upload failed', e);
        dispatchUpdateDataProducts(dpId, {...makeErrorResult(e.message),menu, serDef, analysisActivateFunc});
        return dpdtUploadError(request.getURL(),e);
    }
}


const startUpdateWatcher= (url,dpId) =>
    url && dispatchAddActionWatcher({ id: url, actions:[ImagePlotCntlr.PLOT_PROGRESS_UPDATE],
        callback:watchForUploadUpdate, params:{url,dpId}});

const endUpdateWatcher= (url) => url && dispatchCancelActionWatcher(url);

const makeCacheKey= (request,userInputParams) =>
    hashCode(request.toString() + (userInputParams? JSON.stringify(userInputParams):''));


function getDataProductAnalysisParams(table) {
    const analyzerId= getMetaEntry(table, MetaConst.ANALYZER_ID);
    if (!analyzerId) return {};
    const paramsStr= getMetaEntry(table, MetaConst.ANALYZER_PARAMS);
    let params= {};
    let cParams= {};
    if (paramsStr) {
        const pAry= paramsStr.split(',').map( (s) => s.trim());
        params= pAry.reduce((obj,p) => {
            const kv= p.split('=',2).map( (s) => s.trim());
            if (kv.length===2) obj[kv[0]]= kv[1];
            return obj;
        },{});
    }
    const colParamsStr= getMetaEntry(table, MetaConst.ANALYZER_COLUMNS);
    if (colParamsStr) {
        const cAry= colParamsStr.split(',').map( (s) => s.trim());
        const rowObj= getTblRowAsObj(table);
        cParams= cAry.reduce( (obj,key) => {
            if (rowObj?.[key] !== undefined) obj[key]= rowObj[key];
            return obj;
        },{});
    }
    return {analyzerId, ...params, ...cParams};
}

function clientSideDataProductAnalysis(fileAnalysis) {
    if (!fileAnalysis) return fileAnalysis;
    const {dataProductsAnalyzerId, analyzerFound}= fileAnalysis;
    if (analyzerFound || !dataProductsAnalyzerId) return fileAnalysis;
    return getAppOptions().dataProducts?.clientAnalysis?.[dataProductsAnalyzerId]?.(fileAnalysis) || fileAnalysis;
}





function watchForUploadUpdate(action, cancelSelf, {url,dpId}) {
    const {requestKey,message}= action.payload;
    const d= getDataProducts(dataProductRoot(),dpId);
    if (!d || !d.displayType || !d.rootMessage || !d.isWorkingState || url!==requestKey) return;
    dispatchUpdateDataProducts(dpId,{...d, message: `${d.rootMessage} - ${message}`});
}



function makeErrorResult(message, fileName,url) {
    return dpdtMessageWithDownload(`No displayable data available for this row${message?': '+message:''}`, fileName&&'Download: '+fileName, url);
}

function makeAllImageEntry(request, path, parts, imageViewerId,  tbl_id, row, imagePartsLength) {
    const newReq= request.makeCopy();
    newReq.setFileName(path);
    newReq.setRequestType(RequestType.FILE);
    parts.forEach( (p) => Object.entries(p.additionalImageParams ?? {} )
            .forEach(([k,v]) => newReq.setParam(k,v)));
    const title= request.getTitle() || '';
    return dpdtImage({name: `Show: ${title||'Image Data'} ${imagePartsLength>1? ': All Images in File' :''}`,
        activate: createSingleImageActivate(newReq,imageViewerId,tbl_id,row),
        extraction: createSingleImageExtraction(newReq), request});
}

/**
 *
 * @param obj
 * @param {TableModel} obj.table table of data products
 * @param {number} obj.row active row number
 * @param {WebPlotRequest} obj.request - used for image or just downloading files
 * @param {ActivateParams} obj.activateParams
 * @param {String} obj.serverCacheFileKey - key to use for server calls to access the file
 * @param {FileAnalysisReport} obj.fileAnalysis results of the file analysis server call
 * @param {String} obj.dataTypeHint  stuff like 'spectrum', 'image', 'cube', etc
 * @param {Function} obj.analysisActivateFunc
 * @param {ServiceDescriptorDef} obj.serDef
 * @param {String} obj.originalTitle
 * @param {DataProductsFactoryOptions} obj.options
 * @param {String} obj.menuKey
 * @return {DataProductsDisplayType}
 */
function processAnalysisResult({table, row, request, activateParams,
                                   serverCacheFileKey, fileAnalysis, dataTypeHint,
                                  analysisActivateFunc, serDef, originalTitle, options, menuKey}) {

    const {parts,fileName,fileFormat}= fileAnalysis;
    if (!parts) return makeErrorResult('',fileName,serverCacheFileKey);


    const url= request.getURL() || serverCacheFileKey;

    const immediateResponse= getImmediateResponse(fileFormat,request,url,serDef,parts,menuKey);
    if (immediateResponse) return immediateResponse;

    return deeperInspection({
        table, row, request, activateParams,
        serverCacheFileKey, fileAnalysis, dataTypeHint,
        analysisActivateFunc, serDef, originalTitle, url, options
    });
}


function deeperInspection({ table, row, request, activateParams,
                              serverCacheFileKey, fileAnalysis, dataTypeHint,
                              analysisActivateFunc, serDef, originalTitle, url, options}) {

    const {parts,fileFormat, disableAllImageOption= false}= fileAnalysis;
    const {imageViewerId, dpId}= activateParams;
    const rStr= request.toString();
    const activeItemLookupKey= hashCode(rStr);
    const fileMenu= {fileAnalysis, menu:[],activeItemLookupKey, activeItemLookupKeyOrigin:rStr};

    const partAnalysis= parts.map( (p) => analyzePart(p,request, table, row, fileFormat, dataTypeHint, serverCacheFileKey,activateParams, options));
    const imageParts= partAnalysis.filter( (pa) => pa.imageResult);
    let makeAllImageOption= !disableAllImageOption;
    if (makeAllImageOption) makeAllImageOption= imageParts.length>1 || (imageParts.length===1 && parts.length===1);
    if (makeAllImageOption) makeAllImageOption= !(imageParts.every( (ip) => ip.imageResult.override));
    const duoImageTableParts= partAnalysis.filter( (pa) => pa.imageResult && pa.tableResult);
    if (makeAllImageOption && duoImageTableParts.length===1 && imageParts.length===1) makeAllImageOption= false;
    const useImagesFromPartAnalysis= parts.length>1 || !makeAllImageOption || duoImageTableParts.length;



    const imageEntry= makeAllImageOption &&
        makeAllImageEntry(request,fileAnalysis.filePath, parts,imageViewerId,table?.tbl_id,row,imageParts.length);

    if (imageEntry) fileMenu.menu.push(imageEntry);
    partAnalysis.forEach( (pa) => {
        if (pa.imageResult && isArray(pa.tableResult)) {
            pa.tableResult.forEach( (r) => {
                fileMenu.menu.push({...r, imageActivate:pa.imageResult.activate});
            });
        }
        else if (pa.imageResult && pa.tableResult) {
            fileMenu.menu.push({...pa.tableResult, imageActivate:pa.imageResult.activate});
        }
        else {
            if (useImagesFromPartAnalysis && pa.imageResult) fileMenu.menu.push(pa.imageResult);
            if (pa.tableResult) fileMenu.menu.push(pa.tableResult);
        }
    });

    fileMenu.menu.forEach( (m,idx) => {
        m.menuKey= 'fm-'+idx;
        m.analysisActivateFunc= analysisActivateFunc;
        m.serDef= serDef;
        m.originalTitle= originalTitle;
    });

    fileMenu.initialDefaultIndex= chooseDefaultEntry(fileMenu.menu,dataTypeHint);

    let actIdx=0;
    if (fileMenu.menu.length) {
        const lastActiveFieldItem= getActiveFileMenuKeyByKey(dpId,activeItemLookupKey);
        actIdx= fileMenu.menu.findIndex( (m) => m.menuKey===lastActiveFieldItem);
        if (actIdx<0) actIdx= fileMenu.initialDefaultIndex;
    }
    else {// error case
        return makeErrorDP(parts,fileFormat);
    }
    dispatchUpdateActiveKey({dpId, activeFileMenuKeyChanges:{[fileMenu.activeItemLookupKey]:fileMenu.menu[actIdx].menuKey}});
    return {...fileMenu.menu[actIdx],fileMenu};

}


function makeErrorDP(parts, fileFormat, url) {
    if (parts.every( (p) => p.type===FileAnalysisType.HeaderOnly) && fileFormat==='FITS') {
        const msg= 'You may only download this File - Nothing to display - FITS file has only header HDUs';
        return dpdtMessageWithDownload(msg, 'Download File', url, fileFormat==='FITS'&&'FITS');
    }
    else {
        return dpdtMessage('Cannot analyze file');
    }
}

/**
 * Return a DataProductsDisplayType if it is a simple type, otherwise return undefined
 * @param {String} fileFormat
 * @param request
 * @param {String} url
 * @param {ServiceDescriptorDef} serDef
 * @param parts
 * @param menuKey
 * @return {DataProductsDisplayType}
 */
function getImmediateResponse(fileFormat,request,url,serDef,parts,menuKey) {
    switch (fileFormat) {
        case FileAnalysisType.PDF:
            return dpdtMessageWithDownload('Cannot not display PDF file, you may only download it', 'Download PDF File', url);
        case FileAnalysisType.TAR:
            return dpdtMessageWithDownload('Cannot not display Tar file, you may only download it', 'Download Tar File', url);
        case FileAnalysisType.REGION:
            return dpdtMessageWithDownload('Cannot not display Region file, you may only download it', 'Download Region File', url);
        case FileAnalysisType.PNG:
            return dpdtPNG('PNG Image', url);
        case FileAnalysisType.HTML:
            return dpdtSendToBrowser(url, serDef?.serDefParams);
        case FileAnalysisType.Unknown:
            if (parts[0]?.type === FileAnalysisType.ErrorResponse) {
                const url = request.getURL();
                const m = dpdtMessageWithError(parts[0].desc);
                m.serDefParams = serDef?.serDefParams;
                m.badUrl = url;
                m.resetMenuKey=menuKey;
                return m;
            }
            break;
    }
}



/*
 * Determine which entry should be the default
 * @param {Array.<DataProductsDisplayType>} menu
 * @param {String} dataTypeHint
 * @return {Number|undefined} the index of the default
 */
function chooseDefaultEntry(menu,dataTypeHint) {
    if (!menu?.length) return undefined;
    let defIndex= menu.findIndex( (m) => m.requestDefault);
    if (defIndex > -1) return defIndex;
    const dth= dataTypeHint?.toLowerCase();

    switch (dth) {
        case DataProductTypes.timeseries:
            defIndex= menu.find( (m) => m.displayType===DPtypes.CHART);
            break;
        case DataProductTypes.spectrum:
            defIndex= menu.find( (m) => m.displayType===DPtypes.CHART);
            break;
    }
    return defIndex > -1 ? defIndex : 0;
}
