/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   Dec. 2015
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 * Work History
 *
 *  [Feb-22-2017 LZ]
 *  DM-9500
 *  DM-8963
 */
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get, set, isEmpty, has} from 'lodash';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../core/ComponentCntlr.js';
import {Operation} from '../visualize/PlotState.js';
import {getRootURL} from '../util/BrowserUtil.js';
import {download, downloadViaAnchor, encodeUrl, updateSet} from '../util/WebUtil.js';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import CompleteButton from './CompleteButton.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import FieldGroupUtils, {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {primePlot, getActivePlotView, getAllCanvasLayersForPlot} from '../visualize/PlotViewUtil.js';
import {Band} from '../visualize/Band.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {RequestType} from '../visualize/RequestType.js';
import {ServiceType} from '../visualize/WebPlotRequest.js';
import {isImage} from '../visualize/WebPlot.js';
import {makeRegionsFromPlot} from '../visualize/region/RegionDescription.js';
import {saveDS9RegionFile} from '../rpc/PlotServicesJson.js';
import FieldGroupCntlr from '../fieldGroup/FieldGroupCntlr.js';
import {DownloadOptionsDialog, fileNameValidator, getTypeData, validateFileName,
        WORKSPACE, LOCALFILE} from './DownloadOptionsDialog.jsx';
import {isValidWSFolder, WS_SERVER_PARAM, getWorkspacePath, isWsFolder, dispatchWorkspaceUpdate} from '../visualize/WorkspaceCntlr.js';
import {doDownloadWorkspace, workspacePopupMsg} from './WorkspaceViewer.jsx';
import {ServerParams} from '../data/ServerParams.js';
import {INFO_POPUP, showInfoPopup} from './PopupUtil.jsx';
import {getWorkspaceConfig} from '../visualize/WorkspaceCntlr.js';

import HelpIcon from './HelpIcon.jsx';
import {fetchUrl} from '../util/WebUtil';

const STRING_SPLIT_TOKEN= '--STR--';
const dialogWidth = 500;
const dialogHeightWS = 500;
const dialogHeightLOCAL = 400;

const dialogPopupId = 'fitsDownloadDialog';
const fKeyDef = {
    fileType: {fKey: 'fileType', label: 'Type of files:'},
    opOption: {fKey: 'operationOption', label: 'FITS Image:'},
    colorBand:{fKey: 'threeBandColor', label: 'Color Band:'},
    fileName: {fKey: 'fileName', label: 'Save as:'},
    location: {fKey: 'fileLocation', label: 'File Location:'},
    wsSelect: {fKey: 'wsSelect', label: ''},
    overWritable: {fKey: 'fileOverwritable', label: 'File overwritable: '}
};

const fitsDownGroup = 'FITS_DOWNLOAD_FORM';
const labelWidth = 100;
const defValues = {
    [fKeyDef.fileType.fKey]: Object.assign(getTypeData(fKeyDef.fileType.fKey, 'fits',
        'Please select an file type', fKeyDef.fileType.label, labelWidth), {validator: null}),
    [fKeyDef.opOption.fKey]: Object.assign(getTypeData(fKeyDef.opOption.fKey, 'fileTypeOrig',
        'Please select an option', fKeyDef.opOption.label, labelWidth), {validator: null}),
    [fKeyDef.colorBand.fKey]: Object.assign(getTypeData(fKeyDef.colorBand.fKey, '',
        'Please select a color option', fKeyDef.colorBand.label, labelWidth), {validator: null}),
    [fKeyDef.fileName.fKey]: Object.assign(getTypeData(fKeyDef.fileName.fKey, '',
        'Please enter a filename, a default name will be used if it is blank', fKeyDef.fileName.label, labelWidth), {validator: null}),
    [fKeyDef.location.fKey]: Object.assign(getTypeData(fKeyDef.location.fKey, 'isLocal',
        'select the location where the file is downloaded to', fKeyDef.location.label, labelWidth), {validator: null}),
    [fKeyDef.wsSelect.fKey]: Object.assign(getTypeData(fKeyDef.wsSelect.fKey, '',
        'workspace file system', fKeyDef.wsSelect.label, labelWidth), {validator: null}),
    [fKeyDef.overWritable.fKey]: Object.assign(getTypeData(fKeyDef.overWritable.fKey, '0',
        'File is overwritable', fKeyDef.overWritable.label, labelWidth), {validator: null})
};

