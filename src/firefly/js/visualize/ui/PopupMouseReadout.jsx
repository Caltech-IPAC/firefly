/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {Fragment} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {HIPS_STANDARD_READOUT, dispatchChangeLockUnlockByClick} from '../../visualize/MouseReadoutCntlr.js';
import {getNonFluxDisplayElements, getFluxInfo} from './MouseReadoutUIUtil.js';
import {MouseReadoutLock, DataReadoutItem } from './MouseReadout.jsx';
import './MouseReadout.css';

//--------------- Icons --------------------------------
import LOCKED from    'html/images/icons-2014/lock_20x20.png';
import UNLOCKED from  'html/images/icons-2014/unlock_20x20.png';

const rS = { width: 485, position: 'relative'};
const rSMin = { padding: '2px 0 2px 3px' };

export function PopupMouseReadoutFull({readout, readoutData, showHealpixPixel=false}){
    const {threeColor, readoutType}= readoutData;
    const isHiPS= readoutType===HIPS_STANDARD_READOUT;

    if (!get(readoutData,'readoutItems')) return <div style={rS}/>;
    const displayEle= getNonFluxDisplayElements(readoutData.readoutItems,  readout.readoutPref, isHiPS);
    const {pixelSize, showPixelPrefChange, healpixPixelReadout, healpixNorderReadout}= displayEle;
    const fluxArray = getFluxInfo(readoutData);
    const hipsPixel= showHealpixPixel && isHiPS;
    const showCopy= readout.lockByClick;
    const gridClasses= `mouseReadoutPopupFullGrid ${showCopy?'mouseReadoutPopupFullGrid-withclip' :''}`;

    return (
        <div className={gridClasses} style={rS}>
            <CommonPopReadout {...{readout,displayEle}} />
            <DataReadoutItem lArea='pixSizeLabel' vArea='pixSizeValue'
                                        label={pixelSize.label} value={pixelSize.value} prefChangeFunc={showPixelPrefChange}/>
            {hipsPixel && <DataReadoutItem labelStyle={{gridArea:'redLabel'}} valueStyle={{gridArea:'redValue'}}
                                           label={healpixPixelReadout.label} value={healpixPixelReadout.value}/> }
            {hipsPixel && <DataReadoutItem labelStyle={{gridArea:'greenLabel'}} valueStyle={{gridArea:'greenValue'}}
                                            label={healpixNorderReadout.label} value={healpixNorderReadout.value}/> }
            {!isHiPS && <DataReadoutItem lArea='redLabel' vArea='redValue'
                                         label={fluxArray[0].label} value={fluxArray[0].value}/>}
            {threeColor && <DataReadoutItem lArea='greenLabel' vArea='greenValue'
                                            label={fluxArray[1].label} value={fluxArray[1].value}/> }
            {threeColor && <DataReadoutItem lArea='greenLabel' vArea='blueValue'
                                            label={fluxArray[2].label} value={fluxArray[2].value}/> }
        </div>
    );
}

PopupMouseReadoutFull.propTypes = {
    readout: PropTypes.object,
    showHealpixPixel : PropTypes.bool,
    readoutData: PropTypes.object
};

export function PopupMouseReadoutMinimal({readout,readoutData}){
    if (!readoutData || !readout) return (<div style={rSMin}/>);
    const displayEle= getNonFluxDisplayElements(readoutData.readoutItems,  readout.readoutPref,
                                                readoutData.readoutType===HIPS_STANDARD_READOUT);
    const showCopy= readout.lockByClick;
    const gridClasses= `mouseReadoutPopupMinimalGrid ${showCopy?'mouseReadoutPopupMinimalGrid-withclip' :''}`;
    return (
        <div className={gridClasses} style={rSMin}>
            <CommonPopReadout {...{readout,displayEle}}/>
        </div>
    );
}

PopupMouseReadoutMinimal.propTypes = {
    readout: PropTypes.object,
    readoutData: PropTypes.object
};

function CommonPopReadout({readout,displayEle}) {
    const {readout1, readout2, showReadout1PrefChange, showReadout2PrefChange}= displayEle;
    const showCopy= readout.lockByClick;
    return (
        <Fragment>
            <img style={{gridArea:'lockDialog'}} src= {readout.isLocked ? LOCKED:UNLOCKED}  title= 'Lock the readout panel visible'
                  onClick ={() => dispatchChangeLockUnlockByClick(!readout.isLocked)} />
            <DataReadoutItem lArea='pixReadoutTopLabel' vArea='pixReadoutTopValue' cArea='clipboardIconTop'
                             label={readout1.label} value={readout1.value}
                             copyValue={readout1.copyValue} showCopy={showCopy}
                             prefChangeFunc={showReadout1PrefChange}/>
            <DataReadoutItem lArea='pixReadoutBottomLabel' vArea='pixReadoutBottomValue' cArea='clipboardIconBottom'
                             label={readout2.label} value={readout2.value}
                             copyValue={readout2.copyValue} showCopy={showCopy}
                             prefChangeFunc={showReadout2PrefChange}/>
            <MouseReadoutLock gArea='lock' lockByClick={readout.lockByClick} />
        </Fragment>
    );
}
