/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Typography} from '@mui/joy';
import React from 'react';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import Validate from '../../util/Validate.js';
import {dispatchPlotMask, visRoot} from '../ImagePlotCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {getActivePlotView} from '../PlotViewUtil.js';



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
        <FieldGroup style={{padding: '10px 5px 3px 5px'}} groupKey={'maskChoose'} keepState={true} >
            <FileUpload
                sx={{my: 1/2}}
                fieldKey='maskFile'
                initialState={{
                        tooltip: 'Select a file upload',
                        label: 'Upload File:'}}
            />

            <Typography sx={{pt:1, pr:1, pb:1, pl:4, width:315}}>
                If file is left blank then use an extension of the active plot file.
            </Typography>

            <ValidationField fieldKey='maskIdx'
                             initialState= {{
                                          fieldKey: 'maskIdx',
                                          value: '1',
                                          validator: Validate.floatRange.bind(null, 1, 1000, 'Mask number'),
                                          tooltip: 'mask number',
                                          label : 'Mask number:',
                                      }} />
            <ValidationField fieldKey='hduIdx'
                             initialState= {{
                                          fieldKey: 'hduIdx',
                                          value: '1',
                                          validator: Validate.floatRange.bind(null, 1, 1000, 'HDU index'),
                                          tooltip: 'mask index',
                                          label : 'HDU Index:',
                                      }} />

            <CompleteButton style={{paddingTop: 10}}
                            onSuccess={(r) => resultsSuccess(r,visRoot())}
                            dialogId='MaskChooseDialog'
            />

        </FieldGroup>

        );



}

const maskIdRoot= 'USER_LOADED_MASK_';
var maskCnt= 0;

function resultsSuccess(request,vr) {

    const pv= getActivePlotView(vr);
    const maskV= Number(request.maskIdx);
    const hdu= Number(request.hduIdx);
    const fileKey= request.maskFile;
    dispatchPlotMask({plotId:pv.plotId,imageOverlayId:maskIdRoot+maskCnt, fileKey,
                      maskNumber:maskV, maskValue:Math.pow(2,maskV),
                      uiCanAugmentTitle:true, imageNumber:hdu, title:'bit # '+maskV});
    maskCnt++;
}


