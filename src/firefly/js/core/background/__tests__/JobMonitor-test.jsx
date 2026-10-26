/*eslint-env node, jest */

import React from 'react';
import '@testing-library/jest-dom';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {cloneDeep} from 'lodash';

import * as fetchUtil from '../../../util/fetch.js';
import {getJobInfo} from '../BackgroundUtil.js';
import {dispatchBgLoadJobs} from '../BackgroundCntlr.js';
import {getRootURL} from '../../../util/WebUtil.js';

// __jest__/InitTest.js replaces JobMonitor with a stub for every suite
const {DownloadBtn, ResultsBlock} = jest.requireActual('../JobMonitor.jsx');

const job = {
    meta: {jobId: 'tap-job'},
    jobId: '25113176',
    results: [
        {id: 'result', href: 'https://irsa.ipac.caltech.edu/TAP/async/25113176/results/result'},
        {id: 'log', href: 'https://irsa.ipac.caltech.edu/TAP/async/25113176/results/log'},
    ],
};

// a download() that stays in progress until the test settles it
function stubDownload() {
    let settle;
    const spy = jest.spyOn(fetchUtil, 'download').mockReturnValue(new Promise((resolve) => settle = resolve));
    return {spy, settle: (started) => settle(started)};
}

describe('DownloadBtn', () => {

    afterEach(() => {
        jest.restoreAllMocks();
        dispatchBgLoadJobs({jobs: {}});
    });

    test('renders nothing for a result that has no file to download', () => {
        const asyncJob = {...job, results: [{id: 'result', href: getRootURL() + 'CmdSrv/async/x/results/result'}]};

        const {container} = render(<DownloadBtn job={asyncJob}/>);

        expect(container).toBeEmptyDOMElement();
    });

    test('calls download() with the URL of the row clicked in the Results block', async () => {
        const {spy} = stubDownload();
        render(<ResultsBlock job={job} ActionBtn={DownloadBtn}/>);

        await userEvent.click(screen.getAllByRole('button', {name: 'Click to download file'})[1]);

        expect(spy).toHaveBeenCalledTimes(1);
        expect(new URL(spy.mock.calls[0][0]).searchParams.get('streamURL')).toBe(job.results[1].href);
    });

    test('shows progress, then that the download started', async () => {
        const {settle} = stubDownload();
        render(<DownloadBtn job={job}/>);

        await userEvent.click(screen.getByRole('button'));
        expect(screen.getByRole('button')).toBeDisabled();

        settle(true);
        await waitFor(() => expect(screen.getByRole('button')).toHaveAccessibleName('Download started'));
        expect(screen.getByRole('button')).toBeEnabled();
    });

    test('shows a failed download', async () => {
        const {settle} = stubDownload();
        render(<DownloadBtn job={job}/>);

        await userEvent.click(screen.getByRole('button'));
        settle(false);

        await waitFor(() => expect(screen.getByRole('button')).toHaveAccessibleName('Download may have failed or timed out'));
    });

    test('keeps its state when a poll replaces the job in the store', async () => {
        dispatchBgLoadJobs({jobs: {[job.meta.jobId]: job}});
        const {settle} = stubDownload();
        const {rerender} = render(<DownloadBtn job={getJobInfo(job.meta.jobId)}/>);
        await userEvent.click(screen.getByRole('button'));
        settle(true);
        await waitFor(() => expect(screen.getByRole('button')).toHaveAccessibleName('Download started'));

        dispatchBgLoadJobs({jobs: {[job.meta.jobId]: cloneDeep(job)}});      // what the Job Monitor's poll does
        rerender(<DownloadBtn job={getJobInfo(job.meta.jobId)}/>);

        expect(screen.getByRole('button')).toHaveAccessibleName('Download started');
    });
});
