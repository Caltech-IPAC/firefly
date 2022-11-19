/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Fragment, useState, useEffect} from 'react';
import PropTypes from 'prop-types';
import {throttle, isArray, isNumber} from 'lodash';
import { ToolbarButton, DropDownVerticalSeparator, } from '../../ui/ToolbarButton.jsx';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {dispatchColorChange} from '../ImagePlotCntlr.js';
import {
    primePlot, getPlotViewIdListInOverlayGroup, isThreeColor, getActivePlotView, isAllStretchDataLoaded, } from '../PlotViewUtil.js';
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
import Arrow from 'html/images/popout-arrow_12x12.png';

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
        <div style= {{background: 'rgba(0,0,0,.5)'}} className='loading-mask'/>
        <div style={{
            position: 'absolute',
            top: 132,
            left: 43,
            color: 'white',
            width: 150,
            fontSize: '12pt',
            textAlign: 'center'
        }}>
            Loading Advanced Options
        </div>
    </div> );

const ctMarks = { 0:'0', 3:'3', 6:'6', 9:'9', 12:'12', 15:'15', 18:'18', 21:'21'};
const biasMarks = {8:'.2', 12: '.3', 16:'.4', 20:'.5', 24:'.6', 28:'.7', 32:'.8' };
const contrastMarks = { 0: '0', 5:'5', 10:'1', 15:'1.5',  20:'2'};
const maskWrapper= { position:'absolute', left:0, top:0, width:'100%', height:'100%' };

// const markToBias= (v) =>


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

const AdvancedColorPanel= ({allowPopout}) => {
    const plot = useStoreConnector( () => {
        return primePlot(visRoot());
    });
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

    const makeItems= () =>
        ctArray.map( (ct) =>
            (<ToolbarButton icon={ct.icon} tip={ct.tip} style={{padding: '2px 0 2px 0'}}
                            text={ct.icon ? undefined : 'Default Color Map' }
                            enabled={true} horizontal={false} key={ct.id}
                            hasCheckBox={true} checkBoxOn={colorTableId===ct.id}
                            imageStyle={{height:8}}
                            onClick={() => changeBiasContrastColor(ct.id, bias,contrast)}/>) );


    const makeAdvancedStandardFeatures= () => (
        <div style={{width: 230, height: 180, position:'relative'}}>
            <div style={{display:'flex', flexDirection: 'column', alignItems: 'center'}}>
                <div style={{padding: '5px 0 0 0'}}>Color Bar</div>
                <RangeSliderView {...{
                    style:{paddingTop: 7, width: 200},
                    min:image?0:-1,max:21, step:1,vertical:false, marks:ctMarks,
                    defaultValue:colorTableId, slideValue:colorTableId,
                    handleChange:(v) => changeBiasContrastColor(v, bias,contrast)}} />

            </div>
            {colorTableId!==-1 ? <div style={{display:'flex', flexDirection: 'column', alignItems: 'center'}}>
                <div style={{padding: '30px 0 0 0'}}>Bias</div>
                <RangeSliderView {...{
                    style:{paddingTop: 7, width: 200},
                    min:8,max:32, step:1,vertical:false, marks:biasMarks,
                    defaultValue:biasInt, slideValue:biasInt,
                    handleChange:(v) => changeBiasContrastColor(colorTableId, v/40,contrast)}} />

            </div> : <div/>}
            {colorTableId!==-1 ? <div style={{display:'flex', flexDirection: 'column', alignItems: 'center'}}>
                <div style={{padding: '30px 0 0 0'}}>Contrast</div>
                <RangeSliderView {...{
                    style:{paddingTop: 7, width: 200},
                    min:0,max:20, step:1,vertical:false, marks:contrastMarks,
                    defaultValue:contrastInt, slideValue:contrastInt,
                    handleChange:(v) => changeBiasContrastColor(colorTableId, bias,v/10)}} />
            </div> : <div/>}
            {!allLoaded && makeMask() }
        </div>
    );

    const makeAdvanced3CFeatures= () => (
        <div style={{position:'relative'}}>
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
            <DropDownVerticalSeparator useLine={true}/>
            <div style={{width: 230, height: 126 * plotState.getBands().length}}>
                        {
                            plotState.getBands().map( (b, idx) => (
                                <Fragment key={b.key}>
                                    <div style={{display:'flex', flexDirection: 'column', alignItems: 'center'}}>
                                        <div style={{margin: `${idx?'40':'5'}px 0 0 0`, paddingTop:idx?5:0, textAlign:'center', alignSelf:'stretch', borderTop:`1px solid rgba(0,0,0,${idx?'.1':'0'}`}}>
                                            <span style={{color:b.key}}>{b.key}</span> Bias
                                        </div>
                                        <RangeSliderView {...{
                                            style:{paddingTop: 7, width: 200},
                                            min:8,max:32, step:1,vertical:false, marks:biasMarks,
                                            defaultValue:biasInt[b.value], slideValue:biasInt[b.value],
                                            handleChange:(v) => changeBiasContrastColor(colorTableId, v/40,contrast[b.value],useRed,useGreen,useBlue,b)}} />
                                    </div>
                                    <div style={{display:'flex', flexDirection: 'column', alignItems: 'center'}}>
                                        <div style={{padding: '30px 0 0 0'}}><span style={{color:b.key}}>{b.key}</span> Contrast</div>
                                        <RangeSliderView {...{
                                            style:{paddingTop: 7, width: 200},
                                            min:0,max:20, step:1,vertical:false, marks:contrastMarks,
                                            defaultValue:contrastInt[b.value], slideValue:contrastInt[b.value],
                                                        handleChange:(v) => changeBiasContrastColor(colorTableId, bias[b.value],v/10, useRed,useGreen,useBlue,b)}} />
                                    </div>
                                </Fragment>
                            ))
                        }
            </div>
            {!allLoaded && makeMask() }
        </div>
    );



    return (
        <SingleColumnMenu>
            {allowPopout &&
                <div style={{flex:'0 0 auto', display:'flex', flexDirection:'column', alignItems:'flex-end', height:12}}>
                    <img className='ff-MenuItem-light' onClick={convertToPopoutColorPanel}
                         style={{width:12, height:12, border: '1px solid rgba(0,0,0,.1)', borderRadius: '3px', zIndex:1 }} src={Arrow}/>
                </div>
            }
            {!threeColor && makeItems()}
            {!threeColor && <DropDownVerticalSeparator useLine={true}/>}
            {!threeColor && makeAdvancedStandardFeatures()}
            {threeColor && makeAdvanced3CFeatures()}
        </SingleColumnMenu>
    );
};

export const ColorTableDropDownView= () => {
    dispatchHideDialog(POPOUT_ID);
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
    return <AdvancedColorPanel allowPopout={false}/>;
}
