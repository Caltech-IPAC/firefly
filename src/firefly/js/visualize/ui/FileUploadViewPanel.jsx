/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect, useState} from 'react';
import shallowequal from 'shallowequal';
import SplitPane from 'react-split-pane';
import {get} from 'lodash';


import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {getFieldVal, getField} from '../../fieldGroup/FieldGroupUtils';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getCellValue, getTblById, uniqueTblId, getSelectedDataSync} from '../../tables/TableUtil.js';
import {dispatchTableSearch, dispatchTableFetch} from '../../tables/TablesCntlr.js';
import {getSizeAsString} from '../../util/WebUtil.js';


import {makeFileRequest} from '../../tables/TableRequestUtil.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {getAViewFromMultiView, getMultiViewRoot, IMAGE} from '../MultiViewCntlr.js';
import WebPlotRequest from '../WebPlotRequest.js';
import ImagePlotCntlr, {dispatchPlotImage, visRoot} from '../ImagePlotCntlr.js';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {WorkspaceUpload} from '../../ui/WorkspaceViewer.jsx';
import {isAccessWorkspace, getWorkspaceConfig} from '../WorkspaceCntlr.js';
import {getAppHiPSForMoc} from '../HiPSMocUtil.js';
import {primePlot, getDrawLayersByType, getPlotViewAry} from '../PlotViewUtil.js';
import {getDlAry, dispatchCreateImageLineBasedFootprintLayer, dispatchCreateRegionLayer} from '../DrawLayerCntlr.js';
import LSSTFootprint from '../../drawingLayers/ImageLineBasedFootprint.js';
import {isMOCFitsFromUploadAnalsysis} from '../HiPSMocUtil.js';
import {isLsstFootprintTable} from '../task/LSSTFootprintTask.js';
import {getComponentState, dispatchComponentStateChange} from '../../core/ComponentCntlr.js';

import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {PlotAttribute} from '../PlotAttribute.js';
import {MetaConst} from '../../data/MetaConst.js';

import './FileUploadViewPanel.css';
import {getIntHeader} from '../../metaConvert/PartAnalyzer';
import {FileAnalysisType, Format} from '../../data/FileAnalysis';
import {dispatchValueChange} from 'firefly/fieldGroup/FieldGroupCntlr.js';
import {CompleteButton,NONE} from 'firefly/ui/CompleteButton.jsx';
import {createNewRegionLayerId} from 'firefly/drawingLayers/RegionPlot.js';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from 'firefly/core/MasterSaga.js';


export const panelKey = 'FileUploadAnalysis';
const  FILE_ID = 'fileUpload';
const  URL_ID = 'urlUpload';
const  WS_ID = 'wsUpload';

const SUMMARY_TBL_ID = 'AnalysisTable';
const DETAILS_TBL_ID = 'AnalysisTable-Details';
const UNKNOWN_FORMAT = 'UNKNOWN';
const summaryUiId = SUMMARY_TBL_ID + '-UI';
const detailsUiId = DETAILS_TBL_ID + '-UI';

const SUPPORTED_TYPES=[
    FileAnalysisType.REGION,
    FileAnalysisType.Image,
    FileAnalysisType.Table,
    FileAnalysisType.Spectrum,
    FileAnalysisType.REGION,
];

const TABLES_ONLY_SUPPORTED_TYPES=[
    FileAnalysisType.Table,
];


const isTablesOnly= () => getAppOptions()?.uploadPanelLimit==='tablesOnly';


const uploadOptions = 'uploadOptions';

let currentAnalysisResult, currentReport, currentSummaryModel, currentDetailsModel;

