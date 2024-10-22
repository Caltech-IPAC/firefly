/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Chip, Stack, Typography} from '@mui/joy';
import React, {useContext, useEffect, useState} from 'react';
import shallowequal from 'shallowequal';
import SplitPane from 'react-split-pane';

import {FieldGroupCtx} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {getField} from '../../fieldGroup/FieldGroupUtils';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getCellValue, getTblById} from '../../tables/TableUtil.js';
import {dispatchTableAddLocal, dispatchTableRemove} from '../../tables/TablesCntlr.js';

import {isAnalysisTableDatalink} from '../../voAnalyzer/VoDataLinkServDef.js';
import {getSizeAsString} from '../../util/WebUtil.js';

import {SelectInfo} from '../../tables/SelectInfo.js';
import ImagePlotCntlr from '../ImagePlotCntlr.js';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {WorkspaceUpload} from '../../ui/WorkspaceViewer.jsx';
import {isAccessWorkspace, getWorkspaceConfig} from '../WorkspaceCntlr.js';
import {isMOCFitsFromUploadAnalsysis} from '../HiPSMocUtil.js';
import {isLsstFootprintTable} from '../task/LSSTFootprintTask.js';

import {useFieldGroupMetaState, useFieldGroupValue, useStoreConnector} from '../../ui/SimpleComponent.jsx';

import {getIntHeaderFromAnalysis} from '../../metaConvert/PartAnalyzer';
import {FileAnalysisType} from '../../data/FileAnalysis';
import {Format} from '../../data/FileAnalysis';
import {dispatchValueChange} from 'firefly/fieldGroup/FieldGroupCntlr.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from 'firefly/core/MasterSaga.js';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField.jsx';
import {
    findSingleAxisImages, getFileFormat, getFirstPartType, getSelectedRows, isRegion, isUWS
} from 'firefly/ui/FileUploadProcessor';
import {
    acceptAnyTables, acceptDataLinkTables, acceptImages, acceptMocTables, acceptNonDataLinkTables,
    acceptNonMocTables, acceptOnlyTables, acceptRegions, acceptTableOrSpectrum, acceptUWS, DATA_LINK_TABLES,
    IMAGES, REGIONS, SPECTRUM_TABLES, TABLES, UWS
} from 'firefly/ui/FileUploadUtil';
import {UwsJobInfo} from 'firefly/core/background/JobInfo';
import {uwsJobInfo} from 'firefly/rpc/SearchServicesJson';
export const  FILE_ID = 'fileUpload';
export const  URL_ID = 'urlUpload';
const  WS_ID = 'wsUpload';
const SINGLE_ROW_AS_TABLE= 'singleRowImageAsTable';

const TABLE_MSG = 'Custom catalog or table in IPAC, CSV, TSV, VOTABLE, Parquet, or FITS table format';
const REGION_MSG = 'A ds9 region file';
const IMAGE_MSG = 'Images in FITS format, including multi-extension FITS files with images, tables, or a mixture of both';
const MOC_MSG = 'A Multi-Order Coverage Map (MOC) in FITS format';
const DL_MSG = 'A Data Link Table file';
const UWS_MSG = 'A UWS job file';


const SUPPORTED_TYPES=[
    FileAnalysisType.REGION,
    FileAnalysisType.Image,
    FileAnalysisType.Table,
    FileAnalysisType.Spectrum,
    FileAnalysisType.UWS
];

const TABLES_ONLY_SUPPORTED_TYPES=[
    FileAnalysisType.Table,
];

const uploadOptions = 'uploadOptions';

const FILE_UPLOAD_KEY= 'file-upload-key-';
let keyCnt=0;

