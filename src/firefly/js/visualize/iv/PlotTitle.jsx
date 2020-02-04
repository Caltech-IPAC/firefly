/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import Enum from 'enum';
import {sprintf} from '../../externalSource/sprintf';
import {getZoomDesc} from '../ZoomUtil.js';
import {primePlot} from '../PlotViewUtil.js';

import './PlotTitle.css';
import LOADING from 'html/images/gxt/loading.gif';
import {isImage} from '../WebPlot.js';
import {hasWCSProjection} from '../PlotViewUtil';

export const TitleType= new Enum(['INLINE', 'HEAD', 'EXPANDED']);

export function PlotTitle({plotView:pv, titleType, brief, working}) {
    let styleName= '';
    const plot= primePlot(pv);
    const world= hasWCSProjection(plot);
    switch (titleType) {
        case TitleType.INLINE:
            styleName= 'plot-title-inline-title-container';
        break;
        case TitleType.HEAD:
            styleName= 'plot-title-header-title-container';
            break;
        case TitleType.EXPANDED:
            styleName= 'plot-title-expanded-title-container';
            break;

    }
    const zlRet= getZoomDesc(pv);
    let zlStr= world ? `FOV: ${zlRet.fovFormatted}` : zlRet.zoomLevelFormatted;
    let tooltip= world ? `${plot.title}\nHorizontal field of view: ${zlRet.fovFormatted}` : plot.title;
    if (isImage(plot)) tooltip+= `\nZoom Level: ${zlRet.zoomLevelFormatted}`;
    let rotString= null;
    if (pv.rotation) {
        if (pv.plotViewCtx.rotateNorthLock) {
            rotString= 'North';
        } else {
            const angleStr= sprintf('%d',Math.trunc(360-pv.rotation));
            rotString= angleStr + String.fromCharCode(176);
        }
        zlStr+=',';
        tooltip+= `, ${rotString}`;
    }

    return (
        <div className={styleName} title={tooltip}>
            <div className='plot-title-title'>{plot.title}</div>
            {!brief ? <div className='plot-title-zoom'><div title={tooltip} dangerouslySetInnerHTML={{__html:zlStr}}/> </div> : ''}
            {!brief && rotString ? <div title={tooltip} className='plot-title-rotation'>{rotString}</div> : ''}
            {working ?<img style={{width:14,height:14,padding:'0 3px 0 5px'}} src={LOADING}/> : ''}
        </div>
    );
}

PlotTitle.propTypes= {
    plotView : PropTypes.object,
    titleType: PropTypes.object.isRequired,
    working : PropTypes.bool,
    brief : PropTypes.bool.isRequired
};

