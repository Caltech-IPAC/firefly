import * as jest from 'jest';

jest.mock('firefly/Firefly.js');
jest.mock('firefly/util/BrowserInfo.js', () => {
    return {
        getBrowserType: () => '',
        isIE: () => false,
        isSafari: () => false,
        isFirefox: () => false,
        isChrome: () => true,
        isBrowser: () => false,
        isPlatform: () => false,
        isVersionAtLeast: () => false,
        isVersionBefore: () => false,
        getSupportsCSS3: () => false,
        getSupportsCORS: () => false,
        getPlatformDesc: () => '',
        getBrowserString: () => 'chrome',
        isTouchInput: () => false,
        getVersionString: () => '1.2.3',
        getBrowserDesc: () => 'chrome'

    };
});

