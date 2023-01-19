import {FileAnalysisType, Format} from 'firefly/data/FileAnalysis';
import {showInfoPopup, showYesNoPopup} from 'firefly/ui/PopupUtil';
import {getAppHiPSForMoc, isMOCFitsFromUploadAnalsysis} from 'firefly/visualize/HiPSMocUtil';
import {MetaConst} from 'firefly/data/MetaConst';
import {isLsstFootprintTable} from 'firefly/visualize/task/LSSTFootprintTask';
import {makeFileRequest} from 'firefly/tables/TableRequestUtil';
import {getFieldVal} from 'firefly/fieldGroup/FieldGroupUtils';
import {createNewRegionLayerId} from 'firefly/drawingLayers/RegionPlot';
import {dispatchCreateImageLineBasedFootprintLayer, dispatchCreateRegionLayer, getDlAry} from 'firefly/visualize/DrawLayerCntlr';
import {getDrawLayersByType, getPlotViewAry, primePlot} from 'firefly/visualize/PlotViewUtil';
import {dispatchPlotImage, visRoot} from 'firefly/visualize/ImagePlotCntlr';
import {dispatchTableFetch, dispatchTableSearch} from 'firefly/tables/TablesCntlr';
import {getSelectedDataSync, uniqueTblId} from 'firefly/tables/TableUtil';
import LSSTFootprint from 'firefly/drawingLayers/ImageLineBasedFootprint';
import WebPlotRequest from 'firefly/visualize/WebPlotRequest';
import RangeValues from 'firefly/visualize/RangeValues';
import {getAViewFromMultiView, getMultiViewRoot, IMAGE} from 'firefly/visualize/MultiViewCntlr';
import {PlotAttribute} from 'firefly/visualize/PlotAttribute';
import {isAnalysisTableDatalink} from '../util/VOAnalyzer.js';
import {fetchDatalinkUITable} from './dynamic/FetchDatalinkTable.js';
import {dispatchHideDialog} from 'firefly/core/ComponentCntlr';
import React from 'react';
import {
    acceptAnyTables, acceptDataLinkTables, acceptImages, acceptMocTables, acceptNonDataLinkTables,
    acceptNonMocTables, acceptRegions, acceptTableOrSpectrum, IMAGES, REGIONS, TABLES
} from 'firefly/ui/FileUploadUtil';

const FILE_ID = 'fileUpload';
const uploadOptions = 'uploadOptions';

const SUPPORTED_TYPES=[
    FileAnalysisType.REGION,
    FileAnalysisType.Image,
    FileAnalysisType.Table,
    FileAnalysisType.Spectrum
];

const LOAD_REGION=0;
const LOAD_MOC=1;
const LOAD_DL=2;
const LOAD_FOOTPRINT=3;
const LOAD_IMAGE_AND_TABLES=4;
const LOAD_IMAGE_ONLY=5;
const LOAD_TABLE_ONLY=6;

const imageWarning = 'Only loading the table(s), ignoring any selected image(s).';
const tableWarning = 'Only loading the image(s), ignoring any selected table(s).';


export function resultSuccess(request) {

    const {currentReport, currentDetailsModel, summaryModel, groupKey, acceptList, uniqueTypes}= request.additionalParams ?? {};
    const summaryTblId = groupKey; //FileUploadAnalysis
    const fileCacheKey = getFileCacheKey(groupKey);

    //determine if the file type or selections are valid to be loaded
    const {valid, errorMsg, title} = determineValidity(acceptList, uniqueTypes, summaryModel, summaryTblId, currentReport);

    if (!valid) {
        showInfoPopup(errorMsg, title);
        return false;
    }

    const {loadType, tableIndices, imageIndices} = determineLoadType(acceptList, uniqueTypes, summaryModel,
        summaryTblId, currentReport, fileCacheKey, request, currentDetailsModel);

    switch(loadType) {
        case LOAD_REGION:
            sendRegionRequest(fileCacheKey, currentReport);
            return true;

        case LOAD_MOC:
            const mocMeta= {[MetaConst.PREFERRED_HIPS]: getAppHiPSForMoc()};
            if (request.mocOp==='table') mocMeta[MetaConst.IGNORE_MOC]='true';
            //loadToUI = true if request.mocOp==='table', else loadToUI=false
            sendTableRequest(tableIndices, fileCacheKey, Boolean(request.tablesAsSpectrum==='spectrum'), currentReport, Boolean(request.mocOp==='table'), mocMeta);
            return true;

        case LOAD_DL:
            tableIndices.forEach((idx) => void fetchDatalinkUITable(fileCacheKey,idx) );
            return true;

        case LOAD_FOOTPRINT:
            sendLSSTFootprintRequest(fileCacheKey, request.fileName, tableIndices[0]);
            return true;

        case LOAD_IMAGE_AND_TABLES:
            sendTableRequest(tableIndices, fileCacheKey, Boolean(request.tablesAsSpectrum==='spectrum'), currentReport);
            sendImageRequest(imageIndices, request, fileCacheKey, currentReport);
            return true;

        case LOAD_IMAGE_ONLY:
            if (tableIndices > 0) {
                showWarning(tableWarning, () =>  sendImageRequest(imageIndices, request, fileCacheKey, currentReport));
            }
            else {
                sendImageRequest(imageIndices, request, fileCacheKey, currentReport);
                return true;
            }
            return false;

        case LOAD_TABLE_ONLY:
            if (imageIndices > 0) {
                 showWarning(imageWarning,
                    () => sendTableRequest(tableIndices, fileCacheKey, Boolean(request.tablesAsSpectrum==='spectrum'), currentReport));
            }
            else {
                sendTableRequest(tableIndices, fileCacheKey, Boolean(request.tablesAsSpectrum==='spectrum'), currentReport);
                return true;
            }
            return false;
        default: return false;
    }
    return false;
}

