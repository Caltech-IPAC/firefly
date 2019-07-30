
import React from 'react';
import DrawUtil from '../draw/DrawUtil';
import {SimpleCanvas} from '../draw/SimpleCanvas';
import {DrawSymbol} from '../draw/PointDataObj.js';

const symbolSize= 10;
const mLeft = 5;

const bSty= { display:'inline-block', whiteSpace: 'nowrap' };

const bStyWid = { ...bSty, width: 'calc(33%)' };





export function makeColorChange(color, modifyColor, style= {}) {
    const feedBackStyle= {
        width:symbolSize,
        height:symbolSize,
        backgroundColor: color,
        display:'inline-block',
        marginLeft: mLeft
    };
    return (
        <div style={{...bSty, ...style}}>
            <div style={feedBackStyle} onClick={() => modifyColor()}  />
            <a className='ff-href'
               onClick={() => modifyColor()}
               style={Object.assign({},bSty, {paddingLeft:5})}>Color</a>
        </div>
    );

}

export function makeShape(drawingDef, modifyShape) {

    const [w, h] = [symbolSize, symbolSize];
    const size = DrawUtil.getSymbolSize(w, h, drawingDef.symbol);
    const df = Object.assign({}, drawingDef, {size});
    const {width, height} = DrawUtil.getDrawingSize(size, drawingDef.symbol);

    const feedBackStyle= {
        width,
        height,
        display:'inline-block',
        marginLeft:mLeft
    };

    return (
        <div style={bStyWid} >
            <div style={feedBackStyle} onClick={() => modifyShape()}>
                <SimpleCanvas width={width} height={height} drawIt={ (c) => drawOnCanvas(c, df, width, height)}/>
            </div>
            <a className='ff-href'
               onClick={() => modifyShape()}
               style={Object.assign({}, bSty, {paddingLeft:5})}>Symbol</a>
        </div>
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
