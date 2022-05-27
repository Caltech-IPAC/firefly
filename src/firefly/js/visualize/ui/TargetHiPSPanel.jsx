import React, {useContext,useEffect} from 'react';
import PropTypes, {arrayOf, shape, oneOf, bool, object, string, number} from 'prop-types';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {
    dispatchAttributeChange, dispatchChangeCenterOfProjection,
    dispatchDeletePlotView, dispatchPlotHiPS, visRoot
} from '../ImagePlotCntlr.js';
import {NewPlotMode} from '../MultiViewCntlr.js';
import {WebPlotRequest} from '../WebPlotRequest.js';
import {PlotAttribute} from '../PlotAttribute.js';
import CsysConverter from '../CsysConverter.js';
import {CoordinateSys} from '../CoordSys.js';
import {onPlotComplete} from '../PlotCompleteMonitor.js';
import {isValidPoint, makeDevicePt, makeWorldPt, parseWorldPt, pointEquals} from '../Point.js';
import {
    getDrawLayerByType, getDrawLayersByType, getPlotViewById,
    isDrawLayerAttached, primePlot
} from '../PlotViewUtil.js';
import {
    dispatchAttachLayerToPlot, dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
    dispatchForceDrawLayerUpdate, dlRoot, getDlAry } from '../DrawLayerCntlr.js';
