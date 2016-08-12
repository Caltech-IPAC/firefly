/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {SimpleCanvas} from '../draw/SimpleCanvas.jsx';
import DrawUtil from '../draw/DrawUtil.js';



const bSty= {
    display:'inline-block',
    whiteSpace: 'nowrap'
};

function DrawLayerItemView({maxTitleChars, lastItem, deleteLayer,
                            color, canUserChangeColor, canUserDelete, title, helpLine,
                            isPointData, drawingDef,
                            visible, changeVisible, modifyColor, UIComponent}) {
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
                <div>
                    <input type='checkbox' checked={visible} onChange={() => changeVisible()} />
                    {getTitleTag(title,maxTitleChars)}
                </div>
                <div style={{padding:'0 4px 0 5px'}}>
                    {makeColorChange(color, canUserChangeColor,modifyColor)}
                    {makeShape(isPointData,drawingDef)}
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
    maxTitleChars : React.PropTypes.number.isRequired,
    lastItem      : React.PropTypes.bool.isRequired,
    visible       : React.PropTypes.bool.isRequired,
    canUserChangeColor : React.PropTypes.any.isRequired,
    color         : React.PropTypes.string.isRequired,
    title         : React.PropTypes.any.isRequired,
    helpLine      : React.PropTypes.string.isRequired,
    canUserDelete : React.PropTypes.bool.isRequired,
    isPointData   : React.PropTypes.bool.isRequired,
    drawingDef    : React.PropTypes.object,
    deleteLayer   : React.PropTypes.func,
    changeVisible : React.PropTypes.func,
    modifyColor   : React.PropTypes.func,
    UIComponent   : React.PropTypes.object
};


function getTitleTag(title, maxTitleChars) {
    const tStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap',
        minWidth: (maxTitleChars*.7)+'em',
        // paddingLeft : 5
    };

    return (
        <div style={tStyle}>{title}</div>
        );
}


function makeColorChange(color, canUserChangeColor, modifyColor) {
    const feedBackStyle= {
        width:10,
        height:10,
        backgroundColor: color,
        display:'inline-block',
        marginLeft:5
    };
    if (canUserChangeColor) {
        return (
            <div style={bSty}>
                <div style={feedBackStyle} onClick={() => modifyColor()}  />
                <a className='ff-href'
                   onClick={() => modifyColor()}
                   style={Object.assign({},bSty,{marginLeft:5})}>Color</a>
            </div>
        );
    }
    else {
        return (<div style={Object.assign({},bSty, {width:15})}></div>);
    }

}

function makeShape(isPointData, drawingDef) {
    if (isPointData) {
        return (
            <SimpleCanvas width={20} height={12} drawIt={ (c) => drawOnCanvas(c,drawingDef)}/>
        );
    }
    else {
        return (<div style={Object.assign({},bSty, {width:20})}></div>);
    }

}

function drawOnCanvas(c,drawingDef) {
    if (!c) return;
    DrawUtil.drawSymbol(c.getContext('2d'), 10,5,drawingDef,null,false);
}


function makeHelpLine(helpLine) {
    if (helpLine) {
        return (
            <div style={{paddingTop:10,maxWidth:'30em',marginLeft:'2em'}}>{helpLine}</div>
        );
    }
    else {
        return false;
    }
}

function makeDelete(canUserDelete,deleteLayer) {
    const deleteStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap'
    };
    if (canUserDelete) {
        return (
            <a className='ff-href' onClick={() => deleteLayer()} style={deleteStyle}>Delete</a>
        );
    }
    else {
        return (<div style={Object.assign({},bSty, {width:15})}></div>);
    }

}



export default DrawLayerItemView;
