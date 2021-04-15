/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {useEffect} from 'react';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import Validate from '../util/Validate.js';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {getActivePlotView, primePlot} from '../visualize/PlotViewUtil.js';
import {visRoot, dispatchRotate} from '../visualize/ImagePlotCntlr.js';
import {SimpleLayerOnOffButton} from '../visualize/ui/SimpleLayerOnOffButton';
import {RotateType} from '../visualize/PlotState.js';
import {StateInputField} from './StatedInputfield';
import {useStoreConnector} from './SimpleComponent';
import {isEastLeftOfNorth} from '../visualize/VisUtil';
import {RangeSliderView} from './RangeSliderView';

import HelpIcon from './HelpIcon.jsx';
import ROTATE_NORTH_OFF from 'html/images/icons-2014/RotateToNorth.png';
import ROTATE_NORTH_ON from 'html/images/icons-2014/RotateToNorth-ON.png';
import {hasWCSProjection} from '../visualize/PlotViewUtil';
import {isImage} from '../visualize/WebPlot';

const DIALOG_ID= 'fitsRotationDialog';

export function showFitsRotationDialog() {
    const popup = (
        <PopupPanel title={'Rotate Image'}>
            <FitsRotationImmediatePanel/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(DIALOG_ID, popup);
    dispatchShowDialog(DIALOG_ID);
}

function getCurrentRotation(pv) {
    if (!pv || !pv.rotation || pv.rotation>359 || pv.rotation<.5) return 0;
    const angle=  isEastLeftOfNorth(primePlot(pv)) ? 360-pv.rotation : pv.rotation;
    return Math.round(angle);
}

const marks = { 0: '0', 45:'45', 90:'90', 135: '135', 180:'180', 225: '225', 270:'270', 315:'315', 359:'359' };

function FitsRotationImmediatePanel() {
    const [pv] = useStoreConnector(() => getActivePlotView(visRoot()));

    useEffect(() => {
        (!pv || !isImage(primePlot(pv))) && dispatchHideDialog(DIALOG_ID);
    }, [pv]);

    const plot= primePlot(pv);
    const currRotation= getCurrentRotation(pv);
    const hasWcs= hasWCSProjection(pv);

    const validator= (value) => Validate.intRange(0, 360, 'angle', value, true);

    const changeRotation= (rotation) => {
        const angle= Number(rotation);
        if (!validator(angle).valid) return;
        const rotateType= angle?RotateType.ANGLE:RotateType.UNROTATE;
                           // note on: (currRotation-angle)===6 ?  currRotation : angle
                           // work around for a bug here with rc-slider, in certain cases on mouse up it will give a value 6 less
                           // if the slider width is changed this adjustment will have to be different
        dispatchRotate({plotId:plot.plotId, rotateType, angle: (currRotation-angle)===6 ?  currRotation : angle} );
    };

    const doRotateNorth= (rNorth) =>
                     dispatchRotate({plotId:pv.plotId, rotateType: rNorth?RotateType.NORTH:RotateType.UNROTATE});



    const handleKeyDown= (ev) => {
        if (ev.key !== 'ArrowLeft' && ev.key !== 'ArrowRight' && ev.key !== 'ArrowUp' && ev.key !== 'ArrowDown') return;
        const dir= (ev.key==='ArrowRight' || ev.key==='ArrowUp') ? 1 : -1;
        const newRot= (Math.trunc(currRotation)+dir+360) % 360;
        changeRotation(newRot);
    };

    if (!pv) return (<div style={{whiteSpace:'nowrap', padding:'10px 35px'}}>No Image Loaded</div>);

    return (
        <div className={'disable-select'}>
            <div style={{padding: '25px 20px 10px 20px'}}>
                <div style={{display:'flex', flexDirection: 'column', alignItems:'center',
                                      justifyContent:'space-between', padding: '0 3px'}}>
                    <div style={{display:'flex', alignItems:'center', justifyContent:'space-between'}}>
                        <StateInputField defaultValue={currRotation+''} valueChange={(v) => changeRotation(v.value)}
                                         labelWidth={35} label={'Angle: '}
                                         tooltip={'Enter the angle between 0 and 359 degrees'}
                                         message={'Angle must be a number between 0 and 359'}
                                         showWarning={true}
                                         validator={validator} onKeyDown={handleKeyDown} />

                        <SimpleLayerOnOffButton plotView={pv}
                                                style={{border: '1px solid rgba(0,0,0,.2)'}}
                                                isIconOn={pv&&plot ? pv.plotViewCtx.rotateNorthLock : false }
                                                tip='Rotate this image so that North is up'
                                                enabled={hasWcs}
                                                visible={true}
                                                iconOn={ROTATE_NORTH_ON}
                                                iconOff={ROTATE_NORTH_OFF}
                                                onClick={(pv,rNorth)=> doRotateNorth(rNorth)} />
                    </div>

                    <RangeSliderView {...{
                        wrapperStyle:{paddingTop: 15, width: 225},
                        min:0,max:359, step:1,vertical:false, marks,
                        defaultValue:currRotation, slideValue:currRotation,
                        handleChange:(v) => changeRotation(v)}} />
                </div>
                <div style={{paddingTop:40, textAlign:'center'}}>
                    {hasWcs?'Angle in degrees East of North' : 'Angle in degrees counter clockwise'}
                </div>
            </div>
            <div style={{textAlign:'center', display:'flex', justifyContent:'space-between', padding: '0 16px'}}>
                <CompleteButton text='Close' dialogId={DIALOG_ID} />
                <div style={{ textAlign:'center', marginBottom: 20}}>
                    <HelpIcon helpId={'visualization.rotate'} />
                </div>
            </div>
        </div>
    );
}
