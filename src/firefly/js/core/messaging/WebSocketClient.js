/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, pickBy} from 'lodash';
import {parseUrl} from '../../util/WebUtil.js';
import {Logger} from '../../util/Logger.js';
import {WSCH} from '../History.js';
import {getRootURL} from '../../util/BrowserUtil.js';
import {getAppOptions} from '../AppDataCntlr.js';
import {showLostConnection, hideLostConnection} from '../../ui/LostConnection.jsx';

export const CH_ID = 'channelID';

/**
 * A proxy to the underlining WebSocket connection
 * @typedef {object} wsClient
 * @prop {function} send        function to send a message to the server.  see wsSend for parameters
 * @prop {function} addListener add a message listener.  This function will be called on all incoming message 'matches'
 *                              returns true.  see addListener for details.
 * @prop {object}   listeners   a list of all the listeners.
 * @prop {string}   channel     the channel websocket is connected to.
 * @prop {string}   connId      the connection ID websocket is connected to.
 * @prop {function} isConnected return true if connection active.
 */


const conns = {};       // a map of active connections keyed by baseUrl
const logger = Logger('WebSocket');

/**
 * returns a wsClient for the given baseUrl
 * @param baseUrl   the base URL to connect to
 * @return {Promise<wsClient>}
 */
export function getWsConn(baseUrl=getRootURL()) {
    return conns[baseUrl] || {isConnected: ()=>false, isConnecting: false};
}

/**
 * returns a wsClient for the given baseUrl, create if one does not exists
 * @param baseUrl   the base URL to connect to
 * @return {Promise<wsClient>}
 */
export function getOrCreateWsConn(baseUrl=getRootURL()) {
    const wsProxy = conns[baseUrl];
    if (wsProxy) {
        if (wsProxy.isConnected()) {
            logger.debug(`getOrCreateWsConn -> existing connection: ${baseUrl} :: ${JSON.stringify(wsProxy)}`);
            return Promise.resolve(wsProxy);
        } else if (wsProxy.isConnecting) {
            logger.debug('getOrCreateWsConn -> isConnecting ' + baseUrl);
            return wsProxy.promise;
        }
    }
    const p = new Promise( (resolve, reject) => {
        const mResolve = (proxy) => {
            conns[baseUrl] = proxy;
            hideLostConnection();
            resolve?.(proxy);
        };
        const mReject = (e) => {
            Reflect.deleteProperty(conns, baseUrl);
            reject?.(e);
        };
        logger.debug('getOrCreateWsConn -> create new connection ' + baseUrl);
        makeWsConn(baseUrl, mResolve, mReject);
    });
    conns[baseUrl] = {isConnecting: true, isConnected: ()=>false, promise: p};
    return p;
}

const listeners = [];

function makeWsConn(baseUrl, resolve, reject) {

    baseUrl = baseUrl.replace('https:', 'wss:').replace('http:', 'ws:');

    const urlInfo = parseUrl(document.location);
    const wsch = urlInfo.searchObject?.[WSCH] || window.firefly?.wsch;
    const wschParam = wsch ? `?${CH_ID}=${wsch}` : '';
    const wsUrl = `${baseUrl}sticky/firefly/events${wschParam}`;

    const requireWs = getAppOptions()?.RequireWebSocketUptime ?? !!wsch;        // if flag is not set, defaults to true when wsch is given.

    let pingerId, connectWhenOnline;
    const wsConn = new WebSocket(wsUrl);
    logger.info('connecting to ' + wsUrl);

    /**
     * @param {Object} p
     * @param p.name  - Event's name.  See edu.caltech.ipac.firefly.util.event.Name for a full list.
     * @param p.scope - One of 'SELF', 'CHANNEL', 'SERVER'.
     * @param p.dataType - One of 'JSON', 'BG_STATUS', 'STRING'.
     * @param p.data - String.
     */
    const send = ({name='ping', scope, dataType, data}) => {
        if (name === 'ping') {
            wsConn.send('');
        } else {
            const msg = JSON.stringify( pickBy({name, scope, dataType, data}));
            wsConn.send(msg);
        }
    };

    /**
     * add an event listener to this websocket client.
     * @param {Object} obj
     * @param {Function} obj.matches - matches a function that takes an event({name, data}) as its parameter.  return true if this
     *                    listener will handle the event.
     * @param {Function} obj.onEvent - a function that takes an event({name, data}) as its parameter.  this function is called
     *                    whenever an event matches its listener.
     */
    const addListener = ( {matches, onEvent} ) => {
        listeners.push({matches, onEvent});
    };

    /**
     * mplementation of requireWs feature.
     * - ping every minute to keep connection from being closed by keep-alive settings.
     * - if offline, try to reconnect when back online.
     */
    const requireWsImpl = () => {
        connectWhenOnline && window.removeEventListener('offline', connectWhenOnline);
        connectWhenOnline = () => {
            logger.debug('offline detected -> adding connectWhenOnline');
            window.addEventListener('online', () => {
                window.removeEventListener('online', connectWhenOnline);
                logger.debug('online detected -> attempting to re-connect');
                getOrCreateWsConn().catch(() => showLostConnection());
            });
        };
        window.addEventListener('offline', connectWhenOnline);
        pingerId = setInterval(() => {
            send({name: 'ping'});
            logger.debug('keep-alive ping sent');
        }, 30*1000);   // check every n * seconds
    };

    const isConnected = () => wsConn.readyState === 1;      // 1 == OPEN

    const onMessage = (event) => {
        const eventData = event.data && JSON.parse(event.data);
        if (eventData) {
            logger.tag('onMessage').debug(eventData);
            if (eventData.name === 'EVT_CONN_EST') {
                // connection established.. doing handshake.
                const {connID:connId, channel}  = eventData.data;
                resolve({connId, channel, isConnected, send, addListener});

                logger.info(`connected as (${connId} - ${channel})`);
                if (requireWs) {
                    requireWsImpl();
                }
            } else {
                listeners.forEach( (l) => {
                    if (!l.matches || l.matches(eventData)) {
                        l.onEvent(eventData);
                    }
                });
            }
        }
    };
    const onClose = (msg) => {
        pingerId && clearInterval(pingerId);
        logger.warn(msg);
        const _handler = () => {
            logger.info('window focus detected, attempting to reconnect ');
            window.removeEventListener('focus', _handler);
            getOrCreateWsConn().catch(() => {} /* ignore */);
        };
        window.addEventListener('focus', _handler);
        if (requireWs && window.navigator.onLine) {
            showLostConnection();
        }
    };

    wsConn.onmessage = onMessage;
    wsConn.onopen = () => {};
    wsConn.onerror = (e) => {
        onClose('WebSocket onerror: ' + JSON.stringify(e));
        reject?.(e);
    };
    wsConn.onclose = (r) => {
        onClose('WebSocket is closed: ' + JSON.stringify(r));
    };
}
