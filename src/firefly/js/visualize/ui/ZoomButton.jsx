/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {dispatchZoom} from '../ImagePlotCntlr.js';
import {getZoomMax, getNextZoomLevel, UserZoomTypes} from '../ZoomUtil.js';
import {primePlot} from '../PlotViewUtil.js';
import {showZoomOptionsPopup} from '../../ui/ZoomOptionsPopup.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {isImage} from '../WebPlot.js';
import {MAX_SUPPORTED_HIPS_LEVEL} from '../HiPSUtil.js';

import zoomDown from 'html/images/icons-2014/ZoomOut.png';
import zoomUp from 'html/images/icons-2014/ZoomIn.png';
import zoomOriginal from 'html/images/icons-2014/Zoom1x.png';
import zoomFit from 'html/images/icons-2014/28x28_ZoomFitToSpace.png';
import zoomFill from 'html/images/icons-2014/ZoomFillWidth.png';

const CLICK_TIME= 200;
const makeZT= (name,icon,tip,utilZt) => ({name,icon,tip,utilZt});

export const ZoomType= {
    UP:  makeZT('UP',zoomUp,'Zoom the image in',UserZoomTypes.UP),
    DOWN:makeZT('DOWN',zoomDown,'Zoom the image out',UserZoomTypes.DOWN),
    ONE: makeZT('ONE',zoomOriginal,'Zoom to the original size',UserZoomTypes.ONE),
    FIT: makeZT('FIT',zoomFit,'Zoom the image to fit entirely within its frame', UserZoomTypes.FIT),
    FILL:makeZT('FILL',zoomFill, 'Zoom the image to fill its frame horizontally', UserZoomTypes.FILL)
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
                if (isImage(plot)) {
                    msg= 'You may not zoom beyond ' + getZoomMax(plot) + 'x';
                }
                else {
                    msg= `You may not zoom beyond HiPS Norder level ${MAX_SUPPORTED_HIPS_LEVEL}`;
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

    var nextZ= getNextZoomLevel(plot,UserZoomTypes.UP);
    return (nextZ>zMax );
}

export function ZoomButton({plotView:pv,zoomType,visible}) {
    return (
        <ToolbarButton icon={zoomType.icon} tip={zoomType.tip}
                       enabled={Boolean(primePlot(pv))} visible={visible}
                       horizontal={true} onClick={() => zoom(pv,zoomType)}/>
    );
}


ZoomButton.propTypes= {
    plotView : PropTypes.object,
    visible : PropTypes.bool.isRequired,
    zoomType : PropTypes.oneOf([ZoomType.UP,ZoomType.DOWN,ZoomType.ONE,ZoomType.FIT,ZoomType.FILL])
};

