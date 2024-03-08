import React from 'react';

export const AppPropertiesCtx = React.createContext( {
    appTitle: undefined,
    template: undefined,
    landingPage: undefined,
    footer: undefined,
    showUserInfo: false,
    fileDropEventAction: 'FileUploadDropDownCmd',
});
