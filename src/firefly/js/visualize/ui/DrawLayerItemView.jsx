/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PlotViewUtil  from '../PlotViewUtil.js';
import SimpleCanvas from '../draw/SimpleCanvas.jsx';
import DrawUtil from '../draw/DrawUtil.js';



const bSty= {
    display:'inline-block',
    whiteSpace: 'nowrap'
};

function DrawLayerItemView({drawLayer,pv, maxTitleChars, lastItem}) {
    var style= {
        width:'100%',
        height:'100%',
        position: 'relative',
        overflow:'hidden'
    };

    if (lastItem) {
        style.marginBottom= 18;
    }
    else {
        style.borderBottomStyle= 'solid';
        style.borderBottomWidth= 1;
        style.borderBottomColor= 'rgba(0, 0, 0, 0.298039)';
        style.marginBottom= 13;
        style.paddingBottom= 5;
    }

    var plotId= pv.plotId;
    return (
        <div style={style} className='draw-layer-item'>
            <input type='checkbox'
                   checked={PlotViewUtil.isDrawLayerVisible(drawLayer,plotId)}
                   onChange={changeVisible}
            />
            {getTitleTag(drawLayer,pv,maxTitleChars)}
            {makeColorChange(drawLayer,pv)}
            {makeShape(drawLayer,pv)}
            {makeDelete(drawLayer,pv)}
            <div style={{paddingTop:10,maxWidth:'30em'}}>{drawLayer.helpLine}</div>
        </div>
    );
}


DrawLayerItemView.propTypes= {
    drawLayer : React.PropTypes.object.isRequired,
    pv : React.PropTypes.object.isRequired,
    maxTitleChars : React.PropTypes.number.isRequired,
    lastItem : React.PropTypes.bool.isRequired
};


function getTitleTag(drawLayer,pv,maxTitleChars) {
    const tStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap',
        minWidth: (maxTitleChars*.7)+'em',
        paddingLeft : 5
    };

    return (
        <div style={tStyle}>{`Show: ${PlotViewUtil.getLayerTitle(pv.plotId,drawLayer)}`}</div>
        );
}


function makeColorChange(drawLayer,pv) {
    const feedBackStyle= {
        width:10,
        height:10,
        backgroundColor: drawLayer.drawingDef.color,
        display:'inline-block',
        marginLeft:5
    };
    if (drawLayer.canUserChangeColor) {
        return (
            <div style={bSty}>
                <div style={feedBackStyle} onClick={modifyColor}  />
                <a className='href-item'
                   onClick={modifyColor}
                   style={Object.assign({},bSty,{marginLeft:5})}>Color</a>
            </div>
        );
    }
    else {
        return (<div style={Object.assign({},bSty, {width:15})}></div>);
    }

}

function makeShape(drawLayer,pv) {
    const shapeStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap'
    };
    if (drawLayer.isPointData) {
        return (
            <div style={shapeStyle}>{drawLayer.drawingDef.symbol.key}
                <SimpleCanvas width={20} height={20} drawIt={ (c) => drawOnCanvas(c,drawLayer)}/>
            </div>
        );
    }
    else {
        return (<div style={Object.assign({},bSty, {width:15})}></div>);
    }

}

function drawOnCanvas(c,drawLayer) {
    if (!c) return;
    var ctx= c.getContext('2d');
    var symbol= drawLayer.drawingDef.symbol;
    var color= drawLayer.drawingDef.color;
    DrawUtil.drawCircle(ctx, 7,7,color,1,5);
    //DrawUtil.drawSymbol(ctx, 7,7,drawLayer.drawingDef,null,false);

}

function makeDelete(drawLayer,pv) {
    const deleteStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap'
    };
    if (drawLayer.canUserDelete) {
        return (
            <a className='href-item' onClick={deleteLayer} style={deleteStyle}>Delete</a>
        );
    }
    else {
        return (<div style={Object.assign({},bSty, {width:15})}></div>);
    }

}


function modifyColor() {
    console.log('modify color');
}

function deleteLayer() {
    console.log('delete layer');

}

function changeVisible() {
    console.log('change visible');
}

export default DrawLayerItemView;
