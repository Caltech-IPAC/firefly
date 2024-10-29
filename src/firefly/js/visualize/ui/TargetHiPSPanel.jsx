/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Stack, Typography} from '@mui/joy';
import {dispatchHideDialog, dispatchShowDialog, isDialogVisible} from 'firefly/core/ComponentCntlr.js';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {LayoutType, PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import {ToolbarButton} from 'firefly/ui/ToolbarButton.jsx';
import {computeCentralPointAndRadius,} from 'firefly/visualize/VisUtil.js';
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
import {PlotAttribute} from '../PlotAttribute';
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
import {HelpLines, TargetHipsPanelToolbar} from './TargetHipsPanelToolbar.jsx';
import {
    convertStrToWpAry, initSearchSelectTool, updatePlotOverlayFromUserInput,
    updateUIFromPlot
} from './VisualSearchUtils.js';
import AdsClickIcon from '@mui/icons-material/AdsClick';

const DIALOG_ID= 'HiPSPanelPopup';
const DEFAULT_HIPS= 'ivo://CDS/P/DSS2/color';
const DEFAULT_FOV= 340;
const RADIUS_DISABLED_KEY= 'none---Size';

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
    mocList: arrayOf( shape({ mocUrl: string, title: string, mocColor:string }) ),
};

export function VisualPolygonPanel({label, initValue, tooltip, fieldKey, sx, placeholder,
                                       placeholderHighlight,
                                       manageHiPS=true, ...restOfProps}) {

    const button= manageHiPS &&
        ( <HiPSPanelPopupButton {...{polygonKey:fieldKey, whichOverlay:POLY_CHOICE_KEY, ...restOfProps}} /> );
    return (
        <InputAreaFieldConnected {...{
            fieldKey, placeholder, label, button, tooltip, initialState:{value:initValue}, sx,
            placeholderHighlight
        }} />
    );
}

export function VisualTargetPanel({fieldKey, label, feedbackStyle,
                                      targetPanelExampleRow1, targetPanelExampleRow2, manageHiPS=true, ...restOfProps}) {
    const popupButton= manageHiPS && (
        <Box sx={{pr: 1/4}}>
            <HiPSPanelPopupButton {...{targetKey:fieldKey, whichOverlay:CONE_CHOICE_KEY, ...restOfProps}} />
        </Box>
    );
    return ( <TargetPanel {...{fieldKey, button:popupButton, label, feedbackStyle,
        targetPanelExampleRow1, targetPanelExampleRow2}}/> );
}

VisualTargetPanel.propTypes= {
    targetPanelExampleRow1: PropTypes.arrayOf(PropTypes.string),
    targetPanelExampleRow2: PropTypes.arrayOf(PropTypes.string),
    ...sharedPropTypes
};



export const HiPSTargetView = ({sx, hipsDisplayKey='none',
                                   hipsUrl=DEFAULT_HIPS, hipsFOVInDeg= DEFAULT_FOV, centerPt=makeWorldPt(0,0, CoordinateSys.GALACTIC),
                                   targetKey=DEF_TARGET_PANEL_KEY, sizeKey=RADIUS_DISABLED_KEY, polygonKey='non---Polygon',
                                   whichOverlay= CONE_CHOICE_KEY, toolbarHelpId,
                                   setWhichOverlay, sRegion, coordinateSys, mocList, minSize=1/3600, maxSize=100,
                                   plotId='defaultHiPSTargetSearch', cleanup= false, groupKey}) => {

    const viewerId= plotId+'-viewer';

    const pv= useStoreConnector(() => getPlotViewById(visRoot(),plotId));
    const [getTargetWp,setTargetWp]= useFieldGroupValue(targetKey, groupKey);
    const [getHiPSRadius, setHiPSRadius]= useFieldGroupValue(sizeKey, groupKey);
    const [getPolygon, setPolygon]= useFieldGroupValue(polygonKey, groupKey);
    const [mocError, setMocError]= useState();
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
        setMocError();
        void updateMoc(mocList,plotId,setMocError);
    }, [mocList]);

    useEffect(() => { // if plot view changes then update the target or polygon field
        const setWhichOverlayWrapper= setWhichOverlay ?
            (overLay) => {
                setWhichOverlay(overLay);
                lastWhichOverlay.lastValue= overLay;
            } : undefined;
        updateUIFromPlot({plotId,setWhichOverlay:setWhichOverlayWrapper, whichOverlay,setTargetWp,getTargetWp,
            setHiPSRadius,getHiPSRadius,setPolygon,getPolygon,minSize,maxSize,
            canUpdateModalEndInfo:false
        });
    },[pv]);

    useEffect(() => { // if target or radius field change then hips plot to reflect it
        const canGenerate= whichOverlay!==lastWhichOverlay.lastValue;
        if (canGenerate && whichOverlay===CONE_CHOICE_KEY && !userEnterWorldPt()) {
             const wp = primePlot(visRoot(), plotId)?.attributes[PlotAttribute.USER_SEARCH_WP];
             wp && setTargetWp(wp.toString());
         }
        const radius= sizeKey!==RADIUS_DISABLED_KEY ? userEnterSearchRadius() : undefined;

        updatePlotOverlayFromUserInput(plotId, whichOverlay, userEnterWorldPt(),
            radius, userEnterPolygon(), false, canGenerate);
        lastWhichOverlay.lastValue= whichOverlay;
    }, [getTargetWp, getHiPSRadius, getPolygon, whichOverlay]);

    useEffect(() => {
        attachSRegion(sRegion,plotId);
    }, [sRegion]);

    return (
        <Stack {...{minHeight:200, ...sx,}}>
            {mocError &&
                <Typography level='h4' color='danger' textAlign='center'>
                    {`The coverage MOC is unavailable${mocError?.title?': '+mocError.title:''}`}
                </Typography>}
            <HelpLines whichOverlay={whichOverlay}/>
            <MultiImageViewer viewerId= {viewerId} insideFlex={true}
                              canReceiveNewPlots={NewPlotMode.none.key}
                              showWhenExpanded={true}
                              whichOverlay={whichOverlay}
                              toolbarHelpId={toolbarHelpId}
                              handleToolbar={false}
                              Toolbar={TargetHipsPanelToolbar}/>
        </Stack>
    );
};

