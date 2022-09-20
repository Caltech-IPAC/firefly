/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {isEmpty ,get} from 'lodash';
import {ValidationField} from './ValidationField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import FieldGroupUtils, {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {getWorkspaceList, getWorkspaceErrorMsg,
        dispatchWorkspaceUpdate, isAccessWorkspace} from '../visualize/WorkspaceCntlr.js';
import {WorkspaceSave} from './WorkspaceViewer.jsx';


import LOADING from 'html/images/gxt/loading.gif';
import {useStoreConnector} from 'firefly/ui/SimpleComponent';
import {flux} from "firefly/core/ReduxFlux";
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


export function DownloadOptionsDialog({fromGroupKey, children, fileName, labelWidth, dialogWidth=500, dialogHeight=300,
                                      workspace}) {

    workspace = get({fromGroupKey, children, fileName, labelWidth, dialogWidth, dialogHeight,
        workspace}, 'workspace', false);
    const [isUpdating, setIsUpdating] = useState(() => isAccessWorkspace());
    const [where, setWhere] = useState(() => fromGroupKey? getFieldVal(fromGroupKey, 'fileLocation', LOCALFILE) : LOCALFILE);
    const [wsSelect, setWsSelect] = useState(() => (where === WORKSPACE) ? getFieldVal(fromGroupKey, 'wsSelect', '') : '');
    const [wsList, setWsList] = useState(() => isUpdating ? '' : getWorkspaceList());

    const storeUpdate = (prev={}) => {
            const isUpdating = isAccessWorkspace();
            const wsList = getWorkspaceList();
            const loc = fromGroupKey && getFieldVal(fromGroupKey, 'fileLocation');
            const wsSelect = fromGroupKey && getFieldVal(fromGroupKey, 'wsSelect');

            if (prev.isUpdating === isUpdating && prev.wsList === wsList && prev.loc === loc
                && prev.wsSelect === wsSelect) {
                return prev; //nothing has changed, return old state
            }
            return {isUpdating, wsList, loc, wsSelect};
    };

    const {isUpdatingTemp, wsListTemp, loc, wsSelectTemp} = useStoreConnector((prev) => storeUpdate(prev)); //replaced flux.addListener()

    useEffect(() => {
        if (loc !== where && loc) {
            setWhere(loc);
        }
    }, [loc]);

    useEffect(() => {
        if (isUpdating !== isUpdatingTemp) {
            setIsUpdating(isUpdatingTemp);
        }
    }, [isUpdatingTemp]);

    useEffect(() => {
        if (wsList !== wsListTemp) {
            setWsList(wsListTemp);
        }
    }, [wsListTemp]);

    useEffect(() => {
        if (wsSelect !== wsSelectTemp) {
            setWsSelect(wsSelectTemp);
        }
    }, [wsSelectTemp]);

    useEffect(() => {
        if (loc ===  WORKSPACE) {
            setIsUpdating(isUpdatingTemp);
            dispatchWorkspaceUpdate();
        }
    }, [where]);

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

        const showNoWSFiles = (message) => {
            return (
                <div style={{marginTop: 10,
                             padding: 10,
                             boxSizing: 'border-box',
                             width: 'calc(100%)',
                             textAlign: 'center',
                             border:'1px solid #a3aeb9'}}>
                    {message}
                </div>
            );
        };

        return (
            (isUpdating) ? loading() :
                (!isEmpty(wsList) ? showSave() : showNoWSFiles('Workspace access error: ' + getWorkspaceErrorMsg()))
        );
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
                size={50}
                fieldKey={'fileName'}
            />

            {workspace && showLocation()}

            <div  style={{width: dialogWidth, height: dialogHeight}}>
                {where === WORKSPACE && showWorkspace()}
            </div>
        </div>
    );
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

