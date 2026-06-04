import React, { useState, useEffect } from 'react';
import { ChartPanel } from '../../src/charts/index.js';
import { DataTable, makeFileRequest, dispatchTableSearch } from '../../src/tables/index.js';
import { EventLog } from '../helpers.jsx';

const EXAMPLE_CODE = `\
import { ChartPanel } from '@ipac/firefly-component-library/charts';

() => {
    return (
        <ChartPanel
            chartData={{
                data: [{ type: 'scatter', mode: 'markers', x: [1, 2, 3, 4, 5], y: [2, 4, 1, 5, 3] }],
                layout: { xaxis: { title: { text: 'X' } }, yaxis: { title: { text: 'Y' } } },
            }}
            sx={{ height: 400 }}
        />
    );
}`;

export default {
    title: 'Charts/ChartPanel',
    component: ChartPanel,
    tags: ['autodocs'],
    parameters: {
        controls: { disable: true }, actions: { disable: true },
        docs: { source: { code: EXAMPLE_CODE } },
    },
};

// ─── Static Data ──────────────────────────────────────────────────────────────

export const StaticData = () => (
    <ChartPanel
        chartData={{
            data: [{ type: 'scatter', mode: 'markers', x: [1, 2, 3, 4, 5], y: [2, 4, 1, 5, 3] }],
            layout: { xaxis: { title: { text: 'X' } }, yaxis: { title: { text: 'Y' } } },
        }}
        sx={{ height: 400 }}
    />
);

StaticData.storyName = 'Static Data';
StaticData.parameters = { storyDescription: 'Pure Plotly data, no table needed. Pass any Plotly-compatible data and layout.' };

// ─── Linked Table ─────────────────────────────────────────────────────────────

export const LinkedTable = () => {
    const request = makeFileRequest('WISE Demo', 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl', null, { tbl_id: 'wise-chartpanel-linked' });
    return (
        <div style={{ display: 'flex', gap: 8, height: 380 }}>
            <DataTable sx={{flex: 1}} source={request} />
            <ChartPanel
                chartData={{
                    data: [{
                        tbl_id: 'wise-chartpanel-linked',
                        type: 'scatter', mode: 'markers',
                        x: 'tables::crval1', y: 'tables::crval2',
                    }],
                }}
                sx={{flex: 1}}
            />
        </div>
    );
};

LinkedTable.storyName = 'Linked Table';
LinkedTable.parameters = { storyDescription: 'Bind trace columns to a table using tables::colName. Highlights and filters stay in sync.' };

// ─── Event Hooks ──────────────────────────────────────────────────────────────

export const EventHooks = () => {
    const [log, setLog] = useState([]);
    const emit = (msg) => setLog((prev) => [msg, ...prev].slice(0, 8));

    useEffect(() => {
        dispatchTableSearch(makeFileRequest('WISE Demo', 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl', null, { tbl_id: 'wise-chartpanel-events' }));
    }, []);

    return (
        <div style={{ display: 'flex', gap: 8, height: 380 }}>
            <ChartPanel
                chartData={{
                    data: [{
                        tbl_id: 'wise-chartpanel-events',
                        type: 'scatter', mode: 'markers',
                        x: 'tables::crval1', y: 'tables::crval2',
                    }],
                }}
                sx={{flex: 1}}
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
