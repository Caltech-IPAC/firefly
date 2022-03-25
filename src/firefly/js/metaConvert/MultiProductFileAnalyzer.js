import {get} from 'lodash';
import {FileAnalysisType} from '../data/FileAnalysis';
import {
    createGridImagesActivate,
    createSingleImageActivate,
    createSingleImageExtraction
} from './ImageDataProductsUtil';
import {PlotAttribute} from '../visualize/PlotAttribute';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../core/MasterSaga';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr';
import {
    dpdtImage,
    dpdtMessage,
    dpdtMessageWithDownload, dpdtMessageWithError,
    dpdtPNG,
    dpdtWorkingMessage,
    dpdtWorkingPromise, DPtypes,
} from './DataProductsType';
import {hashCode} from '../util/WebUtil';
import {upload} from '../rpc/CoreServices.js';
import {
    dataProductRoot, dispatchActivateFileMenuItem, dispatchUpdateActiveKey,
    dispatchUpdateDataProducts, getActiveFileMenuKeyByKey, getDataProducts
} from './DataProductsCntlr';
import {analyzePart, chooseDefaultEntry} from './PartAnalyzer';
import {getCellValue, getMetaEntry, getTblRowAsObj, hasRowAccess} from '../tables/TableUtil';
import {MetaConst} from '../data/MetaConst';
import {getAppOptions} from '../core/AppDataCntlr';
import {isDefined} from '../util/WebUtil.js';
import {dpdtSendToBrowser} from './DataProductsType.js';
import {RequestType} from 'firefly/api/ApiUtilImage.jsx';


const uploadedCache= {};

const LOADING_MSG= 'Loading...';

/**
 * create a function to analyze and show a data product
 * @param makeReq
 * @return {function}
 */
export function makeAnalysisGetSingleDataProduct(makeReq) {
    return (table, row, activateParams,dataTypeHint='') => {
        if (!hasRowAccess(table, row)) return Promise.resolve(dpdtMessage('You do not have access to this data.'));
        const retVal= makeReq(table, row, true);
        const r= (retVal && retVal.single) ? retVal.single : retVal;
        const extDP= fileExtensionSingleProductAnalysis(r);
        if (extDP) return Promise.resolve(extDP);
        const retPromise= doUploadAndAnalysis({table,row,request:r,activateParams,dataTypeHint});
        return Promise.resolve(dpdtWorkingPromise(LOADING_MSG,retPromise,r));
    };
}


function fileExtensionSingleProductAnalysis(request,idx=0) {

    const url= request.getURL();
    if (!url) return undefined;
    let ext='';
    const i = url.lastIndexOf('.');
    if (i > 0 &&  i < url.length - 1) ext = url.substring(i+1).toLowerCase();

    if (ext.includes('tar')) {
        return dpdtMessageWithDownload('Cannot display TAR file, you may only download it', 'Download TAR File', url);
    }
    else if (ext.includes('pdf')) {
        return dpdtMessageWithDownload('Cannot display PDF file, you may only download it', 'Download PDF File', url);
    }
    else if (ext.includes('jpeg') || ext.includes('png') || ext.includes('jpg') || ext.includes('gig')) {
        return dpdtPNG('Show PNG image',url,'dlt-'+idx);
    }
    return undefined;
}


/**
 * make a activate function for file analysis
 * @param table
 * @param row
 * @param request
 * @param positionWP
 * @param activateParams
 * @param menuKey
 * @param dataTypeHint
 * @param serDefParams
 * @param originalTitle
 * @return {function}
 */
export function makeFileAnalysisActivate(table, row, request, positionWP, activateParams, menuKey, dataTypeHint, serDefParams, originalTitle) {
    const analysisActivateFunc= (menu, userInputParams) => {
        void doUploadAndAnalysis({
            table,row,request,activateParams,dataTypeHint,
            menu,menuKey, activateResult:true,dispatchWorkingMessage:true,
            serDefParams, userInputParams, analysisActivateFunc,
            originalTitle});
    };
    return analysisActivateFunc;
}

/**
 *
 * Returns a callback function or a Promise<DataProductsDisplayType>.
 * callback: function(table:TableModel, plotRows:Array.<Object>,activeParams:ActivateParams)
 * @param makeReq
 * @return {function | promise}
 */
