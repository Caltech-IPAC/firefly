/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../core/ReduxFlux.js';
import {get, set, flattenDeep, isEmpty} from 'lodash';
import {ServerParams} from '../data/ServerParams.js';
import {workspacePopupMsg} from '../ui/WorkspaceViewer.jsx';
import Enum from 'enum';
import {getCmdSrvSyncURL, getProp, toBoolean, updateMerge, updateSet} from '../util/WebUtil.js';
import {getAppOptions} from '../core/AppDataCntlr.js';
import {fetchUrl} from '../util/fetch';

export const WORKSPACE_PREFIX = 'WorkspaceCntlr';

export const WORKSPACE_PATH = 'workspace';
export const WORKSPACE_CREATE_PATH = `${WORKSPACE_PREFIX}.createPath`;
export const WORKSPACE_RENAME_PATH = `${WORKSPACE_PREFIX}.renamePath`;
export const WORKSPACE_MOVE_PATH = `${WORKSPACE_PREFIX}.movePath`;
export const WORKSPACE_DELETE_PATH = `${WORKSPACE_PREFIX}.deletePath`;
export const WORKSPACE_LIST = `${WORKSPACE_PREFIX}.getList`;
export const WORKSPACE_LIST_UPDATE = `${WORKSPACE_PREFIX}.updateList`;
export const WORKSPACE_IN_LOADING = `${WORKSPACE_PREFIX}.inLoading`;

export default {actionCreators, reducers };
export const WS_HOME = 'WS_Home';
/**
 * @typedef {Object} WS_SERVER_PARAM
 * @prop should_overwrite
 * @prop currentrelpath
 * @prop newpath
 * @type {Enum}
 */
export const WS_SERVER_PARAM = new Enum(['should_overwrite', 'currentrelpath', 'newpath']);


function actionCreators() {
    return {
        [WORKSPACE_CREATE_PATH]: createPath,
        [WORKSPACE_RENAME_PATH]: renamePath,
        [WORKSPACE_MOVE_PATH]: movePath,
        [WORKSPACE_DELETE_PATH]: deletePath,
        [WORKSPACE_LIST]: getPathList,
        [WORKSPACE_LIST_UPDATE]: updatePathList
    };
}

function reducers() {
    return {
        [WORKSPACE_PATH]: reducer
    };
}

export function dispatchWorkspaceSearch(options = {}, dispatcher=flux.process) {
    dispatcher({type: WORKSPACE_LIST, payload: options});
}

export function dispatchWorkspaceUpdate(options = {}, dispatcher=flux.process) {
    dispatcher({type: WORKSPACE_LIST_UPDATE, payload: options});
}

export function dispatchWorkspaceCreatePath(options = {}, dispatcher=flux.process) {
    dispatcher({
        type: WORKSPACE_CREATE_PATH,
        payload: options
    });
}

export function dispatchWorkspaceDeletePath(options = {}, dispatcher=flux.process) {
    dispatcher({type: WORKSPACE_DELETE_PATH, payload: options});

}

export function dispatchWorkspaceMovePath(options = {}, dispatcher=flux.process) {
    dispatcher({type: WORKSPACE_MOVE_PATH, payload: options});

}

function movePath(action) {
    return (dispatch) => {
        const {oldKey, newKey, isFile} = get(action, 'payload');

        if (isFile) {
            const oldFile = getWorkspacePath(oldKey);
            const newFile = getWorkspacePath(newKey);

            const params = {[ServerParams.COMMAND]: ServerParams.WS_MOVE_FILE,
                            [WS_SERVER_PARAM.currentrelpath.key]: oldFile,
                            [WS_SERVER_PARAM.newpath.key]: newFile
            };

            const options={params};

            fetchUrl(getCmdSrvSyncURL(), options).then((response) => {
                response.json().then((value) => {
                    if (value.ok === 'true') {
                        set(action, ['payload', 'oldFile'], oldFile);
                        set(action, ['payload', 'files'], value.result);
                        dispatch(action);
                    } else {
                        workspacePopupMsg('Move workspace file fails: '+ value.status,
                                          'Move workspace file');
                    }
                });
            }).catch( (error) => {
                workspacePopupMsg('Move workspace file fails: ' + error.message,
                                  'Move workspace file');
            });

        }
    };
}

function renamePath() {

}