export function FileUploadViewPanel({setSubmitText, acceptList, acceptOneItem, externalDropEvent}) {

    const {groupKey, keepState}= useContext(FieldGroupCtx);

    const isWsUpdating          = useStoreConnector(() => isAccessWorkspace());
    const [getLoadingOp, setLoadingOp]= useFieldGroupValue(uploadOptions);
    const singleAxisImageAsTable= useFieldGroupValue(SINGLE_ROW_AS_TABLE)[0]()==='singleAxisImage';

    //dropEvent is used for files being dragged and drooped for uploads
    const [dropEvent, setDropEvent] = useState(() => externalDropEvent);

    const summaryTblId = groupKey;
    const detailsTblId = groupKey + '-Details';
    const UNKNOWN_FORMAT = 'UNKNOWN';

    const [getUploadMetaInfo, setUploadMetaInfo]= useFieldGroupMetaState({message:undefined, analysisResult: undefined,
        report: undefined, summaryModel: undefined, detailsModel: undefined, prevAnalysisResult: undefined}, groupKey);

    const {message, analysisResult, report, summaryModel, detailsModel, prevAnalysisResult, selectInfo} =
        useStoreConnector(() => {
            const loadingOp= getLoadingOp();
            const {analysisResult, message}= getField(groupKey, loadingOp) || {};
            const summaryTbl= getTblById(summaryTblId);
            return getNextState(summaryTblId, summaryTbl, detailsTblId, analysisResult, message,
                getUploadMetaInfo(), acceptList, acceptOneItem);
        });

    const {isLoading,statusKey} = getUploadMetaInfo();

    const [loadingMsg,setLoadingMsg]= useState(() => '');
    const [uploadKey,setUploadKey]= useState(() => FILE_UPLOAD_KEY+keyCnt);

    useEffect(() => {
        if (message || analysisResult) {
            setUploadMetaInfo({...getUploadMetaInfo(), prevAnalysisResult, report, summaryModel,
                detailsModel, analysisResult, message, selectInfo});
        }
    }, [message, analysisResult, report, summaryModel, detailsModel, prevAnalysisResult,selectInfo]);

    useEffect(() => {
        dispatchTableAddLocal(summaryModel, undefined, false);
        return (() => {
            if (!keepState) dispatchTableRemove(summaryTblId);
        });
    }, [summaryModel]);

    useEffect(() => {
        dispatchTableAddLocal(detailsModel, undefined, false);
        return (() => {
            if (!keepState) dispatchTableRemove(detailsTblId);
        });
    }, [detailsModel]);

    useEffect(() => {
        setSubmitText?.(getLoadButtonText(summaryTblId,report,detailsModel,summaryModel,acceptList,singleAxisImageAsTable));
    },[report,setSubmitText, summaryModel, detailsModel,selectInfo,singleAxisImageAsTable]);

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

    const workspace = getWorkspaceConfig();
    const uploadMethod = [{value: FILE_ID, label: 'Upload file'},
        {value: URL_ID, label: 'Upload from URL'}
    ].concat(workspace ? [{value: WS_ID, label: 'Upload from workspace'}] : []);

    const clearReport= () => {
        dispatchValueChange({fieldKey:getLoadingOp(), groupKey, value:'', displayValue:'', analysisResult:undefined});
    };

    const isMoc=  isMOCFitsFromUploadAnalsysis(report)?.valid;
    const isDatalink=  isAnalysisTableDatalink(report);

    return (
        <Stack {...{flexGrow:1, position: 'relative', height:1, alignItems: 'stretch'}}>
            <FileDropZone {...{dropEvent, setDropEvent, setLoadingOp}}>
                <Stack sx={{height:1, px:1,flexGrow:1}}>
                    <Box className='ff-FileUploadViewPanel-file' sx={{mt:1, ml:25}}>
                        <RadioGroupInputField
                            initialState={{value: uploadMethod[0].value}}
                            slotProps={{radio:{size:'md'}}}
                            fieldKey={uploadOptions}
                            orientation='horizontal'
                            options={uploadMethod} />
                        <Stack {...{pt: 1, direction:'row', justifyContent:'space-between'}}>
                            <UploadOptions {...{uploadSrc: dropEvent ? FILE_ID : getLoadingOp(), isLoading, isWsUpdating,  uploadKey, dropEvent, setDropEvent}}/>
                            <Stack>
                                {report && <Chip sx={{whiteSpace:'nowrap', ml:1}}
                                                 onClick={() =>{
                                                     clearReport();
                                                     keyCnt++;
                                                     setUploadKey(FILE_UPLOAD_KEY+keyCnt);
                                                 }}>
                                    Clear File
                                </Chip>
                                }
                            </Stack>
                        </Stack>
                    </Box>
                    <FileAnalysis {...{report, summaryModel, detailsModel, isMoc, UNKNOWN_FORMAT, acceptList,
                        isDatalink, acceptOneItem, summaryTblId}}/>
                    <ImageDisplayOption {...{summaryTblId, currentReport:report, currentSummaryModel:summaryModel, acceptList}}/>
                    <TableDisplayOption {...{isMoc, isDatalink, summaryTblId,
                        currentReport:report, currentSummaryModel:summaryModel, currentDetailsModel:detailsModel,
                        acceptList, acceptOneItem}}/>
                </Stack>
            </FileDropZone>
            {(isLoading) && <LoadingMessage message={loadingMsg}/>}
        </Stack>
    );
}

export function FileDropZone({dropEvent, setDropEvent, setLoadingOp, sx, children}) {
    const [isDragging, setIsDragging] = useState(false);

    useEffect(() => {
        if (dropEvent) {
             setLoadingOp('fileUpload');
        }
    }, [dropEvent]);

    // Prevent the default behavior for drag events to allow dropping files
    const handleDragEnter = (e) => {
        e.preventDefault();
        setIsDragging(true);
    };

    const handleDragLeave = () => {
        setIsDragging(false);
    };

    const handleDrop = (e) => {
        e.preventDefault();
        setDropEvent(e);
        setIsDragging(false);
    };

    return (
        <Stack
            direction='row'
            className={`file-drop-zone ${isDragging ? 'dragging' : ''}`}
            onDragEnter={handleDragEnter}
            onDragOver={handleDragEnter}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            sx={{flexGrow:1, height:1, width: 1,...sx}}>
            {children}
        </Stack>
    );
}

