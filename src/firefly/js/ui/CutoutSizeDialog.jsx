/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import ContentCutRoundedIcon from '@mui/icons-material/ContentCutRounded';
import {isString} from 'lodash';
import {bool, string, object} from 'prop-types';
import React, {useEffect} from 'react';
import {Box, Chip, Stack, Typography} from '@mui/joy';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import {MetaConst} from '../data/MetaConst';
import {FixedPtControl} from '../drawingLayers/FixedPtControl';
import {getMetaEntry} from '../tables/TableUtil';
import {dispatchChangePointSelection, visRoot} from '../visualize/ImagePlotCntlr';
import {PlotAttribute} from '../visualize/PlotAttribute';
import {getActivePlotView, primePlot} from '../visualize/PlotViewUtil';
import {isValidPoint, parseWorldPt} from '../visualize/Point';
import {formatWorldPt, formatWorldPtToString} from '../visualize/ui/WorldPtFormat';
import {makeFoVString} from '../visualize/ZoomUtil';
import {getSearchTargetFromTable, isRowTargetCapable} from '../voAnalyzer/TableAnalysis';
import {FieldGroupCollapsible} from './panel/CollapsiblePanel';
import {RadioGroupInputField} from './RadioGroupInputField';
import {
    findCutoutTarget, getCutoutErrorStr,
    getCutoutSize, getCutoutTargetType, ROW_POSITION, SEARCH_POSITION, setAllCutoutParams, setPreferCutout,
    USER_ENTERED_POSITION
} from './tap/Cutout';
import {intValidator} from '../util/Validate';
import CompleteButton from './CompleteButton.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {DEF_TARGET_PANEL_KEY, TargetPanel} from './TargetPanel';
import {ToolbarButton} from './ToolbarButton';
import {ValidationField} from './ValidationField.jsx';
import {showYesNoPopup} from './PopupUtil.jsx';
import {useFieldGroupValue, useStoreConnector} from './SimpleComponent.jsx';
import {SizeInputFields} from './SizeInputField.jsx';

import HelpIcon from './HelpIcon.jsx';
import ReadMoreRoundedIcon from '@mui/icons-material/ReadMoreRounded';
import CrisisAlertRoundedIcon from '@mui/icons-material/CrisisAlertRounded';
import AccessibilityRoundedIcon from '@mui/icons-material/AccessibilityRounded';

const DIALOG_ID= 'cutoutSizeDialog';
const OVERRIDE_TARGET= 'OverrideTarget';
export const SHOWING_FULL='SHOWING_FULL';
export const SHOWING_CUTOUT='SHOWING_CUTOUT';


export function CutoutButton({dataProductsComponentKey,activeTable, serDef,pixelBasedCutout=false,
                          enableCutoutFullSwitching, cutoutToFullWarning=undefined, cutoutMode=undefined, overrideCutoutWpt}) {
    const cutoutValue= useStoreConnector( () => getCutoutSize(dataProductsComponentKey));
    const {positionWP:cutoutCenterWP, requestedType,foundType}= findCutoutTarget(dataProductsComponentKey,serDef,activeTable,activeTable?.highlightedRow);
    const cutoutTypeError= !overrideCutoutWpt && requestedType!==foundType;
    const cSize= dataProductsComponentKey ? pixelBasedCutout ? cutoutValue+'' : makeFoVString(Number(cutoutValue)) : '';

    if (!dataProductsComponentKey || !activeTable) return;

    if (cutoutMode===SHOWING_FULL) {
        return (
            <ToolbarButton
                icon={<ContentCutRoundedIcon/>} text={'Off'}
                title='Showing original image (with all extensions), click here to show a cutout'
                onClick={() =>
                    showCutoutSizeDialog({showingCutout:false,cutoutDefSizeDeg:cutoutValue,pixelBasedCutout,
                        tbl_id:activeTable?.tbl_id, cutoutCenterWP, serDef, overrideCutoutWpt,
                        dataProductsComponentKey,enableCutoutFullSwitching})}/>
        );
    }
    else if (cutoutMode===SHOWING_CUTOUT) {
        return (
            <ToolbarButton
                icon={
                    <CutoutIcon
                        showTypeError={cutoutTypeError}
                        type={isValidPoint(overrideCutoutWpt)
                            ? SEARCH_POSITION
                            : getCutoutTargetType(dataProductsComponentKey,activeTable?.tbl_id, serDef)}
                    />}
                text={`${cSize}`}
                title={
                    <Stack>
                        <Typography>
                            Showing cutout of image, click here to modify cutout or show original image (with all extensions)
                        </Typography>
                        {
                            cutoutTypeError &&
                            <Typography color='warning'>
                                {getCutoutErrorStr(foundType,requestedType)}
                            </Typography>

                        }
                    </Stack>
                }
                onClick={() =>
                    showCutoutSizeDialog({
                        showingCutout:true,cutoutDefSizeDeg:cutoutValue,pixelBasedCutout,
                        tbl_id:activeTable?.tbl_id, cutoutCenterWP, serDef, overrideCutoutWpt,
                        dataProductsComponentKey,enableCutoutFullSwitching, cutoutToFullWarning})}
            />
        );
    }
}

