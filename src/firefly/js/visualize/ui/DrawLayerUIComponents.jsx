import {Box, Button, Chip, Stack, Typography} from '@mui/joy';
import {truncate} from 'lodash';
import React from 'react';
import {dispatchTableUiUpdate} from '../../tables/TablesCntlr';
import {getTableUiByTblId, getTblById} from '../../tables/TableUtil';
import {hideColorPickerDialog, showColorPickerDialog} from '../../ui/ColorPicker';
import {ColorChangeType} from '../draw/DrawLayer';
import {DrawSymbol} from '../draw/DrawSymbol.js';
import DrawUtil from '../draw/DrawUtil';
import {SimpleCanvas} from '../draw/SimpleCanvas';
import {dispatchChangeDrawingDef, getDlAry} from '../DrawLayerCntlr';
import {getDrawLayerById, getDrawLayersByDisplayGroup} from '../PlotViewUtil';

const symbolSize= 10;


export function makeColorChange(color, modifyColor, sx= {}) {
    const feedBackStyle= { width:symbolSize, height:symbolSize, backgroundColor: color};
    return (
            <Chip onClick={() => modifyColor()} sx={{px:.5}} startDecorator={<div style={feedBackStyle} />}>
                Color
            </Chip>
    );

}

export function makeShape(drawingDef, modifyShape) {

    const [w, h] = [symbolSize, symbolSize];
    const size = DrawUtil.getSymbolSize(w, h, drawingDef.symbol);
    const df = {...drawingDef, size};
    const {width, height} = DrawUtil.getDrawingSize(size, drawingDef.symbol);

    const startDecorator=
        (<Stack direction='row' alignItems='center' onClick={() => modifyShape()}>
            <SimpleCanvas width={width} height={height} drawIt={ (c) => drawOnCanvas(c, df, width, height)}/>
        </Stack>);

    return (
        <Chip onClick={() => modifyShape()} startDecorator={startDecorator} sx={{px:.5}}>
            Symbol
        </Chip>
    );
}

export function drawOnCanvas(c,drawingDef, w, h) {
    if (!c) return;
    const [x, y] = drawingDef.symbol === DrawSymbol.ARROW ? [w/2+drawingDef.size/2, h/2+drawingDef.size/2] : [w/2, h/2];
    const ct = c.getContext('2d');
    ct.clearRect(0, 0, w, h);
    DrawUtil.drawSymbol(ct, x, y, drawingDef, null,false);
}

export function getMinMaxWidth(maxTitleChars) {
    const minW = maxTitleChars*.3 < 30 ? Math.max(maxTitleChars*.3, 10) : 30;
    const maxW = maxTitleChars*.7 > 10 ? Math.min(maxTitleChars*.7, 30) : 10;
    return {minW,maxW};
}

export function TableColorTitle({color,drawLayerId, plotId, tbl_id}) {

    const table= getTblById(tbl_id);
    const dl= getDrawLayerById(getDlAry(), drawLayerId);
    if (!table || !dl) return;

    return (
        <Stack direction='row' alignItems='center' spacing={1/4} overflow='hidden'>
            <Button sx={{p:'3px', minHeight:0, zIndex:2}}
                onClick={() => modifyDrawColor(dl,plotId,tbl_id)}>
                <Box {...{
                     width:symbolSize, height:symbolSize, backgroundColor: color,
                    border:'1px solid transparent', borderRadius:3
                }} />
            </Button>
            <Typography level='body-sm' noWrap={true} title={table.title}>{truncate(table.title)}</Typography>
        </Stack>
    );
}

export function makeTableColorTitle(color, drawLayerId, plotId, tbl_id) {
    return <TableColorTitle {...{color, drawLayerId, plotId, tbl_id}} />;
}

export function modifyDrawColor(inDl, plotId, tbl_id) {
    hideColorPickerDialog();
    showColorPickerDialog(inDl.drawingDef.color, inDl.canUserChangeColor === ColorChangeType.STATIC, false,
        (ev) => {
            const {r, g, b, a} = ev.rgb;
            const rgbStr = `rgba(${r},${g},${b},${a})`;

            if (tbl_id && inDl.tableCanControlColor) {
                const dlAryForTable= getDlAry().filter( (dl) => tbl_id===dl.tbl_id && dl.tableCanControlColor);
                dlAryForTable.forEach( (dl) => {
                    dispatchChangeDrawingDef(dl.displayGroupId, Object.assign({}, dl.drawingDef, {color: rgbStr}), plotId, dl.titleMatching);
                    plotId= dl.plotIdAry?.[0];
                    const {tbl_ui_id} = getTableUiByTblId(tbl_id) ?? {};
                    if (!tbl_ui_id && !plotId) return;
                    dispatchTableUiUpdate({tbl_ui_id,
                        title:makeTableColorTitle(rgbStr,dl.drawLayerId,plotId,tbl_id),
                        color: rgbStr
                    });
                });
            }
            else {
                const dl = getDrawLayersByDisplayGroup(getDlAry(), inDl.displayGroupId);
                dispatchChangeDrawingDef(dl.displayGroupId, Object.assign({}, dl.drawingDef, {color: rgbStr}), plotId, dl.titleMatching);
            }
        }, '');
}