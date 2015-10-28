/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Date: Mar 13, 2009
 * @author loi
 */
/* eslint prefer-template:0 */

import Enum from 'enum';
import { getRootURL } from './BrowserUtil.js';

const ParamType= new Enum(['POUND', 'QUESTION_MARK']);

const saveAsIpacUrl = getRootURL() + 'servlet/SaveAsIpacTable';


/**
 * Returns a string where all characters that are not valid for a complete URL have been escaped.
 * Also, it will do URL rewriting for session tracking if necessary.
 * Fires SESSION_MISMATCH if the seesion ID on the client is different from the one on the server.
 *
 * @param url    this could be a full or partial url.  Delimiter characters will be preserved.
 * @param paramType  if the the parameters are for the server use QUESTION_MARK if the client use POUND
 * @param {array|Object} params parameters to be appended to the url.  These parameters may contain
 *               delimiter characters.  Unlike url, delimiter characters will be encoded as well.
 * @return {string} encoded url
 */
export const encodeUrl= function(url, paramType, params) {
    var paramChar= paramType===ParamType.QUESTION_MARK ? '?': '#';
    var parts = url.split('\\'+paramChar, 2);
    var baseUrl = parts[0];
    var queryStr = encodeURI(parts.length===2 ? parts[1] : '');

    var paramAry= params || [];

    if (params.length===1 && Object.keys(params[0]).length>1) {
        paramAry= Object.keys(params[0]).reduce( (ary,key) => {
            ary.push({name:key, value : params[0][key]});
            return ary;
        },[]);
    }

    queryStr= paramAry.reduce((str,param,idx) => {
        if (param && param.name) {
            var key = encodeURI(param.name.trim());
            var val = param.value ? encodeURIComponent(param.value.trim()) : '';
            str += val.length ? key + '=' + val + (idx < paramAry.length ? '&' : '') : key;
            return str;
        }
    },'');

    return encodeURI(baseUrl) + (queryStr.length ? paramChar + queryStr : '');
};


/**
 * Returns a string where all characters that are not valid for a complete URL have been escaped.
 * Also, it will do URL rewriting for session tracking if necessary.
 * Fires SESSION_MISMATCH if the seesion ID on the client is different from the one on the server.
 *
 * @param url    this could be a full or partial url.  Delimiter characters will be preserved.
 * @param params parameters to be appended to the url.  These parameters may contain
 *               delimiter characters.  Unlike url, delimiter characters will be encoded as well.
 * @return encoded url
 */
export const encodeServerUrl= function(url, params) {
    return encodeUrl(url, ParamType.QUESTION_MARK,params);
};




/**
 *
 * @param {ServerRequest} request
 * @return {string} encoded
 */
export const getTableSourceUrl= function(request) {
    request.setStartIndex(0);
    request.setPageSize(Number.MAX_SAFE_INTEGER);
    var source = { name : 'request', value : request.toString()};  //todo : i don't think I got this line right
    var filename = request.getParam('file_name');
    if (!filename) filename = request.getRequestId();
    var fn = { name: 'file_name', value : filename};
    return encodeServerUrl(saveAsIpacUrl, source, fn);
};


