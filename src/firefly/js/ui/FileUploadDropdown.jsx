/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState} from 'react';
import {FormPanel} from './FormPanel.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {panelKey, FileUploadViewPanel, resultSuccess, resultFail} from '../visualize/ui/FileUploadViewPanel.jsx';
import {getAppOptions} from 'firefly/api/ApiUtil.js';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {dispatchHideDialog, dispatchShowDialog} from 'firefly/core/ComponentCntlr.js';
import {PopupPanel} from 'firefly/ui/PopupPanel.jsx';

const maskWrapper= { position:'absolute', left:0, top:0, width:'100%', height:'100%' };

export const FileUploadDropdown= ({style={}, onCancel=dispatchHideDropDown, onSubmit=resultSuccess}) =>{
    const [submitText,setSubmitText]= useState('Load');
    const [doMask, changeMasking]= useState(() => false);
    const helpId = getAppOptions()?.uploadPanelHelpId ?? 'basics.searching';
    return (
        <div style={{width: '100%', ...style}}>
            <FormPanel
                width='auto' height='auto' groupKey={panelKey} onSubmit={onSubmit}
                onError={resultFail}
                onCancel={onCancel}
                submitText={submitText}
                params={{hideOnInvalid: false}}
                changeMasking={changeMasking}
                inputStyle={{height:'100%'}}
                submitBarStyle={{padding: '2px 3px 3px'}} help_id={helpId}>
                <FileUploadViewPanel setSubmitText={setSubmitText}/>
            </FormPanel>
            {doMask && <div style={maskWrapper}> <div className='loading-mask'/> </div> }
        </div>
    );
};

const DIALOG_ID= 'FileUploadDialog';

export function showUploadDialog() {

    DialogRootContainer.defineDialog(DIALOG_ID,
        <PopupPanel title={'Upload'}
                    closeCallback={
                        () => {
                        }
                    }>
            <div style={{resize:'both', overflow: 'hidden', zIndex:1, minWidth:600, minHeight:700}} >
                <FileUploadDropdown
                    style={{height: '100%'}}
                    onCancel={() => dispatchHideDialog(DIALOG_ID)}
                    onSubmit={() => {
                        dispatchHideDialog(DIALOG_ID);
                        resultSuccess();
                    }}
                />
            </div>
        </PopupPanel>
    );
    dispatchShowDialog(DIALOG_ID);
}