const LoadingMessage= ({message}) => (
    <div style={{
        position: 'absolute',
        zIndex:20,
        display:'flex',
        flexDirection: 'column',
        justifyContent:'center',
        background: 'rgba(0,0,0,.25)',
        alignItems: 'center',
        top:1, bottom:5, left:1, right:1 }}>
        <div style={{width:30, height:30}} className='loading-animation' />
        {Boolean(message?.trim()) &&
        <div style={{
            alignSelf:'center', fontSize:'14pt', padding: 8, marginTop:15,
            backgroundColor: 'rgba(255,255,255,.8)', borderRadius:8}}>
            {message}
        </div>}
    </div>
);

function getLoadButtonText(summaryTblId,currentReport,currentDetailsModel,currentSummaryModel,acceptList,singleAxisImageAsTable) {
    const tblCnt = getSelectedRows(FileAnalysisType.Table, summaryTblId, currentReport, currentSummaryModel, singleAxisImageAsTable)?.length ?? 0;
    if (tblCnt && isMOCFitsFromUploadAnalsysis(currentReport).valid && acceptMocTables(acceptList)) return 'Load MOC';
    if (isRegion(currentSummaryModel) && acceptRegions(acceptList)) return 'Load Region';

    const imgCnt = getSelectedRows(FileAnalysisType.Image, summaryTblId, currentReport, currentSummaryModel, singleAxisImageAsTable)?.length ?? 0;

    if (isLsstFootprintTable(currentDetailsModel) ) return 'Load Footprint';
    if ((tblCnt && !imgCnt && acceptAnyTables(acceptList)) || (tblCnt && imgCnt && !acceptImages(acceptList))) return tblCnt>1 ? `Load ${tblCnt} Tables` : 'Load Table';
    if ((!tblCnt && imgCnt && acceptImages(acceptList)) || (tblCnt && imgCnt && !acceptAnyTables(acceptList))) return imgCnt>1 ? `Load ${imgCnt} Images` : 'Load Image';
    if (tblCnt && imgCnt) {
        return `Load ${imgCnt > 1 ? imgCnt + ' ' : ''}Image${imgCnt > 1 ? 's' : ''} and ${tblCnt > 1 ? tblCnt + ' ' : ''}Table${tblCnt > 1 ? 's' : ''}`;
    }
    return  'Load';
}


export function resultFail() {
    showInfoPopup('One or more fields are invalid', 'Validation Error');
}


function isSinglePartFileSupported(currentSummaryModel, acceptList) {
    const supportedTypes = (acceptOnlyTables(acceptList)) ? TABLES_ONLY_SUPPORTED_TYPES : SUPPORTED_TYPES;
    return getFirstPartType(currentSummaryModel) && (supportedTypes.includes(getFirstPartType(currentSummaryModel)));
}


function getFirstExtWithData(parts, acceptList, summaryModel) {
    if (!parts || !summaryModel) return 0;
    if (acceptOnlyTables(acceptList)) {
        return summaryModel.tableData.data.findIndex((p) => p[1].includes(FileAnalysisType.Table));
    }
    if (!acceptImages(acceptList)) {
        return summaryModel.tableData.data.findIndex((p) => (!p[1].includes(FileAnalysisType.Image) && !p[1].includes(FileAnalysisType.HeaderOnly)));
    }
    if (!acceptAnyTables(acceptList)) {
        return summaryModel.tableData.data.findIndex((p) => (!p[1].includes(FileAnalysisType.Table) && !p[1].includes(FileAnalysisType.HeaderOnly)));
    }
    return summaryModel.tableData.data.findIndex((p) => !p[1].includes(FileAnalysisType.HeaderOnly));
}

/*-----------------------------------------------------------------------------------------*/

