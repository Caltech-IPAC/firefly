/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import Enum from 'enum';
import {getProp} from 'firefly/util/WebUtil.js';

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
 * @prop EDGE
 * @prop UNKNOWN
 * @type {Enum}
 */


/** @type Browser */
export const Browser = new Enum ([
    'FIREFOX', 'SEAMONKEY', 'SAFARI', 'WEBKIT_GENERIC', 'IE', 'OPERA', 'CHROME', 'EDGE', 'UNKNOWN'
]);

/**
 * @typedef {Object} Platform
 * enum can be one of
 * @prop MACOS
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
    'MACOS', 'WINDOWS', 'LINUX', 'SOLARIS', 'SUNOS', 'HPUX', 'AIX', 'IPHONE', 'IPAD', 'ANDROID', 'FREE_BSD',
    'SYMBIAN_OS', 'J2ME', 'BLACKBERRY', 'UNKNOWN'
]);

const UNKNOWN_VER= -1;

const BrowserInfo= {
    getBrowserType: () => BrowserInfo.browser,
    isSafari: () => isBrowser(Browser.SAFARI),
    isFirefox: () => isBrowser(Browser.FIREFOX),
    isChrome: () => isBrowser(Browser.CHROME),
    isEdge: () => isBrowser(Browser.EDGE),
    isChromeLike: () => isBrowser(Browser.CHROME) || isBrowser(Browser.EDGE) || isBrowser(Browser.OPERA),
    isBrowser,
    isPlatform: (inPlatform) => inPlatform===BrowserInfo.platform,
    isVersionAtLeast,
    isVersionBefore: (version, minor=UNKNOWN_VER) => !isVersionAtLeast(version,minor),
    getPlatformDesc: () => BrowserInfo.platform.key,
    getBrowserString: () => BrowserInfo.browser.key,
    isTouchInput: () => ([Platform.IPAD,Platform.IPHONE,Platform.ANDROID].includes(BrowserInfo.platform)),
    getBrowserDesc: () => BrowserInfo.browserDesc,
    supportsCssColorMix,
    getMinSupportedVersion,
    isBrowserVersionSupported,
    supportsWebGpu,
    getVersionString: () =>
        BrowserInfo.minorVersion!==UNKNOWN_VER ? `${BrowserInfo.majorVersion}.${BrowserInfo.minorVersion}` : BrowserInfo.majorVersion+'',
    minorVersion: undefined,
    majorVersion: undefined,
    browser: undefined,
};

export default BrowserInfo;


(() => {
    const ua= globalThis?.navigator?.userAgent?.toLowerCase();
    BrowserInfo.userAgent= ua;
    BrowserInfo.platform= evaluatePlatform(ua);
    const {browser,majorVersion,minorVersion} = evaluateBrowser(ua,BrowserInfo.platform);
    BrowserInfo.majorVersion=majorVersion;
    BrowserInfo.minorVersion=minorVersion;
    BrowserInfo.browser=browser;
    BrowserInfo.browserDesc= BrowserInfo.minorVersion!==UNKNOWN_VER ?
            BrowserInfo.getBrowserString()+ ' Version: ' + BrowserInfo.majorVersion + '.' +BrowserInfo.minorVersion :
            BrowserInfo.getBrowserString()+ ' Version: ' + BrowserInfo.majorVersion;
    if (BrowserInfo.isFirefox()) {
        navigator?.gpu?.requestAdapter()
            .then( () => BrowserInfo.fireflyWebGpuEnabled= true)
            .catch(() => BrowserInfo.fireflyWebGpuEnabled= false);
    }
    BrowserInfo.platformMajorVerison= platformMajorVersion(ua,BrowserInfo.platform);
    logIfVersionNotSupported();
})();

function logIfVersionNotSupported() {
    if (isBrowserVersionSupported()) return;
    const minVer= getMinSupportedVersion();
    const bStr= BrowserInfo.getBrowserString();
    const platformStrStr= `(running on ${BrowserInfo.platform} ${BrowserInfo.platformMajorVerison})`;
    console.warn(`Browser not supported: ${BrowserInfo.browserDesc} is too old and not supported, the minimum ${bStr} version is ${minVer} ${platformStrStr}`);
}

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

function isBrowserVersionSupported() {
    return BrowserInfo.isVersionAtLeast(BrowserInfo.getMinSupportedVersion());
}

function getMinSupportedVersion() {
    if (BrowserInfo.isChrome()) return Number(getProp('MIN_CHROME_VERSION','0'));
    if (BrowserInfo.isSafari()) return Number(getProp('MIN_SAFARI_VERSION','0'));
    if (BrowserInfo.isFirefox()) return Number(getProp('MIN_FIREFOX_VERSION','0'));
    if (BrowserInfo.isEdge()) return Number(getProp('MIN_EDGE_VERSION','0'));
    return 0;
}

function evaluateBrowser(ua,p) {
    const SEAMONKEY_KEY= 'seamonkey/';
    const FIREFOX_KEY= 'firefox/';
    const SIMPLE_VERSION_KEY = 'version/';
    const CHROME_KEY= 'chrome/';
    const OPERA_KEY= 'opr/';
    const EDGE_KEY= 'edg/';
    const IE_11_KEY= 'rv:';
    const unknown= {browser:Browser.UNKNOWN,majorVersion:UNKNOWN_VER, minorVersion:UNKNOWN_VER};
    if (!ua) return unknown;
    if (ua.includes('trident') && !ua.includes('edge')) {// IE is deprecated, but I am leaving the code here for now
        return parseVersion(ua,IE_11_KEY,Browser.IE);
    }
    else if (ua.includes(FIREFOX_KEY)) {
        return parseVersion(ua,FIREFOX_KEY,Browser.FIREFOX);
    }
    else if (ua.includes(SEAMONKEY_KEY)) { // as of fall 2025, SeaMonkey is still around
        return parseVersion(ua,SEAMONKEY_KEY,Browser.SEAMONKEY);
    }
    else if  (ua.includes('applewebkit')) {
        if (ua.includes(EDGE_KEY)) {
            return parseVersion(ua,EDGE_KEY,Browser.EDGE);
        }
        else if  (ua.includes(OPERA_KEY)) {
            return parseVersion(ua,OPERA_KEY,Browser.OPERA);
        }
        if (ua.includes(CHROME_KEY)) {
            return parseVersion(ua,CHROME_KEY,Browser.CHROME);
        }
        else if (p===Platform.BLACKBERRY || p===Platform.SYMBIAN_OS || p===Platform.UNKNOWN) {
            return parseVersion(ua,SIMPLE_VERSION_KEY,Browser.WEBKIT_GENERIC);
        }
        else {
            return parseVersion(ua,SIMPLE_VERSION_KEY,Browser.SAFARI);
        }
    }
    return unknown;
}

/**
 * @param ua user agent string
 * @param key
 * @param browser
 * @return {{majorVersion: number, minorVersion: number, browser}}
 */