const popupPanelResizableStyle = {
    width: dialogWidth,
    minWidth: dialogWidth,
    resize: 'both',
    overflow: 'hidden',
    position: 'relative'
};

export function showFitsDownloadDialog() {
    const currentFileLocation = getFieldVal(fitsDownGroup, 'fileLocation', LOCALFILE);
    if (currentFileLocation === WORKSPACE) {
        dispatchWorkspaceUpdate();
    }

    const isWs = getWorkspaceConfig();
    const adHeight = (currentFileLocation === WORKSPACE) ? dialogHeightWS
                                                         : (isWs ? dialogHeightLOCAL : dialogHeightLOCAL - 100);
    const minHeight = (currentFileLocation === LOCALFILE) && (!isWs) ? dialogHeightLOCAL-100 : dialogHeightLOCAL;

    const startWorkspacePopup =  () => {
           const  popup = (
                <PopupPanel title={'Save Image'}>
                    <div style={{...popupPanelResizableStyle, height: adHeight, minHeight}}>
                        < FitsDownloadDialogForm groupKey={fitsDownGroup} popupId={dialogPopupId} isWs={isWs}/>
                    </div>
                </PopupPanel>
            );
            DialogRootContainer.defineDialog(dialogPopupId , popup);
            dispatchShowDialog(dialogPopupId);
        };

    startWorkspacePopup();
}

const mTOP = 10;
const crtFileNameKey = 'currentBandFileNames';
/**
 * This method is called when the dialog is rendered. Only when an image is loaded, the PlotView is available.
 * Then, the color band, plotState etc can be determined.
 * @returns {{plotState, colors: Array, hasThreeColorBand: boolean, hasOperation: boolean}}
 */
function getInitialPlotState() {

    const plotView= getActivePlotView(visRoot());
    var plot = primePlot(plotView);


    var plotState = plot.plotState;

    if (plotState.isThreeColor()) {
        var threeColorBandUsed = true;

        var bands = plotState.getBands();//array of Band

        if (bands !== Band.NO_BAND) {
            var colors = [];
            for (var i=0; i<bands.length; i++) {
                switch (bands[i]){
                    case Band.RED:
                        colors[i] = 'Red';
                        break;
                    case Band.GREEN:
                        colors[i] = 'Green';
                        break;
                    case Band.BLUE:
                        colors[i] = 'Blue';
                        break;
                    default:
                        break;
                }

            }

        }
    }


    var isCrop = plotState.hasOperation(Operation.CROP);
    var isRotation = plotState.hasOperation(Operation.ROTATE);
    var cropNotRotate = isCrop && !isRotation ? true : false;

    return {
        plotView,
        plot,
        colors,
        hasThreeColorBand: threeColorBandUsed,
        hasOperation: cropNotRotate
    };

}

function renderOperationOption(hasOperation) {
    if (hasOperation) {
        return (
            <div style={{display: 'flex', marginTop: mTOP}}>
                <div>
                    <RadioGroupInputField
                        options={[
                            { label:'Original', value:'fileTypeOrig'},
                            { label:'Cropped', value:'fileTypeCrop'}

                            ]}
                        fieldKey='operationOption'

                    />
                </div>
            </div>
        );
    }
    else {
        return false;
    }
}

