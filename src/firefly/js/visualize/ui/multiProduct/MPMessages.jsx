import React from 'react';
import {isArray} from 'lodash';
import {dispatchActivateMenuItem, dispatchSetSearchParams, doDownload} from '../../../metaConvert/DataProductsCntlr.js';
import {CompleteButton} from '../../../ui/CompleteButton.jsx';



export function AdvancedMessage({dpId, dataProductsState, noProductMessage, doResetButton, makeDropDown}) {
    const {complexMessage, menu, message, noProductsAvailable= false, isWorkingState,
        singleDownload, activeMenuLookupKey, detailMsgAry, badUrl, resetMenuKey}= dataProductsState;


    if (complexMessage) {
        return (<ComplexMessage {...{menu, makeDropDown, message,
            detailMsgAry, badUrl, resetMenuKey, dpId, activeMenuLookupKey, doResetButton }} />);
    }
    else {
        const useMessage= noProductsAvailable && noProductMessage ? noProductMessage : message;
        return (<ProductMessage {...{menu, singleDownload, makeDropDown, isWorkingState, message:useMessage}} />);
    }
}


export function ProductMessage({menu, singleDownload, makeDropDown, isWorkingState=false, message, url}) {
    let dMsg = singleDownload && menu[0].name;
    if (dMsg && menu[0].fileType) dMsg = `${dMsg}, type: ${menu[0].fileType}`;
    let actionUrl = url;
    if (singleDownload && !url) actionUrl = isArray(menu) && menu.length && menu[0].url;

    return (
        <div style={{display: 'flex', flexDirection: 'column', background: '#c8c8c8', width: '100%', height: '100%'}}>
            <div style={{height: menu ? 30 : 0}}>
                {makeDropDown && makeDropDown?.()}
            </div>
            <div style={{display: 'flex', flexDirection: 'row', alignSelf: 'center', paddingTop: 40}}>
                {isWorkingState ?
                    <div style={{width: 20, height: 20, marginRight: 10}} className='loading-animation'/> : ''}
                <div style={{alignSelf: 'center', fontSize: '14pt'}}>{message}</div>
            </div>
            {
                singleDownload && <CompleteButton style={{alignSelf: 'center', paddingTop: 25}} text={dMsg}
                                                  onSuccess={() => doDownload(actionUrl)}/>
            }
        </div>
    );
}

export function ComplexMessage({ menu, makeDropDown, message, resetMenuKey, dpId, activeMenuLookupKey, doResetButton,
                                   detailMsgAry = [], badUrl }) {
    return (
        <div style={{display: 'flex', flexDirection: 'column', background: '#c8c8c8', width: '100%', height: '100%'}}>
            <div style={{height: menu ? 30 : 0}}>
                {makeDropDown && makeDropDown?.()}
            </div>
            <div style={{display: 'flex', flexDirection: 'column', alignSelf: 'center', paddingTop: 40}}>
                <div style={{alignSelf: 'center', fontSize: '14pt', paddingBottom: 10}}>{message}</div>
                {detailMsgAry.map((m) => (
                    <div style={{alignSelf: 'center', fontSize: '12pt', paddingTop: 5}} key={m}>{m}</div>))}
                {badUrl &&
                    <div style={{alignSelf: 'left', fontSize: '12pt', paddingTop: 5, maxWidth: 200}}>
                        <span style={{whiteSpace: 'nowrap', paddingRight: 5}}>Failed URL:</span>
                        <a href={badUrl} target={'badURLTarget'}>
                            <span style={{fontSize: '10pt'}}> {badUrl} </span>
                        </a>
                    </div>
                }
            </div>
            {doResetButton && <CompleteButton style={{alignSelf: 'center', paddingTop: 25}} text={'Reset'}
                                              onSuccess={() => {
                                                  dispatchSetSearchParams({
                                                      dpId,
                                                      activeMenuLookupKey,
                                                      menuKey: resetMenuKey,
                                                      params: undefined
                                                  });
                                                  dispatchActivateMenuItem(dpId, resetMenuKey);
                                              }}/>}
        </div>
    );
}