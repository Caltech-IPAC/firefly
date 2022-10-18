/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {dispatchHideDialog, dispatchShowDialog, isDialogVisible} from 'firefly/core/ComponentCntlr.js';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {LayoutType, PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import {ToolbarButton} from 'firefly/ui/ToolbarButton.jsx';
import {closeToolbarModalLayers} from 'firefly/visualize/ui/VisMiniToolbar.jsx';
import {computeCentralPointAndRadius,} from 'firefly/visualize/VisUtil.js';
import CLICK from 'html/images/20x20_click.png';
import SELECT_NONE from 'html/images/icons-2014/28x28_Rect_DD.png';
import PropTypes, {arrayOf, bool, number, object, oneOf, shape, string} from 'prop-types';
import React, {useContext, useEffect, useState} from 'react';
import HiPSMOC from '../../drawingLayers/HiPSMOC.js';
import ImageOutline from '../../drawingLayers/ImageOutline.js';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {ConnectionCtx} from '../../ui/ConnectionCtx.js';
import {FieldGroupCtx} from '../../ui/FieldGroup.jsx';
import {InputAreaFieldConnected} from '../../ui/InputAreaField.jsx';
import {useFieldGroupValue, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {SizeInputFields} from '../../ui/SizeInputField.jsx';
import {DEF_TARGET_PANEL_KEY, TargetPanel} from '../../ui/TargetPanel.jsx';
import {parseObsCoreRegion} from '../../util/ObsCoreSRegionParser.js';
import {CoordinateSys} from '../CoordSys.js';
import {
    dispatchAttachLayerToPlot, dispatchCreateDrawLayer, dispatchDestroyDrawLayer, dlRoot
} from '../DrawLayerCntlr.js';
import {dispatchDeletePlotView, dispatchPlotHiPS, visRoot} from '../ImagePlotCntlr.js';
import {NewPlotMode} from '../MultiViewCntlr.js';
import {onPlotComplete} from '../PlotCompleteMonitor.js';
import {getDrawLayersByType, getPlotViewById, isDrawLayerAttached, primePlot} from '../PlotViewUtil.js';
import {makeWorldPt, parseWorldPt} from '../Point.js';
import {createHiPSMocLayer} from '../task/PlotHipsTask.js';
import {WebPlotRequest} from '../WebPlotRequest.js';
import {HelpIcon} from './../../ui/HelpIcon.jsx';
import {CONE_CHOICE_KEY, POLY_CHOICE_KEY} from './CommonUIKeys.js';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {
    convertStrToWpAry, HelpLines, initSearchSelectTool, updatePlotOverlayFromUserInput,
    updateUIFromPlot
} from './VisualSearchUtils.js';


const DIALOG_ID= 'HiPSPanelPopup';
const DEFAULT_HIPS= 'ivo://CDS/P/DSS2/color';
const DEFAULT_FOV= 340;

const sharedPropTypes= {
    hipsUrl: string,
    centerPt: object,
    hipsFOVInDeg: number,
    style: object,
    plotId:string,
    cleanup: bool,
    groupKey: string,
    minSize: number,
    maxSize: number,
    whichOverlay: string,
    coordinateSys: oneOf([CoordinateSys.GALACTIC, CoordinateSys.EQ_J2000]),
    mocList: arrayOf( shape({ mocUrl: string, title: string }) ),
};

export function VisualPolygonPanel({label, initValue, tooltip, fieldKey, style,
                                       labelStyle={}, labelWidth= 100, manageHiPS=true, ...restOfProps}) {

    const button= manageHiPS &&
        ( <HiPSPanelPopupButton {...{polygonKey:fieldKey, whichOverlay:POLY_CHOICE_KEY, ...restOfProps}} /> );
    return (
        <InputAreaFieldConnected {...{
            fieldKey, label, labelStyle, button, labelWidth, tooltip,
            wrapperStyle:{display:'flex', alignItems:'center'},
            style:{overflow:'auto', height:55, maxHeight:200, minWidth: 100, width:280, maxWidth:360, ...style},
            initialState:{value:initValue},
        }} />
    );
}

export function VisualTargetPanel({fieldKey, labelWidth= 100, label, labelStyle, ...restOfProps}) {
    const popupButton= (
        <div style={{paddingRight: 2}}>
            <HiPSPanelPopupButton {...{targetKey:fieldKey, whichOverlay:CONE_CHOICE_KEY, ...restOfProps}} />
        </div>
    );
    return ( <TargetPanel fieldKey={fieldKey} button={popupButton} labelStyle={labelStyle} labelWidth={labelWidth} label={label}/> );
}

VisualTargetPanel.propTypes= {
    labelWidth: number,
    ...sharedPropTypes
};



export const HiPSTargetView = ({style, hipsDisplayKey='none',
                                   hipsUrl=DEFAULT_HIPS, hipsFOVInDeg= DEFAULT_FOV, centerPt=makeWorldPt(0,0, CoordinateSys.GALACTIC),
                                   targetKey=DEF_TARGET_PANEL_KEY, sizeKey='none---Size', polygonKey='non---Polygon',
                                   whichOverlay= CONE_CHOICE_KEY, sRegion,
                                   coordinateSys, mocList, minSize=1/3600, maxSize=100,
                                   plotId='defaultHiPSTargetSearch', cleanup= false, groupKey}) => {

    const viewerId= plotId+'-viewer';

    const pv= useStoreConnector(() => getPlotViewById(visRoot(),plotId));
    const [getTargetWp,setTargetWp]= useFieldGroupValue(targetKey, groupKey);
    const [getHiPSRadius, setHiPSRadius]= useFieldGroupValue(sizeKey, groupKey);
    const [getPolygon, setPolygon]= useFieldGroupValue(polygonKey, groupKey);

    const userEnterWorldPt= () =>  parseWorldPt(getTargetWp());
    const userEnterSearchRadius= () =>  Number(getHiPSRadius());
    const userEnterPolygon= () => convertStrToWpAry(getPolygon());

    useEffect(() => { // show HiPS plot
        if (!pv || hipsUrl!==pv.request.getHipsRootUrl()) {
            initHiPSPlot({plotId,hipsUrl, viewerId,centerPt,hipsFOVInDeg, coordinateSys, mocList,
                userEnterWorldPt:userEnterWorldPt(), userEnterSearchRadius:userEnterSearchRadius(),
                whichOverlay, userEnterPolygon:userEnterPolygon(),
            });
        }
        else {
            updatePlotOverlayFromUserInput(plotId, whichOverlay, userEnterWorldPt(), userEnterSearchRadius(),
                userEnterPolygon(), true);
        }
        return () => {
            if (cleanup) dispatchDeletePlotView({plotId});
        };
    },[hipsDisplayKey]);

    useEffect(() => { // if plot view changes then update the target or polygon field
        updateUIFromPlot(plotId,whichOverlay,setTargetWp,getTargetWp,setHiPSRadius,setHiPSRadius,setPolygon,getPolygon,minSize,maxSize);
    },[pv]);

    useEffect(() => { // if target or radius field change then hips plot to reflect it
        updatePlotOverlayFromUserInput(plotId, whichOverlay, userEnterWorldPt(),
            userEnterSearchRadius(), userEnterPolygon());
    }, [getTargetWp, getHiPSRadius, getPolygon, whichOverlay]);

    useEffect(() => {
        attachSRegion(sRegion,plotId);
    }, [sRegion]);

    return (
        <div style={{height:500, ...style, display:'flex', flexDirection:'column'}}>
            <div style={{padding: '5px 5px 10px 5px',
                alignSelf: 'stretch', marginBottom: 1,
                display: 'flex', flexDirection:'row',
                justifyContent: 'space-between',
                background: 'rgb(200,200,200',
                borderBottom: '1px solid rgba(0,0,0,.07)'}}>
                <div style={{fontSize:'9pt', paddingRight: 10}}>
                    {<HelpLines whichOverlay={whichOverlay} />}
                </div>
                <HelpIcon style={{alignSelf:'center'}} helpId={'hips.VisualSelection'} />
            </div>
            <MultiImageViewer viewerId= {viewerId} insideFlex={true}
                              canReceiveNewPlots={NewPlotMode.create_replace.key}
                              showWhenExpanded={true}
                              Toolbar={MultiViewStandardToolbar}/>
        </div>
    );
};

HiPSTargetView.propTypes= {
    ...sharedPropTypes,
    sizeKey: string,
    targetKey: string,
};



export const TargetHiPSPanel = ({searchAreaInDeg, style,
                                    minValue:min= 1/3600, maxValue:max= 100, ...restOfProps}) => (
    <div style={{display:'flex', width: 700, height:700, flexDirection:'column', ...style}}>
        <HiPSTargetView {...{...restOfProps, targetKey:DEF_TARGET_PANEL_KEY, sizeKey:'HiPSPanelRadius',
                             minSize:min, maxSize:max, style:{height:400} }}/>
        <div style={{display:'flex', flexDirection:'column', marginLeft:100}}>
            <TargetPanel style={{paddingTop: 10}} labelWidth={100}/>
            <SizeInputFields fieldKey='HiPSPanelRadius' showFeedback={true} labelWidth= {100}  nullAllowed={false}
                             label='Search Area:'
                             labelStyle={{textAlign:'right', paddingRight:4}}
                             initialState={{ unit: 'arcsec', value: searchAreaInDeg+'', min, max }} />
        </div>
    </div>
);

TargetHiPSPanel.propTypes= {
    searchAreaInDeg: PropTypes.number.isRequired,
    ...sharedPropTypes,
};

export const TargetHiPSRadiusPopupPanel = ({searchAreaInDeg, style,
                                               targetKey=DEF_TARGET_PANEL_KEY, sizeKey= 'HiPSPanelRadius', polygonKey,
                                              minValue:min= 1/3600, maxValue:max= 100, targetLabelStyle,
                                               sizeLabel= 'Search Area:', sizeFeedbackStyle, ...restOfProps}) => {
    const [controlConnected, setControlConnected] = useState(false);
    return (
        <ConnectionCtx.Provider value={{controlConnected, setControlConnected}}>
            <div style={{display:'flex', width: 700, paddingBottom: 20, flexDirection:'column', ...style}}>
                <div style={{display:'flex', flexDirection:'column'}}>
                    <VisualTargetPanel {...{style:{paddingTop: 10}, fieldKey:targetKey, sizeKey, polygonKey, labelWidth:100,
                        labelStyle:targetLabelStyle, minSize:min, maxSize:max, ...restOfProps}} />
                    <SizeInputFields {...{
                        fieldKey:sizeKey, showFeedback:true, labelWidth:100, nullAllowed:false,
                        label: sizeLabel,
                        feedbackStyle:sizeFeedbackStyle,
                        labelStyle:{textAlign:'right', paddingRight:4},
                        initialState:{ unit: 'arcsec', value: searchAreaInDeg+'', min, max }
                    }} />
                </div>
            </div>
        </ConnectionCtx.Provider>
    );
};

TargetHiPSRadiusPopupPanel.propTypes= {
    searchAreaInDeg: PropTypes.number.isRequired,
    sizeKey: PropTypes.string,
    ...sharedPropTypes,
};

export const hideHiPSPopupPanel= () => dispatchHideDialog(DIALOG_ID);


function HiPSPanelPopupButton({groupKey:gk, polygonKey, whichOverlay=CONE_CHOICE_KEY, targetKey=DEF_TARGET_PANEL_KEY,
                                  hideHiPSPopupPanelOnDismount= true, centerPt,
                                  tip='Choose search area visually',
                                  hipsFOVInDeg, ...restOfProps}) {

    const context= useContext(FieldGroupCtx);
    const connectContext= useContext(ConnectionCtx);
    const groupKey= gk || context.groupKey;

    const showDialog= (element) => {
        const wp= parseWorldPt(getFieldVal(groupKey,targetKey));
        let workingCenterPt= centerPt;
        let newHipsFOVInDeg= wp ? 10 : hipsFOVInDeg;
        if (whichOverlay===CONE_CHOICE_KEY) {
            if (restOfProps.sizeKey && wp) {
                const sizeValue= Number(getFieldVal(groupKey,restOfProps.sizeKey));
                if (!isNaN(sizeValue) && sizeValue) newHipsFOVInDeg= sizeValue*10;
            }
        }
        else if (whichOverlay===POLY_CHOICE_KEY && polygonKey) {
            const wpAry= convertStrToWpAry(getFieldVal(groupKey,polygonKey));
            if (wpAry?.length>3) {
                const {centralPoint, maxRadius}= computeCentralPointAndRadius(wpAry);
                if (maxRadius>.0002) {
                    if (maxRadius<50) newHipsFOVInDeg= maxRadius*6;
                    else if (maxRadius<120) newHipsFOVInDeg= maxRadius*1.5;
                    else if (maxRadius<150) newHipsFOVInDeg= maxRadius*1.5;
                    else newHipsFOVInDeg= 360;
                }
                if (centralPoint) workingCenterPt= centralPoint;
            }
        }
        showHiPSPanelPopup({
            centerPt:workingCenterPt,
            popupClosing: () => connectContext?.setControlConnected(false),
            polygonKey, targetKey, element, groupKey, whichOverlay, ...restOfProps, hipsFOVInDeg: newHipsFOVInDeg});
        connectContext?.setControlConnected(true);
    };

    useEffect(() => {
        if (isDialogVisible(DIALOG_ID)) {
            showDialog();
        }
        return () => {
            if (hideHiPSPopupPanelOnDismount) {
                hideHiPSPopupPanel();
                connectContext?.setControlConnected(false);
            }
        };
    },[]);

    return (
        <ToolbarButton {...{
            icon:CLICK, tip, bgDark:true, horizontal:true, imageStyle:{height:18, width:18},
            onClick:(element) => showDialog(element)
        }}/>
    );
}




const defPopupPlotId= 'defaultHiPSPopupTargetSearch';

function showHiPSPanelPopup({popupClosing, element, plotId= defPopupPlotId,
                                whichOverlay= CONE_CHOICE_KEY, ...restOfProps}) {


    const doClose= () => {
        closeToolbarModalLayers();
        popupClosing();
    };

    const hipsPanel= (
        <PopupPanel title={'Choose Target'} layoutPosition={LayoutType.TOP_RIGHT_OF_BUTTON} element={element}
                    closeCallback={() => doClose()} >
            <div style={{
                padding: 3, display:'flex', flexDirection:'column', width: 500, height:400,
                background: 'rgb(200,200,200',
                alignItems:'center', resize:'both', overflow: 'hidden', zIndex:1}}>
                <HiPSTargetView {...{
                    plotId,
                    whichOverlay,
                    ...restOfProps,
                    style:{height:'100%', width:'100%', paddingBottom:4} }} />
            </div>
        </PopupPanel>
    );

    DialogRootContainer.defineDialog(DIALOG_ID, hipsPanel, element );
    dispatchShowDialog(DIALOG_ID);
}




let mocCnt=0;
const createMocTableId= () => `moc-table-${++mocCnt}`;

/**
 * plot the HiPS, set the center, FOV, user target selection layer, and the MOC layers.
 * Note this many options are disabled that do not make sense when using the HiPS for user input
 * @param obj
 * @param obj.plotId
 * @param obj.hipsUrl
 * @param obj.viewerId
 * @param obj.centerPt
 * @param obj.hipsFOVInDeg
 * @param obj.coordinateSys
 * @param obj.mocList
 * @param obj.userEnterWorldPt
 * @param obj.userEnterSearchRadius
 * @param obj.whichOverlay
 * @param obj.userEnterPolygon
 * @return {Promise<void>}
 */
async function initHiPSPlot({ hipsUrl, plotId, viewerId, centerPt, hipsFOVInDeg, coordinateSys, mocList,
                                userEnterWorldPt, userEnterSearchRadius, whichOverlay, userEnterPolygon}) {
    getDrawLayersByType(dlRoot(), HiPSMOC.TYPE_ID)
        .forEach( ({drawLayerId}) => dispatchDestroyDrawLayer(drawLayerId));// clean up any old moc layers
    const wpRequest= WebPlotRequest.makeHiPSRequest(hipsUrl, centerPt, hipsFOVInDeg);
    wpRequest.setPlotGroupId(plotId+'-group');
    wpRequest.setOverlayIds([HiPSMOC.TYPE_ID]);
    wpRequest.setPlotId(plotId);
    if (coordinateSys) {
        wpRequest.setHipsUseCoordinateSys(coordinateSys);
    }
    else if (hipsFOVInDeg>200) {
        wpRequest.setHipsUseCoordinateSys(CoordinateSys.GALACTIC);
    }
    wpRequest.setHipsUseAitoffProjection(hipsFOVInDeg>130);
    dispatchPlotHiPS({plotId, wpRequest, viewerId,
        pvOptions: {canBeExpanded:false, useForSearchResults:false, displayFixedTarget:false, userCanDeletePlots: false,
            menuItemKeys: {
                zoomDropDownMenu: false, overlayColorLock: false, matchLockDropDown: false,
                recenter: false, selectArea: true,
            }
        }
    });
    await onPlotComplete(plotId);
    if (mocList) {
        const pList= mocList.map( ({mocUrl,title}) =>
            createHiPSMocLayer(createMocTableId(),title, mocUrl, primePlot(visRoot(),plotId), true, '') );
        await Promise.all(pList);
    }

    initSearchSelectTool(plotId);
    if (userEnterWorldPt || userEnterPolygon?.length) {
        updatePlotOverlayFromUserInput(plotId,whichOverlay, userEnterWorldPt, userEnterSearchRadius, userEnterPolygon, true);
    }
}


function attachSRegion(sRegion, plotId) {
    getDrawLayersByType(dlRoot(), ImageOutline.TYPE_ID)
        .forEach( ({drawLayerId}) => dispatchDestroyDrawLayer(drawLayerId));// clean up any old moc layers
    if (!sRegion) return;
    const {valid,drawObj}= parseObsCoreRegion(sRegion);
    if (!valid) return;
    const dl = dispatchCreateDrawLayer(ImageOutline.TYPE_ID,
        {drawObj, color: 'red', title:'s_region outline', destroyWhenAllDetached: true});
    if (!isDrawLayerAttached(dl, plotId)) dispatchAttachLayerToPlot(dl.drawLayerId, plotId, false);
}