function getNextState(summaryTblId, summaryTbl, detailsTblId, analysisResult, message, oldState, acceptList, acceptOneItem) {
    // because this value is stored in different fields, so we have to check on what options were selected to determine the active value
    let currentReport, currentSummaryModel;

    const prevAnalysisResult = oldState?.analysisResult;
    currentReport = oldState?.report;
    currentSummaryModel = oldState.summaryModel;
    const currentDetailsModel = oldState?.detailsModel;

    if (!analysisResult) { //clearReport sets analysisResult:undefined, so set currentReport=undefined to clear the file
        currentReport = undefined;
    }
    let summaryModelToUseForDetails= summaryTbl ?? currentSummaryModel;

    if (message) {
        return {message, report:undefined, summaryModel:undefined, detailsModel:undefined, selectInfo: undefined};
    } else if (analysisResult) {
        if (analysisResult !== prevAnalysisResult) {
            currentReport = JSON.parse(analysisResult);
            if (currentReport.fileFormat === Format.UNKNOWN) {
                return {message:'Unrecognized file type', report:undefined, summaryModel:undefined, detailsModel:undefined, selectInfo: undefined};
            }

            currentSummaryModel= makeSummaryModel(currentReport, summaryTblId, acceptList);
            summaryModelToUseForDetails= currentSummaryModel;

            const firstExtWithData= getFirstExtWithData(currentReport.parts, acceptList, currentSummaryModel);
            if (firstExtWithData >= 0) {
                const selectInfo = SelectInfo.newInstance({rowCount: currentSummaryModel.tableData.data.length});
                !acceptOneItem && selectInfo.setRowSelect(firstExtWithData, true);        // default select first extension/part with data
                !acceptOneItem && (currentSummaryModel.selectInfo = selectInfo.data);
                summaryModelToUseForDetails.highlightedRow= firstExtWithData;
            }

        }
    }

    const detailsModel = getDetailsModel(summaryModelToUseForDetails, currentReport, detailsTblId, Format.UNKNOWN);
    if (currentSummaryModel) {
        currentSummaryModel.highlightedRow= summaryModelToUseForDetails?.totalRows===currentSummaryModel.totalRows
            ? summaryModelToUseForDetails.highlightedRow
            : getFirstExtWithData(currentReport?.parts, acceptList, currentSummaryModel);
    }

    const newState= {message, analysisResult, report:currentReport, summaryModel:currentSummaryModel, detailsModel,
                    prevAnalysisResult: oldState?.analysisResult, selectInfo:summaryModelToUseForDetails?.selectInfo};
    return statesEqual(oldState, newState) ? oldState : newState;
}

function statesEqual(s1,s2) {
    if (!s1 || !s2) return false;
    if (s1.message!==s2.message) return false;
    if (s1.analysisResult!==s2.analysisResult) return false;
    const r1= s1.report;
    const r2= s2.report;
    if (r1 && r2) {
        if (r1 !== r2) {
            if (r1.fileName !== r2.fileName || r1.fileFormat !== r2.fileFormat ||
                r1.fileSize !== r2.fileSize || r1.parts?.length !== r2.parts?.length) return false;
        }
    }
    if ((r1 && !r2) || (!r1 && r2)) {
        return false;
    }

    if (!shallowequal(s1.selectInfo,s2.selectInfo)) return false;
    if (!summaryModelEqual(s1.summaryModel, s2.summaryModel)) return false;

    const d1= s1.detailsModel;
    const d2= s2.detailsModel;
    if (d1 && d2) {
        if (d1 !== d2 &&
            (d1?.totalRows !== d2?.totalRows ||
                d1?.tableData.data?.find((d, idx) => d?.[2] !== d2?.tableData.data[idx][2]))) return false;
    }
    if ((d1 && !d2) || (!d1 && d2)) {
        return false;
    }
    return true;

}

function summaryModelEqual(sm1,sm2) {
    return !(sm1 !== sm2 &&
        sm1?.totalRows !== sm2?.totalRows &&
        sm1?.selectInfo !== sm2?.selectInfo);
}

function makeSummaryModel(report, summaryTblId, acceptList) {
    const columns = [
        {name: 'Index', type: 'int', desc: 'Extension Index'},
        {name: 'Type', type: 'char', desc: 'Data Type'},
        {name: 'Description', type: 'char', desc: 'Extension Description', width: 30},
        {name: 'AllowedType', type: 'boolean', desc: 'Type in AcceptList', visibility: 'hidden'}
    ];
    const {parts=[]} = report;
    const data = parts.map( (p) => {
        const naxis= getIntHeaderFromAnalysis('NAXIS',p,0);
        const entryType = (naxis===1 && p.type===FileAnalysisType.Image)?FileAnalysisType.Table :p.type;
        const isMoc=  isMOCFitsFromUploadAnalsysis(report)?.valid;
        const isDatalink=  isAnalysisTableDatalink(report);
        let isImageAllowed = true;
        let isTableAllowed = true;
        if (!acceptImages(acceptList)) {
            isImageAllowed = false;
        }
        if (!acceptTableOrSpectrum(acceptList)) {
            //edge case: could still be a MOC/DL table file - which only has table entries - don't pink those out
            isMoc || isDatalink? isTableAllowed= true: isTableAllowed= false;
        }
        let allowedType = true;
        if (entryType === FileAnalysisType.Table || entryType === FileAnalysisType.Image) {
            allowedType = entryType === FileAnalysisType.Table? isTableAllowed: isImageAllowed;
        }
        return [p.index, entryType , p.desc, allowedType];
    });

    const summaryModel = {
        tbl_id: summaryTblId,
        tableMeta: {
            DATARIGHTS_COL: 'AllowedType'
        },
        title: 'File Summary',
        totalRows: data.length,
        tableData: {columns, data}
    };
    return summaryModel;
}

