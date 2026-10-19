/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Box, Button, Divider, Stack, Tooltip, Typography} from '@mui/joy';
import {isUndefined} from 'lodash';
import React, {useEffect, useState} from 'react';
import {getAppOptions} from '../../../api/ApiUtil.js';
import {allowPinnedCharts} from '../../../charts/ChartUtil';
import {ensureDefaultChart} from '../../../charts/ui/ChartsContainer.jsx';
import {pinChart} from '../../../charts/ui/PinnedChartContainer.jsx';
import {downloadChart, PlotlyWrapper} from '../../../charts/ui/PlotlyWrapper.jsx';
import {dispatchShowDialog} from '../../../core/ComponentCntlr.js';
import {dispatchAddActionWatcher} from '../../../core/MasterSaga.js';
import ExtractLineTool, {
    addLineDistAttributesToPlots, COLUMN_SELECTION, FREE_SELECTION, LINE_SELECTION
} from '../../../drawingLayers/ExtractLineTool.js';
import ExtractPointsTool from '../../../drawingLayers/ExtractPointsTool.js';
import {callGetCubeDrillDownAry, callGetFileFlux, callGetPointExtractionAry} from '../../../rpc/PlotServicesJson.js';
import {onTableLoaded} from '../../../tables/TableUtil.js';
import CompleteButton from '../../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../../ui/DialogRootContainer.jsx';
import {FieldGroup} from '../../../ui/FieldGroup';
import HelpIcon from '../../../ui/HelpIcon.jsx';
import {ListBoxInputField, ListBoxInputFieldView} from '../../../ui/ListBoxInputField.jsx';
import {PopupPanel} from '../../../ui/PopupPanel.jsx';
import {CHART_RESIZE_DEBOUNCE, wrapResizeMonitor} from '../../../ui/ResizeMonitor';
import {useFieldGroupValue, useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {ValidationField} from '../../../ui/ValidationField';
import {intValidator} from '../../../util/Validate';
import {Band} from '../../Band';
import {CCUtil, CysConverter} from '../../CsysConverter.js';
import {dispatchAttachLayerToPlot, dispatchCreateDrawLayer, dispatchModifyCustomField} from '../../DrawLayerDispatch';
import {getExtName, hasFloatingData} from '../../FitsHeaderUtil.js';
import {getHiPSFitsTileLocation, getHiPSNorderlevel, isHiPSFitsCube} from '../../HiPSUtil.js';
import {
    dispatchAttributeChange, dispatchChangeHiPS, dispatchChangePointSelection, dispatchChangePrimePlot
} from '../../ImagePlotDispatch';
import {STATUS_VALUE} from '../../MouseReadoutCntlr';
import {PlotAttribute} from '../../PlotAttribute.js';
import {
    convertHDUIdxToImageIdx, currentP, getCubePlaneFromWavelength, getDrawLayerByType, getHDU, getHDUIndex,
    getCubePlaneIdx, getPlotViewAry, hasWCSProjection, hasWLInfo, isDrawLayerAttached, isCube,
    primePlot, getPlotStateAry
} from '../../PlotViewUtil.js';
import {makeImagePt} from '../../Point';
import {PLOT_IMAGE} from '../../VisConst';
import {getDlAry, visRoot} from '../../VisStoreRoots';
import {computeDistance, computeScreenDistance, getLinePointAry} from '../../VisUtil.js';
import {isHiPS, isImage} from '../../WebPlot.js';
import {genPointChartData, genSliceChartData, genZAxisChartData, POINTS_CURVE_NUM} from './ExtractionChart.jsx';
import {keepDataExtraction, keepHiPSPointExtraction, keepZAxisExtraction} from './ExtractionTable.jsx';
import {
    EXTRACT_DIALOG_ID, endExtraction, EXTRACT_END_ID, ZAXIS_POINT_SELECTION_ID, POINT_CHART_TITLE, makeHiPSLinePtAry,
    makeHiPSLineExtractionTitle, makeExtractPtAry, makeLineExtractionTitle, CHART_ID, HIPS_APERTURE_NOTE, CUBE_WARNING,
    SELECT_TYPE_TIP
} from './ExtractionUIUtil';



const exTypeCntl= {
    Z_AXIS: {
        Panel: ZAxisExtractionPanel,
        start: () => dispatchChangePointSelection(ZAXIS_POINT_SELECTION_ID, true),
    },
    LINE: {
        Panel: LineExtractionPanel,
        start: () => enableDrawLayer(ExtractLineTool.TYPE_ID),
    },
    POINTS: {
        Panel: PointExtractionPanel,
        start: () => enableDrawLayer(ExtractPointsTool.TYPE_ID),
    }

};

function enableDrawLayer(typeId) {
    const {pv}= currentP();
    const dl= getDrawLayerByType(getDlAry(), typeId);
    !dl && dispatchCreateDrawLayer(typeId);
    !isDrawLayerAttached(dl,pv.plotId) && dispatchAttachLayerToPlot(typeId,pv.plotId,true,true, true);
}


export function showExtractionDialog(element,extractionType,wasCanceled) {
    endExtraction();
    exTypeCntl[extractionType].start();
    DialogRootContainer.defineDialog(EXTRACT_DIALOG_ID, <ExtractDialog {...{extractionType, wasCanceled}}/>, element );
    dispatchShowDialog(EXTRACT_DIALOG_ID);


    dispatchAddActionWatcher( {
        id: EXTRACT_END_ID,
        callback: () => exTypeCntl[extractionType].start(),
        actions:[PLOT_IMAGE]
    });
}


function ExtractDialog({extractionType,wasCanceled}) {
    const {pv, pvCnt} = useStoreConnector( getStoreState);
    const {canCreateExtractionTable}= getAppOptions().image;
    const {Panel}= exTypeCntl[extractionType];

    const doCancel= () => {
        endExtraction();
        wasCanceled?.();
    };

    return(
        <PopupPanel {...{
            title:`Extract: ${primePlot(pv)?.title ?? ''}`,
            slotProps: {dialogTitle:{sx:{maxWidth:'30rem'}}},
            closeCallback:doCancel,
            requestToClose:doCancel }}>
            <Panel canCreateExtractionTable={canCreateExtractionTable} pv={pv} pvCnt={pvCnt}/>
        </PopupPanel>
    );
}



function getStoreState(prevResult) {
    const vr= visRoot();
    const {pv:activePv}= currentP();
    const pv= primePlot(activePv)
        ? activePv
        : currentP(vr.prevActivePlotId).plot
            ? currentP(vr.prevActivePlotId).pv
            : activePv;
    const pvAry= pv ? getPlotViewAry(visRoot(), pv.plotGroupId) : [];
    if (prevResult && prevResult.pv===pv && prevResult.pvCnt===pvAry.length && primePlot(pv)===primePlot(prevResult.pv)) return prevResult;
    return {pv,pvCnt:pvAry.length};
}


const AVG= 'AVG';
const combineOps= [
    {label: 'Average', value: AVG},
    {label: 'Sum', value: 'SUM'}
];
const combineIntOps= [
    ...combineOps,
    {label: 'Logical OR', value: 'OR'}
];


const canExtractZAxis= (plot) => (isImage(plot) && isCube(plot)) || isHiPSFitsCube(plot);

function afterZAxisChartRedraw(imPt, inPv, chart) {
    chart.on('plotly_click', (ev) => {
        setTimeout( () => {
            const {pv,plot,plotId}= currentP(inPv.plotId);
            const plane= hasWLInfo(plot) ? getCubePlaneFromWavelength(pv,ev.points[0].x,imPt) : ev.points[0].x-1;
            if (isHiPSFitsCube(plot)) {
                if (plane>-1 && plot.cubeIdx!==plane) dispatchChangeHiPS({plotId, cubeIdx:plane});
            }
            else {
                const primeIdx= convertHDUIdxToImageIdx(pv, getHDUIndex(pv,plot), plane);
                if (pv.primeIdx!==primeIdx) dispatchChangePrimePlot({plotId,primeIdx});
            }
        },5);
    });
}

function afterLineChartRedraw(pv, chart,pl, imPtAry) {
    chart.on('plotly_click', (ev) => {
        const evPt= ev.points.find( (p) => p.curveNumber===POINTS_CURVE_NUM); // ignore clicks on the highlight point
        if (!evPt) return;
        setTimeout( () => {
            const plot= primePlot(pv);
            const imPt= makeImagePt(Math.round(imPtAry[evPt.pointNumber].x)+.5, Math.round(imPtAry[evPt.pointNumber].y)+.5);
            dispatchModifyCustomField( ExtractLineTool.TYPE_ID,
                {activePt: hasWCSProjection(plot) ? CCUtil.getWorldCoords(plot,imPt) : imPt},
                pv.plotId);
            dispatchAttributeChange({plotId:plot.plotId,overlayColorScope:false,toAllPlotsInPlotView:false,
                changes:{
                    [PlotAttribute.SELECT_ACTIVE_CHART_PT]: {x:evPt.x,y:evPt.y}
                }
            });
        },5);
    });
}

function afterPointsChartRedraw(pv, chart,pl,chartXAxis, imPtAry) {
    chart.on('plotly_click', (ev) => {
        const evPt= ev.points.find( (p) => p.curveNumber===POINTS_CURVE_NUM); // ignore clicks on the highlight point
        if (!evPt) return;
        setTimeout( () => {
            const plot= primePlot(pv);
            const {x,y,pointNumber:idx}= evPt;
            const imPt= imPtAry[idx];
            if (!imPt) return;
            const cenImPt= makeImagePt(imPt.x+.5, imPt.y+.5);
            dispatchModifyCustomField( ExtractPointsTool.TYPE_ID,
                {activePt: hasWCSProjection(plot) ? CCUtil.getWorldCoords(plot,cenImPt) : cenImPt}, pv.plotId);
            dispatchAttributeChange({plotId:plot.plotId,overlayColorScope:false,toAllPlotsInPlotView:false,
                changes:{[PlotAttribute.SELECT_ACTIVE_CHART_PT]: {x,y,chartXAxis,idx}}
            });
        },5);
    });
}


function ExtractionChart({plotlyDivStyle, plotlyData, plotlyLayout, afterRedraw, size:{width,height}}) {
    return (
        <div style={{width:'100%', display:'flex', flexDirection: 'column', overflow:'hidden'}}>
                <PlotlyWrapper data={plotlyData} layout={{width,height,...plotlyLayout}}  style={plotlyDivStyle}
                               autoSizePlot={true}
                               autoDetectResizing={true}
                               chartId={CHART_ID}
                               divUpdateCB={() => undefined}
                               newPlotCB={ (chart,pl) => afterRedraw(chart,pl) } />
        </div>
    );
}

const ExtractionChartResizeable= wrapResizeMonitor(ExtractionChart,CHART_RESIZE_DEBOUNCE);



function PointExtractionPanel({canCreateExtractionTable, pv, pvCnt}) {
    const [{plotlyDivStyle, plotlyData, plotlyLayout},setChartParams]= useState({});
    const [pointSize,setPointSize]= useState(1);
    const [combineOp,setCombineOp]= useState(AVG);
    const [chartXAxis]= useState('imageX');
    const plot= primePlot(pv);
    const hips= isHiPS(plot);
    const ptAry=plot?.attributes?.[PlotAttribute.PT_ARY] ??[];
    const nOrder= hips ? getHiPSPointNorder(plot) : undefined;
    const {foundPts, imPtAry, hipsTileLocAry, wptAry}= makeExtractPtAry(plot, ptAry, nOrder);

    const {plotId,plotImageId,cubeIdx}= plot ?? {};
    const hduNum= getHDU(plot); // not used for HiPS, the server uses the first image HDU of the tile
    const plane= getCubePlaneIdx(plot)>-1 ? getCubePlaneIdx(plot) : 0;
    const {x:chartX,y:chartY,chartXAxis:lastChartChartXAxis=chartXAxis,idx:chartIdx}=plot?.attributes?.[PlotAttribute.SELECT_ACTIVE_CHART_PT] ?? {};

    useEffect(() => {
        let active= true;
        const getData= async () => {
            if (!foundPts) return;
            const dataAry= await callGetPointExtractionAry(
                {plot, hduNum, plane, ptAry:hips?hipsTileLocAry:imPtAry, ptSizeX:pointSize, ptSizeY:pointSize, combineOp});
            if (!active) return;
            const hipsUnits= hips ? await getHiPSPointUnits(pv, hipsTileLocAry[0], wptAry[0]) : '';
            if (!active) return;
            const chartPtAry= wptAry ?? imPtAry; // RA or Dec for HiPS or an image with a WCS, otherwise image x or y
            let activeIdx= 0;
            const key= lastChartChartXAxis==='imageX' ? 'x' : 'y';
            if (plot.attributes[PlotAttribute.SELECT_ACTIVE_CHART_PT]) {
                activeIdx= chartPtAry[chartIdx]?.[key]===chartX ? chartIdx : chartPtAry.findIndex( (pt) => pt[key]===chartX);
                if (activeIdx<0) activeIdx= 0;
            }
            const chartData=
                genPointChartData(plot, dataAry,chartPtAry, chartPtAry[activeIdx][key], dataAry[activeIdx],
                    pointSize,combineOp, POINT_CHART_TITLE, chartXAxis, activeIdx, hipsUnits);
            setChartParams(chartData);
        };
        getData();
        return () => { active= false; };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    },[ptAry.length,hduNum,plotId,plotImageId,cubeIdx,nOrder,pointSize,combineOp,chartX,chartY,chartXAxis,chartIdx]);

    return (
        <ExtractionPanelView {...{
            pointSize, setPointSize, combineOp, setCombineOp,
            plotlyDivStyle, plotlyData, plotlyLayout, canCreateExtractionTable,
            hasFloatingData:hasFloatingData(plot),
            startUpHelp: (
                <div>
                    <Typography>Click on an image to extract a point, continue clicking to extract more points. </Typography>
                    {pvCnt>1 && <Typography {...{mt:3}}>Shift-click will change the selected image without extracting points. </Typography>}
                    {hips && <Typography {...{mt:3, level:'body-sm'}}>{HIPS_APERTURE_NOTE}</Typography>}
                </div>
            ),
            afterRedraw: (chart,pl) => afterPointsChartRedraw(pv,chart,pl,chartXAxis, imPtAry),
            callKeepExtraction: (save, doOverlay) => hips
                ? keepHiPSPointExtraction({pv, hipsTileLocAry, hipsWptAry: wptAry, plane, pointSize, combineOp, save, doOverlay})
                : keepDataExtraction({pv,baseImPtAry:imPtAry,save,doOverlay,pointSize,combineOp})
        }} /> );
}

/**
 * get the flux units of the HiPS FITS tiles by doing a flux call on one point
 * @param {PlotView} pv
 * @param {{tileUrl:String, tilePt:ImagePt}} tileLoc - the FITS tile location of the point
 * @param {WorldPt} wpt - the point
 * @return {Promise<String>} the units, empty string if they can not be determined
 */
async function getHiPSPointUnits(pv, tileLoc, wpt) {
    try {
        const plot= primePlot(pv);
        const result= await callGetFileFlux(getPlotStateAry(pv), tileLoc.tilePt, wpt, true, tileLoc.tileUrl, plot?.cubeIdx);
        const fluxResult= result?.[Band.NO_BAND.key];
        return fluxResult?.status===STATUS_VALUE ? (fluxResult.unit ?? '') : '';
    } catch {
        return '';
    }
}

/**
 * the norder of the HiPS FITS tiles to extract points from, FITS cube tiles only exist at hips_order,
 * otherwise use the norder that is currently displayed
 * @param {WebPlot} plot
 * @return {number}
 */
const getHiPSPointNorder= (plot) =>
    plot.hasFitsCube ? Number(plot.hipsProperties?.hips_order) : getHiPSNorderlevel(plot, true).norder;



function LineExtractionPanel({canCreateExtractionTable, pv, pvCnt}) {
    const dl= useStoreConnector(() => getDrawLayerByType(getDlAry(), ExtractLineTool.TYPE_ID) ?? {});
    const {selectionType= FREE_SELECTION, helpWarning=false}= dl;
    const [{plotlyDivStyle, plotlyData, plotlyLayout},setChartParams]= useState({});
    const [imPtAry,setImPtAry]= useState(undefined);
    const [pointSize,setPointSize]= useState(1);
    const [combineOp,setCombineOp]= useState(AVG);
    const [axis,setAxis]= useState('x');
    const [hipsLinePts,setHipsLinePts]= useState(undefined);
    const plot= primePlot(pv);
    const hips= isHiPS(plot);
    const hipsNorder= hips ? getHiPSPointNorder(plot) : undefined;
    const {pt0:pt1,pt1:pt2}=plot?.attributes?.[PlotAttribute.ACTIVE_DISTANCE] ?? {};
    const extractionData=plot?.attributes?.[PlotAttribute.EXTRACTION_DATA] ?? false;
    const cc= CysConverter.make(plot);
    const ipt1= cc?.getImageCoords(pt1);
    const ipt2= cc?.getImageCoords(pt2);
    const x1= Math.trunc(ipt1?.x ?? 0);
    const y1= Math.trunc(ipt1?.y ?? 0);
    const x2= Math.trunc(ipt2?.x ?? 0);
    const y2= Math.trunc(ipt2?.y ?? 0);
    const {plotId,plotImageId,cubeIdx}= plot ?? {};
    const hduNum= getHDU(plot); // not used for HiPS, the server uses the first image HDU of the tile
    const plane= getCubePlaneIdx(plot)>-1 ? getCubePlaneIdx(plot) : 0;
    const {x:chartX,y:chartY}=plot?.attributes?.[PlotAttribute.SELECT_ACTIVE_CHART_PT] ?? {};


    useEffect(() => {
        let active= true;
        const getHiPSData= async () => {
            const {imPtAry:newImPtAry, hipsTileLocAry, hipsWptAry}= makeHiPSLinePtAry(plot, ipt1, ipt2, hipsNorder);
            if (!newImPtAry.length) {
                setImPtAry(undefined);
                setHipsLinePts(undefined);
                setChartParams({});
                return;
            }
            const dataAry= await callGetPointExtractionAry(
                {plot, hduNum, plane, ptAry:hipsTileLocAry, ptSizeX:pointSize, ptSizeY:pointSize, combineOp});
            if (!active) return;
            const hipsUnits= await getHiPSPointUnits(pv, hipsTileLocAry[0], hipsWptAry[0]);
            if (!active) return;
            const xOffAry= hipsWptAry.map( (wpt) => computeDistance(hipsWptAry[0],wpt)*3600);
            const chartData= genSliceChartData(plot, xOffAry, dataAry, chartX, chartY, pointSize, combineOp,
                makeHiPSLineExtractionTitle(plot, hipsWptAry), false, hipsUnits);
            setChartParams(chartData);
            setImPtAry(newImPtAry);
            setHipsLinePts({hipsTileLocAry, hipsWptAry});
        };

        const getData= async () => {
            if (ipt1 && ipt2 && plot) {
                if (hips) {
                    await getHiPSData();
                    return;
                }
                const {dataWidth,dataHeight}= plot;
                const {direction,axis:newAxis}= xLineData(ipt1,ipt2);
                const newImPtAry= getLinePointAry(ipt1,ipt2)
                    ?.filter( ({x,y}) => x>=0 && y>=0 && x<dataWidth && y<dataHeight);
                if (!newImPtAry?.length) {
                    setImPtAry(undefined);
                    setChartParams({});
                    return;
                }
                const cc= CysConverter.make(plot);

                const pointSizeX= newAxis==='y' ? pointSize : 1;
                const pointSizeY= newAxis==='x' ? pointSize : 1;
                const dataAry= await callGetPointExtractionAry(
                    {plot, hduNum, plane, ptAry:newImPtAry, ptSizeX:pointSizeX, ptSizeY:pointSizeY, combineOp});
                if (!active) return;
                const chartTitle= makeLineExtractionTitle(pv,x1,y1,x2,y2);
                const pt0= hasWCSProjection(plot) ? cc.getWorldCoords(newImPtAry[0]) : newImPtAry[0];
                const xOffAry= hasWCSProjection(plot)
                    ? newImPtAry.map( (pt) => computeDistance(pt0,cc.getWorldCoords(pt))*3600)
                    : newImPtAry.map( (pt) => computeScreenDistance(pt0.x,pt0.y,pt.x,pt.y));
                const chartData=
                    genSliceChartData(plot, xOffAry, dataAry, chartX, chartY, pointSize, combineOp, chartTitle, direction<0);
                setChartParams(chartData);
                setImPtAry(newImPtAry);
                setAxis(newAxis);
            }
        };
        if (extractionData) void getData();
        return () => { active= false; };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    },[x1,y1,x2,y2,hduNum,plotId,plotImageId,cubeIdx,hipsNorder,pointSize,combineOp,chartX,chartY]);

    const plotlyDataToUse= extractionData&&!helpWarning ? plotlyData : undefined;
    let sizeType= axis==='y' ? SIZE_HORIZONTAL : SIZE_VERTICAL;
    if (hips) sizeType= SIZE_SQUARE; // the tile pixels are not lined up with the line, so use a square aperture

    return (
        <ExtractionPanelView {...{
            pointSize, setPointSize, combineOp, setCombineOp, canCreateExtractionTable,
            plotlyDivStyle, plotlyData: plotlyDataToUse, plotlyLayout, hasFloatingData:hasFloatingData(plot),
            sizeType,
            startUpHelp: <LineStartUpHelp {...{helpWarning,plot,pvCnt}}/>,
            afterRedraw: (chart,pl) => afterLineChartRedraw(pv,chart,pl,imPtAry),
            callKeepExtraction: (save, doOverlay) => hips
                ? keepHiPSPointExtraction({pv, ...hipsLinePts, plane, pointSize, combineOp, save, doOverlay, isLine:true})
                : keepDataExtraction({pv,baseImPtAry:getLinePointAry(ipt1, ipt2),save,
                    doOverlay,axis,pointSize,combineOp, isLine:true})
        }}>
            {!hips && <LineExtractionType {...{plotlyData:plotlyDataToUse, plotId, selectionType}}/>}
        </ExtractionPanelView>
    );
}


const CubeStartUpHelp= ({plot}) => (
    <Stack spacing={3}>
        <Typography >
            {canExtractZAxis(plot) ?
                'Click on a pixel to extract data from all planes of the cube' :
                'Please choose a cube to extract z-axis data'}
        </Typography>
        <Stack spacing={1}>
            <Divider orientation='horizontal'/>
            {canExtractZAxis(plot) && <Typography {...{level:'body-sm', sx: {textAlign: 'left'} }}>
                {CUBE_WARNING}
            </Typography>}
            {isHiPSFitsCube(plot) && <Typography {...{level:'body-sm', sx: {textAlign: 'left'} }}>
                {HIPS_APERTURE_NOTE}
            </Typography>}
        </Stack>
    </Stack>
);


const LineStartUpHelp= ({helpWarning,plot,pvCnt}) => (
    <Stack alignItems='center'>
        <Typography {...{color: helpWarning?'warning':undefined, level:helpWarning?'h4':undefined}}>
            Draw line on image to extract point on line and show chart. </Typography>
        {pvCnt>1 && !helpWarning && <Typography {...{mt:3}}> Shift-click will change the selected image without selecting a new line. </Typography>}
        {!helpWarning && isHiPS(plot) && <Typography {...{mt:3, level:'body-sm'}}>{HIPS_APERTURE_NOTE}</Typography>}
        {!helpWarning && isImage(plot) && <Typography pt={1} color='warning' size={'body-lg'}>OR</Typography>}
        {!helpWarning && isImage(plot) && <Stack pt={1} spacing={1}>
            <FieldGroup groupKey='extract-full-line' >
                <ExtractWholeLine plot={plot}/>
            </FieldGroup>
        </Stack>}
    </Stack>
);

const LineExtractionType= ({plotlyData,plotId,selectionType}) => (
    plotlyData ?
        (<Stack {...{ spacing:1, alignItems:'center', alignSelf:'center', direction:'row', height: 30, maxHeight:30, mt:.5}}>
            <ListBoxInputFieldView {...{
                value: selectionType,
                tooltip: SELECT_TYPE_TIP,
                onChange: (ev, newVal) => {
                    dispatchModifyCustomField( ExtractLineTool.TYPE_ID, {selectionType:newVal}, plotId);
                },
                options:[
                    {label: 'Free Hand Selection - click and draw on image', value: FREE_SELECTION},
                    {label: 'Line Selection - click on image', value: LINE_SELECTION},
                    {label: 'Column Selection - click on image', value: COLUMN_SELECTION}
                ]
            }} />
        </Stack>)
        : undefined
);


function getPts(isLine,line,col,plot) {
    let pt0= undefined;
    let pt1= undefined;
    const {dataHeight,dataWidth}= plot ?? {};
    if (!plot) return {pt0,pt1};

    const validLine= !isNaN(parseInt(line)) && !isNaN(Number(line));
    const validCol= !isNaN(parseInt(col)) && !isNaN(Number(col));

    if ( (isLine && validLine) || (!isLine && validCol) ) {
        if (isLine && line>dataHeight) return {pt0,pt1};
        if (!isLine && col>dataWidth) return {pt0,pt1};
        pt0= isLine ? makeImagePt(0,line) : makeImagePt(col, 0);
        pt1= isLine ? makeImagePt(dataWidth-1,line) : makeImagePt(col, dataHeight-1);
    }
    return {pt0,pt1};
}


function ExtractWholeLine({plot}) {
    const dirVal= useFieldGroupValue('direction')[0]() ?? 'X';
    const col= useFieldGroupValue('column')[0]();
    const line= useFieldGroupValue('line')[0]();
    const isLine= dirVal==='X';
    const {dataHeight,dataWidth}= plot ?? {};
    const singleLine= dataWidth===1 || dataHeight===1;

    const {pt0, pt1}= !singleLine ? getPts(isLine,line,col,plot) :
        dataWidth===1 ? getPts(false, '',0,plot) : getPts(true,0,'',plot);
        

    useEffect(() => {
        if (!plot || (isUndefined(line) && isUndefined(col))) return;
        dispatchModifyCustomField( ExtractLineTool.TYPE_ID,
            {
                newFirst: pt0,
                newCurrent: pt1,
                plotId: plot.plotId,
                newSelectionType: isLine ? LINE_SELECTION : COLUMN_SELECTION,
            },
            plot.plotId);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [line,col,dirVal]);


    if (!plot) return;

    const direction= (
        <ListBoxInputField
                    fieldKey='direction'
                    options={[ {label: 'Line', value: 'X'}, {label: 'Column', value: 'Y'} ]}
                    slotProps={{
                        input: { variant:'plain', sx:{minHeight:'unset'} }
                    }}
                    initialState= {{ value:dirVal}}
                    tooltip='Select line or column' label=''  />
    );


    const extractButton= (
        <Button
                variant= 'solid'
                color= 'primary'
                disabled={Boolean(!pt0 && !pt1)}
                onClick={()=> {
                    const dl= getDrawLayerByType(getDlAry(), ExtractLineTool.TYPE_ID);
                    if (!dl) return;
                    addLineDistAttributesToPlots(dl,plot.plotId,{pt0,pt1});
                }} >
            Extract
        </Button>
    );

    const text= !singleLine
        ? 'Extract a whole line or column'
        : dataWidth===1
            ? 'Extract a whole column'
            : 'Extract a whole line';

    return (
        <Stack spacing={1} alignItems='center'>
            <Typography whiteSpace='nowrap'>{text} </Typography>
            {singleLine ?
                extractButton :
                <ValidationField {...{
                    nullAllowed: true,
                    placeholder: isLine ?
                        `Enter line # 0 - ${dataHeight-1}` :
                        `Enter column # 0 - ${dataWidth-1}`,
                    fieldKey: isLine ? 'line' : 'column',
                    key: isLine ? 'line' : 'column',
                    initialState: {
                        value: undefined,
                        validator: intValidator(0,
                            isLine ? dataHeight - 1 : dataWidth - 1,
                            isLine ? 'line' : 'column')
                    },
                    tooltip: isLine ? 'choose a line' : 'choose a column',
                    startDecorator: direction,
                    endDecorator: extractButton,
                    sx: {
                        width: '27rem',
                        '& .MuiInput-root': {paddingInline: 0},
                        '& .MuiInput-endDecorator': {mr: 1}
                    },
                }} />
            }
        </Stack>
        );
}


const xLineData= (pt1,pt2) => {
    if (usingXAxis(pt1,pt2)) {
        return {
            direction: pt1.x-pt2.x < 0 ? 1 : -1,
            axis:'x'
        };
    }
    else {
        return {
            direction: pt1.y - pt2.y < 0 ? 1 : -1,
            axis:'y'
        };
    }
};

function usingXAxis(pt1,pt2) {
    const deltaX = Math.abs(pt2.x - pt1.x);
    const deltaY = Math.abs(pt2.y - pt1.y);
    return (deltaX > deltaY);
}


function ZAxisExtractionPanel({canCreateExtractionTable, pv}) {
    const [pointSize,setPointSize]= useState(1);
    const [combineOp,setCombineOp]= useState(AVG);
    const [{plotlyDivStyle, plotlyData, plotlyLayout},setChartParams]= useState({});
    const plot= primePlot(pv);
    const {pt}=plot?.attributes?.[PlotAttribute.ACTIVE_POINT] ?? {};
    const hipsCube= isHiPSFitsCube(plot);
    const wpt= hipsCube ? CCUtil.getWorldCoords(plot,pt) : undefined;
    const tileLoc= hipsCube
        ? getHiPSFitsTileLocation(plot, wpt, Number(plot.hipsProperties?.hips_order))
        : {};
    const hipsTileUrl= hipsCube ? tileLoc.tileUrl : undefined;
    const ipt= hipsCube ? tileLoc.tilePt : CCUtil.getImageCoords(plot,pt);
    const x= Math.trunc(ipt?.x ?? 0);
    const y= Math.trunc(ipt?.y ?? 0);
    const {plotId,plotImageId,cubeIdx}= plot ?? {};
    const hduNum= getHDU(plot); // not used for HiPS, the server uses the first image HDU of the tile
    const extName= hipsCube ? undefined : getExtName(plot);

    useEffect(() => {
        let active= true;
        const updateChart= async () => {
            if (ipt && plot) {
                if (!canExtractZAxis(plot)) {
                    setChartParams({});
                    return;
                }
                const dataAry = await callGetCubeDrillDownAry( {plot, hduNum, ipt, pointSize, combineOp, hipsTileUrl});
                if (!active) return;
                const plane=getCubePlaneIdx(plot);
                const chartTitle= hipsCube
                    ? `Z-Axis Preview - Norder ${tileLoc.norder}, Npix ${tileLoc.ipix}, Point: (${x},${y})`
                    : `Z-Axis Preview - ${extName?extName+',':''} HDU #${hduNum}, Point: (${x},${y})`;
                setChartParams(genZAxisChartData(makeImagePt(x,y), pv, dataAry, plane , dataAry[plane] , pointSize, combineOp, chartTitle));
            }
        };
        void updateChart();
        return () => { active= false; };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    },[x,y,hduNum,plotId,pointSize,combineOp,plotImageId,tileLoc.ipix,cubeIdx]);

    return (
        <ExtractionPanelView {...{
            pointSize, setPointSize, combineOp, setCombineOp,
            hasFloatingData:hasFloatingData(plot),
            plotlyDivStyle, plotlyData, plotlyLayout, canCreateExtractionTable,
            startUpHelp: <CubeStartUpHelp {...{plot}}/>,
            afterRedraw: (chart,pl) => afterZAxisChartRedraw(makeImagePt(x,y), pv,chart,pl),
            callKeepExtraction: (download, doOverlay) =>
                keepZAxisExtraction(makeImagePt(x,y), pv, plot,
                    hipsCube ? hipsTileUrl : plot?.plotState.getWorkingFitsFileStr(),
                    hduNum, pointSize, combineOp, download,doOverlay, wpt),
        }} />
    );
}

const pointSizeTip= 'Extract and manipulate the pixel values in the specified aperture centered on the pixel closest to where you clicked.';

const SIZE_SQUARE= 'sizeSquare';
const SIZE_VERTICAL= 'sizeVertical';
const SIZE_HORIZONTAL= 'sizeHorizontal';

function ExtractionPanelView({pointSize, setPointSize, afterRedraw, plotlyDivStyle,
                                 plotlyData, canCreateExtractionTable, sizeType= SIZE_SQUARE,
                                 plotlyLayout, startUpHelp, callKeepExtraction,
                                 combineOp, setCombineOp, hasFloatingData, children}) {


    const sizeOp=[];
    if (sizeType===SIZE_HORIZONTAL) {
        for(let i= 1; i<=7;i+=2) sizeOp.push({label:`${i}x1`, value:i});
    }
    else if (sizeType===SIZE_VERTICAL) {
        for(let i= 1; i<=7;i+=2) sizeOp.push({label:`1x${i}`, value:i});
    }
    else {
        for(let i= 1; i<=7;i+=2) sizeOp.push({label:`${i}x${i}`, value:i});
    }

    const canExtract = plotlyData && canCreateExtractionTable && callKeepExtraction;

    return (
        <Stack {...{p:.5, alignItems:'stretch', overflow: 'hidden', zIndex:1, direction:'column',
            sx:{'& .ff-CompleteButton': {whiteSpace:'nowrap'}, resize:'both'} }}>
            <Stack {...{ spacing:1, alignItems:'center', alignSelf:'center', direction:'row', height: 30, maxHeight:30, mb:.5}}>
                <Tooltip title={pointSizeTip}>
                    <Typography >Aperture (Values will be combined)</Typography>
                </Tooltip>
                <ListBoxInputFieldView
                    value={pointSize} onChange={(ev,newVal) => setPointSize(newVal)}
                    tooltip={ pointSizeTip} options={sizeOp} />
                {pointSize>1 && <ListBoxInputFieldView
                    value={combineOp} onChange={(ev,newVal) => setCombineOp(newVal)}
                    tooltip={pointSizeTip}
                    options={hasFloatingData?combineOps:combineIntOps}
                    />}
            </Stack>
            {plotlyData && <Divider sx={{mt:.5}}/>}
            <Stack {...{minWidth:440, minHeight:200, direction:'row', sx:{flex: '1 1 auto'}}}>
                {plotlyData ?
                    <ExtractionChartResizeable {...{plotlyDivStyle, plotlyData, plotlyLayout,afterRedraw}} /> :
                    <Box sx={{pt:6, textAlign:'center', fontSize:'large', m:1}}>{startUpHelp}</Box>
                }
            </Stack>
            {plotlyData && <Divider sx={{mb:.5}}/>}
            {children}
            <Stack {...{
                textAlign:'center', alignSelf: 'stretch', direction:'row',
                justifyContent:'space-between', pt:2, pl:2, pb:1, pr: 1}}>
                <Stack {...{direction:'row', justifyContent:'space-between', spacing:2}}>
                    {canExtract &&
                        <CompleteButton text= {allowPinnedCharts() ? 'Pin Chart/Table' : 'Pin Table'}
                                        onSuccess={ () => void keepExtractionAndPin(callKeepExtraction, allowPinnedCharts()) } />}
                    {plotlyData && callKeepExtraction &&
                        <CompleteButton primary={false} text='Download as Table'
                                        onSuccess={()=> void callKeepExtraction(true,false)}/>}
                    {plotlyData &&
                        <CompleteButton primary={false} text='Download Chart'
                                        onSuccess={()=> void downloadChart(CHART_ID)}/>}
                </Stack>
                <HelpIcon helpId={'visualization.extraction'}/>
            </Stack>
        </Stack>
    );
}


async function keepExtractionAndPin(callKeepExtraction,allowpinChart=false) {
    const tblIdAry = await callKeepExtraction(false,true);
    if (!allowpinChart || !tblIdAry) return;
    for (const tbl_id of tblIdAry) {
        await onTableLoaded(tbl_id);
        const chartId = ensureDefaultChart(tbl_id);
        if (chartId) pinChart({chartId});
    }
}

