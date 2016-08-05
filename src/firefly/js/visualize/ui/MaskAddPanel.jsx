/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import Validate from '../../util/Validate.js';
import {dispatchPlotMask, visRoot} from '../ImagePlotCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {RequestType} from '../RequestType.js';
import {primePlot, getActivePlotView} from '../PlotViewUtil.js';
import {RotateType} from '../PlotState.js';
import {ZoomType} from '../ZoomType.js';



export function showMaskDialog() {
    const content= (
        <PopupPanel title={'Mask'} >
            <MaskAddPanel visRoot={visRoot()} />
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('MaskChooseDialog', content);
    dispatchShowDialog('MaskChooseDialog');
}



export function MaskAddPanel({vr}) {


    return (
        <FieldGroup groupKey={'maskChoose'} keepState={true} >
            <ValidationField fieldKey='maskIdx'
                             forceReinit={true}
                             initialState= {{
                                          fieldKey: 'maskIdx',
                                          value: '1',
                                          validator: Validate.floatRange.bind(null, 1, 1000, 'Mask number'),
                                          tooltip: 'mask number',
                                          label : 'Mask number:',
                                          labelWidth : 100
                                      }} />
            <ValidationField fieldKey='hduIdx'
                             forceReinit={true}
                             initialState= {{
                                          fieldKey: 'hduIdx',
                                          value: '1',
                                          validator: Validate.floatRange.bind(null, 1, 1000, 'HDU index'),
                                          tooltip: 'mask index',
                                          label : 'HDU Index:',
                                          labelWidth : 100
                                      }} />

            <CompleteButton
                            onSuccess={(r) => resultsSuccess(r,visRoot())}
                            dialogId='MaskChooseDialog'
            />

        </FieldGroup>

        );



}

const maskIdRoot= 'MASK_';
var maskCnt= 0;

function resultsSuccess(request,vr) {

    console.log('make success');
    console.log(request);
    const pv= getActivePlotView(vr);
    const maskV= Number(request.maskIdx);
    const hdu= Number(request.hduIdx);
    dispatchPlotMask({plotId:pv.plotId,imageOverlayId:maskIdRoot+maskCnt, maskNumber:maskV, maskValue:Math.pow(2,maskV),
                      imageNumber:hdu, title:'bit # '+maskV});
    maskCnt++;
}


