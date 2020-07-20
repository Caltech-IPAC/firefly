import {encodeUrl, toNameValuePairs} from './WebUtil.js';

export const REQUEST_WITH = 'X-Requested-With';
export const AJAX_REQUEST = 'XMLHttpRequest';
export const WS_CHANNEL_HD = 'FF-channel';
export const WS_CONNID_HD = 'FF-connID';


/**
 * File is safe to use from a WebWorker
 */

/**
 * Unless you have reason don't call this function directly, call fetchUrl
 * @param url
 * @param options
 * @param doValidation
 * @param logger
 * @return {Promise<Response>}
 */
export async function doFetchUrl(url, options, doValidation, logger) {
    if (options.params) {
        const params = toNameValuePairs(options.params);        // convert to name-value pairs if it's a simple object.
        if (options.method.toLowerCase() === 'get') {
            url = encodeUrl(url, params);
        } else {
            url = encodeUrl(url);
            if (!options.body) {
                // if 'post' but, body is not provided, add the parameters into the body.
                if (options.method.toLowerCase() === 'post') {
                    options.headers['Content-type'] = 'application/x-www-form-urlencoded';
                    options.body = params.map(({name, value = ''}) => encodeURIComponent(name) + '=' + encodeURIComponent(value))
                        .join('&');
                } else if (options.method.toLowerCase() === 'multipart') {
                    options.method = 'post';
                    const data = new FormData();
                    params.forEach(({name, value}) => {
                        data.append(name, value);
                    });
                    options.body = data;
                }
                Reflect.deleteProperty(options, 'params');
            }
        }
    }

    logger?.tag('fetchUrl').debug({url, options});
    // do the actually fetch, then return a promise.
    const response= await fetch(url, options);
    if (!doValidation || response.ok) return response;
    else if (response.status === 401) throw new Error('You are no longer logged in');
    else throw new Error(`Request failed with status ${response.status}: ${url}`);
}
