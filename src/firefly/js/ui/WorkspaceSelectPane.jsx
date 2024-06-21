/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {isEmpty} from 'lodash';

import {ValidationField} from './ValidationField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {getWorkspaceList, isAccessWorkspace} from '../visualize/WorkspaceCntlr.js';
import {WorkspaceSave} from './WorkspaceViewer.jsx';
import {useStoreConnector} from './SimpleComponent.jsx';
import {dispatchWorkspaceUpdate, getWorkspaceErrorMsg} from '../visualize/WorkspaceCntlr.js';
import {getWorkspaceConfig} from '../visualize/WorkspaceCntlr.js';
import {Stack, Typography, Box, Skeleton} from '@mui/joy';

export const LOCALFILE = 'isLocal';
export const WORKSPACE = 'isWs';


/**
 * This intend of this module is to group commonly used components related to workspace.
 * The components should be written so that composition is possible.
 */


/*
 * A selection pane used for saving a file either to local disk or workspace.
 * If workspace is not used, that option is not shown and only Save As is displayed.
 * The magic keys below are used in other places to mean what is used here.  It cannot be replace with arbitrary keys.
 * There are 3 values that may come out of this selection pane.
 * 1. fileLocation: where it should be saved(isLocal or isWs)
 * 2. BaseFileName: what is should be save as.
 * 3. wsSelect:     if workspace, path to the folder to save it to.
 */
export function WsSaveOptions (props) {
    const {groupKey, style, labelWidth} = props;
    let {fileLocProps={}, saveAsProps={}} = props;

    saveAsProps = {label:'Save as:', labelWidth, wrapperStyle:{margin: '3px 0'}, size:30, ...saveAsProps};
    fileLocProps = {label:'File Location:', labelWidth, ...fileLocProps};

    const loc      = useStoreConnector(() => getFieldVal(groupKey, 'fileLocation'));
    const wsSelect = useStoreConnector(() => getFieldVal(groupKey, 'wsSelect'));

    useEffect(() => {
        dispatchWorkspaceUpdate();
    }, [loc]);

    const useWs = getWorkspaceConfig();

    return (
        <Stack style={style} spacing={1}>
            <ValidationField fieldKey='BaseFileName' forceReinit={true} {...saveAsProps}/>
            { useWs &&
                <RadioGroupInputField
                    fieldKey='fileLocation'
                    {...fileLocProps}
                    orientation={'horizontal'}
                    options={[
                        {label: 'Local File', value: LOCALFILE},
                        {label: 'Workspace', value: WORKSPACE }
                    ]}
                />
            }
            { useWs && (loc === WORKSPACE) && <ShowWorkspace {...{wsSelect}}/> }
        </Stack>
    );

}

WsSaveOptions.propTypes = {
    groupKey:       PropTypes.string.isRequired,
    style:          PropTypes.string,
    labelWidth:     PropTypes.number,
    fileLocProps:   PropTypes.object,       // properties to send into the File Location radio button..  see render function for defaults
    saveAsProps:    PropTypes.object        // properties to send into the Save As input box..  see render function for defaults
};


function ShowWorkspace({wsSelect}) {

    const wsList     = useStoreConnector(getWorkspaceList);
    const isUpdating = useStoreConnector(isAccessWorkspace);
    const content = isUpdating ? <Skeleton sx={{inset:0}}/>
                    : isEmpty(wsList) ? <Typography color={'warning'} level='title-md' p={1}> {'Workspace access error: ' + getWorkspaceErrorMsg()} </Typography>
                    : <WorkspaceSave fieldKey='wsSelect' files={wsList} value={wsSelect} />;

    return (
        <Box {...{padding:1/2, marginTop:2, position:'relative', overflow: 'auto', minHeight:60, maxHeight:400, minWidth:550}}>
            {content}
        </Box>
    );
}