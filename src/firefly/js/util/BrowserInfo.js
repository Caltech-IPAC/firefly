/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import pad from 'underscore.string/pad';
import validator from 'validator';


export const Browser = new Enum ([
    'FIREFOX', 'SEAMONKEY', 'SAFARI', 'WEBKIT_GENERIC', 'IE',
    'OPERA', 'CHROME', 'UNKNOWN'
]);



export const Platform = new Enum([
    'MAC',
    'WINDOWS',
    'LINUX',
    'SOLARIS',
    'SUNOS',
    'HPUX',
    'AIX',
    'IPHONE',
    'IPAD',
    'ANDROID',
    'FREE_BSD',
    'SYMBIAN_OS',
    'J2ME',
    'BLACKBERRY',
    'UNKNOWN'
]);

const UNKNOWN_VER= -1;
const SEAMONKEY_KEY= 'seamonkey/';
const FIREFOX_KEY= 'firefox/';
const SIMPLE_VERSION_KEY = 'version/';
const CHROME_KEY= 'chrome/';
const IE_11_KEY= 'rv:';


//var browser;
//var platform;
//var majorVersion;
//var minorVersion;
//var userAgent;


const BrowserInfo= {
    getBrowserType,
        isIE,
        isSafari,
        isFirefox,
        isChrome,
        isBrowser,
        isPlatform,
        isVersionAtLeast,
        isVersionBefore,
        getSupportsCSS3,
        getSupportsCORS,
        getPlatformDesc,
        getBrowserString,
        isTouchInput,
        getVersionString,
        getBrowserDesc
};


export default BrowserInfo;


function evaluate() {
    BrowserInfo.userAgent= navigator.userAgent.toLowerCase();
    BrowserInfo.platform= evaluatePlatform(BrowserInfo.userAgent);
    var ret= evaluateBrowser(BrowserInfo.userAgent,BrowserInfo.platform);
    Object.assign(BrowserInfo,ret);
    BrowserInfo.browser= ret.browser;
    BrowserInfo.majorVersion= ret.majorVersion;
    BrowserInfo.minorVersion= ret.minorVersion;
}

evaluate();


function getBrowserType() { return BrowserInfo.browser; }
function isIE() { return isBrowser(Browser.IE); }
function isSafari() { return isBrowser(Browser.SAFARI); }
function isFirefox() { return isBrowser(Browser.FIREFOX); }
function isChrome() { return isBrowser(Browser.CHROME); }

function isPlatform(inPlatform) { return  inPlatform===BrowserInfo.platform; }


function isBrowser(testBrowsers, version=UNKNOWN_VER ) {
    return testBrowsers===BrowserInfo.browser && (version==UNKNOWN_VER || version==BrowserInfo.majorVersion);
}


function isVersionAtLeast(inBrowser, version, minor= UNKNOWN_VER) {
    var retval= false;
    if (inBrowser===BrowserInfo.browser && version!==UNKNOWN_VER) {
        retval= BrowserInfo.majorVersion>version;
        if (BrowserInfo.majorVersion===version) {
            if (BrowserInfo.minorVersion!==UNKNOWN_VER && BrowserInfo.minorVersion>=minor) retval= true;
            else if (BrowserInfo.minorVersion===UNKNOWN_VER) retval= true;
        }
    }
    return retval;
}

function isVersionBefore(inBrowser, version, minor=UNKNOWN_VER) {
    return !isVersionAtLeast(inBrowser,version,minor);
}




function getSupportsCSS3() {
    var retval=  isBrowser(Browser.CHROME);

    if (!retval) retval= isBrowser(Browser.SAFARI) && BrowserInfo.majorVersion>=5;
    if (!retval) retval= isTouchInput();
    if (!retval) retval= isBrowser(Browser.FIREFOX) && BrowserInfo.majorVersion>3;
    if (!retval) retval= isBrowser(Browser.OPERA) && BrowserInfo.majorVersion>=10;
    if (!retval) retval= isIE() && BrowserInfo.majorVersion>=9;

    return retval;

}


function getSupportsCORS() {
    var retval=  true;
    if      (isFirefox() && isVersionBefore(Browser.FIREFOX,3,5)) retval= false;
    else if (isSafari() && isVersionBefore(Browser.SAFARI,4)) retval= false;
    else if (isIE() && isVersionBefore(Browser.IE,10)) retval= false;
    else if (isBrowser(Browser.OPERA) && isVersionBefore(Browser.OPERA,12)) retval= false;
    return retval;
}


function getPlatformDesc() { return BrowserInfo.platform.getDesc(); }

function getBrowserString() { return BrowserInfo.browser.getDesc(); }

function isTouchInput() {
    return BrowserInfo.platform===Platform.IPAD ||
        BrowserInfo.platform===Platform.IPHONE  ||
        BrowserInfo.platform===Platform.ANDROID;
}