function renderThreeBand(hasThreeColorBand, colors) {

    const fieldKey = FieldGroupUtils.getGroupFields('FITS_DOWNLOAD_FORM');

    if (isEmpty(colors) ||
        (fieldKey && (fieldKey.fileType.value==='png' || fieldKey.fileType.value==='reg')) ){
        return false;
    }

    if (hasThreeColorBand) {
        var optionArray=[];
        for (var i=0; i<colors.length; i++){
            optionArray[i]={label: colors[i], value: colors[i]};
        }

        return (
             <div style={{display: 'flex', marginTop: mTOP}}>
                <div>
                    <RadioGroupInputField
                        options={optionArray}
                        fieldKey='threeBandColor'
                    />
                </div>
            </div>
        );
    }
    else {
        return false;
    }
}

export class FitsDownloadDialogForm extends PureComponent {
    constructor(props) {
        super(props);

        const { plotView, plot, colors, hasThreeColorBand, hasOperation} = getInitialPlotState();

        this.plotView = plotView;
        this.plot = plot;
        this.colors = colors;
        this.hasThreeColorBand = hasThreeColorBand;
        this.hasOperation = hasOperation;
        const {groupKey} = props;

        this.getDefaultFileName = this.getDefaultFileName.bind(this);
        this.getCurrentBand = () => {
            return (hasThreeColorBand ? getFieldVal(groupKey, 'threeBandColor', colors[0])
                                      : Band.NO_BAND.key);
        };
        this.getFileNames = (force= false) => {
            let fileNames = getFieldVal(groupKey, crtFileNameKey, []);

            if (!fileNames || isEmpty(fileNames) || force) {
                fileNames = colors ? colors.reduce((prev, oneColor) => {
                    prev[oneColor] = this.getDefaultFileName(hasOperation, Band.get(oneColor));
                    return prev;
                }, {}) : {};


                const baseName = this.getDefaultFileName(hasOperation, Band.NO_BAND);

                fileNames[Band.NO_BAND.key] = baseName;
                fileNames['png'] = baseName.replace('.fits', '.png');
                fileNames['reg'] = baseName.replace('.fits', '.reg');
            }
            return fileNames;
        };

        this.getCurrentOp = () => {
            return getFieldVal(groupKey, 'operationOption', '');
        };
        this.getCurrentType = () => {
            return getFieldVal(groupKey, 'fileType', 'fits');
        };

        this.state = {currentFileNames: this.getFileNames(),
            currentBand: this.getCurrentBand(),
            currentOp: this.getCurrentOp(),
            currentType: this.getCurrentType(),
            isImage: true};

    }

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.setState( () => ({currentFileNames:this.getFileNames(true)}));
        this.unbinder = FieldGroupUtils.bindToStore(this.props.groupKey, (fields) => {
            if (this.iAmMounted) {
                this.setState((state) => {
                    const band = get(fields, ['threeBandColor', 'value'], '');
                    const op = get(fields, ['operationOption', 'value'], '');
                    const fileName = get(fields, ['fileName', 'value'], '');
                    const fileType = get(fields, ['fileType', 'value'], 'fits');

                    const stateChanges= {};
                    if (band !== state.currentBand || fileType !== state.currentType ||
                        isImage(this.plot)!== state.isImage) {
                        stateChanges.isImage= isImage(this.plot);
                        stateChanges.currentBand=band;
                        stateChanges.currentType=fileType;
                        stateChanges.currentFileNames= this.getFileNames(true);
                    } else {
                        const fKey = (fileType === 'fits') ? band : fileType;

                        if (fileName !== state.currentFileNames[fKey]) {
                            stateChanges.currentFileNames= Object.assign({}, state.currentFileNames);
                            stateChanges.currentFileNames[fKey] = fileName;
                        }
                    }
                    if (op !== state.currentOp) {
                        stateChanges.currentOp=op;
                    }


                    return stateChanges;
                });
            }
        });
    }

    getDefaultFileName(op, band) {
        return makeFileName(this.plot, band);
    }

    render() {
        const {currentType, currentBand, currentFileNames} = this.state;
        const {isWs, popupId} = this.props;
        const labelWidth = 100;
        const fileName = (currentType === 'fits') ? currentFileNames[currentBand] : currentFileNames[currentType];
        const renderOperationButtons = renderOperationOption(this.hasOperation, labelWidth);
        const renderThreeBandButtons = renderThreeBand(this.hasThreeColorBand, this.colors, labelWidth);//true, ['Green','Red', 'Blue']);
        const totalChildren = (isWs ? 3 : 2) +  // fileType + save as + (fileLocation)
                              (renderOperationButtons ? 1 : 0) + (renderThreeBandButtons ? 1 : 0);
        const childH = (totalChildren*(20+mTOP));


        let fileTypeOps;
        if (isImage(primePlot(this.plotView))) {
            fileTypeOps=  [
                {label: 'FITS Image', value: 'fits'},
                {label: 'PNG File', value: 'png' },
                {label: 'Region File', value: 'reg'}
            ];
        }
        else {
            fileTypeOps=  [
                {label: 'PNG File', value: 'png' },
                {label: 'Region File', value: 'reg'}
            ];
        }


        const fileType = () => {
            return (
                <div style={{display: 'flex', marginTop: mTOP}}>
                    <div>
                        <RadioGroupInputField
                            options={fileTypeOps}
                            fieldKey='fileType'
                        />
                    </div>
                </div>
            );
        };

        const fileOptions = () => {
            return (
                <div>
                    {fileType()}
                    {renderOperationButtons}
                    {renderThreeBandButtons}
                </div>
            );
        };


        return (

            <FieldGroup style={{height: 'calc(100% - 10px)', width: '100%'}}
                        groupKey={this.props.groupKey} keepState={false}
                        reducerFunc={FitsDLReducer({band: currentBand, fileName,
                                                    currentBandFileNames: currentFileNames })}>
                <div style={{boxSizing: 'border-box', paddingLeft:5, paddingRight:5,
                             width: '100%', height: 'calc(100% - 70px)'}}>
                    <DownloadOptionsDialog fromGroupKey={this.props.groupKey}
                                           children={fileOptions()}
                                           fileName={fileName}
                                           labelWidth={labelWidth}
                                           dialogWidth={'100%'}
                                           dialogHeight={`calc(100% - ${childH}pt)`}
                                           workspace={isWs}
                    />
                </div>
                <table style={{width:'calc(100% - 20px)', margin: '20px 10px 10px 10px'}}>
                    <colgroup>
                        <col style={{width: '20%'}}/>
                        <col style={{width: '20%'}}/>
                        <col style={{width: '60%'}}/>
                    </colgroup>
                    <tbody>
                    <tr>
                        <td>
                            <div style={{textAlign:'right'}}>
                                < CompleteButton
                                    text='Save'
                                    onSuccess={ (request) => resultsSuccess(request, this.plotView, popupId )}
                                    onFail={resultsFail()}

                                />
                            </div>
                        </td>
                        <td>
                            <div style={{textAlign:'left'}}>
                                <button type='button' className='button std hl'
                                        onClick={() => closePopup(popupId)}>Cancel
                                </button>
                            </div>
                        </td>
                        <td>
                            <div style={{ textAlign:'right'}}>
                                <HelpIcon helpId={'visualization.imageoptions'}/>
                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </FieldGroup>
        );
    }

}

