
import {Button, Chip, Stack} from '@mui/joy';
import React from 'react';
import DrawUtil from '../draw/DrawUtil';
import {SimpleCanvas} from '../draw/SimpleCanvas';
import {DrawSymbol} from '../draw/DrawSymbol.js';

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
