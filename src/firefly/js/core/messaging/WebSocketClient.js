/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, pickBy} from 'lodash';
import {setCookie, parseUrl} from '../../util/WebUtil.js';
import {getRootURL} from '../../util/BrowserUtil.js';
import {dispatchUpdateAppData} from '../AppDataCntlr.js';

export const CH_ID = 'channelID';

var channel;
var connId;
var nRetries = 0;
var pinger;
var connectBaseUrl;
var listenters = [];
var wsConn;

const wsClient = {send: wsSend, addListener};

export function wsConnect(baseUrl=getRootURL()) {
    baseUrl = baseUrl.replace('https:', 'wss:').replace('http:', 'ws:');
    connectBaseUrl = baseUrl;

    const urlInfo = parseUrl(document.location);
    var wsch = get(urlInfo,'searchObject.channelID');
    wsch = get(urlInfo,'pathAry.0.wsch') || wsch;
    wsch = wsch ? `?${CH_ID}=${wsch}` : '';

    const connUrl = `${baseUrl}/sticky/firefly/events${wsch}`;
    console.log('Connecting to ' + connUrl);

    wsConn = new WebSocket(connUrl);
    wsConn.onopen = onOpen;
    wsConn.onerror = onError;
    wsConn.onclose = onClose;
    wsConn.onmessage = onMessage;
    
    return wsClient;
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

/**
 * add an event listener to this websocket client.
 * @param {Object} obj
 * @param {Function} obj.matches - matches a function that takes an event({name, data}) as its parameter.  return true if this
 *                    listener will handle the event.
 * @param {Function} obj.onEvent - a function that takes an event({name, data}) as its parameter.  this function is called
 *                    whenever an event matches its listener.
 */
function addListener( {matches, onEvent} ) {
    listenters.push({matches, onEvent});
}

function onOpen() {
    console.log('WS open: WebSocketClient started');

    if (pinger) clearInterval(pinger);
    pinger = setInterval(() => wsSend({}), 5000);
}

function onError(event) {
    if (nRetries < 5) {
        nRetries++;
        setTimeout( () => wsConnect(connectBaseUrl), 1000 );
        console.log('WS error: initiating retry ' + nRetries);
    } else {
        console.log(`WS error: after ${nRetries} retries.. ${event}`);
    }
}

function onClose(event) {
    console.log('WS closed: ' + JSON.stringify(event));
}

function onMessage(event) {
    const eventData = event.data && JSON.parse(event.data);
    if (eventData) {
        // console.log('ws message: ' + JSON.stringify(eventData));
        if (eventData.name === 'EVT_CONN_EST') {
            // connection established.. doing handshake.
            [connId, channel] = [eventData.data.connID, eventData.data.channel];
            setCookie('seinfo', `${connId}_${channel}`);
            dispatchUpdateAppData({channel});
        } else {
            listenters.forEach( (l) => {
                if (!l.matches || l.matches(eventData)) {
                    l.onEvent(eventData);
                }
            });
        }
    }

}

/**
 * @returns {string}  the channel websocket is connected to.
 */
export function getWsChannel() { return channel; }

/**
 * @returns {string}  the connection ID websocket is connected to.
 */
export function getWsConnId() { return connId; }