function showWarning(warningMsg,sendRequest) {
    showYesNoPopup(warningPrompt(warningMsg),(id, yes) => {
        if (yes) {
            sendRequest();
            dispatchHideDialog(id);
        }
        else {
            dispatchHideDialog(id);
        }
    });
}

const warningPrompt = (warningMsg) => {
    return (<div style={{width: 260}}>
        {warningMsg}
        <br/><br/>
        Are you sure you want to continue? <br/><br/>
        Click 'Yes' to continue or 'No' to cancel. <br/>
    </div>);
};

function determineLoadType(acceptList, uniqueTypes, summaryModel, summaryTblId, currentReport, fileCacheKey, request, currentDetailsModel) {
    const tableIndices = getSelectedRows(FileAnalysisType.Table, summaryTblId, currentReport, summaryModel);
    const imageIndices = getSelectedRows(FileAnalysisType.Image, summaryTblId, currentReport, summaryModel);
    const isMocFits =  isMOCFitsFromUploadAnalsysis(currentReport);
    const isDL=  isAnalysisTableDatalink(currentReport);

    if (isRegion(summaryModel)) {
        return {loadType: LOAD_REGION};
    }
    else if (isMocFits.valid) {
        const mocMeta= {[MetaConst.PREFERRED_HIPS]: getAppHiPSForMoc()};
        if (request.mocOp==='table') mocMeta[MetaConst.IGNORE_MOC]='true';
        //loadToUI = true if request.mocOp==='table', else loadToUI=false
        return {loadType: LOAD_MOC, tableIndices};
    }
    else if (isDL && request.datalinkOp === 'datalinkUI') { // handle Datalink / service descriptor UI
        return {loadType: LOAD_DL, tableIndices};
    }
    else if ( isLsstFootprintTable(currentDetailsModel) ) {
        return {loadType: LOAD_FOOTPRINT, tableIndices};
    }
    else { //either an image/table or combined FITS file
        if (acceptTableOrSpectrum(acceptList) && acceptImages(acceptList)) {
            return {loadType: LOAD_IMAGE_AND_TABLES, tableIndices, imageIndices};
        } else if (!acceptImages(acceptList)) {
            return {loadType: LOAD_TABLE_ONLY, tableIndices, imageIndices};
        } else if (!acceptTableOrSpectrum(acceptList)) {
            return {loadType: LOAD_IMAGE_ONLY, tableIndices, imageIndices};
        }
    }
}

const errorObj = {
    regionMismatchErr: {valid: false, errorMsg: 'You may not load a Region file from here', title: 'File Type Mismatch'},
    headerOnlyErr: {valid: false, errorMsg: 'FITS HDU type of HeaderOnly is not supported. A header-only HDU contains no additional data',title:'Validation Error'},
    noExtensionErr: {valid: false, errorMsg: 'No extension is selected', title:'Validation Error'},
    nonMocFitsErr: {valid: false, errorMsg: 'Warning: Loading a non-MOC FITS file from this dialog is not supported.', title: 'File Type Mismatch'},
    nonDLErr: {valid: false, errorMsg: 'Warning: Loading a non-DataLink Table file from this dialog is not supported.', title: 'File Type Mismatch'},
    nonMocAndDLErr: {valid: false, errorMsg: 'Warning: You may only load a MOC FITS or Data Link Table file from here.', title: 'File Type Mismatch'},
    noImgOrTblErr: {valid: false, errorMsg: 'You may not load a FITS file from here.', title: 'File Type Mismatch'},
    noTblSelectedErr: {valid: false, errorMsg: 'You must select at least one Table.', title: 'Validation Error'},
    noImgSelectedErr: {valid: false, errorMsg: 'You must select at least one Image.', title: 'Validation Error'},
    imgNotAcceptedErr: {valid: false, errorMsg: 'You may not load an image file from here.', title: 'File Type Mismatch'},
    tblNotAcceptedErr: {valid: false, errorMsg: 'You may not load tables from here.', title: 'File Type Mismatch'},
};