function getVersionString() {
    return BrowserInfo.minorVersion!==UNKNOWN_VER ? `${BrowserInfo.majorVersion}.${BrowserInfo.minorVersion}` : BrowserInfo.majorVersion+'';
}


function getBrowserDesc() {
    if (BrowserInfo.minorVersion!==UNKNOWN_VER) {
        return pad(12, getBrowserString())+ 'Version: ' + pad(BrowserInfo.majorVersion + '.' +BrowserInfo.minorVersion,15);
    }
    else {
        return pad(getBrowserString(),12)+ 'Version: ' + pad(BrowserInfo.majorVersion+'',15);
    }
}

function evaluateBrowser(ua,p) {
    var retValue= {browser:Browser.UNKNOWN,majorV:UNKNOWN_VER, minorV:UNKNOWN_VER};
    if (ua.includes('msie')) {
        retValue.browser= Browser.IE;
        if (ua.includes('msie 11') && !ua.includes('trident')) {
            retValue.majorV= 11;
        }
        else if (ua.includes('msie 10')) {
            retValue.majorV= 10;
        }
        else if (ua.includes('msie 9')) {
            retValue.majorV= 9;
        }
        else if (ua.includes('msie 8')) {
            retValue.majorV= 8;
        }
        else if (ua.includes('msie 7')) {
            retValue.majorV= 7;
        }
        else if (ua.includes('msie 6')) {
            retValue.majorV= 6;
        }
    }
    else if (ua.includes('trident') && !ua.contains('edge')) {
        retValue= parseVersion(ua,IE_11_KEY);
        retValue.browser= Browser.IE;
    }else if (ua.includes('edge')) {
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
        else if (p==Platform.BLACKBERRY || p==Platform.SYMBIAN_OS || p==Platform.UNKNOWN) {
            retValue= parseVersion(ua,SIMPLE_VERSION_KEY);
            retValue.browser= Browser.WEBKIT_GENERIC;
        }
        else {
            retValue= parseVersion(ua,SIMPLE_VERSION_KEY);
            retValue.browser= Browser.SAFARI;
        }
    }
    else if  (ua.includes('opera')) {
        if (ua.includes('opera/9')) {
            retValue.majorVersion= 9;
        }
        else if (ua.includes('opera/8')) {
            retValue.majorV= 8;
        }
        else if (ua.includes('opera/7')) {
            retValue.majorV= 7;
        }
        else if (ua.includes('opera/6')) {
            retValue.majorV= 6;
        }
        retValue.browser= Browser.OPERA;
    }
    else {
        retValue.browser= Browser.UNKNOWN;
    }
    return retValue;
}


/**
 *
 * @param ua
 * @param key
 * @return {{majorV: number, minorV: number}}
 */
function parseVersion(ua,key) {
    var idx= ua.indexOf(key);
    if (idx!==-1) idx+= key.length;
    var ver= ua.substring(idx);

    var found= false;
    var i;
    for(i=0; (i<ver.length); i++) {
        if (!validator.isNumeric(ver[i]) && ver[i]!=='.') {
            found= true;
            break;
        }
    }
    if (found) ver= ver.substring(0,i);


    var sAry= ver.split('\.');
    var majorV= validator.isNumeric(sAry[0]) ? parseInt(sAry[0]) : UNKNOWN_VER;
    var minorV= validator.isNumeric(sAry[1]) ? parseInt(sAry[1]) : UNKNOWN_VER;
    return {majorV,minorV};
}


function evaluatePlatform(ua) {
    var p;
    if  (ua.includes('window')) {
        p = Platform.WINDOWS;
    }
    else if  (ua.includes('macintosh')) {
        p = Platform.MAC;
    }
    else if  (ua.includes('ipad')) {
        p = Platform.IPAD;
    }
    else if  (ua.includes('iphone')) {
        p = Platform.IPHONE;
    }
    else if  (ua.includes('linux')) {
        if (ua.includes('android')) p = Platform.ANDROID;
        else                        p = Platform.LINUX;
    }
    else if  (ua.includes('solaris')) {
        p = Platform.SOLARIS;
    }
    else if  (ua.includes('sunos')) {
        p = Platform.SUNOS;
    }
    else if  (ua.includes('aix')) {
        p = Platform.AIX;
    }
    else if  (ua.includes('hpux')) {
        p = Platform.HPUX;
    }
    else if  (ua.includes('freebsd')) {
        p = Platform.FREE_BSD;
    }
    else if  (ua.includes('symbianos')) {
        p = Platform.SYMBIAN_OS;
    }
    else if  (ua.includes('j2me')) {
        p = Platform.J2ME;
    }
    else if  (ua.includes('blackberry')) {
        p = Platform.BLACKBERRY;
    }
    else {
        p = Platform.UNKNOWN;
    }
    return p;
}


