/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import Enum from 'enum';
import {getGlobalObj} from 'firefly/util/WebUtil.js';

/**
 * @typedef {Object} Browser
 * enum can be one of
 * @prop FIREFOX
 * @prop SEAMONKEY
 * @prop SAFARI
 * @prop WEBKIT_GENERIC
 * @prop IE
 * @prop OPERA
 * @prop CHROME
 * @prop UNKNOWN
 * @type {Enum}
 */


/** @type Browser */
export const Browser = new Enum ([
    'FIREFOX', 'SEAMONKEY', 'SAFARI', 'WEBKIT_GENERIC', 'IE', 'OPERA', 'CHROME', 'UNKNOWN'
]);

/**
 * @typedef {Object} Platform
 * enum can be one of
 * @prop MAC
 * @prop WINDOWS
 * @prop LINUX
 * @prop SOLARIS
 * @prop SUNOS
 * @prop HPUX
 * @prop AIX
 * @prop IPHONE
 * @prop IPAD
 * @prop ANDROID
 * @prop FREE_BSD
 * @prop SYMBIAN_OS
 * @prop J2ME
 * @prop BLACKBERRY
 * @prop UNKNOWN
 * @type {Enum}
 */

/** @type Platform */
export const Platform = new Enum([
    'MAC', 'WINDOWS', 'LINUX', 'SOLARIS', 'SUNOS', 'HPUX', 'AIX', 'IPHONE', 'IPAD', 'ANDROID', 'FREE_BSD',
    'SYMBIAN_OS', 'J2ME', 'BLACKBERRY', 'UNKNOWN'
]);

const UNKNOWN_VER= -1;

const BrowserInfo= {
    getBrowserType: () => BrowserInfo.browser,
    isIE: () => isBrowser(Browser.IE),
    isSafari: () => isBrowser(Browser.SAFARI),
    isFirefox: () => isBrowser(Browser.FIREFOX),
    isChrome: () => isBrowser(Browser.CHROME),
    isBrowser,
    isPlatform: (inPlatform) => inPlatform===BrowserInfo.platform,
    isVersionAtLeast,
    isVersionBefore: (version, minor=UNKNOWN_VER) => !isVersionAtLeast(version,minor),
    getPlatformDesc: () => BrowserInfo.platform.key,
    getBrowserString: () => BrowserInfo.browser.key,
    isTouchInput: () => ([Platform.IPAD,Platform.IPHONE,Platform.ANDROID].includes(BrowserInfo.platform)),
    getBrowserDesc: () => BrowserInfo.browserDesc,
    supportsCssColorMix,
    getVersionString: () =>
        BrowserInfo.minorVersion!==UNKNOWN_VER ? `${BrowserInfo.majorVersion}.${BrowserInfo.minorVersion}` : BrowserInfo.majorVersion+'',
    minorVersion: undefined,
    majorVersion: undefined,
    browser: undefined,
};

export default BrowserInfo;


(() => {
    BrowserInfo.userAgent= getGlobalObj()?.navigator?.userAgent?.toLowerCase();
    BrowserInfo.platform= evaluatePlatform(BrowserInfo.userAgent);
    Object.assign(BrowserInfo,evaluateBrowser(BrowserInfo.userAgent,BrowserInfo.platform));
    BrowserInfo.browserDesc= BrowserInfo.minorVersion!==UNKNOWN_VER ?
            BrowserInfo.getBrowserString()+ ' Version: ' + BrowserInfo.majorVersion + '.' +BrowserInfo.minorVersion :
            BrowserInfo.getBrowserString()+ ' Version: ' + BrowserInfo.majorVersion;
})();

function isBrowser(testBrowsers, version=UNKNOWN_VER ) {
    return testBrowsers===BrowserInfo.browser && (version===UNKNOWN_VER || version===BrowserInfo.majorVersion);
}

function isVersionAtLeast(version, minor= UNKNOWN_VER) {
    if (version===UNKNOWN_VER) return false;
    if (BrowserInfo.majorVersion>version) return true;
    if (BrowserInfo.majorVersion===version) {
        if (BrowserInfo.minorVersion===UNKNOWN_VER) return true;
        if (BrowserInfo.minorVersion!==UNKNOWN_VER && BrowserInfo.minorVersion>=minor) return true;
    }
    return false;
}

