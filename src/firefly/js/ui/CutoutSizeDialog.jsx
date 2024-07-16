/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {useEffect} from 'react';
import {
    dispatchShowDialog, dispatchHideDialog, getComponentState, dispatchComponentStateChange
} from '../core/ComponentCntlr.js';
import {SD_CUTOUT_KEY} from '../metaConvert/vo/ServDescProducts';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {FieldGroup} from './FieldGroup';
import {PopupPanel} from './PopupPanel.jsx';
import {useFieldGroupValue, useStoreConnector} from './SimpleComponent';
import {SizeInputFields} from './SizeInputField';

import HelpIcon from './HelpIcon.jsx';
import {Stack} from '@mui/joy';

const DIALOG_ID= 'cutoutSizeDialog';


export function showCutoutSizeDialog(cutoutDefSizeDeg, dataProductsComponentKey) {
    const popup = (
        <PopupPanel title={'Set Cutout Size'}>
            <FieldGroup groupKey={'cutout-size-dialog'} keepState={true}>
                <CutoutSizePanel {...{cutoutDefSizeDeg,dataProductsComponentKey}}/>
            </FieldGroup>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(DIALOG_ID, popup);
    dispatchShowDialog(DIALOG_ID);
}




function CutoutSizePanel({cutoutDefSizeDeg,dataProductsComponentKey}) {
    const cutoutFieldKey= dataProductsComponentKey+'-'+SD_CUTOUT_KEY;
    const cutoutValue= useStoreConnector( () => getComponentState(dataProductsComponentKey)[SD_CUTOUT_KEY] ?? cutoutDefSizeDeg);
    const [getCutoutSize,setCutoutSize]= useFieldGroupValue(cutoutFieldKey);


    useEffect(() => {
        setCutoutSize(cutoutValue);
    },[cutoutFieldKey]);

    return (
        <Stack justifyContent='space-between' alignItems='center' spacing={1}>
            <SizeInputFields fieldKey={cutoutFieldKey} showFeedback={true} label='Cutout Size'
                             initialState={{
                                 value: (cutoutDefSizeDeg/3600).toString(),
                                 tooltip: 'Set cutout size',
                                 unit: 'arcsec', min:  1/3600, max:  5
                             }} />
            <Stack {...{textAlign:'center', direction:'row', justifyContent:'space-between', width:1, px:1, pb:1}}>
                <CompleteButton text='Update Cutout' dialogId={DIALOG_ID}
                                onSuccess={(r) => updateCutout(r,cutoutFieldKey,dataProductsComponentKey)}/>
                <HelpIcon helpId={'visualization.rotate'} />
            </Stack>
        </Stack>
    );
}


function updateCutout(request,cutoutFieldKey,dataProductsComponentKey) {
    dispatchComponentStateChange(dataProductsComponentKey,{[SD_CUTOUT_KEY]:request[cutoutFieldKey]});
}