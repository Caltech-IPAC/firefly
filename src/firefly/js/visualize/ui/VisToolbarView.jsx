/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {getActivePlotView,
    getDrawLayerByType,
    getPlotViewById,
    isDrawLayerAttached,
    getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {ZoomButton, ZoomType} from './ZoomButton.jsx';
import {SimpleLayerOnOffButton} from './SimpleLayerOnOffButton.jsx';
import {showDrawingLayerPopup} from './DrawLayerPanel.jsx';
import {dispatchCreateDrawLayer,
    dispatchAttachLayerToPlot,
    dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';
import {defMenuItemKeys} from '../MenuItemKeys.js';

import DistanceTool from '../../drawingLayers/DistanceTool.js';
import SelectArea from '../../drawingLayers/SelectArea.js';


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


/**
 * Vis Toolbar
 * @param visRoot visualization store root
 * @param {[]} dlAry drawing layer array
 * @param toolTip tool tip to show
 * @return {XML}
 */
export function VisToolbarView({visRoot,dlAry,toolTip}) {

    var rS= {
        width: 'calc(100% - 2px)',
        height: 34,
        display: 'inline-block',
        position: 'relative',
        verticalAlign: 'top'
    };

    var pv= getActivePlotView(visRoot);
    var mi= pv ? pv.menuItemKeys : defMenuItemKeys;

    var enabled= pv ? true : false;

    return (
        <div style={rS}>
            <ZoomButton plotView={pv} zoomType={ZoomType.UP} visible={mi.zoomUp}/>
            <ZoomButton plotView={pv} zoomType={ZoomType.DOWN} visible={mi.zoomDown}/>
            <ZoomButton plotView={pv} zoomType={ZoomType.ONE} visible={mi.zoomOriginal}/>
            <ZoomButton plotView={pv} zoomType={ZoomType.FIT} visible={mi.zoomFit}/>
            <ZoomButton plotView={pv} zoomType={ZoomType.FILL} visible={mi.zoomFill}/>
            <ToolbarHorizontalSeparator/>
            <ToolbarHorizontalSeparator/>

            <SimpleLayerOnOffButton plotView={pv}
                                    dlAry={dlAry}
                                    typeId={'TODO'}
                                    tip='Rotate this image so that North is up'
                                    iconOn={ROTATE_NORTH_ON}
                                    iconOff={ROTATE_NORTH_OFF}
                                    visible={mi.rotateNorth}
                                    todo={true}
            />
            <ToolbarButton icon={ROTATE}
                           tip='Rotate the image to any angle'
                           enabled={enabled}
                           horizontal={true}
                           visible={mi.rotate}
                           todo={true}
                           onClick={() => console.log('todo')}/>


            <ToolbarHorizontalSeparator/>

            <SimpleLayerOnOffButton plotView={pv}
                                    dlAry={dlAry}
                                    typeId={SelectArea.TYPE_ID}
                                    tip='Select an area for cropping or statistics'
                                    iconOn={SELECT_ON}
                                    iconOff={SELECT_OFF}
                                    visible={mi.selectArea}
            />
            <SimpleLayerOnOffButton plotView={pv}
                                    dlAry={dlAry}
                                    typeId={DistanceTool.TYPE_ID}
                                    tip='Select, then click and drag to measure a distance on the image'
                                    iconOn={DIST_ON}
                                    iconOff={DIST_OFF}
                                    visible={mi.distanceTool}
            />

            <SimpleLayerOnOffButton plotView={pv}
                                    dlAry={dlAry}
                                    typeId={'TODO'}
                                    tip='Show the directions of Equatorial J2000 North and East'
                                    iconOn={COMPASS_ON}
                                    iconOff={COMPASS_OFF}
                                    visible={mi.northArrow}
                                    todo={true}
            />

            <SimpleLayerOnOffButton plotView={pv}
                                    dlAry={dlAry}
                                    typeId={'TODO'}
                                    tip='Add grid annotation to image'
                                    iconOn={GRID_ON}
                                    iconOff={GRID_OFF}
                                    visible={mi.grid}
                                    todo={true}
            />




            <ToolbarButton icon={DS9_REGION}
                           tip='Load a DS9 Region File'
                           enabled={enabled}
                           horizontal={true}
                           visible={mi.ds9Region}
                           todo={true}
                           onClick={() => console.log('todo- region dialog')}/>

            <ToolbarButton icon={MASK}
                           tip='Overlay a mask Image'
                           enabled={enabled}
                           horizontal={true}
                           visible={mi.maskOverlay}
                           todo={true}
                           onClick={() => console.log('todo- mask dialog')}/>



            <LayerButton plotView={pv} dlAry={dlAry} visible={mi.layer}/>

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
                           todo={true}
                           onClick={() => console.log('todo - restore to defaults')}/>

            <ToolbarButton icon={FITS_HEADER}
                           tip='Show FITS header'
                           enabled={enabled}
                           horizontal={true}
                           visible={mi.fitsHeader}
                           todo={true}
                           onClick={() => console.log('todo- fits header dialog')}/>

            {showToolTip(toolTip)}
        </div>
    );
}


VisToolbarView.propTypes= {
    visRoot : PropTypes.object.isRequired,
    toolTip : PropTypes.string,
    dlAry : PropTypes.arrayOf(React.PropTypes.object)
};

const tipStyle= {
    display: 'inline-block',
    position : 'relative',
    overflow : 'hidden',
    width : 300,
    fontSize: 10,
    whiteSpace: 'normal',
    verticalAlign: 'top',
    top : 3,
    left : 15
};

function showToolTip(toolTip) {
    return toolTip ? <div style={tipStyle}>{toolTip}</div> : false;
}


//==================================================================================
//==================================================================================
//==================================================================================
//==================================================================================

export function LayerButton({plotView:pv,dlAry,visible}) {
    var plotLayers= 0;
    var enabled= false;
    if (pv && dlAry) {
        plotLayers= getAllDrawLayersForPlot(dlAry,pv.plotId);
        enabled= true;
    }

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
    plotView : PropTypes.object,
    visible : PropTypes.bool.isRequired,
    dlAry : PropTypes.arrayOf(React.PropTypes.object)
};


