/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {dispatchHideDialog, dispatchShowDialog, isDialogVisible} from 'firefly/core/ComponentCntlr.js';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {LayoutType, PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import {ToolbarButton} from 'firefly/ui/ToolbarButton.jsx';
import {computeCentralPointAndRadius,} from 'firefly/visualize/VisUtil.js';
import CLICK from 'html/images/20x20_click.png';
import PropTypes, {arrayOf, bool, number, object, oneOf, shape, string, func} from 'prop-types';
import React, {useContext, useEffect, useRef, useState} from 'react';
import {getTblById, makeFileRequest, onTableLoaded} from '../../api/ApiUtilTable.jsx';
import {dispatchAddTaskCount, dispatchRemoveTaskCount} from '../../core/AppDataCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import HiPSMOC from '../../drawingLayers/HiPSMOC.js';
import ImageOutline from '../../drawingLayers/ImageOutline.js';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {upload} from '../../rpc/CoreServices.js';
import {dispatchTableFetch} from '../../tables/TablesCntlr.js';
import {ConnectionCtx} from '../../ui/ConnectionCtx.js';
import {FieldGroupCtx} from '../../ui/FieldGroup.jsx';
import {InputAreaFieldConnected} from '../../ui/InputAreaField.jsx';
import {useFieldGroupValue, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {SizeInputFields} from '../../ui/SizeInputField.jsx';
import {DEF_TARGET_PANEL_KEY, TargetPanel} from '../../ui/TargetPanel.jsx';
import {parseObsCoreRegion} from '../../util/ObsCoreSRegionParser.js';
import {CoordinateSys} from '../CoordSys.js';
import {
    dispatchAttachLayerToPlot, dispatchCreateDrawLayer, dispatchDestroyDrawLayer, dlRoot, getDlAry
} from '../DrawLayerCntlr.js';
import {
    dispatchChangeActivePlotView, dispatchChangeHiPS, dispatchDeletePlotView, dispatchPlotHiPS, visRoot
} from '../ImagePlotCntlr.js';
import {NewPlotMode} from '../MultiViewCntlr.js';
import {onPlotComplete} from '../PlotCompleteMonitor.js';
import {
    getActivePlotView, getDrawLayersByType, getPlotViewById, isDrawLayerAttached, primePlot
} from '../PlotViewUtil.js';
import {makeWorldPt, parseWorldPt} from '../Point.js';
import {createHiPSMocLayerFromPreloadedTable} from '../task/PlotHipsTask.js';
import {WebPlotRequest} from '../WebPlotRequest.js';
import {CONE_CHOICE_KEY, POLY_CHOICE_KEY} from './CommonUIKeys.js';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {closeToolbarModalLayers} from './ToolbarToolModalEnd.js';
import {TargetHipsPanelToolbar} from './TargetHipsPanelToolbar.jsx';
import {
    convertStrToWpAry, initSearchSelectTool, updatePlotOverlayFromUserInput,
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
    setWhichOverlay: func,
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

export function VisualTargetPanel({fieldKey, labelWidth= 100, label, labelStyle, feedbackStyle,
                                      targetPanelExampleRow1, targetPanelExampleRow2, ...restOfProps}) {
    const popupButton= (
        <div style={{paddingRight: 2}}>
            <HiPSPanelPopupButton {...{targetKey:fieldKey, whichOverlay:CONE_CHOICE_KEY, ...restOfProps}} />
        </div>
    );
    return ( <TargetPanel {...{fieldKey, button:popupButton, labelStyle, labelWidth, label, feedbackStyle,
        targetPanelExampleRow1, targetPanelExampleRow2}}/> );
}

VisualTargetPanel.propTypes= {
    labelWidth: number,
    targetPanelExampleRow1: PropTypes.arrayOf(PropTypes.string),
    targetPanelExampleRow2: PropTypes.arrayOf(PropTypes.string),
    ...sharedPropTypes
};



export const HiPSTargetView = ({style, hipsDisplayKey='none',
                                   hipsUrl=DEFAULT_HIPS, hipsFOVInDeg= DEFAULT_FOV, centerPt=makeWorldPt(0,0, CoordinateSys.GALACTIC),
                                   targetKey=DEF_TARGET_PANEL_KEY, sizeKey='none---Size', polygonKey='non---Polygon',
                                   whichOverlay= CONE_CHOICE_KEY, toolbarHelpId,
                                   setWhichOverlay, sRegion, coordinateSys, mocList, minSize=1/3600, maxSize=100,
                                   plotId='defaultHiPSTargetSearch', cleanup= false, groupKey}) => {

    const viewerId= plotId+'-viewer';

    const pv= useStoreConnector(() => getPlotViewById(visRoot(),plotId));
    const [getTargetWp,setTargetWp]= useFieldGroupValue(targetKey, groupKey);
    const [getHiPSRadius, setHiPSRadius]= useFieldGroupValue(sizeKey, groupKey);
    const [getPolygon, setPolygon]= useFieldGroupValue(polygonKey, groupKey);
    const {current:lastWhichOverlay}= useRef({lastValue:undefined});

    const userEnterWorldPt= () =>  parseWorldPt(getTargetWp());
    const userEnterSearchRadius= () =>  Number(getHiPSRadius());
    const userEnterPolygon= () => convertStrToWpAry(getPolygon());

    useEffect(() => { // show HiPS plot
        if (!pv || hipsUrl!==pv.request.getHipsRootUrl()) {
            initHiPSPlot({plotId,hipsUrl, viewerId,centerPt,hipsFOVInDeg, coordinateSys,
                userEnterWorldPt, userEnterSearchRadius,
                whichOverlay, userEnterPolygon,
            });
        }
        else {
            const plot= primePlot(pv);
            if (coordinateSys && plot && coordinateSys!==plot.projection.coordSys) {
                dispatchChangeHiPS({plotId,coordSys:coordinateSys});
            }
            updatePlotOverlayFromUserInput(plotId, whichOverlay, userEnterWorldPt(), userEnterSearchRadius(),
                userEnterPolygon(), true);

            if (getActivePlotView(visRoot())?.plotId !== plotId ) dispatchChangeActivePlotView(plotId);
        }
        return () => {
            if (cleanup) dispatchDeletePlotView({plotId});
        };
    },[hipsDisplayKey]);

    useEffect(() => {
        void updateMoc(mocList,plotId);
    }, [mocList]);

    useEffect(() => { // if plot view changes then update the target or polygon field
        updateUIFromPlot({plotId,setWhichOverlay, whichOverlay,setTargetWp,getTargetWp,
            setHiPSRadius,getHiPSRadius,setPolygon,getPolygon,minSize,maxSize,
            canUpdateModalEndInfo:false
        });
    },[pv]);

    useEffect(() => { // if target or radius field change then hips plot to reflect it
        const canGeneratePolygon= whichOverlay!==lastWhichOverlay.lastValue;
        updatePlotOverlayFromUserInput(plotId, whichOverlay, userEnterWorldPt(),
            userEnterSearchRadius(), userEnterPolygon(), false, canGeneratePolygon);
        lastWhichOverlay.lastValue= whichOverlay;
    }, [getTargetWp, getHiPSRadius, getPolygon, whichOverlay]);

    useEffect(() => {
        attachSRegion(sRegion,plotId);
    }, [sRegion]);

    return (
        <div style={{minHeight:200, ...style, display:'flex', flexDirection:'column'}}>
            <MultiImageViewer viewerId= {viewerId} insideFlex={true}
                              canReceiveNewPlots={NewPlotMode.none.key}
                              showWhenExpanded={true}
                              whichOverlay={whichOverlay}
                              toolbarHelpId={toolbarHelpId}
                              Toolbar={TargetHipsPanelToolbar}/>
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
                             minSize:min, maxSize:max, style:{minHeight:400} }}/>
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
 * @param {Function} obj.userEnterWorldPt
 * @param {Function} obj.userEnterSearchRadius
 * @param obj.whichOverlay
 * @param {Function} obj.userEnterPolygon
 * @return {Promise<void>}
 */
async function initHiPSPlot({ hipsUrl, plotId, viewerId, centerPt, hipsFOVInDeg, coordinateSys,
                                userEnterWorldPt, userEnterSearchRadius, whichOverlay, userEnterPolygon}) {
    getDrawLayersByType(dlRoot(), HiPSMOC.TYPE_ID)
        .forEach( ({drawLayerId}) => dispatchDestroyDrawLayer(drawLayerId));// clean up any old moc layers
    const wpRequest= WebPlotRequest.makeHiPSRequest(hipsUrl, centerPt, hipsFOVInDeg);
    wpRequest.setPlotGroupId(plotId+'-group');
    wpRequest.setOverlayIds(['HIPS_GRID_TYPE']);
    wpRequest.setPlotId(plotId);
    if (coordinateSys) {
        wpRequest.setHipsUseCoordinateSys(coordinateSys);
    }
    else if (hipsFOVInDeg>200) {
        wpRequest.setHipsUseCoordinateSys(CoordinateSys.GALACTIC);
    }
    wpRequest.setHipsUseAitoffProjection(hipsFOVInDeg>130);
    dispatchPlotHiPS({plotId, wpRequest, viewerId,
        pvOptions: {
            canBeExpanded:false,
            useForSearchResults:false,
            displayFixedTarget:false,
            userCanDeletePlots: false,
            highlightFeedback: false,
            menuItemKeys: {
                zoomDropDownMenu: false, overlayColorLock: false, matchLockDropDown: false, clickToSearch:false,
                recenter: false, selectArea: true, restore: false,
            }
        }
    });
    await onPlotComplete(plotId);
    dispatchChangeActivePlotView(plotId);

    initSearchSelectTool(plotId);
    if (userEnterWorldPt?.() || userEnterPolygon?.()?.length) {
        await onPlotComplete(plotId);
        updatePlotOverlayFromUserInput(plotId,whichOverlay, userEnterWorldPt?.(), userEnterSearchRadius?.(), userEnterPolygon?.(), true);
    }
}


let abortFunc= undefined;

async function updateMoc(mocList, plotId) {
    abortFunc?.();
    abortFunc= undefined;
    await  onPlotComplete(plotId);
    if (!mocList?.length) { // if no MOCs then remove any on HiPS
        removeAllMocs();
        return;
    }
    const newMocUrls= mocList.map( ({mocUrl}) => mocUrl);
    removeAllMocs(newMocUrls);
    abortFunc= loadMocWithAbort(mocList,plotId);
}

function removeAllMocs(excludeList= []) {
    getDrawLayersByType(getDlAry(), HiPSMOC.TYPE_ID)?.forEach( (dl) => {
        const url= dl?.mocFitsInfo?.mocUrl;
        if (url && excludeList.includes(url)) return;
        dispatchDestroyDrawLayer(dl.drawLayerId);
    });
}


function loadMocWithAbort(mocList, plotId) {
    let abort= false;
    const doAbort= () => abort=true;
    const existingMocUrlsAry= getDrawLayersByType(getDlAry(), HiPSMOC.TYPE_ID)?.map( (dl) => dl.mocFitsInfo.mocUrl);
    const mocAddList= mocList.filter(({mocUrl}) => !existingMocUrlsAry.includes(mocUrl));
    if (!mocAddList.length) {
        return doAbort;
    }

    const loadMOCList= async () => {
        const colors=['yellow', 'red'];

        try {
            for(let i=0; (i<mocAddList.length); i++) {
                const {mocUrl,title,mocColor}= mocAddList[i];

                const tbl_id= 'MOC---'+mocUrl;
                let add= true;
                await  onPlotComplete(plotId);
                if (abort) return;
                if (!getTblById(tbl_id)) {
                    dispatchAddTaskCount(plotId, tbl_id);
                    const {status, cacheKey}=  await upload(mocUrl, 'details', {hipsCache:true});
                    dispatchRemoveTaskCount(plotId, tbl_id);
                    if (abort) return;
                    const request= makeFileRequest(title, cacheKey, undefined, {
                        tbl_id,
                        pageSize: 1,
                        META_INFO: {[MetaConst.IGNORE_MOC]: 'true' }
                    } );
                    dispatchTableFetch(request);
                    dispatchAddTaskCount(plotId, tbl_id);
                    await onTableLoaded(tbl_id);
                    dispatchRemoveTaskCount(plotId, tbl_id);
                    if (abort) return;
                    add= (status === '200');
                }

                if (add) {
                    createHiPSMocLayerFromPreloadedTable({
                        plotId,
                        tbl_id,
                        visible: true,
                        fitsPath: mocUrl,
                        title,
                        color: mocColor ?? colors[i % 2],
                        mocUrl,
                        mocGroupDefColorId: `mocForTargetHipsPanelID-${i}-${mocColor??''}`,
                    });
                }
            }
            const newMocUrls= mocList.map( ({mocUrl}) => mocUrl);
            removeAllMocs(newMocUrls);
        } catch (e) {
            console.log(`loadMOCList, exceptions: ${mocList?.[0]?.mocUrl}`, e);
        }
    };

    setTimeout(() => loadMOCList(),5);
    return () => doAbort;
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