export function FileUploadViewPanel() {

    const [{isLoading,statusKey}, isWsUpdating, uploadSrc, {message, analysisResult, report, summaryModel, detailsModel}] =
        useStoreConnector.bind({comparator: shallowequal}) (
            () => getComponentState(panelKey, {isLoading:false,statusKey:''}),
            () => isAccessWorkspace(),
            () => getFieldVal(panelKey, uploadOptions),
            () => getNextState()
        );

    const [loadingMsg,setLoadingMsg]= useState(() => '');

    useEffect(() => {
        if (message || (analysisResult && analysisResult !== currentAnalysisResult)) {
            if (currentAnalysisResult !== analysisResult) {
                currentAnalysisResult = analysisResult;
            }
            dispatchComponentStateChange(panelKey, {isLoading: false});
        }
    });

    let aWStatusKey;
    useEffect(() => {
        if (isLoading) {
            if (statusKey) {
                aWStatusKey= statusKey;
                aWStatusKey && dispatchCancelActionWatcher(aWStatusKey);

                const watchForUploadUpdate= ({payload}) => {
                    payload.requestKey===statusKey && setLoadingMsg(payload.message);
                };
                dispatchAddActionWatcher({ id: statusKey, actions:[ImagePlotCntlr.PLOT_PROGRESS_UPDATE],
                    callback:watchForUploadUpdate, params:{statusKey}});
            }
        }
        else {
            setLoadingMsg('');
            dispatchCancelActionWatcher(aWStatusKey);
        }
        return (() => {
            aWStatusKey && dispatchCancelActionWatcher(aWStatusKey);
        });
        }, [isLoading, statusKey] );

    const tablesOnly= isTablesOnly();

    const workspace = getWorkspaceConfig();
    const uploadMethod = [{value: FILE_ID, label: 'Upload file'},
        {value: URL_ID, label: 'Upload from URL'}
    ].concat(workspace ? [{value: WS_ID, label: 'Upload from workspace'}] : []);


    const clearReport= () => {
        currentReport= undefined;
        dispatchValueChange({fieldKey:getLoadingFieldName(), groupKey:panelKey, value:'', analysisResult:undefined});
    };

    return (
        <div style={{position: 'relative', height: '100%', display: 'flex', alignItems: 'stretch',
            flexDirection: 'column' }}>
            <FieldGroup groupKey={panelKey} keepState={true} style={{height:'100%',
                display: 'flex', alignItems: 'stretch', flexDirection: 'column'}}>
                <div className='FileUpload'>
                    <div className='FileUpload__input'>
                        <RadioGroupInputField
                            initialState={{value: uploadMethod[0].value}}
                            fieldKey={uploadOptions}
                            alignment={'horizontal'}
                            options={uploadMethod}
                            wrapperStyle={{fontWeight: 'bold', fontSize: 12}}/>
                            <div style={{paddingTop: '10px', display:'flex', flexDirection:'row', justifyContent:'space-between'}}>
                                <UploadOptions {...{uploadSrc, isLoading, isWsUpdating}}/>
                                {report && <CompleteButton text='Clear File' groupKey={NONE} onSuccess={() =>clearReport()}/> }
                            </div>
                    </div>
                    <FileAnalysis {...{report, summaryModel, detailsModel,tablesOnly}}/>
                    <ImageDisplayOption/>
                </div>
                {(isLoading) && <LoadingMessage message={loadingMsg}/>}
            </FieldGroup>
        </div>
    );
}

const LoadingMessage= ({message}) => (
    <div style={{
        position: 'absolute',
        display:'flex',
        flexDirection: 'column',
        justifyContent:'center',
        background: 'rgba(0,0,0,.25)',
        alignItems: 'center',
        top:1, bottom:5, left:1, right:1 }}>
        <div style={{width:30, height:30}} className='loading-animation' />
        <div style={{
            alignSelf:'center', fontSize:'14pt', padding: 8, marginTop:15,
            backgroundColor: 'rgba(255,255,255,.8)', borderRadius:8}}>
            {message}
        </div>
    </div>
);

export function resultFail() {
    showInfoPopup('One or more fields are invalid', 'Validation Error');
}

const getPartCnt= () => currentReport?.parts?.length ?? 1;
const getFirstPartType= () => currentSummaryModel?.tableData.data[0]?.[1];
const getFileFormat= () => currentReport?.fileFormat;
const isRegion= () => getFirstPartType()===FileAnalysisType.REGION;

function isSinglePartFileSupported() {
    const supportedTypes= isTablesOnly() ? TABLES_ONLY_SUPPORTED_TYPES : SUPPORTED_TYPES;
    return getFirstPartType() && (supportedTypes.includes(getFirstPartType()));
}

function isFileSupported() {
    return getFirstPartType() && (SUPPORTED_TYPES.includes(getFirstPartType()) || getFileFormat()===Format.FITS);
}


function getFirstExtWithData(parts) {
    return isTablesOnly() ?
        parts.findIndex((p) => p.type.includes(FileAnalysisType.Table)) :
        parts.findIndex((p) => !p.type.includes(FileAnalysisType.HeaderOnly));
}



