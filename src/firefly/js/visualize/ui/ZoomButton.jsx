/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {UserZoomTypes} from '../ZoomUtil.js';
import {dispatchZoom} from '../ImagePlotCntlr.js';
import {getZoomMax, getNextZoomLevel} from '../ZoomUtil.js';
import {primePlot} from '../PlotViewUtil.js';


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
    FIT: makeZT('FIT',zoomFit,'Zoom the image to fit into the visible space', UserZoomTypes.FIT),
    FILL:makeZT('FILL',zoomFill, 'Zoom the image to Fill the visible space', UserZoomTypes.FILL)
};

const isFitFill= (userZoomType) =>  (userZoomType===UserZoomTypes.FIT || userZoomType===UserZoomTypes.FILL);


function getZoomer() {
    var lastClick= Date.now();
    return (pv,zType) => {
        var time= Date.now();
        var deltaClick= time-lastClick;
        lastClick= time;

        if (zType===ZoomType.UP) {
            if (isZoomMax(pv)) {
                //todo
                console.log('todo show zoom options popup');
                //ZoomOptionsPopup.showZoomOps("You may not zoom beyond " + getZoomMax() + "x", true);
                return;
            }
            else if (deltaClick < CLICK_TIME) {
                //todo
                console.log('todo show zoom options popup');
                //ZoomOptionsPopup.showZoomOps();
                return;
            }
        }
        dispatchZoom(pv.plotId,zType.utilZt,true,false, isFitFill(zType.utilZt));
    };
}

const zoom= getZoomer();


function isZoomMax(pv) {
    var zMax= getZoomMax();
    var {zoomFactor}= primePlot(pv);
    if (zoomFactor>=zMax) return true;

    var nextZ= getNextZoomLevel(zoomFactor,UserZoomTypes.UP);
    return (nextZ>=zMax );
}



export function ZoomButton({plotView:pv,zoomType,visible}) {
    var enable= primePlot(pv) ? true : false;
    return (
        <ToolbarButton icon={zoomType.icon} tip={zoomType.tip}
                       enabled={enable} visible={visible}
                       horizontal={true} onClick={() => zoom(pv,zoomType)}/>
    );
}


ZoomButton.propTypes= {
    plotView : React.PropTypes.object,
    visible : React.PropTypes.bool.isRequired,
    zoomType : React.PropTypes.oneOf([ZoomType.UP,ZoomType.DOWN,ZoomType.ONE,ZoomType.FIT,ZoomType.FILL])
};