FitsDownloadDialogForm.propTypes = {
    groupKey: PropTypes.string.isRequired,
    popupId: PropTypes.string,
    isWs: PropTypes.oneOfType([PropTypes.bool, PropTypes.string ])
};


const FitsDLReducer = ({band, fileName, currentBandFileNames}) => {

    return (inFields, action) => {
        if (!inFields) {
            const defV = Object.assign({}, defValues);

            set(defV, [fKeyDef.colorBand.fKey, 'value'], (band !== Band.NO_BAND.key) ? band : '');
            set(defV, [fKeyDef.fileName.fKey, 'value'], fileName);
            set(defV, [fKeyDef.wsSelect.fKey, 'value'], '');
            set(defV, [fKeyDef.wsSelect.fKey, 'validator'], isWsFolder());
            set(defV, [fKeyDef.fileName.fKey, 'validator'], fileNameValidator(fitsDownGroup));
            set(defV, [crtFileNameKey, 'value'], currentBandFileNames);
            return defV;
        } else {

            const getFileKey = () => {
                const fType = get(inFields, [fKeyDef.fileType.fKey, 'value']);
                return (fType === 'fits') ? get(inFields, [fKeyDef.colorBand.fKey, 'value'])|| Band.NO_BAND.key : fType;
            };

            switch (action.type) {
                case FieldGroupCntlr.VALUE_CHANGE:
                    if (action.payload.fieldKey === fKeyDef.colorBand.fKey ||
                        action.payload.fieldKey === fKeyDef.fileType.fKey) {

                        const fileKey = getFileKey();

                        inFields = updateSet(inFields, [fKeyDef.fileName.fKey, 'value'],
                            get(inFields, [crtFileNameKey, 'value'])[fileKey]);

                    } else if (action.payload.fieldKey === fKeyDef.fileName.fKey) {
                        const fileKey = getFileKey();

                        inFields = updateSet(inFields, [crtFileNameKey, 'value', fileKey],
                            action.payload.value);
                    } else if (action.payload.fieldKey === fKeyDef.wsSelect.fKey) {
                        // change the filename if a file is selected from the file picker
                        const val = action.payload.value;

                        if (val && isValidWSFolder(val, false).valid) {
                            const fName = val.substring(val.lastIndexOf('/') + 1);
                            inFields = updateSet(inFields, [fKeyDef.fileName.fKey, 'value'], fName);
                        }
                    }
                    break;
                case FieldGroupCntlr.MOUNT_FIELD_GROUP:
                    inFields = updateSet(inFields, [fKeyDef.colorBand.fKey, 'value'],
                        ((band !== Band.NO_BAND.key) ? band : ''));
                    inFields = updateSet(inFields, [fKeyDef.fileName.fKey, 'value'], fileName);
                    inFields = updateSet(inFields, [crtFileNameKey, 'value'], currentBandFileNames);
                    break;
            }
            return Object.assign({}, inFields);
        }
    };
};


