/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, pickBy} from 'lodash';
import {parseUrl, isDebug} from '../../util/WebUtil.js';
import {WSCH} from '../History.js';
import {getRootURL} from '../../util/BrowserUtil.js';
import {dispatchUpdateAppData} from '../AppDataCntlr.js';

export const CH_ID = 'channelID';

var wsConn;
var pinger;
var intervalId;

var wsClient;

/**
 * WebSocket client.
 * @typedef {object} WsClient
 * @prop {function} send        function to send a message to the server.  see wsSend for parameters
 * @prop {function} addListener add a message listener.  This function will be called on all incoming message 'matches'
 *                              returns true.  see addListener for details.
 * @prop {object} listeners     a list of all the listeners.
 * @prop {string} channel       the channel websocket is connected to.
 * @prop {string} connId        the connection ID websocket is connected to.
 */

/**
 * @returns {WsClient}  return the active websocket client used.
 */
export function getWsClient() { return wsClient; }

/**
 * @returns {string}  the channel websocket is connected to.
 */
export function getWsChannel() { return wsClient && wsClient.channel; }

/**
 * @returns {string}  the connection ID websocket is connected to.
 */
export function getWsConnId() { return wsClient && wsClient.connId; }

export function wsConnect(callback, baseUrl=getRootURL()) {
    baseUrl = baseUrl.replace('https:', 'wss:').replace('http:', 'ws:');

    const urlInfo = parseUrl(document.location);
    let wsch = get(urlInfo,['searchObject', WSCH], ''); // get channel from url

    if (!wsch && get(window,'firefly.wsch')) { // if not defined, try window.firefly
        wsch= window.firefly.wsch;
    }
    const wschParam = wsch ? `?${CH_ID}=${wsch}` : '';
    const wsUrl = `${baseUrl}/sticky/firefly/events${wschParam}`;
    console.log('Connecting to ' + wsUrl);
    makeConnection(wsUrl);
    pinger = makePinger(callback, wsUrl);
}

function makeConnection(wsUrl) {
    get(wsConn, 'readyState') === WebSocket.OPEN && wsConn.close();

    wsConn = new WebSocket(wsUrl);
    wsConn.onopen = onOpen;
    wsConn.onerror = onError;
    wsConn.onclose = onClose;
    wsConn.onmessage = onMessage;
}

/**
 * @param {Object} p
 * @param p.name  - Event's name.  See edu.caltech.ipac.firefly.util.event.Name for a full list.
 * @param p.scope - One of 'SELF', 'CHANNEL', 'SERVER'.
 * @param p.dataType - One of 'JSON', 'BG_STATUS', 'STRING'.
 * @param p.data - String.
 */
function wsSend({name='ping', scope, dataType, data}) {

    if (name === 'ping') {
        wsConn.send('');
    } else {
        const msg = JSON.stringify( pickBy({name, scope, dataType, data}));
        wsConn.send(msg);
    }
}


function onOpen() {}

function onError(event) {
    pinger && pinger.onError(event);
}

function onClose(e) {
    console.log('WebSocket is closed: ' + JSON.stringify(e));
}

function onMessage(event) {
    const eventData = event.data && JSON.parse(event.data);

    if (eventData) {
        // console.log('ws message: ' + JSON.stringify(eventData));
        if (eventData.name === 'EVT_CONN_EST') {
            // connection established.. doing handshake.
            const [connId, channel] = [eventData.data.connID, eventData.data.channel];
            pinger && pinger.onConnected(channel, connId);
        } else {
            const {listeners=[]} = wsClient || {};
            listeners.forEach( (l) => {
                if (!l.matches || l.matches(eventData)) {
                    l.onEvent(eventData);
                }
            });
        }
    }

}

function makeWsClient(channel, connId) {
    const listeners = [];

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

    return {send: wsSend, addListener, listeners, channel, connId};
}

function makePinger(onConnectCallback, wsUrl) {

    intervalId && clearInterval(intervalId);

    const check = (from='ping') => {

        if (wsConn.readyState === WebSocket.OPEN) {
            wsSend({});
        } else {
            dispatchUpdateAppData({websocket: {isConnected: false}});
            makeConnection(wsUrl);
        }

        if (isDebug()) {
            console.log(`ping initiated from ${from} on: ${Date()}`);
        }
    };

    const onConnected = (channel, connId) => {
        console.log(`WebSocket connected.  connId:${connId} channel:${channel}`);
        wsClient = makeWsClient(channel, connId);
        dispatchUpdateAppData({websocket: {isConnected: true, channel, connId}});
        onConnectCallback && onConnectCallback(wsClient);
    };

    const onError = () => {
        dispatchUpdateAppData({websocket: {isConnected: false}});
    };

    intervalId = setInterval(check, 10*1000);   // check every n * seconds
    window.addEventListener('online',  () => check('online'));
    window.addEventListener('offline', () => check('offline'));

    return {onError, check, onConnected};

}