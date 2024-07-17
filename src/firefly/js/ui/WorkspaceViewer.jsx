import {Button, Skeleton, Stack} from '@mui/joy';
import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {get, truncate} from 'lodash';
import {isFunction, isNil, isEmpty} from 'lodash';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import {FilePicker} from '../externalSource/FilePicker/FilePicker.jsx';
import {dispatchWorkspaceCreatePath,
        dispatchWorkspaceDeletePath,
        dispatchWorkspaceMovePath,
        dispatchWorkspaceUpdate,
        getWorkspaceErrorMsg,
        getWorkspaceList, getFolderUnderLevel,
        getWorkspacePath, isAccessWorkspace, isWsFolder,
        WS_SERVER_PARAM, WS_HOME, WORKSPACE_IN_LOADING, WORKSPACE_LIST_UPDATE} from '../visualize/WorkspaceCntlr.js';
import {CompleteButton} from './CompleteButton.jsx';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../core/ComponentCntlr.js';
import {PopupPanel} from './PopupPanel.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {ServerParams} from '../data/ServerParams.js';
import {showInfoPopup, INFO_POPUP} from './PopupUtil.jsx';
import {HelpIcon} from './HelpIcon.jsx';
import {dispatchAddActionWatcher} from '../core/MasterSaga.js';

const HMargin = '1rem';
const VMargin = '1rem';
const workspacePopupGroup = 'workspacePopupGroup';
const workspaceUploadDef = { file: {fkey: 'uploadfile', label: 'Workspace Upload'} };
const workspacePopupId = 'workspacePopupId';

import LOADING from 'html/images/gxt/loading.gif';
import ComponentCntlr from '../core/ComponentCntlr.js';
import {isExistWorspaceFile} from '../visualize/WorkspaceCntlr';
import {parseUploadResults} from '../rpc/CoreServices.js';
import {fetchUrl} from '../util/fetch';
import {getCmdSrvSyncURL} from '../util/WebUtil';
import {Stacker} from 'firefly/ui/Stacker.jsx';

/*-----------------------------------------------------------------------------------------*/
/* core component as FilePicker wrapper
/*-----------------------------------------------------------------------------------------*/

export const WorkspaceView = memo( (props) => {

    const {canCreateFolder=false, canCreateFiles=false, canRenameFolder=false, canRenameFile=false,
        canDeleteFolder=false, canDeleteFile=false, onClickItem, files=[], wrapperStyle={width: '100%', height: '100%'},
        keepSelect, folderLevel=1, selectedItem} = props;
    const eventHandlers = {
        onCreateFolder: canCreateFolder ? onCreateFolder : undefined,
        onCreateFiles: canCreateFiles ? onCreateFiles : undefined,
        onRenameFolder: canRenameFolder ? onRenameFolder : undefined,
        onRenameFile: canRenameFile ? onRenameFile : undefined,
        onDeleteFolder: canDeleteFolder ? onDeleteFolder : undefined,
        onDeleteFile: canDeleteFile ? onDeleteFile : undefined,
        onClickItem};

    let openFolders = getFolderUnderLevel(folderLevel);
    // keep selected folder open initially
    // it requires the ancestor folders to be open too
    const selectedFile = files.find((oneFile) => oneFile.key === selectedItem);
    if (selectedFile && selectedFile.isFolder && selectedFile.childrenRetrieved) {
        openFolders = {};
        const parts = selectedItem.split('/').filter((e)=>e);
        parts.map((e,i,a)=>a.slice(0,i+1)
            .join('/'))
            .forEach((e) => {openFolders[e+'/'] = true;});
    }
    return (
        <div style={wrapperStyle}>
            {FilePicker({files, selectedItem, keepSelect, openFolders, ...eventHandlers})}
        </div>
    );
});


WorkspaceView.propTypes = {
    canCreateFolder: PropTypes.bool,
    canCreateFiles: PropTypes.bool,
    canRenameFolder: PropTypes.bool,
    canRenameFile: PropTypes.bool,
    canDeleteFolder: PropTypes.bool,
    canDeleteFile: PropTypes.bool,
    onClickItem: PropTypes.func,
    files: PropTypes.arrayOf(PropTypes.object),
    wrapperStyle: PropTypes.object,
    selectedItem: PropTypes.string,
    keepSelect: PropTypes.bool,
    folderLevel: PropTypes.number
};

/*-----------------------------------------------------------------------------------------*/

/**
 /* WorkspaceView as an input field
 */

export const WorkspaceViewField = memo( (props) => {
    const {viewProps, fireValueChange} = useFieldGroupConnector(props);

    const onClickItem = (key) => {
        fireValueChange({value: key});
        const files = get(props, 'files', []);
        const file = files.find((oneFile) => oneFile.key === key);
        if (file && file.isFolder && !file.childrenRetrieved) {
            dispatchWorkspaceUpdate({relPath: file.relPath});
        }
    };

    return (
        <WorkspaceView {...{...viewProps, selectedItem: viewProps.value}}
                       onClickItem={onClickItem}/>
    );
});