function evaluateBrowser(ua,p) {
    const SEAMONKEY_KEY= 'seamonkey/';
    const FIREFOX_KEY= 'firefox/';
    const SIMPLE_VERSION_KEY = 'version/';
    const CHROME_KEY= 'chrome/';
    const IE_11_KEY= 'rv:';
    let retValue= {browser:Browser.UNKNOWN,majorVersion:UNKNOWN_VER, minorVersion:UNKNOWN_VER};
    if (!ua) return retValue;
    if (ua.includes('msie')) {
        retValue.browser= Browser.IE;
        if (ua.includes('msie 11') && !ua.includes('trident')) retValue.majorVersion= 11;
        else if (ua.includes('msie 10')) retValue.majorVersion= 10;
        else if (ua.includes('msie 9')) retValue.majorVersion= 9;
        else if (ua.includes('msie 8')) retValue.majorVersion= 8;
        else if (ua.includes('msie 7')) retValue.majorVersion= 7;
        else if (ua.includes('msie 6')) retValue.majorVersion= 6;
    }
    else if (ua.includes('trident') && !ua.includes('edge')) {
        retValue= parseVersion(ua,IE_11_KEY);
        retValue.browser= Browser.IE;
    }
    else if (ua.includes('edge')) {
        retValue= parseVersion(ua,'edge/');
        retValue.browser= Browser.IE;
    }
    else if (ua.includes(FIREFOX_KEY)) {
        retValue= parseVersion(ua,FIREFOX_KEY);
        retValue.browser= Browser.FIREFOX;
    }
    else if (ua.includes(SEAMONKEY_KEY)) {
        retValue= parseVersion(ua,SEAMONKEY_KEY);
        retValue.browser= Browser.SEAMONKEY;
    }
    else if  (ua.includes('applewebkit')) {
        if (ua.includes(CHROME_KEY)) {
            retValue= parseVersion(ua,CHROME_KEY);
            retValue.browser= Browser.CHROME;
        }
        else if (p===Platform.BLACKBERRY || p===Platform.SYMBIAN_OS || p===Platform.UNKNOWN) {
            retValue= parseVersion(ua,SIMPLE_VERSION_KEY);
            retValue.browser= Browser.WEBKIT_GENERIC;
        }
        else {
            retValue= parseVersion(ua,SIMPLE_VERSION_KEY);
            retValue.browser= Browser.SAFARI;
        }
    }
    else if  (ua.includes('opera')) {
        if (ua.includes('opera/9')) retValue.majorVersion= 9;
        else if (ua.includes('opera/8')) retValue.majorVersion= 8;
        else if (ua.includes('opera/7')) retValue.majorVersion= 7;
        else if (ua.includes('opera/6')) retValue.majorVersion= 6;
        retValue.browser= Browser.OPERA;
    }
    return retValue;
}

/**
 * @param ua user agent string
 * @param key
 * @return {{majorVersion: number, minorVersion: number}}
 */
function parseVersion(ua,key) {
    let idx= ua.indexOf(key);
    if (idx!==-1) idx+= key.length;
    const ver= ua.substring(idx).match(/[0-9.]+/)?.[0];
    const sAry= ver?.split('\.') ?? [];
    return {
        majorVersion: !isNaN(Number(sAry[0])) ? parseInt(sAry[0]) : UNKNOWN_VER,
        minorVersion: !isNaN(Number(sAry[1])) ? parseInt(sAry[1]) : UNKNOWN_VER
    };
}

function evaluatePlatform(ua) {
    if (!ua) return Platform.UNKNOWN;
    if  (ua.includes('window')) return Platform.WINDOWS;
    else if  (ua.includes('macintosh')) return Platform.MAC;
    else if  (ua.includes('ipad')) return Platform.IPAD;
    else if  (ua.includes('iphone')) return Platform.IPHONE;
    else if  (ua.includes('linux')) return (ua.includes('android')) ? Platform.ANDROID : Platform.LINUX;
    else if  (ua.includes('solaris')) return Platform.SOLARIS;
    else if  (ua.includes('sunos')) return Platform.SUNOS;
    else if  (ua.includes('aix')) return Platform.AIX;
    else if  (ua.includes('hpux')) return Platform.HPUX;
    else if  (ua.includes('freebsd')) return Platform.FREE_BSD;
    else if  (ua.includes('symbianos')) return Platform.SYMBIAN_OS;
    else if  (ua.includes('j2me')) return Platform.J2ME;
    else if  (ua.includes('blackberry')) return Platform.BLACKBERRY;
    else return Platform.UNKNOWN;
}


function supportsCssColorMix() {
    if (isBrowser(Browser.CHROME) && isVersionAtLeast(111)) return true;
    if (isBrowser(Browser.SAFARI) && isVersionAtLeast(16,2)) return true;
    if (isBrowser(Browser.FIREFOX) && isVersionAtLeast(121)) return true;
    if (isBrowser(Browser.IE) && isVersionAtLeast(111)) return true;
    return false;
}