CutoutButton.propTypes= {
    dataProductsComponentKey: string,
    activeTable: object,
    serDef: object,
    pixelBasedCutout: bool,
    enableCutoutFullSwitching: bool,
    cutoutToFullWarning: string,
    cutoutMode: string,
    overrideCutoutWpt: object,
};



const CutoutIcon= ({type, showTypeError}) => {
    if (!type) return <ContentCutRoundedIcon/>;

    let typeIcon;
    const sx={position:'absolute', transform: 'scale(.9)', top:3, left:15};
    switch (type) {
        case USER_ENTERED_POSITION:
            typeIcon= <AccessibilityRoundedIcon {...{color: showTypeError?'danger':undefined ,sx:{...sx}}}/>;
            break;
        case SEARCH_POSITION:
            typeIcon= <CrisisAlertRoundedIcon {...{color: showTypeError?'danger':undefined ,sx:{...sx, transform:'scale(.8)', top:sx.top+1}}}/>;
            break;
        case ROW_POSITION:
            typeIcon= <ReadMoreRoundedIcon {...{color: showTypeError?'danger':undefined ,sx:{...sx,transform:'scale(1.1)', left:sx.left-2}}}/>;
            break;
    }
    const icon= (
        <Box sx={{width:24,height:24, position:'relative'}}>
            <ContentCutRoundedIcon sx={{position:'absolute', top:4, left:-1}}/>
            {typeIcon}
        </Box>
    );
    return icon;
};






