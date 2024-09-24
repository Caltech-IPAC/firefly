import {FileAnalysisType, Format} from 'firefly/data/FileAnalysis';
import {showInfoPopup, showYesNoPopup} from 'firefly/ui/PopupUtil';
import {getAppHiPSForMoc, isMOCFitsFromUploadAnalsysis} from 'firefly/visualize/HiPSMocUtil';
import {MetaConst} from 'firefly/data/MetaConst';
import {isLsstFootprintTable} from 'firefly/visualize/task/LSSTFootprintTask';
import {makeFileRequest, makeTblRequest} from 'firefly/tables/TableRequestUtil';
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
import {getTableHeaderFromAnalysis} from '../metaConvert/PartAnalyzer.js';

import {isAnalysisTableDatalink} from '../voAnalyzer/VoDataLinkServDef.js';
import {fetchDatalinkUITable} from './dynamic/FetchDatalinkTable.js';
import {dispatchHideDialog} from 'firefly/core/ComponentCntlr';
import React from 'react';
import {
    acceptAnyTables, acceptDataLinkTables, acceptImages, acceptMocTables, acceptNonDataLinkTables,
    acceptNonMocTables, acceptRegions, acceptTableOrSpectrum, acceptUWS, IMAGES, REGIONS, TABLES, UWS
} from 'firefly/ui/FileUploadUtil';

const FILE_ID = 'fileUpload';
const uploadOptions = 'uploadOptions';

const SUPPORTED_TYPES=[
    FileAnalysisType.REGION,
    FileAnalysisType.Image,
    FileAnalysisType.Table,
    FileAnalysisType.Spectrum,
    FileAnalysisType.UWS
];

const LOAD_REGION=0;
const LOAD_MOC=1;
const LOAD_DL=2;
const LOAD_FOOTPRINT=3;
const LOAD_IMAGE_AND_TABLES=4;
const LOAD_IMAGE_ONLY=5;
const LOAD_TABLE_ONLY=6;
const LOAD_UWS=7;

const imageWarning = 'Only loading the table(s), ignoring any selected image(s).';
const tableWarning = 'Only loading the image(s), ignoring any selected table(s).';


