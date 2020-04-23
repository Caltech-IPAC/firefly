/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {memoize, find} from 'lodash';
import {sprintf} from '../externalSource/sprintf.js';

/*------------------------------- Logger Implementation ----------------------------------

-- A typical use case:

    import {Logger} from './WebUtils.js'
    const logger = Logger('a-tag');

    logger.info('a-message')
    logger.trace('how did i get here?')

    // or multiple messages including objects
    logger.debug('on dispatch', inputParams, payload);


-- If you don't want to tag your messages, simply import logger directly:
    import {logger} from './WebUtils.js'

    logger.info('useful info')
    logger.tag('something').error('unexpected error', error);   // adding a tag for this one message.  a bit wasteful, since a new Logger is created for this one message.


Don't forget, you want to do as little as possible when debug is not enabled.
Sometimes when it is costly to create the message, you can use isDebug to check before executing it, i.e.

    logger.isDebug() && logger.debug( something_costly() )


-- How to turn on debugging

set window.firefly.debug to one of the predefined levels: error, warn, info, debug, trace.
They are in descending order.  For example, setting debug='warn' will only output warnings and above.


-- How to filter messages

window.firefly.debugTags
There could be an overwhelming amount of logs depending on what level it is set at.
To filter or control what logs get displayed, use window.firefly.debugTags.
This is a comma-separated list of pattern.  Any logs in which its tag matches one of the patterns
will be displayed.
For example, debugTags='Web,Table' will show any log with tag containing the word 'Web' or 'Table'.
It can also be a regex, like '^Web,Table$'.  In this case, only log with tag that starts with 'Web' or ends with 'Table'.

--------------------------------------------------------------------------------------------*/



/**
 * A proxy to the underlining WebSocket connection
 * @typedef {object} Logger
 * @prop {function} error   log in red with error icon
 * @prop {function} warn    log in yellow with warn icon
 * @prop {function} info    log in black
 * @prop {function} log     same as info
 * @prop {function} debug   log black. More verbose, meant for debugging
 * @prop {function} trace   Very verbose.  Print message plus a full stack trace
 * @prop {function} tag     returns a new Logger with the given tag appended to the current one.
 *                          tag is used for filtering.
 * @prop {function} isDebug return true if debugging is enabled.  this is a lightweight check useful
 *                          for avoiding execution of wasteful debugging logic
 *                          i.e.   logger.isDebug() && logger.debug(do-lots-of-work());
 */


/**
 * returns a Logger with the given tag
 * @param tag   a tag associate with this logger.
 * @returns {Logger}
 */
export function Logger(tag) {
    const logger = (level) => (...msg) => {
        msg.forEach((m) => log(m, {level, tag}));
    };
    return {
        isDebug: () => debugLevel() >= 0,
        tag: (t) => Logger(tag ? `${tag}-${t}` : t),
        error: logger(0),
        warn:  logger(1),
        info:  logger(2),
        log:   logger(2),  // same as info
        debug: logger(3),
        trace: logger(4)   // this will print a full stack trace
    };
}

/**
 * A generic Logger without a tag.
 * @type {Logger}
 */
export const logger = Logger();




const debugLevel = memoize( () => {
    const levels = ['error', 'warn', 'info', 'debug', 'trace'];
    let debug = window.firefly?.debug;
    if (!debug && debugTags().length > 0) debug = 'debug';
    if (debug) {
        let level =  levels.indexOf(debug);
        level = level >= 0 ? level : 2;
        if (level >= 0) console.log('debugLevel set at: ' + levels[level]);
        return level;
    }
    return -1;
}, () => window.firefly?.debug + '|' + window.firefly?.debugTags + '');



const debugTags = memoize( () => {
    const tags = window.firefly?.debugTags ?? '';
    if (tags) console.log('debugTags set at: ' + tags);
    return tags.split(',').map((s) => s.trim()).filter((t) => t);
}, () => window.firefly?.debugTags + '');

function log(msg, {level=2, tag=''}) {
    if (debugLevel() >=  level) {
        const doLog = debugTags().length === 0 || find(debugTags(), (t) => !!tag.match(t));
        if (doLog) {
            const now = new Date();
            const ts = '%c' + sprintf('%2s:%2d:%2d.%3d ', now.getHours(), now.getMinutes(), now.getSeconds(), now.getMilliseconds());

            tag = tag ? tag + ':' : ':';
            if (typeof msg === 'object') {
                console.log(ts, 'color:maroon', tag, '==>');
            }

            if (level === 0) console.error(ts, 'color:maroon', tag, msg);
            if (level === 1) console.warn(ts, 'color:maroon', tag, msg);
            if (level === 2) console.info(ts, 'color:maroon', tag, msg);
            if (level === 3) console.debug(ts, 'color:maroon', tag, msg);
            if (level === 4) console.trace(ts, 'color:maroon', tag, msg);
        }
    }
};

