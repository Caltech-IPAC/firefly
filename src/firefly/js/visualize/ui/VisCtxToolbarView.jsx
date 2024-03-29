/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Checkbox, Divider, Stack, Tooltip, Typography} from '@mui/joy';
import React, {memo,Fragment} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, isString} from 'lodash';
import shallowequal from 'shallowequal';
import {sprintf} from '../../externalSource/sprintf';
import BrowserInfo from '../../util/BrowserInfo.js';
import {
    convertHDUIdxToImageIdx, convertImageIdxToHDU, getActivePlotView, getCubePlaneCnt, getFormattedWaveLengthUnits,
    getHDU, getHDUCount, getHDUIndex, getPlotViewById, getPtWavelength, hasPlaneOnlyWLInfo, isImageCube, isMultiHDUFits,
    primePlot, pvEqualExScroll,
} from '../PlotViewUtil.js';
import {getExtName, getExtType} from '../FitsHeaderUtil.js';
import {makeWorldPt} from '../Point.js';
import {isHiPS, isHiPSAitoff, isImage} from '../WebPlot.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView.jsx';
import {dispatchExtensionActivate} from '../../core/ExternalAccessCntlr.js';
import {
    dispatchChangeCenterOfProjection, dispatchChangeHiPS, dispatchChangeHipsImageConversion,
    dispatchChangePrimePlot, visRoot } from '../ImagePlotCntlr.js';
import {makePlotSelectionExtActivateData} from '../../core/ExternalAccessUtils.js';
import {ListBoxInputFieldView} from '../../ui/ListBoxInputField';
import {showHiPSSurveysPopup} from '../../ui/HiPSImageSelect.jsx';
import {StateInputField} from '../../ui/StatedInputfield.jsx';
import Validate from '../../util/Validate.js';
import {CoordinateSys} from '../CoordSys.js';
import {convertToHiPS, convertToImage, doHiPSImageConversionIfNecessary} from '../task/PlotHipsTask.js';
import {
    BeforeButton,
    CenterOnSelection, CheckedButton, CheckedClearButton, CropButton, FilterAddButton, FiltersOffButton, NextButton,
    StatsButton
} from './Buttons.jsx';
import {
    clearFilterDrawingLayer, crop, filterDrawingLayer, recenterToSelection, selectDrawingLayer, stats,
    unselectDrawingLayer, zoomIntoSelection
} from './CtxToolbarFunctions';
import {isSpacialActionsDropVisible, ActionsDropDownButton} from '../../ui/ActionsDropDownButton.jsx';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {DropDownToolbarButton} from 'firefly/ui/DropDownToolbarButton.jsx';

import SELECTED_ZOOM from 'html/images/icons-2014/ZoomFitToSelectedSpace.png';

const image24x24={width:24, height:24};


function makeExtensionButtons(extensionAry,pv) {
    if (!extensionAry) return false;
    return extensionAry.map( (ext,idx) => {
            return (
                <ToolbarButton icon={ext.imageUrl} text={ext.title}
                               tip={ext.toolTip} key={ext.id} shortcutKey={ext.shortcutKey}
                               lastTextItem={idx===(extensionAry.length-1)}
                               onClick={() => {
                                   if (getActivePlotView(visRoot())?.plotId===pv.plotId) {
                                       dispatchExtensionActivate(ext,makePlotSelectionExtActivateData(ext,pv));
                                   }
                               }}/>
            );
        }
    );
}


export function canConvertHipsAndFits(pv) {
    if (!pv || !pv.plotViewCtx.hipsImageConversion) return false;
    const {hipsRequestRoot, imageRequestRoot}= pv.plotViewCtx.hipsImageConversion;
    return (hipsRequestRoot &&  imageRequestRoot);
}

