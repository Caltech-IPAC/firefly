/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {memo, useEffect, useRef, useState} from 'react';
import PropTypes from 'prop-types';
import {omit} from 'lodash';
import shallowequal from 'shallowequal';
import {
    dispatchChangeActivePlotView, dispatchChangeExpandedMode, dispatchChangeHiPS, dispatchFlip,
    dispatchOverlayColorLocking, dispatchRestoreDefaults, dispatchRotate, ExpandType, visRoot
} from '../ImagePlotCntlr.js';
import {dispatchComponentStateChange, getComponentState} from 'firefly/core/ComponentCntlr.js';
import {dispatchSetLayoutMode, LO_MODE, LO_VIEW} from 'firefly/core/LayoutCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {
    getAllDrawLayersForPlot, getActivePlotView, primePlot, hasWCSProjection,
    findPlotGroup, hasOverlayColorLock, isImageCube, isThreeColor, pvEqualExScroll
} from '../PlotViewUtil.js';
import {isHiPS} from 'firefly/visualize/WebPlot.js';
import {getPreference} from '../../core/AppDataCntlr.js';
import {ImageCenterDropDown, TARGET_LIST_PREF} from './ImageCenterDropDown.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import CoordinateSys from 'firefly/visualize/CoordSys.js';
import {ToolbarButton, ToolbarHorizontalSeparator} from 'firefly/ui/ToolbarButton.jsx';
import {showFitsDownloadDialog} from 'firefly/ui/FitsDownloadDialog.jsx';
import {ZoomButton, ZoomType} from 'firefly/visualize/ui/ZoomButton.jsx';
import {DropDownToolbarButton} from 'firefly/ui/DropDownToolbarButton.jsx';
import {ColorTableDropDownView, showColorDialog} from 'firefly/visualize/ui/ColorTableDropDownView.jsx';
import {StretchDropDownView} from 'firefly/visualize/ui/StretchDropDownView.jsx';
import {showFitsRotationDialog} from 'firefly/ui/FitsRotationDialog.jsx';
import {SimpleLayerOnOffButton} from 'firefly/visualize/ui/SimpleLayerOnOffButton.jsx';
import {getSelectedAreaIcon, SelectAreaDropDownView} from 'firefly/visualize/ui/SelectAreaDropDownView.jsx';
import {MarkerDropDownView} from 'firefly/visualize/ui/MarkerDropDownView.jsx';
import {showRegionFileUploadPanel} from 'firefly/visualize/region/RegionFileUploadView.jsx';
import {showMaskDialog} from 'firefly/visualize/ui/MaskAddPanel.jsx';
import {MatchLockDropDown} from 'firefly/visualize/ui/MatchLockDropDown.jsx';
import {HelpIcon} from 'firefly/ui/HelpIcon.jsx';
import {RotateType} from 'firefly/visualize/PlotState.js';
import {showImageSelPanel} from 'firefly/visualize/ui/ImageSearchPanelV2.jsx';
import {findUnactivatedRelatedData} from 'firefly/visualize/RelatedDataUtil.js';
import {showDrawingLayerPopup} from 'firefly/visualize/ui/DrawLayerPanel.jsx';
import {SingleColumnMenu} from 'firefly/ui/DropDownMenu.jsx';
import {wrapResizer} from '../../ui/SizeMeConfig.js';
import {endExtraction, LINE, POINTS, showExtractionDialog, Z_AXIS} from 'firefly/visualize/ui/ExtractionDialog.jsx';

