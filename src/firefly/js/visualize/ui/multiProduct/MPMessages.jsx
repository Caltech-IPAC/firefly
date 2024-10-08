import {Box, Stack, Typography} from '@mui/joy';
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
    let dMsg = singleDownload && (menu?.[0].name ?? '');
    if (dMsg && menu?.[0].fileType) dMsg = `${dMsg}, type: ${menu[0].fileType}`;
    let actionUrl = url;
    if (singleDownload && !url) actionUrl = isArray(menu) && menu.length && menu[0].url;

    return (
        <Stack {...{width: '100%', height: '100%'}}>
            <Box style={{height: menu ? 30 : 0}}>
                {makeDropDown && makeDropDown?.()}
            </Box>
            <Stack {...{direction: 'row', alignSelf: 'center', pt: 5}}>
                {isWorkingState ?
                    <Box sx={{width: 20, height: 20, mr: 1}} className='loading-animation'/> : ''}
                <Typography level='title-lg' sx={{alignSelf: 'center', textAlign:'center'}}>{message}</Typography>
            </Stack>
            {
                actionUrl && singleDownload && <CompleteButton sx={{alignSelf: 'center', pt: 3}} text={dMsg}
                                                  onSuccess={() => doDownload(actionUrl)}/>
            }
        </Stack>
    );
}

export function ComplexMessage({ menu, makeDropDown, message, resetMenuKey, dpId, activeMenuLookupKey, doResetButton,
                                   detailMsgAry = [], badUrl }) {
    return (
        <Stack {...{width: '100%', height: '100%'}}>
            <Box style={{height: menu ? 30 : 0}}>
                {makeDropDown && makeDropDown?.()}
            </Box>
            <Stack {...{alignSelf: 'center', pt: 5}}>
                <Typography {...{level:'body-sm', mb:1, alignSelf:'center'}}>{message}</Typography>
                {detailMsgAry.map((m) => (<Typography sx={{alignSelf: 'center', pt:.5}} key={m}>{m}</Typography>))}
                {badUrl &&
                    <Typography style={{alignSelf: 'left', fontSize: '12pt', paddingTop: 5, maxWidth: 200}} component='div'>
                        <span style={{whiteSpace: 'nowrap', paddingRight: 5}}>Failed URL:</span>
                        <a href={badUrl} target={'badURLTarget'}>
                            <Typography level='body-xs'>{badUrl}</Typography>
                        </a>
                    </Typography>
                }
            </Stack>
            {doResetButton && <CompleteButton style={{alignSelf: 'center', paddingTop: 25}} text='Reset'
                                              onSuccess={() => {
                                                  dispatchSetSearchParams({
                                                      dpId,
                                                      activeMenuLookupKey,
                                                      menuKey: resetMenuKey,
                                                      params: undefined
                                                  });
                                                  dispatchActivateMenuItem(dpId, resetMenuKey);
                                              }}/>}
        </Stack>
    );
}