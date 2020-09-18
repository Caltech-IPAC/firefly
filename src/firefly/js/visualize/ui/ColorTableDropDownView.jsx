/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState, useEffect} from 'react';
import PropTypes from 'prop-types';
import {
    ToolbarButton,
    DropDownVerticalSeparator,
    } from '../../ui/ToolbarButton.jsx';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {dispatchColorChange} from '../ImagePlotCntlr.js';
import {
    primePlot,
    getPlotViewIdListInOverlayGroup,
    isThreeColor,
    getActivePlotView,
    isAllStretchDataLoadable, isAllStretchDataLoaded
} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {Band} from '../Band.js';


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
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {RangeSliderView} from '../../ui/RangeSliderView.jsx';
import {throttle} from 'lodash';

//=================================

const colorTables=[
    { id: 0,  icon: ColorTable0, tip:'Gray Scale' },
    { id: 1,  icon: ColorTable1, tip: 'Reverse Gray Scale' },
    { id: 2,  icon: ColorTable2, tip: 'Color Cube' },
    { id: 3,  icon: ColorTable3, tip: 'Spectrum' },
    { id: 4,  icon: ColorTable4, tip: 'For False Color' },
    { id: 5,  icon: ColorTable5, tip: 'For False Color - Reversed' },
    { id: 6,  icon: ColorTable6, tip: 'For False Color - Compressed' },
    { id: 7,  icon: ColorTable7, tip: 'For difference images' },
    { id: 8,  icon: ColorTable8, tip: `DS9's a color bar` },
    { id: 9,  icon: ColorTable9, tip: `DS9's b color bar` },
    { id: 10, icon: ColorTable10, tip: `DS9's bb color bar` },
    { id: 11, icon: ColorTable11, tip: `DS9's he color bar` },
    { id: 12, icon: ColorTable12, tip: `DS9's i8 color bar` },
    { id: 13, icon: ColorTable13, tip: `DS9's aips color bar` },
    { id: 14, icon: ColorTable14, tip: `DS9's sls color bar` },
    { id: 15, icon: ColorTable15, tip: `DS9's hsv color bar` },
    { id: 16, icon: ColorTable16, tip: 'Heat (ds9)' },
    { id: 17, icon: ColorTable17, tip: 'Cool (ds9)' },
    { id: 18, icon: ColorTable18, tip: 'Rainbow (ds9)' },
    { id: 19, icon: ColorTable19, tip: 'Standard (ds9)' },
    { id: 20, icon: ColorTable20, tip: 'Staircase (ds9)' },
    { id: 21, icon: ColorTable21, tip: 'Color (ds9)' }
];



//====================================

// function makeItems(pv,ctAry) {
//     const id= Number(primePlot(pv).plotState.getColorTableId());
//     return ctAry.map( (ct,cbarIdx) => {
//         return (
//             <ToolbarButton icon={ct.icon} tip={ct.tip} style={{padding: '2px 0 2px 0'}}
//                            enabled={true} horizontal={false} key={cbarIdx}
//                            hasCheckBox={true} checkBoxOn={id===ct.id}
//                            onClick={() => handleColorChange(pv,cbarIdx)}/>
//         );
//     });
// }
//

const isAllThreeColor= (vr,plotIdAry) => plotIdAry.every( (id) => isThreeColor(primePlot(vr,id)));

const handleColorChange= (pv,cbarId, bias= .5, contrast= 1) => {
    const vr= visRoot();
    const plotIdAry= getPlotViewIdListInOverlayGroup(vr,pv);

    if (isAllThreeColor(vr,plotIdAry)) {
        showInfoPopup('This is a three color plot, you can not change the color.', 'Color change not allowed');
    }
    else {
        dispatchColorChange({plotId:pv.plotId,cbarId, bias, contrast});
    }
};


// export function ColorTableDropDownView({plotView:pv}) {
//     return (
//         <SingleColumnMenu>
//             {makeItems(pv,colorTables)}
//         </SingleColumnMenu>
//         );
//
// }

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
const biasMarks = {20:'.2', 30: '.3', 40:'.4', 50:'.5', 60:'.6', 70:'.7', 80:'.8' };
const contrastMarks = { 0: '0', 5:'5', 10:'1', 15:'1.5',  20:'2'};

const maskWrapper= { position:'absolute', left:0, top:0, width:'100%', height:'100%' };


