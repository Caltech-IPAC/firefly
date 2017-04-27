/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import Enum from 'enum';
import React, {PropTypes} from 'react';
import numeral from 'numeral';
import {convertZoomToString} from '../ZoomUtil.js';
import {primePlot} from '../PlotViewUtil.js';
import {getTaskCount} from '../../core/AppDataCntlr.js';

import './PlotTitle.css';
import LOADING from 'html/images/gxt/loading.gif';

export const TitleType= new Enum(['INLINE', 'HEAD', 'EXPANDED']);

export function PlotTitle({plotView:pv, titleType, brief}) {
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
    let zlStr= convertZoomToString(plot.zoomFactor);
    let rotString= null;
    if (pv.rotation) {
        if (pv.plotViewCtx.rotateNorthLock) {
            rotString= 'North';
        } else {
            const angleStr= numeral(360-pv.rotation).format('#');
            rotString= angleStr + String.fromCharCode(176);
        }
        zlStr+=',';
    }
    const showWorking= getTaskCount(pv.plotId);

    return (
        <div className={styleName}>
            <div className='plot-title-title' >{plot.title}</div>
            {!brief ? <div className='plot-title-zoom'>{zlStr}</div> : ''}
            {!brief && rotString ? <div className='plot-title-rotation'>{rotString}</div> : ''}
            {showWorking ?<img style={{width:14,height:14,padding:'0 3px 0 5px'}} src={LOADING}/> : ''}
        </div>
    );
}

PlotTitle.propTypes= {
    plotView : PropTypes.object,
    titleType: PropTypes.object.isRequired,
    annotationOps : PropTypes.object,
    brief : PropTypes.bool.isRequired
};

