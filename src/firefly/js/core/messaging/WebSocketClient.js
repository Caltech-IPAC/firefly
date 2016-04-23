/**
 * Created by loi on 1/19/16.
 */

var nRetries = 0;
var pinger;
var connectBaseUrl;
var listenters = [];


export function wsConnect(baseUrl) {
    if (!window.firefly) {
        window.firefly = {};
    }
    connectBaseUrl = baseUrl;

    var l = window.location;
    if (baseUrl == null) {
        var proto = (l.protocol === 'https:') ? 'wss://' : 'ws://';
        var port = (l.port != 80 && l.port != 443) ? ':' + l.port : '';
        var pathname = l.pathname.substring(0, l.pathname.lastIndexOf('/'));
        baseUrl = proto + l.hostname + port + '/' + pathname;
    } else {
        baseUrl = baseUrl.replace('https:', 'wss:').replace('http:', 'ws:');
    }
    var queryString = l.hash ? '?' + decodeURIComponent(l.hash.substring(1)) : '';

    console.log('Connecting to ' + baseUrl + '/sticky/firefly/events' + queryString);

    var wsClient = new WebSocket(baseUrl + '/sticky/firefly/events' + queryString);
    wsClient.onopen = onOpen;
    wsClient.onerror = onError;
    wsClient.onclose = onClose;
    wsClient.onmessage = onMessage;

    window.firefly.WebSocketClient = wsClient;
}

/**
 *
 * @param name  Event's name.  See edu.caltech.ipac.firefly.util.event.Name for a full list.
 * @param scope One of 'SELF', 'CHANNEL', 'SERVER'.
 * @param dataType One of 'JSON', 'BG_STATUS', 'STRING'.
 * @param data String.
 */
export function wsSend({name='ping', scope, dataType, data}) {
    if (name === 'ping') {
        window.firefly.WebSocketClient.send('');
    } else {
        const msg = JSON.stringify(arguments[0]);
        window.firefly.WebSocketClient.send(msg);
    }

}

/**
 * add an event listener to this websocket client.
 * @param matches a function that takes an event({name, data}) as its parameter.  return true if this
 *                listener will handle the event.
 * @param onEvent a function that takes an event({name, data}) as its parameter.  this function is called
 *                whenever an event matches its listener.
 */
export function addListener( {matches, onEvent} ) {
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
    // console.log('ws message: ' + JSON.stringify(eventData));

    listenters.forEach( (l) => {
       if (!l.matches || l.matches(eventData)) {
           l.onEvent(eventData);
       }
    });
}