function doHiPSFitsConvert(pv,target) {
    if (!canConvertHipsAndFits(pv)) return;
    if (target==='fits') {
        if (isHiPS(primePlot(pv))) convertToImage(pv,true);
    }
    else if (target==='sin') {
        if (isImage(primePlot(pv))) convertToHiPS(pv,true, false);
        else dispatchChangeCenterOfProjection({plotId:pv.plotId, fullSky:false});
    }
    else if (target==='aitoff') {
        if (isImage(primePlot(pv))) convertToHiPS(pv,true, true);
        else dispatchChangeCenterOfProjection({plotId:pv.plotId, fullSky:true});
    }

}

function changeAutoConvert(pv, auto) {
    dispatchChangeHipsImageConversion({plotId:pv.plotId, hipsImageConversionChanges:{autoConvertOnZoom:auto}});
    const nextPv= getPlotViewById(visRoot(), pv.plotId);
    if (auto) doHiPSImageConversionIfNecessary(nextPv);
}


const projOptions= [
    {label: 'HiPS', value: 'sin', tooltip: 'All-sky multi-resolution picture with spherical projection, up to 180 degrees'},
    {label: 'HiPS/Aitoff', value: 'aitoff', tooltip: 'All-sky multi-resolution picture with AITOFF projection, up to 360 degrees'},
];
const defHFOptions= [
    {label: 'FITS', value: 'fits', tooltip: 'Scientific pixel data over limited regions'},
    ...projOptions
];

function HipsFitsConvertButton({pv}) {
    const plot= primePlot(pv);
    if (!plot) return undefined;

    let value= 'fits';
    if (isHiPS(plot)) value= isHiPSAitoff(plot) ? 'aitoff' : 'sin';

    const buttonGroupTip= 'Auto-transition between FITS and HiPS depending on zoom';

    const {autoConvertOnZoom:auto}= pv.plotViewCtx.hipsImageConversion;
    return (
        <Stack direction='row' alignItems='center' pl='.5'>
            <RadioGroupInputFieldView {...{
                options: defHFOptions, value, buttonGroup:true,
                sx: {button:{height:'1.5em'}},
                onChange: (ev) => doHiPSFitsConvert(pv,ev.target.value)
            }}/>
            <Checkbox label='Auto' checked={auto} onChange={() => changeAutoConvert(pv, !auto)}
                      title={buttonGroupTip}
                      sx={{pl:.5}} />
        </Stack>
    );
}

HipsFitsConvertButton.propTypes= { pv : PropTypes.object.isRequired};



function HipsProjConvertButton({pv}) {

    const plot= primePlot(pv);
    if (!plot) return undefined;
    const value= isHiPSAitoff(plot) ? 'aitoff' : 'sin';
    const buttonGroupTip= 'Change HiPS projection between spherical (180 deg) and Aitoff (360 deg)';
    return (
        <RadioGroupInputFieldView {...{
            options: projOptions, value, buttonGroup:true, title:buttonGroupTip,
            sx: {pl:.5, button:{height:'1.5em'}},
            onChange: (ev) => dispatchChangeCenterOfProjection({plotId:pv.plotId, fullSky:ev.target.value==='aitoff'})
        }}/>
    );
}

function makeHiPSImageTable(pv) {
    if (!primePlot(pv)) return null;
    const mi= pv.plotViewCtx.menuItemKeys;

    const dropDown= (
        <SingleColumnMenu>
            <ToolbarButton text={'Change HiPS'} tip={'Choose a different HiPS Survey'}
                           key={'change Hips'}
                           visible={mi.hipsSurveyPopup}
                           onClick={()=>showHiPSSurveysPopup(pv)} />
            <ToolbarButton text={'Add MOC Layer'} tip={'Add a new MOC layer to the HiPS Survey'}
                           key={'add'}
                           visible={mi.mocLayerPopup}
                           onClick={()=>showHiPSSurveysPopup(pv,true)} />
        </SingleColumnMenu>
    );

    return (
        <div style={{display:'flex'}}>
                <DropDownToolbarButton
                    text={'HiPS / MOC'} tip='Change displayed HiPS or add a MOC'
                    useDropDownIndicator={true} dropDown={dropDown} />
        </div>
    );
}


