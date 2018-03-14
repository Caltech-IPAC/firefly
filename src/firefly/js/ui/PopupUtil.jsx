/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
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

/**
 * Creates and shows the modal dialog.
 * @param {string | object}  content can be a string or a react component
 * @param {boolean} [show=true] show or hide this dialog
 */
export function showModal(content, show=true) {

    const popTitle = 'Plot Parameters';

    if (show) {

        const dialogContent= (
            <PopupPanel title={popTitle} >
            <ModalDialog>
                {content}
            </ModalDialog>
            </PopupPanel>
        );
        DialogRootContainer.defineDialog(MODAL_DIALOG_ID, dialogContent);

        dispatchShowDialog(MODAL_DIALOG_ID);
    } else {
        dispatchHideDialog(MODAL_DIALOG_ID);
    }
}


/**
 * Show a simple information popup
 * @param {string | object} content can be a string or a react component
 * @param {string} title
 * @return {object}
 */
export function showInfoPopup(content, title='Information') {
    var results= (
        <PopupPanel title={title} >
            {makeContent(content)}
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(INFO_POPUP, results);
    dispatchShowDialog(INFO_POPUP);
}

function makeContent(content) {
    return (
        <div style={{padding:5}}>
            <div style={{minWidth:190, maxWidth: 400, padding:10, fontSize:'120%'}}>
                {content}
            </div>
            <div style={{padding:'0 0 5px 10px'}}>
                <CompleteButton dialogId={INFO_POPUP} />
            </div>
        </div>
    );
}

export function showYesNoPopup(content, clickSelection,  title='Information') {
    var results= (
        <PopupPanel title={title} >
            {makeYesNoContent(content, clickSelection)}
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(INFO_POPUP, results);
    dispatchShowDialog(INFO_POPUP);
}

function makeYesNoContent(content, clickSelection) {
    return (
        <div style={{padding:5}}>
            <div style={{minWidth:190, maxWidth: 400, padding:10, fontSize:'120%'}}>
                {content}
            </div>
            <div style={{display: 'flex'}}>
                <div style={{padding:'0 0 5px 10px'}}>
                    <button type='button' className='button std hl'
                            onClick={clickSelection(INFO_POPUP, true)}>Yes
                    </button>
                </div>
                <div style={{padding:'0 0 5px 10px'}}>
                    <button type='button' className='button std hl'
                            onClick={clickSelection(INFO_POPUP, false)}>No
                    </button>
                </div>
            </div>
        </div>
    );
}