function tablesOnlyResultSuccess() {
    const tableIndices = getSelectedRows(FileAnalysisType.Table);
    const imageIndices = getSelectedRows(FileAnalysisType.Image);

    if (tableIndices.length>0) {
        imageIndices.length>0 && showInfoPopup('Only loading the tables, ignoring the images.');
        sendTableRequest(tableIndices, getFileCacheKey());
        return true;
    }
    else {
        showInfoPopup('You may only upload tables.');
        return false;
    }
}


export function resultSuccess(request) {
    if (isTablesOnly()) return tablesOnlyResultSuccess();
    const fileCacheKey = getFileCacheKey();

    const tableIndices = getSelectedRows(FileAnalysisType.Table);
    const imageIndices = getSelectedRows(FileAnalysisType.Image);

    if (!isFileSupported()) {
        showInfoPopup(`File type of ${getFirstPartType()} is not supported.`);
        return false;
    }

    if (!isRegion() && tableIndices.length + imageIndices.length === 0) {
        if (getSelectedRows('HeaderOnly')?.length) {
            showInfoPopup('FITS HDU type of HeaderOnly is not supported. A header-only HDU contains no additional data.', 'Validation Error');
        }
        else {
            showInfoPopup('No extension is selected', 'Validation Error');
        }
        return false;
    }

    const isMocFits =  isMOCFitsFromUploadAnalsysis(currentReport);
    if (isRegion()) {
        sendRegionRequest(fileCacheKey);
    }
    else if (isMocFits.valid) {
        sendTableRequest(tableIndices, fileCacheKey, {[MetaConst.PREFERRED_HIPS]: getAppHiPSForMoc()}, false);
    } else if ( isLsstFootprintTable(currentDetailsModel) ) {
        sendLSSTFootprintRequest(fileCacheKey, request.fileName, tableIndices[0]);
    } else {
        sendTableRequest(tableIndices, fileCacheKey);
        sendImageRequest(imageIndices, request, fileCacheKey);
    }
}

/*-----------------------------------------------------------------------------------------*/

const getLoadingFieldName= () => getFieldVal(panelKey, uploadOptions) || FILE_ID;

function getNextState() {

    // because this value is stored in different fields.. so we have to check on what options were selected to determine the active value
    const fieldState = getField(panelKey, getLoadingFieldName()) || {};
    const {analysisResult, message} = fieldState;
    let modelToUseForDetails= getTblById(SUMMARY_TBL_ID)?? currentSummaryModel;

    if (message) {
        return {message, report:undefined, summaryModel:undefined, detailsModel:undefined};
    } else  if (analysisResult) {
        if (analysisResult && analysisResult !== currentAnalysisResult) {
            currentReport = JSON.parse(analysisResult);

            const columns = [
                {name: 'Index', type: 'int', desc: 'Extension Index'},
                {name: 'Type', type: 'char', desc: 'Data Type'},
                {name: 'Description', type: 'char', desc: 'Extension Description'}
            ];
            const {parts=[]} = currentReport;
            const data = parts.map( (p) => {
                            const naxis= getIntHeader('NAXIS',p,0);
                            return [p.index, (naxis===1 && p.type===FileAnalysisType.Image)?FileAnalysisType.Table :p.type, p.desc];
                        });

            currentSummaryModel = {
                tbl_id: SUMMARY_TBL_ID,
                title: 'File Summary',
                totalRows: data.length,
                tableData: {columns, data}
            };
            modelToUseForDetails= currentSummaryModel;

            const firstExtWithData= getFirstExtWithData(parts);
            if (firstExtWithData >= 0) {
                const selectInfo = SelectInfo.newInstance({rowCount: data.length});
                selectInfo.setRowSelect(firstExtWithData, true);        // default select first extension/part with data
                currentSummaryModel.selectInfo = selectInfo.data;
                modelToUseForDetails.highlightedRow= firstExtWithData;
            }

        }
    }
    let detailsModel = getDetailsModel( modelToUseForDetails,currentReport);
    if (shallowequal(detailsModel, currentDetailsModel)) {
        detailsModel = currentDetailsModel;
    }
    currentDetailsModel = detailsModel;

    return {message, analysisResult, report:currentReport, summaryModel:currentSummaryModel, detailsModel};
}

function getDetailsModel(tableModel, report) {
    if (!tableModel) return;
    const {highlightedRow=0} = tableModel;
    const partNum = getCellValue(tableModel, highlightedRow, 'Index');
    const type = getCellValue(tableModel, highlightedRow, 'Type');
    if (type===UNKNOWN_FORMAT) return undefined;
    const details = report?.parts?.[partNum]?.details;
    if (details) details.tbl_id = DETAILS_TBL_ID;
    return details;
}

