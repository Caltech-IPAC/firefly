/*eslint-env node, jest */

import {getResultDownloadUrl} from '../BackgroundUtil.js';
import {getRootURL} from '../../../util/WebUtil.js';

const downloadServlet = () => getRootURL() + 'servlet/Download';

// splits a download URL into the servlet it calls and the parameters the Firefly server decodes from it
function parse(url) {
    const {origin, pathname, searchParams} = new URL(url);
    return {servlet: origin + pathname, params: Object.fromEntries(searchParams)};
}

const makeJob = (results, title = 'TAP 2MASS M81') => ({jobId: '25113176', jobInfo: {title}, results});

describe('getResultDownloadUrl', () => {

    test('builds a Download servlet URL with the remote href as streamURL, unchanged', () => {
        // a signed storage URL breaks if a single character changes, including its escapes
        const href = 'https://storage.example.org/b/result.parquet?X-Goog-Signature=ab%2Fcd%3D&X-Goog-Expires=3600';

        const {servlet, params} = parse(getResultDownloadUrl(makeJob([{id: 'result', href}])));

        expect(servlet).toBe(downloadServlet());
        expect(params).toEqual({streamURL: href, return: 'TAP 2MASS M81'});
    });

    test('builds a Download servlet URL for a SEARCH job\'s saved file, with its extension on the name', () => {
        const href = '${stage}/jobs/GX/GatorQuery2114264923686407498.tbl';

        const {servlet, params} = parse(getResultDownloadUrl(makeJob([{id: 'result', href}], 'Gator 2MASS M81')));

        expect(servlet).toBe(downloadServlet());
        expect(params).toEqual({file: href, return: 'Gator 2MASS M81.tbl'});
    });

    test('returns a PACKAGE result, which is already a Firefly download URL, unchanged', () => {
        const href = downloadServlet() + '?log=true&file=%24%7Bstage%7D%2Fjobs%2FKJ%2FSIASearch-K5na.zip&return=SIASearch.zip';

        expect(getResultDownloadUrl(makeJob([{id: 'SIASearch.zip', href}]))).toBe(href);
    });

    test('returns no URL for a Firefly async result', () => {
        const href = getRootURL() + 'CmdSrv/async/KJ2wy2kHzq6eW7Lz9KK5na/results/result';

        expect(getResultDownloadUrl(makeJob([{id: 'result', href}]))).toBeUndefined();
    });

    test.each([
        ['no results', undefined],
        ['an empty href', [{href: '  '}]],
        ['a relative href', [{href: 'results/result'}]],
        ['an ftp href', [{href: 'ftp://ftp.example.org/x.fits'}]],
        ['a file href', [{href: 'file:///etc/passwd'}]],
    ])('returns no URL for %s', (desc, results) => {
        expect(getResultDownloadUrl(makeJob(results))).toBeUndefined();
    });

    test('appends each result\'s id, or its position when it has none, to the download name of a multi-result job', () => {
        const job = makeJob([
            {id: 'curl-script', href: 'https://example.org/a.sh'},
            {href: 'https://example.org/b.txt'},
        ]);

        expect(parse(getResultDownloadUrl(job, 0)).params).toEqual({streamURL: 'https://example.org/a.sh', return: 'TAP 2MASS M81-curl-script'});
        expect(parse(getResultDownloadUrl(job, 1)).params).toEqual({streamURL: 'https://example.org/b.txt', return: 'TAP 2MASS M81-1'});
    });

    test('names the download after the jobId when the job has no title or runId', () => {
        const job = {jobId: '25113176', results: [{id: 'result', href: 'https://irsa.ipac.caltech.edu/TAP/async/25113176/results/result'}]};

        expect(parse(getResultDownloadUrl(job)).params.return).toBe('25113176');
    });
});
