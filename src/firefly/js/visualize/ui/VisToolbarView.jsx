/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {get,isEmpty} from 'lodash';
import {getActivePlotView,
    primePlot,
    hasGroupLock,
    findPlotGroup,
    getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {dispatchRotate, dispatchFlip, dispatchRecenter, 
        dispatchRestoreDefaults,dispatchGroupLocking, ActionScope} from '../ImagePlotCntlr.js';
import {RotateType} from '../PlotState.js';
import {ToolbarButton, ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';
import {ZoomButton, ZoomType} from './ZoomButton.jsx';
import {SimpleLayerOnOffButton} from './SimpleLayerOnOffButton.jsx';
import {showDrawingLayerPopup} from './DrawLayerPanel.jsx';
import {defMenuItemKeys} from '../MenuItemKeys.js';
import {StretchDropDownView} from './StretchDropDownView.jsx';
import {ColorTableDropDownView} from './ColorTableDropDownView.jsx';

import {showFitsDownloadDialog} from '../../ui/FitsDownloadDialog.jsx';
import {showFitsRotationDialog} from '../../ui/FitsRotationDialog.jsx';
import DistanceTool from '../../drawingLayers/DistanceTool.js';
import SelectArea from '../../drawingLayers/SelectArea.js';
import NorthUpCompass from '../../drawingLayers/NorthUpCompass.js';
import { fitsHeaderView} from './FitsHeaderView.jsx';
import sCompare from 'react-addons-shallow-compare';
import { getDlAry } from '../DrawLayerCntlr.js';
import WebGrid from '../../drawingLayers/WebGrid.js';
import {showRegionFileUploadPanel} from '../region/RegionFileUploadView.jsx';
import {MarkerDropDownView} from './MarkerDropDownView.jsx';
import {dispatchShowDropDown} from '../../core/LayoutCntlr.js';
import {showImageSelPanel} from './ImageSelectPanel.jsx';


//===================================================
//--------------- Icons --------------------------------
//===================================================


import LAYER_ICON from 'html/images/icons-2014/TurnOnLayers.png';
import DIST_ON from 'html/images/icons-2014/Measurement-ON.png';
import DIST_OFF from 'html/images/icons-2014/Measurement.png';
import SELECT_OFF from 'html/images/icons-2014/Marquee.png';
import SELECT_ON from 'html/images/icons-2014/Marquee-ON.png';
import GRID_OFF from 'html/images/icons-2014/GreenGrid.png';
import GRID_ON from 'html/images/icons-2014/GreenGrid-ON.png';
import COMPASS_OFF from 'html/images/icons-2014/28x28_Compass.png';
import COMPASS_ON from 'html/images/icons-2014/28x28_CompassON.png';
import ROTATE_NORTH_OFF from 'html/images/icons-2014/RotateToNorth.png';
import ROTATE_NORTH_ON from 'html/images/icons-2014/RotateToNorth-ON.png';
import ROTATE from 'html/images/icons-2014/Rotate.png';
import RESTORE from 'html/images/icons-2014/RevertToDefault.png';
import FITS_HEADER from 'html/images/icons-2014/28x28_FITS_Information.png';
import DS9_REGION from 'html/images/icons-2014/DS9.png';
import MASK from 'html/images/mask_28x28.png';
import CATALOG from 'html/images/catalog_28x28.png';
import SAVE from 'html/images/icons-2014/Save.png';
import FLIP_Y from 'html/images/icons-2014/Mirror.png';
import RECENTER from 'html/images/icons-2014/RecenterImage.png';
import LOCKED from 'html/images/icons-2014/BkgLocked.png';
import UNLOCKED from 'html/images/icons-2014/BkgUnlocked.png';
import NEW_IMAGE from 'html/images/icons-2014/28x28_FITS_NewImage.png';

import COLOR from 'html/images/icons-2014/28x28_ColorPalette.png';
import STRETCH from 'html/images/icons-2014/28x28_Log.png';
import MARKER from 'html/images/icons-2014/MarkerCirclesIcon_28x28.png';


export const VIS_TOOLBAR_HEIGHT=34;
export const VIS_TOOLBAR_V_HEIGHT=48;


const tipStyle= {
    display: 'inline-block',
    position : 'relative',
    overflow : 'hidden',
    maxWidth : 300,
    fontSize: 10,
    whiteSpace: 'normal',
    verticalAlign: 'top',
    top : 3,
    left : 15,
    textOverflow: 'ellipsis',
    flex: '1 1 auto'
};




/**
 * Vis Toolbar
 * @param visRoot visualization store root
 * @param toolTip tool tip to show
 * @param dlCount drawing layer count
 * @param messageUnder put the help message under 
 * @return {XML}
 */
export function VisToolbarViewWrapper({visRoot,toolTip,dlCount, messageUnder}) {

    var rS= {
        width: 'calc(100% - 2px)',
        height: messageUnder ? VIS_TOOLBAR_V_HEIGHT : VIS_TOOLBAR_HEIGHT,
        display: 'inline-flex',
        position: 'relative',
        verticalAlign: 'top',
        whiteSpace: 'nowrap',
        flexWrap:'nowrap',
        flexDirection: messageUnder ? 'column' : 'row'
    };


    const ts= messageUnder ? Object.assign({},tipStyle, {maxWidth:'100%'}) : tipStyle;

    return (
        <div style={rS}>
            <VisToolbarView visRoot={visRoot} dlCount={dlCount}/>
            <div style={ts}>{toolTip}</div>
        </div>
    );

}



//onClick={dispatchShowDropDown.bind(null, {view: 'ImageSelectDropDownCmd'})}/>}

VisToolbarViewWrapper.propTypes= {
    visRoot : PropTypes.object.isRequired,
    toolTip : PropTypes.string,
    dlCount : PropTypes.number,
    messageUnder : PropTypes.bool
};


/**
 * Vis Toolbar
 * @param visRoot visualization store root
 * @param toolTip tool tip to show
 * @return {XML}
 */


export class VisToolbarView extends Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np,ns) {
        return sCompare(this,np,ns);
    }

    render() {
        const {visRoot,dlCount}= this.props;
        var rS= {
            display: 'inline-block',
            position: 'relative',
            verticalAlign: 'top',
            whiteSpace: 'nowrap'
        };
        const {apiToolsView}= visRoot;

        var pv= getActivePlotView(visRoot);
        var plot= primePlot(pv);
        var plotGroupAry= visRoot.plotGroupAry;

        var mi= pv ? pv.menuItemKeys : defMenuItemKeys;

        var enabled= pv ? true : false;

        return (
            <div style={rS}>
                <ToolbarButton icon={SAVE}
                               tip='Save the Fits file, PNG file, or save the overlays as a region'
                               enabled={enabled}
                               horizontal={true}
                               visible={mi.fitsDownload}
                               onClick={showFitsDownloadDialog.bind(null, 'Load Region')}/>


                {apiToolsView && <ToolbarButton icon={NEW_IMAGE}
                                             tip='Select a new image'
                                             enabled={enabled}
                                             horizontal={true}
                                             visible={mi.imageSelect}
                                             onClick={showImagePopup}/>}



                <ToolbarHorizontalSeparator/>

                <ZoomButton plotView={pv} zoomType={ZoomType.UP} visible={mi.zoomUp}/>
                <ZoomButton plotView={pv} zoomType={ZoomType.DOWN} visible={mi.zoomDown}/>
                <ZoomButton plotView={pv} zoomType={ZoomType.ONE} visible={mi.zoomOriginal}/>
                <ZoomButton plotView={pv} zoomType={ZoomType.FIT} visible={mi.zoomFit}/>
                <ZoomButton plotView={pv} zoomType={ZoomType.FILL} visible={mi.zoomFill}/>
                <ToolbarHorizontalSeparator/>


                <DropDownToolbarButton icon={COLOR}
                                       tip='Change the color table'
                                       enabled={enabled} horizontal={true}
                                       visible={true}
                                       dropDown={<ColorTableDropDownView plotView={pv}/>} />

                <DropDownToolbarButton icon={STRETCH}
                                       tip='Quickly change the background image stretch'
                                       enabled={enabled} horizontal={true}
                                       visible={mi.stretchQuick}
                                       dropDown={<StretchDropDownView plotView={pv}/>} />

                <ToolbarButton icon={FLIP_Y} tip='Flip the image on the Y Axis'
                               enabled={enabled} horizontal={true}
                               visible={mi.flipImageY}
                               onClick={() => flipY(pv)}/>

                <ToolbarButton icon={RECENTER} tip='Re-center image on last query or center of image'
                               enabled={enabled} horizontal={true}
                               visible={mi.recenter}
                               onClick={() => recenter(pv)}/>


                <ToolbarHorizontalSeparator/>

                <SimpleLayerOnOffButton plotView={pv}
                                        isIconOn={pv&&plot ? pv.plotViewCtx.rotateNorthLock : false }
                                        tip='Rotate this image so that North is up'
                                        iconOn={ROTATE_NORTH_ON}
                                        iconOff={ROTATE_NORTH_OFF}
                                        visible={mi.rotateNorth}
                                        onClick={doRotateNorth}
                />

                <ToolbarButton icon={ROTATE}
                               tip='Rotate the image to any angle'
                               enabled={enabled}
                               horizontal={true}
                               visible={mi.rotate}
                               onClick={showFitsRotationDialog}/>



                <ToolbarHorizontalSeparator/>

                <SimpleLayerOnOffButton plotView={pv}
                                        typeId={SelectArea.TYPE_ID}
                                        tip='Select an area for cropping or statistics'
                                        iconOn={SELECT_ON}
                                        iconOff={SELECT_OFF}
                                        visible={mi.selectArea} />
                <SimpleLayerOnOffButton plotView={pv}
                                        typeId={DistanceTool.TYPE_ID}
                                        tip='Select, then click and drag to measure a distance on the image'
                                        iconOn={DIST_ON}
                                        iconOff={DIST_OFF}
                                        visible={mi.distanceTool} />

                <DropDownToolbarButton icon={MARKER}
                                       tip='Overlay Markers and Instrument Footprints'
                                       enabled={enabled} horizontal={true}
                                       visible={mi.markerToolDD}
                                       dropDown={<MarkerDropDownView plotView={pv}/>} />

                <SimpleLayerOnOffButton plotView={pv}
                                        typeId={NorthUpCompass.TYPE_ID}
                                        tip='Show the directions of Equatorial J2000 North and East'
                                        iconOn={COMPASS_ON}
                                        iconOff={COMPASS_OFF}
                                        visible={mi.northArrow}
                />

                <SimpleLayerOnOffButton plotView={pv}
                                        typeId={WebGrid.TYPE_ID}
                                        tip='Add grid layer to the image'
                                        iconOn={GRID_ON}
                                        iconOff={GRID_OFF}
                                        visible={mi.grid}
                />





                <ToolbarButton icon={DS9_REGION}
                               tip='Load a DS9 Region File'
                               enabled={enabled}
                               horizontal={true}
                               visible={mi.ds9Region}
                               onClick={showRegionFileUploadPanel}/>

                <ToolbarButton icon={MASK}
                               tip='Overlay a mask Image'
                               enabled={enabled}
                               horizontal={true}
                               visible={mi.maskOverlay}
                               todo={true}
                               onClick={() => console.log('todo- mask dialog')}/>



                <LayerButton plotId={get(pv,'plotId')}  dlCount={dlCount} visible={mi.layer}/>

                <ToolbarHorizontalSeparator/>


                <ToolbarButton icon={CATALOG}
                               tip='Show a catalog'
                               enabled={enabled}
                               horizontal={true}
                               visible={mi.irsaCatalog}
                               todo={true}
                               onClick={() => console.log('todo- irsa catalog dialog')}/>


                <ToolbarButton icon={RESTORE}
                               tip='Restore to the defaults'
                               enabled={enabled}
                               horizontal={true}
                               visible={mi.restore}
                               onClick={() => dispatchRestoreDefaults({plotId:pv.plotId})}/>

                <SimpleLayerOnOffButton plotView={pv}
                                isIconOn={pv&&plot? isGroupLocked(pv,plotGroupAry) : false }
                                tip='lock images of all bands for zooming, scrolling etc.'
                                iconOn={LOCKED}
                                iconOff={UNLOCKED}
                                visible={mi.lockRelated}
                                onClick={() => toggleLockRelated(pv,plotGroupAry)}
                                 />


                <ToolbarButton icon={FITS_HEADER}
                               tip='Show FITS header'
                               enabled={enabled}
                               horizontal={true}
                               visible={mi.fitsHeader}
                               onClick={() =>  fitsHeaderView(pv)}/>




            </div>
        );

    }
}

