import {isArray,get} from 'lodash';
import {doUpload} from '../ui/FileUpload';
import {FileAnalysisType} from '../data/FileAnalysis';
import {
    createGridImagesActivate,
    createSingleImageActivate
} from './ImageDataProductsUtil';
import {PlotAttribute} from '../visualize/PlotAttribute';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../core/MasterSaga';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr';
import {
    dpdtDownload,
    dpdtImage,
    dpdtMessage,
    dpdtMessageWithDownload,
    dpdtPNG,
    dpdtWorkingMessage,
    dpdtWorkingPromise,
} from './DataProductsType';
import {hashCode} from '../util/WebUtil';
import {
    dataProductRoot, dispatchActivateFileMenuItem, dispatchUpdateActiveKey,
    dispatchUpdateDataProducts, getActiveFileMenuKeyByKey, getDataProducts
} from './DataProductsCntlr';
import {analyzePart, arrangeAnalysisMenu} from './PartAnalyzer';
import {hasRowAccess} from '../tables/TableUtil';


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
        return dpdtMessageWithDownload('Cannot not display TAR file, you may only download it', 'Download TAR File', url);
    }
    else if (ext.includes('pdf')) {
        return dpdtMessageWithDownload('Cannot not display PDF file, you may only download it', 'Download PDF File', url);
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
 * @param menu
 * @param menuKey
 * @param dataTypeHint
 * @return {function}
 */
export function makeFileAnalysisActivate(table, row, request, positionWP, activateParams, menu, menuKey, dataTypeHint) {
    return () => {
        doUploadAndAnalysis({
            table,row,request,activateParams,dataTypeHint,menu,menuKey, activateFirst:true,dispatchWorkingMessage:true });
    };
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
                    if (!get(result,'fileMenu.fileAnalysis')) return false;
                    return gridEntryHasImages(result.fileMenu.fileAnalysis) && r;
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
            .catch( (e) => {
                return makeErrorResult();
            });

        return Promise.resolve(dpdtWorkingPromise(LOADING_MSG,retPromise));
    };
}



const parseAnalysis= (serverCacheFileKey, analysisResult) =>
                            serverCacheFileKey && analysisResult && JSON.parse(analysisResult);


/**
 *
 * @param obj
 * @param obj.table
 * @param obj.row
 * @param obj.request
 * @param obj.activateParams
 * @param obj.dataTypeHint
 * @param obj.menu
 * @param obj.menuKey
 * @param obj.activateFirst
 * @param obj.dispatchWorkingMessage
 * @return {Promise.<DataProductsDisplayType>}
 */
function doUploadAndAnalysis({
                                 table,
                                 row,
                                 request,
                                 activateParams={},
                                 dataTypeHint='',
                                 menu=undefined,
                                 menuKey= undefined,
                                 activateFirst=false,
                                 dispatchWorkingMessage= false
                             }) {

    const {dpId}= activateParams;
    const cacheKey= hashCode(request.toString());


    const processAndActivate= (serverCacheFileKey, fileFormat, fileAnalysis) => {
        const {parts,fileName}= fileAnalysis;
        if (!parts || fileFormat==='UNKNOWN') return makeErrorResult('Could not parse file',fileName,serverCacheFileKey);

        const result= processAnalysisResult({ table, row, request, activateParams, serverCacheFileKey, fileAnalysis, dataTypeHint });
        activateFirst  && dispatchActivateFileMenuItem({dpId,fileMenu:result.fileMenu,menu,currentMenuKey:menuKey});
        return result;
    };

    if (uploadedCache[cacheKey]) {
        const {serverCacheFileKey, fileFormat, fileAnalysis}= uploadedCache[cacheKey];
        const result= processAndActivate(serverCacheFileKey, fileFormat, fileAnalysis);
        return Promise.resolve(result);
    }

    if (dispatchWorkingMessage) dispatchUpdateDataProducts(dpId, dpdtWorkingMessage(LOADING_MSG,request,{menuKey}));
    const url= request.getURL();

    url && dispatchAddActionWatcher({ id: url, actions:[ImagePlotCntlr.PLOT_PROGRESS_UPDATE],
        callback:watchForUploadUpdate, params:{url,dpId}});

    return doUpload(undefined, {fileAnalysis:'Details',webPlotRequest:request.toString()})
        .then(({status, message, cacheKey:serverCacheFileKey, fileFormat, analysisResult}) => {
            url && dispatchCancelActionWatcher(url);
            const fileAnalysis= parseAnalysis(serverCacheFileKey, analysisResult);
            if (fileAnalysis) uploadedCache[cacheKey]= {serverCacheFileKey,fileFormat,fileAnalysis};
            return processAndActivate(serverCacheFileKey, fileFormat, fileAnalysis);
        })
        .catch( (e) => {
            url && dispatchCancelActionWatcher(url);
            console.log('Call to Upload failed', e);
            dispatchUpdateDataProducts(dpId, makeErrorResult(e.message));
        });

}

