import React, { useEffect } from 'react';
import { HistogramChart } from '../../src/charts/index.js';
import { DataTable, makeFileRequest, dispatchTableSearch } from '../../src/tables/index.js';

const EXAMPLE_CODE = `\
import { HistogramChart } from '@ipac/firefly-component-library/charts';

() => {
    return <HistogramChart tbl_id="my-table" column="magzp" sx={{ height: 400 }} />;
}`;

export default {
    title: 'Charts/HistogramChart',
    component: HistogramChart,
    tags: ['autodocs'],
    parameters: {
        controls: { disable: true }, actions: { disable: true },
        docs: { source: { code: EXAMPLE_CODE } },
    },
};

// ─── Basic ────────────────────────────────────────────────────────────────────

export const Basic = () => {
    useEffect(() => {
        const url = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
        dispatchTableSearch(makeFileRequest('WISE Demo', url, null, { tbl_id: 'wise-histogram-basic' }));
    }, []);
    return <HistogramChart tbl_id="wise-histogram-basic" column="magzp" sx={{ height: 400 }} />;
};

Basic.storyName = 'Basic';
Basic.parameters = { storyDescription: 'Minimal usage. Required props only.' };

// ─── Bayesian Blocks ──────────────────────────────────────────────────────────

export const BayesianBlocks = () => {
    useEffect(() => {
        const url = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
        dispatchTableSearch(makeFileRequest('WISE Demo', url, null, { tbl_id: 'wise-histogram-bayes' }));
    }, []);
    return (
        <HistogramChart
            tbl_id="wise-histogram-bayes"
            column="magzp"
            sx={{ height: 400 }}
            chartData={{
                data: [{ firefly: { options: { algorithm: 'bayesianBlocks', falsePositiveRate: 0.05 } } }],
                layout: { title: { text: 'Magnitude Zero Point: Bayesian Blocks' } },
            }}
        />
    );
};

BayesianBlocks.storyName = 'Bayesian Blocks';
BayesianBlocks.parameters = { storyDescription: 'Adaptive binning via chartData. Finds natural breaks rather than using a fixed bin count.' };

// ─── Linked Table ─────────────────────────────────────────────────────────────

export const LinkedTable = () => {
    const url = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
    const request = makeFileRequest('WISE Demo', url, null, { tbl_id: 'wise-histogram-linked' });
    return (
        <div style={{ display: 'flex', gap: 8, height: 380 }}>
            <DataTable source={request} sx={{ flex: 1 }} />
            <HistogramChart tbl_id="wise-histogram-linked" column="magzp" sx={{ flex: 1 }} />
        </div>
    );
};

LinkedTable.storyName = 'Linked Table';
LinkedTable.parameters = { storyDescription: 'Table and histogram share the same tbl_id. Filtering the table updates the histogram.' };