function getFileCacheKey() {
    // because this value is stored in different fields.. so we have to check on what options were selected to determine the active value
    const uploadSrc = getFieldVal(panelKey, uploadOptions) || FILE_ID;
    return getFieldVal(panelKey, uploadSrc);
}

function ImageDisplayOption() {

    const [selectedImages] = useStoreConnector(() => getSelectedRows('Image'));
    if ( selectedImages.length < 2) return null;

    const imgOptions = [{value: 'oneWindow', label: 'All images in one window'},
        {value: 'mulWindow', label: 'One extension image per window'}];
    return (
        <div style={{marginTop: 3}}>
            <RadioGroupInputField
                tooltip='display image extensions in one window or multiple windows'
                fieldKey='imageDisplay'
                options={imgOptions}
            />
        </div>
    );
}

function UploadOptions({uploadSrc=FILE_ID, isloading, isWsUpdating}) {

    const onLoading = (loading, statusKey) => {
        dispatchComponentStateChange(panelKey, {isLoading: loading, statusKey:loading?statusKey:''});
    };

    if (uploadSrc === FILE_ID) {
        return (
            <FileUpload
                innerStyle={{width: 90}}
                fileNameStyle={{marginLeft: 5, fontSize: 12}}
                fieldKey={FILE_ID}
                fileAnalysis={onLoading}
                tooltip='Select a file with FITS, VOTABLE, CSV, TSV, or IPAC format'
            />
        );
    } else if (uploadSrc === URL_ID) {
        return (
            <FileUpload
                innerStyle={{width: 300}}
                fieldKey={URL_ID}
                fileAnalysis={onLoading}
                isFromURL={true}
                label='Enter URL of a file:'
                tooltip='Select a URL with file in FITS, VOTABLE, CSV, TSV, or IPAC format'
            />
        );
    } else if (uploadSrc === WS_ID) {
        return (
            <WorkspaceUpload
                wrapperStyle={{marginRight: 32}}
                preloadWsFile={true}
                fieldKey={WS_ID}
                isLoading={isloading || isWsUpdating}
                fileAnalysis={onLoading}
                tooltip='Select a file in FITS, VOTABLE, CSV, TSV, or IPAC format from workspace'
            />
        );
    }
    return null;
}

function AnalysisInfo({report,supported=true}) {
    const partDesc = report.fileFormat === 'FITS' ? 'Extensions:' :
                     report.fileFormat === UNKNOWN_FORMAT ? '' : 'Parts:';
    const partCnt= report?.parts?.length ?? 1;
    return (
        <div className='FileUpload__headers'>
            <div className='keyword-label'>Format:</div>  <div className='keyword-value'>{report.fileFormat}</div>
            <div className='keyword-label'>Size:</div>  <div className='keyword-value'>{getSizeAsString(report.fileSize)}</div>
            {partCnt>1 && <div className='keyword-label'>{partDesc}</div>}
            {partCnt>1 &&<div className='keyword-value'>{partCnt}</div> }
            {!supported && <div style={{color:'red', fontSize:'larger'}}>{`File type of ${getFirstPartType()} is not supported`}</div>}
        </div>
    );
}

const tblOptions = {showToolbar:false, border:false, showOptionButton: false, showFilters: true};

function AnalysisTable({summaryModel, detailsModel, report}) {
    if (!summaryModel) return null;

    // Details table need to render first to create a stub to collect data when Summary table is loaded.
    return (
        <div className='FileUpload__summary'>
            {(summaryModel.tableData.data.length>1) ?
                <MultiDataSet summaryModel={summaryModel} detailsModel={detailsModel}/> :
                <SingleDataSet type={summaryModel.tableData.data[0][1]} desc={summaryModel.tableData.data[0][2]}
                               detailsModel={detailsModel} report={report}/>
            }
        </div>
    );
}

function SingleDataSet({type, desc, detailsModel, report, supported=isSinglePartFileSupported()}) {
    const showDetails= supported && detailsModel;
    return (
        <div style={{display:'flex', flex:'1 1 auto', justifyContent: showDetails?'start':'center'}}>
            <div style={{padding:'30px 20px 0 0'}}>
                <div style={{whiteSpace:'nowrap', fontSize:'larger', fontWeight:'bold', paddingBottom:40}}>
                    {type}{desc ? ` - ${desc}` : ''}
                </div>
                <AnalysisInfo report={report} supported={supported} />
                <div style={{paddingTop:15}}>No other detail about this file</div>
            </div>
            {  showDetails && <Details detailsModel={detailsModel}/>}
        </div>
    );
}

