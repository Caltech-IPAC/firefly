import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {get} from 'lodash';
import {isFunction, isNil, isEmpty} from 'lodash';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import {FilePicker} from '../externalSource/FilePicker/FilePicker.jsx';
import {dispatchWorkspaceCreatePath,
        dispatchWorkspaceDeletePath,
        dispatchWorkspaceMovePath,
        dispatchWorkspaceUpdate,
        getWorkspaceStatus,
        getWorkspaceList, getFolderUnderLevel,
        getWorkspacePath, isWsFolder, WS_SERVER_PARAM, WS_HOME} from '../visualize/WorkspaceCntlr.js';
import {CompleteButton} from './CompleteButton.jsx';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../core/ComponentCntlr.js';
import {PopupPanel} from './PopupPanel.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {fetchUrl} from '../util/WebUtil.js';
import {getRootURL} from '../util/BrowserUtil.js';
import {ServerParams} from '../data/ServerParams.js';
import {showInfoPopup, INFO_POPUP} from './PopupUtil.jsx';
import {HelpIcon} from './HelpIcon.jsx';

const UL_URL = `${getRootURL()}sticky/CmdSrv`;
const HMargin = 15;
const VMargin = 15;
const workspaceUploadGroup = 'workspaceUpload';
const workspaceUploadDef = { file: {fkey: 'uploadfile', label: 'Workspace Upload'} };
const wsUploadPopupId = 'workspacesUpload';

import LOADING from 'html/images/gxt/loading.gif';

export function onRenameFile() {
    return (oldKey, newKey) => {
        dispatchWorkspaceMovePath({oldKey, newKey, isFile: true});
    };
}

export function onRenameFolder() {
    return (oldKey, newKey) => {
    };
}

export function onCreateFolder() {
    return (key) => {
        dispatchWorkspaceCreatePath({newPath: key});
    };
}

export function onCreateFiles() {
    return (files) => {
        dispatchWorkspaceCreatePath({files: files});
    };
}

export function onDeleteFile() {
    return (key) => {
        dispatchWorkspaceDeletePath({file: key});
    };
}

export function onDeleteFolder() {
    return (key) => {
        dispatchWorkspaceDeletePath();
    };
}

// component as FilePicker wrapper
export class WorkspaceView extends PureComponent {
    constructor(props) {
        super(props);
        this.state = {files: this.props.files, currentSelection: ''};
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const workspaceList = getWorkspaceList();
            if (this.state.files !== workspaceList) {
                this.setState({files: workspaceList});
            }
        }
    }


    render () {
        const {onCreateFolder, onCreateFiles, onRenameFolder, onRenameFile,
               onDeleteFolder, onDeleteFile, onClickItem, files, wrapperStyle={width: '100%', height: '100%'},
               keepSelect, folderLevel=1} = this.props;
        const {selectedItem} = this.props;
        const eventHandlers = {
                onCreateFolder,
                onCreateFiles,
                onRenameFolder,
                onRenameFile,
                onDeleteFolder,
                onDeleteFile,
                onClickItem};

        const openFolders = getFolderUnderLevel(folderLevel);
        return (
            <div style={wrapperStyle}>
                {FilePicker({files, selectedItem, keepSelect, openFolders, ...eventHandlers})}
            </div>
        );
    }
}

WorkspaceView.defaultProps = {
    onCreateFolder: null,
    onCreateFiles: null,
    onRenameFolder: null,
    onRenameFile: null,
    onDeleteFolder: null,
    onDeleteFile: null,
    onClickItem: null,
    files: []
};

WorkspaceView.propTypes = {
    onCreateFolder: PropTypes.func,
    onCreateFiles: PropTypes.func,
    onRenameFolder: PropTypes.func,
    onRenameFile: PropTypes.func,
    onDeleteFolder: PropTypes.func,
    onDeleteFile: PropTypes.func,
    onClickItem: PropTypes.func,
    files: PropTypes.arrayOf(PropTypes.object),
    wrapperStyle: PropTypes.object,
    selectedItem: PropTypes.string,
    keepSelect: PropTypes.bool,
    folderLevel: PropTypes.number
};


export const WorkspaceViewField = fieldGroupConnector(WorkspaceView, getViewProps);

// get key from FilePicker
function getViewProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            selectedItem: params.value,
            onClickItem: (key) => {
                fireValueChange({value: key});
            }
    });
}

// component for 'save to workspace' - FilePicker with 'add folder' action
export const WorkspaceSave = ({fieldKey, files, value}) => {
    return (
        <WorkspaceViewField fieldKey={fieldKey}
                            files={files}
                            value={value}
                            keepSelect={true}
                            onCreateFolder={onCreateFolder()}

        />
    );
};

WorkspaceSave.propTypes = {
    fieldKey: PropTypes.string.isRequired,
    files: PropTypes.arrayOf(PropTypes.object),
    value: PropTypes.string
};

