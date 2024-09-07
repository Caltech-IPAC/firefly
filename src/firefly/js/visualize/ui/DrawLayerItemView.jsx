/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {Box, ChipDelete, Divider, Stack, Switch, Tooltip, Typography} from '@mui/joy';
import PropTypes from 'prop-types';
import {isFunction} from 'lodash';
import {getMinMaxWidth, makeColorChange, makeShape} from './DrawLayerUIComponents';


export function DrawLayerItemView({maxTitleChars, lastItem, deleteLayer,
                            color, canUserChangeColor, canUserDelete, title, helpLine,
                            isPointData, drawingDef, autoFormatTitle, canUserHide=true,
                            packWithNext=false,
                            visible, changeVisible, modifyColor, modifyShape, UIComponent}) {

    const sx= { width:1, height:1, pr:1.5, position: 'relative', overflow:'hidden', whiteSpace : 'nowrap'};
    const useDivide= lastItem || !packWithNext;
    return (
        <Box sx={sx}>
           <Stack {...{lineHeight:'1em', position: 'relative', direction:'row', flexWrap:'nowrap',
                         justifyContent: 'space-between', alignItems: 'center', width:'100%' }} >
                <Stack {...{direction: 'row', alignItems: 'center'}}>
                   <Switch {...{checked:visible, sx:{visibility: canUserHide?'inherit':'hidden', pr:.5},
                        onChange:() => changeVisible() }} />
                    {getTitleTag(title,maxTitleChars, autoFormatTitle)}
                </Stack>
                <Stack {...{direction:'row', spacing:1/4, py:.5, width: 180, justifyContent: 'flex-end'}}>
                    {makePointDataShape(isPointData,drawingDef, modifyShape)}
                    {makeColorChangeUIElement(color, canUserChangeColor,modifyColor)}
                    {makeDelete(canUserDelete,deleteLayer)}
                </Stack>
            </Stack>
            <div style={{paddingTop:5, marginLeft:'2em'}}>
                {UIComponent || ''}
            </div>
            {makeHelpLine(helpLine)}
            {useDivide && <Divider orientation='horizontal' sx={{mb:1.5, mt:1}} />}
        </Box>
    );
}


DrawLayerItemView.propTypes= {
    maxTitleChars  : PropTypes.number.isRequired,
    lastItem       : PropTypes.bool.isRequired,
    visible        : PropTypes.bool.isRequired,
    canUserChangeColor : PropTypes.any.isRequired,
    color          : PropTypes.string.isRequired,
    title          : PropTypes.any.isRequired,
    helpLine       : PropTypes.string.isRequired,
    canUserDelete  : PropTypes.bool.isRequired,
    canUserHide    : PropTypes.bool,
    isPointData    : PropTypes.bool.isRequired,
    drawingDef     : PropTypes.object,
    deleteLayer    : PropTypes.func,
    changeVisible  : PropTypes.func,
    modifyColor    : PropTypes.func,
    modifyShape    : PropTypes.func,
    UIComponent    : PropTypes.object,
    autoFormatTitle: PropTypes.bool,
    packWithNext: PropTypes.bool,
};


function getTitleTag(title, maxTitleChars, autoFormatTitle) {
    if (!autoFormatTitle) {
        return isFunction(title) ? title() : title;
    }
    const {minW,maxW}= getMinMaxWidth(maxTitleChars);

    return (
        <Typography {...{
            whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden',
            minWidth: minW + 'em', maxWidth: maxW + 'em'}}>
            {title}
        </Typography>
    );
}


function makeColorChangeUIElement(color, canUserChangeColor, modifyColor) {
    return canUserChangeColor ? makeColorChange(color,modifyColor, {width: 'calc(33%)'}) : false;
}

function makePointDataShape(isPointData, drawingDef, modifyShape) {
    return isPointData ? makeShape(drawingDef,modifyShape) : false;
}

function makeHelpLine(helpLine) {
    if (helpLine) {
        return (
            <Typography {...{level:'body-sm',
                paddingBottom:1,maxWidth:'30em',ml:2, whiteSpace: 'normal'}}>
                {helpLine}
            </Typography>
        );
    }
    else {
        return false;
    }
}

function makeDelete(canUserDelete,deleteLayer) {
    if (!canUserDelete) return <Box sx={{width:23}}/>
    return (
        <Tooltip title='Close Layer'
            placement='right-start'>
            <ChipDelete {...{
                onClick: () => deleteLayer(),
                sx:{'--Chip-deleteSize': '1.9em'}
            }}/>
        </Tooltip>
    );

}

