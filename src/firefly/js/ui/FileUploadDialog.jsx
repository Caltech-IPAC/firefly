/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {TargetPanel} from './TargetPanel.jsx';
import {InputGroup} from './InputGroup.jsx';
import Validate from '../util/Validate.js';
import {ValidationField} from './ValidationField.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {SuggestBoxInputField} from './SuggestBoxInputField.jsx';
import {Histogram} from '../charts/ui/Histogram.jsx';
import CompleteButton from './CompleteButton.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import {dispatchMultiValueChange, dispatchRestoreDefaults} from '../fieldGroup/FieldGroupCntlr.js';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import FieldGroupUtils, {revalidateFields} from '../fieldGroup/FieldGroupUtils';
import {FileUpload} from './FileUpload.jsx';
import {FileUploadViewPanel} from '../visualize/ui/FileUploadViewPanel.jsx';
import {FileUploadDropdown} from '../ui/FileUploadDropdown.jsx';

import {CollapsiblePanel} from './panel/CollapsiblePanel.jsx';
import {Tabs, Tab,FieldGroupTabs} from './panel/TabPanel.jsx';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';



function getDialogBuilder() {
    var popup= null;
    return () => {
        if (!popup) {
            const popup= (
                <PopupPanel title={'File Upload Dialog'} >
                    <FileUploadDropdown  groupKey={'FILEUPLOAD_FORM'} />
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