export function makeAnalysisGetGridDataProduct(makeReq) {
    return (table, plotRows, activateParams) => {

        const {imageViewerId}= activateParams;

        const highlightedPlotRow= plotRows.find( (r) => r.row===table.highlightedRow);
        const highlightedId= highlightedPlotRow && highlightedPlotRow.plotId;
        

        const reqAry= plotRows
            .filter( (pR) => hasRowAccess(table,pR.row))
            .map( (pR) => {
                const r = makeReq(table, pR.row, true);
                if (!r || !r.single) return;
                r.single.setPlotId(pR.plotId);
                r.single.setAttributes({
                    [PlotAttribute.DATALINK_TABLE_ROW]: pR.row + '',
                    [PlotAttribute.DATALINK_TABLE_ID]: table.tbl_id
                });
                return r.single;
            })
            .filter( (r) => r);


        const promiseAry= reqAry.map( (r) => {
            return doUploadAndAnalysis({table,request:r,activateParams})
                .then( (result) => {
                    if (!result?.fileMenu?.fileAnalysis?.parts) return false;
                    const {parts}= result.fileMenu.fileAnalysis;
                    if (!gridEntryHasImages(parts)) return false;
                    const newReq= r.makeCopy();
                    parts
                        .forEach( (p) => Object.entries(p.additionalImageParams ?? {} )
                        .forEach(([k,v]) => newReq.setParam(k,v)));
                    return newReq;
                });
        });

        const retPromise= Promise.all(promiseAry)
            .then( (reqAry) => {
                const newReqAry= reqAry.filter( (r) => r);
                if (newReqAry.length && newReqAry.find( (r) => r.getPlotId()===highlightedId) ) {
                    const activate= createGridImagesActivate(newReqAry,imageViewerId,table.tbl_id,plotRows);
                    return Promise.resolve( dpdtImage('Image',activate));
                }
                else {
                    return Promise.resolve( dpdtMessage('This product cannot be show in image grid',undefined, {gridNotSupported: true} ));
                }
            })
            .catch( () => {
                return makeErrorResult();
            });

        return Promise.resolve(dpdtWorkingPromise(LOADING_MSG,retPromise));
    };
}



const parseAnalysis= (serverCacheFileKey, analysisResult) =>
                            serverCacheFileKey && analysisResult && JSON.parse(analysisResult);


function doActivateResult(result, menu,menuKey,dpId, serDefParams) {
    if (result.displayType===DPtypes.MESSAGE && !result.singleDownload) {
        result.menu= menu;
        result.resetMenuKey= menuKey;
        dispatchUpdateDataProducts(dpId, result);
    }
    else if (result.displayType===DPtypes.MESSAGE) {
        dispatchUpdateDataProducts(dpId, result);
    }
    else if (result.displayType===DPtypes.SEND_TO_BROWSER) {
        window.open(result.url, '_blank');
        dispatchUpdateDataProducts(dpId, dpdtMessage('Loaded in new tab',menu,{complexMessage:true, menuKey, resetMenuKey:menuKey, serDefParams}));
    }
    else {
        dispatchActivateFileMenuItem({dpId,fileMenu:result.fileMenu,menu,currentMenuKey:menuKey});
    }
}

/**
 * This is the core function to upload and analyze the data
 * @param obj
 * @param {TableModel} obj.table table of data products
 * @param {number} obj.row active row number
 * @param {WebPlotRequest} obj.request - used for image or just downloading files
 * @param {ActivateParams} obj.activateParams
 * @param {String} obj.dataTypeHint  stuff like 'spectrum', 'image', 'cube', etc
 * @param obj.menu
 * @param obj.menuKey
 * @param obj.activateResult
 * @param obj.dispatchWorkingMessage
 * @param {Function} obj.analysisActivateFunc
 * @return {Promise.<DataProductsDisplayType>}
 */
