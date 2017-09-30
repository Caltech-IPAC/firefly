/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {FileUploadSelectPanel} from '../visualize/ui/FileUploadSelectPanel.jsx';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';



function getDialogBuilder() {
    var popup= null;
    return () => {
        if (!popup) {
            const popup= (
                <PopupPanel title={'File Upload Dialog'} >
                    <FileUploadSelectPanel  groupKey={'FileUploadSelect'} />
                </PopupPanel>
            );
            DialogRootContainer.defineDialog('FileUploadDialog', popup);
        }
        return popup;
    };
}

const dialogBuilder= getDialogBuilder();

export function showFileUploadDialog() {
    dialogBuilder();
    dispatchShowDialog('FileUploadDialog');
}

