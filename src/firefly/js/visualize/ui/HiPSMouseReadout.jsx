/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {getNonFluxDisplayElements} from './MouseReadoutUIUtil.js';
import {MouseReadoutLock,DataReadoutItem} from './MouseReadout';

import './MouseReadout.css';

export function HiPSMouseReadout({readout, readoutData, showHealpixPixel=true }){


    if (!readoutData.readoutItems) return (<div className='mouseReadoutDisplaySpace'/>);

    const title= get(readoutData, 'readoutItems.title.value','');

    const displayEle= getNonFluxDisplayElements(readoutData.readoutItems,  readout.readoutPref, true);
    const {readout1, readout2, pixelSize, showReadout1PrefChange,
         showReadout2PrefChange, showPixelPrefChange, healpixPixelReadout, healpixNorderReadout}= displayEle;

    return (
        <div className='mouseReadoutHiPSGrid mouseReadoutDisplaySpace'>

            {showHealpixPixel && <DataReadoutItem lArea='nOrderLabel' vArea='nOrderValue'
                                                  label={healpixNorderReadout.label}
                                                  value={healpixNorderReadout.value}/>}
            {showHealpixPixel && <DataReadoutItem lArea='healpixPixelLabel' vArea='healpixPixelValue'
                                                  label={healpixPixelReadout.label}
                                                  value={healpixPixelReadout.value}/>}

            <DataReadoutItem lArea='pixReadoutTopLabel' vArea='pixReadoutTopValue'
                             label={readout1.label} value={readout1.value} prefChangeFunc={showReadout1PrefChange}/>
            <DataReadoutItem lArea='pixReadoutBottomLabel' vArea='pixReadoutBottomValue'
                             label={readout2.label} value={readout2.value} prefChangeFunc={showReadout2PrefChange}/>
            <DataReadoutItem lArea='pixSizeLabel' vArea='pixSizeValue'
                             label={pixelSize.label} value={pixelSize.value} prefChangeFunc={showPixelPrefChange}/>
            <MouseReadoutLock gArea='lock' lockByClick={readout.lockByClick} />

            <div style={{gridArea:'plotTitle', paddingLeft:4}}> {title}  </div>
        </div>
    );
}

HiPSMouseReadout.propTypes = {
    readout: PropTypes.object,
    readoutData: PropTypes.object,
    showHealpixPixel : PropTypes.bool
};
