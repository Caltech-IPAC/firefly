import {FileAnalysisType} from '../data/FileAnalysis.js';
import {hasRowAccess} from '../tables/TableUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {getObsCoreAccessFormat, getObsTitle} from '../voAnalyzer/TableAnalysis';
import {
    isGzipType, isHtmlType, isJSONType, isPDFType, isPlainTextType, isSimpleImageType, isTarType, isYamlType
} from '../voAnalyzer/VoDataLinkServDef';
import {dispatchActivateFileMenuItem, dispatchUpdateDataProducts} from './DataProductsCntlr.js';
import {
    dpdtDownload,
    dpdtImage, dpdtMessage, dpdtMessageWithDownload, dpdtPNG, dpdtSimpleMsg, dpdtText, dpdtWorkingMessage,
    dpdtWorkingPromise,
    DPtypes
} from './DataProductsType.js';
import {createGridImagesActivate} from './ImageDataProductsUtil.js';
import {doUploadAndAnalysis} from './UploadAndAnalysis.js';

const LOADING_MSG= 'Loading...';

const gridEntryHasImages= (parts) => parts.find( (p) => p.type===FileAnalysisType.Image);





/**
 * create a function to analyze and show a data product
 * @param makeReq
 * @return {function}
 */
export function makeAnalysisGetSingleDataProduct(makeReq) {
    return async (table, row, activateParams, options, dataTypeHint = '') => {
        const reqObj = makeReq(table, row, true);
        const request = reqObj?.single ?? reqObj;
        return uploadAndAnalyze({request, table, row, activateParams, dataTypeHint, options});
    };
}


export async function uploadAndAnalyze({request, table, row, activateParams, dataTypeHint = '', options, serviceDescMenuList}) {
    const ct= getObsCoreAccessFormat(table,row);
    const obsTitle= getObsTitle(table,row);
    if (!hasRowAccess(table, row)) dpdtSimpleMsg('You do not have access to this data.');
    if (isNonServerAnalysisType(request?.getURL(), ct)) return doFileNameAndTypeAnalysis({url:request?.getURL(),ct, obsTitle});
    const analysisPromise = doUploadAndAnalysis({table, row, request, activateParams, dataTypeHint, options, serviceDescMenuList});
    return dpdtWorkingPromise(LOADING_MSG, analysisPromise, request);
}


/**
 *
 * Returns a function that returns a Promise<DataProductsDisplayType>.
 * callback: function(table:TableModel, plotRows:Array.<Object>,activeParams:ActivateParams)
 * @param makeReq
 * @return {function | promise}
 */
export function makeAnalysisGetGridDataProduct(makeReq) {
    return async (table, plotRows, activateParams) => {

        const {imageViewerId} = activateParams;

        const highlightedPlotRow = plotRows.find((r) => r.row === table.highlightedRow);
        const highlightedId = highlightedPlotRow && highlightedPlotRow.plotId;


        const reqAry = plotRows
            .filter((pR) => hasRowAccess(table, pR.row))
            .map((pR) => {
                const r = makeReq(table, pR.row, true);
                if (!r || !r.single) return;
                r.single.setPlotId(pR.plotId);
                r.single.setAttributes({
                    [PlotAttribute.RELATED_TABLE_ROW]: pR.row + '',
                    [PlotAttribute.RELATED_TABLE_ID]: table.tbl_id
                });
                return r.single;
            })
            .filter((r) => r);


        const promiseAry = reqAry.map((r) => {
            return doUploadAndAnalysis({table, request: r, activateParams})
                .then((result) => {
                    if (!result?.fileMenu?.fileAnalysis?.parts) return false;
                    const {parts} = result.fileMenu.fileAnalysis;
                    if (!gridEntryHasImages(parts)) return false;
                    const newReq = r.makeCopy();
                    parts
                        .forEach((p) => Object.entries(p.additionalImageParams ?? {})
                            .forEach(([k, v]) => newReq.setParam(k, v)));
                    return newReq;
                });
        });

        const retPromise = Promise.all(promiseAry)
            .then((reqAry) => {
                const newReqAry = reqAry.filter((r) => r);
                if (newReqAry.length && newReqAry.find((r) => r.getPlotId() === highlightedId)) {
                    const activate = createGridImagesActivate(newReqAry, imageViewerId, table.tbl_id, plotRows);
                    return Promise.resolve(dpdtImage({name:'Image', activate}));
                } else {
                    return Promise.resolve(dpdtMessage('This product cannot be show in image grid', undefined, {gridNotSupported: true}));
                }
            })
            .catch(() => {
                return makeErrorResult();
            });

        return dpdtWorkingPromise(LOADING_MSG, retPromise);
    };
}

