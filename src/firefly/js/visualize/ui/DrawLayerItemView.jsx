/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isString} from 'lodash';
import React from 'react';
import {Box, ChipDelete, Divider, Stack, Switch, Tooltip, Typography} from '@mui/joy';
import {object, any, bool, func, number, string} from 'prop-types';
import {getTitleTag, makeColorChange, makeShape} from './DrawLayerUIComponents';
import Layers from '@mui/icons-material/Layers';


export function DrawLayerItemView({maxTitleChars, lastItem, deleteLayer,
                            color, canUserChangeColor=true, canUserDelete=false, title, helpLine='',
                            isPointData=false, drawingDef, autoFormatTitle=true, canUserHide=true,
                            packWithNext=false,
                            visible, changeVisible, modifyColor, modifyShape, UIComponent=undefined}) {

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
    maxTitleChars  : number.isRequired,
    lastItem       : bool.isRequired,
    visible        : bool.isRequired,
    canUserChangeColor : any,
    color          : string.isRequired,
    title          : any.isRequired,
    helpLine       : string.isRequired,
    canUserDelete  : bool,
    canUserHide    : bool,
    isPointData    : bool,
    drawingDef     : object,
    deleteLayer    : func,
    changeVisible  : func,
    modifyColor    : func,
    modifyShape    : func,
    UIComponent    : object,
    autoFormatTitle: bool,
    packWithNext: bool,
};



export function DrawLayerLegendView({maxTitleChars, color, canUserChangeColor, title, tip='',
                                      autoFormatTitle, canUserHide=true, visible, changeVisible, modifyColor}) {

    const sx= { width:1, height:1, pr:1.5, position: 'relative', overflow:'hidden', whiteSpace : 'nowrap'};
    const tipTitle=  (
        <Stack>
            {
                isString(tip)
                    ? <Typography>{tip}</Typography>
                    : <div>{tip}</div>
            }
            <Typography component='div'>More options under the layers (
                <Layers viewBox={'2 0 22 20'}/>
                ) button</Typography>
        </Stack>
    );

    return (
        <Box sx={sx}>
            <Stack {...{lineHeight:'1em', position: 'relative', direction:'row', flexWrap:'nowrap',
                justifyContent: 'space-between', alignItems: 'center', width:'100%' }} >
                <Tooltip title={tipTitle}>
                    <Stack {...{direction: 'row', alignItems: 'center'}}>
                        <Switch {...{checked:visible, size:'sm', sx:{visibility: canUserHide?'inherit':'hidden', pr:.5},
                            onChange:() => changeVisible() }} />
                        {getTitleTag(title,maxTitleChars, autoFormatTitle, 'body-xs', {minWidth:undefined}, 17)}
                    </Stack>
                </Tooltip>
                <Stack {...{direction:'row', spacing:1/4, py:.5, width: '2em', justifyContent: 'flex-end'}}>
                    {makeColorChangeUIElement(color, canUserChangeColor,modifyColor,'')}
                </Stack>
            </Stack>
        </Box>
    );
}



function makeColorChangeUIElement(color, canUserChangeColor, modifyColor, text) {
    return canUserChangeColor ? makeColorChange(color,modifyColor, text) : false;
}

function makePointDataShape(isPointData, drawingDef, modifyShape) {
    return isPointData ? makeShape(drawingDef,modifyShape) : false;
}

function makeHelpLine(helpLine) {
    if (!helpLine) return false;
    return (
        <Typography {...{level:'body-sm',
            paddingBottom:1,maxWidth:'30em',ml:2, whiteSpace: 'normal'}}>
            {helpLine}
        </Typography>
    );
}

function makeDelete(canUserDelete,deleteLayer) {
    if (!canUserDelete) return <Box sx={{width:23}}/>;
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