function deletePath(action) {
    return (dispatch) => {
        const {file} = get(action, 'payload');

        if (file) {
            const wsFile = getWorkspacePath(file);

            const params = {[ServerParams.COMMAND]: ServerParams.WS_DELETE_FILE,
                             [WS_SERVER_PARAM.currentrelpath.key]: wsFile
            };

            fetchUrl(getCmdSrvSyncURL(), {params}).then((response) => {
                response.json().then((value) => {
                    if (value.ok === 'true') {
                        set(action, ['payload', 'file'], wsFile);
                        dispatch(action);
                    } else {
                        workspacePopupMsg('Delete workspace file fails: '+ value.status,
                            'Delete workspace file');
                    }
                });
            }).catch( (error) => {
                workspacePopupMsg('Delete workspace file fails: ' + error.message,
                                  'Delete workspace file');
            });

        }
    };
}

function startLoadWorkspace(dispatch) {
    dispatch({type: WORKSPACE_IN_LOADING, payload: {isLoading: true}});
}

function createPath(action) {
    return (dispatch) => {
        if (get(action, ['payload', 'files'])) {
            dispatch(action);           // add new files after download operation
        } else {
            const newpath = get(action, ['payload', 'newPath']);

            if (newpath) {
                startLoadWorkspace(dispatch);
                const wsPath = getWorkspacePath(newpath);
                const params = {[ServerParams.COMMAND]: ServerParams.WS_CREATE_FOLDER,
                                [WS_SERVER_PARAM.newpath.key] : wsPath,
                                [WS_SERVER_PARAM.currentrelpath.key]: wsPath
                };

                fetchUrl(getCmdSrvSyncURL(), {params}).then((response) => {
                    response.json().then((value) => {
                        if (value.ok === 'true') {
                            set(action, ['payload', 'newPath'], value.result);
                            dispatch(action);
                        } else {
                            workspacePopupMsg('Create workspace path fails: '+ value.status,
                                             'Create workspace folder');
                        }
                    });
                }).catch( (error) => {
                    workspacePopupMsg('Create workspace path fails: ' + error.message,
                        '             Create workspace folder');
                });
            }
        }
    };
}

function getPathList(action) {
    return (dispatch) => {
        startLoadWorkspace(dispatch);
        if (!get(action, ['payload', 'files'], null)) {
            const params = {[WS_SERVER_PARAM.currentrelpath.key]: '/',
                            [ServerParams.COMMAND]: ServerParams.WS_LIST};

            fetchUrl(getCmdSrvSyncURL(), {params}).then((response) => {
                response.json().then( (value) => {
                    if (value.ok === 'true') {
                        set(action, ['payload', 'files'], value.result);
                        dispatch(action);
                    } else {
                        set(action, ['payload', 'files'], []);
                        set(action, ['payload', 'status'], value.status);
                        set(action, ['payload', 'statusCode'], value.statusCode);

                        dispatch(action);
                    }
                });
            }).catch( (error) => {
                workspacePopupMsg('Workspace access request sending error: ' + error.message,
                                  'Workspace error');
            });
        } else {
            dispatch(action);
        }
    };
}


function updatePathList(action) {
    return (dispatch) => {
        startLoadWorkspace(dispatch);
        const doUpdate = (files, status, statusCode) => {
            const result = createWorkspaceList(files, null, status, statusCode);

            action = updateMerge(action, 'payload', result);
            dispatch(action);
        };

        if (!get(action, ['payload', 'files'], null)) {

            const relPath = get(action, ['payload', 'relPath']);

            const params = {
                [WS_SERVER_PARAM.currentrelpath.key]: relPath || '/',
                [ServerParams.COMMAND]: ServerParams.WS_LIST
            };

            fetchUrl(getCmdSrvSyncURL(), {params}).then((response) => {
                response.json().then((value) => {
                    doUpdate(value.result, value.status, value.statusCode);
                });
            }).catch((error) => {
                workspacePopupMsg('Workspace update fails" ' + error.message,
                    'Workspace access');
            });
        } else {
            doUpdate(action.payload.files);
        }
    };
}

export function getWorkspaceConfig() {
    return get(getAppOptions(), ['workspace', 'showOptions'], false);
}

export function getWorkspaceFiles() {
    const files = get(flux.getState(), [WORKSPACE_PATH, 'files']);
    return files ? flattenDeep(files) : [];
}


