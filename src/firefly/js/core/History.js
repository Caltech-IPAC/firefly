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

import {get, pick} from 'lodash';

import {flux} from '../Firefly.js';
import {TABLE_SEARCH} from '../tables/TablesCntlr.js';
import {encodeUrl, parseUrl} from '../util/WebUtil.js';
import * as LO from './LayoutCntlr.js';

const MAX_HISTORY_LENGTH = 20;
const DEF_HANDLER = genericHandler('fftools.html');

/**
 * a map of all actions that should be in history
 * @type {{}}
 */
const historyAware = [  TABLE_SEARCH, LO.LO_EXPANDED, LO.LO_STANDARD,
                        LO.SHOW_DROPDOWN_UI
                    ].reduce( (o, v) => {o[v] = DEF_HANDLER; return o;}, {});

var isHistoryEvent = false;

window.onpopstate = function(event) {
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

export function getActionFromUrl() {
    const urlObj = parseUrl(document.location);
    if (urlObj.searchObject) {
        const type = get(urlObj.pathAry, '0.a');
        if (type) {
            return {type, payload: urlObj.searchObject};
        }
    }
    return undefined;
}

export function recordHistory(action={}) {
    if (isHistoryEvent) return;

    const handler = historyAware[action.type];
    if (get(handler, 'actionToUrl')) {
        const url = handler.actionToUrl(action);
        history.pushState(pick(action, ['type', 'payload']), url, url);
    }
}


function genericHandler(baseUrl='') {
    return {
        actionToUrl: (action) => {
            return encodeUrl(`${baseUrl};a=${action.type}?`, action.payload);
        }
    };
}