function getDetailsModel(tableModel, report, detailsTblId, UNKNOWN_FORMAT) {
    if (!tableModel) return;
    const {highlightedRow=0} = tableModel;
    const partNum = getCellValue(tableModel, highlightedRow, 'Index');
    const type = getCellValue(tableModel, highlightedRow, 'Type');
    if (type===UNKNOWN_FORMAT) return undefined;
    const details = report?.parts?.[partNum]?.details;
    const rowCnt= report?.parts?.[partNum]?.totalTableRows ?? 0;
    if (details) {
        details.tbl_id = detailsTblId;
        details.tableMeta= {...details?.tableMeta, TOTAL_TABLE_ROWS:rowCnt};
    }
    return details;
}

function TableDisplayOption({isMoc, isDatalink, summaryTblId, currentReport, currentSummaryModel,
                                currentDetailsModel, acceptList, acceptOneItem}) {

    const highlightedRowIdx = currentSummaryModel?.highlightedRow;
    const highlightedRow = currentSummaryModel?.tableData?.data?.[highlightedRowIdx];
    //this is to make sure we show TableDisplayOptions even if only one table entry is selected when acceptOneItem === true
    const entryType = highlightedRow?
        highlightedRow[1]: currentSummaryModel?.tableData?.data?.[0]?.[1];

    const selectedTables = getFileFormat(currentReport) ?
        getSelectedRows('Table', summaryTblId, currentReport, currentSummaryModel) : [];

    if ( selectedTables.length < 1 && !acceptOneItem) return null;
    if (!currentReport || (acceptOneItem && (!highlightedRowIdx || entryType !== FileAnalysisType.Table))) return null;

    //Possibly from the Upload Panel in the HiPS/MOC 'Add MOC Layer' tab
    if (isMoc && acceptMocTables(acceptList) && !(acceptTableOrSpectrum(acceptList))) {
        return null;
    }
    else if (isMoc && acceptTableOrSpectrum(acceptList)) {
        const options= [{label:'Load as MOC Overlay', value:'moc'}, {label:'Load as Table', value:'table'}];
        return (
            <RadioGroupInputField {...{ options, sx:{pt:1/2}, orientation:'horizontal',
                                  initialState: {value: 'moc'}, fieldKey:'mocOp' }}/>
        );
    }
    else if (isDatalink && acceptList.includes(DATA_LINK_TABLES)) {
        const options= [{label:'Load as Datalink Search UI', value:'datalinkUI'}, {label:'Load as Table', value:'table'}];
        let defaultValue= 'table';
        if (currentDetailsModel?.resources.some( ({utype=''}) => utype.toLowerCase().startsWith('cisx'))) {
            defaultValue= 'datalinkOp';
        }
        return (
            <RadioGroupInputField {...{sx: {py:1/2}, options, alignment:'horizontal',
                initialState: {value: defaultValue}, fieldKey:'datalinkOp' }} />
        );
    }

    return (
        <div style={{marginTop: 3}}>
                {acceptList.includes(SPECTRUM_TABLES) && <CheckboxGroupInputField
                    sx={{mx:1}}
                    options={[{value: 'spectrum',
                        title:'If possible - interpret table columns names to fit into a spectrum data model',
                        label:'Attempt to interpret tables as spectra'}]}
                    fieldKey='tablesAsSpectrum'
                />}
        </div>
    );
}

function ImageDisplayOption({summaryTblId, currentReport, currentSummaryModel, acceptList}) {
    const selectedImages = getSelectedRows('Image', summaryTblId, currentReport, currentSummaryModel);

    const singleAxis= findSingleAxisImages(currentReport);

    if ( selectedImages.length < 2 && !singleAxis.length) return null;

    const imgOptions = [{value: 'oneWindow', label: 'All images in one window'},
        {value: 'mulWindow', label: 'One extension image per window'}];
    if (acceptList.includes(IMAGES)) {
        return (
            <Stack mt={1/2} mx={1} spacing={1}>
                {(selectedImages.length > 1) && <RadioGroupInputField
                    orientation='horizontal'
                    tooltip='display image extensions in one window or multiple windows'
                    fieldKey='imageDisplay'
                    options={imgOptions}
                />}
                {singleAxis.length > 0 &&
                    <CheckboxGroupInputField
                                        options={[{value: 'singleAxisImage',
                                            title:'Show Nx1 images as table and chart',
                                            label:'Show Nx1 images as table and chart',
                                            }]}
                                        initialState={{value: 'singleAxisImage'}}
                                        fieldKey={SINGLE_ROW_AS_TABLE}
                                    />}

            </Stack>
        );
    }
    else { //could still be a FITS file, but acceptList only accepts Tables & not Images
        return null;
    }
}