function MultiDataSet({summaryModel, detailsModel}) {
    return (
        <SplitPane split='vertical' maxSize={-20} minSize={20} defaultSize={350}>
            <TablePanel showTypes={false} title='File Summary' tableModel={summaryModel} tbl_ui_id={summaryUiId} {...tblOptions} />
            <Details detailsModel={detailsModel}/>
        </SplitPane>
    );
}


function Details({detailsModel}) {
    if (!detailsModel) return <div className='FileUpload__noDetails'>Details not available</div>;

    return (
        <TablePanel showTypes={false}  title='File Details'
                    tableModel={detailsModel} tbl_ui_id={detailsUiId}
                    {...tblOptions} showMetaInfo={true} selectable={false}/>
    );

}


function getTableArea(report, summaryModel, detailsModel) {
    if (report?.fileFormat === UNKNOWN_FORMAT) {
        return (
            <div style={{flexGrow: 1, marginTop: 40, textAlign:'center', fontSize: 'larger', color: 'red'}}>
                Error: Unrecognized Format
            </div>
        );
    }
    return <AnalysisTable {...{summaryModel, detailsModel, report}} />;
}


const FileAnalysis = React.memo( ({report, summaryModel, detailsModel, tablesOnly}) => {
    if (report) {
        return (
            <div className='FileUpload__report'>
                {summaryModel.tableData.data.length>1 && <AnalysisInfo report={report} />}
                {getTableArea(report, summaryModel, detailsModel)}
            </div>
        );
    }
    else {
        const liStyle= {listStyleType:'circle'};
        return (<div style={{color:'gray', margin:'20px 0 0 200px', fontSize:'larger', lineHeight:'1.3em'}}>
            You can load any of the following types of files:
            <ul>
                <li style={liStyle}>Custom catalog in IPAC, CSV, TSV, VOTABLE, or FITS table format</li>
                {!tablesOnly && <li style={liStyle}>Any FITS file with tables or images (including multiple HDUs)</li>}
                {!tablesOnly && <li style={liStyle}>A Region file</li> }
                {!tablesOnly && <li style={liStyle}>A MOC FITS file</li> }
            </ul>
        </div>);

    }
});





function getSelectedRows(type) {
    if (getPartCnt()===1) {
        if (type===getFirstPartType()) {
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



function sendRegionRequest(fileCacheKey) {
    const drawLayerId = createNewRegionLayerId();
    const title= currentReport.fileName ?? 'Region File';
    dispatchCreateRegionLayer(drawLayerId, title, fileCacheKey, null);
    if (!getPlotViewAry(visRoot())?.length) {
        showInfoPopup('The region file is loaded but you will not be able to see it until you load an image (FITS or HiPS)', 'Warning');
    }
}

function sendTableRequest(tableIndices, fileCacheKey, metaData, loadToUI= true) {
    const {fileName, parts=[]} = currentReport;

    tableIndices.forEach((idx) => {
        const {index} = parts[idx];
        const title = parts.length > 1 ? `${fileName}-${index}` : fileName;
        const options=  metaData && { META_INFO: metaData};
        const tblReq = makeFileRequest(title, fileCacheKey, null, options);
        tblReq.tbl_index = index;
        loadToUI ? dispatchTableSearch(tblReq) : dispatchTableFetch(tblReq);

    });
}

/**
 * send request to get the data of image unit, the extension index is mapped to be that at
 * the server side
 * @param imageIndices
 * @param request
 * @param fileCacheKey
 */
function sendImageRequest(imageIndices, request, fileCacheKey) {

    if (imageIndices.length === 0) return;

    const {fileName, parts=[]} = currentReport;

    const wpRequest = WebPlotRequest.makeFilePlotRequest(fileCacheKey);
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
            const extList = imageIndices.map((idx) => get(parts, [idx, 'index'])).join();

            wpRequest.setMultiImageExts(extList);
            if (!extList.includes('-1')) wpRequest.setAttributes({[PlotAttribute.POST_TITLE]:`- ext. ${extList}`});

            const plotId = `${fileName.replace('.', '_')}-${imageIndices.join('_')}`;
            dispatchPlotImage({plotId, wpRequest, viewerId});
        }
    }
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
