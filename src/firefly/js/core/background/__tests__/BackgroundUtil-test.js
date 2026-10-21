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

    test('relays a remote result through the Firefly server, with the href intact', () => {
        // a signed storage URL breaks if a single character changes, including its escapes
        const href = 'https://storage.example.org/b/result.parquet?X-Goog-Signature=ab%2Fcd%3D&X-Goog-Expires=3600';

        const {servlet, params} = parse(getResultDownloadUrl(makeJob([{id: 'result', href}])));

        expect(servlet).toBe(downloadServlet());
        expect(params).toEqual({streamURL: href, return: 'TAP 2MASS M81'});
    });

    test('downloads the file the Firefly server saved for a SEARCH job, keeping its extension', () => {
        const href = '${stage}/jobs/GX/GatorQuery2114264923686407498.tbl';

        const {servlet, params} = parse(getResultDownloadUrl(makeJob([{id: 'result', href}], 'Gator 2MASS M81')));

        expect(servlet).toBe(downloadServlet());
        expect(params).toEqual({file: href, return: 'Gator 2MASS M81.tbl'});
    });

    test('returns a PACKAGE result, which is already a Firefly download URL, unchanged', () => {
        const href = downloadServlet() + '?log=true&file=%24%7Bstage%7D%2Fjobs%2FKJ%2FSIASearch-K5na.zip&return=SIASearch.zip';

        expect(getResultDownloadUrl(makeJob([{id: 'SIASearch.zip', href}]))).toBe(href);
    });

    test('offers no download for a Firefly async result, which reruns the search and returns JSON', () => {
        const href = getRootURL() + 'CmdSrv/async/KJ2wy2kHzq6eW7Lz9KK5na/results/result';

        expect(getResultDownloadUrl(makeJob([{id: 'result', href}]))).toBeUndefined();
    });

    test.each([
        ['no results', undefined],
        ['an empty href', [{href: '  '}]],
        ['a relative href', [{href: 'results/result'}]],
        ['an ftp href', [{href: 'ftp://ftp.example.org/x.fits'}]],
        ['a file href', [{href: 'file:///etc/passwd'}]],
    ])('offers no download for %s', (desc, results) => {
        expect(getResultDownloadUrl(makeJob(results))).toBeUndefined();
    });

    test('names each result of a multi-result job after its id, or its position when it has none', () => {
        const job = makeJob([
            {id: 'curl-script', href: 'https://example.org/a.sh'},
            {href: 'https://example.org/b.txt'},
        ]);

        expect(parse(getResultDownloadUrl(job, 0)).params).toEqual({streamURL: 'https://example.org/a.sh', return: 'TAP 2MASS M81-curl-script'});
        expect(parse(getResultDownloadUrl(job, 1)).params).toEqual({streamURL: 'https://example.org/b.txt', return: 'TAP 2MASS M81-1'});
    });

    test('works for a UWS job opened from the Upload panel, which has no job type or title', () => {
        const job = {jobId: '25113176', results: [{id: 'result', href: 'https://irsa.ipac.caltech.edu/TAP/async/25113176/results/result'}]};

        expect(parse(getResultDownloadUrl(job)).params.return).toBe('25113176');
    });
});
