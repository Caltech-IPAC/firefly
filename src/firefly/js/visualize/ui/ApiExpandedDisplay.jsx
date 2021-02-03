/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {visRoot} from '../ImagePlotCntlr.js';
import {VisHeaderView} from './VisHeaderView.jsx';
import {ImageExpandedMode} from '../iv/ImageExpandedMode.jsx';
import {readoutRoot} from '../MouseReadoutCntlr.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {lastMouseImageReadout, lastMouseCtx} from '../VisMouseSync.js';
import {useMouseStoreConnector} from './MouseStoreConnector.jsx';

function makeState() {
    return {vr:visRoot(), currMouseState:lastMouseCtx(), readoutData:lastMouseImageReadout(), readout:readoutRoot()};
}

export const ApiExpandedDisplay= memo( ({closeFunc=undefined, viewerId, showHealpixPixel}) => {

    const showHP= showHealpixPixel ??getAppOptions()?.hips?.readoutShowsPixel;
    const {vr,currMouseState, readout, readoutData}= useMouseStoreConnector(makeState);

    return (
        <div style={{width:'100%', height:'100%', display:'flex', flexWrap:'nowrap',
            alignItems:'stretch', flexDirection:'column'}}>
            <div style={{position: 'relative', marginBottom:'6px'}} className='banner-background'>
                <VisHeaderView visRoot={vr} currMouseState={currMouseState}
                               readoutData={readoutData} readout={readout}
                               style={{
                                   height: 34,
                                   minHeight: 34,
                                   padding: '2px 0 1px 0'
                               }}
                               showHealpixPixel={showHP}/>
            </div>
            <div style={{flex: '1 1 auto', display:'flex'}}>
                <ImageExpandedMode   {...{key:'results-plots-expanded', closeFunc, viewerId}}/>
            </div>
        </div>
    );
});

ApiExpandedDisplay.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    viewerId: PropTypes.string,
    showHealpixPixel: PropTypes.string // getAppOptions()?.hips?.readoutShowsPixel
};
