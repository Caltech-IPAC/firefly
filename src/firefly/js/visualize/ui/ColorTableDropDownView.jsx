/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState, useEffect} from 'react';
import PropTypes from 'prop-types';
import {throttle, isArray, isNumber} from 'lodash';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {ToolbarButton, ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {dispatchColorChange, dispatchOverlayColorLocking} from '../ImagePlotCntlr.js';
import {
    primePlot, getPlotViewIdListInOverlayGroup, isThreeColor, getActivePlotView, isAllStretchDataLoaded, findPlotGroup,
    hasOverlayColorLock,
} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {isImage} from '../WebPlot.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {Band} from '../Band.js';
import {LayoutType, PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {RangeSliderView} from '../../ui/RangeSliderView.jsx';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {dispatchHideDialog, dispatchShowDialog} from 'firefly/core/ComponentCntlr.js';
import {DROP_DOWN_KEY} from 'firefly/ui/DropDownToolbarButton.jsx';
import {Typography, Box, Stack, Divider, IconButton, Skeleton} from '@mui/joy';
import LockIcon from '@mui/icons-material/Lock';
import LockOpenOutlinedIcon from '@mui/icons-material/LockOpenOutlined';
import ArrowOutwardOutlinedIcon from '@mui/icons-material/ArrowOutwardOutlined';

import ColorTable0 from 'html/images/cbar/ct-0-gray.png';
import ColorTable1 from 'html/images/cbar/ct-1-reversegray.png';
import ColorTable2 from 'html/images/cbar/ct-2-colorcube.png';
import ColorTable3 from 'html/images/cbar/ct-3-spectrum.png';
import ColorTable4 from 'html/images/cbar/ct-4-false.png';
import ColorTable5 from 'html/images/cbar/ct-5-reversefalse.png';
import ColorTable6 from 'html/images/cbar/ct-6-falsecompressed.png';
import ColorTable7 from 'html/images/cbar/ct-7-difference.png';
import ColorTable8 from 'html/images/cbar/ct-8-a-ds9.png';
import ColorTable9 from 'html/images/cbar/ct-9-b-ds9.png';
import ColorTable10 from 'html/images/cbar/ct-10-bb-ds9.png';
import ColorTable11 from 'html/images/cbar/ct-11-he-ds9.png';
import ColorTable12 from 'html/images/cbar/ct-12-i8-ds9.png';
import ColorTable13 from 'html/images/cbar/ct-13-aips-ds9.png';
import ColorTable14 from 'html/images/cbar/ct-14-sls-ds9.png';
import ColorTable15 from 'html/images/cbar/ct-15-hsv-ds9.png';
import ColorTable16 from 'html/images/cbar/ct-16-heat-ds9.png';
import ColorTable17 from 'html/images/cbar/ct-17-cool-ds9.png';
import ColorTable18 from 'html/images/cbar/ct-18-rainbow-ds9.png';
import ColorTable19 from 'html/images/cbar/ct-19-standard-ds9.png';
import ColorTable20 from 'html/images/cbar/ct-20-staircase-ds9.png';
import ColorTable21 from 'html/images/cbar/ct-21-color-ds9.png';

//=================================

const POPOUT_ID= 'AdvancedColorPopout';

const colorTables=[
    { id: 0,  icon: ColorTable0, tip:'Gray Scale' },
    { id: 1,  icon: ColorTable1, tip: 'Reverse Gray Scale' },
    { id: 2,  icon: ColorTable2, tip: 'Color Cube' },
    { id: 3,  icon: ColorTable3, tip: 'Spectrum' },
    { id: 4,  icon: ColorTable4, tip: 'For False Color' },
    { id: 5,  icon: ColorTable5, tip: 'For False Color - Reversed' },
    { id: 6,  icon: ColorTable6, tip: 'For False Color - Compressed' },
    { id: 7,  icon: ColorTable7, tip: 'For difference images' },
    { id: 8,  icon: ColorTable8, tip: 'DS9\'s a color bar' },
    { id: 9,  icon: ColorTable9, tip: 'DS9\'s b color bar' },
    { id: 10, icon: ColorTable10, tip: 'DS9\'s bb color bar' },
    { id: 11, icon: ColorTable11, tip: 'DS9\'s he color bar' },
    { id: 12, icon: ColorTable12, tip: 'DS9\'s i8 color bar' },
    { id: 13, icon: ColorTable13, tip: 'DS9\'s aips color bar' },
    { id: 14, icon: ColorTable14, tip: 'DS9\'s sls color bar' },
    { id: 15, icon: ColorTable15, tip: 'DS9\'s hsv color bar' },
    { id: 16, icon: ColorTable16, tip: 'Heat (ds9)' },
    { id: 17, icon: ColorTable17, tip: 'Cool (ds9)' },
    { id: 18, icon: ColorTable18, tip: 'Rainbow (ds9)' },
    { id: 19, icon: ColorTable19, tip: 'Standard (ds9)' },
    { id: 20, icon: ColorTable20, tip: 'Staircase (ds9)' },
    { id: 21, icon: ColorTable21, tip: 'Color (ds9)' }
];

const hipsColorTables=[ { id: -1,  icon: undefined, tip:'Original' }, ...colorTables, ];

const isAllThreeColor= (vr,plotIdAry) => plotIdAry.every( (id) => isThreeColor(primePlot(vr,id)));

const handleColorChange= (plot,cbarId, bias= .5, contrast= 1) => {
    const vr= visRoot();
    const plotIdAry= getPlotViewIdListInOverlayGroup(vr,plot.plotId);

    if (isAllThreeColor(vr,plotIdAry)) {
        showInfoPopup('This is a three color plot, you can not change the color.', 'Color change not allowed');
    }
    else {
        dispatchColorChange({plotId:plot.plotId,cbarId, bias, contrast});
    }
};

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
    { label: '0', value: 0 }, { label: '3', value: 3 }, { label: '6', value: 6 }, { label: '9', value: 9 },
    { label: '12', value: 12 }, { label: '15', value: 15 }, { label: '18', value: 18 }, { label: '21', value: 21 }
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

function isOverlayColorLocked(vr){
    const pv= getActivePlotView(vr);
    if (!pv) return false;
    return hasOverlayColorLock(pv,findPlotGroup(pv?.plotGroupId,vr.plotGroupAry));
}

const AdvancedColorPanel= ({allowPopout}) => {
    const plot = useStoreConnector( () => primePlot(visRoot()) );
    const overlayColorLocked = useStoreConnector( () => isOverlayColorLocked(visRoot()) );
    const pv= getActivePlotView(visRoot());
    const allLoaded = useStoreConnector(() => isAllStretchDataLoaded(visRoot()));
    const [bias,setBias]= useState( () => getBias(plot));
    const [contrast,setContrast]= useState( () => getContrast(plot));
    const [colorTableId,setColorTableId]= useState( () => Number(plot?.colorTableId));
    const [useRed,setUseRed]= useState( () => plot?.rawData.useRed);
    const [useGreen,setUseGreen]= useState( () => plot?.rawData.useGreen);
    const [useBlue,setUseBlue]= useState( () => plot?.rawData.useBlue);
    const threeColor= isThreeColor(plot);

    const biasInt= isArray(bias) ? bias.map( (b) => Math.trunc(b*40))  : Math.trunc(bias*40);
    const contrastInt= isArray(contrast) ? contrast.map( (c) => Math.trunc(c*10)) : Math.trunc(contrast*10);
    const image= isImage(plot);
    const plotId= plot?.plotId;

    useEffect(() => {
        if (!plot) return;
        const b= getBias(plot);
        const c= getContrast(plot);
        setBias(b);
        setContrast(c);
        setColorTableId(Number(plot.colorTableId));
    }, [plotId]);

    if (!plot) return <div/>;

    const {plotState}= plot;
    const changeBiasContrastColor= (colorTableId, newBias, newContrast, useRed=true, useGreen=true, useBlue=true, band= Band.NO_BAND) => {
        if (!plot) return;
        let newBiasAry=[];
        let newContrastAry=[];

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

        setColorTableId(Number(colorTableId));
        setUseRed(useRed);
        setUseGreen(useGreen);
        setUseBlue(useBlue);
        const colorChangeParam=  {
                        plotId:plot.plotId,
                        cbarId: Number(colorTableId),
                        bias: band===Band.NO_BAND ? newBias : newBiasAry,
                        contrast: band===Band.NO_BAND ? newContrast : newContrastAry,
                        useRed, useBlue, useGreen,
                    };
        image ? dispatchColorChange(colorChangeParam) : dispatchColorChangeThrottled(colorChangeParam);
    };

    const ctArray= (image? colorTables : hipsColorTables);

    const makeItems = () =>
        ctArray.map((ct) =>
            (<ToolbarButton icon={ct.icon ? <img src={ct.icon} style={{height:8, width:200}}/> : undefined}
                            tip={ct.tip} style={{padding: '2px 0 2px 0'}}
                            text={ct.icon ? undefined : 'Default Color Map'}
                            enabled={true} horizontal={false} key={ct.id}
                            hasCheckBox={true} checkBoxOn={colorTableId === ct.id}
                            onClick={() => changeBiasContrastColor(ct.id, bias, contrast)}/>));

    const makeAdvancedStandardFeatures= () => (
        <Box width={230} height={180}>
            <Stack alignItems='center' spacing={0}>
                <Typography level='body-xs' sx={{pt:0.625, pb:0.25}}>Color Bar</Typography>
                <RangeSliderView {...{
                    sx:{pb:0.5, mt:-1.5, width: 200},
                    min:image?0:-1,max:21, step:1,vertical:false, marks:ctMarks,
                    defaultValue:colorTableId, slideValue:colorTableId,
                    handleChange:(v) => changeBiasContrastColor(v, bias,contrast)}} />

            </Stack>
            {colorTableId!==-1 ? <Stack alignItems='center' spacing={0}>
                <Typography level='body-xs' sx={{pt:0.625, pb:0.25}}>Bias</Typography>
                <RangeSliderView {...{
                    sx:{pb:0.5, mt:-1.5, width: 200},
                    min:8,max:32, step:1,vertical:false, marks:biasMarks,
                    defaultValue:biasInt, slideValue:biasInt,
                    handleChange:(v) => changeBiasContrastColor(colorTableId, v/40,contrast)}} />

            </Stack> : <div/>}
            {colorTableId!==-1 ? <Stack alignItems='center'>
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

    const makeAdvanced3CFeatures= () => (
        <Box style={{position:'relative'}}>
            {plotState.isBandUsed(Band.RED) && <ToolbarButton text= {'Use Red Band'} tip={'Use Red Band'}
                           enabled={true} horizontal={false} key={'red'}
                           hasCheckBox={true} checkBoxOn={useRed}
                           onClick={() => changeBiasContrastColor(colorTableId,bias,contrast,!useRed,useGreen,useBlue)}/>}
            {plotState.isBandUsed(Band.GREEN) && <ToolbarButton text= {'Use Green Band'} tip={'Use Green Band'}
                           enabled={true} horizontal={false} key={'green'}
                           hasCheckBox={true} checkBoxOn={useGreen}
                           onClick={() => changeBiasContrastColor(colorTableId,bias,contrast,useRed,!useGreen,useBlue)}/>}
            {plotState.isBandUsed(Band.BLUE) && <ToolbarButton text= {'Use Blue Band'} tip={'Use Blue Band'}
                           enabled={true} horizontal={false} key={'blue'}
                           hasCheckBox={true} checkBoxOn={useBlue}
                           onClick={() => changeBiasContrastColor(colorTableId,bias,contrast,useRed,useGreen,!useBlue)}/>}

            <Divider sx={{p: 0}}/>
            <Box width={230} height={126 * plotState.getBands().length} pb={1}>
                        {
                            plotState.getBands().map( (b, idx) => (
                                <Box key={b.key}>
                                    <Stack alignItems='center' spacing={0}>
                                        {idx > 0 && <Divider sx={{pt:0, mt:'10px'}}/>}
                                            <Typography level='body-xs' sx={{pt:0.625, pb:0.25}}>
                                                <Typography level='body-xs' sx={{color:b.key}}>{b.key}</Typography> Bias
                                            </Typography>
                                        <RangeSliderView {...{
                                            sx:{pt:0, mt:-1.5, width: 200},
                                            min:8,max:32, step:1,vertical:false, marks:biasMarks,
                                            defaultValue:biasInt[b.value], slideValue:biasInt[b.value],
                                            handleChange:(v) => changeBiasContrastColor(colorTableId, v/40,contrast[b.value],useRed,useGreen,useBlue,b)}} />
                                    </Stack>
                                    <Stack alignItems='center' spacing={0}>
                                        <Typography level='body-xs' sx={{pt:1, pb:0.25}}>
                                            <Typography level='body-xs' sx={{color:b.key}}>{b.key}</Typography> Contrast
                                        </Typography>
                                        <RangeSliderView {...{
                                            sx:{pt:0, mt:-1.5, width: 200},
                                            min:0,max:20, step:1,vertical:false, marks:contrastMarks,
                                            defaultValue:contrastInt[b.value], slideValue:contrastInt[b.value],
                                                        handleChange:(v) => changeBiasContrastColor(colorTableId, bias[b.value],v/10, useRed,useGreen,useBlue,b)}} />
                                    </Stack>
                                </Box>
                            ))
                        }
            </Box>
            {!allLoaded && makeMask() }
        </Box>
    );


    const sx=  allowPopout ? {} : {boxShadow: 'none'};
    return (
        <SingleColumnMenu {...{sx}}>

            {allowPopout &&
                <Stack sx={{flex:'0 0 auto', alignItems:'flex-end'}}>
                    <IconButton onClick={convertToPopoutColorPanel} sx={{minWidth:'unset', minHeight:'unset', p:'1px'}}>
                        <ArrowOutwardOutlinedIcon />
                    </IconButton>
                </Stack>
            }
            <ToolbarButton plotView={pv}
                           sx={allowPopout? {mt:-2.5, width:.85} : {}}
                           hasCheckBox={true}
                           checkBoxOn={overlayColorLocked}
                           CheckboxOnIcon={<LockIcon sx={{pt: 1/4}}/>}
                           CheckboxOffIcon={<LockOpenOutlinedIcon sx={{pt: 1/4}}/>}
                           text={ overlayColorLocked? 'Color & overlays locked' : 'Color & overlays unlocked' }
                           tip='Lock all images for color changes and overlays.'
                           onClick={() => toggleOverlayColorLock(pv,visRoot().plotGroupAry)} />
            <ToolbarHorizontalSeparator/>
            {!threeColor && makeItems()}
            {!threeColor && <Divider sx={{p: 0.1, mt: 0.2}}/>}
            {!threeColor && makeAdvancedStandardFeatures()}
            {threeColor && makeAdvanced3CFeatures()}
        </SingleColumnMenu>
    );
};


function toggleOverlayColorLock(pv,plotGroupAry){
    const plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);
    dispatchOverlayColorLocking(pv.plotId,!hasOverlayColorLock(pv,plotGroup));
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