function parseVersion(ua,key,browser) {
    let idx= ua.indexOf(key);
    if (idx!==-1) idx+= key.length;
    const ver= ua.substring(idx).match(/[0-9.]+/)?.[0];
    const sAry= ver?.split('\.') ?? [];
    return {
        browser,
        majorVersion: !isNaN(Number(sAry[0])) ? parseInt(sAry[0]) : UNKNOWN_VER,
        minorVersion: !isNaN(Number(sAry[1])) ? parseInt(sAry[1]) : UNKNOWN_VER
    };
}

function platformMajorVersion(ua,platform) {
    if (!ua) return 0;
    const osPart= ua.substring(ua.indexOf('(')+1, ua.indexOf(')'))?.split(';')[1];
    const osParts= osPart.split(/\s+/);
    const ver= osParts[osParts.length-1];
    const [majorVer,minorVer]= ver.split(/[._]/);
    return platform===Platform.MACOS ? Number(minorVer) : Number(majorVer);
}

function evaluatePlatform(ua) {
    if (!ua) return Platform.UNKNOWN;
    if  (ua.includes('window')) return Platform.WINDOWS;
    else if  (ua.includes('macintosh')) return Platform.MACOS;
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

function supportsWebGpu() {
    const foundGpu= Boolean(navigator.gpu);
    if (isBrowser(Browser.CHROME)) return isVersionAtLeast(113) && foundGpu;
    if (isBrowser(Browser.EDGE)) return isVersionAtLeast(113) && foundGpu;
    if (isBrowser(Browser.SAFARI)) return isVersionAtLeast(26) && foundGpu;
    if (isBrowser(Browser.FIREFOX)) return isVersionAtLeast(141) && foundGpu && Boolean(BrowserInfo.fireflyWebGpuEnabled);
    if (isBrowser(Browser.OPERA)) return isVersionAtLeast(99) && foundGpu;
    return foundGpu;
}