/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {Box, Button, Divider, Stack, Tooltip, Typography} from '@mui/joy';
import {isUndefined} from 'lodash';
import React, {useEffect, useState} from 'react';
import sizeMe from 'react-sizeme';
import {getAppOptions} from '../../../api/ApiUtil.js';
import {isDefined} from '../../../util/WebUtil';
import {makeImagePt} from '../../Point';
import {allowPinnedCharts} from '../../../charts/ChartUtil';
import {ensureDefaultChart} from '../../../charts/ui/ChartsContainer.jsx';
import {pinChart} from '../../../charts/ui/PinnedChartContainer.jsx';
import {downloadChart, PlotlyWrapper} from '../../../charts/ui/PlotlyWrapper.jsx';
import {dispatchHideDialog, dispatchShowDialog} from '../../../core/ComponentCntlr.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../../../core/MasterSaga.js';
import ExtractLineTool, {
    addLineDistAttributesToPlots, COLUMN_SELECTION, FREE_SELECTION, LINE_SELECTION
} from '../../../drawingLayers/ExtractLineTool.js';
import ExtractPointsTool from '../../../drawingLayers/ExtractPointsTool.js';
import {callGetCubeDrillDownAry, callGetPointExtractionAry} from '../../../rpc/PlotServicesJson.js';
import {onTableLoaded} from '../../../tables/TableUtil.js';
import CompleteButton from '../../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../../ui/DialogRootContainer.jsx';
import {FieldGroup} from '../../../ui/FieldGroup';
import HelpIcon from '../../../ui/HelpIcon.jsx';
import {ListBoxInputField, ListBoxInputFieldView} from '../../../ui/ListBoxInputField.jsx';
import {PopupPanel} from '../../../ui/PopupPanel.jsx';
import {RadioGroupInputFieldView} from '../../../ui/RadioGroupInputFieldView.jsx';
import {useFieldGroupValue, useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {ValidationField} from '../../../ui/ValidationField';
import {intValidator} from '../../../util/Validate';
import {CCUtil, CysConverter} from '../../CsysConverter.js';
import {
    dispatchAttachLayerToPlot, dispatchCreateDrawLayer, dispatchDestroyDrawLayer, dispatchDetachLayerFromPlot,
    dispatchModifyCustomField, getDlAry
} from '../../DrawLayerCntlr.js';
import {getExtName, hasFloatingData} from '../../FitsHeaderUtil.js';
import ImagePlotCntlr, {
    dispatchAttributeChange, dispatchChangePointSelection, dispatchChangePrimePlot, visRoot
} from '../../ImagePlotCntlr.js';
import {PlotAttribute} from '../../PlotAttribute.js';
import {
    convertHDUIdxToImageIdx, getActivePlotView, getCubePlaneFromWavelength, getDrawLayerByType, getHDU, getHDUIndex,
    getImageCubeIdx, getPlotViewAry, getPlotViewById, hasWCSProjection, hasWLInfo, isDrawLayerAttached, isImageCube,
    isMultiHDUFits, primePlot
} from '../../PlotViewUtil.js';
import {computeDistance, computeScreenDistance, getLinePointAry} from '../../VisUtil.js';
import {genPointChartData, genSliceChartData, genZAxisChartData} from './ExtractionChart';
import {keepLineExtraction, keepPointsExtraction, keepZAxisExtraction} from './ExtractionTable';


const DIALOG_ID= 'extractionDialog';
const CHART_ID= 'extractionChart';
const ZAXIS_POINT_SELECTION_ID= 'z-axisExtraction';

const SELECT_TYPE_TIP= 'Choose mouse selection type: mouse line lock, mouse column lock or free selection';

export const Z_AXIS= 'Z_AXIS';
export const LINE= 'LINE';
export const POINTS= 'POINTS';

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
    const pv= getActivePlotView(visRoot());
    const dl= getDrawLayerByType(getDlAry(), typeId);
    !dl && dispatchCreateDrawLayer(typeId);
    !isDrawLayerAttached(dl,pv.plotId) && dispatchAttachLayerToPlot(typeId,pv.plotId,true,true, true);
}

