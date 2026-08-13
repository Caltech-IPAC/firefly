import React from 'react';
import { Box } from '@mui/joy';
import PropTypes from 'prop-types';
import { CoverageViewer } from 'firefly/visualize/ui/CoveraeViewer.jsx';
import {DEFAULT_COVERAGE_VIEWER_ID} from 'firefly/visualize/VisConst.js';

/**
 * Displays table coverage/footprint overlaid on a sky image as markers.
 *
 * Features:
 * - **Auto-detection**: watches loaded tables and renders coverage when available.
 * - **Sky background**: shows a HiPS or FITS image behind the coverage footprints.
 * - **Table sync**: coverage updates automatically when the active table changes.
 * - **No setup required**: just mount it alongside a `DataTable` with the same `tbl_id`.
 */
export function Coverage({ viewerId = DEFAULT_COVERAGE_VIEWER_ID, noCovMessage, workingMessage, sx }) {
    return (
        <Box sx={{ width: 1, height: 1, ...sx }}>
            <CoverageViewer
                viewerId={viewerId}
                noCovMessage={noCovMessage}
                workingMessage={workingMessage}
            />
        </Box>
    );
}

Coverage.propTypes = {
    /**
     * Viewer ID for the underlying image viewer.
     * @default 'CoverageImages'
     */
    viewerId: PropTypes.string,

    /**
     * Message shown when the active table has no coverage data.
     * @default 'No coverage available'
     */
    noCovMessage: PropTypes.string,

    /**
     * Message shown while coverage is being computed.
     * @default 'Working...'
     */
    workingMessage: PropTypes.string,

    /** MUI Joy `sx` prop applied to the container. Use to set height, width, etc. */
    sx: PropTypes.object,
};
