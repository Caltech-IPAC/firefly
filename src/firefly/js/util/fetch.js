/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isPlainObject, truncate} from 'lodash';
import slug from 'slug';
import {getOrCreateWsConn} from '../core/messaging/WebSocketClient.js';
import {ServerParams} from '../data/ServerParams.js';
import {showInfoPopup} from '../ui/PopupUtil.jsx';
import {logger} from './Logger.js';
import {encodeParams, parseUrl, toNameValuePairs, AJAX_REQUEST, lowLevelDoFetch, REQUEST_WITH, WS_CHANNEL_HD, WS_CONNID_HD} from './WebUtil.js';


/**
 * A wrapper for the underlying window.fetch function.
 * see https://github.com/github/fetch for usage.
 * see https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API for current status on the API
 * see https://fetch.spec.whatwg.org/ for official standard
 *
 * This function applies default behaviors before fetching.
 *
 * @param {string} url the URL to connect
 * @param {Object} options
 * @param {Object} options.params a parameter map to send along with the request.  It does not need to
 *                                  be encoded.  Base on the method used, it will be handled internally.
 * @param {string} options.method can be one of get, post, or multipart
 *                                when 'multipart', it will post with 'multipart/form-data' encoding.
 * @param {boolean} doValidation
 * @param {boolean} [enableDefOptions= true]
 * @return {Promise} a promise of the response when successful, or reject with an Error.
 */
export async function fetchUrl(url, options={}, doValidation = true, enableDefOptions = true) {

    if (!url) return;

    // define defaults request options
    if (!enableDefOptions) return lowLevelDoFetch(url, options, doValidation, logger?.tag('fetchUrl').debug);
    const {connId, channel}= await getOrCreateWsConn();
    const optionsWithDef= {
        method: 'get',
        mode: 'cors',
        credentials: 'include',
        cache: 'default',
        ...options,
        headers: {
            [WS_CHANNEL_HD]: channel,
            [WS_CONNID_HD]: connId,
            [REQUEST_WITH]: AJAX_REQUEST,
            ...options.headers
        }
    };
    return lowLevelDoFetch(url, optionsWithDef, doValidation, logger?.tag('fetchUrl').debug);
}

export function downloadBlob(blob, filename) {
    if (!blob) return;
    window.URL = window.URL || window.webkitURL;
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = window.URL.createObjectURL(blob);
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => window.URL.revokeObjectURL(a.href), 10000);     // revoking right away may cancel the download in some browsers
}

const DOWNLOAD_ERROR_MSG = 'Download failed.  The server did not return a file.';
const DOWNLOAD_TOKEN = 'ffDownloadToken';           // must match AnyFileDownload.DOWNLOAD_TOKEN
const DOWNLOAD_COOKIE_PREFIX = 'ffdl_';             // must match AnyFileDownload.sendDownloadStartedCookie
const POLL_INTERVAL = 500;
const MAX_WAIT = 30 * 60 * 1000;                    // stop waiting for the download to start after this long

/**
 * Downloads a file via a hidden form/iframe, allowing the browser to stream it directly to disk.
 * Use a token cookie to detect when the download starts.
 * @param {string} url  the url to download.  Its query parameters are sent as a POST body.
 * @return {Promise<boolean>} true when started, false on error; never rejects.
 */
export async function download(url) {
    if (!url) return false;
    const {origin, protocol, host, path, hash, searchObject = {}} = parseUrl(url);
    const {connId, channel} = await getOrCreateWsConn();
    const token = Date.now() + '-' + Math.random().toString(36).slice(2);

    // cmd and websocket info go on the query string; the server reads them before the body is parsed
    const cmd = searchObject[ServerParams.COMMAND];
    const query = encodeParams({...(cmd && {[ServerParams.COMMAND]: cmd}), [WS_CONNID_HD]: connId, [WS_CHANNEL_HD]: channel, [DOWNLOAD_TOKEN]: token});
    const action = `${protocol}//${host}${path}?${query}` + (hash ? '#' + hash : '');

    const params = Object.fromEntries(
        Object.entries(searchObject)
            .filter(([k]) => k !== ServerParams.COMMAND)
            .map(([k, v]) => [k, (isPlainObject(v) ? JSON.stringify(v) : v)]));          // convert object back into JSON if needed.

    const iframe = document.createElement('iframe');
    iframe.name = 'ff-download-' + token;
    iframe.style.display = 'none';
    document.body.appendChild(iframe);

    const failed = waitForErrorPage(iframe).then((msg) => {
        showInfoPopup(truncate(msg, {length: 200}), 'Unexpected error');
        iframe.remove();
    });

    submitForm(action, params, iframe.name);

    if (origin !== window.location.origin) return true;       // cannot tell when it starts

    let stopped = false;
    try {
        return await Promise.race([
            waitForCookie(DOWNLOAD_COOKIE_PREFIX + token, () => stopped).then(() => true),
            failed.then(() => false)
        ]);
    } finally {
        stopped = true;     // stop polling for the cookie if the error came first
    }
}

