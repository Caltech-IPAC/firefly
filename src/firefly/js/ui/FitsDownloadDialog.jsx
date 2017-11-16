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
import {get, set, isEmpty} from 'lodash';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../core/ComponentCntlr.js';
import {Operation} from '../visualize/PlotState.js';
import {getRootURL} from '../util/BrowserUtil.js';
import {download, encodeUrl} from '../util/WebUtil.js';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import CompleteButton from './CompleteButton.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {primePlot, getActivePlotView} from '../visualize/PlotViewUtil.js';
import {Band} from '../visualize/Band.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {RequestType} from '../visualize/RequestType.js';
import {ServiceType} from '../visualize/WebPlotRequest.js';
import {makeRegionsFromPlot} from '../visualize/region/RegionDescription.js';
import {saveDS9RegionFile, getImagePng} from '../rpc/PlotServicesJson.js';
import FieldGroupCntlr from '../fieldGroup/FieldGroupCntlr.js';
import {updateSet} from '../util/WebUtil.js';
import {DownloadOptionsDialog, fileNameValidator, getTypeData, validateFileName, WORKSPACE, LOCALFILE} from './DownloadOptionsDialog.jsx';
import {isValidWSFolder, WS_SERVER_PARAM, getWorkspacePath, isWsFolder, getWorkspaceList} from '../visualize/WorkspaceCntlr.js';
import {doDownloadWorkspace, workspacePopupMsg} from './WorkspaceViewer.jsx';
import {ServerParams} from '../data/ServerParams.js';
import {INFO_POPUP} from './PopupUtil.jsx';

import HelpIcon from './HelpIcon.jsx';

const STRING_SPLIT_TOKEN= '--STR--';
const dialogWidth = 500;
const dialogHeightWS = 500;
const dialogHeightLOCAL = 400;

const dialogPopupId = 'fitsDownloadDialog';
const fKeyDef = {
    fileType: {fKey: 'fileType', label: 'Type of files:'},
    opOption: {fKey: 'operationOption', label: 'FITS file:'},
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
    minHeight: dialogHeightLOCAL,
    resize: 'both',
    overflow: 'hidden',
    position: 'relative'
};

function getDialogBuilder() {

    const currentFileLocation = FieldGroupUtils.getFldValue(fitsDownGroup, 'fileLocation', LOCALFILE);
    const adHeight = (currentFileLocation === LOCALFILE) ? dialogHeightLOCAL : dialogHeightWS;

    var popup = null;
    return () => {
        if (!popup) {
            popup = (
                <PopupPanel title={'FITS Download Dialog'}>
                    <div style={{...popupPanelResizableStyle, height: adHeight}}>
                        < FitsDownloadDialogForm groupKey={'FITS_DOWNLOAD_FORM'} popupId={dialogPopupId}/>
                    </div>
                </PopupPanel>
            );
            DialogRootContainer.defineDialog(dialogPopupId , popup);
        }
        return popup;
    };
}

const dialogBuilder = getDialogBuilder();

export function showFitsDownloadDialog() {
    dialogBuilder();
    dispatchShowDialog('fitsDownloadDialog');
}

