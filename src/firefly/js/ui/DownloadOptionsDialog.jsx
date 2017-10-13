/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {ValidationField} from './ValidationField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {getWorkspaceList, isExistWorspaceFile, getWorkspacePath} from '../visualize/WorkspaceCntlr.js';
import {WorkspaceSave, workspacePopupMsg} from './WorkspaceViewer.jsx';

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

        const where = props.fromGroupKey ? getFieldVal(props.fromGroupKey, 'fileLocation', LOCALFILE)
                                         : LOCALFILE;
        const fileOverwritable =  props.fromGroupKey ? getFieldVal(props.fromGroupKey, 'fileOverwritable', 0) : 0;
        const wsSelect = getFieldVal(props.fromGroupKey, 'wsSelect', '');
        this.state = {where, fileName: props.fileName, wsSelect, fileOverwritable, wsList: getWorkspaceList()};
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
            const wsList = getWorkspaceList();
            const loc = this.props.fromGroupKey && getFieldVal(this.props.fromGroupKey, 'fileLocation');
            const wsSelect = this.props.fromGroupKey && getFieldVal(this.props.fromGroupKey, 'wsSelect');

            this.setState((state) => {
                if (wsList !== state.wsList) {
                    state.wsList = wsList;
                }
                if (loc !== state.where) {
                    state.where = loc;
                }
                if (wsSelect !== state.wsSelect) {
                    state.wsSelect = wsSelect;
                }
                return state;
            });
        }
    }


    render() {
        const {where, wsSelect, wsList} = this.state;
        const {children, labelWidth=100, dialogWidth=400} = this.props;

        return (
            <div style={{width: dialogWidth}}>
                <div>
                    {children}
                </div>
                <ValidationField
                    wrapperStyle={{marginTop: 10}}
                    size={(dialogWidth - labelWidth - 10)/10}
                    fieldKey={'fileName'}
                />

                <div style={{marginTop: 10}}>
                   <RadioGroupInputField
                        options={[{label: 'Local File', value: LOCALFILE},
                                  {label: 'Workspace', value: WORKSPACE }] }
                        fieldKey={'fileLocation'}/>
                 </div>

                <div>
                    {(where === WORKSPACE) && (
                        <div style={{display: 'flex', flexDirection: 'column'}}>
                            <div style={{marginTop: 10,
                                         width: (dialogWidth-30), height: 200,
                                         overflow: 'auto',
                                         padding: 10,
                                         border:'1px solid #a3aeb9'
                                         }}>
                                <WorkspaceSave fieldKey={'wsSelect'}
                                               files={wsList}
                                               value={wsSelect}
                                               />
                            </div>
                        </div>)}
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
    dialogWidth: PropTypes.number
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
