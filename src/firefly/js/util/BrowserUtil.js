/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Created by roby on 4/3/15.
 */
import {once} from 'lodash';
import {getProp} from './WebUtil.js';

const getScriptURL = once(() => {
    const SCRIPT_NAME = getProp('SCRIPT_NAME');
    return [...document.getElementsByTagName('script')]
        .filter( (s) => SCRIPT_NAME
            .some( (name) =>  s.src.indexOf(name)>-1 ))[0]?.src;
});


export const getRootURL = once(() => {
    if (getProp('SCRIPT_NAME')=== undefined) return '//localhost:8080/';
    const workingURL= getScriptURL() || window.location.href;
    return workingURL.substring(0,workingURL.lastIndexOf('/')) + '/';
});

/**
 * given a partial url and a rootPath, create a full url, if not rootPath the use document.URL for the rootpath
 * @param {string} url
 * @param {string} rootPath
 * @return {string}
 */
export function modifyURLToFull(url, rootPath) {
    const docUrl = document.URL;
    const lastSlash = docUrl.lastIndexOf('/');
    if (!url && !rootPath) return (lastSlash===docUrl.indexOf('//')+1) ? docUrl : docUrl.substring(0, lastSlash + 1);
    if (!url) return rootPath;
    if (isFull(url)) return url;
    if (rootPath) return rootPath.endsWith('/') ? rootPath + url : rootPath + '/' + url;
    if (lastSlash===docUrl.indexOf('//')+1) return docUrl + '/' + url;
    return docUrl.substring(0, lastSlash + 1) + url;
}

function isFull(url) {
    const hPref= ['http', 'https', '/', 'file'];
    url = url.toLowerCase();
    return hPref.some( (s) => url.startsWith(s));
}
