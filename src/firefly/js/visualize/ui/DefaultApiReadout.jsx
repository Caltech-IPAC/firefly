/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {visRoot} from '../ImagePlotCntlr.js';
import {readoutRoot} from '../MouseReadoutCntlr.js';
import {getActivePlotView, getPlotViewById} from '../PlotViewUtil.js';
import {ThumbnailView} from './ThumbnailView.jsx';
import {MagnifiedView} from './MagnifiedView.jsx';
import {lastMouseImageReadout, lastMouseCtx} from '../VisMouseSync';
import {useMouseStoreConnector} from '../../ui/SimpleComponent';

const style= {
    display: 'inline-block',
    position: 'relative',
    verticalAlign: 'top',
    cursor:'pointer',
    whiteSpace : 'nowrap',
    overflow : 'hidden'
};

const makeState= () => ({currMouseState:lastMouseCtx(), readoutData:lastMouseImageReadout(), readout:readoutRoot()});

export const DefaultApiReadout= memo( ({MouseReadoutComponent, showThumb=true, showMag=true}) => {
    const {currMouseState, readout, readoutData}= useMouseStoreConnector(makeState);

    if (!showThumb && !showMag) {
        return (
            <div style={{display:'inline-block', float:'right', whiteSpace:'nowrap'}}>
                <MouseReadoutComponent readout={readout} readoutData={readoutData}/>
            </div>
            );
    }
    else {
        const vr= visRoot();
        return (
            <div style={{display:'flex',flexWrap:'nowrap'}}>
                <div style={style}>
                    <div style={{position:'relative', color:'black', height:'100%'}}>
                        <MouseReadoutComponent readout={readout} readoutData={readoutData} showMag={ showMag} showThumb={showThumb}/>
                    </div>
                </div>
                {showThumb && <ThumbnailView  plotView={getActivePlotView(vr)}/>}
                {showMag && <MagnifiedView plotView={getPlotViewById(vr, currMouseState.plotId)}
                                           size={70} mouseState={currMouseState}/>}
            </div>
        );
    }
});

DefaultApiReadout.propTypes= {
    closeFunc: PropTypes.func,
    showThumb: PropTypes.bool,
    showMag: PropTypes.bool,
    MouseReadoutComponent : PropTypes.elementType
};
