/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Box, Card, DialogContent, DialogTitle, IconButton, Stack, Tooltip} from '@mui/joy';
import React, {memo, useState, useEffect, useRef} from 'react';
import Enum from 'enum';
import {object,element,func,number,string,bool,oneOfType} from 'prop-types';
import {debounce} from 'lodash';
import {getDefaultPopupPosition, humanStart, humanMove, humanStop} from './PopupPanelHelper.js';

import DELETE from 'images/blue_delete_10x10.png';

/**
 * @typedef {Object} LayoutType
 * enum can be one of
 * @prop CENTER
 * @prop TOP_EDGE_CENTER
 * @prop TOP_CENTER
 * @prop TOP_LEFT
 * @prop TOP_RIGHT
 * @prop TOP_RIGHT_OF_BUTTON
 * @prop NONE
 * @prop USER_POSITION
 */

/** @type LayoutType */
export const LayoutType= new Enum(['CENTER', 'TOP_EDGE_CENTER', 'TOP_CENTER', 'TOP_LEFT', 'TOP_RIGHT', 'NONE', 'USER_POSITION', 'TOP_RIGHT_OF_BUTTON']);

export const PopupPanel= memo((props) => {
    const {title='', visible=true, layoutPosition=LayoutType.TOP_CENTER, closePromise, closeCallback, modal=false,
        requestToClose, mouseInDialog, requestOnTop, dialogId, zIndex=0, children, style,
        initLeft, initTop, onMove, element}= props;
    const [{left,top}, setPos]= useState({left:0,top:0});
    const [layout, setLayout]= useState(LayoutType.NONE);
    const {current:ctxRef} = useRef({ mouseCtx: undefined, popupRef : undefined, titleBarRef: undefined});

    const updateLayoutPosition= () => {
        if (layoutPosition===LayoutType.USER_POSITION) {
            setPos({left:initLeft,top:initTop});
        }
        else {
            setPos(getDefaultPopupPosition(ctxRef.popupRef,layoutPosition, element, initTop, initLeft));
        }
        setLayout(layoutPosition);
    };

    const askParentToClose= () => {
        requestToClose?.();
    };
    const onMouseEnter= () => mouseInDialog?.(true);
    const onMouseLeave= () => mouseInDialog?.(false);

    const dialogMoveStart = (ev) => {
        requestOnTop?.(dialogId);
        ctxRef.mouseCtx= humanStart(ev,ctxRef.popupRef,ctxRef.titleBarRef);
    };
    const dialogMove = (ev) => {
        const r = humanMove(ev,ctxRef.mouseCtx,ctxRef.titleBarRef);
        r && setPos({left:r.left, top:r.top});
        r && onMove?.({left:r?.left,top:r?.top});
    };
    const dialogMoveEnd = (ev) => {
        humanStop(ev, ctxRef.mouseCtx);
    };

    useEffect(() => {
        setTimeout( updateLayoutPosition, 10);
        const browserResizeCallback= debounce(updateLayoutPosition,150);
        window.addEventListener('resize', browserResizeCallback);
        document.addEventListener('mousemove', dialogMove);
        document.addEventListener('mouseup', dialogMoveEnd);
        closePromise?.then(()=> askParentToClose());
        return () => {
            window.removeEventListener('resize', browserResizeCallback);
            document.removeEventListener('mousemove', dialogMove);
            document.removeEventListener('mouseup', dialogMoveEnd);
            closeCallback?.();
        };
    },[]);

    if (!visible) return false;
    return (
        <PopupHeaderTop {...{modal,zIndex,left,top,ctxRef,dialogMoveStart,dialogMoveEnd, onMouseEnter,onMouseLeave, style,
            dialogMove,children,title,askParentToClose, visibility:layout===LayoutType.NONE ? 'hidden' : 'visible'}}>
            {children}
        </PopupHeaderTop>
    );
});

PopupPanel.propTypes= {
    layoutPosition : object,
    title : oneOfType([string,element]),
    closePromise : object,
    requestToClose : func,
    requestOnTop : func,
    closeCallback : func,
    dialogId : string,
    zIndex : number,
    mouseInDialog : func,
    modal : bool,
    visible : bool,
    style : object,
    initLeft: number,
    initTop: number,
    onMove: func,
    element,
};

function PopupHeaderTop({modal,zIndex,left,top,visibility,ctxRef,dialogMoveStart,dialogMoveEnd,
                            onMouseEnter,onMouseLeave,dialogMove,children,title,askParentToClose}) {
    return (
        <Box style={{zIndex, position:'relative'}}>
            {modal &&
                <Box sx={{ position: 'fixed', backgroundColor: 'rgba(0, 0, 0, 0.2)',
                         top: 0, left: 0, bottom: 0, right: 0, }}/>}
            <Card {...{className:'ff-PopupPanel', color:'neutral', variant:'plain', ref:(c) => ctxRef.popupRef=c,
                onTouchStart:dialogMoveStart, onTouchMove:dialogMove,
                onTouchEnd:dialogMoveEnd, onMouseEnter, onMouseLeave,
                sx:(theme) => (
                    {
                        left,
                        top,
                        position: 'absolute',
                        p:.5,
                        visibility,
                        boxShadow: `1px 1px 5px ${theme.vars.palette.primary.softActiveColor}`,
                        '.ff-dialog-title-bar' : { cursor:'grab' },
                        '.ff-dialog-title-bar:active' : { cursor:'grabbing' }
                    }) }}>
                <Stack direction='row' justifyContent='space-between' className='ff-dialog-title-bar'
                       sx={{position:'relative', height:'1.8em', width:'100%', mb:1}}
                       ref={(c) => ctxRef.titleBarRef=c}
                       onTouchStart={dialogMoveStart} onTouchMove={dialogMove}
                       onTouchEnd={dialogMoveEnd} onMouseDownCapture={dialogMoveStart}>
                    <DialogTitle  sx= {{ ml:.5, mt:.5}} >
                        {title}
                    </DialogTitle>
                    <Tooltip placement="left" title='Close'>
                        <IconButton onClick={askParentToClose} title='Close'
                                    sx={{minHeight:12, minWidth:12, p:.5}}>
                            <img src={DELETE}/>
                        </IconButton>
                    </Tooltip>
                </Stack>
                <DialogContent sx={{ml:.5}}>
                    {children}
                </DialogContent>
            </Card>
        </Box>
    );
}
