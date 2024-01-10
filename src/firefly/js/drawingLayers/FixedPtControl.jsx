import {IconButton, Stack, Tooltip} from '@mui/joy';
import CENTER from 'images/20x20-center-small.png';
import CHECKED from 'images/20x20_clipboard-checked.png';
import CLIPBOARD from 'images/20x20_clipboard.png';
import PropTypes from 'prop-types';
import React, {useState} from 'react';
import {copyToClipboard} from '../util/WebUtil.js';
import CoordinateSys from '../visualize/CoordSys.js';
import {dispatchRecenter} from '../visualize/ImagePlotCntlr.js';
import Point from '../visualize/Point.js';
import {formatLonLatToString} from '../visualize/ui/WorldPtFormat.jsx';
import {convert} from '../visualize/VisUtil.js';

export function FixedPtControl({pv, wp, sx = {}}) {
    const llStr = wp.type === Point.W_PT ?
        formatLonLatToString(convert(wp, CoordinateSys.EQ_J2000)) : `${Math.round(wp.x)},${Math.round(wp.y)}`;
    const [clipIcon, setClipIcon] = useState(CLIPBOARD);

    const doCopy = (str) => {
        copyToClipboard(str);
        setTimeout(() => {
            setClipIcon(CHECKED);
            setTimeout(() => setClipIcon(CLIPBOARD), 750);
        }, 10);
    };

    return (
        <Stack direction='row' sx={sx}>
            <Tooltip title={`Copy to the clipboard: ${llStr}`}>
                <IconButton onClick={() => doCopy(llStr)}>
                    <img src={clipIcon}/>
                </IconButton>
            </Tooltip>
            <Tooltip title='Center on this position'>
                <IconButton onClick={() => pv && dispatchRecenter({plotId: pv.plotId, centerPt: wp})}>
                    <img src={CENTER}/>
                </IconButton>
            </Tooltip>
        </Stack>
    );
}

FixedPtControl.propTypes = {
    pv: PropTypes.object,
    wp: PropTypes.object.isRequired,
    sx: PropTypes.object
};