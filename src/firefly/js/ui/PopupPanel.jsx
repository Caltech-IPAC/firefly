/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Sheet} from '@mui/joy';
import React, {memo, useState, useEffect, useRef} from 'react';
import Enum from 'enum';
import PropTypes from 'prop-types';
import {debounce} from 'lodash';
import {getDefaultPopupPosition, humanStart, humanMove, humanStop} from './PopupPanelHelper.js';
import './PopupPanel.css';
import DEL_ICO from 'html/images/blue_delete_10x10.png';

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
    layoutPosition : PropTypes.object,
    title : PropTypes.oneOfType([PropTypes.string,PropTypes.element]),
    closePromise : PropTypes.object,
    requestToClose : PropTypes.func,
    requestOnTop : PropTypes.func,
    closeCallback : PropTypes.func,
    dialogId : PropTypes.string,
    zIndex : PropTypes.number,
    mouseInDialog : PropTypes.func,
    modal : PropTypes.bool,
    visible : PropTypes.bool,
    style : PropTypes.object,
    initLeft: PropTypes.number,
    initTop: PropTypes.number,
    onMove: PropTypes.func
};

function PopupHeaderTop({modal,zIndex,left,top,visibility,ctxRef,dialogMoveStart,dialogMoveEnd,
                            onMouseEnter,onMouseLeave,dialogMove,children,title,askParentToClose, style}) {
    return (
        <Sheet style={{zIndex, position:'relative'}}>
            {modal && <div className='popup-panel-glass'/>}
            <div ref={(c) => ctxRef.popupRef=c} style={{left, top, position: 'absolute', visibility}}
                 className={'popup-panel-shadow enable-select'}
                 onTouchStart={dialogMoveStart} onTouchMove={dialogMove}
                 onTouchEnd={dialogMoveEnd} onMouseEnter={onMouseEnter} onMouseLeave={onMouseLeave} >
                <div className={'standard-border'}>
                    <div style={{position:'relative', height:'16px', width:'100%', cursor:'default'}}
                         className={'title-bar title-color popup-panel-title-background'}
                         onTouchStart={dialogMoveStart} onTouchMove={dialogMove}
                         onTouchEnd={dialogMoveEnd} onMouseDownCapture={dialogMoveStart}>
                        <div ref={(c) => ctxRef.titleBarRef=c}
                             style= {{position:'absolute', left: 0, top: 0, bottom: 0, width:'100%', padding: '3px 0 3px 10px'}}
                             onMouseDownCapture={dialogMoveStart} onTouchStart={dialogMoveStart}
                             onTouchMove={dialogMove} onTouchEnd={dialogMoveEnd}
                             className={'title-label'} >
                            <div className={'text-ellipsis'} style={{width:'80%', height: '100%'}}>
                                {title}
                            </div>
                        </div>
                        <img className='popup-panel-header' src= {DEL_ICO}
                             style= {{position:'absolute', right:0, top:0}} onClick={askParentToClose} />
                    </div>
                    <Sheet style={{display:'flex', ...style}}> {children} </Sheet>
                </div>
            </div>
        </Sheet>
    );
}
