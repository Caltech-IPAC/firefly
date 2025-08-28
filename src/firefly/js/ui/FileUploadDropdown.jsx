/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState} from 'react';
import {Box, Skeleton} from '@mui/joy';
import {getAppOptions} from 'firefly/api/ApiUtil.js';
import {dispatchHideDialog, dispatchShowDialog} from 'firefly/core/ComponentCntlr.js';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {FieldGroup} from 'firefly/ui/FieldGroup';
import {defaultAcceptList, resultSuccess} from 'firefly/ui/FileUploadProcessor';
import {TABLES} from 'firefly/ui/FileUploadUtil';
import {PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import {FileUploadViewPanel, resultFail} from '../visualize/ui/FileUploadViewPanel.jsx';
import {FormPanel} from './FormPanel.jsx';

const panelKey = 'FileUploadAnalysis';
const tableOnlyDefaultAcceptList = [ TABLES ];

export const FileUploadDropdown= ({sx, onCancel, onSubmit=resultSuccess, keepState=true,
                                      initArgs,
                                      groupKey=panelKey,
                                      acceptOneItem=false,
                                      acceptList= getAppOptions()?.uploadPanelLimit==='tablesOnly'
                                          ? tableOnlyDefaultAcceptList
                                          : defaultAcceptList,
                                      }) =>{
    const [submitText,setSubmitText]= useState('Load');
    const [doMask, changeMasking]= useState(() => false);
    const helpId = getAppOptions()?.uploadPanelHelpId ?? 'basics.searching';
    return (
        <Box position='relative' sx={{width: 1, height:1, ...sx}}>
            <FieldGroup groupKey={groupKey} keepState={keepState} sx={{height:1, width:1,
                display: 'flex', alignItems: 'stretch', flexDirection: 'column'}}>
                <FormPanel
                    groupKey={groupKey} onSuccess={onSubmit} onError={resultFail}
                    onCancel={onCancel}
                    completeText={submitText}
                    cancelText={onCancel?'Cancel':''}
                    help_id={helpId}
                    slotProps={{
                        input: {height:1},
                        searchBar: {p:1},
                        completeBtn:{
                            changeMasking,
                        },
                    }}>

                    <FileUploadViewPanel {...{setSubmitText, acceptList, acceptOneItem,
                        externalDropEvent:initArgs?.searchParams?.dropEvent, initArgs}}/>
                </FormPanel>
            </FieldGroup>
            { doMask && <Skeleton sx={{inset:0, zIndex:10}}/> }
        </Box>
    );
};

const DIALOG_ID= 'FileUploadDialog';

export function showUploadDialog(acceptList, keepState, groupKey, acceptOneItem) {

    DialogRootContainer.defineDialog(DIALOG_ID,
        <PopupPanel title={'Upload'}
                    closeCallback={
                        () => {
                        }
                    }>
            <Box sx={{resize:'both', overflow: 'hidden', zIndex:1, minWidth:600, minHeight:700}} >
                <FileUploadDropdown
                    sx={{height: 1}}
                    onCancel={() => dispatchHideDialog(DIALOG_ID)}
                    onSubmit={
                        (request) => {
                            if (resultSuccess(request)) dispatchHideDialog(DIALOG_ID);
                        }
                    }
                    keepState={keepState}
                    groupKey={groupKey}
                    acceptList={acceptList}
                    acceptOneItem={acceptOneItem}
                />
            </Box>
        </PopupPanel>
    );
    dispatchShowDialog(DIALOG_ID);
}
