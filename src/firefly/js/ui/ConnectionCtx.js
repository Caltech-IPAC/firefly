import React from 'react';

export const ConnectionCtx = React.createContext( {
    controlConnected: false,
    setControlConnected: () => undefined
});