VisToolbarView.propTypes= {
    visRoot : PropTypes.object.isRequired,
    dlCount : PropTypes.number
};

function doRotateNorth(pv,rotate) {
    const rotateType= rotate?RotateType.NORTH:RotateType.UNROTATE;
    dispatchRotate({plotId:pv.plotId, rotateType});
}

function recenter(pv) { dispatchRecenter({plotId:pv.plotId}); }

function flipY(pv) {
    dispatchFlip({plotId:pv.plotId});
}



// function showToolTip(toolTip) {
//     return <div style={tipStyle}>{toolTip}</div>;
// }

function isGroupLocked(pv,plotGroupAry){
    var plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);
    var lockEnabled = hasGroupLock(pv,plotGroup);
    return lockEnabled;

}
function toggleLockRelated(pv,plotGroupAry){
    var plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);
    dispatchGroupLocking(pv.plotId,!hasGroupLock(pv,plotGroup));
}

function showImagePopup() {
    showImageSelPanel('Images');
}

//==================================================================================
//==================================================================================
//==================================================================================
//==================================================================================

export function LayerButton({plotId,visible}) {
    const plotLayers= getAllDrawLayersForPlot(getDlAry(),plotId);
    const enabled= !isEmpty(plotLayers);

    return (
        <ToolbarButton icon={LAYER_ICON}
                       tip='Manipulate overlay display: Control color, visibility, and advanced options'
                       enabled={enabled}
                       badgeCount={plotLayers.length}
                       horizontal={true}
                       visible={visible}
                       onClick={showDrawingLayerPopup}/>
        );
}

LayerButton.propTypes= {
    plotId : PropTypes.string,
    visible : PropTypes.bool.isRequired,
    dlCount : PropTypes.number.isRequired // must be here. We don't use directly but it forces an update
};