function UploadOptions({uploadSrc, isLoading, isWsUpdating, uploadKey, dropEvent, setDropEvent}) {

    const [getUploadMetaInfo, setUploadMetaInfo]= useFieldGroupMetaState({isLoading:undefined, statusKey: undefined });
    const onLoading = (loading, statusKey) => {
        setUploadMetaInfo({...getUploadMetaInfo(), isLoading:loading, statusKey: loading?statusKey:''});
        setDropEvent(null);
    };

    if (uploadSrc === FILE_ID) {
        return (
            <FileUpload
                key={uploadKey}
                fieldKey={FILE_ID}
                fileAnalysis={onLoading}
                tooltip='Select a file with FITS, VOTABLE, CSV, TSV, or IPAC format'
                dropEvent={dropEvent}
                canDragDrop={true}
            />
        );
    } else if (uploadSrc === URL_ID) {
        return (
            <FileUpload
                key={uploadKey}
                fieldKey={URL_ID}
                fileAnalysis={onLoading}
                isFromURL={true}
                label='Enter URL of a file'
                tooltip='Select a URL with file in FITS, VOTABLE, CSV, TSV, or IPAC format'
                dropEvent={dropEvent}
                canDragDrop={true}
            />
        );
    } else if (uploadSrc === WS_ID) {
        return (
            <WorkspaceUpload
                key={uploadKey}
                wrapperStyle={{marginRight: 32}}
                preloadWsFile={true}
                fieldKey={WS_ID}
                isLoading={isLoading || isWsUpdating}
                fileAnalysis={onLoading}
                tooltip='Select a file in FITS, VOTABLE, CSV, TSV, or IPAC format from workspace'
            />
        );
    }
    return null;
}

function AnalysisInfo({report,supported=true,UNKNOWN_FORMAT}) {
    const partDesc = report.fileFormat === 'FITS' ? 'Extensions:' :
        report.fileFormat === UNKNOWN_FORMAT ? '' : 'Parts:';
    const partCnt= report?.parts?.length ?? 1;
    return (
        <Stack {...{direction:'row', spacing:2}}>
            <Stack spacing={1/2} direction='row'>
                <Typography>Format:</Typography>
                <Typography color='warning'>{report.fileFormat}</Typography>
            </Stack>
            <Stack spacing={1/2} direction='row'>
                <Typography>Size:</Typography>
                <Typography color='warning'>{getSizeAsString(report.fileSize)}</Typography>
            </Stack>
            <Stack spacing={1/2} direction='row'>
                <Typography color='warning'>{report.fileName}</Typography>
            </Stack>
            <Stack spacing={1/2} direction='row'>
                {partCnt>1 && <Typography>{partDesc}</Typography>}
                {partCnt>1 && <Typography color='warning'>{partCnt}</Typography> }
            </Stack>
            {!supported && <div style={{color:'red', fontSize:'larger'}}>
                {getFirstPartType() ? `File type of ${getFirstPartType()} is not supported` : 'Could not recognize the file type'}</div>}
        </Stack>
    );
}

const tblOptions = {showToolbar:false, border:false, showOptionButton:false, showFilters:true};

function AnalysisTable({summaryModel, detailsModel, report, isMoc, UNKNOWN_FORMAT, acceptList, acceptOneItem}) {
    if (!summaryModel) return null;

    // Details table needs to render first to create a stub to collect data when Summary table is loaded.
    return (
        <Stack {...{direction:'row', flexGrow:1, position:'relative', minHeight:'1rem'}}>
            {(summaryModel.tableData.data.length>1) ?
                <MultiDataSet summaryModel={summaryModel} detailsModel={detailsModel} isMoc={isMoc}
                              acceptOneItem={acceptOneItem}/> :
                <SingleDataSet type={summaryModel.tableData.data[0][1]} desc={summaryModel.tableData.data[0][2]}
                               detailsModel={detailsModel} report={report} UNKNOWN_FORMAT={UNKNOWN_FORMAT}
                               currentSummaryModel={summaryModel} acceptList={acceptList}
                />
            }
        </Stack>
    );
}