const hipsCoordOptions= [
    {label: 'Galactic', value:0, c: CoordinateSys.GALACTIC},
    {label: 'Eq J2000', value:1, c: CoordinateSys.EQ_J2000},
];


const HiPSCoordSelect= memo(({plotId, imageCoordSys}) =>{
    if (!imageCoordSys || !plotId) return undefined;
    const selectedIdx= Math.max(hipsCoordOptions.findIndex( (s) => s.c===imageCoordSys), 0);
    return (
        <div>
            <ListBoxInputFieldView {...{
                sx: {'& .MuiSelect-root': {'minHeight': '1.6em'}},
                inline: true, value: selectedIdx, options:hipsCoordOptions,
                onChange: (ev,newValue) => dispatchChangeHiPS( {plotId,  coordSys: hipsCoordOptions[Number(newValue)].c}),
                tooltip:'Change HiPS survey coordinate system',
            }}/>
        </div>
    );
});
HiPSCoordSelect.propTypes= {
    plotId : PropTypes.string,
    imageCoordSys : PropTypes.object,
};


/**
 * @param {Object} props
 * @param props.pv
 * @param props.extensionAry
 * @param props.showSelectionTools
 * @param props.showCatSelect
 * @param props.showCatUnSelect
 * @param props.showFilter
 * @param props.showClearFilter
 * @param props.searchActions
 * @param props.showMultiImageController
 */
