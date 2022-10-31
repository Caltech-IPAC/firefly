/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useContext, useState} from 'react';
import {FormPanel} from './FormPanel.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {
    panelKey,
    FileUploadViewPanel,
    resultFail
} from '../visualize/ui/FileUploadViewPanel.jsx';
import {getAppOptions} from 'firefly/api/ApiUtil.js';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {dispatchHideDialog, dispatchShowDialog} from 'firefly/core/ComponentCntlr.js';
import {PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import {resultSuccess} from 'firefly/ui/FileUploadProcessor';
import {FieldGroup, FieldGroupCtx} from 'firefly/ui/FieldGroup';

const maskWrapper= { position:'absolute', left:0, top:0, width:'100%', height:'100%' };

export const FileUploadDropdown= ({style={}, onCancel=dispatchHideDropDown, onSubmit=resultSuccess}) =>{ //onSubmit=resultSuccess
    const [submitText,setSubmitText]= useState('Load');
    const [doMask, changeMasking]= useState(() => false);
    const helpId = getAppOptions()?.uploadPanelHelpId ?? 'basics.searching';
    //onSubmit={onSubmit}
    return (

        <div style={{width: '100%', ...style}}>
            <FieldGroup groupKey={panelKey} keepState={true} style={{height:'100%', width: '100%',
                display: 'flex', alignItems: 'stretch', flexDirection: 'column'}}>
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
            </FieldGroup>
            {doMask && <div style={maskWrapper}> <div className='loading-mask'/> </div> }
        </div>


    );
};

const DIALOG_ID= 'FileUploadDialog';

export function showUploadDialog() {

    //can call this showUploadDialog from within the "Add MOC layer" popup dialog for HiPS/MOC upload...
    //removing the onCancel and onSubmit below from the call to FileUploadDropdown fixes it so that the  req obj
    //is not empty when resultSuccess is called from FileUploadDropdown
    //but showUploadDialog is only called from HiPsImageSelect.jsx if (visRoot().apiToolsView) -> what is that if condition?

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
