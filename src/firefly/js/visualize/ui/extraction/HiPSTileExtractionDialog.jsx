import {Stack, Typography} from '@mui/joy';
import React, {useEffect, useState} from 'react';
import {dispatchShowDialog} from '../../../core/ComponentCntlr';
import ExtractHiPSTileTool from '../../../drawingLayers/ExtractHiPSTileTool';
import {CheckboxGroupInputField} from '../../../ui/CheckboxGroupInputField';
import {CompleteButton} from '../../../ui/CompleteButton';
import DialogRootContainer from '../../../ui/DialogRootContainer';
import {FieldGroup} from '../../../ui/FieldGroup';
import {PopupPanel} from '../../../ui/PopupPanel';
import {useFieldValueOnly, useStoreConnector} from '../../../ui/SimpleComponent';
import {dispatchAttachLayerToPlot, dispatchCreateDrawLayer} from '../../DrawLayerDispatch';
import {extractFitsFromHiPS} from '../../HiPSUtil';
import {dispatchChangeActivePlotView, dispatchWcsMatch} from '../../ImagePlotDispatch';
import {PlotAttribute} from '../../PlotAttribute';
import {currentP, getDrawLayerByType, getPlotViewAry, isDrawLayerAttached, primePlot} from '../../PlotViewUtil';
import {WcsMatchType} from '../../VisConst';
import {getDlAry, visRoot} from '../../VisStoreRoots';
import {isHiPS} from '../../WebPlot';
import {endExtraction, HIPS_TILE_EXTRACT_DIALOG_ID} from './ExtractionUIUtil';


export function showHiPSTileExtractionDialog(element, wasCanceled) {
    endExtraction();
    const dialog= <HiPSTileExtractionDialog{...{wasCanceled}}/>;
    DialogRootContainer.defineDialog(HIPS_TILE_EXTRACT_DIALOG_ID, dialog, element );
    dispatchShowDialog(HIPS_TILE_EXTRACT_DIALOG_ID);
}


function HiPSTileExtractionDialog({wasCanceled}) {
    const {pv,plot} = useStoreConnector( () => currentP());
    const hipsPlotCnt = useStoreConnector(
        () => getPlotViewAry(visRoot())?.filter( (pv) => isHiPS(primePlot(pv))).length ?? 0);

    const doCancel= () => {
        endExtraction();
        wasCanceled?.();
    };

    useEffect(() => {
        startExtraction(pv);
    }, []);


    useEffect(() => {
        if (!hipsPlotCnt) doCancel();
    }, [hipsPlotCnt]);


    const title= isHiPS(plot)
        ? `Extract: ${plot?.title ?? ''}`
        : 'Not a HiPS image';

    return(
        <PopupPanel {...{
            title,
            slotProps: {dialogTitle:{sx:{maxWidth:'30rem'}}},
            closeCallback:doCancel, requestToClose:doCancel }}>
            <HiPSTileExtractionPanel pv={pv} plot={plot}/>
        </PopupPanel>
    );
}


function HiPSTileExtractionPanel({pv,plot}) {
    return (
        <FieldGroup groupKey='HIPS_TILE_EXTRACT_DIALOG_ID' keepState={true}>
            <TileExtractContent plot={plot}/>
        </FieldGroup>
    );
}


function TileExtractContent({plot}) {

    const [warn,setWarn] = useState(false);
    const useWcs= useFieldValueOnly('wcsMatch', 'wcs');
    const hipsCell= plot?.attributes[PlotAttribute.ACTIVE_HIPS_CELL];
    const norder= plot?.attributes[PlotAttribute.ACTIVE_HIPS_NORDER];

    useEffect(() => {
        if (hipsCell && norder && warn) {
            setWarn(false);
        }
    }, [hipsCell,norder]);

    if (!plot) return;

    if (!isHiPS(plot)) {
        return (
            <Typography sx={{p:4, whiteSpace: 'nowrap', minWidth: '30em'}}>
                HiPS tile extraction only available for a HiPS display
            </Typography>
        );
    }


    return (
        <Stack {...{p:.5, spacing:2, alignItems:'stretch', minWidth: '30em',
            overflow: 'hidden', zIndex:1, direction:'column'}}>
            <Typography>
                Click on anywhere on the HiPS display and click 'Extract Tile'
            </Typography>
            <Typography color={(hipsCell && norder) ? undefined : 'warning'}>
                {hipsCell && norder
                    ? `Extract tile: norder:${norder}, tile:${hipsCell?.ipix??'none'}`
                    : 'No tile selected'
                }
            </Typography>
            <CheckboxGroupInputField fieldKey='wcsMatch'
                initialState= {{ value: 'wcs'}}
                options={[ {label: 'Enable WCS Match', value: 'wcs'}]} />
            {!plot.hasFitsCube &&
                <Typography color='warning' level='body-sm'>
                    Warning: Some FITS tiles do not have valid WCS information
                </Typography>}
            <Stack direction='horizontal' alignItems='center' >
                <CompleteButton text= 'Extract Tile' onSuccess={ () => extractTile(plot, useWcs==='wcs',setWarn) } />
                {warn && <Typography color='warning' sx={{pl:4}}> Click on image </Typography> }
            </Stack>
        </Stack>
    );
}

function extractTile(plot,useWcsMatch, setWarn) {
    if (!plot?.hasFits) return;
    const hipsCell= plot.attributes[PlotAttribute.ACTIVE_HIPS_CELL];
    const norder= plot.attributes[PlotAttribute.ACTIVE_HIPS_NORDER];
    if (!hipsCell || !norder) {
        setWarn(true);
        return;
    }
    extractFitsFromHiPS(plot,norder,hipsCell.ipix);
    if (!useWcsMatch) return;
    setTimeout(() => {
        dispatchChangeActivePlotView(plot.plotId);
        dispatchWcsMatch({ plotId: plot.plotId, matchType: WcsMatchType.Standard});
    }, 5 );
}

function startExtraction(pv) {
    if (!pv) return;
    const typeId= ExtractHiPSTileTool.TYPE_ID;
    let extractDl= getDrawLayerByType(getDlAry(), typeId);
    if (!extractDl) dispatchCreateDrawLayer(typeId);
    extractDl= getDrawLayerByType(getDlAry(),typeId);
    !isDrawLayerAttached(extractDl,pv.plotId) && dispatchAttachLayerToPlot(typeId,pv.plotId,true,true, true);
}