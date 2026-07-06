import { firefly } from 'firefly/Firefly.js';
import { setRootURL } from 'firefly/util/WebUtil.js';

const DEFAULT_SERVER_URL = 'http://localhost:8080/firefly/';

/**
 * Initialize the Firefly component library.
 * Returns a Promise that resolves when Firefly is fully ready.
 * Await it (e.g. in Storybook's beforeAll) before rendering image components.
 *
 * @param {object}  [options]
 * @param {string}  [options.serverUrl]    URL of the Firefly server. Defaults to http://localhost:8080/firefly/
 * @param {object}  [options.appOptions]   Additional app options passed to Firefly's store.
 * @returns {Promise<void>}
 */
export function initFirefly({ serverUrl = DEFAULT_SERVER_URL, appOptions = {} } = {}) {
    if (typeof window === 'undefined') return Promise.resolve();
    setRootURL(serverUrl);
    return firefly.bootstrap({}, { serverUrl, ...appOptions });
}