const AdvancedColorPanel= ({pv:pvFromProp}) => {
    const [allLoaded] = useStoreConnector(() => isAllStretchDataLoaded(visRoot()));
    const [bias,setBias]= useState( () => primePlot(pvFromProp)?.rawData.bandData[0].bias);
    const [contrast,setContrast]= useState( () => primePlot(pvFromProp)?.rawData.bandData[0].contrast);
    const [colorTableId,setColorTableId]= useState( () => Number(primePlot(pvFromProp)?.plotState.getColorTableId()));
    const [useRed,setUseRed]= useState( () => primePlot(pvFromProp)?.rawData.useRed);
    const [useGreen,setUseGreen]= useState( () => primePlot(pvFromProp)?.rawData.useGreen);
    const [useBlue,setUseBlue]= useState( () => primePlot(pvFromProp)?.rawData.useBlue);
    const plot= primePlot(pvFromProp);
    const {plotState}= plot;
    const biasInt= Math.trunc(bias*100);
    const contrastInt= Math.trunc(contrast*10);
    const threeColor= isThreeColor(pvFromProp);

    useEffect(() => {
        const plot= primePlot(pvFromProp);
        const {bias:b,contrast:c}= plot.rawData.bandData[0];
        setBias(b);
        setContrast(c);
        setColorTableId(Number(plotState.getColorTableId()));
    }, [pvFromProp]);

    const changeBiasContrastColor= (colorTableId,bias,contrast,useRed=true, useGreen=true, useBlue=true) => {
        if (!plot) return;
        setBias(bias);
        setContrast(contrast);
        setColorTableId(Number(colorTableId));
        setUseRed(useRed);
        setUseGreen(useGreen);
        setUseBlue(useBlue);
        dispatchColorChange({
            plotId:plot.plotId,
            cbarId: colorTableId,
            bias,
            contrast,
            useRed, useBlue, useGreen,
        });
    };

    const makeItems= () =>
        colorTables.map( (ct) =>
            (<ToolbarButton icon={ct.icon} tip={ct.tip} style={{padding: '2px 0 2px 0'}}
                            enabled={true} horizontal={false} key={ct.id}
                            hasCheckBox={true} checkBoxOn={colorTableId===ct.id}
                            imageStyle={{height:8}}
                            onClick={() => handleColorChange(pvFromProp,ct.id)}/>) );


    const allLoadAble= isAllStretchDataLoadable(visRoot());



    const makeAdvancedStandardFeatures= () => (
        <div style={{width: 230, height: 180, position:'relative'}}>
            <div style={{display:'flex', flexDirection: 'column', alignItems: 'center'}}>
                <div style={{padding: '5px 0 0 0'}}>Color Bar</div>
                <RangeSliderView {...{
                    wrapperStyle:{paddingTop: 7, width: 200},
                    min:0,max:21, step:1,vertical:false, marks:ctMarks,
                    defaultValue:colorTableId, slideValue:colorTableId,
                    handleChange:(v) => changeBiasContrastColor(v, bias,contrast)}} />

            </div>
            <div style={{display:'flex', flexDirection: 'column', alignItems: 'center'}}>
                <div style={{padding: '30px 0 0 0'}}>Bias</div>
                <RangeSliderView {...{
                    wrapperStyle:{paddingTop: 7, width: 200},
                    min:20,max:80, step:1,vertical:false, marks:biasMarks,
                    defaultValue:biasInt, slideValue:biasInt,
                    handleChange:(v) => changeBiasContrastColor(colorTableId, v/100,contrast)}} />

            </div>
            <div style={{display:'flex', flexDirection: 'column', alignItems: 'center'}}>
                <div style={{padding: '30px 0 0 0'}}>Contrast</div>
                <RangeSliderView {...{
                    wrapperStyle:{paddingTop: 7, width: 200},
                    min:0,max:20, step:1,vertical:false, marks:contrastMarks,
                    defaultValue:contrastInt, slideValue:contrastInt,
                    handleChange:(v) => changeBiasContrastColor(colorTableId, bias,v/10)}} />
            </div>
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
            <div style={{width: 230, height: 120}}>
                <div style={{display:'flex', flexDirection: 'column', alignItems: 'center'}}>
                    <div style={{padding: '5px 0 0 0'}}>Bias</div>
                    <RangeSliderView {...{
                        wrapperStyle:{paddingTop: 7, width: 200},
                        min:20,max:80, step:1,vertical:false, marks:biasMarks,
                        defaultValue:biasInt, slideValue:biasInt,
                        handleChange:(v) => changeBiasContrastColor(colorTableId, v/100,contrast,useRed,useGreen,useBlue)}} />

                </div>
                <div style={{display:'flex', flexDirection: 'column', alignItems: 'center'}}>
                    <div style={{padding: '30px 0 0 0'}}>Contrast</div>
                    <RangeSliderView {...{
                        wrapperStyle:{paddingTop: 7, width: 200},
                        min:0,max:20, step:1,vertical:false, marks:contrastMarks,
                        defaultValue:contrastInt, slideValue:contrastInt,
                        handleChange:(v) => changeBiasContrastColor(colorTableId, bias,v/10, useRed,useGreen,useBlue)}} />
                </div>
            </div>
            {!allLoaded && makeMask() }
        </div>
);



    return (
        <SingleColumnMenu>
            {!threeColor && makeItems()}
            {allLoadAble && <DropDownVerticalSeparator useLine={true}/>}
            {allLoadAble && !threeColor && makeAdvancedStandardFeatures()}
            {allLoadAble && threeColor && makeAdvanced3CFeatures()}
        </SingleColumnMenu>
    );
};

export const ColorTableDropDownView= ({plotView:pv}) => {
    return (
        <AdvancedColorPanel pv={pv}/>
    );
};

ColorTableDropDownView.propTypes= {
    plotView : PropTypes.object
};