import SAVE from 'images/icons-2014/Save.png';
import NEW_IMAGE from 'images/icons-2014/28x28_FITS_NewImage.png';
import COLOR from 'images/icons-2014/28x28_ColorPalette.png';
import STRETCH from 'images/icons-2014/28x28_Log.png';
import ROTATE from 'images/icons-2014/Rotate.png';
import ROTATE_NORTH_ON from 'images/icons-2014/RotateToNorth-ON.png';
import ROTATE_NORTH_OFF from 'images/icons-2014/RotateToNorth.png';
import FLIP_Y_ON from 'images/icons-2014/Mirror-ON.png';
import FLIP_Y from 'images/icons-2014/Mirror.png';
import SelectArea from 'firefly/drawingLayers/SelectArea.js';
import DistanceTool from 'firefly/drawingLayers/DistanceTool.js';
import DIST_ON from 'images/icons-2014/Measurement-ON.png';
import DIST_OFF from 'images/icons-2014/Measurement.png';
import MARKER from 'images/icons-2014/MarkerCirclesIcon_28x28.png';
import NorthUpCompass from 'firefly/drawingLayers/NorthUpCompass.js';
import COMPASS_ON from 'images/icons-2014/28x28_CompassON.png';
import COMPASS_OFF from 'images/icons-2014/28x28_Compass.png';
import WebGrid from 'firefly/drawingLayers/WebGrid.js';
import GRID_ON from 'images/icons-2014/GreenGrid-ON.png';
import GRID_OFF from 'images/icons-2014/GreenGrid.png';
import DS9_REGION from 'images/icons-2014/DS9.png';
import MASK from 'images/mask_28x28.png';
import RESTORE from 'images/icons-2014/RevertToDefault.png';
import LOCKED from 'images/OverlayLocked.png';
import UNLOCKED from 'images/OverlayUnlocked.png';
import FITS_HEADER from 'images/icons-2014/28x28_FITS_Information.png';
import LAYER_ICON from 'images/icons-2014/TurnOnLayers.png';
import ZOOM_DROP from 'images/Zoom-drop.png';
import TOOL_DROP from 'images/tools-again-try2.png';
import GRID_EXPAND from 'images/icons-2014/24x24_ExpandArrows-grid-3.png';
import OUTLINE_EXPAND from 'images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
import DRILL_DOWN from 'images/drill-down.png';
import LINE_EXTRACTION from 'images/line-extract.png';
import POINT_EXTRACTION from 'images/points.png';
import {showPlotInfoPopup} from 'firefly/visualize/ui/PlotInfoPopup.js';
import {getDefMenuItemKeys} from 'firefly/visualize/MenuItemKeys.js';

const omList= ['plotViewAry'];
const image24x24={width:24, height:24};
const emptyModalEndInfo= {f:undefined,s:undefined};

export function closeToolbarModalLayers() {
    const modalEndInfo= getComponentState('ModalEndInfo', emptyModalEndInfo);
    modalEndInfo?.f?.();
}

/**
 * Return a new State if some values have changed in the store. If critical check show nothing has changed
 * return the old state. If the old state if returned the component will not update.
 * Check if any of the following changed.
 *  - drawing layer count
 *  - the part of visRoot that is not the plotViewAry
 *  - active plot id
 *  - the plotViewCtx in the active plot view
 *  This is a optimization so that the toolbar does not re-render every time the the plot scrolls
 *  @param {Object} oldState
 */
function getStoreState(oldState) {
    const vr= visRoot();
    const {activePlotId}= vr;
    const newPv= getActivePlotView(vr);
    const recentTargetAry= getPreference(TARGET_LIST_PREF, []);
    const dlCount= activePlotId ?
            getAllDrawLayersForPlot(getDlAry(),activePlotId).length + (newPv?.overlayPlotViews?.length??0)  : 0;
    const modalEndInfo= getComponentState('ModalEndInfo', emptyModalEndInfo);

    const newState= {visRoot:vr, dlCount, recentTargetAry, modalEndInfo};
    if (!oldState) return newState;

       // -- if old state is passed, then do some comparisons to see if the state needs to update updated
    const tAryIsEqual= shallowequal(recentTargetAry, oldState?.recentTargetAry);

    if (vr===oldState.visRoot && dlCount===oldState.dlCount && tAryIsEqual && modalEndInfo===oldState.modalEndInfo) return oldState; // nothing has changed

    let needsUpdate= dlCount!==oldState.dlCount || !tAryIsEqual || activePlotId!==oldState.visRoot.activePlotId;
    if (!needsUpdate) needsUpdate= modalEndInfo!==oldState.modalEndInfo;
    if (!needsUpdate) needsUpdate= !shallowequal(omit(vr,omList),omit(oldState.visRoot,omList));

    const oldPv= getActivePlotView(oldState.visRoot);
    if (!needsUpdate) needsUpdate= !pvEqualExScroll(oldPv,newPv);
    
    return (needsUpdate) ? newState : oldState;
}

