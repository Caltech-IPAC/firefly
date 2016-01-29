/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import Enum from 'enum';
import React, {PropTypes} from 'react';
import numeral from 'numeral';
import {RotateType}  from '../PlotState.js';
import {convertZoomToString} from '../ZoomUtil.js';
import {getTaskCount} from '../../core/AppDataCntlr.js';
import './PlotTitle.css';
import LOADING from 'html/images/gxt/loading.gif';

export const TitleType= new Enum(['INLINE', 'HEAD', 'EXPANDED']);

export function PlotTitle({plotId, titleType, brief,titleStr,zoomFactor,plotState}) {
    var styleName= '';
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
    var zlStr= convertZoomToString(zoomFactor);
    var rotString= null;
    if (plotState.isRotated()) {
        if (plotState.getRotateType()===RotateType.NORTH) {
            rotString= 'North';
        } else {
            var angleStr= numeral(plotState.getRotationAngle()).format('#');
            rotString= angleStr + String.fromCharCode(176);
        }
        zlStr+=',';
    }
    var showWorking= getTaskCount(plotId);

    return (
        <div className={styleName}>
            <div className='plot-title-title' >{titleStr}</div>
            {!brief ? <div className='plot-title-zoom'>{zlStr}</div> : ''}
            {!brief && rotString ? <div className='plot-title-rotation'>{rotString}</div> : ''}
            {showWorking ?<img style={{width:14,height:14,padding:'0 3px 0 5px'}} src={LOADING}/> : ''}
        </div>
    );
}

PlotTitle.propTypes= {
    plotId: PropTypes.string,
    titleType: PropTypes.object.isRequired,
    titleStr: PropTypes.string,
    zoomFactor:PropTypes.number,
    annotationOps : PropTypes.object,
    plotState : PropTypes.object,
    brief : PropTypes.bool.isRequired
};