/**
 * return an activate function that will upload and analyze the file then dispatch the new DataProductsDisplayType
 * as the active data product. This will be picked up by the MultiProductViewer
 * @param {Object} obj
 * @param obj.table
 * @param obj.row
 * @param obj.request
 * @param obj.activateParams
 * @param obj.menuKey
 * @param obj.dataTypeHint
 * @param {ServiceDescriptorDef} [obj.serDef]
 * @param {DatalinkData} [obj.dlData]
 * @param [obj.originalTitle]
 * @param {DataProductsFactoryOptions} [obj.options]
 * @return {function}
 */
export function makeAnalysisActivateFunc({table, row, request, activateParams, menuKey,
                                             dataTypeHint, serDef, originalTitle, options, dlData}) {
    const analysisActivateFunc = async (menu, userInputParams) => {
        const {dpId}= activateParams;
        dispatchUpdateDataProducts(dpId, dpdtWorkingMessage(LOADING_MSG,menuKey));
        // do the uploading and analyzing
        const dPDisplayType= await doUploadAndAnalysis({ table, row, request, activateParams, dataTypeHint, options, menu,
            serDef, dlData, userInputParams, analysisActivateFunc, originalTitle, menuKey});
        // activate the result of the analysis
       dispatchResult(dPDisplayType, menu,menuKey,dpId, serDef, analysisActivateFunc);
    };
    return analysisActivateFunc;
}


/**
 * dispatch the new DataProductsDisplayType
 * @param {DataProductsDisplayType} dpType
 * @param {Array.<DataProductsDisplayType>} menu
 * @param {string} menuKey
 * @param {string} dpId
 * @param {ServiceDescriptorDef} serDef
 * @param {function} analysisActivateFunc
 */
function dispatchResult(dpType, menu,menuKey,dpId, serDef, analysisActivateFunc) {
    const modifiedResult= {...dpType, menu, menuKey, serDef, analysisActivateFunc};
    if (dpType.displayType===DPtypes.MESSAGE) {
        dispatchUpdateDataProducts(dpId, modifiedResult);
    }
    else if (dpType.displayType===DPtypes.SEND_TO_BROWSER) {
        window.open(dpType.url, '_blank');
        dispatchUpdateDataProducts(dpId, dpdtMessage('Loaded in new tab',menu,{complexMessage:true, menuKey, resetMenuKey:menuKey, serDef}));
    }
    else if (dpType.fileMenu) {
        dispatchActivateFileMenuItem({dpId,fileMenu:dpType.fileMenu,menu,currentMenuKey:menuKey});
    }
    else {
        console.log('AnalysisUtils: nothing to dispatch');
    }
}


export const isNonServerAnalysisType= (url, ct) => Boolean(doFileNameAndTypeAnalysis({url,ct}));

export function getExtensionFromUrl(url) {
    if (!url) return '';
    const i = url.lastIndexOf('.');
    if (i > 0 &&  i < url.length - 1) return url.substring(i+1).toLowerCase();
    return '';
}

export function doFileNameAndTypeAnalysis({url, ct, wrapWithMessage=true, name, obsTitle}) {
    if (!url) return undefined;
    const ext= getExtensionFromUrl(url);
    let item= undefined;
    const imExt= [ 'jpeg', 'jpg', 'png', 'gif'];

    if (isUsableDownloadType(ext,ct)) item= makeDownloadType(url,ext,ct,wrapWithMessage, name, obsTitle);
    else if (imExt.some( (e) => ext.includes(e)) || isSimpleImageType(ct)) item= makePngEntry(url, name, obsTitle);
    // else if (ext.endsWith('txt') || isPlainTextType(ct)) item= makeTextEntry(url, name, obsTitle);
    else if (ext.endsWith('yaml') || isYamlType(ct)) item= makeYamlEntry(url, name, obsTitle);
    else if (ext.endsWith('json') || isJSONType(ct))  item= makeJsonEntry(url, name, obsTitle);
    if (item) item.contentType= ct;
    return item;
}


export function isUsableDownloadType(ext='', ct) {
    const downloadExts= [ 'tar', 'pdf', 'html', 'gzip'];
    if (downloadExts.some( (e) => ext.includes(e))) return true;
    if (!ct) return false;
    if (isTarType(ct)) return true;
    if (isGzipType(ct)) return true;
    if (isPDFType(ct)) return true;
    if (isHtmlType(ct)) return true;
    return false;
}


