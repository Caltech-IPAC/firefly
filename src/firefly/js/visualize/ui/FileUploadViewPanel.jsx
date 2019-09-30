/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import shallowequal from 'shallowequal';
import SplitPane from 'react-split-pane';
import {get} from 'lodash';


import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {getFieldVal, getField} from '../../fieldGroup/FieldGroupUtils';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getCellValue, getTblById, uniqueTblId, getSelectedDataSync} from '../../tables/TableUtil.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {dispatchAddActionWatcher} from '../../core/MasterSaga.js';
import {getSizeAsString} from '../../util/WebUtil.js';


import {makeFileRequest} from '../../tables/TableRequestUtil.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {getAViewFromMultiView, getMultiViewRoot, IMAGE} from '../MultiViewCntlr.js';
import WebPlotRequest from '../WebPlotRequest.js';
import {dispatchPlotImage, visRoot, dispatchPlotHiPS} from '../ImagePlotCntlr.js';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {WorkspaceUpload} from '../../ui/WorkspaceViewer.jsx';
import {isAccessWorkspace, getWorkspaceConfig} from '../WorkspaceCntlr.js';
import {getAppHiPSForMoc, addNewMocLayer} from '../HiPSMocUtil.js';
import {primePlot, getDrawLayerById, getDrawLayersByType} from '../PlotViewUtil.js';
import {genHiPSPlotId} from './ImageSearchPanelV2.jsx';
import DrawLayerCntlr, {dispatchAttachLayerToPlot, dlRoot, getDlAry, dispatchCreateImageLineBasedFootprintLayer} from '../DrawLayerCntlr.js';
import HiPSMOC from '../../drawingLayers/HiPSMOC.js';
import LSSTFootprint from '../../drawingLayers/ImageLineBasedFootprint.js';
import {isMOCFitsFromUploadAnalsysis, MOCInfo, UNIQCOL} from '../HiPSMocUtil.js';
import {isLsstFootprintTable} from '../task/LSSTFootprintTask.js';
import {getComponentState, dispatchComponentStateChange} from '../../core/ComponentCntlr.js';

import {useStoreConnector} from '../../ui/SimpleComponent.jsx';

import './FileUploadViewPanel.css';
import {PlotAttribute} from '../PlotAttribute';


export const panelKey = 'FileUploadAnalysis';
const  fileId = 'fileUpload';
const  urlId = 'urlUpload';
const  wsId = 'wsUpload';

const summaryTblId = 'AnalysisTable';
const detailsTblId = 'AnalysisTable-Details';
const summaryUiId = summaryTblId + '-UI';
const detailsUiId = detailsTblId + '-UI';
const unknownFormat = 'UNKNOWN';

const uploadOptions = 'uploadOptions';

let currentAnalysisResult, currentReport, currentSummaryModel, currentDetailsModel;

export function FileUploadViewPanel() {

    const [isLoading, isWsUpdating, uploadSrc, {message, analysisResult, report, summaryModel, detailsModel}] = useStoreConnector.bind({comparator: shallowequal}) (
        () => get(getComponentState(panelKey), 'isLoading'),
        () => isAccessWorkspace(),
        () => getFieldVal(panelKey, uploadOptions),
        () => getNextState()
    );

    useEffect(() => {
        if (message || (analysisResult && analysisResult !== currentAnalysisResult)) {
            if (currentAnalysisResult !== analysisResult) {
                currentAnalysisResult = analysisResult;
            }
            dispatchComponentStateChange(panelKey, {isLoading: false});
        }
    });

    const workspace = getWorkspaceConfig();
    const uploadMethod = [{value: fileId, label: 'Upload file'},
        {value: urlId, label: 'Upload from URL'}
    ].concat(workspace ? [{value: wsId, label: 'Upload from workspace'}] : []);


    return (
        <div style={{position: 'relative'}}>
            <FieldGroup groupKey={panelKey} keepState={true}>
                <div className='FileUpload'>
                    <div className='FileUpload__input'>
                        <RadioGroupInputField
                            initialState={{value: uploadMethod[0].value}}
                            fieldKey={uploadOptions}
                            alignment={'horizontal'}
                            options={uploadMethod}
                            wrapperStyle={{fontWeight: 'bold', fontSize: 12}}/>
                        <UploadOptions {...{uploadSrc, isLoading, isWsUpdating}}/>
                    </div>
                    <FileAnalysis {...{report, summaryModel, detailsModel}}/>
                    <ImageDisplayOption/>
                </div>
            </FieldGroup>
            {isLoading && <div style={{top: 1}} className='loading-mask'/>}
        </div>
    );
}

