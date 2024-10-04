/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isString} from 'lodash';
import React, {useEffect} from 'react';
import {
    dispatchShowDialog, dispatchHideDialog, getComponentState, dispatchComponentStateChange
} from '../core/ComponentCntlr.js';
import {SD_CUTOUT_KEY} from '../metaConvert/vo/ServDescProducts';
import {intValidator} from '../util/Validate';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {FieldGroup} from './FieldGroup';
import {PopupPanel} from './PopupPanel.jsx';
import {useFieldGroupValue, useStoreConnector} from './SimpleComponent';
import {SizeInputFields} from './SizeInputField';

import HelpIcon from './HelpIcon.jsx';
import {Stack} from '@mui/joy';
import {ValidationField} from './ValidationField';

const DIALOG_ID= 'cutoutSizeDialog';


export function showCutoutSizeDialog(cutoutDefSizeDeg, pixelBasedCutout, dataProductsComponentKey) {
    const popup = (
        <PopupPanel title={'Set Cutout Size'}>
            <FieldGroup groupKey={'cutout-size-dialog'} keepState={true}>
                <CutoutSizePanel {...{cutoutDefSizeDeg,pixelBasedCutout,dataProductsComponentKey}}/>
            </FieldGroup>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(DIALOG_ID, popup);
    dispatchShowDialog(DIALOG_ID);
}




function CutoutSizePanel({cutoutDefSizeDeg,pixelBasedCutout,dataProductsComponentKey}) {
    const cutoutFieldKey= dataProductsComponentKey+'-'+SD_CUTOUT_KEY;
    let cutoutValue= useStoreConnector( () => getComponentState(dataProductsComponentKey)[SD_CUTOUT_KEY] ?? cutoutDefSizeDeg);
    const [getCutoutSize,setCutoutSize]= useFieldGroupValue(cutoutFieldKey);

    if (isString(cutoutValue) && cutoutValue?.endsWith('px')) {
       cutoutValue= cutoutValue.substring(0,cutoutValue.length-2);
    }

    useEffect(() => {
        setCutoutSize(cutoutValue);
    },[cutoutFieldKey]);

    return (
        <Stack justifyContent='space-between' alignItems='center' spacing={1}>
            { pixelBasedCutout ?
                <ValidationField {...{
                    nullAllowed: false,
                    placeholder:  'enter pixel value',
                    label: 'Cutout Size in pixels',
                    fieldKey: cutoutFieldKey,
                    initialState: {
                        value: Number(cutoutValue),
                        validator: intValidator(0, undefined, 'cutout size in pixels')
                    },
                    tooltip: 'cutout size'
                }} />
                :
                <SizeInputFields fieldKey={cutoutFieldKey} showFeedback={true} label='Cutout Size'
                                 initialState={{
                                     value: (cutoutDefSizeDeg/3600).toString(),
                                     tooltip: 'Set cutout size',
                                     unit: 'arcsec', min:  1/3600, max:  5
                                 }} />

            }
            <Stack {...{textAlign:'center', direction:'row', justifyContent:'space-between', width:1, px:1, pb:1}}>
                <CompleteButton text='Update Cutout' dialogId={DIALOG_ID}
                                onSuccess={(r) => updateCutout(r,cutoutFieldKey,dataProductsComponentKey,pixelBasedCutout)}/>
                <HelpIcon helpId={'visualization.rotate'} />
            </Stack>
        </Stack>
    );
}


function updateCutout(request,cutoutFieldKey,dataProductsComponentKey,pixelBasedCutout) {
    dispatchComponentStateChange(dataProductsComponentKey,
        {
            [SD_CUTOUT_KEY]:`${request[cutoutFieldKey]}${pixelBasedCutout?'px':''}`
        });
}