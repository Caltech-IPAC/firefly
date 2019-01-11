import * as jest from 'jest';

jest.mock('firefly/Firefly.js');
//
// -- we are going to have to do something like this eventually, I think, right now I can't make it work
// jest.mock('firefly/Firefly.js', () => {
//     return {
//         flux : {
//             process : () => {},
//             getState : () => ({
//                 app_data : {},
//                 allPlots : {},
//                 fieldGroup: {}
//             })
//         }
//     }
// });


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


jest.mock('firefly/core/AppDataCntlr.js', () => {
    return {
        getAppOptions : () => ({})
    }
});