export function resultFail() {
    showInfoPopup('One or more fields are invalid', 'Validation Error');
};

export function resultSuccess(request) {
    const fileCacheKey = getFileCacheKey();

    const tableIndices = getSelectedRows('Table');
    const imageIndices = getSelectedRows('Image');

    if (tableIndices.length + imageIndices.length === 0) {
        showInfoPopup('no extenstion is selected', 'Validation Error');
        return false;
    }

    const isMocFits =  isMOCFitsFromUploadAnalsysis(currentReport);
    if (isMocFits.valid) {
        sendMocRequest(fileCacheKey, currentReport.fileName, isMocFits);
    } else if ( isLsstFootprintTable(currentDetailsModel) ) {
        sendLSSTFootprintRequest(fileCacheKey, request.fileName, tableIndices[0]);
    } else {
        sendTableRequest(tableIndices, fileCacheKey);
        sendImageRequest(imageIndices, request, fileCacheKey);
    }
}

/*-----------------------------------------------------------------------------------------*/

function getNextState() {

    // because this value is stored in different fields.. so we have to check on what options were selected to determine the active value
    const uploadSrc = getFieldVal(panelKey, uploadOptions) || fileId;
    const fieldState = getField(panelKey, uploadSrc) || {};
    const {analysisResult, message} = fieldState;

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
                            return [p.index, p.type, p.desc];
                        });

            currentSummaryModel = {
                tbl_id: summaryTblId,
                title: 'File Summary',
                totalRows: data.length,
                tableData: {columns, data}
            };

            const firstExtWithData = parts.findIndex((p) => !p.type.includes('HeaderOnly'));
            if (firstExtWithData >= 0) {
                const selectInfo = SelectInfo.newInstance({rowCount: data.length});
                selectInfo.setRowSelect(firstExtWithData, true);        // default select first extension/part with data
                currentSummaryModel.selectInfo = selectInfo.data;
            }

        }
    }
    let detailsModel = getDetailsModel();
    if (shallowequal(detailsModel, currentDetailsModel)) {
        detailsModel = currentDetailsModel;
    }
    currentDetailsModel = detailsModel;

    return {message, analysisResult, report:currentReport, summaryModel:currentSummaryModel, detailsModel};
}

function getDetailsModel() {
    const tableModel = getTblById(summaryTblId);
    if (tableModel) {
        const {highlightedRow=0} = tableModel;
        const partNum = getCellValue(tableModel, highlightedRow, 'Index');
        const type = getCellValue(tableModel, highlightedRow, 'Type');
        let details = get(currentReport, ['parts', partNum, 'details'], {});
        details.tbl_id = detailsTblId || '';
        if (type === unknownFormat) {
            details = undefined;
        }
        return details;
    }
}

