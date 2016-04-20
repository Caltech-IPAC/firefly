/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import CompleteButton from './CompleteButton.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';
import DialogRootContainer from './DialogRootContainer.jsx';

const INFO_POPUP= 'InfoPopup';


// ------------------------------------------------------------
// ------------------------------------------------------------
// More types of popup can be added here
// ------------------------------------------------------------
// ------------------------------------------------------------


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
            <div style={{maxWidth: 400, padding:10, fontSize:'120%'}}>
                {content}
            </div>
            <div style={{padding:'0 0 5px 10px'}}>
                <CompleteButton dialogId={INFO_POPUP} />
            </div>
        </div>
    );
}
