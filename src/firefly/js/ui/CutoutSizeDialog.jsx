/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {useEffect} from 'react';
import {
    dispatchShowDialog, dispatchHideDialog, getComponentState, dispatchComponentStateChange
} from '../core/ComponentCntlr.js';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {FieldGroup} from './FieldGroup';
import {PopupPanel} from './PopupPanel.jsx';
import {useFieldGroupValue, useStoreConnector} from './SimpleComponent';
import {SizeInputFields} from './SizeInputField';

import HelpIcon from './HelpIcon.jsx';
import {Stack} from '@mui/joy';

const DIALOG_ID= 'cutoutSizeDialog';


export function showCutoutSizeDialog(cutoutParam,dataProductsComponentKey) {
    const popup = (
        <PopupPanel title={'Set Cutout Size'}>
            <FieldGroup groupKey={'cutout-size-dialog'} keepState={true}>
                <CutoutSizePanelPanel {...{cutoutParam,dataProductsComponentKey}}/>
            </FieldGroup>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(DIALOG_ID, popup);
    dispatchShowDialog(DIALOG_ID);
}




function CutoutSizePanelPanel({cutoutParam,dataProductsComponentKey}) {

    const [cutoutKey,defValue]= Object.entries(cutoutParam)[0];
    const cutoutValue= useStoreConnector( () => getComponentState(dataProductsComponentKey)[cutoutKey] ) ?? defValue;
    const [getCutoutSize,setCutoutSize]= useFieldGroupValue(cutoutKey);

    useEffect(() => {
        setCutoutSize(cutoutValue);
    },[cutoutKey]);

    return (
        <Stack justifyContent='space-between' alignItems='center' spacing={1}>
            <SizeInputFields fieldKey={cutoutKey} showFeedback={true} label='Cutout Size'
                             initialState={{
                                 value: (defValue/3600).toString(),
                                 tooltip: 'Set cutout size',
                                 unit: 'arcsec', min:  1/3600, max:  5
                             }} />
            <Stack {...{textAlign:'center', direction:'row', justifyContent:'space-between', width:1, px:1, pb:1}}>
                <CompleteButton text='Update Cutout' dialogId={DIALOG_ID}
                                onSuccess={(r) => updateCutout(r,cutoutKey,dataProductsComponentKey)}/>
                <HelpIcon helpId={'visualization.rotate'} />
            </Stack>
        </Stack>
    );
}


function updateCutout(request,cutoutKey,dataProductsComponentKey) {
    console.log(request);
    dispatchComponentStateChange(dataProductsComponentKey,{[cutoutKey]:request[cutoutKey]});
}