/*
 * view of WorkspaceUpload
 * @param wrapperStyle
 * @param onClickUpload
 * @param value key from file picker
 * @param isLoading
 * @returns {XML}
 * @constructor
 */
function WorkspaceReadView({wrapperStyle, onClickUpload, value, isLoading}) {
    const style = Object.assign({whiteSpace:'nowrap', display: 'inline-block', height: 22}, wrapperStyle);

    const inputEntry = () => {
        return (
            <div style={style}>
                <input  type='button'
                        value='Workspace upload'
                        onClick={()=>updateAndShowWorkspace({onClickUpload, value})} />
            </div>
        );
    };

    // value is key from file picker
    const resultFile = () => {
        return (
            <div style={{display:'inline-block', marginLeft: 5}}>
                {value ? getWorkspacePath(value) : 'No workspace file chosen'}
            </div>
        );
    };


    return (
        <div>
            {inputEntry()}
            {resultFile()}
            {isLoading && <img style={{display: 'inline-block', marginLeft: 10, width:14,height:14}} src={LOADING}/> }
        </div>
    );
}

WorkspaceReadView.propTypes = {
    value: PropTypes.string,
    wrapperStyle: PropTypes.object,
    onClickUpload: PropTypes.func.isRequired,
    isLoading: PropTypes.bool.isRequired
};

WorkspaceReadView.defaultProps = {
    isLoading: false
};

// component for 'read from workspace' - a button and popup panel showing a FilePicker when the button is clicked
export  const WorkspaceUpload =  fieldGroupConnector(WorkspaceReadView, getUploadProps);

// the value defined is display value as shown on UI
WorkspaceUpload.propTypes = {
    fieldKey: PropTypes.string.isRequired,
    fileAnalysis: PropTypes.oneOfType([PropTypes.func, PropTypes.bool]),
    wrapperStyle: PropTypes.object,
    isLoading: PropTypes.bool,
    value: PropTypes.string,
    preloadWsFile: PropTypes.bool
};


function getUploadProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            value: params.displayValue,
            onClickUpload: resultSuccess(fireValueChange, params.fileAnalysis, params.preloadWsFile)
        }
    );
}

function resultSuccess(fireValueChange, fileAnalysis, preloadWsFile) {
    return (request) => {
        const itemValue = get(request, workspaceUploadDef.file.fkey);
        const value= (itemValue && itemValue.startsWith(WS_HOME)) && itemValue.substring(WS_HOME.length);
        if (preloadWsFile) {
            handleUpload(itemValue, fireValueChange, fileAnalysis);
        }
        else {
            fireValueChange({
                displayValue: itemValue,
                value
            });
        }

        if (isDialogVisible(INFO_POPUP)) {
            dispatchHideDialog(INFO_POPUP);
        }
    };
}


function resultFail() {
    return (request) => {
        const name = getWorkspacePath(get(request, [workspaceUploadDef.file.fkey], ''));

        showInfoPopup(name + ' is not a file to be read from workspace', 'Upload from workspace');

        return false;
    };
}

function resultCancel() {
    dispatchHideDialog(wsUploadPopupId);
    if (isDialogVisible(INFO_POPUP)) {
        dispatchHideDialog(INFO_POPUP);
    }
}

function updateAndShowWorkspace({onClickUpload, value}) {
    dispatchWorkspaceUpdate({callback: showWorkspaceUploadPopup({onClickUpload, value})});
}

/* workspace upload popup */
/* get displayValue from store => 'value' to WorkspaceUpload => 'value' to WorkspaceReadValue => popup =>
   selectedItem in WorkspaceViewField => WorkspaceView
 */