export function getWorkspaceList() {
    return get(flux.getState(), [WORKSPACE_PATH, 'data']);
}

export function getWorkspaceStatus() {
    return isEmpty(getWorkspaceList()) && get(flux.getState(), [WORKSPACE_PATH, 'status']);
}


export function getWorkspaceStatusCode() {

    return isEmpty(getWorkspaceList()) && get(flux.getState(), [WORKSPACE_PATH, 'statusCode']);
}
export function getWorkspaceErrorMsg(){
    const statusCode = getWorkspaceStatusCode();

    var errorMsg;
    switch (statusCode){
        case '401':
            errorMsg = 'You are not logged in. Please click \'Login\' in the upper right corner of the window.';
            break;
        case '405':
            errorMsg = 'You are not logged in or not allowed to access workspace from this server.';
            break;
        case '403':
            errorMsg = 'You do not have access right';
            break;
        case '404':
            errorMsg = 'The request resources can not be found';
            break;
        case '500':
            errorMsg = 'Internal Server Error';
            break;
        case '412':
            errorMsg = 'Precondition Failed: one or more preconditions were not met';
            break;

        default:
            errorMsg = getWorkspaceStatus();
            break;
    }
    return errorMsg;
}
export function isAccessWorkspace() {
    return get(flux.getState(), [WORKSPACE_PATH, 'isLoading']);
}

export function isExistWorkspaceList() {
    const list = getWorkspaceList();

    return (!list || !isEmpty(list));
}
/**
 * get full path for FilePicker based on the given path (file or folder) plus filename
 * the full path is compliant to the path from the server
 * @param path
 * @param filename
 * @returns {*}
 */
export function getWorkspacePath(path, filename) {
    if (!path) return '/'+filename;

    const idx = path.indexOf(WS_HOME);

    const newPath = idx >= 0 ? path.substring(idx + WS_HOME.length) : path;

    if (filename) {
        return newPath.substring(0, newPath.lastIndexOf('/') + 1) + (filename ? filename : '');
    } else {
        return newPath;
    }
}

/**
 * check if the selected item is a folder or not a folder, validator for workspace download or upload
 * @param isFolderValid
 * @returns {Function}
 */
export function isWsFolder(isFolderValid=true) {
    return (key) => isValidWSFolder(key, isFolderValid);
}

export function isValidWSFolder(key, isFolderValid=true) {
    /* key = '', representing the root */
    const path = getWorkspacePath(key);
    if (!path) {
        return {
            valid: false, message: isFolderValid ? 'please select a folder below' :
                'please select a file'
        };
    } else if (path === '/') {
        return {valid: isFolderValid, message: (!isFolderValid)&&'Please select a file' };
    }

    const files = getWorkspaceFiles();
    const file = files&&files.find((oneItem) => {
            return (oneItem.relPath === path || oneItem.relPath === ('/'+path)
                || oneItem.relPath+'/' === path);
        });
    let valid, message;

    if (file) {
        // isFolderValid xor file.isFolder
        const p = isFolderValid ? 1 : 0;
        const q = (file.isFolder) ? 1 : 0;
        valid = file && ((p+q)%2 === 0);
        if (!valid) {
            message = isFolderValid ? `${path} is not a folder` :
                `${path} is a folder`;
        }
    } else {
        valid = false;
        message = `${path} doesn't exist`;
    }

    return {valid, message};
}


/**
 * get all folders under (including) the given level, used for openFoloder object for FilePicker
 * @param level
 * @returns {*}
 */
export function getFolderUnderLevel(level) {
    const list = getWorkspaceList();

    return list.reduce((prev, oneItem) => {
                if (oneItem.isFolder) {
                    const atLevel = (oneItem.key).split('/').length-1;

                    if (!level || atLevel <= level) {
                        prev[oneItem.key] = true;
                    }
                }
                return prev;
            }, {});

}