function resultsFail() {
    return (request) => {
        const {wsSelect, fileLocation} = request;

        if (fileLocation === WORKSPACE) {
            if (!wsSelect) {
                workspacePopupMsg('please select a workspace folder', 'Save to workspace');
            } else {
                const isAFolder = isValidWSFolder(wsSelect);
                if (!isAFolder.valid) {
                    workspacePopupMsg(isAFolder.message, 'Save to workspace');
                }
            }
        }
    };
}

function closePopup(popupId) {
    dispatchHideDialog(popupId);
    if (isDialogVisible(INFO_POPUP)) {
        dispatchHideDialog(INFO_POPUP);
    }
}


/**
 * This function process the request
 * @param request
 * @param plotView
 * @param popupId
 */
function resultsSuccess(request, plotView, popupId) {
    // var rel = showResults(true, request);

    const plot= primePlot(plotView);
    var plotState = plot.plotState;

    if (!Object.keys(request).length) {
        console.log(request);
        return resultsFail(request);
    }

    const {fileType:ext, threeBandColor:bandSelect, operationOption:whichOp,
           fileLocation, wsSelect} = request;

    let {fileName} = request;
    var band = Band.NO_BAND;
    if (bandSelect) {
        band= Band.get(bandSelect);
    }

    const rebuldFileName = () => {
        if (ext) {
            fileName = (fileName || makeFileName(plot, band)).replace('.fits', '.'+ ext.toLowerCase());
        }
    };

    rebuldFileName();

    const isWorkspace = () => (fileLocation && fileLocation === WORKSPACE);

    if (isWorkspace()) {
        if (!validateFileName(wsSelect, fileName)) return false;
    }

    const getRegionsDes = (bSeperateText) => {
        var regionDes;

        regionDes = makeRegionsFromPlot(plot, bSeperateText);
        return `[${regionDes.join(STRING_SPLIT_TOKEN)}]`;
    };

    const downloadFile = (params) => {
        const url = isWorkspace() ? `${getRootURL()}sticky/CmdSrv`
                                  : getRootURL() + 'servlet/Download';

        if (isWorkspace()) {
            doDownloadWorkspace(url, {params});
        } else {
            download(encodeUrl(url, params));
        }

        if (popupId) {
            dispatchHideDialog(popupId);
            if (isDialogVisible(INFO_POPUP)) {
                dispatchHideDialog(INFO_POPUP);
            }
        }
    };

    const getWSCommand = (fName) => {
        return (!isWorkspace()) ? {} :  {wsCmd: ServerParams.WS_PUT_IMAGE_FILE,
                                        [ServerParams.COMMAND]: ServerParams.WS_PUT_IMAGE_FILE,
                                        [WS_SERVER_PARAM.currentrelpath.key]: getWorkspacePath(wsSelect, fName),
                                        [WS_SERVER_PARAM.newpath.key] : fName,
                                        [WS_SERVER_PARAM.should_overwrite.key]: true};


    };

    const wsCmd = getWSCommand(fileName);

    if (ext && ext.toLowerCase() === 'fits') {
        const fitsFile = !plotState.getOriginalFitsFileStr(band) || !whichOp ?
                          plotState.getWorkingFitsFileStr(band) :
                          plotState.getOriginalFitsFileStr(band);

        const param={file: fitsFile, return: fileName, log: true, fileLocation,...wsCmd};
        downloadFile(param);

    } else if (ext && ext.toLowerCase() === 'png') {
        if (isWorkspace()) {
            makePngWorkspace(plotView.plotId, getWorkspacePath(wsSelect, fileName), fileName);
        }
        else {
            makePngLocal(plotView.plotId, fileName);
        }
        if (popupId) {
            dispatchHideDialog(popupId);
            if (isDialogVisible(INFO_POPUP)) dispatchHideDialog(INFO_POPUP);
        }

    } else if (ext && ext.toLowerCase() === 'reg') {

        saveDS9RegionFile(getRegionsDes(false)).then( (result ) => {
            if (result.success) {
                const rgFile = get(result, 'RegionFileName');

                if (rgFile) {
                    const param = {file: rgFile, return: fileName, log: true, fileLocation, ...wsCmd};

                    downloadFile(param);
                }
            } else {
                showInfoPopup(get(result, 'briefFailReason', 'download region file error'), 'region file download');
            }
        }, () => {
            console.log('error');
        });
        
    }

}

