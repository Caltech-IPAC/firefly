/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {isUndefined, isString} from 'lodash';
import CompleteButton from './CompleteButton.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {ModalDialog} from './ModalDialog.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import DialogRootContainer from './DialogRootContainer.jsx';

export const INFO_POPUP= 'InfoPopup';
import './PopupPanel.css';

// ------------------------------------------------------------
// ------------------------------------------------------------
// More types of popup can be added here
// ------------------------------------------------------------
// ------------------------------------------------------------

export const MODAL_DIALOG_ID = 'ModalDialog';
export const POPUP_DIALOG_ID = 'ModalDialog';


/**
 * Creates and shows the modal dialog.
 * @param {string | object}  content can be a string or a react component
 * @param {boolean} [show=true] show or hide this dialog
 */
export function showModal(content, show=true) {
    if (show) {
        const dialogContent= (
            <ModalDialog>
                    {content}
            </ModalDialog>
        );
        DialogRootContainer.defineDialog(MODAL_DIALOG_ID, dialogContent);

        dispatchShowDialog(MODAL_DIALOG_ID);
    } else {
        dispatchHideDialog(MODAL_DIALOG_ID);
    }
}

export function showTmpModal(content, displayTime=3000) {
    showModal(content);
    setTimeout( () => showModal(undefined,false), displayTime);
}

export function showPinMessage(text) {
    showTmpModal(
        ( <div style={{fontSize:'16pt', padding: 10}}> {text} </div> ), 500 );
}

/**
 * Creates and shows the modal dialog.
 * @param {object} p
 * @param {string | object}  p.content can be a string or a react component
 * @param {string} [p.title] popup title
 * @param {boolean} [p.modal=false] when true, glass panel will be under the popup
 * @param {boolean} [p.show=true] show or hide this dialog
 */
export function showOptionsPopup({content, title='Options', modal = false, show=true}) {

  if (show) {
      const dialogContent= (
          <PopupPanel title={title} modal={modal}>

                  {content}

          </PopupPanel>
      );
    DialogRootContainer.defineDialog(POPUP_DIALOG_ID, dialogContent);

    dispatchShowDialog(POPUP_DIALOG_ID);
  } else {
    dispatchHideDialog(POPUP_DIALOG_ID);
  }
}
/**
 * Show a simple information popup
 *
 * @param {string | object} content can be a string or a react component
 * @param {string} title
 * @param {boolean} [isCopyable] true or false, if undefined, then the default to true if it is a string
 * @return {object}
 */
export function showInfoPopup(content, title='Information', isCopyable=undefined) {
    if (isUndefined(isCopyable)) isCopyable = isString(content);
    var results= (
        <PopupPanel title={title} >
            {makeContent(content, isCopyable)}
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(INFO_POPUP, results);
    dispatchShowDialog(INFO_POPUP);
}

/**
 * Hide the info popup
 */
export function hideInfoPopup() {
    dispatchHideDialog(INFO_POPUP);
}

function makeContent(content, isCopyable) {
    return (
        <div style={{padding:5}}>
            <div style={{minWidth:350, maxWidth: 400, padding:'10px 10px 15px 10px', fontSize:'120%', overflow: 'hidden'}}
                 className={isCopyable ? 'popup-panel-textcopyable' : ''}>
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
                    <button type='button' className='button std hl' style={{width: 50}}
                            onClick={() => clickSelection(INFO_POPUP, true)}>Yes
                    </button>
                </div>
                <div style={{padding:'0 0 5px 10px'}}>
                    <button type='button' className='button std hl' style={{width: 50}}
                            onClick={() => clickSelection(INFO_POPUP, false)}>No
                    </button>
                </div>
            </div>
        </div>
    );
}