async function doUploadAndAnalysis({
                                 table,
                                 row,
                                 request,
                                 activateParams={},
                                 dataTypeHint='',
                                 menu=undefined,
                                 menuKey= undefined,
                                 activateResult=false,
                                 dispatchWorkingMessage= false,
                                 serDefParams,
                                 userInputParams,
                                 analysisActivateFunc,
                                 originalTitle,
                             }) {

    const {dpId}= activateParams;


    /**
     * Process the file analysis and possibly active an entry
     * @param serverCacheFileKey
     * @param fileFormat
     * @param fileAnalysis
     * @return {DataProductsDisplayType}
     */
    const processAndActivate= (serverCacheFileKey, fileFormat, fileAnalysis) => {
        const {parts,fileName}= fileAnalysis;
        if (!parts || fileFormat==='UNKNOWN') return makeErrorResult('Could not parse file',fileName,serverCacheFileKey);

        const result= processAnalysisResult({ table, row, request, activateParams, serverCacheFileKey,
                                              fileAnalysis, dataTypeHint, analysisActivateFunc, serDefParams,
                                              originalTitle});
        activateResult && doActivateResult(result, menu,menuKey,dpId, serDefParams);
        return result;
    };



    //-----
    //----- setup and make the upload and analysis call - in a promise, process the analysis
    //-----
    if (dispatchWorkingMessage) dispatchUpdateDataProducts(dpId, dpdtWorkingMessage(LOADING_MSG,request,{menuKey}));
    if (request.getURL() && (serDefParams || userInputParams)) {
        request= request.makeCopy();
        const sendParams={};
        serDefParams?.filter( ({value}) => isDefined(value)).forEach( ({name,value}) => sendParams[name]= value);
        serDefParams?.filter( ({ref}) => ref).forEach( (p) => sendParams[p.name]= getCellValue(table, row, p.colName));
        userInputParams && Object.entries(userInputParams).forEach( ([k,v]) => v && (sendParams[k]= v) );
        const newUrl= new URL(request.getURL());

        Object.entries(sendParams).forEach( ([k,v]) => newUrl.searchParams.append(k,v));
        request.setURL(newUrl.toString());
    }


    //-----
    //----- if we have the analysis already cached then process it and return
    //-----

    const cacheKey= hashCode(request.toString() + (userInputParams? JSON.stringify(userInputParams):''));
    if (uploadedCache[cacheKey] && uploadedCache[cacheKey].dataTypes!==FileAnalysisType.ErrorResponse) {
        const {serverCacheFileKey, fileFormat, fileAnalysis}= uploadedCache[cacheKey];
        const result= processAndActivate(serverCacheFileKey, fileFormat, fileAnalysis);
        return Promise.resolve(result);
    }

    const url= request.getURL();
    url && dispatchAddActionWatcher({ id: url, actions:[ImagePlotCntlr.PLOT_PROGRESS_UPDATE],
        callback:watchForUploadUpdate, params:{url,dpId}});


    try {
        const {cacheKey:serverCacheFileKey, fileFormat, analysisResult}=
                                    await upload(request, 'Details',getDataProductAnalysisParams(table));
        url && dispatchCancelActionWatcher(url);
        let fileAnalysis= parseAnalysis(serverCacheFileKey, analysisResult);
        fileAnalysis= clientSideDataProductAnalysis(fileAnalysis);
        if (fileAnalysis) uploadedCache[cacheKey]= {serverCacheFileKey,fileFormat,fileAnalysis};
        return processAndActivate(serverCacheFileKey, fileFormat, fileAnalysis);
    }
    catch (e) {
        url && dispatchCancelActionWatcher(url);
        console.log('Call to Upload failed', e);
        dispatchUpdateDataProducts(dpId, makeErrorResult(e.message));
    }
}

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


const gridEntryHasImages= (parts) => parts.find( (p) => p.type===FileAnalysisType.Image);

function makeErrorResult(message, fileName,url) {
    return dpdtMessageWithDownload(`No displayable data available for this row${message?': '+message:''}`, fileName&&'Download: '+fileName, url);
}