export const VisMiniToolbar = memo( ({style, manageExpand=true, expandGrid=false}) => {
    const {visRoot,dlCount, recentTargetAry, modalEndInfo} = useStoreConnector(getStoreState);
    const setModalEndInfo= (info) => dispatchComponentStateChange('ModalEndInfo',  {...emptyModalEndInfo, ...info});

    return (
        <VisMiniTBWrapper {...{visRoot, dlCount, style, recentTargetAry,
                          manageExpand, expandGrid, modalEndInfo, setModalEndInfo}} />
    );
});

VisMiniToolbar.propTypes= {
    style : PropTypes.object,
    manageExpand : PropTypes.bool,
    expandGrid: PropTypes.bool
};

const rS= {
    width: 'calc(100% - 2px)',
    height: 34,
    display: 'flex',
    position: 'relative',
    flexWrap:'nowrap',
    flexDirection: 'row',
    justifyContent: 'flex-end'
};

const VisMiniTBWrapper= wrapResizer(
    ({visRoot, dlCount, style= {}, size:{width}, manageExpand, expandGrid, modalEndInfo, setModalEndInfo}) => (
        <div style={{...rS, ...style}} className='disable-select' >
            <VisMiniToolbarView {...{visRoot, dlCount, availableWidth:width,
                                manageExpand, expandGrid,modalEndInfo, setModalEndInfo}} />
        </div>
    ));