function determineValidity(acceptList, uniqueTypes, summaryModel, summaryTblId, currentReport) {
    let {errorMsg, title} = '';
    let valid = true;
    const tableIndices = getSelectedRows(FileAnalysisType.Table, summaryTblId, currentReport, summaryModel);
    const imageIndices = getSelectedRows(FileAnalysisType.Image, summaryTblId, currentReport, summaryModel);
    const isMocFits =  isMOCFitsFromUploadAnalsysis(currentReport);
    const isDL=  isAnalysisTableDatalink(currentReport);

    if (uniqueTypes.includes(REGIONS) && !acceptRegions(acceptList)) {
        return errorObj.regionMismatchErr;
    }

    if (!isFileSupported(summaryModel, currentReport)) {
        errorMsg = getFirstPartType(summaryModel) ? `File type of ${getFirstPartType(summaryModel)} is not supported.`: 'Could not recognize the file type';
        title = 'File Type Error';
        valid = false;
        return {valid, errorMsg, title};
    }

    if (!uniqueTypes.includes(REGIONS) && tableIndices.length + imageIndices.length === 0) {
        if (getSelectedRows('HeaderOnly', summaryTblId, currentReport, summaryModel)?.length) {
            return errorObj.headerOnlyErr;
        }
        else {
            return errorObj.noExtensionErr;
        }
    }

    //uniqueTypes are the types of the parts of the uploaded file (retrieved in FileAnalysis)
    if (uniqueTypes.includes(IMAGES) && uniqueTypes.includes(TABLES)) {
        if (!acceptImages(acceptList) && !acceptTableOrSpectrum(acceptList)) {
            return errorObj.noImgOrTblErr;
        }
        else if (!acceptImages(acceptList)) {
            if (tableIndices.length === 0) {
                return errorObj.noTblSelectedErr;
            }
        }
        else if (!acceptTableOrSpectrum(acceptList)){
            if (imageIndices.length === 0) {
                return errorObj.noImgSelectedErr;
            }
        }
    }
    else if (uniqueTypes.includes(IMAGES)) {
        if (!acceptImages(acceptList)) {
            return errorObj.imgNotAcceptedErr;
        }
        if (imageIndices.length === 0) {
            return errorObj.noImgSelectedErr;
        }
    }
    else if (uniqueTypes.includes(TABLES)) {
        if (!acceptAnyTables(acceptList))  {
            return errorObj.tblNotAcceptedErr;
        }
        if (acceptMocTables((acceptList)) && !isMocFits.valid &&
            (acceptList.length===1 || !acceptNonMocTables(acceptList) )) {
            return errorObj.nonMocFitsErr;
        }
        if (acceptDataLinkTables(acceptList) && !isDL &&
            (acceptList.length===1 || !acceptNonDataLinkTables(acceptList) )) {
            return errorObj.nonDLErr;
        }
        if (acceptMocTables((acceptList)) && acceptDataLinkTables(acceptList) && !isDL && !isMocFits.valid) {
            //edge case - in case acceptList= [DATA_LINK_TABLES, MOC_TABLES] and user uploads some other table file
            return errorObj.nonMocAndDLErr;
        }
        if (tableIndices.length === 0) {
            return errorObj.noTblSelectedErr;
        }
    }
    return {valid, errorMsg, title}; //valid will be true here
}

export function getFileCacheKey(groupKey) {
    // because this value is stored in different fields, so we have to check on what options were selected to determine the active value
    const uploadSrc = getFieldVal(groupKey, uploadOptions) || FILE_ID;
    return getFieldVal(groupKey, uploadSrc);
}

export const getPartCnt= (currentReport) => currentReport?.parts?.length ?? 1;
export const getFirstPartType= (currentSummaryModel) => currentSummaryModel?.tableData.data[0]?.[1];
export const getFileFormat= (currentReport) => currentReport?.fileFormat;
export const isRegion= (currentSummaryModel) => getFirstPartType(currentSummaryModel)===FileAnalysisType.REGION;

