/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {dispatchZoom} from '../ImagePlotCntlr.js';
import {getZoomMax, getNextZoomLevel, UserZoomTypes, getZoomDesc} from '../ZoomUtil.js';
import {primePlot} from '../PlotViewUtil.js';
import {showZoomOptionsPopup} from '../../ui/ZoomOptionsPopup.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {isImage} from '../WebPlot.js';
import {MAX_SUPPORTED_HIPS_LEVEL} from '../HiPSUtil.js';

import { Zoom1XButton, ZoomDownButton, ZoomFillButton, ZoomFitButton, ZoomUpButton, } from './Buttons.jsx';

const CLICK_TIME= 175;
const makeZT= (name,tip,utilZt) => ({name,tip,utilZt});

export const ZoomType= {
    UP:  makeZT('UP', 'Zoom the image in',UserZoomTypes.UP),
    DOWN:makeZT('DOWN', 'Zoom the image out',UserZoomTypes.DOWN),
    ONE: makeZT('ONE', 'Zoom to the original size',UserZoomTypes.ONE),
    FIT: makeZT('FIT', 'Zoom the image to fit entirely within its frame', UserZoomTypes.FIT),
    FILL:makeZT('FILL', 'Zoom the image to fill its frame horizontally', UserZoomTypes.FILL)
};

const isFitFill= (userZoomType) =>  (userZoomType===UserZoomTypes.FIT || userZoomType===UserZoomTypes.FILL);


function getZoomer() {
    let lastClick= Date.now();
    return (pv,zType) => {
        const time= Date.now();
        const deltaClick= time-lastClick;
        const plot= primePlot(pv);
        lastClick= time;

        if (zType===ZoomType.UP) {
            if (isZoomMax(pv)) {
                let msg;
                const zlRet= getZoomDesc(pv);
                if (isImage(plot)) {
                    msg= `Maximum Zoom: You reached your maximum zoom: FOV: ${zlRet.fovFormatted}`;
                }
                else {
                    msg= `You may not zoom beyond HiPS Norder level ${MAX_SUPPORTED_HIPS_LEVEL}, FOV: ${zlRet.fovFormatted}`;
                }

                const renderContent= <div style={{padding: '5px 0 5px 0',whiteSpace: 'nowrap'}}> {msg} </div> ;
                showInfoPopup(renderContent, 'Zoom Info');
                return;
            }
        }

        if (deltaClick < CLICK_TIME   && isImage(plot) && !plot.projection.isWrappingProjection()) {
            showZoomOptionsPopup();
            return;
        }

        dispatchZoom({ plotId:pv.plotId, userZoomType:zType.utilZt, forceDelay:!isFitFill(zType.utilZt) });
    };
}

const zoom= getZoomer();


export function isZoomMax(pv) {
    const plot= primePlot(pv);
    const {zoomFactor}= plot;
    const zMax= getZoomMax(plot);
    if (zoomFactor>=zMax) return true;
    const nextZ= getNextZoomLevel(plot,UserZoomTypes.UP);
    return (nextZ>zMax );
}

export function ZoomButton({plotView:pv,zoomType,visible=true}) {
    const sx= {'.ff-toolbar-iconbutton':{p:'1px'}};
    const enabled= Boolean(primePlot(pv));
    const tip= zoomType.tip;
    const onClick= () => zoom(pv,zoomType);
    switch (zoomType) {
        case ZoomType.UP : return ( <ZoomUpButton {...{tip, enabled, visible, sx, onClick}}/> );
        case ZoomType.DOWN : return ( <ZoomDownButton {...{tip, enabled, visible, sx, onClick}}/> );
        case ZoomType.ONE : return ( <Zoom1XButton {...{tip, enabled, visible, sx, onClick}}/> );
        case ZoomType.FIT : return ( <ZoomFitButton {...{tip, enabled, visible, sx, onClick}}/> );
        case ZoomType.FILL : return ( <ZoomFillButton {...{tip, enabled, visible, sx, onClick}}/> );
    };
}

ZoomButton.propTypes= {
    plotView : PropTypes.object,
    visible : PropTypes.bool,
    size: PropTypes.number,
    zoomType : PropTypes.oneOf([ZoomType.UP,ZoomType.DOWN,ZoomType.ONE,ZoomType.FIT,ZoomType.FILL])
};