export const VisCtxToolbarView= memo((props) => {
    const {
        plotView:pv, extensionAry, showSelectionTools=false,
        showCatSelect=false, showCatUnSelect=false, width,
        showFilter=false, showClearFilter=false,
        searchActions= undefined,
        showMultiImageController=false }= props;


    const extraLine= showMultiImageController && width<350;
    const plot= primePlot(pv);
    const image= isImage(plot);
    const canConvertHF= canConvertHipsAndFits(pv);
    const hips= isHiPS(plot);
    const mi= pv?.plotViewCtx.menuItemKeys;
    const showOptions= showSelectionTools|| showCatSelect|| showCatUnSelect ||
        showFilter || showClearFilter || !isEmpty(extensionAry) || hips || canConvertHF;

    const makeButtons= () => (
        <Stack {...{direction:'row', flexWrap:'wrap'}}>

            {showSelectionTools && image &&
            <CropButton tip='Crop the image to the selected area' visible={mi.crop} onClick={() => crop(pv)}/>}


            {showCatSelect &&
            <CheckedButton tip='Mark data in area as selected' visible={mi.selectTableRows}
                           onClick={() => selectDrawingLayer(pv)}/>}

            {showCatUnSelect &&
            <CheckedClearButton tip='Mark all data unselected' visible={mi.unselectTableRows}
                           onClick={() => unselectDrawingLayer(pv)}/>}

            {showFilter &&
            <FilterAddButton tip='Filter in the selected area' visible={mi.filterTableRows}
                             onClick={() => filterDrawingLayer(pv)}/>}

            {showClearFilter &&
            <FiltersOffButton tip='Clear all the Filters' visible={mi.clearTableFilters}
                              onClick={() => clearFilterDrawingLayer(pv)}/>}

            {showSelectionTools &&
            <ToolbarButton icon={SELECTED_ZOOM} tip='Zoom to fit selected area'
                           imageStyle={image24x24} visible={mi.zoomToSelection}
                           onClick={() => zoomIntoSelection(pv)}/>}

            { showSelectionTools &&
            <CenterOnSelection tip='Recenter image to selected area'
                           visible={mi.recenterToSelection} onClick={() => recenterToSelection(pv)}/>}

            {showSelectionTools && image &&
            <StatsButton tip='Show statistics for the selected area' visible={mi.imageStatistics}
                           onClick={() => stats(pv)}/>}

            {isSpacialActionsDropVisible(searchActions,pv) && <ActionsDropDownButton {...{searchActions,pv, style:{marginTop:3}}}/> }
            {makeExtensionButtons(extensionAry,pv)}
        </Stack>
        );


    const makeHipsControls= () => (
        <Fragment>
            {canConvertHF && <HipsFitsConvertButton pv={pv}/>}
            {hips && !canConvertHF && <HipsProjConvertButton pv={pv}/>}
            {hips && <Divider orientation='vertical' sx={{mx:.5}}/> }
            {hips && <HiPSCoordSelect plotId={plot?.plotId} imageCoordSys={plot?.imageCoordSys}/>}
            {hips && <Divider orientation='vertical' sx={{mx:.5}}/> }
            {hips && makeHiPSImageTable(pv)}
            {isHiPSAitoff(plot) &&
                <ToolbarButton text='Center Galactic' tip='Align Aitoff HiPS to Galactic 0,0'
                               onClick={() => dispatchChangeHiPS({
                                   plotId:plot.plotId,
                                   coordSys:CoordinateSys.GALACTIC,
                                   centerProjPt:makeWorldPt(0, 0, CoordinateSys.GALACTIC) })
                               } />
            }
        </Fragment>
    );

   const makeTbSX= (theme) => ({
       backgroundColor: ctxToolbarBG(theme,94),
       width: 1,
       position: 'relative',
       whiteSpace: 'nowrap',
       overflow: 'hidden',
       verticalAlign: 'top',
       flexWrap: 'wrap',
       alignItems: 'center',
       pr: 1.5,
   } );


    if (extraLine && showMultiImageController && showOptions) {
        return (
            <Stack direction='column'>
                <Stack {...{direction:'row', sx: makeTbSX }}>
                    <MultiImageControllerView plotView={pv} />
                </Stack>
                <Stack {...{direction:'row', sx: makeTbSX }}>
                    {makeButtons()}
                    {makeHipsControls()}
                </Stack>
            </Stack>
        );
    }
    else {
        return (
            <Stack {...{direction:'row', sx: makeTbSX }}>
                {showMultiImageController && <MultiImageControllerView plotView={pv} />}
                {showMultiImageController && showOptions && <Divider orientation='vertical' sx={{mx:.5}}  />}
                {makeButtons()}
                {makeHipsControls()}
            </Stack>
        );
    }
},
    (prevP,nextP) => {
        return (shallowequal({...prevP, plotView:undefined}, {...nextP,plotView:undefined}) &&
                pvEqualExScroll(prevP.plotView, nextP.plotView));
    }
);

VisCtxToolbarView.propTypes= {
    plotView : PropTypes.object.isRequired,
    extensionAry : PropTypes.arrayOf(PropTypes.object),
    showSelectionTools : PropTypes.bool,
    showCatSelect : PropTypes.bool,
    showCatUnSelect : PropTypes.bool,
    showFilter : PropTypes.bool,
    showClearFilter : PropTypes.bool,
    searchActions: PropTypes.arrayOf(PropTypes.object),
    width : PropTypes.number,
    showMultiImageController : PropTypes.bool
};


export const ctxToolbarBG= (theme, opacity=90) =>
    BrowserInfo.supportsCssColorMix() ?
        `color-mix(in srgb, ${theme.vars.palette.neutral.softBg} ${opacity}%, transparent)` :
        theme.vars.palette.neutral.softBg;


