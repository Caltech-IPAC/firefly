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

export const LOCALFILE = 'isLocal';
export const WORKSPACE = 'isWs';


/**
 * This intend of this module is to group commonly used components related to workspace.
 * The components should be written so that composition is possible.
 */



/*
 * A selection pane used for saving a file either to local disk or workspace.
 * There are 3 values that may come out of this selection pane.
 * 1. where it should be saved(isLocal or isWs).  default keyed as 'fileLocation'
 * 2. what is should be save as.  default keyed as 'fileName'
 * 3. if workspace, path to the folder to save it to.  default keyed as 'wsSelect'
 */
export function WsSaveOptions (props) {
    const {groupKey, style, wsSelectKey='wsSelect', labelWidth} = props;
    let {fileLocProps={}, saveAsProps={}} = props;

    saveAsProps = {fieldKey:'fileName', label:'Save as:', labelWidth, wrapperStyle:{margin: '3px 0'}, size:30, ...saveAsProps};
    fileLocProps = {fieldKey:'fileLocation', label:'File Location:', labelWidth, ...fileLocProps};

    const [loc, wsSelect] = useStoreConnector(  () => getFieldVal(groupKey, fileLocProps.fieldKey),
                                                () => getFieldVal(groupKey, wsSelectKey));

    useEffect(() => {
        dispatchWorkspaceUpdate();
    }, [loc]);


    return (
        <div style={style}>
            <ValidationField {...saveAsProps}/>
            <RadioGroupInputField
                {...fileLocProps}
                options={[
                    {label: 'Local File', value: LOCALFILE},
                    {label: 'Workspace', value: WORKSPACE }
                ]}
            />
            {loc === WORKSPACE && <ShowWorkspace {...{wsSelect, wsSelectKey}}/>}
        </div>
    );

}

WsSaveOptions.propTypes = {
    groupKey:       PropTypes.string.isRequired,
    style:          PropTypes.string,
    labelWidth:     PropTypes.number,
    fileLocProps:   PropTypes.object,       // properties to send into the File Location radio button..  see render function for defaults
    saveAsProps:    PropTypes.object,       // properties to send into the Save As input box..  see render function for defaults
    wsSelectKey:    PropTypes.string
};



function ShowWorkspace({wsSelect, wsSelectKey}) {

    const [wsList, isUpdating] = useStoreConnector(getWorkspaceList, isAccessWorkspace);

    const content = isUpdating ? <div className='loading-mask'/>
                    : isEmpty(wsList) ? <div style={{color:'maroon', fontStyle:'italic', padding:10}}> {'Workspace access error: ' + getWorkspaceErrorMsg()} </div>
                    : <WorkspaceSave fieldKey={wsSelectKey} files={wsList} value={wsSelect} />;

    return (
        <div style={{ border:'1px solid #eaeaea', padding:3, position:'relative', minHeight:60, minWidth:550}}>
            {content}
        </div>
    );
}





