HiPSTargetView.propTypes= {
    ...sharedPropTypes,
    sizeKey: string,
    targetKey: string,
};


export const TargetHiPSPanel = ({searchAreaInDeg, style,
                                    minValue:min= 1/3600, maxValue:max= 100, ...restOfProps}) => (
    <Stack {...{width: 700, height:700, ...style}}>
        <HiPSTargetView {...{...restOfProps, targetKey:DEF_TARGET_PANEL_KEY, sizeKey:'HiPSPanelRadius',
                             minSize:min, maxSize:max, sx:{minHeight:400} }}/>
        <Stack {...{ml:12.5}}>
            <TargetPanel sx={{pt: 1, width:'34rem'}}/>
            <SizeInputFields fieldKey='HiPSPanelRadius' showFeedback={true} nullAllowed={false}
                             label='Search Area'
                             labelStyle={{textAlign:'right', paddingRight:4}}
                             initialState={{ unit: 'arcsec', value: searchAreaInDeg+'', min, max }} />
        </Stack>
    </Stack>
);

TargetHiPSPanel.propTypes= {
    searchAreaInDeg: PropTypes.number.isRequired,
    ...sharedPropTypes,
};

export const TargetHiPSRadiusPopupPanel = ({searchAreaInDeg, sx,
                                               targetKey=DEF_TARGET_PANEL_KEY, sizeKey= 'HiPSPanelRadius', polygonKey,
                                              minValue:min= 1/3600, maxValue:max= 100, targetLabelStyle,
                                               sizeLabel= 'Search Area:', ...restOfProps}) => {
    const [controlConnected, setControlConnected] = useState(false);
    return (
        <ConnectionCtx.Provider value={{controlConnected, setControlConnected}}>
            <Stack {...{width: 700, pb: 3, ...sx}}>
                <Stack >
                    <VisualTargetPanel {...{style:{paddingTop: 10}, fieldKey:targetKey, sizeKey, polygonKey,
                        labelStyle:targetLabelStyle, minSize:min, maxSize:max, ...restOfProps}} />
                    <SizeInputFields {...{
                        fieldKey:sizeKey, showFeedback:true, nullAllowed:false,
                        label: sizeLabel,
                        labelStyle:{textAlign:'right', paddingRight:4},
                        initialState:{ unit: 'arcsec', value: searchAreaInDeg+'', min, max }
                    }} />
                </Stack>
            </Stack>
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
            icon:<AdsClickIcon/> , tip, imageStyle:{height:18, width:18},
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
            <Stack style={{
                p:1/2, width: 500, height:400,
                alignItems:'center', resize:'both', overflow: 'hidden', zIndex:1}}>
                <HiPSTargetView {...{ plotId, whichOverlay, ...restOfProps, sx:{height:1, width:1, pb:1/5} }} />
            </Stack>
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
            embedMainToolbar: true,
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

async function updateMoc(mocList, plotId,setMocError) {
    abortFunc?.();
    abortFunc= undefined;
    await  onPlotComplete(plotId);
    if (!mocList?.length) { // if no MOCs then remove any on HiPS
        removeAllMocs();
        return;
    }
    const newMocUrls= mocList.map( ({mocUrl}) => mocUrl);
    removeAllMocs(newMocUrls);
    abortFunc= loadMocWithAbort(mocList,plotId,setMocError);
}

function removeAllMocs(excludeList= []) {
    getDrawLayersByType(getDlAry(), HiPSMOC.TYPE_ID)?.forEach( (dl) => {
        const url= dl?.mocFitsInfo?.mocUrl;
        if (url && excludeList.includes(url)) return;
        dispatchDestroyDrawLayer(dl.drawLayerId);
    });
}


function loadMocWithAbort(mocList, plotId,setMocError) {
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
                    setMocError();
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
            setMocError(mocList?.[0]);
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



