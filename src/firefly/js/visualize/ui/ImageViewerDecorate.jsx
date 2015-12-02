/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import PlotViewUtil  from '../PlotViewUtil.js';
import PlotGroup  from '../PlotGroup.js';
import ImageViewerView  from './ImageViewerView.jsx';
import React from 'react';

export default ImageViewDecorate;

function ImageViewDecorate({plotView,allPlots}) {

    var style= {width:'100%',
                height:'100%',
                overflow:'hidden',
                position:'relative',
                borderStyle: 'solid',
                borderWidth: '3px 2px 2px 2px',
                borderColor: getBorderColor(plotView)
    };

    return (
        <div className='image-viewer-decorate' style={style}>
            <ImageViewerView plotView={plotView}/>
        </div>
    );
}


ImageViewDecorate.propTypes= {
    plotView : React.PropTypes.object.isRequired,
    allPlots : React.PropTypes.object.isRequired
};


function getBorderColor(pv) {
    if (!pv && !pv.plotId) return 'rgba(0,0,0,.4)';
    if (PlotViewUtil.isActivePlotView(pv.plotId)) return 'orange';

    var group= PlotGroup.getPlotGroupById(pv.plotGroupId);

    if (group && group.lockRelated) return '#005da4';
    else return 'rgba(0,0,0,.4)';
}

