
import Layers from '@mui/icons-material/Layers';
import {isString} from 'lodash';
import React, {useRef} from 'react';
import {Badge, Box, Button, Card, Stack, Typography} from '@mui/joy';
import {getWorkingTask} from '../../core/AppDataCntlr';
import Catalog from '../../drawingLayers/Catalog';
import HpxCatalog from '../../drawingLayers/hpx/HpxCatalog';
import HiPSMOC from '../../drawingLayers/HiPSMOC.js';
import {CollapsibleItem} from '../../ui/panel/CollapsiblePanel';
import {useStoreConnector} from '../../ui/SimpleComponent';
import BrowserInfo from '../../util/BrowserInfo';
import {
    getAllDrawLayersForPlot, getLayerTitle, getPlotViewById, isDrawLayerVisible, primePlot
} from '../PlotViewUtil';
import {getDlAry} from '../VisStoreRoots';
import {isHiPS} from '../WebPlot';
import {DrawLayerLegendView} from './DrawLayerItemView';
import {showDrawingLayerPopup} from './DrawLayerPanel';
import {changeVisible, modifyDrawColor} from './DrawLayerUIComponents';


export function SmallLegend(props) {
    const {visRoot,plotId}= props;
    const dlAry= useStoreConnector(() => getDlAry());
    const {message}= useStoreConnector(() => getWorkingTask(plotId));
    const {current:divElementRef}= useRef({divElement:undefined});
    const pv= getPlotViewById(visRoot,plotId);
    if (!pv) return;
    if (pv.viewDim.width<250 || pv.viewDim.height<250) return;
    const smallWhenClosed= (pv.viewDim.width<400 || pv.viewDim.height<300);
    const allLayers= getAllDrawLayersForPlot(dlAry,plotId);
    if (!allLayers) return;
    const layers= allLayers.filter( (dl) => {
        const {drawLayerTypeId, mocTable, }= dl;
        const visible= isDrawLayerVisible(dl,plotId);
        return (
            (drawLayerTypeId===HiPSMOC.TYPE_ID && (mocTable || visible))
            || drawLayerTypeId===HpxCatalog.TYPE_ID
            || drawLayerTypeId===Catalog.TYPE_ID
        );
    });

    const layersLoading= message==='MOC' ? 'Layers Loading...' : undefined;

    const hasLayers= Boolean(layers.length || layersLoading);

    if (!hasLayers) return;
    const showMoreLayers= pv.plotViewCtx.useForSearchResults;

    const layerCnt=  primePlot(pv) ? (allLayers.length + pv.overlayPlotViews.length) : 0;
    const maxTitleChars= layers.reduce( (max,l) => {
        const t= getLayerTitle(pv.plotId,l);
        let tLen= 0;
        if (t) tLen= l.autoFormatTitle? t.length : 30;
        return Math.max(max, tLen);
    },20);

    const sx= (theme) => ({
        '--Card-padding': '.4rem',
        mr:.25,
        overflow:'hidden',
        backgroundColor: BrowserInfo.supportsCssColorMix() ?
            `color-mix(in srgb, ${theme.vars.palette.neutral.softBg} 90%, transparent)` :
            theme.vars.palette.neutral.softBg,
    });

    return (
        <Card sx={sx} ref={(e) => divElementRef.divElement=e}>
            <CollapsibleItem {...{
                componentKey:`${plotId}--Small-legend-state`,
                header: (isOpen) => (<Header {...{isOpen,smallWhenClosed,layers}}/>),
                isOpen:isHiPS(primePlot(pv)),
                slotProps: {
                    header: {
                        slotProps: {
                            button: {
                                component: 'div',
                                sx: { gap: '2px', border:'none', borderRadius: '5px' },
                            }
                        }
                    },
                } }} >
                <Stack>
                    <Box sx={ {maxHeight:'12em', overflowY:'auto', pb: 1/2}}>
                        <Stack>
                            {layersLoading && <Typography level={'body-sm'}>Layers Loading....</Typography>}
                            {layers.map( (dl, idx) => (
                                <DrawLayerLegendView key={idx} {...{
                                    maxTitleChars,
                                    color: dl.drawingDef.color,
                                    canUserChangeColor: dl.canUserChangeColor,
                                    title: getShortTitle(plotId,dl),
                                    tip: getLayerTitle(plotId,dl),
                                    autoFormatTitle: dl.autoFormatTitle,
                                    canUserHide: dl.canUserHide,
                                    visible: isDrawLayerVisible(dl,plotId),
                                    changeVisible: () => changeVisible(dl, !isDrawLayerVisible(dl,plotId),plotId ),
                                    modifyColor: () => modifyDrawColor(dl,pv.plotId,dl.tbl_id),
                                }}/>
                            )) }
                        </Stack>
                    </Box>
                    {showMoreLayers &&
                        <Stack direction='row' sx={{justifyContent: 'flex-end'}}>
                                <Button {...{
                                    size: 'xs',
                                    onClick: () => showDrawingLayerPopup(divElementRef.divElement),
                                    startDecorator:<Layers viewBox={'0 2 20 20'}/>,
                                    endDecorator:
                                        <Badge {...{
                                            badgeContent:layerCnt+'',
                                            sx:{'& .MuiBadge-badge': {top:'-.4rem', right:'1rem'}}
                                            }}>
                                            <Box {...{sx:{width: '2em'}}}/>
                                        </Badge>,
                                    sx: { pl:1/2, pr:0 }
                                }}>
                                    More...
                                </Button>
                        </Stack>
                    }
                </Stack>
            </CollapsibleItem>
        </Card>
    );
}



const Header= ({isOpen,smallWhenClosed,layers}) => {
    const text= (isOpen || !layers?.length)
        ? 'Legend'
        :smallWhenClosed
            ? `(${layers.length})`
            : `Legend (${layers.length})`;
    return (
        <Typography level='body-xs' sx={{minWidth:isOpen ? '21em' : undefined, fontWeight:'bold'}}>
            {text}
        </Typography>
    );
};

function getShortTitle(plotId,dl) {
    const shortTitle= dl.shortTitle;
    if (shortTitle) return shortTitle;
    const title= getLayerTitle(plotId,dl);
    if (isString(title) && title.startsWith('MOC - ')) return title.substr(6);
    return title;
}