const mTOP = 10;
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
        const baseName = this.getDefaultFileName(hasOperation, Band.NO_BAND.key);
        const currentFileName = colors ? colors.reduce((prev, oneColor) => {
                prev[oneColor] = this.getDefaultFileName(hasOperation, Band.get(oneColor));
                return prev;
        }, {}) : {};

        currentFileName[Band.NO_BAND.key] = baseName;
        currentFileName['png'] = baseName.replace('.fits', '.png');
        currentFileName['reg'] = baseName.replace('.fits', '.reg');

        this.state = {currentFileName,
                      currentBand: hasThreeColorBand ? colors[0] : Band.NO_BAND.key,
                      currentOp: '',
                      currentType: 'fits'};
        this.getDefaultFileName = this.getDefaultFileName.bind(this);
    }

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.unbinder = FieldGroupUtils.bindToStore(this.props.groupKey, (fields) => {
            if (this.iAmMounted) {
                this.setState((state) => {
                    const band = get(fields, ['threeBandColor', 'value'], '');
                    const op = get(fields, ['operationOption', 'value'], '');
                    const fileName = get(fields, ['fileName', 'value'], '');
                    const fileType = get(fields, ['fileType', 'value'], 'fits');

                    if (band !== state.currentBand || fileType !== state.currentType) {
                        state.currentBand = band;
                        state.currentType = fileType;
                    } else {
                        const fKey = (fileType === 'fits') ? band : fileType;

                        if (fileName !== state.currentFileName[fKey]) {
                            state.currentFileName[fKey] = fileName;
                        }
                    }
                    if (op !== state.currentOp) {
                        state.currentOp = op;
                    }


                    return state;
                });
            }
        });
    }

    getDefaultFileName(op, band) {
        return makeFileName(this.plot, band);
    }

    render() {
        const {currentType, currentBand, currentFileName} = this.state;
        const labelWidth = 100;
        const fileName = (currentType === 'fits') ? currentFileName[currentBand] : currentFileName[currentType];
        const renderOperationButtons = renderOperationOption(this.hasOperation, labelWidth);
        const renderThreeBandButtons = renderThreeBand(this.hasThreeColorBand, this.colors, labelWidth);//true, ['Green','Red', 'Blue']);
        const {popupId} = this.props;
        const totalChildren = 3 + (renderOperationButtons ? 1 : 0) + (renderThreeBandButtons ? 1 : 0);
        const childH = (totalChildren*(20+mTOP));

        const fileType = () => {
            return (
                <div style={{display: 'flex', marginTop: mTOP}}>
                    <div>
                        <RadioGroupInputField
                            options={ [
                                          {label: 'FITS File', value: 'fits'},
                                          {label: 'PNG File', value: 'png' },
                                          {label: 'Region File', value: 'reg'}
                                        ]}
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
                        groupKey={this.props.groupKey} keepState={true}
                        reducerFunc={FitsDLReducer({band: currentBand, fileName,
                                                    currentBandFileName: currentFileName })}>
                <div style={{boxSizing: 'border-box', paddingLeft:5, paddingRight:5,
                             width: '100%', height: 'calc(100% - 70px)'}}>
                    <DownloadOptionsDialog fromGroupKey={this.props.groupKey}
                                           children={fileOptions()}
                                           fileName={fileName}
                                           labelWidth={labelWidth}
                                           dialogWidth={'100%'}
                                           dialogHeight={`calc(100% - ${childH}pt)`}/>
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
                            <div style={{textAlign:'left'}}>
                                < CompleteButton
                                    text='Download'
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
    groupKey: PropTypes.string,
    popupId: PropTypes.string
};


const FitsDLReducer = ({band, fileName, currentBandFileName}) => {
    const crtFileNameKey = 'currentBandFileName';

    return (inFields, action) => {
        if (!inFields) {
            const defV = Object.assign({}, defValues);

            set(defV, [fKeyDef.colorBand.fKey, 'value'], (band !== Band.NO_BAND.key) ? band : '');
            set(defV, [fKeyDef.fileName.fKey, 'value'], fileName);
            set(defV, [fKeyDef.wsSelect.fKey, 'value'], '');
            set(defV, [fKeyDef.wsSelect.fKey, 'validator'], isWsFolder());
            set(defV, [fKeyDef.fileName.fKey, 'validator'], fileNameValidator(fitsDownGroup));
            set(defV, [crtFileNameKey, 'value'], currentBandFileName);
            return defV;
        } else {

            const getFileKey = () => {
                const fType = get(inFields, [fKeyDef.fileType.fKey, 'value']);
                return (fType === 'fits') ? get(inFields, [fKeyDef.colorBand.fKey, 'value']) : fType;
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
                    inFields = updateSet(inFields, [crtFileNameKey, 'value'], currentBandFileName);
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

    const isWorkspace = () => (fileLocation && fileLocation === WORKSPACE);

    var band = Band.NO_BAND;
    if (bandSelect) {
        band= Band.get(bandSelect);
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

    let wsCmd;

    if (ext && ext.toLowerCase() === 'fits') {
        const fitsFile = !plotState.getOriginalFitsFileStr(band) || !whichOp ?
                          plotState.getWorkingFitsFileStr(band) :
                          plotState.getOriginalFitsFileStr(band);

        fileName = (fileName || makeFileName(plot, band));
        wsCmd = getWSCommand(fileName);

        const param={file: fitsFile, return: fileName, log: true, fileLocation,...wsCmd};
        downloadFile(param);

    } else if (ext && ext.toLowerCase() === 'png') {

        const {flipY, rotation, plotViewCtx:{rotateNorthLock} }= plotView;

        getImagePng(plotState, getRegionsDes(true), rotateNorthLock, rotation? ((rotation-180)+360)%360 : 0, flipY).then((result) => {
            //const imgFile = getReturnName(fileName || get(result, 'ImageFileName'));

            const imgFile = get(result, 'ImageFileName');
            fileName = (fileName || makeFileName(plot, band)).replace('.fits', '.png');

            wsCmd = getWSCommand(fileName);
            if (imgFile) {
                const param = isWorkspace() ? {file: imgFile,...wsCmd} :
                              {file: imgFile, return: fileName, log: true};

                downloadFile(param);
            }
        }, () => {
            console.log('error');
        });

    } else if (ext && ext.toLowerCase() === 'reg') {

        saveDS9RegionFile(getRegionsDes(false)).then( (result ) => {
            const rgFile = get(result, 'RegionFileName');

            fileName = (fileName || makeFileName(plot, band)).replace('.fits', '.reg');
            wsCmd = getWSCommand(fileName);

            if (rgFile) {
                const param={file: rgFile, return:fileName, log: true, fileLocation,...wsCmd};

                downloadFile(param);
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
            retval= 'USE_SERVER_NAME';
            break;
        case RequestType.URL:
            retval= makeTitleFileName(band);
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
    var sArray=str.split(/[ :]+/).filter(Boolean);

    var fName=sArray[0];
    for(var i=1; i<sArray.length; i++){
        fName=fName+'-'+sArray[i];
    }
    return fName;
}
