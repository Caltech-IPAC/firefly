import React, { useId, useEffect } from 'react';
import { Box } from '@mui/joy';
import PropTypes from 'prop-types';
import { ApiFullImageDisplay } from 'firefly/visualize/ui/ApiFullImageDisplay.jsx';
import { dispatchPlotImage, dispatchDeletePlotView } from 'firefly/visualize/ImagePlotCntlr.js';
import WebPlotRequest from 'firefly/visualize/WebPlotRequest.js';

/**
 * Displays a FITS image from a URL or a custom WebPlotRequest.
 *
 * Features:
 * - **FITS rendering**: display any URL-accessible FITS file.
 * - **Toolbar**: zoom, pan, rotate, stretch, and color controls.
 * - **Full-screen expand**: expand the image to fill the viewport.
 * - **Advanced requests**: supply a `WebPlotRequest` for service-based or workspace images.
 */
export function ImagePlot({ plotId: plotId_prop, viewerId: viewerId_prop, url, request, sx }) {
    const genId    = useId();
    const plotId   = plotId_prop   ?? `image-${genId}`;
    const viewerId = viewerId_prop ?? `viewer-${genId}`;

    useEffect(() => {
        const wpRequest = request ?? (url ? WebPlotRequest.makeURIPlotRequest(url) : null);
        if (wpRequest) dispatchPlotImage({ plotId, wpRequest, viewerId });
        return () => dispatchDeletePlotView({ plotId });
    }, [plotId, viewerId]); // eslint-disable-line react-hooks/exhaustive-deps

    return (
        <Box sx={{ width: 1, height: 1, ...sx }}>
            <ApiFullImageDisplay viewerId={viewerId} />
        </Box>
    );
}

ImagePlot.propTypes = {
    /**
     * Unique ID for this plot in the Firefly store. Auto-generated if omitted.
     */
    plotId: PropTypes.string,

    /**
     * Unique ID for the viewer container. Auto-generated if omitted.
     * Use a stable ID when coordinating multiple plots in one viewer.
     */
    viewerId: PropTypes.string,

    /**
     * URL of the FITS file to display.
     * Ignored when `request` is provided.
     */
    url: PropTypes.string,

    /**
     * A `WebPlotRequest` instance for advanced use — service queries,
     * workspace files, three-color composites, etc.
     * Takes precedence over `url` when both are provided.
     */
    request: PropTypes.object,

    /** MUI Joy `sx` prop applied to the container. Use to set height, width, etc. */
    sx: PropTypes.object,
};
