/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {isDrawLayerVisible, getLayerTitle}  from '../PlotViewUtil.js';
import {SimpleCanvas} from '../draw/SimpleCanvas.jsx';
import DrawUtil from '../draw/DrawUtil.js';
import {ColorChangeType} from '../draw/DrawLayer.js';
import {dispatchChangeDrawingDef, dispatchChangeVisibility,
                         dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';
import {showColorPickerDialog} from '../../ui/ColorPicker.jsx';



const bSty= {
    display:'inline-block',
    whiteSpace: 'nowrap'
};

function DrawLayerItemView({drawLayer,pv, maxTitleChars, lastItem, getUIComponent}) {
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

    var plotId= pv.plotId;
    return (
        <div style={style} className='draw-layer-item'>
            <div style={{lineHeight:'1em', position: 'relative', display:'inline-flex',
                         flexDirection:'row', flexWrap:'nowrap',
                         justifyContent: 'space-between',
                         alignItems: 'center',
                         width:'100%'
                         }} >
                <div>
                    <input type='checkbox'
                           checked={isDrawLayerVisible(drawLayer,plotId)}
                           onChange={(ev) => changeVisible(drawLayer, plotId)}
                    />
                    {getTitleTag(drawLayer,pv,maxTitleChars)}
                </div>
                <div style={{padding:'0 4px 0 5px'}}>
                    {makeColorChange(drawLayer,pv)}
                    {makeShape(drawLayer,pv)}
                    {makeDelete(drawLayer,pv)}
                </div>
            </div>
            <div style={{paddingTop:5, marginLeft:'2em'}}>
                {getUIComponent ? getUIComponent(drawLayer,pv) : ''}
            </div>
            {makeHelpLine(drawLayer.helpLine)}
        </div>
    );
}


DrawLayerItemView.propTypes= {
    drawLayer     : React.PropTypes.object.isRequired,
    pv            : React.PropTypes.object.isRequired,
    maxTitleChars : React.PropTypes.number.isRequired,
    lastItem      : React.PropTypes.bool.isRequired,
    getUIComponent: React.PropTypes.func
};


function getTitleTag(drawLayer,pv,maxTitleChars) {
    const tStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap',
        minWidth: (maxTitleChars*.7)+'em',
        // paddingLeft : 5
    };

    return (
        <div style={tStyle}>{`Show: ${getLayerTitle(pv.plotId,drawLayer)}`}</div>
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
                <a className='ff-href'
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
            <a className='ff-href' onClick={() => deleteLayer(drawLayer,pv.plotId)} style={deleteStyle}>Delete</a>
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
    dispatchDetachLayerFromPlot(dl.displayGroupId,plotId,true, true, dl.destroyWhenAllDetached);
}

function changeVisible(dl, plotId) {
    dispatchChangeVisibility(dl.displayGroupId, !isDrawLayerVisible(dl,plotId),plotId );
}

export default DrawLayerItemView;