export function resultSuccess(request) {

    const {report, detailsModel, summaryModel, groupKey, acceptList, uniqueTypes,
        acceptOneItem}= request.additionalParams ?? {};
    const summaryTblId = groupKey; //FileUploadAnalysis
    const fileCacheKey = getFileCacheKey(groupKey);

    //determine if the file type or selections are valid to be loaded
    const {valid, errorMsg, title} = determineValidity(acceptList, uniqueTypes, summaryModel,
        summaryTblId, report, acceptOneItem);

    if (!valid) {
        showInfoPopup(errorMsg, title);
        return false;
    }

    const {loadType, tableIndices, imageIndices} = determineLoadType(acceptList, uniqueTypes, summaryModel,
        summaryTblId, report, fileCacheKey, request, detailsModel, acceptOneItem);

    switch(loadType) {
        case LOAD_REGION:
            sendRegionRequest(fileCacheKey, report);
            return true;

        case LOAD_MOC:
            const mocMeta= {[MetaConst.PREFERRED_HIPS]: getAppHiPSForMoc()};
            if (request.mocOp==='table') mocMeta[MetaConst.IGNORE_MOC]='true';
            //loadToUI = true if request.mocOp==='table', else loadToUI=false
            sendTableRequest(tableIndices, fileCacheKey, request.tablesAsSpectrum==='spectrum', report, request.mocOp==='table', mocMeta);
            return true;

        case LOAD_DL:
            tableIndices.forEach((idx) => void fetchDatalinkUITable(fileCacheKey,idx,{searchParams:{url:fileCacheKey}}, 'DLGeneratedDropDownCmd') );
            return true;

        case LOAD_FOOTPRINT:
            sendLSSTFootprintRequest(fileCacheKey, request.fileName, tableIndices[0]);
            return true;

        case LOAD_IMAGE_AND_TABLES:
            sendTableRequest(tableIndices, fileCacheKey, request.tablesAsSpectrum==='spectrum', report);
            sendImageRequest(imageIndices, request, fileCacheKey, report);
            return true;

        case LOAD_IMAGE_ONLY:
            if (tableIndices > 0) {
                showWarning(tableWarning, () =>  sendImageRequest(imageIndices, request, fileCacheKey, report));
            }
            else {
                sendImageRequest(imageIndices, request, fileCacheKey, report);
                return true;
            }
            return false;

        case LOAD_TABLE_ONLY:
            if (imageIndices > 0) {
                 showWarning(imageWarning,
                    () => sendTableRequest(tableIndices, fileCacheKey, request.tablesAsSpectrum==='spectrum', report));
            }
            else {
                sendTableRequest(tableIndices, fileCacheKey, request.tablesAsSpectrum==='spectrum', report);
                return true;
            }
            return false;

        case LOAD_UWS:
            const title= report.fileName ?? 'UWS Job File';
            const jobUrl = report?.parts[0].url;
            const req = makeTblRequest('UwsJob', title, {jobUrl});
            dispatchTableSearch(req);
            return true;

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

function determineLoadType(acceptList, uniqueTypes, summaryModel, summaryTblId, report, fileCacheKey,
                           request, detailsModel, acceptOneItem) {

    //highlighted row and entryType is used when acceptOneItem === true
    const highlightedRowIdx = summaryModel?.highlightedRow;
    const highlightedRow = summaryModel?.tableData?.data?.[highlightedRowIdx];
    const entryType = highlightedRow?
        highlightedRow[1]: summaryModel?.tableData?.data?.[0]?.[1];
    const singleAxisImageAsTable= request.singleRowImageAsTable==='singleAxisImage';

    const tableIndices = acceptOneItem && entryType === FileAnalysisType.Table? [highlightedRowIdx] :
        getSelectedRows(FileAnalysisType.Table, summaryTblId, report, summaryModel, singleAxisImageAsTable);
    const imageIndices = acceptOneItem && entryType === FileAnalysisType.Image? [highlightedRowIdx] :
        getSelectedRows(FileAnalysisType.Image, summaryTblId, report, summaryModel, singleAxisImageAsTable);

    const isMocFits =  isMOCFitsFromUploadAnalsysis(report);
    const isDL=  isAnalysisTableDatalink(report);

    if (isRegion(summaryModel)) {
        return {loadType: LOAD_REGION};
    }
    else if (isUWS(report)) {
        return {loadType: LOAD_UWS};
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
    else if ( isLsstFootprintTable(detailsModel) ) {
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

export function findSingleAxisImages(report) {
    const singleAxis= report?.parts
            .filter( ({type}) => type==='Image')
            .map( (part) => {
                return (
                    {
                        index: part.index,
                        NAXIS1: Number(getTableHeaderFromAnalysis('NAXIS1',part)),
                        NAXIS2: Number(getTableHeaderFromAnalysis('NAXIS2',part))
                    }
                );
            })
            .filter( ({NAXIS1,NAXIS2})=> !isNaN(NAXIS1) || !isNaN(NAXIS2))
            .filter( ({NAXIS2})=> NAXIS2===1)
        ?? [];
    return singleAxis;
}

const errorObj = {
    regionMismatchErr: {valid: false, errorMsg: 'You may not load a Region file from here', title: 'File Type Mismatch'},
    uwsMismatchErr: {valid: false, errorMsg: 'You may not load a UWS Job File from here', title: 'File Type Mismatch'},
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

function determineValidity(acceptList, uniqueTypes, summaryModel, summaryTblId,
                           report, acceptOneItem) {
    let {errorMsg, title} = '';
    let valid = true;

    //highlighted row and entryType is used when acceptOneItem === true
    const highlightedRowIdx = summaryModel?.highlightedRow;
    const highlightedRow = summaryModel?.tableData?.data?.[highlightedRowIdx];
    const entryType = highlightedRow?
        highlightedRow[1]: summaryModel?.tableData?.data?.[0]?.[1];

    const tableIndices = acceptOneItem && entryType === FileAnalysisType.Table? [highlightedRowIdx] :
        getSelectedRows(FileAnalysisType.Table, summaryTblId, report, summaryModel);
    const imageIndices = acceptOneItem && entryType === FileAnalysisType.Image? [highlightedRowIdx] :
        getSelectedRows(FileAnalysisType.Image, summaryTblId, report, summaryModel);
    const isMocFits =  isMOCFitsFromUploadAnalsysis(report);
    const isDL=  isAnalysisTableDatalink(report);


    if (uniqueTypes.includes(REGIONS) && !acceptRegions(acceptList)) {
        return errorObj.regionMismatchErr;
    }

    //check for url because user may try and upload a uws job xml file from the 'Upload File' option instead of 'Upload from URL'
    if (uniqueTypes.includes(UWS) && (!acceptUWS(acceptList) || !report.parts[0].url)) {
        return errorObj.uwsMismatchErr;
    }

    if (!isFileSupported(summaryModel, report)) {
        errorMsg = getFirstPartType(summaryModel) ? `File type of ${getFirstPartType(summaryModel)} is not supported.`: 'Could not recognize the file type';
        title = 'File Type Error';
        valid = false;
        return {valid, errorMsg, title};
    }

    //Intentionally put this code block under the regions & files not supported check, to avoid checking for those 2 again here
    if (acceptOneItem) {
        switch (entryType) {
            case FileAnalysisType.Table:
                if (!acceptAnyTables(acceptList)) return errorObj.tblNotAcceptedErr;
                break;
            case FileAnalysisType.Image:
                if (!acceptImages(acceptList)) return errorObj.imgNotAcceptedErr;
                break;
            case FileAnalysisType.HeaderOnly: return errorObj.headerOnlyErr;
        }
    }

    if (!uniqueTypes.includes(REGIONS) && !uniqueTypes.includes(UWS) && tableIndices.length + imageIndices.length === 0) {
        if (getSelectedRows('HeaderOnly', summaryTblId, report, summaryModel)?.length) {
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
        if (acceptMocTables((acceptList)) && acceptDataLinkTables(acceptList) && !isDL && !isMocFits.valid
            && !acceptTableOrSpectrum(acceptList)) {
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
export const isUWS= (report) => report.fileFormat === 'UWS';

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

const hasExtName= (part) => Boolean(getTableHeaderFromAnalysis('EXTNAME', part));

function getExtMarker(part, index) {
    return getTableHeaderFromAnalysis('EXTNAME', part) ??
        getTableHeaderFromAnalysis('UTYPE', part) ??
        index;
}

function sendTableRequest(tableIndices, fileCacheKey, treatAsSpectrum, currentReport, loadToUI= true, metaData={}) {
    const {fileName, parts=[]} = currentReport;

    tableIndices.forEach((idx) => {
        const {index} = parts[idx];
        const fileRoot= fileName?.split('.')?.[0] ?? fileName;
        const extMarker= getExtMarker(parts[idx], idx);
        const title = parts.length > 1
            ? hasExtName(parts[idx])
                ? `${extMarker}:${fileRoot}`
                : `${fileRoot}:${extMarker}` : fileRoot;
        const META_INFO= {...metaData};
        if (treatAsSpectrum) META_INFO[MetaConst.DATA_TYPE_HINT]= 'spectrum';
        const options=  {META_INFO};
        const tblReq = makeFileRequest(title, fileCacheKey, null, options);
        if (currentReport.parts[idx]?.searchProcessorId) tblReq.id= currentReport.parts[idx].searchProcessorId;
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

export function getSelectedRows(type, summaryTblId, report, currentSummaryModel, singleAxisImageAsTable=false) {

    const singleAxisIdxs= singleAxisImageAsTable ? findSingleAxisImages(report).map( ({index}) => index) : [];

    let retRows= [];
    if (getPartCnt(report)===1) {
        if (type===getFirstPartType(currentSummaryModel)) retRows= [0];
    }
    else {
        const {totalRows=0, tableData} = getSelectedDataSync(summaryTblId, ['Index', 'Type']);
        if (totalRows>0) {
            retRows= tableData.data.filter((row) => row[1] === type)            // take only rows with the right type
                .map((row) => row[0]);                       // returns only the index
        }
    }
    if (!singleAxisIdxs.length) return retRows;

    if (type===FileAnalysisType.Table) {
        retRows.push(...singleAxisIdxs);
    }
    else if (type===FileAnalysisType.Image){
        retRows= retRows.filter( (idx) => !singleAxisIdxs.includes(idx));
    }
    return retRows;
}