function  makeFileName(plot,  band) {

    var plotState = plot.plotState;
    var req= plotState.getWebPlotRequest(band);

    if (req.getDownloadFileNameRoot()) {
        return req.getDownloadFileNameRoot()+'.fits';
    }


   var rType= req.getRequestType();

    var retval;
    switch (rType) {
        case RequestType.SERVICE:
            retval= makeServiceFileName(req,plot, band);
            break;
        case RequestType.FILE:
            //retval= 'USE_SERVER_NAME';
            retval = makeTitleFileName(plot, band);
            break;
        case RequestType.URL:
            retval= makeTitleFileName(plot, band);
            break;
        case RequestType.ALL_SKY:
            retval= 'allsky.fits';
            break;
        case RequestType.BLANK:
            retval= 'blank.fits';
            break;
        case RequestType.PROCESSOR:
            retval= makeTitleFileName(plot, band);
            break;
        case RequestType.RAWDATASET_PROCESSOR:
            retval= makeTitleFileName(plot, band);
            break;
        case RequestType.TRY_FILE_THEN_URL:
            retval= makeTitleFileName(plot, band);
            break;
        default:
            retval= makeTitleFileName(plot, band);
            break;

    }
    return retval;
}

function  makeServiceFileName(req,plot, band) {

    var sType= req.getServiceType();
    var retval;
    switch (sType) {
        case ServiceType.IRIS:
            retval= 'iris-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.ISSA:
            retval= 'issa-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.DSS:
            retval= 'dss-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.SDSS:
            retval= 'sdss-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.TWOMASS:
            retval= 'twomass-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.MSX:
            retval= 'msx-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.DSS_OR_IRIS:
            retval= 'fits-'+req.getSurveyBand()+'.fits';
            break;
        case ServiceType.WISE:
            retval= 'wise-'+req.getSurveyKey()+'-'+req.getSurveyBand()+'.fits';
            break;
        case ServiceType.ATLAS:
            retval= 'atlas-'+req.getSurveyKey()+'-'+req.getSurveyBand()+'.fits';
            break;
        case ServiceType.NONE:
            retval= makeTitleFileName(plot, band);
            break;
        default:
            retval= makeTitleFileName(plot, band);
            break;
    }
    return retval;
}
function  makeTitleFileName(plot, band) {


    var retval = plot.title;
    if (band !== Band.NO_BAND) {
        retval= retval + '-'+ band;
    }
    retval= getHyphenatedName(retval);

    return retval +  '.fits';
}
/**
 * This method split a string by ':' and white spaces.
 * After the str is spitted to an array, reconnect the array to a string
 * using '-'.
 *
 * @param str input string
 * @returns {T} a string by replace ':' and white spaces by '-' in the input str.
 */
