import {LayoutType, PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {dispatchShowDialog} from 'firefly/core/ComponentCntlr.js';
import React from 'react';
import {lastMouseCtx, lastMouseImageReadout, MouseState} from 'firefly/visualize/VisMouseSync.js';
import {HIPS_STANDARD_READOUT, isLockByClick, readoutRoot} from 'firefly/visualize/MouseReadoutCntlr.js';
import {useMouseStoreConnector} from 'firefly/visualize/ui/MouseStoreConnector.jsx';
import {visRoot} from 'firefly/visualize/ImagePlotCntlr.js';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';
import {ThumbnailView} from 'firefly/visualize/ui/ThumbnailView.jsx';
import {MagnifiedView} from 'firefly/visualize/ui/MagnifiedView.jsx';
import {getActivePlotView, getPlotViewById, primePlot} from 'firefly/visualize/PlotViewUtil.js';
import {get} from 'lodash';
import {getFluxInfo, getNonFluxDisplayElements} from 'firefly/visualize/ui/MouseReadoutUIUtil.js';
import {DataReadoutItem, MouseReadoutLock} from 'firefly/visualize/ui/MouseReadout.jsx';
import {isImage} from 'firefly/visualize/WebPlot.js';

export const MOUSE_READOUT_DIALOG_ID= 'MouseReadoutPopoutAll';

export function showMouseReadoutPopout(closeCallback) {
    const popup = (
        <PopupPanel title={primePlot(visRoot())?.title || 'Mouse Readout'} closeCallback={closeCallback} layoutPosition={LayoutType.TOP_RIGHT}>
            <PopoutMouseReadoutContents/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(MOUSE_READOUT_DIALOG_ID, popup);
    dispatchShowDialog(MOUSE_READOUT_DIALOG_ID);
}



function makeState() {
    return {vr:visRoot(), currMouseState:lastMouseCtx(), readoutData:lastMouseImageReadout(), readout:readoutRoot()};
}


function PopoutMouseReadoutContents() {
    const {vr,currMouseState, readout, readoutData}= useMouseStoreConnector(makeState);
    const showHealpixPixel= getAppOptions()?.hips?.readoutShowsPixel ?? false;
    const pv= getActivePlotView(vr);
    const mousePv= getPlotViewById(vr,currMouseState.plotId);
    const pvForThumbnail= currMouseState.mouseState===MouseState.EXIT ? pv : mousePv
    const image= isImage(primePlot(pvForThumbnail));
    const lockByClick= isLockByClick(readoutRoot());

    return (
        <div>
            {image &&
            <div style={{padding: '5px 5px 2px 5px', display: 'flex', flexDirection: 'row', alignItems: 'center'}}>
                <ThumbnailView plotView={pvForThumbnail} loadedSize={140}/>
                <MagnifiedView plotView={lockByClick ? pv : mousePv} size={140} mouseState={currMouseState} lockByClick={lockByClick} />
            </div>}
            <Readout readout={readout} readoutData={readoutData} showHealpixPixel={showHealpixPixel}/>
        </div>

    );
}



const rS = { width: 244, position: 'relative', padding: 5};
const ls= {color:'rgb(90,90,90)'};


function Readout({readout, readoutData, showHealpixPixel=false}){
    const {threeColor, readoutType}= readoutData;
    const isHiPS= readoutType===HIPS_STANDARD_READOUT;

    if (!get(readoutData,'readoutItems')) return <div style={rS}/>;
    const displayEle= getNonFluxDisplayElements(readoutData.readoutItems,  readout.readoutPref, isHiPS);
    const {pixelSize, showPixelPrefChange, healpixPixelReadout, healpixNorderReadout}= displayEle;
    const fluxArray = getFluxInfo(readoutData);
    const hipsPixel= showHealpixPixel && isHiPS;
    const showCopy= readout.lockByClick;
    const gridClasses= `mouseReadoutPopupInlineFullGrid${threeColor?' mouseReadoutPopupInlineFullGrid-3c':''}` ;
    const {readout1, readout2, showReadout1PrefChange, showReadout2PrefChange}= displayEle;

    return (
        <div className={gridClasses} style={rS}>
            <DataReadoutItem lArea='pixReadoutTopLabel' vArea='pixReadoutTopValue' cArea='clipboardIconTop'
                             label={readout1.label} value={readout1.value}
                             copyValue={readout1.copyValue} showCopy={showCopy}
                             labelStyle={ls}
                             prefChangeFunc={showReadout1PrefChange}/>
            <DataReadoutItem lArea='pixReadoutBottomLabel' vArea='pixReadoutBottomValue' cArea='clipboardIconBottom'
                             label={readout2.label} value={readout2.value}
                             copyValue={readout2.copyValue} showCopy={showCopy}
                             labelStyle={ls}
                             prefChangeFunc={showReadout2PrefChange}/>
            <DataReadoutItem lArea='pixSizeLabel' vArea='pixSizeValue'
                             labelStyle={ls}
                             label={pixelSize.label} value={pixelSize.value} prefChangeFunc={showPixelPrefChange}/>
            {hipsPixel && <DataReadoutItem labelStyle={{gridArea:'redLabel',...ls}} valueStyle={{gridArea:'redValue'}}
                                           label={healpixPixelReadout.label} value={healpixPixelReadout.value}/> }
            {hipsPixel && <DataReadoutItem labelStyle={{gridArea:'greenLabel',...ls}} valueStyle={{gridArea:'greenValue'}}
                                           label={healpixNorderReadout.label} value={healpixNorderReadout.value}/> }
            {!isHiPS && <DataReadoutItem lArea='redLabel' vArea='redValue'
                                         labelStyle={ls}
                                         label={fluxArray[0].label} value={fluxArray[0].value}/>}
            {threeColor && <DataReadoutItem lArea='greenLabel' vArea='greenValue'
                                            labelStyle={ls}
                                            label={fluxArray[1].label} value={fluxArray[1].value}/> }
            {threeColor && <DataReadoutItem lArea='blueLabel' vArea='blueValue'
                                            labelStyle={ls}
                                            label={fluxArray[2].label} value={fluxArray[2].value}/> }
            <MouseReadoutLock gArea='lock' lockByClick={readout.lockByClick} />
        </div>
    );
}

