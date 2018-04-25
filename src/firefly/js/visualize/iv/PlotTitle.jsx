/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import Enum from 'enum';
import numeral from 'numeral';
import {getZoomDesc} from '../ZoomUtil.js';
import {primePlot} from '../PlotViewUtil.js';

import './PlotTitle.css';
import LOADING from 'html/images/gxt/loading.gif';
import {isImage} from '../WebPlot.js';

export const TitleType= new Enum(['INLINE', 'HEAD', 'EXPANDED']);

export function PlotTitle({plotView:pv, titleType, brief, working}) {
    let styleName= '';
    const plot= primePlot(pv);
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
    let zlStr= `FOV: ${zlRet.fovFormatted}`;
    let tooltip= `${plot.title}\nHorizontal field of view: ${zlRet.fovFormatted}`;
    if (isImage(plot)) tooltip+= `\nZoom Level: ${zlRet.zoomLevelFormatted}`;
    let rotString= null;
    if (pv.rotation) {
        if (pv.plotViewCtx.rotateNorthLock) {
            rotString= 'North';
        } else {
            const angleStr= numeral(360-pv.rotation).format('#');
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
    annotationOps : PropTypes.object,
    brief : PropTypes.bool.isRequired
};