function SingleDataSet({type, desc, detailsModel, report, UNKNOWN_FORMAT, currentSummaryModel, acceptList}) {
    const supported = isSinglePartFileSupported(currentSummaryModel, acceptList);
    const showDetails= supported && detailsModel;
    const isUWSJobFile = isUWS(report);
    const jobUrl = report.parts[0].url;
    return (
        <Stack spacing={2} {...{direction:'row', flex:'1 1 auto', justifyContent: showDetails || isUWSJobFile?'start':'center', position:'relative'}}>
            <Stack spacing={1} sx={{ml: isUWSJobFile?4:0, flexShrink:0}}>
                <Typography level='title-lg' sx={{whiteSpace:'nowrap', pb:5}}>
                    {type}{desc ? `: ${desc}` : ''}
                </Typography>
                <AnalysisInfo {...{report, supported, UNKNOWN_FORMAT}} />
                {isUWSJobFile && <UWSInfo {...{jobUrl}}/>}
                <Typography sx={{pt:2}}>No other detail about this file</Typography>
            </Stack>
            {
                showDetails &&
                <Stack spacing={1} sx={{flexGrow:1, position:'relative'}}>
                    <Details detailsModel={detailsModel}/>
                </Stack>
            }
        </Stack>
    );
}

function MultiDataSet({summaryModel, detailsModel, isMoc, acceptOneItem}) {
    return (
        <Stack {...{width: 1}}>
            {
                isMoc &&
                <Typography level='title=lg' sx={{height: '3rem', alignSelf: 'center'}}>
                    This table is a MOC and can be overlaid on a HiPS Survey
                </Typography>
            }
            <Box sx={{height:1, position:'relative'}}>
                <SplitPane split='vertical' maxSize={-20} minSize={20} defaultSize={350}>
                    {acceptOneItem && <TablePanel {...{showTypes:false, title:'File Summary', tableModel:summaryModel,
                        ...tblOptions, selectable:false, }} />}
                    {!acceptOneItem && <TablePanel {...{sx:{mr:1}, showTypes:false, title:'File Summary', tableModel:summaryModel,
                        ...tblOptions}} />}
                    <Details detailsModel={detailsModel}/>
                </SplitPane>
            </Box>
        </Stack>
    );
}


function Details({detailsModel}) {
    if (!detailsModel) {
        return (
            <Typography level='body-lg' sx={{w:1, textAlign:'center',mt:2}}>
                Details not available
            </Typography>
        );
    }

    return (
        <TablePanel showTypes={false}  title='File Details'
                    tableModel={detailsModel}
                    sx={{ml:1, position:'relative', inset:0}}
                    {...tblOptions} showMetaInfo={true} selectable={false}/>
    );

}


function getTableArea(report, summaryModel, detailsModel, isMoc, UNKNOWN_FORMAT, acceptList, acceptOneItem) {
    if (report?.fileFormat === UNKNOWN_FORMAT) {
        return (
            <Typography color='danger' level='body-lg' sx={{flexGrow: 1, mt: 4, textAlign:'center'}}>
                Error: Unrecognized Format
            </Typography>
        );
    }
    return <AnalysisTable {...{summaryModel, detailsModel, report, isMoc, UNKNOWN_FORMAT, acceptList, acceptOneItem}} />;
}

function buildAllowedTypes(acceptList) {
    const allowedTypes = [];
    acceptTableOrSpectrum(acceptList) && allowedTypes.push(TABLE_MSG);
    acceptRegions(acceptList) && allowedTypes.push(REGION_MSG);
    acceptImages(acceptList) && allowedTypes.push(IMAGE_MSG);
    acceptMocTables(acceptList) && allowedTypes.push(MOC_MSG);
    acceptDataLinkTables(acceptList) && allowedTypes.push(DL_MSG);
    acceptUWS(acceptList) && allowedTypes.push(UWS_MSG);
    return allowedTypes;
}

const AcceptedList = (props) => {
    const {list} = props;
    const allowedTypes = buildAllowedTypes(list);
    const liStyle= {listStyleType:'circle'};
    return (
        <Box className='ff-FileUploadViewPanel-acceptList' sx={{ml:25, mt:2}}>
            <Typography component='div'>
                You can load any of the following types of files:
                <ul>
                    {allowedTypes.map((fileType, index) => {
                        return <li key={index} style={liStyle}>{fileType}</li>;
                    })}
                </ul>
            </Typography>
        </Box>);
};

const NotAccepted = (props) => {
    const  {types} = props;
    const liStyle= {listStyleType:'circle'};
    return (
        <Box className='ff-FileUploadViewPanel-acceptList' sx={{ml:25, mt:2}}>
            <Typography component='div'>
                Warning: You cannot upload the following file type(s) from here:
                <ul>
                    {types.map((fileType, index) => {
                        return <li key={index} style={liStyle}>{fileType}</li>;
                    })}
                </ul>
            </Typography>
        </Box>);
};