/**
 * The iframe's load event only fires when the server returned a page instead of a file, i.e. an error.
 * @param {HTMLIFrameElement} iframe
 * @return {Promise<string>} resolves to the page's text when readable (same origin), otherwise a generic message.
 */
function waitForErrorPage(iframe) {
    return new Promise((resolve) => {
        iframe.onload = () => {
            let msg;
            try {
                if (iframe.contentWindow.location.href === 'about:blank') return;    // initial load of the empty iframe
                const doc = iframe.contentDocument;
                const text = doc?.body?.innerText?.trim();
                msg = doc?.contentType === 'application/json' ? getJsonErrorMsg(text) : text;
            } catch {}
            resolve(msg || DOWNLOAD_ERROR_MSG);
        };
    });
}

/**
 * Extracts the root-cause message from a server JSON error.
 * @return {string|undefined} the error message, or undefined if invalid.
 */
function getJsonErrorMsg(text) {
    try {
        const causes = Object.values(JSON.parse(text)?.error ?? {});
        return causes.at(-1)?.replace(/^[\w.$]+: /, '');    // the last one is most specific; remove ClassName info
    } catch {
        return undefined;       // not JSON, e.g. the browser rendered it with a JSON viewer
    }
}

/**
 * Wait for the cookie to appear, then remove it.
 * @param {string} name  cookie name
 * @param {function(): boolean} isStopped  stop waiting when it returns true
 * @return {Promise<void>} resolves when the cookie appears, when stopped, or after MAX_WAIT
 */
async function waitForCookie(name, isStopped) {
    const startTime = Date.now();
    while (!isStopped() && Date.now() - startTime < MAX_WAIT) {
        if (document.cookie.split(';').some((c) => c.trim().startsWith(name + '='))) {
            document.cookie = `${name}=; max-age=0; path=/`;
            return;
        }
        await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL));
    }
}

function submitForm(action, params, target) {
    const form = document.createElement('form');
    form.method = 'post';
    form.action = action;
    form.target = target;
    form.style.display = 'none';
    toNameValuePairs(params).forEach(({name, value = ''}) => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        form.appendChild(input);
    });
    document.body.appendChild(form);
    form.submit();
    form.remove();
}


/**
 * create the default download filename
 * @param {String} root - should be one of 'image', 'HiPS', 'table', 'chart'  (we can add to this list as appropriate)
 * @param {String} [title] - the title associated with the visualizer, if falsy then the name is build without a title
 * @param {String} [ext] - the extensionn (without the dot). i.e. 'fits', 'png', 'cvs', etc
 * @return {string}
 */
export function makeDefaultDownloadFileName(root= 'unknown', title='', ext= 'png') {
    const MAX_SAVE_FILE_LENGTH= 50;
    const DOT_SUB= 'DOTDOTDOTDOTDOT';
    const US_SUB= 'UNDERSCOREUNDERSCOREUNDERSCORE';
    const dotExt= '.'  + ext;
    const base= title ? `${root}_${title}` : root;
    let filename=  (base.length<=MAX_SAVE_FILE_LENGTH) ? base : base.substring(0,MAX_SAVE_FILE_LENGTH);
    filename= filename.toLowerCase().endsWith(dotExt) ? filename.substring(0,filename.length-dotExt.length) : filename;
    const tmpF= filename.replaceAll('.',DOT_SUB).replaceAll('_',US_SUB);
    const slugTmpF= slug(tmpF, {lower:false});
    filename= slugTmpF.replaceAll(DOT_SUB, '.').replaceAll(US_SUB,'_');
    return filename+dotExt;
}
