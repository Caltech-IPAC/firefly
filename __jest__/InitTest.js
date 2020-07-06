import * as jest from 'jest';
import {bootstrapRedux} from '../src/firefly/js/core/ReduxFlux';
import {getBootstrapRegistry} from '../src/firefly/js/core/BootstrapRegistry';


jest.mock('firefly/Firefly.js', () => {
    return {
    };
});

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

jest.mock('firefly/visualize/DrawLayerCntlr.js', () => {
    return {
        dispatchDestroyDrawLayer: () => '',
        getDlAry : () => [],
        getDrawLayerCntlrDef: () => ({
            reducers: () => ({}),
            actionCreators: () => ({})
        })
    };
});

bootstrapRedux(
    getBootstrapRegistry(),
    () => '',
    () => ''
);
