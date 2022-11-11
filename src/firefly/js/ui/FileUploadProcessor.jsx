import {FileAnalysisType, Format} from 'firefly/data/FileAnalysis';
import {showInfoPopup} from 'firefly/ui/PopupUtil';
import {getAppHiPSForMoc, isMOCFitsFromUploadAnalsysis} from 'firefly/visualize/HiPSMocUtil';
import {MetaConst} from 'firefly/data/MetaConst';
import {isLsstFootprintTable} from 'firefly/visualize/task/LSSTFootprintTask';
import {makeFileRequest} from 'firefly/tables/TableRequestUtil';
import {getAppOptions} from 'firefly/core/AppDataCntlr';
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

const FILE_ID = 'fileUpload';
const uploadOptions = 'uploadOptions';

const SUPPORTED_TYPES=[
    FileAnalysisType.REGION,
    FileAnalysisType.Image,
    FileAnalysisType.Table,
    FileAnalysisType.Spectrum,
    FileAnalysisType.REGION,
];

export function resultSuccess(request) {

    const currentReport = request.additionalParams?.currentReport;
    const currentDetailsModel = request.additionalParams?.currentDetailsModel;
    const summaryModel = request.additionalParams?.summaryModel;
    const groupKey = request.additionalParams?.groupKey;
    const SUMMARY_TBL_ID = groupKey; //FileUploadAnalysis

    const isTablesOnly= () => getAppOptions()?.uploadPanelLimit==='tablesOnly';
    if (isTablesOnly()) return tablesOnlyResultSuccess(request, SUMMARY_TBL_ID, currentReport, summaryModel, groupKey);
    const fileCacheKey = getFileCacheKey(groupKey);

    const tableIndices = getSelectedRows(FileAnalysisType.Table, SUMMARY_TBL_ID, currentReport, summaryModel);
    const imageIndices = getSelectedRows(FileAnalysisType.Image, SUMMARY_TBL_ID, currentReport, summaryModel);

    if (!isFileSupported(summaryModel, currentReport)) {
        showInfoPopup(getFirstPartType(summaryModel) ? `File type of ${getFirstPartType(summaryModel)} is not supported.`: 'Could not recognize the file type');
        return false;
    }

    if (!isRegion(summaryModel) && tableIndices.length + imageIndices.length === 0) {
        if (getSelectedRows('HeaderOnly', SUMMARY_TBL_ID, currentReport, summaryModel)?.length) {
            showInfoPopup('FITS HDU type of HeaderOnly is not supported. A header-only HDU contains no additional data.', 'Validation Error');
        }
        else {
            showInfoPopup('No extension is selected', 'Validation Error');
        }
        return false;
    }

    const isMocFits =  isMOCFitsFromUploadAnalsysis(currentReport);
    if (isRegion(summaryModel)) {
        sendRegionRequest(fileCacheKey, currentReport);
    }
    else if (isMocFits.valid) {
        const mocMeta= {[MetaConst.PREFERRED_HIPS]: getAppHiPSForMoc()};
        if (request.mocOp==='table') mocMeta[MetaConst.IGNORE_MOC]='true';
        //loadToUI = true if request.mocOp==='table', else loadToUI=false
        sendTableRequest(tableIndices, fileCacheKey, Boolean(request.tablesAsSpectrum==='spectrum'), currentReport, Boolean(request.mocOp==='table'), mocMeta);
    } else if ( isLsstFootprintTable(currentDetailsModel) ) {
        sendLSSTFootprintRequest(fileCacheKey, request.fileName, tableIndices[0]);
    } else {
        sendTableRequest(tableIndices, fileCacheKey, Boolean(request.tablesAsSpectrum==='spectrum'), currentReport);
        sendImageRequest(imageIndices, request, fileCacheKey, currentReport);
    }
}

function tablesOnlyResultSuccess(request, SUMMARY_TBL_ID, currentReport, currentSummaryModel, groupKey) {
    const tableIndices = getSelectedRows(FileAnalysisType.Table, SUMMARY_TBL_ID, currentReport, currentSummaryModel);
    const imageIndices = getSelectedRows(FileAnalysisType.Image, SUMMARY_TBL_ID, currentReport, currentSummaryModel);

    if (tableIndices.length>0) {
        imageIndices.length>0 && showInfoPopup('Only loading the tables, ignoring the images.');
        sendTableRequest(tableIndices, getFileCacheKey(groupKey), Boolean(request.tablesAsSpectrum==='spectrum'), currentReport);
        return true;
    }
    else {
        showInfoPopup('You may only upload tables.');
        return false;
    }
}

function getFileCacheKey(groupKey) {
    // because this value is stored in different fields.. so we have to check on what options were selected to determine the active value
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
            if (!extList.includes('-1')) wpRequest.setAttributes({[PlotAttribute.POST_TITLE]:`- ext. ${extList}`});

            const plotId = `${fileName.replace('.', '_')}-${imageIndices.join('_')}`;
            dispatchPlotImage({plotId, wpRequest, viewerId});
        }
    }
}

export function getSelectedRows(type, SUMMARY_TBL_ID, currentReport, currentSummaryModel) {
    if (getPartCnt(currentReport)===1) {
        if (type===getFirstPartType(currentSummaryModel)) {
            return [0];
        }
        return [];
    }
    const {totalRows=0, tableData} = getSelectedDataSync(SUMMARY_TBL_ID, ['Index', 'Type']);
    if (totalRows === 0) return [];
    const selectedRows = tableData.data;
    return selectedRows.filter((row) => row[1] === type)            // take only rows with the right type
        .map((row) => row[0]);                       // returns only the index
}