function makeAllImageEntry(request, path, parts, imageViewerId,  tbl_id, row, imagePartsLength) {
    const newReq= request.makeCopy();
    newReq.setFileName(path);
    newReq.setRequestType(RequestType.FILE);
    parts.forEach( (p) => Object.entries(p.additionalImageParams ?? {} )
            .forEach(([k,v]) => newReq.setParam(k,v)));
    return dpdtImage(`Image Data ${imagePartsLength>1? ': All Images in File' :''}`,
        createSingleImageActivate(newReq,imageViewerId,tbl_id,row),
        createSingleImageExtraction(newReq),
        'image-'+0, {extractionText: 'Pin Image', request});
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
 * @return {DataProductsDisplayType}
 */
function processAnalysisResult({table, row, request, activateParams,
                                   serverCacheFileKey, fileAnalysis, dataTypeHint,
                                   analysisActivateFunc, serDefParams, originalTitle}) {


    const {imageViewerId, dpId}= activateParams;
    const rStr= request.toString();
    const activeItemLookupKey= hashCode(rStr);


    const {parts,fileName,fileFormat, disableAllImageOption= false}= fileAnalysis;
    if (!parts) return makeErrorResult('',fileName,serverCacheFileKey);


    const fileMenu= {fileAnalysis, menu:[],activeItemLookupKey, activeItemLookupKeyOrigin:rStr};

    const url= request.getURL() || serverCacheFileKey;

    // any of the following formats we respond to immediately
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
            return dpdtSendToBrowser(url, {serDefParams});
        case FileAnalysisType.Unknown:
            if (parts[0]?.type===FileAnalysisType.ErrorResponse) {
                const url= request.getURL();
                const m= dpdtMessageWithError(parts[0].desc);
                m.serDefParams= serDefParams;
                m.badUrl= url;
                return m;
            }
            break;
    }


    // do deeper inspection looking for charts, tables, and images

    const partAnalysis= parts.map( (p) => analyzePart(p,request, table, row, fileFormat, dataTypeHint, serverCacheFileKey,activateParams));
    const imageParts= partAnalysis.filter( (pa) => pa.imageResult);
    let makeAllImageOption= !disableAllImageOption;
    if (makeAllImageOption) makeAllImageOption= imageParts.length>1 || (imageParts.length===1 && parts.length===1);
    if (makeAllImageOption) makeAllImageOption= !(imageParts.every( (ip) => ip.imageResult.override));
    const duoImageTableParts= partAnalysis.filter( (pa) => pa.imageResult && pa.tableResult);
    if (makeAllImageOption && duoImageTableParts.length===1 && imageParts===1) makeAllImageOption= false;
    const useImagesFromPartAnalysis= parts.length>1 || !makeAllImageOption || duoImageTableParts.length;



    const imageEntry= makeAllImageOption &&
        makeAllImageEntry(request,fileAnalysis.filePath, parts,imageViewerId,table.tbl_id,row,imageParts.length);

    if (imageEntry) fileMenu.menu.push(imageEntry);
    partAnalysis.forEach( (pa) => {
        if (pa.imageResult && pa.tableResult) {
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
        m.serDefParams= serDefParams;
        m.originalTitle= originalTitle;
    });

    fileMenu.initialDefaultIndex= chooseDefaultEntry(fileMenu.menu,parts,fileFormat, dataTypeHint);

    let actIdx=0;
    if (fileMenu.menu.length) {
        const lastActiveFieldItem= getActiveFileMenuKeyByKey(dpId,activeItemLookupKey);
        actIdx= fileMenu.menu.findIndex( (m) => m.menuKey===lastActiveFieldItem);
        if (actIdx<0) actIdx= fileMenu.initialDefaultIndex;
    }
    else {// error case
        const msg= makeErrorMsg(parts,fileFormat);
        return dpdtMessageWithDownload(msg, 'Download File', url, fileFormat==='FITS'&&'FITS');
    }
    dispatchUpdateActiveKey({dpId, activeFileMenuKeyChanges:{[fileMenu.activeItemLookupKey]:fileMenu.menu[actIdx].menuKey}});
    return {...fileMenu.menu[actIdx],fileMenu};
}

function makeErrorMsg(parts, fileFormat) {
    if (parts.every( (p) => p.type===FileAnalysisType.HeaderOnly) && fileFormat==='FITS') {
        return 'You may only download this File - Nothing to display - FITS file has only header HDUs';
    }
    else {
        return 'Cannot analyze file';
    }
}