const VisMiniToolbarView= memo( ({visRoot,dlCount,availableWidth, manageExpand, expandGrid,
                                            modalEndInfo, setModalEndInfo}) => {
    const {apiToolsView}= visRoot;
    const {current:divref}= useRef({element:undefined});
    const [colorDrops,setColorDrops]= useState(true);

    useEffect(() => {
        if (divref.element) {
            const rect= divref.element.getBoundingClientRect();
            if (!rect) return;
            setColorDrops(Boolean(window.innerHeight-rect.bottom>560));
        }
        return () => {
            const modalEndInfo= getComponentState('ModalEndInfo', emptyModalEndInfo);
            if (modalEndInfo.offOnNewPlot) closeToolbarModalLayers();
        };
    }, []);

    const pv= getActivePlotView(visRoot);
    const plot= primePlot(pv);
    const image= !isHiPS(plot);
    const hips= isHiPS(plot);
    const plotGroupAry= visRoot.plotGroupAry;
    const mi= pv?.plotViewCtx.menuItemKeys ?? getDefMenuItemKeys();
    const enabled= Boolean(plot);
    const isExpanded= visRoot.expandedMode!==ExpandType.COLLAPSE;
    const farLeftButtonEnabled= mi.overlayColorLock && mi.matchLockDropDown && mi.expand;

    let showRotateLocked= false;
    if (plot) showRotateLocked = image ? pv.plotViewCtx.rotateNorthLock : plot.imageCoordSys===CoordinateSys.EQ_J2000;
    const unavailableCnt= getUnavailableCnt(availableWidth, mi, apiToolsView,image, isExpanded, manageExpand);
    const rS= { display: 'flex', alignItems:'center', position: 'relative', whiteSpace: 'nowrap' };

    return (
        <div style={rS} ref={ (c) => divref.element=c}>


            {modalEndInfo.f  && modalEndInfo.s &&
                <React.Fragment>
                    <ToolbarButton
                        style={{color:'#f60a0a'}}
                        text={modalEndInfo.s} tip={modalEndInfo.s}
                        horizontal={true} onClick={modalEndInfo.f}/>
                    <ToolbarHorizontalSeparator/>
                </React.Fragment>
            }


            {apiToolsView && <ToolbarButton icon={NEW_IMAGE} tip='Select a new image'
                                            horizontal={true} visible={mi.imageSelect} onClick={showImagePopup}/>}

            <DropDownToolbarButton icon={TOOL_DROP} tip='Tools drop down' enabled={enabled} horizontal={true}
                                   imageStyle={image24x24}
                                   dropDown={<ToolsDrop pv={pv} mi={mi} image={image} hips={hips} visRoot={visRoot}
                                                        setModalEndInfo={setModalEndInfo}
                                                        modalEndInfo={modalEndInfo}
                                                        plot={plot} unavailableCnt={unavailableCnt}
                                                        plotGroupAry={plotGroupAry}
                                                        showRotateLocked={showRotateLocked}/>} />

            {mi.zoomDropDownMenu && <DropDownToolbarButton icon={ZOOM_DROP} tip='Zoom drop down'
                                                          enabled={enabled} horizontal={true}
                                                          imageStyle={image24x24}
                                                          dropDown={<ZoomDrop pv={pv} mi={mi} image={image}/>} />}

            <ToolbarHorizontalSeparator/>
            <ColorButton colorDrops={colorDrops} enabled={enabled} pv={pv} />

            <DropDownToolbarButton icon={STRETCH} tip='Stretch drop down. Quickly change the background image stretch'
                                   enabled={enabled} horizontal={true} visible={mi.stretchQuick && image}
                                   imageStyle={image24x24} dropDown={<StretchDropDownView plotView={pv}/>} />

            <ImageCenterDropDown visRoot={visRoot} visible={mi.recenter} mi={mi} />

            <SimpleLayerOnOffButton plotView={pv} typeId={SelectArea.TYPE_ID}
                                    tip='Select Drop down. Select an area for cropping or statistics'
                                    iconOn={getSelectedAreaIcon()} iconOff={getSelectedAreaIcon(false)}
                                    visible={mi.selectArea} imageStyle={image24x24}
                                    modalEndInfo={modalEndInfo}
                                    setModalEndInfo={setModalEndInfo}
                                    dropDown={<SelectAreaDropDownView plotView={pv}
                                                                      modalEndInfo={modalEndInfo}
                                                                      setModalEndInfo={setModalEndInfo} />} />


            <LayerButton pv={pv} dlCount={dlCount}/>

            {unavailableCnt<2 && farLeftButtonEnabled && <ToolbarHorizontalSeparator/>}

            {unavailableCnt<2 && <SimpleLayerOnOffButton plotView={pv}
                                    isIconOn={pv&&plot? isOverlayColorLocked(pv,plotGroupAry) : false }
                                    tip='Lock all images for color changes and overlays.'
                                    iconOn={LOCKED} iconOff={UNLOCKED}
                                    visible={mi.overlayColorLock} imageStyle={image24x24}
                                    onClick={() => toggleOverlayColorLock(pv,plotGroupAry)} />
            }

            {unavailableCnt<1 &&
                    <MatchLockDropDown visRoot={visRoot} enabled={enabled} imageStyle={image24x24}
                                       visible={mi.matchLockDropDown} />
            }

            {manageExpand && <ToolbarButton icon={expandGrid? GRID_EXPAND : OUTLINE_EXPAND}
                           tip='Expand this panel to take up a larger area'
                           horizontal={true} visible={!isExpanded && pv?.plotViewCtx.canBeExpanded}
                                            onClick={() =>expand(pv?.plotId, expandGrid)}/>
            }
        </div>
    );
});

VisMiniToolbarView.propTypes= {
    visRoot : PropTypes.object.isRequired,
    dlCount : PropTypes.number,
    manageExpand : PropTypes.bool,
    expandGrid: PropTypes.bool,
    availableWidth: PropTypes.number,
    modalEndInfo: PropTypes.object,
    setModalEndInfo: PropTypes.func,
};


function getUnavailableCnt(availableWidth, mi, apiToolsView,image, isExpanded, manageExpand) {
    const maxWidth= getEstimatedWidth(mi, apiToolsView,image, isExpanded, manageExpand );
    const fullSize= availableWidth>maxWidth;
    const buttonSize = 28;
    return fullSize ? 0 : Math.round((maxWidth-availableWidth) / buttonSize)+1;
}

