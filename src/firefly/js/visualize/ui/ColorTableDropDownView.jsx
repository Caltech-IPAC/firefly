/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState, useEffect, useCallback, useRef} from 'react';
import PropTypes from 'prop-types';
import {throttle, isArray, isNumber} from 'lodash';
import {hideColorPickerDialog, showColorPickerDialog} from '../../ui/ColorPicker';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {ToolbarButton, ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {getBestContrast, toRGBAString} from '../../util/Color';
import {dispatchColorChange, dispatchOverlayColorLocking} from '../ImagePlotCntlr.js';
import {
    primePlot, isThreeColor, getActivePlotView, isAllStretchDataLoaded, findPlotGroup,
    hasOverlayColorLock,
} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {
    findAContrastColor, getColorModel, baseIdMatchesForOrRev, isReversedColor, makeColorTableImage, NO_COLOR_TABLE,
    reverseId, getCbarNumIds, getCbarTip
} from '../rawData/rawAlgorithm/ColorTable';
import {isImage} from '../WebPlot.js';
import {Band} from '../Band.js';
import {LayoutType, PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {RangeSliderView} from '../../ui/RangeSliderView.jsx';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {dispatchHideDialog, dispatchShowDialog} from 'firefly/core/ComponentCntlr.js';
import {DROP_DOWN_KEY} from 'firefly/ui/DropDownToolbarButton.jsx';
import {Typography, Box, Stack, Divider, IconButton, Skeleton, Chip} from '@mui/joy';
import LockIcon from '@mui/icons-material/Lock';
import LockOpenOutlinedIcon from '@mui/icons-material/LockOpenOutlined';
import ArrowOutwardOutlinedIcon from '@mui/icons-material/ArrowOutwardOutlined';
import CheckIcon from '@mui/icons-material/Check';
import ReverseIcon from '@mui/icons-material/FlipCameraAndroidOutlined';

//=================================

const POPOUT_ID= 'AdvancedColorPopout';


const reversedDisabledAry=['0','1'];
const ignoreAry=['5'];
const colorTables= getCbarNumIds().map( (id) => {
    return {
        id,
        tip: getCbarTip(id),
        icon: makeColorTableImage(getColorModel(id), 80,8).toDataURL('image/png'),
        reversedIcon: makeColorTableImage(getColorModel(reverseId(id)), 80,8).toDataURL('image/png'),
        forwardIconShowing: true,
        ignore: ignoreAry.includes(id),
        disableReverse: reversedDisabledAry.includes(id),
    };
});

// const colorTables=[
//     { id: '0',  tip:'Gray Scale', disableReverse:true },
//     { id: '1',  tip: 'Reverse Gray Scale', disableReverse:true  },
//     { id: '2',  tip: 'Color Cube' },
//     { id: '3',  tip: 'Spectrum' },
//     { id: '4',  tip: 'For False Color' },
//     { id: '5',  tip: 'For False Color - Reversed', ignore:true },
//     { id: '6',  tip: 'For False Color - Compressed' },
//     { id: '7',  tip: 'For difference images' },
//     { id: '8',  tip: 'DS9\'s a color bar' },
//     { id: '9',  tip: 'DS9\'s b color bar' },
//     { id: '10', tip: 'DS9\'s bb color bar' },
//     { id: '11', tip: 'DS9\'s he color bar' },
//     { id: '12', tip: 'DS9\'s i8 color bar' },
//     { id: '13', tip: 'DS9\'s aips color bar' },
//     { id: '14', tip: 'DS9\'s sls color bar' },
//     { id: '15', tip: 'DS9\'s hsv color bar' },
//     { id: '16', tip: 'Heat (ds9)' },
//     { id: '17', tip: 'Cool (ds9)' },
//     { id: '18', tip: 'Rainbow (ds9)' },
//     { id: '19', tip: 'Standard (ds9)' },
//     { id: '20', tip: 'Staircase (ds9)' },
//     { id: '21', tip: 'Color (ds9)' },
//     { id: '22', tip: 'turbo' },
//     { id: '23', tip: 'cividis' },
//     { id: '24', tip: 'veridis' },
//     { id: '25', tip: 'plasma' },
//     { id: '26', tip: 'magma' }
// ].map( (ct) => ({
//     ...ct,
//     icon: makeColorTableImage(getColorModel(ct.id), 80,8).toDataURL('image/png'),
//     reversedIcon: makeColorTableImage(getColorModel(reverseId(ct.id)), 80,8).toDataURL('image/png'),
//     forwardIconShowing: true,
// }));

const hipsColorTables=[ { id: NO_COLOR_TABLE,  icon: undefined, disableReverse:true, tip:'Original' }, ...colorTables, ];

const isAllThreeColor= (vr,plotIdAry) => plotIdAry.every( (id) => isThreeColor(primePlot(vr,id)));


const makeMask= () => (
    <div style={maskWrapper}>
        <Skeleton/>
        <div style={{
            position: 'absolute',
            top: 132,
            left: 43,
            color: 'white',
            width: 150,
            fontSize: '12pt',
            textAlign: 'center',
            zIndex: 10
        }}>
            Loading Advanced Options
        </div>
    </div> );

const ctMarks = [
    { label: '0', value: 0 }, { label: '6', value: 6 }, { label: '12', value: 12 }, { label: '18', value: 18 },
    { label: '24', value: 24 }, { label: '30', value: 30 }, { label: '36', value: 36 }, { label: '42', value: 42 }
];
const biasMarks = [
    { label: '.2', value: 8 }, { label: '.3', value: 12 }, { label: '.4', value: 16 }, { label: '.5', value: 20 },
    { label: '.6', value: 24 }, { label: '.7', value: 28 }, { label: '.8', value: 32 }
];
const contrastMarks = [
    { label: '0', value: 0 }, { label: '5', value: 5 }, { label: '1', value: 10 }, { label: '1.5', value: 15 },
    { label: '2', value: 20 }
];
const maskWrapper= { position:'absolute', left:0, top:0, width:'100%', height:'100%' };


const dispatchColorChangeThrottled= throttle((param) => {
    dispatchColorChange(param);
}, 500);


function getBias(plot) {
    const bias= plot?.rawData?.bandData[0].bias;
    if (isThreeColor(plot)) {
        return isArray(bias) ? bias : [.5,.5,.5];
    }
    else {
        return isNumber(bias) ? bias : .5;
    }
}

function getContrast(plot) {
    const contrast= plot?.rawData?.bandData[0].contrast;
    if (isThreeColor(plot)) {
        return isArray(contrast) ? contrast : [1,1,1];
    }
    else {
        return isNumber(contrast) ? contrast : 1;
    }
}

export function ColorStretchLockButton({allowPopout=false, sx={}}) {
    const overlayColorLocked= useStoreConnector( () => {
        const vr= visRoot();
        const pv= getActivePlotView(vr);
        return pv ? hasOverlayColorLock(pv,findPlotGroup(pv?.plotGroupId,vr.plotGroupAry)) : false;
    } );
    const pv= getActivePlotView(visRoot());
    return (
        <ToolbarButton plotView={pv}
                       sx={allowPopout? {mt:-2.5, width:.85, ...sx} : sx}
                       hasCheckBox={true}
                       checkBoxOn={overlayColorLocked}
                       CheckboxOnIcon={<LockIcon sx={{pt: 1/4}}/>}
                       CheckboxOffIcon={<LockOpenOutlinedIcon sx={{pt: 1/4}}/>}
                       text={ overlayColorLocked? 'Color, stretch & overlays locked' : 'Color, stretch & overlays unlocked' }
                       tip='Lock all images for color changes and overlays.'
                       onClick={() => dispatchOverlayColorLocking(pv.plotId,!overlayColorLocked)} />
    );
}

function getCtArrayEntry(ctArray, colorTableId) {
    return ctArray.find( ({id}) => baseIdMatchesForOrRev(id, colorTableId));
}


function ColorItem({colorTableId,ct,bias,contrast,changeBiasContrastColor}) {
    const forwardMap= ct.forwardIconShowing;

    if (ct.ignore) return;
    const doReverse= !ct.disableReverse;
    const checkBoxOn= baseIdMatchesForOrRev(ct.id, colorTableId);
    const icon= ct.icon ? <img src={forwardMap?ct.icon:ct.reversedIcon} style={{height: 8, width: '16.5rem'}}/> : undefined;

    const reverseOnClick= () => {
        changeBiasContrastColor( forwardMap ? reverseId(ct.id) : ct.id, bias, contrast);
        ct.forwardIconShowing= !forwardMap;
    };

    const revSx= {transform: 'scale(.7)', borderRadius: 4, p:0, border:0};
    const onRevIcon= <ReverseIcon sx={revSx}/>;
    const offRevIcon= (
        <ReverseIcon sx={
            (theme) => ( {
                ...revSx,
                color: theme?.palette?.neutral.softBg,
                background: theme?.palette?.neutral.softColor,
            })
        }/>
    );


    return (
        <Stack direction='row' spacing={1} key={ct.id}>
            <ToolbarButton {...{
                icon,
                tip: ct.tip,
                text: ct.id===NO_COLOR_TABLE ? 'Default Color Map' : undefined,
                CheckboxOnIcon:<CheckIcon/>,
                horizontal: false,
                hasCheckBox:true,
                checkBoxOn,
                slotProps:{tbCheckBox: {sx:{height:20, width:22}}},
                onClick:() => changeBiasContrastColor(forwardMap ? ct.id : reverseId(ct.id), bias, contrast)
            }}/>
            {doReverse && <ToolbarButton {...{
                tip: `reverse ${ct.tip} `,
                hasCheckBox:true,
                text: '',
                CheckboxOnIcon:onRevIcon,
                CheckboxOffIcon:offRevIcon,
                checkBoxOn: forwardMap,
                slotProps: { tbCheckBox: {sx:{height:20}}},
                onClick: reverseOnClick }}/>
            }
        </Stack>
    );
}


function StandardFeatures({lastCbar,ctArray, plot, allColorTableIDs, changeBiasContrastColor,allLoaded, colorTableId,bias,contrast}) {
    const realBias= isArray(bias) ? plot.rawData.bandData[Band.NO_BAND.value].bias : bias;
    const biasInt= Math.trunc(realBias*40);
    const realContrast= isArray(contrast) ? plot.rawData.bandData[Band.NO_BAND.value].contrast : contrast;
    const contrastInt= Math.trunc(realContrast*10);
    const cSlideVal= allColorTableIDs.findIndex( (v) => v===colorTableId);
    const r= isReversedColor(colorTableId);
    const cBarTitle= (ctArray.find( (ct) => baseIdMatchesForOrRev(ct.id,colorTableId))?.tip ?? '') + (r ? ' - reversed' : '');
    return  (
        <Box width={230} height={180}>
            <Stack alignItems='center' spacing={0}>
                <Typography level='body-xs' sx={{pt:0.625, pb:0.25}}>{cBarTitle}</Typography>
                <RangeSliderView {...{
                    sx:{pb:0.5, mt:-1.5, width: 200},
                    min:0,max:allColorTableIDs.length-1, step:1,vertical:false, marks:ctMarks,
                    defaultValue:cSlideVal, slideValue:cSlideVal,
                    onChangeCommitted: () => lastCbar.prevCaryState= undefined,
                    handleChange:(v) => {
                        if (!lastCbar.prevCaryState) {
                            lastCbar.prevCaryState= ctArray.map( (ct) => ({...ct}));
                        }
                        ctArray.forEach( (ct,idx) => ct.forwardIconShowing=lastCbar.prevCaryState[idx].forwardIconShowing);
                        const idStr= allColorTableIDs[Math.trunc(v)];
                        const ct= getCtArrayEntry(ctArray,idStr);
                        ct.forwardIconShowing= !isReversedColor(idStr);
                        changeBiasContrastColor(idStr, bias,contrast);
                    } }}/>

            </Stack>
            {colorTableId!==NO_COLOR_TABLE ? <Stack alignItems='center' spacing={0}>
                <Typography level='body-xs' sx={{pt:0.625, pb:0.25}}>Bias</Typography>
                <RangeSliderView {...{
                    sx:{pb:0.5, mt:-1.5, width: 200},
                    min:8,max:32, step:1,vertical:false, marks:biasMarks,
                    defaultValue:biasInt, slideValue:biasInt,
                    handleChange:(v) => changeBiasContrastColor(colorTableId, v/40,contrast)}} />

            </Stack> : <div/>}
            {colorTableId!==NO_COLOR_TABLE ? <Stack alignItems='center'>
                <Typography level='body-xs' sx={{pt:0.625, pb:0.25}}>Contrast</Typography>
                <RangeSliderView {...{
                    sx:{pb:0.5, mt:-1.5, width: 200},
                    min:0,max:20, step:1,vertical:false, marks:contrastMarks,
                    defaultValue:contrastInt, slideValue:contrastInt,
                    handleChange:(v) => changeBiasContrastColor(colorTableId, bias,v/10)}} />
            </Stack> : <div/>}
            { !allLoaded && makeMask() }
        </Box>
    );
}

function ThreeCFeatures({allLoaded,useRed,useGreen,useBlue,plot, changeBiasContrastColor, bias, contrast, colorTableId}) {
    const {plotState}= plot;
    const bAry= isArray(bias) ? bias : plot.rawData.bandData.map( (bd) => bd.bias);
    const biasInt= bAry.map( (b) => Math.trunc(b*40));
    const cAry= isArray(contrast) ? contrast : plot.rawData.bandData.map( (bd) => bd.contrast);
    const contrastInt= cAry.map( (c) => Math.trunc(c*10));
    return (
        <Box style={{position: 'relative'}}>
            {plotState.isBandUsed(Band.RED) && <ToolbarButton text={'Use Red Band'} tip={'Use Red Band'}
                                                              enabled={true} horizontal={false} key={'red'}
                                                              hasCheckBox={true} checkBoxOn={useRed}
                                                              onClick={() => changeBiasContrastColor(colorTableId, bias, contrast, undefined, !useRed, useGreen, useBlue)}/>}
            {plotState.isBandUsed(Band.GREEN) && <ToolbarButton text={'Use Green Band'} tip={'Use Green Band'}
                                                                enabled={true} horizontal={false} key={'green'}
                                                                hasCheckBox={true} checkBoxOn={useGreen}
                                                                onClick={() => changeBiasContrastColor(colorTableId, bias, contrast, undefined, useRed, !useGreen, useBlue)}/>}
            {plotState.isBandUsed(Band.BLUE) && <ToolbarButton text={'Use Blue Band'} tip={'Use Blue Band'}
                                                               enabled={true} horizontal={false} key={'blue'}
                                                               hasCheckBox={true} checkBoxOn={useBlue}
                                                               onClick={() => changeBiasContrastColor(colorTableId, bias, contrast, undefined, useRed, useGreen, !useBlue)}/>}

            <Divider sx={{p: 0}}/>
            <Box width={230} height={126 * plotState.getBands().length} pb={1}>
                {
                    plotState.getBands().map((b, idx) => (
                        <Box key={b.key}>
                            <Stack alignItems='center' spacing={0}>
                                {idx > 0 && <Divider sx={{pt: 0, mt: '10px'}}/>}
                                <Typography level='body-xs' sx={{pt: 0.625, pb: 0.25}}>
                                    <Typography level='body-xs' sx={{color: b.key}}>{b.key}</Typography> Bias
                                </Typography>
                                <RangeSliderView {...{
                                    sx: {pt: 0, mt: -1.5, width: 200},
                                    min: 8, max: 32, step: 1, vertical: false, marks: biasMarks,
                                    defaultValue: biasInt[b.value], slideValue: biasInt[b.value],
                                    handleChange: (v) => changeBiasContrastColor(colorTableId, v / 40, contrast[b.value], undefined, useRed, useGreen, useBlue, b)
                                }} />
                            </Stack>
                            <Stack alignItems='center' spacing={0}>
                                <Typography level='body-xs' sx={{pt: 1, pb: 0.25}}>
                                    <Typography level='body-xs' sx={{color: b.key}}>{b.key}</Typography> Contrast
                                </Typography>
                                <RangeSliderView {...{
                                    sx: {pt: 0, mt: -1.5, width: 200},
                                    min: 0, max: 20, step: 1, vertical: false, marks: contrastMarks,
                                    defaultValue: contrastInt[b.value], slideValue: contrastInt[b.value],
                                    handleChange: (v) => changeBiasContrastColor(colorTableId, bias[b.value], v / 10, undefined, useRed, useGreen, useBlue, b)
                                }} />
                            </Stack>
                        </Box>
                    ))
                }
            </Box>
            {!allLoaded && makeMask()}
        </Box>

    );
}


let defaultNanColorLocked= false;


const AdvancedColorPanel= ({allowPopout}) => {
    const plot = useStoreConnector( () => primePlot(visRoot()) );
    const allLoaded = useStoreConnector(() => isAllStretchDataLoaded(visRoot()));
    const [bias,setBias]= useState( () => plot?.rawData.bandData[0].bias);
    const [contrast,setContrast]= useState( () => getContrast(plot));
    const [ctIdIn,setColorTableId]= useState( () => plot?.colorTableId);
    const [nanPixelColor, setNanPixelColor]= useState( () => plot?.rawData.bandData[0].nanPixelColor );
    const [useRed,setUseRed]= useState( () => plot?.rawData.useRed);
    const [useGreen,setUseGreen]= useState( () => plot?.rawData.useGreen);
    const [useBlue,setUseBlue]= useState( () => plot?.rawData.useBlue);
    const [nanColorLocked,setInnerNanColorLocked]= useState(defaultNanColorLocked);
    const threeColor= isThreeColor(plot);
    const {current:lastCbar}= useRef({prevCaryState:undefined});
    const colorTableId= ctIdIn+'';

    const setNanColorLocked= (lock)  => {
        defaultNanColorLocked= lock;
        setInnerNanColorLocked(lock);
    };

    const image= isImage(plot);
    const plotId= plot?.plotId;

    useEffect(() => {
        if (!plot) return;
        const b= getBias(plot);
        const c= getContrast(plot);
        setBias(b);
        setContrast(c);
        setColorTableId(plot.colorTableId);
    }, [plotId]);

    if (!plot) return <div/>;

    const defNanPixelColor= nanPixelColor;
    const changeBiasContrastColor= (colorTableId, newBias, newContrast, nanPixelColor=undefined, useRed=true, useGreen=true, useBlue=true, band= Band.NO_BAND) => {
        if (!plot) return;
        let newBiasAry=[];
        let newContrastAry=[];

        if (!isThreeColor(plot) && !nanPixelColor) {
            nanPixelColor= defaultNanColorLocked ? defNanPixelColor : findAContrastColor(colorTableId);
        }

        if (band!==Band.NO_BAND) {
            newBiasAry= [...bias];
            newBiasAry[band.value]= newBias;
            newContrastAry= [...contrast];
            newContrastAry[band.value]= newContrast;
            setBias(newBiasAry);
            setContrast(newContrastAry);
        }
        else {
            setBias(newBias);
            setContrast(newContrast);
        }

        setColorTableId(colorTableId);
        setUseRed(useRed);
        setUseGreen(useGreen);
        setUseBlue(useBlue);
        const colorChangeParam=  {
            plotId:plot.plotId,
            cbarId: colorTableId,
            bias: band===Band.NO_BAND ? newBias : newBiasAry,
            contrast: band===Band.NO_BAND ? newContrast : newContrastAry,
            nanPixelColor,
            useRed, useBlue, useGreen,
        };
        image ? dispatchColorChange(colorChangeParam) : dispatchColorChangeThrottled(colorChangeParam);
    };

    const ctArray= (image ? colorTables : hipsColorTables);

    const makeItems = () => {
        return ctArray.map((ct) => {
            if (ct.ignore) return undefined;
            return <ColorItem {...{key:ct.id,colorTableId:colorTableId+'',ct,bias,contrast,changeBiasContrastColor}}/>;
        }).filter(Boolean);
    };

    const allColorTableIDs=[];
    ctArray.forEach(({id,disableReverse,ignore}) => {
        if (ignore) return;
        allColorTableIDs.push(id);
        if (disableReverse) return;
        allColorTableIDs.push(reverseId(id));
    });

    const sx=  allowPopout ? {minWidth: '21rem'} : {boxShadow: 'none'};
    return (
        <SingleColumnMenu {...{sx}}>

            {allowPopout &&
                <Stack sx={{flex:'0 0 auto', alignItems:'flex-end'}}>
                    <IconButton onClick={convertToPopoutColorPanel} sx={{minWidth:'unset', minHeight:'unset', p:'1px'}}>
                        <ArrowOutwardOutlinedIcon />
                    </IconButton>
                </Stack>
            }
            <ColorStretchLockButton {...{allowPopout}}/>
            <ToolbarHorizontalSeparator/>
            {!threeColor && makeItems()}
            {!threeColor && image && <Divider sx={{p: 0.1, mt: 0.2}}/>}
            {!threeColor && image && <NanColor {...{plot, nanColorLocked, setNanColorLocked,
                                          changeColor: (nanPixelColor) => {
                                              setNanPixelColor(nanPixelColor);
                                              changeBiasContrastColor(colorTableId, bias, contrast, nanPixelColor);
                                          }
            }}  />}
            {!threeColor && <Divider sx={{p: 0.1, mt: 0.2}}/>}
            {!threeColor && <StandardFeatures{...{lastCbar,plot,ctArray, allColorTableIDs,
                changeBiasContrastColor,allLoaded, colorTableId,bias,contrast}}/>}
            {threeColor && <ThreeCFeatures {...{allLoaded,useRed,useGreen,useBlue,plot,
                changeBiasContrastColor, bias, contrast, colorTableId}}/>}
        </SingleColumnMenu>
    );
};


function NanColor({plot, changeColor, nanColorLocked, setNanColorLocked}) {
    const colorStr= toRGBAString(plot.rawData.bandData[0].nanPixelColor ?? [0,0,0]);

    const checkIcon= (
        <LockIcon {...{sx: (theme) => {
                const {neutral}= theme?.palette ?? {};
                return {
                    transform: 'scale(.8)',
                    color: getBestContrast(colorStr,neutral[500], neutral[400])
                };
            }
        }}/>);

    const modifyColor= () => {
        hideColorPickerDialog();
        showColorPickerDialog({
            colorStr,
            helpId:'visualization.colorpicker.NaNPixel',
            postTitle:': NaN Color',
            pickerType:'compact',
            cb:(ev) => {
                const {r, g, b} = ev.rgb;
                setNanColorLocked(true);
                changeColor([r,g,b]);
            },
        });
    };
    return (
        <ToolbarButton {...{
            tip:'set NaN color',
            slotProps:{tbCheckBox:{
                sx: { backgroundColor: colorStr, width: 17.5, height: 20, borderRadius: 3.5}
            }},
            text:`NaN Pixel Color ${nanColorLocked?' (Locked)': ' (Auto computed)'}`,
            hasCheckBox:true, checkBoxOn:defaultNanColorLocked, CheckboxOnIcon: checkIcon,
            onClick:() => {
                if (nanColorLocked) {
                    setNanColorLocked(false);
                    changeColor(undefined);
                }
                else {
                    modifyColor();
                }
            }
        }}/>
    );
}


export const ColorTableDropDownView= () => {
    setTimeout(() => dispatchHideDialog(POPOUT_ID), 5);
    return ( <AdvancedColorPanel allowPopout={true}/> );
};

ColorTableDropDownView.propTypes= {
    plotView : PropTypes.object
};

function convertToPopoutColorPanel() {
    showColorDialog();
    dispatchHideDialog(DROP_DOWN_KEY);
}

export function showColorDialog() {
    const content= (
        <PopupPanel title={'Modify Color'} layoutPosition={LayoutType.TOP_RIGHT}>
            <PopoutColorPanel/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(POPOUT_ID, content);
    dispatchShowDialog(POPOUT_ID);
}


function PopoutColorPanel() {
    const pv = useStoreConnector( () => getActivePlotView(visRoot()));
    if (!primePlot(pv)) return <div/>;
    return (
        <Stack>
            <AdvancedColorPanel allowPopout={false}/>
            <HelpIcon helpId='visualization.advanceColorPanel' sx={{alignSelf:'flex-end'}}/>
        </Stack>
    );

}