export function makeDownloadType(url,ext,ct,wrapWithMessage,name, obsTitle) {
    const ctL= ct?.toLowerCase();
    let downloadItem;
    if (ext.includes('tar') || isTarType(ctL)) {
        downloadItem= makeTarEntry(url, name, obsTitle);
    }
    else if (ext.includes('pdf') || isPDFType(ct)) {
        downloadItem= makePdfEntry(url, name, obsTitle);
    }
    else if (ext.includes('gzip') || isGzipType(ctL)) {
        downloadItem= makeGzipEntry(url,name, obsTitle);
    }
    else if (ext.endsWith('html') || isHtmlType(ct)) {
        downloadItem= makeHtmlEntry(url, name, obsTitle);
    }
    else {
        downloadItem= makeAnyEntry(url,name, obsTitle);
    }
    if (downloadItem) {
        return wrapWithMessage ? dpdtMessage(downloadItem.message, [downloadItem]) : downloadItem;
    }
}

function makeOtMsg(obsTitle) {
    if (!obsTitle) return '';
    const otBase= obsTitle.length>25 ? obsTitle.substring(0,29) : obsTitle;
    return ` (${otBase})`;
}


export function makePdfEntry(url,name, obsTitle) {
    const otMsg= makeOtMsg(obsTitle);
    return dpdtDownload('Download PDF File'+otMsg, url, 'download-0', 'pdf',
        {
            message: 'This is a PDF file. It may be downloaded or opened in another tab',
            loadInBrowserMsg: 'Open PDF File'+otMsg,
            dropDownText: name ? `${name}${otMsg} (pdf file)` : undefined,
        }
    );
}

export function makeHtmlEntry(url,name, obsTitle) {
    const otMsg= makeOtMsg(obsTitle);
    return dpdtDownload('Open', url, 'download-0', 'html',
        {
            message: 'This is a web page or web application. It can be open in another tab',
            loadInBrowserMsg: 'Open Page'+otMsg,
            dropDownText: name ? `${name}${otMsg} (html)` : undefined,
        }
    );
}

export function makeJsonEntry(url,name, obsTitle) {
    const otMsg= makeOtMsg(obsTitle);
    return dpdtText('Show JSON file'+otMsg, url, undefined,
        {
            dropDownText: name ? `${name}${otMsg} (JSON File)` : undefined,
            fileType: 'json',
        }
    );
}

export function makeTarEntry(url,name, obsTitle) {
    const otMsg= makeOtMsg(obsTitle);
    return dpdtDownload('Download TAR File'+otMsg, url, 'download-0', 'tar',
        {
            message: 'This is a TAR file. It may only be downloaded',
            dropDownText: name ? `${name}${otMsg} (tar file)` : undefined,
        }
    );
}

export function makeGzipEntry(url,name,obsTitle) {
    const otMsg= makeOtMsg(obsTitle);
    return dpdtDownload('Download GZip File'+otMsg, url, 'download-0', 'gzip',
        {
            message: 'This is a GZip file. It may only be downloaded',
            dropDownText: name ? `${name}${otMsg} (GZip file)` : undefined,
        }
    );
}

export function makeAnyEntry(url,name, obsTitle) {
    const otMsg= makeOtMsg(obsTitle);
    return dpdtDownload('Download File'+otMsg, url, 'download-0', 'unknown',
        {
            message: 'This file may only only be downloaded',
            dropDownText: name ? `${name}${otMsg} (GZip file)` : undefined,
        }
    );
}

export function makePngEntry(url,name, obsTitle) {
    const otMsg= makeOtMsg(obsTitle);
    return dpdtPNG('Show PNG image'+otMsg,url,undefined,
        {
            dropDownText: name ? `${name}${otMsg} (image)` : undefined,
        }
    );
}

export function makeTextEntry(url,name, obsTitle) {
    const otMsg= makeOtMsg(obsTitle);
    return dpdtText('Show text file'+otMsg,url, undefined,
        {
            dropDownText: name ? `${name}${otMsg} (Plain text file)` : undefined,
            fileType: 'text',
        }
    );
}


export function makeYamlEntry(url,name, obsTitle) {
    const otMsg= makeOtMsg(obsTitle);
    return dpdtText('Show yaml file'+otMsg,url,undefined,
        {
            dropDownText: name ? `${name}${otMsg}  (Plain text file)` : undefined,
            fileType: 'yaml',
        }
    );
}

const makeErrorResult= (message, fileName,url) =>
    dpdtMessageWithDownload(`No displayable data available for this row${message?': '+message:''}`, fileName&&'Download: '+fileName, url);
