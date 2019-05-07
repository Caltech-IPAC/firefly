import * as jest from 'jest';

// jest.mock('firefly/Firefly.js');
//
//-- we are going to have to do something like this eventually, I think, right now I can't make it work
jest.mock('firefly/Firefly.js', () => {
    return {
        flux : {
            process : () => {},
            getState : () => ({
                app_data : {},
                allPlots : {},
                fieldGroup: {}
            })
        }
    };
});


jest.mock('firefly/rpc/SearchServicesJson.js', () => {
    return {
        fetchTable: () => '',
        queryTable: () => '',
        selectedValues: () => '',
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

// jest.mock('firefly/util/WebUtil.js', () => {
//     return {
//         isDefined: (x) => x!==undefined,
//         replaceAll: (str, find, replace) => str.replace(new RegExp(find, 'g'), replace),
//         getProp: (s) => s,
//         parseUrl: (s) => s
//     };
// });



jest.mock('firefly/visualize/ImagePlotCntlr.js', () => {
    return {
        ExpandType: '', WcsMatchType: '',
    };
});

jest.mock('firefly/visualize/DrawLayerCntlr.js', () => {
    return {
        dispatchDestroyDrawLayer: () => '',
        getDlAry : () => [],
    };
});

jest.mock('firefly/visualize/WorkspaceCntlr.js', () => {
    return {
        WS_HOME : 'WS_Home'
    };
});


jest.mock('firefly/tables/TablesCntlr.js', () => {
    return {
        TABLE_UPDATE:'',TABLE_REPLACE:'',TABLE_SPACE_PATH: '', TABLE_LOADED: 'table.loaded',
        dispatchTableFilter: () => '',
        dispatchTableSelect: () => '',
    };
});

jest.mock('firefly/visualize/draw/DrawOp.js', () => {
    return {
        getCenterPt: (p) => p,
    };
});

jest.mock('firefly/drawingLayers/SelectArea.js', () => {
    return {
        SelectedShape :{ circle:{key:'circle',}, square: {key: 'square'}}
    };
});


jest.mock('firefly/ui/PopupUtil.jsx', () => {
    return {
        showInfoPopup: () => '',
    };
});

jest.mock('firefly/ui/FileUpload.jsx', () => {
    return {
        doUpload: () => '',
    };
});



jest.mock('firefly/core/AppDataCntlr.js', () => {
    return {
        getAppOptions : () => ({})
    };
});

jest.mock('firefly/core/MasterSaga.js', () => {
    return {
        dispatchCancelActionWatcher: () => '',
        dispatchAddActionWatcher: () => ''
    };
});

jest.mock('firefly/core/messaging/WebSocketClient.js', () => {
    return {
        getWsConnId: () => '',
        dispatchAddActionWatcher: () => ''
    };
});