const defaultWorkspaceFieldPropTypes = {    fieldKey: PropTypes.string.isRequired,
                                            files: PropTypes.arrayOf(PropTypes.object),
                                            value: PropTypes.string};



// Workspace input field used for 'save to workspace' - same as WorkspaceViewField with 'add folder' enabled
export const WorkspaceSave = ({fieldKey, files, value}) => {
    return (
        <WorkspaceViewField fieldKey={fieldKey}
                            files={files}
                            initialState= {{value}}
                            keepSelect={true}
                            canCreateFolder={true}

        />
    );
};
WorkspaceSave.propTypes = defaultWorkspaceFieldPropTypes;


/*
 * Show WorkspaceView as a pop up field.  A button is used to trigger the popup.
 * A label next to the button to show the selected item.
 */
export const WorkspacePickerPopup = memo( ({fieldKey='WorkspacePickerPopup', onComplete, value={}, ...rest}) => {
    const {viewProps, fireValueChange} = useFieldGroupConnector({fieldKey, ...rest});

    return (
        <WorkspaceAsPopup {...viewProps}
                          {...{fieldKey, keepSelect: true, value: value[fieldKey]}}
                          onComplete={(v) => {
                              fireValueChange({value: v});
                              onComplete && onComplete(v[fieldKey]);
                          }}/>
    );
});


WorkspacePickerPopup.propTypes = {
    defaultWorkspaceFieldPropTypes,
    ...{fieldKey: PropTypes.string}
};



/*-----------------------------------------------------------------------------------------*/

/**
 * Show workspace as a popup with updated content.
 * @param {Object} p param
 * @param {function} p.onComplete   called when selection completes
 * @param {string} p.value          selected value
 * @param {string} p.fieldKey       the key used to store the selected value sent back from 'onComplete'
 */
export function showWorkspaceDialog({onComplete, value, fieldKey}) {
    dispatchAddActionWatcher({
        id: 'workspaceRead',
        actions:[WORKSPACE_IN_LOADING, WORKSPACE_LIST_UPDATE, ComponentCntlr.HIDE_DIALOG],
        callback: (a , cancelSelf) => {
            switch (a.type) {
                case ComponentCntlr.HIDE_DIALOG:
                    const dialogId = get(a.payload, 'dialogId');
                    if (dialogId === workspacePopupId) {
                        cancelSelf();
                    }
                    break;
                case WORKSPACE_IN_LOADING:
                    if (!isDialogVisible(workspacePopupId)) return;
                    showWorkspaceAsPopup({onComplete, value, fieldKey});
                    break;
                case WORKSPACE_LIST_UPDATE:
                    const newList = getWorkspaceList() || [];
                    if (isEmpty(newList)) {
                        workspacePopupMsg('Workspace access error: ' + getWorkspaceErrorMsg(), 'Workspace access');
                    } else {
                        showWorkspaceAsPopup({onComplete, value, fieldKey});
                    }
                    break;
            }
        }
    });
    dispatchWorkspaceUpdate();
}

/*-----------------------------------------------------------------------------------------*/

export function WorkspaceAsPopup({wrapperStyle, onComplete, value, isLoading=false, fieldKey}) {
    
    return (
        <Stack spacing={2} justifyContent={'center'} sx={{px: 2, wrapperStyle}}>
            <Stack sx={{flexDirection: 'row'}}>
                <Stack>
                    <input  type='button'
                            value='Choose Workspace File'
                            onClick={()=>showWorkspaceDialog({onComplete, value, fieldKey})} />
                </Stack>
                <Stack sx={{mx:2}}>
                    {value ? getWorkspacePath(value) : 'No workspace file chosen'}
                </Stack>
            </Stack>
            <Stack justifyContent={'center'}>
                {isLoading && <img style={{display: 'inline-block', marginLeft: '40%', width:14,height:14}} src={LOADING}/> }
            </Stack>
        </Stack>
    );
}

WorkspaceAsPopup.propTypes = {
    fieldKey: PropTypes.string,
    value: PropTypes.string,
    wrapperStyle: PropTypes.object,
    onComplete: PropTypes.func.isRequired,
    isLoading: PropTypes.bool.isRequired
};


/**
 *  Custom input field used to select a file from workspace then upload it to Firefly server.
 *  The value of this field is an ID/key to the uploaded file.
 *  Field is represented as a button.  Upon clicked, WorkspaceView will appear as a popup.
 */
export const WorkspaceUpload= memo( (props) => {
    const {viewProps, fireValueChange} = useFieldGroupConnector(props);
    const {fileAnalysis, preloadWsFile, fieldKey}= viewProps;
    return (
        <WorkspaceAsPopup {...{...viewProps, value: viewProps.displayValue, fieldKey}}
                          onComplete= {resultSuccess(fireValueChange, fileAnalysis, preloadWsFile, fieldKey)} />
           );
});




