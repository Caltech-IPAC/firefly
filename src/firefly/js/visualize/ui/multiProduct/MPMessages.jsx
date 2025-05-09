import {Box, Button, Link, Stack, Typography} from '@mui/joy';
import {isObject, isString} from 'lodash';
import React, {useEffect, useState} from 'react';
import {
    dispatchActivateMenuItem, dispatchSetSearchParams, doDownload
} from '../../../metaConvert/DataProductsCntlr.js';
import {DPtypes} from '../../../metaConvert/DataProductsType';
import {getTextFile} from '../../../rpc/CoreServices';
import {CompleteButton} from '../../../ui/CompleteButton.jsx';
import {showInfoPopup} from '../../../ui/PopupUtil';
import {isURL, MEG} from '../../../util/WebUtil';



export function AdvancedMessage({dpId, dataProductsState, noProductMessage, doResetButton, makeDropDown}) {
    const {complexMessage, menu, message, noProductsAvailable= false, isWorkingState,
        activeMenuLookupKey, detailMsgAry, badUrl, resetMenuKey, details=[]}= dataProductsState;


    if (complexMessage) {
        return (<ComplexMessage {...{menu, makeDropDown, message,
            detailMsgAry, badUrl, resetMenuKey, dpId, activeMenuLookupKey, doResetButton }} />);
    }
    else if (menu?.length===1 && menu?.[0]?.url &&
        (menu?.[0].displayType===DPtypes.DOWNLOAD || menu?.[0].displayType===DPtypes.DOWNLOAD_MENU_ITEM)) {
        return (
            <ProductDownload {...{ menu, makeDropDown, message, fileType:menu[0].fileType,
                downloadName:menu[0].name, url:menu[0].url, loadInBrowserMsg:menu[0].loadInBrowserMsg} } />
        );
    }
    else {
        const useMessage= noProductsAvailable && noProductMessage ? noProductMessage : message;
        return (<ProductMessage {...{menu, makeDropDown, isWorkingState, message:useMessage, details}} />);
    }
}


export function ProductMessage({menu, makeDropDown, isWorkingState=false, message, details=[]}) {

    return (
        <Stack {...{width: '100%', height: '100%'}}>
            <Box style={{height: menu ? 30 : 0}}>
                {makeDropDown && makeDropDown?.()}
            </Box>
            <Stack {...{direction: 'row', alignSelf: 'center', pt: 5}}>
                {isWorkingState ? <Box sx={{width: 20, height: 20, mr: 1}} className='loading-animation'/> : ''}
                <Stack spacing={2} alignItems='center'>
                    <Typography level='title-lg' sx={{alignSelf: 'center', textAlign:'center'}}>{message}</Typography>
                    {details?.length>0 &&
                        <Button onClick={ () =>
                            showInfoPopup(
                                ( <Stack spacing={1}> {details.map( (d) => <PopupEntry msg={d}/> )} </Stack> ),
                                'Details'
                            )
                        }>
                            View Details
                        </Button>
                    }
                </Stack>
            </Stack>
        </Stack>
    );
}

function PopupEntry({msg}) {
    if (isString(msg)) return <Typography>{msg}</Typography>;
    if (isObject(msg)) {
        if (isURL(msg.url)) {
            return <Link href={msg.url} target='_blank'>{msg.text??'URL'}</Link>;
        }
        if (msg.text) {
            if (msg.title) {
                return (
                    <Stack>
                        <Typography level='title-md'>{msg.title}</Typography>
                        <Typography>{msg.text}</Typography>
                    </Stack>
                );
            }
            else {
                return <Typography>{msg.text}</Typography>;
            }
        }
    }
    return <Typography>Unknown Error Message</Typography>;
}

export function ProductDownload({menu, makeDropDown, message, url, downloadName, loadInBrowserMsg, fileType}) {
    const downloadButtonText = downloadName ?? 'Download';
    const showOpenUrl= url && loadInBrowserMsg;
    if (!url) return;

    return (
        <Stack {...{width: '100%', height: '100%'}}>
            <Box style={{height: menu ? 30 : 0}}>
                {makeDropDown?.()}
            </Box>
            <Stack {...{direction: 'row', alignSelf: 'center', pt: 5}}>
                <Typography level='title-lg' sx={{alignSelf: 'center', textAlign:'center'}}>{message}</Typography>
            </Stack>
            { fileType!=='html' &&
                <CompleteButton sx={{alignSelf: 'center', pt: 3}} text={downloadButtonText}
                                                               onSuccess={() => doDownload(url)}/>
            }
            { showOpenUrl &&
                <Stack alignItems='center' spacing={1}>
                    <CompleteButton sx={{alignSelf: 'center', pt: 1}} text={loadInBrowserMsg}
                                    onSuccess={() => window.open(url,'ffOpenInBrowser')}
                    />
                </Stack>
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
                <Typography {...{mb:1, alignSelf:'center'}}>{message}</Typography>
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


export function TextFileViewer({url,menu,makeDropDown,fileType}) {

    const [text, setText] = useState('');
    const [error, setError] = useState(false);

    useEffect(() => {
        const doFetch= async () => {
            try {
                setText(await getTextFile(url, 2*MEG));
                // setText(await getTextFile(url, 100));
                setError(false);
            }
            catch (e) {
                setError(e);
            }
        };
        void doFetch();
    }, [url]);

    if (error) {
        return <TextFileViewerError {...{error, menu, makeDropDown, url}} />;
    }

    let finalText= text;
    if (fileType==='json') {
        try {
            finalText= JSON.stringify(JSON.parse(text), null, 2, 2);
        } catch (e) { }      // if text is not JSON, just show as is.
    }

    return (
        <Stack overflow='hidden'>
            <Box style={{height: menu ? 30 : 0}}>
                {makeDropDown && makeDropDown?.()}
            </Box>
            <Stack direction='row' spacing={2} mt={1} mr={2} justifyContent={'flex-end'}>
                <Button onClick={() => doDownload(url)}>
                    {`Download ${fileType} file`}
                </Button>
                <Button onClick={() => window.open(url)}>
                    {`Open ${fileType} file in browser`}
                </Button>
            </Stack>
            <Stack overflow='scroll' m={1}>
                <Typography sx={{whiteSpace:'pre-wrap'}} >
                    {finalText}
                </Typography>
            </Stack>
        </Stack>
        );
}

function TextFileViewerError({error, menu, makeDropDown, url}) {
    if (error.cause?.includes('large')) {
        return (
            <ProductDownload {...{menu,makeDropDown,message:error.cause, url, downloadName:'Download large file'}}/>
        );
    }
    else {
        return (
            <Stack alignItems='center' mt={7} spacing={1}>
                <Box style={{height: menu ? 30 : 0}}>
                    {makeDropDown && makeDropDown?.()}
                </Box>
                <Typography level='title-lg' >Error loading file</Typography>
                <Typography >{`${url}`}</Typography>
                <Typography>{`${error.cause ?? error}`}</Typography>
            </Stack>
        );
    }
}