function isFileSupported(summaryModel, currentReport) {
    return getFirstPartType(summaryModel) && (SUPPORTED_TYPES.includes(getFirstPartType(summaryModel)) || getFileFormat(currentReport)===Format.FITS);
}

function sendRegionRequest(fileCacheKey,currentReport) {
    const drawLayerId = createNewRegionLayerId();
    const title= currentReport.fileName ?? 'Region File';
    dispatchCreateRegionLayer(drawLayerId, title, fileCacheKey, null);
    if (!getPlotViewAry(visRoot())?.length) {
        showInfoPopup('The region file is loaded but you will not be able to see it until you load an image (FITS or HiPS)', 'Warning');
    }
}

function sendTableRequest(tableIndices, fileCacheKey, treatAsSpectrum, currentReport, loadToUI= true, metaData={}) {
    const {fileName, parts=[]} = currentReport;

    tableIndices.forEach((idx) => {
        const {index} = parts[idx];
        const title = parts.length > 1 ? `${fileName}-${index}` : fileName;
        const META_INFO= {...metaData};
        if (treatAsSpectrum) META_INFO[MetaConst.DATA_TYPE_HINT]= 'spectrum';
        const options=  {META_INFO};
        const tblReq = makeFileRequest(title, fileCacheKey, null, options);
        tblReq.tbl_index = index;
        loadToUI ? dispatchTableSearch(tblReq) : dispatchTableFetch(tblReq);

    });
}

function sendLSSTFootprintRequest(uploadPath, displayValue, tblIdx) {
    const dl_id = getLSSTFootprintId();
    const pv = primePlot(visRoot());
    const pIds = pv ? [pv.plotId]: [];

    dispatchCreateImageLineBasedFootprintLayer(dl_id, displayValue,
        null, pIds,
        uploadPath, null, tblIdx);
}


function getLSSTFootprintId() {
    const dlId = uniqueTblId();
    let   idx = 1;
    let   fpLayerId = dlId;
    const dls = getDrawLayersByType(getDlAry(), LSSTFootprint.TYPE_ID);

    while (true) {
        const dl = dls.find((oneLayer) => oneLayer.drawLayerId === fpLayerId);

        if (!dl) return dlId;
        fpLayerId = dlId + `${idx++}`;
    }
}

/**
 * send request to get the data of image unit, the extension index is mapped to be that at
 * the server side
 * @param imageIndices
 * @param request
 * @param fileCacheKey
 * @param currentReport
 */
function sendImageRequest(imageIndices, request, fileCacheKey, currentReport) {

    if (imageIndices.length === 0) return;

    const {fileName, parts=[]} = currentReport;

    const wpRequest = WebPlotRequest.makeFilePlotRequest(fileCacheKey);
    wpRequest.setInitialRangeValues(RangeValues.make2To10SigmaLinear());
    const {viewerId=''} = getAViewFromMultiView(getMultiViewRoot(), IMAGE) || {};

    if (viewerId) {
        wpRequest.setPlotGroupId(viewerId);

        if (request.imageDisplay === 'mulWindow') {
            // plot each image separately

            imageIndices.forEach( (idx) => {
                const plotId = `${fileName}-${idx}`;
                const {index} = parts[idx];

                // if (index !== -1)  wpRequest.setPostTitle(`- ext. ${idx}`); // not primary
                if (index !== -1)  wpRequest.setAttributes({[PlotAttribute.POST_TITLE]:`- ext. ${idx}`}); // not primary

                wpRequest.setMultiImageExts(`${index}`);
                dispatchPlotImage({plotId, wpRequest, viewerId});
            });
        } else {
            const extList = imageIndices.map((idx) => parts?.[idx]?.index).join();

            wpRequest.setMultiImageExts(extList);

            const plotId = `${fileName.replace('.', '_')}-${imageIndices.join('_')}`;
            dispatchPlotImage({plotId, wpRequest, viewerId});
        }
    }
}

export function getSelectedRows(type, summaryTblId, currentReport, currentSummaryModel) {

    if (getPartCnt(currentReport)===1) {
        if (type===getFirstPartType(currentSummaryModel)) {
            return [0];
        }
        return [];
    }
    const {totalRows=0, tableData} = getSelectedDataSync(summaryTblId, ['Index', 'Type']);
    if (totalRows === 0) return [];
    const selectedRows = tableData.data;
    return selectedRows.filter((row) => row[1] === type)            // take only rows with the right type
        .map((row) => row[0]);                       // returns only the index
}
