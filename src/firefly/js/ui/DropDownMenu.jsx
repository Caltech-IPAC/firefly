/*eslint "prefer-template": 0*/

import {isFunction} from 'lodash';
import React, {useContext, useEffect, useRef, useState} from 'react';
import {Box, Button, Card, Stack, Tooltip} from '@mui/joy';
import PropTypes from 'prop-types';
import {DropDownDirCtx} from './DropDownDirContext.js';

const DEFAULT_DROPDOWN_DIR = 'right';
export const DROP_DOWN_WRAPPER_CLASSNAME= 'ff-dropdown-menu';

const computePosition= (tgtX,tgtY)  => ({x:tgtX,y:tgtY+18});

function placeDropDown(e,x,y, beforeVisible) {
    const pos= computePosition(x,y);
    let left= pos.x - 10;
    if (left<5) left= 5;
    e.style.left= left +'px';
    e.style.top= (pos.y + 10)+'px';
    let isAbove= false;
    if (isFunction(beforeVisible)) {
        const {above}= beforeVisible(e) ?? {above:false};
        isAbove= above;
    }
    e.style.visibility='visible';
    return isAbove;
}


export function SingleColumnMenu({children, sx, color, variant, kind='drop'}) {
    return (
        <DropDownMenu {...{sx, kind, color, variant}}>
            <Stack direction='column' sx={{'& .ff-toolbar-button':{width:1, justifyContent:'flex-start'}}}>
                {children}
            </Stack>
        </DropDownMenu>
    );
}


export function DropDownMenu({children, sx, color='neutral', variant='outline', kind='drop'}) {
    const {dropdownVertical}= useContext(DropDownDirCtx);

    const above= dropdownVertical==='above';
    const bRadius= kind==='drop' ?  above ?
        {borderBottomLeftRadius: '2px', borderBottomRightRadius: '2px'} :
        {borderTopLeftRadius: '2px', borderTopRightRadius: '2px'} :
        {};

    const shadow= above ? '1px -1px 3px' : '1px 1px 3px';

    return (
        <Card {...{
            color, variant:'outline',
            sx:(theme) => {
                return {
                    p:.5,
                    ...bRadius,
                    boxShadow: `${shadow} ${theme.vars.palette.primary.softActiveColor}`,
                    ...(isFunction(sx) ? sx(theme) : sx),
                };
            }
        }}>
            {children}
        </Card>
    );
}


export function DropDownMenuWrapper({x,y,content,beforeVisible, visible,zIndex}) {

    const [element,setElement]= useState();
    const [above, setAbove]= useState(false);

    useEffect(() => {
        if (!element) return;
        setTimeout(
            () =>{
                const isAbove= placeDropDown(element,x,y, beforeVisible );
                setAbove(isAbove);
            },5
        );
    }, [element,x,y,content]);

    if (!visible) return false;
    if (!x && !y && !content) return false;
    return (
        <DropDownDirCtx.Provider value={{dropdownVertical: above ? 'above' : 'below'}}>
            <Box {...{className:'ff-MenuWrapper', position:'absolute', left:0, top:0, visibility:'hidden', zIndex,
                ref:(c) => setElement(c)}} >
                <Box sx={{padding: .5}} className={DROP_DOWN_WRAPPER_CLASSNAME}>
                    {content}
                </Box>
            </Box>
        </DropDownDirCtx.Provider>
    );
}

DropDownMenuWrapper.propTypes= {
    visible : PropTypes.bool,
    x : PropTypes.number.isRequired,
    y : PropTypes.number.isRequired,
    content : PropTypes.object.isRequired,
    zIndex : PropTypes.number,
    beforeVisible : PropTypes.func
};



export function DropDownSubMenu({text, tip, visible=true, sx, children}) {
    const [showSubMenu,setShowSubMenu]= useState(false);
    const {current:timerRef}= useRef({timer:undefined});
    const [cascadeElement,setCascadeElement]= useState();
    const [buttonElement,setButtonElement]= useState();
    const {dropdownDirection} = useContext(DropDownDirCtx) || DEFAULT_DROPDOWN_DIR;

    useEffect(() => {
        if (cascadeElement && buttonElement) {
            if (dropdownDirection==='left') {
                const rect = cascadeElement.getBoundingClientRect();
                if (!rect) return;
                cascadeElement.style.left=-rect.width+'px';
            }
            else {
                const rect = buttonElement.getBoundingClientRect();
                if (!rect) return;
                cascadeElement.style.left=rect.width+'px';
            }
            cascadeElement.style.right='auto';
            cascadeElement.style.visibility='visible';
        }
    }, [cascadeElement, buttonElement]);

    if (!visible) return false;

    const show= () => {
        if (timerRef.timer) clearTimeout(timerRef.timer);
        setShowSubMenu(true);
    };

    const hide= () => {
        timerRef.timer= setTimeout(() => setShowSubMenu(false), 75);
    };

    return (
        <Tooltip title={tip} >
            <Box onMouseEnter={show} onMouseLeave={hide} sx={{position:'relative'}}>
                <Button {...{variant:'plain', color:'neutral', 'aria-label':tip,
                    ref:(c) => setButtonElement(c),
                    endDecorator: (<div style={{marginLeft:5}} className='arrow-right'/>),
                    sx:{whiteSpace:'nowrap', py:.4, minHeight: 'unset'},
                }}>
                    {text}
                </Button>
                <Box>
                    {showSubMenu &&
                        <Box {...{ref: (c) => setCascadeElement(c),
                             position: 'absolute', zIndex: 1, visibility:'hidden', top:0}}
                            onMouseEnter={show}>
                            <SingleColumnMenu kind='cascade'>
                                {isFunction(children) ? children() : children}
                            </SingleColumnMenu>
                        </Box>
                    }
                </Box>
            </Box>
        </Tooltip>
    );
}

DropDownSubMenu.propTypes= {
    text: PropTypes.string.isRequired,
    visible: PropTypes.bool,
    tip: PropTypes.string,
    direction: PropTypes.string
};

