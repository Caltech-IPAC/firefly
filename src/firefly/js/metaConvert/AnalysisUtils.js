import {FileAnalysisType} from '../data/FileAnalysis.js';
import {hasRowAccess} from '../tables/TableUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {dispatchActivateFileMenuItem, dispatchUpdateDataProducts} from './DataProductsCntlr.js';
import {
    dpdtImage, dpdtMessage, dpdtMessageWithDownload, dpdtPNG, dpdtSimpleMsg, dpdtWorkingMessage, dpdtWorkingPromise,
    DPtypes
} from './DataProductsType.js';
import {createGridImagesActivate} from './ImageDataProductsUtil.js';
import {doUploadAndAnalysis} from './UploadAndAnalysis.js';

const LOADING_MSG= 'Loading...';

const gridEntryHasImages= (parts) => parts.find( (p) => p.type===FileAnalysisType.Image);

const makeErrorResult= (message, fileName,url) =>
    dpdtMessageWithDownload(`No displayable data available for this row${message?': '+message:''}`, fileName&&'Download: '+fileName, url);




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
    if (!hasRowAccess(table, row)) dpdtSimpleMsg('You do not have access to this data.');
    if (isNonAnalysisType(request)) return fileExtensionSingleProductAnalysis(request);
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
 * @param [obj.originalTitle]
 * @param {DataProductsFactoryOptions} [obj.options]
 * @return {function}
 */
export function makeAnalysisActivateFunc({table, row, request, activateParams, menuKey,
                                             dataTypeHint, serDef, originalTitle, options}) {
    const analysisActivateFunc = async (menu, userInputParams) => {
        const {dpId}= activateParams;
        dispatchUpdateDataProducts(dpId, dpdtWorkingMessage(LOADING_MSG,menuKey));
        // do the uploading and analyzing
        const dPDisplayType= await doUploadAndAnalysis({ table, row, request, activateParams, dataTypeHint, options, menu,
            serDef, userInputParams, analysisActivateFunc, originalTitle, menuKey});
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

const isNonAnalysisType= (request) => Boolean(fileExtensionSingleProductAnalysis(request));