export function showCutoutSizeDialog({showingCutout, cutoutDefSizeDeg, pixelBasedCutout, tbl_id, serDef,
                                         cutoutCenterWP, overrideCutoutWpt,
                                     dataProductsComponentKey, enableCutoutFullSwitching, cutoutToFullWarning}) {
    const popup = (
        <PopupPanel title={'Cutout Settings'} closeCallback={hideDialog} >
            <FieldGroup groupKey={'cutout-size-dialog'} keepState={true}>
                <CutoutSizePanel {...{showingCutout, cutoutDefSizeDeg,pixelBasedCutout, tbl_id, serDef,
                    cutoutCenterWP,overrideCutoutWpt,
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


function CutoutSizePanel({showingCutout,cutoutDefSizeDeg,pixelBasedCutout,dataProductsComponentKey, tbl_id, serDef,
                             cutoutCenterWP, overrideCutoutWpt,
                             enableCutoutFullSwitching, cutoutToFullWarning}) {
    const cutoutFieldKey= dataProductsComponentKey+'-CutoutSize';
    const cutoutTypeFieldKey= dataProductsComponentKey+'-CutoutType';
    let cutoutValue= useStoreConnector( () => getCutoutSize(dataProductsComponentKey));
    const [getCutoutFieldSize,setCutoutFieldSize]= useFieldGroupValue(cutoutFieldKey);
    const [getOverrideTarget,setOverrideTarget]= useFieldGroupValue(OVERRIDE_TARGET);
    const [getEnteredTarget,setEnteredTarget]= useFieldGroupValue(DEF_TARGET_PANEL_KEY);
    const [getCutoutType,setCutoutType]= useFieldGroupValue(cutoutTypeFieldKey);
    // const wpOverride= getOverrideTarget();
    const cutoutType= getCutoutType();

    const activeWp= useStoreConnector(() => {
        if (overrideCutoutWpt) return overrideCutoutWpt;
        const plot= primePlot(visRoot());
        if (!plot) return;
        const {pt}=plot.attributes[PlotAttribute.ACTIVE_POINT] ?? {};
        return pt;
    });

    const hasSearchTarget= Boolean(getSearchTargetFromTable(tbl_id));
    const rowCapable= isRowTargetCapable(tbl_id) || Boolean(parseWorldPt(getMetaEntry(tbl_id,MetaConst.ROW_TARGET)));

    useEffect(() => {
        if (overrideCutoutWpt) return;
        setCutoutType(getModifiedCutoutTargetType(dataProductsComponentKey,tbl_id, serDef,hasSearchTarget,rowCapable));
    }, [hasSearchTarget,rowCapable,overrideCutoutWpt]);

    useEffect(() => {
        if (activeWp) {
            setOverrideTarget(activeWp);
            setEnteredTarget(activeWp);
            setCutoutType(USER_ENTERED_POSITION);
        }
    }, [activeWp]);



    if (isString(cutoutValue) && cutoutValue?.endsWith('px')) {
       cutoutValue= cutoutValue.substring(0,cutoutValue.length-2);
    }

    useEffect(() => {
        setCutoutFieldSize(cutoutValue);
    },[cutoutFieldKey]);

    const options=[
        {label: (
                <Stack direction='row' spacing={1}>
                    <AccessibilityRoundedIcon/>
                    <Typography>Entered Position</Typography>
                </Stack>
            ),
            value:USER_ENTERED_POSITION},
    ];
    if (rowCapable) {
        options.unshift(
            {label: (
                    <Stack direction='row' spacing={1}>
                        <ReadMoreRoundedIcon/>
                        <Typography>Table Row Position</Typography>
                    </Stack>
                ),
                value:ROW_POSITION}
        );
    }
    if (hasSearchTarget) {
        options.unshift(
            {
                label: (
                    <Stack direction='row' spacing={1}>
                        <CrisisAlertRoundedIcon/>
                        <Typography>Search Target Center</Typography>
                    </Stack>
                ),
                value:SEARCH_POSITION,
            });
    }
    const cutoutInfoStr= showingCutout
            ? `Current cutout size: ${makeFoVString(Number(cutoutValue))}`
            : 'Now displaying full image. Cutouts are turned off.';

    return (
        <Stack justifyContent='space-between' alignItems='center' spacing={1} minWidth='25rem' ml={1}>
            <Stack spacing={4} minWidth={'40rem'}>
                <Stack spacing={1}>
                    <Typography level='title-md'>{cutoutInfoStr}</Typography>
                    {overrideCutoutWpt && <OverrideTargetFeedback {...{overrideCutoutWpt,showingCutout}}/>  }
                </Stack>
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
                                         unit: 'arcsec', min:  1/3600, max:  5 }} />
                }
                {!overrideCutoutWpt &&
                    <StandardCutoutControls {...{cutoutTypeFieldKey, cutoutCenterWP, cutoutType,options,
                        dataProductsComponentKey, tbl_id, serDef}} />
                }
            </Stack>

            <Stack {...{textAlign:'center', direction:'row', justifyContent:'space-between', width:1, pb:1, pt:1}}>
                <Stack {...{textAlign:'center', direction:'row', spacing:1}}>
                    <CompleteButton text={showingCutout?'Update Cutout':'Show Cutout'} dialogId={DIALOG_ID}
                                    onSuccess={(r) => updateCutout(r,cutoutFieldKey,dataProductsComponentKey,
                                        cutoutType,pixelBasedCutout,getOverrideTarget(),tbl_id,serDef, overrideCutoutWpt)}/>
                    {showingCutout && enableCutoutFullSwitching &&
                        <CompleteButton text='Show Full Image'
                                        onSuccess={(r) =>
                                            showFullImage(r,cutoutFieldKey,dataProductsComponentKey,
                                                cutoutToFullWarning,tbl_id)}/>
                    }
                </Stack>
                <HelpIcon helpId={'visualization.cutout'} />
            </Stack>
        </Stack>
    );
}

function OptionalTarget({cutoutCenterWP,sx}) {

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
        <Stack {...{direction:'row', justifyContent:'space-between', alignItems:'baseline', width:1}}>
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
        <FieldGroupCollapsible {...{header,
                               initialState: { value:'closed' },
                               fieldKey:'optionalTarget', sx }}>
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
}

const StandardCutoutControls= ({cutoutTypeFieldKey, cutoutCenterWP, cutoutType,options,
                                   dataProductsComponentKey, tbl_id, serDef}) => (
    <>
        <RadioGroupInputField {...{
            fieldKey:cutoutTypeFieldKey, orientation: 'horizontal', options,
            initialState:{ value: getCutoutTargetType(dataProductsComponentKey,tbl_id, serDef) }
        }}/>
        {cutoutType===SEARCH_POSITION &&
            <Typography color='warning'  level='body-sm'>
                Warning: If search position is not on the source image then the cutout will use row position
            </Typography>
        }
        {cutoutType!==USER_ENTERED_POSITION &&
            <Typography level='body-sm'>
                You may also click on the image to change position
            </Typography>
        }
        <OptionalTarget cutoutCenterWP={cutoutCenterWP}
                        sx={{visibility: cutoutType===USER_ENTERED_POSITION?'visible':'hidden'}} />
    </>
);

const OverrideTargetFeedback= ({overrideCutoutWpt, showingCutout}) => (
    <Stack {...{direction:'row', alignItems:'center', spacing:1}} title={formatWorldPtToString(overrideCutoutWpt)}>
        <Stack {...{direction:'row', alignItems:'baseline', spacing:1}}>
            <Typography>{`${showingCutout?'Cutouts':'Full images'} include position:`}</Typography>
            {formatWorldPt(overrideCutoutWpt,5,false)}
        </Stack>
        {<FixedPtControl wp={overrideCutoutWpt} pv={getActivePlotView(visRoot())}/>}
    </Stack>
);


function updateCutout(request,cutoutFieldKey,dataProductsComponentKey,cutoutType,
                      pixelBasedCutout,overrideTarget,tbl_id,serDef,overrideCutoutWpt) {
    hideDialog();
    const size= `${request[cutoutFieldKey]}${pixelBasedCutout?'px':''}`;
    if (overrideCutoutWpt) {
        setAllCutoutParams( dataProductsComponentKey, tbl_id, serDef, SEARCH_POSITION, size, overrideCutoutWpt);
    }
    else {
        setAllCutoutParams( dataProductsComponentKey, tbl_id, serDef, cutoutType, size, overrideTarget);
    }
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
                dispatchHideDialog(id);
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

function getModifiedCutoutTargetType(dataProductsComponentKey,tbl_id, setDef, hasSearchTarget, rowCapable) {
    const cType= getCutoutTargetType(dataProductsComponentKey,tbl_id, setDef);
    switch (cType) {
        case ROW_POSITION:
            if (rowCapable) return ROW_POSITION;
            if (hasSearchTarget) return SEARCH_POSITION;
            return USER_ENTERED_POSITION;
        case SEARCH_POSITION:
            if (hasSearchTarget) return SEARCH_POSITION;
            if (rowCapable) return ROW_POSITION;
            return USER_ENTERED_POSITION;
        case USER_ENTERED_POSITION:
            return cType;
    }
    return cType;
}