import {createHiPSMocLayer} from '../task/PlotHipsTask.js';
import {GroupKeyCtx} from '../../ui/FieldGroup.jsx';
import {useFieldGroupValue, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {SizeInputFields} from '../../ui/SizeInputField.jsx';
import SearchSelectTool from '../../drawingLayers/SearchSelectTool.js';
import HiPSMOC from '../../drawingLayers/HiPSMOC.js';
import {ToolbarButton} from 'firefly/ui/ToolbarButton.jsx';
import CLICK from 'html/images/20x20_click.png';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {dispatchShowDialog, dispatchHideDialog} from 'firefly/core/ComponentCntlr.js';
import {LayoutType, PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import {computeDistance} from 'firefly/visualize/VisUtil.js';
import {closeToolbarModalLayers} from 'firefly/visualize/ui/VisMiniToolbar.jsx';

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
    coordinateSys: oneOf([CoordinateSys.GALACTIC, CoordinateSys.EQ_J2000]),
    mocList: arrayOf( shape({ mocUrl: string, title: string }) ),
};

export function VisualTargetPanel({groupKey:gk, fieldKey, labelWidth= 100, ...restOfProps}) {
    useEffect(() => {
        return () => {
            dispatchHideDialog(DIALOG_ID);
        };
    },[]);

    const context= useContext(GroupKeyCtx);
    const groupKey= gk || context.groupKey;
    const popupButton= (
        <div style={{paddingRight: 2}}>
            <ToolbarButton icon={CLICK} tip={'Choose target visually'} bgDark={true}
                           imageStyle={{height:18, width:18}} horizontal={true}
                           onClick={(element) =>
                               showHiPSPanelPopup({ ...restOfProps, targetKey:fieldKey, element, groupKey})} />
        </div>
    );

    return (
        <TargetPanel fieldKey={fieldKey} button={popupButton} labelWidth={labelWidth}/>
    );
}

VisualTargetPanel.propTypes= {
    labelWidth: number,
    ...sharedPropTypes
};



export const HiPSTargetView = ({style, hipsUrl=DEFAULT_HIPS, hipsFOVInDeg= DEFAULT_FOV, centerPt=makeWorldPt(0,0),
                                   targetKey='UserTargetWorldPt', sizeKey='none', coordinateSys, mocList,
                                   minSize=1/3600, maxSize=100,
                                   plotId='defaultHiPSTargetSearch', cleanup= false, groupKey}) => {

    const viewerId= plotId+'-viewer';

    const pv= useStoreConnector(() => getPlotViewById(visRoot(),plotId));
    const [getTargetWp,setTargetWp]= useFieldGroupValue(targetKey, groupKey);
    const [getHiPSRadius, setHiPSRadius]= useFieldGroupValue(sizeKey, groupKey);

    const userEnterWorldPt= () =>  parseWorldPt(getTargetWp());
    const userEnterSearchRadius= () =>  Number(getHiPSRadius());

    useEffect(() => { // show HiPS plot
        if (!pv || hipsUrl!==pv.request.getHipsRootUrl()) {
            initHiPSPlot({plotId,hipsUrl, viewerId,centerPt,hipsFOVInDeg, coordinateSys, mocList,
                minSize, maxSize,
                userEnterWorldPt:userEnterWorldPt(), userEnterSearchRadius:userEnterSearchRadius()});
        }
        else {
            updatePlotOverlayFromUserInput(plotId, userEnterWorldPt(), userEnterSearchRadius(), true);
        }
        return () => {
            if (cleanup) dispatchDeletePlotView({plotId});
        };
    },[]);

    useEffect(() => { // if plot view changes then update the target field
        const plot= primePlot(visRoot(),plotId);
        if (!plot) return;

        if (plot.attributes[PlotAttribute.SELECTION]) {
            const {cenWpt, radius}= getPtRadiusFromSelection(plot);
            if (!cenWpt) return;
            const drawRadius= radius<=maxSize ? (radius >= minSize ? radius : minSize) : maxSize;
            if (pointEquals(userEnterWorldPt(),cenWpt) && drawRadius===userEnterSearchRadius()) return;
            setTargetWp(cenWpt.toString());
            setHiPSRadius(drawRadius+'');
            updatePlotOverlayFromUserInput(plotId, cenWpt, drawRadius);
        }
        else {
            const wp= plot.attributes[PlotAttribute.USER_SEARCH_WP];
            if (!wp) return;
            const utWPt= userEnterWorldPt();
            if (!utWPt || (isValidPoint(utWPt) && !pointEquals(wp,utWPt ))) {
                setTargetWp(wp.toString());
            }
        }

    },[pv]);

    useEffect(() => { // if target or radius field change then hips plot to reflect it
        updatePlotOverlayFromUserInput(plotId, userEnterWorldPt(), userEnterSearchRadius());
    }, [getTargetWp, getHiPSRadius]);

    // if (!groupKey) {
    //     logger.error('group key must be defined, as property or part of context');
    //     return <div/>;
    // }

    return (
        <div style={{height:500, ...style, display:'flex', flexDirection:'column'}}>
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
        <HiPSTargetView {...{...restOfProps, targetKey:'UserTargetWorldPt', sizeKey:'HiPSPanelRadius',
                             minSize:min, maxSize:max, style:{height:400} }}/>
        <div style={{display:'flex', flexDirection:'column', marginLeft:100}}>
            <TargetPanel style={{paddingTop: 10}} labelWidth={100}/>
            <SizeInputFields fieldKey='HiPSPanelRadius' showFeedback={true} labelWidth= {100}  nullAllowed={false}
                             label={'Search Area'}
                             initialState={{ unit: 'arcsec', value: searchAreaInDeg+'', min, max }} />
        </div>
    </div>
);

TargetHiPSPanel.propTypes= {
    searchAreaInDeg: PropTypes.number.isRequired,
    ...sharedPropTypes,
};

export const TargetHiPSRadiusPopupPanel = ({searchAreaInDeg, targetKey='UserTargetWorldPt', sizeKey= 'HiPSPanelRadius', style,
                                              minValue:min= 1/3600, maxValue:max= 100,
                                               ...restOfProps}) => (
    <div style={{display:'flex', width: 700, paddingBottom: 20, flexDirection:'column', ...style}}>
        <div style={{display:'flex', flexDirection:'column'}}>
            <VisualTargetPanel {...{style:{paddingTop: 10}, fieldKey:targetKey, sizeKey, labelWidth:100,
                minSize:min, maxSize:max, ...restOfProps}} />
            <SizeInputFields {...{
                fieldKey:sizeKey, showFeedback:true, labelWidth:100, nullAllowed:false,
                label:'Search Area',
                initialState:{ unit: 'arcsec', value: searchAreaInDeg+'', min, max }
            }} />
        </div>
    </div>
);

TargetHiPSRadiusPopupPanel.propTypes= {
    searchAreaInDeg: PropTypes.number.isRequired,
    sizeKey: PropTypes.string,
    ...sharedPropTypes,
};


const defPopupPlotId= 'defaultHiPSPopupTargetSearch';

function showHiPSPanelPopup({element, plotId= defPopupPlotId, ...restOfProps}) {

    const doClose= () => {
        closeToolbarModalLayers();
    };

    const hipsPanel= (
        <PopupPanel title={'Choose Target'} layoutPosition={LayoutType.TOP_RIGHT}
                    closeCallback={() => doClose()} >
            <div style={{
                padding: 3, display:'flex', flexDirection:'column', width: 500,
                alignItems:'center', resize:'both', overflow: 'hidden', zIndex:1}}>
                <HiPSTargetView {...{
                    plotId,
                    ...restOfProps,
                    style:{height:400, width:'100%', paddingBottom:4} }} />
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
 * @param obj.minSize
 * @param obj.maxSize
 * @param obj.userEnterWorldPt
 * @param obj.userEnterSearchRadius
 * @return {Promise<void>}
 */
async function initHiPSPlot({ hipsUrl, plotId, viewerId, centerPt, hipsFOVInDeg, coordinateSys, mocList,
                                minSize, maxSize, userEnterWorldPt, userEnterSearchRadius }) {
    getDrawLayersByType(dlRoot(), HiPSMOC.TYPE_ID)
        .forEach( ({drawLayerId}) => dispatchDestroyDrawLayer(drawLayerId));// clean up any old moc layers
    const wpRequest= WebPlotRequest.makeHiPSRequest(hipsUrl, centerPt, hipsFOVInDeg);
    wpRequest.setPlotGroupId(plotId+'-group');
    wpRequest.setOverlayIds(['none']);
    wpRequest.setPlotId(plotId);
    coordinateSys && wpRequest.setHipsUseCoordinateSys(coordinateSys);
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
    
    const dl= getDrawLayerByType(getDlAry(), SearchSelectTool.TYPE_ID);
    !dl && dispatchCreateDrawLayer(SearchSelectTool.TYPE_ID);
    !isDrawLayerAttached(dl,plotId) && dispatchAttachLayerToPlot(SearchSelectTool.TYPE_ID,plotId,false);
    if (userEnterWorldPt) {
        updatePlotOverlayFromUserInput(plotId,userEnterWorldPt, userEnterSearchRadius, true);
    }
}

function updatePlotOverlayFromUserInput(plotId, wp, radius, forceCenterOn= false) {
    const dl= getDrawLayerByType(getDlAry(), SearchSelectTool.TYPE_ID);
    if (!dl) return;
    dispatchAttributeChange( {plotId, changes:{[PlotAttribute.USER_SEARCH_WP]:wp} });
    dispatchAttributeChange( {plotId, changes:{[PlotAttribute.USER_SEARCH_RADIUS_DEG]:radius}});
    dispatchForceDrawLayerUpdate(dl.drawLayerId, plotId);

    const cc= CsysConverter.make(primePlot(visRoot(),plotId));
    if ((!cc || cc.pointInView(wp)) && !forceCenterOn) return;
    dispatchChangeCenterOfProjection({plotId, centerProjPt:wp});
}

/**
 * @param {WebPlot} plot
 * @return {{}|{radius: number, cenWpt: WorldPt}}
 */
function getPtRadiusFromSelection(plot) {
    const {pt0,pt1}= plot.attributes[PlotAttribute.SELECTION];
    if (!pt0 || !pt1) return {};
    const cc= CsysConverter.make(plot);
    const dPt0= cc.getDeviceCoords(pt0);
    const dPt1= cc.getDeviceCoords(pt1);
    const cen= makeDevicePt( (dPt0.x+dPt1.x)/2, (dPt0.y+dPt1.y)/2 );
    const cenWpt= cc.getWorldCoords(cen);
    if (!cenWpt) return {};
    const sideWPx= cc.getWorldCoords( makeDevicePt( dPt0.x,cen.y));
    const sideWPy= cc.getWorldCoords( makeDevicePt( cen.x,dPt0.y));
    const radius= Math.min(computeDistance(sideWPx,cenWpt), computeDistance(sideWPy,cenWpt));
    return {cenWpt, radius};
}