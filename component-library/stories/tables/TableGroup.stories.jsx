import React, { useEffect, useState } from 'react';
import { TableGroup, dispatchTableSearch, makeFileRequest } from '../../src/tables/index.js';
import {EventLog} from '../helpers.jsx';

const EXAMPLE_CODE = `\
import { TableGroup, dispatchTableSearch, makeFileRequest } from '@ipac/firefly-components';

() => {
    useEffect(() => {
        const tblUrl = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
        dispatchTableSearch(makeFileRequest('Table 1', tblUrl));
        dispatchTableSearch(makeFileRequest('Table 2', tblUrl));
    }, []);
    return <TableGroup height: 400 />
}
`;

export default {
    title: 'Tables/TableGroup',
    component: TableGroup,
    tags: ['autodocs'],
    parameters: {
        controls: { disable: true }, actions: { disable: true },
        docs: { source: { code: EXAMPLE_CODE } },
    },
};

// ─── Multiple Groups ─────────────────────────────────────────────────────────
export const MultipleGroups = () => {
    useEffect(() => {
        const tblUrl = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
        dispatchTableSearch(makeFileRequest('Alpha 1', tblUrl), { tbl_group: 'demo-group-a' });
        dispatchTableSearch(makeFileRequest('Alpha 2', tblUrl), { tbl_group: 'demo-group-a' });
        dispatchTableSearch(makeFileRequest('Beta 1',  tblUrl), { tbl_group: 'demo-group-b' });
        dispatchTableSearch(makeFileRequest('Beta 2',  tblUrl), { tbl_group: 'demo-group-b' });
    }, []);

    return (
        <div style={{ display: 'flex', gap: 8, height: 380 }}>
            <TableGroup tbl_group='demo-group-a' style={{ flex: 1, height: '100%' }} />
            <TableGroup tbl_group='demo-group-b' style={{ flex: 1, height: '100%' }} />
        </div>
    );
};

MultipleGroups.storyName = 'Multiple Groups';
MultipleGroups.parameters = { storyDescription: 'When working with multiple TableGroups, `tbl_group` is used as identifier' };

// ─── Event Hooks ─────────────────────────────────────────────────────────────────
export const EventHooks = () => {
    const [log, setLog] = useState([]);
    const emit = (msg) => setLog((prev) => [msg, ...prev].slice(0, 6));

    useEffect(() => {
        const tblUrl = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
        dispatchTableSearch(makeFileRequest('Table 1', tblUrl), { tbl_group: 'demo-default' });
        dispatchTableSearch(makeFileRequest('Table 2', tblUrl), { tbl_group: 'demo-default' });
    }, []);

    return (
        <div style={{ display: 'flex', gap: 24, alignItems: 'flex-start' }}>
            <TableGroup
                sx={{ flex: '1 1 0', height: 380 }}
                tbl_group='demo-default'
                events={{
                    onActiveChange: (tbl_id) => emit(`onActiveChange: ${tbl_id}`),
                    onTableAdded:   (tbl_id) => emit(`onTableAdded: ${tbl_id}`),
                    onTableRemoved: (tbl_id) => emit(`onTableRemoved: ${tbl_id}`),
                }}
            />
            <EventLog log={log} label='switch tabs or close a table' />
        </div>
    );
};
EventHooks.storyName = 'Event Hooks';
EventHooks.parameters = { storyDescription: 'Watches tabs events' };

