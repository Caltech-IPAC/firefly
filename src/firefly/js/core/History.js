/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * url pattern
 * /{module_name}/{launch_page};{path_params}?{query_params}
 *
 *  module_name: synonymous to context name, or application deployed path. i.e.  fftools, application/wise
 *  launch_page: the single html page for launching the application.  i.e. fftools.html
 *  path_params: a=action.type; v=view;
 *               this is used to restore the application's state
 *  query_params: key/value pairs.  this is used to populate action.payload.
 */

import {get, pick, isString} from 'lodash';

import {flux} from '../Firefly.js';
import {TABLE_SEARCH} from '../tables/TablesCntlr.js';
import {encodeUrl, parseUrl} from '../util/WebUtil.js';
import {CH_ID} from '../core/messaging/WebSocketClient.js';
import {SHOW_DROPDOWN} from './LayoutCntlr.js';

const MAX_HISTORY_LENGTH = 20;
const DEF_HANDLER = genericHandler(document.location);

/**
 * a map of all actions that should be in history
 * @type {{}}
 */
const historyAware = [TABLE_SEARCH, SHOW_DROPDOWN]
                        .reduce( (o, v) => {o[v] = DEF_HANDLER; return o;}, {});

var isHistoryEvent = false;

window.onpopstate = function(event) {
    if (get(window, 'firefly.ignoreHistory', false)) return;
    isHistoryEvent = true;
    try {
        if (event.state) {
            flux.process(event.state);
        } else {
            const action = getActionFromUrl();
            action && flux.process(action);
        }
    } finally {
        isHistoryEvent = false;
    }

};

/**
 * returns an action if exists by parsing the url string.
 * The keys and values of the payload will be urldecoded and if the value is
 * a valid JSON string, it will be parsed as well.
 * @returns {Action}
 */
export function getActionFromUrl() {
    if (get(window, 'firefly.ignoreHistory', false)) return;
    const urlInfo = parseUrl(document.location);
    if (urlInfo.searchObject) {
        const type = get(urlInfo, 'pathAry.0.a');
        if (type) {
            const payload = urlInfo.searchObject || {};
            return {type, payload};
        }
    }
    return undefined;
}

export function recordHistory(action={}) {
    if (get(window, 'firefly.ignoreHistory', false) || isHistoryEvent) return;

    const handler = historyAware[action.type];
    if (get(handler, 'actionToUrl')) {
        const url = handler.actionToUrl(action);
        try {
            history.pushState(pick(action, ['type', 'payload']), url, url);
        } catch(e) {}
    }
}

function genericHandler(url='') {
    const urlInfo = parseUrl(url);
    var filename = get(urlInfo,'filename', '');
    var wsch = get(urlInfo, ['searchObject',CH_ID]);
    wsch = get(urlInfo,'pathAry.0.wsch', wsch);
    wsch = wsch ? `;wsch=${wsch}` : '';

    const staticPart = filename + wsch;
    return {
        actionToUrl: (action) => {
            return encodeUrl(`${staticPart};a=${action.type}?`, action.payload);
        }
    };
}
