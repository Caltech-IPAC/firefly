/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PlotViewUtil  from '../PlotViewUtil.js';
import DrawLayerItemView from './DrawLayerItemView.jsx';




function DrawLayerPanelView({dlAry, plotView, dialogId, drawLayerFactory}) {
    var style= {width:'calc(100% - 12px)',
        height:'100%',
        padding: 6,
        position: 'relative',
        overflow:'hidden'};

    var layers= PlotViewUtil.getAllDrawLayersForPlot(dlAry,plotView.plotId);
    var maxTitleChars= layers.reduce( (max,l) => {
        var t= PlotViewUtil.getLayerTitle(plotView.plotId,l);
        return Math.max(max, t?t.length:0);
    },3);

    return (
        <div style={style}>
            {makeDrawLayerItemAry(layers,plotView,maxTitleChars, drawLayerFactory)}
        </div>
    );
}


DrawLayerPanelView.propTypes= {
    plotView : React.PropTypes.object.isRequired,
    dlAry : React.PropTypes.array.isRequired,
    dialogId : React.PropTypes.string.isRequired
};


function makeDrawLayerItemAry(layers,pv, maxTitleChars, factory) {
    var last= layers.length-1;
    return layers.map( (l,idx) => <DrawLayerItemView key={l.drawLayerId}
                                                     drawLayer={l}
                                                     pv={pv}
                                                     maxTitleChars={maxTitleChars}
                                                     lastItem={idx===last}
                                                     getUIComponent={factory.getGetUIComponentFunc(l)}
    />);
}



export default DrawLayerPanelView;