/* workspace data for testing */
/*
const workspaceFiles = [
    [
        {
            'createdDate':'2017-10-06T23:32:00Z',
            'modifiedDate':'Fri, 06 Oct 2017 23:32:00 GMT',
            'relPath':'/tmp1/',
            'contentType':'folder',
            'url':'https://irsa.ipac.caltech.edu\/ssospace\/test@ipac.caltech.edu/tmp1/',
            'sizeBytes':-1
        }
    ],
    [
        {
            'createdDate':'2017-10-06T23:32:00Z',
            'modifiedDate':'Fri, 06 Oct 2017 23:32:00 GMT',
            'relPath':'/tmp1/gaia-binary.png',
            'contentType':null,
            'url':'https://irsa.ipac.caltech.edu/ssospace/test@ipac.caltech.edu/tmp1/gaia-binary.vot',
            'sizeBytes':66515
        }
    ],
    [
        {
            'createdDate':'2017-10-06T23:32:01Z',
            'modifiedDate':'Fri, 06 Oct 2017 23:32:01 GMT',
            'relPath':'/tmp2/',
            'contentType':'folder',
            'url':'https://irsa.ipac.caltech.edu/ssospace/test@ipac.caltech.edu/tmp2/',
            'sizeBytes':-1
        }
    ],
    [
        {
            'createdDate':'2017-10-06T23:32:00Z',
            'modifiedDate':'Fri, 06 Oct 2017 23:32:00 GMT',
            'relPath':'/tmp2/gaia-binary.png',
            'contentType':null,
            'url':'https://irsa.ipac.caltech.edu/ssospace/test@ipac.caltech.edu/tmp2/gaia-binary.vot',
            'sizeBytes':66515
        }
    ],
    [
        {
            'createdDate':'2017-10-06T23:32:01Z',
            'modifiedDate':'Fri, 06 Oct 2017 23:32:01 GMT',
            'relPath':'/tmp2/gaia-binary2.pdf',
            'extraProperties':{
                'tooltip':'gaia-binary2.vot -url:https://irsa.ipac.caltech.edu/ssospace/test@ipac.caltech.edu/tmp2/gaia-binary2.vot'
            },
            'contentType':null,
            'url':'https://irsa.ipac.caltech.edu/ssospace/test@ipac.caltech.edu/tmp2/gaia-binary2.vot',
            'sizeBytes':67835
        }
    ]
];

*/

export function initWorkspace() {
    //dispatchWorkspaceSearch({files: workspaceFiles});  // testing

    // since workspace can be changed externally,
    // we should get fresh data on every interaction,
    // no need to search workspace here
    //dispatchWorkspaceSearch({});
}

/**
 * check if the full path (server path format) is one of the workspace files
 * @param fName
 * @returns {*}
 */
export function isExistWorspaceFile(fName) {
    const files = getWorkspaceFiles();

    return files&&files.find((oneFile) => oneFile.relPath === fName);
}

function itemForHome(list) {

    let url = '';

    if (list) {
        const oneUrl = get(list, [0, 'url']);
        const idx = oneUrl && oneUrl.indexOf(get(list, [0, 'relPath']));

        if (idx && idx > 0) {
            url = oneUrl.substring(0, idx+1);
        }
    }
    return [{key: WS_HOME+'/', modified: '', size: -1, url, relPath: '/', isFolder: true, childrenRetrieved: true}];
}

const convertFileToKey = (wsFile) => {
    return  WS_HOME + '/' + (wsFile.startsWith('/') ? wsFile.substring(1) : wsFile);
};

/**
 * convert files from the server to a list of file/folder for FilePicker
 * currently React-Keyed-file-browser is used and the key (file path) starts without '/'
 * @param wFiles
 * @returns {*}
 */
function convertFilesToList(wFiles) {
    const supportsInfinity = toBoolean(getProp('workspace.propfind.infinity', true));
    const list = flattenDeep(wFiles).reduce((prev, oneFile) => {
        const {relPath, modifiedDate:modified, sizeBytes, url, isFolder:isFolderVal} = oneFile;

        if (relPath && !relPath.includes('/.')) {
            let key = convertFileToKey(relPath);
            const isFolder = isFolderVal || (key.lastIndexOf('/') === (key.length - 1));
            if (isFolderVal && (key.lastIndexOf('/') !== (key.length - 1))) {
                    key += '/'; // folder keys end with '/'
                }

            const size = isFolder ? 0 : sizeBytes;

            //const key = relPath;
            const fileProps = {key, modified, size, url, relPath, isFolder};
            if (isFolder && supportsInfinity) {
                fileProps['childrenRetrieved'] = true;
            }
            prev.push(fileProps);
        }
        return prev;
    }, []);

    return list;
}

