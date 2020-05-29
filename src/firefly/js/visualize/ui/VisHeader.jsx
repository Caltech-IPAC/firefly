/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {visRoot} from '../ImagePlotCntlr.js';
import {VisHeaderView, VisPreview} from './VisHeaderView.jsx';
import {readoutRoot} from '../MouseReadoutCntlr.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {lastMouseCtx, lastMouseImageReadout} from '../VisMouseSync';
import {useMouseStoreConnector} from '../../ui/SimpleComponent';

function makeState() {
    return {vr:visRoot(), currMouseState:lastMouseCtx(), readoutData:lastMouseImageReadout(), readout:readoutRoot()};
}

export const VisHeader= memo( ({
                     showHeader=true, showPreview=true, showHealpixPixel= getAppOptions().hips?.readoutShowsPixel}) =>{

    const {vr, currMouseState,readout, readoutData}= useMouseStoreConnector(makeState);
    return (
        <div>
            {showHeader && <VisHeaderView {...{readout, readoutData, showHealpixPixel:Boolean(showHealpixPixel)}}/>}
            {showPreview && <VisPreview {...{visRoot:vr, currMouseState}}/>}
        </div>
    );
});

VisHeader.propTypes= {
    showHeader : PropTypes.bool,
    showPreview : PropTypes.bool,
    showHealpixPixel : PropTypes.bool, // defaults to appOptions
};
