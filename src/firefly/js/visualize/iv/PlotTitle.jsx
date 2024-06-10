/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {CircularProgress, Stack, Tooltip, Typography} from '@mui/joy';
import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {sprintf} from '../../externalSource/sprintf';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {HALF, QUARTER} from '../rawData/RawDataCommon.js';
import {getDataCompress} from '../rawData/RawDataOps.js';
import {ctxToolbarBG} from '../ui/VisCtxToolbarView.jsx';
import {getZoomDesc} from '../ZoomUtil.js';
import {primePlot} from '../PlotViewUtil.js';
import {isImage} from '../WebPlot.js';
import {hasWCSProjection, pvEqualExScroll} from '../PlotViewUtil';

export const PlotTitle= memo(({plotView:pv, brief, working}) => {
        const dataCompress= useStoreConnector(() => getDataCompress(primePlot(pv).plotImageId));
        const plot= primePlot(pv);
        const world= hasWCSProjection(plot);
        const zlRet= getZoomDesc(pv);
        const flipString= pv.flipY ? ', Flip Y' : '';
        const rotString= getRotateStr(pv);

        const zlStr= world ? `${getSpaces(dataCompress)}FOV:${zlRet.fovFormatted}` : zlRet.zoomLevelFormatted;

        const tooltip= (
            <Stack direction='column'>
                <Typography level={plot.title?.length > 30 ? 'title-xs' : 'title-md'}>{plot.title}</Typography>
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
                    <Typography {...{component:'div', level:'title-sm', textOverflow: 'ellipsis', overflow: 'hidden', maxWidth: '35ch'}}>
                        <div/>
                        {plot.title}
                    </Typography>
                    <Typography {...{level:'body-sm', pl:.75, overflow: 'hidden', component:'div'}}>
                        <div dangerouslySetInnerHTML={{__html:zlStr}}/>
                    </Typography>
                    {Boolean(!brief && rotString) && <Typography level='body-sm' color='warning'>{`, ${rotString}`}</Typography> }
                    {Boolean(!brief && flipString) && <Typography level='body-sm' color='warning'>{flipString}</Typography>}
                    {working && <WorkingIndicator/> }
                </Stack>
            </Tooltip>
        );
    },
    (p, np) => p.titleType===np.titleType && p.working===np.working && p.brief===np.brief &&
        pvEqualExScroll(p.plotView, np.plotView),
);

const WorkingIndicator= () => (
    <CircularProgress
        color='success'
        sx={{
            ml:1,
            '--CircularProgress-percent': '50',
            '--CircularProgress-size': '16px',
            '--CircularProgress-progressThickness': '2px',
            '--CircularProgress-circulation': '1.2s linear 0s infinite normal none running',
        }}
        style= {{
            '--CircularProgress-percent': '51', // for some reason joyUI put this in style and this is the only way to override
        }}
    />);

function getSpaces(dataCompress) {
    if (dataCompress===QUARTER) return '&nbsp;&nbsp;';
    if (dataCompress===HALF) return '&nbsp;';
    return '';
}

function getRotateStr(pv) {
    if (!pv?.rotation) return '';
    if (pv.plotViewCtx.rotateNorthLock) {
        return 'North';
    } else {
        const angleStr= sprintf('%d',Math.trunc(360-pv.rotation));
        return angleStr + String.fromCharCode(176);
    }
}

const tipEntry= (label,value) => (
    <Stack direction='row' spacing={1}>
        <Typography >{label}</Typography>
        <Typography color='warning'>{value}</Typography>
    </Stack>
);


PlotTitle.propTypes= {
    plotView : PropTypes.object,
    working : PropTypes.bool,
    brief : PropTypes.bool.isRequired
};

