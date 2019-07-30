/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {getMinMaxWidth, makeColorChange, makeShape} from './DrawLayerUIComponents';


const bSty= {
    display:'inline-block',
    whiteSpace: 'nowrap'
};
const bStyWid = {
    ...bSty, width: 'calc(33%)'
};

const symbolSize= 10;
const mLeft = 5;

export function DrawLayerItemView({maxTitleChars, lastItem, deleteLayer,
                            color, canUserChangeColor, canUserDelete, title, helpLine,
                            isPointData, drawingDef, autoFormatTitle, canUserHide=true,
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
                    <input type='checkbox' style={{visibility: canUserHide?'visible':'hidden'}} checked={visible} onChange={() => changeVisible()} />
                    {getTitleTag(title,maxTitleChars, autoFormatTitle)}
                </div>
                <div style={{padding:'0 4px 0 5px', width: 180, display: 'flex', justifyContent: 'flex-end'}}>
                    {makeColorChangeUIElement(color, canUserChangeColor,modifyColor)}
                    {makePointDataShape(isPointData,drawingDef, modifyShape)}
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
    maxTitleChars  : PropTypes.number.isRequired,
    lastItem       : PropTypes.bool.isRequired,
    visible        : PropTypes.bool.isRequired,
    canUserChangeColor : PropTypes.any.isRequired,
    color          : PropTypes.string.isRequired,
    title          : PropTypes.node.isRequired,
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
};


function getTitleTag(title, maxTitleChars, autoFormatTitle) {
    if (!autoFormatTitle) return title;
    const {minW,maxW}= getMinMaxWidth(maxTitleChars);

    const tStyle= {
        minWidth: minW + 'em',
        maxWidth: maxW + 'em',
        lineHeight: '1.1em'
    };

    return (
        <div style={tStyle} className='text-ellipsis' title={title}>{title}</div>
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
        marginLeft: mLeft*2+symbolSize,
        visibility: canUserDelete ? 'visible' : 'hidden'
    };
    return (
        <div style={bStyWid}>
            <a className='ff-href'
               onClick={() => deleteLayer()} style={deleteStyle}>Delete</a>
        </div>
    );

}