function getEstimatedWidth(mi, apiToolsView,image, isExpanded, manageExpand) {
    let retval= 340;
    if (!apiToolsView || !mi.imageSelect) retval-= 28;
    if (!mi.stretchQuick || !image) retval-= 28;
    if (!mi.recenter) retval-= 28;
    if (!mi.selectArea) retval-=28;
    if (!mi.overlayColorLock) retval-=28;
    if (!mi.matchLockDropDown) retval-=28;
    if (isExpanded || !manageExpand) retval-=28;
    return retval;
}


function doRotateNorth(pv,rotate) {
    if (isHiPS(primePlot(pv))) {
        dispatchChangeHiPS( {plotId:pv.plotId,  coordSys: rotate?CoordinateSys.EQ_J2000:CoordinateSys.GALACTIC});
    }
    else {
        dispatchRotate({plotId:pv.plotId, rotateType:rotate?RotateType.NORTH:RotateType.UNROTATE});
    }
}


function isOverlayColorLocked(pv,plotGroupAry){
    const plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);
    return hasOverlayColorLock(pv,plotGroup);
}

function toggleOverlayColorLock(pv,plotGroupAry){
    const plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);
    dispatchOverlayColorLocking(pv.plotId,!hasOverlayColorLock(pv,plotGroup));
}

export function expand(plotId, grid) {
    dispatchChangeActivePlotView(plotId);
    dispatchSetLayoutMode( LO_MODE.expanded, LO_VIEW.images );
    grid ? dispatchChangeExpandedMode(ExpandType.GRID) : dispatchChangeExpandedMode(true);
}

const showImagePopup= () => showImageSelPanel('Images');

export function LayerButton({pv}) {
    const layerCnt=  primePlot(pv) ? (getAllDrawLayersForPlot(getDlAry(),pv.plotId).length + pv.overlayPlotViews.length) : 0;
    const enabled= Boolean(layerCnt || findUnactivatedRelatedData(pv).length);
    return (
        <ToolbarButton icon={LAYER_ICON}
                       tip='Manipulate overlay display: Control color, visibility, and advanced options'
                       enabled={enabled} badgeCount={layerCnt} horizontal={true}
                       imageStyle={image24x24} onClick={showDrawingLayerPopup}/>
    );
}

LayerButton.propTypes= {
    pv : PropTypes.object,
    dlCount : PropTypes.number.isRequired // must be here. We don't use directly but it forces an update
};


const colorTip= 'Color Drop down. Change the color table';

const ColorButton= ({colorDrops,enabled,pv}) => (
    colorDrops ?
            <DropDownToolbarButton icon={COLOR} tip={colorTip} enabled={enabled} visible={!primePlot(pv)?.blank}
                                   imageStyle={image24x24} dropDown={<ColorTableDropDownView plotView={pv}/>}/>
            :
            <ToolbarButton icon={COLOR} tip={colorTip} enabled={enabled} visible={!primePlot(pv)?.blank}
                           imageStyle={image24x24} onClick={() =>showColorDialog()}/>
);

const ZoomDrop= ({pv,mi, image}) => (
    <SingleColumnMenu style={{minWidth:1}}>
        <ZoomButton plotView={pv} zoomType={ZoomType.UP} visible={mi.zoomUp} horizontal={false}/>
        <ZoomButton plotView={pv} zoomType={ZoomType.DOWN} visible={mi.zoomDown} horizontal={false}/>
        <ZoomButton plotView={pv} zoomType={ZoomType.ONE} visible={mi.zoomOriginal && image} horizontal={false}/>
        <ZoomButton plotView={pv} zoomType={ZoomType.FIT} visible={mi.zoomFit} horizontal={false}/>
        <ZoomButton plotView={pv} zoomType={ZoomType.FILL} visible={mi.zoomFill} horizontal={false}/>
    </SingleColumnMenu>
);


