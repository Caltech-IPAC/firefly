/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo,useState,useEffect} from 'react';
import PropTypes from 'prop-types';
import CanvasWrapper from './CanvasWrapper.jsx';
import {getDrawLayerById} from '../PlotViewUtil.js';
import {dlRoot} from '../DrawLayerCntlr.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';


const isVisible= (drawLayer,plotId) => drawLayer?.visiblePlotIdAry.includes(plotId);

function findDrawLayerInStore(prevDrawLayer, drawLayerId) {
    if (!drawLayerId) return undefined;
    const drawLayer= getDrawLayerById(dlRoot(), drawLayerId);
    return prevDrawLayer!==drawLayer ? drawLayer : prevDrawLayer;
}

export const DrawerComponent= memo( (props) => {

    const {plot, width, height, idx, drawLayerId, setSimpleUpdateNotify, getDrawLayer }=  props;
    const [drawLayerManual, setDrawLayerManual]= useState(() => undefined);
    const drawLayerFromStore = useStoreConnector((prev)  => findDrawLayerInStore(prev,drawLayerId));

    useEffect(() => {
            if (!setSimpleUpdateNotify) return;
            let mounted= true;
            setSimpleUpdateNotify( (newDL) => newDL !== drawLayerManual && mounted && setDrawLayerManual(newDL) );
            return () => { mounted= false; };
        }
    );

    const drawLayerToUse= (plot && drawLayerId) ? drawLayerFromStore : drawLayerManual;

    const getDrawLayerWrapper= getDrawLayer ? getDrawLayer : () => drawLayerToUse;
    if (plot&& !isVisible(drawLayerToUse,plot.plotId)) return false;

    return (
        <div className='drawerComponent' style={{position:'absolute',left:0,right:0,width,height}}>
            <CanvasWrapper {...props} getDrawLayer={getDrawLayerWrapper} idx={idx}/>
        </div>
    );
});

DrawerComponent.propTypes= {
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    plot: PropTypes.object, // plot is not used if drawLayer is not passed
    drawLayerId : PropTypes.string, //drawLayer or drawData is Required
    getDrawLayer : PropTypes.func,
    idx : PropTypes.number,
    setSimpleUpdateNotify : PropTypes.func
};
