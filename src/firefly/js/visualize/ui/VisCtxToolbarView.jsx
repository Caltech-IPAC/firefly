/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo,Fragment} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, isString} from 'lodash';
import shallowequal from 'shallowequal';
import {sprintf} from '../../externalSource/sprintf';
import {
    convertHDUIdxToImageIdx, convertImageIdxToHDU, getActivePlotView, getCubePlaneCnt, getFormattedWaveLengthUnits,
    getHDU, getHDUCount, getHDUIndex, getPlotViewById, getPtWavelength, hasPlaneOnlyWLInfo, isImageCube, isMultiHDUFits,
    primePlot, pvEqualExScroll,
} from '../PlotViewUtil.js';
import {getExtName, getExtType} from '../FitsHeaderUtil.js';
import {makeWorldPt} from '../Point.js';
import {isHiPS, isHiPSAitoff, isImage} from '../WebPlot.js';
import {ToolbarButton, ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView.jsx';
import {dispatchExtensionActivate} from '../../core/ExternalAccessCntlr.js';
import {
    dispatchChangeCenterOfProjection, dispatchChangeHiPS, dispatchChangeHipsImageConversion, dispatchChangePrimePlot,
    visRoot
} from '../ImagePlotCntlr.js';
import {makePlotSelectionExtActivateData} from '../../core/ExternalAccessUtils.js';
import {ListBoxInputFieldView} from '../../ui/ListBoxInputField';
import {showHiPSSurveysPopup} from '../../ui/HiPSImageSelect.jsx';
import {StateInputField} from '../../ui/StatedInputfield.jsx';
import Validate from '../../util/Validate.js';
import {CoordinateSys} from '../CoordSys.js';
import {convertToHiPS, convertToImage, doHiPSImageConversionIfNecessary} from '../task/PlotHipsTask.js';
import {
    clearFilterDrawingLayer, crop, filterDrawingLayer, recenterToSelection, selectDrawingLayer, stats,
    unselectDrawingLayer, zoomIntoSelection
} from './CtxToolbarFunctions';
import {isSpacialActionsDropVisible, ActionsDropDownButton} from '../../ui/ActionsDropDownButton.jsx';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {DropDownToolbarButton} from 'firefly/ui/DropDownToolbarButton.jsx';

import CROP from 'html/images/icons-2014/24x24_Crop.png';
import STATISTICS from 'html/images/icons-2014/24x24_Statistics.png';
import SELECTED from 'html/images/icons-2014/24x24_Checkmark.png';
import UNSELECTED from 'html/images/icons-2014/24x24_CheckmarkOff_Circle.png';
import FILTER from 'html/images/icons-2014/24x24_FilterAdd.png';
import CLEAR_FILTER from 'html/images/icons-2014/24x24_FilterOff_Circle.png';
import PAGE_RIGHT from 'html/images/icons-2014/20x20_PageRight.png';
import PAGE_LEFT from 'html/images/icons-2014/20x20_PageLeft.png';
import SELECTED_ZOOM from 'html/images/icons-2014/ZoomFitToSelectedSpace.png';
import SELECTED_RECENTER from 'html/images/icons-2014/RecenterImage-selection.png';

const image24x24={width:24, height:24};


function makeExtensionButtons(extensionAry,pv) {
    if (!extensionAry) return false;
    return extensionAry.map( (ext,idx) => {
            return (
                <ToolbarButton icon={ext.imageUrl} text={ext.title}
                               tip={ext.toolTip} key={ext.id} shortcutKey={ext.shortcutKey}
                               horizontal={true} enabled={true} useBorder={true}
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
        <div style={{display: 'flex', alignItems: 'center', padding: '1px 2px 1px 2px', margin: '0 5px 0 5px',
            border: '1px solid rgba(60,60,60,.2)', borderRadius: '5px'}}>
            <RadioGroupInputFieldView options={defHFOptions}  value={value} buttonGroup={true}
                                      onChange={(ev) => doHiPSFitsConvert(pv,ev.target.value)} />
            <div style={{paddingLeft: 3, display:'flex', alignItems:'center'}} title={buttonGroupTip}>
                <input type='checkbox' checked={auto} onChange={() => changeAutoConvert(pv, !auto)} />
                Auto
            </div>
        </div>
    );
}

HipsFitsConvertButton.propTypes= { pv : PropTypes.object.isRequired};



function HipsProjConvertButton({pv}) {

    const plot= primePlot(pv);
    if (!plot) return undefined;
    const value= isHiPSAitoff(plot) ? 'aitoff' : 'sin';
    const buttonGroupTip= 'Change HiPS projection between spherical (180 deg) and Aitoff (360 deg)';
    return (
        <div style={{display: 'flex', alignItems: 'center', padding: '1px 2px 1px 2px', margin: '0 5px 0 5px',
            border: '1px solid rgba(60,60,60,.2)', borderRadius: '5px'}}>
            <RadioGroupInputFieldView options={projOptions}  value={value} buttonGroup={true} title={buttonGroupTip}
                                      onChange={(ev) =>
                                          dispatchChangeCenterOfProjection({plotId:pv.plotId, fullSky:ev.target.value==='aitoff'})}
            />
        </div>
    );
}

function makeHiPSImageTable(pv) {
    if (!primePlot(pv)) return null;
    const mi= pv.plotViewCtx.menuItemKeys;

    const dropDown= (
        <SingleColumnMenu>
            <ToolbarButton text={'Change HiPS'} tip={'Choose a different HiPS Survey'}
                           horizontal={false} key={'change Hips'}
                           visible={mi.hipsSurveyPopup}
                           onClick={()=>showHiPSSurveysPopup(pv)} />
            <ToolbarButton text={'Add MOC Layer'} tip={'Add a new MOC layer to the HiPS Survey'}
                           horizontal={false} key={'add'}
                           visible={mi.mocLayerPopup}
                           onClick={()=>showHiPSSurveysPopup(pv,true)} />
        </SingleColumnMenu>
    );

    return (
        <div style={{display:'flex'}}>
                <DropDownToolbarButton
                    text={'HiPS / MOC'} tip='Change displayed HiPS or add a MOC'
                    horizontal={true} style={{margin: '0 5px 0 4px'}}
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
            <ListBoxInputFieldView
                inline={true} value={selectedIdx}
                onChange={(ev) => dispatchChangeHiPS( {plotId,  coordSys: hipsCoordOptions[Number(ev.target.value)].c})}
                labelWidth={0} label={' '} tooltip={ 'Change HiPS survey coordinate system'}
                options={hipsCoordOptions} multiple={false}
            />
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

    const rS= {
        width: '100%',
        display:'flex',
        height: 28,
        position: 'relative',
        verticalAlign: 'top',
        whiteSpace: 'nowrap',
        flexDirection:'row',
        flexWrap:'nowrap',
        background:'rgba(227, 227, 227, .8)',
        overflow: 'hidden',
        alignItems: 'center',
    };

    const extraLine= showMultiImageController && width<350;
    const plot= primePlot(pv);
    const image= isImage(plot);
    const canConvertHF= canConvertHipsAndFits(pv);
    const hips= isHiPS(plot);
    const mi= pv?.plotViewCtx.menuItemKeys;
    const showOptions= showSelectionTools|| showCatSelect|| showCatUnSelect ||
        showFilter || showClearFilter || !isEmpty(extensionAry) || hips || canConvertHF;

    const makeButtons= () => (
        <Fragment>

            {showSelectionTools && image &&
            <ToolbarButton icon={CROP} tip='Crop the image to the selected area'
                           imageStyle={image24x24}
                           visible={mi.crop}
                           horizontal={true} onClick={() => crop(pv)}/>}


            {showCatSelect &&
            <ToolbarButton icon={SELECTED} tip='Mark data in area as selected'
                           imageStyle={image24x24}
                           visible={mi.selectTableRows}
                           horizontal={true} onClick={() => selectDrawingLayer(pv)}/>}

            {showCatUnSelect &&
            <ToolbarButton icon={UNSELECTED} tip='Mark all data unselected'
                           imageStyle={image24x24}
                           visible={mi.unselectTableRows}
                           horizontal={true} onClick={() => unselectDrawingLayer(pv)}/>}

            {showFilter &&
            <ToolbarButton icon={FILTER} tip='Filter in the selected area'
                           imageStyle={image24x24}
                           visible={mi.filterTableRows}
                           horizontal={true} onClick={() => filterDrawingLayer(pv)}/>}

            {showClearFilter &&
            <ToolbarButton icon={CLEAR_FILTER} tip='Clear all the Filters'
                           imageStyle={image24x24}
                           visible={mi.clearTableFilters}
                           horizontal={true} onClick={() => clearFilterDrawingLayer(pv)}/>}

            {showSelectionTools &&
            <ToolbarButton icon={SELECTED_ZOOM} tip='Zoom to fit selected area'
                           imageStyle={image24x24}
                           horizontal={true}
                           visible={mi.zoomToSelection}
                           onClick={() => zoomIntoSelection(pv)}/>}

            { showSelectionTools &&
            <ToolbarButton icon={SELECTED_RECENTER} tip='Recenter image to selected area'
                           imageStyle={image24x24}
                           visible={mi.recenterToSelection}
                           horizontal={true} onClick={() => recenterToSelection(pv)}/>}

            {showSelectionTools && image &&
            <ToolbarButton icon={STATISTICS} tip='Show statistics for the selected area'
                           imageStyle={image24x24}
                           visible={mi.imageStatistics}
                           horizontal={true} onClick={() => stats(pv)}/>}

            {isSpacialActionsDropVisible(searchActions,pv) && <ActionsDropDownButton {...{searchActions,pv}}/> }
            {makeExtensionButtons(extensionAry,pv)}
        </Fragment>
        );


    const makeHipsControls= () => (
        <Fragment>
            {canConvertHF && <HipsFitsConvertButton pv={pv}/>}
            {hips && !canConvertHF && <HipsProjConvertButton pv={pv}/>}
            {hips && <HiPSCoordSelect plotId={plot?.plotId} imageCoordSys={plot?.imageCoordSys}/>}
            {hips && makeHiPSImageTable(pv)}
            {isHiPSAitoff(plot) &&
                <ToolbarButton text='Center Galactic' tip='Align Aitoff HiPS to Galactic 0,0'
                               horizontal={true}
                               onClick={() => dispatchChangeHiPS({
                                   plotId:plot.plotId,
                                   coordSys:CoordinateSys.GALACTIC,
                                   centerProjPt:makeWorldPt(0, 0, CoordinateSys.GALACTIC) })
                               } />
            }
        </Fragment>
    );

    if (extraLine && showMultiImageController && showOptions) {
        return (
            <div style={{display:'flex', flexDirection:'column'}}>
                <div style={rS}>
                    <MultiImageControllerView plotView={pv} />
                </div>
                <div style={rS}>
                    {makeButtons()}
                    {makeHipsControls()}
                </div>
            </div>
        );
    }
    else {
        return (
            <div style={rS}>
                {showMultiImageController && <MultiImageControllerView plotView={pv} />}
                {showMultiImageController && showOptions && <ToolbarHorizontalSeparator/>}
                {makeButtons()}
                {makeHipsControls()}
            </div>
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


const leftImageStyle= {
    cursor:'pointer',
    paddingLeft: 3
};

const mulImStyle= {
    display:'inline-flex',
    height: 28,
    position: 'relative',
    verticalAlign: 'top',
    whiteSpace: 'nowrap',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center',
};


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
        <div style={mulImStyle} title={tooltip}>
            {startStr && <div style={{
                width: '13em', overflow: 'hidden',
                textOverflow: 'ellipsis', padding: '0 0 0 5px', textAlign: 'end'} }>
                <span style={{fontStyle: 'italic', fontWeight: 'bold'}}> {startStr} </span>
                <span> {hduDesc} </span>
            </div>}

            {multiHdu && <FrameNavigator {...{pv, currPlotIdx:cIdx, minForInput:4, displayType:'hdu',tooltip}} />}
            {cube && multiHdu && <ToolbarHorizontalSeparator style={{height: 20}}/>}
            {cube &&
            <div style={{
                fontStyle: 'italic', fontWeight: 'bold',
                overflow: 'hidden', textOverflow: 'ellipsis', padding: '0 0 0 5px'}
            }> {'Plane: '} </div> }
            {wlStr && <div style={{paddingLeft: 6}}>{wlStr}</div>}
            {cube && <FrameNavigator {...{pv, currPlotIdx:cIdx, minForInput:6, displayType:image?'cube':'hipsCube',tooltip}} /> }
        </div>
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

function getEmLength(len) {
   const size= Math.trunc(Math.log10(len)) + 1;
   return size>=4 ? '4em' : size+'em';
}


function FrameNavigator({pv, currPlotIdx, minForInput, displayType, tooltip}) {

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
        <div title= {tooltip}
             style={{ display:'inline-flex', flexDirection:'row', flexWrap:'nowrap', alignItems: 'center', }}>
            <img title= {tooltip} style={leftImageStyle} src={PAGE_LEFT}
                                      onClick={() => changeFrameIdx({value:prevIdx+1}) }/>
            <img title= {tooltip} style={{verticalAlign:'bottom', cursor:'pointer', paddingRight:4}} src={PAGE_RIGHT}
                                       onClick={() => changeFrameIdx({value:nextIdx+1})} />

            {showNavControl ? <StateInputField defaultValue={currStr} valueChange={changeFrameIdx} labelWidth={0} label={''}
                                tooltip={`Enter frame number to jump to, right arrow goes forward, left arrow goes back\n${tooltip}`}
                                showWarning={false} style={{width:getEmLength(len), textAlign:'right'}}
                                validator={validator} onKeyDown={handleKeyDown} />
                                : currStr}
            {` / ${len}`}
        </div>
    );
}

FrameNavigator.propTypes= {
    pv: PropTypes.object,
    currPlotIdx: PropTypes.number,
    minForInput: PropTypes.number,
    displayType: PropTypes.oneOf(['hdu', 'cube', 'images', 'hipsCube']),
    tooltip: PropTypes.string,
};
