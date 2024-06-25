/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Sheet, Stack, Typography} from '@mui/joy';
import React from 'react';
import {isString} from 'lodash';
import CompleteButton from './CompleteButton.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {ModalDialog} from './ModalDialog.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import DialogRootContainer from './DialogRootContainer.jsx';

export const INFO_POPUP= 'InfoPopup';

// ------------------------------------------------------------
// ------------------------------------------------------------
// More types of popup can be added here
// ------------------------------------------------------------
// ------------------------------------------------------------

export const MODAL_DIALOG_ID = 'ModalDialog';
export const POPUP_DIALOG_ID = 'ModalDialog';


/**
 * Creates and shows the modal dialog.
 * @param {string | object}  content can be a string or a React component
 * @param {boolean} [show=true] show or hide this dialog
 */
export function showModal(content, show=true) {
    if (!show) {
        dispatchHideDialog(MODAL_DIALOG_ID);
        return;
    }
    DialogRootContainer.defineDialog(MODAL_DIALOG_ID, <ModalDialog>{content}</ModalDialog>);
    dispatchShowDialog(MODAL_DIALOG_ID);
}

export function showTmpModal(content, displayTime=3000) {
    showModal(content);
    setTimeout( () => showModal(undefined,false), displayTime);
}

export function showPinMessage(text) {
    showTmpModal( ( <Typography color='primary' level='h2'>{text}</Typography> ), 500 );
}

/**
 * Creates and shows the modal dialog.
 * @param {object} p
 * @param {string | object}  p.content can be a string or a React component
 * @param {string} [p.title] popup title
 * @param {boolean} [p.modal=false] when true, glass panel will be under the popup
 * @param {boolean} [p.show=true] show or hide this dialog
 */
export function showOptionsPopup({content, title='Options', modal = false, show=true}) {
    show? showPopup({ID: POPUP_DIALOG_ID, content, title, modal}) : dispatchHideDialog(POPUP_DIALOG_ID);
}

/**
 * Creates and shows the modal dialog.
 * @param {object} p
 * @param {string} [p.ID] ID of the popup dialog
 * @param {string | object}  p.content can be a string or a react component
 * @param {string} [p.title] popup title
 * @param {boolean} [p.modal=false] when true, glass panel will be under the popup
 * @return {function} return function to hide the popup
 */
export function showPopup({ID, content, title='Options', modal = false}) {
    const dialogContent= (
        <PopupPanel title={title} modal={modal}>
            {content}
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(ID, dialogContent);
    dispatchShowDialog(ID);
    return () => dispatchHideDialog(ID);
}


/**
 * Show a simple information popup
 *
 * @param {string | object} content can be a string or a react component
 * @param {string} title
 * @param {Object} sx
 * @return {object}
 */
export function showInfoPopup(content, title='Information', sx) {
    const results= ( <PopupPanel title={title} sx={sx}> {makeContent(content)} </PopupPanel> );
    DialogRootContainer.defineDialog(INFO_POPUP, results);
    dispatchShowDialog(INFO_POPUP);
}

/**
 * Hide the info popup
 */
export function hideInfoPopup() {
    dispatchHideDialog(INFO_POPUP);
}

function makeContent(content) {
    return (
        <Stack {...{px:2, py:1, spacing:2, className:'FF-Popup-Content-root'}}>
            <Stack {...{className:'FF-Popup-Content', minWidth:350, maxWidth: 500, overflow: 'hidden'}}>
                {isString(content) ? ( <Typography level='body-md'>{content}</Typography> ) : content}
            </Stack>
            <CompleteButton dialogId={INFO_POPUP} />
        </Stack>
    );
}

export function showYesNoPopup(content, clickSelection,  title='Information') {
    const results= (
        <PopupPanel title={title} >
            {makeYesNoContent(content, clickSelection)}
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(INFO_POPUP, results);
    dispatchShowDialog(INFO_POPUP);
}

function makeYesNoContent(content, clickSelection) {
    return (
        <Stack direction='column' p={1} spacing={2}>
            <Sheet style={{minWidth:190, maxWidth: 400, p:1}}>
                {content}
            </Sheet>
            <Stack direction='row' spacing={1}>
                <CompleteButton onSuccess={() => clickSelection(INFO_POPUP, true)} text= 'Yes'/>
                <CompleteButton onSuccess={() => clickSelection(INFO_POPUP, false)} text= 'No'/>
            </Stack>
        </Stack>
    );
}