export function MultiImageControllerView({plotView:pv}) {

    const {plots}= pv;
    const plot= primePlot(pv);
    const image= isImage(plot);

    let cIdx;
    let length;
    let wlStr= '';
    let startStr;
    const  cube= isImageCube(plot) || !image;
    const multiHdu= isMultiHDUFits(pv);
    let hduDesc= '';
    let tooltip;

    if (image) {
        tooltip= '';
        cIdx= plots.findIndex( (p) => p.plotImageId===plot.plotImageId);
        if (cIdx<0) cIdx= 0;
        length= plots.length;
        if (multiHdu) {
            const hduNum= getHDU(plot);
            startStr= 'Image: ';
            const desc= plot.plotDesc ?? '';
            startStr= `HDU (#${hduNum}): `;
            hduDesc= `${desc || getExtName(plot) || getExtType(plot)}`;
            tooltip+= `HDU: ${hduNum} ${hduDesc?', '+hduDesc:''}`;
        }
        if (plot.cubeIdx>-1) {
            tooltip+= `${multiHdu ? ', ':''} Cube: ${plot.cubeIdx+1}/${getCubePlaneCnt(plot)}`;

            if (hasPlaneOnlyWLInfo(plot)) {
                const wl= doFormat(getPtWavelength(plot,undefined, plot.cubeIdx),4);
                const unitStr= getFormattedWaveLengthUnits(plot);
                wlStr= `${wl} ${unitStr}`;
            }
        }
        tooltip+= `, Image Count: ${cIdx+1} / ${length}`;
    }
    else {
        wlStr= getHipsCubeDesc(plot);
        cIdx= plot.cubeIdx;
        tooltip= `HiPS Cube: ${wlStr},  ${cIdx+1} / ${plot.cubeDepth+1}`;
    }
    if (cIdx<0) cIdx= 0;

    return (
        <Tooltip title={tooltip} placement='top-end'>
            <Stack {...{direction:'row', flexWrap:'wrap', alignItems:'center',  position:'relative' }}>
                {startStr && <Typography {...{level:'body-sm', width: '13em', overflow: 'hidden',
                    textOverflow: 'ellipsis', pl:1, textAlign: 'end'} }>
                    <span style={{fontStyle: 'italic', fontWeight: 'bold'}}> {startStr} </span>
                    <Typography level='body-sm' color='warning'> {hduDesc} </Typography>
                </Typography>}

                {multiHdu && <FrameNavigator {...{pv, currPlotIdx:cIdx, minForInput:4, displayType:'hdu',tooltip}} />}
                <Stack {...{direction:'row', alignItems:'center'}}>
                    {cube && multiHdu && <Divider orientation='vertical' sx={{mx:.5}}  />}
                    {cube &&
                        <Typography {...{level:'body-sm', fontWeight:'bold', fontStyle: 'italic',
                            overflow: 'hidden', textOverflow: 'ellipsis', pl: 1}}>Plane: </Typography>}
                    {wlStr && <Typography {...{level:'body-sm', color:'warning', pl:.5}}>{wlStr}</Typography>}
                    {cube && <FrameNavigator {...{pv, currPlotIdx:cIdx, minForInput:6, displayType:image?'cube':'hipsCube'}} /> }
                </Stack>
            </Stack>
        </Tooltip>
    );
}

MultiImageControllerView.propTypes= {
    plotView : PropTypes.object.isRequired,
};

const doFormat= (v,precision) => precision>0 ? sprintf(`%.${precision}f`,v) : Math.trunc(v)+'';

function getHipsCubeDesc(plot) {
    if (!isHiPS(plot)) return '';
    const {hipsProperties}= plot;
    const {data_cube_crpix3, data_cube_crval3, data_cube_cdelt3, data_cube_bunit3=''}= hipsProperties;
    if (!data_cube_crpix3 || !data_cube_crval3 || !data_cube_cdelt3) return '';
    const crpix3= Number(data_cube_crpix3);
    const crval3= Number(data_cube_crval3);
    const cdelt3= Number(data_cube_cdelt3);
    const dp= Math.abs(cdelt3)>10 ? 0 :  1- Math.trunc(Math.log10(Math.abs(cdelt3))); // number of decimal points suggestion of gpdf
    if (isNaN(crpix3) || isNaN(crval3) || isNaN(cdelt3)) return '';
    const value = crval3 + ( plot.cubeIdx - crpix3 ) * cdelt3;
    const bunit3= (data_cube_bunit3!=='null' && data_cube_bunit3!=='nil' && data_cube_bunit3!=='undefined') ?
                               data_cube_bunit3 : '';
    return `${doFormat(value,dp)} ${getFormattedWaveLengthUnits(bunit3)}`;
}