function updateWorkspaceList(wfiles, ws_data, status, statusCode, state) {
    const files = wfiles || [];
    const data = ws_data || [];
    status = (isEmpty(files)) ? (status||'No Source') : status;

    return Object.assign({}, state, {files, data, status, statusCode});
}

function createWorkspaceList(wFiles = [], state, status, statusCode) {
    if (isEmpty(wFiles)) {
        return Object.assign({}, {data: [], files: [], statusCode:statusCode, status: status||'No source'});
    }

    const list = convertFilesToList(wFiles);
    const root = list.find((oneItem) => oneItem.key === (WS_HOME+'/'));

    if (!root) {
        list.push(itemForHome(list)[0]);
    }

    const newData = {data: list, files: wFiles, status: ''};

    return state ? Object.assign({}, state, newData) : Object.assign({}, newData);
}

function addToWorkspaceList(wFiles = [], state) {
    const newState = Object.assign({}, state);
    const loadedRelPaths = newState.files.map((e) => e[0].relPath);

    // make sure the added files are not yet added
    const wFilesNoDup = wFiles.filter((e) => !loadedRelPaths.includes(e[0].relPath));
    const list = convertFilesToList(wFilesNoDup);

    newState.files = newState.files ? newState.files.concat(wFiles) : wFilesNoDup;
    newState.data = newState.data ? newState.data.concat(list) : list;

    return newState;
}


function deleteWorkspaceFile(file, state) {
    const newState = Object.assign({}, state);
    const delKey = convertFileToKey(file);

    newState.files = state.files.filter((oneFileSet) => {
        return !(oneFileSet.find((oneFile) => oneFile.relPath === file));
    });
    newState.data = state.data.filter((oneItem) => (oneItem.key !== delKey));

    return newState;
}

function moveWorkspaceList(oldFile, files, state) {
    const newState = deleteWorkspaceFile(oldFile, state);

    return addToWorkspaceList(files, newState);
}

/**
 * If infinite depth is not supported by the server,
 * folders are populated as they are clicked to be open.
 * This function sets childrenRetrieved attribute of the object
 * representing folder node in the state.
 * @param folderRelPath
 * @param state
 * @returns {*}
 */
function updateFolderStatus(folderRelPath, state) {
    const folderIdx = get(state, 'data', []).findIndex((oneFile) => oneFile.relPath === folderRelPath);
    if (folderIdx >= 0) {
        return updateSet(state, ['data', folderIdx, 'childrenRetrieved'], true);
    } else {
        return state;
    }
}

function reducer(state=initState(), action={}) {
    const {type} = action;

    if (!type || !action.payload) return state;

    const {oldFile, file, files, data, newPath, status, statusCode, isLoading} = action.payload;
    let   retState = state;

    switch(type) {
        case WORKSPACE_CREATE_PATH:
            if (files) {
                retState = addToWorkspaceList(files, state);
            } else if (newPath) {
                retState = addToWorkspaceList(newPath, state);
                // new folder has children retrieved
                const relPath = get(newPath, [0, 0, 'relPath']);
                if (relPath) {
                    retState =  updateFolderStatus(relPath, retState);
                }
            }
            retState.isLoading = false;
            break;
        case WORKSPACE_RENAME_PATH:
            break;
        case WORKSPACE_MOVE_PATH:
            retState = moveWorkspaceList(oldFile, files, state);
            break;
        case WORKSPACE_DELETE_PATH:
            if (file) {
                retState = deleteWorkspaceFile(file, state);
            }
            break;
        case WORKSPACE_LIST_UPDATE:
            if (action.payload.relPath) {
                // populate data for the folder
                retState = addToWorkspaceList(files, state);
                // update folder status (childrenRetrieved attribute)
                retState =  updateFolderStatus(action.payload.relPath, retState);
            } else {
                retState = updateWorkspaceList(files, data, status, statusCode, state);
            }
            retState.isLoading = false;
            break;
        case WORKSPACE_LIST:
            retState = createWorkspaceList(files, state, status, statusCode);
            retState.isLoading = false;
            break;
        case WORKSPACE_IN_LOADING:
            retState = Object.assign({}, state, {isLoading});
            break;
    }
    return retState;
}

function initState() {
    return {data: [], valid: true, files:[]};
}

