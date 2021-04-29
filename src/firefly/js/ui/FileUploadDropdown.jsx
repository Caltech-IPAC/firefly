/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState} from 'react';
import {FormPanel} from './FormPanel.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {panelKey, FileUploadViewPanel, resultSuccess, resultFail} from '../visualize/ui/FileUploadViewPanel.jsx';
import {getAppOptions} from 'firefly/api/ApiUtil.js';

const dropdownName = 'FileUploadDropDownCmd';

const maskWrapper= { position:'absolute', left:0, top:0, width:'100%', height:'100%' };

export const FileUploadDropdown= () =>{
    const [doMask, changeMasking]= useState(() => false);
    const helpId = getAppOptions()?.uploadPanelHelpId ?? 'basics.searching';
    return (
        <div style={{width: '100%'}}>
            <FormPanel
                width='auto' height='auto' groupKey={panelKey} onSubmit={resultSuccess}
                onError={resultFail} onCancel={dispatchHideDropDown} params={{hideOnInvalid: false}}
                changeMasking={changeMasking} submitText={'Load'}
                inputStyle={{height:'100%'}}
                submitBarStyle={{padding: '2px 3px 3px'}} help_id={helpId}>
                <FileUploadViewPanel/>
            </FormPanel>
            {doMask && <div style={maskWrapper}> <div className='loading-mask'/> </div> }
        </div>
    );
};
