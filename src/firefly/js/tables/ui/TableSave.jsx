/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {get, set} from 'lodash';

import {download, updateSet} from '../../util/WebUtil.js';
import {getTblById, getAsyncTableSourceUrl, getTableSourceUrl} from '../TableUtil.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {DEF_BASE_URL} from '../../core/JsonUtils.js';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {CompleteButton} from '../../ui/CompleteButton.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {doDownloadWorkspace, workspacePopupMsg} from '../../ui/WorkspaceViewer.jsx';
import {DownloadOptionsDialog, fileNameValidator, getTypeData, validateFileName,
        WORKSPACE} from '../../ui/DownloadOptionsDialog.jsx';
import {WS_SERVER_PARAM, isWsFolder, isValidWSFolder,
        getWorkspacePath} from  '../../visualize/WorkspaceCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';
import {INFO_POPUP} from '../../ui/PopupUtil.jsx';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';


const fKeyDef = {
    fileName: {fKey: 'fileName', label: 'Save as:'},
    location: {fKey: 'fileLocation', label: 'File Location:'},
    wsSelect: {fKey: 'wsSelect', label: ''},
    overWritable: {fKey: 'fileOverwritable', label: 'File overwritable: '}
};

const labelWidth = 100;
const defValues = {
    [fKeyDef.fileName.fKey]: Object.assign(getTypeData(fKeyDef.fileName.fKey, '',
        'Please enter a filename, a default name will be used if it is blank', fKeyDef.fileName.label, labelWidth), {validator: null}),
    [fKeyDef.location.fKey]: Object.assign(getTypeData(fKeyDef.location.fKey, 'isLocal',
        'select the location where the file is downloaded to', fKeyDef.location.label, labelWidth), {validator: null}),
    [fKeyDef.wsSelect.fKey]: Object.assign(getTypeData(fKeyDef.wsSelect.fKey, '',
        'workspace file system', fKeyDef.wsSelect.label, labelWidth), {validator: null}),
    [fKeyDef.overWritable.fKey]: Object.assign(getTypeData(fKeyDef.overWritable.fKey, '0',
        'File is overwritable', fKeyDef.overWritable.label, labelWidth), {validator: null})
};

const tblDownloadGroupKey = 'TABLE_DOWNLOAD_FORM';


export function showTableDownloadDialog({tbl_id, tbl_ui_id}) {
    return () => {
        const popupId = 'TABLE_DOWNLOAD_POPUP';
        const dialogWidth = 400;

        const startTableDownloadPopup = () => {
            const popup = (
                <PopupPanel title={'Download table'}>
                    <div style={{margin: 10, width: dialogWidth}}>
                        <FieldGroup groupKey={tblDownloadGroupKey} keepState={true}
                                    reducerFunc={TableDLReducer(tbl_id)}>
                            <DownloadOptionsDialog fromGroupKey={tblDownloadGroupKey}
                                                   />
                        </FieldGroup>
                        <div style={{display: 'flex', justifyContent: 'space-between', marginTop: 30}}>
                            <div style={{display: 'flex', width: '60%', alignItems: 'flex-end'}}>
                                <div style={{marginRight: 10}}>
                                    <CompleteButton
                                        groupKey={tblDownloadGroupKey}
                                        onSuccess={resultSuccess(tbl_id, tbl_ui_id, popupId)}
                                        onFail={resultFail()}
                                        text={'Download'}/>
                                </div>
                                <div>
                                    <button type='button' className='button std hl'
                                            onClick={() => closePopup(popupId)}>Cancel
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

            DialogRootContainer.defineDialog(popupId, popup);
            dispatchShowDialog(popupId);
        };

        startTableDownloadPopup();
    };
}

function TableDLReducer(tbl_id) {
    return (inFields, action) => {
        const {request} = getTblById(tbl_id) || {};

        if (!inFields) {
            const defV = Object.assign({}, defValues);

            set(defV, [fKeyDef.wsSelect.fKey, 'value'], '');
            set(defV, [fKeyDef.wsSelect.fKey, 'validator'], isWsFolder());
            set(defV, [fKeyDef.fileName.fKey, 'validator'], fileNameValidator(tblDownloadGroupKey));
            set(defV, [fKeyDef.fileName.fKey, 'value'], get(request, 'META_INFO.title'));
            return defV;
        } else {
            switch (action.type) {
                case FieldGroupCntlr.MOUNT_FIELD_GROUP:
                    inFields = updateSet(inFields, [fKeyDef.fileName.fKey, 'value'], get(request, 'META_INFO.title'));
                    break;
            }
            return Object.assign({}, inFields);
        }
    };
}

function closePopup(popupId) {
    dispatchHideDialog(popupId);
    if (isDialogVisible(INFO_POPUP)) {
        dispatchHideDialog(INFO_POPUP);
    }
}


function resultFail() {
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

function resultSuccess(tbl_id, tbl_ui_id, popupId) {
    return (request) => {
        const {fileName, fileLocation, wsSelect} = request || {};
        const isWorkspace = () => (fileLocation && fileLocation === WORKSPACE);

        if (isWorkspace()) {
            if (!validateFileName(wsSelect, fileName)) return false;
        }

        const getOtherParams = (fName) => {
            return (!isWorkspace()) ? {file_name: fName}
                                    : {wsCmd: ServerParams.WS_PUT_TABLE_FILE,
                                      [WS_SERVER_PARAM.currentrelpath.key]: getWorkspacePath(wsSelect, fName),
                                      [WS_SERVER_PARAM.newpath.key] : fName,
                                      [WS_SERVER_PARAM.should_overwrite.key]: true};

        };

        const downloadFile = (urlOrOp) => {
            if (isWorkspace()) {
                doDownloadWorkspace(DEF_BASE_URL,
                                    {params: urlOrOp});
            } else {
                download(urlOrOp);
            }

            if (popupId) {
                dispatchHideDialog(popupId);
                if (isDialogVisible(INFO_POPUP)) {
                    dispatchHideDialog(INFO_POPUP);
                }
            }
        };

        const {origTableModel} = getTblById(tbl_id) || {};
        if (origTableModel) {
            getAsyncTableSourceUrl(tbl_ui_id, getOtherParams(fileName)).then((urlOrOp) => {
                downloadFile(urlOrOp);
            });
        } else {
            const urlOrOp = getTableSourceUrl(tbl_ui_id, getOtherParams(fileName));
            downloadFile(urlOrOp);
        }
    };
}