const determineAccepted = (acceptList, uniqueTypes, isMoc, isDL) => {
    const notAcceptedTypes = [];
    let accepted;
    if (uniqueTypes.includes(REGIONS)) {
        if (!acceptRegions(acceptList)) {
            notAcceptedTypes.push('Region');
        }
    }
    else if (uniqueTypes.includes(UWS)) {
        if (!acceptUWS(acceptList)) {
            notAcceptedTypes.push('UWS');
        }
    }
    else if (uniqueTypes.includes(IMAGES) && uniqueTypes.includes(TABLES)) { //possibly FITs
        if (!acceptImages(acceptList) && !acceptTableOrSpectrum(acceptList)) {
            notAcceptedTypes.push('Images', 'Tables');
        }
    }
    else if (uniqueTypes.includes(IMAGES)) { //but not table
        if (!acceptImages(acceptList)) {
            notAcceptedTypes.push('Images');
        }
    }
    else if (uniqueTypes.includes(TABLES)) { //but not image - could be moc fits, vot, tsv, etc.
        //even if one of these is in the acceptList, we can assume tables are allowed, but if not - throw a warning
        if (!acceptAnyTables(acceptList)) {
            notAcceptedTypes.push('Tables');
        }
        //assuming if acceptList = 1, in this case we only accept MOC fits files and nothing else
        if (acceptMocTables((acceptList)) && !isMoc &&
            (acceptList.length===1 || !acceptNonMocTables(acceptList))) {
            notAcceptedTypes.push('Any non MOC FITS files');
        }
        if (acceptDataLinkTables(acceptList) && !isDL &&
            (acceptList.length===1 || !acceptNonDataLinkTables(acceptList))) {
            notAcceptedTypes.push('Any non Datalink Tables');
        }
    }

    notAcceptedTypes.length === 0? accepted=true: accepted=false;
    return {accepted, notAcceptedTypes};
};

const warningMessage = (acceptList, uniqueTypes) => {
    //if this is a file with both table and image entries, but either table or image is not in acceptList, there will be some pinked out entries (tables or images)
    if (uniqueTypes.includes('Table') && uniqueTypes.includes('Image') && (!acceptImages((acceptList)) || !acceptTableOrSpectrum(acceptList))) {
        return (<Typography level='title-md' color='warning' sx={{m: '20px 0 0 0'}}>
            Note: Any selected entries with highlighted pink lines will not be loaded.
        </Typography>);
    }
    else {
        return null;
    }
};

function UWSInfo ({jobUrl}) {
    const [jobInfo, setJobInfo] = useState('');
    const [errMsg, setErrMsg] = useState(null);
    useEffect(  () => {
        uwsJobInfo(jobUrl).then((info) => {
            setJobInfo(info);
        })
            .catch(() => {
                setErrMsg('Error: Please upload UWS files via the \'Upload from URL\' option');
            });
    },[jobUrl]);

    if (!jobUrl) { //user uploaded a UWS file from 'Upload file' option (instead of 'Upload from URL')
        return (
            <Stack>
                {errMsg && <Typography level='body-lg' color='danger' sx={{mt:2}}> {errMsg} </Typography>}
            </Stack>
        );
    }

    return (
        <Stack {...{
            width: 1, minHeight: 100, minWidth: 250, maxWidth: 1000, resize: 'both', overflow: 'hidden'}}>
            <UwsJobInfo jobInfo={jobInfo} isOpen={true}/>
        </Stack>
    );
}


const FileAnalysis = ({report, summaryModel, detailsModel, isMoc, UNKNOWN_FORMAT, acceptList,
                          isDL, acceptOneItem}) => {
    //getting FieldGroup context and adding required params to the request object (used in resultSuccess in FileUploadProcessor)
    const {groupKey, register, unregister}= useContext(FieldGroupCtx);

    const types= summaryModel?.tableData?.data.map( (d) => d[1]) ?? [];

    //types will have repeated 'Image', 'Table', etc. - getting only unique values from types
    const uniqueTypes = types.filter((value, index, self) => self.indexOf(value) === index);

    const additionalReqObjs = {summaryModel, report, detailsModel, groupKey, acceptList, uniqueTypes, acceptOneItem};
    useEffect(() => {
        register('additionalParams', () => additionalReqObjs);
        return () => unregister('additionalParams');
    }, [report]);

    if (report) {

        const {accepted, notAcceptedTypes}= determineAccepted(acceptList, uniqueTypes, isMoc, isDL);

        if  (accepted) { //no errors/warnings: show the AnalysisInfo
            return (
                <Stack flexGrow={1} spacing={2} mt={1}>
                    {summaryModel.tableData.data.length>1 && <AnalysisInfo {...{report}} />}
                    {warningMessage(acceptList, uniqueTypes)}
                    {getTableArea(report, summaryModel, detailsModel, isMoc, UNKNOWN_FORMAT, acceptList, acceptOneItem)}
                </Stack>
            );
        }
        else {
            return  (<NotAccepted types={notAcceptedTypes}/>);
        }

    }

    else {
        return (
            <>
            <AcceptedList list={acceptList}/>
                <Typography level='h2' component='div' color='warning' sx={{minHeight:'5rem', flex:'1 1 auto', mt:'4rem', textAlign: 'center'}}>
                    Drag & drop your files here
                </Typography>
            </>
        );
    }
};