function ToolsDrop({visRoot, pv,plot, mi, enabled, image, hips, setModalEndInfo, modalEndInfo,
                       showRotateLocked, unavailableCnt, plotGroupAry}) {

    const makeMatchLock= mi.matchLockDropDown && unavailableCnt>0;
    const makeColorLock= mi.overlayColorLock && unavailableCnt>1;
    const showExtract= Boolean(image) && mi.extract;
    return (
        <div className='ff-MenuItem-dropDown'>
            <div style={{display:'inline-block', height:'100%', flex:'0 0 auto', margin:'0px 0 0 300px'}}>
                <HelpIcon helpId={'visualization.toolbar'}/>
            </div>
            <SaveRestoreRow style={{marginTop:-20}} pv={pv} mi={mi} enabled={enabled} image={image} hips={hips}/>
            <RotateFlipRow style={{paddingTop:10}} pv={pv} mi={mi} enabled={enabled}
                           showRotateLocked={showRotateLocked} image={image}/>
            <LayersRow style={{paddingTop:10}} pv={pv} mi={mi} enabled={enabled} image={image}
                       modalEndInfo={modalEndInfo}
                       setModalEndInfo={setModalEndInfo}
            />
            {showExtract && <ExtractRow style={{paddingTop:10}} pv={pv} mi={mi} enabled={enabled} image={image}
                                        modalEndInfo={modalEndInfo} setModalEndInfo={setModalEndInfo} />
            }
            {(makeMatchLock || makeColorLock) && <div style={{display:'flex', alignItems:'center', paddingTop:10}}>
                <div style={{width:130, fontSize:'larger'}}>More: </div>
                {makeColorLock && <SimpleLayerOnOffButton plotView={pv}
                                                          isIconOn={pv&&plot? isOverlayColorLocked(pv,plotGroupAry) : false }
                                                          tip='Lock all images for color changes and overlays.'
                                                          iconOn={LOCKED} iconOff={UNLOCKED}
                                                          visible={mi.overlayColorLock}
                                                          onClick={() => toggleOverlayColorLock(pv,plotGroupAry)} />}
                {makeMatchLock && <MatchLockDropDown visRoot={visRoot} enabled={enabled} inDropDown={true}
                                                     visible={mi.matchLockDropDown} />}
            </div> }
        </div>
    );
}

const SaveRestoreRow= ({style,image,hips,mi,pv,enabled}) => (
    <div style={{display:'flex', alignItems:'center', ...style}}>
        <div style={{width:130, fontSize:'larger'}}>Save / Restore / Info: </div>
        <ToolbarButton icon={SAVE} tip='Save the FITS file, PNG file, or save the overlays as a region'
                       enabled={enabled} horizontal={true} visible={mi.fitsDownload}
                       onClick={showFitsDownloadDialog.bind(null, 'Load Region')}/>
        <ToolbarButton icon={RESTORE} tip='Restore to the defaults' enabled={enabled}
                       horizontal={true} visible={mi.restore}
                       onClick={() => dispatchRestoreDefaults({plotId:pv.plotId})}/>
        <ToolbarButton icon={FITS_HEADER}
                       tip={image ? 'Show FITS header' : (hips ? 'Show HiPS properties' : '')}
                       enabled={enabled} horizontal={true} visible={mi.directFileAccessData}
                       onClick={(element) => showPlotInfoPopup(pv, element )} />
    </div>
);

const RotateFlipRow= ({style,image,mi,showRotateLocked,pv,enabled}) => (
    <div style={{display:'flex', alignItems:'center', ...style}}>
        <div style={{width:130, fontSize:'larger'}}>{image?'Rotate / Flip:' : 'Rotate J2000 North'}</div>
        <ToolbarButton icon={ROTATE} tip='Rotate the image to any angle' enabled={enabled}
                       horizontal={true} visible={mi.rotate && image} onClick={showFitsRotationDialog}/>

        <SimpleLayerOnOffButton plotView={pv} isIconOn={showRotateLocked}
                                tip={`Rotate this ${image?'image': 'HiPS'} so that EQ J2000 North is up`}
                                enabled={hasWCSProjection(pv)}
                                iconOn={ROTATE_NORTH_ON} iconOff={ROTATE_NORTH_OFF}
                                visible={mi.rotateNorth} onClick={doRotateNorth} />

        <SimpleLayerOnOffButton plotView={pv} isIconOn={pv ? pv.flipY : false }
                                tip='Flip the image on the Y Axis (i.e. Invert X)'
                                iconOn={FLIP_Y_ON} iconOff={FLIP_Y}
                                visible={mi.flipImageY && image} onClick={() => dispatchFlip({plotId:pv.plotId})} />
    </div>
);


