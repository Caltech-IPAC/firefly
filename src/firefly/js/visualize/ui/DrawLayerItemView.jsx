/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PlotViewUtil, {isDrawLayerVisible}  from '../PlotViewUtil.js';
import SimpleCanvas from '../draw/SimpleCanvas.jsx';
import DrawUtil from '../draw/DrawUtil.js';
import {ColorChangeType} from '../draw/DrawLayer.js';
import {dispatchChangeDrawingDef, dispatchChangeVisibility,
                         dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';
import {showColorPickerDialog} from '../../ui/ColorPicker.jsx';



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
            <div style={{lineHeight:'1em'}} >
                <input type='checkbox'
                       checked={isDrawLayerVisible(drawLayer,plotId)}
                       onChange={(ev) => changeVisible(drawLayer, plotId)}
                />
                {getTitleTag(drawLayer,pv,maxTitleChars)}
                <div style={{float:'right'}}>
                    {makeColorChange(drawLayer,pv)}
                    {makeShape(drawLayer,pv)}
                    {makeDelete(drawLayer,pv)}
                </div>
            </div>
            <div style={{paddingTop:10,maxWidth:'30em',marginLeft:'2em'}}>{drawLayer.helpLine}</div>
        </div>
    );
}


DrawLayerItemView.propTypes= {
    drawLayer     : React.PropTypes.object.isRequired,
    pv            : React.PropTypes.object.isRequired,
    maxTitleChars : React.PropTypes.number.isRequired,
    lastItem      : React.PropTypes.bool.isRequired
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
                <div style={feedBackStyle} onClick={() => modifyColor(drawLayer,pv.plotId)}  />
                <a className='href-item'
                   onClick={() => modifyColor(drawLayer)}
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
            <SimpleCanvas width={20} height={12} drawIt={ (c) => drawOnCanvas(c,drawLayer)}/>
        );
    }
    else {
        return (<div style={Object.assign({},bSty, {width:20})}></div>);
    }

}

function drawOnCanvas(c,drawLayer) {
    if (!c) return;
    DrawUtil.drawSymbol(c.getContext('2d'), 10,5,drawLayer.drawingDef,null,false);
}

function makeDelete(drawLayer,pv) {
    const deleteStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap'
    };
    if (drawLayer.canUserDelete) {
        return (
            <a className='href-item' onClick={() => deleteLayer(drawLayer,pv.plotId)} style={deleteStyle}>Delete</a>
        );
    }
    else {
        return (<div style={Object.assign({},bSty, {width:15})}></div>);
    }

}


function modifyColor(dl,plotId) {
    showColorPickerDialog(dl.drawingDef.color, dl.canUserChangeColor===ColorChangeType.STATIC,
        (ev) => {
            var {r,g,b,a}= ev.rgb;
            var rgbStr= `rgba(${r},${g},${b},${a})`;
            dispatchChangeDrawingDef(dl.displayGroupId, Object.assign({},dl.drawingDef,{color:rgbStr}),plotId);
        });
}

function deleteLayer(dl,plotId) {
    dispatchDetachLayerFromPlot(dl.displayGroupId,plotId,true);

}

function changeVisible(dl, plotId) {
    dispatchChangeVisibility(dl.displayGroupId, !isDrawLayerVisible(dl,plotId),plotId );
}

export default DrawLayerItemView;
