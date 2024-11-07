/*eslint-env node, mocha */

// to run: from firefly/src/firefly
// node ../../node_modules/mocha/bin/mocha js/util/expr/__test__/*test.js --compilers js:babel-core/register --requires ignore-styles

import {encodeUrl, encodeUrlString} from '../WebUtil.js';

describe('A test suite for WebUtil.js', () => {

    /* run before every test-case*/
    beforeEach(() => {
        }
    );
    /* run after every test-case*/
    afterEach(() => {

        }
    );

    test('encodeUrlString', () => {
        // use baseUrl for relative link
        expect(encodeUrlString('/relative-path/ref', 'http://localhost:8080'))
                        .toBe('http://localhost:8080/relative-path/ref');
        // ignore baseUrl if full url is given
        expect(encodeUrlString('http://acme.org/relative-path/ref', 'http://localhost'))
                        .toBe('http://acme.org/relative-path/ref');
        // encode path component
        expect(encodeUrlString('http://acme.org/relative path/ref'))
                        .toBe('http://acme.org/relative%20path/ref');
        // ignore path segment parameters
        expect(encodeUrlString('http://acme.org/relative path;id=42/ref'))
                        .toBe('http://acme.org/relative%20path;id=42/ref');
        // encode parameters
        expect(encodeUrlString('http://acme.org/relative-path/ref?redirectA=http://acme.org/path a/&redirectB=/path b/'))
                        .toBe('http://acme.org/relative-path/ref?redirectA=http%3A%2F%2Facme.org%2Fpath+a%2F&redirectB=%2Fpath+b%2F');
        // encode '=' in param value
        const encoded = encodeUrlString('http://acme.org/relative-path/ref?a=key=123');
        expect(encoded).toBe('http://acme.org/relative-path/ref?a=key%3D123');
        const enUrl = new URL(encoded);
        expect(enUrl.searchParams.get('a')).toBe('key=123');
        // encode hash
        expect(encodeUrlString( 'http://acme.org/relative-path/ref#key=123'))
            .toBe('http://acme.org/relative-path/ref#key%3D123');
        expect(encodeUrlString( 'http://acme.org/relative-path/ref#ID'))
            .toBe('http://acme.org/relative-path/ref#ID');

        // identify encoded URL and avoid re-encoding it.
        const first = encodeUrlString('http://acme.org/relative path/ref?a=key=123');
        const again = encodeUrlString(first);
        expect(first).toBe(again);
    });
});