function startExtraction(element,type,setModalEndInfo, modalEndInfo) {
    modalEndInfo?.f?.();
    let ended= false;
    showExtractionDialog(element, type, () => {
        if (!ended) setModalEndInfo({});
    });
    setModalEndInfo({
        s: 'End Extraction',
        f: () => {
            ended= true;
            endExtraction();
            setModalEndInfo({});
        },
        offOnNewPlot: false
    });

}

const ExtractRow= ({style,pv,enabled,setModalEndInfo, modalEndInfo,mi}) => {
    const standIm= !isThreeColor(pv);
    return (
        <div style={{display:'flex', alignItems:'center', ...style}}>
            <div style={{width:130, fontSize:'larger'}}>Extract: </div>
            <ToolbarButton icon={DRILL_DOWN} tip='Extract Z-axis from cube' enabled={standIm&&isImageCube(primePlot(pv))&&enabled}
                           horizontal={true} onClick={(element) => startExtraction(element,Z_AXIS,setModalEndInfo,modalEndInfo)}
                           visible={mi.extractZAxis}/>
            <ToolbarButton icon={LINE_EXTRACTION} tip='Extract line from image' enabled={standIm&&enabled}
                           horizontal={true} onClick={(element) => startExtraction(element,LINE,setModalEndInfo,modalEndInfo)}
                           visible={mi.extractLine}/>
            <ToolbarButton icon={POINT_EXTRACTION} tip='Extract points from image' enabled={standIm&&enabled}
                           horizontal={true} onClick={(element) => startExtraction(element,POINTS,setModalEndInfo,modalEndInfo)}
                           visible={mi.extractPoint}/>
        </div>
        );
};

const LayersRow= ({style,image, pv,mi,enabled,setModalEndInfo, modalEndInfo}) => (

    <div style={{display:'flex', alignItems:'center', ...style}}>
        <div style={{width:130, fontSize:'larger'}}>Layers: </div>
        <SimpleLayerOnOffButton plotView={pv} typeId={NorthUpCompass.TYPE_ID}
                                tip='Show the directions of Equatorial J2000 North and East'
                                iconOn={COMPASS_ON} iconOff={COMPASS_OFF} visible={mi.northArrow} />
        <SimpleLayerOnOffButton plotView={pv} typeId={WebGrid.TYPE_ID} tip='Add grid layer to the image'
                                iconOn={GRID_ON} iconOff={GRID_OFF}
                                plotTypeMustMatch={true} visible={mi.grid} />
        <SimpleLayerOnOffButton plotView={pv} typeId={DistanceTool.TYPE_ID}
                                tip='Select, then click and drag to measure a distance on the image'
                                endText={'End Distance'}
                                modalEndInfo={modalEndInfo}
                                setModalEndInfo={setModalEndInfo}
                                iconOn={DIST_ON} iconOff={DIST_OFF} visible={mi.distanceTool} />
        <ToolbarButton icon={DS9_REGION} tip='Load a DS9 Region File' enabled={enabled}
                       horizontal={true} visible={mi.ds9Region} onClick={showRegionFileUploadPanel}/>

        <ToolbarButton icon={MASK} tip='Add mask to image' enabled={enabled} horizontal={true}
                       visible={mi.maskOverlay && image}
                       onClick={showMaskDialog}/>

        <DropDownToolbarButton icon={MARKER} tip='Overlay Markers and Instrument Footprints'
                               enabled={enabled} horizontal={true} visible={mi.markerToolDD}
                               menuMaxWidth={580} disableHiding={true} dropDownKey={'marker'}
                               dropDown={<MarkerDropDownView plotView={pv}/>} />
    </div>
);