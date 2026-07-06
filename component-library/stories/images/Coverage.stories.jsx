import React from 'react';
import { Coverage } from '../../src/images/index.js';
import { DataTable, makeFileRequest } from '../../src/tables/index.js';

const WISE_URL = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';

const EXAMPLE_CODE = `\
import { Coverage } from '@ipac/firefly-components/images';
import { DataTable, makeFileRequest } from '@ipac/firefly-components/tables';

() => {
    const request = makeFileRequest('My Table', 'https://example.com/catalog.tbl', null, { tbl_id: 'my-table' });
    return (
        <div style={{ display: 'flex', gap: 8, height: 600 }}>
            <DataTable sx={{ flex: '1 1 0' }} source={request} />
            <Coverage sx={{ flex: '1 1 0' }} />
        </div>
    );
}`;

export default {
    title: 'Images/Coverage',
    component: Coverage,
    tags: ['autodocs'],
    parameters: {
        controls: { disable: true }, actions: { disable: true },
        docs: { source: { code: EXAMPLE_CODE } },
    },
};

// ─── With Table ───────────────────────────────────────────────────────────────

export const WithTable = () => {
    const request = makeFileRequest('WISE Demo', WISE_URL, null, { tbl_id: 'wise-coverage' });
    return (
        <div style={{ display: 'flex', gap: 8, height: 600 }}>
            <DataTable sx={{ flex: '1 1 0' }} source={request} />
            <Coverage sx={{ flex: '1 1 0' }} />
        </div>
    );
};

WithTable.storyName = 'With Table';
WithTable.parameters = { storyDescription: 'Coverage auto-detects RA/Dec columns from the active table and overlays footprints on a sky image.' };
