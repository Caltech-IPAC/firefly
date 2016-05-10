/**
 * Created by loi on 1/19/16.
 */
import {setCookie} from '../../util/WebUtil.js';
import {getRootURL} from '../../util/BrowserUtil.js';

var nRetries = 0;
var pinger;
var connectBaseUrl;
var listenters = [];
var wsConn;

const wsClient = {send: wsSend, addListener};

export function wsConnect(baseUrl=getRootURL()) {
    baseUrl = baseUrl.replace('https:', 'wss:').replace('http:', 'ws:');
    connectBaseUrl = baseUrl;
    console.log('Connecting to ' + baseUrl + '/sticky/firefly/events');

    wsConn = new WebSocket(baseUrl + '/sticky/firefly/events');
    wsConn.onopen = onOpen;
    wsConn.onerror = onError;
    wsConn.onclose = onClose;
    wsConn.onmessage = onMessage;
    
    return wsClient;
}

/**
 *
 * @param name  Event's name.  See edu.caltech.ipac.firefly.util.event.Name for a full list.
 * @param scope One of 'SELF', 'CHANNEL', 'SERVER'.
 * @param dataType One of 'JSON', 'BG_STATUS', 'STRING'.
 * @param data String.
 */
function wsSend({name='ping', scope, dataType, data}) {
    if (name === 'ping') {
        wsConn.send('');
    } else {
        const msg = JSON.stringify(arguments[0]);
        wsConn.send(msg);
    }

}

/**
 * add an event listener to this websocket client.
 * @param matches a function that takes an event({name, data}) as its parameter.  return true if this
 *                listener will handle the event.
 * @param onEvent a function that takes an event({name, data}) as its parameter.  this function is called
 *                whenever an event matches its listener.
 */
function addListener( {matches, onEvent} ) {
    listenters.push(arguments[0]);
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
            const sEventInfo = eventData.data.connID + '_' + eventData.data.channel;
            setCookie('seinfo', sEventInfo);
        } else {
            listenters.forEach( (l) => {
                if (!l.matches || l.matches(eventData)) {
                    l.onEvent(eventData);
                }
            });
        }
    }

}


