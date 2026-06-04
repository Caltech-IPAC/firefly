import React, { useState, useEffect } from 'react';
import { ScatterChart } from '../../src/charts/index.js';
import { DataTable, makeFileRequest, dispatchTableSearch } from '../../src/tables/index.js';
import { EventLog } from '../helpers.jsx';

const EXAMPLE_CODE = `\
import { ScatterChart } from '@ipac/firefly-component-library/charts';

() => {
    return <ScatterChart tbl_id="my-table" x_axis="ra" y_axis="dec" sx={{ height: 400 }} />;
}`;

export default {
    title: 'Charts/ScatterChart',
    component: ScatterChart,
    tags: ['autodocs'],
    parameters: {
        controls: { disable: true }, actions: { disable: true },
        docs: { source: { code: EXAMPLE_CODE } },
    },
};

// ─── Basic ────────────────────────────────────────────────────────────────────

export const Basic = () => {
    useEffect(() => {
        dispatchTableSearch(makeFileRequest('WISE Demo', 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl', null, { tbl_id: 'wise-scatter-basic' }));
    }, []);
    return <ScatterChart tbl_id="wise-scatter-basic" x_axis="crval1" y_axis="crval2" sx={{ height: 400 }} />;
};

Basic.storyName = 'Basic';
Basic.parameters = { storyDescription: 'Minimal usage. Required props only.' };

// ─── Linked Table ─────────────────────────────────────────────────────────────

export const LinkedTable = () => {
    const request = makeFileRequest('WISE Demo', 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl', null, { tbl_id: 'wise-scatter-linked' });
    return (
        <div style={{ display: 'flex', gap: 8, height: 380 }}>
            <DataTable sx={{flex: 1}} source={request} />
            <ScatterChart tbl_id="wise-scatter-linked" x_axis="crval1" y_axis="crval2" sx={{flex: 1}} />
        </div>
    );
};

LinkedTable.storyName = 'Linked Table';
LinkedTable.parameters = { storyDescription: 'Table and chart share the same tbl_id. Highlights and filters stay in sync.' };

// ─── Event Hooks ──────────────────────────────────────────────────────────────

export const EventHooks = () => {
    const [log, setLog] = useState([]);
    const emit = (msg) => setLog((prev) => [msg, ...prev].slice(0, 8));

    useEffect(() => {
        dispatchTableSearch(makeFileRequest('WISE Demo', 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl', null, { tbl_id: 'wise-scatter-events' }));
    }, []);

    return (
        <div style={{ display: 'flex', gap: 24, alignItems: 'flex-start' }}>
            <ScatterChart
                tbl_id="wise-scatter-events"
                x_axis="crval1"
                y_axis="crval2"
                sx={{ flex: '1 1 0', height: 400 }}
                events={{
                    onHighlight: (highlighted) => emit(`onHighlight: ${JSON.stringify(highlighted)}`),
                    onSelect:    (selected)    => emit(`onSelect: ${JSON.stringify(selected)}`),
                    onLoaded:    (_, chartId)  => emit(`onLoaded: ${chartId}`),
                }}
            />
            <EventLog log={log} label="click or lasso-select points" />
        </div>
    );
};

EventHooks.storyName = 'Event Hooks';
EventHooks.parameters = { storyDescription: 'Click or lasso-select points to see event callbacks fire in real time.' };