// the value defined is display value as shown on UI
WorkspaceUpload.propTypes = {
    fieldKey: PropTypes.string.isRequired,
    fileAnalysis: PropTypes.oneOfType([PropTypes.func, PropTypes.bool]),
    wrapperStyle: PropTypes.object,
    isLoading: PropTypes.bool,
    value: PropTypes.string,
    preloadWsFile: PropTypes.bool
};



function resultSuccess(fireValueChange, fileAnalysis, preloadWsFile, fieldKey=workspaceUploadDef.file.fkey) {
    return (request) => {
        const itemValue = get(request, fieldKey);
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


function resultFail(fieldKey=workspaceUploadDef.file.fkey) {
    return (request) => {
        const name = getWorkspacePath(get(request, [fieldKey], ''));

        workspacePopupMsg(name + ' is not a file to be read from workspace', 'Upload from workspace');

        return false;
    };
}

function resultCancel() {
    dispatchHideDialog(workspacePopupId);
    if (isDialogVisible(INFO_POPUP)) {
        dispatchHideDialog(INFO_POPUP);
    }
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

    return fetchUrl(getCmdSrvSyncURL(), options).then( (response) => {
        return response.text().then((text) => parseUploadResults(text) );
    });
}

/**
 * save image or table to workspace
 * @param url
 * @param options
 */
export function doDownloadWorkspace(url, options) {
    fetchUrl(url, {method: 'post', ...options}).then( (response) => {
        response.json().then( (value) => {
            if (value.ok === 'true') {
                dispatchWorkspaceCreatePath({files: value.result});
            } else {
                workspacePopupMsg('Save error - '+ value.status,
                                 'Save to workspace');
            }
        });
    }).catch(({message}) => showInfoPopup(truncate(message, {length: 200}), 'Unexpected error'));
}


export function validateFileName(wsSelect, fileName) {
    if (isNil(wsSelect)) {
        return false;
    }
    const fullPath = getWorkspacePath(wsSelect, fileName);

    if (isExistWorspaceFile(fullPath)) {
        workspacePopupMsg(`the file, ${fullPath}, already exists in workspace, please change the file name.`,
            'Save to workspace');
        return false;
    } else {
        return true;
    }

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


/* workspace upload popup */
/* get displayValue from store => 'value' to WorkspaceUpload => 'value' to WorkspaceReadValue => popup =>
 selectedItem in WorkspaceViewField => WorkspaceView
 */
function showWorkspaceAsPopup({onComplete, value, fieldKey=workspaceUploadDef.file.fkey}) {
    const newList = getWorkspaceList() || [];
    const style = {
        marginLeft: HMargin,
        marginRight: HMargin,
        paddingLeft: '1rem',
        paddingRight: '1rem',
        paddingTop: '1rem',
        border: '1px solid #a3aeb9',
        borderRadius: 5,
        overflow: 'auto'
    };

    const startWorkspaceReadPopup = () => {
        const showMask = isAccessWorkspace();
        const popup = (
            <PopupPanel title={'Read file from workspace'}>

                <Stack minWidth='40rem' minHeight='20rem' height='60vh' p={1} sx={{resize:'both', overflow:'hidden'}}>
                    <Stack flexGrow={1} overflow='auto'>
                        <FieldGroup groupKey={workspacePopupGroup} keepState={true}>
                            <WorkspaceViewField fieldKey={fieldKey}
                                            files={newList}
                                            keepSelect={true}
                                            initialState={{value, validator: isWsFolder(false)}}/>
                            {showMask && <Skeleton/>}
                        </FieldGroup>
                    </Stack>
                    <Stacker endDecorator={<HelpIcon helpId={'visualization.imageoptions'}/>}>
                        <CompleteButton
                            groupKey={workspacePopupGroup}
                            onSuccess={onComplete}
                            onFail={resultFail(fieldKey)}
                            text={'Open'}
                            dialogId={workspacePopupId}
                        />
                        <Button onClick={() => resultCancel()}>Cancel</Button>
                    </Stacker>
                </Stack>
            </PopupPanel>
        );

        DialogRootContainer.defineDialog(workspacePopupId, popup);
        dispatchShowDialog(workspacePopupId);
    };

    if (!newList || isEmpty(newList)) {
        workspacePopupMsg('Workspace access error: ' + getWorkspaceErrorMsg() , 'Workspace access');
    } else {
        startWorkspaceReadPopup();
    }
}


/*----------------------    default WS opertion impl     -------------------------------*/
function onRenameFile(oldKey, newKey) {
    dispatchWorkspaceMovePath({oldKey, newKey, isFile: true});
}

function onRenameFolder(oldKey, newKey) {
    // not implemented, yet.
}

function onCreateFolder(key) {
    dispatchWorkspaceCreatePath({newPath: key});
}

function onCreateFiles(files){
    dispatchWorkspaceCreatePath({files});
}

function onDeleteFile(key) {
    dispatchWorkspaceDeletePath({file: key});
}

function onDeleteFolder(key) {
    dispatchWorkspaceDeletePath();
}
/*-----------------------------------------------------------------------------------------*/
