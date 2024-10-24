import {IconButton, Stack, Tooltip} from '@mui/joy';
import PropTypes from 'prop-types';
import React from 'react';
import CoordinateSys from '../visualize/CoordSys.js';
import {dispatchRecenter} from '../visualize/ImagePlotCntlr.js';
import Point from '../visualize/Point.js';
import {CopyToClipboard} from '../visualize/ui/MouseReadout.jsx';
import {formatLonLatToString} from '../visualize/ui/WorldPtFormat.jsx';
import {convertCelestial} from '../visualize/VisUtil.js';
import GpsFixedIcon from '@mui/icons-material/GpsFixed';

export function FixedPtControl({pv, wp, sx = {}}) {
    if (!wp) return <div/>;
    const llStr = wp.type === Point.W_PT ?
        formatLonLatToString(convertCelestial(wp, CoordinateSys.EQ_J2000)) : `${Math.round(wp.x)},${Math.round(wp.y)}`;

    return (
        <Stack direction='row' sx={sx} alignItems='center'>
            <CopyToClipboard size={20} title={`Copy to the clipboard: ${llStr}`} value={llStr} />
            <Tooltip title='Center on this position'>
                <IconButton onClick={() => pv && dispatchRecenter({plotId: pv.plotId, centerPt: wp})}>
                    <GpsFixedIcon/>
                </IconButton>
            </Tooltip>
        </Stack>
    );
}

FixedPtControl.propTypes = {
    pv: PropTypes.object,
    wp: PropTypes.object,
    sx: PropTypes.object
};