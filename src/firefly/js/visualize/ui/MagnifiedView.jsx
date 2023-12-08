/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack, Typography} from '@mui/joy';
import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {MouseState} from '../VisMouseSync.js';
import {isHiPS} from '../WebPlot.js';
import {hasLocalStretchByteData, primePlot} from '../PlotViewUtil.js';
import {makeThumbnailTransformCSS} from '../PlotTransformUtils.js';
import {SimpleCanvas} from '../draw/SimpleCanvas.jsx';
import {PlotAttribute} from 'firefly/visualize/PlotAttribute.js';
import {CCUtil} from 'firefly/visualize/CsysConverter.js';
import {getLocalScreenTileAtZoom} from 'firefly/visualize/rawData/RawTileDrawer.js';


const defStyle= {
    width: 70,
    height: 70,
    display: 'inline-block',
    overflow : 'hidden',
    position: 'relative',
    border: '1px solid transparent'
};


const makeEmpty= (size) => (<div style={{...defStyle, width:size, height:size}}/>);

export const MagnifiedView= memo(({plotView:pv,size,mouseState,lockByClick=false}) => {
    const magMouse= [MouseState.DRAG_COMPONENT.key, MouseState.DRAG.key,
        MouseState.MOVE.key, MouseState.DOWN.key, MouseState.CLICK.key];
    if (!pv || !mouseState?.screenPt) return makeEmpty(size);
    const p= primePlot(pv);
    if (!p || isHiPS(p) ) return makeEmpty(size);
    let pt;
    if (lockByClick && p?.attributes[PlotAttribute.ACTIVE_POINT]) {
        pt= CCUtil.getScreenCoords(p, p.attributes[PlotAttribute.ACTIVE_POINT]?.pt);
        if (!pt) return makeEmpty(size);
    }
    else {
        if (!magMouse.includes(mouseState.mouseState?.key)) return makeEmpty(size);
        pt= mouseState.screenPt;
    }

    return (
        <div style={{...defStyle, width:size, height:size, border: '1px solid rgb(187, 187, 187)'}}>
            {showMag(pt,pv, p,size)}
        </div>
    );
});

MagnifiedView.propTypes= {
    plotView: PropTypes.object,
    size: PropTypes.number.isRequired,
    lockByClick: PropTypes.bool,
    mouseState: PropTypes.object
};

const showTooHighZoomMessage= () => (
    <Stack {...{direction:'column' ,
            alignItems: 'center',
            justifyContent: 'center',
            mt:1,
        }} >
        <Typography level='body-sm'>Zoom Factor</Typography>
        <Typography level='body-sm'>Too High</Typography>
        <Typography level='body-sm'>Magnifier Off</Typography>
    </Stack> );


function showMag(spt,pv, plot,size) {
    if (!plot) return false;
    if (plot.zoomFactor > 6) return showTooHighZoomMessage();

    const {width:screenW, height:screenH }= plot.screenSize;
    let sizeOffX, sizeOffY;

    let x = spt.x - size / 2;
    let y = spt.y - size / 2;

    if (x < 0) {
        x = 0;
        sizeOffX = spt.x;
    } else if (x > screenW - size) {
        x = screenW - size;
        sizeOffX = (size / 2) + ((size / 2) - (screenW - spt.x));
    } else {
        sizeOffX = size / 2;
    }

    if (y < 0) {
        y = 0;
        sizeOffY = spt.y;
    } else if (y > screenH - size) {
        y = screenH - size;
        sizeOffY = (size / 2) + ((size / 2) - (screenH - spt.y));
    } else {
        sizeOffY = size / 2;
    }

    return showMagUsingLocal(x,y,pv,plot,size,sizeOffX,sizeOffY);
}

function showMagUsingLocal(x,y,pv, plot,size,sizeOffX,sizeOffY) {
    if (!hasLocalStretchByteData(plot)) return <div/>;
    const style= {
        transform :makeThumbnailTransformCSS(pv.rotation,pv.flipX, pv.flipY),
        width: size,
        height: size,
        position: 'relative'
    };
    const magFactor=2;
    const magCanvas= getLocalScreenTileAtZoom(plot,(x*magFactor+sizeOffX),(y*magFactor+sizeOffY),size,size,plot.zoomFactor*magFactor);
    if (!magCanvas) return <div/>;
    // const dataUrl=  magCanvas.toDataURL();

    const drawOnCanvas= (targetCanvas) => {
        if (!targetCanvas) return;
        const ctx= targetCanvas.getContext('2d');
        ctx.clearRect(0, 0, targetCanvas.width, targetCanvas.height);
        targetCanvas.getContext('2d').drawImage(magCanvas,0,0);
    };

    // const s= { position : 'absolute', left : 0, top : 0};
    return (
        <div style={style}>
            <SimpleCanvas drawIt={drawOnCanvas} width={size} height={size}
                          id={'magnifier'}/>
        </div>
    );
}
