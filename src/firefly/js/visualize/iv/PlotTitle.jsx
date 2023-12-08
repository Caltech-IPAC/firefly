/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {Stack, Tooltip, Typography} from '@mui/joy';
import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {sprintf} from '../../externalSource/sprintf';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {FULL, HALF, QUARTER} from '../rawData/RawDataCommon.js';
import {getDataCompress} from '../rawData/RawDataOps.js';
import {ctxToolbarBG} from '../ui/VisCtxToolbarView.jsx';
import {getZoomDesc} from '../ZoomUtil.js';
import {primePlot} from '../PlotViewUtil.js';
import {isImage} from '../WebPlot.js';
import {hasWCSProjection, pvEqualExScroll} from '../PlotViewUtil';

import './PlotTitle.css';
import LOADING from 'html/images/gxt/loading.gif';


export const PlotTitle= memo(({plotView:pv, brief, working}) => {
        const dataCompress= useStoreConnector(() => getDataCompress(primePlot(pv).plotImageId));
        const plot= primePlot(pv);
        const world= hasWCSProjection(plot);
        const zlRet= getZoomDesc(pv);

        let colons= ':';
        let spaces= '';
        switch (dataCompress) {
            case QUARTER:
                spaces= '&nbsp;&nbsp;';
                colons= ':::';
                break;
            case HALF:
                spaces= '&nbsp;';
                colons= '::';
                break;
            case FULL:
                spaces= '';
                colons= ':';
                break;
        }
        const zlStr= world ? `${spaces}FOV:${zlRet.fovFormatted}` : zlRet.zoomLevelFormatted;

        let rotString;
        let flipString;
        if (pv.rotation) {
            if (pv.plotViewCtx.rotateNorthLock) {
                rotString= 'North';
            } else {
                const angleStr= sprintf('%d',Math.trunc(360-pv.rotation));
                rotString= angleStr + String.fromCharCode(176);
            }
        }
        if (pv.flipY) {
            flipString= ', Flip Y';
        }

        const tooltip= (
            <Stack direction='column'>
                <Typography level={plot.title?.length > 30 ? 'body-xs' : 'body-md'}>{plot.title}</Typography>
                {world && tipEntry('Horizontal field of view:',zlRet.fovFormatted) }
                {isImage(plot) && tipEntry('Zoom Level:', zlRet.zoomLevelFormatted)}
                {Boolean(pv.rotation) && tipEntry('Rotation:', rotString)}
                {Boolean(pv.flipY) &&<Typography level='body-sm'>Flipped on Y axis</Typography>}
            </Stack>
        );

        const plotTitleInlineTitleContainer = (theme) => ({
            position : 'relative',
            alignSelf: 'flex-start',
            px : .5,
            backgroundColor : ctxToolbarBG(theme, 85),
            whiteSpace : 'nowrap',
            borderRadius : '0 0 5px 0',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            maxWidth: '100%',
        });

        return (
            <Tooltip title={tooltip} placement='right-end'>
                <Stack direction='row' alignItems='center' sx={plotTitleInlineTitleContainer}>
                    <Typography {...{level:'body-xs', textOverflow: 'ellipsis', overflow: 'hidden', maxWidth: '35ch'}}>{plot.title}</Typography>
                    <Typography {...{level:'body-xs', color:'warning', pl:.75, overflow: 'hidden'}}>
                        <div dangerouslySetInnerHTML={{__html:zlStr}}/>
                    </Typography>
                    {Boolean(!brief && rotString) && <Typography level='body-xs' color='warning'>{`, ${rotString}`}</Typography> }
                    {Boolean(!brief && flipString) && <Typography level='body-xs' color='warning'>{flipString}</Typography>}
                    {working && <img className={'plot-title-working'} src={LOADING}/>}
                </Stack>
            </Tooltip>
        );
    },
    (p, np) => p.titleType===np.titleType && p.working===np.working && p.brief===np.brief &&
        pvEqualExScroll(p.plotView, np.plotView),
);


const tipEntry= (label,value) => (
    <Stack direction='row' spacing={1}>
        <Typography level='body-sm'>{label}</Typography>
        <Typography level='body-sm' color='warning'>{value}</Typography>
    </Stack>
);


PlotTitle.propTypes= {
    plotView : PropTypes.object,
    working : PropTypes.bool,
    brief : PropTypes.bool.isRequired
};