const EXTRACT_END_ID= 'extractEndId';


export function showExtractionDialog(element,extractionType,wasCanceled) {
    endExtraction();
    exTypeCntl[extractionType].start();
    DialogRootContainer.defineDialog(DIALOG_ID, <ExtractDialog {...{extractionType, wasCanceled}}/>, element );
    dispatchShowDialog(DIALOG_ID);


    dispatchAddActionWatcher( {
        id: EXTRACT_END_ID,
        callback: (action) => exTypeCntl[extractionType].start(),
        actions:[ImagePlotCntlr.PLOT_IMAGE]
    });
}


export function endExtraction() {
    cancelPointExtraction();
    cancelZaxisExtraction();
    cancelLineExtraction();
    dispatchCancelActionWatcher(EXTRACT_END_ID);
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
        <PopupPanel title={`Extract: ${primePlot(pv)?.title ?? ''}`}
                    closeCallback={doCancel} requestToClose={doCancel}  >
            <Panel canCreateExtractionTable={canCreateExtractionTable} pv={pv} pvCnt={pvCnt}/>
        </PopupPanel>
    );
}



function getStoreState(prevResult) {
    const vr= visRoot();
    const activePv= getActivePlotView(vr);
    const pv= primePlot(activePv) ? activePv :
        primePlot(vr, vr.prevActivePlotId) ? getPlotViewById(vr,vr.prevActivePlotId) : activePv;
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


function afterZAxisChartRedraw(imPt, pv, chart) {
    chart.on('plotly_click', (ev) => {
        setTimeout( () => {
            const plane= hasWLInfo(primePlot(pv)) ?
                getCubePlaneFromWavelength(pv,ev.points[0].x,imPt) : ev.points[0].x-1;
            const primeIdx= convertHDUIdxToImageIdx(pv, getHDUIndex(pv,primePlot(pv)), plane);
            if (pv.primeIdx!==primeIdx) {
                dispatchChangePrimePlot({plotId:pv.plotId,primeIdx});
            }
        },5);
    });
}

function afterLineChartRedraw(pv, chart,pl, imPtAry, pt1, pt2) {
    chart.on('plotly_click', (ev) => {
        setTimeout( () => {
            const plot= primePlot(pv);
            const imPt= makeImagePt(Math.round(imPtAry[ev.points[0].pointNumber].x)+.5, Math.round(imPtAry[ev.points[0].pointNumber].y)+.5);
            dispatchModifyCustomField( ExtractLineTool.TYPE_ID,
                {activePt: hasWCSProjection(plot) ? CCUtil.getWorldCoords(plot,imPt) : imPt},
                pv.plotId);
            dispatchAttributeChange({plotId:plot.plotId,overlayColorScope:false,toAllPlotsInPlotView:false,
                changes:{
                    [PlotAttribute.SELECT_ACTIVE_CHART_PT]: {x:ev.points[0].x,y:ev.points[0].y}
                }
            });
        },5);
    });
}

function afterPointsChartRedraw(pv, chart,pl,chartXAxis, imPtAry) {
    chart.on('plotly_click', (ev) => {
        setTimeout( () => {
            const plot= primePlot(pv);
            const key= chartXAxis==='imageX' ? 'x' : 'y';
            const {x,y}= ev.points[0];
            const imPt= imPtAry.find( (pt) => pt[key]===x);
            const cenImPt= makeImagePt(imPt.x+.5, imPt.y+.5);
            dispatchModifyCustomField( ExtractPointsTool.TYPE_ID,
                {activePt: hasWCSProjection(plot) ? CCUtil.getWorldCoords(plot,cenImPt) : cenImPt}, pv.plotId);
            dispatchAttributeChange({plotId:plot.plotId,overlayColorScope:false,toAllPlotsInPlotView:false,
                changes:{[PlotAttribute.SELECT_ACTIVE_CHART_PT]: {x,y,chartXAxis}}
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

export const wrapResizerForExtraction = sizeMe( {
        monitorWidth: true, monitorHeight: false, monitorPosition: false,
        refreshRate: 100, refreshMode: 'debounce', noPlaceholder: false
    } );

const ExtractionChartResizeable= wrapResizerForExtraction(ExtractionChart);

function makeLineExtractionTitle(pv,x1,y1,x2,y2) {
    const plot= primePlot(pv);
    let hduInfo= '';
    let cubeInfo= '';
    if (isMultiHDUFits(pv)) {
        const extName= getExtName(plot);
        hduInfo= (extName || ` HDU #${getHDU(plot)}`)  + ' - ';
    }
    if (getImageCubeIdx(plot)>-1) {
        cubeInfo= `Plane #${getImageCubeIdx(plot)+1} - `;
    }
    return  `Line Extract Preview - ${hduInfo}${cubeInfo}(${x1},${y1}) to (${x2},${y2})`;
}


function PointExtractionPanel({canCreateExtractionTable, pv, pvCnt}) {
    const [{plotlyDivStyle, plotlyData, plotlyLayout},setChartParams]= useState({});
    const [pointSize,setPointSize]= useState(1);
    const [combineOp,setCombineOp]= useState(AVG);
    const [allRelatedHDUS,setAllRelatedHDUS]= useState(true);
    const [chartXAxis,setChartXAxis]= useState('imageX');
    const plot= primePlot(pv);
    const ptAry=plot?.attributes?.[PlotAttribute.PT_ARY] ??[];
    const cc= CysConverter.make(plot);
    const imPtAry= ptAry
        .map( (pt) => cc?.getImageCoords(pt))
        .map( (pt) => pt? makeImagePt(Math.trunc(pt.x), Math.trunc(pt.y)) : undefined)
        .filter((pt) => pt);

    const {plotId,plotImageId}= plot ?? {};
    const hduNum= getHDU(plot);
    const plane= getImageCubeIdx(plot)>-1 ? getImageCubeIdx(plot) : 0;
    const {x:chartX,y:chartY,chartXAxis:lastChartChartXAxis=chartXAxis}=plot?.attributes?.[PlotAttribute.SELECT_ACTIVE_CHART_PT] ?? {};


    const bottomUI = plotlyData ?
            (<RadioGroupInputFieldView value={chartXAxis} buttonGroup={true}
                                       options={ [ {label: 'Image X', value: 'imageX'}, {label: 'Image Y', value: 'imageY'} ]}
                                       onChange={(ev) => setChartXAxis(ev.target.value)} />) :
            undefined;

    useEffect(() => {
        const getData= async () => {
            if (imPtAry && imPtAry.length && plot) {
                const dataAry= await callGetPointExtractionAry(plot, hduNum, plane, imPtAry, pointSize, pointSize, combineOp, allRelatedHDUS);
                const chartTitle= 'Point Extract Preview';
                let activeIdx= 0;
                const key= lastChartChartXAxis==='imageX' ? 'x' : 'y';
                if (plot.attributes[PlotAttribute.SELECT_ACTIVE_CHART_PT]) {
                    activeIdx= imPtAry.findIndex( (pt) => pt[key]===chartX);
                    if (activeIdx<0) activeIdx= 0;
                }
                const chartData=
                    genPointChartData(plot, dataAry,imPtAry, imPtAry[activeIdx][key], dataAry[activeIdx],
                        pointSize,combineOp, chartTitle, chartXAxis, activeIdx);
                setChartParams(chartData);
            }
            // if (!pv) cancelPointExtraction();
        };
        getData();
    },[ptAry.length,hduNum,plotId,plotImageId,pointSize,combineOp,chartX,chartY,chartXAxis]);

    return (
        <ExtractionPanelView {...{
            allRelatedHDUS, setAllRelatedHDUS, pointSize, setPointSize, combineOp, setCombineOp,
            plotlyDivStyle, plotlyData, plotlyLayout, canCreateExtractionTable,
            hasFloatingData:hasFloatingData(plot),
            startUpHelp: (
                <div>
                    <Typography>Click on an image to extract a point, continue clicking to extract more points. </Typography>
                    {pvCnt>1 && <Typography {...{mt:3}}>Shift-click will change the selected image without extracting points. </Typography>}
                </div>
            ),
            afterRedraw: (chart,pl) => afterPointsChartRedraw(pv,chart,pl,chartXAxis, imPtAry),
            callKeepExtraction: (download, doOverlay) =>
                keepPointsExtraction(imPtAry, pv, plot,
                    plot.plotState.getWorkingFitsFileStr(), hduNum, plane,
                    pointSize,combineOp, download, doOverlay),
        }} /> );
}



function LineExtractionPanel({canCreateExtractionTable, pv, pvCnt}) {
    const dl= useStoreConnector(() => getDrawLayerByType(getDlAry(), ExtractLineTool.TYPE_ID) ?? {});
    const {selectionType= FREE_SELECTION, helpWarning=false}= dl;
    const [{plotlyDivStyle, plotlyData, plotlyLayout},setChartParams]= useState({});
    const [imPtAry,setImPtAry]= useState(undefined);
    const [pointSize,setPointSize]= useState(1);
    const [combineOp,setCombineOp]= useState(AVG);
    const [allRelatedHDUS,setAllRelatedHDUS]= useState(true);
    const [axis,setAxis]= useState('x');
    const plot= primePlot(pv);
    const {pt0:pt1,pt1:pt2}=plot?.attributes?.[PlotAttribute.ACTIVE_DISTANCE] ?? {};
    const extractionData=plot?.attributes?.[PlotAttribute.EXTRACTION_DATA] ?? false;
    const cc= CysConverter.make(plot);
    const ipt1= cc?.getImageCoords(pt1);
    const ipt2= cc?.getImageCoords(pt2);
    const x1= Math.trunc(ipt1?.x ?? 0);
    const y1= Math.trunc(ipt1?.y ?? 0);
    const x2= Math.trunc(ipt2?.x ?? 0);
    const y2= Math.trunc(ipt2?.y ?? 0);
    const {plotId,plotImageId}= plot ?? {};
    const hduNum= getHDU(plot);
    const plane= getImageCubeIdx(plot)>-1 ? getImageCubeIdx(plot) : 0;
    const {x:chartX,y:chartY}=plot?.attributes?.[PlotAttribute.SELECT_ACTIVE_CHART_PT] ?? {};


    useEffect(() => {
        const getData= async () => {
            if (ipt1 && ipt2 && plot) {
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
                const dataAry= await callGetPointExtractionAry(plot, hduNum, plane, newImPtAry, pointSizeX, pointSizeY, combineOp, allRelatedHDUS);
                const chartTitle= makeLineExtractionTitle(pv,x1,y1,x2,y2);
                const pt0= hasWCSProjection(plot) ? cc.getWorldCoords(newImPtAry[0]) : newImPtAry[0];
                const xOffAry= hasWCSProjection(plot)
                    ? newImPtAry.map( (pt) => computeDistance(pt0,cc.getWorldCoords(pt))*3600)
                    : newImPtAry.map( (pt) => computeScreenDistance(pt0.x,pt0.y,pt.x,pt.y));
                const chartData=
                    genSliceChartData(plot, ipt1,ipt2,xOffAry, dataAry, chartX, chartY, pointSize, combineOp, chartTitle, direction<0);
                setChartParams(chartData);
                setImPtAry(newImPtAry);
                setAxis(newAxis);
            }
            if (!pv) cancelLineExtraction();
        };
        if (extractionData) void getData();
    },[x1,y1,x2,y2,hduNum,plotId,plotImageId,pointSize,combineOp,chartX,chartY]);

    const plotlyDataToUse= extractionData&&!helpWarning ? plotlyData : undefined;

    return (
        <ExtractionPanelView {...{
            allRelatedHDUS, setAllRelatedHDUS, pointSize, setPointSize, combineOp, setCombineOp, canCreateExtractionTable,
            plotlyDivStyle, plotlyData: plotlyDataToUse, plotlyLayout, hasFloatingData:hasFloatingData(plot),
            sizeType: axis==='y'?SIZE_HORIZONTAL:SIZE_VERTICAL,
            startUpHelp: <LineStartUpHelp {...{helpWarning,plot,pvCnt}}/>,
            afterRedraw: (chart,pl) => afterLineChartRedraw(pv,chart,pl,imPtAry,makeImagePt(x1,y1), makeImagePt(x2,y2)),
            callKeepExtraction: (download, doOverlay) =>
                keepLineExtraction(ipt1,ipt2, pv, plot, plot.plotState.getWorkingFitsFileStr(),
                    hduNum, plane,
                    axis==='x' ? 1 : pointSize,
                    axis==='y' ? 1: pointSize,
                    combineOp, download, doOverlay),
        }}>
            <LineExtractionType {...{plotlyData:plotlyDataToUse, plotId, selectionType}}/>
        </ExtractionPanelView>
    );
}

const LineStartUpHelp= ({helpWarning,plot,pvCnt}) => (
    <Stack alignItems='center'>
        <Typography {...{color: helpWarning?'warning':undefined, level:helpWarning?'h4':undefined}}>
            Draw line on image to extract point on line and show chart. </Typography>
        {pvCnt>1 && !helpWarning && <Typography {...{mt:3}}> Shift-click will change the selected image without selecting a new line. </Typography>}
        {!helpWarning && <Typography pt={1} color='warning' size={'body-lg'}>OR</Typography>}
        {!helpWarning && <Stack pt={1} spacing={1}>
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
            startValue: minValue(pt1,pt2),
            direction: pt1.x-pt2.x < 0 ? 1 : -1,
            axis:'x'
        };
    }
    else {
        return {
            startValue: minValue(pt1,pt2),
            direction: pt1.y - pt2.y < 0 ? 1 : -1,
            axis:'y'
        };
    }
};

const minValue= (pt1,pt2) => usingXAxis(pt1,pt2) ? Math.min(pt1.x,pt2.x) : Math.min(pt1.y,pt2.y);
// const maxValue= (pt1,pt2) => usingXAxis(pt1,pt2) ? Math.max(pt1.x,pt2.x) : Math.max(pt1.y,pt2.y);

function usingXAxis(pt1,pt2) {
    const deltaX = Math.abs(pt2.x - pt1.x);
    const deltaY = Math.abs(pt2.y - pt1.y);
    return (deltaX > deltaY);
}


function ZAxisExtractionPanel({canCreateExtractionTable, pv}) {
    const [pointSize,setPointSize]= useState(1);
    const [allRelatedHDUS,setAllRelatedHDUS]= useState(true);
    const [combineOp,setCombineOp]= useState(AVG);
    const [{plotlyDivStyle, plotlyData, plotlyLayout},setChartParams]= useState({});
    const plot= primePlot(pv);
    const {pt}=plot?.attributes?.[PlotAttribute.ACTIVE_POINT] ?? {};
    const ipt= CCUtil.getImageCoords(plot,pt);
    const x= Math.trunc(ipt?.x ?? 0);
    const y= Math.trunc(ipt?.y ?? 0);
    const {plotId,plotImageId}= plot ?? {};
    const hduNum= getHDU(plot);
    const extName= getExtName(plot);

    useEffect(() => {
        const updateChart= async () => {
            if (ipt && plot) {
                if (!isImageCube(plot)) {
                    setChartParams({});
                    return;
                }
                const dataAry = await callGetCubeDrillDownAry(plot, hduNum, ipt, pointSize, combineOp, allRelatedHDUS);
                const plane=getImageCubeIdx(plot);
                const chartTitle= `Z Axis Preview - ${extName?extName+',':''} HDU #${hduNum}, Point: (${x},${y})`;
                setChartParams(genZAxisChartData(makeImagePt(x,y), pv, dataAry, plane , dataAry[plane] , pointSize, combineOp, chartTitle));
            }
            // if (!pv) cancelZaxisExtraction();
        };
        void updateChart();
    },[x,y,hduNum,plotId,pointSize,combineOp,plotImageId]);

    return (
        <ExtractionPanelView {...{
            allRelatedHDUS, setAllRelatedHDUS, pointSize, setPointSize, combineOp, setCombineOp,
            hasFloatingData:hasFloatingData(plot),
            plotlyDivStyle, plotlyData, plotlyLayout, canCreateExtractionTable,
            startUpHelp:
                (<Typography>
                    {isImageCube(plot) ?
                    'Click on a pixel to extract data from all planes of the cube' :
                    'Please choose a cube to extract z-axis data'}
                </Typography>),
            afterRedraw: (chart,pl) => afterZAxisChartRedraw(makeImagePt(x,y), pv,chart,pl),
            callKeepExtraction: (download, doOverlay) =>
                keepZAxisExtraction(makeImagePt(x,y), pv, plot, plot?.plotState.getWorkingFitsFileStr(),
                    hduNum, pointSize, combineOp, download,doOverlay),
        }} />
    );
}

const pointSizeTip= 'Extract and manipulate the pixel values in the specified aperture centered on the pixel closest to where you clicked.';

const extractAddedChartInfo = ({payload}) => ( {chartId: payload.chartId, tbl_id: payload.data?.[0].tbl_id});

const SIZE_SQUARE= 'sizeSquare';
const SIZE_VERTICAL= 'sizeVertical';
const SIZE_HORIZONTAL= 'sizeHorizontal';

function ExtractionPanelView({pointSize, setPointSize, afterRedraw, plotlyDivStyle,
                                 plotlyData, canCreateExtractionTable, sizeType= SIZE_SQUARE,
                                 plotlyLayout, startUpHelp, callKeepExtraction,
                                 bottomUI, combineOp, setCombineOp, hasFloatingData, children}) {


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
            {bottomUI && <div>{bottomUI} </div>}
            <Stack {...{
                textAlign:'center', alignSelf: 'stretch', direction:'row',
                justifyContent:'space-between', pt:2, pl:2, pb:1, pr: 1}}>
                <Stack {...{direction:'row', justifyContent:'space-between'}}>
                    {plotlyData && canCreateExtractionTable && !allowPinnedCharts() &&
                    <CompleteButton style={{paddingLeft: 15}} text='Pin Table' onSuccess={()=> callKeepExtraction(false,true)} />}
                    {plotlyData && canCreateExtractionTable && allowPinnedCharts() &&
                        <CompleteButton style={{paddingLeft: 15}} text='Pin Chart/Table' onSuccess={() => {
                            const tbl_id = callKeepExtraction(false,true);
                            onTableLoaded(tbl_id).then(() => {
                                const chartId = ensureDefaultChart(tbl_id);
                                if (chartId) pinChart({chartId});
                            });
                        }} />}
                    {plotlyData &&
                    <CompleteButton sx={{pl: 2}} primary={false} text='Download as Table' onSuccess={()=> callKeepExtraction(true,false)}/>}
                    {plotlyData &&
                    <CompleteButton sx={{pl: 2}} primary={false} text='Download Chart' onSuccess={()=> downloadChart(CHART_ID)}/>}
                </Stack>
                <HelpIcon helpId={'visualization.extraction'}/>
            </Stack>
        </Stack>
    );
}



function cancelZaxisExtraction() {
    dispatchChangePointSelection(ZAXIS_POINT_SELECTION_ID, false);
    dispatchHideDialog(DIALOG_ID);
}

function cancelLineExtraction() {
    const pv= getActivePlotView(visRoot());
    if (pv) {
        dispatchDetachLayerFromPlot(ExtractLineTool.TYPE_ID,pv.plotId,true);
        dispatchAttributeChange({plotId:pv.plotId,overlayColorScope:true,
            changes:{[PlotAttribute.SELECT_ACTIVE_CHART_PT]: undefined }});
        dispatchDestroyDrawLayer(ExtractLineTool.TYPE_ID);
    }
    dispatchHideDialog(DIALOG_ID);
}

function cancelPointExtraction() {
    const pv= getActivePlotView(visRoot());
    if (pv) {
        dispatchDetachLayerFromPlot(ExtractPointsTool.TYPE_ID,pv.plotId,true);
        dispatchDestroyDrawLayer(ExtractPointsTool.TYPE_ID);
    }
    dispatchHideDialog(DIALOG_ID);
}



