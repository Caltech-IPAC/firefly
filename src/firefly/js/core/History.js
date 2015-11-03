/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import set from 'lodash/object/set';
import get from 'lodash/object/get';

var HISTORY_PATH = 'history';
const MAX_HISTORY_LENGTH = 20;


function getCurrent(state=flux.getState()) {
    var list = getList(state);
    if (list.length > 0) {
        return list.get(list.length-1);
    } else {
        return null;
    }
}


function getList(state={}) {
    var list = get(state, HISTORY_PATH);
    if (!list) {
        list = [];
        set (state, HISTORY_PATH, list);
    }
    return list;
}

function add(state={}, action=null, title=null, url=null) {
    if (action) {
        var list = getList(state);
        list.push({'type': action.type, 'payload': action.payload, title, url});
        if (list.length > MAX_HISTORY_LENGTH) {
            list.shift();
        }
    }
}


const history = {
    HISTORY_PATH,
    getList,
    getCurrent,
    add
};

export default history;