export function showWorkspaceUploadPopup({onClickUpload, value}) {
    return (files) => {
        const newList = files || getWorkspaceList() || [];
        const dialogWidth = 500;
        const dialogHeight = 350;
        const popupPanelResizableStyle = {
                        width: dialogWidth,
                        height: dialogHeight,
                        minWidth: dialogWidth,
                        minHeight: dialogHeight,
                        resize: 'both',
                        overflow: 'hidden'};
        const style = {
                        marginLeft: HMargin,
                        marginRight: HMargin,
                        marginTop: VMargin,
                        width: `calc(100% - ${HMargin*2+20}px)`,
                        height: `calc(100% - ${VMargin+25}px)`,
                        paddingLeft: 10,
                        paddingRight: 10,
                        paddingTop: 10,
                        border: '1px solid #a3aeb9',
                        borderRadius: 5,
                        overflow: 'auto'
        };

        const startWorkspaceReadPopup = () => {
            const popup = (
                <PopupPanel title={'Read file from workspace'}>
                    <div style={popupPanelResizableStyle}>
                        <FieldGroup style={{height: 'calc(100% - 80px)', width: '100%'}}
                                    groupKey={workspaceUploadGroup} keepState={true}>
                            <div style={style}>
                                <WorkspaceViewField fieldKey={workspaceUploadDef.file.fkey}
                                                    files={newList}
                                                    keepSelect={true}
                                                    value={value}
                                                    initialState={{validator: isWsFolder(false)}}/>
                            </div>
                        </FieldGroup>

                        <div style={{display: 'flex', justifyContent: 'space-between',
                                     margin: `20px ${HMargin}px ${VMargin}px ${HMargin}px`}}>
                            <div style={{display: 'flex', width: '60%', alignItems: 'flex-end'}}>
                                <div style={{marginRight: 10}}>
                                    <CompleteButton
                                        groupKey={workspaceUploadGroup}
                                        onSuccess={onClickUpload}
                                        onFail={resultFail()}
                                        text={'Open'}
                                        dialogId={wsUploadPopupId}
                                    />
                                </div>
                                <div>
                                    <button type='button' className='button std hl'
                                            onClick={() => resultCancel()}>Cancel
                                    </button>
                                </div>
                            </div>
                            <div style={{ textAlign:'right', marginRight: 10}}>
                                <HelpIcon helpId={'visualization.imageoptions'}/>
                            </div>
                        </div>
                    </div>
                </PopupPanel>
            );

            DialogRootContainer.defineDialog(wsUploadPopupId, popup);
            dispatchShowDialog(wsUploadPopupId);
        };

        if (!newList || isEmpty(newList)) {
            workspacePopupMsg('Workspace access error: ' + getWorkspaceStatus(), 'Workspace access');
        } else {
            startWorkspaceReadPopup();
        }
    };
}

/*
displayValue: the selected item name (folder name of file name)
value: file name at the server
 */
function handleUpload(key, fireValueChange, fileAnalysis) {
    fireValueChange({
        displayValue: key,
        value: !fileAnalysis ? makeDoUpload(key) : makeDoUpload(key, fileAnalysis)()
    });
}

function makeDoUpload(file, fileAnalysis) {
    return () => {
        return doUploadWorkspace(file, {fileAnalysis}).then(({status, message, cacheKey, fileFormat, analysisResult})=> {
            let valid = status === '200';
            if (valid) {        // json file is not supported currently
                if (!isNil(fileFormat)) {
                    if (fileFormat.toLowerCase() === 'json') {
                        valid = false;
                        message = 'json file is not supported';
                        analysisResult = '';
                    }
                }
            }

            return {isLoading: false, valid, message, value: cacheKey, analysisResult};
        }).catch((err) => {
            const msg = `Unable to upload file from ${getWorkspacePath(file)}`;

            workspacePopupMsg(msg, 'Workspace upload error');
            return {
                isLoading: false, valid: false,
                message: msg
            };
        });
    };
}

/**
 * upload file from workspace
 * @param file
 * @param params
 * @returns {*}
 */
function doUploadWorkspace(file, params={}) {
    if (!file) return Promise.reject('Required file parameter not given');

    file = getWorkspacePath(file);

    params = Object.assign(params, {[WS_SERVER_PARAM.currentrelpath.key]: file,
                                    [WS_SERVER_PARAM.newpath.key]: file,
                                     wsCmd: ServerParams.WS_UPLOAD_FILE,
                                     cmd: ServerParams.UPLOAD});
    const options = {method: 'POST', params};

    if (params.fileAnalysis && isFunction(params.fileAnalysis)) {
        params.fileAnalysis();
        options.params.fileAnalysis = true;
    }

    return fetchUrl(UL_URL, options).then( (response) => {
        return response.text().then((text) => {
            // text is in format ${status}::${message}::${message}::${cacheKey}::${analysisResult}
            const result = text.split('::');
            const [status, message, cacheKey, fileFormat] = result.slice(0, 4);
            const analysisResult = result.slice(4).join('::');
            return {status, message, cacheKey, fileFormat, analysisResult};
        });
    });
}

/**
 * save image or table to workspace
 * @param url
 * @param options
 */
export function doDownloadWorkspace(url, options) {
    fetchUrl(url, options).then( (response) => {
        response.json().then( (value) => {
            if (value.ok === 'true') {
                dispatchWorkspaceCreatePath({files: value.result});
            } else {
                workspacePopupMsg('Save error - '+ value.status,
                                 'Save to workspace');
            }
        });
    });
}

export function workspacePopupMsg(msg, title) {
    /*
     const popup = (
     <div style={{padding:5}}>
     <div style={{minWidth:190, maxWidth: 400, padding:10, fontSize:'120%'}}>
     {msg}
     </div>
     <div style={{padding:'0 0 5px 10px'}}>
     <CompleteButton dialogId={MODAL_DIALOG_ID}/>
     </div>
     </div>
     );
     showModal(popup);
     */
    showInfoPopup(msg, title);
}