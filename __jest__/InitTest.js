import * as jest from 'jest';
import {bootstrapRedux} from '../src/firefly/js/core/ReduxFlux';
import {getBootstrapRegistry} from '../src/firefly/js/core/BootstrapRegistry.js';


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

jest.mock('firefly/core/background/BackgroundMonitor.jsx', () => {
    return {
        showBackgroundMonitor: () => '',
    };
});

jest.mock('firefly/core/LayoutCntlr.js', () => {
    return {
        CHART_ADD: 'CHART_ADD',
        CHART_REMOVE: 'CHART_REMOVE',
        CHART_SPACE_PAT:'CHART_SPACE_PAT',
        LAYOUT_PATH: 'layout',
        reducer: (x) => x ?? {},
    };
});

jest.mock('firefly/ui/UploadTableChooser.js', () => {
    return {
        showUploadTableChooser: () => '',
    };
});


jest.mock('firefly/ui/ExampleDialog.jsx', () => {
    return {
        showExampleDialog: () => '',
    };
});

jest.mock('firefly/ui/ActionsDropDownButton.jsx', () => {
    return {
        ActionsDropDownButton: () => '',
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

bootstrapRedux( getBootstrapRegistry());
