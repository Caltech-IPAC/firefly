import {Box, Stack} from '@mui/joy';
import React, {useEffect, useRef, useState} from 'react';
import {MultiChartViewer} from '../../../charts/ui/MultiChartViewer.jsx';
import {getActiveFileMenuKeyByKey} from '../../../metaConvert/DataProductsCntlr';
import {SHOW_CHART, SHOW_IMAGE, SHOW_TABLE} from '../../../metaConvert/DataProductsType.js';
import {getMetaEntry, getTableGroup, getTblById, onTableLoaded} from '../../../tables/TableUtil.js';
import {TablesContainer} from '../../../tables/ui/TablesContainer.jsx';
import {RadioGroupInputFieldView} from '../../../ui/RadioGroupInputFieldView.jsx';
import {useStoreConnector} from '../../../ui/SimpleComponent';
import {ToolbarHorizontalSeparator} from '../../../ui/ToolbarButton';
import {dispatchChangePrimePlot, visRoot} from '../../ImagePlotCntlr';
import {NewPlotMode} from '../../MultiViewCntlr.js';
import { convertHDUIdxToImageIdx, getActivePlotView, getHDUIndex, hasImageCubes} from '../../PlotViewUtil';
import {ImageMetaDataToolbar} from '../ImageMetaDataToolbar.jsx';
import {MultiImageViewer} from '../MultiImageViewer.jsx';


const imageOp= {label: 'Image', value: SHOW_IMAGE};

let lastChartDefault= 'Chart';



export function MultiProductChoice({ dataProductsState, dpId,
                                       makeDropDown, chartViewerId, imageViewerId, metaDataTableId,
                                       tableGroupViewerId, whatToShow, onChange, mayToggle = false, factoryKey
                                   }) {
    const {enableCutout, pixelBasedCutout=false, dlData, gridForceRowSize}= dataProductsState;
    const {serDef, cutoutToFullWarning, dlAnalysis:{cutoutFullPair=false}={}}= dataProductsState.dlData ?? {};
    const primeIdx= useStoreConnector(() => getActivePlotView(visRoot())?.primeIdx ?? -1);
    const {current:showingStatus}= useRef({oldWhatToShow:undefined});
    const stateDef= dlData ? dlData?.dlAnalysis?.isSpectrum ? 'Spectrum' : 'Chart' : lastChartDefault;
    const [chartName, setChartName] = useState(stateDef);
    const chartTableOptions = [{label: 'Table', value: SHOW_TABLE}, {label: chartName, value: SHOW_CHART}];
    const options = !imageViewerId ? chartTableOptions : [...chartTableOptions, imageOp];
    options[1].label = chartName;
    const tbl_id = getTableGroup(tableGroupViewerId)?.active;
    const table = tbl_id ? getTblById(tbl_id) : undefined;
    useEffect(() => {
        onTableLoaded(tbl_id).then(() => {
            const name = (getMetaEntry(tbl_id, 'utype') === 'spec:Spectrum') ? 'Spectrum' : 'Chart';
            setChartName(name);
            lastChartDefault= name;
        });
    }, [dataProductsState.activate,tbl_id]);

    const activeItemKey= getActiveFileMenuKeyByKey(dpId,dataProductsState?.fileMenu?.activeItemLookupKey);
    const cubeIdx= dataProductsState?.fileMenu?.menu.find( (i) => i.menuKey===activeItemKey)?.cubeIdx ?? -1;

    useEffect(() => {
        if (!imageViewerId) return;
        const pv= getActivePlotView(visRoot());
        if (!pv || !hasImageCubes(pv) || cubeIdx<0) return;
        showingStatus.oldWhatToShow= whatToShow;
        showingStatus.cubeSet= false;
    }, [whatToShow,cubeIdx]);

    useEffect(() => {
        if (!imageViewerId || primeIdx===-1 || cubeIdx<0) return;
        if (showingStatus.cubeSet) return;
        const pv= getActivePlotView(visRoot());
        if (!pv || !hasImageCubes(pv)) return;
        showingStatus.cubeSet= true;
        const hduIdx= getHDUIndex(pv);
        const newPrimeIdx= convertHDUIdxToImageIdx(pv,hduIdx,cubeIdx) ?? 0;
        if (primeIdx!==newPrimeIdx) dispatchChangePrimePlot({plotId:pv.plotId,primeIdx:newPrimeIdx});
    }, [table,primeIdx,cubeIdx]);



    const toolbar = (
        <Stack {...{direction:'row', alignItems:'center', height:30, divider:<ToolbarHorizontalSeparator/>}}>
            {makeDropDown && <Box sx={{height: 30}}> {makeDropDown()} </Box>}
            {mayToggle && <RadioGroupInputFieldView {...{options, value: whatToShow, buttonGroup: true, onChange}} />}
        </Stack>);

    switch (whatToShow) {
        case SHOW_CHART:
            return (
                <Stack {...{ width:'100%', height:'calc(100% - 30px)', direction:'column'}}>
                    {toolbar}
                    <MultiChartViewer viewerId={chartViewerId} closeable={false}
                                      autoRowOriented={false}
                                      canReceiveNewItems={NewPlotMode.none.key}/>
                </Stack>
            );
        case SHOW_TABLE:
            return (
                <Stack {...{direction: 'column', width:'100%', height:'100%'}}>
                    {toolbar}
                    <TablesContainer tbl_group={tableGroupViewerId} closeable={false} expandedMode={false}/>
                </Stack>
            );
        case SHOW_IMAGE:
            return (
                <Stack {...{direction: 'column', width:'100%', height:'100%'}}>
                    {mayToggle && toolbar}
                    <MultiImageViewer {...{
                        viewerId:imageViewerId, insideFlex:true, serDef, enableCutout,pixelBasedCutout,
                        enableCutoutFullSwitching:cutoutFullPair,
                        canReceiveNewPlots: NewPlotMode.none.key, tableId:metaDataTableId, controlViewerMounting:false,
                        forceRowSize: gridForceRowSize,
                        cutoutToFullWarning,
                        makeDropDown: !mayToggle ? makeDropDown : undefined,
                        Toolbar:ImageMetaDataToolbar, factoryKey}} />
                </Stack>
            );
    }
    return false;
}
