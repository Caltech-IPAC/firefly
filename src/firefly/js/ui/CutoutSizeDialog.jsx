/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isString} from 'lodash';
import React, {useEffect} from 'react';
import {Chip, Stack, Typography} from '@mui/joy';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import {dispatchChangePointSelection, visRoot} from '../visualize/ImagePlotCntlr';
import {PlotAttribute} from '../visualize/PlotAttribute';
import {getActivePlotView, primePlot} from '../visualize/PlotViewUtil';
import {FieldGroupCollapsible} from './panel/CollapsiblePanel';
import { getCutoutSize, setAllCutoutParams, setPreferCutout } from './tap/ObsCoreOptions';
import {intValidator} from '../util/Validate';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {DEF_TARGET_PANEL_KEY, TargetPanel} from './TargetPanel';
import {ValidationField} from './ValidationField.jsx';
import {showYesNoPopup} from './PopupUtil.jsx';
import {useFieldGroupValue, useStoreConnector} from './SimpleComponent.jsx';
import {SizeInputFields} from './SizeInputField.jsx';

import HelpIcon from './HelpIcon.jsx';

const DIALOG_ID= 'cutoutSizeDialog';
const OVERRIDE_TARGET= 'OverrideTarget';



export function showCutoutSizeDialog({showingCutout, cutoutDefSizeDeg, pixelBasedCutout, tbl_id, cutoutCenterWP,
                                     dataProductsComponentKey, enableCutoutFullSwitching, cutoutToFullWarning}) {
    const popup = (
        <PopupPanel title={'Cutout Settings'} closeCallback={hideDialog} >
            <FieldGroup groupKey={'cutout-size-dialog'} keepState={true}>
                <CutoutSizePanel {...{showingCutout, cutoutDefSizeDeg,pixelBasedCutout, tbl_id, cutoutCenterWP,
                    dataProductsComponentKey,enableCutoutFullSwitching, cutoutToFullWarning}}/>
            </FieldGroup>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(DIALOG_ID, popup);
    showDialog();
}


function showDialog() {
    dispatchChangePointSelection('cutoutDialog', true);
    dispatchShowDialog(DIALOG_ID);
}

function hideDialog() {
    dispatchChangePointSelection('cutoutDialog', false);
    dispatchHideDialog(DIALOG_ID);
}


function CutoutSizePanel({showingCutout,cutoutDefSizeDeg,pixelBasedCutout,dataProductsComponentKey, tbl_id,
                             cutoutCenterWP, enableCutoutFullSwitching, cutoutToFullWarning}) {
    const cutoutFieldKey= dataProductsComponentKey+'-CutoutSize';
    let cutoutValue= useStoreConnector( () => getCutoutSize(dataProductsComponentKey));
    const [getCutoutFieldSize,setCutoutFieldSize]= useFieldGroupValue(cutoutFieldKey);
    const [getOverrideTarget,setOverrideTarget]= useFieldGroupValue(OVERRIDE_TARGET);
    const [getEnteredTarget,setEnteredTarget]= useFieldGroupValue(DEF_TARGET_PANEL_KEY);

    const activeWp= useStoreConnector(() => {
        const plot= primePlot(visRoot());
        if (!plot) return;
        const {pt}=plot.attributes[PlotAttribute.ACTIVE_POINT] ?? {};
        return pt;
    });

    useEffect(() => {
        if (activeWp) {
            setOverrideTarget(activeWp);
            setEnteredTarget(activeWp);
        }
    }, [activeWp]);



    if (isString(cutoutValue) && cutoutValue?.endsWith('px')) {
       cutoutValue= cutoutValue.substring(0,cutoutValue.length-2);
    }

    useEffect(() => {
        setCutoutFieldSize(cutoutValue);
    },[cutoutFieldKey]);

    return (
        <Stack justifyContent='space-between' alignItems='center' spacing={1} minWidth='25rem'>
            <Stack spacing={4} minWidth={'40rem'}>
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
                <OptionalTarget cutoutCenterWP={cutoutCenterWP} activePt/>
            </Stack>
            <Stack {...{textAlign:'center', direction:'row', justifyContent:'space-between', width:1, px:1, pb:1}}>
                <Stack {...{textAlign:'center', direction:'row', spacing:1}}>
                    <CompleteButton text={showingCutout?'Update Cutout':'Show Cutout'} dialogId={DIALOG_ID}
                                    onSuccess={(r) => updateCutout(r,cutoutFieldKey,dataProductsComponentKey,pixelBasedCutout,getOverrideTarget(),tbl_id)}/>
                    {showingCutout && enableCutoutFullSwitching &&
                        <CompleteButton text='Show Full Image'
                                        onSuccess={(r) =>
                                            showFullImage(r,cutoutFieldKey,dataProductsComponentKey,
                                                cutoutToFullWarning,tbl_id)}/>
                    }
                </Stack>
                <HelpIcon helpId={'visualization.rotate'} />
            </Stack>
        </Stack>
    );
}

function OptionalTarget({cutoutCenterWP}) {

    const [getEnteredTarget,setEnteredTarget]= useFieldGroupValue(DEF_TARGET_PANEL_KEY);
    const [getOverrideTarget,setOverrideTarget]= useFieldGroupValue(OVERRIDE_TARGET);
    const wpOverride= getOverrideTarget();

    useEffect(() => {
        const newWp= getEnteredTarget();
        if (newWp?.then) return; // ignore promises
        if (!newWp || newWp?.toString()===cutoutCenterWP?.toString()) {
            setOverrideTarget(undefined);
        }
        else {
            setOverrideTarget(newWp);
        }
    }, [getEnteredTarget]);

    const header= (
        <Stack direction='row' justifyContent='space-between' alignItems='baseline' width={1}>
            <Stack direction='row' spacing={1}>
                <Typography level='title-md'>
                    Change Cutout Center:
                </Typography>
                <Typography level='body-md'>
                    Enter new point or click on image
                </Typography>
            </Stack>
            <Typography level='body-sm'>
                {`(${wpOverride?'Using Custom':'Using Default'})`}
            </Typography>
        </Stack>

    );

    return (
        <FieldGroupCollapsible header={header}
                               initialState= {{ value:'closed' }}
                               fieldKey='optionalTarget'>
            <Stack spacing={2}>
                <TargetPanel
                    defaultToActiveTarget={false}
                    initialState={{
                        value: wpOverride ?? cutoutCenterWP
                    }}
                />
                <Chip onClick={() => {
                    setOverrideTarget(undefined);
                    setEnteredTarget(cutoutCenterWP);
                }}>
                    reset to default
                </Chip>
                <Typography color='warning'  level='body-sm'>
                    Warning: Changing the center could cause search to fail if new center is off of the tile
                </Typography>
            </Stack>
        </FieldGroupCollapsible>
    );
};


function updateCutout(request,cutoutFieldKey,dataProductsComponentKey,pixelBasedCutout,overrideTarget,tbl_id) {
    hideDialog();
    const size= `${request[cutoutFieldKey]}${pixelBasedCutout?'px':''}`;
    setAllCutoutParams( dataProductsComponentKey, tbl_id, size, overrideTarget, true);

}

function showFullImage(request,cutoutFieldKey,dataProductsComponentKey,cutoutToFullWarning,tbl_id) {
    if (cutoutToFullWarning) {
        showYesNoPopup(
            <Stack minWidth='30em' spacing={2} m={1}>
                <Typography>{cutoutToFullWarning}</Typography>
                <Typography>Show full image?</Typography>
            </Stack>,
            (id, yes) => {
                if (yes) {
                    hideDialog();
                    setPreferCutout(dataProductsComponentKey,tbl_id,false);
                }
                hideDialog();
            },
            'Very large image',
            {maxWidth:'50rem'}
        );
    }
    else {
        hideDialog();
        setPreferCutout(dataProductsComponentKey,tbl_id,false);
    }

}
