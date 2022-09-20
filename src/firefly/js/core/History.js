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

import {get, pick, omitBy, set, isEmpty} from 'lodash';

import {flux} from './ReduxFlux';
import {TABLE_SEARCH, LOG_HISTORY} from '../tables/TablesCntlr.js';
import {encodeParams, parseUrl} from '../util/WebUtil.js';
import {SHOW_DROPDOWN} from './LayoutCntlr.js';

export const ACTION = '__action';
export const WSCH = '__wsch';

export function setLogHistory (action, flg) {
    return set(action, 'payload.options.logHistory', flg);
}

function logHistory (action, def) {
    return get(action, 'payload.options.logHistory', def);
}

const DEF_HANDLER = {
    actionToUrl: (action) => {
        return urlPrefix + `${ACTION}=${action.type}&` + encodeParams(action.payload);
    },
    urlToAction: (type,urlInfo) => {
        const payload = omitBy(urlInfo.searchObject, (v,k) => k.startsWith && k.startsWith('__')) || {};
        return {type, payload};
    }
};

const urlPrefix = (() => {
    const  urlInfo = parseUrl(document.location);
    const filename = get(urlInfo,'filename', '');
    let wsch = get(urlInfo, `searchObject.${WSCH}`);
    wsch = wsch ? `${WSCH}=${wsch}&` : '';
    return filename + '?' + wsch;
})();

const tableSearchHandler = {
    actionToUrl: (action) => {
        const logHistory = get(action, ['payload', 'options', LOG_HISTORY], true);
        return logHistory && urlPrefix + `${ACTION}=${action.type}&` + encodeParams(action.payload);
    },
    urlToAction: DEF_HANDLER.urlToAction
};

const dropdownHandler = {
    actionToUrl: (action) => {
        const logHistory = get(action, 'payload.visible', true);
        if (!logHistory) return false;
        const {view,initArgs={}}= action.payload ?? {};
        const {urlApi}= initArgs;
        let params={};
        if (!isEmpty(urlApi)) {
            params= Object.entries(urlApi).reduce((obj,[k,v]) => {
                obj['initArgs.urlApi.'+k]=v;
                return obj;
            },{view});
        }
        else if (view) {
            params= {view};
        }
        return urlPrefix + `${ACTION}=${action.type}&` + encodeParams(params);
    },
    urlToAction: (type,urlInfo) => {
        const prefix= 'initArgs';
        const {payload} = DEF_HANDLER.urlToAction(type, urlInfo);
        if (Object.keys(payload).find((k) => k.startsWith(prefix))) {
            const startIdx= prefix.length+1;
            const newPayload= Object.entries(payload).reduce(
                (obj,[k,v]) =>{
                    if (k.startsWith(prefix) ) obj.initArgs[k.substring(startIdx)]=v;
                    else obj[k]= v;
                    return obj;
                },{initArgs:{}});
            return {type,payload:newPayload};
        }
        else {
            return {type,payload};
        }
    }
};

/**
 * a map of all actions that should be in history
 * @type {{}}
 */
const getCustomHistoryHandlers = () => ({
    [TABLE_SEARCH]: tableSearchHandler,
    [SHOW_DROPDOWN]: dropdownHandler
});

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
        const type = get(urlInfo,['searchObject', ACTION]);
        if (type) {
            const handler = getCustomHistoryHandlers()[type] || DEF_HANDLER;
            return handler.urlToAction(type,urlInfo);
        }
    }
    return undefined;
}

export function recordHistory(action={}) {
    if (get(window, 'firefly.ignoreHistory', false) || isHistoryEvent) return;

    let handler = getCustomHistoryHandlers()[action.type];
    if (!handler) {
        if (logHistory(action, false)) {
            handler = DEF_HANDLER;
        }
    }

    if (get(handler, 'actionToUrl')) {
        const url = handler.actionToUrl(action);
        if (url) {
            try {
                history.pushState(pick(action, ['type', 'payload']), url, url);
            } catch(e) {}
        }
    }
}