function getHyphenatedName(str){

	//filter(Boolean) will only keep the truthy values in the array.
    const sArray=str.split(/[ :]+/).filter(Boolean);

    let fName=sArray[0];
    for(let i=1; i<sArray.length; i++){
        fName=fName+'-'+sArray[i];
    }
    return fName;
}

const UL_URL = `${getRootURL()}sticky/CmdSrv?${ServerParams.COMMAND}=${ServerParams.UPLOAD}`;

function doUpload(file, params={}) {
    params = Object.assign(params, {file});   // file should be the last param due to AnyFileUpload limitation
    const options = {method: 'multipart', params};
    return fetchUrl(UL_URL, options);
}

function makePngLocal(plotId, filename= 'a.png') {
    const canvas= makeImageCanvas(plotId);

    if (canvas) {
        canvas.toBlob( (blob) => {
            const url= URL.createObjectURL(blob);
            downloadViaAnchor(url, filename);
            URL.revokeObjectURL(url);
        }, 'image/png');
    }



}

function makePngWorkspace(plotId, path, filename= 'a.png') {
    const canvas= makeImageCanvas(plotId);
    if (canvas) {
        canvas.toBlob( (blob) => {
            const params = {
                type:'PNG',
                filename,
                workspacePut:true,
                [WS_SERVER_PARAM.currentrelpath.key]: path,
                [WS_SERVER_PARAM.newpath.key] : filename,
                [WS_SERVER_PARAM.should_overwrite.key]: true
            };

            return doUpload(blob, params).then( ({status, cacheKey}) => {
            });
        }, 'image/png');
    }
}


function makeImageCanvas(plotId) {
    const cAry= getAllCanvasLayersForPlot(plotId);
    if (isEmpty(cAry)) return;
    const canvas = document.createElement('canvas');
    canvas.width= cAry[0].width;
    canvas.height= cAry[0].height;
    const ctx= canvas.getContext('2d');
    cAry.forEach( (c) => ctx.drawImage(c, 0,0));
    return canvas;
}

