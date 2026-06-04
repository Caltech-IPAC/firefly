import React, { useMemo, useState } from 'react';
import { DataTable, makeFileRequest } from '../../src/tables/index.js';
import { EventLog } from '../helpers.jsx';

const EXAMPLE_CODE = `\
import { DataTable, makeFileRequest } from '@ipac/firefly-component-library/tables';

() => {
    const request = makeFileRequest('My Table', 'https://sample-data.com/my-table.vot');
    return <DataTable source={request} sx={{ height: 400 }} />;
}
`;

export default {
    title: 'Tables/DataTable',
    component: DataTable,
    tags: ['autodocs'],
    parameters: {
        controls: { disable: true }, actions: { disable: true },
        docs: { source: { code: EXAMPLE_CODE } },
    },
};

// ─── Demo ────────────────────────────────────────────────────────────────
export const Demo = ({ showToolbar, showTitle, showPaging, showFilterButton, showSave, expandable }) => {
    const url = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
    const request = useMemo(() => makeFileRequest('WISE Table', url), []);
    return (
        <DataTable
            sx={{ height: 380 }}
            source={request}
            options={{ showToolbar, showTitle, showPaging, showFilterButton, showSave, expandable }}
        />
    );
};

Demo.storyName = 'Demo';
Demo.parameters = { controls: { disable: false }, storyDescription: 'Toggle toolbar options with the Controls panel below.' };
Demo.argTypes = Object.fromEntries(         // hide these from Controls
    ['tbl_ui_id', 'options', 'events', 'source']
    .map((k) => [k, { table: { disable: true } }])
);
// Not adding showFilters, selectable, and border to Storybook Controls because of how Firefly stores its UI state.
Demo.args = {
    showToolbar:      true,
    showTitle:        true,
    showPaging:       true,
    showFilterButton: true,
    showSave:         true,
    expandable:       true,
};

// ─── Basic ────────────────────────────────────────────────────────────────
export const Basic = () => {
    const url = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
    return <DataTable sx={{ height: 320 }} source={makeFileRequest('Basic', url)}/>;
};

Basic.storyName = 'Basic';
Basic.parameters = { storyDescription: 'Most basic use case' };

// ─── Custom ────────────────────────────────────────────────────────────────
export const Custom = () => {
    const url = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
    const request = makeFileRequest('Plain table', url, null, { pageSize: -1, removeable: false });
    return (
        <DataTable
            sx={{ height: 250, width: 600 }}
            source={request}
            options={{ selectable: false, showPaging: false, removable: false }}
        />
    );
};

Custom.storyName = 'Custom';
Custom.parameters = { storyDescription: 'Custom: No paging with fixed dimension. Useful when embedding inside a larger layout.' };

// ─── Headless ────────────────────────────────────────────────────────────────
export const Headless = () => {
    const url = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
    const request = makeFileRequest('Plain table', url);
    return (
        <DataTable
            sx={{ height: 320 }}
            source={request}
            options={{ selectable: false, showToolbar: false, border: false }}
        />
    );
};

Headless.storyName = 'Headless (no toolbar)';
Headless.parameters = { storyDescription: 'All toolbar chrome removed. Useful when embedding inside a larger layout.' };

// ─── Event Hooks ─────────────────────────────────────────────────────────────
export const EventHooks = () => {
    const [log, setLog] = useState([]);
    const emit = (msg) => setLog((prev) => [msg, ...prev].slice(0, 8));

    const url = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
    const request = useMemo(() => makeFileRequest('Event Hooks', url), []);

    return (
        <div style={{ display: 'flex', gap: 24, alignItems: 'flex-start' }}>
            <DataTable
                sx={{ flex: '1 1 0', height: 380 }}
                source={request}
                events={{
                    onLoaded:    (tbl)        => emit(`onLoaded: ${tbl.totalRows} rows`),
                    onHighlight: (rowIdx)     => emit(`onHighlight: row ${rowIdx}`),
                    onSelect:    (selectInfo) => emit(`onSelect: ${JSON.stringify(selectInfo)}`),
                    onSort:      (sortInfo)   => emit(`onSort: ${sortInfo}`),
                    onFilter:    (filters)    => emit(`onFilter: ${filters}`),
                }}
            />
            <EventLog log={log} label='interact with the table' />
        </div>
    );
};

EventHooks.storyName = 'Event Hooks';
EventHooks.parameters = { storyDescription: 'Watches table events.' };

