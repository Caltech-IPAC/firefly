/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {isEmpty, get} from 'lodash';
import {ValidationField} from './ValidationField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {getWorkspaceList, isExistWorspaceFile, getWorkspacePath, getWorkspaceStatus,
        dispatchWorkspaceUpdate, isAccessWorkspace} from '../visualize/WorkspaceCntlr.js';
import {WorkspaceSave, workspacePopupMsg} from './WorkspaceViewer.jsx';


import LOADING from 'html/images/gxt/loading.gif';
export const LOCALFILE = 'isLocal';
export const WORKSPACE = 'isWs';

export function getTypeData(key, val='', tip = '', labelV='', labelW) {
    return {
        fieldKey: key,
        label: labelV,
        value: val,
        tooltip: tip,
        labelWidth: labelW
    };
}


export class DownloadOptionsDialog extends PureComponent {
    constructor(props) {
        super(props);

        this.workspace = get(props, 'workspace', false);
        const where = props.fromGroupKey? getFieldVal(props.fromGroupKey, 'fileLocation', LOCALFILE)
                                        : LOCALFILE;

        const fileOverwritable = props.fromGroupKey ? getFieldVal(props.fromGroupKey, 'fileOverwritable', 0) : 0;
        const wsSelect = (where === WORKSPACE) ? getFieldVal(props.fromGroupKey, 'wsSelect', '') : '';
        const isUpdating = isAccessWorkspace();
        const wsList = isUpdating ? '' : getWorkspaceList();
        this.state = {where, fileName: props.fileName, wsSelect, fileOverwritable, wsList, isUpdating};
    }

    componentWillReceiveProps(nextProps) {
        const {fileName} = nextProps;

        if (fileName !== this.state.fileName) {
            this.setState({fileName});
        }
    }

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const isUpdating = isAccessWorkspace();
            const wsList = getWorkspaceList();
            const loc = this.props.fromGroupKey && getFieldVal(this.props.fromGroupKey, 'fileLocation');
            const wsSelect = this.props.fromGroupKey && getFieldVal(this.props.fromGroupKey, 'wsSelect');

            this.setState((state) => {
                if (loc !== state.where) {
                    state.where = loc;
                    if (loc ===  WORKSPACE) {
                        state.isUpdating = true;
                        dispatchWorkspaceUpdate();
                    }
                }

                if (wsList !== state.wsList) {
                    state.wsList = wsList;
                }

                if (wsSelect !== state.wsSelect) {
                    state.wsSelect = wsSelect;
                }

                if (isUpdating !== state.isUpdating) {
                    state.isUpdating = isUpdating;
                }
                return state;
            });
        }
    }


    render() {
        const {where, wsSelect, wsList, isUpdating} = this.state;
        const {children, dialogWidth=500, dialogHeight=300} = this.props;
        const showWorkspace = () => {

            const loading = () => {
                return (
                    <div style={{width: '100%', height: '100%', display:'flex', justifyContent: 'center', alignItems: 'center'}}>
                        <img style={{width:14,height:14}} src={LOADING}/>
                    </div>
                );
            };

            const showSave = () => {
                return (
                    <div style={{marginTop: 10,
                                 boxSizing: 'border-box',
                                 width: 'calc(100%)', height: 'calc(100% - 10px)',
                                 overflow: 'auto',
                                 padding: 5,
                                 border:'1px solid #a3aeb9'
                                 }}>
                        <WorkspaceSave fieldKey={'wsSelect'}
                                       files={wsList}
                                       value={wsSelect}
                        />
                    </div>
                );
            };

            return (
                (isUpdating) ? loading() :
                    (!isEmpty(wsList) ? showSave() :
                        workspacePopupMsg('Workspace access error: ' + getWorkspaceStatus(), 'Workspace access')));
        };

        const showLocation = () => {
            return (
                <div style={{marginTop: 10}}>
                    <RadioGroupInputField
                        options={[{label: 'Local File', value: LOCALFILE},
                                  {label: 'Workspace', value: WORKSPACE }] }
                        fieldKey={'fileLocation'}
                    />
                </div>
            );
        };

        return (
            <div style={{height: '100%', width: '100%'}}>
                <div>
                    {children}
                </div>
                <ValidationField
                    wrapperStyle={{marginTop: 10}}
                    size={40}
                    fieldKey={'fileName'}
                />

                {this.workspace && showLocation()}

                <div  style={{width: dialogWidth, height: dialogHeight}}>
                    {where === WORKSPACE && showWorkspace()}
                </div>
            </div>
        );
    }
}

DownloadOptionsDialog.propTypes = {
    fromGroupKey: PropTypes.string,
    children: PropTypes.object,
    fileName: PropTypes.string,
    labelWidth: PropTypes.number,
    dialogWidth: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    dialogHeight: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    workspace: PropTypes.oneOfType([PropTypes.bool, PropTypes.string])
};


/**
 * file name on download options dialog validator
 * @returns {Function}
 */
export function fileNameValidator() {
    return (valStr) => {
        const valid = (typeof valStr === 'string') ;
        const retRes = {valid};

        if (!retRes.valid) {
            //retRes.message = `the same file, ${valStr}, exists in workspace and is not writable`;
            retRes.message = 'illegal file name';
        }

        return retRes;
    };
}

export function validateFileName(wsSelect, fileName) {
    const fullPath = getWorkspacePath(wsSelect, fileName);

    if (isExistWorspaceFile(fullPath)) {
        workspacePopupMsg(`the file, ${fullPath}, already exists in workspace, please change the file name.`,
                          'Save to workspace');
        return false;
    } else {
        return true;
    }

}