function watchForUploadUpdate(action, cancelSelf, {url,dpId}) {
    const {requestKey,message}= action.payload;
    const d= getDataProducts(dataProductRoot(),dpId);
    if (!d || !d.displayType || !d.rootMessage || !d.isWorkingState || url!==requestKey) return;
    dispatchUpdateDataProducts(dpId,{...d, message: `${d.rootMessage} - ${message}`});
}


const gridEntryHasImages= (fileAnalysis) => fileAnalysis.parts.find( (p) => p.type===FileAnalysisType.Image);

function makeErrorResult(message, fileName,url) {
    return dpdtMessageWithDownload(`No displayable data available for this row${message?': '+message:''}`, fileName&&'Download: '+fileName, url);
}

/**
 *
 * @param obj
 * @param obj.table
 * @param obj.row
 * @param obj.request
 * @param obj.activateParams
 * @param obj.serverCacheFileKey
 * @param obj.fileAnalysis
 * @param obj.dataTypeHint
 * @return {DataProductsDisplayType}
 */
function processAnalysisResult({table, row, request, activateParams, serverCacheFileKey, fileAnalysis, dataTypeHint}) {


    const {imageViewerId, dpId}= activateParams;
    const rStr= request.toString();
    const activeItemLookupKey= hashCode(rStr);


    const {parts,fileName,fileFormat, filePath}= fileAnalysis;
    if (!parts) return makeErrorResult('',fileName,serverCacheFileKey);


    const fileMenu= {fileAnalysis, menu:[],activeItemLookupKey, activeItemLookupKeyOrigin:rStr};

    const url= request.getURL() || serverCacheFileKey;
    if (fileFormat===FileAnalysisType.PDF) {
        return dpdtMessageWithDownload('Cannot not display PDF file, you may only download it', 'Download PDF File', url);
    }
    if (fileFormat===FileAnalysisType.TAR) {
        return dpdtMessageWithDownload('Cannot not display Tar file, you may only download it', 'Download Tar File', url);
    }



    const partAnalysis= parts.map( (p) => analyzePart(p,fileFormat, serverCacheFileKey,activateParams));
    const imageParts= partAnalysis.filter( (pa) => pa.isImage);
    const hasImages= imageParts.length>0;


    const hasOnlySingleAxisImages= Boolean(partAnalysis.every( (pa) => pa.imageSingleAxis));
    if (hasOnlySingleAxisImages) {
        return dpdtMessageWithDownload('Cannot not display One-dimensional images (NAXIS==1)', 'download fits file', url);
    }



    const imageEntry= hasImages &&
        dpdtImage(`Image Data ${imageParts.length>1? ': All Images in File' :''}`,
            createSingleImageActivate(request,imageViewerId,table.tbl_id,row),'image-'+0, {request});


    if (imageEntry) fileMenu.menu.push(imageEntry);
    partAnalysis.forEach( (pa) => {
        let pAry= [];
        if (pa.tableResult) pAry= isArray(pa.tableResult) ? pa.tableResult : [pa.tableResult];
        if (pa.chartResult) pAry= isArray(pa.chartResult) ? [...pAry,...pa.chartResult] : [...pAry,pa.chartResult];

        pAry.forEach( (r) => fileMenu.menu.push(r));
    });

    const oneDErr= partAnalysis.reduce( (str,pa,idx) => {
        if (pa.imageSingleAxis) str+= `${str?', ':''}${idx}`;
        return str;
    },'');
    if (oneDErr) fileMenu.menu.push(dpdtDownload(`Download Only, Ext ${oneDErr}: Cannot display One-dimensional images (NAXIS==1)`, url));

    fileMenu.menu= arrangeAnalysisMenu(fileMenu.menu,parts,fileFormat, dataTypeHint);


    fileMenu.menu.forEach( (m,idx) => m.menuKey= 'fm-'+idx);

    let actIdx=0;
    if (fileMenu.menu.length) {
        const lastActiveFieldItem= getActiveFileMenuKeyByKey(dpId,activeItemLookupKey);
        actIdx= fileMenu.menu.findIndex( (m) => m.menuKey===lastActiveFieldItem);
        if (actIdx<0) actIdx= 0;
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