function getFileCacheKey() {
    // because this value is stored in different fields.. so we have to check on what options were selected to determine the active value
    const uploadSrc = getFieldVal(panelKey, uploadOptions) || fileId;
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

function UploadOptions({uploadSrc=fileId, isloading, isWsUpdating}) {

    const onLoading = () => dispatchComponentStateChange(panelKey, {isLoading: true});

    if (uploadSrc === fileId) {
        return (
            <FileUpload
                innerStyle={{width: 80}}
                fileNameStyle={{marginLeft: 5, fontSize: 12}}
                fieldKey={fileId}
                fileAnalysis={onLoading}
                tooltip='Select a file with FITS, VOTABLE, CSV, TSV, or IPAC format'
            />
        );
    } else if (uploadSrc === urlId) {
        return (
            <FileUpload
                innerStyle={{width: 300}}
                fieldKey={urlId}
                fileAnalysis={onLoading}
                isFromURL={true}
                label='Enter URL of a file:'
                tooltip='Select a URL with file in FITS, VOTABLE, CSV, TSV, or IPAC format'
            />
        );
    } else if (uploadSrc === wsId) {
        return (
            <WorkspaceUpload
                wrapperStyle={{marginRight: 32}}
                preloadWsFile={true}
                fieldKey={wsId}
                isLoading={isloading || isWsUpdating}
                fileAnalysis={onLoading}
                tooltip='Select a file in FITS, VOTABLE, CSV, TSV, or IPAC format from workspace'
            />
        );
    }
    return null;
}

function AnalysisInfo({report}) {
    const partDesc = report.fileFormat === 'FITS' ? 'Extensions:' :
                     report.fileFormat === unknownFormat ? '' : 'Parts:';
    return (
        <div className='FileUpload__headers'>
            <div className='keyword-label'>Format:</div>  <div className='keyword-value'>{report.fileFormat}</div>
            <div className='keyword-label'>Size:</div>  <div className='keyword-value'>{getSizeAsString(report.fileSize)} KB</div>
            <div className='keyword-label'>{partDesc}</div> <div className='keyword-value'>{get(report, 'parts.length')}</div>
        </div>
    );
}

function AnalysisTable({summaryModel, detailsModel}) {
    if (!summaryModel) return null;

    const tblOptions = {showToolbar:false, border:false, showOptionButton: false, showFilters: true};
    const details = ! detailsModel ? <div className='FileUpload__noDetails'>Details not available</div>
                    : <TablePanel title='File Details' tableModel={detailsModel} tbl_ui_id={detailsUiId} {...tblOptions} showMetaInfo={true} selectable={false}/>;

    // Details table need to render first to create a stub to collect data when Summary table is loaded.
    return (
        <div className='FileUpload__summary'>
            <SplitPane split='vertical' maxSize={-20} minSize={20} defaultSize={350}>
                <TablePanel title='File Summary' tableModel={summaryModel} tbl_ui_id={summaryUiId} {...tblOptions} />
                {details}
            </SplitPane>
        </div>
    );
}

const FileAnalysis = React.memo( ({report, summaryModel, detailsModel}) => {
    const isUnknownFormat = get(report, 'fileFormat') === unknownFormat;
    const tableArea = isUnknownFormat
                        ? <div style={{flexGrow: 1, marginTop: 40, fontSize: 'larger', color: 'red'}}>Unrecognized Format Error!</div>
                        : <AnalysisTable {...{summaryModel, detailsModel}} />;
    if (report) {
        return (
            <div className='FileUpload__report'>
                <AnalysisInfo report={report} />
                {tableArea}
            </div>
        );
    }
    return null;
});


function getSelectedRows(type) {
    const {totalRows=0, tableData} = getSelectedDataSync(summaryTblId, ['Index', 'Type']);
    if (totalRows === 0) return [];
    const selectedRows = tableData.data;
    return selectedRows.filter((row) => row[1] === type)            // take only rows with the right type
        .map((row) => row[0]);                       // returns only the index
}

function sendTableRequest(tableIndices, fileCacheKey) {
    const {fileName, parts=[]} = currentReport;

    tableIndices.forEach((idx) => {
        const {index} = parts[idx];
        const title = parts.length > 1 ? `${fileName}-${index}` : fileName;

        const tblReq = makeFileRequest(title, fileCacheKey, null);
        tblReq.tbl_index = index;
        dispatchTableSearch(tblReq);

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

function sendMocRequest(uploadPath, displayValue, mocFits) {

    const tblId = uniqueTblId();
    const pv = primePlot(visRoot());

    const overlayMocOnPlot = (plotId) => {
        const dl = addNewMocLayer(tblId, uploadPath, null, get(mocFits, [MOCInfo, UNIQCOL]));
        if (dl) {
            dispatchAttachLayerToPlot(dl.drawLayerId, plotId, true, true);
        }
    };

    if (!pv) {
        const pId = genHiPSPlotId.next().value;

        const watcher = (action, cancelSelf) => {
            const {plotIdAry, plotId, drawLayerId} = action.payload;
            const dl = getDrawLayerById(dlRoot(), drawLayerId);

            // after hips moc of default HiPS is attached to the plot
            if ((dl.drawLayerTypeId === HiPSMOC.TYPE_ID) &&
                (drawLayerId !== tblId) && ((plotIdAry && plotIdAry[0] === pId) || (plotId && plotId === pId))) {
                overlayMocOnPlot(pId);
                cancelSelf();
            }
        };

        const hipsUrl = getAppHiPSForMoc();
        showInfoPopup('There is no image in the viewer. The MOC will be shown on top of the HiPS image from \''
            + hipsUrl +'\'', 'MOC fits search info');

        dispatchAddActionWatcher({actions: [DrawLayerCntlr.ATTACH_LAYER_TO_PLOT], callback: watcher});

        const wpRequest = WebPlotRequest.makeHiPSRequest(hipsUrl);
        const {viewerId=''} = getAViewFromMultiView(getMultiViewRoot(), IMAGE) || {};

        wpRequest.setPlotGroupId(viewerId);
        wpRequest.setPlotId(pId);
        wpRequest && dispatchPlotHiPS({plotId: pId, viewerId, wpRequest});
    } else {
        overlayMocOnPlot(pv.plotId);
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
