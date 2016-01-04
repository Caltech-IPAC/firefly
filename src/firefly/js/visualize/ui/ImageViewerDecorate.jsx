/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {getPlotGroupById}  from '../PlotGroup.js';
import {visRoot} from '../ImagePlotCntlr.js';
import PlotViewUtil from '../PlotViewUtil.js';
import ImageViewerView  from './ImageViewerView.jsx';
import React from 'react';

export default ImageViewDecorate;

function ImageViewDecorate({plotView,drawLayersAry,visRoot}) {

    var style= {width:'100%',
                height:'100%',
                overflow:'hidden',
                position:'relative',
                borderStyle: 'solid',
                borderWidth: '3px 2px 2px 2px',
                borderColor: getBorderColor(plotView,visRoot)
    };

    return (
        <div className='image-viewer-decorate' style={style}>
            <ImageViewerView plotView={plotView} drawLayersAry={drawLayersAry}/>
        </div>
    );
}


ImageViewDecorate.propTypes= {
    plotView : React.PropTypes.object.isRequired,
    drawLayersAry: React.PropTypes.array.isRequired,
    visRoot: React.PropTypes.object.isRequired
};


function getBorderColor(pv,visRoot) {
    if (!pv && !pv.plotId) return 'rgba(0,0,0,.4)';

    if (PlotViewUtil.isActivePlotView(visRoot,pv.plotId)) return 'orange';

    var group= getPlotGroupById(visRoot,pv.plotGroupId);

    if (group && group.lockRelated) return '#005da4';
    else return 'rgba(0,0,0,.4)';
}