function getEmLength(len) {
   const size= Math.trunc(Math.log10(len)) + 1;
   return size>=4 ? '4em' : size+'em';
}


function FrameNavigator({pv, currPlotIdx, minForInput, displayType}) {

    const typeConvert= {
        hdu: {
            getLen: getHDUCount,
            getContextIdx: (pv,idx) => convertImageIdxToHDU(pv,idx).hduIdx,
            getPlotIdx: (pv,idx) => convertHDUIdxToImageIdx(pv,idx, 'follow'),
        },
        cube: {
            getLen: getCubePlaneCnt,
            getContextIdx: (pv,idx) => convertImageIdxToHDU(pv,idx).cubeIdx,
            getPlotIdx: (pv,idx) => convertHDUIdxToImageIdx(pv, getHDUIndex(pv, primePlot(pv)), idx)
        },
        images: {
            getLen: (pv) => pv.plots.length,
            getContextIdx: (pv,idx) => idx,
            getPlotIdx: (pv,idx) => idx
        },
        hipsCube: {
            getLen: (pv) => primePlot(pv).cubeDepth,
            getContextIdx: (pv,idx) => idx,
            getPlotIdx: (pv,idx) => idx
        }
    };


    const plot= primePlot(pv);
    const {plotId}= pv;
    const converter= typeConvert[displayType];
    const len= converter.getLen(pv);
    const currIdx= converter.getContextIdx(pv, currPlotIdx);
    const nextIdx= currIdx===len-1 ? 0 : currIdx+1;
    const prevIdx= currIdx ? currIdx-1 : len-1;

    const doDispatchChange= (idx) => {
        isImage(plot) ? dispatchChangePrimePlot({plotId,primeIdx:idx}) : dispatchChangeHiPS({plotId, cubeIdx:idx});
    };

    const changeFrameIdx= ({value,valid=true}) => {
        if (!valid || (isString(value) && value.trim()==='')) return;
        doDispatchChange(converter.getPlotIdx(pv,Number(value)-1));
    };

    const handleKeyDown= (ev) => {
        if (ev.key !== 'ArrowLeft' && ev.key !== 'ArrowRight') return;
        doDispatchChange(converter.getPlotIdx(pv,ev.key==='ArrowLeft' ? prevIdx : nextIdx));
    };

    const validator= (value) => {
        return Validate.intRange(1, len+1, 'step range', value, true);
    };

    const showNavControl= minForInput<=len;
    const currStr= `${currIdx+1}`;


    return (
        <Stack direction='row' alignItems='center' flexWrap='nowrap'>
            <BeforeButton title={'Next frame'} onClick={() => changeFrameIdx({value:prevIdx+1})}/>
            <NextButton title={'Previous fraome'} onClick={() => changeFrameIdx({value:nextIdx+1})}/>
            {showNavControl ?
                <StateInputField defaultValue={currStr} valueChange={changeFrameIdx}
                                 sx={{'& .MuiInput-root':{'minHeight':'3px', 'borderRadius':4, width:'5em'}}}
                                 tooltip={'Enter frame number to jump to, right arrow goes forward, left arrow goes back'}
                                 style={{width:getEmLength(len), textAlign:'right'}}
                                 type='number'
                                 validator={validator} onKeyDown={handleKeyDown} />
                :
                <Typography level='body-sm'>{currStr}</Typography>
            }
            <Typography level='body-sm'> {` / ${len}`} </Typography>
        </Stack>
    );
}

FrameNavigator.propTypes= {
    pv: PropTypes.object,
    currPlotIdx: PropTypes.number,
    minForInput: PropTypes.number,
    displayType: PropTypes.oneOf(['hdu', 'cube', 'images', 'hipsCube']),
    tooltip: PropTypes.string,
};
