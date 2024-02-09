/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Stack} from '@mui/joy';
import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {isEmpty} from 'lodash';
import {ValidationField} from './ValidationField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {getWorkspaceList, getWorkspaceErrorMsg,
        dispatchWorkspaceUpdate, isAccessWorkspace} from '../visualize/WorkspaceCntlr.js';
import {WorkspaceSave} from './WorkspaceViewer.jsx';
import {useFieldGroupValue, useStoreConnector} from 'firefly/ui/SimpleComponent';
import LOADING from 'html/images/gxt/loading.gif';

export const LOCALFILE = 'isLocal';
export const WORKSPACE = 'isWs';

export function getTypeData(key, val='', tip = '', labelV='', labelW='') {
    return {
        fieldKey: key,
        label: labelV,
        value: val,
        tooltip: tip,
        labelWidth: labelW
    };
}


export function DownloadOptionsDialog({fromGroupKey, children, fileName, labelWidth, style={}, dialogWidth, dialogHeight,
                                      workspace}) {

    const isUpdating = useStoreConnector(isAccessWorkspace);
    const wsList = useStoreConnector(getWorkspaceList);
    const [getLoc] = useFieldGroupValue('fileLocation', fromGroupKey);
    const where= fromGroupKey && getLoc();
    const [getWs] = useFieldGroupValue('wsSelect', fromGroupKey);
    const wsSelect= fromGroupKey && getWs();

    useEffect(() => {
        if (where ===  WORKSPACE) {
            dispatchWorkspaceUpdate();
        }
    }, [where]);

   //Todo: for workspace related components, they will be included in another ticket Firefly-1400
    const ShowWorkspace = () => {

        const loading  = (
                <div style={{width: '100%', height: '100%', display:'flex', justifyContent: 'center', alignItems: 'center'}}>
                    <img style={{width:14,height:14}} src={LOADING}/>
                </div>
        );

        const showSave = (
            <div  style={{width: dialogWidth, height: dialogHeight}}>
                <div style={{marginTop: 10,
                             boxSizing: 'border-box',
                             width: 'calc(100%)', height: 'calc(100% - 10px)',
                             overflow: 'auto',
                             padding: 5,
                             border:'1px solid #a3aeb9'}}>
                    <WorkspaceSave fieldKey={'wsSelect'} files={wsList} value={wsSelect}
                        tooltip='workspace file system'/>
                </div>
            </div>
        );

        const showNoWSFiles = (
                <div style={{marginTop: 10,
                             padding: 10,
                             boxSizing: 'border-box',
                             width: 'calc(100%)',
                             textAlign: 'center',
                             border:'1px solid #a3aeb9'}}>
                    {'Workspace access error: ' + getWorkspaceErrorMsg()}
                </div>
        );

        return isUpdating ? loading : !isEmpty(wsList) ? showSave : showNoWSFiles;
    };

    const showLocation = (
        <Stack spacing={2} sx={{'.MuiFormLabel-root': {width: labelWidth}}}>
                <RadioGroupInputField
                    options={[{label: 'Local File', value: LOCALFILE},
                              {label: 'Workspace', value: WORKSPACE }] }
                    fieldKey={'fileLocation'}
                    orientation='horizontal'
                    label='File location:'
                    tooltip='select the location where the file is downloaded to'
                />
        </Stack>
    );

    return (
        <Stack spacing={2} justifyContent={'center'}
               sx={{
                 width: '80%',
               }}>
            <div>
                {children}
            </div>
            <ValidationField
                fieldKey={'fileName'}
                initialState= {{
                    value: fileName
                }}
                label='File name'
                tooltip='Please enter a filename, a default name will be used if it is blank'
            />

            {workspace && showLocation}

            {where === WORKSPACE && <ShowWorkspace/>}
        </Stack>
    );
}

DownloadOptionsDialog.propTypes = {
    fromGroupKey: PropTypes.string,
    children: PropTypes.object,
    fileName: PropTypes.string,
    labelWidth: PropTypes.string,
    dialogWidth: PropTypes.oneOfType([PropTypes.string, PropTypes.string]),
    dialogHeight: PropTypes.oneOfType([PropTypes.string, PropTypes.string]),
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

