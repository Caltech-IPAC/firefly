import React from 'react';
import {CssBaseline, ScopedCssBaseline, CssVarsProvider, useColorScheme, GlobalStyles} from '@mui/joy';
import {getTheme} from './ThemeSetup.js';





export function FireflyRoot({children}) {
    const newTheme= getTheme();
    const useBaseline= false;

    return (
        <CssVarsProvider theme={newTheme}>
            {useBaseline && <CssBaseline/>}
            <GlobalStyles styles={{
                    // CSS object styles
                    html: {fontSize:'87.5%'}
                }}/>
            <App>{children}</App>
        </CssVarsProvider>
    );
}

function App({children}) { // provide a way to experiment with light and dark theme
    const { mode, setMode } = useColorScheme();
    // setMode('dark');
    setMode('light');
    return ( children );
}

