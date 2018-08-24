/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {SimpleCanvas} from '../draw/SimpleCanvas.jsx';
import DrawUtil from '../draw/DrawUtil.js';
import {DrawSymbol} from '../draw/PointDataObj.js';


const bSty= {
    display:'inline-block',
    whiteSpace: 'nowrap'
};
const bStyWid = {
    ...bSty, width: 'calc(33%)'
};

const symbolSize= 10;
const mLeft = 5;

function DrawLayerItemView({maxTitleChars, lastItem, deleteLayer,
                            color, canUserChangeColor, canUserDelete, title, helpLine,
                            isPointData, drawingDef,
                            visible, changeVisible, modifyColor, modifyShape, UIComponent}) {
    var style= {
        width:'100%',
        height:'100%',
        position: 'relative',
        overflow:'hidden',
        whiteSpace : 'nowrap'
    };

    if (lastItem) {
        style.marginBottom= 10;
    }
    else {
        style.borderBottomStyle= 'solid';
        style.borderBottomWidth= 1;
        style.borderBottomColor= 'rgba(0, 0, 0, 0.298039)';
        style.marginBottom= 13;
        style.paddingBottom= 5;
    }

    return (
        <div style={style} className='draw-layer-item'>
            <div style={{lineHeight:'1em', position: 'relative', display:'inline-flex',
                         flexDirection:'row', flexWrap:'nowrap',
                         justifyContent: 'space-between',
                         alignItems: 'center',
                         width:'100%'
                         }} >
                <div style={{display: 'flex', alignItems: 'center'}}>
                    <input type='checkbox' checked={visible} onChange={() => changeVisible()} />
                    {getTitleTag(title,maxTitleChars)}
                </div>
                <div style={{padding:'0 4px 0 5px', width: 180, display: 'flex', justifyContent: 'flex-end'}}>
                    {makeColorChange(color, canUserChangeColor,modifyColor)}
                    {makeShape(isPointData,drawingDef, modifyShape)}
                    {makeDelete(canUserDelete,deleteLayer)}
                </div>
            </div>
            <div style={{paddingTop:5, marginLeft:'2em'}}>
                {UIComponent || ''}
            </div>
            {makeHelpLine(helpLine)}
        </div>
    );
}


DrawLayerItemView.propTypes= {
    maxTitleChars : PropTypes.number.isRequired,
    lastItem      : PropTypes.bool.isRequired,
    visible       : PropTypes.bool.isRequired,
    canUserChangeColor : PropTypes.any.isRequired,
    color         : PropTypes.string.isRequired,
    title         : PropTypes.any.isRequired,
    helpLine      : PropTypes.string.isRequired,
    canUserDelete : PropTypes.bool.isRequired,
    isPointData   : PropTypes.bool.isRequired,
    drawingDef    : PropTypes.object,
    deleteLayer   : PropTypes.func,
    changeVisible : PropTypes.func,
    modifyColor   : PropTypes.func,
    UIComponent   : PropTypes.object
};


function getTitleTag(title, maxTitleChars) {
    const minW = maxTitleChars*.3 < 30 ? Math.max(maxTitleChars*.3, 10) : 30;
    const maxW = maxTitleChars*.7 > 10 ? Math.min(maxTitleChars*.7, 30) : 10;

    const tStyle= {
        minWidth: minW + 'em',
        maxWidth: maxW + 'em',
        lineHeight: '1.1em'
    };

    return (
        <div style={tStyle} className='text-ellipsis' title={title}>{title}</div>
        );
}


function makeColorChange(color, canUserChangeColor, modifyColor) {
    const feedBackStyle= {
        width:symbolSize,
        height:symbolSize,
        backgroundColor: color,
        display:'inline-block',
        marginLeft: mLeft
    };
    if (canUserChangeColor) {
        return (
            <div style={bStyWid}>
                <div style={feedBackStyle} onClick={() => modifyColor()}  />
                <a className='ff-href'
                   onClick={() => modifyColor()}
                   style={Object.assign({},bSty, {paddingLeft:5})}>Color</a>
            </div>
        );
    }
    else {
        return false;
    }

}

function makeShape(isPointData, drawingDef, modifyShape) {

    if (isPointData) {
        var [w, h] = [symbolSize, symbolSize];
        var size = DrawUtil.getSymbolSize(w, h, drawingDef.symbol);
        var df = Object.assign({}, drawingDef, {size});
        var {width, height} = DrawUtil.getDrawingSize(size, drawingDef.symbol);

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
    else {
        return false;
    }

}

export function drawOnCanvas(c,drawingDef, w, h) {
    if (!c) return;

    var [x, y] = drawingDef.symbol === DrawSymbol.ARROW ? [w/2+drawingDef.size/2, h/2+drawingDef.size/2] : [w/2, h/2];
    var ct = c.getContext('2d');
    ct.clearRect(0, 0, w, h);

    DrawUtil.drawSymbol(ct, x, y, drawingDef, null,false);
}


function makeHelpLine(helpLine) {
    if (helpLine) {
        return (
            <div style={{paddingTop:10,paddingBottom:5,maxWidth:'30em',marginLeft:'2em', whiteSpace: 'normal'}}>{helpLine}</div>
        );
    }
    else {
        return false;
    }
}

function makeDelete(canUserDelete,deleteLayer) {
    const deleteStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap',
        marginLeft: mLeft*2+symbolSize
    };
    if (canUserDelete) {
        return (
            <div style={bStyWid}>
                <a className='ff-href'
                   onClick={() => deleteLayer()} style={deleteStyle}>Delete</a>
            </div>
        );
    }
    else {
        return false;
    }

}



export default DrawLayerItemView;
