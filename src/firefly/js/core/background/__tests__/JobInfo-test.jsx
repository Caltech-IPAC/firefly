/*eslint-env node, jest */

import React from 'react';
import '@testing-library/jest-dom';
import {render as rtlRender, screen} from '@testing-library/react';
import {CssVarsProvider} from '@mui/joy';

import {UwsJobInfo} from '../JobInfo.jsx';

const jobUrl = 'https://irsa.ipac.caltech.edu/TAP/async/25113176';

const tapJob = {
    meta: {jobId: 'tap-job', type: 'TAP'},
    jobId: '25113176',
    ownerId: 'loi',
    phase: 'COMPLETED',
    executionDuration: 3600,
    creationTime: '2026-10-21T22:00:00Z',
    startTime: '2026-10-21T22:00:05Z',
    endTime: '2026-10-21T22:01:35Z',
    quote: '2026-10-21T23:00:00Z',
    destruction: '2026-10-28T22:00:00Z',
    parameters: {lang: 'ADQL', query: 'SELECT TOP 10 * FROM fp_psc'},
    jobInfo: {title: 'TAP 2MASS M81', jobUrl},
};

// the ADQL viewer reads the color scheme, which needs a Joy theme above it; jsdom has no matchMedia for the theme to query
window.matchMedia ??= () => ({matches: false, addListener: () => {}, removeListener: () => {}});
const render = (ui) => rtlRender(ui, {wrapper: CssVarsProvider});

// A KeywordBlock renders its label right before its value, so a value is checked as 'Label:value' in the page's text.

describe('UwsJobInfo', () => {

    test('shows the job title', () => {
        const {container} = render(<UwsJobInfo jobInfo={tapJob}/>);

        expect(container).toHaveTextContent('Title:TAP 2MASS M81');
    });

    describe('Job ID', () => {

        test('labels a TAP job ID, and links it to the job', () => {
            render(<UwsJobInfo jobInfo={tapJob}/>);

            expect(screen.getByText('TAP Job ID:')).toBeInTheDocument();
            expect(screen.getByRole('link', {name: '25113176'})).toHaveAttribute('href', jobUrl);
        });

        test('labels a UWS job ID', () => {
            render(<UwsJobInfo jobInfo={{...tapJob, meta: {type: 'UWS'}}}/>);

            expect(screen.getByText('UWS Job ID:')).toBeInTheDocument();
        });

        test('labels a Firefly job ID, without a link', () => {
            const {container} = render(<UwsJobInfo jobInfo={{...tapJob, meta: {type: 'SEARCH'}}}/>);

            expect(container).toHaveTextContent('Job ID:25113176');
            expect(screen.queryByRole('link', {name: '25113176'})).not.toBeInTheDocument();
        });
    });

    test('shows times in UTC, and the actual run time', () => {
        const {container} = render(<UwsJobInfo jobInfo={tapJob}/>);

        expect(container).toHaveTextContent('Start Time:2026-10-21 22:00:05');
        expect(container).toHaveTextContent('Actual run time:90s');
    });

    describe('Progress', () => {

        test('is shown while the job is active', () => {
            const job = {...tapJob, phase: 'EXECUTING', jobInfo: {...tapJob.jobInfo, progress: {message: 'Querying the archive'}}};
            render(<UwsJobInfo jobInfo={job}/>);

            expect(screen.getByText('Progress:')).toBeInTheDocument();
            expect(screen.getByText('Querying the archive')).toBeInTheDocument();
        });

        test('is hidden once the job is done', () => {
            render(<UwsJobInfo jobInfo={tapJob}/>);

            expect(screen.queryByText('Progress:')).not.toBeInTheDocument();
        });
    });

    describe('ADQL query', () => {

        test('is shown for an ADQL job', () => {
            render(<UwsJobInfo jobInfo={tapJob}/>);

            expect(screen.getByText('ADQL QUERY')).toBeInTheDocument();
        });

        test('is hidden when the query is not ADQL', () => {
            render(<UwsJobInfo jobInfo={{...tapJob, parameters: {lang: 'PQL', query: 'whatever'}}}/>);

            expect(screen.queryByText('ADQL QUERY')).not.toBeInTheDocument();
        });
    });

    test('lists a parameter once for each value in a \':::\' delimited list', () => {
        const {container} = render(<UwsJobInfo jobInfo={{...tapJob, parameters: {band: 'J:::H'}}} isOpen={true}/>);

        expect(container).toHaveTextContent('band:J');
        expect(container).toHaveTextContent('band:H');
    });

    test('shows the error summary, with a link to its details', () => {
        const job = {...tapJob, phase: 'ERROR', errorSummary: {message: 'Table not found', type: 'fatal', hasDetail: true}};
        const {container} = render(<UwsJobInfo jobInfo={job} isOpen={true}/>);

        expect(container).toHaveTextContent('Message:Table not found');
        expect(container).toHaveTextContent('Type:fatal');
        expect(screen.getByRole('link', {name: `${jobUrl}/error`})).toHaveAttribute('href', `${jobUrl}/error`);
    });
});
