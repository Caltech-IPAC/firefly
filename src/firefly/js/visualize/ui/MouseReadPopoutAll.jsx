import {Box} from '@mui/joy';
import {LayoutType, PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {dispatchShowDialog} from 'firefly/core/ComponentCntlr.js';
import React from 'react';
import {lastMouseCtx, lastMouseImageReadout, MouseState} from 'firefly/visualize/VisMouseSync.js';
import {
    HIPS_STANDARD_READOUT, isLockByClick, readoutRoot, STANDARD_READOUT
} from 'firefly/visualize/MouseReadoutCntlr.js';
import {useMouseStoreConnector} from 'firefly/visualize/ui/MouseStoreConnector.jsx';
import {visRoot} from 'firefly/visualize/ImagePlotCntlr.js';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';
import {ThumbnailView} from 'firefly/visualize/ui/ThumbnailView.jsx';
import {MagnifiedView} from 'firefly/visualize/ui/MagnifiedView.jsx';
import {getActivePlotView, getPlotViewById, primePlot} from 'firefly/visualize/PlotViewUtil.js';
import {getFluxInfo, getNonFluxDisplayElements} from 'firefly/visualize/ui/MouseReadoutUIUtil.js';
import {DataReadoutItem, MouseReadoutLock} from 'firefly/visualize/ui/MouseReadout.jsx';
import {isImage} from 'firefly/visualize/WebPlot.js';
import {getBixPix} from '../FitsHeaderUtil.js';
import {showMouseReadoutFluxRadixDialog} from './MouseReadoutOptionPopups.jsx';

export const MOUSE_READOUT_DIALOG_ID= 'MouseReadoutPopoutAll';

export function showMouseReadoutPopout(closeCallback) {
    DialogRootContainer.defineDialog(MOUSE_READOUT_DIALOG_ID, <MousePopup closeCallback={closeCallback}/>);
    dispatchShowDialog(MOUSE_READOUT_DIALOG_ID);
}



function makeState() {
    return {vr:visRoot(), currMouseState:lastMouseCtx(), readoutData:lastMouseImageReadout(), readout:readoutRoot()};
}

function MousePopup({closeCallback,visible, requestOnTop, dialogId, zIndex, requestToClose}) {
    const {vr,currMouseState, readout, readoutData}= useMouseStoreConnector(makeState);
    return (
        <div>
            <PopupPanel {...{title:primePlot(visRoot(),currMouseState.plotId)?.title || 'Mouse Readout',
                closeCallback, layoutPosition:LayoutType.TOP_RIGHT,
                visible, requestOnTop, dialogId, zIndex, requestToClose} }>
                <PopoutMouseReadoutContents {...{vr,currMouseState, readout, readoutData}}/>
            </PopupPanel>
        </div>
    );
}

function PopoutMouseReadoutContents({vr,currMouseState, readout, readoutData}) {
    const showHealpixPixel= getAppOptions()?.hips?.readoutShowsPixel ?? false;
    const pv= getActivePlotView(vr);

    const mousePv= getPlotViewById(vr,currMouseState.plotId);
    const pvForThumbnail= currMouseState.mouseState===MouseState.EXIT ? pv : mousePv;
    const image= isImage(primePlot(pvForThumbnail));
    const lockByClick= isLockByClick(readoutRoot());
    const pvToUse= lockByClick ? pv : mousePv;
    const {readoutPref}= readoutRoot();
    const radix= Number(getBixPix(primePlot(pvToUse))>0 ? readoutPref.intFluxValueRadix : readoutPref.floatFluxValueRadix);

    return (
        <div>
            {image &&
            <div style={{padding: '5px 5px 2px 5px', display: 'flex', flexDirection: 'row', alignItems: 'center'}}>
                <ThumbnailView plotView={pvForThumbnail} loadedSize={140}/>
                <MagnifiedView plotView={pvToUse} size={140} mouseState={currMouseState} lockByClick={lockByClick} />
            </div>}
            <Readout readout={readout} readoutData={readoutData} showHealpixPixel={showHealpixPixel} radix={radix}/>
        </div>

    );
}



const rS = { width: '23rem', position: 'relative', p: .5};


function Readout({readout, readoutData, showHealpixPixel=false, radix}){
    const {threeColor, readoutType}= readoutData;
    const isHiPS= readoutType===HIPS_STANDARD_READOUT;
    const image= readoutType===STANDARD_READOUT;

    if (!readoutData?.readoutItems) return <Box style={rS}/>;
    const displayEle= getNonFluxDisplayElements(readoutData,  readout.readoutPref, isHiPS);
    const {pixelSize, showPixelPrefChange, healpixPixelReadout, healpixNorderReadout}= displayEle;
    const fluxArray = getFluxInfo(readoutData, radix);
    const hipsPixel= showHealpixPixel && isHiPS;
    const showCopy= readout.lockByClick;
    const {readout1, readout2, showReadout1PrefChange, showReadout2PrefChange, showWavelengthFailed, waveLength}= displayEle;

    return (
        <Box sx={{
            display: 'grid',
            columnGap: .5,
            rowGap: .75,
            gridTemplateColumns: '6em 14px auto',
            gridTemplateRows: `2em 1.4em 1.4em 1.4em 1.4em${threeColor ? ' 1.4em 1.4em' : ''}`,
            alignItems: 'center',
            gridTemplateAreas: `". . lock"
                                "pixSizeLabel . pixSizeValue"
                                "pixReadoutTopLabel clipboardIconTop pixReadoutTopValue"
                                "pixReadoutBottomLabel clipboardIconBottom pixReadoutBottomValue"
                                "redLabel . redValue"
                                "greenLabel . greenValue"
                                " blueLabel . blueValue"`,
            ...rS
        }}>
            <DataReadoutItem lArea='pixReadoutTopLabel' vArea='pixReadoutTopValue' cArea='clipboardIconTop'
                             label={readout1.label} value={readout1.value}
                             copyValue={readout1.copyValue} showCopy={showCopy}
                             prefChangeFunc={showReadout1PrefChange}/>
            <DataReadoutItem lArea='pixReadoutBottomLabel' vArea='pixReadoutBottomValue' cArea='clipboardIconBottom'
                             label={readout2.label} value={readout2.value}
                             copyValue={readout2.copyValue} showCopy={showCopy}
                             prefChangeFunc={showReadout2PrefChange}/>
            <DataReadoutItem lArea='pixSizeLabel' vArea='pixSizeValue'
                             label={pixelSize.label} value={pixelSize.value} prefChangeFunc={showPixelPrefChange}/>
            {hipsPixel && <DataReadoutItem labelStyle={{gridArea:'redLabel'}} valueStyle={{gridArea:'redValue'}}
                                           label={healpixPixelReadout.label} value={healpixPixelReadout.value}/> }
            {hipsPixel && <DataReadoutItem labelStyle={{gridArea:'greenLabel'}} valueStyle={{gridArea:'greenValue'}}
                                           label={healpixNorderReadout.label} value={healpixNorderReadout.value}/> }
            {!isHiPS && <DataReadoutItem lArea='redLabel' vArea='redValue'
                                         monoFont={radix===16}
                                         prefChangeFunc={() =>showMouseReadoutFluxRadixDialog(readout.readoutPref)}
                                         label={fluxArray[0].label||'Value:'}
                                         unit={fluxArray[0].unit??''}
                                         value={fluxArray[0].value}/>}
            {!threeColor && waveLength && image &&
                <DataReadoutItem lArea='greenLabel' vArea='greenValue' label={waveLength.label} value={waveLength.value}
                                                     prefChangeFunc={showWavelengthFailed} /> }
            {threeColor && <DataReadoutItem lArea='greenLabel' vArea='greenValue'
                                            monoFont={radix===16}
                                            prefChangeFunc={() =>showMouseReadoutFluxRadixDialog(readout.readoutPref)}
                                            label={fluxArray[1].label}
                                            unit={fluxArray[1].unit??''}
                                            value={fluxArray[1].value}/> }
            {threeColor && <DataReadoutItem lArea='blueLabel' vArea='blueValue'
                                            monoFont={radix===16}
                                            prefChangeFunc={() =>showMouseReadoutFluxRadixDialog(readout.readoutPref)}
                                            label={fluxArray[2].label}
                                            unit={fluxArray[2].unit??''}
                                            value={fluxArray[2].value}/> }
            <MouseReadoutLock gArea='lock' lockByClick={readout.lockByClick} />
        </Box>
    );
}

