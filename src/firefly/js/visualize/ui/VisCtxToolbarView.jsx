/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Divider, Stack, Tooltip, Typography} from '@mui/joy';
import {DropDownToolbarButton} from 'firefly/ui/DropDownToolbarButton.jsx';

import SELECTED_ZOOM from 'html/images/icons-2014/ZoomFitToSelectedSpace.png';
import {isEmpty, isString} from 'lodash';
import {arrayOf, bool, func, number, object, oneOf, string} from 'prop-types';
import React, {Fragment, memo} from 'react';
import shallowequal from 'shallowequal';
import {dispatchExtensionActivate} from '../../core/ExternalAccessCntlr.js';
import {makePlotSelectionExtActivateData} from '../../core/ExternalAccessUtils.js';
import {sprintf} from '../../externalSource/sprintf';
import {ActionsDropDownButton, isSpacialActionsDropVisible} from '../../ui/ActionsDropDownButton.jsx';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {showHiPSSurveysPopup} from '../../ui/HiPSImageSelect.jsx';
import {StateInputField} from '../../ui/StatedInputfield.jsx';
import {DropDownVerticalSeparator, ToolbarButton, ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import BrowserInfo from '../../util/BrowserInfo.js';
import Validate from '../../util/Validate.js';
import {CoordinateSys} from '../CoordSys.js';
import {getExtName, getExtType, getHeader} from '../FitsHeaderUtil.js';
import {
    dispatchChangeCenterOfProjection, dispatchChangeHiPS, dispatchChangeHipsImageConversion, dispatchChangePrimePlot,
    visRoot
} from '../ImagePlotCntlr.js';
import {PlotAttribute} from '../PlotAttribute';
import {
    canConvertBetweenHipsAndFits, convertHDUIdxToImageIdx, convertImageIdxToHDU, getActivePlotView, getCubePlaneCnt,
    getFormattedWaveLengthUnits, getHDU, getHDUCount, getHDUIndex, getPlotViewById, getPtWavelength, hasPlaneOnlyWLInfo,
    isImageCube, isMultiHDUFits, primePlot, pvEqualExScroll,
} from '../PlotViewUtil.js';
import {makeWorldPt} from '../Point.js';
import {convertToHiPS, convertToImage, doHiPSImageConversionIfNecessary} from '../task/PlotHipsTask.js';
import {isHiPS, isHiPSAitoff, isImage} from '../WebPlot.js';
import {
    BeforeButton, CenterOnSelection, CheckedButton, CheckedClearButton, CropButton, FilterAddButton, FiltersOffButton,
    NextButton, StatsButton
} from './Buttons.jsx';
import {
    clearFilterDrawingLayer, crop, filterDrawingLayer, recenterToSelection, selectDrawingLayer, stats,
    unselectDrawingLayer, zoomIntoSelection
} from './CtxToolbarFunctions';

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


function doHiPSFitsConvert(pv,target) {
    if (!canConvertBetweenHipsAndFits(pv)) return;
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

function HipsOptionsDropdown({pv}) {
    const AITOFF_TEXT= 'Aitoff';
    const SPHER_TEXT= 'Spherical';
    const plot= primePlot(pv);
    if (!plot) return undefined;
    const {plotId}= plot;
    const csysText= plot.imageCoordSys===CoordinateSys.GALACTIC ? 'Gal' : 'Equ';
    const projText= isHiPSAitoff(plot) ? AITOFF_TEXT : SPHER_TEXT;

    // const canConvertHF= canConvertBetweenHipsAndFits(pv);
    // const text= canConvertHF ? 'Coverage Options' : `${csysText} / ${projText}`;
    const text= `${csysText} / ${projText}`;


    const dropDown= (
        <SingleColumnMenu sx={ (theme) => ({
            background:ctxToolbarBG(theme, 98),
        })}>
            {isHiPS(plot) &&
                <>
                    <Tooltip title='Choose the orientation of the HiPS all-sky image'>
                        <Typography sx={{whiteSpace:'nowrap'}}>Orientation</Typography>
                    </Tooltip>
                    <ToolbarButton text='Galactic' tip='Use Galactic coordinate system' key={'gal'}
                                   hasCheckBox={true}
                                   checkBoxOn={plot.imageCoordSys===CoordinateSys.GALACTIC}
                                   onClick={()=>dispatchChangeHiPS( {plotId,  coordSys: CoordinateSys.GALACTIC})}/>
                    <ToolbarButton text='Eq J2000' tip='Use Equatorial J2000 coordinate system' key={'eqj'}
                                   hasCheckBox={true}
                                   checkBoxOn={plot.imageCoordSys===CoordinateSys.EQ_J2000}
                                   onClick={()=>dispatchChangeHiPS( {plotId,  coordSys: CoordinateSys.EQ_J2000})}/>
                    <DropDownVerticalSeparator useLine={true}/>
                    <ToolbarButton text='Center Galactic' tip='Align Aitoff HiPS to Galactic 0,0'
                                   sx={{button:{px:0}}}
                                   onClick={() => dispatchChangeHiPS({
                                       plotId:pv.plotId,
                                       coordSys:CoordinateSys.GALACTIC,
                                       centerProjPt:makeWorldPt(0, 0, CoordinateSys.GALACTIC) })
                                   } />
                    <DropDownVerticalSeparator useLine={true}/>
                    <Tooltip title='Choose the projection for the all-sky HiPS image'>
                        <Typography>Projection</Typography>
                    </Tooltip>
                    <ToolbarButton {...{
                        hasCheckBox: true,
                        checkBoxOn: !isHiPSAitoff(plot),
                        key: 'change Hips',
                        text: SPHER_TEXT,
                        tip: 'All-sky multi-resolution image with spherical projection, up to 180 degrees',
                        onClick: () => dispatchChangeCenterOfProjection({plotId: pv.plotId, fullSky: false})
                    }}/>
                    <ToolbarButton {...{
                        hasCheckBox: true,
                        checkBoxOn: isHiPSAitoff(plot),
                        key: 'change aitoff',
                        text: AITOFF_TEXT,
                        tip: 'All-sky multi-resolution image with Aitoff projection, up to 360 degrees',
                        onClick: () => dispatchChangeCenterOfProjection({plotId: pv.plotId, fullSky: true})
                    }}/>
                </>
            }
        </SingleColumnMenu>
    );


    return (
        <DropDownToolbarButton {...{
            sx:{button:{minWidth: '11rem', px:0}},
            text,
            tip:'Change image orientation and projection',
            useDropDownIndicator:true, dropDown}} />
    );


}

function HiPSDataSelect({pv}) {
    const plot= primePlot(pv);
    if (!plot) return undefined;
    const mi= pv.plotViewCtx.menuItemKeys;
    const imageTitle= pv.plotViewCtx.hipsImageConversion?.imageRequestRoot?.getTitle()  ?? '';
    const autoTipStart= 'Auto-transition between FITS and HiPS depending on zoom:';
    const canConvertHF= canConvertBetweenHipsAndFits(pv);
    const {autoConvertOnZoom:auto=false}= pv.plotViewCtx.hipsImageConversion ?? {};
    // const text= canConvertHF ? 'Coverage Options' : `${csysText} / ${projText}`;

    const dropDown= (
        <SingleColumnMenu sx={ (theme) => ({
            background:ctxToolbarBG(theme, 98),
        })}>
            {isHiPS(plot) && <>
                <Typography sx={{whiteSpace:'nowrap'}}>Data Options</Typography>
                <ToolbarButton text='Change HiPS' tip='Choose a different HiPS (all-sky multi-resolution) image'
                               key='change Hips'
                               hasCheckBox={true}
                               visible={mi.hipsSurveyPopup}
                               onClick={()=>showHiPSSurveysPopup(pv)} />
                <ToolbarButton text='Add MOC Layer' tip='Add a new MOC layer to the HiPS Survey'
                               key='add'
                               hasCheckBox={true}
                               visible={mi.mocLayerPopup}
                               onClick={()=>showHiPSSurveysPopup(pv,true)} />
            </>
            }
            {canConvertHF  && isHiPS(plot) && <DropDownVerticalSeparator useLine={true}/>}
            {canConvertHF && <>
                <Typography sx={{whiteSpace:'nowrap'}}>HiPS to FITS Conversion</Typography>
                <ToolbarButton {...{
                    hasCheckBox: true, checkBoxOn: auto, key: 'autoFITS',
                    text: isHiPS(plot) ? `Auto Zoom-in to ${imageTitle} FITS` : 'Auto Zoom-out to HiPS',
                    tip: isHiPS(plot) ?
                        `${autoTipStart} Switch to ${imageTitle} FITS image at current view center; coverage extent will be limited` :
                        `${autoTipStart} Switch to All-Sky (HiPS) image`,
                    onClick: () => changeAutoConvert(pv, !auto)
                }}/>
                <ToolbarButton {...{
                    hasCheckBox: true, key: 'toFITS',
                    text: isHiPS(plot) ? `Switch to ${imageTitle} FITS image` : 'Switch to HiPS',
                    tip: isHiPS(plot) ?
                        `Switch to ${imageTitle} FITS image at current view center; coverage extent will be limited` :
                        'Switch to All-Sky (HiPS) image',
                    onClick: () => doHiPSFitsConvert(pv,isHiPS(plot) ? 'fits' : 'sin')
                }}/>

            </>}
        </SingleColumnMenu>
    );

    return (
        <DropDownToolbarButton {...{
            text: canConvertHF ? 'HiPS / FITS / MOC' : 'HiPS / MOC',
            tip:'Select all-sky images and data collection coverage maps',
            sx:{button:{px:0}}, useDropDownIndicator:true, dropDown }}/>
    );
}

function HipsControls({pv})  {
    const plot= primePlot(pv);
    const hips= isHiPS(plot);
    const convert= canConvertBetweenHipsAndFits(pv);
    if (!hips && !convert) return;
    return (
        <Stack pl={1} direction='row' alignItems='center' flexWrap='wrap' divider={<ToolbarHorizontalSeparator/>}>
            { (hips|| convert) && <HiPSDataSelect pv={pv}/>}
            { (hips) && <HipsOptionsDropdown pv={pv}/>}
            { (hips || convert) && <></>}
        </Stack>
    );
}

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
        showMultiImageController=false,
        makeToolbar}= props;


    const extraLine= showMultiImageController && width<350;
    const plot= primePlot(pv);
    const image= isImage(plot);
    const mi= pv?.plotViewCtx.menuItemKeys;
    const showOptions= showSelectionTools|| showCatSelect|| showCatUnSelect ||
        showFilter || showClearFilter || !isEmpty(extensionAry) || isHiPS(plot) || canConvertBetweenHipsAndFits(pv);

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



   const makeTbSX= (theme) => ({
       backgroundColor: ctxToolbarBG(theme,94),
       width: 1,
       position: 'relative',
       whiteSpace: 'nowrap',
       overflow: 'hidden',
       verticalAlign: 'top',
       flexWrap: 'wrap',
       alignItems: 'center',
       pr: pv?.plotViewCtx?.userCanDeletePlots ? 1.5 : .25,
   } );


    if (extraLine && showMultiImageController && showOptions) {
        return (
            <Stack direction='column'>
                <Stack {...{direction:'row', sx: makeTbSX }}>
                    <MultiImageControllerView plotView={pv} />
                </Stack>
                <Stack {...{direction:'row', sx: makeTbSX }}>
                    <HipsControls pv={pv}/>
                    {makeButtons()}
                </Stack>
                {pv.pv}
            </Stack>
        );
    }
    else {
        return (
            <Stack {...{direction:'row', justifyContent:'space-between',  sx: makeTbSX }}>
                <Stack {...{direction:'row', alignItems: 'center',flexWrap: 'wrap',}}>
                    {showMultiImageController && <MultiImageControllerView plotView={pv} />}
                    {showMultiImageController && showOptions && <Divider orientation='vertical' sx={{m:.5}}  />}
                    <HipsControls pv={pv}/>
                    {makeButtons()}
                </Stack>
                {pv.plotViewCtx.embedMainToolbar && makeToolbar?.()}
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
    plotView : object.isRequired,
    extensionAry : arrayOf(object),
    showSelectionTools : bool,
    showCatSelect : bool,
    showCatUnSelect : bool,
    showFilter : bool,
    showClearFilter : bool,
    searchActions: arrayOf(object),
    width : number,
    showMultiImageController : bool,
    makeToolbar: func
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
            const {HDU_TITLE_DESC, HDU_TITLE_HEADER}= PlotAttribute;
            const {attributes:att, plotDesc:desc=''}= plot;
            const hduNum= getHDU(plot);
            startStr= 'Image: ';
            startStr= att[HDU_TITLE_DESC] ? att[HDU_TITLE_DESC] + ': ': `HDU (#${hduNum}): `;

            const hduTitleHeader= att[HDU_TITLE_HEADER] ;
            const reqHeaderTitle= hduTitleHeader ? `${getHeader(plot,hduTitleHeader)}` : '';
            const reqHduInfo= (att[HDU_TITLE_DESC] && reqHeaderTitle) ? `${att[HDU_TITLE_DESC]}: ${reqHeaderTitle}, ` : '';
            const nameOrType= getExtName(plot) || getExtType(plot);
            hduDesc= `${desc || reqHeaderTitle || nameOrType}`;
            tooltip+= `${reqHduInfo}HDU: ${hduNum} ${nameOrType?', '+hduDesc:''}`;
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
                {startStr && <Typography {...{
                    component:'div',
                    level:'body-sm', width: '13em', overflow: 'hidden',
                    textOverflow: 'ellipsis', pl:1, textAlign: 'end'} }>
                    <div/>
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
    plotView : object.isRequired,
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
            <NextButton title={'Previous frame'} onClick={() => changeFrameIdx({value:nextIdx+1})}/>
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
    pv: object,
    currPlotIdx: number,
    minForInput: number,
    makeToolbar: func,
    displayType: oneOf(['hdu', 'cube', 'images', 'hipsCube']),
    tooltip: string,
};
